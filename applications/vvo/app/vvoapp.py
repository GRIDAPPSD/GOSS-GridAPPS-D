#-------------------------------------------------------------------------------
# Copyright 2017, Battelle Memorial Institute All rights reserved.
# Battelle Memorial Institute (hereinafter Battelle) hereby grants permission to any person or entity 
# lawfully obtaining a copy of this software and associated documentation files (hereinafter the 
# Software) to redistribute and use the Software in source and binary forms, with or without modification. 
# Such person or entity may use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
# the Software, and may permit others to do so, subject to the following conditions:
# Redistributions of source code must retain the above copyright notice, this list of conditions and the 
# following disclaimers.
# Redistributions in binary form must reproduce the above copyright notice, this list of conditions and 
# the following disclaimer in the documentation and/or other materials provided with the distribution.
# Other than as used herein, neither the name Battelle Memorial Institute or Battelle may be used in any 
# form whatsoever without the express written consent of Battelle.
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
# EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
# MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL 
# BATTELLE OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, 
# OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE 
# GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED 
# AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
# NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED 
# OF THE POSSIBILITY OF SUCH DAMAGE.
# General disclaimer for use with OSS licenses
# 
# This material was prepared as an account of work sponsored by an agency of the United States Government. 
# Neither the United States Government nor the United States Department of Energy, nor Battelle, nor any 
# of their employees, nor any jurisdiction or organization that has cooperated in the development of these 
# materials, makes any warranty, express or implied, or assumes any legal liability or responsibility for 
# the accuracy, completeness, or usefulness or any information, apparatus, product, software, or process 
# disclosed, or represents that its use would not infringe privately owned rights.
# 
# Reference herein to any specific commercial product, process, or service by trade name, trademark, manufacturer, 
# or otherwise does not necessarily constitute or imply its endorsement, recommendation, or favoring by the United 
# States Government or any agency thereof, or Battelle Memorial Institute. The views and opinions of authors expressed 
# herein do not necessarily state or reflect those of the United States Government or any agency thereof.
# 
# PACIFIC NORTHWEST NATIONAL LABORATORY operated by BATTELLE for the 
# UNITED STATES DEPARTMENT OF ENERGY under Contract DE-AC05-76RL01830
#-------------------------------------------------------------------------------
"""
Created on Jan 6, 2017

@author: fish334
@author: poorva1209
"""

__version__ = "0.0.1"

import logging
import os
import shutil
import sys
import time
import traceback

import json
import stomp
import yaml

from vvo import VoltVarControl

logger_name = "vvo-app"
user_home = os.path.expanduser("~")
logger_location = os.path.join(user_home, "var/log/" + logger_name + ".log")

if not os.path.exists(os.path.dirname(logger_location)):
    os.makedirs(os.path.dirname(logger_location))

write_topic = '/topic/goss/gridappsd/fncs/input'  # this should match GridAppsDConstants.topic_FNCS_input
write_queue = '/queue/goss/gridappsd/fncs/input'  # this should match GridAppsDConstants.topic_FNCS_input

read_topic = '/topic/goss/gridappsd/fncs/output'  # this should match GridAppsDConstants.topic_FNCS_output
read_queue = '/queue/goss/gridappsd/fncs/output'  # this should match GridAppsDConstants.topic_FNCS_output
goss_connection = None
is_initialized = False
# Number 
simulation_id = None

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
main_app = None
opts = None
static_config = None


class GOSSListener(object):
    def __init__(self, t0):
        self.t0 = t0

    def on_message(self, headers, msg):
        global main_app
        message = {}
        try:
            self.t0 += 1
            logger.debug('received message ' + str(msg))
            json_msg = yaml.safe_load(str(msg))
            output = yaml.safe_load(json_msg['output'])
            # Ignore null output data. (Assumes initializing)
            if output is None:
              return

            print("the output is: {}".format(output))
            # This is the start of the application processes.
            if main_app is None:
                # Start the main application class.  Note we are passsing the function
                # app_output which will be called when output from the application is
                # necessary.
                main_app = VoltVarControl(static_config, output, app_output)

            main_app.input(output)
            main_app.reg_control(self.t0)
            main_app.cap_control(self.t0)
            main_app.output()

        except Exception as e:
            traceback.print_exc(file=sys.stdout)
            logger.error(type(e))
            logger.error(e.args)
            logger.error('Error in command ' + str(e))

    def on_error(self, headers, message):
        logger.error('Error in goss listener ' + str(message))

    def on_disconnected(self):
        logger.error('Disconnected')


def app_output(outputDict):
    """
    Callback function for messages that are going to from the application
    back to goss.
    :param outputDict:
    :return:
    """
    payload = dict(command='update', message={}) #, simulation_id=simulation_id)
    # Assumes now that simulation name is the top level of the outputDict
    # in vvo.py
    payload['message'] = outputDict
    logger.debug("Sending payload from vvo {}".format(payload))
    goss_connection.send(write_topic , json.dumps(payload))

def _keep_alive():
    while 1:
        time.sleep(0.1)


def _register_with_goss(username, password, gossServer='localhost',
                      stompPort='61613', ):
    """Register with the GOSS server broker and return.

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
    """
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
    global goss_connection
    goss_connection = stomp.Connection12([(gossServer, stompPort)])
    goss_connection.start()
    goss_connection.connect(username, password, wait=True)
    goss_connection.set_listener('GOSSListener', GOSSListener(opts.t0))
    goss_connection.subscribe(read_topic, 1)
    goss_connection.subscribe(read_topic, 2)

    print(
        'Registered with GOSS on topic ' + read_topic + ' ' + str(
            goss_connection.is_connected()))


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(version=__version__)

    parser.add_argument("simulationId",
                        help="Simulation id to use for responses on the message bus.")
# FYI Changed this to use either -c or -f so the config can be passed in on the command line						
#    parser.add_argument('infile', nargs='?', type=argparse.FileType('r'),
#                   default=sys.stdin,
#                        help="Static configuration file for VVO app")
    parser.add_argument("-c", "--config",  
                        help="The static configuration string for VVO app, if used then -f will be ignored")						
    parser.add_argument("-f", "--infile",  type=argparse.FileType('r'),
                        help="The static configuration file for VVO app, will be ignored if -c is also used")						
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
    
    if not opts.config is None:
        static_config = yaml.safe_load(opts.config)
    elif not opts.infile is None:
        static_config = yaml.safe_load(opts.infile.read())
    else:
        sys.stderr.write("Error: config or infile parameter expected")
        logger.error('Error: config or infile parameter expected')
        exit(1)
    logger.debug("Received static config "+str(static_config))
    # TODO validate that we are getting the correct things here.
    keys = static_config['static_inputs'].keys()
    simulation_name = keys[0] #static_config['static_inputs'][]
    static_config = static_config['static_inputs']

    # Connect and listen to the message bus for content.
    # The port should be cast to string because that makes the opening socket easier.
    _register_with_goss(opts.user, opts.password, opts.address, str(opts.port))

    # Sleep until notified of new data.
    _keep_alive()



