import json
import sys
import stomp
import time
import os
import argparse

goss_output_topic = 'goss.gridappsd/fncs/output'
goss_simulation_status_topic = 'goss.gridappsd/simulation/status/'
goss_sim ="goss.gridappsd.process.request.simulation"
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

def _startTest(username,password,gossServer='localhost',stompPort='61613', simulationID=1234, rulePort=5000, topic="input"):
    loc = os.path.realpath(__file__)
    loc =  os.path.dirname(loc)
    print (loc)
    testCfg = {"testConfigPath":loc+"/VVOTestConfig.json",
            "testScriptPath":loc+"/VVOTestScript.json",
            "simulationID": 1234,
            "rulePort": 5000,
            "topic":"input",
            "expectedResult":loc+"/expected_result_series.json"
            }
    testCfg =json.dumps(testCfg)
    print(testCfg)

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
    gossConnection.subscribe(destination=responseQueueTopic,id=2)
    gossConnection.send(body=testCfg, destination=test_topic, headers={'reply-to': '/queue/reply'})

    time.sleep(3)
    print('sent test request')


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("-t","--topic", type=str, help="topic, the default is input", default="input", required=False)
    parser.add_argument("-p","--port", type=int, help="port number, the default is 5000", default=5000, required=False)
    parser.add_argument("-i", "--id", type=int, help="simulation id", required=False)
    parser.add_argument("--start_date", type=str, help="Simulation start date", default="2017-07-21 12:00:00", required=False)
    parser.add_argument("--end_date", type=str, help="Simulation end date" , default="2017-07-22 12:00:00", required=False)
    # parser.add_argument('-o', '--options', type=str, default='{}')
    args = parser.parse_args()


    _startTest('system','manager',gossServer='127.0.0.1',stompPort='61613', simulationID=args.id, rulePort=args.port, topic=args.topic)
