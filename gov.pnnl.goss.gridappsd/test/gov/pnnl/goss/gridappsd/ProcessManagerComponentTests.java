package gov.pnnl.goss.gridappsd;

import static gov.pnnl.goss.gridappsd.TestConstants.REQUEST_SIMULATION_CONFIG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.api.StatusReporter;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.RequestSimulation;
import gov.pnnl.goss.gridappsd.process.ProcessManagerImpl;
import gov.pnnl.goss.gridappsd.process.ProcessNewSimulationRequest;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;

import java.io.Serializable;
import java.text.ParseException;

import javax.jms.Destination;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import pnnl.goss.core.Client;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.GossResponseEvent;

@RunWith(MockitoJUnitRunner.class)
public class ProcessManagerComponentTests {
	
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
	
	@Mock
	ProcessNewSimulationRequest newSimulationProcess;
	
	
	@Captor
	ArgumentCaptor<String> argCaptor;
	
	@Captor
	ArgumentCaptor<LogMessage> argCaptorLogMessage;
	
	
	
	
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
		
		ProcessManagerImpl processManager = new ProcessManagerImpl(clientFactory, 
											configurationManager, simulationManager, 
											statusReporter, logManager, newSimulationProcess);
		processManager.start();
		
		Mockito.verify(logManager).log(argCaptorLogMessage.capture());
		
		LogMessage logMessage = argCaptorLogMessage.getAllValues().get(0);
		
		assertEquals(logMessage.getLog_level(), "debug");
		assertEquals(logMessage.getLog_message(), "Starting "+ProcessManagerImpl.class.getName());
		assertEquals(logMessage.getProcess_status(), "running");
		
		try {
			assertNotNull(GridAppsDConstants.GRIDAPPSD_DATE_FORMAT.parse(logMessage.getTimestamp()));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
	}

