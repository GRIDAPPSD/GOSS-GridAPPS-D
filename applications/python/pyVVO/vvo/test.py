'''
Created on Aug 30, 2017

@author: thay838
'''
from powerflow import writeCommands
from genetic import population
import time
import util.gld
import util.db
import util.helper
import copy
    
def main(populationInputs={}):
    # import subprocess
    # Start MySQL server (no problem if already running). This assumes MySQL
    # was used with a Windows installer and is setup as a Windows service.
    #subprocess.run("net start MySQL57")
    
    # print('The time is {}'.format(time.ctime(), flush=True))
    
    # *****************************************************************************
    # Stuff to start
    timezone= "EST+5EDT"
    tz_offset = 10800
    starttime= "2009-07-21 00:00:00"
    stoptime = "2009-07-21 00:15:00"
    tFmt = util.gld.DATE_FMT
    inPath = "C:/Users/thay838/git_repos/GOSS-GridAPPS-D/applications/python/pyVVO/test/ieee8500_base.glm"
    playerFile = "C:/Users/thay838/git_repos/GOSS-GridAPPS-D/applications/python/pyVVO/test/zipload_schedule.player"
    outDir = "C:/Users/thay838/git_repos/GOSS-GridAPPS-D/applications/python/pyVVO/test/output"
    tLoad = 'tLoad' # triplex load group.
    numInd = 80 # Best if this is a multiple of num cores.
    numGen = 10
    numIntervals = 16
    tInt = 60 * 15 # 15 minutes
    energyPrice=0.00008
    tapChangeCost=0.5
    capSwitchCost=2
    voltCost = 0.01
    # Results file
    f = open(outDir + '/results/'
             + time.strftime('%m-%d_%H-%M', time.localtime()) + '.txt', 'w')
    f.write('Number of individuals: {}\n'.format(numInd))
    f.write('Number of generations: {}\n'.format(numGen))
    f.write('Number of time intervals: {}\n'.format(numIntervals))
    f.write('Energy price: {}\n'.format(energyPrice))
    f.write('Tap change cost: {}\n'.format(tapChangeCost))
    f.write('Capacitor switching cost: {}\n'.format(capSwitchCost))
    f.write('Model is CONSTANT POWER.\n')
    f.write('*'*80 + '\n')
    # Drop all tables in the database
    cnxn = util.db.connect()
    util.db.dropAllTables(cnxn)
    cnxn.close()
    #******************************************************************************
    # Definitions of regulators and capacitors. TODO: Pull from CIM.
    reg={'reg_VREG4': {
                        'raise_taps': 16, 
                        'lower_taps': 16,
                        'phases': {'A': {'prevState': 11},
                                   'B': {'prevState': 11},
                                   'C': {'prevState': 4}
                        },
                       },
         'reg_VREG2': {
                        'raise_taps': 16,
                        'lower_taps': 16,
                        'phases': {'A': {'prevState': 9},
                                   'B': {'prevState': 6},
                                   'C': {'prevState': 1}
                                   }
                       },
         'reg_VREG3': {
                        'raise_taps': 16,
                        'lower_taps': 16,
                        'phases': {'A': {'prevState': 16},
                                   'B': {'prevState': 10},
                                   'C': {'prevState': 1}
                                   }
                       },
         'reg_FEEDER_REG': {
                        'raise_taps': 16,
                        'lower_taps': 16,
                        'phases': {'A': {'prevState': 2},
                                   'B': {'prevState': 2},
                                   'C': {'prevState': 1}
                                   }
                            },
         }
    # Get copy for benchmark to modify
    regBench = copy.deepcopy(reg)
    
    cap={
        'cap_capbank2a': {'phases': {'A': {'prevState': 'CLOSED'}}},
        'cap_capbank2b': {'phases': {'B': {'prevState': 'CLOSED'}}},
        'cap_capbank2c': {'phases': {'C': {'prevState': 'CLOSED'}}},
        'cap_capbank1a': {'phases': {'A': {'prevState': 'CLOSED'}}},
        'cap_capbank1b': {'phases': {'B': {'prevState': 'CLOSED'}}},
        'cap_capbank1c': {'phases': {'C': {'prevState': 'CLOSED'}}},
        'cap_capbank0a': {'phases': {'A': {'prevState': 'CLOSED'}}},
        'cap_capbank0b': {'phases': {'B': {'prevState': 'CLOSED'}}},
        'cap_capbank0c': {'phases': {'C': {'prevState': 'CLOSED'}}},
        # cap_capbank3 appears to be not controllable.
        #'cap_capbank3': ['switchA', 'switchB', 'switchC']
    }
    # Get copy for benchmark to modify
    capBench = copy.deepcopy(cap)
    #******************************************************************************
    # Modify model. NOTE: Things will be in reverse order intentionally.
    # Read the base model as a string.
    with open(inPath, 'r') as f1:
        strModel = f1.read()
    
    # Get filename of benchmark file
    benchFile = writeCommands.writeCommands.addFileSuffix(inPath=inPath,
                                                          suffix='benchmark',
                                                          outDir=outDir)
    # Get a writeCommands object for the benchmark
    writeBench = writeCommands.writeCommands(strModel = strModel,
                                           pathModelOut=benchFile)

    # Setup the model
    writeBench.setupModel(vSource=69715.065,
                          playerFile=playerFile, tz_offset=tz_offset)
    
    # Make a copy of the writeCommands object for use in the genetic algorithm.
    # At time of writing, the writeCommands object only has string properties,
    # so no need for a deepcopy
    writeGenetic = copy.copy(writeBench)
    #******************************************************************************
    # Benchmark model:
    # Switch control strategy to commented version.
    writeBench.switchControl()
    # Record the swing node.
    swingDat = writeBench.recordSwing(suffix='benchmark')
    # Record capacitor switching.
    capTable = 'capCount_benchmark'
    # In order to get capacitor table to initialize correctly, we need to 
    # have all capacitors record all phases even if they aren't connected.
    capTableChangeCols = ['cap_A_switch_count', 'cap_B_switch_count',
                          'cap_C_switch_count']
    capTableStatusCols = ['switchA', 'switchB', 'switchC']
    allCapCols = []
    allCapCols.extend(capTableChangeCols)
    allCapCols.extend(capTableStatusCols)
    for c, p in capBench.items():
        # Create recorder for this capacitor
        writeBench.addMySQLRecorder(parent = c, table=capTable,
                                    properties=allCapCols, interval=tInt)
    
    # Record regulator tap changing
    regTable = 'regCount_benchmark'
    # In order to have capacitor table initialize correctly, we need to 
    # have all regulators record all phases even if they aren't connected.
    regTableChangeCols = ['tap_A_change_count', 'tap_B_change_count',
                          'tap_C_change_count']
    regTableStatusCols = ['tap_A', 'tap_B', 'tap_C']
    allRegCols = []
    allRegCols.extend(regTableChangeCols)
    allRegCols.extend(regTableStatusCols)
    for r, ph in regBench.items():        
        # Create recorder for this regulator.    
        writeBench.addMySQLRecorder(parent=r, table=regTable,
                                    properties=allRegCols, interval=tInt)
        
    # Add a group to the triplex loads
    writeBench.addGroupToObjects(objectRegex=writeCommands.TRIPLEX_LOAD_REGEX,
                                groupName=tLoad)
    # TODO: uncomment when ready, maybe
    '''
    # Record the triplex voltages.
    triplexDat = writeBench.recordTriplex(suffix='benchmark')
    '''
    '''
    # Add triplex_meters.
    writeBench.addTriplexMeters()
    # Add metrics collector writer.
    writeBench.addMetricsCollectorWriter(interval=60, suffix='benchmark')
    # Add metric collectors to all triplex_meters
    writeBench.addMetricsCollectors()
    '''
    # Loop over the number of time intervals
    benchTotal = 0
    geneticTotal = 0 
    individualsList = []
    nextUID=0
    for t in range(numIntervals): 
        # Write to file
        print('*'*80, file=f, flush=True)
        print('*'*80, file=f, flush=True)
        #print('Running for {} through {}'.format(starttime, stoptime),
        #      flush=True)
        print('Running for {} through {}'.format(starttime, stoptime),
              file=f, flush=True)
        # Command the benchmark regulators and capacitors
        writeBench.commandCapacitors(cap=capBench)
        writeBench.commandRegulators(reg=regBench)
        # Update clocks.
        writeBench.updateClock(starttime=starttime, stoptime=stoptime,
                               timezone=timezone)
        writeGenetic.updateClock(stoptime=stoptime, starttime=starttime,
                                 timezone=timezone)
        
        # Add voltdumps to the benchmark model. Record each minute.
        benchVoltFiles = writeBench.addVoltDumps(starttime=starttime, 
                                                stoptime=stoptime,
                                                group=tLoad, fileDir=outDir,
                                                baseFile='dump.csv',
                                                suffix='benchmark', interval=60)
        
        # Write benchmark model
        writeBench.writeModel()
        # Run the model
        t0 = time.time()
        result = util.gld.runModel(writeBench.pathModelOut)
        t1 = time.time()
        print('Benchmark model run in {:.2f} seconds.'.format(t1-t0), file=f,
              flush=True)
        
        # Evaluate model.
        cnxn = util.db.connect()
        cursor = cnxn.cursor()
        benchScores = util.gld.computeCosts(cursor=cursor,
                                          powerTable=swingDat['table'],
                                          powerColumns=swingDat['columns'],
                                          powerInterval=swingDat['interval'],
                                          energyPrice=energyPrice,
                                          starttime=starttime, stoptime=stoptime,
                                          tapChangeCost=tapChangeCost,
                                          capSwitchCost=capSwitchCost,
                                          tapChangeTable=regTable,
                                          tapChangeColumns=regTableChangeCols,
                                          capSwitchTable=capTable,
                                          capSwitchColumns=capTableChangeCols,
                                          voltCost=voltCost,
                                          voltdumpDir=outDir,
                                          voltdumpFiles=benchVoltFiles
                                          )
        # Write result to file.
        print('Benchmark scores:', file = f, flush=True)
        print('  Total: {:.4g}'.format(benchScores['total']), file=f,
              flush=True)
        print('  Energy: {:.4g}'.format(benchScores['energy']), file=f,
              flush=True)
        print('  Tap: {:.4g}'.format(benchScores['tap']), file=f,
              flush=True)
        print('  Cap: {:.4g}'.format(benchScores['cap']), file=f,
              flush=True)
        print('  Undervoltage: {:.4g}'.format(benchScores['undervoltage']),
              file=f, flush=True)
        print('  Overvoltage: {:.4g}'.format(benchScores['overvoltage']),
              file=f, flush=True)
        # TODO: Uncomment
        '''
        print('Benchmark violations: ')
        print('    High: {}'.format(violations['high']))
        print('    Low: {}'.format(violations['low']))
        '''
        # Increment benchmark grand total
        benchTotal += benchScores['total']
        #**********************************************************************
        # Set bench settings for next iteration.
        regBench = util.db.updateStatus(inDict=regBench, dictType='reg',
                                   cursor=cursor, table=regTable,
                                   phaseCols=regTableStatusCols, t=stoptime,
                                   nameCol='name', tCol='t')
        capBench = util.db.updateStatus(inDict=capBench, dictType='cap',
                           cursor=cursor, table=capTable,
                           phaseCols=capTableStatusCols, t=stoptime,
                           nameCol='name', tCol='t')
        
        # Rotate the 'newState' to 'oldState'
        regBench, capBench = util.helper.rotateVVODicts(reg=regBench, cap=capBench,
                                                   deleteFlag=True)
        # Close database cursor and connection.
        cursor.close()
        cnxn.close()
        #******************************************************************************
        # Run genetic algorithm.
        # print('Initializing population...', flush=True)
        print('*' * 80, file=f)
        t0 = time.time()
        popObj = population.population(starttime=starttime, stoptime=stoptime,
                                       inPath=inPath,
                                       strModel=writeGenetic.strModel,
                                       numInd=numInd, numGen=numGen,
                                       reg=reg,
                                       cap=cap,
                                       outDir=outDir,
                                       energyPrice=energyPrice,
                                       tapChangeCost=tapChangeCost,
                                       capSwitchCost=capSwitchCost,
                                       individualsList=individualsList,
                                       nextUID=nextUID,
                                       **populationInputs
                                      )
        # print('Beginning genetic algorithm...', flush=True)
        bestIndividual = popObj.ga()
        # Pull the first 10% of individuals
        individualsList=popObj.individualsList[0:round(len(popObj.individualsList)*0.1)]
        nextUID = popObj.nextUID
        # print('Genetic algorithm complete.',flush=True)
        # Update 'reg' and 'cap' based on the most fit individual.
        reg = copy.deepcopy(bestIndividual.reg)
        cap = copy.deepcopy(bestIndividual.cap)
        reg, cap = util.helper.rotateVVODicts(reg=reg, cap=cap, deleteFlag=True)
        # print('Printing results to file...', flush=True)
        # print('The time is {}'.format(time.ctime(), flush=True))
        t1 = time.time()
        print('{} individuals per {} generations'.format(numInd, numGen),
              file=f, flush=True)
        print('Runtime: {:.0f} s'.format(t1-t0), file=f, flush=True)
        print('Scores: ', file=f,flush=True)
        for s in popObj.generationBest:
            print('{:.4g}'.format(s), end=', ', file=f, flush=True)
        # Increment genetic total
        geneticTotal += popObj.generationBest[-1]
        print(file=f)
        print('Best Individual:', file=f, flush=True)
        print(bestIndividual, file=f, flush=True)
        print('*' * 80, file=f, flush=True)
        
        # Increment the time
        # TODO: Daylight savings problems?
        # TODO: We're running an extra minute of simulation each run.
        starttime = util.helper.incrementTime(t=starttime, fmt=tFmt,
                                              interval=tInt)
        stoptime = util.helper.incrementTime(t=stoptime, fmt=tFmt,
                                              interval=tInt)
          
    print('*'*80, file=f, flush=True)
    print('Benchmark grand total: {:.5g}'.format(benchTotal), file=f)
    print('Genetic grand total: {:.5g}'.format(geneticTotal), file=f)  
    print('Results printed to file. All done.', flush=True)
    
    return bestIndividual.fitness
    
if __name__ == '__main__':
    main()