package gov.pnnl.goss.gridappsd;

import org.slf4j.Logger;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import gov.pnnl.goss.gridappsd.api.StatusReporter;
import pnnl.goss.core.Client;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.gridappsd.utils.StatusReporterImpl;

@RunWith(MockitoJUnitRunner.class)
public class StatusReporterComponentTests {
	
	@Mock
	Logger logger;
	
	@Mock
	ClientFactory clientFactory;
	
	@Mock
	Client client;
	
	@Captor
	ArgumentCaptor<String> argCaptor;
	
	@Test
	public void debugCalledWhen_reportStatusExecuted(){
		
		// ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
		
		StatusReporter statusReporter = new StatusReporterImpl(clientFactory, logger) ;
		
		statusReporter.reportStatus("Testing Status");
		
		Mockito.verify(logger).debug(argCaptor.capture());
		
		assertEquals("Testing Status", argCaptor.getValue());
				
	}
	
	@Test
	public void whenReportStatusOnTopic_clientPublishCalled(){

		// ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
		
		try {
			Mockito.when(clientFactory.create(Mockito.any(), Mockito.any())).thenReturn(client);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		StatusReporter statusReporter = new StatusReporterImpl(clientFactory, logger) ;
		
		try {
			statusReporter.reportStatus("big/status", "Things are good");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Mockito.verify(client).publish(argCaptor.capture(), argCaptor.capture());
		
		List<String> allValues = argCaptor.getAllValues();
		assertEquals(2, allValues.size());
		assertEquals("big/status", allValues.get(0));
		assertEquals("Things are good", allValues.get(1));
				
	}
	
	

}
