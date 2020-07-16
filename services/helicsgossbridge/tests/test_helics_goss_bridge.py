from datetime import datetime
import json
import os
try:
    from Queue import Queue
except:
    from queue import Queue
import sys
import unittest.mock
from unittest.mock import call, patch, PropertyMock

from gridappsd import GridAPPSD, utils, topics
import helics
import pytest

from service.helics_goss_bridge import HelicsGossBridge, _main as helics_main


@pytest.fixture()
def messages_fixture():
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
            "reverse_differences" : {
                "allInputOutage" : False,
                "inputOutageList" : [],
                "allOutputOutage" : False,
                "outputOutageList" : []
            },
            "forward_fidderences" : {
                "allInputOutage" : False,
                "inputOutageList" : [],
                "allOutputOutage" : False,
                "outputOutageList" : []
            }
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
@patch.object(helics,'helicsFederateGetState', return_value=2)
@pathc.object(HelicsGossBridge,'_gad_connection')
@patch.object(datetime,'utcnow',datetime(
        2017, 8, 25, 10, 33, 6, 150642))
@patch.object(helics,'helicsFederateGlobalError')
@patch.object(HelicsGossBridge,'_close_helics_connection')
def test_helics_goss_bridge_on_message(mock_close_helics_connection,
        mock_helicsFederateGlobalError,mock_send,mock_utcnow,
        mock_send_simulation_status,mock_helicsFederateGetState,mock_init):
    bridge = HelicsGossBridge()
    bridge.on_message({},{})