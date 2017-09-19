package gov.pnnl.goss.gridappsd;

import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.service.ServiceManagerImpl;

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
		
		Hashtable<String, String> props = new Hashtable<String, String>();
		props.put("applications.path", "C:/Users/shar064/git/GOSS-GridAPPS-D/applications");
		props.put("services.path", "C:/Users/shar064/git/GOSS-GridAPPS-D/services");
		servManagerImpl.updated(props);
		
		servManagerImpl.start();
		
		//for(SerrivservManagerImpl.list)
		
		servManagerImpl.startServiceForSimultion("gridlabd", "testFile.glm", "simulation_1");
		
	}

}
