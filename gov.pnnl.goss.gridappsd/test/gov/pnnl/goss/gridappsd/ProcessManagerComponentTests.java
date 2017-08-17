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

import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.ProcessManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.api.StatusReporter;
import gov.pnnl.goss.gridappsd.process.ProcessManagerImpl;
import pnnl.goss.core.Client;
import pnnl.goss.core.ClientFactory;
import gov.pnnl.goss.gridappsd.utils.StatusReporterImpl;

@RunWith(MockitoJUnitRunner.class)
public class ProcessManagerComponentTests {
	
	@Mock
	Logger logger;
	
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
	
	@Test
	/**
	 *    Succeeds when client subscribe is called
	 */
	public void startCalledWhen_startExecuted(){
		
		// ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
		
		ProcessManager processManager = new ProcessManagerImpl(logger, clientFactory, 
											configurationManager, simulationManager, 
											statusReporter, logManager);
//		StatusReporter statusReporter = new StatusReporterImpl(clientFactory, logger) ;
		
		statusReporter.reportStatus("Testing Status");
		
		Mockito.verify(logger).debug(argCaptor.capture());
		
		assertEquals("Testing Status", argCaptor.getValue());
				
	}
	
	
	//when client subscribed
	
	//simulation id returned when start executed
	
	//status reported new message
	
	//error with bad config
	
	//error if no simulation config is created
	
	//start simulation called
	
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
