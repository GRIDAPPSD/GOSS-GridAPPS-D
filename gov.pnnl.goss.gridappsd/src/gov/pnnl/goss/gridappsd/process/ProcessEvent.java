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
package gov.pnnl.goss.gridappsd.process;

import gov.pnnl.goss.gridappsd.api.AppManager;
import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.ServiceManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.api.TestManager;
import gov.pnnl.goss.gridappsd.dto.ConfigurationRequest;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.PlatformStatus;
import gov.pnnl.goss.gridappsd.dto.RequestPlatformStatus;
import gov.pnnl.goss.gridappsd.dto.RequestSimulation;
import gov.pnnl.goss.gridappsd.dto.RequestSimulation.SimulationRequestType;
import gov.pnnl.goss.gridappsd.dto.RequestSimulationResponse;
import gov.pnnl.goss.gridappsd.dto.RuntimeTypeAdapterFactory;
import gov.pnnl.goss.gridappsd.dto.YBusExportResponse;
import gov.pnnl.goss.gridappsd.dto.events.CommOutage;
import gov.pnnl.goss.gridappsd.dto.events.Event;
import gov.pnnl.goss.gridappsd.dto.events.Fault;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Date;

import javax.jms.Destination;

import pnnl.goss.core.Client;
import pnnl.goss.core.DataError;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.core.Response;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * ProcessEvent class processes requests received by the Process Manager.
 * These requests include:
 * <p>
 * 1. Start a simulation <br>
 * 2. Query data <br>
 * 3. Logg messages <br>
 * 4. Query platform status
 * 
 * @author Poorva Sharma
 * @author Tara D Gibson
 *
 */
public class ProcessEvent implements GossResponseEvent {

	Client client;
	ProcessNewSimulationRequest newSimulationProcess;
	ProcessManagerImpl processManger;
	ConfigurationManager configurationManager;
	SimulationManager simulationManager;
	AppManager appManager;
	LogManager logManager;
	ServiceManager serviceManager;
	DataManager dataManager;
	TestManager testManager;


	public ProcessEvent(ProcessManagerImpl processManager, 
			Client client, ProcessNewSimulationRequest newSimulationProcess, 
			ConfigurationManager configurationManager, SimulationManager simulationManager, 
			AppManager appManager, LogManager logManager, ServiceManager serviceManager, 
			DataManager dataManager, TestManager testManager){
		this.client = client;
		this.processManger = processManager;
		this.newSimulationProcess = newSimulationProcess;
		this.configurationManager = configurationManager;
		this.simulationManager = simulationManager;
		this.appManager = appManager;
		this.logManager = logManager;
		this.serviceManager = serviceManager;
		this.dataManager = dataManager;
		this.testManager = testManager;
	}


