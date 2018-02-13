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

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Date;

import javax.jms.Destination;

import org.apache.felix.dm.annotation.api.Component;

import com.google.gson.JsonSyntaxException;

import pnnl.goss.core.Client;
import pnnl.goss.core.DataError;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.core.Response;
import gov.pnnl.goss.gridappsd.api.AppManager;
import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.ServiceManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.dto.ConfigurationRequest;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;

/**
 * SimulationEvent starts a single instance of simulation
 * 
 * @author shar064
 *
 */
@Component
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
	 
	
	public ProcessEvent(ProcessManagerImpl processManager, 
			Client client, ProcessNewSimulationRequest newSimulationProcess, 
			ConfigurationManager configurationManager, SimulationManager simulationManager, 
			AppManager appManager, LogManager logManager, ServiceManager serviceManager, DataManager dataManager){
		this.client = client;
		this.processManger = processManager;
		this.newSimulationProcess = newSimulationProcess;
		this.configurationManager = configurationManager;
		this.simulationManager = simulationManager;
		this.appManager = appManager;
		this.logManager = logManager;
		this.serviceManager = serviceManager;
		this.dataManager = dataManager;
	}
	

	/**
	 * message is in the JSON string format {'SimulationId': 1,
	 * 'SimulationFile': '/path/name'}
	 */
	@Override
	public void onMessage(Serializable message) {
//System.out.println("PROCESSMANAGER "+message +" "+message.getClass());
		DataResponse event = (DataResponse)message;
		//TODO:Get username from message's metadata e.g. event.getUserName()
		String username  = GridAppsDConstants.username;
		
		int processId = ProcessManagerImpl.generateProcessId();
		LogMessage logMessageObj = new LogMessage();
		logMessageObj.setLogLevel(LogLevel.DEBUG);
		logMessageObj.setProcessId(this.getClass().getName());
		logMessageObj.setProcessStatus(ProcessStatus.RUNNING);
		logMessageObj.setStoreToDb(true);
		logMessageObj.setTimestamp(new Date().getTime());
		logMessageObj.setLogMessage("Received message: "+ event.getData() +" on topic "+event.getDestination()+" from user "+username);
		client.publish(GridAppsDConstants.topic_platformLog, logMessageObj);
		
		
		
		//TODO: create registry mapping between request topics and request handlers.
		if(event.getDestination().contains(GridAppsDConstants.topic_requestSimulation )){
			//generate simulation id and reply to event's reply destination.
			
			try {
				int simPort = processManger.assignSimulationPort(processId);
				client.publish(event.getReplyDestination(), processId);
				newSimulationProcess.process(configurationManager, simulationManager, processId, event, logMessageObj, appManager, serviceManager);
			} catch (Exception e) {
				e.printStackTrace();
				logMessageObj.setTimestamp(new Date().getTime());
				logMessageObj.setLogLevel(LogLevel.ERROR);
				logMessageObj.setLogMessage(e.getMessage());
				client.publish(GridAppsDConstants.topic_platformLog, logMessageObj);
			}
		} else if(event.getDestination().contains(GridAppsDConstants.topic_requestApp )){
			try{
				appManager.process(processId, event, message);
			}
			catch(Exception e){
				e.printStackTrace();
				logMessageObj.setTimestamp(new Date().getTime());
				logMessageObj.setLogLevel(LogLevel.ERROR);
				logMessageObj.setLogMessage(e.getMessage());
				client.publish(GridAppsDConstants.topic_platformLog, logMessageObj);
			}
		} else if(event.getDestination().contains(GridAppsDConstants.topic_requestData)){
			System.out.println("DATA REQUEST "+message.toString());
			String outputTopics = String.join(".", 
					GridAppsDConstants.topic_responseData,
					String.valueOf(processId),
					"output");
			System.out.println("OUTPUT TOPICS"+outputTopics);
			String logTopic = String.join(".", 
					GridAppsDConstants.topic_responseData,
					String.valueOf(processId),
					"log");
			System.out.println("LOG TOPIC "+logTopic);

			
			String requestTopicExtension = event.getDestination().substring(event.getDestination().indexOf(GridAppsDConstants.topic_requestData)+GridAppsDConstants.topic_requestData.length());
			if(requestTopicExtension.length()>0){
				requestTopicExtension = requestTopicExtension.substring(1);
			}
			
			if(requestTopicExtension.indexOf(".")>0){
				requestTopicExtension = requestTopicExtension.substring(0, requestTopicExtension.indexOf("."));
			}
			String type = requestTopicExtension;
			
			System.out.println("PARSE DATA REQUEST "+type);
			Serializable request;
			if (message instanceof DataResponse){
				request = ((DataResponse)message).getData();
			} else {
				request = message;
			}
			
//			PowergridModelDataRequest request;
//			if(message instanceof PowergridModelDataRequest){
//				request = (PowergridModelDataRequest)message;
//			}else {
//				request = PowergridModelDataRequest.parse(message.toString());
//			}
//			System.out.println("PARSED DATA REQUEST");
			try {
				Response r = dataManager.processDataRequest(request, type, processId, configurationManager.getConfigurationProperty(GridAppsDConstants.GRIDAPPSD_TEMP_PATH));
				client.publish(event.getReplyDestination(), r);
			} catch (Exception e) {
				e.printStackTrace();
				//TODO log error and send error response
			}
			
		} else if(event.getDestination().contains(GridAppsDConstants.topic_requestConfig)){
			System.out.println("CONFIG REQUEST "+message.toString());

			Serializable request;
			if (message instanceof DataResponse){
				request = ((DataResponse)message).getData();
			} else {
				request = message;
			}
			
			ConfigurationRequest configRequest = null;
			if(message instanceof ConfigurationRequest){
				configRequest = ((ConfigurationRequest)request);
			} else{
				try{
					configRequest = ConfigurationRequest.parse(request.toString());
				}catch(JsonSyntaxException e){
					//TODO log error
					sendError(client, event.getReplyDestination(), e.getMessage());
				}
			}
			if(configRequest!=null){
				StringWriter sw = new StringWriter();
				PrintWriter out = new PrintWriter(sw);
				try {
					configurationManager.generateConfiguration(configRequest.getConfigurationType(), configRequest.getParameters(), out);
				} catch (Exception e) {
					//TODO log error
					sendError(client, event.getReplyDestination(), e.getMessage());
				}
				String result = sw.toString();
				sendData(client, event.getReplyDestination(), result);
				
			} else {
				sendError(client, event.getReplyDestination(), "No valid configuration request received, request: "+request);
			}
			
			
		} else if(event.getDestination().contains("log")){
			logManager.log(LogMessage.parse(message.toString()), username);
		}
		
		//case GridAppsDConstants.topic_requestData : processDataRequest(); break;
		//case GridAppsDConstants.topic_requestSimulationStatus : processSimulationStatusRequest(); break;
	}


	void sendData(Client client, Destination replyDestination, Serializable data){
		try {
			DataResponse r = new DataResponse();
			r.setData(data);
			r.setResponseComplete(true);
			client.publish(replyDestination, r);
		} catch (Exception e) {
			e.printStackTrace();
			//TODO log error and send error response
		}
	}

	
	void sendError(Client client, Destination replyDestination, String error){
		try {
			DataResponse r = new DataResponse();
			r.setError(new DataError(error));
			r.setResponseComplete(true);
			client.publish(replyDestination, r);
		} catch (Exception e) {
			e.printStackTrace();
			//TODO log error and send error response
		}
	}
	
}
