
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
import traceback
from readline import get_begidx
"""
Created on Jan 6, 2017

@author: fish334
@author: poorva1209
"""
import argparse
import cmath
from datetime import datetime
import json
import math
import os
try:
    from Queue import Queue
except:
    from queue import Queue
import sys
import time

import stomp
import yaml

from gridappsd import GridAPPSD, utils, topics

try:
    from fncs import fncs
except:
    if not os.environ.get("CI"):
        raise ValueError("fncs.py is unavailable on the python path.")
    else:
        sys.stdout.write("Running tests.\n")
        fncs = {}

input_from_goss_topic = '/topic/goss.gridappsd.fncs.input' #this should match GridAppsDConstants.topic_FNCS_input
output_to_simulation_manager = 'goss.gridappsd.fncs.output'
output_to_goss_topic = '/topic/goss.gridappsd.simulation.output.' #this should match GridAppsDConstants.topic_FNCS_output
simulation_input_topic = '/topic/goss.gridappsd.simulation.input.'

goss_connection= None
is_initialized = False
simulation_id = None
stop_simulation = False

difference_attribute_map = {
    "RegulatingControl.mode" : {
        "capacitor" : {
            "property" : ["control"],
            "prefix" : "cap_"
        }
    },
    "RegulatingControl.targetDeadband" : {
        "capacitor" : {
            "property" : ["voltage_deadband", "VAr_deadband", "current_deadband"],
            "prefix" : "cap_"
        },
        "regulator" : {
            "property" : ["band_width"],
            "prefix" : "rcon_"
        }
    },
    "RegulatingControl.targetValue" : {
        "capacitor" : {
            "property" : ["voltage_center", "VAr_center", "current_center"],
            "prefix" : "cap_"
        },
        "regulator" : {
            "property" : ["band_center"],
            "prefix" : "rcon_"
        }
    },
    "RotatingMachine.p" : {
        "diesel_dg" : {
            "property" : ["real_power_out_{}"],
            "prefix" : "dg_"
        }
    },
    "RotatingMachine.q" : {
        "diesel_dg" : {
            "property" : ["reactive_power_out_{}"],
            "prefix" : "dg_"
        }
    },
    "ShuntCompensator.aVRDelay" : {
        "capacitor" : {
            "property" : ["dwell_time"],
            "prefix" : "cap_"
        }
    },
    "ShuntCompensator.sections" : {
        "capacitor" : {
            "property" : ["switch{}"],
            "prefix" : "cap_"
        }
    },
    "PowerElectronicsConnection.p": {
        "pv": {
            "property": ["P_Out"],
            "prefix": "inv_pv_"
        },
        "battery": {
            "property": ["P_Out"],
            "prefix": "inv_bat_"
        }
    },
    "PowerElectronicsConnection.q": {
        "pv": {
            "property": ["Q_Out"],
            "prefix": "inv_pv_"
        },
        "battery": {
            "property": ["Q_Out"],
            "prefix": "inv_bat_"
        }
    },
    "Switch.open" : {
        "switch" : {
            "property" : ["status"],
            "prefix" : "swt_"
        },
        "recloser" : {
            "property" : ["status"],
            "prefix" : "swt_"
        }
    },
    "TapChanger.initialDelay" : {
        "regulator" : {
            "property" : ["dwell_time"],
            "prefix" : "rcon_"
        }
    },
    "TapChanger.step" : {
        "regulator" : {
            "property" : ["tap_{}"],
            "prefix" : "reg_"
        }
    },
    "TapChanger.lineDropCompensation" : {
        "regulator" : {
            "property" : ["Control"],
            "prefix" : "rcon_"
        }
    },
    "TapChanger.lineDropR" : {
        "regulator" : {
            "property" : ["compensator_r_setting_{}"],

            "prefix" : "rcon_"
        }
    },
    "TapChanger.lineDropX" : {
        "regulator" : {
            "property" : ["compensator_x_setting_{}"],
            "prefix" : "rcon_"
        }
    }
}

