"""Module for interfacing with GridLAB-D.

IMPORTANT: 
For consistency, there will be one global dictionary format for regulators and
one global dictionary format for capacitors. That is defined below:

Global regulator dictionary:
Top level keys are regulator names.

regulators={'reg_VREG4': {'raise_taps': 16,
                          'lower_taps': 16,
                          'phases': {'A': {'newState': 10, 'prevState': 11,
                                           'chromInd': (0, 4)},
                                     'B': {'newState': 7, 'prevState': 11, 
                                           'chromInd': (4, 8)},
                                     'C': {'newState': 2, 'prevState': 4,
                                           'chromInd': (8, 12)},
                                  },
                          'Control': 'MANUAL'
                         },
            'reg_VREG2': {'raise_taps': 16,
                          'lower_taps': 16,
                          'phases': {'A': {'newState': 10, 'prevState': 11,
                                           'chromInd': (12, 16)},
                                     'B': {'newState': 7, 'prevState': 11, 
                                           'chromInd': (16, 20)},
                                     'C': {'newState': 2, 'prevState': 4,
                                           'chromInd': (20, 24)},
                                  },
                          'Control': 'MANUAL'
                          }
            }

capacitors={'cap_capbank2a': {'phases': {'A': {'newState': 'CLOSED',
                                               'prevState': 'OPEN', 
                                               'chromInd': 0
                                               },
                                         'B': {'newState': 'OPEN',
                                               'prevState': 'OPEN',
                                               'chromInd': 1
                                               },
                                         },
                              'control': 'MANUAL'
                             },
            'cap_capbank2c': {'phases': {'A': {'newState': 'CLOSED',
                                               'prevState': 'OPEN',
                                               'chromInd': 2
                                               },
                                         'C': {'newState': 'OPEN',
                                               'prevState': 'OPEN',
                                               'chromInd': 3}
                                         }
                              }
            }

Created on Aug 29, 2017

@author: thay838
"""
import subprocess
import csv
import os
import util.constants

# definitions for regulator and capacitor properties
REG_CHANGE_PROPS = ['tap_A_change_count', 'tap_B_change_count',
                    'tap_C_change_count']
REG_STATE_PROPS = ['tap_A', 'tap_B', 'tap_C']
CAP_CHANGE_PROPS = ['cap_A_switch_count', 'cap_B_switch_count',
                    'cap_C_switch_count']
CAP_STATE_PROPS = ['switchA', 'switchB', 'switchC']
MEASURED_POWER = ['measured_power_A', 'measured_power_B', 'measured_power_C']
MEASURED_ENERGY = ['measured_real_energy']

def runModel(modelPath, gldPath=None):
    #, gldPath=r'C:/gridlab-d/develop'):
    """Function to run GridLAB-D model.
    
    IMPORTANT NOTE: the gridlabd path is assumed to be setup.
    See http://gridlab-d.shoutwiki.com/wiki/MinGW/Eclipse_Installation#Linux_Installation
    and do a search for 'Environment Setup'
    In short, assuming build is in gridlab-d/develop:
        PATH must contain gridlab-d/develop/bin
        GLPATH must contain gridlab-d/develop/lib/gridlabd and gridlab-d/develop/share/gridlabd
        CXXFLAGS must be set to include gridlab-d/develop/share/gridlabd
    
    
    TODO: Add support for command line inputs.
    """
    cwd, model = os.path.split(modelPath)
    """
    # Set up the gridlabd path, run model
    # TODO: Get this adapated for Unix.
    # NOTE: On Windows, spaces between the '&&'s causes everything to break...
    cmd = r'set PATH={}/bin;%PATH%'.format(gldPath)
    cmd += r'&&set GLPATH={0}/share/gridlabd;{0}/lib/gridlabd'.format(gldPath)
    cmd += r'&&set CXXFLAGS=-I{}/share/gridlabd'.format(gldPath)
    cmd += r'&&gridlabd'
    cmd += r' ' + model
    """
    # Setup environment if necessary
    if gldPath:
        # We'll use forward slashes here since GLD can have problems with 
        # backslashes... Ugh.
        gldPath = gldPath.replace('\\', '/')
        env = os.environ
        binStr = "{}/bin".format(gldPath)
        # We can form a kind of memory leak where we grow the environment
        # variables if we add these elements repeatedly, so check before 
        # adding or changing.
        if binStr not in env['PATH']:
            env['PATH'] = binStr + os.pathsep + env['PATH']

        env['GLPATH'] = ("{}/lib/gridlabd".format(gldPath) + os.pathsep
                         + "{}/share/gridlabd".format(gldPath))
        
        env['CXXFLAGS'] = "-I{}/share/gridlabd".format(gldPath)
    else:
        env = None
    
    # Run command. Note with check=True exception will be thrown on failure.
    # TODO: rather than using check=True, handle the event of a GridLAB-D error
    # NOTE: it's best practice to pass args as a list. If args is a list, using
    # shell=True creates differences across platforms. Just don't do it.
    output = subprocess.run(['gridlabd', model], stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE, cwd=cwd, env=env,
                            check=True)
    return output

