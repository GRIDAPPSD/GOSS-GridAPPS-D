'''
Created on Oct 24, 2017

@author: thay838
'''
from glm import modGLM
import re
from pmaps import constants as CONST
import os
import shutil
import util.helper
import util.db
import util.gld
from genetic import individual
import time
from genetic import population
import threading
from queue import Queue
import csv

# Paths to input/output models.
partial =  CONST.BASE_PATH + '/' + CONST.MODEL
MODEL_POPULATED = partial + r'_populated.glm'
MODEL_AMI = partial + r'_AMI.glm'
MODEL_STRIPPED = partial + r'_stripped.glm'
MODEL_STRIPPED_VVO = partial + r'_stripped_vvo.glm'
MODEL_MANUAL = partial + r'_manual.glm'
MODEL_VVO = partial + r'_vvo.glm'
MODEL_BASELINE_2 = partial + r'_baseline_2.glm'
MODEL_BASELINE_3 = partial + r'_baseline_3.glm'
MODEL_ZIP = partial + r'_ZIP.glm'
MODEL_STRIPPED_DUMP = partial + r'_stripped_dump.glm'

def populatedToAMI(interval=900, group=CONST.TRIPLEX_GROUP):
    """Function to take the full populated GridLAB-D model, and get it ready to
    run in order to generate all the data we need for load modeling.
    
    In essence, this function will find every house, extract its parent, then
    if the parent is a triplex_node, give it a group definition and
    AMI_averaging_interval.
    
    After this function, the model should be ready to run.
    """
    
    # Get a modGLM object
    obj = modGLM.modGLM(pathModelIn=MODEL_POPULATED,
                                      pathModelOut=MODEL_AMI)
    
    # Define the properties we want to modify for triplex_meters
    propDict = {'AMI_averaging_interval': interval, 'groupid': group}
    
    # For shorthand, get house regex
    houseExp = modGLM.HOUSE_REGEX
    
    # Find the first house in the model
    m = houseExp.search(obj.strModel)
    
    # Object counts for sanity check
    houseCount = 0
    
    # Keep track of the triplex meters.
    triplexList = []
    
    # Loop over all houses
    while m:
        # Increment counter
        houseCount += 1
        # Extract the house object
        house = obj.extractObject(objMatch=m)
        # Extract the parent of the house
        n = obj.extractProperties(house['obj'], ['parent'])
        name = n['parent']['prop']
        # Find the triplex_meter parent. NOTE: exception raised if the parent
        # isn't a triplex meter.
        if name not in triplexList:
            triplexList.append(name)
            parent = obj.extractObjectByNameAndType(name=name,
                                                    objRegEx=modGLM.TRIPLEX_METER_REGEX)
            
            # Modify the parent object and splice it in.
            parent['obj'] = obj.modObjProps(objStr=parent['obj'],
                                            propDict=propDict)
            obj.replaceObject(parent)
            
            # Determine length change of object
            delta = len(parent['obj']) - (parent['end'] - parent['start'])
            
            # The length of the parent object has changed, so we can't simply
            # rely on house['end']. Figure out the search index.
            if parent['start'] > house['end']:
                # Parent comes after house. house['end'] is still trustworthy.
                searchIndex = house['end'] 
            else:
                # Parent comes before house. Need to add delta to ending index
                # of the house.
                searchIndex = house['end'] + delta
        else:
            searchIndex = house['end']
            
        # Find the next house
        m = houseExp.search(obj.strModel, searchIndex)

    print('{} houses found, and {} triplex_meters updated.'.format(houseCount,
                                                                   len(triplexList)))
    
    # Update the clock
    obj.updateClock(starttime=CONST.STARTTIME, stoptime=CONST.STOPTIME,
                    timezone=CONST.TIMEZONE)
    print('Clock updated.')
    
    # Update powerflow (add KLU solver)
    obj.updatePowerflow(line_capacitance='FALSE')
    print('Powerflow module updated.')
    
    # Remove recorders and collectors
    # TODO - if we want statistics about the model, we'll want this information
    obj.removeObjectsByType(typeList=['collector', 'recorder'])
    print('collectors and recorders removed.')
    # The remaining group_recorder objects need modified - need to update the
    # interval and in/out times. 
    # NOTE: We could also just eliminate all group_recorders and explicitely
    # add new ones.
    gr_exp = modGLM.GROUP_RECORDER_REGEX
    m = gr_exp.search(obj.strModel)
    propDict = {'interval': CONST.AMI_INTERVAL,
                'in': '"{}"'.format(CONST.STARTTIME),
                'out': '"{}"'.format(CONST.STOPTIME),
                'group': '"groupid={}"'.format(CONST.TRIPLEX_GROUP)
               }
    
    # Loop over group recorders:
    while m:
        # Extract the object
        gr = obj.extractObject(objMatch=m)
        # Modify the group_recorder and splice it in
        gr['obj'] = obj.modObjProps(gr['obj'], propDict)
        obj.replaceObject(objDict=gr)
        # Find the next match
        m = gr_exp.search(obj.strModel, gr['start'] + len(gr['obj']))
    
    print('group_recorders modified.')
    # Add a voltage magnitude group_recorder for the triplex_meters
    propDict = {'property': 'AMI_average_voltage12',
                'interval': CONST.AMI_INTERVAL,
                'group': '"groupid={}"'.format(CONST.TRIPLEX_GROUP),
                'file': 'output/R2_12_47_2_AMI_residential_phase12_mag_voltage.csv',
                'in': "{}".format(CONST.STARTTIME),
                'out': "{}".format(CONST.STOPTIME),
                'complex_part': 'MAG'}
    obj.addObject(objType='group_recorder', properties=propDict, place='end')
    print('voltage magnitude group_recorder added.')
    
    # Add the climate recorder back in
    propDict = {'property': 'temperature', 'interval': CONST.RECORD_INT,
                'parent': 'ClimateWeather',
                'file': 'output/R2_12_47_2_climate.csv', 
                'in': '"{}"'.format(CONST.STARTTIME),
                'out': '"{}"'.format(CONST.STOPTIME)}
    obj.addObject(objType='recorder', properties=propDict, place='end')
    print('climate recorder added.')
    
    # Write the model.
    obj.writeModel()
    print('AMI model written.')
    
