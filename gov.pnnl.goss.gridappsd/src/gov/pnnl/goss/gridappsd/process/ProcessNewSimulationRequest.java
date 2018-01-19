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
import gov.pnnl.goss.gridappsd.api.ServiceManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.dto.ApplicationObject;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.RequestSimulation;
import gov.pnnl.goss.gridappsd.dto.SimulationConfig;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pnnl.goss.core.DataResponse;

public class ProcessNewSimulationRequest {

	public ProcessNewSimulationRequest() {
	}

	public ProcessNewSimulationRequest(LogManager logManager) {
		this.logManager = logManager;
	}

	private volatile LogManager logManager;

	public void process(ConfigurationManager configurationManager,
			SimulationManager simulationManager, int simulationId,
			DataResponse event, Serializable message, AppManager appManager,
			ServiceManager serviceManager) {
		process(configurationManager, simulationManager, simulationId, message,
				SimulationConfig.DEFAULT_SIMULATION_BROKER_PORT, appManager,
				serviceManager);
	}

	public void process(ConfigurationManager configurationManager,
			SimulationManager simulationManager, int simulationId,
			Serializable message, int simulationPort, AppManager appManager,
			ServiceManager serviceManager) {

		try {

			String username = GridAppsDConstants.username;
			String source = this.getClass().getSimpleName();
			String simId = new Integer(simulationId).toString();
			String simulationLogTopic = GridAppsDConstants.topic_simulationLog
					+ simId;

			RequestSimulation config = RequestSimulation.parse(message
					.toString());
			config.simulation_config.setSimulation_broker_port(simulationPort);
			logManager.log(new LogMessage(this.getClass().getName(),
					new Integer(simulationId).toString(), new Date().getTime(),
					"Parsed config " + config, LogLevel.INFO,
					ProcessStatus.RUNNING, false), username,
					GridAppsDConstants.topic_simulationLog + simulationId);
			if (config == null || config.getPower_system_config() == null
					|| config.getSimulation_config() == null) {
				logManager.log(
						new LogMessage(this.getClass().getName(), new Integer(
								simulationId).toString(), new Date().getTime(),
								"No simulation file returned for request "
										+ config, LogLevel.ERROR,
								ProcessStatus.ERROR, false), username,
						GridAppsDConstants.topic_simulationLog + simulationId);
				throw new RuntimeException("Invalid configuration received");
			}

			// make request to configuration Manager to get power grid model
			// file locations and names
			logManager.log(new LogMessage(source, simId,
					 new Date().getTime(),
					"Creating simulation and power grid model files for simulation Id "
							+ simulationId,
							LogLevel.ERROR,
							ProcessStatus.ERROR,true), simulationLogTopic);

			File simulationFile = new File("test.sim");
			/*File simulationFile = configurationManager.getSimulationFile(
					simulationId, config);
			if (simulationFile == null) {
				logManager.log(
						new LogMessage(this.getClass().getName(), new Integer(
								simulationId).toString(), new Date().getTime(),
								"No simulation file returned for request "
										+ config, LogLevel.ERROR,
								ProcessStatus.ERROR, false), username,
						GridAppsDConstants.topic_platformLog);
				throw new Exception("No simulation file returned for request "
						+ config);
			}*/

			logManager
					.log(new LogMessage(source, simId,new Date().getTime(),
							"Simulation and power grid model files generated for simulation Id ",LogLevel.DEBUG, ProcessStatus.RUNNING,true),
							simulationLogTopic);

			
			
			// Start Apps and Services
			
			Map<String,Object> simulationContext = new HashMap<String,Object>();
			simulationContext.put("simulationId",simId);
			simulationContext.put("simulationHost","127.0.0.1");
			simulationContext.put("simulationPort",simulationPort);
			simulationContext.put("simulationDir",simulationFile.getParentFile());
			simulationContext.put("simulatorPath",serviceManager.getService(config.getSimulation_config().getSimulator()).getExecution_path());
		
			List<String> connectServiceInstanceIds = new ArrayList<String>();
			List<String> connectedAppInstanceIds = new ArrayList<String>();
		
			for (ApplicationObject app : config.application_config
					.getApplications()) {
				// TODO: Ask Tara: is simulation id same as request id
				List<String> prereqsList = appManager.getApp(app.getName())
						.getPrereqs();
				for (String prereqs : prereqsList) {
					String serviceInstanceId = serviceManager.startServiceForSimultion(prereqs, null,simulationContext);
					connectServiceInstanceIds.add(serviceInstanceId);
					logManager.log(new LogMessage(source, simId, new Date().getTime(),"Started "
							+ app.getName() + " with instance id "
							+ serviceInstanceId,LogLevel.DEBUG, ProcessStatus.RUNNING, true),
							GridAppsDConstants.topic_simulationLog
									+ simulationId);
				}

				String appInstanceId = appManager.startAppForSimultion(app
						.getName(), app.getConfig_string(), simulationContext);
				connectedAppInstanceIds.add(appInstanceId);
				logManager.log(
						new LogMessage(source, simId, new Date().getTime(),"Started "
								+ app.getName() + " with instance id "
								+ appInstanceId, LogLevel.DEBUG, ProcessStatus.RUNNING, true),
						GridAppsDConstants.topic_simulationLog + simulationId);

			}
			
			simulationContext.put("connectedServiceInstanceIds",connectServiceInstanceIds);
			simulationContext.put("connectedAppInstanceIds",connectedAppInstanceIds);

			// start simulation
			logManager.log(new LogMessage(source, simId,new Date().getTime(),
					"Starting simulation for id " + simulationId,LogLevel.DEBUG, ProcessStatus.RUNNING,true),
					simulationLogTopic);
			simulationManager.startSimulation(simulationId, config.getSimulation_config(),simulationContext);
			logManager.log(new LogMessage(source, simId,new Date().getTime(),
					"Started simulation for id " + simulationId,LogLevel.DEBUG, ProcessStatus.RUNNING,true),
					simulationLogTopic);

		} catch (Exception e) {
			e.printStackTrace();
			try {
				logManager.log(
						new LogMessage(this.getClass().getName(), new Integer(
								simulationId).toString(), new Date().getTime(),
								"Process Initialization error: "
										+ e.getMessage(), LogLevel.ERROR,
								ProcessStatus.ERROR, false),
						GridAppsDConstants.username,
						GridAppsDConstants.topic_platformLog);

			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}
}
