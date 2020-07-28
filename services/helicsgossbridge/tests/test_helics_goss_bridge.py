from datetime import datetime
import json
import os
try:
    from Queue import Queue
except:
    from queue import Queue
import sys
import time
import unittest.mock
from unittest.mock import call, patch, PropertyMock

from gridappsd import GridAPPSD, utils, topics
import helics
import pytest

from service.helics_goss_bridge import HelicsGossBridge, _main as helics_main

message_header = {
    "header_1": "Hello",
    "header_2": "There!"
}
message_is_initialized = {
    "command" : "isInitialized"
}
message_update = {
    "command" : "update",
    "input" : {
        "simulation_id" : "123456",
        "message" : {
            "timestamp" : 1357048800,
            "difference_mrid" : "123a456b-789c-012d-345e-678f901a235b",
            "reverse_differences" : [
                {
                    "object" : "61A547FB-9F68-5635-BB4C-F7F537FD824E",
                    "attribute" : "ShuntCompensator.sections",
                    "value" : 1
                },
                {
                    "object" : "E3CA4CD4-B0D4-9A83-3E2F-18AC5F1B55BA",
                    "attribute" : "ShuntCompensator.sections",
                    "value" : 0
                }
            ],
            "forward_differences": [
                {
                    "object" : "61A547FB-9F68-5635-BB4C-F7F537FD824E",
                    "attribute" : "ShuntCompensator.sections",
                    "value" : 0
                },
                {
                    "object" : "E3CA4CD4-B0D4-9A83-3E2F-18AC5F1B55BA",
                    "attribute" : "ShuntCompensator.sections",
                    "value" : 1
                }
            ]
        }
    }
}
message_start_simulation = {
    "command" : "StartSimulation"
}
message_comm_outage = {
    "command" : "CommOutage",
    "input" : {
        "reverse_differences" : [
            {
                "allInputOutage" : False,
                "inputOutageList" : [],
                "allOutputOutage" : False,
                "outputOutageList" : []
            }
        ],
        "forward_differences" : [
            {
                "allInputOutage" : False,
                "inputOutageList" : [
                    "61A547FB-9F68-5635-BB4C-F7F537FD824E",
                    "E3CA4CD4-B0D4-9A83-3E2F-18AC5F1B55BA"
                ],
                "allOutputOutage" : False,
                "outputOutageList" : [
                    "61A547FB-9F68-5635-BB4C-F7F537FD824E",
                    "E3CA4CD4-B0D4-9A83-3E2F-18AC5F1B55BA"
                ]
            }
        ]
    }
}
message_stop = {
    "command" : "stop"
}
message_pause = {
    "command" : "pause"
}
message_resume = {
    "command" : "resume"
}
message_resume_pause_at = {
    "command" : "resumePauseAt",
    "input" : {
        "pauseIn" : 20
    }
}
message_junk = {}


@patch.object(HelicsGossBridge,'__init__', return_value=None)
@patch.object(HelicsGossBridge,'get_is_initialized', return_value=True)
@patch.object(HelicsGossBridge,'get_start_simulation', return_value=True)
@patch.object(HelicsGossBridge,'get_stop_simulation', return_value=False)
@patch.object(HelicsGossBridge,'get_simulation_finished', return_value=False)
@patch.object(HelicsGossBridge,'run_simulation', return_value=None)
def test_helics_goss_bridge_main_function(mock_run_simulation,
        mock_simulation_finished,mock_stop_simulation,mock_start_simulation,
        mock_is_initialized, mock_init):
    helics_main(123,5570,
        {"simulation_config":1,"duration":120,"simulation_start":0})
    mock_init.assert_called_once_with(123,5570,
        {"simulation_config":1,"duration":120,"simulation_start":0})
    mock_is_initialized.assert_called_once()
    mock_start_simulation.assert_called_once()
    mock_stop_simulation.assert_called_once()
    mock_simulation_finished.assert_called_once()
    mock_run_simulation.assert_called_once()

    
@patch.object(HelicsGossBridge,'_register_with_goss')
@patch.object(HelicsGossBridge,'_register_with_helics')
@patch.object(HelicsGossBridge,'_create_cim_object_map')
def test_helics_goss_bridge_init(mock_cim_object_map,mock_register_with_helics,
        mock_register_with_goss):
    bridge = HelicsGossBridge(123,5570,
        {"simulation_config":1,"duration":120,"simulation_start":0})
    assert bridge.get_simulation_id() == 123
    assert bridge.get_broker_port() == 5570
    assert bridge.get_simulation_request() == {"simulation_config":1,
                                               "duration":120,
                                               "simulation_start":0}
    mock_register_with_goss.assert_called_once()
    mock_register_with_helics.assert_called_once()
    mock_cim_object_map.assert_called_once()
    
    
