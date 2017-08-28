"""
Module to write setpoint commands to a GridLAB-D model (.glm)

NOTE: This module is in 'prototype' mode. As such, it certainly isn't
optimally efficient.

Created on Jul 27, 2017

@author: thay838

"""

import re
import os

def readModel(modelIn):
    '''Simple function to read file as string'''
    # TODO: strip this function out of this file.
    with open(modelIn, 'r') as f:
        s = f.read()
    return s

class writeCommands:
    """"Class for reading GridLAB-D model and changing it.
    
        Some capabilities: regulator taps, capacitor switches, clock change,
            mysql connection, swing recorder
    """
    # Define some constants for GridLAB-D model parsing.
    REGOBJ_REGEX = re.compile(r'\bobject\b(\s+)\bregulator\b')
    REGCONF_REGEX = re.compile(r'\bobject\b(\s+)\bregulator_configuration\b')
    CAP_REGEX = re.compile(r'\bobject\b(\s+)\bcapacitor\b')
    CLOCK_REGEX = re.compile(r'\bclock\b(\s*)(?={)')
    TAPE_REGEX1 = re.compile(r'\bmodule\b(\s+)\btape\b(\s*);')
    TAPE_REGEX2 = re.compile(r'\bmodule\b(\s+)\btape\b(\s*)(?={)')
    SWING_REGEX = re.compile(r'\bbustype\b(\s+)\bSWING\b')
    OBJ_REGEX = re.compile(r'\bobject\b')
    NODE_REGEX = re.compile(r'\bobject\b(\s+)\bnode\b')
    PROFILER_REGEX = re.compile(r'#(\s*)\bset\b(\s+)\bprofiler\b(\s*)=(\s*)[01]')
    SUPPRESS_REGEX = re.compile(r'#(\s*)\bset\b(\s+)\bsuppress_repeat_messages\b(\s*)=(\s*)[01]')
    
    def __init__(self, strModel, pathModelOut='', pathModelIn=''):
        """"Initialize class with input/output GridLAB-D models
        
        strModel should be a string representing a GridLAB-D model.
            Can be obtained by calling readModel function in this module.
            It will be modified to send new commands.
            
        pathModelOut is the path to the new output model.
        """

        # Set properties.
        self.strModel = strModel
        self.pathModelIn = pathModelIn
        self.pathModelOut = pathModelOut
        
    def writeModel(self):
        """"Simple method to write strModel to file"""
        
        with open(self.pathModelOut, 'w') as f:
            f.write(self.strModel)
    
    def commandRegulators(self, regulators):
        """"Function to change tap positions on a given regulator.
        
        This is performed by finding its configuration and changing 'tap_pos.'
        
        INPUT: Dictionary of dictionaries. Top level keys are regulator
            names. Each subdict can have up to two keys: 'regulator' and 
            'configuration.' Within the 'regulator' or 'configuration' dicts,
            keys are properties to change (e.g. tap_A or Control) mappped to
            the new desired value (e.g. 2 or MANUAL)
            
        Example regulators input:
        regulators={'R2-12-47-2_reg_1': {
                        'regulator': {
                            'tap_A':1,
                            'tap_B':2,
                            'tap_C':3
                        },
                        'configuration': {
                            'Control': 'MANUAL'
                        }
                    },
                    'R2-12-47-2_reg_2': {
                        'regulator': {
                            'tap_A':4,
                            'tap_B':5,
                            'tap_C':6
                        },
                        'configuration': {
                            'Control': 'MANUAL'
                        }
                    }
        }
        
        OUTPUT: The GridLAB-D model in self.strModel is modified
        """
        # First, find all regulators
        regMatch = self.REGOBJ_REGEX.search(self.strModel)
        
        # Loop through regulators to find names and configs.
        # Note that this could be more efficient, but oh well.
        regDict = dict()
        while regMatch is not None:
            # Extract the object:
            reg = self.extractObject(regMatch)
            
            # Extract name and configuration properties and assign to dict
            d = self.extractProperties(reg['obj'],
                                                ['name', 'configuration'])
            name = d['name']['prop']
            regDict[name] = d
            
            # If this regulator is in our input dictionary, alter properties
            if (name in regulators) and ('regulator' in regulators[name]):
                
                # Modify the properties of the regulator
                reg['obj'] = self.modObjProps(reg['obj'], 
                                                       regulators[name]['regulator'])
                
                # Replace regulator with new modified regulator
                self.replaceObject(reg)
            
            # Find the next regulator using index offset
            regEndInd = reg['start'] + len(reg['obj'])
            regMatch = self.REGOBJ_REGEX.search(self.strModel,
                                                              regEndInd)
            
            
            
        # Find the configurations for the requested regulators and put in list.
        # NOTE: confList and regList MUST be one to one.
        confList = []
        regList = []
        for regName, commandDict in regulators.items():
            # If we didn't find it, raise an exception
            if regName not in regDict:
                raise ObjNotFoundError(obj=regName, model=self.pathModelIn)
            
            # If we're commanding the configuration, add it to the list.
            if 'configuration' in commandDict:
                # Extract name of the configuration, put in list
                confList.append(regDict[regName]['configuration']['prop'])
                # Put the regulator in the list
                regList.append(regName)
        
        # Next, loop through and command regulator configurations. Since we'll
        # be modifying the model as we go, we shouldn't use the 'finditer' 
        # method.
        regConfMatch = self.REGCONF_REGEX.search(self.strModel)
        
        while regConfMatch is not None:
            # Extract the object
            regConf = self.extractObject(regConfMatch)
            
            # Extract the name
            d = self.extractProperties(regConf['obj'], ['name'])
            
            # If the regulator is in our configuration list, alter config.
            if d['name']['prop'] in confList:
                # Get the name of the regulator to command
                regInd = confList.index(d['name']['prop'])
                regName = regList[regInd]
                
                # Modify the configuration
                regConf['obj'] = self.modObjProps(regConf['obj'],
                                                  regulators[regName]['configuration'])
                    
                # Regulator configuration has been updated, now update model
                self.replaceObject(regConf)
            
            # Find the next regulator configuration, using index offset
            regEndInd = regConf['start'] + len(regConf['obj'])
            regConfMatch = self.REGCONF_REGEX.search(self.strModel,
                                                              regEndInd)
        
    def commandCapacitors(self, capacitors):
        """"Function to change state of capacitors.
        
        INPUT: Dictionary of dictionaries. Top level keys are capacitor
            names. Each subdict's keys are properties to change (e.g. 
            switchA) mappped to the new desired value (e.g. OPEN)
        
        Example capacitors input:
        capacitors={'R2-12-47-2_cap_1': {
                        'switchA':'OPEN',
                        'switchB':'CLOSED',
                        'control': 'MANUAL'
                    },
                    'R2-12-47-2_cap_4': {
                        'switchA':'CLOSED',
                        'switchB':'CLOSED',
                        'switchC': 'OPEN',
                        'control': 'MANUAL'
                    }
        }
        
            
        OUTPUT: The GridLAB-D model in self.strModel is modified
        """
        # Find the first capacitor
        capMatch = self.CAP_REGEX.search(self.strModel)
        
        # Loop through the capacitors
        while capMatch is not None:
            # Extract the object
            cap = self.extractObject(capMatch)
            
            # Extract its name
            capName = self.extractProperties(cap['obj'], ['name'])
            n = capName['name']['prop']
            
            # If the capacitor is in the list to command, do so
            if n in capacitors:
                # Modify the capacitor object to implement commands
                cap['obj'] = self.modObjProps(cap['obj'], 
                                                       capacitors[n])
                                 
                # Splice new capacitor object into model
                self.replaceObject(cap)
                                
            # Find the next capacitor, using index offset
            capEndInd = cap['start'] + len(cap['obj'])
            capMatch = self.CAP_REGEX.search(self.strModel, capEndInd)
            
    def updateClock(self, start, stop):
        """Function to set model time.
        
        INPUTS:
            start: simulation start time (starttime in GLD). Should be
                surrounded in single quotes, and be in the format
                'yyyy-mm-dd HH:MM:SS'
            stop: simulation stop time (stoptime in GLD). Same format as start.
            
            NOTE: Timezones can be included in start and stop.
        """
        
        # Find and extract the clock object
        clockMatch = self.CLOCK_REGEX.search(self.strModel)
        clock = self.extractObject(clockMatch)
        
        # Modify the times
        clock['obj'] = self.modObjProps(clock['obj'],
                                                 {'starttime': start,
                                                  'stoptime': stop})
        
        # Splice in new clock object
        self.replaceObject(clock)
        
    def removeTape(self):
        """Method to remove tape module from model."""
        # First, look for simple version: 'module tape;'
        tapeMatch = self.TAPE_REGEX1.search(self.strModel)
        
        # If simple version is found, eliminate it
        if tapeMatch is not None:
            s = tapeMatch.span()[0]
            e = tapeMatch.span()[1]
            self.strModel = self.strModel[0:s] + self.strModel[e+1:]
        else:
            # Find "more full" definition of tape
            tapeMatch = self.TAPE_REGEX2.search(self.strModel)
            if tapeMatch is not None:
                # Extract the object
                tapeObj = self.extractObject(tapeMatch)
                # Eliminate the object
                self.strModel = (self.strModel[0:tapeObj['start']]
                                 + self.strModel[tapeObj['end']+1:])
                
    def addMySQL(self, hostname='localhost', username='gridlabd',
                 password='', schema='gridlabd', port='3306',
                 socketname='/tmp/mysql.sock'):
        """Method to add mysql module and database connection to model
        
        For now, it will simply be added to the beginning of the model.
        
        Note that GridLAB-D must be built with the mysql connector, the
        connector library must be on the system path, and to run a model the
        database server must be running.
        
        Two relevant GridLAB-D wiki pages:
        http://gridlab-d.shoutwiki.com/wiki/Mysql
        http://gridlab-d.shoutwiki.com/wiki/Database
        
        TODO: Add the rest of the options for the database object:
            clientflags, options, on_init, on_sync, on_term, sync_interval,
            tz_offset, uses_dst
        """
        # Construct the beginning of the necessary string
        dbStr = (
        "module mysql;\n"
        "object database {{\n"
        '   hostname "{host}";\n'
        '   username "{usr}";\n'
        '   password "{pwd}";\n'
        '   schema "{schema}";\n'
        '   port {port};\n'
        ).format(host=hostname, usr=username, pwd=password, schema=schema,
                   port=port)
        # If we're on Mac or Linux, need to include the sockenamae
        if os.name == 'posix':
            dbStr = (dbStr + 'socketname "{sock}";\n').format(sock=socketname)
            
        self.strModel = dbStr + '}\n' + self.strModel
        
    def repeatMessages(self, val=0):
        """Method to set 'suppress_repeat_messages'
        
        TODO: unit test.
        """
        # See if the model already has the constant
        m = self.SUPPRESS_REGEX.search(self.strModel)
        if m:
            # Simply replace last character.
            self.strModel = (self.strModel[0:(m.span()[1] - 1)] + str(val)
                             + self.strModel[m.span()[1]:])
        else:
            # Add to beginning of model.
            self.strModel = ('#set suppress_repeat_messages={}'.format(val) 
                             + '\n') + self.strModel
                         
    def toggleProfile(self, val=0):
        """Method to toggle the profiler
        
        TODO: unit test.
        """
        # See if the model has the profiler set already
        m = self.PROFILER_REGEX.search(self.strModel)
        if m:
            # Simply replace the last character with val.
            self.strModel = (self.strModel[0:(m.span()[1] - 1)] + str(val)
                             + self.strModel[m.span()[1]:])
        else:
            # Add the profiler string.
            self.strModel = ('#set profiler={}'.format(val) + '\n'
                             + self.strModel)
        
    def findSwing(self):
        """Method to find the name of the swing bus.
        
        Returns name of the swing bus.
        
        IMPORTANT NOTE: This WILL NOT WORK if there are nested objects in the
            swing node object.
            
        IMPORTANT NOTE 2: Multiple swing buses will also ruin this.
            
        TODO: Rather than finding the swing by traversing the model, this
            should be pulled from the CIM.
            
        TODO: Handle multiple swing case
        """
        # Find the swing node
        swingMatch = self.SWING_REGEX.search(self.strModel)
        
        # Find the closest open curly brace:
        sInd = swingMatch.span()[0]
        c = self.strModel[sInd]
        while c != '{':
            sInd -= 1
            c = self.strModel[sInd]
        
        # Find the closest close curly brace:
        eInd = swingMatch.span()[1]
        c = self.strModel[eInd]
        while c != '}':
            eInd += 1
            c = self.strModel[eInd]
            
        # To ensure eInd includes the curly brace, add one to it.
        eInd += 1
        
        # Extract the name of the swing
        sName = self.extractProperties(self.strModel[sInd:eInd+1], ['name'])
        
        # Find the true beginning of the node by finding the nearest 'object'
        swingIter = self.OBJ_REGEX.finditer(self.strModel, 0, sInd)
        
        # Loop over the iterator until the last one. This feels dirty.
        # TODO: Make this more efficient?
        for m in swingIter:
            pass
        
        # Extract the starting index of the object.
        sInd = m.span()[0]
        
        return {'name': sName['name']['prop'], 'start': sInd, 'end': eInd,
                'obj': self.strModel[sInd:eInd]}
    
    def recordSwing(self, interval=60, uid=0):
        """Add recorder to model to record the power flow on the swing.
        
        NOTE: swing will be changed from node to meter if it's a node
        
        NOTE: This is for a database recorder.
        
        OUTPUT: name of table.
        """
        # Find the name and indices of the swing.
        swing = self.findSwing()
        
        # If the swing object is a node, replace it with a meter.
        nodeMatch = self.NODE_REGEX.match(swing['obj'])
        
        if nodeMatch is not None:
            # Replace 'object node' with 'object meter'
            swing['obj'] = (swing['obj'][0:nodeMatch.span()[0]] 
                             + 'object meter' 
                             + swing['obj'][nodeMatch.span()[1]:])
            
            # Splice in the new object
            self.replaceObject(swing)
        
        # Define table to use.
        table = 'swing_' + str(uid)
        
        # Create recorder.
        self.addRecorder(parent=swing['name'], table=table,
                         properties=['measured_power_A', 'measured_power_B',
                                     'measured_power_C'],
                         interval=interval)
        
        # Return the name of the table
        return table
        
    def addRecorder(self, parent, table, properties, interval):
        """Method to add database recorder to end of model
        
        INPUTS:
            parent: name of object to record
            table: database table to save outputs in
            properties: list of properties to record. Ex: ['power_A',
                'power_B', 'power_C']
            interval: interval in seconds to record
            
        TODO: Add more properties
        """
        # Add formatted string to end of model.
        self.strModel = self.strModel + ("\n"
            "object recorder {{\n"
            "    parent {parent};\n"
            '    table "{table}";\n'
            '    property {propList};\n'
            '    interval {interval};\n'
            '}};').format(parent=parent, table=table,
                         propList=','.join(properties), interval=interval)
        pass
            
    def replaceObject(self, objDict):
        """Function to replace object in the model string with a modified
        one.
        
        INPUTS: objDict: object dictionary in the format returned by 
            writeCommands.extractObject
            
        OUTPUS: directly modifies self.strModel to replace object with new one
        """
        self.strModel = (self.strModel[0:objDict['start']] + objDict['obj']
                         + self.strModel[objDict['end']:])
                                
    @staticmethod
    def modObjProps(objStr, propDict):
        """"Function to modify an object's properties"""
        
        # Loop through the properties and modify/create them
        for prop, value in propDict.items():
            try:
                propVal = writeCommands.extractProperties(objStr, [prop])
            except PropNotInObjError:
                # If the property doesn't exist, append it to end of object
                
                # Determine if linesep is necessary before appended line
                if objStr[-2] == '\n':
                    preSep = ''
                else:
                    preSep = '\n'
                    
                # Determine if linesep is necessary after appended line.
                # It would appear GridLAB-D requires closing curly brace to 
                # be on its own line.
                if objStr[-1] == '}':
                    postSep = '\n'
                else:
                    postSep = ''
                    
                objStr = (objStr[0:-1] + preSep + prop + " " + str(value)
                          + ";" + postSep + objStr[-1])
            else:
                # Replace previous property value with this one
                objStr = (objStr[0:propVal[prop]['start']] + str(value)
                          + objStr[propVal[prop]['end']:])
                         
        # Return the modified object
        return objStr
                
    def extractObject(self, objMatch):
        """"Function to a GridLAB-D object from the larger model as a string.
        
        objMatch is a match object returned from the re package after calling
            re.search or one member of re.finditer.
        
        OUTPUT:
        dict with three fields: 'start,' 'end,' and 'obj'
            start indicates the starting index of the object in the full model
            end indicates the ending index of the object in the full model
            
        """
        
        # Extract the starting index of the regular expression match.
        startInd =  objMatch.span()[0]
        # Initialize the ending index (to be incremented in loop).
        endInd = startInd
        # Initialize counter for braces (to avoid problems with nested objects)
        braceCount = 0
        
        for c in self.strModel[startInd:]:
            # Increment the index
            endInd += 1
            
            # To avoid troubles with nested objects, keep track of braces
            if c == '{':
                braceCount += 1
            elif c == '}':
                braceCount -= 1
                
            # Break loop if c is a closing curly brace. Since the index is
            # incremented first, we ensure the closing bracket is included.
            if c == '}' and braceCount == 0:
                break
            
        # We now know the range of this object. Extract it.
        objStr = self.strModel[startInd:endInd]
        out = {'start':startInd, 'end':endInd, 'obj':objStr}
        return out
    
    @staticmethod
    def extractProperties(objString, props):
        """"Function to extract properties from a string of an object.
        
        INPUTS:
            objString: string representing object
            props: list of desired properties to extract
            
        OUTPUT: 
            dict mapping props to extracted values
        """
        # Initialize return
        outDict = dict()
        
        # Loop over the properties
        for p in props:
            # Create regular expression to extract the property after the 
            # property name and before the semi-colon.
            exp = r'(?<=\b' + p + r'\b\s)(.*?)(?=;)'
            prop = re.search(exp, objString)
            
            # If the property was not found, raise an exception.
            # TODO: make exception better
            if not prop:
                raise PropNotInObjError(obj = objString, prop = p)
            
            # Get property value and assign to output dictionary
            propStr = prop.group().strip()
            outDict[p] = {'prop': propStr, 'start': prop.span()[0],
                          'end': prop.span()[1]} 
            
        return outDict

