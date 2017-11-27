package gov.pnnl.goss.gridappsd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.api.StatusReporter;
import gov.pnnl.goss.gridappsd.api.TestManager;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.RequestTest;
import gov.pnnl.goss.gridappsd.dto.TestConfiguration;
import gov.pnnl.goss.gridappsd.dto.TestScript;
import gov.pnnl.goss.gridappsd.testmanager.TestManagerImpl;
import gov.pnnl.goss.gridappsd.testmanager.TestManagerQueryFactory;
import pnnl.goss.core.Client;
import pnnl.goss.core.ClientFactory;

@RunWith(MockitoJUnitRunner.class)
public class TestManagerComponentTest {
	
	TestManager tm = new TestManagerImpl();
	
	@Mock
	ClientFactory clientFactory;
	
	@Mock
	Client client;
	
	@Mock
	ConfigurationManager configurationManager;
	
	@Mock
	SimulationManager simulationManager;
	
	@Mock 
	StatusReporter statusReporter;
	
	@Mock
	LogManager logManager;
	
	@Captor
	ArgumentCaptor<String> argCaptor;
	
	@Captor
	ArgumentCaptor<LogMessage> argCaptorLogMessage;
	
	/**
	 *    Succeeds when info log message is called at the start of the test manager implementation with the expected message
	 */
	@Test
	public void infoCalledWhen_processManagerStarted(){
		
//		ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
		try {
			Mockito.when(clientFactory.create(Mockito.any(),  Mockito.any())).thenReturn(client);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		TestManagerImpl testManager = new TestManagerImpl(clientFactory, 
											configurationManager, simulationManager, 
											statusReporter,logManager);
		testManager.start();
		
		Mockito.verify(logManager).log(argCaptorLogMessage.capture());
		
		LogMessage logMessage = argCaptorLogMessage.getAllValues().get(0);

		assertEquals(logMessage.getLog_level(), LogLevel.DEBUG);
		assertEquals(logMessage.getLog_message(), "Starting "+TestManagerImpl.class.getName());
		assertEquals(logMessage.getProcess_status(), ProcessStatus.RUNNING);
		
		assertNotNull(logMessage.getTimestamp());

	}
	
	@Test
	public void testLoadConfig(){	
		try {
			Mockito.when(clientFactory.create(Mockito.any(),  Mockito.any())).thenReturn(client);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		TestManagerImpl testManager = new TestManagerImpl(clientFactory, 
											configurationManager, simulationManager, 
											statusReporter,logManager);
		testManager.start();
		String path = "./applications/python/exampleTestConfig.json";
//		/Users/jsimpson/git/adms/GOSS-GridAPPS-D/gov.pnnl.goss.gridappsd/applications/python/exampleTestConfig.json
		TestConfiguration testConfig = testManager.loadTestConfig(path);
		assertEquals(testConfig.getPowerSystemConfiguration(),"ieee8500");
	}

	@Test
	public void testLoadScript(){
		try {
			Mockito.when(clientFactory.create(Mockito.any(),  Mockito.any())).thenReturn(client);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		TestManagerImpl testManager = new TestManagerImpl(clientFactory, 
											configurationManager, simulationManager, 
											statusReporter,logManager);
		testManager.start();
		String path = "./applications/python/exampleTestScript.json";
		TestScript testScript = testManager.loadTestScript(path);
		assertEquals(testScript.name,"VVO");
		assertEquals(testScript.getOutputs().get("substation_link").get(0), "xf_hvmv_sub");
		assertEquals(testScript.getOutputs().get("regulator_list").get(0), "reg_FEEDER_REG");
		
		ArrayList<String> list = new ArrayList<String>(Arrays.asList( "reg_FEEDER_REG", "reg_VREG2", "reg_VREG3", "reg_VREG4"));
		assertEquals(testScript.getOutputs().get("regulator_list"), list);

	}
	
	@Test
	public void testRequest(){	
		String testCfg = "{\"testConfigPath\":\"./applications/python/exampleTestConfig.json\",\"testScriptPath\":\"/Users/jsimpson/git/adms/GOSS-GridAPPS-D/gov.pnnl.goss.gridappsd/applications/python/exampleTestScript.json\"}";
		RequestTest.parse(testCfg);

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
	

}
