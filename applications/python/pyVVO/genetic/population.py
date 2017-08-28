'''
Created on Aug 15, 2017

@author: thay838
'''
from genetic import individual
from powerflow import writeCommands
import math
import random
import os

class population:
    
    # Scores will be stored in a list of tuples in format (uid, score)
    UIDIND = 0
    SCOREIND = 1

    def __init__(self, numInd, numGen, modelIn, outDir, reg, cap, 
                 start="'2017-01-01 00:00:00'", stop="'2017-01-01 00:15:00'"):
        """Initialize a population of individuals.
        
        INPUTS:
            numInd: Number of individuals to create
            numGen: Number of generations to run
            modelIn: Path and filename to the base GridLAB-D model to be 
                modified.
        """
        # TODO: rather than being input, reg and cap should be read from the
        # CIM.
        
        # Seed random module
        random.seed()
        
        # Read the base model as a string.
        with open(modelIn, 'r') as f:
            self.strModel = f.read()
        
        # Get a writeCommands object
        writeObj = writeCommands.writeCommands(strModel = self.strModel)
        
        # Update the clock.
        writeObj.updateClock(start=start, stop=stop)
        # Remove the tape module if it exists.
        # TODO: likely don't need this when model is pulled from the CIM.
        writeObj.removeTape()
        # Add mysql model and database connection.
        # TODO: Get database inputs rather than just using the default.
        writeObj.addMySQL()
        # Stop suppressing messages
        writeObj.repeatMessages()
        # Stop using the profiler
        writeObj.toggleProfile()
        
        # Update the population's base model with the modified one.
        self.strModel = writeObj.strModel
            
        # Set the number of generations and individuals.
        self.numGen = numGen
        self.numInd = numInd
        
        # Set regulators and capacitors as property
        self.reg = reg
        self.cap = cap
        
        # Set modelIn and outDir
        self.modelIn = modelIn
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
            # Write and run each individual's model
            for individual in self.indList:
                # Only writeRunEval if this individual hasn't done so yet.
                if individual.fitness is None:
                    self.writeRunEval(individual=individual)
            
            # Sort the fitness levels by score in format of (uid, score).
            self.indFitness.sort(key=lambda x: x[self.SCOREIND])
            
            # Track best score for this generation.
            self.generationBest.append(self.indFitness[0][self.SCOREIND])
            
            # Increment generation counter.
            g += 1
            
            # TODO: performing this check again is annoyingly inefficient
            if g < self.numGen:
                # Select the fittest individuals and some unfit ones.
                self.naturalSelection()
                
                # Replenish the population by crossing individuals.
                self.crossAndMutate()
                
        
    def writeRunEval(self, individual):
        """Write individual's model, run the model, and evaluate fitness
        """
        # Write the model
        individual.writeModel(strModel=self.strModel, inPath=self.modelIn,
                              outDir=self.outDir)
        
        # Run the model. gridlabd.exe must be on the path
        individual.runModel()
        
        # Evaluate the individuals fitness
        individual.evalFitness()
        
        # Add this individual's score to the list. Note the tuple format of
        # (uid, score)
        self.indFitness.append((individual.uid, individual.fitness))
    
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
                uid = self.indFitness[k][self.UIDIND]
                ind = self.uids.index(uid)
                # Delete this individual's model file.
                # TODO: This should be done in some cleanup stage rather
                # than during the optimization
                os.remove(self.indList[ind].model)
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
            self.fitSum += s[self.SCOREIND]
            
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
                                        + (1 / (s[self.SCOREIND]/self.fitSum)))
    
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
            
            # Write individual's model, run it, evaluate fitness
            self.writeRunEval(self.indList[-1])
            
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
    import time
    import matplotlib.pyplot as plt
    n = 3
    t = []
    for k in range(n):
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
        t.append(t1-t0)
        x = list(range(len(popObj.generationBest)))
        plt.plot(x, popObj.generationBest)
        plt.xlabel('Generations')
        plt.ylabel('Best Score')
        plt.title('Best Score for Each Generation')
        plt.grid(True)
        plt.savefig('C:/Users/thay838/Desktop/vvo/run_{}.png'.format(k))
        #plt.show()
        print('hooray')
        
    # print the runtimes
    for el in t:
        print('Runtime: {:.2f} s'.format(el))