	@Override
	public void onMessage(Serializable message) {

		DataResponse event = (DataResponse)message;
		String username  = GridAppsDConstants.username;

		int processId = ProcessManagerImpl.generateProcessId();
		this.debug(processId, "Received message: "+ event.getData() +" on topic "+event.getDestination()+" from user "+username);


		try{ 

			if(event.getDestination().contains(GridAppsDConstants.topic_requestSimulation )){
				//Parse simluation request
				Serializable request;
				if (message instanceof DataResponse){
					request = ((DataResponse)message).getData();
				} else {
					request = message;
				}

				RequestSimulation simRequest = null;
				if(request instanceof ConfigurationRequest){
					simRequest = ((RequestSimulation)request);
				} else{
					if(request!=null){
						//make sure it doesn't fail if request is null, although it should never be null
						try{
							GsonBuilder gsonBuilder = new GsonBuilder();
					        RuntimeTypeAdapterFactory<Event> commandAdapterFactory = RuntimeTypeAdapterFactory.of(Event.class, "event_type")
					        .registerSubtype(CommOutage.class,"CommOutage").registerSubtype(Fault.class, "Fault");
					        gsonBuilder.registerTypeAdapterFactory(commandAdapterFactory);
					        gsonBuilder.setPrettyPrinting();
					        Gson gson = gsonBuilder.create();
					        simRequest = gson.fromJson(request.toString(), RequestSimulation.class);
							//simRequest = RequestSimulation.parse(request.toString());
						}catch(JsonSyntaxException e){
							e.printStackTrace();
							//TODO log error
							sendError(client, event.getReplyDestination(), e.getMessage(), processId);
						}
					} else {
						sendError(client, event.getReplyDestination(), "Simulation request is null", processId);
					}
				}
				if(simRequest!=null){
					//if new simulation		
					if (simRequest.simulation_request_type==null || simRequest.simulation_request_type.equals(SimulationRequestType.NEW)){
						RequestSimulationResponse response = new RequestSimulationResponse();
						response.setSimulationId(Integer.toString(processId));
						//RequestSimulation config = RequestSimulation.parse(message.toString());
						if(simRequest.getTest_config()!=null)
							response.setEvents(testManager.sendEventsToSimulation(simRequest.getTest_config().getEvents(), Integer.toString(processId)));
						client.publish(event.getReplyDestination(), response);
						//TODO also verify that we have the correct sub-configurations as part of the request
						//newSimulationProcess.process(configurationManager, simulationManager, processId, event, event.getData(), appManager, serviceManager);
						newSimulationProcess.process(configurationManager, simulationManager, processId, simRequest,processManger.assignSimulationPort(processId), appManager,serviceManager, testManager);
					} else if (simRequest.simulation_request_type.equals(SimulationRequestType.PAUSE)) { //if pause
						simulationManager.pauseSimulation(simRequest.getSimulation_id());
					} else if (simRequest.simulation_request_type.equals(SimulationRequestType.RESUME)) { //if play
						simulationManager.resumeSimulation(simRequest.getSimulation_id());
					} else if (simRequest.simulation_request_type.equals(SimulationRequestType.STOP)) { //if stop
						simulationManager.endSimulation(simRequest.getSimulation_id());
					} else{
						sendError(client, event.getReplyDestination(), "Simulation request type not recognized: "+simRequest.simulation_request_type, processId);
					}
				} else {
					sendError(client, event.getReplyDestination(), "Simulation request could not be parsed", processId);
				}
			} else if(event.getDestination().contains(GridAppsDConstants.topic_requestApp )){
				appManager.process(processId, event, message);

			} else if(event.getDestination().contains(GridAppsDConstants.topic_requestData)){

				String requestTopicExtension = event.getDestination().substring(event.getDestination().indexOf(GridAppsDConstants.topic_requestData)+GridAppsDConstants.topic_requestData.length());
				if(requestTopicExtension.length()>0){
					requestTopicExtension = requestTopicExtension.substring(1);
				}

				if(requestTopicExtension.indexOf(".")>0){
					requestTopicExtension = requestTopicExtension.substring(0, requestTopicExtension.indexOf("."));
				}
				String type = requestTopicExtension;

				this.debug(processId, "Received data request of type: "+type);

				Serializable request;
				if (message instanceof DataResponse){
					request = ((DataResponse)message).getData();
				} else {
					request = message;
				}

				Response r = dataManager.processDataRequest(request, type, processId, configurationManager.getConfigurationProperty(GridAppsDConstants.GRIDAPPSD_TEMP_PATH), username);
				//client.publish(event.getReplyDestination(), r);
				sendData(client, event.getReplyDestination(), ((DataResponse)r).getData(), processId);


			} else if(event.getDestination().contains(GridAppsDConstants.topic_requestConfig)){

				Serializable request;
				if (message instanceof DataResponse){
					request = ((DataResponse)message).getData();
				} else {
					request = message;
				}

				ConfigurationRequest configRequest = null;
				if(request instanceof ConfigurationRequest){
					configRequest = ((ConfigurationRequest)request);
				} else{
					try{
						configRequest = ConfigurationRequest.parse(request.toString());
					}catch(JsonSyntaxException e){
						//TODO log error
						sendError(client, event.getReplyDestination(), e.getMessage(), processId);
					}
				}
				if(configRequest!=null){
					StringWriter sw = new StringWriter();
					PrintWriter out = new PrintWriter(sw);
					try {
						configurationManager.generateConfiguration(configRequest.getConfigurationType(), configRequest.getParameters(), out, new Integer(processId).toString(), username);
					} catch (Exception e) {
						StringWriter sww = new StringWriter();
						PrintWriter pw = new PrintWriter(sww);
						e.printStackTrace(pw);
						this.error(processId,sww.toString());
						sendError(client, event.getReplyDestination(), e.getMessage(), processId);
					}
					String result = sw.toString();
					
					if(configRequest.getConfigurationType().equals("YBus Export")){
						Gson gson = new Gson();
						YBusExportResponse response = gson.fromJson(result, YBusExportResponse.class);
						sendData(client, event.getReplyDestination(), response, processId);
					}
					else
						sendData(client, event.getReplyDestination(), result, processId);

				} else {
					this.error(processId, "No valid configuration request received, request: "+request);
					sendError(client, event.getReplyDestination(), "No valid configuration request received, request: "+request, processId);
				}


			} else if(event.getDestination().contains(GridAppsDConstants.topic_requestPlatformStatus)){
				
				RequestPlatformStatus request = RequestPlatformStatus.parse(event.getData().toString());
				 PlatformStatus platformStatus = new PlatformStatus();
				if(request.isApplications())
					platformStatus.setApplications(appManager.listApps());
				if(request.isServices())
					platformStatus.setServices(serviceManager.listServices());
				if(request.isAppInstances())
					platformStatus.setAppInstances(appManager.listRunningApps());
				if(request.isServiceInstances())
					platformStatus.setServiceInstances(serviceManager.listRunningServices());
				client.publish(event.getReplyDestination(), platformStatus);
			}
		}catch(Exception e ){
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			this.error(processId,sw.toString());
			sendError(client, event.getReplyDestination(), sw.toString(), processId);
		}
	}