def translateTaps(lowerTaps, pos):
    """Method to translate tap integer in range 
    [0, lowerTaps + raiseTaps] to range [-lower_taps, raise_taps]
    """
    # TODO: unit test
    # Hmmm... is it this simple? 
    posOut = pos - lowerTaps
    return posOut

def inverseTranslateTaps(lowerTaps, pos):
    """Method to translate tap integer in range
        [-lower_taps, raise_taps] to range [0, lowerTaps + raiseTaps]
    """
    # TODO: unit test
    # Hmmm... is it this simle? 
    posOut = pos + lowerTaps
    return posOut

def computeCosts(dbObj, swingData, costs, starttime, stoptime,
                 voltFilesDir=None,
                 voltFiles=None, tCol='t', idCol='id', tapChangeCount=None,
                 tapChangeTable=None, tapChangeColumns=None,
                 capSwitchCount=None, capSwitchTable=None,
                 capSwitchColumns=None
                 ):
    """Method to compute VVO costs for a given time interval. This includes
    cost of energy, capacitor switching, and regulator tap changing. Later this
    should include cost of DER engagement.
    
    INPUTS:
        dbObj: initialized util/db.db class object
        swingData: dict with the following fields:
            table: name of table for getting swing node power.
            columns: name of the columns corresponding to the swing table.
                These will vary depending on node-type (substation vs. meter)
            interval: recording interval (seconds) of the swing table
        costs: dictionary with the following fields:
            realEnergy: price of energy in $/Wh
            tapChange: cost ($) of changing one regulator tap one position.
            capSwitch: cost ($) of switching a single capacitor
            undervoltage: cost of an undervoltage violation
            overvoltage: cost of an overvoltage violation
            powerFactorLead: dict with two fields:
                limit: minimum tolerable leading powerfactor
                cost: cost of a 0.01 pf deviation from the lead limit
            powerFactorLag: dict with two fields:
                limit: minimum tolerable lagging powerfactor
                cost: cost of a 0.01 pf deviation from the lag limit
        starttime: starting timestamp (yyyy-mm-dd HH:MM:SS) of interval in
            question.
        stoptime: stopping ""
        voltFilesDir: Directory of files to be read to determine voltage 
            violations
        voltFiles: list of voltFiles (without the path 'head'). Note that files
            are assumed to monitor the same nodes, just different phases.
        tCol: name of time column. Assumed to be the same for tap/cap tables.
            Only include if a table is given.
        tapChangeCount: total number of all tap changes. Don't include this 
            input if tapChangeTable and tapChangeColumns are included.
        tapChangeTable: table for recording regulator tap changes. Don't
            include this input if tapChangeCount is specified
        tapChangeColumns: list of columns corresponding to tapChangeTable for
            computing total operations. Likely will be
            ['tap_A_change_count', ...]
        capSwitchCount: total number of all capacitor switching events. Don't
            include this if capChangeTable and capChangeColumns are given.
        capSwitchTable: table for recording capacitor switching events. Don't
            include this if capSwitchCount is specified.
        capSwitchColumns: columns of capSwitchTable to sum over to get total 
            count. Likely to be ['cap_A_switch_count',...]
            
    NOTE: At this point, all capacitors and regulators are assigned the same
    cost. If desired later, it wouldn't be too taxing to break that cost down
    by piece of equipment.
    """
    # Initialize dictionary
    costDict = {}
    # *************************************************************************
    # ENERGY COST
    
    # Read energy database. Note times - this should return a single row only.
    energyRows = dbObj.fetchAll(table=swingData['energy']['table'],
                                cols=swingData['energy']['columns'],
                                starttime=stoptime, stoptime=stoptime)

    # Due to time and ID filtering, we should get exactly one row.
    assert len(energyRows) == 1
    
    # Compute the cost
    costDict['realEnergy'] = energyRows[0][0] * costs['realEnergy']
    #**************************************************************************
    # POWER FACTOR COST
    # Initialize costs
    costDict['powerFactorLead'] = 0
    costDict['powerFactorLag'] = 0
    
    # Get list of sums of rows (sum three phase power) from the database
    power = dbObj.sumComplexPower(table=swingData['power']['table'],
                                  cols=swingData['power']['columns'],
                                  starttime=starttime, stoptime=stoptime)
    
    # Loop over each row, compute power factor, and assign cost
    for p in power['rowSums']:
        pf, direction = util.helper.powerFactor(p)
        # Determine field to use based on whether pf is leading or lagging
        if direction == 'lag':
            field = 'powerFactorLag'
        elif direction == 'lead':
            field = 'powerFactorLead'
        
        # If the pf is below the limit, add to the relevant cost 
        if pf < costs[field]['limit']:
            # Cost represents cost of a 0.01 deviation, so multiple violation
            # by 100 before multiplying by the cost.
            costDict[field] += ((costs[field]['limit'] - pf) * 100
                                * costs[field]['cost'])
    
    # *************************************************************************
    # TAP CHANGING COST
    
    # If tap table and columns given, compute the total count.    
    if (tapChangeTable and tapChangeColumns and tCol):
        # Sum all the tap changes.
        tapChangeCount = dbObj.sumMatrix(table=tapChangeTable,
                                         cols=tapChangeColumns, tCol=tCol,
                                         starttime=stoptime, stoptime=stoptime)
    elif (tapChangeCount is None):
        assert False, ("If tapChangeCount is not specified, tapChangeTable and"
                       " tapChangeColumns must be specified!")
        
    # Simply multiply cost by number of operations.   
    costDict['tapChange'] = costs['tapChange'] * tapChangeCount
    
    # *************************************************************************
    # CAP SWITCHING COST
    
    # If cap table and columns given, compute the total count.
    if (capSwitchTable and capSwitchColumns and tCol):
        # Sum all the tap changes.
        capSwitchCount = dbObj.sumMatrix(table=capSwitchTable,
                                         cols=capSwitchColumns, tCol=tCol,
                                         starttime=stoptime, stoptime=stoptime)
    elif (capSwitchCount is None):
        assert False, ("If capSwitchCount is not specified, capSwitchTable and"
                       " capSwitchColumns must be specified!")
        
    # Simply multiply cost by number of operations.   
    costDict['capSwitch'] = costs['capSwitch'] * capSwitchCount
    
    # *************************************************************************
    # VOLTAGE VIOLATION COSTS
    if voltFilesDir and voltFiles:
        # Get all voltage violations. Use default voltages and tolerances for
        # now.
        v = violationsFromRecorderFiles(fileDir=voltFilesDir, files=voltFiles)
        costDict['overvoltage'] = sum(v['high']) * costs['overvoltage']
        costDict['undervoltage'] = sum(v['low']) * costs['undervoltage']
    
    # *************************************************************************
    # DER COSTS
    # TODO
    
    # *************************************************************************
    # TOTAL AND RETURN
    t = 0
    for _, v in costDict.items():
        t += v
    
    costDict['total'] = t   
    return costDict

