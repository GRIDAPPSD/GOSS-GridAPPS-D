'''
Created on Aug 15, 2017

@author: thay838
'''
from genetic import individual
import math
import random
import os
from queue import Queue
import threading
import util.db
import sys
import copy

class population:

    def __init__(self, strModel, numInd, numGen, inPath, outDir, reg, cap,
                 starttime, stoptime, numThreads=os.cpu_count(),
                 energyPrice=0.00008, tapChangeCost=0.5, capSwitchCost=2,
                 voltCost = 0.5,
                 individualsList=[], nextUID=0, topPct=0.2, weakProb=0.2, 
                 popMutateProb=0.2, crossProb=0.7, capGeneMutateProb=0.1,
                 regGeneMutateProb=0.05):
        """Initialize a population of individuals.
        
        INPUTS:
            numInd: Number of individuals to create
            numGen: Number of generations to run
            inPath: Path and filename to the base GridLAB-D model to be 
                modified.
            outDir: Directory for new models to be written.
        """
        # TODO: rather than being input, reg and cap should be read from the
        # CIM.   
        
        # Get database connection pool (threadsafe)
        # self.cnxnpool = util.db.connectPool(pool_name='popObjPool',
        #                           pool_size=numThreads)
        
        # Define some globals for use by the threads.
        # These would be class constants, except we don't want to have to pass
        # in a whole object or a million inputs to the thread-target function.
        global STARTTIME
        STARTTIME = starttime
        global STOPTIME
        STOPTIME = stoptime
        global ENERGYPRICE
        ENERGYPRICE = energyPrice
        global TAPCHANGECOST
        TAPCHANGECOST = tapChangeCost
        global CAPSWITCHCOST
        CAPSWITCHCOST = capSwitchCost
        global VOLTCOST
        VOLTCOST = voltCost
        
        # Initialize queues and threads for running GLD models in parallel.
        self.threads = []
        self.qIn = Queue()
        self.qOut = Queue()
        
        # Set times
        self.starttime = starttime
        self.stoptime = stoptime
        
        # Start the threads.
        for _ in range(numThreads):
            #t = threading.Thread(target=population.writeRunEval,
            #           args=(self.qIn, self.qOut, self.cnxnpool))
            t = threading.Thread(target=population.writeRunEval,
                                 args=(self.qIn,))
            self.threads.append(t)
            t.start()

        #print('Threads started.', flush=True)
        
        # Seed random module
        random.seed()
        
        # Set population base model
        self.strModel = strModel
        
        # Set the number of generations and individuals.
        self.numGen = numGen
        self.numInd = numInd
        
        # Set regulators and capacitors as property
        self.reg = reg
        self.cap = cap
        
        # Set modelIn and outDir
        self.inPath = inPath
        self.outDir = outDir
        
        # Track the best scores for each generation.
        self.generationBest = []
        
        # Track the sum of fitness - used to compute roulette wheel weights
        self.fitSum = None
        
        # Track weights of fitness for "roulette wheel" method
        self.rouletteWeights = None
        
        # Assign probabilites, etc.
        self.topPct = topPct
        self.weakProb = weakProb 
        self.popMutateProb = popMutateProb
        self.crossProb = crossProb
        self.capGeneMutateProb = capGeneMutateProb
        self.regGeneMutateProb = regGeneMutateProb
        
        # Initialize individuals.
        # TODO: make this loop parallel?
        # TODO: individuals should take strModel + paths as input to
        # constructor
        self.individualsList = individualsList
        self.nextUID = nextUID
        
        # Create 'extreme' indivuals - all caps in/out, regs maxed up/down
        c = len(self.individualsList)
        for allCap in individual.CAPSTATUS:
            for peg in individual.REGPEG:
                self.individualsList.append(\
                    individual.individual(uid=self.nextUID,
                                          reg=copy.deepcopy(reg),
                                          peg=peg,
                                          cap=copy.deepcopy(cap),
                                          allCap=allCap,
                                          starttime=self.starttime,
                                          stoptime=self.stoptime
                                          )
                                            )
                c += 1
                self.nextUID += 1
                
        # Create individuals with biased regulator positions
        # TODO: Stop hard-coding the number.
        # TODO: Consider leaving capacitors the same.
        for _ in range(c, c+4):
            self.individualsList.append(\
                individual.individual(uid=self.nextUID,
                                      reg=copy.deepcopy(reg),
                                      regBias=True,
                                      cap=copy.deepcopy(cap),
                                      starttime=self.starttime,
                                      stoptime=self.stoptime
                                     )
                                        )
            c += 1
            self.nextUID += 1
        
        # Randomly create the rest of the individuals.
        for _ in range(c, numInd):
            # Initialize individual.
            self.individualsList.append(\
                individual.individual(uid=self.nextUID,
                                      reg=copy.deepcopy(reg), 
                                      cap=copy.deepcopy(cap),
                                      starttime=self.starttime,
                                      stoptime=self.stoptime
                                      )
                                        )
            self.nextUID += 1
            
        
    def ga(self):
        """Main function to run the genetic algorithm.
        """
        g = 0
        # Put all individuals in the queue for processing.
        for individual in self.individualsList:
            # TODO: Making so many copies of 'strModel' feels REALLY
            # inefficient...
            self.qIn.put_nowait({'individual':individual,
                                 'strModel': self.strModel,
                                 'inPath': self.inPath,
                                 'outDir': self.outDir})
        # Loop over the generations
        while g < self.numGen:
            # Wait until all models have been run and evaluated.
            self.qIn.join()
                
            # Sort the individualsList by score.
            self.individualsList.sort(key=lambda x: x.fitness)
            
            # Track best score for this generation.
            self.generationBest.append(self.individualsList[0].fitness)
            
            # Increment generation counter.
            g += 1
            
            # TODO: performing this check again is annoyingly inefficient
            if g < self.numGen:
                # Select the fittest individuals and some unfit ones.
                self.naturalSelection()
                
                # Get the number of remaining individuals
                n = len(self.individualsList)
                
                # Measure diversity
                # regDiff, capDiff = self.measureDiversity()
                
                # Replenish the population by crossing individuals.
                self.crossAndMutate()
                
                # Put the new individuals in the queue for processing
                # TODO: If this is performed in crossAndMutate, we'll see a
                # small speedup and a moderate code simplification.
                for ind in range(n, len(self.individualsList)):
                    self.qIn.put_nowait({'individual':self.individualsList[ind],
                                         'strModel': self.strModel,
                                         'inPath': self.inPath,
                                         'outDir': self.outDir})
            
        # Signal to threads that we're done.
        for _ in self.threads: self.qIn.put_nowait(None)
        for t in self.threads: t.join()
        #print('Threads terminated.', flush=True)
        
        # Return the best individual.
        return self.individualsList[0]
                
    @staticmethod 
    def writeRunEval(qIn):
                    #, cnxnpool):
        #tEvent):
        """Write individual's model, run the model, and evaluate fitness.
        
        NOTE: will take no action if an individual's model has already been
            run.
        
        NOTE: This function is static due to the threading involved. This feels
            memory inefficient but should save some headache.
            
        NOTE: This function is specifically formatted to be run via a thread
            object which is terminated when a 'None' object is put in the 
            qIn.
            
        NOTE: This function depends on population's __init__ method setting
            some global variables.
            
        INPUTS:
            qIn: dictionary with individual, strModel, inPath, outDir fields
                from a population object.
            qOut: queue to put the modified indivdual in. NOTE: if the
                a given individual has already been run, None will be placed
                instead. Queue contains pairs of (uid, individual)
        """
        while True:
            try:
                # Extract an individual from the queue.
                inDict = qIn.get()
                
                # Check input.
                if inDict is None:
                    # If None is returned, we're all done here.
                    qIn.task_done()
                    # TODO: we should probably close if an error occurs.
                    #print('Thread {} terminating'.format(os.getpid()),
                    #      flush=True)
                    break
                
                # Connect to the database. For some reason, we have to get a new
                # connection for each iteration, otherwise we'll get that nasty
                # '1412 (HY000): Table definition has changed, please retry
                # transaction' error. It took way too long to find this as the 
                # problem...
                #cnxn = cnxnpool.get_connection()
                cnxn = util.db.connect()
                cursor = cnxn.cursor()
                
                # Write the individual's model.
                inDict['individual'].writeModel(strModel=inDict['strModel'],
                                               inPath=inDict['inPath'],
                                               outDir=inDict['outDir'])

                # Run the model.
                # TODO: need gridlabd path information here
                inDict['individual'].runModel()
                if inDict['individual'].modelOutput.returncode:
                    print("FAILURE! Individual {}'s model gave non-zero returncode.".format(inDict['individual'].uid))
                
                # Evaluate the individuals fitness.
                inDict['individual'].evalFitness(cursor,
                                                 energyPrice=ENERGYPRICE,
                                                 tapChangeCost=TAPCHANGECOST,
                                                 capSwitchCost=CAPSWITCHCOST,
                                                 voltCost=VOLTCOST
                                                 )
            
                # Denote task as complete.
                qIn.task_done()
                
            except:
                print('Exception occurred!', flush=True)
                error_type, error, traceback = sys.exc_info
                print(error_type, flush=True)
                print(error, flush=True)
                print(traceback, flush=True)
            finally:
                cursor.close()
                cnxn.close()
                
    def naturalSelection(self):
        """Determines which individuals will be used to create next generation.
        """
        # Determine how many individuals to keep for certain.
        k = math.ceil(self.topPct * len(self.individualsList))
        
        # Loop over the unfit individuals, and either delete or keep based on
        # the weakProb
        while len(self.individualsList) > k:
            # Randomly decide whether or not to keep an unfit individual
            if random.random() > self.weakProb:
                # Delete this individual
                del self.individualsList[k]
            else:
                # Increment k
                k += 1
        
        # Compute fitness sum for surviving individuals and assign a weight
        self.fitSum = 0
        # TODO: If locks are used, the threaded fitness computation could add
        # to the fitSum, and we could cut down on some looping.
        for individual in self.individualsList:
            # Add the score.
            self.fitSum += individual.fitness
            '''
            # Give the weight as a fraction of the best score. Higher weight
            # means higher likelihood of being picked for crossing/mutation
            self.rouletteWeights.append(self.individualsList[0].fitness
                                        / individual.fitness)
            '''
            '''
            self.rouletteWeights.append(round(3 ** 
                                        (self.individualsList[-1].fitness
                                         / individual.fitness), 2))
            '''
        self.rouletteWeights = []
        for individual in self.individualsList:
            self.rouletteWeights.append(1 / (individual.fitness/self.fitSum))
    
    def crossAndMutate(self):
        """Crosses traits from surviving individuals to regenerate population.
        
        INPUTS:
            popMutate: chance of an individual to enter the mutation phase
            crossChance: chance of an individual being created by crossover
            mutateChance: chance an individual gene mutates
        """
        # Loop until population has been replenished.
        # Extract the number of individuals.
        n = len(self.individualsList)
        #chooseCount = []
        while len(self.individualsList) < self.numInd:
            if random.random() < self.crossProb:
                # Since we're crossing over, we won't force a mutation.
                forceMutate = False

                # Prime loop to select two unique individuals. Loop ensures
                # unique individuals are chosen.
                _individualsList = [0, 0]
                while _individualsList[0] == _individualsList[1]:
                    # Pick two individuals based on cumulative weights.
                    _individualsList = random.choices(self.individualsList[0:n],
                                                      weights=\
                                                        self.rouletteWeights,
                                                      k=2)
                # Keep track of who created these next individuals.
                parents = (_individualsList[0].uid, _individualsList[1].uid)
                
                # Cross the regulator chromosomes
                regChroms = self.crossChrom(
                                        chrom1=_individualsList[0].regChrom,
                                        chrom2=_individualsList[1].regChrom)
                
                # Cross the capaictor chromosomes
                capChroms = self.crossChrom(
                                        chrom1=_individualsList[0].capChrom,
                                        chrom2=_individualsList[1].capChrom)
                
                # TODO: Cross DER chromosomes
            else:
                # We're not crossing over, so force mutation.
                forceMutate = True
                # Draw an individual.
                _individualsList = random.choices(self.individualsList[:n],
                                                  weights=self.rouletteWeights,
                                                  k=1)
                
                # Track parents
                parents = (_individualsList[0].uid,)
                # Grab the necessary chromosomes, put in a list
                regChroms = [_individualsList[0].regChrom]
                capChroms = [_individualsList[0].capChrom]
                # TODO. DER chromosome.
            
            # Track chosen individuals.
            """
            for i in _individualsList:
                uids = [x[1] for x in chooseCount]
                if i.uid in uids:
                    ind = uids.index(i.uid)
                    # Increment the occurence count
                    chooseCount[ind][2] += 1
                else:
                    chooseCount.append([i.fitness, i.uid, 1])
            """
            
            # Possibly mutate individual(s).
            if forceMutate or (random.random() < self.popMutateProb):
                # Mutate regulator chromosome:
                regChroms = self.mutateChroms(c=regChroms,
                                            mutateChance=self.regGeneMutateProb)
                # Mutate capacitor chromosome:
                capChroms = self.mutateChroms(c=capChroms,
                                            mutateChance=self.capGeneMutateProb)
                # TODO: Mutate DER chromosome
                """
                print('Reg: {} genes mutated. Cap: {} genes mutated.'.format(
                        rCount, cCount), flush=True)
                """
            
            # Create individuals based on new chromosomes,
            # add to individualsList
            for i in range(len(regChroms)):
                self.individualsList.append(
                    individual.individual(uid=self.nextUID, 
                                          regChrom=regChroms[i],
                                          capChrom=capChroms[i],
                                          reg=copy.deepcopy(
                                              _individualsList[0].reg),
                                          cap=copy.deepcopy(
                                              _individualsList[0].cap
                                                            ),
                                          parents=parents,
                                          starttime=self.starttime,
                                          stoptime=self.stoptime
                                          )
                                            )
                
                # Increment UID
                self.nextUID += 1
        
        """
        # Sort the chooseCount by number of occurences
        chooseCount.sort(key=lambda x: x[2])
        print('Fitness, UID, Occurences', flush=True)
        for el in chooseCount:
            print('{:.2f},{},{}'.format(el[0], el[1], el[2]))
        """
        
    @staticmethod
    def mutateChroms(c, mutateChance):
        """Take a chromosome and randomly mutate it.
        
        INPUTS:
            c: list of chromsomes, which are tuples of 1's and 0's. Ex: (1, 0, 0, 1, 0)
            mutateChance: decimal in set [0.0, 1.0] to determine chance of
                mutating (bit-flipping) an individual gene
        """
        out = []
        for chrom in c:
            newC = list(chrom)
            #count = 0
            for ind in range(len(c)):
                if random.random() < mutateChance:
                    # Flip the bit!
                    newC[ind] = 1 - newC[ind]
                    #count += 1
                    
            # Convert to tuple, put in output list
            out.append(tuple(newC))
            
        return out
    
    @staticmethod
    def crossChrom(chrom1, chrom2):
        """Take two chromosomes and create two new ones.
        
        INPUTS:
            chrom1: tuple of 1's and 0's, same length as chrom2
            chrom2: tuple of 1's and 0's, same length as chrom1
            
        OUTPUTS:
            c1: new list of 1's and 0's, same length as chrom1 and 2
            c2: ""
        """
        # Force chromosomes to be same length
        assert len(chrom1) == len(chrom2)
        
        # Randomly determine range of crossover
        r = range(random.randint(0, len(chrom1)), len(chrom1))
        
        # Initialize the two chromosomes to be copies of 1 and 2, respectively
        c1 = list(chrom1)
        c2 = list(chrom2)
        
        # Loop over crossover range
        for k in r:
            # Note that random() is on interval [0.0, 1.0). Thus, we'll
            # consider [0.0, 0.5) and [0.5, 1.0) for our intervals. 
            # Note that since we initialized chrom to be a copy of chrom1, 
            # there's no need for an else case.
            if random.random() < 0.5:
                # Crossover.
                c1[k] = chrom2[k]
                c2[k] = chrom1[k]
                
        # Return the new chromosomes
        return [tuple(c1), tuple(c2)]
    
    def measureDiversity(self):
        """Function to loop over chromosomes and count differences between
        individuals. This information is useful in a histogram.
        """
        # Compute diversity
        n = 0
        regDiff = []
        capDiff = []
        # Loop over all individuals in the list
        for ind in self.individualsList:
            n += 1
            # Loop over all individuals later in the list
            for i in range(n, len(self.individualsList)):
                # Loop over reg chrom, count differences.
                regCount = 0
                for g in range(0, len(ind.regChrom)):
                    if ind.regChrom[g] != self.individualsList[i].regChrom[g]:
                        regCount += 1
                        
                regDiff.append(regCount)
                
                # Loop over cap chrom, count differences.
                capCount = 0
                for g in range(0, len(ind.capChrom)):
                    if ind.capChrom[g] != self.individualsList[i].capChrom[g]:
                        capCount += 1
                        
                capDiff.append(capCount)
                
        return regDiff, capDiff

