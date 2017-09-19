package gov.pnnl.goss.gridappsd;

import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.service.ServiceManagerImpl;

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
		
		servManagerImpl.startServiceForSimultion("gridlabd", " filename.txt", "simulation_1");
		
	}

}