def stripModel(fIn=MODEL_AMI, fOut=MODEL_STRIPPED):
    """Function to take the full populated GridLAB-D model, and strip out the
    pieces that aren't needed for the genetic algorithm.
    
    NOTE: This is really slow due to performing regular expression searches and
    lots of string recreation. However, it's a "run once and done" function, so
    no need to over-optimize.
    """
    
    # Create modGLM object
    obj = modGLM.modGLM(pathModelIn=fIn, pathModelOut=fOut)
    
    # Strip out objects we don't need. NOTE: May need climate later
    # TODO: Do we want to vary the SWING voltage or just let it be constant?
    # Removing the player files in the lines below makes the SWING voltage
    # constant.
    # NOTE: If we remove the players, bad nominal_voltage definitions cause
    # problems and make the model run at a voltage which is too high by a
    # factor of sqrt(3). This could be fixed, but let's leave it for now.
    obj.removeObjectsByType(typeList=['ZIPload', 'waterheater', 'house',
                                      'collector', 'recorder',
                                      'group_recorder', 'climate'])
    
    # Remove market, residential, and climate modules (reduce bloat)
    obj.removeObjectsByType(typeList=['market', 'residential', 'climate'],
                            objStr='module')
    
    # Remove the include lines - no need for schedules in the stripped model
    obj.strModel = re.sub(r'#(\s*)include(.)+\n', '', obj.strModel)
    
    # Tidy up the model
    obj = tidyModel(writeObj=obj)
    
    # Return the modGLM object - we'll want to do further tweaks
    return obj
    
def baselineModel(fIn=MODEL_AMI, fOut=MODEL_BASELINE_3, replaceClimate=True):
    """Function to build baseline model. In short, remove collectors,
    recorders, and group recorders and clean up the model (see tidyModel fn).
    
    NOTE: Replaces tmy2 weather with tmy3 so we aren't using the training data
    for evaluation. Since we aren't modeling temperature dependence, this may
    not go well.
    
    Baseline models will also need recorders added, voltdumps added, etc. This
    should happen by constructing an individual.
    """
    
    # Get a write object.
    writeObj = modGLM.modGLM(pathModelIn=fIn, pathModelOut=fOut)
    
    # Strip off all collectors, recorders, and group_recorders
    writeObj.removeObjectsByType(typeList=['collector', 'recorder',
                                           'group_recorder'])
    
    # Remove the market module - it isn't needed
    writeObj.removeObjectsByType(typeList=['market'], objStr='module')
    
    # Tidy things up
    writeObj = tidyModel(writeObj=writeObj)
    
    # Replace IL-Chicago.tmy2 with IL-Chicago-Ohare
    if replaceClimate:
        writeObj.strModel = writeObj.strModel.replace('IL-Chicago.tmy2', 
                                                      'IL-Chicago-Ohare.tmy3')
    
    # Return the write object
    return writeObj
    
