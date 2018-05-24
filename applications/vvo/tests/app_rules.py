from durable.lang import *
import argparse
import dateutil.parser
from collections import defaultdict
import stomp
import json
import math

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

    def send_log_msg(msg):
        logMsg['logMessage'] = msg
        logMsgStr = json.dumps(logMsg)
        gossConnection.send(body=logMsgStr, destination=goss_log,
                            headers={'reply-to': "/temp-queue/response-queue"})

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


    testInput = ruleset(topic)

    shunt_dict = defaultdict(lambda: {'count':0})
    shunt_threshold = 1

    switch_dict = defaultdict(lambda: {'count':0})
    switch_threshold = 1

    base_kv = 12470 / 3

    with testInput:
        ## Get start and end from TestConfig

        # Check a certain mrid's measurement Voltage P.U.
        @when_all(m.message.measurement.measurement_mrid == "123a456b-789c-012d-345e-678f901a234b")
        def node_meas_check2(c):
            # consequent
            mag = c.m.message.measurement.magnitude
            ang = c.m.message.measurement.angle
            c_temp = complex(mag, ang)
            volt_pu = (math.sqrt(c_temp.real**2 + c_temp.imag**2) / (base_kv) ) / math.sqrt(3)
            if volt_pu < .95 or volt_pu > 1.05:
                print "Voltage out of threshold " + str(volt_pu) + " at " + c.m.message.timestamp
                send_log_msg("Voltage out of threshold " + str(volt_pu) + " at " + c.m.message.timestamp)


        # A Reverse and a Forward difference is a state change.
        @when_all((m.message.reverse_difference.attribute == 'Switch.open') & (
        m.message.reverse_difference.attribute == 'Switch.open'))
        def switch_open(c):
            # consequent
            c.post({'mrid': c.m.message.difference_mrid,
                    'action': c.m.message.reverse_difference.attribute,
                    'timestamp': c.m.message.timestamp})

        @when_all(+m.mrid)
        def count_switch(c):
            switch_dict[c.m.mrid]['count']+=1
            if switch_dict[c.m.mrid]['count'] == switch_threshold:
                print ("For Posting: 3 changes at different times at the same switch.")
                send_log_msg(str(switch_threshold) + " changes at different times at the same switch.")


        # A Reverse and a Forward difference is a state change.
        @when_all((m.message.reverse_differences.allItems(item.attribute == 'ShuntCompensator.sections')) & (
        m.message.reverse_differences.allItems(item.attribute == 'ShuntCompensator.sections')))
        def shunt_change(c):
            # consequent
            # print ('Shunt' + c.m.message.reverse_differences[0])
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
                print ('Shunt change threshold exceeded for shunt object ' + c.m.shunt_object)
                send_log_msg('Shunt change threshold exceeded for shunt object ' + c.m.shunt_object)


        @when_start
        def start(host):

            host.assert_fact(topic, {'mrid': 1, 'time':1})
            host.assert_fact(topic, {'mrid': 1, 'time':2})
            host.assert_fact(topic, {'mrid': 2, 'time':2})
            host.assert_fact(topic, {'mrid': 1, 'time':3})

            host.post(topic, {'mrid': 1234, 'time': 1})
            host.post(topic, {'mrid': 1234, 'time': 1})
            host.post(topic, {'mrid': 1234, 'time': 1})
            meas1 = {
                "simulation_id" : "12ae2345",
                "message" : {
                    "timestamp" : "2018-01-08T13:27:00.000Z",
                    "measurement" : {
                        "measurement_mrid" : "123a456b-789c-012d-345e-678f901a234b",
                        "magnitude" : 1960.512425,
                        "angle" : 6912.904192
                    }
                }
            }
            meas2 = {
                "simulation_id" : "12ae2345",
                "message" : {
                    "timestamp" : "2018-01-08T13:27:00.000Z",
                    "measurement" : {
                        "measurement_mrid" : "123a456b-789c-012d-345e-678f901a234b",
                        "magnitude" : 4154.196028,
                        "angle" : -4422.093355
                    }
                }
            }
            host.post(topic, meas1)
            host.post(topic, meas2)
            # host.post(topic, {"simulation_id": "12ae2345", "message": {"timestamp": "2018-01-08T13:27:00.000Z",
            #                                                      "difference_mrid": "123a456b-789c-012d-345e-678f901a235c",
            #                                                      "reverse_differences": [
            #                                                          {"object": "61A547FB-9F68-5635-BB4C-F7F537FD824E",
            #                                                           "attribute": "ShuntCompensator.sections",
            #                                                           "value": "1"},
            #                                                          {"object": "E3CA4CD4-B0D4-9A83-3E2F-18AC5F1B55BA",
            #                                                           "attribute": "ShuntCompensator.sections",
            #                                                           "value": "0"}], "forward_differences": [
            # {"object": "61A547FB-9F68-5635-BB4C-F7F537FD824E", "attribute": "ShuntCompensator.sections", "value": "0"},
            # {"object": "E3CA4CD4-B0D4-9A83-3E2F-18AC5F1B55BA", "attribute": "ShuntCompensator.sections",
            #  "value": "1"}]}})
            # host.post(topic, {"simulation_id": "12ae2345", "message": {"timestamp": "2018-01-08T13:27:00.000Z",
            #                                                      "difference_mrid": "123a456b-789c-012d-345e-678f901a235c",
            #                                                      "reverse_differences": [
            #                                                          {"object": "61A547FB-9F68-5635-BB4C-F7F537FD824E",
            #                                                           "attribute": "ShuntCompensator.sections",
            #                                                           "value": "1"},
            #                                                          {"object": "E3CA4CD4-B0D4-9A83-3E2F-18AC5F1B55BA",
            #                                                           "attribute": "ShuntCompensator.sections",
            #                                                           "value": "0"}], "forward_differences": [
            # {"object": "61A547FB-9F68-5635-BB4C-F7F537FD824E", "attribute": "ShuntCompensator.sections", "value": "0"},
            # {"object": "E3CA4CD4-B0D4-9A83-3E2F-18AC5F1B55BA", "attribute": "ShuntCompensator.sections",
            #  "value": "1"}]}})
            # host.post(topic, {"simulation_id": "12ae2345", "message": {"timestamp": "2018-01-08T13:27:00.000Z",
            #                                                      "difference_mrid": "123a456b-789c-012d-345e-678f901a235c",
            #                                                      "reverse_differences": [
            #                                                          {"object": "61A547FB-9F68-5635-BB4C-F7F537FD824E",
            #                                                           "attribute": "ShuntCompensator.sections",
            #                                                           "value": "1"},
            #                                                          {"object": "E3CA4CD4-B0D4-9A83-3E2F-18AC5F1B55BA",
            #                                                           "attribute": "ShuntCompensator.sections",
            #                                                           "value": "0"}], "forward_differences": [
            # {"object": "61A547FB-9F68-5635-BB4C-F7F537FD824E", "attribute": "ShuntCompensator.sections", "value": "0"},
            # {"object": "E3CA4CD4-B0D4-9A83-3E2F-18AC5F1B55BA", "attribute": "ShuntCompensator.sections",
            #  "value": "1"}]}})

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
