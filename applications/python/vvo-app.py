'''
Created on Jan 6, 2017

@author: fish334
@author: poorva1209
'''
#from vvo import VoltVarControl
import json
import yaml
import sys
import time
import shutil
import stomp
import logging
import traceback
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

from vvo import VoltVarControl

class GOSSListener(object):
    def __init__(self, t0):
        self.t0 = t0

    def on_message(self, headers, msg):
        global mainApp
        message = {}
        try:
            self.t0 += 1
            logger.debug('received message ' + str(msg))
            jsonmsg = yaml.safe_load(str(msg))
            output = yaml.safe_load(jsonmsg['output'])
            # Ignore null output data. (Assumes initializing)
            if output is None:
              return

            print("the output is: {}".format(output))
            # This is the start of the application processes.
            if mainApp is None:
                # Start the main application class.  Note we are passsing the function
                # appOutput which will be called when output from the application is
                # necessary.
                mainApp = VoltVarControl(static_config, output, appOutput)

            mainApp.Input(output)
            mainApp.RegControl(self.t0)
            mainApp.CapControl(self.t0)
            mainApp.Output()

        except Exception as e:
            traceback.print_exc(file=sys.stdout)
            logger.error(type(e))
            logger.error(e.args)
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
    # Assumes now that simulation name is the top level of the outputDict
    # in vvo.py
    payload['message'] = outputDict
    logger.debug("Sending payload from vvo {}".format(payload))
    gossConnection.send(write_topic , json.dumps(payload))

def _keepAlive():
    while 1:
        time.sleep(0.1)


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
    static_config = yaml.safe_load(opts.infile.read())
    logger.debug("Received static config "+str(static_config))
    # TODO validate that we are getting the correct things here.
    keys = static_config['static_inputs'].keys()
    simulation_name = keys[0] #static_config['static_inputs'][]
    static_config = static_config['static_inputs']

    # Connect and listen to the message bus for content.
    # The port should be cast to string because that makes the opening socket easier.
    _registerWithGOSS(opts.user, opts.password, opts.address, str(opts.port))

    # Sleep until notified of new data.
    _keepAlive()



