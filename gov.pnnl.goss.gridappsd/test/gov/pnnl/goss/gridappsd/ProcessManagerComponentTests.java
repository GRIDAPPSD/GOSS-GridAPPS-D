/*******************************************************************************
 * Copyright (c) 2017, Battelle Memorial Institute All rights reserved.
 * Battelle Memorial Institute (hereinafter Battelle) hereby grants permission to any person or entity 
 * lawfully obtaining a copy of this software and associated documentation files (hereinafter the 
 * Software) to redistribute and use the Software in source and binary forms, with or without modification. 
 * Such person or entity may use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Software, and may permit others to do so, subject to the following conditions:
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the 
 * following disclaimers.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and 
 * the following disclaimer in the documentation and/or other materials provided with the distribution.
 * Other than as used herein, neither the name Battelle Memorial Institute or Battelle may be used in any 
 * form whatsoever without the express written consent of Battelle.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL 
 * BATTELLE OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, 
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE 
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED 
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED 
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * General disclaimer for use with OSS licenses
 * 
 * This material was prepared as an account of work sponsored by an agency of the United States Government. 
 * Neither the United States Government nor the United States Department of Energy, nor Battelle, nor any 
 * of their employees, nor any jurisdiction or organization that has cooperated in the development of these 
 * materials, makes any warranty, express or implied, or assumes any legal liability or responsibility for 
 * the accuracy, completeness, or usefulness or any information, apparatus, product, software, or process 
 * disclosed, or represents that its use would not infringe privately owned rights.
 * 
 * Reference herein to any specific commercial product, process, or service by trade name, trademark, manufacturer, 
 * or otherwise does not necessarily constitute or imply its endorsement, recommendation, or favoring by the United 
 * States Government or any agency thereof, or Battelle Memorial Institute. The views and opinions of authors expressed 
 * herein do not necessarily state or reflect those of the United States Government or any agency thereof.
 * 
 * PACIFIC NORTHWEST NATIONAL LABORATORY operated by BATTELLE for the 
 * UNITED STATES DEPARTMENT OF ENERGY under Contract DE-AC05-76RL01830
 ******************************************************************************/
package gov.pnnl.goss.gridappsd;

import static gov.pnnl.goss.gridappsd.TestConstants.REQUEST_SIMULATION_CONFIG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import gov.pnnl.goss.gridappsd.api.AppManager;
import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
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
	AppManager appManager;
	
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

    // TODO the clientFactory doesn't return a satisfactory client so that there is a null pointer exception that is thrown in start of manager.