class GOSSListener(object):

    def __init__(self, sim_length, sim_start):
        self.goss_to_fncs_message_queue = Queue()
        self.start_simulation = False
        self.stop_simulation = False
        self.pause_simulation = False
        self.simulation_finished = True
        self.simulation_length = sim_length
        self.simulation_start = sim_start
        self.simulation_time = 0
        self.measurement_filter = []
        self.command_filter = []
        self.filter_all_commands = False
        self.filter_all_measurements = False
        self.message_id_list = []

    def run_simulation(self,run_realtime):
        try:
            message = {}
            current_time = 0;
            message['command'] = 'nextTimeStep'
            for current_time in range(self.simulation_length):
                while self.pause_simulation == True:
                    time.sleep(1)
                if self.stop_simulation == True:
                    if fncs.is_initialized():
                        fncs.die()
                    break
                goss_connection.send("goss.gridappsd.fncs.timestamp.{}".format(simulation_id), json.dumps({"timestamp": current_time + self.simulation_start}))
                #forward messages from FNCS to GOSS
                if self.filter_all_measurements == False:
                    message['output'] = _get_fncs_bus_messages(simulation_id, self.measurement_filter)
                else:
                    message['output'] = {}
                response_msg = json.dumps(message['output'])
                if message['output']!={}:
                    goss_connection.send(output_to_goss_topic + "{}".format(simulation_id) , response_msg)
                #forward messages from GOSS to FNCS
                while not self.goss_to_fncs_message_queue.empty():
                    _publish_to_fncs_bus(simulation_id, self.goss_to_fncs_message_queue.get(), self.command_filter)
                _done_with_time_step(current_time) #current_time is incrementing integer 0 ,1, 2.... representing seconds
                message_str = 'done with timestep '+str(current_time)
                _send_simulation_status('RUNNING', message_str, 'DEBUG')
                message_str = 'incrementing to '+str(current_time + 1)
                _send_simulation_status('RUNNING', message_str, 'DEBUG')
                if run_realtime == True:
                    time.sleep(1)
            self.stop_simulation = True
            message['command'] = 'simulationFinished'
            del message['output']
            goss_connection.send(output_to_simulation_manager, json.dumps(message))
            _send_simulation_status('COMPLETE', 'Simulation {} has finsihed.'.format(simulation_id), 'INFO')
        except Exception as e:
            message_str = 'Error in run simulation '+str(e)
            _send_simulation_status('ERROR', message_str, 'ERROR')
            self.stop_simulation = True
            if fncs.is_initialized():
                fncs.die()


    def on_message(self, headers, msg):
        message = {}
        try:
            headers_dict = yaml.safe_load(str(headers))
            destination = headers_dict['destination']
            message_id = headers_dict['message-id']
            if str(destination).startswith('/temp-queue'):
                return
            if str(message_id) in self.message_id_list:
                return
            self.message_id_list.append(str(message_id))
            message_str = 'received message '+str(headers)+'________________'+str(msg)

            if fncs.is_initialized():
                _send_simulation_status('RUNNING', message_str, 'DEBUG')
            else:
                _send_simulation_status('STARTED', message_str, 'DEBUG')
            json_msg = yaml.safe_load(str(msg))
            #print("\n{}\n".format(json_msg['command']))
            if json_msg.get('command', '') == 'isInitialized':
                message_str = 'isInitialized check: '+str(is_initialized)
                if fncs.is_initialized():
                    _send_simulation_status('RUNNING', message_str, 'DEBUG')
                else:
                    _send_simulation_status('STARTED', message_str, 'DEBUG')
                message['command'] = 'isInitialized'
                message['response'] = str(is_initialized)
                t_now = datetime.utcnow()
                message['timestamp'] = int(time.mktime(t_now.timetuple()))
                goss_connection.send(output_to_simulation_manager , json.dumps(message))
            elif json_msg.get('command', '') == 'update':
                message['command'] = 'update'
                if self.filter_all_commands == False:
                    self.goss_to_fncs_message_queue.put(json.dumps(json_msg['input']))
                #_publish_to_fncs_bus(simulation_id, json.dumps(json_msg['input'])) #does not return
            elif json_msg.get('command', '') == 'StartSimulation':
                if self.start_simulation == False:
                    self.start_simulation = True
                #message['command'] = 'nextTimeStep'
                #current_time = json_msg['currentTime']
                #message_str = 'incrementing to '+str(current_time + 1)
                #_send_simulation_status('RUNNING', message_str, 'DEBUG')
                #_done_with_time_step(current_time) #current_time is incrementing integer 0 ,1, 2.... representing seconds
                #message['response'] = "True"
                #t_now = datetime.utcnow()
                #message['timestamp'] = int(time.mktime(t_now.timetuple()))
                #goss_connection.send(output_to_simulation_manager, json.dumps(message))
                #del message['response']
                #message_str = 'done with timestep '+str(current_time)
                #_send_simulation_status('RUNNING', message_str, 'DEBUG')
                #message['output'] = _get_fncs_bus_messages(simulation_id)
                #response_msg = json.dumps(message['output'])
                #goss_connection.send(output_to_goss_topic + "{}".format(simulation_id) , response_msg)
            elif json_msg.get('command', '') == 'CommOutage':
                rev_diffs = json_msg.get('input',{}).get('reverse_differences', [])
                for_diffs = json_msg.get('input',{}).get('forward_differences', [])
                for d in rev_diffs:
                    if d.get('allInputOutage', False) == True:
                        self.filter_all_commands = False
                    else:
                        for x in d.get('inputOutageList', []):
                            try:
                                #idx = self.command_filter.find(x)
                                #del self.command_filter[idx]
                                self.command_filter.remove(x)
                            except ValueError as ve:
                                pass
                    if d.get('allOutputOutage', False) == True:
                        self.filter_all_measurements = False
                    else:
                        for x in d.get('outputOutageList', []):
                            try:
                                #idx = self.measurement_filter.find(x)
                                #del self.measurement_filter[idx]
                                self.measurement_filter.remove(x)
                            except ValueError as ve:
                                pass
                for d in for_diffs:
                    if d.get('allInputOutage', False) == True:
                        self.filter_all_commands = True
                    else:
                        for x in d.get('inputOutageList', []):
                            if x not in self.command_filter:
                                self.command_filter.append(x)
                    if d.get('allOutputOutage', False) == True:
                        self.filter_all_measurements = True
                    else:
                        for x in d.get('outputOutageList', []):
                            if x not in self.measurement_filter:
                                self.measurement_filter.append(x)
            elif json_msg.get('command', '') == 'stop':
                message_str = 'Stopping the simulation'
                _send_simulation_status('CLOSED', message_str, 'INFO')
                self.stop_simulation = True
                if fncs.is_initialized():
                    if self.simulation_finished == False:
                        fncs.die()
            elif json_msg.get('command', '') == 'pause':
                if self.pause_simulation == True:
                    _send_simulation_status('PAUSED', 'The simulation is already paused.', 'WARN')
                else:
                    self.pause_simulation = True
                    _send_simulation_status('PAUSED', 'The simulation has paused.', 'INFO')
            elif json_msg.get('command', '') == 'resume':
                if self.pause_simulation == False:
                    _send_simulation_status('RUNNING', 'The simulation is already running.', 'WARN')
                else:
                    self.pause_simulation = False
                    _send_simulation_status('RUNNING', 'The simulation has resumed.', 'INFO')
            elif json_msg.get('command', '') == '':
                _send_simulation_status('WARNING', 'The message recieved did not have a command key. Ignoring malformed message.', 'WARN')

        except Exception as e:
            message_str = 'Error '+str(e)+' in command '+str(msg)
            _send_simulation_status('ERROR', message_str, 'ERROR')
            self.stop_simulation = True
            if fncs.is_initialized():
                fncs.die()
        
    def on_error(self, headers, message):
        message_str = 'Error in goss listener '+str(message)
        _send_simulation_status('ERROR', message_str, 'ERROR')
        self.stop_simulation = True
        if fncs.is_initialized():
            fncs.die()


    def on_disconnected(self):
        self.stop_simulation = True
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
        ('STARTED', message_str, 'INFO')

        message_str = 'still connected to goss 1 '+str(goss_connection.connected)
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
        goss_listener_instance.stop_simulation = True
        if fncs.is_initialized():
            fncs.die()

    if not fncs.is_initialized():
        message_str = 'fncs.initialize(configuration_zpl) failed!\n' + 'configuration_zpl = {0}'.format(configuration_zpl)
        _send_simulation_status('ERROR', message_str, 'ERROR')
        goss_listener_instance.stop_simulation = True
        if fncs.is_initialized():
            fncs.die()
        raise RuntimeError(
            'fncs.initialize(configuration_zpl) failed!\n'
            + 'configuration_zpl = {0}'.format(configuration_zpl))


