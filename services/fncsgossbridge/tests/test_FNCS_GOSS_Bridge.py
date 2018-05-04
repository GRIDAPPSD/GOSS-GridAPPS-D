from datetime import datetime
import json
import os
import sys

import mock
from mock import call, patch
import pytest

from service.fncs_goss_bridge import GOSSListener, _main as fncs_main,\
    _get_fncs_bus_messages, simulation_id, _create_cim_object_map, _publish_to_fncs_bus


#class Fncs:

#    @patch('fncs.get_events')
#    def test_get_events(self):
#        return "TEST"
#    @patch('fncs.get_value')
#    def test_get_value(self, simulation_id):
#        return "TEST"

@pytest.fixture
def my_fixture():
    print ("setup")
    goss_messages = [
        '{"command" : "isInitialized"}',
        '{"command" : "update","message" : {"object" : {"attribute" : "value"'\
            + '}}}',
        '{"command" : "nextTimeStep","currentTime" : 12}',
        '{"command" : "stop"}'
    ]
    #goss_log_messages = [
    #'{"source": "fncs_goss_bridge.py", "processId": "123", "timestamp": 1503682536642, "procesStatus": "STARTED", "logMessage": "Registered with GOSS on topic /topic/goss.gridappsd.fncs.input True", "logLevel": "INFO", "storeToDb": true}',
    #'{"source": "fncs_goss_bridge.py", "processId": "123", "timestamp": 1503682536642, "procesStatus": "STARTED", "logMessage": "Registering with FNCS broker 123 and broker tcp://localhost:5570", "logLevel": "INFO", "storeToDb": true}',
    #'{"source": "fncs_goss_bridge.py", "processId": "123", "timestamp": 1503682536642, "procesStatus": "STARTED", "logMessage": "still connected to goss 1 True", "logLevel": "INFO", "storeToDb": true}',
    #'{"source": "fncs_goss_bridge.py", "processId": "123", "timestamp": 1503682536642, "procesStatus": "RUNNING", "logMessage": "Registered with fncs True", "logLevel": "INFO", "storeToDb": true}',
    ##'{"source": "fncs_goss_bridge.py", "processId": "123", "timestamp": 1503682536642, "procesStatus": "RUNNING", "logMessage": "received message {\\"command\\" : \\"isInitialized\\"}", "logLevel": "DEBUG", "storeToDb": true}'
    #'{"source": "fncs_goss_bridge.py", "processId": "123", "timestamp": 1503682536642, "procesStatus": "RUNNING", "logMessage": "isInitialized check: True", "logLevel": "DEBUG", "storeToDb": true}',
    #'{"source": "fncs_goss_bridge.py", "processId": "123", "timestamp": 1503682536642, "procesStatus": "RUNNING", "logMessage": "about to get fncs events", "logLevel": "DEBUG", "storeToDb": true}',
    #'{"source": "fncs_goss_bridge.py", "processId": "123", "timestamp": 1503682536642, "procesStatus": "RUNNING", "logMessage": "fncs events [\'123\']", "logLevel": "DEBUG", "storeToDb": true}',
    #'{"source": "fncs_goss_bridge.py", "processId": "123", "timestamp": 1503682536642, "procesStatus": "ERROR", "logMessage": "Error on get FncsBusMessages for 123 Expecting value: line 1 column 1 (char 0)", "logLevel": "ERROR", "storeToDb": true}',
    #'{"source": "fncs_goss_bridge.py", "processId": "123", "timestamp": 1503682536642, "procesStatus": "RUNNING", "logMessage": "Added isInitialized output, sending message {\'command\': \'isInitialized\', \'response\': \'True\', \'output\': None} connection <MagicMock name=\'goss_connection\' id=\'2414325157504\'>", "logLevel": "DEBUG", "storeToDb": true}'
    #]
    
    expected1 = [call('/topic/goss.gridappsd.simulation.log.123', '{"source": "fncs_goss_bridge.py", "processId": "123", "timestamp": 1503682536642, "procesStatus": "RUNNING", "logMessage": "received message {\\"command\\" : \\"isInitialized\\"}", "logLevel": "DEBUG", "storeToDb": true}'),
         call('/topic/goss.gridappsd.simulation.log.123', '{"source": "fncs_goss_bridge.py", "processId": "123", "timestamp": 1503682536642, "procesStatus": "RUNNING", "logMessage": "isInitialized check: True", "logLevel": "DEBUG", "storeToDb": true}'),
         call('/topic/goss.gridappsd.simulation.log.123', '{"source": "fncs_goss_bridge.py", "processId": "123", "timestamp": 1503682536642, "procesStatus": "RUNNING", "logMessage": "about to get fncs events", "logLevel": "DEBUG", "storeToDb": true}'),
         call('/topic/goss.gridappsd.simulation.log.123', '{"source": "fncs_goss_bridge.py", "processId": "123", "timestamp": 1503682536642, "procesStatus": "RUNNING", "logMessage": "fncs events [\'123\']", "logLevel": "DEBUG", "storeToDb": true}'),
         call('/topic/goss.gridappsd.simulation.log.123', '{"source": "fncs_goss_bridge.py", "processId": "123", "timestamp": 1503682536642, "procesStatus": "ERROR", "logMessage": "Error on get FncsBusMessages for 123 Expecting value: line 1 column 1 (char 0)", "logLevel": "ERROR", "storeToDb": true}'),
         call('/topic/goss.gridappsd.simulation.log.123', '{"source": "fncs_goss_bridge.py", "processId": "123", "timestamp": 1503682536642, "procesStatus": "RUNNING", "logMessage": "Added isInitialized output, sending message {\'command\': \'isInitialized\', \'response\': \'True\', \'output\': None} connection <MagicMock name=\'goss_connection\' id=\'2186087916544\'>", "logLevel": "DEBUG", "storeToDb": true}'),
         call('/topic/goss.gridappsd.simulation.output.123', '{"command": "isInitialized", "response": "True", "output": null, "timestamp": 150642}')]
    expected2 = [(('/topic/goss.gridappsd.simulation.log.123', '{"source": "fncs_goss_bridge.py", "processId": "123", "timestamp": 1503682536642, "procesStatus": "ERROR", "logMessage": "Error in goss listener This is an error message", "logLevel": "ERROR", "storeToDb": true}'),)]
    expected3 = [(('/topic/goss.gridappsd.simulation.log.123', '{"source": "fncs_goss_bridge.py", "processId": "123", "timestamp": 1503682536642, "procesStatus": "ERROR", "logMessage": "Error in goss listener This is an error message", "logLevel": "ERROR", "storeToDb": true}'),)]
    expected4 = [(('/topic/goss.gridappsd.simulation.log.123', '{"source": "fncs_goss_bridge.py", "processId": "123", "timestamp": 1503682536642, "procesStatus": "ERROR", "logMessage": "Error in goss listener This is an error message", "logLevel": "ERROR", "storeToDb": true}'),)]
    expected5 = [(('/topic/goss.gridappsd.simulation.log.123', '{"source": "fncs_goss_bridge.py", "processId": "123", "timestamp": 1503682536642, "procesStatus": "ERROR", "logMessage": "Error in goss listener This is an error message", "logLevel": "ERROR", "storeToDb": true}'),)]
    yield [goss_messages, expected1, expected2, expected3, expected4, expected5]
    print ("tear down")
    
