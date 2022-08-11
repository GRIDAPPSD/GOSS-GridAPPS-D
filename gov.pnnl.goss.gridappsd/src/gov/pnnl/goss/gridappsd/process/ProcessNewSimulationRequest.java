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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.gson.Gson;

import gov.pnnl.goss.gridappsd.api.AppManager;
import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.ServiceManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.api.TestManager;
import gov.pnnl.goss.gridappsd.configuration.DSSAllConfigurationHandler;
import gov.pnnl.goss.gridappsd.configuration.GLDAllConfigurationHandler;
import gov.pnnl.goss.gridappsd.configuration.OchreAllConfigurationHandler;
import gov.pnnl.goss.gridappsd.dto.AppInfo;
import gov.pnnl.goss.gridappsd.dto.ApplicationObject;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.ModelCreationConfig;
import gov.pnnl.goss.gridappsd.dto.RequestSimulation;
import gov.pnnl.goss.gridappsd.dto.ServiceConfig;
import gov.pnnl.goss.gridappsd.dto.ServiceInfo;
import gov.pnnl.goss.gridappsd.dto.SimulationConfig;
import gov.pnnl.goss.gridappsd.dto.SimulationContext;
import gov.pnnl.goss.gridappsd.dto.SimulationOutput;
import gov.pnnl.goss.gridappsd.dto.SimulationOutputObject;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.security.SecurityConfig;

public class ProcessNewSimulationRequest {

	public ProcessNewSimulationRequest() {
	}

	public ProcessNewSimulationRequest(LogManager logManager, SecurityConfig securityConfig) {
		this.logManager = logManager;
		this.securityConfig = securityConfig;
	}

	private volatile LogManager logManager;
	private volatile SecurityConfig securityConfig;

	public void process(ConfigurationManager configurationManager,
			SimulationManager simulationManager, String simulationId,
			DataResponse event, RequestSimulation simRequest, AppManager appManager,
			ServiceManager serviceManager, TestManager testManager,
			DataManager dataManager, String username) {
		process(configurationManager, simulationManager, simulationId, simRequest,
				SimulationConfig.DEFAULT_SIMULATION_BROKER_PORT, appManager,
				serviceManager, testManager, dataManager, username);
	}

