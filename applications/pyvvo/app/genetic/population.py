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
                 starttime, stoptime, voltdumpFiles,
                 numModelThreads=os.cpu_count(),
                 costs = {'energy': 0.00008, 'tapChange': 0.5, 'capSwitch': 2,
                          'undervoltage': 0.05, 'overvoltage': 0.05},
                 probabilities = {'top': 0.2, 'weak': 0.2, 'mutate': 0.2,
                                  'cross': 0.7, 'capMutate': 0.1,
                                  'regMutate': 0.05},
                 baseControlFlag=None
                 ):
        """Initialize a population of individuals.
        
        INPUTS:
            numInd: Number of individuals to create
            numGen: Number of generations to run
            inPath: Path and filename to the base GridLAB-D model to be 
                modified.
            outDir: Directory for new models to be written.
            reg: Dictionary as described in gld.py docstring
            cap: Dictionary as described in gld.py docstring
            starttime: simulation starttime
            stoptime: simulation stoptime
            voltdumpFiles: listing of voltdump file names
            numModelThreads: number of threads for running models. Since the
                threads start subprocesses, this corresponds to number of
                cores used for simulation.
                
            costs: Dictionary describing costs associated with model fitness.
                energy: price of energy, $/Wh
                tapChange: cost to move one tap one position, $
                capSwitch: cost to switch a single capacitor phase, $
                undervoltage: cost of undervoltage violations, $.
                overvoltage: cost of overvoltage violations, $.
                    
            probabilities: Dictionary describing various probabilities
                associated with the genetic algorithm.
                
                top: THIS IS NOT A PROBABILITY. Decimal representing how many
                    of the top individuals to keep between generations. [0, 1)
                weak: probability of keeping an individual which didn't make
                    the 'top' cut for the next generation
                mutate: probability to mutate a given offspring or
                    individual.
                cross: While replenishing population, chance to cross two 
                    individuals (rather than just mutate one individual)
                capMutate: probability each capacitor chromosome gene gets
                    mutated.
                regMutate: probability each regulator chromosome gene gets
                    mutated.
            baseControlFlag: control flag for baseline individual. See inputs
                to an individual's constructor for details.
            
        """
        # TODO: rather than being input, reg and cap should be read from the
        # CIM.
        
        # Initialize list to hold all individuals in population.
        self.individualsList = []
        
        # To ensure all individual uid's are unique, track uids.
        self.nextUID = 0
        
        # Set voltdumpFile.
        self.voltdumpFiles = voltdumpFiles
        
        # Assign costs.
        self.costs = costs
        
        # Assign probabilites.
        self.probabilities = probabilities

        # Seed random module
        random.seed()
        
        # Set the number of generations and individuals.
        self.numGen = numGen
        self.numInd = numInd
        
        # Set inPath and outDir
        self.inPath = inPath
        self.outDir = outDir
        
        # Track the baseControlFlag
        self.baseControlFlag = baseControlFlag
        
        # Initialize queues and threads for running GLD models in parallel and
        # cleaning up models we're done with.
        self.modelThreads = []
        self.modelQueue = Queue()
        self.cleanupThreads = []
        self.cleanupQueue = Queue()
        
        # Start the threads to be used for running GridLAB-D models. These 
        # models are run in a seperate subprocess, so we need to be sure this
        # is limited to the number of available cores.
        for _ in range(numModelThreads):
            #t = threading.Thread(target=population.writeRunEval,
            #           args=(self.modelQueue, self.qOut, self.cnxnpool))
            t = threading.Thread(target=writeRunEval, args=(self.modelQueue,
                                                            self.costs,
                                                            )
                                 )
            self.modelThreads.append(t)
            t.start()
            
        # Start the threads to be used for cleaning up after model runs. Each
        # generation we'll be removing approximately (topPct + weakProb) * 
        # numInd. We might be able to bump this higher later.
        for _ in range(math.ceil((self.probabilities['top']
                                    + self.probabilities['weak'])
                                 * self.numInd)):
            t = threading.Thread(target=individual.cleanup, 
                                 args=(self.cleanupQueue,))
            self.cleanupThreads.append(t)
            t.start()
        
        # Call the 'prep' function which sets several object attributes AND
        # initializes the population.
        self.prep(starttime=starttime, stoptime=stoptime, strModel=strModel,
                  cap=cap, reg=reg)
        
        # Get database connection pool (threadsafe)
        # self.cnxnpool = util.db.connectPool(pool_name='popObjPool',
        #                           pool_size=numModelThreads)
        
    def prep(self, starttime, stoptime, strModel, cap, reg, keep=0.1):
        """Method to 'prepare' a population object. This method has two uses:
        initializing the population, and updating it for the next run.
        
        TODO: More inputs (like costs and probabilities) should be added here
        when desired.
        
        INPUTS:
            starttime, stoptime, strModel, cap, and reg are described in 
            __init__.
            
            keep is for keeping individuals between time periods.
                Essentially, we'll be seeding this population with 'keep' of 
                the best individuals.
        """
        # Set times.
        self.starttime = starttime
        self.stoptime = stoptime
        
        # Set population base model
        self.strModel = strModel
        
        # Set regulators and capacitors as property. Since the population
        # object will modify reg and cap, make deep copies.
        self.reg = copy.deepcopy(reg)
        self.cap = copy.deepcopy(cap)
        
        # If the population includes a 'baseline' model, we need to track it.
        # TODO: May want to update this to track multiple baseline individuals
        self.baselineIndex = None
        self.baselineData = None
        
        # Track the best scores for each generation.
        self.generationBest = []
        
        # Track the sum of fitness - used to compute roulette wheel weights
        self.fitSum = None
        
        # Track weights of fitness for "roulette wheel" method
        self.rouletteWeights = None
        
        # Intiailize temporary list of individuals, to be used to manage
        # multithreaded deletions/cleanup.
        self.tempList = []
        
        # If there are individuals in the list, keep some.
        if len(self.individualsList) > 0:
            # Determine how many to keep
            numKeep = round(len(self.individualsList) * keep)
            # Cleanup individuals we don't want to keep.
            for ind in self.individualsList[numKeep:]:
                d = ind.buildCleanupDict()
                self.cleanupQueue.put_nowait(d)
            
            # Truncate the list.
            self.individualsList = self.individualsList[0:numKeep]
            
            # Cleanup the remaining individuals. We'll truncate their tables
            # rather than deleting to save a tiny bit of time.
            for ind in self.individualsList:
                d = ind.buildCleanupDict(truncateFlag=True)
                self.cleanupQueue.put_nowait(d)
                
            # The following double looping is intentional. We want to get the
            # cleanup threads running ASAP so that we can do other prep work
            # in parallel.
            for ind in self.individualsList:
                # Prep the individual (reset old attributes)
                ind.prep(starttime=self.starttime, stoptime=self.stoptime,
                         reg=self.reg, cap=self.cap)
        
        # Initialize the population.
        self.initializePop()
        
        # Wait for the cleanupQueue to be cleared before continuing.
        self.cleanupQueue.join()
            
            
    def initializePop(self):
        """Method to initialize the population.
        
        TODO: Make more flexible.
        """
        # Create baseline individual.
        if self.baseControlFlag is not None:
            # Set regFlag and capFlag
            if self.baseControlFlag:
                # Non-zero case --> volt or volt_var control
                regFlag = capFlag = 3
            else:
                # Manual control, use 'newState'
                regFlag = capFlag = 4
             
            # Add a baseline individual with the given control flag   
            self.addIndividual(individual=\
                individual.individual(uid=self.nextUID,
                                      reg=self.reg,
                                      cap=self.cap,
                                      regFlag=regFlag,
                                      capFlag=capFlag,
                                      starttime=self.starttime,
                                      stoptime=self.stoptime,
                                      voltdumpFiles=self.voltdumpFiles,
                                      controlFlag=self.baseControlFlag))
            
            # Track the baseline individual's index.
            self.baselineIndex = len(self.individualsList) - 1
        
        # Create 'extreme' indivuals - all caps in/out, regs maxed up/down
        for n in range(len(individual.CAPSTATUS)):
            for regFlag in range(2):
                self.addIndividual(individual=\
                    individual.individual(uid=self.nextUID,
                                          reg=self.reg,
                                          regFlag=regFlag,
                                          cap=self.cap,
                                          capFlag=n,
                                          starttime=self.starttime,
                                          stoptime=self.stoptime,
                                          voltdumpFiles=self.voltdumpFiles
                                          )
                                            )
                
        # Create individuals with biased regulator and capacitor positions
        # TODO: Stop hard-coding the number.
        for _ in range(4):
            self.addIndividual(individual=\
                individual.individual(uid=self.nextUID,
                                      reg=self.reg,
                                      regFlag=2,
                                      cap=self.cap,
                                      capFlag=2, 
                                      starttime=self.starttime,
                                      stoptime=self.stoptime,
                                      voltdumpFiles=self.voltdumpFiles
                                     )
                                        )
        
        # Randomly create the rest of the individuals.
        while len(self.individualsList) < self.numInd:
            # Initialize individual.
            self.addIndividual(individual=\
                individual.individual(uid=self.nextUID,
                                      reg=self.reg, 
                                      cap=self.cap,
                                      regFlag=5,
                                      capFlag=5,
                                      starttime=self.starttime,
                                      stoptime=self.stoptime,
                                      voltdumpFiles=self.voltdumpFiles
                                      )
                                        )
            
    def addIndividual(self, individual):
        """Simple function to add an individual to the population.
        """
        # Add individual to list, increment uid counter
        self.individualsList.append(individual)
        self.nextUID += 1
        
    def ga(self):
        """Main function to run the genetic algorithm.
        """
        g = 0
        # Put all individuals in the queue for processing.
        for individual in self.individualsList:
            # TODO: Making so many copies of 'strModel' feels REALLY
            # inefficient...
            self.modelQueue.put_nowait({'individual':individual,
                                 'strModel': self.strModel,
                                 'inPath': self.inPath,
                                 'outDir': self.outDir})
        # Loop over the generations
        while g < self.numGen:
            # Wait until all models have been run and evaluated.
            self.modelQueue.join()
            
            # If this is the first generation and we're tracking a baseline, 
            # save the requisite information.
            if (g == 0) and (self.baselineIndex is not None):
                # Get a reference to the individual
                bInd = self.individualsList[self.baselineIndex]
                # Clear the index (individual will get sorted
                self.baselineIndex = None
                # Save information.
                self.baselineData = {'costs': copy.deepcopy(bInd.costs),
                                     'cap': copy.deepcopy(bInd.cap),
                                     'reg': copy.deepcopy(bInd.reg)}
                # Get a well formatted string representation
                self.baselineData['str'] = \
                    util.helper.getSummaryStr(costs=self.baselineData['costs'],
                                              reg=self.baselineData['reg'],
                                              cap=self.baselineData['cap'])
            
            # Sort the individualsList by score.
            self.individualsList.sort(key=lambda x: x.costs['total'])
            
            # Track best score for this generation.
            self.generationBest.append(self.individualsList[0].costs['total'])
            
            # Increment generation counter.
            g += 1
            
            # TODO: performing this check again is annoyingly inefficient
            if g < self.numGen:
                # Select the fittest individuals and some unfit ones.
                self.naturalSelection()
                
                # Measure diversity
                # regDiff, capDiff = self.measureDiversity()
                
                # Replenish the population by crossing and mutating individuals
                # then run their models.
                self.crossMutateRun()
                
                # Ensure the individuals who didn't make it are cleaned up.
                # TODO: Where is the best place to do this? Right here seems
                # okay, but there's really no reason to wait to start the new
                # models. The only argument would be to make sure memory/disk
                # space is freed up, but that likely won't be a problem.
                self.cleanupQueue.join()
                # Delete individuals in the cleanup list.
                self.tempList.clear()
        
        # Return the best individual.
        return self.individualsList[0]
    
    def addToModelQueue(self, individual):
        """Helper function to put an individual and relevant inputs into a
            dictionary to run a model.
        """
        self.modelQueue.put_nowait({'individual': individual,
                                    'strModel': self.strModel,
                                    'inPath': self.inPath,
                                    'outDir': self.outDir})
                
    def naturalSelection(self):
        """Determines which individuals will be used to create next generation.
        """
        # Determine how many individuals to keep for certain.
        k = math.ceil(self.probabilities['top'] * len(self.individualsList))
        
        # Loop over the unfit individuals, and either delete or keep based on
        # the weakProb
        while len(self.individualsList) > k:
            # Randomly decide whether or not to keep an unfit individual
            if random.random() > self.probabilities['weak']:
                # Remove indiviual from individualsList and insert into 
                # tempList
                ind = self.individualsList.pop(k)
                self.tempList.append(ind)
                # Get dictionary for individual cleanup.
                d = ind.buildCleanupDict()
                # Put dictionary in the cleanupQueue.
                self.cleanupQueue.put_nowait(d)
            else:
                # Increment k
                k += 1
        
        # Compute fitness sum for surviving individuals and assign a weight
        self.fitSum = 0
        # TODO: If locks are used, the threaded fitness computation could add
        # to the fitSum, and we could cut down on some looping.
        for individual in self.individualsList:
            # Add the score.
            self.fitSum += individual.costs['total']
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
            self.rouletteWeights.append(1 / (individual.costs['total'] 
                                             / self.fitSum))
    
    def crossMutateRun(self):
        """Crosses traits from surviving individuals to regenerate population,
            then runs the new individuals.
        
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
            if random.random() < self.probabilities['cross']:
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
                regChroms = crossChrom(chrom1=_individualsList[0].regChrom,
                                       chrom2=_individualsList[1].regChrom)
                
                # Cross the capaictor chromosomes
                capChroms = crossChrom(chrom1=_individualsList[0].capChrom,
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
            if forceMutate or (random.random() < self.probabilities['mutate']):
                # Mutate regulator chromosome:
                regChroms = mutateChroms(c=regChroms,
                                         prob=self.probabilities['regMutate'])
                # Mutate capacitor chromosome:
                capChroms = mutateChroms(c=capChroms,
                                         prob=self.probabilities['capMutate'])
                # TODO: Mutate DER chromosome
                """
                print('Reg: {} genes mutated. Cap: {} genes mutated.'.format(
                        rCount, cCount), flush=True)
                """
            
            # Create individuals based on new chromosomes, add to list, put
            # in queue for processing.
            for i in range(len(regChroms)):
                # Initialize new individual
                ind = individual.individual(uid=self.nextUID, 
                                            regChrom=regChroms[i],
                                            capChrom=capChroms[i],
                                            reg=_individualsList[0].reg,
                                            cap=_individualsList[0].cap,
                                            parents=parents,
                                            starttime=self.starttime,
                                            stoptime=self.stoptime,
                                            voltdumpFiles=self.voltdumpFiles
                                          )
                # Put individual in the list and the queue.
                self.addIndividual(individual=ind)
                self.addToModelQueue(individual=ind)
        
        """
        # Sort the chooseCount by number of occurences
        chooseCount.sort(key=lambda x: x[2])
        print('Fitness, UID, Occurences', flush=True)
        for el in chooseCount:
            print('{:.2f},{},{}'.format(el[0], el[1], el[2]))
        """
    
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
    
    def stopThreads(self, timeout=10):
        """Function to gracefully stop the running threads.
        """
        # Signal to threads that we're done by putting 'None' in the queue.
        for _ in self.modelThreads: self.modelQueue.put_nowait(None)
        for _ in self.cleanupThreads: self.cleanupQueue.put_nowait(None)
        for t in self.modelThreads: t.join(timeout=timeout)
        for t in self.cleanupThreads: t.join(timeout=timeout)
        #print('Threads terminated.', flush=True)
    
def writeRunEval(modelQueue, costs,
                 database={'database': 'gridlabd'}):
                #, cnxnpool):
    #tEvent):
    """Write individual's model, run the model, and evaluate costs. This is
    effectively a wrapper for individual.writeRunUpdateEval()
    
    NOTE: will take no action if an individual's model has already been
        run.
    
    NOTE: This function is static due to the threading involved. This feels
        memory inefficient but should save some headache.
        
    NOTE: This function is specifically formatted to be run via a thread
        object which is terminated when a 'None' object is put in the 
        modelQueue.
        
    INPUTS:
        modelQueue: queue which will have dictionaries inserted into it.
            dictionaries should contain individual, strModel, inPath, 
            and outDir fields from a population object.
    """
    while True:
        try:
            # Extract an individual from the queue.
            inDict = modelQueue.get()
            
            # Check input.
            if inDict is None:
                # If None is returned, we're all done here.
                modelQueue.task_done()
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
            # TODO: database inputs should be provided in inDict.
            cnxn = util.db.connect(**database)
            cursor = cnxn.cursor()
            
            # Modify the input's outDir to ensure models go in their own
            # folder. NOTE: This won't be necessary when voltage recording can
            # take place in the MySQL database.
            outDir = (inDict['outDir'] + '/ind_'
                      + str(inDict['individual'].uid))
            
            # Write, run, update, and evaluate the individaul
            inDict['individual'].writeRunUpdateEval(strModel=inDict['strModel'],
                                                    inPath=inDict['inPath'],
                                                    outDir=outDir,
                                                    cursor=cursor,
                                                    costs=costs)
            
            # Denote task as complete.
            modelQueue.task_done()
            
        except:
            print('Exception occurred!', flush=True)
            error_type, error, traceback = sys.exc_info
            print(error_type, flush=True)
            print(error, flush=True)
            print(traceback, flush=True)
        finally:
            cursor.close()
            cnxn.close()

def mutateChroms(c, prob):
    """Take a chromosome and randomly mutate it.
    
    INPUTS:
        c: list of chromsomes, which are tuples of 1's and 0's. Ex: 
            (1, 0, 0, 1, 0)
        prob: decimal in set [0.0, 1.0] to determine chance of
            mutating (bit-flipping) an individual gene
    """
    out = []
    for chrom in c:
        newC = list(chrom)
        #count = 0
        for ind in range(len(c)):
            if random.random() < prob:
                # Flip the bit!
                newC[ind] = 1 - newC[ind]
                #count += 1
                
        # Convert to tuple, put in output list
        out.append(tuple(newC))
        
    return out

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