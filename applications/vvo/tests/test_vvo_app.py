import pytest
import mock
import sys
import os
import json
import yaml
from datetime import datetime
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))
from vvoapps.vvoapp import GOSSListener, _registerWithGOSS as vvo_reg
import vvoapps.vvoapp as vvoapp
import argparse

@pytest.fixture
def my_fixture():
    print "setup"
    vvo_static_config = {
		'ieee8500' : {
		    'control_method' : 'ACTIVE',
		    'capacitor_delay': 60.0,
		    'regulator_delay': 60.0,
		    'desired_pf': 0.99,
		    'd_max': 0.9,
		    'd_min': 0.1,
		    'substation_link': 'xf_hvmv_sub',
		    'regulator_list': ['reg_FEEDER_REG','reg_VREG2','reg_VREG3','reg_VREG4'
				],
		    'regulator_configuration_list': ["rcon_FEEDER_REG", "rcon_VREG2",
		        "rcon_VREG3", "rcon_VREG4"],
		    'capacitor_list': ['cap_capbank0a', 'cap_capbank0b', 'cap_capbank0c', 
		        'cap_capbank1a', 'cap_capbank1b', 'cap_capbank1c', 
		        'cap_capbank2a', 'cap_capbank2b', 'cap_capbank2c', 'cap_capbank3'],
		    'voltage_measurements': ['nd_l2955047,1','nd_l3160107,1',
		    'nd_l2673313,2','nd_l2876814,2','nd_m1047574,3','nd_l3254238,4'],
		    'maximum_voltages': [7500, 7500, 7500, 7500],
		    'minimum_voltages': [6500, 6500, 6500, 6500],
		    'max_vdrop': [5200, 5200, 5200, 5200],
		    'high_load_deadband': [100, 100, 100, 100],
		    'desired_voltages': [7000, 7000, 7000, 7000],
		    'low_load_deadband': [100, 100, 100, 100],
		    'pf_phase': 'ABC'
		}
    }
    vvo_message_dict = { 
	    "ieee8500":{
	        "cap_capbank0a":{
	            "capacitor_A":400000.0,
	            "control":"MANUAL",
	            "control_level":"BANK",
	            "dwell_time":100.0,
	            "phases":"AN",
	            "phases_connected":"NA",
	            "pt_phase":"A",
	            "switchA":"CLOSED"
	        },
	        "cap_capbank0b":{
	            "capacitor_B":400000.0,
	            "control":"MANUAL",
	            "control_level":"BANK",
	            "dwell_time":101.0,
	            "phases":"BN",
	            "phases_connected":"NB",
	            "pt_phase":"B",
	            "switchB":"CLOSED"
	        },
	        "cap_capbank0c":{
	            "capacitor_C":400000.0,
	            "control":"MANUAL",
	            "control_level":"BANK",
	            "dwell_time":102.0,
	            "phases":"CN",
	            "phases_connected":"NC",
	            "pt_phase":"C",
	            "switchC":"CLOSED"
	        },
	        "cap_capbank1a":{
	            "capacitor_A":300000.0,
	            "control":"MANUAL",
	            "control_level":"BANK",
	            "dwell_time":100.0,
	            "phases":"AN",
	            "phases_connected":"NA",
	            "pt_phase":"A",
	            "switchA":"CLOSED"
	        },
	        "cap_capbank1b":{
	            "capacitor_B":300000.0,
	            "control":"MANUAL",
	            "control_level":"BANK",
	            "dwell_time":101.0,
	            "phases":"BN",
	            "phases_connected":"NB",
	            "pt_phase":"B",
	            "switchB":"CLOSED"
	        },
	        "cap_capbank1c":{
	            "capacitor_C":300000.0,
	            "control":"MANUAL",
	            "control_level":"BANK",
	            "dwell_time":102.0,
	            "phases":"CN",
	            "phases_connected":"NC",
	            "pt_phase":"C",
	            "switchC":"CLOSED"
	        },
	        "cap_capbank2a":{
	            "capacitor_A":300000.0,
	            "control":"MANUAL",
	            "control_level":"BANK",
	            "dwell_time":100.0,
	            "phases":"AN",
	            "phases_connected":"NA",
	            "pt_phase":"A",
	            "switchA":"CLOSED"
	        },
	        "cap_capbank2b":{
	            "capacitor_B":300000.0,
	            "control":"MANUAL",
	            "control_level":"BANK",
	            "dwell_time":101.0,
	            "phases":"BN",
	            "phases_connected":"NB",
	            "pt_phase":"B",
	            "switchB":"CLOSED"
	        },
	        "cap_capbank2c":{
	            "capacitor_C":300000.0,
	            "control":"MANUAL",
	            "control_level":"BANK",
	            "dwell_time":102.0,
	            "phases":"CN",
	            "phases_connected":"NC",
	            "pt_phase":"C",
	            "switchC":"CLOSED"
	        },
	        "cap_capbank3":{
	            "capacitor_A":300000.0,
	            "capacitor_B":300000.0,
	            "capacitor_C":300000.0,
	            "control":"MANUAL",
	            "control_level":"INDIVIDUAL",
	            "dwell_time":0.0,
	            "phases":"ABCN",
	            "phases_connected":"NCBA",
	            "pt_phase":"",
	            "switchA":"CLOSED",
	            "switchB":"CLOSED",
	            "switchC":"CLOSED"
	        },
	        "nd_190-7361":{
	            "voltage_A":"6410.387411-4584.456974j V",
	            "voltage_B":"-7198.592139-3270.308372j V",
	            "voltage_C":"642.547265+7539.531175j V"
	        },
	        "nd_190-8581":{
	            "voltage_A":"6485.244722-4692.686497j V",
	            "voltage_B":"-7183.641237-3170.693324j V",
	            "voltage_C":"544.875720+7443.341013j V"
	        },
	        "nd_190-8593":{
	            "voltage_A":"6723.279162-5056.725836j V",
	            "voltage_B":"-7494.205738-3101.034602j V",
	            "voltage_C":"630.475857+7534.534977j V"
	        },
	        "nd__hvmv_sub_lsb":{
	            "voltage_A":"6261.474438-3926.148203j V",
	            "voltage_B":"-6529.409296-3466.545236j V",
	            "voltage_C":"247.131622+7348.295282j V"
	        },
	        "nd_l2673313":{
	            "voltage_A":"6569.522312-5003.052614j V",
	            "voltage_B":"-7431.486583-3004.840139j V",
	            "voltage_C":"644.553331+7464.115915j V"
	        },
	        "nd_l2876814":{
	            "voltage_A":"6593.064915-5014.031801j V",
	            "voltage_B":"-7430.572726-3003.995538j V",
	            "voltage_C":"643.473396+7483.558765j V"
	        },
	        "nd_l2955047":{
	            "voltage_A":"5850.305846-4217.166594j V",
	            "voltage_B":"-6729.652722-2987.617376j V",
	            "voltage_C":"535.302083+7395.127354j V"
	        },
	        "nd_l3160107":{
	            "voltage_A":"5954.507575-4227.423005j V",
	            "voltage_B":"-6662.357613-3055.346879j V",
	            "voltage_C":"600.213657+7317.832960j V"
	        },
	        "nd_l3254238":{
	            "voltage_A":"6271.490549-4631.254028j V",
	            "voltage_B":"-7169.987847-3099.952683j V",
	            "voltage_C":"751.609655+7519.062260j V"
	        },
	        "nd_m1047574":{
	            "voltage_A":"6306.632406-4741.568924j V",
	            "voltage_B":"-7214.626338-2987.055914j V",
	            "voltage_C":"622.058711+7442.125124j V"
	        },
	        "rcon_FEEDER_REG":{
	            "Control":"MANUAL",
	            "PT_phase":"CBA",
	            "band_center":126.5,
	            "band_width":2.0,
	            "connect_type":"WYE_WYE",
	            "control_level":"INDIVIDUAL",
	            "dwell_time":15.0,
	            "lower_taps":16,
	            "raise_taps":16,
	            "regulation":0.10000000000000001
	        },
	        "rcon_VREG2":{
	            "Control":"MANUAL",
	            "PT_phase":"CBA",
	            "band_center":125.0,
	            "band_width":2.0,
	            "connect_type":"WYE_WYE",
	            "control_level":"INDIVIDUAL",
	            "dwell_time":15.0,
	            "lower_taps":16,
	            "raise_taps":16,
	            "regulation":0.10000000000000001
	        },
	        "rcon_VREG3":{
	            "Control":"MANUAL",
	            "PT_phase":"CBA",
	            "band_center":125.0,
	            "band_width":2.0,
	            "connect_type":"WYE_WYE",
	            "control_level":"INDIVIDUAL",
	            "dwell_time":15.0,
	            "lower_taps":16,
	            "raise_taps":16,
	            "regulation":0.10000000000000001
	        },
	        "rcon_VREG4":{
	            "Control":"MANUAL",
	            "PT_phase":"CBA",
	            "band_center":125.0,
	            "band_width":2.0,
	            "connect_type":"WYE_WYE",
	            "control_level":"INDIVIDUAL",
	            "dwell_time":15.0,
	            "lower_taps":16,
	            "raise_taps":16,
	            "regulation":0.10000000000000001
	        },
	        "reg_FEEDER_REG":{
	            "configuration":"rcon_FEEDER_REG",
	            "phases":"ABC",
	            "tap_A":2,
	            "tap_B":2,
	            "tap_C":1,
	            "to":"nd__hvmv_sub_lsb"
	        },
	        "reg_VREG2":{
	            "configuration":"rcon_VREG2",
	            "phases":"ABC",
	            "tap_A":10,
	            "tap_B":6,
	            "tap_C":2,
	            "to":"nd_190-8593"
	        },
	        "reg_VREG3":{
	            "configuration":"rcon_VREG3",
	            "phases":"ABC",
	            "tap_A":16,
	            "tap_B":10,
	            "tap_C":1,
	            "to":"nd_190-8581"
	        },
	        "reg_VREG4":{
	            "configuration":"rcon_VREG4",
	            "phases":"ABC",
	            "tap_A":12,
	            "tap_B":12,
	            "tap_C":5,
	            "to":"nd_190-7361"
	        },
	        "xf_hvmv_sub":{
	            "power_in_A":"1739729.121744-774784.928874j VA",
	            "power_in_B":"1659762.622236-785218.729252j VA",
	            "power_in_C":"1709521.679116-849734.584017j VA"
	        }
	    }
	}
    vvo_output_dict = {
		"ieee8500": {
			"reg_FEEDER_REG": {
				"tap_C": 0,
				"tap_B": 1,
				"tap_A": 1
			},
			"reg_VREG4": {
				"tap_C": 4,
				"tap_B": 11,
				"tap_A": 11
			},
			"reg_VREG2": {
				"tap_C": 1,
				"tap_B": 5,
				"tap_A": 9
			},
			"reg_VREG3": {
				"tap_C": 0,
				"tap_B": 9,
				"tap_A": 15
			}
		}
    }
    vvo_message = {"output":json.dumps(vvo_message_dict)}
    yield [vvo_static_config, json.dumps(vvo_message), vvo_output_dict]
    print "tear down"