if __name__ == "__main__":
    pass
"""
if __name__ == "__main__":
    import time
    #import matplotlib.pyplot as plt
    n = 1
    f = open('C:/Users/thay838/Desktop/vvo/output.txt', 'w')
    for k in range(n):
        print('*' * 80, file=f)
        print('Generation {}'.format(k), file=f)
        t0 = time.time()
        popObj = population(numInd=100, numGen=10,
                            modelIn='C:/Users/thay838/Desktop/R2-12.47-2.glm',
                            reg={'R2-12-47-2_reg_1': {
                                                    'raise_taps': 16, 
                                                    'lower_taps': 16,
                                                    'taps': [
                                                        'tap_A',
                                                        'tap_B',
                                                        'tap_C'
                                                    ]
                                                   },
                              'R2-12-47-2_reg_2': {
                                                    'raise_taps': 16,
                                                    'lower_taps': 16,
                                                    'taps': [
                                                        'tap_A',
                                                        'tap_B',
                                                        'tap_C'
                                                    ]
                                                   }
                            },
                            cap={
                                'R2-12-47-2_cap_1': ['switchA', 'switchB', 'switchC'],
                                'R2-12-47-2_cap_2': ['switchA', 'switchB', 'switchC'],
                                'R2-12-47-2_cap_3': ['switchA', 'switchB', 'switchC'],
                                'R2-12-47-2_cap_4': ['switchA', 'switchB', 'switchC']
                            },
                            outDir='C:/Users/thay838/Desktop/vvo'
                            )
        popObj.ga()
        t1 = time.time()
        print('Runtime: {:.0f} s'.format(t1-t0), file=f)
        print('Scores: ', file=f)
        for s in popObj.generationBest:
            print('{:.4g}'.format(s), end=', ', file=f)
            
        print(file=f)
        print('Best Individual:', file=f)
        bestUID = popObj.indFitness[0][UIDIND]
        for ix in popObj.individualsList:
            if ix.uid == bestUID:
                print('\tCapacitor settings:', file=f)
                for capName, capDict in ix.cap.items():
                    print('\t\t' + capName + ':', file=f)
                    for switchName, switchDict in capDict.items():
                        print('\t\t\t' + switchName + ': ' 
                              + switchDict['status'], file=f)
                print(file=f)
                
                print('\tRegulator settings:', file=f)
                for regName, regDict in ix.reg.items():
                    print('\t\t' + regName + ':', file=f)
                    for tapName, tapDict in regDict['taps'].items():
                        print('\t\t\t' + tapName + ': ' + str(tapDict['pos']),
                              file=f)
                    
                break
            else:
                pass
        print('*' * 80, file=f, flush=True)
        #x = list(range(len(popObj.generationBest)))
        #plt.plot(x, popObj.generationBest)
        #plt.xlabel('Generations')
        #plt.ylabel('Best Score')
        #plt.title('Best Score for Each Generation')
        #plt.grid(True)
        #plt.savefig('C:/Users/thay838/Desktop/vvo/run_{}.png'.format(k))
        #plt.close()
        #plt.show()
        """