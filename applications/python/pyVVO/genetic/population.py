'''
Created on Aug 15, 2017

@author: thay838
'''
from genetic import individual
from powerflow import writeCommands

class population:

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
        
        # Update the population's base model with the modified one.
        self.strModel = writeObj.strModel
            
        # Set the number of generations.
        self.numGen = numGen
        
        # Initialize individuals and write their models.
        # TODO: make this loop parallel?
        self.indList = []
        self.bestInd = None
        self.bestScore = complex(float("inf"), float("inf"))
        for n in range(numInd):
            # Initialize individual.
            self.indList.append(individual.individual(uid=n, reg=reg, cap=cap,
                                                     ))
            
            # Write individual's modified model.
            self.indList[n].writeModel(strModel=self.strModel,
                                       inPath=modelIn,
                                       outDir=outDir)
            
            # Run the individual's model. gridlabd.exe must be on the path.
            self.indList[n].runModel()
            
            # Evalute the individuals fitness
            self.indList[n].evalFitness()
            
            # Update best individual
            if self.indList[n].fitness.__abs__() < self.bestScore.__abs__():
                self.bestInd = n
                self.bestScore = self.indList[n].fitness
            
    
    def naturalSelection(self, top=0.2):
        """Determines which individuals will be used to create next generation.
        
        """
        pass
    
    def crossover(self):
        """Crosses traits from surviving individuals to regenerate population.
        
        """
        pass
    
    def mutate(self):
        """Randomly mutate some individuals of the population
        
        """
        pass

if __name__ == "__main__":
    popObj = population(numInd=10, numGen=1,
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
    print('hooray')