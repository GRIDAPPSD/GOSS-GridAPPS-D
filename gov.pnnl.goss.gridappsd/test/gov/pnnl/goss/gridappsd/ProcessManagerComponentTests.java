package gov.pnnl.goss.gridappsd;

import org.slf4j.Logger;

import static org.junit.Assert.*;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.api.StatusReporter;
import gov.pnnl.goss.gridappsd.process.ProcessManagerImpl;
import pnnl.goss.core.Client;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.GossResponseEvent;

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
	
	
	/**
	 *    Succeeds when info log message is called at the start of the process manager implementation with the expected message
	 */
	@Test
	public void infoCalledWhen_processManagerStarted(){
		
		 ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
		try {
			Mockito.when(clientFactory.create(Mockito.any(),  Mockito.any())).thenReturn(client);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		ProcessManagerImpl processManager = new ProcessManagerImpl(logger, clientFactory, 
											configurationManager, simulationManager, 
											statusReporter, logManager);
		processManager.start();
		
		Mockito.verify(logger).info(argCaptor.capture());
		
		assertEquals("Starting gov.pnnl.goss.gridappsd.process.ProcessManagerImpl", argCaptor.getValue());
				
	}

	/**
	 *    Succeeds when client subscribe is called with the topic goss.gridappsd.process.>
	 */
	@Test
	public void clientSubscribedWhen_startExecuted(){
		
		try {
			Mockito.when(clientFactory.create(Mockito.any(),  Mockito.any())).thenReturn(client);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		ProcessManagerImpl processManager = new ProcessManagerImpl(logger, clientFactory, 
											configurationManager, simulationManager, 
											statusReporter, logManager);
		processManager.start();

		Mockito.verify(client).subscribe(argCaptor.capture(), Mockito.any());
		assertEquals("goss.gridappsd.process.>", argCaptor.getValue());
				
	}
	
	/**
	 *    Succeeds when process manager logs that it received a message, for this the destination and content don't matter
	 */
	@Test
	public void debugMessageReceivedWhen_startExecuted(){
		
		try {
			Mockito.when(clientFactory.create(Mockito.any(),  Mockito.any())).thenReturn(client);
		} catch (Exception e) {
			e.printStackTrace();
		}
		ArgumentCaptor<GossResponseEvent> gossResponseEventArgCaptor = ArgumentCaptor.forClass(GossResponseEvent.class);

		ProcessManagerImpl processManager = new ProcessManagerImpl(logger, clientFactory, 
											configurationManager, simulationManager, 
											statusReporter, logManager);
		processManager.start();
		client.publish("goss.gridappsd.process.start", "some message");

		
		Mockito.verify(client).subscribe(Mockito.anyString(), gossResponseEventArgCaptor.capture());

		DataResponse dr = new DataResponse("1234");
		dr.setDestination("");
		GossResponseEvent response = gossResponseEventArgCaptor.getValue();
		response.onMessage(dr);
		
		
		Mockito.verify(logger).debug(argCaptor.capture());
		assertEquals("Process manager received message ", argCaptor.getValue());
				
	}
	
	/**
	 *    Succeeds when client publish is called with a long value (representing simulation id)
	 */
	@Test
	public void simIdPublishedWhen_startExecuted(){
		
		try {
			Mockito.when(clientFactory.create(Mockito.any(),  Mockito.any())).thenReturn(client);
		} catch (Exception e) {
			e.printStackTrace();
		}

		ProcessManagerImpl processManager = new ProcessManagerImpl(logger, clientFactory, 
											configurationManager, simulationManager, 
											statusReporter, logManager);
		processManager.start();

		
		//TODO listen for client publish
		
//		Mockito.verify(client).subscribe(argCaptor.capture(), Mockito.any());
//		assertEquals("goss.gridappsd.process.>", argCaptor.getValue());
				
	}
	
	
	
	//status reported new message
	
	//error with bad config
	
	//error if no simulation config is created
	
	
	

}