def tidyModel(writeObj):
    """Simple helper function to do some model cleanup:
    1) Remove AMI_averaging_interval lines
    2) Remove stylesheet line
    3) Eliminate double newlines (make model more compact)
    4) Eliminate lines which only have comments
    5) Replace player objects with tape.player --> avoid issues with using tape
        and mysql modules at the same time.
    6) Replace relative include paths with full include paths.
    """
    # Remove AMI_averaging_interval lines
    writeObj.strModel = re.sub(r'(\t)*(AMI_averaging_interval)(.)+\n', '',
                               writeObj.strModel)
    
    # Remove the stylesheet line
    writeObj.strModel = re.sub(r'#(\s*)define(.)+\n', '', writeObj.strModel)
    
    # Eliminate any double newlines
    writeObj.strModel = re.sub(r'\n\s*\n', '\n', writeObj.strModel)
    
    # Eliminate lines which now only have comments
    writeObj.strModel = re.sub(r'//\s*\n', '', writeObj.strModel)
    
    # Replace player objects with tape.player objects
    writeObj.strModel = re.sub(r'object(\s+)player(\s*){', 'object tape.player {',
                          writeObj.strModel)
    
    # Replace relative include paths with full include paths
    writeObj.strModel = writeObj.strModel.replace('../include',
                                                  CONST.INCLUDE_DIR)
    
    # No need to actually return here, but may as well be explicit
    return writeObj

def writeRunEvalModel(outDir, starttime, stoptime, fIn, fOut):
    """Function to write, run, and evaluate the baseline model.
    """
    # If directory exists, delete it first (start fresh).
    if os.path.isdir(outDir):
        print('Output directory exists, deleting ')
        shutil.rmtree(outDir)
        
    # Create directory.
    os.mkdir(outDir)
    
    # Connect to database and drop tables.
    cnxn = util.db.connect(database=CONST.BASELINE_DB['schema'])
    util.db.dropAllTables(cnxn=cnxn)
    # Get a modGLM object for the model.
    writeObj = baselineModel(fIn=fIn, fOut=fOut)
    
    # Get the model runtime in seconds.
    t = util.helper.timeDiff(t1=starttime, t2=stoptime, fmt=util.gld.DATE_FMT)
    # Define voltdump input.
    voltdump = {'num': round(t/CONST.RECORD_INT) + 1,
                'group': CONST.TRIPLEX_GROUP,
                'outDir': outDir}
    
    # Setup the model.
    dumpFiles = writeObj.setupModel(starttime=starttime, stoptime=stoptime,
                                    timezone=CONST.TIMEZONE,
                                    database=CONST.BASELINE_DB,
                                    voltdump=voltdump, vSource=None)
    
    print('Voltdumps added.')
    
    # Instantiate an individual - while we don't need all the bells and 
    # whistles of an individual, it has the capability to add recorders, run
    # its own model, evaluate it's own model, etc.
    baseInd = individual.individual(starttime=starttime, stoptime=stoptime,
                                    voltdumpFiles=dumpFiles, reg=CONST.REG,
                                    cap=CONST.CAP, regFlag=3, capFlag=3,
                                    controlFlag=4, uid=0)
    print('Individual created.')
    print('Writing, running, and evaluating baseline...')
    # Get database cursor
    cursor = cnxn.cursor()
    baseInd.writeRunUpdateEval(strModel=writeObj.strModel,
                               inPath=fOut, outDir=outDir,
                               cursor=cursor, costs=CONST.COSTS)
    print('Run complete.')
    print(baseInd)
    
    return baseInd

