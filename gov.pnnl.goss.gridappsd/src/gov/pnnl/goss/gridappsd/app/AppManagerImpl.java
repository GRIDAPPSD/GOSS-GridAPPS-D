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
package gov.pnnl.goss.gridappsd.app;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;

import javax.jms.Destination;


import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;

import gov.pnnl.goss.gridappsd.api.AppManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.StatusReporter;
import gov.pnnl.goss.gridappsd.dto.AppInfo;
import gov.pnnl.goss.gridappsd.dto.AppInfo.AppType;
import gov.pnnl.goss.gridappsd.dto.AppInstance;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.RequestAppList;
import gov.pnnl.goss.gridappsd.dto.RequestAppRegister;
import gov.pnnl.goss.gridappsd.dto.RequestAppStart;
import gov.pnnl.goss.gridappsd.dto.ResponseAppInfo;
import gov.pnnl.goss.gridappsd.dto.ResponseAppInstance;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.Client;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.DataError;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.Client.PROTOCOL;

/**
 * This class implements subset of functionalities for Internal Functions
 * 405 Simulation Manager and 406 Power System Model Manager.
 * ConfigurationManager is responsible for:
 * - subscribing to configuration topics and 
 * - converting configuration message into simulation configuration files
 *   and power grid model files.
 * @author shar064
 *
 */

@Component
public class AppManagerImpl implements AppManager{
	private static final String CONFIG_PID = "pnnl.goss.gridappsd";

	final String CONFIG_DIR_NAME = "config";
	final String CONFIG_FILE_NAME = "appinfo.json";
	
	//@ServiceDependency
	//private volatile StatusReporter statusReporter;
	
	@ServiceDependency
	private volatile ClientFactory clientFactory;
	
	@ServiceDependency
	private volatile LogManager logManager;
	
	
	private Dictionary<String, ?> configurationProperties;
	
	private HashMap<String, AppInfo> apps = new HashMap<String, AppInfo>();
	
	private HashMap<String, AppInstance> appInstances = new HashMap<String, AppInstance>();
	
	private Client client;
	
	public AppManagerImpl() {
	}
	 
	public AppManagerImpl(StatusReporter statusReporter, LogManager logManager, ClientFactory clientFactory) {
		this.statusReporter = statusReporter;
		this.logManager = logManager;
		this.clientFactory = clientFactory;

	}
	
	@Override
	public void process(StatusReporter statusReporter, int processId, DataResponse event, Serializable message) throws Exception {
		if(client==null){
			Credentials credentials = new UsernamePasswordCredentials(
					GridAppsDConstants.username, GridAppsDConstants.password);
			client = clientFactory.create(PROTOCOL.STOMP,credentials);
		}
		Destination replyDestination = event.getReplyDestination();
		
		String destination = event.getDestination();
		if(destination.startsWith(GridAppsDConstants.topic_app_list)){
			RequestAppList requestObj = RequestAppList.parse(message.toString());
			if(!requestObj.list_running_only){
				List<AppInfo> appResponseList = listApps();
				//TODO publish on event.getReplyDestination(); 
				client.publish(replyDestination, new ResponseAppInfo(appResponseList));
				
			} else {
				List<AppInstance> appInstanceResponseList;
				if (requestObj.app_id!=null){
					appInstanceResponseList = listRunningApps(requestObj.app_id);
				} else {
					appInstanceResponseList = listRunningApps();
				}	
				//TODO publish list  on event.getReplyDestination();
				client.publish(replyDestination, new ResponseAppInstance(appInstanceResponseList));

			}
			 
		} else if(destination.startsWith(GridAppsDConstants.topic_app_get)){
			String appId = message.toString();
			AppInfo appInfo = getApp(appId);
			//TODO publish appinfo object on reply destination
			client.publish(replyDestination, appInfo);

			
		} else if(destination.startsWith(GridAppsDConstants.topic_app_register)){
			RequestAppRegister requestObj = RequestAppRegister.parse(message.toString());
			registerApp(requestObj.app_info, requestObj.app_package);
			
		} else if(destination.startsWith(GridAppsDConstants.topic_app_deregister)){
			String appId = message.toString();
			deRegisterApp(appId);
			
		} else if(destination.startsWith(GridAppsDConstants.topic_app_start)){
			RequestAppStart requestObj = RequestAppStart.parse(message.toString());
			String instanceId = null;
			if(requestObj.getSimulation_id()!=null){
				instanceId = startApp(requestObj.getApp_id(),requestObj.getRuntime_options());
			} else {
				instanceId = startAppForSimultion(requestObj.getApp_id(),requestObj.getRuntime_options(), requestObj.getSimulation_id());
			}
			//TODO publish instance id
			client.publish(replyDestination, instanceId);

		} else if(destination.startsWith(GridAppsDConstants.topic_app_stop)){
			String appId = message.toString();
			stopApp(appId);
		} else if(destination.startsWith(GridAppsDConstants.topic_app_stop_instance)){
			String appInstanceId = message.toString();
			stopAppInstance(appInstanceId);
			
		} else {
			//throw error, destination unrecognized
			client.publish(replyDestination, new DataError("App manager destination not recognized: "+event.getDestination()));

		}
		
	}
	
	
	@Start
	public void start(){
		//statusReporter.reportStatus(String.format("Starting %s", this.getClass().getName()));
		logManager.log(new LogMessage(this.getClass().getName(), 
				new Long(new Date().getTime()).toString(), 
				"Starting "+this.getClass().getName(), 
				"INFO", 
				"Running", 
				true));
		
		scanForApps();
		
		logManager.log(new LogMessage(this.getClass().getName(), 
				new Long(new Date().getTime()).toString(), 
				String.format("Found %s applications", apps.size()), 
				"INFO", 
				"Running", 
				true));
	}
	
