import json
import sys
import stomp
import time
import os


goss_output_topic = 'goss.gridappsd/fncs/output'
goss_simulation_status_topic = 'goss.gridappsd/simulation/status/'
goss_sim ="goss.gridappsd.process.request.simulation"
# goss_sim = '/queue/goss/gridappsd/process/request/simulation'
test_topic = 'goss.gridappsd.process.test'
test_topic = 'goss.gridappsd.test'

responseQueueTopic = '/temp-queue/response-queue'
goss_simulation_status_topic = '/topic/goss.gridappsd/simulation/status/'


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
        raise ValueError('gossServer must be a nonempty string.\n'
                         + 'gossServer = {0}'.format(gossServer))
    if (stompPort == None or stompPort == ''
        or type(stompPort) != str):
        raise ValueError('stompPort must be a nonempty string.\n'
                         + 'stompPort = {0}'.format(stompPort))
    gossConnection = stomp.Connection12([(gossServer, stompPort)])
    gossConnection.start()
    gossConnection.connect(username,password)
    gossConnection.set_listener('GOSSStatusListener', GOSSStatusListener())
    gossConnection.subscribe(goss_output_topic,1)

_simulationRequest = {
        "power_system_config": {
            "GeographicalRegion_name": "ieee8500nodecktassets_Region",
            "SubGeographicalRegion_name": "ieee8500nodecktassets_SubRegion",
            "Line_name": "ieee8500"
        },
        "simulation_config": {
            "start_time": "2009-07-21 00:00:00",
            "duration": "120",
            "simulator": "GridLAB-D",
            "timestep_frequency": "1000",
            "timestep_increment": "1000",
            "simulation_name": "ieee8500",
            "power_flow_solver_method": "NR",
            "simulation_output": {
                "output_objects": [{
                    "name": "rcon_FEEDER_REG",
                    "properties": ["connect_type", "Control", "control_level", "PT_phase", "band_center", "band_width", "dwell_time", "raise_taps", "lower_taps", "regulation"]
                }, {
                    "name": "rcon_VREG2",
                    "properties": ["connect_type", "Control", "control_level", "PT_phase", "band_center", "band_width", "dwell_time", "raise_taps", "lower_taps", "regulation"]
                }, {
                    "name": "rcon_VREG3",
                    "properties": ["connect_type", "Control", "control_level", "PT_phase", "band_center", "band_width", "dwell_time", "raise_taps", "lower_taps", "regulation"]
                }, {
                    "name": "rcon_VREG4",
                    "properties": ["connect_type", "Control", "control_level", "PT_phase", "band_center", "band_width", "dwell_time", "raise_taps", "lower_taps", "regulation"]
                }, {
                    "name": "reg_FEEDER_REG",
                    "properties": ["configuration", "phases", "to", "tap_A", "tap_B", "tap_C"]
                }, {
                    "name": "reg_VREG2",
                    "properties": ["configuration", "phases", "to", "tap_A", "tap_B", "tap_C"]
                }, {
                    "name": "reg_VREG3",
                    "properties": ["configuration", "phases", "to", "tap_A", "tap_B", "tap_C"]
                }, {
                    "name": "reg_VREG4",
                    "properties": ["configuration", "phases", "to", "tap_A", "tap_B", "tap_C"]
                }, {
                    "name": "cap_capbank0a",
                    "properties": ["phases", "pt_phase", "phases_connected", "control", "control_level", "capacitor_A", "dwell_time", "switchA"]
                }, {
                    "name": "cap_capbank1a",
                    "properties": ["phases", "pt_phase", "phases_connected", "control", "control_level", "capacitor_A", "dwell_time", "switchA"]
                }, {
                    "name": "cap_capbank2a",
                    "properties": ["phases", "pt_phase", "phases_connected", "control", "control_level", "capacitor_A", "dwell_time", "switchA"]
                }, {
                    "name": "cap_capbank0b",
                    "properties": ["phases", "pt_phase", "phases_connected", "control", "control_level", "capacitor_B", "dwell_time", "switchB"]
                }, {
                    "name": "cap_capbank1b",
                    "properties": ["phases", "pt_phase", "phases_connected", "control", "control_level", "capacitor_B", "dwell_time", "switchB"]
                }, {
                    "name": "cap_capbank2b",
                    "properties": ["phases", "pt_phase", "phases_connected", "control", "control_level", "capacitor_B", "dwell_time", "switchB"]
                }, {
                    "name": "cap_capbank0c",
                    "properties": ["phases", "pt_phase", "phases_connected", "control", "control_level", "capacitor_C", "dwell_time", "switchC"]
                }, {
                    "name": "cap_capbank1c",
                    "properties": ["phases", "pt_phase", "phases_connected", "control", "control_level", "capacitor_C", "dwell_time", "switchC"]
                }, {
                    "name": "cap_capbank2c",
                    "properties": ["phases", "pt_phase", "phases_connected", "control", "control_level", "capacitor_C", "dwell_time", "switchC"]
                }, {
                    "name": "cap_capbank3",
                    "properties": ["phases", "pt_phase", "phases_connected", "control", "control_level", "capacitor_A", "capacitor_B", "capacitor_C", "dwell_time", "switchA", "switchB", "switchC"]
                }, {
                    "name": "xf_hvmv_sub",
                    "properties": ["power_in_A", "power_in_B", "power_in_C"]
                }, {
                    "name": "nd_l2955047",
                    "properties": ["voltage_A", "voltage_B", "voltage_C"]
                }, {
                    "name": "nd_l2673313",
                    "properties": ["voltage_A", "voltage_B", "voltage_C"]
                }, {
                    "name": "nd_l3160107",
                    "properties": ["voltage_A", "voltage_B", "voltage_C"]
                }, {
                    "name": "nd_l2876814",
                    "properties": ["voltage_A", "voltage_B", "voltage_C"]
                }, {
                    "name": "nd_l3254238",
                    "properties": ["voltage_A", "voltage_B", "voltage_C"]
                }, {
                    "name": "nd_m1047574",
                    "properties": ["voltage_A", "voltage_B", "voltage_C"]
                }, {
                    "name": "nd__hvmv_sub_lsb",
                    "properties": ["voltage_A", "voltage_B", "voltage_C"]
                }, {
                    "name": "nd_190-8593",
                    "properties": ["voltage_A", "voltage_B", "voltage_C"]
                }, {
                    "name": "nd_190-8581",
                    "properties": ["voltage_A", "voltage_B", "voltage_C"]
                }, {
                    "name": "nd_190-7361",
                    "properties": ["voltage_A", "voltage_B", "voltage_C"]
                }]
            },
            "model_creation_config": {
                "load_scaling_factor": "1",
                "schedule_name": "ieeezipload",
                "z_fraction": "0",
                "i_fraction": "1",
                "p_fraction": "0"
            }
        },
        "application_config":{
            "applications": [
                {"name":"vvo","config_string":"{\"static_inputs\": {\"ieee8500\" : {\"control_method\": \"ACTIVE\", \"capacitor_delay\": 60, \"regulator_delay\": 60, \"desired_pf\": 0.99, \"d_max\": 0.9, \"d_min\": 0.1,\"substation_link\": \"xf_hvmv_sub\",\"regulator_list\": [\"reg_FEEDER_REG\", \"reg_VREG2\", \"reg_VREG3\", \"reg_VREG4\"],\"regulator_configuration_list\": [\"rcon_FEEDER_REG\", \"rcon_VREG2\", \"rcon_VREG3\", \"rcon_VREG4\"],\"capacitor_list\": [\"cap_capbank0a\",\"cap_capbank0b\", \"cap_capbank0c\", \"cap_capbank1a\", \"cap_capbank1b\", \"cap_capbank1c\", \"cap_capbank2a\", \"cap_capbank2b\", \"cap_capbank2c\", \"cap_capbank3\"], \"voltage_measurements\": [\"nd_l2955047,1\", \"nd_l3160107,1\", \"nd_l2673313,2\", \"nd_l2876814,2\", \"nd_m1047574,3\", \"nd_l3254238,4\"],       \"maximum_voltages\": 7500, \"minimum_voltages\": 6500,\"max_vdrop\": 5200,\"high_load_deadband\": 100,\"desired_voltages\": 7000,   \"low_load_deadband\": 100,\"pf_phase\": \"ABC\"}}}"
                }
            ]

        }
    }

