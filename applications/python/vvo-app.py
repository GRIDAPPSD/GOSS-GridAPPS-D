'''
Created on Jan 6, 2017

@author: fish334
@author: poorva1209
'''
from vvo import VoltVarControl
import json
import sys
import time
import shutil
import stomp
import logging
import os

__version__ = "0.0.1"

logger_name = os.path.basename(__file__)[:-3]
user_home = os.path.expanduser("~")
logger_location = os.path.join(user_home, "var/log/" + logger_name + ".log")

if not os.path.exists(logger_location):
    os.makedirs(os.path.dirname(logger_location))

write_topic = '/topic/goss/gridappsd/fncs/input'  # this should match GridAppsDConstants.topic_FNCS_input
write_queue = '/queue/goss/gridappsd/fncs/input'  # this should match GridAppsDConstants.topic_FNCS_input

read_topic = '/topic/goss/gridappsd/fncs/output'  # this should match GridAppsDConstants.topic_FNCS_output
read_queue = '/queue/goss/gridappsd/fncs/output'  # this should match GridAppsDConstants.topic_FNCS_output
gossConnection = None
isInitialized = False
# Number 
simulationId = None

# This is currently what gridlabd will use for its object name.
# TODO change to use concat of sim id and sim name.
simulation_name = None

hdlr = logging.FileHandler(logger_location)
formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
hdlr.setFormatter(formatter)

logger = logging.getLogger(logger_name)
logger.addHandler(hdlr)
logger.setLevel(logging.DEBUG)

# Created in the bottom of the script before the instaniation of the
# GossListener.
mainApp = None
opts = None
static_config = None


class GOSSListener(object):
    def __init__(self, t0):
        self.t0 = t0

    def on_message(self, headers, msg):
        global mainApp
        message = {}
        try:
            self.t0 += 1
            logger.debug('received message ' + str(msg))
            jsonmsg = json.loads(str(msg))

            # This is the start of the application processes.
            if mainApp is None:
                # Start the main application class.  Note we are passsing the function
                # appOutput which will be called when output from the application is
                # necessary.
                mainApp = VoltVarControl(static_config, jsonmsg, appOutput)

            mainApp.Input(jsonmsg)
            mainApp.RegControl(self.t0)
            mainApp.CapControl(self.t0)
            mainApp.Output()

        except Exception as e:
            logger.error('Error in command ' + str(e))

    def on_error(self, headers, message):
        logger.error('Error in goss listener ' + str(message))

    def on_disconnected(self):
        logger.error('Disconnected')


def appOutput(outputDict):
    """
    Callback function for messages that are going to from the application
    back to goss.
    :param outputDict:
    :return:
    """
    payload = dict(command='update', message={}) #, simulation_id=simulationId)

    payload['message'][simulation_name] = outputDict

    gossConnection.send(write_topic , json.dumps(payload))

def _keepAlive():
    while 1:
        time.sleep(0.1)


# def _registerWithFncsBroker(
#         simId, brokerLocation='tcp://localhost:5570'):
#     '''Register with the fncs_broker and return.

#     Function arguments:
#         simulationId -- Type: string. Description: The simulation id.
#             It must not be an empty string. Default: None.
#         brokerLocation -- Type: string. Description: The ip location and port
#             for the fncs_broker. It must not be an empty string.
#             Default: 'tcp://localhost:5570'.
#     Function returns:
#         None.
#     Function exceptions:
#         RuntimeError()
#         ValueError()
#     '''
#     global simulationId
#     global isInitialized
#     simulationId = simId
#     try:
#         logger.info('Registering with FNCS broker ' + str(
#             simulationId) + ' and broker ' + brokerLocation)

#         logger.debug(
#             'still connected to goss 1 ' + str(gossConnection.is_connected()))
#         if simulationId == None or simulationId == '' or type(
#                 simulationId) != str:
#             raise ValueError(
#                 'simulationId must be a nonempty string.\n'
#                 + 'simulationId = {0}'.format(simulationId))

#         if (brokerLocation == None or brokerLocation == ''
#             or type(brokerLocation) != str):
#             raise ValueError(
#                 'brokerLocation must be a nonempty string.\n'
#                 + 'brokerLocation = {0}'.format(brokerLocation))
#         fncsConfiguration = {
#             'name': 'FNCS_GOSS_Bridge_' + simulationId,
#             'time_delta': '1s',
#             'broker': brokerLocation,
#             'values': {
#                 simulationId: {
#                     'topic': simulationId + '/fncs_output',
#                     'default': '{}',
#                     'type': 'JSON',
#                     'list': 'false'
#                 }
#             }
#         }