class Error(Exception):
    """"Base class for exceptions in this module"""
    pass

class ObjNotFoundError(Error):
    """"Exception raised if requested object doesn't exist in model
    
    Attributes:
        obj: requested object
        model: model file in question
        message: simple message
    """
    def __init__(self, obj, model):
        self.obj = obj
        self.model = model
        self.message = "The object '" + obj + "' doesn't exist in " + model
        
    def __str__(self):
        return(repr(self.message))
        
class PropNotInObjError(Error):
    """"Exception raised if an object doesn't have the property required
    
    Attributes:
        obj: object in which a property is being looked for in
        prop: property being searched for in an object
        model: model file being searched
        message: simple message
    """
    def __init__(self, obj, prop, model=''):
        self.obj = obj
        self.prop = prop
        self.model = model
        self.message = ("The property '" + prop + "' doesn't exist in "
                        + "the object '" + obj + "' in the model " + model)
            
    def __str__(self):
        return(repr(self.message))

if __name__ == "__main__":
    inPath = 'C:/Users/thay838/Desktop/R2-12.47-2.glm'
    strModel = readModel(inPath)
    obj = writeCommands(strModel=strModel, pathModelIn=inPath, pathModelOut='C:/Users/thay838/Desktop/vvo/R2-12.47-2-copy.glm')
    obj.commandRegulators(regulators={'R2-12-47-2_reg_1': {'regulator': {'tap_A':1, 'tap_B':2, 'tap_C':3}, 'configuration': {'Control': 'MANUAL'}}, 'R2-12-47-2_reg_2': {'regulator': {'tap_A':4, 'tap_B':5, 'tap_C':6}, 'configuration': {'Control': 'MANUAL'}}})
    obj.commandCapacitors(capacitors={'R2-12-47-2_cap_1': {'switchA':'OPEN', 'switchB':'CLOSED', 'control': 'MANUAL'}, 'R2-12-47-2_cap_4': {'switchA':'CLOSED', 'switchB':'CLOSED', 'switchC': 'OPEN', 'control': 'MANUAL'}})
    obj.updateClock(start="'2015-03-01 05:15:00'", stop="'2015-03-01 05:30:00'")
    obj.removeTape()
    obj.addMySQL()
    obj.recordSwing()
    obj.writeModel()