	private void sendData(Client client, Destination replyDestination, Serializable data, int processId){
		try {
			String r = "{\"data\":"+data+",\"responseComplete\":true,\"id\":\""+processId+"\"}";
			/*DataResponse r = new DataResponse();
			r.setData(data);
			r.setResponseComplete(true);*/
			client.publish(replyDestination, r);
		} catch (Exception e) {
			e.printStackTrace();
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			this.error(processId,sw.toString());
			//TODO log error and send error response
		}
	}


	private void sendError(Client client, Destination replyDestination, String error, int processId){
		try {
			DataResponse r = new DataResponse();
			r.setError(new DataError(error));
			r.setResponseComplete(true);
			client.publish(replyDestination, r);
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			this.error(processId,sw.toString());
		}
	}


	private void debug(int processId, String message) {

		LogMessage logMessage = new LogMessage();
		logMessage.setSource(this.getClass().getSimpleName());
		logMessage.setProcessId(Integer.toString(processId));
		logMessage.setLogLevel(LogLevel.DEBUG);
		logMessage.setProcessStatus(ProcessStatus.RUNNING);
		logMessage.setLogMessage(message);
		logMessage.setStoreToDb(true);
		logMessage.setTimestamp(new Date().getTime());

		logManager.log(logMessage, GridAppsDConstants.topic_platformLog);	

	}

	private void error(int processId, String message) {

		LogMessage logMessage = new LogMessage();
		logMessage.setSource(this.getClass().getSimpleName());
		logMessage.setProcessId(Integer.toString(processId));
		logMessage.setLogLevel(LogLevel.ERROR);
		logMessage.setProcessStatus(ProcessStatus.ERROR);
		logMessage.setLogMessage(message);
		logMessage.setStoreToDb(true);
		logMessage.setTimestamp(new Date().getTime());

		logManager.log(logMessage, GridAppsDConstants.topic_platformLog);	

	}

}