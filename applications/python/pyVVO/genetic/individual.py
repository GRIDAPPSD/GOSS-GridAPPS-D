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

class individual:
    
    def __init__(self, uid, reg={}, cap={}):
        """Initialize and individual by building their "chromosome" based
        on available voltage control devices
        
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
            
            start: simulation start time (starttime in GLD). Should be
                surrounded in single quotes, and be in the format
                'yyyy-mm-dd HH:MM:SS'
            stop: simulation stop time (stoptime in GLD). Same format as start.
            
            NOTE: Timezones can be included in start and stop.
            
        TODO: add controllable DERs
                
        """
        
        # Assign the unique identifier
        self.uid = uid
        # The writeModel method assigns to 'model' and 'table.'
        self.model = None
        self.table = None
        # The dbConnect method assigns to 'cnxn' and 'cursor'
        self.cnxn = None
        self.cursor = None
        # The evalFitness method assigns to fitness
        self.fitness = None
        
        # Get an instance of the random class and seed it
        rand = random.Random()
        rand.seed()
        
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
            
            # Define the upper tap bound (tb).
            tb = v['raise_taps'] + v['lower_taps'] - 1
            
            # Loop through the phases
            for tap in v['taps']:
                # Initialize dict for this tap.
                self.reg[r][tap] = dict()
                
                # Randomly select a tap.
                tapPos = rand.randint(0, tb)
                
                # Express tap setting as binary list.
                binList = [int(x) for x in "{0:b}".format(tapPos)]
                
                # Extend the regulator chromosome.
                self.regChrom.extend(binList)
                
                # Increment end index.
                e += len(binList) + 1
                
                # Translate tapPos for GridLAB-D.
                if tapPos <= (v['lower_taps'] - 1):
                    self.reg[r][tap]['pos'] = tapPos - v['lower_taps'] + 1
                else:
                    self.reg[r][tap]['pos'] = tapPos - v['raise_taps'] + 1
                
                # Assign indices for this regulator
                self.reg[r][tap]['ind'] = (s, e)
                
                # Increment start index.
                s += len(binList) + 1
            
        # Initialize chromosome for capacitors and dict to store list indices.
        self.capChrom = []
        self.cap = dict()
        # Initialize index counters.
        s = 0;
        e = 0;
        # Loop through the capacitors, randomly assign state for each phase
        # 0 --> open, 1 --> closed
        for c, p in cap.items():
            # Increment end index based on number of phases
            e += len(p) + 1
            
            # Assign indices for this capacitor
            self.cap[c] = {'ind': (s, e), 'status': {}}
            
            # Increment the starting index for the next iteration
            s += len(p) + 1
            
            # Loop through the phases and randomly decide state
            for phase in p:
                # Note that random() is on interval [0.0, 1.0). Thus, we'll
                # consider [0.0, 0.5) and [0.5, 1.0) for our intervals 
                if rand.random() < 0.5:
                    capBinary = 0
                    capStatus = 'OPEN'
                else:
                    capBinary = 1
                    capStatus = 'CLOSED'
                
                # Assign to the capacitor
                self.capChrom.append(capBinary)
                self.cap[c]['status'][phase] = capStatus
                
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
            for tap in tapDict:
                regDict[reg]['regulator'][tap] = tapDict[tap]['pos']
                
        # Get capacitors in dict urable by writeCommands
        capDict = dict()
        for cap, capData in self.cap.items():
            # Put 'status' dictionary into capDict
            capDict[cap] = capData['status']
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
    
    def runModel(self, gldPath='gridlabd', quiet=True):
        """Function to run GridLAB-D model and write results to file.
        
        If gridlabd is not setup to just run with 'gridlabd,' pass in
            the full path to the executable in gldPath
        """
        
        # Construct command and run model. 
        # TODO: Pipe output to file for fitness evaluation? Overloads, etc.
        # TODO: Raise exception if self.model is an empty string.
        # TODO: How to handle a model that doesn't converge?
        cmd = gldPath + ' ' + self.model
        # Quiet the output
        if quiet:
            cmd += ' --quiet'
            
        #result = 
        subprocess.run(cmd)#, stdout=subprocess.PIPE,
                            #stderr=subprocess.PIPE)
                            
    def evalFitness(self):
        """Function to evaluate fitness of individual.
        
        For now, this will just be total energy consumed.
        
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
        row = self.cursor.fetchone()
        t = 0+0j
        cols = ['measured_power_A', 'measured_power_B', 'measured_power_C']
        
        while row is not None:
            for col in cols:
                # Strip off the unit included with the complex number and
                # add it to the total
                v = getattr(row, col)
                t += complex(v.split()[0])
                
            row = self.cursor.fetchone()
                
        self.fitness = t
                            
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
            
        
        