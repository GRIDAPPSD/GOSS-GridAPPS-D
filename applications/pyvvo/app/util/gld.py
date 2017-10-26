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
import util.db
import csv
import os

# GridLAB-D should get dates in this format.
DATE_FMT = "%Y-%m-%d %H:%M:%S"

# definitions for regulator and capacitor properties
REG_CHANGE_PROPS = ['tap_A_change_count', 'tap_B_change_count',
                    'tap_C_change_count']
REG_STATE_PROPS = ['tap_A', 'tap_B', 'tap_C']
CAP_CHANGE_PROPS = ['cap_A_switch_count', 'cap_B_switch_count',
                    'cap_C_switch_count']
CAP_STATE_PROPS = ['switchA', 'switchB', 'switchC']

def runModel(modelPath, gldPath=r'C:/gridlabd/develop/install64'):
    """Function to run GridLAB-D model.
    
    TODO: Add support for command line inputs.
    """
    cwd, model = os.path.split(modelPath)
    # Set up the gridlabd path, run model
    # TODO: Get this adapated for Unix.
    # NOTE: On Windows, spaces between the '&&'s causes everything to break...
    cmd = r'set PATH={}/bin;%PATH%'.format(gldPath)
    cmd += r'&&set GLPATH={0}/share/gridlabd;{0}/lib/gridlabd'.format(gldPath)
    cmd += r'&&set CXXFLAGS=-I{}/share/gridlabd'.format(gldPath)
    cmd += r'&&gridlabd'
    cmd += r' ' + model
    # Run command. Note with check=True exception will be thrown on failure.
    # TODO: rather than using check=True, handle the event of a GridLAB-D error
    output = subprocess.run(cmd, stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE, shell=True, 
                            cwd=cwd)#, check=True)
    return output

def translateTaps(lowerTaps, pos):
    """Method to translate tap integer in range 
    [0, lowerTaps + raiseTaps - 1] to range [-(lower_taps - 1), raise_taps]
    """
    # TODO: unit test
    # Hmmm... is it this simple? 
    posOut = pos - lowerTaps + 1
    return posOut

def inverseTranslateTaps(lowerTaps, pos):
    """Method to translate tap integer in range
        [-(lower_taps - 1), raise_taps] to range [0, lowerTaps + raiseTaps - 1]
    """
    # TODO: unit test
    # Hmmm... is it this simle? 
    posOut = pos + lowerTaps - 1
    return posOut

