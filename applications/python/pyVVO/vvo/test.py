'''
Created on Aug 30, 2017

@author: thay838
'''

if __name__ == '__main__':
    from powerflow import writeCommands
    from genetic import population
    import time
    from util import db
    from util import gld
    import copy
    # import subprocess
    # Start MySQL server (no problem if already running). This assumes MySQL
    # was used with a Windows installer and is setup as a Windows service.
    #subprocess.run("net start MySQL57")
    print('The time is {}'.format(time.ctime(), flush=True))
    # *****************************************************************************
    # Stuff to start
    timezone= "EST+5EDT"
    tz_offset = 10800
    starttime= "2009-07-21 00:00:00"
    stoptime = "2009-07-21 00:15:00"
    tFmt = "%Y-%m-%d %H:%M:%S"
    inPath = "C:/Users/thay838/git_repos/GOSS-GridAPPS-D/applications/python/pyVVO/test/ieee8500_base.glm"
    playerFile = "C:/Users/thay838/git_repos/GOSS-GridAPPS-D/applications/python/pyVVO/test/zipload_schedule.player"
    outDir = "C:/Users/thay838/git_repos/GOSS-GridAPPS-D/applications/python/pyVVO/test/output"
    numInd = 40 # Best if this is a multiple of num cores.
    numGen = 3
    numIntervals = 4
    tInt = 60 * 15 # 15 minutes
    energyPrice=0.00008
    tapChangeCost=0.5
    capSwitchCost=2
    # Results file
    f = open(outDir + '/output.txt', 'w')
    # Drop all tables in the database
    cnxn = db.connect()
    db.dropAllTables(cnxn)
    cnxn.close()
    #******************************************************************************
    # Definitions of regulators and capacitors. TODO: Pull from CIM.
    reg={'reg_VREG4': {
                        'raise_taps': 16, 
                        'lower_taps': 16,
                        'taps': {
                            'tap_A': 11,
                            'tap_B': 11,
                            'tap_C': 4
                        },
                       },
         'reg_VREG2': {
                        'raise_taps': 16,
                        'lower_taps': 16,
                        'taps': {
                            'tap_A': 9,
                            'tap_B': 6,
                            'tap_C': 1
                        }
                       },
         'reg_VREG3': {
                        'raise_taps': 16,
                        'lower_taps': 16,
                        'taps': {
                            'tap_A': 16,
                            'tap_B': 10,
                            'tap_C': 1
                        }
                       },
         'reg_FEEDER_REG': {
                        'raise_taps': 16,
                        'lower_taps': 16,
                        'taps': {
                            'tap_A': 2,
                            'tap_B': 2,
                            'tap_C': 1
                        }
                       },
         }
    
    cap={
        'cap_capbank2a': {'switchA': 'CLOSED'},
        'cap_capbank2b': {'switchB': 'CLOSED'},
        'cap_capbank2c': {'switchC': 'CLOSED'},
        'cap_capbank1a': {'switchA': 'CLOSED'},
        'cap_capbank1b': {'switchB': 'CLOSED'},
        'cap_capbank1c': {'switchC': 'CLOSED'},
        'cap_capbank0a': {'switchA': 'CLOSED'},
        'cap_capbank0b': {'switchB': 'CLOSED'},
        'cap_capbank0c': {'switchC': 'CLOSED'},
        # cap_capbank3 appears to be not controllable.
        #'cap_capbank3': ['switchA', 'switchB', 'switchC']
    }
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
    writeBench.setupModel(timezone=timezone, vSource=69715.065,
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
    for c, p in cap.items():
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
    for r, ph in reg.items():        
        # Create recorder for this regulator.    
        writeBench.addMySQLRecorder(parent=r, table=regTable,
                                    properties=allRegCols, interval=tInt)
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
    for t in range(numIntervals): 
        # Write to file
        print('*'*80, file=f, flush=True)
        print('*'*80, file=f, flush=True)
        print('Running for {} through {}'.format(starttime, stoptime),
              flush=True)
        print('Running for {} through {}'.format(starttime, stoptime),
              file=f, flush=True)
        # Update clocks.
        writeBench.updateClock(starttime=starttime, stoptime=stoptime)
        writeGenetic.updateClock(stoptime=stoptime, starttime=starttime)
        # Write benchmark model
        writeBench.writeModel()
        # Run the model
        t0 = time.time()
        result = gld.runModel(writeBench.pathModelOut)
        t1 = time.time()
        print('Benchmark model run in {:.2f} seconds.'.format(t1-t0), file=f,
              flush=True)
        
        # Evaluate model.
        cnxn = db.connect()
        cursor = cnxn.cursor()
        benchScores = gld.computeCosts(cursor=cursor,
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
                                      capSwitchColumns=capTableChangeCols)

        # Close database cursor and connection.
        cursor.close()
        cnxn.close()
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
        print('')
        # TODO: Uncomment
        '''
        print('Benchmark violations: ')
        print('    High: {}'.format(violations['high']))
        print('    Low: {}'.format(violations['low']))
        '''
        # Increment benchmark grand total
        benchTotal += benchScores['total']
        #******************************************************************************
        # Run genetic algorithm.
        print('Beginning genetic algorithm...', flush=True)
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
                                       capSwitchCost=capSwitchCost
                                      )
        popObj.ga()
        print('Genetic algorithm complete, printing results to file...',
              flush=True)
        print('The time is {}'.format(time.ctime(), flush=True))
        t1 = time.time()
        print('{} individuals per {} generations'.format(numInd, numGen),
              file=f)
        print('Runtime: {:.0f} s'.format(t1-t0), file=f)
        print('Scores: ', file=f)
        for s in popObj.generationBest:
            print('{:.4g}'.format(s), end=', ', file=f)
        # Increment genetic total
        geneticTotal += popObj.generationBest[-1]
        print(file=f)
        print('Best Individual:', file=f)
        bestUID = popObj.indFitness[0][population.UIDIND]
        for ix in popObj.indList:
            if ix.uid == bestUID:
                print('Scores: ', file=f, flush=True)
                print('  Total: {:.4g}'.format(ix.fitness), file=f,
                      flush=True)
                print('  Energy: {:.4g}'.format(ix.energyCost), file=f,
                      flush=True)
                print('  Reg: {:.4g}'.format(ix.tapCost), file=f,
                      flush=True)
                print('  Cap: {:.4g}'.format(ix.capCost), file=f,
                      flush=True)
                print('\tCapacitor settings:', file=f)
                for capName, capDict in ix.cap.items():
                    print('\t\t' + capName + ':', file=f)
                    for switchName, switchDict in capDict.items():
                        print('\t\t\t' + switchName + ': ' 
                              + switchDict['status'], file=f)
                print(file=f)
                
                print('\tRegulator settings:', file=f)
                for regName, regDict in ix.reg.items():
                    print('\t\t' + regName + ':', file=f)
                    for tapName, tapDict in regDict['taps'].items():
                        print('\t\t\t' + tapName + ': ' + str(tapDict['pos']),
                              file=f)
                    
                break
            else:
                pass
            
        print('*' * 80, file=f, flush=True)
        
        # Increment the time
        # TODO: Daylight savings problems?
        # TODO: We're running an extra minute of simulation each run.
        ts = time.mktime(time.strptime(starttime, tFmt)) + tInt
        te = time.mktime(time.strptime(stoptime, tFmt)) + tInt
        starttime = time.strftime(tFmt, time.localtime(ts))
        stoptime = time.strftime(tFmt, time.localtime(te))
          
    print('*'*80, file=f, flush=True)
    print('Benchmark grand total: {:.5g}'.format(benchTotal), file=f)
    print('Genetic grand total: {:.5g}'.format(geneticTotal), file=f)  
    print('Results printed to file. All done.', flush=True)