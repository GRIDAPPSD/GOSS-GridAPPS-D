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
from util import db
import sys
import copy
    
class population:

    def __init__(self, strModel, numInd, numGen, inPath, outDir, reg, cap,
                 starttime, stoptime, numThreads=os.cpu_count(),
                 energyPrice=0.00008, tapChangeCost=0.5, capSwitchCost=2):
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
        # self.cnxnpool = db.connectPool(pool_name='popObjPool',
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
        
        # Initialize individuals.
        # TODO: make this loop parallel?
        # TODO: individuals should take strModel + paths as input to
        # constructor
        self.individualsList = []
        
        # Create 'extreme' indivuals - all caps in/out, regs maxed up/down
        c = 0
        for allCap in individual.CAPSTATUS:
            for peg in individual.REGPEG:
                self.individualsList.append(individual.individual(uid=c,
                                                          reg=copy.deepcopy(reg),
                                                          peg=peg,
                                                          cap=copy.deepcopy(cap),
                                                          allCap=allCap))
                c += 1
                
        # Create individuals with biased regulator positions
        # TODO: Stop hard-coding the number.
        # TODO: Consider leaving capacitors the same.
        for n in range(c, c+4):
            self.individualsList.append(individual.individual(uid=n,
                                                      reg=copy.deepcopy(reg),
                                                      regBias=True,
                                                      cap=copy.deepcopy(cap)))
            c += 1
        
        # Randomly create the rest of the individuals.
        for n in range(c, numInd):
            # Initialize individual.
            self.individualsList.append(individual.individual(uid=n, 
                                                      reg=copy.deepcopy(reg), 
                                                      cap=copy.deepcopy(cap)))
            
        # Track the current UID
        self.nextUID = n + 1
        
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
                
                # Replenish the population by crossing individuals.
                self.crossAndMutate()
                
                # Put the new individuals in the queue for processing
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
                cnxn = db.connect()
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
                                                 starttime=STARTTIME,
                                                 stoptime=STOPTIME,
                                                 energyPrice=ENERGYPRICE,
                                                 tapChangeCost=TAPCHANGECOST,
                                                 capSwitchCost=CAPSWITCHCOST)
            
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
                
    def naturalSelection(self, top=0.2, keepProb=0.2):
        """Determines which individuals will be used to create next generation.
        
        INPUTS:
            top: decimal in set (0, 1). Determines percentage of top
                individuals to be used to regenerate the population
            keepProb: decimal in set [0, 1). Probability another random
                individual is kept to regenerate the population
        """
        # Determine how many individuals to keep for certain.
        k = math.ceil(top * len(self.individualsList))
        
        # Loop over the unfit individuals, and either delete or keep based on
        # the keepProb
        while len(self.individualsList) > k:
            # Randomly decide whether or not to keep an unfit individual
            if random.random() > keepProb:
                # Delete this individual
                del self.individualsList[k]
            else:
                # Increment k
                k += 1
        
        # Compute fitness sum for surviving individuals and assign a weight
        self.fitSum = 0
        self.rouletteWeights = []
        for individual in self.individualsList:
            # Add the score.
            self.fitSum += individual.fitness
            # Give the weight as a fraction of the best score. Higher weight
            # means higher likelihood of being picked for crossing/mutation
            self.rouletteWeights.append(self.individualsList[0].fitness
                                        / individual.fitness)
    
    def crossAndMutate(self, popMutate=0.2, crossChance=0.7, mutateChance=0.1):
        """Crosses traits from surviving individuals to regenerate population.
        
        INPUTS:
            popMutate: chance of an individual to enter the mutation phase
            crossChance: chance of an individual being created by crossover
            mutateChance: chance an individual gene mutates
        """
        # Loop until population has been replenished.
        # Extract the number of individuals.
        n = len(self.individualsList)
        
        while len(self.individualsList) < self.numInd:
            if random.random() < crossChance:
                # Since we're crossing over, we won't force a mutation.
                forceMutate = False

                # Prime loop to select two unique individuals. Loop ensures
                # unique individuals are chosen.
                _individualsList = [0, 0]
                while _individualsList[0] == _individualsList[1]:
                    # Pick two individuals based on cumulative weights.
                    _individualsList = random.choices(self.individualsList[:n],
                                                      weights=\
                                                        self.rouletteWeights,
                                                      k=2)
            
                # Cross the regulator chromosomes
                regChrom = self.crossChrom(chrom1=_individualsList[0].regChrom,
                                           chrom2=_individualsList[1].regChrom)
                
                # Cross the capaictor chromosomes
                capChrom = self.crossChrom(chrom1=_individualsList[0].capChrom,
                                           chrom2=_individualsList[1].capChrom)
                
                # TODO: Cross DER chromosomes
            else:
                # We're not crossing over, so force mutation.
                forceMutate = True
                # Draw an individual.
                _individualsList = random.choices(self.individualsList[:n],
                                                  weights=self.rouletteWeights,
                                                  k=1)
                
                # Grab the necessary chromosomes
                regChrom = _individualsList[0].regChrom
                capChrom = _individualsList[0].capChrom
                # TODO. DER chromosome.
            
            # Possibly mutate this new individual.
            if forceMutate or (random.random() < popMutate):
                # Mutate regulator chromosome:
                regChrom = self.mutateChrom(c=regChrom,
                                            mutateChance=mutateChance)
                # Mutate capacitor chromosome:
                capChrom = self.mutateChrom(c=capChrom,
                                            mutateChance=mutateChance)
                # TODO: Mutate DER chromosome
            
            # Create individual based on new chromosomes, add to individualsList
            self.individualsList.append(
                individual.individual(uid=self.nextUID, 
                                      regChrom=regChrom,
                                      capChrom=capChrom,
                                      reg=copy.deepcopy(
                                          _individualsList[0].reg),
                                      cap=copy.deepcopy(
                                          _individualsList[0].cap)
                                      )
                                        )
            
            # Increment UID
            self.nextUID += 1
            
    @staticmethod
    def mutateChrom(c, mutateChance):
        """Take a chromosome and randomly mutate it.
        
        INPUTS:
            c: chromosome, list of 1's and 0's. Ex: [1, 0, 0, 1, 0]
            mutateChance: decimal in set [0.0, 1.0] to determine chance of
                mutating (bit-flipping) and individual gene
        """
        for ind in range(len(c)):
            if random.random() < mutateChance:
                # Flip the bit!
                c[ind] = 1 - c[ind]
        
        return c
    
    @staticmethod
    def crossChrom(chrom1, chrom2):
        """Take two chromosomes and create a new one
        
        INPUTS:
            chrom1: list of 1's and 0's, same length as chrom2
            chrom2: list of 1's and 0's, same length as chrom1
            
        OUTPUTS:
            chrom: new list of 1's and 0's, same length as chrom1 and 2
        """
        # Force chromosomes to be same length
        assert len(chrom1) == len(chrom2)
        
        # Randomly determine range of crossover
        r = range(random.randint(0, len(chrom1)), len(chrom1))
        
        # Initialize chrom to return to be copy of chrom1
        chrom = chrom1
        
        # Loop over crossover range
        for k in r:
            # Note that random() is on interval [0.0, 1.0). Thus, we'll
            # consider [0.0, 0.5) and [0.5, 1.0) for our intervals. 
            # Note that since we initialized chrom to be a copy of chrom1, 
            # there's no need for an else case.
            if random.random() < 0.5:
                chrom[k] = chrom2[k]
                
        # Return the new chromosome
        return chrom

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