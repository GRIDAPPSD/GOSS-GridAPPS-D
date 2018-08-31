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

import gov.pnnl.goss.gridappsd.api.AppManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.dto.AppInfo;
import gov.pnnl.goss.gridappsd.dto.AppInfo.AppType;
import gov.pnnl.goss.gridappsd.dto.AppInstance;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.RemoteApplicationRegistrationResponse;
import gov.pnnl.goss.gridappsd.dto.RequestAppList;
import gov.pnnl.goss.gridappsd.dto.RequestAppRegister;
import gov.pnnl.goss.gridappsd.dto.RequestAppStart;
import gov.pnnl.goss.gridappsd.dto.ResponseAppInfo;
import gov.pnnl.goss.gridappsd.dto.ResponseAppInstance;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jms.Destination;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;

import com.google.gson.JsonSyntaxException;

import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.DataError;
import pnnl.goss.core.DataResponse;

/**
 * This class implements subset of functionalities for Internal Functions 405
 * Simulation Manager and 406 Power System Model Manager. ConfigurationManager
 * is responsible for: - subscribing to configuration topics and - converting
 * configuration message into simulation configuration files and power grid
 * model files.
 * 
 * @author shar064
 *
 */

@Component
public class AppManagerImpl implements AppManager {
	private static final String CONFIG_PID = "pnnl.goss.gridappsd";

	final String CONFIG_FILE_EXT = ".config";

	@ServiceDependency
	private volatile ClientFactory clientFactory;

	@ServiceDependency
	private volatile LogManager logManager;

	private Dictionary<String, ?> configurationProperties;

	private HashMap<String, AppInfo> apps = new HashMap<String, AppInfo>();

	private HashMap<String, AppInstance> appInstances = new HashMap<String, AppInstance>();

	private String username;

	private Client client;
	
	private RemoteApplicationHeartbeatMonitor remoteAppMonitor;

	public AppManagerImpl() {
	}

	public AppManagerImpl(LogManager logManager,
			ClientFactory clientFactory) {
		this.logManager = logManager;
		this.clientFactory = clientFactory;

	}

	@Override
	public void process(int processId, DataResponse event, Serializable message)
			throws Exception {

		// TODO:Get username from message's metadata e.g. event.getUserName()
		username = GridAppsDConstants.username;

		if (client == null) {
			Credentials credentials = new UsernamePasswordCredentials(
					GridAppsDConstants.username, GridAppsDConstants.password);
			client = clientFactory.create(PROTOCOL.STOMP, credentials);
		}
		Destination replyDestination = event.getReplyDestination();

		String destination = event.getDestination();
		if (destination.contains(GridAppsDConstants.topic_app_list)) {
			RequestAppList requestObj = RequestAppList
					.parse(message.toString());
			if (!requestObj.list_running_only) {
				List<AppInfo> appResponseList = listApps();
				// TODO publish on event.getReplyDestination();
				client.publish(replyDestination, new ResponseAppInfo(
						appResponseList));

			} else {
				List<AppInstance> appInstanceResponseList;
				if (requestObj.app_id != null) {
					appInstanceResponseList = listRunningApps(requestObj.app_id);
				} else {
					appInstanceResponseList = listRunningApps();
				}
				// TODO publish list on event.getReplyDestination();
				client.publish(replyDestination, new ResponseAppInstance(
						appInstanceResponseList));

			}

		} else if (destination.contains(GridAppsDConstants.topic_app_get)) {
			String appId = message.toString();
			AppInfo appInfo = getApp(appId);
			// TODO publish appinfo object on reply destination
			client.publish(replyDestination, appInfo);

		} else if (destination.contains(GridAppsDConstants.topic_app_register_remote)) {
			Serializable request;
			if (message instanceof DataResponse){
				request = ((DataResponse)message).getData();
			} else {
				request = message;
			}
			// The request should just send the AppInfo object over rather than
			// the RequestAppRegister option.
			AppInfo app_info = AppInfo.parse(request.toString());
			RemoteApplicationRegistrationResponse topics = registerRemoteApp(app_info);
			client.publish(replyDestination, topics.toString());
			//registerApp(requestObj.app_info, requestObj.app_package);

		} else if (destination.contains(GridAppsDConstants.topic_app_register)) {
			RequestAppRegister requestObj = RequestAppRegister.parse(message
					.toString());
			registerApp(requestObj.app_info, requestObj.app_package);

		} else if (destination
				.contains(GridAppsDConstants.topic_app_deregister)) {
			String appId = message.toString();
			deRegisterApp(appId);

		} else if (destination.contains(GridAppsDConstants.topic_app_start)) {
			RequestAppStart requestObj;
			try {
				requestObj = RequestAppStart.parse(message.toString());
			}
			catch (JsonSyntaxException ex){
				requestObj = RequestAppStart.parse(event.getData().toString());
			}
					
			String instanceId = null;
			if (requestObj.getSimulation_id() == null) {
				instanceId = startApp(requestObj.getApp_id(),
						requestObj.getRuntime_options(),
						new Integer(processId).toString());
			} else {
				instanceId = startAppForSimultion(requestObj.getApp_id(),
						requestObj.getRuntime_options(),null);
			}

			client.publish(replyDestination, instanceId);

		} else if (destination.contains(GridAppsDConstants.topic_app_stop)) {
			String appId = message.toString();
			stopApp(appId);
		} else if (destination
				.contains(GridAppsDConstants.topic_app_stop_instance)) {
			String appInstanceId = message.toString();
			stopAppInstance(appInstanceId);

		} else {
			// throw error, destination unrecognized
			client.publish(replyDestination,
					new DataError("App manager destination not recognized: "
							+ event.getDestination()));

		}

	}