def _get_gld_object_name(object_mrid):
    prefix = ""
    stored_object = object_mrid_to_name.get(object_mrid)
    if stored_object == None:
        cim_object_dict = goss_connection.query_object_dictionary(model_id=model_mrid, object_id=object_mrid)
        object_base_name = (cim_object_dict.get("data",[]))[0].get("IdentifiedObject.name","")
        object_type = (cim_object_dict.get("data",[]))[0].get("type","")
        if object_type == "LinearShuntCompensator":
            prefix = "cap_"
        elif object_type == "PowerTransformer":
            prefix = "xf_"
        elif object_type == "ACLineSegment":
            prefix = "line_"
        elif object_type in ["LoadBreakSwitch","Recloser","Breaker"]:
            prefix = "sw_"
        elif object_type == "RatioTapChanger":
            prefix = "reg_"
    else:
        object_base_name = stored_object.get("name","")
        prefix = stored_object.get("prefix","")
    object_name = prefix + object_base_name
    return object_name
    

def _publish_to_fncs_bus(simulation_id, goss_message, command_filter):
    """publish a message received from the GOSS bus to the FNCS bus.

    Function arguments:
        simulation_id -- Type: string. Description: The simulation id.
            It must not be an empty string. Default: None.
        goss_message -- Type: string. Description: The message from the GOSS bus
            as a json string. It must not be an empty string. Default: None.
        command_filter -- Type: list. Description: The list of
            command attributes to filter from the simulator input.
    Function returns:
        None.
    Function exceptions:
        RuntimeError()
        ValueError()
    """
    message_str = 'translating following message for fncs simulation '+simulation_id+' '+str(goss_message)
    _send_simulation_status('RUNNING', message_str, 'DEBUG')
    print(message_str)

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
        fncs_input_topic = '{0}/fncs_input'.format(simulation_id)
        fncs_input_message = {"{}".format(simulation_id) : {}}
        fncs_input_message["{}".format(simulation_id)]["external_event_handler"] = {}
        forward_differences_list = test_goss_message_format["message"]["forward_differences"]
        reverse_differences_list = test_goss_message_format["message"]["reverse_differences"]
        fault_list = []
        for x in forward_differences_list:
            command_pair = {
                "objectMRID": x.get("object", ""),
                "attribute": x.get("attribute", "")
            }
            if x.get("attribute", "") != "IdentifiedObject.Fault":
                if command_pair not in command_filter:
                    object_name = (object_mrid_to_name.get(x.get("object"))).get("name")
                    # _send_simulation_status("ERROR", "Jeff1 " + object_name, "ERROR")
                    object_phases = (object_mrid_to_name.get(x.get("object"))).get("phases")
                    # _send_simulation_status("ERROR", "Jeff2 " + object_phases, "ERROR")
                    object_total_phases = (object_mrid_to_name.get(x.get("object"))).get("total_phases")
                    # _send_simulation_status("ERROR", "Jeff3 " + object_total_phases, "ERROR")
                    object_type = (object_mrid_to_name.get(x.get("object"))).get("type")
                    # _send_simulation_status("ERROR", "Jeff4 " + object_type + " " + x.get("attribute"), "ERROR")
                    object_name_prefix = ((difference_attribute_map.get(x.get("attribute"))).get(object_type)).get("prefix")
                    # _send_simulation_status("ERROR", "Jeff5 " + object_name_prefix, "ERROR")
                    cim_attribute = x.get("attribute")
        
                    object_property_list = ((difference_attribute_map.get(x.get("attribute"))).get(object_type)).get("property")
                    # _send_simulation_status("ERROR", "Jeff6 " + str(object_property_list), "ERROR")
                    phase_in_property = ((difference_attribute_map.get(x.get("attribute"))).get(object_type)).get("phase_sensitive",False)
                    # _send_simulation_status("ERROR", "Jeff7 " + str(phase_in_property), "ERROR")
                    if (object_name_prefix + object_name) not in fncs_input_message["{}".format(simulation_id)].keys():
                        fncs_input_message["{}".format(simulation_id)][object_name_prefix + object_name] = {}
                    if cim_attribute == "RegulatingControl.mode":
                        val = int(x.get("value"))
                        if val == 0:
                            fncs_input_message["{}".format(simulation_id)][object_name_prefix + object_name][object_property_list[0]] = "VOLT"
                        if val == 1:
                            fncs_input_message["{}".format(simulation_id)][object_name_prefix + object_name][object_property_list[0]] = "MANUAL"
                        elif val == 2:
                            fncs_input_message["{}".format(simulation_id)][object_name_prefix + object_name][object_property_list[0]] = "VAR"
                        elif val == 3:
                            fncs_input_message["{}".format(simulation_id)][object_name_prefix + object_name][object_property_list[0]] = "CURRENT"
                        else:
                            fncs_input_message["{}".format(simulation_id)][object_name_prefix + object_name][object_property_list[0]] = "MANUAL"
                            _send_simulation_status("RUNNING", "Unsupported capacitor control mode requested. The only supported control modes for capacitors are voltage, VAr, volt/VAr, and current. Setting control mode to MANUAL.","WARN")
                    elif cim_attribute == "RegulatingControl.targetDeadband":
                        for y in difference_attribute_map[cim_attribute][object_type]["property"]:
                            fncs_input_message["{}".format(simulation_id)][object_name_prefix + object_name][y] = float(x.get("value"))
                    elif cim_attribute == "RegulatingControl.targetValue":
                        for y in difference_attribute_map[cim_attribute][object_type]["property"]:
                            fncs_input_message["{}".format(simulation_id)][object_name_prefix + object_name][y] = float(x.get("value"))
                    elif cim_attribute == "RotatingMachine.p":
                        for y in object_phases:
                            fncs_input_message["{}".format(simulation_id)][object_name_prefix + object_name][object_property_list[0].format(y)] = float(x.get("value"))/3.0
                    elif cim_attribute == "RotatingMachine.q":
                        for y in object_phases:
                            fncs_input_message["{}".format(simulation_id)][object_name_prefix + object_name][object_property_list[0].format(y)] = float(x.get("value"))/3.0
                    elif cim_attribute == "ShuntCompensator.aVRDelay":
                        for y in difference_attribute_map[cim_attribute][object_type]["property"]:
                            fncs_input_message["{}".format(simulation_id)][object_name_prefix + object_name][y] = float(x.get("value"))
                    elif cim_attribute == "ShuntCompensator.sections":
                        if int(x.get("value")) == 1:
                            val = "CLOSED"
                        else:
                            val = "OPEN"
                        for y in object_phases:
                            fncs_input_message["{}".format(simulation_id)][object_name_prefix + object_name][object_property_list[0].format(y)] = "{}".format(val)
                    elif cim_attribute == "Switch.open":
                        if int(x.get("value")) == 1:
                            val = "OPEN"
                        else:
                            val = "CLOSED"
                        fncs_input_message["{}".format(simulation_id)][object_name_prefix + object_name][object_property_list[0]] = "{}".format(val)
                    elif cim_attribute == "TapChanger.initialDelay":
                        for y in object_property_list:
                            fncs_input_message["{}".format(simulation_id)][object_name_prefix + object_name][y] = float(x.get("value"))
                    elif cim_attribute == "TapChanger.step":
                        for y in object_phases:
                            fncs_input_message["{}".format(simulation_id)][object_name_prefix + object_name][object_property_list[0].format(y)] = int(x.get("value"))
                    elif cim_attribute == "TapChanger.lineDropCompensation":
                        if int(x.get("value")) == 1:
                            val = "LINE_DROP_COMP"
                        else:
                            val = "MANUAL"
                        fncs_input_message["{}".format(simulation_id)][object_name_prefix + object_name][object_property_list[0]] = "{}".format(val)
                    elif cim_attribute == "TapChanger.lineDropR":
                        for y in object_phases:
                            fncs_input_message["{}".format(simulation_id)][object_name_prefix + object_name][object_property_list[0].format(y)] = float(x.get("value"))
                    elif cim_attribute == "TapChanger.lineDropX":
                        for y in object_phases:
                          fncs_input_message["{}".format(simulation_id)][object_name_prefix + object_name][object_property_list[0].format(y)] = float(x.get("value"))
                    elif cim_attribute == "PowerElectronicsConnection.p":
                        for y in object_phases:
                            fncs_input_message["{}".format(simulation_id)][object_name_prefix + object_name][object_property_list[0]] = float(x.get("value"))
                    elif cim_attribute == "PowerElectronicsConnection.q":
                        for y in object_phases:
                            fncs_input_message["{}".format(simulation_id)][object_name_prefix + object_name][object_property_list[0]] = float(x.get("value"))
                    else:
                        _send_simulation_status("RUNNING", "Attribute, {}, is not a supported attribute in the simulator at this current time. ignoring difference.", "WARN")
    
            else:
                fault_val_dict = {}
                fault_val_dict["name"] = x.get("object","")
                fault_object_mrid = (x.get("value",{})).get("ObjectMRID","")               
                fault_val_dict["fault_object"] = _get_gld_object_name(fault_object_mrid)
                phases = (x.get("value",{})).get("PhaseCode","")
                fault_kind_type = (x.get("value",{})).get("PhaseConnectedFaultKind","")
                fault_type = ""
                if fault_kind_type == "lineToGround":
                    fault_type = "SLG-{}".format(phases)
                elif fault_kind_type == "lineToLine":
                    if len(phases) == 3:
                        fault_type = "TLL"
                    else:
                        fault_type = "LL-{}".format(phases)
                elif fault_kind_type == "lineToLineToGround":
                    if len(phases) == 3:
                        fault_type = "TLG"
                    else:
                        fault_type = "DLG-{}".format(phases)
                elif fault_kind_type == "lineOpen":
                    if len(phases) == 3:
                        fault_type = "OC3"
                    elif len(phases) == 2:
                        fault_type = "OC2-{}".format(phases)
                    else:
                        fault_type = "OC-{}".format(phases)
                fault_val_dict["type"] = fault_type
                fault_list.append(fault_val_dict)
        for x in reverse_differences_list:
            if x.get("attribute", "") == "IdentifiedObject.Fault":
                fault_val_dict = {}
                fault_val_dict["name"] = x.get("object", "")
                fault_list.append(fault_val_dict)
        if len(fault_list) != 0:
            fncs_input_message["{}".format(simulation_id)]["external_event_handler"]["external_fault_event"] = json.dumps(fault_list)
        goss_message_converted = json.dumps(fncs_input_message)
        _send_simulation_status("RUNNING", "Sending the following message to the simulator. {}".format(goss_message_converted),"INFO")
        if fncs.is_initialized() and fncs_input_message["{}".format(simulation_id)] != {}:
            fncs.publish_anon(fncs_input_topic, goss_message_converted)
    except ValueError as ve:
        raise ValueError(ve)
    except Exception as ex:
        _send_simulation_status("ERROR","An error occured while trying to translate the update message received","ERROR")
        _send_simulation_status("ERROR",str(ex),"ERROR")
	#raise RuntimeError("An error occurred while trying to translate the update message recieved.\n{}: {}".format(type(ex).__name__, ex.message))