#         configurationZpl = ('name = {0}\n'.format(fncsConfiguration['name'])
#                             + 'time_delta = {0}\n'.format(
#             fncsConfiguration['time_delta'])
#                             + 'broker = {0}\nvalues'.format(
#             fncsConfiguration['broker']))
#         for x in fncsConfiguration['values'].keys():
#             configurationZpl += '\n    {0}'.format(x)
#             configurationZpl += '\n        topic = {0}'.format(
#                 fncsConfiguration['values'][x]['topic'])
#             configurationZpl += '\n        default = {0}'.format(
#                 fncsConfiguration['values'][x]['default'])
#             configurationZpl += '\n        type = {0}'.format(
#                 fncsConfiguration['values'][x]['type'])
#             configurationZpl += '\n        list = {0}'.format(
#                 fncsConfiguration['values'][x]['list'])
#         fncs.initialize(configurationZpl)

#         isInitialized = fncs.is_initialized()
#         logger.info('Registered with fncs ' + str(isInitialized))


#     except Exception as e:
#         logger.error('Error while registering with fncs broker ' + str(e))

#     if not fncs.is_initialized():
#         raise RuntimeError(
#             'fncs.initialize(configurationZpl) failed!\n'
#             + 'configurationZpl = {0}'.format(configurationZpl))


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
    logger.debug('publish to fncs bus ' + simulationId + ' ' + gossMessage)

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
    logger.debug('fncs input topic ' + fncsInputTopic)
    # fncs.publish_anon(fncsInputTopic, gossMessage)


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
        if simulationId == None or simulationId == '' or type(
                simulationId) != str:
            raise ValueError(
                'simulationId must be a nonempty string.\n'
                + 'simulationId = {0}'.format(simulationId))
        logger.debug('about to get fncs events')
        messageEvents = fncs.get_events()
        logger.debug('fncs events ' + str(messageEvents))
        if simulationId in messageEvents:
            simulatorMessageOutput = fncs.get_value(simulationId)
        logger.debug('simulatorMessageOutput ' + str(simulatorMessageOutput))
        return simulatorMessageOutput
    except Exception as e:
        logger.error(
            'Error on get FncsBusMessages for ' + str(simulationId) + ' ' + str(
                e))


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
        logger.debug('In done with timestep ' + str(currentTime))
        if currentTime == None or type(currentTime) != int:
            raise ValueError(
                'currentTime must be an integer.\n'
                + 'currentTime = {0}'.format(currentTime))
        timeRequest = currentTime + 1
        logger.debug('calling time_request ' + str(timeRequest))
        timeApproved = fncs.time_request(timeRequest)
        logger.debug('time approved ' + str(timeApproved))
        if timeApproved != timeRequest:
            raise RuntimeError(
                'The time approved from fncs_broker is not the time requested.\n'
                + 'timeRequest = {0}.\ntimeApproved = {1}'.format(timeRequest,
                                                                  timeApproved))
    except Exception as e:
        logger.error('Error in fncs timestep ' + str(e))


def _registerWithGOSS(username, password, gossServer='localhost',
                      stompPort='61613', ):
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
    global gossConnection
    gossConnection = stomp.Connection12([(gossServer, stompPort)])
    gossConnection.start()
    gossConnection.connect(username, password, wait=True)
    gossConnection.set_listener('GOSSListener', GOSSListener(opts.t0))
    gossConnection.subscribe(read_topic, 1)
    gossConnection.subscribe(read_topic, 2)

    print(
        'Registered with GOSS on topic ' + read_topic + ' ' + str(
            gossConnection.is_connected()))


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(version=__version__)

    parser.add_argument("simulationId",
                        help="Simulation id to use for responses on the message bus.")
    parser.add_argument('infile', nargs='?', type=argparse.FileType('r'),
                        default=sys.stdin,
                        help="Static configuration file for VVO app")
    parser.add_argument("-u", "--user", default="system",
                        help="The username to authenticate with the message bus.")
    parser.add_argument("-p", "--password", default="manager",
                         help="The password to authenticate with the message bus.")
    parser.add_argument("-a", "--address", default="127.0.0.1",
                        help="tcp address of the mesage bus.")
    parser.add_argument("--port", default=61613, type=int,
                        help="the stomp port on the message bus.")
    parser.add_argument("-t0", default=2, type=int,
                        help="T0 start value for the application.")
    opts = parser.parse_args()

    logger.debug("Waiting for ")
    static_config = json.loads(opts.infile.read())

    # TODO validate that we are getting the correct things here.
    keys = static_config['static_inputs'].keys()
    simulation_name = keys[0] #static_config['static_inputs'][]

    # Connect and listen to the message bus for content.
    # The port should be cast to string because that makes the opening socket easier.
    _registerWithGOSS(opts.user, opts.password, opts.address, str(opts.port))

    # Sleep until notified of new data.
    _keepAlive()



