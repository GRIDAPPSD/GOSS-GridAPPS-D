package gov.pnnl.goss.gridappsd;

import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.service.ServiceManagerImpl;

import java.io.File;
import java.util.Hashtable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

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
		
		//use directory relative to current running directory
		File f = new File("");
		File currentDir = new File(f.getAbsolutePath());
		File parentDir = currentDir.getParentFile();
		
		Hashtable<String, String> props = new Hashtable<String, String>();
		props.put("applications.path", parentDir.getAbsolutePath()+File.separator+"applications");
		props.put("services.path", parentDir.getAbsolutePath()+File.separator+"services");
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
