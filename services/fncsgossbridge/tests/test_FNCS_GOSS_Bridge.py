from datetime import datetime
import json
import os
import sys

import mock
import pytest

from service.fncs_goss_bridge import GOSSListener, _main as fncs_main


@pytest.fixture
def my_fixture():
    print "setup"
    goss_messages = [
        '{"command" : "isInitialized"}',
        '{"command" : "update","message" : {"object" : {"attribute" : "value"'\
            + '}}}',
        '{"command" : "nextTimeStep","currentTime" : 12}',
        '{"command" : "stop"}'
    ]
    goss_log_messages = [
    '{"status": "started", "timestamp": "2017-08-25 10:33:06", '\
        + '"simulation_id": "123", "log_level": "info", '\
        + '"log_message": "Registered with GOSS on topic '\
        + '/topic/goss/gridappsd/fncs/input True"}',
    '{"status": "started", "timestamp": "2017-08-25 10:33:06", '\
        + '"simulation_id": "123", "log_level": "info", '\
        + '"log_message": "Registering with FNCS broker 123 and broker '\
        + 'tcp://localhost:5570"}',
    '{"status": "started", "timestamp": "2017-08-25 10:33:06", '\
        + '"simulation_id": "123", "log_level": "info", '\
        + '"log_message": "still connected to goss 1 True"}',
    '{"status": "running", "timestamp": "2017-08-25 10:33:06", '\
        + '"simulation_id": "123", "log_level": "info", '\
        + '"log_message": "Registered with fncs True"}',
    '{"status": "running", "timestamp": "2017-08-25 10:33:06", '\
        + '"simulation_id": "123", "log_level": "debug", '\
        + '"log_message": "received message {'\
        + '\\"command\\" : \\"isInitialized\\"}"}',
    '{"status": "running", "timestamp": "2017-08-25 10:33:06", '\
        + '"simulation_id": "123", "log_level": "debug", '\
        + '"log_message": "isInitialized check: True"}',
    '{"status": "running", "timestamp": "2017-08-25 10:33:06", '\
        + '"simulation_id": "123", "log_level": "debug", '\
        + '"log_message": "about to get fncs events"}',
    '{"status": "running", "timestamp": "2017-08-25 10:33:06", '\
        + '"simulation_id": "123", "log_level": "debug", '\
        + '"log_message": "fncs events [\'123\']"}',
    '{"status": "running", "timestamp": "2017-08-25 10:33:06", '\
        + '"simulation_id": "123", "log_level": "debug", '\
        + '"log_message": "fncs_output hello there!"}',
    '{"status": "running", "timestamp": "2017-08-25 10:33:06", '\
        + '"simulation_id": "123", "log_level": "debug", '\
        + '"log_message": "received message {\\"command\\" : \\"update\\",'\
        + '\\"message\\" : {\\"object\\" : {\\"attribute\\" : \\"value\\"}}}"}',
    '{"status": "running", "timestamp": "2017-08-25 10:33:06", '\
        + '"simulation_id": "123", "log_level": "debug", '\
        + '"log_message": "publish to fncs bus 123 {\\"object\\": '\
        + '{\\"attribute\\": \\"value\\"}}"}',
    '{"status": "running", "timestamp": "2017-08-25 10:33:06", '\
        + '"simulation_id": "123", "log_level": "debug", '\
        + '"log_message": "fncs input topic 123/fncs_input"}',
    '{"status": "running", "timestamp": "2017-08-25 10:33:06", '\
        + '"simulation_id": "123", "log_level": "debug", "log_message": '\
        + '"received message {\\"command\\" : \\"nextTimeStep\\",'\
        + '\\"currentTime\\" : 12}"}',
    '{"status": "running", "timestamp": "2017-08-25 10:33:06", '\
        + '"simulation_id": "123", "log_level": "debug", "log_message": "is '\
        + 'next timestep"}',
    '{"status": "running", "timestamp": "2017-08-25 10:33:06", '\
        + '"simulation_id": "123", "log_level": "debug", "log_message": '\
        + '"incrementing to 12"}',
    '{"status": "running", "timestamp": "2017-08-25 10:33:06", '\
        + '"simulation_id": "123", "log_level": "debug", "log_message": "In '\
        + 'done with timestep 12"}',
    '{"status": "running", "timestamp": "2017-08-25 10:33:06", '\
        + '"simulation_id": "123", "log_level": "debug", "log_message": '\
        + '"calling time_request 13"}',
    '{"status": "running", "timestamp": "2017-08-25 10:33:06", '\
        + '"simulation_id": "123", "log_level": "debug", "log_message": '\
        + '"time approved 13"}',
    '{"status": "running", "timestamp": "2017-08-25 10:33:06", '\
        + '"simulation_id": "123", "log_level": "debug", "log_message": '\
        + '"done with timestep 12"}',
    '{"status": "running", "timestamp": "2017-08-25 10:33:06", '\
        + '"simulation_id": "123", "log_level": "debug", "log_message": '\
        + '"simulation id 123"}',
    '{"status": "running", "timestamp": "2017-08-25 10:33:06", '\
        + '"simulation_id": "123", "log_level": "debug", "log_message": '\
        + '"about to get fncs events"}',
    '{"status": "running", "timestamp": "2017-08-25 10:33:06", '\
        + '"simulation_id": "123", "log_level": "debug", "log_message": '\
        + '"fncs events [\'123\']"}',
    '{"status": "running", "timestamp": "2017-08-25 10:33:06", '\
        + '"simulation_id": "123", "log_level": "debug", "log_message": '\
        + '"fncs_output hello there!"}',
    '{"status": "running", "timestamp": "2017-08-25 10:33:06", '\
        + '"simulation_id": "123", "log_level": "debug", "log_message": '\
        + '"sending fncs output message {\\"output\\": \\"hello there!\\", '\
        + '\\"command\\": \\"nextTimeStep\\"}"}',
    '{"status": "running", "timestamp": "2017-08-25 10:33:06", '\
        + '"simulation_id": "123", "log_level": "debug", "log_message": '\
        + '"received message {\\"command\\" : \\"stop\\"}"}',
    '{"status": "stopped", "timestamp": "2017-08-25 10:33:06", '\
        + '"simulation_id": "123", "log_level": "info", "log_message": '\
        + '"Stopping the simulation"}',
    '{"status": "error", "timestamp": "2017-08-25 10:33:06", '\
        + '"simulation_id": "123", "log_level": "error", "log_message": '\
        + '"Error in goss listener This is an error message"}'
    ]
    yield [goss_messages,goss_log_messages]
    print "tear down"


