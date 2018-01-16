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

import static gov.pnnl.goss.gridappsd.TestConstants.*;
import static org.junit.Assert.assertEquals;
import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.api.StatusReporter;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.process.ProcessNewSimulationRequest;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;

import java.io.File;
import java.util.Random;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import pnnl.goss.core.DataResponse;

@RunWith(MockitoJUnitRunner.class)
public class ProcessNewSimulationRequestComponentTests {
	
	@Captor
	ArgumentCaptor<String> argCaptor;
	
	@Captor
	ArgumentCaptor<LogMessage> argCaptorLogMessage;
	
	@Mock
	LogManager logManager;
	@Mock
	ConfigurationManager configurationManager;
	@Mock
	SimulationManager simulationManager;
	@Mock
	StatusReporter statusReporter;
	@Mock
	DataResponse event;
	
	
	
	
	/**
	 *    Succeeds when info log message is called at the start of the process new simulation request implementation with the expected message
	 */
	@Test
	public void callsMadeWhen_processStarted(){
		
		try {
			Mockito.when(configurationManager.getSimulationFile(Mockito.anyInt(),  Mockito.any())).thenReturn(new File("test"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		int simulationId =  Math.abs(new Random().nextInt());
		ProcessNewSimulationRequest request = new ProcessNewSimulationRequest(logManager);
		request.process(configurationManager, simulationManager, simulationId, event, REQUEST_SIMULATION_CONFIG);
		
		//	request simulation object parsed successfully and first log info call made
		Mockito.verify(logManager, Mockito.times(5)).log(argCaptorLogMessage.capture(), argCaptor.capture()); //GridAppsDConstants.username);
		LogMessage capturedMessage = argCaptorLogMessage.getAllValues().get(0);
		assertEquals( "Parsed config " + REQUEST_SIMULATION_CONFIG, capturedMessage.getLogMessage());
		assertEquals(LogLevel.INFO, capturedMessage.getLogLevel());
		assertEquals(ProcessNewSimulationRequest.class.getName(), capturedMessage.getSource());
		assertEquals(new Integer(simulationId).toString(), capturedMessage.getRequestId());
		assertEquals(ProcessStatus.RUNNING, capturedMessage.getProcessStatus());
		assertEquals(false, capturedMessage.getStoreToDb());
		
		//	get simulation file called
		try {
			//todo capture and verify object
			Mockito.verify(configurationManager).getSimulationFile(Mockito.anyInt(), Mockito.any());
		} catch (Exception e) {
			e.printStackTrace();
			assert(false);
		}
		
		//	start simulation called
		//todo capture and verify object
		Mockito.verify(simulationManager).startSimulation(Mockito.anyInt(), Mockito.any(), Mockito.any());
		
		
	}
	
	
	// on error report status called and log message called

	/**
	 *    Succeeds when an error status message is sent if it encounters an error (forcing this by sending invalid simulation config)
	 */
	@Test
	public void callsMadeWhen_processError(){
		
		try {
			Mockito.when(configurationManager.getSimulationFile(Mockito.anyInt(),  Mockito.any())).thenReturn(new File("test"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		int simulationId =  Math.abs(new Random().nextInt());
		ProcessNewSimulationRequest request = new ProcessNewSimulationRequest(logManager);
		request.process(configurationManager, simulationManager, simulationId, event, "Bad"+REQUEST_SIMULATION_CONFIG);
		
//		try {
//			Mockito.verify(statusReporter).reportStatus(Mockito.any(), argCaptor.capture());
//			assert(argCaptor.getValue().startsWith("Process Initialization error: "));
//		} catch (Exception e) {
//			e.printStackTrace();
//			assert(false);
//		}
		
//		request error log call made
		Mockito.verify(logManager).log(argCaptorLogMessage.capture(), argCaptor.capture()); // GridAppsDConstants.username);
		LogMessage capturedMessage = argCaptorLogMessage.getValue();
		assertEquals(true, capturedMessage.getLogMessage().startsWith("Process Initialization error: "));
		assertEquals(LogLevel.ERROR, capturedMessage.getLogLevel());
		assertEquals(ProcessNewSimulationRequest.class.getName(), capturedMessage.getSource());
		assertEquals(new Integer(simulationId).toString(), capturedMessage.getRequestId());
		assertEquals(ProcessStatus.ERROR, capturedMessage.getProcessStatus());
		assertEquals(false, capturedMessage.getStoreToDb());
	}
	
	/**
	 *    Succeeds when an error status message is sent if it encounters an error (forcing this by sending null config)
	 */
	@Test
	public void callsMadeWhen_processErrorBecauseNullConfig(){
		
		try {
			Mockito.when(configurationManager.getSimulationFile(Mockito.anyInt(),  Mockito.any())).thenReturn(new File("test"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		int simulationId =  Math.abs(new Random().nextInt());
		ProcessNewSimulationRequest request = new ProcessNewSimulationRequest(logManager);
		request.process(configurationManager, simulationManager, simulationId, event, null);
		
//		try {
//			Mockito.verify(statusReporter).reportStatus(Mockito.any(), argCaptor.capture());
//			assert(argCaptor.getValue().startsWith("Process Initialization error: "));
//		} catch (Exception e) {
//			e.printStackTrace();
//			assert(false);
//		}
		
//		request error log call made
		Mockito.verify(logManager).log(argCaptorLogMessage.capture(), argCaptor.capture()); //GridAppsDConstants.username);
		LogMessage capturedMessage = argCaptorLogMessage.getValue();
		assertEquals(true, capturedMessage.getLogMessage().startsWith("Process Initialization error: "));
		assertEquals(LogLevel.ERROR, capturedMessage.getLogLevel());
		assertEquals(new Integer(simulationId).toString(), capturedMessage.getRequestId());
		assertEquals(ProcessNewSimulationRequest.class.getName(), capturedMessage.getSource());
		assertEquals(ProcessStatus.ERROR, capturedMessage.getProcessStatus());
		assertEquals(false, capturedMessage.getStoreToDb());
	}
	

	/**
	 *    Succeeds when an error status message is sent if it encounters a null simulation file
	 */
	@Test
	public void callsMadeWhen_processErrorBecauseNullSimulationFile(){
		
		try {
			Mockito.when(configurationManager.getSimulationFile(Mockito.anyInt(),  Mockito.any())).thenReturn(null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		int simulationId =  Math.abs(new Random().nextInt());
		ProcessNewSimulationRequest request = new ProcessNewSimulationRequest(logManager);
		request.process(configurationManager, simulationManager, simulationId, event, REQUEST_SIMULATION_CONFIG);
		
		
//		request error log call made
		Mockito.verify(logManager, Mockito.times(4)).log(argCaptorLogMessage.capture(), argCaptor.capture()); // GridAppsDConstants.username);
		LogMessage capturedMessage = argCaptorLogMessage.getAllValues().get(2);
		assertEquals(true, capturedMessage.getLogMessage().startsWith("No simulation file returned for request "));
		assertEquals(LogLevel.ERROR, capturedMessage.getLogLevel());
		assertEquals(ProcessStatus.ERROR, capturedMessage.getProcessStatus());
		assertEquals(false, capturedMessage.getStoreToDb());
	}
	
	
	
	

}
