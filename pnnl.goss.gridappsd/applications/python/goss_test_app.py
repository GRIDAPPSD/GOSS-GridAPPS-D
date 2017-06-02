#-------------------------------------------------------------------------------
# Copyright © 2017, Battelle Memorial Institute All rights reserved.
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
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS “AS IS” AND ANY 
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
'''
Created on Jan 6, 2017

@author: tdtalbot
'''
import json
import sys
import stomp 
import time

goss_simulation_topic = '/queue/goss/gridappsd/process/request/simulation'  
goss_simulation_status_topic = '/topic/goss/gridappsd/simulation/status/'  
gossConnection= None
isInitialized = None
simulationId = None

class GOSSStatusListener(object):
  def on_message(self, headers, msg):
    message = ''
    print('status ',msg)
  def on_error(self, headers, msg):
    print('simulation status error      ',msg)
class GOSSSimulationStartListener(object):
  def on_message(self, headers, msg):
    message = ''
    print('simulation start ', msg)    
    _registerWithGOSS('system','manager', msg,gossServer='localhost',stompPort='61613')
  def on_error(self, headers, msg):
    print('simulation start error     ',msg)
           
def _registerWithGOSS(username,password,simulationId,gossServer='localhost', 
                      stompPort='61613'):
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
    gossConnection.set_listener('GOSSStatusListener', GOSSStatusListener())
    gossConnection.subscribe(goss_simulation_status_topic+simulationId,1)
    
def _startSimulation(username,password,gossServer='localhost',stompPort='61613'):
    simulationCfg = '{"power_system_config":{"GeographicalRegion_name":"ieee8500nodecktassets_Region","SubGeographicalRegion_name":"ieee8500nodecktassets_SubRegion","Line_name":"ieee8500"}, "simulation_config":{"start_time":"03/07/2017 00:00:00","duration":"60","simulator":"GridLAB-D","simulation_name":"my test simulation","power_flow_solver_method":"FBS"}}'
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
    gossConnection.set_listener('GOSSSimulationStartListener',GOSSSimulationStartListener())
    gossConnection.subscribe(destination='/queue/reply',id=2)
    gossConnection.send(body=simulationCfg, destination=goss_simulation_topic, headers={'reply-to': '/queue/reply'})   
    time.sleep(3) 
    print('sent simulation request')
    
        
if __name__ == "__main__":
    #TODO: send simulationId, fncsBrokerLocation, gossLocation, 
    #stompPort, username and password as command line arguments 
    
    _startSimulation('system','manager',gossServer='127.0.0.1',stompPort='61613')
    
    
    