	protected void scanForApps(){
		//Get directory for apps from the config
		File appConfigDir = getAppConfigDirectory();
		
		//for each app found, parse the appinfo.json config file to create appinfo object and add to apps map
		File[] appDirs = appConfigDir.listFiles();
		for(File appDir: appDirs){
			AppInfo appInfo = parseAppInfo(appDir);
			apps.put(appInfo.getId(), appInfo);
		}
	}
	

	
	
	@Override
	public void registerApp(AppInfo appInfo, Serializable appPackage) {
		//appPackage should be received as a zip file. extract this to the apps directory, and write appinfo to a json file under config/
		//TODO extract
		
		writeAppInfo(appInfo);
		
		//add to apps
		apps.put(appInfo.getId(), appInfo);
	}

	@Override
	public List<AppInfo> listApps() {
		List<AppInfo> result = new ArrayList<AppInfo>();
		result.addAll(apps.values());
		return result;
		
	}
	
	@Override
	public List<AppInstance> listRunningApps() {
		List<AppInstance> result = new ArrayList<AppInstance>();
		result.addAll(appInstances.values());
		return result;
	}
	
	@Override
	public List<AppInstance> listRunningApps(String appId) {
		List<AppInstance> result = new ArrayList<AppInstance>();
		for(String instanceId: appInstances.keySet()){
			AppInstance instance = appInstances.get(instanceId);
			if(instance.getApp_info().getId().equals(appId)){
				result.add(instance);
			}
		}
		return result;
	}

	@Override
	public AppInfo getApp(String appId) {
		return apps.get(appId);
	}

	@Override
	public void deRegisterApp(String appId) {
		// find and stop any running instances
		stopApp(appId);
		
		//remove app from mapping
		apps.remove(appId);
		
		//get app directory from config and remove files for app_id
		File configDir = getAppConfigDirectory();
		File appDir = new File(configDir.getAbsolutePath()+File.separator+appId);
		appDir.delete();
		
	}

	@Override
	public String startApp(String appId, HashMap<String, String> runtimeOptions) {
		return startAppForSimultion(appId, runtimeOptions, null);
	}

