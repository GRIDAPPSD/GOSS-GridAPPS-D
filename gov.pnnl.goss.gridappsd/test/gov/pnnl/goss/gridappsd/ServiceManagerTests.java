package gov.pnnl.goss.gridappsd;

import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.service.ServiceManagerImpl;

import java.io.File;
import java.util.Hashtable;

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
	
	@Test
	public void test(){
		ServiceManagerImpl servManagerImpl = new ServiceManagerImpl(logManager, clientFactory);
		
		//use directory relative to current running directory
		File f = new File("");
		File currentDir = new File(f.getAbsolutePath());
		File parentDir = currentDir.getParentFile();
		
		Hashtable<String, String> props = new Hashtable<String, String>();
		props.put("applications.path", parentDir.getAbsolutePath()+File.separator+"applications");
		props.put("services.path", parentDir.getAbsolutePath()+File.separator+"/services");
		servManagerImpl.updated(props);
		
		servManagerImpl.start();
		
		//for(SerrivservManagerImpl.list)
		
		servManagerImpl.startServiceForSimultion("gridlabd", "testFile.glm", "simulation_1");
		
	}

}