def evaluateZIP(starttime, stoptime, runInterval, resultsFile='results',
                logFile='log'):
    """Function to run the populated baseline model and the ZIP baseline model,
    and write output data to file. 
    """
    # Setup the models. Note this depends on the existence of MODEL_AMI and 
    # MODEL_STRIPPED. If they don't exist:
    '''
    populatedToAMI()
    writeObj = stripModel()
    writeObj.writeModel()
    '''
    # Connect to database and drop tables.
    cnxn = util.db.connect(database=CONST.BASELINE_DB['schema'])
    util.db.dropAllTables(cnxn=cnxn)
    
    # If the output directory doesn't exist, make it
    if not os.path.isdir(CONST.COMPARE_OUT):
        os.mkdir(CONST.COMPARE_OUT)
    
    # Open the results files
    fBase2 = open(CONST.COMPARE_OUT + '/' + resultsFile + '_base_2.csv',
                 newline='', mode='w')
    fBase3 = open(CONST.COMPARE_OUT + '/' + resultsFile + '_base_3.csv',
                 newline='', mode='w')
    fZIP = open(CONST.COMPARE_OUT + '/' + resultsFile + '_ZIP.csv', newline='',
                mode='w')
    
    # Open the log files
    logBase2 = open(CONST.COMPARE_OUT + '/' + logFile + '_2.txt',
                    newline='', mode='w')
    logBase3 = open(CONST.COMPARE_OUT + '/' + logFile + '_3.txt',
                    newline='', mode='w')
    logZIP = open(CONST.COMPARE_OUT + '/' + logFile + '_ZIP.txt',
                  newline='', mode='w')
    
    # Get the cost fields, and write them to the file as headers.
    # HARD-CODE total field in
    costList = ['time', 'total'] + list(CONST.COSTS.keys())
    
    # Initialize csv writers and write headers
    csvBase2 = csv.DictWriter(f=fBase2, fieldnames=costList,
                              quoting=csv.QUOTE_NONNUMERIC)
    csvBase2.writeheader()
    csvBase3 = csv.DictWriter(f=fBase3, fieldnames=costList,
                              quoting=csv.QUOTE_NONNUMERIC)
    csvBase3.writeheader()
    csvZIP = csv.DictWriter(f=fZIP, fieldnames=costList,
                            quoting=csv.QUOTE_NONNUMERIC) 
    csvZIP.writeheader()
    
    # Initialize queues for running and cleaning up models
    modelQueue = Queue()
    cleanupQueue = Queue()
    
    # Define database input
    database={'database': CONST.BASELINE_DB['schema']}
    
    # Start threads for running models and cleaning them up
    modelThreads = []
    cleanupThreads = []
    
    for _ in range(3):
        # Initialize threads
        tRun = threading.Thread(target=population.writeRunEval,
                                args=(modelQueue, CONST.COSTS, database))
        tClean = threading.Thread(target=individual.cleanup,
                                  args=(cleanupQueue, database))
        
        # Add to thread lists
        modelThreads.append(tRun)
        cleanupThreads.append(tClean)
        
        # Start threads
        tRun.start()
        tClean.start()
        
    print('Model and cleanup threads started.')
    
    # Note defaults for setting up models assumes ZIP files are for each hour
    dumpfiles = setupBaseAndBaseZIP()
    print('Baseline and stripped models setup.')
    
    # Get write objects
    writeZIP = modGLM.modGLM(pathModelIn=MODEL_STRIPPED_DUMP)
    writeBase2 = modGLM.modGLM(pathModelIn=MODEL_BASELINE_2)
    writeBase3 = modGLM.modGLM(pathModelIn=MODEL_BASELINE_3)
    
    # Get our times ready
    s = starttime
    e = util.helper.incrementTime(t=starttime, fmt=util.gld.DATE_FMT,
                                  interval=runInterval)
    
    # Use different ID's for the individuals
    bID2 = 0
    bID3 = 1
    zID = 2
    
    # Instantiate individuals - while we don't need all the bells and 
    # whistles of an individual, it has the capability to add recorders, run
    # its own model, evaluate it's own model, etc.
    # NOTE: We could consider using copy.deepcopy and then modifying UID.
    baseInd2 = individual.individual(starttime=s, stoptime=e,
                                     voltdumpFiles=dumpfiles, reg=CONST.REG,
                                     cap=CONST.CAP, regFlag=3, capFlag=3,
                                     controlFlag=4, uid=bID2)
    
    baseInd3 = individual.individual(starttime=s, stoptime=e,
                                     voltdumpFiles=dumpfiles, reg=CONST.REG,
                                     cap=CONST.CAP, regFlag=3, capFlag=3,
                                     controlFlag=4, uid=bID3)
    
    ZIPInd = individual.individual(starttime=s, stoptime=e,
                                   voltdumpFiles=dumpfiles, reg=CONST.REG,
                                   cap=CONST.CAP, regFlag=3, capFlag=3,
                                   controlFlag=4, uid=zID)
    
    # Initialize dictionaries for threading use
    baseDict2 = {'outDir': CONST.COMPARE_OUT,
                 'individual': baseInd2,
                 'inPath': MODEL_BASELINE_2,
                 'strModel': ''}
    baseDict3 = {'outDir': CONST.COMPARE_OUT,
                 'individual': baseInd3,
                 'inPath': MODEL_BASELINE_3,
                 'strModel': ''}
    ZIPDict = {'outDir': CONST.COMPARE_OUT,
               'individual': ZIPInd,
               'inPath': MODEL_ZIP,
               'strModel': ''}
    
    # Loop over time until we've hit the stoptime
    while util.helper.timeDiff(t1=e, t2=stoptime, fmt=util.gld.DATE_FMT) >= 0:
        print('Running for {} through {}'.format(s, e), flush=True)
        
        # *********************************************************************
        # Get the base model ready to run, put it in the queue.
        baseInd2.prep(starttime=s, stoptime=e)
        writeBase2.updateClock(starttime=s, stoptime=e)
        writeBase2.addRuntimeToVoltDumps(starttime=s, stoptime=e,
                                        interval=CONST.RECORD_INT)
        baseDict2['strModel'] = writeBase2.strModel
        modelQueue.put_nowait(baseDict2)
        
        baseInd3.prep(starttime=s, stoptime=e)
        writeBase3.updateClock(starttime=s, stoptime=e)
        writeBase3.addRuntimeToVoltDumps(starttime=s, stoptime=e,
                                        interval=CONST.RECORD_INT)
        baseDict3['strModel'] = writeBase3.strModel
        modelQueue.put_nowait(baseDict3)

        # *********************************************************************
        # Get ZIP model ready.
        # print('Prepping ZIP model for run.', flush=True)
        ZIPInd.prep(starttime=s, stoptime=e)
        writeZIP.updateClock(starttime=s, stoptime=e)
        writeZIP.addRuntimeToVoltDumps(starttime=s, stoptime=e,
                                       interval=CONST.RECORD_INT)

        # Add ZIP models to the ZIP object
        writeZIP.addZIP(zipDir=CONST.ZIP_DIR, starttime=s,
                        stoptime=e)
        # print('ZIP models added.', flush=True)
        
        # Put the updated models in the dictionaries
        ZIPDict['strModel'] = writeZIP.strModel
        # Add the individuals to the modelQueue
        modelQueue.put_nowait(ZIPDict)
        # print('ZIP model running for {} through {}'.format(s, e), flush=True)
        
        # Wait for the models to run
        modelQueue.join()
        # print('Models run and evaluated.')
        
        # Build cleanup dictionaries for the individuals
        baseClean2 = baseInd2.buildCleanupDict(truncateFlag=True)
        baseClean3 = baseInd3.buildCleanupDict(truncateFlag=True)
        ZIPClean = ZIPInd.buildCleanupDict(truncateFlag=True)
        
        # Write to csv, log, cleanup, and rotate dictionaries
        for g in [(baseInd2, logBase2, csvBase2, baseClean2),
                  (baseInd3, logBase3, csvBase3, baseClean3),
                  (ZIPInd, logZIP, csvZIP, ZIPClean)]:
            
            # Cleanup
            cleanupQueue.put_nowait(g[3])
            
            # Write to csv
            g[2].writerow({'time': s, **g[0].costs})
            
            # Log file
            print('*'*80, file=g[1])
            print(s, file=g[1])
            print(g[0], file=g[1])
            
            # Rotate dictionaries
            g[0].reg, g[0].cap = util.helper.rotateVVODicts(reg=g[0].reg,
                                                            cap=g[0].cap)
        
        # Increment the times
        s = util.helper.incrementTime(t=s, fmt=util.gld.DATE_FMT,
                                      interval=runInterval)
        e = util.helper.incrementTime(t=e, fmt=util.gld.DATE_FMT,
                                      interval=runInterval)
        
        # Ensure cleanup is complete before moving on
        cleanupQueue.join()
        
    # Shut down the threads and close the file
    for _ in modelThreads: modelQueue.put_nowait(None)
    for _ in cleanupThreads: cleanupQueue.put_nowait(None)
    for t in modelThreads: t.join(timeout=10)
    for t in cleanupThreads: t.join(timeout=10)
    
    # Close the files
    fBase2.close()
    fBase3.close()
    fZIP.close()
    logBase2.close()
    logBase3.close()
    logZIP.close()
    
    print('Threads stopped and files closed. All done.')
    
    