	/**
	 *    Succeeds when client subscribe is called with the topic goss.gridappsd.process.>
	 */
	@Test
	public void clientSubscribedWhen_startExecuted(){
		
		//Initialize so that will return a mock client when clientfactory.create() is called
		try {
			Mockito.when(clientFactory.create(Mockito.any(),  Mockito.any())).thenReturn(client);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//Initialize process manager with mock objects
		ProcessManagerImpl processManager = new ProcessManagerImpl( clientFactory, 
											configurationManager, simulationManager, 
											statusReporter, logManager, newSimulationProcess);
		//In junit the start() must be explicitly called
		processManager.start();

		
		//Verify that client.subscribe() is called and that the client create succeeded
		Mockito.verify(client).subscribe(argCaptor.capture(), Mockito.any());
		//Verify that it subscribed to the expected topic
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

		ProcessManagerImpl processManager = new ProcessManagerImpl(clientFactory, 
											configurationManager, simulationManager, 
											statusReporter, logManager, newSimulationProcess);
		processManager.start();
		client.publish("goss.gridappsd.process.start", "some message");

		Mockito.verify(client).subscribe(Mockito.anyString(), gossResponseEventArgCaptor.capture());

		
		
		DataResponse dr = new DataResponse("1234");
		dr.setDestination("");
		GossResponseEvent response = gossResponseEventArgCaptor.getValue();
		response.onMessage(dr);
		
		Mockito.verify(logManager, Mockito.times(2)).log(argCaptorLogMessage.capture());

		LogMessage logMessage = argCaptorLogMessage.getAllValues().get(0);
		
		assertEquals(logMessage.getLog_level(), "debug");
		assertEquals(logMessage.getLog_message(), "Recevied message: "+ dr.getData() +" on topic " + dr.getDestination());
		assertEquals(logMessage.getProcess_status(), "running");
		
		try {
			assertNotNull(GridAppsDConstants.GRIDAPPSD_DATE_FORMAT.parse(logMessage.getTimestamp()));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		logMessage = argCaptorLogMessage.getAllValues().get(1);
		
		assertEquals(logMessage.getLog_level(), "debug");
		assertEquals(logMessage.getLog_message(), "Recevied message: "+ dr.getData() +" on topic " + dr.getDestination());
		assertEquals(logMessage.getProcess_status(), "running");
		
		try {
			assertNotNull(GridAppsDConstants.GRIDAPPSD_DATE_FORMAT.parse(logMessage.getTimestamp()));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
	}
	
	
	/**
	 *    Succeeds when client publish is called with a long value (representing simulation id) after a request message is sent
	 */
	@Test
	public void simIdPublishedWhen_messageSent(){
		
		try {
			Mockito.when(clientFactory.create(Mockito.any(),  Mockito.any())).thenReturn(client);
		} catch (Exception e) {
			e.printStackTrace();
		}		
		ArgumentCaptor<GossResponseEvent> gossResponseEventArgCaptor = ArgumentCaptor.forClass(GossResponseEvent.class);


		ProcessManagerImpl processManager = new ProcessManagerImpl( clientFactory, 
											configurationManager, simulationManager, 
											statusReporter, logManager, newSimulationProcess);
		processManager.start();

		Mockito.verify(client).subscribe(Mockito.anyString(), gossResponseEventArgCaptor.capture());
		
		
		DataResponse dr = new DataResponse(REQUEST_SIMULATION_CONFIG);
		dr.setDestination("goss.gridappsd.process.request.simulation");
		GossResponseEvent response = gossResponseEventArgCaptor.getValue();
		response.onMessage(dr);
		
		ArgumentCaptor<Serializable> argCaptorSerializable= ArgumentCaptor.forClass(Serializable.class) ;
		//listen for client publish
		Mockito.verify(client).publish(Mockito.any(Destination.class), argCaptorSerializable.capture());

		Long l = new Long(argCaptorSerializable.getValue().toString());
				
	}
	
	
	
	
	//status reported new message
	/**
	 *    Succeeds when the correct message is logged after valid simulation request is sent to the simulation topic
	 */
	@Test
	public void loggedStatusWhen_simulationTopicSent(){
		try {
			Mockito.when(clientFactory.create(Mockito.any(),  Mockito.any())).thenReturn(client);
		} catch (Exception e) {
			e.printStackTrace();
		}		
		ArgumentCaptor<GossResponseEvent> gossResponseEventArgCaptor = ArgumentCaptor.forClass(GossResponseEvent.class);


		ProcessManagerImpl processManager = new ProcessManagerImpl( clientFactory, 
											configurationManager, simulationManager, 
											statusReporter, logManager, newSimulationProcess);
		processManager.start();

		Mockito.verify(client).subscribe(Mockito.anyString(), gossResponseEventArgCaptor.capture());

		
		DataResponse dr = new DataResponse(REQUEST_SIMULATION_CONFIG);
		dr.setDestination("goss.gridappsd.process.request.simulation");
		GossResponseEvent response = gossResponseEventArgCaptor.getValue();
		response.onMessage(dr);
		Mockito.verify(logManager, Mockito.times(2)).log(argCaptorLogMessage.capture());

		LogMessage logMessage = argCaptorLogMessage.getAllValues().get(0);
		
		assertEquals(logMessage.getLog_level(), "debug");
		assertEquals(logMessage.getLog_message(), "Recevied message: "+ dr.getData() +" on topic " + dr.getDestination());
		assertEquals(logMessage.getProcess_status(), "running");
		
		try {
			assertNotNull(GridAppsDConstants.GRIDAPPSD_DATE_FORMAT.parse(logMessage.getTimestamp()));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		logMessage = argCaptorLogMessage.getAllValues().get(1);
		
		assertEquals(logMessage.getLog_level(), "debug");
		assertEquals(logMessage.getLog_message(), "Recevied message: "+ dr.getData() +" on topic " + dr.getDestination());
		assertEquals(logMessage.getProcess_status(), "running");
		
		try {
			assertNotNull(GridAppsDConstants.GRIDAPPSD_DATE_FORMAT.parse(logMessage.getTimestamp()));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}	
	
	
	//status reported new message
	/**
	 *    Succeeds when the process method is called after valid simulation request is sent to the simulation topic, also verifies that request message can be parsed
	 */
	@Test
	public void processStartedWhen_simulationTopicSent(){
		try {
			Mockito.when(clientFactory.create(Mockito.any(),  Mockito.any())).thenReturn(client);
		} catch (Exception e) {
			e.printStackTrace();
		}		
		ArgumentCaptor<GossResponseEvent> gossResponseEventArgCaptor = ArgumentCaptor.forClass(GossResponseEvent.class);


		ProcessManagerImpl processManager = new ProcessManagerImpl( clientFactory, 
											configurationManager, simulationManager, 
											statusReporter, logManager, newSimulationProcess);
		processManager.start();

		Mockito.verify(client).subscribe(Mockito.anyString(), gossResponseEventArgCaptor.capture());


		DataResponse dr = new DataResponse(REQUEST_SIMULATION_CONFIG);
		dr.setDestination("goss.gridappsd.process.request.simulation");
		GossResponseEvent response = gossResponseEventArgCaptor.getValue();
		response.onMessage(dr);
		ArgumentCaptor<Serializable> argCaptorSerializable= ArgumentCaptor.forClass(Serializable.class) ;


		Mockito.verify(newSimulationProcess).process(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.any(), argCaptorSerializable.capture());
		String messageString = argCaptorSerializable.getValue().toString();

		assertNotNull(RequestSimulation.parse(messageString));
		
	}	
	
//	/**
//	 *    Succeeds when process manager reports error because of bad config (when bad config is sent)
//	 */
//	@Test
//	public void processErrorWhen_badSimulationRequestSent(){
//		try {
//			Mockito.when(clientFactory.create(Mockito.any(),  Mockito.any())).thenReturn(client);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}		
//		ArgumentCaptor<GossResponseEvent> gossResponseEventArgCaptor = ArgumentCaptor.forClass(GossResponseEvent.class);
//
//
//		ProcessManagerImpl processManager = new ProcessManagerImpl(logger, clientFactory, 
//											configurationManager, simulationManager, 
//											statusReporter, logManager, newSimulationProcess);
//		processManager.start();
//
//		Mockito.verify(client).subscribe(Mockito.anyString(), gossResponseEventArgCaptor.capture());
//
//		
//		DataResponse dr = new DataResponse("BADFORMAT"+REQUEST_SIMULATION_CONFIG);
//		dr.setDestination("goss.gridappsd.process.request.simulation");
//		GossResponseEvent response = gossResponseEventArgCaptor.getValue();
//		response.onMessage(dr);
//	}
//	
	
	//error if no simulation config is created
	/**
	 *    Succeeds when the correct message is logged after valid message is sent to the log topic
	 */
	@Test
	public void loggedStatusWhen_logTopicSent(){
		
		try {
			Mockito.when(clientFactory.create(Mockito.any(),  Mockito.any())).thenReturn(client);
		} catch (Exception e) {
			e.printStackTrace();
		}		
		ArgumentCaptor<GossResponseEvent> gossResponseEventArgCaptor = ArgumentCaptor.forClass(GossResponseEvent.class);


		ProcessManagerImpl processManager = new ProcessManagerImpl( clientFactory, 
											configurationManager, simulationManager, 
											statusReporter, logManager, newSimulationProcess);
		processManager.start();

		Mockito.verify(client).subscribe(Mockito.anyString(), gossResponseEventArgCaptor.capture());
		String logMessage = "My Test Log Message";
		
		DataResponse dr = new DataResponse(logMessage);
		dr.setDestination("goss.gridappsd.process.log");
		GossResponseEvent response = gossResponseEventArgCaptor.getValue();
		response.onMessage(dr);
		
		Mockito.verify(logManager).log(argCaptor.capture());

		assertEquals(logMessage, argCaptor.getValue());
	}
	
	

}
