'''
Module to write setpoint commands to a GridLAB-D model (.glm)

NOTE: This module is in 'prototype' mode. As such, it certainly isn't
optimally efficient.

Created on Jul 27, 2017

@author: thay838

TODO: This class is currently really I/O heavy, as it reads/writes files.
    This could be improved by doing one read and one write, and keeping the
    model completely in memory. This could be expensive if there are lots of 
    'agents' performing this simultaneously
'''
import re

def readModel(modelIn):
    '''Simple function to read file as string'''
    with open(modelIn, 'r') as f:
        s = f.read()
    return s

class writeCommands:
    '''Class for reading GridLAB-D model and changing setpoints in voltage
        regulating equipment (OLTC, caps, etc.)
    '''
    # Define some constants for GridLAB-D model parsing.
    REGOBJ_REGEX = r'\bobject\b\s\bregulator\b'
    REGCONF_REGEX = r'\bobject\b\s\bregulator_configuration\b'
    TAP = 'tap_pos_'
    PHASES = ['A', 'B', 'C']
    REGTAPS = ['tap_pos_' + p for p in PHASES]
    
    # Regular expression to match -99 through 99. We don't know how many taps
    # a regulator will have, but three digits simply wouldn't be reasonable.
    TAP_EXP = '-?0*([1-8][0-9]|9[0-9]|[0-9])'
    
    def __init__(self, strModel, pathModelOut, pathModelIn=''):
        '''Initialize class with input/output GridLAB-D models
        
        strModel should be a string representing a GridLAB-D model.
            Can be obtained by calling readModel function in this module.
            It will be modified to send new commands.
            
        pathModelOut is the path to the new output model.
        '''

        # Set properties.
        self.strModel = strModel
        self.pathModelIn = pathModelIn
        self.pathModelOut = pathModelOut
        
    def writeModel(self):
        '''Simple method to write strModel to file'''
        
        with open(self.pathModelOut, 'w') as f:
            f.write(self.strModel)
    
    def commandRegulators(self, regulators):
        '''Function to change tap positions on a given regulator.
        
        This is performed by finding its configuration and changing 'tap_pos.'
        '''
        
        # First, find all regulators
        regIterator = re.finditer(writeCommands.REGOBJ_REGEX, self.strModel)
        
        # Loop through regulators to find names and configs.
        # Note that this could be more efficient, but oh well.
        regDict = dict()
        for it in regIterator:
            # Extract the object:
            reg = self.extractObject(it)
            
            # Extract name and configuration properties and assign to dict
            d = writeCommands.extractProperties(reg['obj'],
                                                ['name', 'configuration'])
            regDict[d['name']['obj']] = d
            
        # Find the configurations for the requested regulators and put in list.
        # NOTE: confList and regList MUST be one to one.
        confList = []
        regList = []
        for r in regulators:
            # If we didn't find it, raise an exception
            if r not in regDict:
                raise ObjNotFoundError(obj=r, model=self.pathModelIn)
            
            # Extract the name of the configuration, put in configuration list
            confList.append(regDict[r]['configuration']['obj'])
            # Put the regulator in the list
            regList.append(r)
            
        # Next, find all regulator configurations
        regConfIterator = re.finditer(writeCommands.REGCONF_REGEX, 
                                      self.strModel)
        
        for it in regConfIterator:
            # Extract the object
            regConf = self.extractObject(it)
            
            # Extract the name
            d = writeCommands.extractProperties(regConf['obj'], ['name'])
            
            # If the regulator is in our configuration list, alter taps.
            if d['name']['obj'] in confList:
                # Get the name of the regulator to command
                regInd = confList.index(d['name']['obj'])
                regName = regList[regInd]
                
                # Loop through the commands and modify the taps
                for phase, position in regulators[regName].items():
                    # Find the tap to change
                    tapStr = writeCommands.TAP + phase
                    tap = writeCommands.extractProperties(regConf['obj'], 
                                                          [tapStr])
                    
                    # Modify the regulator configuration by replacing the 
                    # previous tap position with the new commanded position
                    posStart = tap[tapStr]['start']
                    posEnd = tap[tapStr]['end']
                    regConf['obj'] = regConf['obj'][0:posStart] + str(position) \
                                 + regConf['obj'][posEnd:]
                
                # Regulator configuration has been updated, now update model
                confStart = regConf['start']
                confEnd = regConf['end']
                self.strModel = self.strModel[0:confStart] + regConf['obj'] \
                                + self.strModel[confEnd:]
                
    def extractObject(self, regMatch):
        '''Function to a GridLAB-D object from the larger model as a string.
        
        regMatch is a match object returned from the re package after calling
            re.search or one member of re.finditer.
        
        HUGE ASSUMPTION: This will break down if there are nested objects in
            an object.
            
        TODO: Make this robust enough to handle nested objects
        
        OUTPUT:
        dict with three fields: 'start,' 'end,' and 'obj'
            start indicates the starting index of the object in the full model
            end indicates the ending index of the object in the full model
            
        '''
        # ASSUMPTION: no nested objects in regulator configurations.
        startInd =  regMatch.span()[0]
        endInd = startInd
        
        for c in self.strModel[startInd:]:
            # Increment the index
            endInd += 1
            
            # Break loop if c is a closing curly brace. Since the index is
            # incremented first, we ensure the closing bracket is included.
            if c == '}':
                break
            
        # We now know the range of this object. Extract it.
        objStr = self.strModel[startInd:endInd]
        out = {'start':startInd, 'end':endInd, 'obj':objStr}
        return out
    
    @staticmethod
    def extractProperties(objString, props):
        '''Function to extract properties from a string of an object.
        
        INPUTS:
            objString: string representing object
            props: list of desired properties to extract
            
        OUTPUT: 
            dict mapping props to extracted values
        '''
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
                raise Exception
            
            # Get property value and assign to output dictionary
            propStr = prop.group().strip()
            outDict[p] = {'obj': propStr, 'start': prop.span()[0],
                          'end': prop.span()[1]} 
            
        return outDict
    
    def commandReg(self):
        pass

class Error(Exception):
    '''Base class for exceptions in this module'''
    pass

class ObjNotFoundError(Error):
    '''Exception raised if requested object doesn't exist in model
    
    Attributes:
        obj: requested object
        model: model file in question
        message: simple message
    '''
    def __init__(self, obj, model):
        self.obj = obj
        self.model = model
        self.message = "The object '" + obj + "' doesn't exist in " + model
        
    def __str__(self):
        return(repr(self.message))
        
class PropertyNotInObject(Error):
    '''Exception raised if an object doesn't have the property required
    
    Attributes:
        obj: object in which a property is being looked for in
        prop: property being searched for in an object
        model: model file being searched
        message: simple message
    '''
    def __init__(self, obj, prop, model):
        self.obj = obj
        self.prop = prop
        self.model = model
        self.message = "The property '" + prop + "' doesn't exist in " + \
            "the object '" + obj + "' in the model " + model
            
    def __str__(self):
        return(repr(self.message))

inPath = 'C:/Users/thay838/Desktop/R3-12.47-2.glm'
strModel = readModel(inPath)         
obj = writeCommands(strModel=strModel, pathModelIn=inPath, pathModelOut='C:/Users/thay838/Desktop/R3-12.47-2-copy.glm')
obj.commandRegulators(regulators={'R3-12-47-2_reg_1': {'A':1, 'B':2, 'C':3}})
obj.writeModel()