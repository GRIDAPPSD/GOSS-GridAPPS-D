package gov.pnnl.goss.gridappsd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

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
//			ProvenResponse response = provenProducer.sendMessage("{\"queryMeasurement\": \"simulation\", \"queryFilter\": {\"hasSimulationId\": \"182942650\"},\"responseFormat\": \"JSON\"}", "22");
			responseStr = provenTimeSeriesDataManager.query(request).toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(responseStr);
		assertNotNull(responseStr);
	}
	
	@Test
	public void testExpectedVersusResponse(){
		HistoricalComparison hc = new HistoricalComparison(dataManager, "system", client);
		TestResultSeries testResultSeries = new TestResultSeries();
		CompareResults compareResults = new CompareResults(client, "123");
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

		TestResultSeries testResultSeries1 =hc.processWithAllTimes(expected_series, "123", res);
		assertEquals(testResultSeries1.getTotal(), 1);
		
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
		HistoricalComparison hc = new HistoricalComparison(dataManager, "system", client);
		String expected_proven = "{\"appId\": \"sample_app\", \"expectedResults\": {\"output\": {\"1248156002\": {\"simulation_id\": \"559402036\", \"message\": {\"timestamp\": 1535574871, \"measurements\": [{\"measurement_mrid\": \"_0055de94-7d7e-4931-a884-cab596cc191b\", \"angle\": -4.066423674487563, \"magnitude\": 2361.0733024639117, \"simulation_id\": \"1961648576\", \"time\": 1248156002}, {\"measurement_mrid\": \"_fff9a11e-d5d1-4824-a457-13d944ffcfdf\", \"angle\": -122.80107769837849, \"magnitude\": 2520.2169329056983, \"simulation_id\": \"1961648576\", \"time\": 1248156002}, {\"measurement_mrid\": \"_0058123f-da11-4f7c-a429-e47e5949465f\", \"angle\": -122.70461031091335, \"magnitude\": 2522.818525429715, \"simulation_id\": \"1961648576\", \"time\": 1248156002}]}}}, \"input\": {\"1248156002\": {\"simulation_id\": \"559402036\", \"message\": {\"timestamp\": 1535574871, \"measurements\": [{\"hasMeasurementDifference\": \"FORWARD\", \"difference_mrid\": \"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\", \"simulation_id\": \"1961648576\", \"time\": 1587670650, \"attribute\": \"ShuntCompensator.sections\", \"value\": 0.0, \"object\": \"_232DD3A8-9A3C-4053-B972-8A5EB49FD980\"}, {\"hasMeasurementDifference\": \"REVERSE\", \"difference_mrid\": \"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\", \"simulation_id\": \"1961648576\", \"time\": 1587670650, \"attribute\": \"ShuntCompensator.sections\", \"value\": 1.0, \"object\": \"_EEC4FD4B-9214-442C-BA83-C91B8EFD06CB\"}]}}}}}";
		expected_proven = "{\"expectedResults\": {\"output\": {\"1248156002\": {\"simulation_id\": \"559402036\", \"message\": {\"timestamp\": 1535574871, \"measurements\": [{\"measurement_mrid\": \"_0055de94-7d7e-4931-a884-cab596cc191b\", \"angle\": -5.066423674487563, \"magnitude\": 2361.0733024639117, \"simulation_id\": \"1961648576\", \"time\": 1248156002}, {\"measurement_mrid\": \"_fff9a11e-d5d1-4824-a457-13d944ffcfdf\", \"angle\": -122.80107769837849, \"magnitude\": 2520.2169329056983, \"simulation_id\": \"1961648576\", \"time\": 1248156002}, {\"measurement_mrid\": \"_0058123f-da11-4f7c-a429-e47e5949465f\", \"angle\": -122.70461031091335, \"magnitude\": 2522.818525429715, \"simulation_id\": \"1961648576\", \"time\": 1248156002}]}}}, \"input\": {\"0\": {\"simulation_id\": \"559402036\", \"message\": {\"timestamp\": 1587670650, \"measurements\": [{\"hasMeasurementDifference\": \"FORWARD\", \"difference_mrid\": \"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\", \"simulation_id\": \"1961648576\", \"time\": 1587670650, \"attribute\": \"ShuntCompensator.sections\", \"value\": 1.0, \"object\": \"_307E4291-5FEA-4388-B2E0-2B3D22FE8183\"}, {\"hasMeasurementDifference\": \"REVERSE\", \"difference_mrid\": \"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\", \"simulation_id\": \"1961648576\", \"time\": 1587670650, \"attribute\": \"ShuntCompensator.sections\", \"value\": 0.0, \"object\": \"_307E4291-5FEA-4388-B2E0-2B3D22FE8183\"}]}}, \"15\": {\"simulation_id\": \"559402036\", \"message\": {\"timestamp\": 1587670665, \"measurements\": [{\"hasMeasurementDifference\": \"FORWARD\", \"difference_mrid\": \"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\", \"simulation_id\": \"1961648576\", \"time\": 1587670665, \"attribute\": \"ShuntCompensator.sections\", \"value\": 0.0, \"object\": \"_307E4291-5FEA-4388-B2E0-2B3D22FE8183\"}, {\"hasMeasurementDifference\": \"REVERSE\", \"difference_mrid\": \"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\", \"simulation_id\": \"1961648576\", \"time\": 1587670665, \"attribute\": \"ShuntCompensator.sections\", \"value\": 1.0, \"object\": \"_307E4291-5FEA-4388-B2E0-2B3D22FE8183\"}]}}}}}";
		JsonObject expected_series = CompareResults.getSimulationJson(expected_proven).get("expectedResults").getAsJsonObject();
		
		String res = "{\"data\":\"[{\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_0055de94-7d7e-4931-a884-cab596cc191b\\\", \\\"angle\\\": -2.066423674487563, \\\"magnitude\\\": 2361.0733024639117, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_fff9a11e-d5d1-4824-a457-13d944ffcfdf\\\", \\\"angle\\\": -122.80107769837849, \\\"magnitude\\\": 2520.2169329056983, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_0058123f-da11-4f7c-a429-e47e5949465f\\\", \\\"angle\\\": -122.70461031091335, \\\"magnitude\\\": 2522.818525429715, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasMeasurementDifference\\\": \\\"FORWARD\\\", \\\"hasSimulationMessageType\\\": \\\"INPUT\\\", \\\"difference_mrid\\\": \\\"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\\\", \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1587670650, \\\"attribute\\\": \\\"ShuntCompensator.sections\\\", \\\"value\\\": 0.0, \\\"object\\\": \\\"_307E4291-5FEA-4388-B2E0-2B3D22FE8183\\\"}, {\\\"hasMeasurementDifference\\\": \\\"REVERSE\\\", \\\"hasSimulationMessageType\\\": \\\"INPUT\\\", \\\"difference_mrid\\\": \\\"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\\\", \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1587670650, \\\"attribute\\\": \\\"ShuntCompensator.sections\\\", \\\"value\\\": 1.0, \\\"object\\\": \\\"_307E4291-5FEA-4388-B2E0-2B3D22FE8183\\\"}, {\\\"hasMeasurementDifference\\\": \\\"FORWARD\\\", \\\"hasSimulationMessageType\\\": \\\"INPUT\\\", \\\"difference_mrid\\\": \\\"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\\\", \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1587670665, \\\"attribute\\\": \\\"ShuntCompensator.sections\\\", \\\"value\\\": 1.0, \\\"object\\\": \\\"_307E4291-5FEA-4388-B2E0-2B3D22FE8183\\\"}, {\\\"hasMeasurementDifference\\\": \\\"REVERSE\\\", \\\"hasSimulationMessageType\\\": \\\"INPUT\\\", \\\"difference_mrid\\\": \\\"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\\\", \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1587670665, \\\"attribute\\\": \\\"ShuntCompensator.sections\\\", \\\"value\\\": 0.0, \\\"object\\\": \\\"_307E4291-5FEA-4388-B2E0-2B3D22FE8183\\\"}]\"}";

		JsonObject jsonObject = CompareResults.getSimulationJson(res); 
		String data = jsonObject.get("data").getAsString();
		System.out.println(data.substring(0, 100));

		TestResultSeries testResultSeries1 = hc.processWithAllTimes(expected_series, "123", res);
		testResultSeries1.ppprint();
		assertEquals(testResultSeries1.getTotal(), 5);
	}	
	
	@Test
	public void testExpectedVersusResponse3(){
		// Check for empty values
		HistoricalComparison hc = new HistoricalComparison(dataManager, "system",client);

		String expected = "{\"expectedResults\": {}}";
		JsonObject expected_series = CompareResults.getSimulationJson(expected).get("expectedResults").getAsJsonObject();
		
		String res = "{\"data\":\"[{\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_0055de94-7d7e-4931-a884-cab596cc191b\\\", \\\"angle\\\": -2.066423674487563, \\\"magnitude\\\": 2361.0733024639117, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_fff9a11e-d5d1-4824-a457-13d944ffcfdf\\\", \\\"angle\\\": -122.80107769837849, \\\"magnitude\\\": 2520.2169329056983, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_0058123f-da11-4f7c-a429-e47e5949465f\\\", \\\"angle\\\": -122.70461031091335, \\\"magnitude\\\": 2522.818525429715, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasMeasurementDifference\\\": \\\"FORWARD\\\", \\\"hasSimulationMessageType\\\": \\\"INPUT\\\", \\\"difference_mrid\\\": \\\"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\\\", \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1587670650, \\\"attribute\\\": \\\"ShuntCompensator.sections\\\", \\\"value\\\": 0.0, \\\"object\\\": \\\"_307E4291-5FEA-4388-B2E0-2B3D22FE8183\\\"}, {\\\"hasMeasurementDifference\\\": \\\"REVERSE\\\", \\\"hasSimulationMessageType\\\": \\\"INPUT\\\", \\\"difference_mrid\\\": \\\"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\\\", \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1587670650, \\\"attribute\\\": \\\"ShuntCompensator.sections\\\", \\\"value\\\": 1.0, \\\"object\\\": \\\"_307E4291-5FEA-4388-B2E0-2B3D22FE8183\\\"}, {\\\"hasMeasurementDifference\\\": \\\"FORWARD\\\", \\\"hasSimulationMessageType\\\": \\\"INPUT\\\", \\\"difference_mrid\\\": \\\"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\\\", \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1587670665, \\\"attribute\\\": \\\"ShuntCompensator.sections\\\", \\\"value\\\": 1.0, \\\"object\\\": \\\"_307E4291-5FEA-4388-B2E0-2B3D22FE8183\\\"}, {\\\"hasMeasurementDifference\\\": \\\"REVERSE\\\", \\\"hasSimulationMessageType\\\": \\\"INPUT\\\", \\\"difference_mrid\\\": \\\"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\\\", \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1587670665, \\\"attribute\\\": \\\"ShuntCompensator.sections\\\", \\\"value\\\": 0.0, \\\"object\\\": \\\"_307E4291-5FEA-4388-B2E0-2B3D22FE8183\\\"}]\"}";

		JsonObject jsonObject = CompareResults.getSimulationJson(res); 
		String data = jsonObject.get("data").getAsString();
		System.out.println(data.substring(0, 100));

		TestResultSeries testResultSeries1 = hc.processWithAllTimes(expected_series, "123", res);
		testResultSeries1.ppprint();
		assertEquals(testResultSeries1.getTotal(), 0);
	}	
	
	@Test
	public void testTimeMap(){
		HistoricalComparison hc = new HistoricalComparison(dataManager, "system",client);
		
		SortedSet<Integer> inputKeys1 = new TreeSet<>();
		SortedSet<Integer> inputKeys2 = new TreeSet<>();
		HashMap<Integer,Integer> newKeys1 = new HashMap<Integer,Integer>();

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

		Iterator<Integer> it2 = inputKeys2.iterator();

		while (it2.hasNext()) {
			Integer key2 = it2.next();
			diff = key2-first2;
			first1+=diff;
			newKeys1.put(first1, key2);
			first2 = key2;
		}
		System.out.println(newKeys1);
//		java.lang.AssertionError: expected:<{1590527357=30, 1590527327=0, 1590527342=15, 1590527354=27, 1590527360=33}> 

		assertEquals(newKeys1.get(1590527354).intValue(), 27);
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
		assertEquals(inputCount, 3);
	}

	@Test
	public void testTwoSimulationResponse(){	
		HistoricalComparison hc = new HistoricalComparison(dataManager, "system", client);
		String res1 = "{\"data\":\"[{\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_84541f26-084d-4ea7-a254-ea43678d51f9\\\", \\\"angle\\\": -4.066423674487563, \\\"magnitude\\\": 2361.0733024639117, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_fff9a11e-d5d1-4824-a457-13d944ffcfdf\\\", \\\"angle\\\": -122.80107769837849, \\\"magnitude\\\": 2520.2169329056983, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_0058123f-da11-4f7c-a429-e47e5949465f\\\", \\\"angle\\\": -122.70461031091335, \\\"magnitude\\\": 2522.818525429715, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasMeasurementDifference\\\": \\\"FORWARD\\\", \\\"hasSimulationMessageType\\\": \\\"INPUT\\\", \\\"difference_mrid\\\": \\\"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\\\", \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002, \\\"attribute\\\": \\\"ShuntCompensator.sections\\\", \\\"value\\\": 0.0, \\\"object\\\": \\\"_232DD3A8-9A3C-4053-B972-8A5EB49FD980\\\"}, {\\\"hasMeasurementDifference\\\": \\\"REVERSE\\\", \\\"hasSimulationMessageType\\\": \\\"INPUT\\\", \\\"difference_mrid\\\": \\\"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\\\", \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002, \\\"attribute\\\": \\\"ShuntCompensator.sections\\\", \\\"value\\\": 1.0, \\\"object\\\": \\\"_EEC4FD4B-9214-442C-BA83-C91B8EFD06CB\\\"}]\"}";
		String res2 = "{\"data\":\"[{\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_84541f26-084d-4ea7-a254-ea43678d51f9\\\", \\\"angle\\\": -5.066423674487563, \\\"magnitude\\\": 2361.0733024639117, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_fff9a11e-d5d1-4824-a457-13d944ffcfdf\\\", \\\"angle\\\": -122.80107769837849, \\\"magnitude\\\": 2520.2169329056983, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_0058123f-da11-4f7c-a429-e47e5949465f\\\", \\\"angle\\\": -122.70461031091335, \\\"magnitude\\\": 2522.818525429715, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasMeasurementDifference\\\": \\\"FORWARD\\\", \\\"hasSimulationMessageType\\\": \\\"INPUT\\\", \\\"difference_mrid\\\": \\\"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\\\", \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002, \\\"attribute\\\": \\\"ShuntCompensator.sections\\\", \\\"value\\\": 1.0, \\\"object\\\": \\\"_232DD3A8-9A3C-4053-B972-8A5EB49FD980\\\"}, {\\\"hasMeasurementDifference\\\": \\\"REVERSE\\\", \\\"hasSimulationMessageType\\\": \\\"INPUT\\\", \\\"difference_mrid\\\": \\\"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\\\", \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002, \\\"attribute\\\": \\\"ShuntCompensator.sections\\\", \\\"value\\\": 0.0, \\\"object\\\": \\\"_EEC4FD4B-9214-442C-BA83-C91B8EFD06CB\\\"}]\"}";

		TestResultSeries testResultSeries1 = hc.processWithAllTimes("123", "345", res1,res1);
		assertEquals(testResultSeries1.getTotal(), 0);
		
		TestResultSeries testResultSeries2 = hc.processWithAllTimes("123","345", res1, res2);
		assertEquals(testResultSeries2.getTotal(), 3);
		System.out.println(testResultSeries2.toJson(false));

//		testManager.storeResults("appID","testID", "currentSimulationIdOne", "currentSimulationIdTwo", testResultSeries2);
	}
	
	@Test
	public void testTwoSimulationResponseNoInput(){	
		HistoricalComparison hc = new HistoricalComparison(dataManager, "system", client);
		String res1 = "{\"data\":\"[{\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_84541f26-084d-4ea7-a254-ea43678d51f9\\\", \\\"angle\\\": -4.066423674487563, \\\"magnitude\\\": 2361.0733024639117, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_fff9a11e-d5d1-4824-a457-13d944ffcfdf\\\", \\\"angle\\\": -122.80107769837849, \\\"magnitude\\\": 2520.2169329056983, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_0058123f-da11-4f7c-a429-e47e5949465f\\\", \\\"angle\\\": -122.70461031091335, \\\"magnitude\\\": 2522.818525429715, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasMeasurementDifference\\\": \\\"FORWARD\\\", \\\"hasSimulationMessageType\\\": \\\"INPUT\\\", \\\"difference_mrid\\\": \\\"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\\\", \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002, \\\"attribute\\\": \\\"ShuntCompensator.sections\\\", \\\"value\\\": 0.0, \\\"object\\\": \\\"_232DD3A8-9A3C-4053-B972-8A5EB49FD980\\\"}, {\\\"hasMeasurementDifference\\\": \\\"REVERSE\\\", \\\"hasSimulationMessageType\\\": \\\"INPUT\\\", \\\"difference_mrid\\\": \\\"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4\\\", \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002, \\\"attribute\\\": \\\"ShuntCompensator.sections\\\", \\\"value\\\": 1.0, \\\"object\\\": \\\"_EEC4FD4B-9214-442C-BA83-C91B8EFD06CB\\\"}]\"}";
		String res2 = "{\"data\":\"[{\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_84541f26-084d-4ea7-a254-ea43678d51f9\\\", \\\"angle\\\": -5.066423674487563, \\\"magnitude\\\": 2361.0733024639117, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_fff9a11e-d5d1-4824-a457-13d944ffcfdf\\\", \\\"angle\\\": -122.80107769837849, \\\"magnitude\\\": 2520.2169329056983, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}, {\\\"hasSimulationMessageType\\\": \\\"OUTPUT\\\", \\\"measurement_mrid\\\": \\\"_0058123f-da11-4f7c-a429-e47e5949465f\\\", \\\"angle\\\": -122.70461031091335, \\\"magnitude\\\": 2522.818525429715, \\\"simulation_id\\\": \\\"1961648576\\\", \\\"time\\\": 1248156002}]\"}";
		
		TestResultSeries testResultSeries2 = hc.processWithAllTimes("123","345", res1, res2);
		assertEquals(testResultSeries2.getTotal(), 1);
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
		assertEquals(forwardDifferenceMap.get("_EEC4FD4B-9214-442C-BA83-C91B8EFD06CB").getAsJsonObject().get("value").getAsInt(),0);
//		java.lang.AssertionError: expected:<{_EEC4FD4B-9214-442C-BA83-C91B8EFD06CB={"object":"_EEC4FD4B-9214-442C-BA83-C91B8EFD06CB","attribute":"ShuntCompensator.sections","value":0,"difference_mrid":"d5f5516a-87a9-43d3-9dba-931b4388eabc","hasMeasurementDifference":"FORWARD"}, _9A74DCDC-EA5A-476B-9B99-B4FB90DC37E3={"object":"_9A74DCDC-EA5A-476B-9B99-B4FB90DC37E3","attribute":"ShuntCompensator.sections","value":0,"difference_mrid":"d5f5516a-87a9-43d3-9dba-931b4388eabc","hasMeasurementDifference":"FORWARD"}, _232DD3A8-9A3C-4053-B972-8A5EB49FD980={"object":"_232DD3A8-9A3C-4053-B972-8A5EB49FD980","attribute":"ShuntCompensator.sections","value":0,"difference_mrid":"d5f5516a-87a9-43d3-9dba-931b4388eabc","hasMeasurementDifference":"FORWARD"}, _307E4291-5FEA-4388-B2E0-2B3D22FE8183={"object":"_307E4291-5FEA-4388-B2E0-2B3D22FE8183","attribute":"ShuntCompensator.sections","value":0,"difference_mrid":"d5f5516a-87a9-43d3-9dba-931b4388eabc","hasMeasurementDifference":"FORWARD"}}> but was:<0>
		
	}
	
	@Test
	public void testConfig(){
		String config=  "{\"testId\": \"12333\",\"appId\": \"sample_app\" }";
//		config=  "{\"appId\": \"sample_app\" }";
		TestConfig tc = TestConfig.parse(config);
		System.out.println(tc.toString());
		assertEquals(tc.getAppId(), "sample_app");
		assertEquals(tc.getTestId(), "12333");
	}
	
}
