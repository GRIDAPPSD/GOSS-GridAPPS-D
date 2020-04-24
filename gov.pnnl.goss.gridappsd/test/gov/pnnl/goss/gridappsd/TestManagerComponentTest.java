package gov.pnnl.goss.gridappsd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map.Entry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gov.pnnl.goss.gridappsd.api.AppManager;
import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.api.TestManager;
import gov.pnnl.goss.gridappsd.api.TimeseriesDataManager;
import gov.pnnl.goss.gridappsd.configuration.ConfigurationManagerImpl;
import gov.pnnl.goss.gridappsd.data.DataManagerImpl;
import gov.pnnl.goss.gridappsd.data.ProvenTimeSeriesDataManagerImpl;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.RequestTestUpdate;
import gov.pnnl.goss.gridappsd.dto.RequestTimeseriesData;
//import gov.pnnl.goss.gridappsd.dto.TestConfiguration;
//import gov.pnnl.goss.gridappsd.dto.TestScript;
import gov.pnnl.goss.gridappsd.testmanager.CompareResults;
import gov.pnnl.goss.gridappsd.testmanager.HistoricalComparison;
import gov.pnnl.goss.gridappsd.testmanager.TestManagerImpl;
import gov.pnnl.goss.gridappsd.testmanager.TestResultSeries;
import gov.pnnl.goss.gridappsd.testmanager.TestResults;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.Response;
import gov.pnnl.proven.api.producer.ProvenProducer;
import gov.pnnl.proven.api.producer.ProvenResponse;
import pnnl.goss.core.Client;
import pnnl.goss.core.ClientFactory;

@RunWith(MockitoJUnitRunner.class)
public class TestManagerComponentTest {

	TestManager tm = new TestManagerImpl();

	@Mock
	AppManager appManager;

	@Mock
	ClientFactory clientFactory;

	@Mock
	Client client;

	@Mock
	ConfigurationManager configurationManager;

	@Mock
	DataManager dataManager;

//	@Mock
//	DataManager DataManager;

	@Mock
	SimulationManager simulationManager;

	@Mock
	LogManager logManager;

	@Captor
	ArgumentCaptor<String> argCaptor;

	@Captor
	ArgumentCaptor<LogMessage> argCaptorLogMessage;

	/**
	 *    Succeeds when info log message is called at the start of the process manager implementation with the expected message
	 */
	public void infoCalledWhen_processManagerStarted(){

//		ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
		try {
			Mockito.when(clientFactory.create(Mockito.any(),  Mockito.any())).thenReturn(client);
		} catch (Exception e) {
			e.printStackTrace();
		}

		TestManagerImpl testManager = new TestManagerImpl(clientFactory, logManager, dataManager, simulationManager);
		testManager.start();


		Mockito.verify(logManager).log(argCaptorLogMessage.capture(), argCaptor.capture(),argCaptor.capture()); //GridAppsDConstants.username);

		LogMessage logMessage = argCaptorLogMessage.getAllValues().get(0);

		assertEquals(logMessage.getLogLevel(), LogLevel.DEBUG);
		assertEquals(logMessage.getLogMessage(), "Starting "+TestManagerImpl.class.getName());
		assertEquals(logMessage.getProcessStatus(), ProcessStatus.RUNNING);

		assertNotNull(logMessage.getTimestamp());
	}

	@Test
	public void testLoadConfig(){
		try {
			Mockito.when(clientFactory.create(Mockito.any(),  Mockito.any())).thenReturn(client);
		} catch (Exception e) {
			e.printStackTrace();
		}

		TestManagerImpl testManager =  new TestManagerImpl(clientFactory, logManager, dataManager, simulationManager);
		testManager.start();
//		String path = "./applications/python/SampleTestConfig.json";
		String path = "./test/gov/pnnl/goss/gridappsd/SampleTestConfig.json";
//		/Users/jsimpson/git/adms/GOSS-GridAPPS-D/gov.pnnl.goss.gridappsd/applications/python/exampleTestConfig.json
//		TestConfiguration testConfig = testManager.loadTestConfig(path);
//		System.out.println(testConfig.getFeeder_name());
//		assertEquals(testConfig.getFeeder_name(),"ieee8500");
	}