	public void process(ConfigurationManager configurationManager,
			SimulationManager simulationManager, String simulationId,
			RequestSimulation simRequest, int simulationPort, AppManager appManager,
			ServiceManager serviceManager, TestManager testManager,DataManager dataManager, String username) {

		try {

			simRequest.simulation_config.setSimulation_broker_port(simulationPort);
			logManager.info(ProcessStatus.RUNNING, simulationId, "Parsed config " + simRequest);
			if (simRequest == null || simRequest.getPower_system_config() == null
					|| simRequest.getSimulation_config() == null) {
				logManager.info(ProcessStatus.RUNNING, simulationId, "No simulation file returned for request "+ simRequest);
				throw new RuntimeException("Invalid configuration received");
			}

			// make request to configuration Manager to get power grid model
			// file locations and names
			logManager.info(ProcessStatus.RUNNING, simulationId,"Creating simulation and power grid model files for simulation Id "+simulationId);


//			StringWriter simulationConfigDirOut = new StringWriter();
//			File simulationFile = configurationManager.getSimulationFile(
//					simulationId, config);
//			String simulationConfigDir = simulationConfigDirOut.toString();
			String simulationConfigDir = configurationManager.getConfigurationProperty(GridAppsDConstants.GRIDAPPSD_TEMP_PATH);
			if (simulationConfigDir == null || simulationConfigDir.trim().length()==0) {
				logManager.error(ProcessStatus.ERROR, simulationId, "No simulation file returned for request "+ simRequest);
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
			simContext.simulationId = simulationId;
			simContext.simulationPort = simulationPort;
			simContext.simulationDir = tempDataPathDir.getAbsolutePath();
			if(simRequest.getSimulation_config().getSimulator().equals("GridLAB-D"))
				simContext.startupFile = tempDataPathDir.getAbsolutePath()+File.separator+"model_startup.glm";
			else if(simRequest.getSimulation_config().getSimulator().equals("OCHRE"))
				simContext.startupFile = tempDataPathDir.getAbsolutePath()+File.separator+"ochre_helics_config.json";
			simContext.simulationUser = username;
			try{
				simContext.simulatorPath = serviceManager.getService(simRequest.getSimulation_config().getSimulator()).getExecution_path();
			}catch(NullPointerException e){
				if(serviceManager.getService(simRequest.getSimulation_config().getSimulator()) == null){
					logManager.error(ProcessStatus.ERROR, simulationId,"Cannot find service with id ="+simRequest.getSimulation_config().getSimulator());
				}else if(serviceManager.getService(simRequest.getSimulation_config().getSimulator()).getExecution_path() == null){
					logManager.error(ProcessStatus.ERROR, simulationId,"Cannot find execution path for service ="+simRequest.getSimulation_config().getSimulator());
				}
				e.printStackTrace();
			}

			
			String gldInterface = null;
			ServiceInfo gldService = serviceManager.getService("GridLAB-D");
			if(gldService!=null){
				List<String> deps = gldService.getService_dependencies();
				gldInterface = GridAppsDConstants.getGLDInterface(deps);
			} 

			int numFederates = 2;
			String simulator = simRequest.getSimulation_config().getSimulator();
			//generate config files for requested simulator
			//if requested simulator is opendss
			if(simulator.equalsIgnoreCase(DSSAllConfigurationHandler.CONFIGTARGET)){
				Properties simulationParams = generateSimulationParameters(simRequest);
				simulationParams.put(DSSAllConfigurationHandler.SIMULATIONID, simulationId);
				simulationParams.put(DSSAllConfigurationHandler.DIRECTORY, tempDataPathDir.getAbsolutePath());
				if(gldInterface!=null){
					simulationParams.put(GridAppsDConstants.GRIDLABD_INTERFACE, gldInterface);
				}
				configurationManager.generateConfiguration(DSSAllConfigurationHandler.TYPENAME, simulationParams, new PrintWriter(new StringWriter()), simulationId, username);
			}
			else if(simulator.equalsIgnoreCase(OchreAllConfigurationHandler.TYPENAME)){
				numFederates = 42;
				Properties simulationParams = generateSimulationParameters(simRequest);
				simulationParams.put(DSSAllConfigurationHandler.SIMULATIONID, simulationId);
				simulationParams.put(DSSAllConfigurationHandler.DIRECTORY, tempDataPathDir.getAbsolutePath());
				if(gldInterface!=null){
					simulationParams.put(GridAppsDConstants.GRIDLABD_INTERFACE, gldInterface);
				}
				configurationManager.generateConfiguration(GLDAllConfigurationHandler.TYPENAME, simulationParams, new PrintWriter(new StringWriter()), simulationId, username);
				configurationManager.generateConfiguration(OchreAllConfigurationHandler.TYPENAME, simulationParams, new PrintWriter(new StringWriter()), simulationId, username);
			}
			else { //otherwise use gridlabd
				Properties simulationParams = generateSimulationParameters(simRequest);
				simulationParams.put(GLDAllConfigurationHandler.SIMULATIONID, simulationId);
				simulationParams.put(GLDAllConfigurationHandler.DIRECTORY, tempDataPathDir.getAbsolutePath());
				if(gldInterface!=null){
					simulationParams.put(GridAppsDConstants.GRIDLABD_INTERFACE, gldInterface);
				}
				configurationManager.generateConfiguration(GLDAllConfigurationHandler.TYPENAME, simulationParams, new PrintWriter(new StringWriter()), simulationId, username);
			}
			
			logManager.debug(ProcessStatus.RUNNING, simulationId, "Simulation and power grid model files generated for simulation Id ");


			// Start Apps and Services

			Map<String,Object> simulationContext = new HashMap<String,Object>();
			simulationContext.put("request",simRequest);
			simulationContext.put("simulationId",simulationId);
			simulationContext.put("simulationHost","127.0.0.1");
			simulationContext.put("simulationPort",simulationPort);
			simulationContext.put("simulationDir",simulationConfigDir);
			simulationContext.put("numFederates",numFederates);

			if(simRequest.getSimulation_config().getSimulator().equals("GridLAB-D"))
				simulationContext.put("simulationFile",tempDataPathDir.getAbsolutePath()+File.separator+"model_startup.glm");
			else if(simRequest.getSimulation_config().getSimulator().equals("OCHRE"))
				simulationContext.put("simulationFile",tempDataPathDir.getAbsolutePath()+File.separator+"ochre_helics_config.json");
			simulationContext.put("logLevel", logManager.getLogLevel());
			simulationContext.put("username", securityConfig.getManagerUser());
			simulationContext.put("password", securityConfig.getManagerPassword());
			try{
				simulationContext.put("simulatorPath",serviceManager.getService(simRequest.getSimulation_config().getSimulator()).getExecution_path());
			}catch(NullPointerException e){
				if(serviceManager.getService(simRequest.getSimulation_config().getSimulator()) == null){
					logManager.error(ProcessStatus.ERROR, simulationId,"Cannot find service with id ="+simRequest.getSimulation_config().getSimulator());
				}else if(serviceManager.getService(simRequest.getSimulation_config().getSimulator()).getExecution_path() == null){
					logManager.error(ProcessStatus.ERROR, simulationId,"Cannot find execution path for service ="+simRequest.getSimulation_config().getSimulator());
				}
				e.printStackTrace();
			}

			List<String> connectServiceInstanceIds = new ArrayList<String>();
			List<String> connectServiceIds = new ArrayList<String>();
			List<String> connectedAppInstanceIds = new ArrayList<String>();
			logManager.info(ProcessStatus.RUNNING, simulationId, "Service configs "+simRequest.service_configs);
			if (simRequest.service_configs == null) {
				logManager.warn(ProcessStatus.RUNNING, simulationId, "No services found in request  ="+simRequest.getSimulation_config().getSimulator());
			}
			else{
				for(ServiceConfig serviceConfig : simRequest.service_configs){
					logManager.info(ProcessStatus.RUNNING, simulationId, "Starting service"+serviceConfig.getId());

					String serviceInstanceId = serviceManager.startServiceForSimultion(serviceConfig.getId(), null, simulationContext);
					if(serviceInstanceId!=null){
						connectServiceInstanceIds.add(serviceInstanceId);
						connectServiceIds.add(serviceConfig.getId());
					}
				}
			}
			
			

			if (simRequest.application_config == null) {
				logManager.warn(ProcessStatus.RUNNING, simulationId, "No applications found in request  ="+simRequest.getSimulation_config().getSimulator());
			}
			else {
				for (ApplicationObject app : simRequest.application_config
						.getApplications()) {
					AppInfo appInfo = appManager.getApp(app.getName());
					if(appInfo==null) {
						logManager.error(ProcessStatus.ERROR, simulationId, "Cannot start application "+ app.getName() +". Application not available");
						throw new RuntimeException("Cannot start application "+ app.getName() +". Application not available");

					}



					List<String> prereqsList = appManager.getApp(app.getName())
							.getPrereqs();
					for (String prereqs : prereqsList) {

						if(!connectServiceIds.contains(prereqs)){
							String serviceInstanceId = serviceManager.startServiceForSimultion(prereqs, null,simulationContext);
							if(serviceInstanceId!=null){
								connectServiceInstanceIds.add(serviceInstanceId);
								logManager.info(ProcessStatus.RUNNING, simulationId, "Started " + prereqs + " with instance id " + serviceInstanceId);
							}
						}
					}

					String appInstanceId = appManager.startAppForSimultion(app
							.getName(), app.getConfig_string(), simulationContext);
					connectedAppInstanceIds.add(appInstanceId);
					logManager.info(ProcessStatus.RUNNING, simulationId, "Started "+ app.getName() + " with instance id "+ appInstanceId);

				}
			}

			simulationContext.put("connectedServiceInstanceIds",connectServiceInstanceIds);
			simulationContext.put("connectedAppInstanceIds",connectedAppInstanceIds);
			simContext.serviceInstanceIds = connectServiceInstanceIds;
			simContext.appInstanceIds = connectedAppInstanceIds;
			
			ServiceInfo simulationServiceInfo = serviceManager.getService(simRequest.getSimulation_config().simulator);
			List<String> serviceDependencies = simulationServiceInfo.getService_dependencies();
			for(String service : serviceDependencies) {
				String serviceInstanceId = serviceManager.startServiceForSimultion(service, null, simulationContext);
				if(serviceInstanceId!=null)
					simContext.addServiceInstanceIds(serviceInstanceId);
			}
			
			dataManager.processDataRequest(simContext, "timeseries", simulationId, null, username);
		
			// start test if requested 
			testManager.handleTestRequest(simRequest.getTest_config(), simContext);
			
			// start simulation
			logManager.debug(ProcessStatus.RUNNING, simulationId,"Starting simulation for id " + simulationId);
			simulationManager.startSimulation(simulationId, simRequest.getSimulation_config(),simContext, simulationContext);
			logManager.info(ProcessStatus.RUNNING, simulationId,"Started simulation for id " + simulationId);
			

		} catch (Exception e) {
			e.printStackTrace();
			try {
				logManager.error(ProcessStatus.ERROR, simulationId, "Failed to start simulation correctly: "+ e.getMessage());

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

		if(modelConfig.schedule_name!=null){
			params.put(GLDAllConfigurationHandler.SCHEDULENAME, modelConfig.schedule_name);
		} else {
			 params.put(GLDAllConfigurationHandler.SCHEDULENAME, "");
		}
		params.put(GLDAllConfigurationHandler.SIMULATIONNAME, requestSimulation.getSimulation_config().simulation_name);
		params.put(GLDAllConfigurationHandler.SOLVERMETHOD, requestSimulation.getSimulation_config().power_flow_solver_method);

		params.put(GLDAllConfigurationHandler.SIMULATIONBROKERHOST, requestSimulation.getSimulation_config().getSimulation_broker_location());
		params.put(GLDAllConfigurationHandler.SIMULATIONBROKERPORT, new Integer(requestSimulation.getSimulation_config().getSimulation_broker_port()).toString());

		params.put(GLDAllConfigurationHandler.SIMULATIONSTARTTIME, requestSimulation.getSimulation_config().start_time);
		params.put(GLDAllConfigurationHandler.SIMULATIONDURATION, new Integer(requestSimulation.getSimulation_config().duration).toString());
		
		if(modelConfig.getModel_state()!=null){
			Gson  gson = new Gson();
			params.put(GLDAllConfigurationHandler.MODEL_STATE, gson.toJson(modelConfig.getModel_state()));
		}
		
		params.put(GLDAllConfigurationHandler.SIMULATOR, requestSimulation.getSimulation_config().getSimulator());
		if(modelConfig.separated_loads_file!=null){
			params.put(GLDAllConfigurationHandler.SEPARATED_LOADS_FILE, modelConfig.separated_loads_file);
		} else {
			 params.put(GLDAllConfigurationHandler.SEPARATED_LOADS_FILE, "");
		}
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
