'''
Created on Jan 6, 2017

@author: fish334
@author: poorva1209
'''
import fncs
import json
import yaml
import sys
import time
import stomp
from datetime import datetime

input_from_goss_topic = '/topic/goss/gridappsd/fncs/input' #this should match GridAppsDConstants.topic_FNCS_input
input_from_goss_queue = '/queue/goss/gridappsd/fncs/input' #this should match GridAppsDConstants.topic_FNCS_input

output_to_goss_topic = '/topic/goss/gridappsd/fncs/output' #this should match GridAppsDConstants.topic_FNCS_output
output_to_goss_queue = '/queue/goss/gridappsd/fncs/output' #this should match GridAppsDConstants.topic_FNCS_output
gossConnection= None
isInitialized = False 
simulationId = None

class GOSSListener(object):
    def on_message(self, headers, msg):
        message = {}
        try:
            message_str = 'received message '+str(msg)
            if fncs.is_initialized():
                _sendSimulationStatus('running', message_str, 'debug')
            else:
                _sendSimulationStatus('started', message_str, 'debug')
            jsonmsg = yaml.safe_load(str(msg))
            if jsonmsg['command'] == 'isInitialized':
                message_str = 'isInitialized check: '+str(isInitialized)
                if fncs.is_initialized():
                    _sendSimulationStatus('running', message_str, 'debug')
                else:
                    _sendSimulationStatus('started', message_str, 'debug')
                message['command'] = 'isInitialized'
                message['response'] = str(isInitialized)
                if (simulationId != None):
                    message['output'] = _getFncsBusMessages(simulationId)
                message_str = 'Added isInitialized output, sending message '+str(message)+' connection '+str(gossConnection)
                if fncs.is_initialized():
                    _sendSimulationStatus('running', message_str, 'debug')
                else:
                    _sendSimulationStatus('started', message_str, 'debug')
    
                gossConnection.send(output_to_goss_topic , json.dumps(message))
                gossConnection.send(output_to_goss_queue , json.dumps(message))
            elif jsonmsg['command'] == 'update':
                message['command'] = 'update'
                _publishToFncsBus(simulationId, json.dumps(jsonmsg['message'])) #does not return
            elif jsonmsg['command'] == 'nextTimeStep':
                message_str = 'is next timestep'
                _sendSimulationStatus('running', message_str, 'debug')
                message['command'] = 'nextTimeStep'
                currentTime = jsonmsg['currentTime']
                message_str = 'incrementing to '+str(currentTime)
                _sendSimulationStatus('running', message_str, 'debug')
                _doneWithTimestep(currentTime) #currentTime is incrementing integer 0 ,1, 2.... representing seconds
                message_str = 'done with timestep '+str(currentTime)
                _sendSimulationStatus('running', message_str, 'debug')
                message_str = 'simulation id '+str(simulationId)
                _sendSimulationStatus('running', message_str, 'debug')
                message['output'] = _getFncsBusMessages(simulationId)
                responsemsg = json.dumps(message)
                message_str = 'sending fncs output message '+str(responsemsg)
                _sendSimulationStatus('running', message_str, 'debug')
    
                gossConnection.send(output_to_goss_topic , responsemsg)
                gossConnection.send(output_to_goss_queue , responsemsg)
            elif jsonmsg['command'] == 'stop':
                message_str = 'Stopping the simulation'
                _sendSimulationStatus('stopped', message_str, 'info')
                fncs.die()
                sys.exit()
                
        except Exception as e:
            message_str = 'Error in command '+str(e)
            _sendSimulationStatus('error', message_str, 'error')
            if fncs.is_initialized():
                fncs.die()
           
        
    def on_error(self, headers, message):
        message_str = 'Error in goss listener '+str(message)
        _sendSimulationStatus('error', message_str, 'error')
        if fncs.is_initialized():
            fncs.die()
    
    
    def on_disconnected(self):
        if fncs.is_initialized():
            fncs.die()
  
  
