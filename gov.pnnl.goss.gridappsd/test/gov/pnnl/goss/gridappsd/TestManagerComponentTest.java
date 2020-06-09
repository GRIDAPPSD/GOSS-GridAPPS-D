package gov.pnnl.goss.gridappsd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gov.pnnl.goss.gridappsd.api.AppManager;
import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.api.LogDataManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.api.TestManager;
import gov.pnnl.goss.gridappsd.configuration.ConfigurationManagerImpl;
import gov.pnnl.goss.gridappsd.data.DataManagerImpl;
import gov.pnnl.goss.gridappsd.data.ProvenTimeSeriesDataManagerImpl;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.RequestTimeseriesData;
import gov.pnnl.goss.gridappsd.dto.TestConfig;
//import gov.pnnl.goss.gridappsd.dto.TestConfiguration;
//import gov.pnnl.goss.gridappsd.dto.TestScript;
import gov.pnnl.goss.gridappsd.testmanager.CompareResults;
import gov.pnnl.goss.gridappsd.testmanager.HistoricalComparison;
import gov.pnnl.goss.gridappsd.testmanager.TestManagerImpl;
import gov.pnnl.goss.gridappsd.testmanager.TestResultSeries;
import gov.pnnl.goss.gridappsd.testmanager.TestResults;
import gov.pnnl.proven.api.producer.ProvenProducer;
import gov.pnnl.proven.api.producer.ProvenResponse;
import pnnl.goss.core.Client;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.Response;

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
	

	@Mock
	LogDataManager logDataManager;

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

		TestManagerImpl testManager = new TestManagerImpl(clientFactory, logManager, logDataManager, dataManager, simulationManager);
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

		TestManagerImpl testManager =  new TestManagerImpl(clientFactory, logManager, logDataManager, dataManager, simulationManager);
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

		TestManagerImpl testManager = new TestManagerImpl(clientFactory, logManager, logDataManager, dataManager, simulationManager);
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
			r = dataM.processDataRequest(request1, "timeseries", 1598820656, "/tmp/gridappsd_tmp", "system");
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
	public void testExpectedVersusResponse(){
		HistoricalComparison hc = new HistoricalComparison(dataManager, "system");
		TestResultSeries testResultSeries = new TestResultSeries();
		CompareResults compareResults = new CompareResults();
		String expected = "{\"output\":{\"1248156002\":{\"simulation_id\":\"559402036\",\"message\":{\"timestamp\":1535574871,\"measurements\":[{\"angle\":-122.66883087158203,\"magnitude\":2438.561767578125,\"measurement_mrid\":\"_84541f26-084d-4ea7-a254-ea43678d51f9\"},{\"angle\":21.723935891052907,\"magnitude\":45368.78524042436,\"measurement_mrid\":\"_c48d8d88-12be-4b15-8b44-eedc752250c6\"},{\"measurement_mrid\":\"_6e033599-c62a-4821-8ddf-68ba11be60a2\",\"value\":1}]}},\"1248156005\":{\"simulation_id\":\"559402036\",\"message\":{\"timestamp\":1535574872,\"measurements\":[{\"angle\":-38.381605233862224,\"magnitude\":52769.16136465681,\"measurement_mrid\":\"_84541f26-084d-4ea7-a254-ea43678d51f9\"},{\"angle\":21.723935891052907,\"magnitude\":45368.78524042436,\"measurement_mrid\":\"_c48d8d88-12be-4b15-8b44-eedc752250c6\"},{\"measurement_mrid\":\"_6e033599-c62a-4821-8ddf-68ba11be60a2\",\"value\":0}]}}}}";
		       expected = "{\"expectedResults\": {\"output\": {\"1248156002\": {\"simulation_id\": \"559402036\", \"message\": {\"timestamp\": 1535574871, \"measurements\": [{\"angle\": -122.66883087158203, \"magnitude\": 2438.561767578125, \"measurement_mrid\": \"_84541f26-084d-4ea7-a254-ea43678d51f9\"}, {\"angle\": 21.723935891052907, \"magnitude\": 45368.78524042436, \"measurement_mrid\": \"_c48d8d88-12be-4b15-8b44-eedc752250c6\"}, {\"measurement_mrid\": \"_6e033599-c62a-4821-8ddf-68ba11be60a2\", \"value\": 1}]}}, \"1248156005\": {\"simulation_id\": \"559402036\", \"message\": {\"timestamp\": 1535574872, \"measurements\": [{\"angle\": -38.381605233862224, \"magnitude\": 52769.16136465681, \"measurement_mrid\": \"_84541f26-084d-4ea7-a254-ea43678d51f9\"}, {\"angle\": 21.723935891052907, \"magnitude\": 45368.78524042436, \"measurement_mrid\": \"_c48d8d88-12be-4b15-8b44-eedc752250c6\"}, {\"measurement_mrid\": \"_6e033599-c62a-4821-8ddf-68ba11be60a2\", \"value\": 0}]}}}, \"input\": {\"1248156002\": {\"simulation_id\": \"559402036\", \"message\": {\"timestamp\": 1535574871, \"measurements\": [{\"hasMeasurementDifference\": \"FORWARD\", \"difference_mrid\": \"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\", \"simulation_id\": \"1961648576\", \"time\": 1248156002, \"attribute\": \"ShuntCompensator.sections\", \"value\": 0.0, \"object\": \"_232DD3A8-9A3C-4053-B972-8A5EB49FD980\"}, {\"hasMeasurementDifference\": \"REVERSE\", \"difference_mrid\": \"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\", \"simulation_id\": \"1961648576\", \"time\": 1248156002, \"attribute\": \"ShuntCompensator.sections\", \"value\": 1.0, \"object\": \"_EEC4FD4B-9214-442C-BA83-C91B8EFD06CB\"}]}}}}}";		
		String expected_proven = "{\"appId\": \"sample_app\", \"expectedResults\": {\"output\": {\"1248156002\": {\"simulation_id\": \"559402036\", \"message\": {\"timestamp\": 1535574871, \"measurements\": [{\"measurement_mrid\": \"_0055de94-7d7e-4931-a884-cab596cc191b\", \"angle\": -4.066423674487563, \"magnitude\": 2361.0733024639117, \"simulation_id\": \"1961648576\", \"time\": 1248156002}, {\"measurement_mrid\": \"_fff9a11e-d5d1-4824-a457-13d944ffcfdf\", \"angle\": -122.80107769837849, \"magnitude\": 2520.2169329056983, \"simulation_id\": \"1961648576\", \"time\": 1248156002}, {\"measurement_mrid\": \"_0058123f-da11-4f7c-a429-e47e5949465f\", \"angle\": -122.70461031091335, \"magnitude\": 2522.818525429715, \"simulation_id\": \"1961648576\", \"time\": 1248156002}]}}}, \"input\": {\"1248156002\": {\"simulation_id\": \"559402036\", \"message\": {\"timestamp\": 1535574871, \"measurements\": [{\"hasMeasurementDifference\": \"FORWARD\", \"difference_mrid\": \"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\", \"simulation_id\": \"1961648576\", \"time\": 1587670650, \"attribute\": \"ShuntCompensator.sections\", \"value\": 0.0, \"object\": \"_232DD3A8-9A3C-4053-B972-8A5EB49FD980\"}, {\"hasMeasurementDifference\": \"REVERSE\", \"difference_mrid\": \"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\", \"simulation_id\": \"1961648576\", \"time\": 1587670650, \"attribute\": \"ShuntCompensator.sections\", \"value\": 1.0, \"object\": \"_EEC4FD4B-9214-442C-BA83-C91B8EFD06CB\"}]}}}}}";
		JsonObject expected_series = CompareResults.getSimulationJson(expected_proven).get("expectedResults").getAsJsonObject();
		
		String res = "[{\"hasSimulationMessageType\":\"OUTPUT\",\"measurement_mrid\":\"_84541f26-084d-4ea7-a254-ea43678d51f9\",\"angle\":-4.066423674487563,\"magnitude\":2361.0733024639117,\"simulation_id\":\"1961648576\",\"time\":1248156002},{\"hasSimulationMessageType\":\"OUTPUT\",\"measurement_mrid\":\"_fff9a11e-d5d1-4824-a457-13d944ffcfdf\",\"angle\":-122.80107769837849,\"magnitude\":2520.2169329056983,\"simulation_id\":\"1961648576\",\"time\":1248156002},{\"hasSimulationMessageType\":\"OUTPUT\",\"measurement_mrid\":\"_0058123f-da11-4f7c-a429-e47e5949465f\",\"angle\":-122.70461031091335,\"magnitude\":2522.818525429715,\"simulation_id\":\"1961648576\",\"time\":1248156002},{\"hasMeasurementDifference\":\"FORWARD\",\"hasSimulationMessageType\":\"INPUT\",\"difference_mrid\":\"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\",\"simulation_id\":\"1961648576\",\"time\":1248156002,\"attribute\":\"ShuntCompensator.sections\",\"value\":0.0,\"object\":\"_307E4291-5FEA-4388-B2E0-2B3D22FE8183\"}]";
		res = "{\"hasSimulationMessageType\":\"OUTPUT\",\"measurement_mrid\":\"_0055de94-7d7e-4931-a884-cab596cc191b\",\"angle\":-4.066423674487563,\"magnitude\":2361.0733024639117,\"simulation_id\":\"1961648576\",\"time\":1248156002}";
		res = "{\"data\":\"[{\\\"hasSimulationMessageType\\\":\\\"OUTPUT\\\"}]\" }" ; 
		res = "{\"data\":\"[{\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_84541f26-084d-4ea7-a254-ea43678d51f9\\\", \\\"angle\\\": -4.066423674487563, \\\"magnitude\\\": 2361.0733024639117, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_fff9a11e-d5d1-4824-a457-13d944ffcfdf\\\", \\\"angle\\\": -122.80107769837849, \\\"magnitude\\\": 2520.2169329056983, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_0058123f-da11-4f7c-a429-e47e5949465f\\\", \\\"angle\\\": -122.70461031091335, \\\"magnitude\\\": 2522.818525429715, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}]\"}";
		res = "{\"data\":\"[{\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_0055de94-7d7e-4931-a884-cab596cc191b\\\", \\\"angle\\\": -2.066423674487563, \\\"magnitude\\\": 2361.0733024639117, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_fff9a11e-d5d1-4824-a457-13d944ffcfdf\\\", \\\"angle\\\": -122.80107769837849, \\\"magnitude\\\": 2520.2169329056983, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_0058123f-da11-4f7c-a429-e47e5949465f\\\", \\\"angle\\\": -122.70461031091335, \\\"magnitude\\\": 2522.818525429715, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasMeasurementDifference\\\": \\\"FORWARD\\\", \\\"hasSimulationMessageType\\\": \\\"INPUT\\\", \\\"difference_mrid\\\": \\\"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\\\", \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002, \\\"attribute\\\": \\\"ShuntCompensator.sections\\\", \\\"value\\\": 0.0, \\\"object\\\": \\\"_232DD3A8-9A3C-4053-B972-8A5EB49FD980\\\"}, {\\\"hasMeasurementDifference\\\": \\\"REVERSE\\\", \\\"hasSimulationMessageType\\\": \\\"INPUT\\\", \\\"difference_mrid\\\": \\\"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\\\", \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002, \\\"attribute\\\": \\\"ShuntCompensator.sections\\\", \\\"value\\\": 1.0, \\\"object\\\": \\\"_EEC4FD4B-9214-442C-BA83-C91B8EFD06CB\\\"}]\"}";
		res = "{\"data\":\"[{\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_0055de94-7d7e-4931-a884-cab596cc191b\\\", \\\"angle\\\": -2.066423674487563, \\\"magnitude\\\": 2361.0733024639117, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_fff9a11e-d5d1-4824-a457-13d944ffcfdf\\\", \\\"angle\\\": -122.80107769837849, \\\"magnitude\\\": 2520.2169329056983, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_0058123f-da11-4f7c-a429-e47e5949465f\\\", \\\"angle\\\": -122.70461031091335, \\\"magnitude\\\": 2522.818525429715, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasMeasurementDifference\\\": \\\"FORWARD\\\", \\\"hasSimulationMessageType\\\": \\\"INPUT\\\", \\\"difference_mrid\\\": \\\"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\\\", \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1587670650, \\\"attribute\\\": \\\"ShuntCompensator.sections\\\", \\\"value\\\": 0.0, \\\"object\\\": \\\"_232DD3A8-9A3C-4053-B972-8A5EB49FD980\\\"}, {\\\"hasMeasurementDifference\\\": \\\"REVERSE\\\", \\\"hasSimulationMessageType\\\": \\\"INPUT\\\", \\\"difference_mrid\\\": \\\"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\\\", \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1587670650, \\\"attribute\\\": \\\"ShuntCompensator.sections\\\", \\\"value\\\": 1.0, \\\"object\\\": \\\"_EEC4FD4B-9214-442C-BA83-C91B8EFD06CB\\\"}]\"}";

		JsonObject jsonObject = CompareResults.getSimulationJson(res); 
		String data = jsonObject.get("data").getAsString();
		System.out.println(data.substring(0, 100));
		JsonParser parser = new JsonParser();
		JsonArray measurements = (JsonArray) parser.parse(data);
		
		JsonObject expectedObject = hc.buildExpectedFromTimeseries(measurements);
		JsonObject simOutputObject = expectedObject.get("output").getAsJsonObject();

		hc.processWithAllTimes(expected_series, "123", res);
		
		JsonObject expected_output_series = expected_series.get("output").getAsJsonObject();
		int index = 0;
		for (Entry<String, JsonElement> time_entry : simOutputObject.entrySet()) {
			TestResults tr = compareResults.compareExpectedWithSimulationOutput(time_entry.getKey(), time_entry.getValue().getAsJsonObject(), expected_output_series);
			if (tr != null) {
				testResultSeries.add(time_entry.getKey(),time_entry.getKey(), tr);
			}
			index++;
		}
		System.out.println("testExpectedVersusResponse");
		testResultSeries.ppprint();
		System.out.println("Index: " + index + " TestManager number of conflicts: "+ " total " + testResultSeries.getTotal());
		
		JsonObject simInputObject = expectedObject.get("input").getAsJsonObject();
		JsonObject expected_input_series = expected_series.get("input").getAsJsonObject();
		index = 0;
		for (Entry<String, JsonElement> time_entry : simInputObject.entrySet()) {
			System.out.println(time_entry);
			TestResults tr = compareResults.compareExpectedWithSimulationInput(time_entry.getKey(), time_entry.getKey(), time_entry.getValue().getAsJsonObject(), expected_input_series);
			if (tr != null) {
				tr.pprint();
				testResultSeries.add(time_entry.getKey(), time_entry.getKey(), tr);
			}
			index++;
		}
		testResultSeries.ppprint();
		System.out.println("Index: " + index + " TestManager number of conflicts: "+ " total " + testResultSeries.getTotal());
		
//		{\"hasMeasurementDifference\":\"FORWARD\",\"hasSimulationMessageType\":\"INPUT\",\"difference_mrid\":\"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\",\"simulation_id\":\"1961648576\",\"time\":1587670650,\"attribute\":\"ShuntCompensator.sections\",\"value\":0.0,\"object\":\"_307E4291-5FEA-4388-B2E0-2B3D22FE8183\"}]"
	}
	
	@Test
	public void testExpectedVersusResponse2(){
		HistoricalComparison hc = new HistoricalComparison(dataManager, "system");
		TestResultSeries testResultSeries = new TestResultSeries();
		CompareResults compareResults = new CompareResults();
		String expected = "{\"expectedResults\": {\"output\": {\"1248156002\": {\"simulation_id\": \"559402036\", \"message\": {\"timestamp\": 1535574871, \"measurements\": [{\"measurement_mrid\": \"_0055de94-7d7e-4931-a884-cab596cc191b\", \"angle\": -5.066423674487563, \"magnitude\": 2361.0733024639117, \"simulation_id\": \"1961648576\", \"time\": 1248156002}, {\"measurement_mrid\": \"_fff9a11e-d5d1-4824-a457-13d944ffcfdf\", \"angle\": -122.80107769837849, \"magnitude\": 2520.2169329056983, \"simulation_id\": \"1961648576\", \"time\": 1248156002}, {\"measurement_mrid\": \"_0058123f-da11-4f7c-a429-e47e5949465f\", \"angle\": -122.70461031091335, \"magnitude\": 2522.818525429715, \"simulation_id\": \"1961648576\", \"time\": 1248156002}]}}}, \"input\": {\"0\": {\"simulation_id\": \"559402036\", \"message\": {\"timestamp\": 1587670650, \"measurements\": [{\"hasMeasurementDifference\": \"FORWARD\", \"difference_mrid\": \"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\", \"simulation_id\": \"1961648576\", \"time\": 1587670650, \"attribute\": \"ShuntCompensator.sections\", \"value\": 1.0, \"object\": \"_307E4291-5FEA-4388-B2E0-2B3D22FE8183\"}, {\"hasMeasurementDifference\": \"REVERSE\", \"difference_mrid\": \"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\", \"simulation_id\": \"1961648576\", \"time\": 1587670650, \"attribute\": \"ShuntCompensator.sections\", \"value\": 0.0, \"object\": \"_307E4291-5FEA-4388-B2E0-2B3D22FE8183\"}]}}, \"15\": {\"simulation_id\": \"559402036\", \"message\": {\"timestamp\": 1587670665, \"measurements\": [{\"hasMeasurementDifference\": \"FORWARD\", \"difference_mrid\": \"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\", \"simulation_id\": \"1961648576\", \"time\": 1587670665, \"attribute\": \"ShuntCompensator.sections\", \"value\": 0.0, \"object\": \"_307E4291-5FEA-4388-B2E0-2B3D22FE8183\"}, {\"hasMeasurementDifference\": \"REVERSE\", \"difference_mrid\": \"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\", \"simulation_id\": \"1961648576\", \"time\": 1587670665, \"attribute\": \"ShuntCompensator.sections\", \"value\": 1.0, \"object\": \"_307E4291-5FEA-4388-B2E0-2B3D22FE8183\"}]}}}}}";		
		String expected_proven = "{\"appId\": \"sample_app\", \"expectedResults\": {\"output\": {\"1248156002\": {\"simulation_id\": \"559402036\", \"message\": {\"timestamp\": 1535574871, \"measurements\": [{\"measurement_mrid\": \"_0055de94-7d7e-4931-a884-cab596cc191b\", \"angle\": -4.066423674487563, \"magnitude\": 2361.0733024639117, \"simulation_id\": \"1961648576\", \"time\": 1248156002}, {\"measurement_mrid\": \"_fff9a11e-d5d1-4824-a457-13d944ffcfdf\", \"angle\": -122.80107769837849, \"magnitude\": 2520.2169329056983, \"simulation_id\": \"1961648576\", \"time\": 1248156002}, {\"measurement_mrid\": \"_0058123f-da11-4f7c-a429-e47e5949465f\", \"angle\": -122.70461031091335, \"magnitude\": 2522.818525429715, \"simulation_id\": \"1961648576\", \"time\": 1248156002}]}}}, \"input\": {\"1248156002\": {\"simulation_id\": \"559402036\", \"message\": {\"timestamp\": 1535574871, \"measurements\": [{\"hasMeasurementDifference\": \"FORWARD\", \"difference_mrid\": \"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\", \"simulation_id\": \"1961648576\", \"time\": 1587670650, \"attribute\": \"ShuntCompensator.sections\", \"value\": 0.0, \"object\": \"_232DD3A8-9A3C-4053-B972-8A5EB49FD980\"}, {\"hasMeasurementDifference\": \"REVERSE\", \"difference_mrid\": \"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\", \"simulation_id\": \"1961648576\", \"time\": 1587670650, \"attribute\": \"ShuntCompensator.sections\", \"value\": 1.0, \"object\": \"_EEC4FD4B-9214-442C-BA83-C91B8EFD06CB\"}]}}}}}";
		expected_proven = "{\"expectedResults\": {\"output\": {\"1248156002\": {\"simulation_id\": \"559402036\", \"message\": {\"timestamp\": 1535574871, \"measurements\": [{\"measurement_mrid\": \"_0055de94-7d7e-4931-a884-cab596cc191b\", \"angle\": -5.066423674487563, \"magnitude\": 2361.0733024639117, \"simulation_id\": \"1961648576\", \"time\": 1248156002}, {\"measurement_mrid\": \"_fff9a11e-d5d1-4824-a457-13d944ffcfdf\", \"angle\": -122.80107769837849, \"magnitude\": 2520.2169329056983, \"simulation_id\": \"1961648576\", \"time\": 1248156002}, {\"measurement_mrid\": \"_0058123f-da11-4f7c-a429-e47e5949465f\", \"angle\": -122.70461031091335, \"magnitude\": 2522.818525429715, \"simulation_id\": \"1961648576\", \"time\": 1248156002}]}}}, \"input\": {\"0\": {\"simulation_id\": \"559402036\", \"message\": {\"timestamp\": 1587670650, \"measurements\": [{\"hasMeasurementDifference\": \"FORWARD\", \"difference_mrid\": \"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\", \"simulation_id\": \"1961648576\", \"time\": 1587670650, \"attribute\": \"ShuntCompensator.sections\", \"value\": 1.0, \"object\": \"_307E4291-5FEA-4388-B2E0-2B3D22FE8183\"}, {\"hasMeasurementDifference\": \"REVERSE\", \"difference_mrid\": \"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\", \"simulation_id\": \"1961648576\", \"time\": 1587670650, \"attribute\": \"ShuntCompensator.sections\", \"value\": 0.0, \"object\": \"_307E4291-5FEA-4388-B2E0-2B3D22FE8183\"}]}}, \"15\": {\"simulation_id\": \"559402036\", \"message\": {\"timestamp\": 1587670665, \"measurements\": [{\"hasMeasurementDifference\": \"FORWARD\", \"difference_mrid\": \"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\", \"simulation_id\": \"1961648576\", \"time\": 1587670665, \"attribute\": \"ShuntCompensator.sections\", \"value\": 0.0, \"object\": \"_307E4291-5FEA-4388-B2E0-2B3D22FE8183\"}, {\"hasMeasurementDifference\": \"REVERSE\", \"difference_mrid\": \"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\", \"simulation_id\": \"1961648576\", \"time\": 1587670665, \"attribute\": \"ShuntCompensator.sections\", \"value\": 1.0, \"object\": \"_307E4291-5FEA-4388-B2E0-2B3D22FE8183\"}]}}}}}";
		JsonObject expected_series = CompareResults.getSimulationJson(expected_proven).get("expectedResults").getAsJsonObject();
		
		String res = "{\"data\":\"[{\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_0055de94-7d7e-4931-a884-cab596cc191b\\\", \\\"angle\\\": -2.066423674487563, \\\"magnitude\\\": 2361.0733024639117, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_fff9a11e-d5d1-4824-a457-13d944ffcfdf\\\", \\\"angle\\\": -122.80107769837849, \\\"magnitude\\\": 2520.2169329056983, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_0058123f-da11-4f7c-a429-e47e5949465f\\\", \\\"angle\\\": -122.70461031091335, \\\"magnitude\\\": 2522.818525429715, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasMeasurementDifference\\\": \\\"FORWARD\\\", \\\"hasSimulationMessageType\\\": \\\"INPUT\\\", \\\"difference_mrid\\\": \\\"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\\\", \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1587670650, \\\"attribute\\\": \\\"ShuntCompensator.sections\\\", \\\"value\\\": 0.0, \\\"object\\\": \\\"_307E4291-5FEA-4388-B2E0-2B3D22FE8183\\\"}, {\\\"hasMeasurementDifference\\\": \\\"REVERSE\\\", \\\"hasSimulationMessageType\\\": \\\"INPUT\\\", \\\"difference_mrid\\\": \\\"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\\\", \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1587670650, \\\"attribute\\\": \\\"ShuntCompensator.sections\\\", \\\"value\\\": 1.0, \\\"object\\\": \\\"_307E4291-5FEA-4388-B2E0-2B3D22FE8183\\\"}, {\\\"hasMeasurementDifference\\\": \\\"FORWARD\\\", \\\"hasSimulationMessageType\\\": \\\"INPUT\\\", \\\"difference_mrid\\\": \\\"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\\\", \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1587670665, \\\"attribute\\\": \\\"ShuntCompensator.sections\\\", \\\"value\\\": 1.0, \\\"object\\\": \\\"_307E4291-5FEA-4388-B2E0-2B3D22FE8183\\\"}, {\\\"hasMeasurementDifference\\\": \\\"REVERSE\\\", \\\"hasSimulationMessageType\\\": \\\"INPUT\\\", \\\"difference_mrid\\\": \\\"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\\\", \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1587670665, \\\"attribute\\\": \\\"ShuntCompensator.sections\\\", \\\"value\\\": 0.0, \\\"object\\\": \\\"_307E4291-5FEA-4388-B2E0-2B3D22FE8183\\\"}]\"}";

		JsonObject jsonObject = CompareResults.getSimulationJson(res); 
		String data = jsonObject.get("data").getAsString();
		System.out.println(data.substring(0, 100));
		JsonParser parser = new JsonParser(); 
		JsonArray measurements = (JsonArray) parser.parse(data);

		TestResultSeries testResultSeries1 = hc.processWithAllTimes(expected_series, "123", res);
		testResultSeries1.ppprint();
		assertEquals(testResultSeries1.getTotal(), 5);

	}	
	
	@Test
	public void testKeys(){
		HistoricalComparison hc = new HistoricalComparison(dataManager, "system");
		SortedSet<Integer> inputKeys1 = new TreeSet<>();
		SortedSet<Integer> inputKeys2 = new TreeSet<>();
		HashMap<Integer,Integer> newKeys1 = new HashMap<Integer,Integer>();
		HashMap<Integer,Integer> newKeys2 = new HashMap<Integer,Integer>();
//		[1590527327, 1590527342, 1590527357]
//				[0, 15, 27, 30]
//		inputKeys1.add(1590612488); inputKeys1.add(1590612503); inputKeys1.add(1590612518);
//		inputKeys2.add(1590616253); inputKeys2.add(1590616268); inputKeys2.add(1590616283);
		inputKeys1.add(1590527327); inputKeys1.add(1590527342); inputKeys1.add(1590527357);
		inputKeys2.add(0); inputKeys2.add(15); inputKeys2.add(27); inputKeys2.add(30);
		
		System.out.println("input keys");
		System.out.println(inputKeys1.toString());
		System.out.println(inputKeys2.toString());
		HashMap<Integer,Integer> x = hc.getTimeMap(inputKeys1, inputKeys2);
		System.out.println(x);
		
		inputKeys1.add(1590527327); inputKeys1.add(1590527342); inputKeys1.add(1590527357); inputKeys1.add(1590527360);
		inputKeys2.add(0); inputKeys2.add(15); inputKeys2.add(27); inputKeys2.add(30); inputKeys2.add(33);
	
		System.out.println("input keys");
		System.out.println(inputKeys1.toString());
		System.out.println(inputKeys2.toString());
		x = hc.getTimeMap(inputKeys1, inputKeys2);
		System.out.println(x);
		
		 
		Integer first1 = inputKeys1.first();
		Integer first2 = inputKeys2.first();
		
		int diff = 0;
		Integer baseTime = first2;

		Iterator<Integer> it1 = inputKeys1.iterator();
		Iterator<Integer> it2 = inputKeys2.iterator();

//						newKeys1.put(first1, first2);
		while (it2.hasNext()) {
//			Integer key1 = it1.next();
//			if it2.
			Integer key2 = it2.next();
			diff = key2-first2;
			first1+=diff;
			newKeys1.put(first1, key2);
			first2 = key2;
		}
		System.out.println(newKeys1);
	}
	
	@Test
	public void testInputCount(){
		TestManagerImpl testManager = new TestManagerImpl(clientFactory, logManager, logDataManager, dataManager, simulationManager);
		
		SortedSet<Integer> inputKeys2 = new TreeSet<>();
		inputKeys2.add(0); inputKeys2.add(15); inputKeys2.add(27); inputKeys2.add(30);
		int first1 = 1590773528;
		int simulationTimestamp = 1590773558;
		int inputCount=0; 
		
		inputCount = testManager.getNextCount(inputKeys2, simulationTimestamp, first1, inputCount);
		System.out.println(inputCount);
		
	}

	@Test
	public void testResponse2(){
		TestManagerImpl testManager = new TestManagerImpl(clientFactory, logManager, logDataManager, dataManager, simulationManager);
		
		HistoricalComparison hc = new HistoricalComparison(dataManager, "system");
		String res1 = "{\"data\":\"[{\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_84541f26-084d-4ea7-a254-ea43678d51f9\\\", \\\"angle\\\": -4.066423674487563, \\\"magnitude\\\": 2361.0733024639117, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_fff9a11e-d5d1-4824-a457-13d944ffcfdf\\\", \\\"angle\\\": -122.80107769837849, \\\"magnitude\\\": 2520.2169329056983, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_0058123f-da11-4f7c-a429-e47e5949465f\\\", \\\"angle\\\": -122.70461031091335, \\\"magnitude\\\": 2522.818525429715, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasMeasurementDifference\\\": \\\"FORWARD\\\", \\\"hasSimulationMessageType\\\": \\\"INPUT\\\", \\\"difference_mrid\\\": \\\"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\\\", \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002, \\\"attribute\\\": \\\"ShuntCompensator.sections\\\", \\\"value\\\": 0.0, \\\"object\\\": \\\"_232DD3A8-9A3C-4053-B972-8A5EB49FD980\\\"}, {\\\"hasMeasurementDifference\\\": \\\"REVERSE\\\", \\\"hasSimulationMessageType\\\": \\\"INPUT\\\", \\\"difference_mrid\\\": \\\"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\\\", \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002, \\\"attribute\\\": \\\"ShuntCompensator.sections\\\", \\\"value\\\": 1.0, \\\"object\\\": \\\"_EEC4FD4B-9214-442C-BA83-C91B8EFD06CB\\\"}]\"}";
		String res2 = "{\"data\":\"[{\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_84541f26-084d-4ea7-a254-ea43678d51f9\\\", \\\"angle\\\": -5.066423674487563, \\\"magnitude\\\": 2361.0733024639117, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_fff9a11e-d5d1-4824-a457-13d944ffcfdf\\\", \\\"angle\\\": -122.80107769837849, \\\"magnitude\\\": 2520.2169329056983, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_0058123f-da11-4f7c-a429-e47e5949465f\\\", \\\"angle\\\": -122.70461031091335, \\\"magnitude\\\": 2522.818525429715, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasMeasurementDifference\\\": \\\"FORWARD\\\", \\\"hasSimulationMessageType\\\": \\\"INPUT\\\", \\\"difference_mrid\\\": \\\"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\\\", \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002, \\\"attribute\\\": \\\"ShuntCompensator.sections\\\", \\\"value\\\": 1.0, \\\"object\\\": \\\"_232DD3A8-9A3C-4053-B972-8A5EB49FD980\\\"}, {\\\"hasMeasurementDifference\\\": \\\"REVERSE\\\", \\\"hasSimulationMessageType\\\": \\\"INPUT\\\", \\\"difference_mrid\\\": \\\"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\\\", \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002, \\\"attribute\\\": \\\"ShuntCompensator.sections\\\", \\\"value\\\": 0.0, \\\"object\\\": \\\"_EEC4FD4B-9214-442C-BA83-C91B8EFD06CB\\\"}]\"}";

		TestResultSeries testResultSeries1 = hc.processWithAllTimes("123", res1,res1);
		assertEquals(testResultSeries1.getTotal(), 0);
		
		TestResultSeries testResultSeries2 = hc.processWithAllTimes("123", res1, res2);
		assertEquals(testResultSeries2.getTotal(), 3);
		testResultSeries2.ppprint();
				
		Gson gson = new Gson();
		System.out.println(testResultSeries2.toJson(false));

//		testManager.storeResults("appID","testID", "currentSimulationIdOne", "currentSimulationIdTwo", testResultSeries2);
	}
	
	@Test
	public void inputMapTest(){
		String data = "{\"simulation_id\": \"1432157818\", \"message\": {\"timestamp\": 0, \"difference_mrid\": \"d5f5516a-87a9-43d3-9dba-931b4388eabc\", \"reverse_differences\": [{\"object\": \"_232DD3A8-9A3C-4053-B972-8A5EB49FD980\", \"attribute\": \"ShuntCompensator.sections\", \"value\": 1}, {\"object\": \"_9A74DCDC-EA5A-476B-9B99-B4FB90DC37E3\", \"attribute\": \"ShuntCompensator.sections\", \"value\": 1}, {\"object\": \"_EEC4FD4B-9214-442C-BA83-C91B8EFD06CB\", \"attribute\": \"ShuntCompensator.sections\", \"value\": 1}, {\"object\": \"_307E4291-5FEA-4388-B2E0-2B3D22FE8183\", \"attribute\": \"ShuntCompensator.sections\", \"value\": 1}], \"forward_differences\": [{\"object\": \"_232DD3A8-9A3C-4053-B972-8A5EB49FD980\", \"attribute\": \"ShuntCompensator.sections\", \"value\": 0}, {\"object\": \"_9A74DCDC-EA5A-476B-9B99-B4FB90DC37E3\", \"attribute\": \"ShuntCompensator.sections\", \"value\": 0}, {\"object\": \"_EEC4FD4B-9214-442C-BA83-C91B8EFD06CB\", \"attribute\": \"ShuntCompensator.sections\", \"value\": 0}, {\"object\": \"_307E4291-5FEA-4388-B2E0-2B3D22FE8183\", \"attribute\": \"ShuntCompensator.sections\", \"value\": 0}]}}";
		JsonParser parser = new JsonParser();
		JsonObject measurements = (JsonObject) parser.parse(data);
		Map<String, JsonElement> forwardDifferenceMap = new HashMap<String,JsonElement>();
		JsonObject tempObj = measurements.getAsJsonObject("message");

		JsonArray temp = tempObj.getAsJsonArray("forward_differences");
		for (JsonElement jsonElement : temp) {
			jsonElement.getAsJsonObject().add("difference_mrid",tempObj.get("difference_mrid"));
			jsonElement.getAsJsonObject().add("hasMeasurementDifference",parser.parse("FORWARD"));
			forwardDifferenceMap.put(jsonElement.getAsJsonObject().get("object").getAsString(), jsonElement);
		}
	}
	
	@Test
	public void testConfig(){
		String config=  "{\"testId\": \"123\",\"appId\": \"sample app\" }";
		TestConfig tc = TestConfig.parse(config);
		System.out.println(tc.toString());
		
		
	}
	
}