	@Start
	public void start(){
		//statusReporter.reportStatus(String.format("Starting %s", this.getClass().getName()));
		try{
		logManager.log(new LogMessage(this.getClass().getName(), 
				null,
				new Date().getTime(), 
				"Starting "+this.getClass().getName(), 
				LogLevel.INFO, 
				ProcessStatus.RUNNING, 
				true),GridAppsDConstants.username,
				GridAppsDConstants.topic_platformLog);
		
		scanForApps();
		
		logManager.log(new LogMessage(this.getClass().getName(),
				null,
				new Date().getTime(), 
				String.format("Found %s applications", apps.size()), 
				LogLevel.INFO, 
				ProcessStatus.RUNNING, 
				true),GridAppsDConstants.username);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	protected void scanForApps() {
		// Get directory for apps from the config
		File appConfigDir = getAppConfigDirectory();

		// for each app found, parse the appinfo.config file to create appinfo
		// object and add to apps map
		File[] appConfigFiles = appConfigDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(CONFIG_FILE_EXT);
			}
		});
		for (File appConfigFile : appConfigFiles) {
			AppInfo appInfo = parseAppInfo(appConfigFile);
			apps.put(appInfo.getId(), appInfo);

		}
	}

	public RemoteApplicationRegistrationResponse registerRemoteApp(AppInfo appInfo) {
		if (remoteAppMonitor == null) {
			remoteAppMonitor = new RemoteApplicationHeartbeatMonitor(logManager, client);
		}
		
		System.out.println(appInfo.toString());
		// Simple routine to make sure appid is unique.
		int app_count = 1;
		String app_id = appInfo.getId() + app_count;
		while (apps.containsKey(app_id)) {
			app_count += 1;
			app_id = appInfo.getId() + app_count;
		}
		appInfo.setId(app_id);
		apps.put(app_id, appInfo);
		
		RemoteApplicationRegistrationResponse response = new RemoteApplicationRegistrationResponse();
		response.applicationId = app_id;
		response.errorTopic="Error";
		response.heartbeatTopic="/queue/" + GridAppsDConstants.topic_remoteapp_heartbeat+"."+app_id;
		response.startControlTopic="/topic/" + GridAppsDConstants.topic_remoteapp_start+"."+app_id;
		response.stopControlTopic="/topic/" + GridAppsDConstants.topic_remoteapp_stop+"."+app_id;
		System.out.println(response.toString());
		remoteAppMonitor.addRemoteApplication(app_id, response);		
		
		return response;
	}
	// TODO probably need an updateApp call or integrate this with register app

	@Override
	public void registerApp(AppInfo appInfo, byte[] appPackage)
			throws Exception {
		System.out.println("REGISTER REQUEST RECEIVED ");
		// appPackage should be received as a zip file. extract this to the apps
		// directory, and write appinfo to a json file under config/
		File appHomeDir = getAppConfigDirectory();
		if (!appHomeDir.exists()) {
			appHomeDir.mkdirs();
			if (!appHomeDir.exists()) {
				// if it still doesn't exist throw an error
				throw new Exception(
						"App home directory does not exist and cannot be created "
								+ appHomeDir.getAbsolutePath());
			}
		}
		System.out.println("HOME DIR " + appHomeDir.getAbsolutePath());
		// create a directory for the app
		File newAppDir = new File(appHomeDir.getAbsolutePath() + File.separator
				+ appInfo.getId());
		if (newAppDir.exists()) {
			throw new Exception("App " + appInfo.getId() + " already exists");
		}

		try {
			newAppDir.mkdirs();
			// Extract zip file into new app dir
			ZipInputStream zipIn = new ZipInputStream(new ByteArrayInputStream(
					appPackage));
			ZipEntry entry = zipIn.getNextEntry();
			// iterates over entries in the zip file and write to dir
			while (entry != null) {
				String filePath = newAppDir + File.separator + entry.getName();
				if (!entry.isDirectory()) {
					// if the entry is a file, extracts it
					extractFile(zipIn, filePath);
				} else {
					// if the entry is a directory, make the directory
					File dir = new File(filePath);
					dir.mkdir();
				}
				zipIn.closeEntry();
				entry = zipIn.getNextEntry();
			}
			zipIn.close();

			writeAppInfo(appInfo);

			// add to apps hashmap
			apps.put(appInfo.getId(), appInfo);
		} catch (Exception e) {
			// Clean up app dir if fails
			newAppDir.delete();
			throw e;
		}
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
		for (String instanceId : appInstances.keySet()) {
			AppInstance instance = appInstances.get(instanceId);
			if (instance.getApp_info().getId().equals(appId)) {
				result.add(instance);
			}
		}
		return result;
	}

	@Override
	public AppInfo getApp(String appId) {
		appId = appId.trim();
		return apps.get(appId);
	}

	@Override
	public void deRegisterApp(String appId) {
		appId = appId.trim();
		// find and stop any running instances
		stopApp(appId);

		// remove app from mapping
		apps.remove(appId);

		// get app directory from config and remove files for app_id
		File configDir = getAppConfigDirectory();
		File appDir = new File(configDir.getAbsolutePath() + File.separator
				+ appId);
		try {
			Files.walkFileTree(appDir.toPath(), new FileVisitor<Path>() {
				@Override
				public FileVisitResult postVisitDirectory(Path dir,
						IOException exc) throws IOException {
					if (dir.toFile().delete()) {
						return FileVisitResult.CONTINUE;
					} else {
						return FileVisitResult.TERMINATE;
					}
				}

				@Override
				public FileVisitResult preVisitDirectory(Path dir,
						BasicFileAttributes attrs) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file,
						BasicFileAttributes attrs) throws IOException {
					if (file.toFile().delete()) {
						return FileVisitResult.CONTINUE;
					} else {
						return FileVisitResult.TERMINATE;
					}
				}

				@Override
				public FileVisitResult visitFileFailed(Path file,
						IOException exc) throws IOException {
					return FileVisitResult.TERMINATE;
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}

		appDir.delete();

		File appInfoFile = new File(configDir.getAbsolutePath()
				+ File.separator + appId + CONFIG_FILE_EXT);
		appInfoFile.delete();

	}

	@Override
	public String startApp(String appId, String runtimeOptions, String requestId) {
		return startAppForSimultion(appId, runtimeOptions, null);
	}

	@Override
	public String startAppForSimultion(String appId, String runtimeOptions, Map simulationContext) {
		
		String simulationId = null;
		if (simulationContext != null) {
			simulationId = simulationContext.get("simulationId").toString();
		}
		
		appId = appId.trim();
		String instanceId = appId + "-" + new Date().getTime();
		// get execution path
		AppInfo appInfo = apps.get(appId);
		if (appInfo == null) {
			throw new RuntimeException("App not found: " + appId);
		}

		// are multiple allowed? if not check to see if it is already running,
		// if it is then fail
		if (!appInfo.isMultiple_instances()
				&& listRunningApps(appId).size() > 0) {
			throw new RuntimeException(
					"App is already running and multiple instances are not allowed: "
							+ appId);
		}

		// build options
		// might need a standard method for replacing things like SIMULATION_ID
		// in the input/output options
		/*String optionsString = appInfo.getOptions();
		if (simulationId != null) {
			if (optionsString.contains("SIMULATION_ID")) {
				optionsString = optionsString.replace("SIMULATION_ID",
						simulationId);
			}
			if (runtimeOptions.contains("SIMULATION_ID")) {
				runtimeOptions = runtimeOptions.replace("SIMULATION_ID",
						simulationId);
			}
		}*/
		
		
		
		File appDirectory = new File(getAppConfigDirectory().getAbsolutePath()
				+ File.separator + appId);

		Process process = null;
		// something like
		if (AppType.REMOTE.equals(appInfo.getType())) {
			List<String> commands = buildCommandString(runtimeOptions, simulationContext, appInfo);
			String args = String.join(" ", commands);
			remoteAppMonitor.startRemoteApplication(appInfo.getId(), args);
					
		}
		else if (AppType.PYTHON.equals(appInfo.getType())) {
			List<String> commands = buildCommandString(runtimeOptions, simulationContext, appInfo);
			commands.add(0, "python");
			ProcessBuilder processAppBuilder = new ProcessBuilder(commands);
			processAppBuilder.redirectErrorStream(true);
			processAppBuilder.redirectOutput();
			processAppBuilder.directory(appDirectory);
			logManager.log(new LogMessage(this.getClass().getSimpleName(), 
					simulationId, new Date().getTime(),
					"Starting app with command "+ String.join(" ",commands), 
					LogLevel.DEBUG, ProcessStatus.RUNNING, true), 
					GridAppsDConstants.topic_simulationLog+simulationId);
			try {
				process = processAppBuilder.start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// ProcessBuilder fncsBridgeBuilder = new ProcessBuilder("python",
			// getPath(GridAppsDConstants.FNCS_BRIDGE_PATH),
			// simulationConfig.getSimulation_name());
			// fncsBridgeBuilder.redirectErrorStream(true);
			// fncsBridgeBuilder.redirectOutput(new
			// File(defaultLogDir.getAbsolutePath()+File.separator+"fncs_goss_bridge.log"));
			// fncsBridgeProcess = fncsBridgeBuilder.start();
			// // Watch the process
			// watch(fncsBridgeProcess, "FNCS GOSS Bridge");
			// during watch, send stderr/out to logmanager

		} else if (AppType.JAVA.equals(appInfo.getType())) {
			// ProcessBuilder fncsBridgeBuilder = new ProcessBuilder("python",
			// getPath(GridAppsDConstants.FNCS_BRIDGE_PATH),
			// simulationConfig.getSimulation_name());
			// fncsBridgeBuilder.redirectErrorStream(true);
			// fncsBridgeBuilder.redirectOutput(new
			// File(defaultLogDir.getAbsolutePath()+File.separator+"fncs_goss_bridge.log"));
			// fncsBridgeProcess = fncsBridgeBuilder.start();
			// // Watch the process
			// watch(fncsBridgeProcess, "FNCS GOSS Bridge");
			// during watch, send stderr/out to logmanager

		} else if (AppType.WEB.equals(appInfo.getType())) {
			// ProcessBuilder fncsBridgeBuilder = new ProcessBuilder("python",
			// getPath(GridAppsDConstants.FNCS_BRIDGE_PATH),
			// simulationConfig.getSimulation_name());
			// fncsBridgeBuilder.redirectErrorStream(true);
			// fncsBridgeBuilder.redirectOutput(new
			// File(defaultLogDir.getAbsolutePath()+File.separator+"fncs_goss_bridge.log"));
			// fncsBridgeProcess = fncsBridgeBuilder.start();
			// // Watch the process
			// watch(fncsBridgeProcess, "FNCS GOSS Bridge");
			// during watch, send stderr/out to logmanager

		} else {
			throw new RuntimeException("Type not recognized "
					+ appInfo.getType());
		}

		// create appinstance object
		AppInstance appInstance = new AppInstance(instanceId, appInfo,
				runtimeOptions, simulationId, simulationId, process);
		appInstance.setApp_info(appInfo);
		if (!AppType.REMOTE.equals(appInfo.getType())){
			watch(appInstance);
		}
		// add to app instances map
		appInstances.put(instanceId, appInstance);

		return instanceId;
	}

	private List<String> buildCommandString(String runtimeOptions, Map simulationContext, AppInfo appInfo) {
		List<String> commands = new ArrayList<String>();
		commands.add(appInfo.getExecution_path());
		
		//Check if static args contain any replacement values
		List<String> staticArgsList = appInfo.getOptions();
		if (staticArgsList != null) {
			for(String staticArg : staticArgsList) {
			    if(staticArg!=null){
			    	if(staticArg.contains("(")){
				    	 String[] replaceArgs = StringUtils.substringsBetween(staticArg, "(", ")");
				    	 for(String args : replaceArgs){
				    		staticArg = staticArg.replace("("+args+")",simulationContext.get(args).toString());
				    	 }
			    	}
			    	commands.add(staticArg);
			    }
			}
		}
		
		if(runtimeOptions!=null && !runtimeOptions.isEmpty()){
			String runTimeString = runtimeOptions.replace(" ", "").replace("\n","");
			commands.add(runTimeString);
		}
		return commands;
	}

	@Override
	public void stopApp(String appId) {
		appId = appId.trim();
		for (AppInstance instance : listRunningApps(appId)) {
			if (instance.getApp_info().getId().equals(appId)) {
				stopAppInstance(instance.getInstance_id());
			}
		}

	}

	@Override
	public void stopAppInstance(String instanceId) {
		instanceId = instanceId.trim();
		AppInstance instance = appInstances.get(instanceId);
		instance.getProcess().destroy();
		appInstances.remove(instanceId);

	}

	@ConfigurationDependency(pid = CONFIG_PID)
	public synchronized void updated(Dictionary<String, ?> config) {
		this.configurationProperties = config;
	}

	public String getConfigurationProperty(String key) {
		if (this.configurationProperties != null) {
			Object value = this.configurationProperties.get(key);
			if (value != null)
				return value.toString();
		}
		return null;
	}

	public File getAppConfigDirectory() {
		String configDirStr = getConfigurationProperty(GridAppsDConstants.APPLICATIONS_PATH);
		if (configDirStr == null) {
			configDirStr = "applications";
		}

		File configDir = new File(configDirStr);
		if (!configDir.exists()) {
			configDir.mkdirs();
			if (!configDir.exists()) {
				throw new RuntimeException("Applications directory "
						+ configDir.getAbsolutePath()
						+ " does not exist and cannot be created.");
			}
		}

		return configDir;

	}

	protected AppInfo parseAppInfo(File appConfigFile) {
		AppInfo appInfo = null;

		if (!appConfigFile.exists()) {
			throw new RuntimeException("App config file does not exist: "
					+ appConfigFile.getAbsolutePath());
		}

		try {
			String appConfigStr = new String(Files.readAllBytes(appConfigFile
					.toPath()));
			appInfo = AppInfo.parse(appConfigStr);
		} catch (IOException e) {
			logManager.log(new LogMessage(this.getClass().getName(),null,new Date().getTime(), "Error while reading app config file: "+e.getMessage(), LogLevel.ERROR, ProcessStatus.ERROR, false),username,GridAppsDConstants.topic_platformLog);
		}

		return appInfo;

	}

	protected void writeAppInfo(AppInfo appInfo) {
		File appConfigDirectory = getAppConfigDirectory();

		File confFile = new File(appConfigDirectory.getAbsolutePath()
				+ File.separator + appInfo.getId() + CONFIG_FILE_EXT);
		try {
			Files.write(confFile.toPath(), appInfo.toString().getBytes());
		} catch (IOException e) {
			logManager.log(new LogMessage(this.getClass().getName(),null, new Date().getTime(), "Error while writing app config file: "+e.getMessage(), LogLevel.ERROR, ProcessStatus.ERROR, false),username,GridAppsDConstants.topic_platformLog);
		}
	}

	private static final int BUFFER_SIZE = 4096;

	private void extractFile(ZipInputStream zipIn, String filePath)
			throws IOException {
		BufferedOutputStream bos = new BufferedOutputStream(
				new FileOutputStream(filePath));
		byte[] bytesIn = new byte[BUFFER_SIZE];
		int read = 0;
		while ((read = zipIn.read(bytesIn)) != -1) {
			bos.write(bytesIn, 0, read);
		}
		bos.close();
	}

	private void watch(final AppInstance appInstance) {
		System.out.println("WATCHING "+appInstance.getInstance_id());
	    new Thread() {
	        public void run() {
	            BufferedReader input = new BufferedReader(new InputStreamReader(appInstance.getProcess().getInputStream()));
	            String line = null; 
	            try {
	                while ((line = input.readLine()) != null) {
	                	logManager.log(new LogMessage(this.getClass().getName(),appInstance.getInstance_id(), new Date().getTime(), line, LogLevel.INFO, ProcessStatus.RUNNING, false), username, GridAppsDConstants.topic_platformLog);
	                }
	            } catch (IOException e) {
	            	e.printStackTrace();
                	logManager.log(new LogMessage(this.getClass().getName(),appInstance.getInstance_id(), new Date().getTime(), e.getMessage(), LogLevel.ERROR, ProcessStatus.ERROR, false), username, GridAppsDConstants.topic_platformLog);
	            }
	        }
	    }.start();
	}

	private List<String> splitOptionsString(String optionsStr) {
		// first replace all \" with a string that won't get parsed
		optionsStr = optionsStr.replaceAll("\\\\\"", "ESC_QUOTE");
		List<String> list = new ArrayList<String>();
		Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(
				optionsStr);
		while (m.find()) {
			// revert back all ESC_QUOTE to \"
			String groupStr = m.group(1);
			groupStr = groupStr.replaceAll("ESC_QUOTE", "\\\\\"");
			list.add(groupStr); // Add .replace("\"", "") to remove surrounding
								// quotes.
		}

		return list;
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		for (AppInstance instance : appInstances.values()) {
			instance.getProcess().destroyForcibly();
		}
	}
}