def _registerWithFncsBroker(brokerLocation='tcp://localhost:5570'):
    '''Register with the fncs_broker and return.
    
    Function arguments:
        brokerLocation -- Type: string. Description: The ip location and port
            for the fncs_broker. It must not be an empty string.
            Default: 'tcp://localhost:5570'.
    Function returns:
        None.
    Function exceptions:
        RuntimeError()
        ValueError()
    '''
    global isInitialized
    try:
        message_str = 'Registering with FNCS broker '+str(simulationId)+' and broker '+brokerLocation
        _sendSimulationStatus('started', message_str, 'info')
        
        message_str = 'still connected to goss 1 '+str(gossConnection.is_connected())
        _sendSimulationStatus('started', message_str, 'info')
        if simulationId == None or simulationId == '' or type(simulationId) != str:
            raise ValueError(
                'simulationId must be a nonempty string.\n'
                + 'simulationId = {0}'.format(simulationId))
    
        if (brokerLocation == None or brokerLocation == ''
                or type(brokerLocation) != str):
            raise ValueError(
                'brokerLocation must be a nonempty string.\n' 
                + 'brokerLocation = {0}'.format(brokerLocation))
        fncsConfiguration = {
            'name' : 'FNCS_GOSS_Bridge_' + simulationId,
            'time_delta' : '1s',
            'broker' : brokerLocation,
            'values' : {
                simulationId : {
                    'topic' : simulationId + '/fncs_output',
                    'default' : '{}',
                    'type' : 'JSON',
                    'list' : 'false'
                }
            }
        }  
    
        
        configurationZpl = ('name = {0}\n'.format(fncsConfiguration['name'])
            + 'time_delta = {0}\n'.format(fncsConfiguration['time_delta'])
            + 'broker = {0}\nvalues'.format(fncsConfiguration['broker']))
        for x in fncsConfiguration['values'].keys():
            configurationZpl += '\n    {0}'.format(x)
            configurationZpl += '\n        topic = {0}'.format(
                fncsConfiguration['values'][x]['topic'])
            configurationZpl += '\n        default = {0}'.format(
                fncsConfiguration['values'][x]['default'])
            configurationZpl += '\n        type = {0}'.format(
                fncsConfiguration['values'][x]['type'])
            configurationZpl += '\n        list = {0}'.format(
                fncsConfiguration['values'][x]['list'])
        fncs.initialize(configurationZpl)
        
        isInitialized = fncs.is_initialized()
        if isInitialized:
            message_str = 'Registered with fncs '+str(isInitialized)
            _sendSimulationStatus('running', message_str, 'info')
    
    
    except Exception as e:
        message_str = 'Error while registering with fncs broker '+str(e)
        _sendSimulationStatus('error', message_str, 'error')
        if fncs.is_initialized():
            fncs.die()

    if not fncs.is_initialized():
        message_str = 'fncs.initialize(configurationZpl) failed!\n' + 'configurationZpl = {0}'.format(configurationZpl)
        _sendSimulationStatus('error', message_str, 'error')
        if fncs.is_initialized():
            fncs.die()
        raise RuntimeError(
            'fncs.initialize(configurationZpl) failed!\n'
            + 'configurationZpl = {0}'.format(configurationZpl))
        
    
def _publishToFncsBus(simulationId, gossMessage):
    '''publish a message received from the GOSS bus to the FNCS bus.
    
    Function arguments:
        simulationId -- Type: string. Description: The simulation id. 
            It must not be an empty string. Default: None.
        gossMessage -- Type: string. Description: The message from the GOSS bus
            as a json string. It must not be an empty string. Default: None.
    Function returns:
        None.
    Function exceptions:
        RuntimeError()
        ValueError()
    '''
    message_str = 'publish to fncs bus '+simulationId+' '+str(gossMessage)
    _sendSimulationStatus('running', message_str, 'debug')

    if simulationId == None or simulationId == '' or type(simulationId) != str:
        raise ValueError(
            'simulationId must be a nonempty string.\n'
            + 'simulationId = {0}'.format(simulationId))
    if gossMessage == None or gossMessage == '' or type(gossMessage) != str:
        raise ValueError(
            'gossMessage must be a nonempty string.\n'
            + 'gossMessage = {0}'.format(gossMessage))
    if not fncs.is_initialized():
        raise RuntimeError(
            'Cannot publish message as there is no connection'
            + ' to the FNCS message bus.')
    try:
        testGossMessageFormat = yaml.safe_load(gossMessage)
        if type(testGossMessageFormat) != dict:
            raise ValueError(
                'gossMessage is not a json formatted string.'
                + '\ngossMessage = {0}'.format(gossMessage))
    except ValueError as ve:
        raise ValueError(ve)
    except:
        raise RuntimeError(
            'Unexpected error occured while executing yaml.safe_load(gossMessage'
            + '{0}'.format(sys.exc_info()[0]))
    fncsInputTopic = '{0}/fncs_input'.format(simulationId)
    message_str = 'fncs input topic '+fncsInputTopic
    _sendSimulationStatus('running', message_str, 'debug')
    fncs.publish_anon(fncsInputTopic, gossMessage)
    
    
def _getFncsBusMessages(simulationId):
    '''publish a message received from the GOSS bus to the FNCS bus.
    
    Function arguments:
        simulationId -- Type: string. Description: The simulation id. 
            It must not be an empty string. Default: None.
    Function returns:
        fncsOutput -- Type: string. Description: The json structured output
            from the simulation. If no output was sent from the simulation then
            it returns None.
    Function exceptions:
        ValueError()
    '''
    try:
        simulatorMessageOutput = None
        if simulationId == None or simulationId == '' or type(simulationId) != str:
            raise ValueError(
                'simulationId must be a nonempty string.\n'
                + 'simulationId = {0}'.format(simulationId))
        message_str = 'about to get fncs events'
        _sendSimulationStatus('running', message_str, 'debug')
        messageEvents = fncs.get_events()
        message_str = 'fncs events '+str(messageEvents)
        _sendSimulationStatus('running', message_str, 'debug')
        if simulationId in messageEvents:
            simulatorMessageOutput = fncs.get_value(simulationId)
        message_str = 'simulatorMessageOutput '+str(simulatorMessageOutput)
        _sendSimulationStatus('running', message_str, 'debug')
        return simulatorMessageOutput
    except Exception as e:
        message_str = 'Error on get FncsBusMessages for '+str(simulationId)+' '+str(e)
        _sendSimulationStatus('error', message_str, 'error')
        
        