@patch.object(HelicsGossBridge,'_register_with_goss')
@patch.object(HelicsGossBridge,'_register_with_helics')
@patch.object(HelicsGossBridge,'_create_cim_object_map')
def test_helics_goss_bridge_getters(mock_cim_object_map,
        mock_register_with_helics,mock_register_with_goss):
    bridge = HelicsGossBridge(123,5570,
        {"simulation_config":1,"duration":120,"simulation_start":0})
    assert bridge.get_simulation_id() == 123
    assert bridge.get_broker_port() == 5570
    assert bridge.get_gad_connection() == None
    assert bridge.get_helics_configuration() == {}
    assert bridge.get_helics_federate() == None
    assert bridge.get_is_initialized() == False
    assert bridge.get_simulation_manager_input_topic() == 'goss.gridappsd.fncs.output'
    assert type(bridge.get_simulation_command_queue()) == type(Queue())
    assert bridge.get_simulation_command_queue().empty()
    assert bridge.get_start_simulation() == False
    assert bridge.get_filter_all_commands() == False
    assert bridge.get_filter_all_measurements() == False
    assert bridge.get_command_filter() == []
    assert bridge.get_measurement_filter() == []
    assert bridge.get_stop_simulation() == False
    assert bridge.get_simulation_finished() == False
    assert bridge.get_pause_simulation() == False
    assert bridge.get_simulation_time() == 0
    assert bridge.get_pause_simulation_at() == -1
    assert bridge.get_object_property_to_measurement_id() == None
    assert bridge.get_object_mrid_to_name() == None
    assert bridge.get_model_mrid() == None
    assert bridge.get_difference_attribute_map() == {
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
                "property" : ["base_power_{}"]
            }
        }
    }

    