def _get_fncs_bus_messages(simulation_id, measurement_filter):
    """publish a message received from the GOSS bus to the FNCS bus.

    Function arguments:
        simulation_id -- Type: string. Description: The simulation id.
            It must not be an empty string. Default: None.
        measurement_filter -- Type: list. Description: The list of
            measurement id's to filter from the simulator output.
    Function returns:
        fncs_output -- Type: string. Description: The json structured output
            from the simulation. If no output was sent from the simulation then
            it returns None.
    Function exceptions:
        ValueError()
    """
    propertyName = ""
    objectName = ""
    objectType = ""
    propertyValue = ""
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
        t_now = datetime.utcnow()
        cim_output = {}
        if simulation_id in message_events:
            cim_measurements_dict = {
                "simulation_id": simulation_id,
                "message" : {
                    "timestamp" : int(time.mktime(t_now.timetuple())),
                    "measurements" : []
                }
            }

            fncs_output = fncs.get_value(simulation_id)
            fncs_output_dict = json.loads(fncs_output) #json_loads_byteified(fncs_output)

            sim_dict = fncs_output_dict.get(simulation_id, None)

            if sim_dict != None:
                simulation_time = int(sim_dict.get("globals",{"clock" : "0"}).get("clock", "0"))
                if simulation_time != 0:
                    cim_measurements_dict["message"]["timestamp"] = simulation_time
                for x in object_property_to_measurement_id.keys():
                    objectName = x
                    gld_properties_dict = sim_dict.get(x,None)
                    if gld_properties_dict == None:
                        err_msg = "All measurements for object {} are missing from the simulator output.".format(x)
                        _send_simulation_status('RUNNING', err_msg, 'WARN')
                        #raise RuntimeError(err_msg)
                    else:
                        for y in object_property_to_measurement_id.get(x,{}):
                            measurement = {}
                            property_name = y["property"]
                            propertyName = property_name
                            if y["measurement_mrid"] not in measurement_filter:
                                measurement["measurement_mrid"] = y["measurement_mrid"]
                                phases = y["phases"]
                                conducting_equipment_type_str = y["conducting_equipment_type"]
                                prop_val_str = gld_properties_dict.get(property_name, None)
                                propertyValue = prop_val_str
                                objectType = conducting_equipment_type_str
                                if prop_val_str == None:
                                    err_msg = "{} measurement for object {} is missing from the simulator output.".format(property_name, x)
                                    _send_simulation_status('RUNNING', err_msg, 'WARN')
                                    #raise RuntimeError("{} measurement for object {} is missing from the simulator output.".format(property_name, x))
                                else:
                                    val_str = str(prop_val_str).split(" ")[0]
                                    conducting_equipment_type = str(conducting_equipment_type_str).split("_")[0]
                                    if conducting_equipment_type == "LinearShuntCompensator":
                                        if property_name in ["shunt_"+phases,"voltage_"+phases]:
                                            val = complex(val_str)
                                            (mag,ang_rad) = cmath.polar(val)
                                            ang_deg = math.degrees(ang_rad)
                                            measurement["magnitude"] = mag
                                            measurement["angle"] = ang_deg
                                        else:
                                            if val_str == "OPEN":
                                                measurement["value"] = 0
                                            else:
                                                measurement["value"] = 1
                                    elif conducting_equipment_type == "PowerTransformer":
                                        if property_name in ["power_in_"+phases,"voltage_"+phases,"current_in_"+phases]:
                                            val = complex(val_str)
                                            (mag,ang_rad) = cmath.polar(val)
                                            ang_deg = math.degrees(ang_rad)
                                            measurement["magnitude"] = mag
                                            measurement["angle"] = ang_deg
                                        else:
                                            measurement["value"] = int(val_str)
                                    elif conducting_equipment_type in ["ACLineSegment","EnergyConsumer","PowerElectronicsConnection","SynchronousMachine"]:
                                        val = complex(val_str)
                                        (mag,ang_rad) = cmath.polar(val)
                                        ang_deg = math.degrees(ang_rad)
                                        measurement["magnitude"] = mag
                                        measurement["angle"] = ang_deg
                                    elif conducting_equipment_type in ["LoadBreakSwitch", "Recloser", "Breaker"]:
                                        if property_name in ["power_in_"+phases,"voltage_"+phases,"current_in_"+phases]:
                                            val = complex(val_str)
                                            (mag,ang_rad) = cmath.polar(val)
                                            ang_deg = math.degrees(ang_rad)
                                            measurement["magnitude"] = mag
                                            measurement["angle"] = ang_deg
                                        else:
                                            if val_str == "OPEN":
                                                measurement["value"] = 0
                                            else:
                                                measurement["value"] = 1
                                    elif conducting_equipment_type == "RatioTapChanger":
                                        if property_name in ["power_in_"+phases,"voltage_"+phases,"current_in_"+phases]:
                                            val = complex(val_str)
                                            (mag,ang_rad) = cmath.polar(val)
                                            ang_deg = math.degrees(ang_rad)
                                            measurement["magnitude"] = mag
                                            measurement["angle"] = ang_deg
                                        else:
                                            measurement["value"] = int(val_str)
                                    else:
                                        _send_simulation_status('RUNNING', conducting_equipment_type+" not recognized", 'WARN')
                                        raise RuntimeError("{} is not a recognized conducting equipment type.".format(conducting_equipment_type))
                                        # Should it raise runtime?
                                    cim_measurements_dict["message"]["measurements"].append(measurement)
                cim_output = cim_measurements_dict
            else:
                err_msg = "The message recieved from the simulator did not have the simulation id as a key in the json message."
                _send_simulation_status('ERROR', err_msg, 'ERROR')
                raise RuntimeError(err_msg)
        #_send_simulation_status('RUNNING', message_str, 'INFO')
        return cim_output
        #return fncs_output
    except ValueError as ve:
        raise RuntimeError("{}.\nObject Name: {}\nObject Type: {}\nProperty Name: {}\n Property Value{}".format(str(ve), objectName, objectType, propertyName, propertyValue))
    except Exception as e:
        message_str = 'Error on get FncsBusMessages for '+str(simulation_id)+' '+str(traceback.format_exc())
        print(message_str)
        traceback.print_exc()
        _send_simulation_status('ERROR', message_str, 'ERROR')
        return {}


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
        message_str = 'Done with timestep '+str(current_time)
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
                      stomp_port='61613', sim_duration=86400, sim_start=0):
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
    global goss_listener_instance
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
    goss_listener_instance = GOSSListener(sim_duration, sim_start)
    
    #goss_connection = stomp.Connection12([(goss_server, stomp_port)])
    #goss_connection.start()
    #goss_connection.connect(username,password, wait=True)
    goss_connection = GridAPPSD(simulation_id, address=utils.get_gridappsd_address(),
                                username=utils.get_gridappsd_user(), password=utils.get_gridappsd_pass())
    #goss_connection.set_listener('GOSSListener', goss_listener_instance)
    #goss_connection.subscribe(input_from_goss_topic,1)
    #goss_connection.subscribe(simulation_input_topic + "{}".format(simulation_id),2)
    goss_connection.subscribe(input_from_goss_topic, goss_listener_instance)
    goss_connection.subscribe("{}{}".format(simulation_input_topic, simulation_id), goss_listener_instance)

    message_str = 'Registered with GOSS on topic '+input_from_goss_topic+' '+str(goss_connection.connected)
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
    simulation_status_topic = "/topic/goss.gridappsd.simulation.log.{}".format(simulation_id)

    valid_status = ['STARTING', 'STARTED', 'RUNNING', 'ERROR', 'CLOSED', 'COMPLETE']
    valid_level = ['TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR', 'FATAL']
    if status in valid_status:
        if log_level not in valid_level:
            log_level = 'INFO'
        t_now = datetime.utcnow()
        status_message = {
            "source" : os.path.basename(__file__),
            "processId" : str(simulation_id),
            "timestamp" : int(time.mktime(t_now.timetuple()))*1000,
            "processStatus" : status,
            "logMessage" : str(message),
            "logLevel" : log_level,
            "storeToDb" : True
        }
        status_str = json.dumps(status_message)
        goss_connection.send(simulation_status_topic,status_str)


