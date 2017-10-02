package gov.pnnl.goss.gridappsd;

import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.dto.ServiceInfo;
import gov.pnnl.goss.gridappsd.service.ServiceManagerImpl;
import pnnl.goss.core.ClientFactory;

@RunWith(MockitoJUnitRunner.class)
public class ServiceManagerTests {
	
	@Mock
	LogManager logManager;
	
	@Mock
	ClientFactory clientFactory;
	
	ServiceManagerImpl serviceManager;
	
	@Before
	public void beforeTests(){
		serviceManager = new ServiceManagerImpl(logManager, clientFactory);
		
		Hashtable<String, String> props = new Hashtable<String, String>();
		props.put("services.path", "C:/Users/shar064/git/GOSS-GridAPPS-D/services");
		serviceManager.updated(props);
		
		serviceManager.start();
	}
	
	@Test
	public void testPythonServiceStart_WithNoDependencyNoSimulation(){
		serviceManager.startService("fncs", null);
	}
	
	@Test
	public void testPythonServiceStart_WithDependencyAndSimulation(){
		serviceManager.startService("fncsgossbridge", "simulation_1");
	}
	
	@Test
	public void testCppServiceStart_WithDependencyAndSimulation(){
		serviceManager.startService("gridlabd", "simulation_1");
	}

}