def computeCosts(cursor, swingData, costs, starttime, stoptime, voltdumpDir,
                 voltdumpFiles, tCol='t', tapChangeCount=None,
                 tapChangeTable=None, tapChangeColumns=None,
                 capSwitchCount=None, capSwitchTable=None,
                 capSwitchColumns=None
                 ):
    """Method to compute VVO costs for a given time interval. This includes
    cost of energy, capacitor switching, and regulator tap changing. Later this
    should include cost of DER engagement.
    
    INPUTS:
        cursor: database connection cursor
        swingData: dict with the following fields:
            table: name of table for getting swing node power.
            columns: name of the columns corresponding to the swing table.
                These will vary depending on node-type (substation vs. meter)
            interval: recording interval (seconds) of the swing table
        costs: dictionary with the following fields:
            energy: price of energy in $/VAh
            tapChange: cost ($) of changing one regulator tap one position.
            capSwitch: cost ($) of switching a single capacitor
            volt: cost of a voltage violation
        starttime: starting timestamp (yyyy-mm-dd HH:MM:SS) of interval in
            question.
        stoptime: stopping ""
        voltdumpDir: Directory of voltdump files
        voltdumpFiles: list of voltdump files (without the path 'head')
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
    # *************************************************************************
    # ENERGY COST
    
    # Sum the total three-phase complex power over the given interval.
    powerSum = util.db.sumComplexPower(cursor=cursor, table=swingData['table'], 
                                  cols=swingData['columns'],
                                  starttime=starttime, stoptime=stoptime)
    
    # Perform 'integration' by multiplying each measurement by its time
    # duration (convert seconds to hours), then multiply this area (energy in 
    # VAh) by price to get cost.
    energyCost = ((powerSum['sum'].__abs__() * (swingData['interval'] / 3600))
                  * costs['energy'])
    
    # *************************************************************************
    # TAP CHANGING COST
    
    # If tap table and columns given, compute the total count.    
    if (tapChangeTable and tapChangeColumns and tCol):
        # Sum all the tap changes.
        tapChangeCount = util.db.sumMatrix(cursor=cursor, table=tapChangeTable,
                                      cols=tapChangeColumns, tCol=tCol,
                                      starttime=stoptime, stoptime=stoptime)
    elif (tapChangeCount is None):
        assert False, ("If tapChangeCount is not specified, tapChangeTable and"
                       " tapChangeColumns must be specified!")
        
    # Simply multiply cost by number of operations.   
    tapCost = costs['tapChange'] * tapChangeCount
    
    # *************************************************************************
    # CAP SWITCHING COST
    
    # If cap table and columns given, compute the total count.
    if (capSwitchTable and capSwitchColumns and tCol):
        # Sum all the tap changes.
        capSwitchCount = util.db.sumMatrix(cursor=cursor, table=capSwitchTable,
                                      cols=capSwitchColumns, tCol=tCol,
                                      starttime=stoptime, stoptime=stoptime)
    elif (capSwitchCount is None):
        assert False, ("If capSwitchCount is not specified, capSwitchTable and"
                       " capSwitchColumns must be specified!")
        
    # Simply multiply cost by number of operations.   
    capCost = costs['capSwitch'] * capSwitchCount
    
    # *************************************************************************
    # VOLTAGE VIOLATION COSTS
    
    # Get all voltage violations. Use default voltages and tolerances for now.
    v = sumVoltViolations(fileDir=voltdumpDir, files=voltdumpFiles)
    overvoltage = v['high'] * costs['volt']
    undervoltage = v['low'] * costs['volt']
    
    # TODO: uncomment when ready
    '''
    # Get the voltage violations
    self.violations = util.db.voltageViolations(cursor=cursor,
                                      table=self.triplexTable, 
                                      starttime=starttime,
                                      stoptime=stoptime)
    '''
    # *************************************************************************
    # DER COSTS
    # TODO
    
    # *************************************************************************
    # RETURN
    return {'total': energyCost + tapCost + capCost + overvoltage
            + undervoltage, 'energy': energyCost, 
            'tap': tapCost, 'cap': capCost, 'overvoltage': overvoltage,
            'undervoltage': undervoltage}
    
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
        v = voltViolationsFromDump(fName=fileDir + '/' + f, vNom=vNom,
                                   vTol=vTol)
        # Add to the running total.
        violations['low'] += v['low']
        violations['high'] += v['high']
        
    return violations
    
if __name__ == '__main__':
    """
    f = r'C:/Users/thay838/git_repos/GOSS-GridAPPS-D/applications/python/pyVVO/test/output/dump1.csv'
    # voltViolationsFromDump(fName=f)
    t = sumVoltViolations(fileDir=r'C:/Users/thay838/git_repos/GOSS-GridAPPS-D/applications/python/pyVVO/test/output',
                          baseName='dump')
    print(t)
    """
    """
    # Hack this to print the version
    output = runModel('--version')
    print('OUTPUT:')
    print(output.stdout.decode('utf-8'))
    print('ERROR:')
    print(output.stderr.decode('utf-8'))
    """
    output = runModel(modelPath='C:/Users/thay838/git_repos/GOSS-GridAPPS-D/applications/pyvvo/tests/output/ieee8500_base_benchmark.glm')
    print(output.stdout.decode('utf-8'))
    print(output.stderr.decode('utf-8'))
    print('yay')