def _byteify(data, ignore_dicts = False):
    # if this is a unicode string, return its string representation
    if isinstance(data, unicode):
        return data.encode('utf-8')
    # if this is a list of values, return list of byteified values
    if isinstance(data, list):
        return [ _byteify(item, ignore_dicts=True) for item in data ]
    # if this is a dictionary, return dictionary of byteified keys and values
    # but only if we haven't already byteified it
    if isinstance(data, dict) and not ignore_dicts:
        return {
            _byteify(key, ignore_dicts=True): _byteify(value, ignore_dicts=True)
            for key, value in data.iteritems()
        }
    # if it's anything else, return it in its original form
    return data


def _create_cim_object_map(map_file=None):
    global object_property_to_measurement_id
    global object_mrid_to_name
    global model_mrid
    if map_file==None:
        object_property_to_measurement_id = None
        object_mrid_to_name = None
    else:
        try:
            with open(map_file, "r", encoding="utf-8") as file_input_stream:
                file_dict = json.load(file_input_stream) #json_load_byteified(file_input_stream)
            feeders = file_dict.get("feeders",[])
            object_property_to_measurement_id = {}
            object_mrid_to_name = {}
            for x in feeders:
                model_mrid = x.get("mRID","")
                measurements = x.get("measurements",[])
                capacitors = x.get("capacitors",[])
                regulators = x.get("regulators",[])
                switches = x.get("switches",[])
                batteries = x.get("batteries", [])
                solarpanels = x.get("solarpanels",[])
                synchronousMachines = x.get("synchronousmachines", [])
                breakers = x.get("breakers", [])
                reclosers = x.get("reclosers", [])
                #TODO: add more object types to handle
                for y in measurements:
                    measurement_type = y.get("measurementType")
                    phases = y.get("phases")
                    if phases == "s1":
                        phases = "1"
                    elif phases == "s2":
                        phases = "2"
                    conducting_equipment_type = y.get("name")
                    conducting_equipment_name = y.get("SimObject")
                    connectivity_node = y.get("ConnectivityNode")
                    measurement_mrid = y.get("mRID")
                    if "LinearShuntCompensator" in conducting_equipment_type:
                        if measurement_type == "VA":
                            object_name = conducting_equipment_name;
                            property_name = "shunt_" + phases;
                        elif measurement_type == "Pos":
                            object_name = conducting_equipment_name;
                            property_name = "switch" + phases;
                        elif measurement_type == "PNV":
                            object_name = conducting_equipment_name;
                            property_name = "voltage_" + phases;
                        else:
                            raise RuntimeError("_create_cim_object_map: The value of measurement_type is not a valid type.\nValid types for LinearShuntCompensators are VA, Pos, and PNV.\nmeasurement_type = {}.".format(measurement_type))
                    elif "PowerTransformer" in conducting_equipment_type:
                        if measurement_type == "VA":
                            object_name = conducting_equipment_name;
                            property_name = "power_in_" + phases;
                        elif measurement_type == "PNV":
                            object_name = connectivity_node;
                            property_name = "voltage_" + phases;
                        elif measurement_type == "A":
                            object_name = conducting_equipment_name;
                            property_name = "current_in_" + phases;
                        else:
                            raise RuntimeError("_create_cim_object_map: The value of measurement_type is not a valid type.\nValid types for PowerTransformer are VA, PNV, and A.\nmeasurement_type = {}.".format(measurement_type))
                    elif "RatioTapChanger" in conducting_equipment_type:
                        if measurement_type == "VA":
                            object_name = conducting_equipment_name;
                            property_name = "power_in_" + phases;
                        elif measurement_type == "PNV":
                            object_name = connectivity_node;
                            property_name = "voltage_" + phases;
                        elif measurement_type == "Pos":
                            object_name = conducting_equipment_name;
                            property_name = "tap_" + phases;
                        elif measurement_type == "A":
                            object_name = conducting_equipment_name;
                            property_name = "current_in_" + phases;
                        else:
                            raise RuntimeError("_create_cim_object_map: The value of measurement_type is not a valid type.\nValid types for RatioTapChanger are VA, PNV, Pos, and A.\nmeasurement_type = {}.".format(measurement_type))
                    elif "ACLineSegment" in conducting_equipment_type:
                        if measurement_type == "VA":
                            object_name = conducting_equipment_name;
                            if phases == "1":
                                property_name = "power_in_A"
                            elif phases == "2":
                                property_name = "power_in_B"
                            else:
                                property_name = "power_in_" + phases
                        elif measurement_type == "PNV":
                            object_name = connectivity_node;
                            property_name = "voltage_" + phases;
                        elif measurement_type == "A":
                            object_name = conducting_equipment_name;
                            if phases == "1":
                                property_name = "current_in_A"
                            elif phases == "2":
                                property_name = "current_in_B"
                            else:
                                property_name = "current_in_" + phases
                        else:
                            raise RuntimeError("_create_cim_object_map: The value of measurement_type is not a valid type.\nValid types for ACLineSegment are VA, PNV, and A.\nmeasurement_type = {}.".format(measurement_type))
                    elif "LoadBreakSwitch" in conducting_equipment_type or "Recloser" in conducting_equipment_type or "Breaker" in conducting_equipment_type:
                        if measurement_type == "VA":
                            object_name = conducting_equipment_name;
                            property_name = "power_in_" + phases;
                        elif measurement_type == "PNV":
                            object_name = connectivity_node;
                            property_name = "voltage_" + phases;
                        elif measurement_type == "Pos":
                            object_name = conducting_equipment_name
                            property_name = "status"
                        elif measurement_type == "A":
                            object_name = conducting_equipment_name;
                            property_name = "current_in_" + phases;
                        else:
                            raise RuntimeError("_create_cim_object_map: The value of measurement_type is not a valid type.\nValid types for LoadBreakSwitch are VA, PNV, and A.\nmeasurement_type = {}.".format(measurement_type))
                    elif "EnergyConsumer" in conducting_equipment_type:
                        if measurement_type == "VA":
                            object_name = conducting_equipment_name;
                            if phases in ["1","2"]:
                                property_name = "measured_power_" + phases;
                            else:
                                property_name = "measured_power_" + phases;
                        elif measurement_type == "PNV":
                            object_name = connectivity_node;
                            property_name = "voltage_" + phases;
                        elif measurement_type == "A":
                            object_name = connectivity_node;
                            property_name = "measured_current_" + phases;
                        else:
                            raise RuntimeError("_create_cim_object_map: The value of measurement_type is not a valid type.\nValid types for EnergyConsumer are VA, A, and PNV.\nmeasurement_type = %s.".format(measurement_type))
                    elif "PowerElectronicsConnection" in conducting_equipment_type:
                        if measurement_type == "VA":
                            object_name = conducting_equipment_name;
                            if phases in ["1","2"]:
                                property_name = "indiv_measured_power_" + phases;
                            else:
                                property_name = "measured_power_" + phases;
                        elif measurement_type == "PNV":
                            object_name = conducting_equipment_name;
                            property_name = "voltage_" + phases;
                        elif measurement_type == "A":
                            object_name = conducting_equipment_name;
                            property_name = "measured_current_" + phases;
                        else:
                            raise RuntimeError("_create_cim_object_map: The value of measurement_type is not a valid type.\nValid types for PowerElectronicsConnection are VA, A, and PNV.\nmeasurement_type = %s.".format(measurement_type))
                    elif "SynchronousMachine" in conducting_equipment_type:
                        if measurement_type == "VA":
                            object_name = conducting_equipment_name;
                            property_name = "measured_power_" + phases;
                        elif measurement_type == "PNV":
                            object_name = connectivity_node;
                            property_name = "voltage_" + phases;
                        elif measurement_type == "A":
                            object_name = connectivity_node;
                            property_name = "measured_current_" + phases;
                        else:
                            raise RuntimeError("_create_cim_object_map: The value of measurement_type is not a valid type.\nValid types for SynchronousMachine are VA, A, and PNV.\nmeasurement_type = %s.".format(measurement_type))
                    else:
                        raise RuntimeError("_create_cim_object_map: The value of conducting_equipment_type is not a valid type.\nValid types for conducting_equipment_type are ACLineSegment, LinearShuntCompesator, LoadBreakSwitch, PowerElectronicsConnection, EnergyConsumer, RatioTapChanger, and PowerTransformer.\conducting_equipment_type = {}.".format(conducting_equipment_type))

                    property_dict = {
                        "property" : property_name,
                        "conducting_equipment_type" : conducting_equipment_type,
                        "measurement_mrid" : measurement_mrid,
                        "phases" : phases
                    }
                    if object_name in object_property_to_measurement_id.keys():
                        object_property_to_measurement_id[object_name].append(property_dict)
                    else:
                        object_property_to_measurement_id[object_name] = []
                        object_property_to_measurement_id[object_name].append(property_dict)
                for y in capacitors:
                    object_mrid_to_name[y.get("mRID")] = {
                        "name" : y.get("name"),
                        "phases" : y.get("phases"),
                        "total_phases" : y.get("phases"),
                        "type" : "capacitor",
                        "prefix" : "cap_"
                    }
                for y in regulators:
                    object_mrids = y.get("mRID",[])
                    object_name = y.get("bankName")
                    object_phases = y.get("endPhase",[])
                    for z in range(len(object_mrids)):
                        object_mrid_to_name[object_mrids[z]] = {
                            "name" : object_name,
                            "phases" : object_phases[z],
                            "total_phases" : "".join(object_phases),
                            "type" : "regulator",
                            "prefix" : "reg_"
                        }
                for y in switches:
                    object_mrid_to_name[y.get("mRID")] = {
                        "name" : y.get("name"),
                        "phases" : y.get("phases"),
                        "total_phases" : y.get("phases"),
                        "type" : "switch",
                        "prefix" : "sw_"
                    }
                for y in solarpanels:
                    object_mrid_to_name[y.get("mRID")] = {
                        "name" : y.get("name"),
                        "phases" : y.get("phases"),
                        "total_phases" : y.get("phases"),
                        "type" : "pv",
                        "prefix" : "pv_"
                    }
                for y in batteries:
                    object_mrid_to_name[y.get("mRID")] = {
                        "name" : y.get("name"),
                        "phases" : y.get("phases"),
                        "total_phases" : y.get("phases"),
                        "type" : "battery",
                        "prefix" : "batt_"
                    }
                for y in synchronousMachines:
                    object_mrid_to_name[y.get("mRID")] = {
                        "name" : y.get("name"),
                        "phases" : y.get("phases"),
                        "total_phases" : y.get("phases"),
                        "type" : "diesel_dg",
                        "prefix" : "dg_"
                    }
                for y in breakers:
                    object_mrid_to_name[y.get("mRID")] = {
                        "name" : y.get("name"),
                        "phases" : y.get("phases"),
                        "total_phases" : y.get("phases"),
                        "type" : "switch",
                        "prefix" : "sw_"
                    }
                for y in reclosers:
                    object_mrid_to_name[y.get("mRID")] = {
                        "name" : y.get("name"),
                        "phases" : y.get("phases"),
                        "total_phases" : y.get("phases"),
                        "type" : "recloser",
                        "prefix" : "sw_"
                    }
        except Exception as e:
            _send_simulation_status('STARTED', "The measurement map file, {}, couldn't be translated.\nError:{}".format(map_file, e), 'ERROR')
            pass
        #_send_simulation_status('STARTED', str(object_mrid_to_name), 'INFO')


