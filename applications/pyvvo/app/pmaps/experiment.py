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

# Paths to input/output models.
partial =  CONST.BASE_PATH + '/' + CONST.MODEL
MODEL_POPULATED = partial + r'_populated.glm'
MODEL_AMI = partial + r'_AMI.glm'
MODEL_STRIPPED = partial + r'_stripped.glm'
MODEL_MANUAL = partial + r'_manual.glm'
MODEL_VVO = partial + r'_vvo.glm'
MODEL_BASELINE = partial + r'_baseline.glm'
MODEL_ZIP = partial + r'_ZIP.glm'
MODEL_ZIP_VVO = partial + r'_ZIP_VVO.glm'

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
    obj = modGLM.modGLM(pathModelIn=fIn,
                                      pathModelOut=fOut)
    
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
    
def baselineModel(fIn=MODEL_AMI, fOut=MODEL_BASELINE):
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
    
    # Tidy things up
    writeObj = tidyModel(writeObj=writeObj)
    
    # Replace IL-Chicago.tmy2 with IL-Chicago-Ohare
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
    
if __name__ == '__main__':

    # Get the popluated model ready to run.
    populatedToAMI()

    # Strip the full model.
    writeObj = stripModel()
    print('Full model stripped down.')
    # Define voltdump input:
    voltdump = {'num': round(CONST.MODEL_RUNTIME/CONST.RECORD_INT) + 1,
                'group': CONST.TRIPLEX_GROUP}
    """
    # NOTE: We only want to setup the model for the genetic algorithm. NOT to
    # run the writeRunEval function.
    # Setup the model (add voltdumps, database, etc.)
    writeObj.setupModel(voltdump=voltdump, vSource=None)
    print('Voltdumps and database stuff added.')
    """
    # Save the new model
    writeObj.writeModel()
    print('Stripped model written.')

    s = '2016-02-19 00:00:00'
    e = '2016-02-19 01:00:00'

    zipDir = 'E:/ami/ZIP-Constrained'
    writeObj = modGLM.modGLM(pathModelOut=MODEL_ZIP,
                             pathModelIn=MODEL_STRIPPED) 
    # Add zip models
    writeObj.addZIP(zipDir=zipDir, starttime=s, stoptime=e)
    writeObj.writeModel()
    print('ZIP models added.')

    """
    # These times are near to the hottest days
    s = '2016-07-19 14:00:00'
    e = '2016-07-19 15:00:00'
    """
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
                                 fIn=MODEL_ZIP, fOut=MODEL_ZIP_VVO)
    t1 = time.time()
    print('ZIP model run in {:.2f}s'.format(t1-t0))
    print('all done. Need to add threading...')