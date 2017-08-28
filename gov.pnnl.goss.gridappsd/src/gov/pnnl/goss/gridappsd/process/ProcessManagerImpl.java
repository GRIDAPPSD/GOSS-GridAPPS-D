/*******************************************************************************
 * Copyright � 2017, Battelle Memorial Institute All rights reserved.
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
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS �AS IS� AND ANY 
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

import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.ProcessManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.api.StatusReporter;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;

import java.io.Serializable;
import java.util.Date;
import java.util.Random;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;

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
	private volatile StatusReporter statusReporter;
	
	@ServiceDependency
	private volatile LogManager logManager;
	
	ProcessNewSimulationRequest newSimulationProcess = new ProcessNewSimulationRequest();

	public ProcessManagerImpl(){}
	public ProcessManagerImpl(ClientFactory clientFactory, 
			ConfigurationManager configurationManager,
			SimulationManager simulationManager,
			StatusReporter statusReporter,
			LogManager logManager, 
			ProcessNewSimulationRequest newSimulationProcess){
		this.clientFactory = clientFactory;
		this.configurationManager = configurationManager;
		this.simulationManager = simulationManager;
		this.statusReporter = statusReporter;
		this.logManager = logManager;
		this.newSimulationProcess = newSimulationProcess;
	}

	
	
	@Start
	public void start(){
		
		LogMessage logMessageObj = new LogMessage();
		
		try{
			
			
			logMessageObj.setLog_level("debug");
			logMessageObj.setProcess_id(this.getClass().getName());
			logMessageObj.setProcess_status("running");
			logMessageObj.setStoreToDB(true);
			
			logMessageObj.setTimestamp(GridAppsDConstants.GRIDAPPSD_DATE_FORMAT.format(new Date()));
			logMessageObj.setLog_message("Starting "+this.getClass().getName());
			logManager.log(logMessageObj);
			
			Credentials credentials = new UsernamePasswordCredentials(
					GridAppsDConstants.username, GridAppsDConstants.password);
			Client client = clientFactory.create(PROTOCOL.STOMP,credentials);
			client.subscribe(GridAppsDConstants.topic_process_prefix+".>", new GossResponseEvent() {
				
				@Override
				public void onMessage(Serializable message) {
					
					DataResponse event = (DataResponse)message;
					
					logMessageObj.setTimestamp(GridAppsDConstants.GRIDAPPSD_DATE_FORMAT.format(new Date()));
					logMessageObj.setLog_message("Recevied message: "+ event.getData() +" on topic "+event.getDestination());
					logManager.log(logMessageObj);
					
					//TODO: create registry mapping between request topics and request handlers.
					if(event.getDestination().contains(GridAppsDConstants.topic_requestSimulation )){
						//generate simulation id and reply to event's reply destination.
						int simulationId = generateSimulationId();
						client.publish(event.getReplyDestination(), simulationId);
						newSimulationProcess.process(configurationManager, simulationManager, statusReporter, simulationId, event, message);
					}
					else if(event.getDestination().contains(GridAppsDConstants.topic_log_prefix)){
						logManager.log(message.toString());
					}
					//case GridAppsDConstants.topic_requestData : processDataRequest(); break;
					//case GridAppsDConstants.topic_requestSimulationStatus : processSimulationStatusRequest(); break;
					
					
				}
			});
		}
		catch(Exception e){
			e.printStackTrace();
			logMessageObj.setTimestamp(GridAppsDConstants.GRIDAPPSD_DATE_FORMAT.format(new Date()));
			logMessageObj.setLog_level("error");
			logMessageObj.setLog_message(e.getMessage());
			logManager.log(logMessageObj);
		}
		
	}
	
	
	public void runProcess(){
		
	}
	
	
	
	/**
	 * Generates and returns simulation id
	 * @return simulation id
	 */
	static int generateSimulationId(){
		/*
		 * TODO: 
		 * Get the latest simulation id from database and return +1 
		 * Store the new id in database
		 */
		return Math.abs(new Random().nextInt());
	}
	


	
}
