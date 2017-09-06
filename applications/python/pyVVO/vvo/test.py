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
    starttime= "2009-07-21 15:00:00"
    stoptime = "2009-07-21 15:15:00"
    tFmt = "%Y-%m-%d %H:%M:%S"
    inPath = "C:/Users/thay838/git_repos/GOSS-GridAPPS-D/applications/python/pyVVO/test/ieee8500_base.glm"
    playerFile = "C:/Users/thay838/git_repos/GOSS-GridAPPS-D/applications/python/pyVVO/test/zipload_schedule.player"
    outDir = "C:/Users/thay838/Desktop/vvo/test_8500"
    numInd = 16 # Best if this is a multiple of num cores.
    numGen = 2
    numIntervals = 3
    tInt = 60 * 15 # 15 minutes
    # Results file
    f = open(outDir + '/output.txt', 'w')
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
    # Write model
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
        '''
        t0 = time.time()
        result = gld.runModel(writeBench.pathModelOut)
        t1 = time.time()
        print('Benchmark model run in {:.2f} seconds.'.format(t1-t0), file=f,\
              flush=True)
        
        # Evaluate model.
        cnxn = db.connect()
        cursor = cnxn.cursor()
        t, _ = db.sumComplexPower(cursor=cursor, cols=swingDat['swingColumns'],
                                  table=swingDat['table'], starttime=starttime,
                                  stoptime=stoptime)
        # Close database cursor and connection.
        cursor.close()
        cnxn.close()
        # Write result to file.
        print('Benchmark score: {:.4g}'.format(t.__abs__()), file=f, flush=True)
        # Increment benchmark grand total
        benchTotal += t.__abs__()
        '''
        #******************************************************************************
        # Run genetic algorithm.
        print('Beginning genetic algorithm...', flush=True)
        print('*' * 80, file=f)
        t0 = time.time()
        popObj = population.population(starttime=starttime, stoptime=stoptime,
                                       inPath=inPath,
                                       strModel=writeGenetic.strModel,
                                       numInd=numInd, numGen=numGen,
                            reg={'reg_VREG4': {
                                                'raise_taps': 16, 
                                                'lower_taps': 16,
                                                'taps': [
                                                    'tap_A',
                                                    'tap_B',
                                                    'tap_C'
                                                ]
                                               },
                                 'reg_VREG2': {
                                                'raise_taps': 16,
                                                'lower_taps': 16,
                                                'taps': [
                                                    'tap_A',
                                                    'tap_B',
                                                    'tap_C'
                                                ]
                                               },
                                 'reg_VREG3': {
                                                'raise_taps': 16,
                                                'lower_taps': 16,
                                                'taps': [
                                                    'tap_A',
                                                    'tap_B',
                                                    'tap_C'
                                                ]
                                               },
                                 'reg_FEEDER_REG': {
                                                'raise_taps': 16,
                                                'lower_taps': 16,
                                                'taps': [
                                                    'tap_A',
                                                    'tap_B',
                                                    'tap_C'
                                                ]
                                               },
                            },
                            cap={
                                'cap_capbank2a': ['switchA'],
                                'cap_capbank2b': ['switchB'],
                                'cap_capbank2c': ['switchC'],
                                'cap_capbank1a': ['switchA'],
                                'cap_capbank1b': ['switchB'],
                                'cap_capbank1c': ['switchC'],
                                'cap_capbank0a': ['switchA'],
                                'cap_capbank0b': ['switchB'],
                                'cap_capbank0c': ['switchC'],
                                'cap_capbank3': ['switchA', 'switchB', 'switchC']
                            },
                            outDir=outDir
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