	@Override
	public String startAppForSimultion(String appId, HashMap<String, String> runtimeOptions, String simulationId) {
		String instanceId = appId+"-"+new Date().getTime();
		// get execution path
		AppInfo appInfo = apps.get(appId);
		if(appInfo==null){
			throw new RuntimeException("App not found: "+appId);
		}
		
		// are multiple allowed? if not check to see if it is already running, if it is then fail
		if(!appInfo.isMultiple_instances() && listRunningApps(appId).size()>0){
			throw new RuntimeException("App is already running and multiple instances are not allowed: "+appId);
		}

		//build options
		//might need a standard method for replacing things like SIMULATION_ID in the input/output options
		
		Process process = null;
		//something like 
		if(appInfo.getType().equals(AppType.PYTHON)){
			List<String> commands = new ArrayList<String>();
			commands.add("python");
			commands.add(appInfo.getExecution_path());
			//TODO add other options
			
			
			ProcessBuilder processAppBuilder = new ProcessBuilder(commands);
			processAppBuilder.redirectErrorStream(true);
			processAppBuilder.redirectOutput();
			
//		ProcessBuilder fncsBridgeBuilder = new ProcessBuilder("python", getPath(GridAppsDConstants.FNCS_BRIDGE_PATH), simulationConfig.getSimulation_name());
//		fncsBridgeBuilder.redirectErrorStream(true);
//		fncsBridgeBuilder.redirectOutput(new File(defaultLogDir.getAbsolutePath()+File.separator+"fncs_goss_bridge.log"));
//		fncsBridgeProcess = fncsBridgeBuilder.start();
//		// Watch the process
//		watch(fncsBridgeProcess, "FNCS GOSS Bridge");
		//during watch, send stderr/out to logmanager
			
		} else if(appInfo.getType().equals(AppType.JAVA)){
//			ProcessBuilder fncsBridgeBuilder = new ProcessBuilder("python", getPath(GridAppsDConstants.FNCS_BRIDGE_PATH), simulationConfig.getSimulation_name());
//			fncsBridgeBuilder.redirectErrorStream(true);
//			fncsBridgeBuilder.redirectOutput(new File(defaultLogDir.getAbsolutePath()+File.separator+"fncs_goss_bridge.log"));
//			fncsBridgeProcess = fncsBridgeBuilder.start();
//			// Watch the process
//			watch(fncsBridgeProcess, "FNCS GOSS Bridge");
			//during watch, send stderr/out to logmanager
				
		} else if(appInfo.getType().equals(AppType.WEB)){
//			ProcessBuilder fncsBridgeBuilder = new ProcessBuilder("python", getPath(GridAppsDConstants.FNCS_BRIDGE_PATH), simulationConfig.getSimulation_name());
//			fncsBridgeBuilder.redirectErrorStream(true);
//			fncsBridgeBuilder.redirectOutput(new File(defaultLogDir.getAbsolutePath()+File.separator+"fncs_goss_bridge.log"));
//			fncsBridgeProcess = fncsBridgeBuilder.start();
//			// Watch the process
//			watch(fncsBridgeProcess, "FNCS GOSS Bridge");
			//during watch, send stderr/out to logmanager
				
		} else {
			throw new RuntimeException("Type not recognized "+appInfo.getType());
		}
		
		
		//create appinstance object
		AppInstance appInstance = new AppInstance(instanceId, appInfo, runtimeOptions, simulationId, process);
		appInstance.setApp_info(appInfo);
		
		//add to app instances map
		appInstances.put(instanceId, appInstance);
		
		
		return instanceId;
	}

	@Override
	public void stopApp(String appId) {
		for(AppInstance instance: listRunningApps(appId)){
			if(instance.getApp_info().getId().equals(appId)){
				stopAppInstance(instance.getInstance_id());
			}
		}
		
	}
	
	@Override
	public void stopAppInstance(String instanceId) {
		AppInstance instance = appInstances.get(instanceId);
		instance.getProcess().destroy();
		appInstances.remove(instanceId); 

		
	}
	
	
	
	@ConfigurationDependency(pid=CONFIG_PID)
	public synchronized void updated(Dictionary<String, ?> config)  {
		this.configurationProperties = config;
	}
	
	public String getConfigurationProperty(String key){
		if(this.configurationProperties!=null){
			Object value = this.configurationProperties.get(key);
			if(value!=null)
				return value.toString();
		}
		return null;
	}

	
	protected File getAppConfigDirectory(){
		String configDirStr = getConfigurationProperty(GridAppsDConstants.APPLICATIONS_PATH);
		if (configDirStr==null){
			configDirStr = "applications";
		}
		
		File configDir = new File(configDirStr);
		if(!configDir.exists()){
			configDir.mkdirs();
			if(!configDir.exists()){
				throw new RuntimeException("Applications directory "+configDir.getAbsolutePath()+" does not exist and cannot be created.");
			}
		}
			
		return configDir;
		
	}
	
	protected AppInfo parseAppInfo(File appDirectory){
		AppInfo appInfo = null;
		File confFile = new File(appDirectory.getAbsolutePath()+File.separator+CONFIG_DIR_NAME+File.separator+CONFIG_FILE_NAME);
		if(!confFile.exists()){
			throw new RuntimeException("App config file does not exist: "+confFile.getAbsolutePath());
		}
		
		try {
			String appConfigStr = new String(Files.readAllBytes(confFile.toPath()));
			appInfo = AppInfo.parse(appConfigStr);
		} catch (IOException e) {
			logManager.log(new LogMessage("App Manager", new Long(new Date().getTime()).toString(), "Error while reading app config file: "+e.getMessage(), "ERROR", "failed", false));
		}
		
		return appInfo;
		
	}
	
	protected void writeAppInfo(AppInfo appInfo){
		File appConfigDirectory = getAppConfigDirectory();
		
		File confFile = new File(appConfigDirectory.getAbsolutePath()+File.separator+appInfo.getId()+File.separator+CONFIG_DIR_NAME+File.separator+CONFIG_FILE_NAME);
		try {
			Files.write(confFile.toPath(), appInfo.toString().getBytes());
		} catch (IOException e) {
			logManager.log(new LogMessage("App Manager", new Long(new Date().getTime()).toString(), "Error while writing app config file: "+e.getMessage(), "ERROR", "failed", false));
		}
	}


}
