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
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.ProcessManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.api.StatusReporter;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;

import java.awt.GridBagConstraints;
import java.io.Serializable;
import java.util.Date;
import java.util.Hashtable;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.mockito.internal.matchers.InstanceOf;

import com.google.gson.JsonSyntaxException;

import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.GossResponseEvent;



/**
 * Process Manager subscribe to all the requests coming from Applications
 * and forward them to appropriate managers.
 * @author shar064
 *
 */
@Component
public class ProcessManagerImpl implements ProcessManager {
	
	@ServiceDependency
	private volatile ClientFactory clientFactory;
	
	@ServiceDependency
	private volatile ConfigurationManager configurationManager;
	
	@ServiceDependency
	private volatile SimulationManager simulationManager;
	
	@ServiceDependency
	private volatile AppManager appManager;
	
	@ServiceDependency
	private volatile StatusReporter statusReporter;
	
	@ServiceDependency
	private volatile LogManager logManager;
	
	ProcessNewSimulationRequest newSimulationProcess = null;
	
	private Hashtable<Integer, AtomicInteger> simulationPorts = new Hashtable<Integer, AtomicInteger>();
	
	private Random randPort = new Random();

	public ProcessManagerImpl(){}
	public ProcessManagerImpl(ClientFactory clientFactory, 
			ConfigurationManager configurationManager,
			SimulationManager simulationManager,
			StatusReporter statusReporter,
			LogManager logManager, 
			AppManager appManager,
			ProcessNewSimulationRequest newSimulationProcess){
		this.clientFactory = clientFactory;
		this.configurationManager = configurationManager;
		this.simulationManager = simulationManager;
		this.statusReporter = statusReporter;
		this.appManager = appManager;
		this.newSimulationProcess = newSimulationProcess;
	}

	
	
	@Start
	public void start(){
		
		LogMessage logMessageObj = new LogMessage();
		
		try{
		
			Credentials credentials = new UsernamePasswordCredentials(
					GridAppsDConstants.username, GridAppsDConstants.password);
			Client client = clientFactory.create(PROTOCOL.STOMP,credentials);
		
			logMessageObj.setLogLevel(LogLevel.DEBUG);
			logMessageObj.setSource(this.getClass().getName());
			logMessageObj.setProcessStatus(ProcessStatus.RUNNING);
			logMessageObj.setStoreToDb(true);
			logMessageObj.setLogMessage("Starting "+ this.getClass().getName());
			client.publish(GridAppsDConstants.topic_platformLog, logMessageObj);
			
			if(newSimulationProcess==null)
				newSimulationProcess = new ProcessNewSimulationRequest(); 
			
			
			
			logMessageObj.setTimestamp(new Date().getTime());
			logMessageObj.setLogMessage("Starting "+this.getClass().getName());
			client.publish(GridAppsDConstants.topic_platformLog, logMessageObj);
			
			
			client.subscribe(GridAppsDConstants.topic_prefix+".>", new GossResponseEvent() {
				@Override
				public void onMessage(Serializable message) {
					
					
					DataResponse event = (DataResponse)message;
					//TODO:Get username from message's metadata e.g. event.getUserName()
					String username  = GridAppsDConstants.username;
					int processId = generateProcessId();
					
					logMessageObj.setTimestamp(new Date().getTime());
					logMessageObj.setLogMessage("Received message: "+ event.getData() +" on topic "+event.getDestination()+" from user "+username);
					client.publish(GridAppsDConstants.topic_platformLog, logMessageObj);
					
					
					
					//TODO: create registry mapping between request topics and request handlers.
					if(event.getDestination().contains(GridAppsDConstants.topic_requestSimulation )){
						//generate simulation id and reply to event's reply destination.
						
						try {
							int simPort = assignSimulationPort(processId);
							client.publish(event.getReplyDestination(), processId);
							newSimulationProcess.process(configurationManager, simulationManager, processId, message, simPort);
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
						
						String outputTopics = String.join(".", 
								GridAppsDConstants.topic_responseData,
								String.valueOf(processId),
								"output");
						
						String logTopic = String.join(".", 
								GridAppsDConstants.topic_responseData,
								String.valueOf(processId),
								"log");
						
						logManager.get(LogMessage.parse(message.toString()), outputTopics, logTopic);
						//TODO: catch JsonSyntaxException and call  get ModelDataManager or SimulationOutputDataManager
						
					} else if(event.getDestination().contains("log")){
						logManager.log(LogMessage.parse(message.toString()), username);
					}
					
					//case GridAppsDConstants.topic_requestData : processDataRequest(); break;
					//case GridAppsDConstants.topic_requestSimulationStatus : processSimulationStatusRequest(); break;
				}
			});
		}
		catch(Exception e){
			e.printStackTrace();
			logMessageObj.setTimestamp(new Date().getTime());
			logMessageObj.setLogLevel(LogLevel.ERROR);
			logMessageObj.setLogMessage(e.getMessage());
			logManager.log(logMessageObj, GridAppsDConstants.username);
		}
		
	}
	
	/**
	 * Generates and returns process id
	 * @return process id
	 */
	static int generateProcessId(){
		return Math.abs(new Random().nextInt());
	}
	
	public int assignSimulationPort(int simulationId) throws Exception {
		Integer simIdKey = new Integer(simulationId);
		if (!simulationPorts.containsKey(simIdKey)) {
			int tempPort = 49152 + randPort.nextInt(16384);
			AtomicInteger tempPortObj = new AtomicInteger(tempPort);
			while (simulationPorts.containsValue(tempPortObj)) {
				int newTempPort = 49152 + randPort.nextInt(16384);
				tempPortObj.set(newTempPort);
			}
			simulationPorts.put(simIdKey, tempPortObj);
			return tempPortObj.get();
			//TODO: test host:port is available
		} else {
			throw new Exception("The simulation id already exists. This indicates that the simulation id is part of a"
					+ "simulation in progress.");
		}
	}
	
}
