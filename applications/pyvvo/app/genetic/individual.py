"""
required packages: pyodbc

Created on Aug 9, 2017

@author: thay838
"""
import random
from powerflow import writeCommands
import util.gld
import util.helper
import math
import os

# Define cap status, to be accessed by binary indices.
CAPSTATUS = ['OPEN', 'CLOSED']
# Define the percentage of the tap range which sigma should be for drawing
# tap settings. Recall the 68-95-99.7 rule for normal distributions -
# 1 std dev, 2 std dev, 3 std dev probabilities
TAPSIGMAPCT = 0.1
# Define mode of triangular distribution for biasing capacitors. NOTE: By
# making the mode greater than 0.5, we're more likely to draw a number which
# rounds to one. Therefore, we should maintain the previous state if the draw
# rounds to one.
CAPTRIANGULARMODE = 0.8
 
class individual:
    
    def __init__(self, uid, starttime, stoptime, voltdumpFiles, reg=None,
                 regFlag=5, cap=None, capFlag=5, regChrom=None, 
                 capChrom=None, parents=None):
        """An individual contains information about Volt/VAR control devices
        
        Individuals can be initialized in two ways: 
        1) From scratch: Provide self, uid, reg, and cap inputs. Positions
            will be randomly generated
            
        2) From a chromosome: Provide self, reg, cap, uid, regChrom, and 
            capChrom. After crossing two individuals, a new chromosome will
            result. Use this chromosome to update reg and cap
        
        INPUTS:
            starttime: date/time str in format of gld.DATE_FMT for model start.
            stoptime: "..." end 
            voltdumpFiles: list of filenames of voltdump files. Used to
                evaluate voltage violations.
            reg: Dictionary as described in the docstring for the gld module.
            
                Note possible tap positions be in interval [-(lower_taps - 1),
                raise_taps].
            
            regFlag: Flag for special handling of regulators.
                0: All regulator taps set to their minimum tap position
                1: All regulator taps set to their maximum tap position
                2: Regulator tap positions will be biased (via a Gaussian 
                    distribution and the TAPSIGMAPCT constant) toward the
                    previous tap positions
                3: Regulator state unchanged - simply reflects what's in the 
                    reg input's 'prevState'
                4: Regulator state given in reg input's 'newState' - just need
                    to generate chromosome and count tap changes
                5: Regulator tap positions will be determined randomly 
                    
            cap: Dictionary describing capacitors as described in the docstring
                for the gld module.
                
            capFlag: Flag for special handling of capacitors.
                0: All capacitors set to CAPSTATUS[0] (OPEN)
                1: All capacitors set to CAPSTATUS[1] (CLOSED)
                2: Capacitor states will be biased (via a triangular
                    distribution with the mode at CAPTRIANGULARMODE) toward the
                    previous states
                3: Capacitor state unchanged - simply reflects what's in the
                    'cap' input's 'prevState'.
                4: Capacitor state given in cap input's 'newState' - just need
                    to generate chromosome and count switching events
                5: Capacitor positions will be determined randomly.
                
            uid: Unique ID of individual. Should be an int.
            
            regChrom: Regulator chromosome resulting from crossing two
                individuals. List of ones and zeros representing tap positions. 
                
            capChrom: Capacitor chromosome resulting from crossing two
                individuals. List of ones and zeros representing switch status.
                
            parents: Tuple of UIDs of parents that created this individual.
                None if individual was randomly created at population init.
            
        TODO: add controllable DERs
                
        """
        # Initialize some attributes that also need reset when re-using an
        # individual.
        self.prep(starttime=starttime, stoptime=stoptime)
        
        # Assign voltdumpFiles
        self.voltdumpFiles = voltdumpFiles
        
        # Assign reg and cap dicts
        self.reg = reg
        self.cap = cap
        
        # Assign the unique identifier.
        self.uid = uid
        
        # Parent tracking is useful to see how well the GA is working
        self.parents = parents
        
        # When writing model, output directory will be saved.
        self.outDir = None
        
        # If not given a regChrom or capChrom, generate them.
        if (regChrom is None) and (capChrom is None):
            # Generate regulator chromosome:
            self.genRegChrom(flag=regFlag)
            # Generate capacitor chromosome:
            self.genCapChrom(flag=capFlag)
            # TODO: DERs
        else:
            # Use the given chromosomes to update the dictionaries.
            self.regChrom = regChrom
            self.modifyRegGivenChrom()
            self.capChrom = capChrom
            self.modifyCapGivenChrom()
            
    def prep(self, starttime, stoptime, reg=None, cap=None):
        """Method to get an individual ready for use/re-use - in the genetic
            algorithm, the population for the next time interval should be
            seeded with the best individuals from the previous time interval.
            
        During a call to the constructor, this gets attributes initialized.
        
        Pass in 'reg' and 'cap' ONLY to get an individual ready to be used in
        a new population.
        """
        # Assing times.
        self.starttime = starttime
        self.stoptime = stoptime
        # Full path to output model.
        self.model = None
        # Table information
        self.swingTable = None
        self.swingColumns = None
        self.swingInterval = None
        
        # When the model is run, output will be saved.
        self.modelOutput = None

        # The evalFitness method assigns costs
        self.costs = None
        
        # Update the 'prevState' of the individuals reg and cap dictionaries.
        if reg and cap:
            out = util.helper.updateVVODicts(regOld=self.reg, capOld=self.cap,
                                             regNew=reg, capNew=cap)
            
            self.reg = out['reg']
            self.cap = out['cap']
            self.tapChangeCount = out['tapChangeCount']
            self.capSwitchCount = out['capSwitchCount']
        else:
            # Track tap changes and capacitor switching
            self.tapChangeCount = 0
            self.capSwitchCount = 0
            
    def __eq__(self, other):
        """Compare individuals by looping over their chromosomes
        
        TODO: This isn't very sophisticated. It doesn't check if chromosomes
        are the same length, exist, etc.
        """
        # Start with regulator
        for k in range(len(self.regChrom)):
            if self.regChrom[k] != other.regChrom[k]:
                return False
            
        # On to capacitor
        for k in range(len(self.capChrom)):
            if self.capChrom[k] != other.capChrom[k]:
                return False
            
        return True
        
            
    def __str__(self):
        """Individual's string should include fitness and reg/cap info.
        
        This is a simple wrapper to call helper.getSummaryStr since the
        benchmark system should be displayed in the same way.
        """
        s = util.helper.getSummaryStr(costs=self.costs, reg=self.reg,
                                      cap=self.cap, regChrom=self.regChrom,
                                      capChrom=self.capChrom,
                                      parents=self.parents)
        
        return s
        
    def genRegChrom(self, flag):
        """Method to randomly generate an individual's regulator chromosome
        
        INPUTS:
            flag: dictates how regulator tap positions are created.
                0: All regulator taps set to their minimum tap position
                1: All regulator taps set to their maximum tap position
                2: Regulator tap positions will be biased (via a Gaussian 
                    distribution and the TAPSIGMAPCT constant) toward the
                    previous tap positions
                3: Regulator state unchanged - simply reflects what's in the 
                    reg input's 'prevState'
                4: Regulator state given in reg input's 'newState' - just need
                    to generate chromosome and count tap changes
                5: Regulator tap positions will be determined randomly 
        """
        # Initialize chromosome for regulator and dict to store list indices.
        self.regChrom = ()
         
        # Intialize index counters.
        s = 0;
        e = 0;
        
        # Loop through the regs and create binary representation of taps.
        for r, v in self.reg.items():
            
            # Define the upper tap bound (tb).
            tb = v['raise_taps'] + v['lower_taps'] - 1
            
            # Compute the needed field width to represent the upper tap bound
            width = math.ceil(math.log(tb, 2))
            
            # Define variables as needed based on the flag. I started to try to
            # make micro-optimizations for code factoring, but let's go for
            # readable instead.
            if flag== 0:
                newState = 0
            elif flag == 1:
                newState = tb
            elif flag == 2:
                # If we're biasing from the previous position, get a sigma for
                # the Gaussian distribution.
                tapSigma = round(TAPSIGMAPCT * (tb + 1))
            elif flag == 3:
                state = 'prevState'
            elif flag == 4:
                state = 'newState'
            
            # Loop through the phases
            for phase, phaseData in v['phases'].items():
                
                # If we're biasing new positions based on previous positions:
                if flag == 2:
                    # Randomly draw tap position from gaussian distribution.
                    
                    # Translate previous position to integer on interval [0,tb]
                    prevState = \
                        util.gld.inverseTranslateTaps(lowerTaps=v['lower_taps'],
                                                 pos=phaseData['prevState'])
                        
                    # Initialize the newState for while loop.
                    newState = -1
                    
                    # The standard distribution runs from (-inf, +inf) - draw 
                    # until position is valid. Recall valid positions are
                    # [0, tb]
                    while (newState < 0) or (newState > tb):
                        # Draw the tap position from the normal distribution.
                        # Here oure 'mu' is the previous value
                        newState = round(random.gauss(prevState, tapSigma))
                        
                elif (flag == 3) or (flag == 4):
                    # Translate position to integer on interval [0, tb]
                    newState = \
                        util.gld.inverseTranslateTaps(lowerTaps=v['lower_taps'],
                                                      pos=phaseData[state])
                        
                elif flag == 5:
                    # Randomly draw.
                    newState = random.randint(0, tb)
                
                # Express tap setting as binary list with consistent width.
                binTuple = tuple([int(x) for x in "{0:0{width}b}".format(newState,
                                                                  width=width)])
                
                # Extend the regulator chromosome.
                self.regChrom += binTuple
                
                # Increment end index.
                e += len(binTuple)
                
                # Translate newState for GridLAB-D.
                self.reg[r]['phases'][phase]['newState'] = \
                    util.gld.translateTaps(lowerTaps=v['lower_taps'], pos=newState)
                    
                # Increment the tap change counter (previous pos - this pos)
                self.tapChangeCount += \
                    abs(self.reg[r]['phases'][phase]['prevState']
                        - self.reg[r]['phases'][phase]['newState'])
                
                # Assign indices for this phase
                self.reg[r]['phases'][phase]['chromInd'] = (s, e)
                
                # Increment start index.
                s += len(binTuple)
                
    def genCapChrom(self, flag):
        """Method to generate an individual's capacitor chromosome.
        
        INPUTS:
            flag:
                0: All capacitors set to CAPSTATUS[0] (OPEN)
                1: All capacitors set to CAPSTATUS[1] (CLOSED)
                2: Capacitor states will be biased (via a triangular
                    distribution with the mode at CAPTRIANGULARMODE) toward the
                    previous states
                3: Capacitor state unchanged - simply reflects what's in the
                    'cap' input's 'prevState'.
                4: Capacitor state given in cap input's 'newState' - just need
                    to generate chromosome and count switching events
                5: Capacitor positions will be determined randomly.
                
                
        OUTPUTS:
            modifies self.cap, sets self.capChrom
        """
        # If we're forcing all caps to the same status, determine the binary
        # representation. TODO: add input checking.
        if flag < 2:
            capBinary = flag
            capStatus = CAPSTATUS[flag]
        elif flag == 3:
            state = 'prevState'
        elif flag == 4:
            state = 'newState'
        
        # Initialize chromosome for capacitors and dict to store list indices.
        self.capChrom = ()

        # Keep track of chromosome index
        ind = 0
        
        # Loop through the capacitors, randomly assign state for each phase
        for c, capData in self.cap.items():
            
            # Loop through the phases and randomly decide state
            for phase in capData['phases']:
                
                # Take action based on flag.
                if flag == 2:
                    # Use triangular distribution to bias choice
                    draw = round(random.triangular(mode=CAPTRIANGULARMODE))
                    
                    # Extract the previous state
                    prevState = self.cap[c]['phases'][phase]['prevState']
                    
                    # If the draw rounded to one, use the previous state.
                    # If the draw rounded to zero, use the opposite state
                    if draw:
                        capStatus = prevState
                        capBinary = CAPSTATUS.index(capStatus)
                    else:
                        # Flip the bit
                        capBinary = 1 - CAPSTATUS.index(prevState) 
                        capStatus = CAPSTATUS[capBinary]
                        
                elif (flag == 3) or (flag == 4):
                    # Use either 'prevState' or 'newState' for this state
                    capStatus = self.cap[c]['phases'][phase][state]
                    capBinary = CAPSTATUS.index(capStatus)
                elif flag == 5:
                    # Randomly determine state
                    capBinary = round(random.random())
                    capStatus = CAPSTATUS[capBinary]
                    
                # Assign to the capacitor
                self.capChrom += (capBinary,)
                self.cap[c]['phases'][phase]['newState'] = capStatus
                self.cap[c]['phases'][phase]['chromInd'] = ind
                
                # Increment the switch counter if applicable
                if (self.cap[c]['phases'][phase]['newState'] !=
                        self.cap[c]['phases'][phase]['prevState']):
                    
                    self.capSwitchCount += 1
                
                # Increment the chromosome counter
                ind += 1
                
    def modifyRegGivenChrom(self):
        """Modifiy self.reg based on self.regChrom
        """
        # Loop through self.reg and update 'newState'
        for r, regData in self.reg.items():
            for phase, phaseData in regData['phases'].items():
                
                # Extract the binary representation of tap position.
                tapBin = \
                    self.regChrom[phaseData['chromInd'][0]:\
                                  phaseData['chromInd'][1]]
                    
                # Convert the binary to an integer
                posInt = util.helper.bin2int(tapBin)
                
                # Convert integer to tap position and assign to new position
                self.reg[r]['phases'][phase]['newState'] = \
                    util.gld.translateTaps(lowerTaps=self.reg[r]['lower_taps'],
                                      pos=posInt)
                    
                # Increment the tap change counter (previous pos - this pos)
                self.tapChangeCount += \
                    abs(self.reg[r]['phases'][phase]['prevState']
                        - self.reg[r]['phases'][phase]['newState'])
    
    def modifyCapGivenChrom(self):
        """Modify self.cap based on self.capChrom
        """
        # Loop through the capDict and assign 'newState'
        for c, capData in self.cap.items():
            for phase in capData['phases']:
                # Read chromosome and assign newState
                self.cap[c]['phases'][phase]['newState'] = \
                    CAPSTATUS[self.capChrom[self.cap[c]['phases'][phase]\
                                            ['chromInd']]]
                
                # Bump the capacitor switch count if applicable
                if self.cap[c]['phases'][phase]['newState'] != \
                        self.cap[c]['phases'][phase]['prevState']:
                    
                    self.capSwitchCount += 1
                
    def writeModel(self, strModel, inPath, outDir):
        """Create a GridLAB-D .glm file for the given individual by modifying
        setpoints for controllable devices (capacitors, regulators, eventually
        DERs)
        
        INPUTS:
            self: constructed individual
            strModel: string of .glm file found at inPath
            inPath: path to model to modify volt control settings
            outDir: directory to write new model to. Filename will be inferred
                from the inPath, and the individuals uid preceded by an
                underscore will be added
            dumpGroup: Name of group used for triplex_loads.
                
        OUTPUTS:
            Path to new model after it's created
        """
        # Check if directory exists - if not, create it.
        if not os.path.isdir(outDir):
            os.mkdir(outDir)
        
        # Assign output directory.
        self.outDir = outDir
        
        # Get the filename of the original model and create output path
        model = \
            writeCommands.writeCommands.addFileSuffix(inPath=\
                                                        os.path.basename(inPath),
                                                      suffix=str(self.uid))
        
        # Track the output path for running the model later.
        self.model = model
        
        # Instantiate a writeCommands object.
        writeObj = writeCommands.writeCommands(strModel=strModel,
                                               pathModelIn=inPath,
                                               pathModelOut=(outDir + '/'
                                                             + model))
        
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
        # Set regulator and capacitor control schemes to manual.
        for r in self.reg:
            self.reg[r]['Control'] = 'MANUAL'
            
        for c in self.cap:
            self.cap[c]['control'] = 'MANUAL'
        
        # Change capacitor and regulator statuses/positions.
        writeObj.commandRegulators(reg=self.reg)
        writeObj.commandCapacitors(cap=self.cap)
        
        # Write the modified model to file.
        writeObj.writeModel()
    
    def runModel(self):
        """Function to run GridLAB-D model.
        """
        self.modelOutput = util.gld.runModel((self.outDir + '/' + self.model))
                            
    def evalFitness(self, cursor, energyPrice, tapChangeCost, capSwitchCost,
                    voltCost, tCol='t'):
        """Function to evaluate fitness of individual. This is essentially a
            wrapper to call util.gld.computeCosts
        
        TODO: Add more evaluators of fitness like voltage violations, etc.
        
        INPUTS:
            cursor: pyodbc cursor from pyodbc connection 
            energyPrice: price of energy
            tapChangeCost: cost changing regulator taps
            capSwitchCost: cost of switching a capacitor
            voltCost: cost of voltage violations.
            tCol: name of time column(s)
            starttime: starting time to evaluate
            stoptime: ending time to evaluate
        """
        self.costs = util.gld.computeCosts(cursor=cursor,
                                            powerTable=self.swingTable,
                                            powerColumns=self.swingColumns,
                                            powerInterval=self.swingInterval,
                                            energyPrice=energyPrice,
                                            starttime=self.starttime,
                                            stoptime=self.stoptime,
                                            tCol=tCol,
                                            tapChangeCost=tapChangeCost,
                                            capSwitchCost=capSwitchCost,
                                            tapChangeCount=self.tapChangeCount,
                                            capSwitchCount=self.capSwitchCount,
                                            voltCost=voltCost,
                                            voltdumpDir=self.outDir,
                                            voltdumpFiles=self.voltdumpFiles
                                            )
        
    def buildCleanupDict(self, truncateFlag=False):
        """Function to build dictionary to be passed to the 'cleanupQueue' of 
        the 'cleanup' method.
        """
        d = {'tables': [self.swingTable], 'files': list(self.voltdumpFiles),
             'dir': self.outDir}
        # Add the model file to the file list.
        d['files'].append(self.model)
        
        # Set flag for table truncation (rather than deletion)
        d['truncateFlag'] = truncateFlag
        
        # Return.
        return d

