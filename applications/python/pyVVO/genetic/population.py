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

# Scores will be stored in a list of tuples in format (uid, score)
UIDIND = 0
SCOREIND = 1
INDIND = 1 # qOut will hold (uid, individual)
    
class population:

    def __init__(self, strModel, numInd, numGen, inPath, outDir, reg, cap,
                 starttime, stoptime, numThreads=os.cpu_count()):
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
                                 args=(self.qIn, self.qOut, self.starttime,
                                       self.stoptime))
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
        
        # Track the sum of fitness.
        self.fitSum = None
        
        # Track weights of fitness for "roulette wheel" method
        self.rouletteWeights = None
        
        # Initialize individuals and write their models.
        # TODO: make this loop parallel?
        self.indList = []
        self.indFitness = []
        self.uids = list(range(numInd))

        for n in range(numInd):
            # Initialize individual.
            self.indList.append(individual.individual(uid=n, 
                                                      reg=self.reg, 
                                                      cap=self.cap))
            
        # Track the current UID
        self.lastUID = n + 1
        
    def ga(self):
        """Main function to run the genetic algorithm.
        """
        g = 0
        while g < self.numGen:
            #print('Starting generation {}'.format(g), flush=True)
            
            # Put all the individuals in the queue for processing.
            for individual in self.indList:
                # TODO: Making so many copies of 'strModel' feels REALLY
                # inefficient...
                self.qIn.put_nowait({'individual':individual,
                                     'strModel': self.strModel,
                                     'inPath': self.inPath,
                                     'outDir': self.outDir})
            
            #print('Input queue filled with individuals. Waiting for runs...',
            #      flush=True)
                
            # Wait until all models have been run and evaluated.
            self.qIn.join()
            
            #print('All runs should now be complete. Starting to process output queue.',
            #      flush=True)
            
            # Loop over the resulting individuals to update the indList and
            # indFitness.
            while not self.qOut.empty():
                # Extract the (uid, individual) pair from the queue.
                pair = self.qOut.get()
                # Find the index of this individual by their UID.
                ind = self.uids.index(pair[UIDIND])
                
                # Only add fitness and update list if writeRunEval did
                # something.
                if pair[INDIND] is not None:
                    # Add fitness in form of (uid, score)
                    self.indFitness.append((pair[INDIND].uid,
                                            pair[INDIND].fitness))
                    # Update the individual in the indList
                    self.indList[ind] = pair[INDIND]
                
                # Mark this queue task as complete 
                self.qOut.task_done()
            
            # Make sure we completed all tasks in the queue.
            # TODO - probably don't need this?
            self.qOut.join()
                
            #print('Output queue processed. Moving on to natural selection and mutation.')
                
            # Sort the fitness levels by score in format of (uid, score).
            self.indFitness.sort(key=lambda x: x[SCOREIND])
            
            # Track best score for this generation.
            self.generationBest.append(self.indFitness[0][SCOREIND])
            
            # Increment generation counter.
            g += 1
            
            # TODO: performing this check again is annoyingly inefficient
            if g < self.numGen:
                # Select the fittest individuals and some unfit ones.
                self.naturalSelection()
                
                # Replenish the population by crossing individuals.
                self.crossAndMutate()
                
                #print('Natural selection and mutation complete.')
                
            #print('Generation {} complete.'.format(g-1))
            
        # Signal to threads that we're done.
        for _ in self.threads: self.qIn.put_nowait(None)
        for t in self.threads: t.join()
        #print('Threads terminated.', flush=True)
                
    @staticmethod 
    def writeRunEval(qIn, qOut, starttime, stoptime):
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
                inDict = qIn.get(timeout=30)
                
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
                if inDict['individual'].fitness is None:
                    # If the individual has not been run and evaluated:
                    # Write the model.
                    inDict['individual'].writeModel(strModel=inDict['strModel'],
                                                   inPath=inDict['inPath'],
                                                   outDir=inDict['outDir'])
                    #print("Individual {}'s model written.".format(inDict['individual'].uid))
                    
                    # Run the model. gridlabd.exe must be on the path.
                    inDict['individual'].runModel()
                    if inDict['individual'].modelOutput.returncode:
                        print("FAILURE! Individual {}'s model gave non-zero returncode.".format(inDict['individual'].uid))
                    else:
                        #print("Individual {}'s model successfully run.".format(inDict['individual'].uid))
                        pass
                    
                    # Evaluate the individuals fitness.
                    inDict['individual'].evalFitness(cursor, starttime,
                                                     stoptime)
                    #print("Individual {}'s fitness successfully evaluated.".format(inDict['individual'].uid))
                    
                    # Put the modified individual in the output queue.
                    qOut.put((inDict['individual'].uid, inDict['individual']))
                    #print('Individual {} put in output queue.'.format(inDict['individual'].uid),
                    #      flush=True)
                else:
                    # If the individual has already been evaluated, there's no 
                    # work to do.
                    qOut.put((inDict['individual'].uid, None))
                    #print('Individual {} has already been run. Putting None in output queue'.format(inDict['individual'].uid),
                    #      flush=True)
                
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
        # Determine the index of the first individual that didn't make the cut
        k = math.ceil(top * len(self.indFitness))
        
        # Loop over the unfit individuals, and either delete or keep based on
        # the keepProb
        while k < len(self.indFitness):
            # Randomly decide whether or not to keep an unfit individual
            if random.random() > keepProb:
                # Extract the uid and get the individual's index
                uid = self.indFitness[k][UIDIND]
                ind = self.uids.index(uid)
                # Delete this individual's model file.
                # TODO: This should be done in some cleanup stage rather
                # than during the optimization
                #os.remove(self.indList[ind].model)
                # Delete the individuals score from the list
                del self.indFitness[k]
                # Delete the individual and its reference
                del self.indList[ind]
                del self.uids[ind]
                # Decrement k since we shortened the lists
                k -= 1
            
            # Increment k
            k += 1
        
        # Compute fitness sum for surviving individuals
        self.fitSum = 0
        for s in self.indFitness:
            # Add the score, recalling indFitness is in format (uid, score)
            self.fitSum += s[SCOREIND]
            
        # Determine "roulette wheel" weights. Loop over remaining individuals.
        # TODO: this feels so inefficient...
        self.rouletteWeights = []
        prev = 0
        for s in self.indFitness:
            # Lower scores are better but result in lower pct, hence the '1/'.
            # This is to be used with random.choices. Documentation states it's
            # faster to use cumulative weights.
            # NOTE: the weights don't have to sum to 1 or 100.
            if len(self.rouletteWeights) > 0:
                prev = self.rouletteWeights[-1]

            self.rouletteWeights.append(prev 
                                        + (1 / (s[SCOREIND]/self.fitSum)))
    
    def crossAndMutate(self, popMutate=0.2, crossChance=0.7, mutateChance=0.1):
        """Crosses traits from surviving individuals to regenerate population.
        
        INPUTS:
            popMutate: chance of an individual to enter the mutation phase
            crossChance: chance of an individual being created by crossover
            mutateChance: chance an individual gene mutates
        """
        # Get the UIDs of the individuals eligible to breed
        eligibleUIDs = tuple(self.uids)
        
        while len(self.indList) < self.numInd:
            if random.random() < crossChance:
                # Since we're crossing over, we won't force a mutation.
                forceMutate = False

                # Initialize while loop. Note that random.choices() has 
                # replacement, hence need for while loop.
                uids = [-1, -1]
                while uids[0] == uids[1]:
                    # Pick two individuals based on cumulative weights.
                    uids = random.choices(eligibleUIDs,
                                          cum_weights=self.rouletteWeights,
                                          k=2)
                
                # Extract the indices of the given indivuals.    
                ind1 = self.uids.index(uids[0])
                ind2 = self.uids.index(uids[1])
            
                # Cross the regulator chromosomes
                regChrom = self.crossChrom(chrom1=self.indList[ind1].regChrom,
                                           chrom2=self.indList[ind2].regChrom)
                
                # Cross the capaictor chromosomes
                capChrom = self.crossChrom(chrom1=self.indList[ind1].capChrom,
                                           chrom2=self.indList[ind2].capChrom)
                
                # TODO: Cross DER chromosomes
            else:
                # We're not crossing over, so force mutation.
                forceMutate = True
                # Draw an individual.
                uid = random.choices(eligibleUIDs,
                                     cum_weights=self.rouletteWeights,
                                     k=1)
                # Extract the individual's index.
                ind1 = self.uids.index(uid[0])
                # Grab the necessary chromosomes
                regChrom = self.indList[ind1].regChrom
                capChrom = self.indList[ind1].capChrom
                # TODO. DER chromosome.
            
            # Possibly mutate this individual.
            if forceMutate or (random.random() < popMutate):
                # Mutate regulator chromosome:
                regChrom = self.mutateChrom(c=regChrom,
                                            mutateChance=mutateChance)
                # Mutate capacitor chromosome:
                capChrom = self.mutateChrom(c=capChrom,
                                            mutateChance=mutateChance)
                # TODO: Mutate DER chromosome
            
            # Create individual based on new chromosomes, add to indList
            self.indList.append(individual.individual(uid=self.lastUID, 
                                                      regChrom=regChrom,
                                                      capChrom=capChrom,
                                                      regDict=
                                                      self.indList[ind1].reg,
                                                      capDict=
                                                      self.indList[ind1].cap))
            
            # Add this UID to the list
            self.uids.append(self.lastUID)
            
            # Increment UID
            self.lastUID += 1
            
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
        for ix in popObj.indList:
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