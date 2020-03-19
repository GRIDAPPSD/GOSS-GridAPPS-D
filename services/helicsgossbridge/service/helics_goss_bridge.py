# Copyright (c) 2020, Battelle Memorial Institute All rights reserved.
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
Created on Mar 9, 2020

@author: fish334
"""

import argparse
import cmath
from datetime import datetime
import gzip
import json
import logging
import math
import os
import traceback
try:
    from Queue import Queue
except:
    from queue import Queue
import sqlite3
import sys
import time

import helics
import yaml

from gridappsd import GridAPPSD, utils, topics

log = logging.getLogger(__name__)
log.setLevel(logging.DEBUG)

def HelicsGossBridge(object):
    '''
    ClassDocs
    '''
    _simulation_id = ""
    _broker_port = "5570"
    _simulation_request = {}
    _gapps = None
    _helics_configuration = {}
    _helics_federate = None
    
    def __init__(self, simulation_id, broker_port, simulation_request):
        self._simulation_id = simulation_id
        self._broker_port = broker_port
        self._simulation_request = simulation_request
        # register with GridAPPS-D
        self._register_with_goss()
        # register with HELICS
        self._register_with_helics()
    
    
    def _register_with_goss(self):
        try:
            self._gad_connection = GridAPPSD(self._simulation_id, address=utils.get_gridappsd_address(),
                username=utils.get_gridappsd_user(), password=utils.get_gridappsd_pass())
            log.debug("Successfully registered with the GridAPPS-D platform.")
        except Exception as e:
            log.error("An error occurred when trying to register with the GridAPPS-D platform!", exc_info=True)
            
            
    def _register_with_helics(self):
        try:
            self._helics_configuration = {
                "name": "HELICS_GOSS_Bridge_{}".format(self._simulation_id),
                "period": 1.0,
                "log_level": 7,
                "endpoints": [
                    {
                        "name": "helics_input",
                        "global": False,
                        "destination": "{}/helics_input".format(self._simulation_id),
                        "type": "string",
                        "info": "This is the endpoint which sends CIM attribute commands to the GridLAB-D simulator."
                    },
                    {
                        "name": "helics_output",
                        "global": False,
                        "type": "string",
                        "info": "This is the endpoint which receives CIM measurements from the GridLAB-D simulator."
                    }
                ]
            }
            self._helics_federate = helics.helicsCreateMessageFederateFromConfig(json.dumps(self.helics_configuration))
            log.debug("Successfully registered with the HELICS broker.") 
        except Exception as e:
            err_msg = "An error occurred when trying to register with the HELICS broker!"
            log.error(err_msg, exc_info=True)
            self._gapps.send_simulation_status("ERROR", err_msg, "ERROR")
    
    
def _main(simulation_id, broker_port, simulation_request):
    bridge = HelicsGossBridge(simulation_id, broker_port, simulation_request)

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("simulation_id", help="The simulation id to use for responses on the message bus.")
    parser.add_argument("broker_port", help="The port the helics broker is running on.")
    parser.add_argument("simulation_request", help="The simulation request.")
    args = parser.parse_args()
    _main(args.simulation_id, args.broker_port, args.simulation_request)