def _doneWithTimestep(currentTime):
    '''tell the fncs_broker to move to the next time step.
    
    Function arguments:
        currentTime -- Type: integer. Description: the current time in seconds. 
            It must not be none.
    Function returns:
        None.
    Function exceptions:
        RuntimeError()
        ValueError()
    '''
    try:
        message_str = 'In done with timestep '+str(currentTime)
        _sendSimulationStatus('running', message_str, 'debug')
        if currentTime == None or type(currentTime) != int:
            raise ValueError(
                'currentTime must be an integer.\n'
                + 'currentTime = {0}'.format(currentTime))
        timeRequest = currentTime + 1
        message_str = 'calling time_request '+str(timeRequest)
        _sendSimulationStatus('running', message_str, 'debug')
        timeApproved = fncs.time_request(timeRequest)
        message_str = 'time approved '+str(timeApproved)
        _sendSimulationStatus('running', message_str, 'debug')
        if timeApproved != timeRequest:
            raise RuntimeError(
                'The time approved from fncs_broker is not the time requested.\n'
                + 'timeRequest = {0}.\ntimeApproved = {1}'.format(timeRequest, 
                timeApproved))
    except Exception as e:
        message_str = 'Error in fncs timestep '+str(e)
        _sendSimulationStatus('error', message_str, 'error')
        
            
def _registerWithGOSS(simId,username,password,gossServer='localhost', 
                      stompPort='61613',):
    '''Register with the GOSS server broker and return.
    
    Function arguments:
        simId -- Type: string. Description: The simulation id.
            It must not be an empty string. Default: None.
        gossServer -- Type: string. Description: The ip location
        for the GOSS server. It must not be an empty string.
            Default: 'localhost'.
        stompPort -- Type: string. Description: The port for Stomp 
        protocol for the GOSS server. It must not be an empty string.
            Default: '61613'.
        username -- Type: string. Description: User name for GOSS connection.
        password -- Type: string. Description: Password for GOSS connection.
        
    Function returns:
        None.
    Function exceptions:
        RuntimeError()
    '''
    global simulationId
    simulationId = simId
    if (gossServer == None or gossServer == ''
            or type(gossServer) != str):
        raise ValueError(
            'gossServer must be a nonempty string.\n' 
            + 'gossServer = {0}'.format(gossServer))
    if (stompPort == None or stompPort == ''
            or type(stompPort) != str):
        raise ValueError(
            'stompPort must be a nonempty string.\n' 
            + 'stompPort = {0}'.format(stompPort))
   
    gossConnection = stomp.Connection12([(gossServer, stompPort)])
    gossConnection.start()
    gossConnection.connect(username,password, wait=True)
    gossConnection.set_listener('GOSSListener', GOSSListener())
    gossConnection.subscribe(input_from_goss_topic,1)
    gossConnection.subscribe(input_from_goss_queue,2)

    message_str = 'Registered with GOSS on topic '+input_from_goss_topic+' '+str(gossConnection.is_connected())
    _sendSimulationStatus('started', message_str, 'info')
    
    
def _sendSimulationStatus(status, message, log_level):
    '''send a status message to the GridAPPS-D log manager
    
    Function arguments:
        status -- Type: string. Description: The status of the simulation.
            Default: 'localhost'.
        stompPort -- Type: string. Description: The port for Stomp 
        protocol for the GOSS server. It must not be an empty string.
            Default: '61613'.
        username -- Type: string. Description: User name for GOSS connection.
        password -- Type: string. Description: Password for GOSS connection.
        
    Function returns:
        None.
    Function exceptions:
        RuntimeError()
    '''
    simulation_status_topic = "goss.gridappsd.process.log.simulation"
    valid_status = ['started', 'stopped', 'running', 'error', 'passed', 'failed']
    valid_level = ['info', 'debug', 'error']
    if status in valid_status:
        if log_level not in valid_level:
            log_level = 'info'
        t_now = datetime.utcnow()
        status_message = {
            "timestamp" : t_now.replace(microsecond=0).isoformat(" "),
            "status" : status,
            "log_message" : str(message),
            "log_level" : log_level,
            "simulation_id" : str(simulationId)
        }
        status_str = json.dumps(status_message)
        print(status_str)
        gossConnection.send(simulation_status_topic, status_str)
        

def _keepAlive():
    while 1:
        time.sleep(0.1)
         
def _main(simulationId):
    
    _registerWithGOSS(simulationId,'system','manager',gossServer='127.0.0.1',stompPort='61613')
    _registerWithFncsBroker('tcp://localhost:5570')
    _keepAlive()
        
if __name__ == "__main__":
    #TODO: send simulationId, fncsBrokerLocation, gossLocation, 
    #stompPort, username and password as commmand line arguments
    simulationId = sys.argv[1]
    _main(simulationId) 
    
