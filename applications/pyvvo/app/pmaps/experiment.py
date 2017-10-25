'''
Created on Oct 24, 2017

@author: thay838
'''
from powerflow import writeCommands
import re
from pmaps import constants

# Paths to input/output models.
MODEL_POPULATED = r'\\pnl\projects\VVO-GridAPPS-D\pmaps\experiment\R2_12_47_2\R2_12_47_2_populated.glm'
MODEL_AMI = r'\\pnl\projects\VVO-GridAPPS-D\pmaps\experiment\R2_12_47_2\R2_12_47_2_AMI.glm'
MODEL_OUT_STRIPPED = r'\\pnl\projects\VVO-GridAPPS-D\pmaps\experiment\R2_12_47_2\R2_12_47_2_stripped.glm'
MODEL_OUT_MANUAL = r'\\pnl\projects\VVO-GridAPPS-D\pmaps\experiment\R2_12_47_2\R2_12_47_2_manual.glm'

def populatedToAMI(interval=900, group=constants.TRIPLEX_GROUP):
    """Function to take the full populated GridLAB-D model, and get it ready to
    run in order to generate all the data we need for load modeling.
    
    In essence, this function will find every house, extract its parent, then
    if the parent is a triplex_node, give it a group definition and
    AMI_averaging_interval.
    
    After this function, the model should be ready to run.
    """
    
    # Get a writeCommands object
    obj = writeCommands.writeCommands(pathModelIn=MODEL_POPULATED,
                                      pathModelOut=MODEL_AMI)
    
    # Define the properties we want to modify for triplex_meters
    propDict = {'AMI_averaging_interval': interval, 'groupid': group}
    
    # For shorthand, get house regex
    houseExp = writeCommands.HOUSE_REGEX
    
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
                                                    objRegEx=writeCommands.TRIPLEX_METER_REGEX)
            
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
    obj.updateClock(starttime=constants.STARTTIME, stoptime=constants.STOPTIME,
                    timezone=constants.TIMEZONE)
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
    gr_exp = writeCommands.GROUP_RECORDER_REGEX
    m = gr_exp.search(obj.strModel)
    propDict = {'interval': constants.AMI_INTERVAL,
                'in': '"{}"'.format(constants.STARTTIME),
                'out': '"{}"'.format(constants.STOPTIME),
                'group': '"groupid={}"'.format(constants.TRIPLEX_GROUP)
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
                'interval': constants.AMI_INTERVAL,
                'group': '"groupid={}"'.format(constants.TRIPLEX_GROUP),
                'file': 'output/R2_12_47_2_AMI_residential_phase12_mag_voltage.csv',
                'in': "{}".format(constants.STARTTIME),
                'out': "{}".format(constants.STOPTIME),
                'complex_part': 'MAG'}
    obj.addObject(objType='group_recorder', properties=propDict, place='end')
    print('voltage magnitude group_recorder added.')
    
    # Add the climate recorder back in
    propDict = {'property': 'temperature', 'interval': constants.RECORD_INT,
                'parent': 'ClimateWeather',
                'file': 'output/R2_12_47_2_climate.csv', 
                'in': '"{}"'.format(constants.STARTTIME),
                'out': '"{}"'.format(constants.STOPTIME)}
    obj.addObject(objType='recorder', properties=propDict, place='end')
    print('climate recorder added.')
    
    # Write the model.
    obj.writeModel()
    print('AMI model written.')
    
def stripModel():
    """Function to take the full populated GridLAB-D model, and strip out the
    pieces that aren't needed for the genetic algorithm.
    
    NOTE: This is really slow due to performing regular expression searches and
    lots of string recreation. However, it's a "run once and done" function, so
    no need to over-optimize.
    """
    
    # Create writeCommands object
    obj = writeCommands.writeCommands(pathModelIn=MODEL_AMI,
                                      pathModelOut=MODEL_OUT_STRIPPED)
    
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
    
    # Remove AMI_averaging_interval lines
    obj.strModel = re.sub(r'(\t)*(AMI_averaging_interval)(.)+\n', '',
                          obj.strModel)
    
    # Remove the include lines - no need for schedules in the stripped model
    obj.strModel = re.sub(r'#(\s*)include(.)+\n', '', obj.strModel)
    
    # Remove the stylesheet line
    obj.strModel = re.sub(r'#(\s*)define(.)+\n', '', obj.strModel)
    
    # Eliminate any double newlines
    obj.strModel = re.sub(r'\n\s*\n', '\n', obj.strModel)
    
    # Eliminate lines which now only have comments
    obj.strModel = re.sub(r'//\s*\n', '', obj.strModel)
    
    # Replace player objects with tape.player objects
    obj.strModel = re.sub(r'object(\s+)player(\s*){', 'object tape.player {',
                          obj.strModel)
    
    # Return the writeCommands object - we'll want to do further tweaks
    return obj

def manualControl(fIn=MODEL_OUT_STRIPPED, fOut=MODEL_OUT_MANUAL):
    """Function to switch regulator and capacitor control to MANUAL. This is 
    what the genetic algorithm uses.
    
    INPUTS: fIn is a full path to a .glm to read, fOut is a path to write the
    new .glm to.
    """
    
    # Initialize a writeCommands object
    obj = writeCommands.writeCommands(pathModelIn=fIn, pathModelOut=fOut)
    
    # Loop over the regulators from the constants file.
    reg = {}
    for r in constants.REG:
        reg[r] = {}
        reg[r]['Control'] = 'MANUAL'
        # We need to define phases even if they're empty.
        reg[r]['phases'] = []
    
    # Loop over the capacitors from the constants file.
    cap = {}
    for c in constants.CAP:
        cap[c] = {}
        cap[c]['control'] = 'MANUAL'
        # We need to define phases even if they're empty.
        cap[c]['phases'] = []
        
    # Command regulators and capacitors.
    obj.commandRegulators(reg=reg)
    obj.commandCapacitors(cap=cap)
    
    # Write new model.
    obj.writeModel()
    
if __name__ == '__main__':
    # Get the popluated model ready to run.
    # populatedToAMI()

    # Strip the full model.
    writeObj = stripModel()
    print('Full model stripped down.')
    # Define voltdump input:
    voltdump = {'num': round(constants.MODEL_RUNTIME/constants.RECORD_INT) + 1,
                'group': constants.TRIPLEX_GROUP}
    # Setup the model (add voltdumps, database, etc.)
    writeObj.setupModel(voltdump=voltdump, vSource=None)
    print('Voltdumps and database stuff added.')
    # Save the new model
    writeObj.writeModel()
    print('Stripped model written.')
    
    # TODO: General model setup (database, etc.)
    # TODO: Add ZIP models
    # Create variant with MANUAL control.
    manualControl()
    print('Manual control model variant created.')