def violationsFromRecorderFiles(fileDir, files, vNom=120, vTol=6):
    """Function to read GridLAB-D group_recorder output files and compute
    voltage violations. All files are assumed to have voltages for the same
    nodes, just on different phases.
    
    NOTE: Files should have voltage magnitude only.
    """
    # Open all the files, put in list. Get csv reader
    openFiles = []
    readers = []
    for file in files:
        openFiles.append(open(fileDir + '/' + file, newline=''))
        readers.append(csv.reader(openFiles[-1]))
    
    # Read each file up to the headers.
    # Loop through the files and advance the lines.
    headers = []
    for r in readers:
        done = False
        while not done:
            line = next(r)
            # If we're not yet at the row which starts with timestamp, move on
            if (line[0].strip().startswith('#')) and \
            (not line[0].strip().startswith(util.constants.GLD_TIMESTAMP)):
                pass
            elif line[0].strip().startswith(util.constants.GLD_TIMESTAMP):
                # The row starts with '# timestamp' --> this is the header row
                headers.append(line)
                done = True
            else:
                raise ValueError('File had unexpected format!')
            
    # Ensure all headers are the same. If not, raise an error.
    sameHeaders = True
    for readerIndex in range(1, len(files)):
        sameHeaders = sameHeaders and (headers[0] == headers[readerIndex])
        
    if not sameHeaders:
        raise ValueError('Files do not have identical header rows!')
    
    # Initialize return.
    violations = {'time': [], 'low': [], 'high': []}
    # Compute low and high voltages once.
    lowV = vNom - vTol
    highV = vNom + vTol
    
    # Read all files line by line and check for voltage violations.
    while True:
        # Get the next line for each reader
        # NOTE: The code below BUILDS IN THE ASSUMPTION that the files are of
        # the same length. If one iterator declares itself done, we leave the
        # loop without checking the others. We could use recursion to address
        # this, but meh. Not worth it.
        try:
            lines = [next(r) for r in readers]
        except StopIteration:
            break
            
        # Ensure all files have the same timestamp (first element in row) for
        # the given line.
        sameTime = True
        for readerIndex in range(1, len(files)):
            sameTime = sameTime and (lines[0][0] == lines[readerIndex][0])
            
        if not sameTime:
            raise ValueError('There was a timestamp mismatch!')
        
        # Add the time to the output, start violations for this time at 0
        violations['time'].append(lines[0][0])
        violations['low'].append(0)
        violations['high'].append(0)
        
        # Loop over each element in the row. Since headers are the same, assume
        # lengths are the same. Note range starts at 1 to avoid the timestamp
        for colIndex in range(1, len(lines[0])):
            # Loop over each file, and check for voltage violations.
            # Don't double count - move on if a violation is found. This means
            # that if all phases are undervoltage, we count 1 violation. If 1
            # phase is overvoltage, we count 1 violation.
            lowFlag = False
            highFlag = False
            for line in lines:
                # If we've incremented both violations, break the loop to move
                # on to the next column (object)
                if lowFlag and highFlag:
                    break
                # Check for low voltage
                elif (not lowFlag) and  (float(line[colIndex]) < lowV):
                    violations['low'][-1] += 1
                    lowFlag = True
                elif (not highFlag) and (float(line[colIndex]) > highV):
                    violations['high'][-1] += 1
                    highFlag = True
                
    # Close all the files
    for file in openFiles:
        file.close()
        
    return violations

