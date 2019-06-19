package gov.pnnl.goss.gridappsd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

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
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.RequestTestUpdate;
import gov.pnnl.goss.gridappsd.dto.RequestTimeseriesData;
import gov.pnnl.goss.gridappsd.dto.RequestTimeseriesData.RequestType;
//import gov.pnnl.goss.gridappsd.dto.TestConfiguration;
//import gov.pnnl.goss.gridappsd.dto.TestScript;
import gov.pnnl.goss.gridappsd.testmanager.CompareResults;
import gov.pnnl.goss.gridappsd.testmanager.TestManagerImpl;
import gov.pnnl.goss.gridappsd.testmanager.TestResults;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import gov.pnnl.proven.api.producer.ProvenProducer;
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
	private DataManager dataManager;

	@Mock
	DataManager DataManager;

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

	public void proven(){
		try {
			Mockito.when(clientFactory.create(Mockito.any(),  Mockito.any())).thenReturn(client);
		} catch (Exception e) {
			e.printStackTrace();
		}
//		GldNode gg = new GldNode("7");
//		gg.bSolarInverters = true;

//		ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
		ConfigurationManagerImpl configManager = new ConfigurationManagerImpl(logManager, dataManager);
		configManager.start();

		System.out.println(configManager.getConfigurationProperty(GridAppsDConstants.FNCS_PATH));

//		ProvenTimeSeriesDataManagerImpl provenTimeSeriesDataManager = new ProvenTimeSeriesDataManagerImpl();

		RequestTimeseriesData request = new RequestTimeseriesData();
		HashMap<String,String> queryFilter = new HashMap <String,String>();
		queryFilter.put("hasSimulationId", "182942650");
		request.setQueryMeasurement(RequestType.PROVEN_MEASUREMENT);
		request.setQueryFilter(queryFilter);
//		request.setSimulationId("1278337149");
		String responseStr = null;
		try {
			ProvenProducer provenProducer = new ProvenProducer();
			String provenUri = "http://localhost:18080/hybrid/rest/v1/repository/provenMessage";
			provenProducer.restProducer(provenUri, null, null);
			provenProducer.setMessageInfo("GridAPPSD", "QUERY", this.getClass().getSimpleName(), null);
//			gov.pnnl.proven.message.ProvenMessage pm;
//			ProvenResponse response = provenProducer.sendMessage("{\"queryMeasurement\": \"PROVEN_MEASUREMENT\", \"queryFilter\": {\"hasSimulationId\": \"182942650\"},\"responseFormat\": \"JSON\"}", 22);
//			responseStr = provenTimeSeriesDataManager.query(request).toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(responseStr);
//		assertNotNull(responseStr);
	}

}