def json_loads_byteified(json_text):
    return _byteify(
        json.loads(json_text, object_hook=_byteify),
        ignore_dicts = True
    )


def json_load_byteified(file_handle):
    return _byteify(
        json.load(file_handle, object_hook=_byteify),
        ignore_dicts = True
    )


def _byteify(data, ignore_dicts = False):
    # if this is a unicode string, return its string representation
    if isinstance(data, unicode):
        return data.encode('utf-8')
    # if this is a list of values, return list of byteified values
    if isinstance(data, list):
        return [ _byteify(item, ignore_dicts=True) for item in data ]
    # if this is a dictionary, return dictionary of byteified keys and values
    # but only if we haven't already byteified it
    if isinstance(data, dict) and not ignore_dicts:
        return {
            _byteify(key, ignore_dicts=True): _byteify(value, ignore_dicts=True)
            for key, value in data.iteritems()
        }
    # if it's anything else, return it in its original form
    return data


def _keep_alive(is_realtime):
    simulation_ran = False
    while goss_listener_instance.stop_simulation == False:
        time.sleep(0.1)
        if goss_listener_instance.start_simulation == True and simulation_ran == False:
            goss_listener_instance.run_simulation(is_realtime)
            simulation_ran = True


def _main(simulation_id, simulation_broker_location='tcp://localhost:5570', measurement_map_dir='', is_realtime=True, sim_duration=86400, sim_start=0):

    measurement_map_file=str(measurement_map_dir)+"model_dict.json"
    _register_with_goss(simulation_id,'system','manager','127.0.0.1','61613', sim_duration, sim_start)
    _register_with_fncs_broker(simulation_broker_location)
    _create_cim_object_map(measurement_map_file)
    _keep_alive(is_realtime)

def _get_opts():
    parser = argparse.ArgumentParser()
    parser.add_argument("simulation_id", help="The simulation id to use for responses on the message bus.")
    parser.add_argument("broker_location", help="The location of the FNCS broker.")
    parser.add_argument("simulation_directory", help="The simulation files directory.")
    parser.add_argument("simulation_request", help="The simulation request.")
    opts = parser.parse_args()
    return opts

if __name__ == "__main__":
    #TODO: send simulation_id, fncsBrokerLocation, gossLocation,
    #stomp_port, username and password as commmand line arguments
    opts = _get_opts()
    simulation_id = opts.simulation_id
    sim_broker_location = opts.broker_location
    sim_dir = opts.simulation_directory
    sim_request = json.loads(opts.simulation_request.replace("\'",""))
    run_realtime = sim_request["simulation_config"]["run_realtime"]
    sim_duration = sim_request["simulation_config"]["duration"]
    sim_start_str = int(sim_request["simulation_config"]["start_time"])
    _main(simulation_id, sim_broker_location, sim_dir, run_realtime, sim_duration, sim_start_str)
