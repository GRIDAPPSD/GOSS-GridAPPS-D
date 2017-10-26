'''
Created on Aug 30, 2017

@author: thay838
'''
from powerflow import writeCommands
from genetic import population
from genetic import individual
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
    tFmt = util.gld.DATE_FMT
    tRec = 60 # recording interval
    tInt = 60 * 15 # model runtime
    stoptime = util.helper.incrementTime(t=starttime, fmt=tFmt,
                                         interval=tInt)
    inPath = "C:/Users/thay838/git_repos/GOSS-GridAPPS-D/applications/pyvvo/tests/ieee8500_base.glm"
    playerFile = "C:/Users/thay838/git_repos/GOSS-GridAPPS-D/applications/pyvvo/tests/zipload_schedule.player"
    outDir = "C:/Users/thay838/git_repos/GOSS-GridAPPS-D/applications/pyvvo/tests/output"
    tLoad = 'tLoad' # triplex load group.
    numInd = 16 # Best if this is a multiple of num cores
    numGen = 3
    numIntervals = 2
    costs = {'energy': 0.00008, 'tapChange': 0.5, 'capSwitch': 2, 'volt': 0.05}
    # Results file
    f = open(outDir + '/results/'
             + time.strftime('%m-%d_%H-%M', time.localtime()) + '.txt', 'w')
    f.write('Number of individuals: {}\n'.format(numInd))
    f.write('Number of generations: {}\n'.format(numGen))
    f.write('Number of time intervals: {}\n'.format(numIntervals))
    f.write('Energy price: {}\n'.format(costs['energy']))
    f.write('Tap change cost: {}\n'.format(costs['tapChange']))
    f.write('Capacitor switching cost: {}\n'.format(costs['capSwitch']))
    f.write('Voltage violation cost: {}\n'.format(costs['volt']))
    f.write('Model is CONSTANT CURRENT.\n')
    f.write('*'*80 + '\n')
    # Define database inputs
    database = {'tz_offset': tz_offset}
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
    #******************************************************************************
    # Modify model. NOTE: Things will be in reverse order intentionally.
    # Read the base model as a string.
    with open(inPath, 'r') as f1:
        strModel = f1.read()

    # Get a writeCommands object for the benchmark
    writeObj = writeCommands.writeCommands(strModel = strModel)

    # Setup the model
    dumpFiles = writeObj.setupModel(vSource=69715.065, playerFile=playerFile, 
                                      database=database, triplexGroup=tLoad,
                                      voltdump={'num': round(tInt/tRec) + 1,
                                                'group': tLoad},
                                      powerflowFlag=True
                                      )
    
    #******************************************************************************
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
    # Update write objects.
    updateModel(writeObj=writeObj, starttime=starttime, stoptime=stoptime,
                timezone=timezone, tRec=tRec)
    # Initialize the population. Include an output voltage baseline individual.
    popObj = population.population(starttime=starttime, stoptime=stoptime,
                                   inPath=inPath,
                                   strModel=writeObj.strModel,
                                   numInd=numInd, numGen=numGen,
                                   reg=reg,
                                   cap=cap,
                                   outDir=outDir,
                                   costs=costs,
                                   voltdumpFiles=dumpFiles,
                                   baseControlFlag=2,
                                   **populationInputs
                                  )
        
    # Loop over the number of time intervals
    baselineTotal = 0
    geneticTotal = 0 
    for _ in range(numIntervals): 
        # Write to file
        print('*'*80, file=f, flush=True)
        print('*'*80, file=f, flush=True)
        #print('Running for {} through {}'.format(starttime, stoptime),
        #      flush=True)
        print('Running for {} through {}'.format(starttime, stoptime),
              file=f, flush=True)

        #******************************************************************************
        # Run genetic algorithm.
        # print('Initializing population...', flush=True)
        print('*' * 80, file=f)
        t0 = time.time()
        # print('Beginning genetic algorithm...', flush=True)
        bestIndividual = popObj.ga()
        t1 = time.time()
        # Write baseline information to file and increment score
        print('Baseline score:', file=f,flush=True)
        print(popObj.baselineData['str'], file=f, flush=True)
        # increment baseline total
        baselineTotal += popObj.baselineData['costs']['total']
        # Update 'reg' and 'cap' based on the most fit individual.
        reg = copy.deepcopy(bestIndividual.reg)
        cap = copy.deepcopy(bestIndividual.cap)
        reg, cap = util.helper.rotateVVODicts(reg=reg, cap=cap, deleteFlag=True)
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
        
        # Update the write object
        updateModel(writeObj=writeObj, starttime=starttime,
                    stoptime=stoptime,
                    timezone=timezone, tRec=tRec)
        
        print('Time updated, moving to next timestep.', flush=True)
        # Prep the population for the next run.
        popObj.prep(starttime=starttime, stoptime=stoptime,
                    strModel=writeObj.strModel, cap=cap, reg=reg,
                    keep=0.1)
          
    # Shut down population object threads
    popObj.stopThreads()
    print('*'*80, file=f, flush=True)
    print('Benchmark grand total: {:.5g}'.format(baselineTotal), file=f)
    print('Genetic grand total: {:.5g}'.format(geneticTotal), file=f)  
    print('Results printed to file. All done.', flush=True)
    f.close()

def updateModel(writeObj, starttime, stoptime, timezone, tRec):
        # Update clocks.
        writeObj.updateClock(starttime=starttime, stoptime=stoptime,
                               timezone=timezone)
        
        # Add runtimes to the voltage dumps.
        writeObj.addRuntimeToVoltDumps(starttime=starttime, stoptime=stoptime,
                                       interval=tRec)
if __name__ == '__main__':
    main()