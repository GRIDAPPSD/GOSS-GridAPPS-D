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

