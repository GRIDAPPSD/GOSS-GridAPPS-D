/*******************************************************************************
 * Copyright 2017, Battelle Memorial Institute All rights reserved.
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
package gov.pnnl.goss.gridappsd.simulation;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.pnnl.goss.gridappsd.api.AppManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.ServiceManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.ServiceInfo;
import gov.pnnl.goss.gridappsd.dto.SimulationConfig;
import gov.pnnl.goss.gridappsd.dto.SimulationContext;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.security.SecurityConfig;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.server.ServerControl;


/**
 * This represents Internal Function 405 Simulation Control Manager.
 * This is the management function that controls the running/execution of the Distribution Simulator (401).
 * @author shar064
 */

@Component
public class SimulationManagerImpl implements SimulationManager{

	private static Logger log = LoggerFactory.getLogger(SimulationManagerImpl.class);
	final static int MAX_INIT_ATTEMPTS = 50;

	Client client = null;

	@ServiceDependency
	private volatile ClientFactory clientFactory;

	@ServiceDependency
	ServerControl serverControl;

	@ServiceDependency
	private volatile ServiceManager serviceManager;
	
	@ServiceDependency
	private volatile AppManager appManager;
	
	@ServiceDependency
    private volatile SecurityConfig securityConfig;
	
	@ServiceDependency
	LogManager logManager;
	
	private Map<String, SimulationContext> simContexts  = new HashMap<String, SimulationContext>();
//	private Map<String, SimulationProcess> simProcesses = new HashMap<String, SimulationProcess>();
	public SimulationManagerImpl(){ }


	public SimulationManagerImpl(ClientFactory clientFactory, ServerControl serverControl,
			LogManager logManager) {
		this.clientFactory = clientFactory;
		this.serverControl = serverControl;
		this.logManager = logManager;
		//this.configurationManager = configurationManager;
	}
	@Start
	public void start() throws Exception{
		
		Credentials credentials = new UsernamePasswordCredentials(
				securityConfig.getManagerUser(), securityConfig.getManagerPassword());
		client = clientFactory.create(PROTOCOL.STOMP,credentials);
		client.publish("goss.gridappsd.log.platform", new LogMessage(this.getClass().getSimpleName(),
				null,
				new Date().getTime(), 
				this.getClass().getSimpleName()+" Started", 
				LogLevel.INFO, 
				ProcessStatus.STARTED, 
				true).toString());
	}

	/**
	 * This method is called by Process Manager to start a simulation
	 * @param simulationId
	 * @param simulationFile
	 */
	@Override
	public void startSimulation(int simulationId, SimulationConfig simulationConfig, SimulationContext simContext,  Map<String, Object> simulationContext){
		//TODO: remove simulationContext parameter after refactoring service manager

			try {
				logManager.log(new LogMessage(this.getClass().getSimpleName(),
						Integer.toString(simulationId), 
						new Date().getTime(), 
						"Starting simulation "+simulationId, 
						LogLevel.INFO, 
						ProcessStatus.STARTING, 
						true),simContext.getSimulationUser(),
						GridAppsDConstants.topic_platformLog);
			} catch (Exception e2) {
				log.warn("Error while reporting status "+e2.getMessage());
			}
			
			simContexts.put(simContext.getSimulationId(), simContext);
			
			startServiceDependencies(simulationConfig, simContext, simulationContext);
			
			SimulationProcess simProc = new SimulationProcess(simContext, serviceManager, 
						simulationConfig, simulationId, logManager, appManager, client, securityConfig);
//			simProcesses.put(simContext.getSimulationId(), simProc);
			simProc.start();
	}
	@Override
	public void pauseSimulation(String simulationId){
		//NOt implementing yet
		//client.publish(GridAppsDConstants.topic_FNCS_input, "{\"command\": \"pause\"}");
	}
	@Override
	public void resumeSimulation(String simulationId){
		//Not implementing yet
		//client.publish(GridAppsDConstants.topic_FNCS_input, "{\"command\": \"resume\"}");

	}
	@Override
	public void endSimulation(String simulationId){
		client.publish(GridAppsDConstants.topic_FNCS_input, "{\"command\": \"stop\"}");

	}
	
	
	public void removeSimulation(String simulationId){
		endSimulation(simulationId);
	}


	/*private String getPath(String key){
		String path = configurationManager.getConfigurationProperty(key);
		if(path==null){
			log.warn("Configuration property not found, defaulting to .: "+key);
			path = ".";
		}
		return path;
	}*/

	
	public Map<String, SimulationContext> getSimContexts() {
		return simContexts;
	}

	@Override
	public SimulationContext getSimulationContextForId(String simulationId){
		return this.simContexts.get(simulationId);
	}
	
	@Override
	public void startServiceDependencies(SimulationConfig simulationConfig, SimulationContext simContext, Map<String, Object> simulationContext){
		ServiceInfo simulationServiceInfo = serviceManager.getService(simulationConfig.simulator);
		List<String> serviceDependencies = simulationServiceInfo.getService_dependencies();
		for(String service : serviceDependencies) {
			String serviceInstanceId = serviceManager.startServiceForSimultion(service, null, simulationContext);
			if(serviceInstanceId!=null)
				simContext.addServiceInstanceIds(serviceInstanceId);
		}
		
	}
	
	
}
