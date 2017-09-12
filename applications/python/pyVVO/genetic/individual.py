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
import time

# Define cap status, to be accessed by binary indices.
CAPSTATUS = ['OPEN', 'CLOSED']
REGPEG = ['max', 'min']
# Define the percentage of the tap range which sigma should be for drawing
# tap settings. Recall the 68-95-99.7 rule for normal distributions -
# 1 std dev, 2 std dev, 3 std dev probabilities
TAPSIGMAPCT = 0.1
 
class individual:
    
    def __init__(self, uid, reg=None, regBias=False, peg=None, cap=None,
                 allCap=None, regChrom=None, regDict=None, capChrom=None,
                 capDict=None):
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
                    taps --> dict of tap name (e.g. tap_A) mapped to previous
                        position (e.g. 5)
                Note possible tap positions be in interval [-(lower_taps - 1),
                raise_taps]. 
                    
            cap: Dictionary describing capacitors.
                Keys should be capacitor names, values a dict of phases
                    mapped to the capacitor status on the previous iteration.
                
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
        # Table information
        self.swingTable = None
        self.swingColumns = None
        self.swingInterval = None
        self.triplexTable = None
        self.triplexColumns = None
        self.triplexInterval = None
        
        # Track tap changes and capacitor switching
        self.tapChangeCount = 0
        self.capSwitchCount = 0
        
        # When the model is run, output will be saved.
        self.modelOutput = None

        # The evalFitness method assigns to fitness
        self.fitness = None
        
        # If not given a regChrom or capChrom, generate them.
        if regChrom is None and capChrom is None:
            # Generate regulator chromosome:
            self.genRegChrom(reg=reg, regBias=regBias, peg=peg)
            # Generate capacitor chromosome:
            self.genCapChrom(cap=cap, allCap=allCap)
            # TODO: DERs
        else:
            # Use the given chromosomes to update the dictionaries.
            self.genRegDict(regChrom, regDict, prevReg=reg)
            self.genCapDict(capChrom, capDict, prevCap=cap)
        
    def genRegChrom(self, reg, regBias, peg):
        """Method to randomly generate an individual's regulator chromosome
        
        INPUTS:
            reg: dict as described in __init__
            regBias: Flag, True or False. True indicates that tap positions
                for this individual will be biased (via a Gaussian 
                distribution and the TAPSIGMAPCT constant) toward the previous
                tap positions, while False indicates taps will be chosen purely
                randomly.
            peg: None, 'max', or 'min.' If 'None' is provided, input has no 
                affect. 'max' will put all regulator taps at maximum position,
                and 'min' will put all regulator taps at minimum position.
            
        NOTES:
            If the peg input is not None, regBias must be False.
            
        TODO: track tap changes. 
        """
        # Make sure inputs aren't incompatible.
        if peg:
            assert (not regBias), ("If 'peg' is not None, "
                                    "'regBias' must be False.")
            assert peg in REGPEG
        
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
            
            # If we're pegging, set the position high or low.
            if peg:
                if peg == 'max':
                    tapPos = tb
                elif peg == 'min':
                    tapPos = 0
            elif regBias:
                # If we're biasing from the previous position, get a sigma for
                # the Gaussian distribution.
                tapSigma = round(TAPSIGMAPCT * (tb + 1))
                
            # Randomly choose a viable tap setting to mu for the standard dist.
            # tapMu = random.randint(0, tb)
            
            # Loop through the phases
            for tap, position in v['taps'].items():
                # Initialize dict for this tap.
                self.reg[r]['taps'][tap] = dict()
                
                # Translate given position to integer on interval [0, tb]
                position = gld.inverseTranslateTaps(lowerTaps=v['lower_taps'],
                                                        pos=position)
                
                if regBias:
                    # Randomly draw tap position from gaussian distribution.
                    
                    # Initialize the tapPos for while loop.
                    tapPos = -1
                    
                    # The standard distribution runs from (-inf, +inf) - draw 
                    # until position is valid. Recall valid positions are
                    # [0, tb]
                    while (tapPos < 0) or (tapPos > tb):
                        # Draw the tap position from the normal distribution.
                        # Here oure 'mu' is the previous value
                        tapPos = round(random.gauss(position, tapSigma))
                        
                elif not peg:
                    # If we made it here, we aren't implementing a previous 
                    # bias or 'pegging' the taps. Randomly draw.
                    tapPos = random.randint(0, tb)
                
                # Express tap setting as binary list with consistent width.
                binList = [int(x) for x in "{0:0{width}b}".format(tapPos,
                                                                  width=width)]
                
                # Extend the regulator chromosome.
                self.regChrom.extend(binList)
                
                # Increment end index.
                e += len(binList)
                
                # Translate tapPos for GridLAB-D.
                self.reg[r]['taps'][tap]['pos'] = \
                    gld.translateTaps(lowerTaps=v['lower_taps'], pos=tapPos)
                    
                # Increment the tap change counter (previous pos - this pos)
                self.tapChangeCount += abs(position - tapPos)
                
                # Assign indices for this regulator
                self.reg[r]['taps'][tap]['ind'] = (s, e)
                
                # Increment start index.
                s += len(binList)
                
    def genCapChrom(self, cap, allCap):
        """Method to randomly generate an individual's capacitor chromosome.
        Alternatively, if the allCap input is provided, all capacitors will
        be forced to the same status provided.
        
        INPUTS:
            cap: dict as described in __init__
            allCap: None to do nothing, or one of the strings given in
                CAPSTATUS ('OPEN' or 'CLOSED') which forces all caps to assume
                the given status
        """
        # If we're forcing all caps to the same status, determine the binary
        # representation. TODO: add input checking.
        if allCap:
            capBinary = CAPSTATUS.index(allCap)
            capStatus = allCap
        
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
                
                # Randomly determine capacitor status if allCap is None
                if allCap is None:
                    capBinary = round(random.random())
                    capStatus = CAPSTATUS[capBinary]
                
                # Assign to the capacitor
                self.capChrom.append(capBinary)
                self.cap[c][phase]['status'] = capStatus
                self.cap[c][phase]['ind'] = ind
                
                # Increment the switch counter if applicable
                if capStatus != p[phase]:
                    self.capSwitchCount += 1
                
                # Increment the chromosome counter
                ind += 1
                
    def genRegDict(self, regChrom, regDict, prevReg):
        """Create and assign new regDict from regChrom
        
        INPUTS:
            regChrom: Pre-constructed regulator chromosome. See 'genRegChrom'
            regDict: Dictionary corresponding to the regChrom, except the tap
                positions haven't been updated to reflect the regChrom.
            prevReg: Dictionary as described by 'reg' in __init__. This will be
                used to increment tap-changer count.
        TODO: track tap changes
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
                                      pos=posInt)
                # Increment the tap change counter (previous pos - this pos)
                self.tapChangeCount += abs(prevReg[reg]['taps'][tap] 
                                           - regDict[reg]['taps'][tap]['pos'])
                    
        # Assign the regChrom and regDict
        self.regChrom = regChrom
        self.reg = regDict 
        
    
    def genCapDict(self, capChrom, capDict, prevCap):
        """Create and assign new capDict from capChrom.
        
        INPUTS:
            capChrom: Pre-constructed capacitor chromosom. See 'genCapChrom'
            capDict: Dictionary corresponding to the capChrom, except the phase
                statuses haven't been updated to reflect capChrom.
            prevCap: Dictionary of previous capacitor statues as described
                in __init__ of 'cap' input
        """
        # Loop through the capDict and correct status
        for cap, d in capDict.items():
            for phase, phaseData in d.items():
                # Read chromosome and reassign the capDict
                capDict[cap][phase]['status'] = \
                    CAPSTATUS[capChrom[phaseData['ind']]]
                
                # Bump the capacitor switch count if applicable
                if capDict[cap][phase]['status'] != prevCap[cap][phase]:
                    self.capSwitchCount += 1
                
                    
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
        self.swingColumns = o['columns']
        self.swingInterval = o['interval']
        
        # TODO: Uncomment when ready
        '''
        # Record triplex nodes
        o = writeObj.recordTriplex(suffix=self.uid)
        self.triplexTable = o['table']
        self.triplexColumns = o['columns']
        self.triplexInterval = o['interval']
        ''' 
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
                            
    def evalFitness(self, cursor, energyPrice, tapChangeCost, capSwitchCost,
                    tCol='t', starttime=None, stoptime=None):
        """Function to evaluate fitness of individual. This is essentially a
            wrapper to call gld.computeCosts
        
        TODO: Add more evaluators of fitness like voltage violations, etc.
        
        INPUTS:
            cursor: pyodbc cursor from pyodbc connection 
            energyPrice: price of energy
            tapChangeCost: cost changing regulator taps
            capSwitchCost: cost of switching a capacitor
            tCol: name of time column(s)
            starttime: starting time to evaluate
            stoptime: ending time to evaluate
        """
        r = gld.computeCosts(cursor=cursor,
                                        powerTable=self.swingTable,
                                        powerColumns=self.swingColumns,
                                        powerInterval=self.swingInterval,
                                        energyPrice=energyPrice,
                                        starttime=starttime,
                                        stoptime=stoptime,
                                        tCol=tCol,
                                        tapChangeCost=tapChangeCost,
                                        capSwitchCost=capSwitchCost,
                                        tapChangeCount=self.tapChangeCount,
                                        capSwitchCount=self.capSwitchCount)
        
        self.fitness = r['total']
        self.energyCost = r['energy']
        self.tapCost = r['tap']
        self.capCost = r['cap']
        
'''
DEPRECATED             
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
'''
        
        