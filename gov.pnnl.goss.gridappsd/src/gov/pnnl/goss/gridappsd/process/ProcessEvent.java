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
import gov.pnnl.goss.gridappsd.dto.AppInfo;
import gov.pnnl.goss.gridappsd.dto.AppInstance;
import gov.pnnl.goss.gridappsd.dto.ConfigurationRequest;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.RequestPlatformStatus;
import gov.pnnl.goss.gridappsd.dto.RequestSimulation;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.PlatformStatus;
import gov.pnnl.goss.gridappsd.dto.ServiceInfo;
import gov.pnnl.goss.gridappsd.dto.ServiceInstance;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;

import javax.jms.Destination;

import pnnl.goss.core.Client;
import pnnl.goss.core.DataError;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.core.Response;

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


	@Override
	public void onMessage(Serializable message) {

		DataResponse event = (DataResponse)message;
		String username  = GridAppsDConstants.username;

		int processId = ProcessManagerImpl.generateProcessId();
		this.debug(processId, "Received message: "+ event.getData() +" on topic "+event.getDestination()+" from user "+username);


		try{ 

			if(event.getDestination().contains(GridAppsDConstants.topic_requestSimulation )){
				client.publish(event.getReplyDestination(), processId);
				newSimulationProcess.process(configurationManager, simulationManager, processId, event, event.getData(), appManager, serviceManager);

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

				Response r = dataManager.processDataRequest(request, type, processId, configurationManager.getConfigurationProperty(GridAppsDConstants.GRIDAPPSD_TEMP_PATH));
				client.publish(event.getReplyDestination(), r);


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
						configurationManager.generateConfiguration(configRequest.getConfigurationType(), configRequest.getParameters(), out);
					} catch (Exception e) {
						e.printStackTrace();
						this.error(processId, e.getStackTrace().toString());
						sendError(client, event.getReplyDestination(), e.getMessage(), processId);
					}
					String result = sw.toString();
					sendData(client, event.getReplyDestination(), result);

				} else {
					this.error(processId, "No valid configuration request received, request: "+request);
					sendError(client, event.getReplyDestination(), "No valid configuration request received, request: "+request, processId);
				}


			} else if(event.getDestination().contains("log")){

				logManager.log(LogMessage.parse(message.toString()), username, null);

			}
			else if(event.getDestination().contains(GridAppsDConstants.topic_requestPlatformStatus)){
				
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
			this.error(processId, e.getMessage());
		}
	}


	private void sendData(Client client, Destination replyDestination, Serializable data){
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


	private void sendError(Client client, Destination replyDestination, String error, int processId){
		try {
			DataResponse r = new DataResponse();
			r.setError(new DataError(error));
			r.setResponseComplete(true);
			client.publish(replyDestination, r);
		} catch (Exception e) {
			e.printStackTrace();
			this.error(processId, e.getMessage());
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