def cleanup(cleanupQueue):
    """Method to cleanup (delete) an individuals files, tables, etc. 
    
    As the genetic algorithm grows in sophistication, more output is 
    created. With so many files and tables floating around, it can
    simply take forever to clean things up. This function should be called
    before an individual is deleted.
    
    Note this function is specifically formatted to work with threads.
    
    INPUTS:
        cleanupQueue: queue which will have dictionaries inserted into it.
            Dictionaries should contain a list of tables in 'tables', a list
            of files in 'files', and a directory to find the files in 'dir'.
    """
    while True:
        # Extract inputs from the queue.
        inDict = cleanupQueue.get()
        
        # Check input.
        if inDict is None:
            # 'None' is the done signal.
            cleanupQueue.task_done()
            break
        
        # Connect to the database.
        cnxn = util.db.connect()
        cursor = cnxn.cursor()
        
        # Drop or truncate all tables.
        if inDict['truncateFlag']:
            for t in inDict['tables']:
                util.db.truncateTable(cursor=cursor, table=t)            
        else:
            for t in inDict['tables']:
                util.db.dropTable(cursor=cursor, table=t)
            
        # Delete all files.
        for f in inDict['files']:
            os.remove(inDict['dir'] + '/' + f)
            
        # Delete the directory. This will error if the directory isn't empty -
        # this is good, it'll prevent us from forgetting to clean up.
        os.rmdir(inDict['dir'])
        
        # Cleanup complete.
        cleanupQueue.task_done()
        