	@Test
	public void testLoadScript(){
		try {
			Mockito.when(clientFactory.create(Mockito.any(),  Mockito.any())).thenReturn(client);
		} catch (Exception e) {
			e.printStackTrace();
		}

		TestManagerImpl testManager = new TestManagerImpl(clientFactory, logManager, dataManager, simulationManager);
		testManager.start();
//		String path = "./applications/python/exampleTestScript.json";
		String path = "./test/gov/pnnl/goss/gridappsd/exampleTestScript.json";
//		TestScript testScript = testManager.loadTestScript(path);
////		FailureEvent event = testScript.getEvents().get(0);
////		assertNotNull(event);
//
//		assertEquals(testScript.name,"sample_app");
	}

	@Test
	public void testRequest(){
//		String testCfg = "{\"testConfigPath\":\"./applications/python/exampleTestConfig.json\",\"testScriptPath\":\"./test/gov/pnnl/goss/gridappsd/exampleTestScript.json\"}";
//		RequestTest.parse(testCfg);
	}
	 
	@Test
	public void testRequestJson(){
//		String testCfg = "{\"testConfigPath\":\"./applications/python/exampleTestConfig.json\",\"testScriptPath\":\"./test/gov/pnnl/goss/gridappsd/exampleTestScript.json\"}";
//		String testCfg = "{\"rulePort\": 5000, \"testConfig\": {\"initial_conditions\": {}, \"logging\": \"true\", \"region_name\": \"ieee8500_Region\", \"subregion_name\": \"ieee8500_SubRegion\", \"line_name\": \"ieee8500\", \"duration\": 60, \"run_start\": \"2017-07-21 12:00:00\", \"simulation_configuration\": \"ieee8500\", \"power_system_configuration\": \"ieee8500\", \"default_values\": {}, \"logging_options\": {\"log\": \"true\"}, \"run_end\": \"2017-07-22 12:00:00\"}, \"expectedResultObject\": {\"expected_outputs\": {\"1\": {\"simulation_id\": \"559402036\", \"message\": {\"timestamp\": 1535574872, \"measurements\": [{\"measurement_mrid\": \"_1b154d1b-4e84-467a-b510-71c0b5d0e962\", \"value\": 2}]}}, \"0\": {\"simulation_id\": \"559402036\", \"message\": {\"timestamp\": 1535574871, \"measurements\": [{\"measurement_mrid\": \"_1b154d1b-4e84-467a-b510-71c0b5d0e962\", \"value\": 2}, {\"magnitude\": 7539.054218993357, \"angle\": 89.37059072239458, \"measurement_mrid\": \"_a4d99da6-9d5a-449c-b9f3-e02c0caaf94d\"}]}}}}, \"testScript\": {\"log\": {\"name\": \"string\", \"location\": \"ref_location1234\"}, \"rules\": [{\"topic\": \"input\", \"name\": \"app_rules.py\", \"port\": 5000}], \"outputs\": {\"capacitor_list\": [\"cap_capbank0a\", \"cap_capbank0b\", \"cap_capbank0c\", \"cap_capbank1a\", \"cap_capbank1b\", \"cap_capbank1c\", \"cap_capbank2a\", \"cap_capbank2b\", \"cap_capbank2c\", \"cap_capbank3\"], \"substation_link\": [\"xf_hvmv_sub\"], \"regulator_list\": [\"reg_FEEDER_REG\", \"reg_VREG2\", \"reg_VREG3\", \"reg_VREG4\"], \"voltage_measurements\": [\"nd_l2955047\", \"nd_l3160107\", \"nd_l2673313\", \"nd_l2876814\", \"nd_m1047574\", \"nd_l3254238\"], \"regulator_configuration_list\": [\"rcon_FEEDER_REG\", \"rcon_VREG2\", \"rcon_VREG3\", \"rcon_VREG4\"]}, \"test_configuration\": \"./SampleTestConfig.json\", \"application\": \"sample_app\", \"events\": [{\"phases\": \"ABC\", \"rLineToLine\": 0.5, \"timeInitiated\": \"2017-07-21 12:00:00\", \"kind\": \"lineToGround\", \"equipmentMRID\": \"12345\", \"rGround\": 0.5, \"xGround\": 0.5, \"faultMRID\": \"1234\", \"xLineToLine\": 0.5, \"timeCleared\": \"2017-07-22 12:00:00\"}], \"name\": \"sample_app\"}, \"topic\": \"input\", \"simulationID\": 38882743}";
		String testCfg = "{\"rulePort\": 5000, \"testConfig\": {\"initial_conditions\": {}, \"logging\": \"true\", \"region_name\": \"ieee8500_Region\", \"subregion_name\": \"ieee8500_SubRegion\", \"line_name\": \"ieee8500\", \"duration\": 60, \"run_start\": \"2017-07-21 12:00:00\", \"simulation_configuration\": \"ieee8500\", \"power_system_configuration\": \"ieee8500\", \"default_values\": {}, \"logging_options\": {\"log\": \"true\"}, \"run_end\": \"2017-07-22 12:00:00\"}, \"expectedResultObject\": {\"expected_results\": {\"1\": {\"simulation_id\": \"559402036\", \"message\": {\"timestamp\": 1535574872, \"measurements\": [{\"measurement_mrid\": \"_1b154d1b-4e84-467a-b510-71c0b5d0e962\", \"value\": 2}]}}, \"0\": {\"simulation_id\": \"559402036\", \"message\": {\"timestamp\": 1535574871, \"measurements\": [{\"measurement_mrid\": \"_1b154d1b-4e84-467a-b510-71c0b5d0e962\", \"value\": 2}, {\"magnitude\": 7539.054218993357, \"angle\": 89.37059072239458, \"measurement_mrid\": \"_a4d99da6-9d5a-449c-b9f3-e02c0caaf94d\"}]}}}}, \"testScript\": {\"log\": {\"name\": \"string\", \"location\": \"ref_location1234\"}, \"rules\": [{\"topic\": \"input\", \"name\": \"app_rules.py\", \"port\": 5000}], \"outputs\": {\"capacitor_list\": [\"cap_capbank0a\", \"cap_capbank0b\", \"cap_capbank0c\", \"cap_capbank1a\", \"cap_capbank1b\", \"cap_capbank1c\", \"cap_capbank2a\", \"cap_capbank2b\", \"cap_capbank2c\", \"cap_capbank3\"], \"substation_link\": [\"xf_hvmv_sub\"], \"regulator_list\": [\"reg_FEEDER_REG\", \"reg_VREG2\", \"reg_VREG3\", \"reg_VREG4\"], \"voltage_measurements\": [\"nd_l2955047\", \"nd_l3160107\", \"nd_l2673313\", \"nd_l2876814\", \"nd_m1047574\", \"nd_l3254238\"], \"regulator_configuration_list\": [\"rcon_FEEDER_REG\", \"rcon_VREG2\", \"rcon_VREG3\", \"rcon_VREG4\"]}, \"test_configuration\": \"./SampleTestConfig.json\", \"application\": \"sample_app\", \"events\": [{\"phases\": \"ABC\", \"rLineToLine\": 0.5, \"timeInitiated\": \"1248156005\", \"kind\": \"lineToGround\", \"equipmentMRID\": \"12345\", \"rGround\": 0.5, \"xGround\": 0.5, \"faultMRID\": \"1234\", \"xLineToLine\": 0.5, \"timeCleared\": \"1248156008\"}], \"name\": \"sample_app\"}, \"topic\": \"input\", \"simulationID\": 38882743}";
//		RequestTest reqTest = RequestTest.parse(testCfg);
//		JsonObject expectedResultObject = reqTest.getExpectedResultObject();
		CompareResults compareResults = new CompareResults();
//		String message = "{\"output\": \"{\\\"simulation_id\\\": \\\"2003982953\\\", \\\"message\\\": {\\\"timestamp\\\": \\\"2018-05-07 17:02:42.807081\\\", \\\"measurements\\\": [{\\\"magnitude\\\": 158.30425951917206, \\\"angle\\\": -31.204732405109397, \\\"measurement_mrid\\\": \\\"280a2f7d-192e-4b54-a8b9-5e0a6b51f278\\\"}   ]}}\", \"command\": \"nextTimeStep\", \"timestamp\": 938638}";
		String message = "{\"output\": \"{\\\"simulation_id\\\": \\\"2003982953\\\", \\\"message\\\": {\\\"timestamp\\\": \\\"2018-05-07 17:02:42.807081\\\", \\\"measurements\\\": [{\\\"magnitude\\\": 158.30425951917206, \\\"angle\\\": -31.204732405109397, \\\"measurement_mrid\\\": \\\"280a2f7d-192e-4b54-a8b9-5e0a6b51f278\\\"}, {\\\"measurement_mrid\\\": \\\"_1b154d1b-4e84-467a-b510-71c0b5d0e962\\\", \\\"value\\\": 2}   ]}}\", \"command\": \"nextTimeStep\", \"timestamp\": 938638}";
		
		JsonObject jsonObject = CompareResults.getSimulationJson(message.toString());
		JsonObject simOutputObject  = CompareResults.getSimulationJson(jsonObject.get("output").getAsString());
//		TestResults tr = compareResults.compareExpectedWithSimulationOutput("0",
//				simOutputObject.getAsJsonObject(), expectedResultObject);
//		System.out.println(tr.getNumberOfConflicts());
	}