'''      
def voltViolationsFromDump(fName, vNom=120, vTol=6):
    """Method to read a voltdump file which is recording triplex loads/meters
    and count high and low voltage violations.
    
    NOTE: voltdump should be made with the 'polar' option!
    
    NOTE: This is hard-coded to read two-phase triplex, so phase C is not used.
    
    INPUTS:
        fName: Name of the voltdump .csv file.
        vNom: Nominal voltage magnitude (V)
        vTol: Tolerance +/- (V).
        
    OUTPUTS:
        violations: dictionary with two fields, 'low' and 'high' representing
            the total number of single phase voltage violations in the file.
    """
    # Intialize return:
    violations = {'low': 0, 'high': 0}
    # Compute low and high voltages once.
    lowV = vNom - vTol
    highV = vNom + vTol
    with open(fName, newline='') as f:
        # Advance the file one line, since the first line is metadata.
        f.readline()
        reader = csv.reader(f)
        headers = reader.__next__()
        # Note that voltdump reads phases 1 and 2 as A and B.
        # Get the row indices of the properties we care about.
        magA = headers.index('voltA_mag')
        magB = headers.index('voltB_mag')
        for row in reader:
            # Grab the voltage magnitudes for the two phases
            v = (float(row[magA]), float(row[magB]))
            # For the two phases, increment violations count if one occurs.
            if (v[0] < lowV) or (v[1] < lowV):
                violations['low'] += 1
                
            if (v[0] > highV) or (v[1] > highV):
                violations['high'] += 1
                    
    return violations

def sumVoltViolations(fileDir, files, vNom=120, vTol=6):
    """Helper method which calls voltViolationsFromDump in a loop and sums
        all violations
        
    """
    # Initialize violations count.
    violations = {'low': 0, 'high': 0}
    
    # Loop over each file.
    for f in files:
        # Call method to cound violations.
        v = voltViolationsFromDump(fName=fileDir + '/' + os.path.basename(f),
                                   vNom=vNom, vTol=vTol)
        # Add to the running total.
        violations['low'] += v['low']
        violations['high'] += v['high']
        
    return violations
'''
    
if __name__ == '__main__':
    result = runModel(modelPath=r'C:\Users\thay838\git_repos\GOSS-GridAPPS-D\applications\pyvvo\app\pmaps\output\subVTest.glm',
                      gldPath=r'C:\gridlab-d\unconstrained')
    print('yay')