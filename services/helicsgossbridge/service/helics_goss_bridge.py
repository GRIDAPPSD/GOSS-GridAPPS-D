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
import inspect
import json
import logging
import logging.config
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

logConfig = {
    "version": 1,
    "disable_existing_loggers": True,
    "formatters": {
        "debug": {
            "format": "%(asctime)s [%(levelname)s] %(name)s; %(message)s",
            "datefmt": "%d/%m/%Y %H:%M:%S"
        }
    },
    "handlers": {
        "console": {
            "level": "DEBUG",
            "class": "logging.StreamHandler",
            "formatter": "debug",
            "stream": "ext://sys.stdout"
        },
        "file": {
            "level": "DEBUG",
            "class": "logging.FileHandler",
            "formatter": "debug",
            "filename": "HELICS_GOSS_Bridge.log",
            "mode": "w"
        }
    },
    "loggers": {
        "__main__": {
            "handlers": ["console","file"],
            "level": "DEBUG",
            "propagate": False
        }
    }
}

log = logging.getLogger(inspect.getmodulename(__file__))
log.setLevel(logging.DEBUG)
#logging.config.dictConfig(logConfig)
#log = logging.getLogger(__name__)

class HelicsGossBridge(object):
    '''
    ClassDocs
    '''
    _simulation_id = ""
    _broker_port = "5570"
    _simulation_request = {}
    _gad_connection = None
    _helics_configuration = {}
    _helics_federate = None
    _is_initialized = False
    _simulation_manager_input_topic = 'goss.gridappsd.cosim.output'
    _simulation_command_queue = Queue()
    _start_simulation = False
    _filter_all_commands = False
    _filter_all_measurements = False
    _command_filter = []
    _measurement_filter = []
    _stop_simulation = False
    _simulation_finished = False
    _pause_simulation = False
    _simulation_time = 0
    _pause_simulation_at = -1
    _object_property_to_measurement_id = None
    _object_mrid_to_name = None
    _model_mrid = None
    _difference_attribute_map = {
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
        },
        "EnergyConsumer.p" : {
            "triplex_load" : {
                "property" : ["base_power_{}"],
                "prefix" : "ld_"
            },
            "load" : {
                "property" : ["base_power_{}"],
                "prefix" : ""
            }
        }
    }
    
    
    def __init__(self, simulation_id, broker_port, simulation_request):
        
        self._simulation_id = simulation_id
        self._broker_port = broker_port
        self._simulation_request = simulation_request
        # register with GridAPPS-D
        self._register_with_goss()
        # register with HELICS
        self._register_with_helics()
        # build GLD property names to CIM mrid map
        self._create_cim_object_map()
    
    
    def get_simulation_id(self):
        return self._simulation_id
    
    
    def get_broker_port(self):
        return self._broker_port
    
    
    def get_simulation_request(self):
        return self._simulation_request
    
    
    def get_gad_connection(self):
        return self._gad_connection
    
    
    def get_helics_configuration(self):
        return self._helics_configuration
    
    
    def get_helics_federate(self):
        return self._helics_federate
    
    
    def get_is_initialized(self):
        return self._is_initialized
    
    
    def get_simulation_manager_input_topic(self):
        return self._simulation_manager_input_topic
    
    
    def get_simulation_command_queue(self):
        return self._simulation_command_queue
    
        
    def get_start_simulation(self):
        return self._start_simulation
    
    
    def get_filter_all_commands(self):
        return self._filter_all_commands
    
    
    def get_filter_all_measurements(self):
        return self._filter_all_measurements
    
    
    def get_command_filter(self):
        return self._command_filter
    
    
    def get_measurement_filter(self):
        return self._measurement_filter
    
    
    def get_stop_simulation(self):
        return self._stop_simulation
    
    
    def get_simulation_finished(self):
        return self._simulation_finished
    
    
    def get_pause_simulation(self):
        return self._pause_simulation
    
    
    def get_simulation_time(self):
        return self._simulation_time
    
    
    def get_pause_simulation_at(self):
        return self._pause_simulation_at
    
    
    def get_object_property_to_measurement_id(self):
        return self._object_property_to_measurement_id
    
    
    def get_object_mrid_to_name(self):
        return self._object_mrid_to_name
    
    
    def get_model_mrid(self):
        return self._model_mrid
    
    
    def get_difference_attribute_map(self):
        return self._difference_attribute_map
    
        
    def on_message(self, headers, msg):
        message = {}
        federate_state = helics.helicsFederateGetState(self._helics_federate)
        
        try:
            message_dict = {
                'received message': {
                    'header': headers,
                    'message_content': msg
                }
            }
            message_str = json.dumps(message_dict, indent=4, sort_keys=True)
            if federate_state == 2:
                self._is_initialized = True
                log.debug(message_str)
                self._gad_connection.send_simulation_status("RUNNING", message_str, "DEBUG")
            else:
                self._is_initialized = False
                log.debug(message_str)
                self._gad_connection.send_simulation_status("STARTING", message_str, "DEBUG")
            json_msg = yaml.safe_load(str(msg))
            if json_msg.get('command', '') == 'isInitialized':
                message_str = 'isInitialized check: '+str(self._is_initialized)
                if self._is_initialized:
                    log.debug(message_str)
                    self._gad_connection.send_simulation_status('RUNNING', message_str, 'DEBUG')
                else:
                    log.debug(message_str)
                    self._gad_connection.send_simulation_status('STARTING', message_str, 'DEBUG')
                message['command'] = 'isInitialized'
                message['response'] = str(self._is_initialized)
                t_now = datetime.utcnow()
                message['timestamp'] = int(time.mktime(t_now.timetuple()))
                self._gad_connection.send(self._simulation_manager_input_topic , json.dumps(message))
            elif json_msg.get('command', '') == 'update':
                json_msg['input']["time_received"] = time.perf_counter()
                message['command'] = 'update'
                if self._filter_all_commands == False:
                    self._simulation_command_queue.put(json.dumps(json_msg['input']))
            elif json_msg.get('command', '') == 'StartSimulation':
                self._gad_connection.send_simulation_status('STARTED', f"Simulation {self._simulation_id} has started.", 'INFO')
                if self._start_simulation == False:
                    self._start_simulation = True
            elif json_msg.get('command', '') == 'CommOutage':
                rev_diffs = json_msg.get('input',{}).get('reverse_differences', [])
                for_diffs = json_msg.get('input',{}).get('forward_differences', [])
                for d in rev_diffs:
                    if d.get('allInputOutage', False) == True:
                        self._filter_all_commands = False
                    else:
                        for x in d.get('inputOutageList', []):
                            try:
                                self._command_filter.remove(x)
                            except ValueError as ve:
                                pass
                    if d.get('allOutputOutage', False) == True:
                        self._filter_all_measurements = False
                    else:
                        for x in d.get('outputOutageList', []):
                            try:
                                self._measurement_filter.remove(x)
                            except ValueError as ve:
                                pass
                for d in for_diffs:
                    if d.get('allInputOutage', False) == True:
                        self._filter_all_commands = True
                    else:
                        for x in d.get('inputOutageList', []):
                            if x not in self._command_filter:
                                self._command_filter.append(x)
                    if d.get('allOutputOutage', False) == True:
                        self._filter_all_measurements = True
                    else:
                        for x in d.get('outputOutageList', []):
                            if x not in self._measurement_filter:
                                self._measurement_filter.append(x)
            elif json_msg.get('command', '') == 'stop':
                message_str = 'Stopping the simulation'
                log.info(message_str)
                self._gad_connection.send_simulation_status('CLOSED', message_str, 'INFO')
                self._stop_simulation = True
                if federate_state == 2:
                    if self._simulation_finished == False:
                        helics.helicsFederateGlobalError(self._helics_federate, 1, "Stopping the simulation prematurely at operator's request!")
                        self._close_helics_connection()
            elif json_msg.get('command', '') == 'pause':
                if self._pause_simulation == True:
                    log.warning('Pause command received but the simulation is already paused.')
                    self._gad_connection.send_simulation_status('PAUSED', 'Pause command received but the simulation is already paused.', 'WARN')
                else:
                    self._pause_simulation = True
                    log.info('The simulation has paused.')
                    self._gad_connection.send_simulation_status('PAUSED', 'The simulation has paused.', 'INFO')
            elif json_msg.get('command', '') == 'resume':
                if self._pause_simulation == False:
                    log.warning('Resume command received but the simulation is already running.')
                    self._gad_connection.send_simulation_status('RUNNING', 'Resume command received but the simulation is already running.', 'WARN')
                else:
                    self._pause_simulation = False
                    log.info('The simulation has resumed.')
                    self._gad_connection.send_simulation_status('RUNNING', 'The simulation has resumed.', 'INFO')
            elif json_msg.get('command', '') == 'resumePauseAt':
                if self._pause_simulation == False:
                    log.warning('The resumePauseAt command was received but the simulation is already running.')
                    self._gad_connection.send_simulation_status('RUNNING', 'The resumePauseAt command was received but the simulation is already running.', 'WARN')
                else:
                    self._pause_simulation = False
                    log.info('The simulation has resumed.')
                    self._gad_connection.send_simulation_status('RUNNING', 'The simulation has resumed.', 'INFO')
                    self._pause_simulation_at = self._simulation_time + json_msg.get('input', {}).get('pauseIn',-1)                       
            elif json_msg.get('command', '') == '':
                log.warning('The message received did not have a command key. Ignoring malformed message.')
                self._gad_connection.send_simulation_status('WARNING', 'The message received did not have a command key. Ignoring malformed message.', 'WARN')
        except Exception as e:
            message_str = 'Error in processing command message:\n{}.\nError:\n{}'.format(msg,traceback.format_exc())
            log.error(message_str)
            self._gad_connection.send_simulation_status('ERROR', message_str, 'ERROR')
            self._stop_simulation = True
            if federate_state == 2:
                helics.helicsFederateGlobalError(self._helics_federate, 1, message_str)
            self._close_helics_connection()
            
    
    
    def on_error(self, headers, message):
        message_str = 'Error in HelicsGossBridge: '+str(message)
        log.error(message_str)
        self._gad_connection.send_simulation_status('ERROR', message_str, 'ERROR')
        self._stop_simulation = True
        helics.helicsFederateGlobalError(self._helics_federate, 1, message_str)
        self._close_helics_connection()
        
        
    def on_disconnected(self):
        self._stop_simulation = True
        helics.helicsFederateGlobalError(self._helics_federate, 1, "HelicsGossBridge instance lost connection to GOSS bus.")
        self._close_helics_connection()
            
    def run_simulation(self):
        simulation_output_topic = topics.simulation_output_topic(self._simulation_id)
        run_realtime = self._simulation_request.get("simulation_config",{}).get("run_realtime",1)
        simulation_length = self._simulation_request.get("simulation_config",{}).get("duration",0)
        simulation_start = self._simulation_request.get("simulation_config",{}).get("start_time",0)
        # New archiving variables set here
        # Once the simulation_config is sent directly from the ui, then we can use these,
        # Until then you can change the archive to have a default value for either the
        # archive or the db_archive.  These will be off by default as is the current
        # setup.
        make_db_archive = self._simulation_request.get("simulation_config",{}).get("make_db_archive", False)
        make_archive = self._simulation_request.get("simulation_config",{}).get("make_archive", False)
        only_archive = self._simulation_request.get("simulation_config",{}).get("only_archive", False)
        archive_db_file = None
        if make_db_archive:
            archive_db_file = "/tmp/gridappsd_tmp/{}/archive.sqlite".format(self._simulation_id)
        archive_file = None
        if make_archive:
            archive_file = "/tmp/gridappsd_tmp/{}/archive.tar.gz".format(self._simulation_id)
        targz_file = None
        try:
            if archive_file:
                targz_file = gzip.open(archive_file, "wb")
            if archive_db_file:
                create_db_connection(archive_db_file)
            message = {}
            message['command'] = 'nextTimeStep'
            measurement_message_count = 0
            simulation_run_time_start = time.perf_counter()
            for current_time in range(simulation_length):
                begin_time_step = time.perf_counter()
                federate_state = helics.helicsFederateGetState(self._helics_federate)
                if federate_state == 4:
                    self._gad_connection.send_simulation_status("ERROR",f"The HELICS co-simulation for simulation {self._simulation_id} entered an error state for some unknown reason.", "ERROR")
                    log.error(f"The HELICS co-simulation for simulation {self._simulation_id} entered an error state for some unknown reason.")
                    raise RuntimeError(f"The HELICS co-simulation for simulation {self._simulation_id} entered an error state for some unknown reason.")
                self._simulation_time = current_time
                if self._stop_simulation == True:
                    if federate_state == 2:
                        helics.helicsFederateGlobalError(self._helics_federate, 1, "Stopping the simulation prematurely at operator's request!")
                    break
                self._gad_connection.send("goss.gridappsd.cosim.timestamp.{}".format(self._simulation_id), json.dumps({"timestamp": current_time + simulation_start}))
                #forward messages from HELICS to GOSS
                if self._filter_all_measurements == False:
                    message['output'] = self._get_helics_bus_messages(self._measurement_filter)
                else:
                    message['output'] = {}
                response_msg = json.dumps(message['output'], indent=4, sort_keys=True)

                if message['output']!={}:
                    measurement_message_count += 1
                    log.debug("measurement message recieved at timestep {}.".format(current_time))
                    if not only_archive:
                        self._gad_connection.send(simulation_output_topic, response_msg)
                    if archive_db_file:
                        ts = message['output']['message']['timestamp']
                        meas = message['output']['message']['measurements']
                        log.debug("Passing timestamp {ts} to write_db_archive".format(ts=ts))
                        write_db_archive(ts, meas)
                    if targz_file:
                        targz_file.write((response_msg+"\n").encode('utf-8'))
                if self._simulation_time == self._pause_simulation_at:
                    self._pause_simulation = True
                    log.info('The simulation has paused.')
                    self._gad_connection.send_simulation_status('PAUSED', 'The simulation has paused.', 'INFO')
                while self._pause_simulation == True:
                    time.sleep(1)
                #forward messages from GOSS to HELICS
                while not self._simulation_command_queue.empty():
                    self._publish_to_helics_bus(self._simulation_command_queue.get(), self._command_filter)
                self._done_with_time_step(current_time) #current_time is incrementing integer 0 ,1, 2.... representing seconds
                message_str = 'incrementing to '+str(current_time + 1)
                log.debug(message_str)
                self._gad_connection.send_simulation_status('RUNNING', message_str, 'INFO')
                if run_realtime == True:
                    sleep_time = 1 - time.perf_counter() + begin_time_step
                    if sleep_time < 0:
                        warn_message = f"Simulation {self._simulation_id} is running slower than real time!!!. Time step took {1.0 - sleep_time} seconds to execute"
                        log.warning(warn_message)
                        self._gad_connection.send_simulation_status('RUNNING', warn_message, 'WARN')
                    else:
                        dbg_message = f"Time step took {1 - sleep_time} seconds to execute."
                        log.debug(dbg_message)
                        self._gad_connection.send_simulation_status('RUNNING', dbg_message, 'DEBUG')
                        time.sleep(sleep_time)
            federate_state = helics.helicsFederateGetState(self._helics_federate)
            if not self._stop_simulation:
                self._simulation_time = current_time + 1
            else:
                self._simulation_time = current_time
                if federate_state == 2:
                    helics.helicsFederateGlobalError(self._helics_federate, 1, "Stopping the simulation prematurely at operator's request!")
            self._gad_connection.send("goss.gridappsd.cosim.timestamp.{}".format(self._simulation_id), json.dumps({"timestamp": self._simulation_time + simulation_start}))
            #forward messages from HELICS to GOSS
            if self._filter_all_measurements == False:
                message['output'] = self._get_helics_bus_messages(self._measurement_filter)
            else:
                message['output'] = {}
            response_msg = json.dumps(message['output'], indent=4, sort_keys=True)
            if message['output']!={}:
                measurement_message_count += 1
                log.debug("measurement message recieved at end of simulation.".format(current_time))
                if not only_archive:
                    self._gad_connection.send(simulation_output_topic, response_msg)
                if archive_db_file:
                    ts = message['output']['message']['timestamp']
                    meas = message['output']['message']['measurements']
                    log.debug("Passing timestamp {ts} to write_db_archive".format(ts=ts))
                    write_db_archive(ts, meas)
                if targz_file:
                    targz_file.write((response_msg+"\n").encode('utf-8'))
            if federate_state == 2:
                helics.helicsFederateFinalize(self._helics_federate)
            self._close_helics_connection()
            self._simulation_finished = True
            log.debug(f"Simulation finished in {time.perf_counter() - simulation_run_time_start} seconds.")
            message['command'] = 'simulationFinished'
            del message['output']
            self._gad_connection.send(self._simulation_manager_input_topic, json.dumps(message))
            log.info('Simulation {} has finished.'.format(self._simulation_id))
            self._gad_connection.send_simulation_status('COMPLETE', 'Simulation {} has finished.'.format(self._simulation_id), 'INFO')
            log.debug("total measurement messages recieved {}".format(measurement_message_count))
        except Exception as e:
            message_str = 'Error in run simulation {}'.format(traceback.format_exc())
            log.error(message_str)
            self._gad_connection.send_simulation_status('ERROR', message_str, 'ERROR')
            self._simulation_finished = True
            if helics.helicsFederateGetState(self._helics_federate) == 2:
                helics.helicsFederateGlobalError(self._helics_federate, 1, message_str)
            self._close_helics_connection()
        finally:
            if targz_file:
                targz_file.close()
    
            
    def _register_with_goss(self): 
        try:
            self._gad_connection = GridAPPSD(self._simulation_id)
            log.debug("Successfully registered with the GridAPPS-D platform.")
            self._gad_connection.subscribe(topics.simulation_input_topic(self._simulation_id), self.on_message)
            self._gad_connection.subscribe("/topic/goss.gridappsd.cosim.input", self.on_message)
        except Exception as e:
            log.error("An error occurred when trying to register with the GridAPPS-D platform!", exc_info=True)
            
            
    def _register_with_helics(self):
        try:
            self._helics_configuration = {
                "name": "HELICS_GOSS_Bridge_{}".format(self._simulation_id),
                "period": 1.0,
                "log_level": 7,
                "broker": "127.0.0.1:{}".format(self._broker_port),
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
            self._helics_federate = helics.helicsCreateMessageFederateFromConfig(json.dumps(self._helics_configuration))
            helics.helicsFederateEnterExecutingMode(self._helics_federate)
            log.debug("Successfully registered with the HELICS broker.") 
        except Exception as e:
            err_msg = "An error occurred when trying to register with the HELICS broker!{}".format(traceback.format_exc())
            log.error(err_msg, exc_info=True)
            self._gad_connection.send_simulation_status("ERROR", err_msg, "ERROR")
    
    
    def _close_helics_connection(self):
        helics.helicsFederateFree(self._helics_federate)
        helics.helicsCloseLibrary()
    
    
    def _get_gld_object_name(self, object_mrid):
        prefix = ""
        stored_object = self._object_mrid_to_name.get(object_mrid)
        if stored_object == None:
            cim_object_dict = self._gad_connection.query_object_dictionary(model_id=self._model_mrid, object_id=object_mrid)
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

    
    def _publish_to_helics_bus(self, goss_message, command_filter):
        """publish a message received from the GOSS bus to the HELICS bus.
    
        Function arguments:
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
        publish_to_helics_bus_start = time.perf_counter()
        message_str = 'translating following message for HELICS simulation '+str(self._simulation_id)+' '+str(goss_message)
        log.debug(message_str)
        self._gad_connection.send_simulation_status('RUNNING', message_str, 'DEBUG')
        if self._simulation_id == None or self._simulation_id == '' or type(self._simulation_id) != str:
            raise ValueError(
                'simulation_id must be a nonempty string.\n'
                + 'simulation_id = {0}.\n'.format(self._simulation_id)
                + 'simulation_id type = {0}.'.format(type(self._simulation_id)))
        if goss_message == None or goss_message == '' or type(goss_message) != str:
            raise ValueError(
                'goss_message must be a nonempty string.\n'
                + 'goss_message = {0}.\n'.format(goss_message)
                + 'goss_message type = {}.'.format(type(goss_message)))
        federate_state = helics.helicsFederateGetState(self._helics_federate)
        if federate_state != 2:
            raise RuntimeError(
                'Cannot publish message as there is no connection'
                + ' to the HELICS message bus.')
        try:
            test_goss_message_format = yaml.safe_load(goss_message)
            if type(test_goss_message_format) != dict:
                raise ValueError(
                    'goss_message is not a json formatted string of a python dictionary.'
                    + '\ngoss_message = {0}'.format(goss_message))
            helics_input_endpoint = helics.helicsFederateGetEndpoint(self._helics_federate, "helics_input")
            helics_input_message = {"{}".format(self._simulation_id) : {}}
            helics_input_message["{}".format(self._simulation_id)]["external_event_handler"] = {}
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
                        object_name = (self._object_mrid_to_name.get(x.get("object",{}),{})).get("name")
                        object_phases = (self._object_mrid_to_name.get(x.get("object",{}),{})).get("phases")
                        object_total_phases = (self._object_mrid_to_name.get(x.get("object",{}),{})).get("total_phases")
                        object_type = (self._object_mrid_to_name.get(x.get("object",{}),{})).get("type")
                        object_name_prefix = ((self._difference_attribute_map.get(x.get("attribute",{}),{})).get(object_type,{})).get("prefix")
                        cim_attribute = x.get("attribute")
                        object_property_list = ((self._difference_attribute_map.get(x.get("attribute",{}),{})).get(object_type,{})).get("property")
                        phase_in_property = ((self._difference_attribute_map.get(x.get("attribute",{}),{})).get(object_type,{})).get("phase_sensitive",False)
                        if object_name == None or object_phases == None or object_total_phases == None or object_type == None or object_name_prefix == None or cim_attribute == None or object_property_list == None:
                            parsed_result = {
                                "object_name":object_name,
                                "object_phases":object_phases,
                                "object_total_phases":object_total_phases,
                                "object_type":object_type,
                                "object_name_prefix":object_name_prefix,
                                "cim_attribute":cim_attribute,
                                "object_property_list":object_property_list
                            }
                            raise RuntimeError("Forward difference command cannot be parsed correctly one or more of attributes needed was None.\ndifference:{}\nparsed result:{}".format(json.dumps(x,indent=4,sort_keys=True),json.dumps(parsed_result,indent=4,sort_keys=True)))
                        if (object_name_prefix + object_name) not in helics_input_message["{}".format(self._simulation_id)].keys():
                            helics_input_message["{}".format(self._simulation_id)][object_name_prefix + object_name] = {}
                        if cim_attribute == "RegulatingControl.mode":
                            val = int(x.get("value"))
                            if val == 0:
                                helics_input_message["{}".format(self._simulation_id)][object_name_prefix + object_name][object_property_list[0]] = "VOLT"
                            if val == 1:
                                helics_input_message["{}".format(self._simulation_id)][object_name_prefix + object_name][object_property_list[0]] = "MANUAL"
                            elif val == 2:
                                helics_input_message["{}".format(self._simulation_id)][object_name_prefix + object_name][object_property_list[0]] = "VAR"
                            elif val == 3:
                                helics_input_message["{}".format(self._simulation_id)][object_name_prefix + object_name][object_property_list[0]] = "CURRENT"
                            else:
                                helics_input_message["{}".format(self._simulation_id)][object_name_prefix + object_name][object_property_list[0]] = "MANUAL"
                                log.warning("Unsupported capacitor control mode requested. The only supported control modes for capacitors are voltage, VAr, volt/VAr, and current. Setting control mode to MANUAL.")
                                self._gad_connection.send_simulation_status("RUNNING", "Unsupported capacitor control mode requested. The only supported control modes for capacitors are voltage, VAr, volt/VAr, and current. Setting control mode to MANUAL.","WARN")
                        elif cim_attribute == "RegulatingControl.targetDeadband":
                            for y in self._difference_attribute_map[cim_attribute][object_type]["property"]:
                                helics_input_message["{}".format(self._simulation_id)][object_name_prefix + object_name][y] = float(x.get("value"))
                        elif cim_attribute == "RegulatingControl.targetValue":
                            for y in self._difference_attribute_map[cim_attribute][object_type]["property"]:
                                helics_input_message["{}".format(self._simulation_id)][object_name_prefix + object_name][y] = float(x.get("value"))
                        elif cim_attribute == "RotatingMachine.p":
                            for y in object_phases:
                                helics_input_message["{}".format(self._simulation_id)][object_name_prefix + object_name][object_property_list[0].format(y)] = float(x.get("value"))/3.0
                        elif cim_attribute == "RotatingMachine.q":
                            for y in object_phases:
                                helics_input_message["{}".format(self._simulation_id)][object_name_prefix + object_name][object_property_list[0].format(y)] = float(x.get("value"))/3.0
                        elif cim_attribute == "ShuntCompensator.aVRDelay":
                            for y in self._difference_attribute_map[cim_attribute][object_type]["property"]:
                                helics_input_message["{}".format(self._simulation_id)][object_name_prefix + object_name][y] = float(x.get("value"))
                        elif cim_attribute == "ShuntCompensator.sections":
                            if int(x.get("value")) == 1:
                                val = "CLOSED"
                            else:
                                val = "OPEN"
                            for y in object_phases:
                                helics_input_message["{}".format(self._simulation_id)][object_name_prefix + object_name][object_property_list[0].format(y)] = "{}".format(val)
                        elif cim_attribute == "Switch.open":
                            if int(x.get("value")) == 1:
                                val = "OPEN"
                            else:
                                val = "CLOSED"
                            helics_input_message["{}".format(self._simulation_id)][object_name_prefix + object_name][object_property_list[0]] = "{}".format(val)
                        elif cim_attribute == "TapChanger.initialDelay":
                            for y in object_property_list:
                                helics_input_message["{}".format(self._simulation_id)][object_name_prefix + object_name][y] = float(x.get("value"))
                        elif cim_attribute == "TapChanger.step":
                            for y in object_phases:
                                helics_input_message["{}".format(self._simulation_id)][object_name_prefix + object_name][object_property_list[0].format(y)] = int(x.get("value"))
                        elif cim_attribute == "TapChanger.lineDropCompensation":
                            if int(x.get("value")) == 1:
                                val = "LINE_DROP_COMP"
                            else:
                                val = "MANUAL"
                            helics_input_message["{}".format(self._simulation_id)][object_name_prefix + object_name][object_property_list[0]] = "{}".format(val)
                        elif cim_attribute == "TapChanger.lineDropR":
                            for y in object_phases:
                                helics_input_message["{}".format(self._simulation_id)][object_name_prefix + object_name][object_property_list[0].format(y)] = float(x.get("value"))
                        elif cim_attribute == "TapChanger.lineDropX":
                            for y in object_phases:
                              helics_input_message["{}".format(self._simulation_id)][object_name_prefix + object_name][object_property_list[0].format(y)] = float(x.get("value"))
                        elif cim_attribute == "PowerElectronicsConnection.p":
                            helics_input_message["{}".format(self._simulation_id)][object_name_prefix + object_name][object_property_list[0]] = float(x.get("value"))
                        elif cim_attribute == "PowerElectronicsConnection.q":
                            helics_input_message["{}".format(self._simulation_id)][object_name_prefix + object_name][object_property_list[0]] = float(x.get("value"))
                        elif cim_attribute == "EnergyConsumer.p":
                            phase_count = len(object_phases)
                            if "s1" in object_phases:
                                helics_input_message["{}".format(self._simulation_id)][object_name_prefix + object_name][object_property_list[0].format("1")] = float(x.get("value"))/2.0
                            if "s2" in object_phases:
                                helics_input_message["{}".format(self._simulation_id)][object_name_prefix + object_name][object_property_list[0].format("2")] = float(x.get("value"))/2.0
                            if "A" in object_phases:
                                helics_input_message["{}".format(self._simulation_id)][object_name_prefix + object_name][object_property_list[0].format("A")] = float(x.get("value"))/phase_count
                            if "B" in object_phases:
                                helics_input_message["{}".format(self._simulation_id)][object_name_prefix + object_name][object_property_list[0].format("B")] = float(x.get("value"))/phase_count
                            if "C" in object_phases:
                                helics_input_message["{}".format(self._simulation_id)][object_name_prefix + object_name][object_property_list[0].format("C")] = float(x.get("value"))/phase_count
                        else:
                            log.warning("Attribute, {}, is not a supported attribute in the simulator at this current time. ignoring difference.".format(cim_attribute))
                            self._gad_connection.send_simulation_status("RUNNING", "Attribute, {}, is not a supported attribute in the simulator at this current time. ignoring difference.".format(cim_attribute), "WARN")
                else:
                    fault_val_dict = {}
                    fault_val_dict["name"] = x.get("object","")
                    fault_object_mrid = (x.get("value",{})).get("ObjectMRID","")               
                    fault_val_dict["fault_object"] = self._get_gld_object_name(fault_object_mrid)
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
                helics_input_message["{}".format(self._simulation_id)]["external_event_handler"]["external_fault_event"] = json.dumps(fault_list)
            goss_message_converted = json.dumps(helics_input_message, indent=4, sort_keys=True)
            log.info("Sending the following message to the simulator. {}".format(goss_message_converted))
            self._gad_connection.send_simulation_status("RUNNING", "Sending the following message to the simulator. {}".format(goss_message_converted),"INFO")
            if federate_state == 2 and helics_input_message["{}".format(self._simulation_id)] != {}:
                helics_msg = helics.helicsFederateCreateMessageObject(self._helics_federate)
                helics.helicsMessageSetString(helics_msg, goss_message_converted)
                helics.helicsEndpointSendMessageObject(helics_input_endpoint, helics_msg)
            publish_to_helics_bus_finish = time.perf_counter()
            publish_to_helics_profile = {
                "time_between_receipt_of_message_and_processing": publish_to_helics_bus_start - test_goss_message_format.get("time_received",publish_to_helics_bus_start),
                "time_messege_processing": publish_to_helics_bus_finish - publish_to_helics_bus_start,
                "total_time": publish_to_helics_bus_finish - test_goss_message_format.get("time_received",publish_to_helics_bus_start)
            }
            log.debug(f"Message Processing Profile: {json.dumps(publish_to_helics_profile, indent=4, sort_keys=True)}")
        except ValueError as ve:
            raise ValueError(ve)
        except Exception as ex:
            err_msg = "An error occured while trying to translate the update message received\n{}".format(traceback.format_exc())
            self._gad_connection.send_simulation_status("ERROR",err_msg,"ERROR")
            raise RuntimeError(err_msg)
        
    
    def _get_helics_bus_messages(self, measurement_filter):
        """ retrieve the measurment dictionary from the HELICS message bus
    
        Function arguments:
            measurement_filter -- Type: list. Description: The list of
                measurement id's to filter from the simulator output.
        Function returns:
            helics_output -- Type: string. Description: The json structured output
                from the simulation. If no output was sent from the simulation then
                it returns None.
        Function exceptions:
            ValueError()
        """
        propertyName = ""
        objectName = ""
        objectType = ""
        propertyValue = ""
        get_helics_bus_messages_start = time.perf_counter()
        try:
            helics_message = None
            if self._simulation_id == None or self._simulation_id == '' or type(self._simulation_id) != str:
                raise ValueError(
                    'simulation_id must be a nonempty string.\n'
                    + 'simulation_id = {0}'.format(self._simulation_id))
            helics_output_endpoint = helics.helicsFederateGetEndpoint(self._helics_federate, "helics_output")
            has_message = helics.helicsEndpointHasMessage(helics_output_endpoint)
            if has_message:
                message_str = 'helics_output has a message'
            else:
                message_str = 'helics_output has no messages'
            log.debug(message_str)
            self._gad_connection.send_simulation_status('RUNNING', message_str, 'DEBUG')
            cim_output = {}
            if has_message:
                t_now = datetime.utcnow()
                cim_measurements_dict = {
                    "simulation_id": self._simulation_id,
                    "message" : {
                        "timestamp" : int(time.mktime(t_now.timetuple())),
                        "measurements" : {}
                    }
                }
                helics_message = helics.helicsEndpointGetMessageObject(helics_output_endpoint)
                helics_output = helics.helicsMessageGetString(helics_message)
                helics_output_dict = json.loads(helics_output)
                
                sim_dict = helics_output_dict.get(self._simulation_id, None)
                if sim_dict != None:
                    simulation_time = int(sim_dict.get("globals",{}).get("clock", 0))
                    if simulation_time != 0:
                        cim_measurements_dict["message"]["timestamp"] = simulation_time
                    for x in self._object_property_to_measurement_id.keys():
                        objectName = x
                        gld_properties_dict = sim_dict.get(x,None)
                        if gld_properties_dict == None:
                            err_msg = "All measurements for object {} are missing from the simulator output.".format(x)
                            log.warning(err_msg)
                            self._gad_connection.send_simulation_status('RUNNING', err_msg, 'WARN')
                        else:
                            for y in self._object_property_to_measurement_id.get(x,[]):
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
                                        log.warning(err_msg)
                                        self._gad_connection.send_simulation_status('RUNNING', err_msg, 'WARN')
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
                                            if property_name == "state_of_charge":
                                                measurement["value"] = float(val_str)*100.0
                                            else:
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
                                            log.warning("{} is not a recognized conducting equipment type.".format(conducting_equipment_type))
                                            self._gad_connection.send_simulation_status('RUNNING', conducting_equipment_type+" not recognized", 'WARN')
                                            raise RuntimeError("{} is not a recognized conducting equipment type.".format(conducting_equipment_type))
                                            # Should it raise runtime?
                                        # change to be a dictionary rather than an array
                                        cim_measurements_dict['message']["measurements"][measurement["measurement_mrid"]] = measurement
                    cim_output = cim_measurements_dict
                else:
                    err_msg = "The message recieved from the simulator did not have the simulation id as a key in the json message."
                    log.error(err_msg)
                    self._gad_connection.send_simulation_status('ERROR', err_msg, 'ERROR')
                    raise RuntimeError(err_msg)
            log.debug(f"Message from simulation processing time: {time.perf_counter() - get_helics_bus_messages_start}.")
            return cim_output
        except ValueError as ve:
            raise RuntimeError("{}.\nObject Name: {}\nObject Type: {}\nProperty Name: {}\n Property Value{}".format(str(ve), objectName, objectType, propertyName, propertyValue))
        except Exception as e:
            message_str = 'Error on get HELICS Bus Messages for '+str(self._simulation_id)+' '+str(traceback.format_exc())
            log.error(message_str)
            self._gad_connection.send_simulation_status('ERROR', message_str, 'ERROR')
            return {}
    
    
    def _done_with_time_step(self, current_time):
        """tell the helics_broker to move to the next time step.
    
        Function arguments:
            current_time -- Type: integer. Description: the current time in seconds.
                It must not be none.
        Function returns:
            None.
        Function exceptions:
            RuntimeError()
            ValueError()
        """
        done_with_time_step_start = time.perf_counter()
        try:
            if current_time == None or type(current_time) != int:
                raise ValueError(
                    'current_time must be an integer.\n'
                    + 'current_time = {0}'.format(current_time))
            time_request = float(current_time + 1)
            time_approved = helics.helicsFederateRequestTime(self._helics_federate, time_request)
            if time_approved != time_request:
                raise RuntimeError(
                    'The time approved from helics_broker is not the time requested.\n'
                    + 'time_request = {0}.\ntime_approved = {1}'.format(time_request,
                    time_approved))
            log.debug(f"done_with_time_step took {time.perf_counter() - done_with_time_step_start} seconds to finish.")
        except Exception as e:
            message_str = 'Error in HELICS time request '+str(traceback.format_exc())
            log.error(message_str)
            self._gad_connection.send_simulation_status('ERROR', message_str, 'ERROR')
        
            
    def _create_cim_object_map(self,map_file=None):
        if map_file == None:
            map_file="/tmp/gridappsd_tmp/{}/model_dict.json".format(self._simulation_id)
        try:
            with open(map_file, "r", encoding="utf-8") as file_input_stream:
                file_dict = json.load(file_input_stream)
            feeders = file_dict.get("feeders",[])
            self._object_property_to_measurement_id = {}
            self._object_mrid_to_name = {}
            for x in feeders:
                self._model_mrid = x.get("mRID","")
                measurements = x.get("measurements",[])
                capacitors = x.get("capacitors",[])
                regulators = x.get("regulators",[])
                switches = x.get("switches",[])
                batteries = x.get("batteries", [])
                solarpanels = x.get("solarpanels",[])
                synchronousMachines = x.get("synchronousmachines", [])
                breakers = x.get("breakers", [])
                reclosers = x.get("reclosers", [])
                energy_consumers = x.get("energyconsumers", [])
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
                                property_name = "indiv_measured_power_" + phases;
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
                        elif measurement_type == "SoC":
                            object_name = conducting_equipment_name
                            property_name = "state_of_charge"
                        else:
                            raise RuntimeError("_create_cim_object_map: The value of measurement_type is not a valid type.\nValid types for PowerElectronicsConnection are VA, A, SoC, and PNV.\nmeasurement_type = %s.".format(measurement_type))
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
                        raise RuntimeError("_create_cim_object_map: The value of conducting_equipment_type is not a valid type.\nValid types for conducting_equipment_type are ACLineSegment, LinearShuntCompesator, LoadBreakSwitch, PowerElectronicsConnection, EnergyConsumer, RatioTapChanger, and PowerTransformer.\nconducting_equipment_type = {}.".format(conducting_equipment_type))

                    property_dict = {
                        "property" : property_name,
                        "conducting_equipment_type" : conducting_equipment_type,
                        "measurement_mrid" : measurement_mrid,
                        "phases" : phases
                    }
                    if object_name in self._object_property_to_measurement_id.keys():
                        self._object_property_to_measurement_id[object_name].append(property_dict)
                    else:
                        self._object_property_to_measurement_id[object_name] = []
                        self._object_property_to_measurement_id[object_name].append(property_dict)
                for y in capacitors:
                    self._object_mrid_to_name[y.get("mRID")] = {
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
                        self._object_mrid_to_name[object_mrids[z]] = {
                            "name" : object_name,
                            "phases" : object_phases[z],
                            "total_phases" : "".join(object_phases),
                            "type" : "regulator",
                            "prefix" : "reg_"
                        }
                for y in switches:
                    self._object_mrid_to_name[y.get("mRID")] = {
                        "name" : y.get("name"),
                        "phases" : y.get("phases"),
                        "total_phases" : y.get("phases"),
                        "type" : "switch",
                        "prefix" : "sw_"
                    }
                for y in solarpanels:
                    self._object_mrid_to_name[y.get("mRID")] = {
                        "name" : y.get("name"),
                        "phases" : y.get("phases"),
                        "total_phases" : y.get("phases"),
                        "type" : "pv",
                        "prefix" : "pv_"
                    }
                for y in batteries:
                    self._object_mrid_to_name[y.get("mRID")] = {
                        "name" : y.get("name"),
                        "phases" : y.get("phases"),
                        "total_phases" : y.get("phases"),
                        "type" : "battery",
                        "prefix" : "batt_"
                    }
                for y in synchronousMachines:
                    self._object_mrid_to_name[y.get("mRID")] = {
                        "name" : y.get("name"),
                        "phases" : y.get("phases"),
                        "total_phases" : y.get("phases"),
                        "type" : "diesel_dg",
                        "prefix" : "dg_"
                    }
                for y in breakers:
                    self._object_mrid_to_name[y.get("mRID")] = {
                        "name" : y.get("name"),
                        "phases" : y.get("phases"),
                        "total_phases" : y.get("phases"),
                        "type" : "switch",
                        "prefix" : "sw_"
                    }
                for y in reclosers:
                    self._object_mrid_to_name[y.get("mRID")] = {
                        "name" : y.get("name"),
                        "phases" : y.get("phases"),
                        "total_phases" : y.get("phases"),
                        "type" : "recloser",
                        "prefix" : "sw_"
                    }
                for y in energy_consumers:
                    self._object_mrid_to_name[y.get("mRID")] = {
                        "name" : y.get("name"),
                        "phases" : y.get("phases"),
                        "total_phases" : y.get("phases"),
                        "prefix" : "ld_"
                    }
                    if "s1" in self._object_mrid_to_name[y.get("mRID")]["phases"] or "s2" in self._object_mrid_to_name[y.get("mRID")]["phases"]:
                        self._object_mrid_to_name[y.get("mRID")]["type"] = "triplex_load"
                    else:
                        self._object_mrid_to_name[y.get("mRID")]["type"] = "load"
        except Exception as e:
            log.error("The measurement map file, {}, couldn't be translated.\nError:{}".format(map_file, traceback.format_exc()))
            self._gad_connection.send_simulation_status('STARTED', "The measurement map file, {}, couldn't be translated.\nError:{}".format(map_file, traceback.format_exc()), 'ERROR')
            
            
def _main(simulation_id, broker_port, simulation_request):
    os.environ["GRIDAPPSD_APPLICATION_ID"] = "helics_goss_bridge.py"
    bridge = HelicsGossBridge(simulation_id, broker_port, simulation_request)
    simulation_started = False
    simulation_stopped = False
    timout = 0
    while not simulation_stopped:
        sim_is_initialized = bridge.get_is_initialized()
        start_sim = bridge.get_start_simulation()
        stop_sim = bridge.get_stop_simulation()
        sim_finished = bridge.get_simulation_finished()
        if stop_sim or sim_finished:
            simulation_stopped = True
        elif sim_is_initialized and start_sim and not simulation_started:
            simulation_started = True
            bridge.run_simulation()
            simulation_stopped = True
        else:
            time.sleep(0.1)

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("simulation_id", help="The simulation id to use for responses on the message bus.")
    parser.add_argument("broker_port", help="The port the helics broker is running on.")
    parser.add_argument("simulation_request", help="The simulation request.")
    args = parser.parse_args()
    sim_request = json.loads(args.simulation_request.replace("\'",""))
    _main(args.simulation_id, args.broker_port, sim_request)
