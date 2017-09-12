"""
Module to write setpoint commands to a GridLAB-D model (.glm)

NOTE: This module is in 'prototype' mode. As such, it certainly isn't
optimally efficient.

Created on Jul 27, 2017

@author: thay838

"""

import re
import os

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
METER_REGEX = re.compile(r'\bobject\b(\s+)\bmeter\b')
SUBSTATION_REGEX = re.compile(r'\bobject\b(\s+)\bsubstation\b')
PROFILER_REGEX = re.compile(r'#(\s*)\bset\b(\s+)\bprofiler\b(\s*)=(\s*)[01]')
SUPPRESS_REGEX = re.compile(r'#(\s*)\bset\b(\s+)\bsuppress_repeat_messages\b(\s*)=(\s*)[01]')
POWERFLOW_REGEX = re.compile((r'\bmodule\b(\s+)\bpowerflow\b'))
CONTROL_REGEX = re.compile(r'(?<=(c|C)ontrol(\s))(\s*)(m|M)(a|A)(n|N)(u|U)(a|A)(l|L)(\s*);(\s*)//')
COMMENT_REGEX = re.compile(r'(\S+);')
TRIPLEX_LOAD_REGEX = re.compile(r'\bobject\b(\s+)\btriplex_load\b')
TRIPLEX_METER_REGEX = re.compile(r'\bobject\b(\s+)\btriplex_meter\b')
OBJEND_REGEX = re.compile(r'(\s*)}(\s*)(;*)$')

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
        # Find first regulator
        regMatch = REGOBJ_REGEX.search(self.strModel)
        
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
            regMatch = REGOBJ_REGEX.search(self.strModel, regEndInd)
            
            
            
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
        regConfMatch = REGCONF_REGEX.search(self.strModel)
        
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
            regConfMatch = REGCONF_REGEX.search(self.strModel, regEndInd)
        
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
        capMatch = CAP_REGEX.search(self.strModel)
        
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
            capMatch = CAP_REGEX.search(self.strModel, capEndInd)
            
    def updateClock(self, starttime=None, stoptime=None, timezone=None):
        """Function to set model time. If there's no clock object, it will be
            created.
        
        INPUTS:
            start: simulation start time (starttime in GLD). Should be
                surrounded in single quotes, and be in the format
                'yyyy-mm-dd HH:MM:SS'
            stop: simulation stop time (stoptime in GLD). Same format as start.
            
            NOTE: Timezones can be included in start and stop.
        """
        # Extract a timezone string.
        # TODO: this is AWFUL hard-coding.
        if timezone:
            tzStr = ' ' + timezone[-3:]
        else:
            tzStr = ''
            
        # Look for clock object
        clockMatch = CLOCK_REGEX.search(self.strModel)
        if clockMatch is not None:
            # If clock object exists, extract it.
            clock = self.extractObject(clockMatch)
            
            # Fill out dictionary of included properties
            propDict = dict()
            if timezone:
                propDict['timezone'] = timezone
                
            if starttime:
                propDict['starttime'] = "'{}{}'".format(starttime, tzStr)
                
            if stoptime:
                propDict['stoptime'] = "'{}{}'".format(stoptime, tzStr)
                
            # Modify the times
            clock['obj'] = self.modObjProps(clock['obj'], propDict)
        
            # Splice in new clock object.
            self.replaceObject(clock)
        else:
            # If clock doesn't exist, create it.
            clockStr = "clock {\n"
            
            # Add defined properties.
            if timezone:
                clockStr += "  timezone {};\n".format(timezone)
                
            if starttime:
                clockStr += "  starttime '{}{}';\n".format(starttime,tzStr)
                
            if stoptime:
                clockStr += "  stoptime '{}{}';\n".format(stoptime,tzStr)
                
            clockStr += "}\n"
            
            # Add clock to model
            self.strModel = clockStr + self.strModel
            
    def updatePowerflow(self, solver_method='NR', line_capacitance='TRUE',
                        lu_solver='"KLU"'):
        """Update powerflow module or create it if it doesn't exit.
        
        INPUTS: 
            solver_method: FBS or NR. GS is deprecated. 
            line_capacitance: TRUE or FALSE
            lu_solver: Third party solver. KLU is fast. Download from here:
                https://github.com/gridlab-d/tools/tree/master/solver_klu
                To use native solver, pass None for lu_solver
        """
        pfMatch = POWERFLOW_REGEX.search(self.strModel)
        if pfMatch is not None:
            # If powerflow module definition exists, extract it.
            pf = self.extractObject(pfMatch)
            # Modify the properties
            propDict = {'solver_method': solver_method,
                        'line_capacitance': line_capacitance}
            if lu_solver:
                propDict['lu_solver'] = lu_solver
                
            pf['obj'] = self.modObjProps(pf['obj'],
                                         {'solver_method': solver_method,
                                          'line_capacitance': line_capacitance}
                                         )
            # Splice in new powerflow object
            self.replaceObject(pf)
            
        else:
            # Create module string.
            s = (
                "module powerflow {{\n"
                "  solver_method {solver_method};\n"
                "  line_capacitance {line_capacitance};\n"
                ).format(solver_method=solver_method,
                         line_capacitance=line_capacitance)
                
            if lu_solver:
                s += '  lu_solver {};\n'.format(lu_solver)
                
            s += "};\n"
                
            # Add to model.
            self.strModel = s + self.strModel
            
    def addModule(self, module):
        """Super simple function to add simple module definition to model.
        """
        self.strModel = 'module {};\n'.format(module) + self.strModel
        
    def removeTape(self):
        """Method to remove tape module from model."""
        # First, look for simple version: 'module tape;'
        tapeMatch = TAPE_REGEX1.search(self.strModel)
        
        # If simple version is found, eliminate it
        if tapeMatch is not None:
            s = tapeMatch.span()[0]
            e = tapeMatch.span()[1]
            self.strModel = self.strModel[0:s] + self.strModel[e+1:]
        else:
            # Find "more full" definition of tape
            tapeMatch = TAPE_REGEX2.search(self.strModel)
            if tapeMatch is not None:
                # Extract the object
                tapeObj = self.extractObject(tapeMatch)
                # Eliminate the object
                self.strModel = (self.strModel[0:tapeObj['start']]
                                 + self.strModel[tapeObj['end']+1:])
                
    def addDatabase(self, hostname='localhost', username='gridlabd',
                 password='', schema='gridlabd', port='3306',
                 socketname='/tmp/mysql.sock', tz_offset=0):
        """Method to add mysql database connection to model
        
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
            "object database {{\n"
            '   hostname "{host}";\n'
            '   username "{usr}";\n'
            '   password "{pwd}";\n'
            '   schema "{schema}";\n'
            '   port {port};\n'
            '   tz_offset {tz_offset};\n'
            ).format(host=hostname, usr=username, pwd=password, schema=schema,
                     port=port, tz_offset=tz_offset)
        # If we're on Mac or Linux, need to include the sockenamae
        if os.name == 'posix':
            dbStr = (dbStr + 'socketname "{sock}";\n').format(sock=socketname)
            
        self.strModel = dbStr + '}\n' + self.strModel
        
    def addClass(self, className, properties):
        """Method to add a custom class to the beginning of a .gld model
        
        INPUTS:
            className: class definition --> object className {
            properties: dictionary of names mapped to types. e.g. 
                {'value': 'double'}
        """
        # Build the start of the string 
        s = "class {} {{\n".format(className)
        
        # Loop over the dict and build up the string
        for n, t in properties.items():
            s += '  {} {};\n'.format(t, n)
            
        # Add string to model
        self.strModel = s + '}\n' + self.strModel
        
    def addObject(self, objType, properties, place='beginning', objStr=None):
        """Method to add an object to a .gld model
        
        INPUTS:
            objType: object defition --> object objType  {
            properties: dictionary of properties mapped to their values. e.g.
                {'name': 'zipload_schedule'}
            place: 'beginning' or 'end' --> this is where object will be placed
            objStr: if objStr is none, object will be added to self.strModel.
                Otherwise, object will be nested into the given string object.
                
        NOTE: 'place' input does nothing if objStr is not None. Nested object
            will always be placed at the end of objStr
        """
        # Build the start of the string
        s = "object {} {{\n".format(objType)
        
        # Loop over the dict and build up the model
        for prop, val in properties.items():
            s += '  {} {};\n'.format(prop, val)
         
        # Close object. Include semi-colon just in case.    
        s += '};\n'
        
        if not objStr:
            # Add string to model
            if place == 'beginning':
                self.strModel = s +  self.strModel
            elif place == 'end':
                self.strModel = self.strModel + '\n' + s
            else:
                assert False, ("'place' inputs must be 'beginning' "
                               "or 'end.' '{}' was given.".format(place))
            return None
        else:
            # Add string to the end of the object.
            # Find the end.
            e = OBJEND_REGEX.search(objStr)
            # Splice new object into old object
            strOut = objStr[:e.span()[0]] + '\n' + s + e.group(0)
            return strOut
        
    def addTapePlayer(self, name, file, parent=None, prop=None, loop=0):
        """Method to add a player from the tape module to a model.
        
        INPUTS: see recorder in GridLAB-D Wiki. prop short for property -->
            (Python reserverd keyword)
        """
        # Build string
        s = (
            "object tape.player {{\n"
            "  name {name};\n"
            '  file "{file}";\n'
            "  loop {loop};\n"
            ).format(name=name, file=file, loop=loop)
        
        # Add parent if given.
        if parent:
            s += "  parent {};\n".format(parent)
        
        # Add property if given.
        if prop:
            s += "  property {};\n".format(prop)
        
        # Close the object.
        s += "}\n"
        
        # If neither parent nor property are given, we need to expose the 
        # player's value.
        if (not parent) and (not prop):
            s = "class player {\n  double value;\n}\n" + s
        
        # Add to model
        self.strModel = s + self.strModel
        
    def addLine(self, line):
        """Simple method to add a line to a model.
        """
        self.strModel = line + '\n' + self.strModel
        
        
    def repeatMessages(self, val=1):
        """Method to set 'suppress_repeat_messages'
        
        TODO: unit test.
        """
        # See if the model already has the constant
        m = SUPPRESS_REGEX.search(self.strModel)
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
        m = PROFILER_REGEX.search(self.strModel)
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
        swingMatch = SWING_REGEX.search(self.strModel)
        
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
        swingIter = OBJ_REGEX.finditer(self.strModel, 0, sInd)
        
        # Loop over the iterator until the last one. This feels dirty.
        # TODO: Make this more efficient?
        for m in swingIter:
            pass
        
        # Extract the starting index of the object.
        sInd = m.span()[0]
        
        return {'name': sName['name']['prop'], 'start': sInd, 'end': eInd,
                'obj': self.strModel[sInd:eInd]}
    
    def recordSwing(self, interval=60, suffix=0):
        """Add recorder to model to record the power flow on the swing.
        
        NOTE: swing will be changed from node to meter if it's a node
        
        NOTE: This is for a database recorder.
        
        INPUTS:
            interval: interval (seconds) to record
            suffix: table name will be 'swing_' + suffix
        
        OUTPUT: dict in form of {'table': tableName,
                                 'swingColumns': swingColumns}
                where swingColumns are the names of the fields in the table.
        """
        # Find the name and indices of the swing.
        swing = self.findSwing()
        
        # Find out if the swing is a node, meter, or substation object
        nodeMatch = NODE_REGEX.match(swing['obj'])
        meterMatch = METER_REGEX.match(swing['obj'])
        substationMatch = SUBSTATION_REGEX.match(swing['obj'])
        
        if nodeMatch is not None:
            # Replace 'object node' with 'object meter'
            swing['obj'] = (swing['obj'][0:nodeMatch.span()[0]] 
                             + 'object meter' 
                             + swing['obj'][nodeMatch.span()[1]:])
            
            # Splice in the new object
            self.replaceObject(swing)
            
        # 
        if (nodeMatch is not None) or (meterMatch is not None):
            swingColumns = ['measured_power_A', 'measured_power_B',
                               'measured_power_C']
        elif substationMatch is not None:
            swingColumns = ['distribution_power_A', 'distribution_power_B',
                               'distribution_power_C']
        else:
            # Raise error
            raise UnexpectedSwingType()
        
        # Define table to use.
        table = 'swing_' + str(suffix)
        
        # Create recorder.
        self.addMySQLRecorder(parent=swing['name'], table=table,
                         properties=swingColumns,
                         interval=interval)
        
        # Return the name of the table and powerProperties
        out = {'table': table, 'columns': swingColumns, 'interval': interval}
        return out
    
    def recordTriplex(self, suffix, interval=60):
        """Method to add a recorder for each 'triplex_load' object.
        
        For now, this will only look for triplex_load objects and record the
        voltage_12 property.
        
        INPUTS:
            table: table to put recorded results in.
            interval: interval at which to record results.
        """
        # Define table name
        table = 'triplex_' + str(suffix)
        
        # Get iterator of triplex matches. Note that since we tack recorders
        # onto the end, we don't screw up the indices in each match's span
        # property.
        matches = TRIPLEX_LOAD_REGEX.finditer(self.strModel)
        
        # Loop over each triplex load, and add a recorder for each one.
        for m in matches:
            # Extract the object:
            tObj = self.extractObject(m)
            
            # Get the name
            n = self.extractProperties(tObj['obj'], ['name'])
            name = n['name']['prop']
            
            # Add a mysql recorder
            self.addMySQLRecorder(parent=name, table=table,
                                  properties=['voltage_12'], interval=interval)
            
        # Return the table name and columns
        return {'table': table, 'columns': ['voltage_12'], 'interval':interval}
    
    def addTriplexMeters(self):
        """Method to add a triplex_meter to each triplex_load
        """
        # Get iterator of triplex matches. Note that since we'll tack meters
        # onto the end, we don't screw up the indices in each match's span
        # property.
        matches = TRIPLEX_LOAD_REGEX.finditer(self.strModel)
        
        # Loop over each triplex load, and add a recorder for each one.
        for m in matches:
            # Extract the object:
            tObj = self.extractObject(m)
            
            # Get the name, phases, and nominal_voltage
            n = self.extractProperties(tObj['obj'], ['name', 'phases',
                                                     'nominal_voltage'])
            
            # Add a triplex meter to the end of the model
            self.addObject(objType='triplex_meter',
                           properties = {'name': n['name']['prop'] + '_meter',
                                         'parent': n['name']['prop'],
                                         'phases': n['phases']['prop'],
                                         'nominal_voltage': n['nominal_voltage']['prop']},
                           place='end')
            
        
    def addMySQLRecorder(self, parent, table, properties, interval,
                    header_fieldnames='name', options=None):
        """Method to add database recorder to end of model
        
        INPUTS:
            parent: name of object to record
            table: database table to save outputs in
            properties: list of properties to record. Ex: ['power_A',
                'power_B', 'power_C']
            interval: interval in seconds to record
            header_fieldnames: Specifies the header data to store in each
                record inserted. Valid fieldnames are "name", "class",
                "latitude", and "longitude"
            options: PURGE|UNITS - PURGE drops and recreates table on init.
                NOTE: Brandon found PURGE to be VERY slow experimentally for 
                multiple recorders sharing a table. Table should be truncated
                before use OUTSIDE of GridLAB-D. Maybe all this is true...
            
        TODO: Add more properties
        """
        # Add formatted string to end of model.
        recorder = ('\n'
                    'object mysql.recorder {{\n'
                    '  parent {parent};\n'
                    '  table "{table}";\n'
                    '  property {properties};\n'
                    '  interval {interval};\n'
                    '  header_fieldnames "{header_fieldnames}";\n'
                    ).format(parent=parent, table=table,
                             properties=('"' + ','.join(properties) + '"'),
                             interval=interval,
                             header_fieldnames=header_fieldnames)
        
        # Add options if included
        if options:
            recorder += '  options {options};\n'.format(options=options)
            
        self.strModel = self.strModel + recorder + '}'
            
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
                objStr = (objStr[0:-1] + "  " + prop + " " + str(value)
                          + ";\n" + objStr[-1])
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
            
            # Get property value and assign to output dictionary. Note that
            # we're stripping whitespace then quotes.
            propStr = prop.group().strip().strip('"')
            outDict[p] = {'prop': propStr, 'start': prop.span()[0],
                          'end': prop.span()[1]} 
            
        return outDict
    
    @staticmethod
    def addFileSuffix(inPath, suffix='', outDir=None):
        """Simple function to create a filepath to a file with _suffix 
            added and in the desired directory.
        """
        # Get the filename and extension of original file
        fParts = os.path.splitext(os.path.basename(inPath)) 
        
        # If outDir isn't included, make file path point to directory of inPath
        if outDir is None:
            outDir = os.path.dirname(inPath)
            
        if suffix:
            suffix = '_' + suffix
        
        # Define the output path by adding suffix    
        outPath = os.path.join(outDir, fParts[0] +  suffix + fParts[1])
        
        # Done.
        return outPath
    
    def setupModel(self, starttime=None, stoptime=None, timezone=None,
                   vSource=69715.065, playerFile=None, dbFlag=True,
                   tz_offset=0, profiler=0):
        """Function to add the basics to get a running model. Designed with 
        the output from Tom McDermott's CIM exporter in mind.
        
        NOTE: most of these functions tack lines onto the beginning of the
            file. That's why things might feel like they're in reverse order.
            
        TODO: Document inputs when this is done. Database inputs are going to
            need to be added.
        """
        # Add definition of source voltage
        self.addLine(line='#define VSOURCE={}'.format(vSource))
        
        # Add player details.
        if playerFile:
            # Add a player.
            self.addTapePlayer(name='zip_sched', parent='zipload_schedule',
                                   prop='value', file=playerFile, loop=1)
            # Add hacky object (working around mysql player not being able to 
            # expose 'value', and working around mysql + tape coexistence
            # issues)
            self.addObject(objType='pOut',
                           properties={'name': 'zipload_schedule'})
            # Add hacky class
            self.addClass(className='pOut', properties={'value': 'double'})
            
            # Since we're using a player file, we'll need the tape module.
            self.addModule('tape')
        
        # TODO: Get database inputs rather than just using the default.
        if dbFlag:
            self.addDatabase(tz_offset=tz_offset)
            # Add mysql modules
            self.addModule('mysql')
            
        # Update powerflow (add if it doesn't exist).
        self.updatePowerflow()
        # Ensure we're suppressing messages.
        self.repeatMessages()
        # Set profiler.
        self.toggleProfile(profiler)
        # Update the clock (add if it doesn't exist).
        if starttime or stoptime or timezone:
            self.updateClock(starttime=starttime, stoptime=stoptime,
                                 timezone=timezone)
        
    def switchControl(self):
        """If file has commented out control options, use them instead.
        Example: 'Control MANUAL; // OUTPUT_VOLTAGE;' will be replaced with
        'Control OUTPUT_VOLTAGE;'
        """
        # Find the first control match.
        m = CONTROL_REGEX.search(self.strModel)
        
        # Loop until we've found all CONTROL_REGEX instances.
        while m:
            # Get the indices of the match.
            sInd = m.span()[0]
            eInd = m.span()[1]
            # See if there is another control option after the comment
            m2 = COMMENT_REGEX.search(self.strModel, eInd)
            # If there's another control option, remove the first match to
            # splice in the new one
            if m2:
                self.strModel = self.strModel[:sInd] + self.strModel[eInd:]
                
            # Find the next match, starting with eInd
            # NOTE: starting with eInd may not be 100% robust, but it shouldn't
            # ever be a problem.
            m = CONTROL_REGEX.search(self.strModel, eInd)
            
    def addMetricsCollectorWriter(self, interval=60, suffix=''):
        """Function to add a metrics collector writer.
        """
        # Define filename
        if suffix:
            suffix = '_' + suffix
            
        fName = 'metrics' + suffix + '.json'
        
        # Create object
        self.addObject(objType='metrics_collector_writer', place='end',
                       properties={'interval': interval, 'filename': fName})
        
        # Return the name
        return fName
    
    def addMetricsCollectors(self, interval=60):
        """Function to add metrics collectors to all triplex_meter objects.
        
        If needed, this can easily be adopted to a different type of object.
        
        NOTE: Make sure to add meters first.
        """
        # Find the first triplex_meter.
        m = TRIPLEX_METER_REGEX.search(self.strModel)
        
        # Loop over all the matches
        while m:
            # Extract the object
            tObj = self.extractObject(m)
            
            # Splice in the metrics collector
            tObj['obj'] = self.addObject(objType='metrics_collector',
                                    properties = {'interval': interval},
                                    objStr=tObj['obj'])
            
            # Replace the previous object with the new one.
            self.replaceObject(tObj)
            
            # Get the next match, offsetting by length of new object.
            m = TRIPLEX_METER_REGEX.search(self.strModel,
                                          m.span()[0] + len(tObj['obj']))

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
    
class UnexpectedSwingType(Error):
    """Exception raised if the swing object isn't a node, meter, or substation.
    """
    def __init__(self):
        self.message = ("The given SWING object isn't a node, meter, or "
                        + "substation object. This wasn't anticipated.")
        
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