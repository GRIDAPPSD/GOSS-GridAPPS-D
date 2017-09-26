"""
Created on Sep 20, 2017

@author: fish334
@author: poorva1209
"""
from datetime import datetime
import json
import os
import sys
import time

import stomp

#TO-DO: read topics from exmaple_service.config
input_from_goss_topic = 'topic.goss.gridappsd.fncs.input' #this should match example_service.config input_topics
output_to_goss_topic = 'topic.goss.gridappsd.fncs.input' #this should match example_service.config output_topics

goss_connection= None
simulation_id = None
service_instance_id = None

class GOSSListener(object):
    def on_message(self, headers, msg):
        message = {}
        try:
            message_str = 'received message '+str(msg)
        
        except Exception as e:
            message_str = 'Error in command '+str(e)
            _send_simulation_status('error', message_str, 'error')
            if fncs.is_initialized():
                fncs.die()
           
        
    def on_error(self, headers, message):
        message_str = 'Error in goss listener '+str(message)
        _send_simulation_status('error', message_str, 'error')
            
    def on_disconnected(self):
        print("stopping example_service")
  
  
def _register_with_goss(username,password,goss_server='localhost', 
                      stomp_port='61613',):
    """Register with the GOSS server broker and return.
    
    Function arguments:
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
    
    message_str = 'Registered with GOSS on topic '+input_from_goss_topic+' '+str(goss_connection.is_connected())
    _send_simulation_status('started', message_str, 'info')
    
    
def _send_status_message():
    """send a status message to the GridAPPS-D log manager
    
    Function returns:
        None.
    Function exceptions:
        RuntimeError()
    """
    simulation_status_topic = "goss.gridappsd.process.log.simulation"
    valid_status = ['started', 'stopped', 'running', 'error', 'passed', 'failed']
    valid_level = ['info', 'debug', 'error']
    if status in valid_status:
        if log_level not in valid_level:
            log_level = 'info'
        t_now = datetime.utcnow()
        status_message = {
            "process_id" : "example",
            "parent_procesS_id" : str(simulation_id),
            "timestamp" : t_now.replace(microsecond=0).isoformat(" "),
            "status" : status,
            "log_message" : str(message),
            "log_level" : log_level
        }
        status_str = json.dumps(status_message)
        goss_connection.send(simulation_status_topic, status_str)

def _subcribe_to_input_topics():
    
     #Set a listener that would listen to messages coming on input topic
    goss_connection.set_listener('GOSSListener', GOSSListener())
    
    #Subscribe to input topics
    goss_connection.subscribe(input_from_goss_topic+"."+service_instance_id,1)
    
def _publish_to_output_topics():
    message = {"output":"some output"}
    goss_connection.send(simulation_status_topic+"."+service_instance_id, message)
    
        
def _main(instance_id, simulation_id, username, password, goss_server, stomp_port):
    
    _register_with_goss(username,password,goss_server,stomp_port)
    
    _subcribe_to_input_topics()
    
    _publish_to_output_topics()
    
    _send_status_message()
        
if __name__ == "__main__":
    
    global service_instance_id
    service_instance_id = sys.argv[1]
    
    global simulation_id
    simulation_is = sys.argv[2]
    
    username = sys.argv[3]
    password = sys.args[4]
    goss_server = sys.args[5]
    stomp_port = sys.args[6]
    _main(service_instance_id, simulation_id, username, password, goss_server, stomp_port) 
    
