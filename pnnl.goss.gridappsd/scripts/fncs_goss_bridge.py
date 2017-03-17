'''
Created on Jan 6, 2017

@author: fish334
@author: poorva1209
'''
import fncs
import json
import sys
import stomp
import logging 

input_from_goss_topic = 'goss/gridappsd/fncs/input' #this should match GridAppsDConstants.topic_FNCS_input
output_to_goss_topic = 'goss/gridappsd/fncs/output' #this should match GridAppsDConstants.topic_FNCS_output
gossConnection= None
isInitialized = None
simulationId = None

logger = logging.getLogger('fncs_goss_bridge')
hdlr = logging.FileHandler('/var/log/fncs_goss_bridge.log')
formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
hdlr.setFormatter(formatter)
logger.addHandler(hdlr) 
logger.setLevel(logging.DEBUG)

class GOSSListener(object):
  def on_message(self, headers, msg):
    message = ''
    logger.info('received message '+msg)
    print(msg)
    if msg['command'] == 'isInitialized':
        message['response'] = isInitialized
        message['output'] = _getFncsBusMessages(simulationId)
        gossConnection.send(output_to_goss_topic , message)
    elif msg['command'] == 'update':
        _publishToFncsBus(simulationId, gossMessage) #does not return
    elif msg['command'] == 'nextTimestep':
        currentTime = msg['currentTime']
        _doneWithTimestep(currentTime) #currentTime is incrementing integer 0 ,1, 2.... representing seconds
        message['output'] = _getFncsBusMessages(simulationId)
        gossConnection.send(output_to_goss_topic , message)
    elif msg['command'] == 'stop':
        fncs.die()
    
def _registerWithFncsBroker(
        simulationId, brokerLocation='tcp://localhost:5570'):
    '''Register with the fncs_broker and return.
    
    Function arguments:
        simulationId -- Type: string. Description: The simulation id. 
            It must not be an empty string. Default: None.
        brokerLocation -- Type: string. Description: The ip location and port
            for the fncs_broker. It must not be an empty string.
            Default: 'tcp://localhost:5570'.
    Function returns:
        None.
    Function exceptions:
        RuntimeError()
        ValueError()
    '''
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
                'topic' : simulationId + '/Output',
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
    logger.info('registered with fncs '+isInitialized)

    if not fncs.is_initialized():
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
    logger.debug('publish to fncs bus '+simulationId+' '+gossMessage)

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
        testGossMessageFormat = json.loads(gossMessage)
        if type(testGossMessageFormat) != dict:
            raise ValueError(
                'gossMessage is not a json formatted string.'
                + '\ngossMessage = {0}'.format(gossMessage))
    except ValueError as ve:
        raise ValueError(ve)
    except:
        raise RuntimeError(
            'Unexpected error occured while executing json.loads(gossMessage'
            + '{0}'.format(sys.exc_info()[0]))
    fncsInputTopic = '{0}/Input'.format(simulationId)
    logger.debug('fncs input topic '+fncsInputTopic)
    #fncs.publish_anon(fncsInputTopic, gossMessage)
    
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
    simulatorMessageOutput = None
    if simulationId == None or simulationId == '' or type(simulationId) != str:
        raise ValueError(
            'simulationId must be a nonempty string.\n'
            + 'simulationId = {0}'.format(simulationId))
    messageEvents = fncs.get_events()
    if simulationId in messageEvents:
        simulatorMessageOutput = fncs.get_value(simulationId)
    return simulatorMessageOutput

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
    logger.debug('done with timestep '+currentTime)
    if currentTime == None or type(currentTime) != int:
        raise ValueError(
            'currentTime must be an integer.\n'
            + 'currentTime = {0}'.format(currentTime))
    timeRequest = currentTime + 1
    timeApproved = fncs.time_request(timeRequest)
    if timeApproved != timeRequest:
        raise RuntimeError(
            'The time approved from fncs_broker is not the time requested.\n'
            + 'timeRequest = {0}.\ntimeApproved = {1}'.format(timeRequest, 
            timeApproved))

            
def _registerWithGOSS(username,password,gossServer='localhost', 
                      stompPort='61613',):
    '''Register with the GOSS server broker and return.
    
    Function arguments:
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
    gossConnection.connect(username,password)
    gossConnection.set_listener('GOSSListener', GOSSListener())
    gossConnection.subscribe(input_from_goss_topic,1)
    logger.info('registered with goss on topic '+input_from_goss_topic+' '+gossConnection.is_connected())
    
if __name__ == "__main__":
    #TODO: send simulationId, fncsBrokerLocation, gossLocation, 
    #stompPort, username and password as commmand line arguments 
    _registerWithGOSS('system','password',gossServer='localhost',stompPort='61613')
    _registerWithFncsBroker('simulation1','tcp://localhost:5570')
    
    
    
    
