from durable.lang import *
import argparse
import dateutil.parser
from collections import defaultdict
import stomp
import json

goss_log = 'goss.gridappsd.platform.log'
def run_rules(topic='input',port=5000, run_start = "2017-07-21 12:00:00", run_end = "2017-07-22 12:00:00"):
    #2017-07-21T18:00Z 2017-07-22T18:00Z
    print ("Start data {0} and end date {1}".format(run_start,run_end))
    gossServer = 'localhost'
    stompPort = '61613'
    username = 'system'
    password = 'manager'
    if (gossServer == None or gossServer == '' or type(gossServer) != str):
        raise ValueError(
                         'gossServer must be a nonempty string.\n'
                         + 'gossServer = {0}'.format(gossServer))
    if (stompPort == None or stompPort == ''
        or type(stompPort) != str):
        raise ValueError(
                         'stompPort must be a nonempty string.\n'
                         + 'stompPort = {0}'.format(stompPort))
    gossConnection = stomp.Connection12([(gossServer, stompPort)])
    try:
        gossConnection.start()
        gossConnection.connect(username, password, wait=True)
    except:
        print ("Not connected to GOSS - messages will not be logged on the platform")


    testInput = ruleset(topic)

    shunt_dict = defaultdict(lambda: {'count':0})
    shunt_threshold = 3

    switch_dict = defaultdict(lambda: {'count':0})
    switch_threshold = 3

    logMsg = {
        # 'id': 401,
        'source': 'rule',
        'processId': 'simple_app.rule',
        'timestamp': '2018-01-23 00:00:00',
        'logLevel': 'INFO',
        'logMessage': 'template msg',
        'processStatus': 'RUNNING',
        'username': username,
        'storeToDb': True}

    with testInput:
        ## Get start and end from TestConfig

        @when_all(m.simulation_id == "12ae234")
        def say_hello_id(c):
            # consequent
            # print 'Hello {0}'.format(c.m.simulation_id)
            print('Shunt' + c.m.message.reverse_differences[0])
            for f in c.m.message.reverse_differences:
                print ('Count MRID Fact: {0} '.format(f))

        # Check a certain mrid's measurement magnitude
        @when_all((m.message.measurement.magnitude <= 3410.456) & (
        m.message.measurement.measurement_mrid == "123a456b-789c-012d-345e-678f901a234b"))
        def node_meas_check(c):
            # consequent
            print 'Magnitude {0}'.format(c.m.message.measurement.magnitude)


        # A Reverse and a Forward difference is a state change.
        @when_all((m.message.reverse_difference.attribute == 'Switch.open') & (
        m.message.reverse_difference.attribute == 'Switch.open'))
        def switch_open(c):
            # consequent
            # print('approved {0}'.format(c.m))
            # print 'Switch id {0}'.format(c.m.message.difference_mrid)
            c.post({'mrid': c.m.message.difference_mrid,
                    'action': c.m.message.reverse_difference.attribute,
                    'timestamp': c.m.message.timestamp})

        @when_all(+m.mrid)
        def count_switch(c):
            # print (c)
            switch_dict[c.m.mrid]['count']+=1
            if switch_dict[c.m.mrid]['count'] == switch_threshold:
                print ("For Posting: 3 changes at different times at the same switch.")
                for f in c.m:
                    print ('Count MRID Fact: {0} '.format(f))

                file = open('testfile.txt', 'w')
                for f in c.m:
                    file.write('Count MRID Fact: {0} '.format(f))
                file.close()
                logMsg['logMessage'] = str(switch_threshold) + " changes at different times at the same switch."
                logMsgStr = json.dumps(logMsg)
                gossConnection.send(body=logMsgStr, destination=goss_log,
                                    headers={'reply-to': "/temp-queue/response-queue"})

        # A Reverse and a Forward difference is a state change.
        @when_all((m.message.reverse_differences.allItems(item.attribute == 'ShuntCompensator.sections')) & (
        m.message.reverse_differences.allItems(item.attribute == 'ShuntCompensator.sections')))
        def shunt_change(c):
            # consequent
            # print ('Shunt' + c.m.message.reverse_differences[0])
            # the_date = dateutil.parser.parse(c.m.message.timestamp)
            for i,f in enumerate(c.m.message.reverse_differences):
                print ('Count shunt changes: {0} '.format(f))
                c.post({
                        # 'shunt_object': c.m.message.difference_mrid,
                        'shunt_object' : f['object'],
                        'action': f['attribute'],
                        'timestamp': c.m.message.timestamp})
        @when_all(+m.shunt_object)
        def count_shunt_object(c):
            # print (c)
            shunt_dict[c.m.shunt_object]['count']+=1
            if shunt_dict[c.m.shunt_object]['count'] == shunt_threshold:
                print ('Shunt change threshold exceeded for shunt object' + c.m.shunt_object)
                logMsg['logMessage'] = 'Shunt change threshold exceeded for shunt object' + c.m.shunt_object
                logMsgStr = json.dumps(logMsg)
                gossConnection.send(body=logMsgStr, destination=goss_log,
                                    headers={'reply-to': "/temp-queue/response-queue"})


        @when_start
        def start(host):

            host.assert_fact('input', {'mrid': 1, 'time':1})
            host.assert_fact('input', {'mrid': 1, 'time':2})
            host.assert_fact('input', {'mrid': 2, 'time':2})
            host.assert_fact('input', {'mrid': 1, 'time':3})

            host.post('input', {'mrid': 1234, 'time': 1})
            host.post('input', {'mrid': 1234, 'time': 1})
            host.post('input', {'mrid': 1234, 'time': 1})

            host.post('input', {"simulation_id": "12ae2345", "message": {"timestamp": "2018-01-08T13:27:00.000Z",
                                                                 "difference_mrid": "123a456b-789c-012d-345e-678f901a235c",
                                                                 "reverse_differences": [
                                                                     {"object": "61A547FB-9F68-5635-BB4C-F7F537FD824E",
                                                                      "attribute": "ShuntCompensator.sections",
                                                                      "value": "1"},
                                                                     {"object": "E3CA4CD4-B0D4-9A83-3E2F-18AC5F1B55BA",
                                                                      "attribute": "ShuntCompensator.sections",
                                                                      "value": "0"}], "forward_differences": [
            {"object": "61A547FB-9F68-5635-BB4C-F7F537FD824E", "attribute": "ShuntCompensator.sections", "value": "0"},
            {"object": "E3CA4CD4-B0D4-9A83-3E2F-18AC5F1B55BA", "attribute": "ShuntCompensator.sections",
             "value": "1"}]}})
            host.post('input', {"simulation_id": "12ae2345", "message": {"timestamp": "2018-01-08T13:27:00.000Z",
                                                                 "difference_mrid": "123a456b-789c-012d-345e-678f901a235c",
                                                                 "reverse_differences": [
                                                                     {"object": "61A547FB-9F68-5635-BB4C-F7F537FD824E",
                                                                      "attribute": "ShuntCompensator.sections",
                                                                      "value": "1"},
                                                                     {"object": "E3CA4CD4-B0D4-9A83-3E2F-18AC5F1B55BA",
                                                                      "attribute": "ShuntCompensator.sections",
                                                                      "value": "0"}], "forward_differences": [
            {"object": "61A547FB-9F68-5635-BB4C-F7F537FD824E", "attribute": "ShuntCompensator.sections", "value": "0"},
            {"object": "E3CA4CD4-B0D4-9A83-3E2F-18AC5F1B55BA", "attribute": "ShuntCompensator.sections",
             "value": "1"}]}})
            host.post('input', {"simulation_id": "12ae2345", "message": {"timestamp": "2018-01-08T13:27:00.000Z",
                                                                 "difference_mrid": "123a456b-789c-012d-345e-678f901a235c",
                                                                 "reverse_differences": [
                                                                     {"object": "61A547FB-9F68-5635-BB4C-F7F537FD824E",
                                                                      "attribute": "ShuntCompensator.sections",
                                                                      "value": "1"},
                                                                     {"object": "E3CA4CD4-B0D4-9A83-3E2F-18AC5F1B55BA",
                                                                      "attribute": "ShuntCompensator.sections",
                                                                      "value": "0"}], "forward_differences": [
            {"object": "61A547FB-9F68-5635-BB4C-F7F537FD824E", "attribute": "ShuntCompensator.sections", "value": "0"},
            {"object": "E3CA4CD4-B0D4-9A83-3E2F-18AC5F1B55BA", "attribute": "ShuntCompensator.sections",
             "value": "1"}]}})

    run_all(port=port)