	@Test
	public void compare(){
//		((TestManagerImpl) tm).compare();
		JsonParser parser = new JsonParser();
		JsonElement o1 = parser.parse("{a : {a : 2}, b : 2}");
		JsonElement o2 = parser.parse("{b : 3, a : {a : 2}}");
		JsonElement o3 = parser.parse("{b : 2, a : {a : 2}}");
		System.out.println(o1.equals(o2));
		System.out.println(o1.equals(o3));
//		Assert.assertEquals(o1, o2);
	}
	
	@Test
	public void proven(){
		try {
			Mockito.when(clientFactory.create(Mockito.any(),  Mockito.any())).thenReturn(client);
		} catch (Exception e) {
			e.printStackTrace();
		}
//		{"queryMeasurement": "simulation",
//			"queryFilter": {"simulation_id": "582881157"},
//			"responseFormat": "JSON"}
		
//		GldNode gg = new GldNode("7");
//		gg.bSolarInverters = true;

//		ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
//		dataManager.start();
		DataManagerImpl dataM = new DataManagerImpl();
		ConfigurationManagerImpl configManager = new ConfigurationManagerImpl(logManager, dataManager);
		configManager.start();
		Response r = null;
		String request1 = "{\"queryMeasurement\": \"simulation\", \"queryFilter\": {\"simulation_id\": \"145774843\"}, \"responseFormat\": \"JSON\"}";
		try {
			r = dataM.processDataRequest(request1, "timeseries", 1598820656, "/tmp/gridappsd_tmp", TestConstants.SYSTEM_USER_NAME);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.out.println(r);
//		requestTimeSEriesData = {"queryMeasurement":"simulation","queryFilter":{"simulation_id":"145774843"},"responseFormat":"JSON","queryType":"time-series","simulationYear":0}

//		System.out.println(configManager.getConfigurationProperty(GridAppsDConstants.FNCS_PATH));

		ProvenTimeSeriesDataManagerImpl provenTimeSeriesDataManager = new ProvenTimeSeriesDataManagerImpl();

		RequestTimeseriesData request = new RequestTimeseriesData(); 
		HashMap<String,Object> queryFilter = new HashMap <String,Object>();
		queryFilter.put("hasSimulationId", "145774843");
		request.setQueryMeasurement("simulation");
		request.setQueryFilter(queryFilter);
//		request.setSimulationId("1278337149");
		String responseStr = null;
		try {
			ProvenProducer provenProducer = new ProvenProducer();
			// http://proven:8080/hybrid/rest/v1/repository/provenMessage
//			String provenUri = "http://proven:8080/hybrid/rest/v1/repository/provenMessage";
			String provenUri = "http://localhost:18080/hybrid/rest/v1/repository/provenMessage";
			provenProducer.restProducer(provenUri, null, null);
			provenProducer.setMessageInfo("GridAPPSD", "QUERY", this.getClass().getSimpleName(), null);
//			gov.pnnl.proven.message.ProvenMessage pm;
//			ProvenResponse response = provenQueryProducer.sendMessage(requestTimeseriesData.toString(), requestId);
			ProvenResponse response = provenProducer.sendMessage("{\"queryMeasurement\": \"simulation\", \"queryFilter\": {\"hasSimulationId\": \"182942650\"},\"responseFormat\": \"JSON\"}", "22");
			responseStr = provenTimeSeriesDataManager.query(request).toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(responseStr);
		assertNotNull(responseStr);
	}
	
	@Test
	public void testResponse(){
		HistoricalComparison hc = new HistoricalComparison(dataManager, "system");
		TestResultSeries testResultSeries = new TestResultSeries();
		CompareResults compareResults = new CompareResults();
		String expected = "{\"output\":{\"1248156002\":{\"simulation_id\":\"559402036\",\"message\":{\"timestamp\":1535574871,\"measurements\":[{\"angle\":-122.66883087158203,\"magnitude\":2438.561767578125,\"measurement_mrid\":\"_84541f26-084d-4ea7-a254-ea43678d51f9\"},{\"angle\":21.723935891052907,\"magnitude\":45368.78524042436,\"measurement_mrid\":\"_c48d8d88-12be-4b15-8b44-eedc752250c6\"},{\"measurement_mrid\":\"_6e033599-c62a-4821-8ddf-68ba11be60a2\",\"value\":1}]}},\"1248156005\":{\"simulation_id\":\"559402036\",\"message\":{\"timestamp\":1535574872,\"measurements\":[{\"angle\":-38.381605233862224,\"magnitude\":52769.16136465681,\"measurement_mrid\":\"_84541f26-084d-4ea7-a254-ea43678d51f9\"},{\"angle\":21.723935891052907,\"magnitude\":45368.78524042436,\"measurement_mrid\":\"_c48d8d88-12be-4b15-8b44-eedc752250c6\"},{\"measurement_mrid\":\"_6e033599-c62a-4821-8ddf-68ba11be60a2\",\"value\":0}]}}}}";
		JsonObject expectedJson = CompareResults.getSimulationJson(expected);
		
		String res = "[{\"hasSimulationMessageType\":\"OUTPUT\",\"measurement_mrid\":\"_84541f26-084d-4ea7-a254-ea43678d51f9\",\"angle\":-4.066423674487563,\"magnitude\":2361.0733024639117,\"simulation_id\":\"1961648576\",\"time\":1248156002},{\"hasSimulationMessageType\":\"OUTPUT\",\"measurement_mrid\":\"_fff9a11e-d5d1-4824-a457-13d944ffcfdf\",\"angle\":-122.80107769837849,\"magnitude\":2520.2169329056983,\"simulation_id\":\"1961648576\",\"time\":1248156002},{\"hasSimulationMessageType\":\"OUTPUT\",\"measurement_mrid\":\"_0058123f-da11-4f7c-a429-e47e5949465f\",\"angle\":-122.70461031091335,\"magnitude\":2522.818525429715,\"simulation_id\":\"1961648576\",\"time\":1248156002},{\"hasMeasurementDifference\":\"FORWARD\",\"hasSimulationMessageType\":\"INPUT\",\"difference_mrid\":\"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\",\"simulation_id\":\"1961648576\",\"time\":1587670650,\"attribute\":\"ShuntCompensator.sections\",\"value\":0.0,\"object\":\"_307E4291-5FEA-4388-B2E0-2B3D22FE8183\"}]";
		res = "{\"hasSimulationMessageType\":\"OUTPUT\",\"measurement_mrid\":\"_0055de94-7d7e-4931-a884-cab596cc191b\",\"angle\":-4.066423674487563,\"magnitude\":2361.0733024639117,\"simulation_id\":\"1961648576\",\"time\":1248156002}";
		res = "{\"data\":\"[{\\\"hasSimulationMessageType\\\":\\\"OUTPUT\\\"}]\" }" ; 
		res = "{\"data\":\"[{\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_84541f26-084d-4ea7-a254-ea43678d51f9\\\", \\\"angle\\\": -4.066423674487563, \\\"magnitude\\\": 2361.0733024639117, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_fff9a11e-d5d1-4824-a457-13d944ffcfdf\\\", \\\"angle\\\": -122.80107769837849, \\\"magnitude\\\": 2520.2169329056983, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_0058123f-da11-4f7c-a429-e47e5949465f\\\", \\\"angle\\\": -122.70461031091335, \\\"magnitude\\\": 2522.818525429715, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}]\"}";
		JsonObject jsonObject = CompareResults.getSimulationJson(res);
		String data = jsonObject.get("data").getAsString();
		System.out.println(data.substring(0, 100));
//		JsonObject meas = CompareResults.getSimulationJson(data);
		JsonParser parser = new JsonParser();
		JsonArray measurements = (JsonArray) parser.parse(data);

		
		hc.processWithAllTimes(expectedJson, "123", res);
		
		JsonArray meas_array = new JsonArray();
		JsonObject expectedObject = new JsonObject();
		JsonObject simOutputObject = new JsonObject();

		for (JsonElement measurement : measurements) {
			String time = measurement.getAsJsonObject().get("time").getAsString();
			
			if (measurement.getAsJsonObject().get("hasSimulationMessageType").getAsString().equals("OUTPUT") ){
				if (! simOutputObject.has(time)){
					JsonObject measurementsObject = new JsonObject();
					JsonObject messageObject = new JsonObject();
					measurementsObject.add("measurements", new JsonArray());
					messageObject.add("message", measurementsObject);
//					JsonObject measurementsObject = hc.buildOutputObject("123", simOutputObject, time,  new JsonArray());
					simOutputObject.add(time, messageObject);
				} 
				simOutputObject.get(time).getAsJsonObject().get("message").getAsJsonObject().get("measurements").getAsJsonArray().add(measurement);
			}
			
			System.out.println(measurement.getAsJsonObject().get("time"));
			// Remove unneeded proven metadata
			measurement.getAsJsonObject().remove("hasSimulationMessageType");
			measurement.getAsJsonObject().remove("simulation_id");
			measurement.getAsJsonObject().remove("time");
			meas_array.add(measurement);
		}
//		JsonObject outputObject = hc.buildOutputObject("123", simOutputObject, time, measurements);
		expectedObject.add("output", simOutputObject);
		System.out.println(expectedObject.toString());
		int index = 0;
		for (Entry<String, JsonElement> time_entry : simOutputObject.entrySet()) {
			System.out.println(time_entry.getValue());
			System.out.println(time_entry.getKey());
//			String time = time_entry.get("time").getAsString()
//			JsonObject outputObject = hc.buildOutputObject(simulationId, simOutputObject, time_entry.getKey(), time_entry.getValue().getAsJsonArray());
//			System.out.println(simOutputObject.toString());
			TestResults tr = compareResults.compareExpectedWithSimulationOutput(time_entry.getKey(), time_entry.getValue().getAsJsonObject(), expectedJson);
			if (tr != null) {
				testResultSeries.add(index+"", tr);
			}
			index++;
		}
		System.out.println("Index: " + index + " TestManager number of conflicts: "+ " total " + testResultSeries.getTotal());
		
//		{\"hasMeasurementDifference\":\"FORWARD\",\"hasSimulationMessageType\":\"INPUT\",\"difference_mrid\":\"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\",\"simulation_id\":\"1961648576\",\"time\":1587670650,\"attribute\":\"ShuntCompensator.sections\",\"value\":0.0,\"object\":\"_307E4291-5FEA-4388-B2E0-2B3D22FE8183\"}]"


	}

}
