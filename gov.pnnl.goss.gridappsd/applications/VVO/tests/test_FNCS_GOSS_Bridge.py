import pytest
import mock
import sys
import os
import json
from datetime import datetime
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

from apps.fncs_goss_bridge import GOSSListener, _main as fncs_main



@pytest.fixture
def my_fixture():
    print "setup"
    goss_messages = [
        '{'\
            + '"command" : "isInitialized"'\
        '}',
        '{'\
            + '"command" : "update",'\
            + '"message" : {'\
                + '"object" : {'\
                    + '"attribute" : "value"'\
                + '}'\
            + '}'\
        '}',
        '{'\
            + '"command" : "nextTimeStep",'\
            + '"currentTime" : 12'\
        '}',
        '{'\
            + '"command" : "stop"'\
        '}'
    ]
    yield goss_messages
    print "tear down"

@mock.patch('apps.fncs_goss_bridge.datetime')
@mock.patch('apps.fncs_goss_bridge.stomp')
@mock.patch('apps.fncs_goss_bridge.gossConnection')
@mock.patch('apps.fncs_goss_bridge.fncs')
@mock.patch('apps.fncs_goss_bridge._keepAlive')
def test_goss_listener(mock__keepAlive, mock_fncs, mock_gossConnection, mock_stomp, mock_datetime, my_fixture):
    #testing _registerWithGoss and _registerWithFncsBroker
    mock_fncs.is_initialized.return_value = True
    mock_stomp.Connection12.return_value = mock_gossConnection
    mock_fncs.get_events.return_value = ["123"]
    mock_fncs.get_value.return_value = "hello there!"
    mock_datetime.utcnow.return_value = datetime(2017, 8, 25, 10, 33, 6, 150642)
    mock_gossConnection.is_connected.return_value = True
    fncs_main("123")
    #asserts for _registerWithGoss
    mock_stomp.Connection12.assert_called_once_with([('127.0.0.1', '61613')])
    mock_gossConnection.start.assert_called()
    mock_gossConnection.connect.assert_called_once_with('system','manager', wait=True)
    mock_gossConnection.set_listener.assert_called_once()
    mock_gossConnection.subscribe.assert_any_call('/topic/goss/gridappsd/fncs/input',1)
    mock_gossConnection.subscribe.assert_any_call('/queue/goss/gridappsd/fncs/input',2)
    mock_gossConnection.send.assert_called()
    assert 2 == mock_gossConnection.subscribe.call_count
    #asserts for _registerWithFncsBroker
    fncsConfiguration = 'name = FNCS_GOSS_Bridge_123\n'\
        + 'time_delta = 1s\n'\
        + 'broker = tcp://localhost:5570\n'\
        + 'values\n'\
        + '    123\n'\
        + '        topic = 123/fncs_output\n'\
        + '        default = {}\n'\
        + '        type = JSON\n'\
        + '        list = false'
 
    mock_fncs.initialize.assert_called_once_with(fncsConfiguration)
    assert 2 == mock_fncs.is_initialized.call_count
    assert 4 == mock_gossConnection.send.call_count
    mock__keepAlive.assert_called_once()
    #test GOSSListener class
    mock_stomp.reset_mock()
    mock_gossConnection.reset_mock()
    mock_fncs.reset_mock()
    mock__keepAlive.reset_mock()
    inst = GOSSListener()
    msg = my_fixture
    #test the isInitialized command
    arg = {
        'command' : 'isInitialized',
        'response' : 'True',
        'output' : 'hello there!'
    }
    inst.on_message(None, msg[0])
    mock_fncs.get_value.assert_called_once_with('123')
    assert 8 == mock_gossConnection.send.call_count
    mock_gossConnection.send.assert_any_call('/topic/goss/gridappsd/fncs/output', json.dumps(arg)) 