@mock.patch('service.fncs_goss_bridge.datetime')
@mock.patch('service.fncs_goss_bridge.stomp')
@mock.patch('service.fncs_goss_bridge.goss_connection')
@mock.patch('service.fncs_goss_bridge.fncs')
@mock.patch('service.fncs_goss_bridge._keep_alive')
@mock.patch('service.fncs_goss_bridge.sys')
def test_goss_listener(mock_sys,mock__keep_alive, mock_fncs, mock_goss_connection, 
                       mock_stomp, mock_datetime, my_fixture):
    command_msg = my_fixture[0]
    log_msg = my_fixture[1]
    #testing _register_with_goss and _register_with_fncs_broker
    mock_fncs.is_initialized.return_value = True
    mock_stomp.Connection12.return_value = mock_goss_connection
    mock_fncs.get_events.return_value = ["123"]
    mock_fncs.get_value.return_value = "hello there!"
    mock_datetime.utcnow.return_value = datetime(
        2017, 8, 25, 10, 33, 6, 150642)
    mock_goss_connection.is_connected.return_value = True
    mock_fncs.time_request.return_value = 13
    fncs_main("123")
    #asserts for _register_with_goss
    mock_stomp.Connection12.assert_called_once_with([('127.0.0.1', '61613')])
    mock_goss_connection.start.assert_called()
    mock_goss_connection.connect.assert_called_once_with(
        'system','manager', wait=True)
    mock_goss_connection.set_listener.assert_called_once()
    mock_goss_connection.subscribe.assert_any_call(
        '/topic/goss/gridappsd/fncs/input',1)
    mock_goss_connection.subscribe.assert_any_call(
        '/queue/goss/gridappsd/fncs/input',2)
    mock_goss_connection.send.assert_any_call(
        "goss.gridappsd.process.log.simulation", log_msg[0])
    mock_goss_connection.send.assert_any_call(
        "goss.gridappsd.process.log.simulation", log_msg[1])
    mock_goss_connection.send.assert_any_call(
        "goss.gridappsd.process.log.simulation", log_msg[2])
    mock_goss_connection.send.assert_any_call(
        "goss.gridappsd.process.log.simulation", log_msg[3])
    assert 4 == mock_goss_connection.send.call_count
    assert 2 == mock_goss_connection.subscribe.call_count
    #asserts for _register_with_fncs_broker
    fncs_configuration = 'name = FNCS_GOSS_Bridge_123\n'\
        + 'time_delta = 1s\n'\
        + 'broker = tcp://localhost:5570\n'\
        + 'values\n'\
        + '    123\n'\
        + '        topic = 123/fncs_output\n'\
        + '        default = {}\n'\
        + '        type = JSON\n'\
        + '        list = false'
 
    mock_fncs.initialize.assert_called_once_with(fncs_configuration)
    assert 2 == mock_fncs.is_initialized.call_count
    assert 4 == mock_goss_connection.send.call_count
    mock__keep_alive.assert_called_once()
    #test GOSSListener class
    mock_stomp.reset_mock()
    mock_goss_connection.reset_mock()
    mock_fncs.reset_mock()
    mock__keep_alive.reset_mock()
    inst = GOSSListener()
    #test the isInitialized command
    arg = {
        'command' : 'isInitialized',
        'response' : 'True',
        'output' : 'hello there!'
    }
    log_msg1 = '{"status": "running", "timestamp": "2017-08-25 10:33:06", '\
        + '"simulation_id": "123", "log_level": "debug", '\
        + '"log_message": "Added isInitialized output, sending message {'\
        + '\'output\': \'hello there!\', \'command\': \'isInitialized\', '\
        + '\'response\': \'True\'} connection %s"}' % str(mock_goss_connection)
    inst.on_message(None, command_msg[0])
    mock_fncs.get_events.assert_called_once()
    mock_fncs.get_value.assert_called_once_with('123')
    mock_goss_connection.send.assert_any_call(
        "goss.gridappsd.process.log.simulation", log_msg[4])
    mock_goss_connection.send.assert_any_call(
        "goss.gridappsd.process.log.simulation", log_msg[5])
    mock_goss_connection.send.assert_any_call(
        "goss.gridappsd.process.log.simulation", log_msg[6])
    mock_goss_connection.send.assert_any_call(
        "goss.gridappsd.process.log.simulation", log_msg[7])
    mock_goss_connection.send.assert_any_call(
        "goss.gridappsd.process.log.simulation", log_msg[8])
    mock_goss_connection.send.assert_any_call(
        "goss.gridappsd.process.log.simulation", log_msg1)
    mock_goss_connection.send.assert_any_call(
        '/topic/goss/gridappsd/fncs/output', json.dumps(arg)) 
    mock_goss_connection.send.assert_any_call(
        '/queue/goss/gridappsd/fncs/output', json.dumps(arg))
    assert 8 == mock_goss_connection.send.call_count
    #test the update command
    mock_goss_connection.reset_mock()
    mock_fncs.reset_mock()
    inst.on_message(None, command_msg[1])
    mock_goss_connection.send.assert_any_call(
        "goss.gridappsd.process.log.simulation", log_msg[9])
    mock_goss_connection.send.assert_any_call(
        "goss.gridappsd.process.log.simulation", log_msg[10])
    mock_goss_connection.send.assert_any_call(
        "goss.gridappsd.process.log.simulation", log_msg[11])
    assert 3 == mock_goss_connection.send.call_count
    mock_fncs.publish_anon.assert_called_once_with('123/fncs_input', 
        '{"object": {"attribute": "value"}}')
    #test the nextTimeStep command
    arg = {
        'command' : 'nextTimeStep',
        'output' : 'hello there!'
    }
    mock_goss_connection.reset_mock()
    mock_fncs.reset_mock()
    inst.on_message(None, command_msg[2])
    mock_goss_connection.send.assert_any_call(
        "goss.gridappsd.process.log.simulation", log_msg[12])
    mock_goss_connection.send.assert_any_call(
        "goss.gridappsd.process.log.simulation", log_msg[13])
    mock_goss_connection.send.assert_any_call(
        "goss.gridappsd.process.log.simulation", log_msg[14])
    mock_goss_connection.send.assert_any_call(
        "goss.gridappsd.process.log.simulation", log_msg[15])
    mock_goss_connection.send.assert_any_call(
        "goss.gridappsd.process.log.simulation", log_msg[16])
    mock_goss_connection.send.assert_any_call(
        "goss.gridappsd.process.log.simulation", log_msg[17])
    mock_goss_connection.send.assert_any_call(
        "goss.gridappsd.process.log.simulation", log_msg[18])
    mock_goss_connection.send.assert_any_call(
        "goss.gridappsd.process.log.simulation", log_msg[19])
    mock_goss_connection.send.assert_any_call(
        "goss.gridappsd.process.log.simulation", log_msg[20])
    mock_goss_connection.send.assert_any_call(
        "goss.gridappsd.process.log.simulation", log_msg[21])
    mock_goss_connection.send.assert_any_call(
        "goss.gridappsd.process.log.simulation", log_msg[22])
    mock_goss_connection.send.assert_any_call(
        "goss.gridappsd.process.log.simulation", log_msg[23])
    mock_goss_connection.send.assert_any_call(
        '/topic/goss/gridappsd/fncs/output', json.dumps(arg)) 
    mock_goss_connection.send.assert_any_call(
        '/queue/goss/gridappsd/fncs/output', json.dumps(arg))
    assert 14 == mock_goss_connection.send.call_count
    mock_fncs.time_request.assert_called_once_with(13)
    mock_fncs.get_events.assert_called_once()
    mock_fncs.get_value.assert_called_once_with('123')
    #test the stop command
    mock_goss_connection.reset_mock()
    mock_fncs.reset_mock()
    inst.on_message(None, command_msg[3])
    mock_goss_connection.send.assert_any_call(
        "goss.gridappsd.process.log.simulation", log_msg[24])
    mock_goss_connection.send.assert_any_call(
        "goss.gridappsd.process.log.simulation", log_msg[25])
    assert 2 == mock_goss_connection.send.call_count
    mock_sys.exit.assert_called_once()
    mock_fncs.die.assert_called_once()
    #test the on_error function
    mock_goss_connection.reset_mock()
    mock_fncs.reset_mock()
    mock_sys.reset_mock()
    inst.on_error(None, "This is an error message")
    mock_goss_connection.send.assert_any_call(
        "goss.gridappsd.process.log.simulation", log_msg[26])
    mock_fncs.is_initialized.assert_called_once()
    mock_fncs.die.assert_called_once()
    #test the on_disconnect functions
    mock_goss_connection.reset_mock()
    mock_fncs.reset_mock()
    inst.on_disconnected()
    mock_fncs.is_initialized.assert_called_once()
    mock_fncs.die.assert_called_once()