@patch.object(HelicsGossBridge,'__init__', return_value=None)
@patch.object(helics,'helicsFederateGetState', return_value=1)
@patch.object(HelicsGossBridge,'_gad_connection')
@unittest.mock.patch('service.helics_goss_bridge.datetime')
@patch.object(helics,'helicsFederateGlobalError')
@patch.object(HelicsGossBridge,'_close_helics_connection')
def test_helics_goss_bridge_on_message(mock_close_helics_connection,
        mock_helicsFederateGlobalError,mock_datetime,mock_gad_connection,
        mock_helicsFederateGetState,mock_init):
    mock_datetime.utcnow.return_value = datetime(2017,8,25,10,33,6,150642)
    bridge = HelicsGossBridge()
    #test initialize messages
    bridge.on_message(message_header, message_is_initialized)
    mock_helicsFederateGetState.assert_called_once()
    message_dict = {
        'received message': {
            'header': message_header,
            'message_content': message_is_initialized
        }
    }
    message_str = json.dumps(message_dict, indent=4, sort_keys=True)
    assert mock_gad_connection.send_simulation_status.call_count == 2
    mock_gad_connection.send_simulation_status.called_with("STARTED",
        message_str,"DEBUG")
    message_str = 'isInitialized check: '+str(False)
    mock_gad_connection.send_simulation_status.called_with("STARTED",
        message_str,"DEBUG")
    message={}
    message['command'] = 'isInitialized'
    message['response'] = str(False)
    t_now = mock_datetime.utcnow()
    message['timestamp'] = int(time.mktime(t_now.timetuple()))
    mock_gad_connection.send.assert_called_once_with(
        bridge.get_simulation_manager_input_topic(),json.dumps(message))
    
    mock_helicsFederateGetState.reset_mock()
    mock_gad_connection.reset_mock()
    mock_helicsFederateGetState.return_value = 2
    bridge.on_message(message_header, message_is_initialized)
    mock_helicsFederateGetState.assert_called_once()
    message_dict = {
        'received message': {
            'header': message_header,
            'message_content': message_is_initialized
        }
    }
    message_str = json.dumps(message_dict, indent=4, sort_keys=True)
    assert mock_gad_connection.send_simulation_status.call_count == 2
    mock_gad_connection.send_simulation_status.called_with("RUNNING",
        message_str,"DEBUG")
    message_str = 'isInitialized check: '+str(True)
    mock_gad_connection.send_simulation_status.called_with("RUNNING",
        message_str,"DEBUG")
    message={}
    message['command'] = 'isInitialized'
    message['response'] = str(True)
    t_now = mock_datetime.utcnow()
    message['timestamp'] = int(time.mktime(t_now.timetuple()))
    mock_gad_connection.send.assert_called_once_with(
        bridge.get_simulation_manager_input_topic(),json.dumps(message))
    #test update message
    mock_helicsFederateGetState.reset_mock()
    mock_gad_connection.reset_mock()
    bridge.on_message(message_header, message_update)
    mock_helicsFederateGetState.assert_called_once()
    message_dict['message_content'] = message_update
    message_str = json.dumps(message_dict, indent=4, sort_keys=True)
    assert mock_gad_connection.send_simulation_status.call_count == 1
    mock_gad_connection.send_simulation_status.called_with("RUNNING",
        message_str,"DEBUG")
    q = bridge.get_simulation_command_queue()
    assert q.qsize() == 1
    command = q.get()
    assert command == json.dumps(message_update["input"])
    #test start simulation message
    mock_helicsFederateGetState.reset_mock()
    mock_gad_connection.reset_mock()
    bridge.on_message(message_header, message_start_simulation)
    mock_helicsFederateGetState.assert_called_once()
    message_dict['message_content'] = message_start_simulation
    message_str = json.dumps(message_dict, indent=4, sort_keys=True)
    assert mock_gad_connection.send_simulation_status.call_count == 1
    mock_gad_connection.send_simulation_status.called_with("RUNNING",
        message_str,"DEBUG")
    assert bridge.get_start_simulation()
    #test comm outage message
    mock_helicsFederateGetState.reset_mock()
    mock_gad_connection.reset_mock()
    bridge.on_message(message_header, message_comm_outage)
    mock_helicsFederateGetState.assert_called_once()
    message_dict['message_content'] = message_comm_outage
    message_str = json.dumps(message_dict, indent=4, sort_keys=True)
    assert mock_gad_connection.send_simulation_status.call_count == 1
    mock_gad_connection.send_simulation_status.called_with("RUNNING",
        message_str,"DEBUG")
    command_filter = bridge.get_command_filter()
    assert command_filter == message_comm_outage["input"]["forward_differences"][0]["inputOutageList"]
    measurement_filter = bridge.get_measurement_filter()
    assert measurement_filter == message_comm_outage["input"]["forward_differences"][0]["outputOutageList"]
    assert not bridge.get_filter_all_commands()
    assert not bridge.get_filter_all_measurements()
    #test stop message
    mock_helicsFederateGetState.reset_mock()
    mock_gad_connection.reset_mock()
    bridge.on_message(message_header, message_stop)
    mock_helicsFederateGetState.assert_called_once()
    message_dict['message_content'] = message_stop
    message_str = json.dumps(message_dict, indent=4, sort_keys=True)
    assert mock_gad_connection.send_simulation_status.call_count == 2
    mock_gad_connection.send_simulation_status.called_with("RUNNING",
        message_str,"DEBUG")
    message_str = "Stopping the simulation"
    mock_gad_connection.send_simulation_status.called_with("CLOSED",
        message_str,"INFO")
    assert not bridge.get_simulation_finished()
    mock_helicsFederateGlobalError.called_once_with(
        bridge.get_helics_federate(), 1, 
        "Stopping the simulation prematurely at operator's request!")
    mock_close_helics_connection.assert_called_once()
    #test pause message
    mock_helicsFederateGetState.reset_mock()
    mock_gad_connection.reset_mock()
    mock_helicsFederateGlobalError.reset_mock()
    mock_close_helics_connection.reset_mock()
    bridge.on_message(message_header, message_pause)
    mock_helicsFederateGetState.assert_called_once()
    message_dict['message_content'] = message_pause
    message_str = json.dumps(message_dict, indent=4, sort_keys=True)
    assert mock_gad_connection.send_simulation_status.call_count == 2
    mock_gad_connection.send_simulation_status.called_with("RUNNING",
        message_str,"DEBUG")
    assert bridge.get_pause_simulation()
    mock_gad_connection.send_simulation_status.called_with('PAUSED', 
        'The simulation has paused.', 'INFO')
    mock_gad_connection.reset_mock()
    bridge.on_message(message_header, message_pause)
    assert mock_gad_connection.send_simulation_status.call_count == 2
    mock_gad_connection.send_simulation_status.called_with('PAUSED', 
        'Pause command received but the simulation is already paused.', 'WARN')
    #test resume message
    mock_helicsFederateGetState.reset_mock()
    mock_gad_connection.reset_mock()
    bridge.on_message(message_header, message_resume)
    mock_helicsFederateGetState.assert_called_once()
    message_dict['message_content'] = message_resume
    message_str = json.dumps(message_dict, indent=4, sort_keys=True)
    assert mock_gad_connection.send_simulation_status.call_count == 2
    mock_gad_connection.send_simulation_status.called_with("RUNNING",
        message_str,"DEBUG")
    assert not bridge.get_pause_simulation()
    mock_gad_connection.send_simulation_status.called_with("RUNNING",
        "The simulation has resumed.","INFO")
    mock_gad_connection.reset_mock()
    bridge.on_message(message_header, message_resume)
    assert mock_gad_connection.send_simulation_status.call_count == 2
    mock_gad_connection.send_simulation_status.called_with("RUNNING",
        "Resume command received but the simulation is already running.",
        "INFO")
    #test resume pause at message
    bridge.on_message(message_header, message_pause)
    mock_helicsFederateGetState.reset_mock()
    mock_gad_connection.reset_mock()
    bridge.on_message(message_header, message_resume_pause_at)
    mock_helicsFederateGetState.assert_called_once()
    message_dict['message_content'] = message_resume_pause_at
    message_str = json.dumps(message_dict, indent=4, sort_keys=True)
    assert mock_gad_connection.send_simulation_status.call_count == 2
    mock_gad_connection.send_simulation_status.called_with("RUNNING",
        message_str,"DEBUG")
    assert not bridge.get_pause_simulation()
    mock_gad_connection.send_simulation_status.called_with("RUNNING",
        "The simulation has resumed.","INFO")
    assert bridge.get_pause_simulation_at() == bridge.get_simulation_time() + 20
    mock_gad_connection.reset_mock()
    bridge.on_message(message_header, message_resume_pause_at)
    assert mock_gad_connection.send_simulation_status.call_count == 2
    mock_gad_connection.send_simulation_status.called_with("RUNNING",
        "The resumePauseAt command was received but the simulation is already running.",
        "WARN")
    #test incorrect json schema message
    mock_helicsFederateGetState.reset_mock()
    mock_gad_connection.reset_mock()
    bridge.on_message(message_header, message_junk)
    mock_helicsFederateGetState.assert_called_once()
    message_dict['message_content'] = message_junk
    message_str = json.dumps(message_dict, indent=4, sort_keys=True)
    assert mock_gad_connection.send_simulation_status.call_count == 2
    mock_gad_connection.send_simulation_status.called_with("RUNNING",
        message_str,"DEBUG")
    mock_gad_connection.send_simulation_status.called_with("WARNING",
        "The message received did not have a command key. Ignoring malformed message.",
        "WARN")
        
    