def setupBaseAndBaseZIP(tZIP=CONST.ZIP_INTERVAL, starttime=CONST.STARTTIME,
                        stoptime=CONST.STOPTIME):
    """Function to craft the two baseline models so they can be compared. They
    will then need to be run in a loop.
    """
    # Get a modGLM object for the populated baseline
    writePop2 = baselineModel(fIn=MODEL_AMI, fOut=MODEL_BASELINE_2,
                              replaceClimate=False)
    writePop3 = baselineModel(fIn=MODEL_AMI, fOut=MODEL_BASELINE_3,
                              replaceClimate=True)
    writeZIP = baselineModel(fIn=MODEL_STRIPPED, fOut=MODEL_STRIPPED_DUMP)
    # Get a modGLM object for the zip baseline
    
    # Define voltdump input.
    voltdump = {'num': round(tZIP/CONST.RECORD_INT) + 1,
                'group': CONST.TRIPLEX_GROUP,
                'outDir': None}
    
    # Setup the models. NOTE: with no outDir defined, these dump file outputs
    # should be exactly the same. 
    dumpPop = writePop2.setupModel(starttime=starttime, stoptime=stoptime,
                                   timezone=CONST.TIMEZONE,
                                   database=CONST.BASELINE_DB,
                                   voltdump=voltdump, vSource=None)
    
    _ = writePop3.setupModel(starttime=starttime, stoptime=stoptime,
                             timezone=CONST.TIMEZONE,
                             database=CONST.BASELINE_DB,
                             voltdump=voltdump, vSource=None)
    
    _ = writeZIP.setupModel(starttime=starttime, stoptime=stoptime,
                                  timezone=CONST.TIMEZONE,
                                  database=CONST.BASELINE_DB,
                                  voltdump=voltdump, vSource=None)
    
    # Write the models
    writePop2.writeModel()
    writePop3.writeModel()
    writeZIP.writeModel()
    
    return dumpPop
    
