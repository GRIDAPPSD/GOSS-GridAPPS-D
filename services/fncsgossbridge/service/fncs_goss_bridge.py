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
Created on Jan 6, 2017

@author: fish334
@author: poorva1209
"""
from datetime import datetime
import json
import os
import sys
import time

import stomp
import yaml

try:
    import fncs
except:
    if not os.environ.get("CI"):
        raise ValueError("fncs.py is unavailable on the python path.")
    else:
        sys.stdout.write("Running tests.\n")
        fncs = {}


input_from_goss_topic = '/topic/goss.gridappsd.fncs.input' #this should match GridAppsDConstants.topic_FNCS_input
input_from_goss_queue = '/queue/goss.gridappsd.fncs.input' #this should match GridAppsDConstants.topic_FNCS_input

output_to_goss_topic = '/topic/goss.gridappsd.fncs.output' #this should match GridAppsDConstants.topic_FNCS_output
output_to_goss_queue = '/queue/goss.gridappsd.fncs.output' #this should match GridAppsDConstants.topic_FNCS_output
goss_connection= None
is_initialized = False 
simulation_id = None


class GOSSListener(object):
    def on_message(self, headers, msg):
        message = {}
        try:
            message_str = 'received message '+str(msg)
            if fncs.is_initialized():
                _send_simulation_status('RUNNING', message_str, 'DEBUG')
            else:
                _send_simulation_status('STARTED', message_str, 'DEBUG')
            json_msg = yaml.safe_load(str(msg))
            if json_msg['command'] == 'isInitialized':
                message_str = 'isInitialized check: '+str(is_initialized)
                if fncs.is_initialized():
                    _send_simulation_status('RUNNING', message_str, 'DEBUG')
                else:
                    _send_simulation_status('STARTED', message_str, 'DEBUG')
                message['command'] = 'isInitialized'
                message['response'] = str(is_initialized)
                if (simulation_id != None):
                    message['output'] = _get_fncs_bus_messages(simulation_id)
                message_str = 'Added isInitialized output, sending message '+str(message)+' connection '+str(goss_connection)
                if fncs.is_initialized():
                    _send_simulation_status('RUNNING', message_str, 'DEBUG')
                else:
                    _send_simulation_status('STARTED', message_str, 'DEBUG')
                goss_connection.send(output_to_goss_topic , json.dumps(message))
                goss_connection.send(output_to_goss_queue , json.dumps(message))
            elif json_msg['command'] == 'update':
                message['command'] = 'update'
                _publish_to_fncs_bus(simulation_id, json.dumps(json_msg['message'])) #does not return
            elif json_msg['command'] == 'nextTimeStep':
                message_str = 'is next timestep'
                _send_simulation_status('RUNNING', message_str, 'DEBUG')
                message['command'] = 'nextTimeStep'
                current_time = json_msg['currentTime']
                message_str = 'incrementing to '+str(current_time)
                _send_simulation_status('RUNNING', message_str, 'DEBUG')
                _done_with_time_step(current_time) #current_time is incrementing integer 0 ,1, 2.... representing seconds
                message_str = 'done with timestep '+str(current_time)
                _send_simulation_status('RUNNING', message_str, 'DEBUG')
                message_str = 'simulation id '+str(simulation_id)
                _send_simulation_status('RUNNING', message_str, 'DEBUG')
                message['output'] = _get_fncs_bus_messages(simulation_id)
                response_msg = json.dumps(message)
                message_str = 'sending fncs output message '+str(response_msg)
                _send_simulation_status('RUNNING', message_str, 'DEBUG')
                goss_connection.send(output_to_goss_topic , response_msg)
                goss_connection.send(output_to_goss_queue , response_msg)
            elif json_msg['command'] == 'stop':
                message_str = 'Stopping the simulation'
                _send_simulation_status('stopped', message_str, 'INFO')
                fncs.die()
                sys.exit()
        
        except Exception as e:
            message_str = 'Error in command '+str(e)
            _send_simulation_status('ERROR', message_str, 'ERROR')
            if fncs.is_initialized():
                fncs.die()
           
        
    def on_error(self, headers, message):
        message_str = 'Error in goss listener '+str(message)
        _send_simulation_status('ERROR', message_str, 'ERROR')
        if fncs.is_initialized():
            fncs.die()
    
    
    def on_disconnected(self):
        if fncs.is_initialized():
            fncs.die()
  
  
def _register_with_fncs_broker(broker_location='tcp://localhost:5570'):
    """Register with the fncs_broker and return.
    
    Function arguments:
        broker_location -- Type: string. Description: The ip location and port
            for the fncs_broker. It must not be an empty string.
            Default: 'tcp://localhost:5570'.
    Function returns:
        None.
    Function exceptions:
        RuntimeError()
        ValueError()
    """
    global is_initialized
    configuration_zpl = ''
    try:
        message_str = 'Registering with FNCS broker '+str(simulation_id)+' and broker '+broker_location
        _send_simulation_status('STARTED', message_str, 'INFO')
        
        message_str = 'still connected to goss 1 '+str(goss_connection.is_connected())
        _send_simulation_status('STARTED', message_str, 'INFO')
        if simulation_id == None or simulation_id == '' or type(simulation_id) != str:
            raise ValueError(
                'simulation_id must be a nonempty string.\n'
                + 'simulation_id = {0}'.format(simulation_id))
    
        if (broker_location == None or broker_location == ''
                or type(broker_location) != str):
            raise ValueError(
                'broker_location must be a nonempty string.\n' 
                + 'broker_location = {0}'.format(broker_location))
        fncs_configuration = {
            'name' : 'FNCS_GOSS_Bridge_' + simulation_id,
            'time_delta' : '1s',
            'broker' : broker_location,
            'values' : {
                simulation_id : {
                    'topic' : simulation_id + '/fncs_output',
                    'default' : '{}',
                    'type' : 'JSON',
                    'list' : 'false'
                }
            }
        }  
    
        
        configuration_zpl = ('name = {0}\n'.format(fncs_configuration['name'])
            + 'time_delta = {0}\n'.format(fncs_configuration['time_delta'])
            + 'broker = {0}\nvalues'.format(fncs_configuration['broker']))
        for x in fncs_configuration['values'].keys():
            configuration_zpl += '\n    {0}'.format(x)
            configuration_zpl += '\n        topic = {0}'.format(
                fncs_configuration['values'][x]['topic'])
            configuration_zpl += '\n        default = {0}'.format(
                fncs_configuration['values'][x]['default'])
            configuration_zpl += '\n        type = {0}'.format(
                fncs_configuration['values'][x]['type'])
            configuration_zpl += '\n        list = {0}'.format(
                fncs_configuration['values'][x]['list'])
        fncs.initialize(configuration_zpl)
        
        is_initialized = fncs.is_initialized()
        if is_initialized:
            message_str = 'Registered with fncs '+str(is_initialized)
            _send_simulation_status('RUNNING', message_str, 'INFO')
    
    
    except Exception as e:
        message_str = 'Error while registering with fncs broker '+str(e)
        _send_simulation_status('ERROR', message_str, 'ERROR')
        if fncs.is_initialized():
            fncs.die()

    if not fncs.is_initialized():
        message_str = 'fncs.initialize(configuration_zpl) failed!\n' + 'configuration_zpl = {0}'.format(configuration_zpl)
        _send_simulation_status('ERROR', message_str, 'ERROR')
        if fncs.is_initialized():
            fncs.die()
        raise RuntimeError(
            'fncs.initialize(configuration_zpl) failed!\n'
            + 'configuration_zpl = {0}'.format(configuration_zpl))
        
    
def _publish_to_fncs_bus(simulation_id, goss_message):
    """publish a message received from the GOSS bus to the FNCS bus.
    
    Function arguments:
        simulation_id -- Type: string. Description: The simulation id. 
            It must not be an empty string. Default: None.
        goss_message -- Type: string. Description: The message from the GOSS bus
            as a json string. It must not be an empty string. Default: None.
    Function returns:
        None.
    Function exceptions:
        RuntimeError()
        ValueError()
    """
    message_str = 'publish to fncs bus '+simulation_id+' '+str(goss_message)
    _send_simulation_status('RUNNING', message_str, 'DEBUG')

    if simulation_id == None or simulation_id == '' or type(simulation_id) != str:
        raise ValueError(
            'simulation_id must be a nonempty string.\n'
            + 'simulation_id = {0}'.format(simulation_id))
    if goss_message == None or goss_message == '' or type(goss_message) != str:
        raise ValueError(
            'goss_message must be a nonempty string.\n'
            + 'goss_message = {0}'.format(goss_message))
    if not fncs.is_initialized():
        raise RuntimeError(
            'Cannot publish message as there is no connection'
            + ' to the FNCS message bus.')
    try:
        test_goss_message_format = yaml.safe_load(goss_message)
        if type(test_goss_message_format) != dict:
            raise ValueError(
                'goss_message is not a json formatted string.'
                + '\ngoss_message = {0}'.format(goss_message))
    except ValueError as ve:
        raise ValueError(ve)
    except:
        raise RuntimeError(
            'Unexpected error occured while executing yaml.safe_load(goss_message'
            + '{0}'.format(sys.exc_info()[0]))
    fncs_input_topic = '{0}/fncs_input'.format(simulation_id)
    message_str = 'fncs input topic '+fncs_input_topic
    _send_simulation_status('RUNNING', message_str, 'DEBUG')
    fncs.publish_anon(fncs_input_topic, goss_message)
    
    
def _get_fncs_bus_messages(simulation_id):
    """publish a message received from the GOSS bus to the FNCS bus.
    
    Function arguments:
        simulation_id -- Type: string. Description: The simulation id. 
            It must not be an empty string. Default: None.
    Function returns:
        fncs_output -- Type: string. Description: The json structured output
            from the simulation. If no output was sent from the simulation then
            it returns None.
    Function exceptions:
        ValueError()
    """
    try:
        fncs_output = None
        if simulation_id == None or simulation_id == '' or type(simulation_id) != str:
            raise ValueError(
                'simulation_id must be a nonempty string.\n'
                + 'simulation_id = {0}'.format(simulation_id))
        message_str = 'about to get fncs events'
        _send_simulation_status('RUNNING', message_str, 'DEBUG')
        message_events = fncs.get_events()
        message_str = 'fncs events '+str(message_events)
        _send_simulation_status('RUNNING', message_str, 'DEBUG')
        if simulation_id in message_events:
            fncs_output = fncs.get_value(simulation_id)
        message_str = 'fncs_output '+str(fncs_output)
        _send_simulation_status('RUNNING', message_str, 'DEBUG')
        return fncs_output
    except Exception as e:
        message_str = 'Error on get FncsBusMessages for '+str(simulation_id)+' '+str(e)
        _send_simulation_status('ERROR', message_str, 'ERROR')
        
        
def _done_with_time_step(current_time):
    """tell the fncs_broker to move to the next time step.
    
    Function arguments:
        current_time -- Type: integer. Description: the current time in seconds. 
            It must not be none.
    Function returns:
        None.
    Function exceptions:
        RuntimeError()
        ValueError()
    """
    try:
        message_str = 'In done with timestep '+str(current_time)
        _send_simulation_status('RUNNING', message_str, 'DEBUG')
        if current_time == None or type(current_time) != int:
            raise ValueError(
                'current_time must be an integer.\n'
                + 'current_time = {0}'.format(current_time))
        time_request = current_time + 1
        message_str = 'calling time_request '+str(time_request)
        _send_simulation_status('RUNNING', message_str, 'DEBUG')
        time_approved = fncs.time_request(time_request)
        message_str = 'time approved '+str(time_approved)
        _send_simulation_status('RUNNING', message_str, 'DEBUG')
        if time_approved != time_request:
            raise RuntimeError(
                'The time approved from fncs_broker is not the time requested.\n'
                + 'time_request = {0}.\ntime_approved = {1}'.format(time_request, 
                time_approved))
    except Exception as e:
        message_str = 'Error in fncs timestep '+str(e)
        _send_simulation_status('ERROR', message_str, 'ERROR')
        
            
def _register_with_goss(sim_id,username,password,goss_server='localhost', 
                      stomp_port='61613',):
    """Register with the GOSS server broker and return.
    
    Function arguments:
        sim_id -- Type: string. Description: The simulation id.
            It must not be an empty string. Default: None.
        goss_server -- Type: string. Description: The ip location
        for the GOSS server. It must not be an empty string.
            Default: 'localhost'.
        stomp_port -- Type: string. Description: The port for Stomp 
        protocol for the GOSS server. It must not be an empty string.
            Default: '61613'.
        username -- Type: string. Description: User name for GOSS connection.
        password -- Type: string. Description: Password for GOSS connection.
        
    Function returns:
        None.
    Function exceptions:
        RuntimeError()
    """
    global simulation_id
    global goss_connection
    simulation_id = sim_id
    if (goss_server == None or goss_server == ''
            or type(goss_server) != str):
        raise ValueError(
            'goss_server must be a nonempty string.\n' 
            + 'goss_server = {0}'.format(goss_server))
    if (stomp_port == None or stomp_port == ''
            or type(stomp_port) != str):
        raise ValueError(
            'stomp_port must be a nonempty string.\n' 
            + 'stomp_port = {0}'.format(stomp_port))
   
    goss_connection = stomp.Connection12([(goss_server, stomp_port)])
    goss_connection.start()
    goss_connection.connect(username,password, wait=True)
    goss_connection.set_listener('GOSSListener', GOSSListener())
    goss_connection.subscribe(input_from_goss_topic,1)
    goss_connection.subscribe(input_from_goss_queue,2)

    message_str = 'Registered with GOSS on topic '+input_from_goss_topic+' '+str(goss_connection.is_connected())
    _send_simulation_status('STARTED', message_str, 'INFO')
    
    
def _send_simulation_status(status, message, log_level):
    """send a status message to the GridAPPS-D log manager
    
    Function arguments:
        status -- Type: string. Description: The status of the simulation.
            Default: 'localhost'.
        stomp_port -- Type: string. Description: The port for Stomp 
        protocol for the GOSS server. It must not be an empty string.
            Default: '61613'.
        username -- Type: string. Description: User name for GOSS connection.
        password -- Type: string. Description: Password for GOSS connection.
        
    Function returns:
        None.
    Function exceptions:
        RuntimeError()
    """
    simulation_status_topic = "goss.gridappsd.process.log.simulation."+str(simulation_id)
	
    valid_status = ['STARTING', 'STARTED', 'RUNNING', 'ERROR', 'CLOSED', 'COMPLETE']
    valid_level = ['TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR', 'FATAL']
    if status in valid_status:
        if log_level not in valid_level:
            log_level = 'INFO'
        t_now = datetime.utcnow()
        status_message = {
            "processId" : "fncs_goss_bridge-"+str(simulation_id),
            "timestamp" : t_now.microsecond,
            "procesStatus" : status,
            "logMessage" : str(message),
            "logLevel" : log_level,
        }
        status_str = json.dumps(status_message)
        goss_connection.send(simulation_status_topic, status_str)
 

def _keep_alive():
    while 1:
        time.sleep(0.1)
         
def _main(simulation_id, simulation_broker_location='tcp://localhost:5570'):
    
    _register_with_goss(simulation_id,'system','manager',goss_server='127.0.0.1',stomp_port='61613')
    _register_with_fncs_broker(simulation_broker_location)
    _keep_alive()
        
if __name__ == "__main__":
    #TODO: send simulation_id, fncsBrokerLocation, gossLocation, 
    #stomp_port, username and password as commmand line arguments
    simulation_id = sys.argv[1]
    sim_broker_location = sys.argv[2]
    _main(simulation_id, sim_broker_location) 
    