@patch.object(HelicsGossBridge,'__init__', return_value=None)
@patch.object(HelicsGossBridge,'_gad_connection')
@patch.object(helics,'helicsFederateGlobalError')
@patch.object(HelicsGossBridge,'_close_helics_connection')
def test_helics_goss_bridge_on_error(mock_close_helics_connection,
        mock_helicsFederateGlobalError,mock_gad_connection,mock_init):
    bridge = HelicsGossBridge()
    bridge.on_error(message_header, message_is_initialized)
    message_str = 'Error in HelicsGossBridge: '+str(message_is_initialized)
    mock_gad_connection.send_simulation_status.assert_called_once_with('ERROR',
        message_str, 'ERROR')
    assert bridge.get_stop_simulation()
    mock_helicsFederateGlobalError.assert_called_once_with(
        bridge.get_helics_federate(),1,message_str)
    mock_close_helics_connection.assert_called_once()


@patch.object(HelicsGossBridge,'__init__', return_value=None)
@patch.object(helics,'helicsFederateGlobalError')
@patch.object(HelicsGossBridge,'_close_helics_connection')
def test_helics_goss_bridge_on_disconnected(mock_close_helics_connection,
        mock_helicsFederateGlobalError,mock_init):
    bridge = HelicsGossBridge()
    bridge.on_disconnected()
    message_str = 'HelicsGossBridge instance lost connection to GOSS bus.'
    assert bridge.get_stop_simulation()
    mock_helicsFederateGlobalError.assert_called_once_with(
        bridge.get_helics_federate(),1,message_str)
    mock_close_helics_connection.assert_called_once()


@patch.object(HelicsGossBridge,'__init__', return_value=None)
@patch.object(helics,'helicsFederateGetState', return_value=2)
@patch.object(helics,'helicsFederateGlobalError')
@patch.object(HelicsGossBridge,'_gad_connection')
@patch.object(HelicsGossBridge,'_get_helics_bus_messages',
    return_value=message_update)
@unittest.mock.patch('service.helics_goss_bridge.time')
@patch.object(HelicsGossBridge,'_publish_to_helics_bus')
@patch.object(HelicsGossBridge,'_done_with_time_step')
@patch.object(helics,'helicsFederateFinalize')
@patch.object(HelicsGossBridge,'_close_helics_connection')
def test_helics_goss_bridge_run_simulation(mock_close_helics_connection,
    mock_helicsFederateFinalize,mock_done_with_time_step,
    mock_publish_to_helics_bus,mock_time,mock_get_helics_bus_messages,
    mock_gad_connection,mock_helicsFederateGlobalError,
    mock_helicsFederateGetState,mock_init):
    #test when _stop_simulation == true
    pass
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    