if __name__ == '__main__':
    x = '{"run_start" : "2017-07-21 12:00:00", "run_end" : "2017-07-22 12:00:00"}'
    #--topic input --port 5011 '{"run_start" : "2017-07-21 12:00:00", "run_end" : "2017-07-22 12:00:00"}'
    parser = argparse.ArgumentParser()
    parser.add_argument("-t","--topic", type=str, help="topic, the default is input", default="input")
    parser.add_argument("-p","--port", type=int, help="port number, the default is 5000", default=5000)
    parser.add_argument("-i", "--id", type=int, help="simulation id")
    parser.add_argument("--start_date", type=str, help="Simulation start date", default="2017-07-21 12:00:00", required=False)
    parser.add_argument("--end_date", type=str, help="Simulation end date" , default="2017-07-22 12:00:00", required=False)
    # parser.add_argument('-o', '--options', type=str, default='{}')
    args = parser.parse_args()
    # options_dict = json.loads(args.options)

    run_rules(topic=args.topic, port=args.port, run_start=args.start_date, run_end=args.end_date)


# run_all([{"host":"127.0.0.1","port":5511 }])
# run_all([{'host': 'host_name', 'port': port, 'password': 'password'}]);

# curl -H "Content-type: application/json" -X POST -d '{ "simulation_id" : "12ae2345", "message" : { "timestamp" : "YYYY-MM-DDThh:mm:ss.sssZ", "measurement" : { "measurement_mrid" : "123a456b-789c-012d-345e-678f901a234b", "magnitude" : 3410.456, "angle" : -123.456 } }}' http://localhost:5000/test/events
# curl -H "Content-type: application/json" -X POST -d '{"simulation_id" : "12ae2345", "message" : { "timestamp" : "YYYY-MM-DDThh:mm:ss.sssZ", "difference_mrid" : "123a456b-789c-012d-345e-678f901a234b", "reverse_difference" : { "attribute" : "Switch.open", "value" : "0" }, "forward_difference" : { "attribute" : "Switch.open", "value" : "1" } }}' http://localhost:5000/input/events
# curl -H "Content-type: application/json" -X POST -d '{"subject": "World"}' http://localhost:5000/test/events
# curl -H "Content-type: application/json" -X POST -d '{"simulation_id": "12ae2345",  "message": {"timestamp": "2018-01-08T13:27:00.000Z", "difference_mrid": "123a456b-789c-012d-345e-678f901a235c","reverse_differences": [{"object": "61A547FB-9F68-5635-BB4C-F7F537FD824E","attribute": "ShuntCompensator.sections","value": "1"}, {"object": "E3CA4CD4-B0D4-9A83-3E2F-18AC5F1B55BA","attribute": "ShuntCompensator.sections","value": "0"}],"forward_differences": [{"object": "61A547FB-9F68-5635-BB4C-F7F537FD824E","attribute": "ShuntCompensator.sections","value": "0"},{"object": "E3CA4CD4-B0D4-9A83-3E2F-18AC5F1B55BA","attribute": "ShuntCompensator.sections","value": "1"}]}}' http://localhost:5000/input/events