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
import gov.pnnl.goss.gridappsd.configuration.DSSAllConfigurationHandler;
import gov.pnnl.goss.gridappsd.configuration.GLDAllConfigurationHandler;
import gov.pnnl.goss.gridappsd.dto.AppInfo;
import gov.pnnl.goss.gridappsd.dto.ApplicationObject;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.ModelCreationConfig;
import gov.pnnl.goss.gridappsd.dto.RequestSimulation;
import gov.pnnl.goss.gridappsd.dto.ServiceConfig;
import gov.pnnl.goss.gridappsd.dto.SimulationConfig;
import gov.pnnl.goss.gridappsd.dto.SimulationContext;
import gov.pnnl.goss.gridappsd.dto.SimulationOutput;
import gov.pnnl.goss.gridappsd.dto.SimulationOutputObject;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
			DataResponse event, RequestSimulation simRequest, AppManager appManager,
			ServiceManager serviceManager, TestManager testManager,
			DataManager dataManager) {
		process(configurationManager, simulationManager, simulationId, simRequest,
				SimulationConfig.DEFAULT_SIMULATION_BROKER_PORT, appManager,
				serviceManager, testManager, dataManager);
	}

	public void process(ConfigurationManager configurationManager,
			SimulationManager simulationManager, int simulationId,
			RequestSimulation simRequest, int simulationPort, AppManager appManager,
			ServiceManager serviceManager, TestManager testManager,DataManager dataManager) {

		try {

			String username = GridAppsDConstants.username;
			String source = this.getClass().getSimpleName();
			String simId = new Integer(simulationId).toString();
			String simulationLogTopic = GridAppsDConstants.topic_simulationLog
					+ simId;
			
			simRequest.simulation_config.setSimulation_broker_port(simulationPort);
			logManager.log(new LogMessage(this.getClass().getName(),
					new Integer(simulationId).toString(), new Date().getTime(),
					"Parsed config " + simRequest, LogLevel.INFO,
					ProcessStatus.RUNNING, false), username,
					GridAppsDConstants.topic_simulationLog + simulationId);
			if (simRequest == null || simRequest.getPower_system_config() == null
					|| simRequest.getSimulation_config() == null) {
				logManager.log(
						new LogMessage(this.getClass().getName(), new Integer(
								simulationId).toString(), new Date().getTime(),
								"No simulation file returned for request "
										+ simRequest, LogLevel.INFO,
								ProcessStatus.RUNNING, false), username,
						GridAppsDConstants.topic_simulationLog + simulationId);
				throw new RuntimeException("Invalid configuration received");
			}

			// make request to configuration Manager to get power grid model
			// file locations and names
			logManager.log(new LogMessage(source, simId,
					 new Date().getTime(),
					"Creating simulation and power grid model files for simulation Id "
							+ simulationId,
							LogLevel.INFO,
							ProcessStatus.RUNNING,true), simulationLogTopic);


//			StringWriter simulationConfigDirOut = new StringWriter();
//			File simulationFile = configurationManager.getSimulationFile(
//					simulationId, config);
//			String simulationConfigDir = simulationConfigDirOut.toString();
			String simulationConfigDir = configurationManager.getConfigurationProperty(GridAppsDConstants.GRIDAPPSD_TEMP_PATH);
			if (simulationConfigDir == null || simulationConfigDir.trim().length()==0) {
				logManager.log(
						new LogMessage(this.getClass().getName(), new Integer(
								simulationId).toString(), new Date().getTime(),
								"No simulation file returned for request "
										+ simRequest, LogLevel.ERROR,
								ProcessStatus.ERROR, false), username,
						GridAppsDConstants.topic_platformLog);
				throw new Exception("No simulation file returned for request "
						+ simRequest);
			}
			if(!simulationConfigDir.endsWith(File.separator)){
				simulationConfigDir = simulationConfigDir+File.separator;
			}
			simulationConfigDir = simulationConfigDir+simulationId+File.separator;
			File tempDataPathDir = new File(simulationConfigDir);
			if(!tempDataPathDir.exists()){
				tempDataPathDir.mkdirs();
			}


			SimulationContext simContext = new SimulationContext();
			simContext.setRequest(simRequest);
			simContext.simulationId = simId;
			simContext.simulationPort = simulationPort;
			simContext.simulationDir = tempDataPathDir.getAbsolutePath();
			simContext.startupFile = tempDataPathDir.getAbsolutePath()+File.separator+"model_startup.glm";
			try{
				simContext.simulatorPath = serviceManager.getService(simRequest.getSimulation_config().getSimulator()).getExecution_path();
			}catch(NullPointerException e){
				if(serviceManager.getService(simRequest.getSimulation_config().getSimulator()) == null){
					logManager.log(new LogMessage(this.getClass().getSimpleName(),
							simId,
							new Date().getTime(),
							"Cannot find service with id ="+simRequest.getSimulation_config().getSimulator(),
							LogLevel.DEBUG, ProcessStatus.RUNNING, true), GridAppsDConstants.topic_simulationLog+simulationId);
				}else if(serviceManager.getService(simRequest.getSimulation_config().getSimulator()).getExecution_path() == null){
					logManager.log(new LogMessage(this.getClass().getSimpleName(),
							simId,
							new Date().getTime(),
							"Cannot find execution path for service ="+simRequest.getSimulation_config().getSimulator(),
							LogLevel.DEBUG, ProcessStatus.RUNNING, true), GridAppsDConstants.topic_simulationLog+simulationId);
				}
				e.printStackTrace();
			}




			String simulator = simRequest.getSimulation_config().getSimulator();
			//generate config files for requested simulator
			//if requested simulator is opendss
			if(simulator.equalsIgnoreCase(DSSAllConfigurationHandler.CONFIGTARGET)){
				Properties simulationParams = generateSimulationParameters(simRequest);
				simulationParams.put(DSSAllConfigurationHandler.SIMULATIONID, simId);
				simulationParams.put(DSSAllConfigurationHandler.DIRECTORY, tempDataPathDir.getAbsolutePath());
				configurationManager.generateConfiguration(DSSAllConfigurationHandler.TYPENAME, simulationParams, new PrintWriter(new StringWriter()), new Integer(simulationId).toString(), username);
			} else { //otherwise use gridlabd
				Properties simulationParams = generateSimulationParameters(simRequest);
				simulationParams.put(GLDAllConfigurationHandler.SIMULATIONID, simId);
				simulationParams.put(GLDAllConfigurationHandler.DIRECTORY, tempDataPathDir.getAbsolutePath());
				configurationManager.generateConfiguration(GLDAllConfigurationHandler.TYPENAME, simulationParams, new PrintWriter(new StringWriter()), new Integer(simulationId).toString(), username);
			}
			
			logManager
					.log(new LogMessage(source, simId,new Date().getTime(),
							"Simulation and power grid model files generated for simulation Id ",LogLevel.DEBUG, ProcessStatus.RUNNING,true),
							simulationLogTopic);


			// Start Apps and Services

			Map<String,Object> simulationContext = new HashMap<String,Object>();
			simulationContext.put("request",simRequest);
			simulationContext.put("simulationId",simId);
			simulationContext.put("simulationHost","127.0.0.1");
			simulationContext.put("simulationPort",simulationPort);
			simulationContext.put("simulationDir",simulationConfigDir);
			simulationContext.put("simulationFile",tempDataPathDir.getAbsolutePath()+File.separator+"model_startup.glm");
			try{
				simulationContext.put("simulatorPath",serviceManager.getService(simRequest.getSimulation_config().getSimulator()).getExecution_path());
			}catch(NullPointerException e){
				if(serviceManager.getService(simRequest.getSimulation_config().getSimulator()) == null){
					logManager.log(new LogMessage(this.getClass().getSimpleName(),
							simId,
							new Date().getTime(),
							"Cannot find service with id ="+simRequest.getSimulation_config().getSimulator(),
							LogLevel.WARN, ProcessStatus.RUNNING, true), GridAppsDConstants.topic_simulationLog+simulationId);
				}else if(serviceManager.getService(simRequest.getSimulation_config().getSimulator()).getExecution_path() == null){
					logManager.log(new LogMessage(this.getClass().getSimpleName(),
							simId,
							new Date().getTime(),
							"Cannot find execution path for service ="+simRequest.getSimulation_config().getSimulator(),
							LogLevel.DEBUG, ProcessStatus.RUNNING, true), GridAppsDConstants.topic_simulationLog+simulationId);
				}
				e.printStackTrace();
			}

			List<String> connectServiceInstanceIds = new ArrayList<String>();
			List<String> connectServiceIds = new ArrayList<String>();
			List<String> connectedAppInstanceIds = new ArrayList<String>();
			
			if (simRequest.service_configs == null) {
				logManager.log(new LogMessage(this.getClass().getSimpleName(),
						simId,
						new Date().getTime(),
						"No services found in request  ="+simRequest.getSimulation_config().getSimulator(),
						LogLevel.WARN, ProcessStatus.RUNNING, true), GridAppsDConstants.topic_simulationLog+simulationId);
			}
			else{
				for(ServiceConfig serviceConfig : simRequest.service_configs){
					String serviceInstanceId = serviceManager.startServiceForSimultion(serviceConfig.getId(), null, simulationContext);
					connectServiceInstanceIds.add(serviceInstanceId);
					connectServiceIds.add(serviceConfig.getId());
				}
			}
			
			

			if (simRequest.application_config == null) {
				logManager.log(new LogMessage(this.getClass().getSimpleName(),
						simId,
						new Date().getTime(),
						"No applications found in request  ="+simRequest.getSimulation_config().getSimulator(),
						LogLevel.WARN, ProcessStatus.RUNNING, true), GridAppsDConstants.topic_simulationLog+simulationId);
			}
			else {
				for (ApplicationObject app : simRequest.application_config
						.getApplications()) {
					AppInfo appInfo = appManager.getApp(app.getName());
					if(appInfo==null) {
						logManager.log(new LogMessage(this.getClass().getSimpleName(),
								String.valueOf(simulationId), new Date().getTime(),
								"Cannot start application "+ app.getName() +". Application not available",
								LogLevel.ERROR, ProcessStatus.ERROR, true), GridAppsDConstants.topic_simulationLog
								+ simulationId);
						throw new RuntimeException("Cannot start application "+ app.getName() +". Application not available");

					}



					List<String> prereqsList = appManager.getApp(app.getName())
							.getPrereqs();
					for (String prereqs : prereqsList) {
						if(!connectServiceIds.contains(prereqs)){
							String serviceInstanceId = serviceManager.startServiceForSimultion(prereqs, null,simulationContext);
							connectServiceInstanceIds.add(serviceInstanceId);
							logManager.log(new LogMessage(source, simId, new Date().getTime(),"Started "
									+ prereqs + " with instance id "
									+ serviceInstanceId,LogLevel.DEBUG, ProcessStatus.RUNNING, true),
									GridAppsDConstants.topic_simulationLog
											+ simulationId);
						}
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
			}

			simulationContext.put("connectedServiceInstanceIds",connectServiceInstanceIds);
			simulationContext.put("connectedAppInstanceIds",connectedAppInstanceIds);
			simContext.serviceInstanceIds = connectServiceInstanceIds;
			simContext.appInstanceIds = connectedAppInstanceIds;
			
			dataManager.processDataRequest(simContext, "timeseries", simulationId, null, username);
			
			// start simulation
			logManager.log(new LogMessage(source, simId,new Date().getTime(),
					"Starting simulation for id " + simulationId,LogLevel.DEBUG, ProcessStatus.RUNNING,true),
					simulationLogTopic);
			simulationManager.startSimulation(simulationId, simRequest.getSimulation_config(),simContext, simulationContext);
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


	Properties generateSimulationParameters(RequestSimulation requestSimulation){
		Properties params = new Properties();

		//TODO where to get feeder id?
		params.put(GLDAllConfigurationHandler.MODELID, requestSimulation.power_system_config.Line_name);

		ModelCreationConfig modelConfig = requestSimulation.getSimulation_config().model_creation_config;
		double zFraction = modelConfig.z_fraction;
		double iFraction = modelConfig.i_fraction;
		double pFraction = modelConfig.p_fraction;



		params.put(GLDAllConfigurationHandler.ZFRACTION, new Double(zFraction).toString());
		params.put(GLDAllConfigurationHandler.IFRACTION, new Double(iFraction).toString());
		params.put(GLDAllConfigurationHandler.PFRACTION, new Double(pFraction).toString());
		params.put(GLDAllConfigurationHandler.LOADSCALINGFACTOR, new Double(modelConfig.load_scaling_factor).toString());
		params.put(GLDAllConfigurationHandler.RANDOMIZEFRACTIONS, modelConfig.randomize_zipload_fractions);
		params.put(GLDAllConfigurationHandler.USEHOUSES, modelConfig.use_houses);

		params.put(GLDAllConfigurationHandler.SCHEDULENAME, modelConfig.schedule_name);
		params.put(GLDAllConfigurationHandler.SIMULATIONNAME, requestSimulation.getSimulation_config().simulation_name);
		params.put(GLDAllConfigurationHandler.SOLVERMETHOD, requestSimulation.getSimulation_config().power_flow_solver_method);

		params.put(GLDAllConfigurationHandler.SIMULATIONBROKERHOST, requestSimulation.getSimulation_config().getSimulation_broker_location());
		params.put(GLDAllConfigurationHandler.SIMULATIONBROKERPORT, new Integer(requestSimulation.getSimulation_config().getSimulation_broker_port()).toString());

		params.put(GLDAllConfigurationHandler.SIMULATIONSTARTTIME, requestSimulation.getSimulation_config().start_time);
		params.put(GLDAllConfigurationHandler.SIMULATIONDURATION, new Integer(requestSimulation.getSimulation_config().duration).toString());

		return params;
	}


	/**
	 * Create configfile.json string, should look something like
	 *   "{\"swt_g9343_48332_sw\": [\"status\"],\"swt_l5397_48332_sw\": [\"status\"],\"swt_a8869_48332_sw\": [\"status\"]}";
	 * @param simulationOutput
	 * @return
	 */
	protected void generateConfigFile(File configFile , SimulationOutput simulationOutput){
		StringBuffer configStr = new StringBuffer();
		boolean isFirst = true;
		configStr.append("{");
		for(SimulationOutputObject obj: simulationOutput.getOutputObjects()){
			if(!isFirst){
				configStr.append(",");
			}
			isFirst = false;

			configStr.append("\""+obj.getName()+"\": [");
			boolean isFirstProp = true;
			for(String property: obj.getProperties()){
				if(!isFirstProp){
					configStr.append(",");
				}
				isFirstProp = false;
				configStr.append("\""+property+"\"");
			}
			configStr.append("]");
		}

		configStr.append("}");

		FileWriter fOut;
		try {
			fOut  = new FileWriter(configFile);
			fOut.write(configStr.toString());

		fOut.flush();
		fOut.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