//
//		try {
//			Mockito.when(clientFactory.create(Mockito.any(),  Mockito.any())).thenReturn(client);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		ProcessManagerImpl processManager = new ProcessManagerImpl(clientFactory, 
//											configurationManager, simulationManager, 
//											statusReporter, logManager, appManager, newSimulationProcess);
//		processManager.start();
//		
//		Mockito.verify(logManager).log(argCaptorLogMessage.capture(),GridAppsDConstants.username);
//		
//		LogMessage logMessage = argCaptorLogMessage.getAllValues().get(0);
//		
//		assertEquals(logMessage.getLogLevel(), LogLevel.DEBUG);
//		assertEquals(logMessage.getLogMessage(), "Starting "+ProcessManagerImpl.class.getName());
//		assertEquals(logMessage.getProcessStatus(), ProcessStatus.RUNNING);
//		
//		assertNotNull(logMessage.getTimestamp());

				
	}

	/**
	 *    Succeeds when client subscribe is called with the topic goss.gridappsd.process.>
	 */
	@Test
	public void clientSubscribedWhen_startExecuted(){
		// TODO the clientFactory doesn't return a satisfactory client so that there is a null pointer exception that is thrown in start of manager.
//		
//		//Initialize so that will return a mock client when clientfactory.create() is called
//		try {
//			Mockito.when(clientFactory.create(Mockito.any(),  Mockito.any())).thenReturn(client);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//		//Initialize process manager with mock objects
//		ProcessManagerImpl processManager = new ProcessManagerImpl( clientFactory, 
//											configurationManager, simulationManager, 
//											statusReporter, logManager, appManager, newSimulationProcess);
//		//In junit the start() must be explicitly called
//		processManager.start();
//
//		
//		//Verify that client.subscribe() is called and that the client create succeeded
//		Mockito.verify(client).subscribe(argCaptor.capture(), Mockito.any());
//		//Verify that it subscribed to the expected topic
//		assertEquals("goss.gridappsd.process.>", argCaptor.getValue());
				
	}
	
	/**
	 *    Succeeds when process manager logs that it received a message, for this the destination and content don't matter
	 */
	@Test
	public void debugMessageReceivedWhen_startExecuted(){
		// TODO the clientFactory doesn't return a satisfactory client so that there is a null pointer exception that is thrown in start of manager.
//		try {
//			Mockito.when(clientFactory.create(Mockito.any(),  Mockito.any())).thenReturn(client);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		ArgumentCaptor<GossResponseEvent> gossResponseEventArgCaptor = ArgumentCaptor.forClass(GossResponseEvent.class);
//
//		ProcessManagerImpl processManager = new ProcessManagerImpl(clientFactory, 
//											configurationManager, simulationManager, 
//											statusReporter, logManager, appManager, newSimulationProcess);
//		processManager.start();
//		client.publish("goss.gridappsd.process.start", "some message");
//
//		Mockito.verify(client).subscribe(Mockito.anyString(), gossResponseEventArgCaptor.capture());
//
//		
//		
//		DataResponse dr = new DataResponse("1234");
//		dr.setDestination("");
//		GossResponseEvent response = gossResponseEventArgCaptor.getValue();
//		response.onMessage(dr);
//		
//		Mockito.verify(logManager, Mockito.times(2)).log(argCaptorLogMessage.capture(),GridAppsDConstants.username);
//
//		LogMessage logMessage = argCaptorLogMessage.getAllValues().get(0);
//		
//		assertEquals(logMessage.getLogLevel(), LogLevel.DEBUG);
//		assertEquals(logMessage.getLogMessage(), "Recevied message: "+ dr.getData() +" on topic " + dr.getDestination());
//		assertEquals(logMessage.getProcessStatus(), ProcessStatus.RUNNING);
//		
//		assertNotNull(logMessage.getTimestamp());
//		
//		logMessage = argCaptorLogMessage.getAllValues().get(1);
//		
//		assertEquals(logMessage.getLogLevel(), LogLevel.DEBUG);
//		assertEquals(logMessage.getLogMessage(), "Recevied message: "+ dr.getData() +" on topic " + dr.getDestination());
//		assertEquals(logMessage.getProcessStatus(), ProcessStatus.RUNNING);
//		
//		assertNotNull(logMessage.getTimestamp());
				
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
											 logManager, appManager, newSimulationProcess);
		processManager.start();

		Mockito.verify(client).subscribe(Mockito.anyString(), gossResponseEventArgCaptor.capture());
		
		
		DataResponse dr = new DataResponse(REQUEST_SIMULATION_CONFIG);
		dr.setDestination("goss.gridappsd.process.request.simulation");
		GossResponseEvent response = gossResponseEventArgCaptor.getValue();
		response.onMessage(dr);
		
		ArgumentCaptor<Serializable> argCaptorSerializable= ArgumentCaptor.forClass(Serializable.class) ;
		//listen for client publish
		Mockito.verify(client).publish(Mockito.any(Destination.class), argCaptorSerializable.capture());

		new Long(argCaptorSerializable.getValue().toString());
				
	}
	
	
	
	
	//status reported new message
	/**
	 *    Succeeds when the correct message is logged after valid simulation request is sent to the simulation topic
	 */
	@Test
	public void loggedStatusWhen_simulationTopicSent(){

		// TODO the clientFactory doesn't return a satisfactory client so that there is a null pointer exception that is thrown in start of manager.
//		try {
//			Mockito.when(clientFactory.create(Mockito.any(),  Mockito.any())).thenReturn(client);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}		
//		ArgumentCaptor<GossResponseEvent> gossResponseEventArgCaptor = ArgumentCaptor.forClass(GossResponseEvent.class);
//
//
//		ProcessManagerImpl processManager = new ProcessManagerImpl( clientFactory, 
//											configurationManager, simulationManager, 
//											statusReporter, logManager, appManager, newSimulationProcess);
//		processManager.start();
//
//		Mockito.verify(client).subscribe(Mockito.anyString(), gossResponseEventArgCaptor.capture());
//
//		
//		DataResponse dr = new DataResponse(REQUEST_SIMULATION_CONFIG);
//		dr.setDestination("goss.gridappsd.process.request.simulation");
//		GossResponseEvent response = gossResponseEventArgCaptor.getValue();
//		response.onMessage(dr);
//		Mockito.verify(logManager, Mockito.times(2)).log(argCaptorLogMessage.capture(), argCaptor.capture());
//
//		LogMessage logMessage = argCaptorLogMessage.getAllValues().get(0);
//		
//		assertEquals(logMessage.getLogLevel(), LogLevel.DEBUG);
//		assertEquals(logMessage.getLogMessage(), "Recevied message: "+ dr.getData() +" on topic " + dr.getDestination());
//		assertEquals(logMessage.getProcessStatus(), ProcessStatus.RUNNING);
//		
//		assertNotNull(logMessage.getTimestamp());
//		
//		logMessage = argCaptorLogMessage.getAllValues().get(1);
//		
//		assertEquals(logMessage.getLogLevel(), LogLevel.DEBUG);
//		assertEquals(logMessage.getLogMessage(), "Recevied message: "+ dr.getData() +" on topic " + dr.getDestination());
//		assertEquals(logMessage.getProcessStatus(), ProcessStatus.RUNNING);
//		
//		assertNotNull(logMessage.getTimestamp());
		
		
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
											 logManager, appManager, newSimulationProcess);
		processManager.start();

		Mockito.verify(client).subscribe(Mockito.anyString(), gossResponseEventArgCaptor.capture());


		DataResponse dr = new DataResponse(REQUEST_SIMULATION_CONFIG);
		dr.setDestination("goss.gridappsd.process.request.simulation");
		GossResponseEvent response = gossResponseEventArgCaptor.getValue();
		response.onMessage(dr);
		ArgumentCaptor<Serializable> argCaptorSerializable= ArgumentCaptor.forClass(Serializable.class) ;


		Mockito.verify(newSimulationProcess).process(Mockito.any(), Mockito.any(), 
				Mockito.anyInt(),argCaptorSerializable.capture(), Mockito.anyInt(),Mockito.any(),Mockito.any());
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
	//DOesn't currently support log with string only
