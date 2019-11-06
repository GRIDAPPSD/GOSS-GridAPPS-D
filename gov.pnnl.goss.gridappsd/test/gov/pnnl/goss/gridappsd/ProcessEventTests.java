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

import gov.pnnl.goss.gridappsd.api.AppManager;
import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.api.LogDataManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.RoleManager;
import gov.pnnl.goss.gridappsd.api.ServiceManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.api.TestManager;
import gov.pnnl.goss.gridappsd.data.DataManagerImpl;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.PowergridModelDataRequest;
import gov.pnnl.goss.gridappsd.process.ProcessEvent;
import gov.pnnl.goss.gridappsd.process.ProcessManagerImpl;
import gov.pnnl.goss.gridappsd.process.ProcessNewSimulationRequest;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;

import java.text.ParseException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import pnnl.goss.core.Client;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.DataResponse;

@RunWith(MockitoJUnitRunner.class)
public class ProcessEventTests {
	
	@Mock
	LogDataManager logDataManager;
	@Mock
	ProcessManagerImpl processManager;
	@Mock
	ConfigurationManager configurationManager;
	@Mock
	SimulationManager simulationManager;
	@Mock
	AppManager appManager;
	@Mock
	ServiceManager serviceManager;
//	@Mock
//	DataManager dataManager;
	@Mock
	LogManager logManager;
	@Mock 
	ClientFactory clientFactory;
	@Mock 
	Client client;
	@Mock
	TestManager testManager;
	@Mock
	RoleManager roleManager;
	
	@Captor
	ArgumentCaptor<String> argCaptor;
	@Captor
	ArgumentCaptor<Long> argLongCaptor;
	@Captor
	ArgumentCaptor<LogLevel> argLogLevelCaptor;
	@Captor
	ArgumentCaptor<ProcessStatus> argProcessStatusCaptor;
	
	
	@Test
	public void testWhen_RequestPGQueryRequestSent() throws ParseException{
		
		ProcessNewSimulationRequest newSimulationProcess = new ProcessNewSimulationRequest(); 
		DataManager dataManager = new DataManagerImpl(clientFactory, logManager);
		
		ProcessEvent pe = new ProcessEvent(processManager, client, 
				newSimulationProcess, configurationManager, simulationManager, appManager, logManager, serviceManager, dataManager, testManager,roleManager);
		
		PowergridModelDataRequest pgDataRequest = new PowergridModelDataRequest();
		String queryString = "select ?line_name ?subregion_name ?region_name WHERE {?line rdf:type cim:Line."+
                              			 "?line cim:IdentifiedObject.name ?line_name."+
                                         "?line cim:Line.Region ?subregion."+
                                         "?subregion cim:IdentifiedObject.name ?subregion_name."+
                                         "?subregion cim:SubGeographicalRegion.Region ?region."+
                                         "?region cim:IdentifiedObject.name ?region_name"+
                                        "}";
		pgDataRequest.setRequestType(PowergridModelDataRequest.RequestType.QUERY.toString());
		pgDataRequest.setQueryString(queryString);
		pgDataRequest.setResultFormat(PowergridModelDataRequest.ResultFormat.JSON.toString());
		pgDataRequest.setModelId(null);
		
		DataResponse message = new DataResponse();
		message.setDestination(GridAppsDConstants.topic_requestData+".powergridmodel");
		message.setData(pgDataRequest);
		
		pe.onMessage(message);
		
	}
	
	
	@Test
	public void testWhen_RequestPGQueryObjectTypesRequestSent() throws ParseException{
		
		ProcessNewSimulationRequest newSimulationProcess = new ProcessNewSimulationRequest(); 
		DataManager dataManager = new DataManagerImpl(clientFactory, logManager);
		
		ProcessEvent pe = new ProcessEvent(processManager, client, 
				newSimulationProcess, configurationManager, simulationManager, appManager, logManager, serviceManager, dataManager, testManager,roleManager);
		
		PowergridModelDataRequest pgDataRequest = new PowergridModelDataRequest();
		
		pgDataRequest.setRequestType(PowergridModelDataRequest.RequestType.QUERY_OBJECT_TYPES.toString());
		pgDataRequest.setResultFormat(PowergridModelDataRequest.ResultFormat.JSON.toString());
		pgDataRequest.setModelId("ieee13");
		
		DataResponse message = new DataResponse();
		message.setDestination(GridAppsDConstants.topic_requestData+".powergridmodel");
		message.setData(pgDataRequest);
		
		pe.onMessage(message);
		
	}
	
	
	@Test
	public void testWhen_RequestPGQueryModelNamesRequestSent() throws ParseException{
		
		ProcessNewSimulationRequest newSimulationProcess = new ProcessNewSimulationRequest(); 
		DataManager dataManager = new DataManagerImpl(clientFactory, logManager);
		
		ProcessEvent pe = new ProcessEvent(processManager, client, 
				newSimulationProcess, configurationManager, simulationManager, appManager, logManager, serviceManager, dataManager, testManager,roleManager);
		
		PowergridModelDataRequest pgDataRequest = new PowergridModelDataRequest();
		
		pgDataRequest.setRequestType(PowergridModelDataRequest.RequestType.QUERY_MODEL_NAMES.toString());
		pgDataRequest.setResultFormat(PowergridModelDataRequest.ResultFormat.JSON.toString());
		
		DataResponse message = new DataResponse();
		message.setDestination(GridAppsDConstants.topic_requestData+".powergridmodel");
		message.setData(pgDataRequest);
		
		pe.onMessage(message);
		
	}
	

}