if __name__ == '__main__':
    """
    # Get the popluated model ready to run.
    populatedToAMI()

    # Strip the full model.
    writeObj = stripModel()
    print('Full model stripped down.')
    """
    
    """
    # Define voltdump input:
    voltdump = {'num': round(CONST.MODEL_RUNTIME/CONST.RECORD_INT) + 1,
                'group': CONST.TRIPLEX_GROUP}
    # NOTE: We only want to setup the model for the genetic algorithm. NOT to
    # run the writeRunEval function.
    # Setup the model (add voltdumps, database, etc.)
    writeObj.setupModel(voltdump=voltdump, vSource=None)
    print('Voltdumps and database stuff added.')
    """
    
    """
    # Save the new model
    writeObj.writeModel()
    print('Stripped model written.')
    """
    
    """
    s = '2016-02-19 00:00:00'
    e = '2016-02-19 01:00:00'

    zipDir = 'E:/ami/ZIP-Constrained'
    writeObj = modGLM.modGLM(pathModelOut=MODEL_ZIP,
                             pathModelIn=MODEL_STRIPPED) 
    # Add zip models
    writeObj.addZIP(zipDir=zipDir, starttime=s, stoptime=e)
    writeObj.writeModel()
    print('ZIP models added.')

    outDirBase = r'E:/pmaps/experiment/R2_12_47_2/baselineOut'
    outDirZIP = r'E:/pmaps/experiment/R2_12_47_2/zipOut'
    # Run populated baseline model
    t0 = time.time()
    baseline = writeRunEvalModel(outDir=outDirBase, starttime=s, stoptime=e,
                                 fIn=MODEL_AMI, fOut=MODEL_BASELINE)
    t1 = time.time()
    print('Base model run in {:.2f}s'.format(t1-t0))
    t0 = time.time()
    zip = writeRunEvalModel(outDir=outDirZIP, starttime=s, stoptime=e,
                                 fIn=MODEL_ZIP, fOut=MODEL_STRIPPED_DUMP)
    t1 = time.time()
    print('ZIP model run in {:.2f}s'.format(t1-t0))
    print('all done. Need to add threading...')
    """
    
    """
    # These times are near to the hottest days
    s = '2016-07-19 14:00:00'
    e = '2016-07-19 15:00:00'
    """
    s = '2016-01-01 00:00:00'
    e = '2016-01-01 03:00:00'
    evaluateZIP(starttime=s, stoptime=e, runInterval=CONST.ZIP_INTERVAL)