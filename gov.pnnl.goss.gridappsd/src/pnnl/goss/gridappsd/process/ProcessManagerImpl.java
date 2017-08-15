/*******************************************************************************
 * Copyright © 2017, Battelle Memorial Institute All rights reserved.
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
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS “AS IS” AND ANY 
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
package pnnl.goss.gridappsd.process;

import java.io.File;
import java.io.Serializable;
import java.util.Random;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.ProcessManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.api.StatusReporter;
import gov.pnnl.goss.gridappsd.dto.RequestSimulation;
import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.gridappsd.utils.GridAppsDConstants;

/**
 * Process Manager subscribe to all the requests coming from Applications
 * and forward them to appropriate managers.
 * @author shar064
 *
 */
@Component
public class ProcessManagerImpl implements ProcessManager {
		
	private static Logger log = LoggerFactory.getLogger(ProcessManagerImpl.class);
	
	@ServiceDependency
	private volatile ClientFactory clientFactory;
	
	@ServiceDependency
	private volatile ConfigurationManager configurationManager;
	
	@ServiceDependency
	private volatile SimulationManager simulationManager;
	
	@ServiceDependency
	private volatile StatusReporter statusReporter;
	
	@Start
	public void start(){
		try{
			log.debug("Starting "+this.getClass().getName());
			
			Credentials credentials = new UsernamePasswordCredentials(
					GridAppsDConstants.username, GridAppsDConstants.password);
			Client client = clientFactory.create(PROTOCOL.STOMP,credentials);
			
			//TODO: subscribe to GridAppsDConstants.topic_request_prefix+/* instead of GridAppsDConstants.topic_requestSimulation
			client.subscribe(GridAppsDConstants.topic_requestSimulation, new GossResponseEvent() {
				
				@Override
				public void onMessage(Serializable message) {
					log.debug("Process manager received message ");
					DataResponse event = (DataResponse)message;
					
					statusReporter.reportStatus(String.format("Got new message in %s", getClass().getName()));
					//TODO: create registry mapping between request topics and request handlers.
					switch(event.getDestination().replace("/queue/", "")){
						case GridAppsDConstants.topic_requestSimulation : {
							log.debug("Received simulation request: "+ event.getData());
							
							//generate simulation id and reply to event's reply destination.
							int simulationId = generateSimulationId();
							client.publish(event.getReplyDestination(), simulationId);
							try{
								// TODO: validate simulation request json and create PowerSystemConfig and SimulationConfig dto objects to work with internally.
								Gson  gson = new Gson();
									
								RequestSimulation config = gson.fromJson(message.toString(), RequestSimulation.class);
								log.info("Parsed config "+config);
								if(config==null || config.getPower_system_config()==null || config.getSimulation_config()==null){
									throw new RuntimeException("Invalid configuration received");
								}
								
								
								
									
								
								//make request to configuration Manager to get power grid model file locations and names
								log.debug("Creating simulation and power grid model files for simulation Id "+ simulationId);
								File simulationFile = configurationManager.getSimulationFile(simulationId, config);
								if(simulationFile==null){
									throw new Exception("No simulation file returned for request "+config);
								}
									
									
								log.debug("Simulation and power grid model files generated for simulation Id "+ simulationId);
								
								//start simulation
								log.debug("Starting simulation for id "+ simulationId);
								simulationManager.startSimulation(simulationId, simulationFile, config.getSimulation_config());
								log.debug("Starting simulation for id "+ simulationId);
									
		//								new ProcessSimulationRequest().process(event, client, configurationManager, simulationManager); break;
							}catch (Exception e){
								e.printStackTrace();
								try {
									statusReporter.reportStatus(GridAppsDConstants.topic_simulationStatus+simulationId, "Process Initialization error: "+e.getMessage());
									log.error("Process Initialization error",e);
								} catch (Exception e1) {
									e1.printStackTrace();
								}
							}
						}
						//case GridAppsDConstants.topic_requestData : processDataRequest(); break;
						//case GridAppsDConstants.topic_requestSimulationStatus : processSimulationStatusRequest(); break;
					}
					
				}
			});
		}
		catch(Exception e){
			log.error("Error in process manager",e);
		}
		
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