#@mock.patch('service.fncs_goss_bridge.datetime')
@mock.patch('service.fncs_goss_bridge.stomp')
@mock.patch('service.fncs_goss_bridge.goss_connection')
@mock.patch('service.fncs_goss_bridge.fncs')
@mock.patch('service.fncs_goss_bridge._keep_alive')
@mock.patch('service.fncs_goss_bridge.sys')
#@mock.patch('fncs.get_events')
def test_get_fncs_bus_messages(  mock_sys,mock__keep_alive, mock_fncs, mock_goss_connection, 
                       mock_stomp,  my_fixture):
    expected_cim_output = '{"simulation_id": "123", "message": {"timestamp": "2018-04-09 19:13:01.526000", "measurements": [{"magnitude": 8931.737161960202, "angle": -150.9708014534194, "measurement_mrid": "4fa2bf1d-5175-401d-8fdf-0a4c7d41c91b"}, {"magnitude": 8019.082644421096, "angle": 89.19993071626688, "measurement_mrid": "b8f32890-21b4-451e-830e-79c2e31dd342"}, {"magnitude": 9499.06307634266, "angle": -31.204766906698804, "measurement_mrid": "c73ed79e-162d-4a5a-969f-135a44c4a17a"}, {"measurement_mrid": "0faa3210-676e-43d1-bfaa-be516723d30e", "value": 1}, {"measurement_mrid": "38e6c6d3-19ab-49e3-bd43-67b85a97bb7a", "value": 2}, {"measurement_mrid": "64a5bd4a-8439-4ade-b95b-eb749d934c56", "value": 2}, {"magnitude": 8620.341968878965, "angle": -32.614045341051614, "measurement_mrid": "0d9c79d9-59d8-45d1-bc27-68f99438e8b0"}, {"magnitude": 8110.359210459866, "angle": 87.79864029254014, "measurement_mrid": "7bb837b7-8237-43f9-b6ea-17336e10b1cc"}, {"magnitude": 8625.123658472423, "angle": -152.50244807971913, "measurement_mrid": "e9891aac-330d-42cf-af71-00e83e38be0c"}, {"measurement_mrid": "7b4d7916-07c7-42d6-8df0-7a206d37d442", "value": 1}, {"measurement_mrid": "0c11d3fb-6c8b-4e54-b5a4-4f2800691453", "value": 1}, {"measurement_mrid": "cb28aef4-d837-4785-a434-7e35e5e06598", "value": 1}, {"measurement_mrid": "9780e591-7ea8-4d17-9e8e-33684e3bac15", "value": 11}, {"measurement_mrid": "9889475b-5546-4872-983f-ca8d9038a815", "value": 11}, {"measurement_mrid": "9bc6f49f-b1e5-4e93-8f62-fb9118273d99", "value": 4}, {"measurement_mrid": "3794d3ec-8931-46bc-8ffc-8a7fe308afa6", "value": 1}, {"measurement_mrid": "48f3c959-db42-4090-a70e-6fb54e0f699a", "value": 9}, {"measurement_mrid": "d7bcb7c3-ea91-498f-aac5-0548025ed616", "value": 6}, {"measurement_mrid": "466adb99-ea17-4611-a97f-dfbf1a3d03a3", "value": 1}, {"measurement_mrid": "8814ce85-01e9-43f0-b38b-e33b5d0dab19", "value": 1}, {"measurement_mrid": "9ba1cd82-3067-4a2c-b918-22aa1538e63d", "value": 1}, {"measurement_mrid": "a906a245-aa9f-4606-a828-5dd7a373a65a", "value": 1}, {"measurement_mrid": "03f9dae0-c7e9-44e6-b151-48734888a322", "value": 1}, {"measurement_mrid": "592b56da-f05d-4344-a0e9-2b503e194878", "value": 1}, {"measurement_mrid": "0645138c-cbaa-4e99-843d-ede97d5b4be8", "value": 16}, {"measurement_mrid": "70c31c81-ab42-4cef-8025-2fbcc69c4ef8", "value": 10}, {"measurement_mrid": "f005a9ad-0ce3-4c06-8c08-27e9acd0ecf9", "value": 1}, {"magnitude": 7862.366677120198, "angle": 89.3834956925928, "measurement_mrid": "67aa693d-6bcb-49cc-9664-d4807397e6ed"}, {"magnitude": 7990.605705491685, "angle": -150.75993895240802, "measurement_mrid": "6e28c39e-617a-4bef-a22c-27b44293f38c"}, {"magnitude": 8000.2206619258795, "angle": -30.969229724504086, "measurement_mrid": "d10cff1e-b3ed-4952-af42-5b6d8591da93"}, {"magnitude": 8896.947229825257, "angle": -30.99238758400579, "measurement_mrid": "3588d6b4-cd0b-4fca-9b22-f1174501c610"}, {"magnitude": 7916.009003263509, "angle": 89.3705908572135, "measurement_mrid": "728238ed-f180-4963-8955-1e7d09223f86"}, {"magnitude": 8530.231645917991, "angle": -150.77360316030433, "measurement_mrid": "d997ba33-0f23-41d1-9c51-96b773ebce2a"}, {"magnitude": 7719.512532347869, "angle": -150.0531614060996, "measurement_mrid": "6f72781f-0b5c-4a41-ac92-decafa3fe085"}, {"magnitude": 7666.1155132065, "angle": 89.95236700719664, "measurement_mrid": "ac2e6fe0-25f5-427f-9a93-6e52e2a8ae50"}, {"magnitude": 7721.172820640262, "angle": -30.056139502147833, "measurement_mrid": "ee96fdf8-f1fe-43c6-82ac-f360def42af7"}, {"magnitude": 7982.924963004578, "angle": -151.3980753873751, "measurement_mrid": "262ac9bb-b20f-4418-8957-b4102db4f6e5"}, {"magnitude": 7864.615124693286, "angle": 88.85802362591437, "measurement_mrid": "32181461-33d3-42d1-8262-b0a22e415807"}, {"magnitude": 7980.516881345536, "angle": -31.507225236504475, "measurement_mrid": "8150e70f-23dd-4f27-88cd-a03744866e66"}, {"magnitude": 8931.802185551443, "angle": -150.97120819404157, "measurement_mrid": "17257dfd-66bd-4048-9c24-bf762f240d6e"}, {"magnitude": 9498.904219214668, "angle": -31.20528596266024, "measurement_mrid": "574e0cca-f7ca-4d14-a990-255596439d7a"}, {"magnitude": 8018.971757473087, "angle": 89.20009505823447, "measurement_mrid": "5da8a28b-8692-4207-b87e-258ae0f7f303"}, {"magnitude": 1664800.479420552, "angle": -88.08887327611977, "measurement_mrid": "90816913-9d0f-4c9b-b3c2-3e3387a707c3"}, {"magnitude": 1607463.0759631002, "angle": -90.33971927212367, "measurement_mrid": "caa16523-3139-48e1-a83c-db819502f24c"}, {"magnitude": 1580953.1819473784, "angle": -87.39435684321852, "measurement_mrid": "f334b1fd-12b9-4f5e-a61b-c2bd7a2b1dfd"}, {"measurement_mrid": "cad11a02-6d08-42b2-9701-4b8a792ae18b", "value": 1}, {"measurement_mrid": "749e01cb-a2a3-49d5-99a6-be305d506e2e", "value": 1}, {"measurement_mrid": "14eeaeee-e584-4a22-888c-9aae7e9b4db4", "value": 1}, {"magnitude": 8597.292644220375, "angle": -150.96795465941887, "measurement_mrid": "8c2c70ea-ba7f-4cfb-bae1-3054ab1c7ef1"}, {"magnitude": 8965.628346883273, "angle": -31.203946197153353, "measurement_mrid": "b7f83e79-894b-4d6f-81f4-95ade4c8b084"}, {"magnitude": 7968.932685612167, "angle": 89.20111616901903, "measurement_mrid": "cb6a693a-a29e-410a-9c8f-34c355b123e6"}, {"magnitude": 9499.97670151827, "angle": -31.203974393471896, "measurement_mrid": "1183297c-8004-490f-85db-2b64b6302a04"}, {"magnitude": 8019.051476846362, "angle": 89.20108261547666, "measurement_mrid": "7e9eb48c-8d53-468f-b527-b19a3dcfc4c4"}, {"magnitude": 8932.241159556324, "angle": -150.96804133438502, "measurement_mrid": "88ab7ed2-c777-43f5-975c-23db4bb23fdf"}]}}'
    #fncs_main("123")
    #mock_fncs.get_events().return_value = 'test-val-1'
    #mock_fncs.get_value().return_value = 'test-val-2'
    mock_fncs.get_events.return_value = ['123']
    mock_fncs.get_value.return_value = '{"123":{"190-7361":{"voltage_A":"7298.205511-4508.638196j V","voltage_B":"-7548.475769-4082.499982j V","voltage_C":"185.367356+8070.892767j V"},"190-8581":{"voltage_A":"7626.780982-4581.253307j V","voltage_B":"-7444.309460-4164.986026j V","voltage_C":"86.957683+7915.531372j V"},"190-8593":{"voltage_A":"8125.599155-4921.808174j V","voltage_B":"-7809.897484-4334.793354j V","voltage_C":"111.811936+8018.271926j V"},"_hvmv_sub_lsb":{"voltage_A":"6682.945924-3867.136344j V","voltage_B":"-6688.872322-3853.551712j V","voltage_C":"6.373244+7666.112864j V"},"cap_capbank0a":{"capacitor_A":400000.0,"control":"MANUAL","control_level":"BANK","dwell_time":100.0,"phases":"AN","phases_connected":"NA","pt_phase":"A","switchA":"CLOSED"},"cap_capbank0b":{"capacitor_B":400000.0,"control":"MANUAL","control_level":"BANK","dwell_time":101.0,"phases":"BN","phases_connected":"NB","pt_phase":"B","switchB":"CLOSED"},"cap_capbank0c":{"capacitor_C":400000.0,"control":"MANUAL","control_level":"BANK","dwell_time":102.0,"phases":"CN","phases_connected":"NC","pt_phase":"C","switchC":"CLOSED"},"cap_capbank1a":{"capacitor_A":300000.0,"control":"MANUAL","control_level":"BANK","dwell_time":100.0,"phases":"AN","phases_connected":"NA","pt_phase":"A","switchA":"CLOSED"},"cap_capbank1b":{"capacitor_B":300000.0,"control":"MANUAL","control_level":"BANK","dwell_time":101.0,"phases":"BN","phases_connected":"NB","pt_phase":"B","switchB":"CLOSED"},"cap_capbank1c":{"capacitor_C":300000.0,"control":"MANUAL","control_level":"BANK","dwell_time":102.0,"phases":"CN","phases_connected":"NC","pt_phase":"C","switchC":"CLOSED"},"cap_capbank2a":{"capacitor_A":300000.0,"control":"MANUAL","control_level":"BANK","dwell_time":100.0,"phases":"AN","phases_connected":"NA","pt_phase":"A","switchA":"CLOSED"},"cap_capbank2b":{"capacitor_B":300000.0,"control":"MANUAL","control_level":"BANK","dwell_time":101.0,"phases":"BN","phases_connected":"NB","pt_phase":"B","switchB":"CLOSED"},"cap_capbank2c":{"capacitor_C":300000.0,"control":"MANUAL","control_level":"BANK","dwell_time":102.0,"phases":"CN","phases_connected":"NC","pt_phase":"C","switchC":"CLOSED"},"cap_capbank3":{"capacitor_A":300000.0,"capacitor_B":300000.0,"capacitor_C":300000.0,"control":"MANUAL","control_level":"INDIVIDUAL","dwell_time":0.0,"phases":"ABCN","phases_connected":"NCBA","pt_phase":"","switchA":"CLOSED","switchB":"CLOSED","switchC":"CLOSED"},"l2673313":{"voltage_A":"8124.569175-4921.438518j V","voltage_B":"-7809.753237-4334.148666j V","voltage_C":"111.949027+8018.190286j V"},"l2876814":{"voltage_A":"8124.749633-4921.447219j V","voltage_B":"-7809.665614-4334.172554j V","voltage_C":"111.973574+8018.300841j V"},"l2955047":{"voltage_A":"6859.739401-4116.734870j V","voltage_B":"-6972.448750-3903.170246j V","voltage_C":"84.597669+7861.911536j V"},"l3160107":{"voltage_A":"6803.983341-4170.666660j V","voltage_B":"-7008.743845-3821.596614j V","voltage_C":"156.741214+7863.053049j V"},"l3254238":{"voltage_A":"7261.089023-4646.168514j V","voltage_B":"-7650.748282-3982.311998j V","voltage_C":"311.531249+8104.373807j V"},"m1047574":{"voltage_A":"7668.558158-4644.965816j V","voltage_B":"-7517.029222-4172.255084j V","voltage_C":"111.108449+7968.158072j V"},"rcon_FEEDER_REG":{"Control":"MANUAL","PT_phase":"CBA","band_center":0.0,"band_width":0.0,"connect_type":"WYE_WYE","control_level":"INDIVIDUAL","dwell_time":15.0,"lower_taps":16,"raise_taps":16,"regulation":0.10000000000000001},"rcon_VREG2":{"Control":"MANUAL","PT_phase":"CBA","band_center":0.0,"band_width":0.0,"connect_type":"WYE_WYE","control_level":"INDIVIDUAL","dwell_time":15.0,"lower_taps":16,"raise_taps":16,"regulation":0.10000000000000001},"rcon_VREG3":{"Control":"MANUAL","PT_phase":"CBA","band_center":0.0,"band_width":0.0,"connect_type":"WYE_WYE","control_level":"INDIVIDUAL","dwell_time":15.0,"lower_taps":16,"raise_taps":16,"regulation":0.10000000000000001},"rcon_VREG4":{"Control":"MANUAL","PT_phase":"CBA","band_center":0.0,"band_width":0.0,"connect_type":"WYE_WYE","control_level":"INDIVIDUAL","dwell_time":15.0,"lower_taps":16,"raise_taps":16,"regulation":0.10000000000000001},"reg_FEEDER_REG":{"configuration":"rcon_FEEDER_REG","phases":"ABC","tap_A":2,"tap_B":2,"tap_C":1,"to":"_hvmv_sub_lsb"},"reg_VREG2":{"configuration":"rcon_VREG2","phases":"ABC","tap_A":9,"tap_B":6,"tap_C":1,"to":"190-8593"},"reg_VREG3":{"configuration":"rcon_VREG3","phases":"ABC","tap_A":16,"tap_B":10,"tap_C":1,"to":"190-8581"},"reg_VREG4":{"configuration":"rcon_VREG4","phases":"ABC","tap_A":11,"tap_B":11,"tap_C":4,"to":"190-7361"},"xf_hvmv_sub":{"power_in_A":"55519.878956-1663874.448184j VA","power_in_B":"71872.310497-1579318.629819j VA","power_in_C":"-9530.946103-1607434.820343j VA"}}}'
    _create_cim_object_map('tests/model_dict.json')
    res = _get_fncs_bus_messages("123")
    print("res "+res[70])
    print("res "+expected_cim_output[70])
    #mock_fncs._send_simulation_status.assert_called()
    #offset to avoid timestamp
    assert res[70] == expected_cim_output[70]
        