@mock.patch('vvoapps.vvoapp.gossConnection')
@mock.patch('vvoapps.vvoapp.stomp')
@mock.patch('vvoapps.vvoapp.logging')
def test_vvo_app(mock_logging, mock_stomp, mock_gossConnection, my_fixture):
	#test _registerWithGoss function
	parser = argparse.ArgumentParser(version=vvoapp.__version__)
	parser.add_argument("-t0", default=2, type=int,
                        help="T0 start value for the application.")
	vvoapp.opts = parser.parse_args()
	vvoapp.static_config = my_fixture[0]
	
	msg = my_fixture[1]
	vvo_output = my_fixture[2]
	mock_stomp.Connection12.return_value = mock_gossConnection
	mock_logging.getLogger.return_value = mock_logging
	vvo_reg("username", "password", "gossServer", "13259")
	mock_stomp.Connection12.assert_called_once_with([("gossServer", "13259")])
	mock_gossConnection.start.assert_called_once()
	mock_gossConnection.connect.assert_called_once_with("username", "password", wait=True)
	mock_gossConnection.set_listener.assert_called_once()
	assert 2 == vvoapp.opts.t0
	mock_gossConnection.subscribe.assert_any_call('/topic/goss/gridappsd/fncs/output', 1)
	mock_gossConnection.subscribe.assert_any_call('/topic/goss/gridappsd/fncs/output', 2)
	assert 2 == mock_gossConnection.subscribe.call_count
	mock_gossConnection.is_connected.assert_called_once()
	#test the appOutput function
	mock_stomp.reset_mock()
	mock_gossConnection.reset_mock()
	vvoapp.appOutput(vvo_output)
	mock_gossConnection.send.assert_called_once_with('/topic/goss/gridappsd/fncs/input', json.dumps({"command":"update","message":vvo_output}))
	#test GOSSListener class
	mock_gossConnection.reset_mock()
	inst = GOSSListener(4)
	assert inst.t0 == 4
	#test the on_message function
	inst.on_message(None, msg)
	mock_gossConnection.send.assert_called_once_with('/topic/goss/gridappsd/fncs/input', json.dumps({"command":"update","message":vvo_output}))
	#test the on_error function
	mock_logging.reset_mock()
	vvoapp.logger = mock_logging
	inst.on_error(None, "this is my error message")
	mock_logging.error.assert_called_once_with('Error in goss listener this is my error message')
	#test the on_disconnect function
	mock_logging.reset_mock()
	vvoapp.logger = mock_logging
	inst.on_disconnected()
	mock_logging.error.assert_called_once_with('Disconnected')