//	/**
//	 *    Succeeds when the correct message is logged after valid message is sent to the log topic
//	 */
//	@Test
//	public void loggedStatusWhen_logTopicSent(){
//		
//		try {
//			Mockito.when(clientFactory.create(Mockito.any(),  Mockito.any())).thenReturn(client);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}		
//		ArgumentCaptor<GossResponseEvent> gossResponseEventArgCaptor = ArgumentCaptor.forClass(GossResponseEvent.class);
//
//
//		ProcessManagerImpl processManager = new ProcessManagerImpl( clientFactory, 
//											configurationManager, simulationManager, 
//											statusReporter, logManager, appManager, newSimulationProcess);
//		processManager.start();
//
//		Mockito.verify(client).subscribe(Mockito.anyString(), gossResponseEventArgCaptor.capture());
//		String logMessage = "My Test Log Message";
//		
//		DataResponse dr = new DataResponse(logMessage);
//		dr.setDestination("goss.gridappsd.process.log");
//		GossResponseEvent response = gossResponseEventArgCaptor.getValue();
//		response.onMessage(dr);
//		
//		Mockito.verify(logManager).log(argCaptor.capture());
//
//		assertEquals(logMessage, argCaptor.getValue());
//	}
	
	
	//TODO add appmanaager test

}