#@mock.patch('service.fncs_goss_bridge.datetime')
@mock.patch('service.fncs_goss_bridge.stomp')
@mock.patch('service.fncs_goss_bridge.goss_connection')
@mock.patch('service.fncs_goss_bridge.fncs')
@mock.patch('service.fncs_goss_bridge._keep_alive')
@mock.patch('service.fncs_goss_bridge.sys')        
def test_publish_to_fncs_bus(  mock_sys,mock__keep_alive, mock_fncs, mock_goss_connection, 
                       mock_stomp,  my_fixture):
    mock_fncs.get_events.return_value = ['123']
    message = '{"simulation_id" : "123","message" : {"timestamp" : "2018-04-03T18:02:48.000Z","difference_mrid" : "123a456b-789c-012d-345e-678f901a234b","reverse_difference" : {"attribute" : "Switch.open","value" : "0" },        "forward_difference" : { "attribute" : "Switch.open","value" : "1"}}}'
    _create_cim_object_map('tests/model_dict.json')
    res = _publish_to_fncs_bus("123", message)
    print("res "+str(res))
    assert 1 == 0

@mock.patch('service.fncs_goss_bridge.datetime')
@mock.patch('service.fncs_goss_bridge.stomp')
@mock.patch('service.fncs_goss_bridge.goss_connection')
@mock.patch('service.fncs_goss_bridge.fncs')
@mock.patch('service.fncs_goss_bridge._keep_alive')
@mock.patch('service.fncs_goss_bridge.sys')
def test_goss_listener(mock_sys,mock__keep_alive, mock_fncs, mock_goss_connection, 
                       mock_stomp, mock_datetime, my_fixture):
    input_from_goss_topic = '/topic/goss.gridappsd.fncs.input' 
    output_to_goss_topic = '/topic/goss.gridappsd.simulation.output.123'
    log_topic = "/topic/goss.gridappsd.simulation.log.123"
    command_msg = my_fixture[0]
    #log_msg = my_fixture[1]
    expected1 = my_fixture[1]
    expected2 = my_fixture[2]
    expected3 = my_fixture[3]
    expected4 = my_fixture[4]
    expected5 = my_fixture[5]
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
        input_from_goss_topic,1)
    #mock_goss_connection.send.assert_any_call(
    #    log_topic, log_msg[0])
    #mock_goss_connection.send.assert_any_call(
    #    log_topic, log_msg[1])
    #mock_goss_connection.send.assert_any_call(
    #    log_topic, log_msg[2])
    #mock_goss_connection.send.assert_any_call(
    #    log_topic, log_msg[3])
    assert 5 == mock_goss_connection.send.call_count
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
    assert 5 == mock_goss_connection.send.call_count
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
    #log_msg1 = '{"status": "running", "timestamp": "2017-08-25 10:33:06", '\
    #    + '"simulation_id": "123", "log_level": "debug", '\
    #    + '"log_message": "Added isInitialized output, sending message {'\
    #    + '\'output\': \'hello there!\', \'command\': \'isInitialized\', '\
    #    + '\'response\': \'True\'} connection %s"}' % str(mock_goss_connection)
    #log_msg1 =  '{"command": "isInitialized", "response": "True", "output": null, "timestamp": 150642}'

    inst.on_message(None, command_msg[0])
    mock_fncs.get_events.assert_called_once()
    mock_fncs.get_value.assert_called_once_with('123')
    #mock_goss_connection.send.assert_any_call(
    #    log_topic, log_msg[4])
    args = mock_goss_connection.send.call_args_list
    print(args)
    print(expected1)
    #for arg in args:
    #    assert arg in expected1
    #assert args == expected1
    #mock_goss_connection.send.assert_any_call(
    #    log_topic, log_msg[5])
    #mock_goss_connection.send.assert_any_call(
    #    log_topic, log_msg[6])
    #mock_goss_connection.send.assert_any_call(
    #    log_topic, log_msg[7])
    #mock_goss_connection.send.assert_any_call(
    #    log_topic, log_msg[8])
    #args2 = mock_goss_connection.send.call_args
    #print(args2)
    #mock_goss_connection.send.assert_any_call(
    #    log_topic, log_msg1)
    #mock_goss_connection.send.assert_any_call(
    #    output_to_goss_topic, json.dumps(arg)) 
    #mock_goss_connection.send.assert_any_call(
    #    output_to_goss_topic, json.dumps(arg))
    assert 8 == mock_goss_connection.send.call_count
    #test the update command
    mock_goss_connection.reset_mock()
    mock_fncs.reset_mock()
    inst.on_message(None, command_msg[1])
    #mock_goss_connection.send.assert_any_call(
    #    log_topic, log_msg[9])
    #mock_goss_connection.send.assert_any_call(
    #    log_topic, log_msg[10])
    #mock_goss_connection.send.assert_any_call(
    #    log_topic, log_msg[11])
    args2 = mock_goss_connection.send.call_args_list
    print(args2)
    assert 3 == mock_goss_connection.send.call_count
    
    #TODO fix this one
    #mock_fncs.publish_anon.assert_called_once_with('123/fncs_input', 
    #    '{"object": {"attribute": "value"}}')
    #test the nextTimeStep command
    arg = {
        'command' : 'nextTimeStep',
        'output' : 'hello there!'
    }
    mock_goss_connection.reset_mock()
    mock_fncs.reset_mock()
    inst.on_message(None, command_msg[2])
    args3 = mock_goss_connection.send.call_args_list
    print(args3)
    #mock_goss_connection.send.assert_any_call(
    #    log_topic, log_msg[12])
    #mock_goss_connection.send.assert_any_call(
    #    log_topic, log_msg[13])
    #mock_goss_connection.send.assert_any_call(
    #    log_topic, log_msg[14])
    #mock_goss_connection.send.assert_any_call(
    #    log_topic, log_msg[15])
    #mock_goss_connection.send.assert_any_call(
    #    log_topic, log_msg[16])
    #mock_goss_connection.send.assert_any_call(
    #    log_topic, log_msg[17])
    #mock_goss_connection.send.assert_any_call(
    #    log_topic, log_msg[18])
    #mock_goss_connection.send.assert_any_call(
    #    log_topic, log_msg[19])
    #mock_goss_connection.send.assert_any_call(
    #    log_topic, log_msg[20])
    #mock_goss_connection.send.assert_any_call(
    #    log_topic, log_msg[21])
    #mock_goss_connection.send.assert_any_call(
    #    log_topic, log_msg[22])
    #mock_goss_connection.send.assert_any_call(
    #    log_topic, log_msg[23])
    #mock_goss_connection.send.assert_any_call(
    #    output_to_goss_topic, json.dumps(arg)) 
    #mock_goss_connection.send.assert_any_call(
    #    output_to_goss_topic, json.dumps(arg))
    assert 14 == mock_goss_connection.send.call_count
    mock_fncs.time_request.assert_called_once_with(13)
    mock_fncs.get_events.assert_called_once()
    mock_fncs.get_value.assert_called_once_with('123')
    #test the stop command
    mock_goss_connection.reset_mock()
    mock_fncs.reset_mock()
    inst.on_message(None, command_msg[3])
    args4 = mock_goss_connection.send.call_args_list
    print(args4)
    #mock_goss_connection.send.assert_any_call(
    #    log_topic, log_msg[24])
    #mock_goss_connection.send.assert_any_call(
    #    log_topic, log_msg[25])
    assert 1 == mock_goss_connection.send.call_count
    mock_sys.exit.assert_called_once()
    mock_fncs.die.assert_called_once()
    #test the on_error function
    mock_goss_connection.reset_mock()
    mock_fncs.reset_mock()
    mock_sys.reset_mock()
    inst.on_error(None, "This is an error message")
    args5 = mock_goss_connection.send.call_args_list
    print(args5)
    print(expected5)

    #assert args5 == expected5
    #mock_goss_connection.send.assert_any_call(
    #    log_topic, log_msg[26])
    mock_fncs.is_initialized.assert_called_once()
    mock_fncs.die.assert_called_once()
    #test the on_disconnect functions
    mock_goss_connection.reset_mock()
    mock_fncs.reset_mock()
    inst.on_disconnected()
    mock_fncs.is_initialized.assert_called_once()
    mock_fncs.die.assert_called_once()
