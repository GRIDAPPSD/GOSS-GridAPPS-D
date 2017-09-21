package gov.pnnl.goss.gridappsd;

import static org.junit.Assert.assertEquals;

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
import gov.pnnl.goss.gridappsd.api.TestConfiguration;
import gov.pnnl.goss.gridappsd.api.TestManager;
import gov.pnnl.goss.gridappsd.api.TestScript;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.testmanager.TestManagerImpl;
import pnnl.goss.core.Client;
import pnnl.goss.core.ClientFactory;

@RunWith(MockitoJUnitRunner.class)
public class TestTestManagerLocal {
	
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
	 *    Succeeds when info log message is called at the start of the process manager implementation with the expected message
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
		
//		LogMessage logMessage = argCaptorLogMessage.getAllValues().get(0);
//		
//		assertEquals(logMessage.getLog_level(), "debug");
//		assertEquals(logMessage.getLog_message(), "Starting "+ProcessManagerImpl.class.getName());
//		assertEquals(logMessage.getProcess_status(), "running");
		
//		try {
//			assertNotNull(GridAppsDConstants.GRIDAPPSD_DATE_FORMAT.parse(logMessage.getTimestamp()));
//		} catch (ParseException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}
	
	@Test
	public void testLoadConfig(){	
		String path = "/Users/jsimpson/git/adms/GOSS-GridAPPS-D/gov.pnnl.goss.gridappsd/applications/python/exampleTestConfig.json";
		TestConfiguration testConfig = tm.loadTestConfig(path);
		assertEquals(testConfig.getPowerSystemConfiguration(),"ieee8500");
	}

	@Test
	public void testLoadScript(){	
		String path = "/Users/jsimpson/git/adms/GOSS-GridAPPS-D/gov.pnnl.goss.gridappsd/applications/python/exampleTestScript.json";
		TestScript testScript = tm.loadTestScript(path);
//		assertEquals(testScript.name,"VVO");
	}
	
	@Test
	public void compare(){	
		((TestManagerImpl) tm).compare();
		JsonParser parser = new JsonParser();
		JsonElement o1 = parser.parse("{a : {a : 2}, b : 2}");
		JsonElement o2 = parser.parse("{b : 3, a : {a : 2}}");
		JsonElement o3 = parser.parse("{b : 2, a : {a : 2}}");
		System.out.println(o1.equals(o2));
		System.out.println(o1.equals(o3));
//		Assert.assertEquals(o1, o2);
	}
	

}
