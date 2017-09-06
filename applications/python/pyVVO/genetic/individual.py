"""
required packages: pyodbc

Created on Aug 9, 2017

@author: thay838
"""
import random
from powerflow import writeCommands
from util import db, gld
# import os
import math

# Define cap status, to be accessed by binary indices.
CAPSTATUS = ['OPEN', 'CLOSED']
# Define the percentage of the tap range which sigma should be for drawing
# tap settings. Recall the 68-95-99.7 rule for normal distributions -
# 1 std dev, 2 std dev, 3 std dev probabilities
TAPSIGMAPCT = 0.1
    
class individual:
    
    def __init__(self, uid, reg=None, cap=None, regChrom=None, regDict=None, 
                 capChrom=None, capDict=None):
        """An individual contains information about Volt/VAR control devices
        
        Individuals can be initialized in two ways: 
        1) From scratch: Provide self, uid, reg, and cap inputs. Positions
            will be randomly generated
            
        2) From a chromosome: Provide self, uid, regChrom, capChrom, regDict,
            and capDict. After crossing two individuals, a new chromosome will
            result. Use this chromosome to update regDict and capDict
        
        INPUTS:
            reg: Dictionary describing on-load tap changers.
                Keys should be regulator names, values dictionaries which have
                the following keys:
                    raise_taps
                    lower_taps
                    phases
                Note possible tap positions be in interval [-(lower_taps - 1),
                raise_taps]. 
                    
            cap: Dictionary describing capacitors.
                Keys should be capacitor names, values a list of phases.
                
            uid: Unique ID of individual. Should be an int.
            
            regChrom: Regulator chromosome resulting from crossing two
                individuals. List of ones and zeros representing tap positions. 
                
            regDict: Dictionary mapping to input regChrom. Regulator tap
                positions will be updated based on the regChrom
                
            capChrom: Capacitor chromosome resulting from crossing two
                individuals. List of ones and zeros representing switch status.
            
            capDict: Dictionary mapping to input capChrom. Capacitor switch
                statuses will be updated based on capChrom.
            
            
        TODO: add controllable DERs
                
        """
        
        # Assign the unique identifier.
        self.uid = uid
        # Full path to output model.
        self.model = None
        # Name of table to record swing data.
        self.swingTable = None
        # Name of columns of swingTable
        self.swingColumns = None
        # When the model is run, output will be saved.
        self.modelOutput = None

        # The evalFitness method assigns to fitness
        self.fitness = None
        
        # If not given a regChrom or capChrom, generate them.
        if regChrom is None and capChrom is None:
            # Generate regulator chromosome:
            self.genRegChrom(reg=reg)
            # Generate capacitor chromosome:
            self.genCapChrom(cap=cap)
            # TODO: DERs
        else:
            # Use the given chromosomes to update the dictionaries.
            self.genRegDict(regChrom, regDict)
            self.genCapDict(capChrom, capDict)
        
    def genRegChrom(self, reg):
        """Method to randomly generate an individual's regulator chromosome
        
        INPUTS:
            reg: dict as described in __init__
            
        NOTES:
            Some heuristics are involved here. A single tap position will be
            chosen at random, and then all tap positions will be biased to be
            close to that position.
        """
        
        # Initialize chromosome for regulator and dict to store list indices.
        self.regChrom = []
        self.reg = dict()
        # Intialize index counters.
        s = 0;
        e = 0;
        
        # Loop through the regs and create binary representation of taps.
        for r, v in reg.items():
            
            # Initialize dict for tracking regulator indices in the chromosome
            self.reg[r] = dict()
            self.reg[r]['raise_taps'] = v['raise_taps']
            self.reg[r]['lower_taps'] = v['lower_taps']
            
            # Define the upper tap bound (tb).
            tb = v['raise_taps'] + v['lower_taps'] - 1
            
            # Compute the needed field width to represent the upper tap bound
            width = math.ceil(math.log(tb, 2))
            
            # Initialize dict for taps
            self.reg[r]['taps'] = dict()
            
            # Randomly choose a viable tap setting to mu for the standard dist.
            tapMu = random.randint(0, tb)
            
            # Compute the tap sigma for drawing from the normal distribution.
            tapSigma = round(TAPSIGMAPCT * (tb + 1))
            
            # Loop through the phases
            for tap in v['taps']:
                # Initialize dict for this tap.
                self.reg[r]['taps'][tap] = dict()
                
                # Initialize the tapPos for while loop.
                tapPos = -1
                
                # The standard distribution runs from (-inf, +inf) - draw 
                # until position is valid. Recall valid positions are [0, tb]
                while (tapPos < 0) or (tapPos > tb):
                    # Draw the tap position from the normal distribution.
                    tapPos = round(random.gauss(tapMu, tapSigma))
                
                # Express tap setting as binary list.
                # For now, hard-code 6 positions
                binList = [int(x) for x in "{0:0{width}b}".format(tapPos,
                                                                  width=width)]
                
                # Extend the regulator chromosome.
                self.regChrom.extend(binList)
                
                # Increment end index.
                e += len(binList)
                
                # Translate tapPos for GridLAB-D.
                self.reg[r]['taps'][tap]['pos'] = \
                    gld.translateTaps(lowerTaps=v['lower_taps'],
                                       raiseTaps=v['raise_taps'],
                                       pos=tapPos)
                
                # Assign indices for this regulator
                self.reg[r]['taps'][tap]['ind'] = (s, e)
                
                # Increment start index.
                s += len(binList)
                
    def genCapChrom(self, cap):
        """Method to randomly generate an individual's capacitor chromosome
        
        INPUTS:
            cap: dict as described in __init__
        """
        # Initialize chromosome for capacitors and dict to store list indices.
        self.capChrom = []
        self.cap = dict()
        # Keep track of chromosome index
        ind = 0
        # Loop through the capacitors, randomly assign state for each phase
        # 0 --> open, 1 --> closed
        for c, p in cap.items():
            # Initialize dict for this capacitor
            self.cap[c] = {}
            
            # Loop through the phases and randomly decide state
            for phase in p:
                # Initialize subdict
                self.cap[c][phase] = {}
                
                # Randomly determine capacitor status.
                capBinary = round(random.random())
                capStatus = CAPSTATUS[capBinary]
                
                # Assign to the capacitor
                self.capChrom.append(capBinary)
                self.cap[c][phase]['status'] = capStatus
                self.cap[c][phase]['ind'] = ind
                
                # Increment the chromosome counter
                ind += 1
                
    def genRegDict(self, regChrom, regDict):
        """Create and assign new regDict from regChrom
        """
        # Loop through the regDict and correct tap positions
        for reg, t in regDict.items():
            for tap, tapData in t['taps'].items():
                # Extract the binary representation of tap position.
                tapBin = regChrom[tapData['ind'][0]:tapData['ind'][1]]
                # Convert the binary to an integer
                posInt = self.bin2int(tapBin)
                # Convert integer to tap position
                regDict[reg]['taps'][tap]['pos'] = \
                    gld.translateTaps(lowerTaps=regDict[reg]['lower_taps'],
                                       raiseTaps=regDict[reg]['raise_taps'],
                                       pos=posInt)
                    
        # Assign the regChrom and regDict
        self.regChrom = regChrom
        self.reg = regDict 
        
    
    def genCapDict(self, capChrom, capDict):
        """Create and assign new capDict from capChrom
        """
        # Loop through the capDict and correct status
        for cap, d in capDict.items():
            for phase, phaseData in d.items():
                # Read chromosome and reassign the capDict
                capDict[cap][phase]['status'] = \
                    CAPSTATUS[capChrom[phaseData['ind']]]
                    
        # Assign the capChrom and capDict to the individual
        self.capChrom = capChrom
        self.cap = capDict
        
    
    @staticmethod
    def bin2int(binList):
        """Take list representing binary number (ex: [0, 1, 0, 0, 1]) and 
        convert to an integer
        """
        # TODO: unit test
        # initialize number and counter
        n = 0
        k = 0
        for b in reversed(binList):
            n += b * (2 ** k)
            k += 1
            
        return n
                
    def writeModel(self, strModel, inPath, outDir):
        """Create a GridLAB-D .glm file for the given individual.
        
        INPUTS:
            self: constructed individual
            strModel: string of .glm file found at inPath
            inPath: path to model to modify volt control settings
            outDir: directory to write new model to. Filename will be inferred
                from the inPath, and the individuals uid preceded by an
                underscore will be added
                
        OUTPUTS:
            Path to new model after it's created
        """
        
        # Get the filename of the original model and create output path
        outPath = writeCommands.writeCommands.addFileSuffix(inPath=inPath,
                                                            suffix=str(self.uid),
                                                            outDir = outDir)
        
        # Track the output path for running the model later.
        self.model = outPath
        
        # Instantiate a writeCommands object.
        writeObj = writeCommands.writeCommands(strModel=strModel,
                                               pathModelIn=inPath,
                                               pathModelOut=outPath)
        
        # Update the swing node to a meter and get it writing power to a table
        # TODO: swing table won't be the only table.
        o = writeObj.recordSwing(suffix=self.uid)
        self.swingTable = o['table']
        self.swingColumns = o['swingColumns']
        
        # TODO: incorporate the two loops below into the functions that build
        # self.reg and self.cap. This double-looping is inefficient.
        # Get regulators in dict usable by writeCommands
        regDict = dict()
        for reg, tapDict in self.reg.items():
            # For now, force regulators to be in MANUAL control mode.
            regDict[reg] = {'regulator': {}, 
                            'configuration': {'Control': 'MANUAL'}}
            # Assign tap positions.
            for tap in tapDict['taps']:
                regDict[reg]['regulator'][tap] = tapDict['taps'][tap]['pos']
                
        # Get capacitors in dict usable by writeCommands
        capDict = dict()
        for cap, capData in self.cap.items():
            # Initialize dict for this cap
            capDict[cap] = {}
            for phase in capData:
                capDict[cap][phase] = capData[phase]['status']

            # For now, force capacitors to be in 'MANUAL' control mode.
            capDict[cap]['control'] = 'MANUAL'
            
        # TODO: DERs (PV inverters) will need handled here.
        
        # Change capacitor and regulator statuses/positions
        writeObj.commandRegulators(regulators=regDict)
        writeObj.commandCapacitors(capacitors=capDict)
        
        # Write the modified model to file.
        writeObj.writeModel()
        
        # Return the path to the new file.
        return outPath
    
    def runModel(self):
        """Function to run GridLAB-D model.
        """
        self.modelOutput = gld.runModel(self.model)
                            
    def evalFitness(self, cursor, starttime=None, stoptime=None):
        """Function to evaluate fitness of individual.
        
        For now, this will just be total energy consumed.
        
        TODO: Add more evaluators of fitness like voltage violations, etc.
        
        INPUTS:
            cursor: pyodbc cursor from pyodbc connection 
            starttime: starting time to evaluate
            stoptime: ending time to evaluate
        """
        # Connect to the database.
        # TODO: Add connection options to this function.
        # cnxn = db.connect()
        
        # Sum the power of all three phases over all time.
        f, _ = db.sumComplexPower(cursor=cursor, cols=self.swingColumns,
                                  table=self.swingTable, starttime=starttime,
                                  stoptime=stoptime) 
        
        # For now, consider the fitness to be the absolute value of the
        # total complex power.
        self.fitness = f.__abs__()
        
        # Drop table.
        # db.dropTable(cursor=cursor, table=self.swingTable)
                            
if __name__ == "__main__":
    obj = individual(reg={'R2-12-47-2_reg_1': {
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
                     cap={'R2-12-47-2_cap_1': ['switchA', 'switchB', 'switchC'],
                          'R2-12-47-2_cap_2': ['switchA', 'switchB', 'switchC'],
                          'R2-12-47-2_cap_3': ['switchA', 'switchB', 'switchC'],
                          'R2-12-47-2_cap_4': ['switchA', 'switchB', 'switchC']
                          },
                     uid=1)
    inPath = 'C:/Users/thay838/Desktop/R2-12.47-2.glm'
    strModel = writeCommands.readModel(inPath)
    obj.writeModel(strModel=strModel, inPath=inPath,
                   outDir='C:/Users/thay838/Desktop/vvo')
    r = obj.runModel()
    obj.evalFitness()
    print('hooray')
            
        
        