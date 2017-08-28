"""
required packages: pyodbc

Created on Aug 9, 2017

@author: thay838
"""
import random
from powerflow import writeCommands
import os
import subprocess
import pyodbc
import math

class individual:
    # Define cap status, to be accessed by binary indices.
    CAPSTATUS = ['OPEN', 'CLOSED']
    # Define the percentage of the tap range which sigma should be for drawing
    # tap settings. Recall the 68-95-99.7 rule for normal distributions -
    # 1 std dev, 2 std dev, 3 std dev probabilities
    TAPSIGMAPCT = 0.1
    
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
        
        # Assign the unique identifier
        self.uid = uid
        # The writeModel method assigns to 'model' and 'table.'
        self.model = None
        self.table = None
        # When the model is run, output will be saved.
        self.modelOutput = None
        # The dbConnect method assigns to 'cnxn' and 'cursor'
        self.cnxn = None
        self.cursor = None
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
            tapSigma = round(self.TAPSIGMAPCT * (tb + 1))
            
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
                    self.translateTaps(lowerTaps=v['lower_taps'],
                                       raiseTaps=v['raise_taps'],
                                       pos=tapPos)
                
                # Assign indices for this regulator
                self.reg[r]['taps'][tap]['ind'] = (s, e)
                
                # Increment start index.
                s += len(binList)
    
    @staticmethod
    def translateTaps(lowerTaps, raiseTaps, pos):
        """Method to translate tap integer in range 
        [0, lowerTaps + raiseTaps - 1] to range [-(lower_taps - 1), raise_taps]
        """
        # TODO: unit test
        if pos <= (lowerTaps - 1):
            posOut = pos - lowerTaps + 1
        else:
            posOut = pos - raiseTaps + 1
            
        return posOut
                
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
                capStatus = individual.CAPSTATUS[capBinary]
                
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
                    self.translateTaps(lowerTaps=regDict[reg]['lower_taps'],
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
                    individual.CAPSTATUS[capChrom[phaseData['ind']]]
                    
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
        
                
    def dbConnect(self, driver='MySQL ODBC 5.3 Unicode Driver', usr='gridlabd',
                  host='localhost', schema='gridlabd'):
        """Connect to database.
        
        System will need an odbc driver for MySQL.
        """
        # TODO: Accomodate Linux and maybe Mac
        # Build the connection string.
        c = ('Driver={{{driver}}};Login Prompt=False;UID={usr};'
             'Data Source={host};Database={schema};CHARSET=UTF8')
        
        # Initialize connection
        self.cnxn = pyodbc.connect(c.format(driver=driver, usr=usr, host=host,
                                            schema=schema))
        
        # pyodbc documentation says we need to configure decoding/encoding: 
        # https://github.com/mkleehammer/pyodbc/wiki/Connecting-to-MySQL
        self.cnxn.setdecoding(pyodbc.SQL_WCHAR, encoding='utf-8')
        self.cnxn.setencoding(encoding='utf-8')
        
        # Initialize the cursor
        self.cursor = self.cnxn.cursor()
                
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
        # TODO - this should be performed outside of this function, and the
        # filename should be input
        fFull = os.path.basename(inPath)
        fParts = os.path.splitext(fFull) 
        fName = fParts[0]
        outPath = os.path.join(outDir, fName + '_' + str(self.uid) + fParts[1])
        
        # Track the output path for running the model later.
        self.model = outPath
        
        # Instantiate a writeCommands object.
        writeObj = writeCommands.writeCommands(strModel=strModel,
                                               pathModelIn=inPath,
                                               pathModelOut=outPath)
        
        # Update the swing node to a meter and get it writing power to a table
        # TODO: clear table first?
        # TODO: swing table won't be the only table.
        self.table = writeObj.recordSwing(uid=self.uid)
        
        # Get regulators in dict usable by writeCommands
        regDict = dict()
        for reg, tapDict in self.reg.items():
            # For now, force regulators to be in MANUAL control mode.
            regDict[reg] = {'regulator': {}, 
                            'configuration': {'Control': 'MANUAL'}}
            # Assign tap positions.
            for tap in tapDict['taps']:
                regDict[reg]['regulator'][tap] = tapDict['taps'][tap]['pos']
                
        # Get capacitors in dict urable by writeCommands
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
    
    def runModel(self, gldPath='gridlabd'):
        """Function to run GridLAB-D model and write results to file.
        
        If gridlabd is not setup to just run with 'gridlabd,' pass in
            the full path to the executable in gldPath
        """
        
        # Construct command and run model. 
        # TODO: Pipe output to file for fitness evaluation? Overloads, etc.
        # TODO: Raise exception if self.model is an empty string.
        # TODO: How to handle a model that doesn't converge?
        cmd = gldPath + ' ' + self.model

        self.modelOutput = subprocess.run(cmd, stdout=subprocess.PIPE,
                                stderr=subprocess.PIPE)
                            
    def evalFitness(self):
        """Function to evaluate fitness of individual.
        
        For now, this will just be total energy consumed.
        
        For now, drop table after done.
        TODO: Don't drop table at this point. It slows things down.
        
        TODO: Add more evaluators of fitness like voltage violations, etc.
        """
        # Connect to the database.
        # TODO: Add connection options to this function.
        self.dbConnect()
        
        # Extract all the rows
        # TODO: elminate hard-coding of column names
        # TODO: figure out why the pyodbc binding isn't working....
        s = ('SELECT measured_power_A, measured_power_B, '
             'measured_power_C FROM {}').format(self.table)
        #self.cursor.execute(s, self.table)
        self.cursor.execute(s)
        
        # Loop through the rows, compute total energy
        t = 0+0j
        cols = ['measured_power_A', 'measured_power_B', 'measured_power_C']
        
        for row in self.cursor:
            for col in cols:
                # Strip off the unit included with the complex number and
                # add it to the total
                v = getattr(row, col)
                t += complex(v.split()[0])
        
        # Assign the fitness.        
        self.fitness = t.__abs__()
        
        # Drop table.
        self.dropTable(self.table)
        
    def dropTable(self, table):
        """ Simple method to drop a table from the database.
        """
        # TODO: Figure out why pyodbc binding isn't working
        self.cursor.execute('DROP TABLE {}'.format(table))
                            
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
    print('hooray')
            
        
        