def _startTest(username,password,gossServer='localhost',stompPort='61613'):
    simulationCfg = '{"power_system_config":{"GeographicalRegion_name":"ieee8500nodecktassets_Region","SubGeographicalRegion_name":"ieee8500nodecktassets_SubRegion","Line_name":"ieee8500"}, "simulation_config":{"start_time":"03-07-2017 00:00:00","duration":"60","simulator":"GridLAB-D","simulation_name":"my test simulation","power_flow_solver_method":"FBS"}}'
    # testCfg = "{\"testConfigPath\":\"/Users/jsimpson/git/adms/GOSS-GridAPPS-D/gov.pnnl.goss.gridappsd/applications/python/exampleTestConfig.json\",\"testScriptPath\":\"/Users/jsimpson/git/adms/GOSS-GridAPPS-D/gov.pnnl.goss.gridappsd/applications/python/exampleTestScript.json\"}";
    testCfg = '{"testConfigPath":"/Users/jsimpson/git/adms/GOSS-GridAPPS-D/gov.pnnl.goss.gridappsd/test/gov/pnnl/goss/gridappsd/exampleTestConfig.json", \
    		+	"testScriptPath":"/Users/jsimpson/git/adms/GOSS-GridAPPS-D/gov.pnnl.goss.gridappsd/test/gov/pnnl/goss/gridappsd/exampleTestScript.json", \
    		+	"simulationID": 1234, \
    		+	"expectResults":"/Users/jsimpson/git/adms/GOSS-GridAPPS-D/gov.pnnl.goss.gridappsd/test/gov/pnnl/goss/gridappsd/expected_output_series3.json", \
    		+	"simulationOutputObject":"/Users/jsimpson/git/adms/GOSS-GridAPPS-D/gov.pnnl.goss.gridappsd/test/gov/pnnl/goss/gridappsd/sim_output_object.json" \
    }'
    loc = "/home/gridappsd/gridappsd_project/sources/GOSS-GridAPPS-D/gov.pnnl.goss.gridappsd"
    # loc = "/Users/jsimpson/git/adms/GOSS-GridAPPS-D/gov.pnnl.goss.gridappsd"

    loc = os.path.realpath(__file__)
    loc =  os.path.dirname(os.path.dirname(os.path.dirname(loc)))
    print (loc)
    testCfg = {"testConfigPath":loc+"/test/gov/pnnl/goss/gridappsd/exampleTestConfig.json",
            "testScriptPath":loc+"/test/gov/pnnl/goss/gridappsd/exampleTestScript.json",
            "simulationID": 1234,
            "expectResults":loc+"/test/gov/pnnl/goss/gridappsd/expected_output_series3.json",
            "simulationOutputObject":loc+"/test/gov/pnnl/goss/gridappsd/sim_output_object.json"
            }
    testCfg =json.dumps(testCfg)
    simulationCfg=json.dumps(_simulationRequest)

    print (testCfg)

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
    gossConnection.set_listener('GOSSSimulationStartListener',GOSSSimulationStartListener())

    gossConnection.start()
    gossConnection.connect(username, password, wait=True)
    # gossConnection.subscribe(destination='/queue/reply',id=2)
    gossConnection.subscribe(destination=responseQueueTopic,id=2)
    gossConnection.send(body=testCfg, destination=test_topic, headers={'reply-to': '/queue/reply'})
    gossConnection.send(body=simulationCfg, destination=goss_sim, headers={'reply-to': '/queue/reply'})

    time.sleep(3)
    print('sent test request')


if __name__ == "__main__":
    #TODO: send simulationId, fncsBrokerLocation, gossLocation,
    #stompPort, username and password as command line arguments

    _startTest('system','manager',gossServer='127.0.0.1',stompPort='61613')

