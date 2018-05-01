#-------------------------------------------------------------------------------
# Copyright (c) 2017, Battelle Memorial Institute All rights reserved.
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
Created on Jan 19, 2018

@author: fish334
@author: poorva1209
"""

__version__ = "0.0.1"

import json
import stomp
import uuid

class GOSSListener(stomp.ConnectionListener):
    def __init__(self, conn):
        self.conn = conn

    def on_message(self, headers, msg):
        print('simulation output received as "%s"' % message)
        
        command_message = '''{"simulation_id": "12ae2345",
                           "message": {
                                       "timestamp": "2018-01-08T13:27:00.000Z",
                                       "difference_mrid": "123a456b-789c-012d-345e-678f901a235c",
                                       "reverse_differences": [{
                                                                "object": "61A547FB-9F68-5635-BB4C-F7F537FD824E",
                                                                "attribute": "ShuntCompensator.sections",
                                                                "value": "1"
                                                                },
                                                                {
                                                                "object": "E3CA4CD4-B0D4-9A83-3E2F-18AC5F1B55BA",
                                                                "attribute": "ShuntCompensator.sections",
                                                                "value": "0"
                                                                }
                                                               ],
                                       "forward_differences": [{
                                                                "object": "61A547FB-9F68-5635-BB4C-F7F537FD824E",
                                                                "attribute": "ShuntCompensator.sections",
                                                                "value": "0"
                                                                },
                                                                {
                                                                 "object": "E3CA4CD4-B0D4-9A83-3E2F-18AC5F1B55BA",
                                                                 "attribute": "ShuntCompensator.sections",
                                                                 "value": "1"
                                                                 }
                                                               ]
                                       }
                           }'''
        
        _send(command_message, topic)

    def on_error(self, headers, message):
        logger.error('Error in goss listener ' + str(message))

    def on_disconnected(self):
        logger.error('Disconnected')

def _connect(gossServer, stompPort, username, password):
    global connection
    connection = stomp.Connection12([(gossServer, stompPort)])
    connection.start()
    connection.connect(username, password, wait=True)    
    print('GOSS connection status: ' + str(goss_connection.is_connected()))
    
def _send(message,topic):
    connection.send(topic, message);
    
## Your application code here


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(version=__version__)

    parser.add_argument("simulationId",
                        help="Simulation id to use for responses on the message bus.")
    parser.add_argument("-c", "--static_args",  
                        help="The static input string for app, if used then -f will be ignored")                        
    parser.add_argument("-u", "--user", default="system",
                        help="The username to authenticate with the message bus.")
    parser.add_argument("-p", "--password", default="manager",
                         help="The password to authenticate with the message bus.")
    parser.add_argument("-a", "--address", default="127.0.0.1",
                        help="tcp address of the mesage bus.")
    parser.add_argument("--port", default=61613, type=int,
                        help="the stomp port on the message bus.")
    opts = parser.parse_args()
    
    #Connect with GOSS message bus
    _connect(opts.address, opts.port, opts.user, opts.password)
    
    #Subscribe to simulation output
    connection.subscribe("goss.gridappsd.simulation.log."+opts.simulationId, 1)
    
    






    

    
    