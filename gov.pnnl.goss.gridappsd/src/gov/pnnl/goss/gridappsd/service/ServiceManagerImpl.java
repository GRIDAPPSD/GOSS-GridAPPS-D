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
package gov.pnnl.goss.gridappsd.service;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;

import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.ServiceManager;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.ServiceInfo;
import gov.pnnl.goss.gridappsd.dto.ServiceInfo.ServiceType;
import gov.pnnl.goss.gridappsd.dto.ServiceInstance;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.ClientFactory;

@Component
public class ServiceManagerImpl implements ServiceManager{
	
	private static final String CONFIG_PID = "pnnl.goss.gridappsd";
	
	@ServiceDependency
	LogManager logManager;
	
	@ServiceDependency
	private volatile ClientFactory clientFactory;
	
	private HashMap<String, ServiceInfo> services = new HashMap<String, ServiceInfo>();
	
	private Dictionary<String, ?> configurationProperties;
	
	private HashMap<String, ServiceInstance> serviceInstances = new HashMap<String, ServiceInstance>();
	
	public ServiceManagerImpl() {
	}
	 
	public ServiceManagerImpl(LogManager logManager, ClientFactory clientFactory) {
		this.logManager = logManager;
		this.clientFactory = clientFactory;

	}
	
	@Start
	public void start(){
		//statusReporter.reportStatus(String.format("Starting %s", this.getClass().getName()));
		logManager.log(new LogMessage(this.getClass().getName(), 
				new Date().getTime(), 
				"Starting "+this.getClass().getName(), 
				LogLevel.INFO, 
				ProcessStatus.RUNNING, 
				true),GridAppsDConstants.username);
		
		scanForServices();
		
		logManager.log(new LogMessage(this.getClass().getName(), 
				new Date().getTime(), 
				String.format("Found %s services", services.size()), 
				LogLevel.INFO, 
				ProcessStatus.RUNNING, 
				true),GridAppsDConstants.username);
	}
	
	protected void scanForServices(){
		//Get directory for services from the config
		File serviceConfigDir = getServiceConfigDirectory();
		
		//for each service found, parse the [service].config file to create serviceinfo object and add to services map
		File[] serviceconfigFiles = serviceConfigDir.listFiles(new FileFilter() {
			
			@Override
			public boolean accept(File pathname) {
				if(pathname.isFile() && pathname.getName().endsWith(".config"))
					return true;
				else
					return false;
			}
		});
		for(File serviceConfigFile: serviceconfigFiles){
			ServiceInfo serviceInfo = parseServiceInfo(serviceConfigFile);
			services.put(serviceInfo.getId(), serviceInfo);
		}
	}
	
	public File getServiceConfigDirectory(){
		String configDirStr = getConfigurationProperty(GridAppsDConstants.SERVICES_PATH);
		if (configDirStr==null){
			configDirStr = "services";
		}
		
		File configDir = new File(configDirStr);
		if(!configDir.exists()){
			configDir.mkdirs();
			if(!configDir.exists()){
				throw new RuntimeException("Services directory "+configDir.getAbsolutePath()+" does not exist and cannot be created.");
			}
		}
			
		return configDir;
		
	}
	
	public String getConfigurationProperty(String key){
		if(this.configurationProperties!=null){
			Object value = this.configurationProperties.get(key);
			if(value!=null)
				return value.toString();
		}
		return null;
	}
	
	protected ServiceInfo parseServiceInfo(File serviceConfigFile){
		ServiceInfo serviceInfo = null;
		String serviceConfigStr;
		try {
			serviceConfigStr = new String(Files.readAllBytes(serviceConfigFile.toPath()));
			serviceInfo = ServiceInfo.parse(serviceConfigStr);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return serviceInfo;
		
	}

	@Override
	public List<ServiceInfo> listServices() {
		List<ServiceInfo> result = new ArrayList<ServiceInfo>();
		result.addAll(services.values());
		return result;
		
	}

	@Override
	public ServiceInfo getService(String service_id) {
		service_id = service_id.trim();
		return services.get(service_id);
	}

	@Override
	public String startService(String serviceId,
			String runtimeOptions) {
		return startServiceForSimultion(serviceId, runtimeOptions, null);	
	}

	@Override
	public String startServiceForSimultion(String serviceId, String runtimeOptions, String simulationId) {
		
		String instanceId = serviceId+"-"+new Date().getTime();
		// get execution path
		ServiceInfo serviceInfo = services.get(serviceId);
		if(serviceInfo==null){
			//TODO: publish error on status topic
			throw new RuntimeException("Service not found: "+serviceId);
		}
		
		// are multiple allowed? if not check to see if it is already running, if it is then fail
		if(!serviceInfo.isMultiple_instances() && listRunningServices(serviceId).size()>0){
			throw new RuntimeException("Service is already running and multiple instances are not allowed: "+serviceId);
		}

		//build options
		//might need a standard method for replacing things like SIMULATION_ID in the input/output options
		
		Process process = null;
		//something like 
		if(serviceInfo.getType().equals(ServiceType.PYTHON)){
			List<String> commands = new ArrayList<String>();
			commands.add("python");
			commands.add(serviceInfo.getExecution_path());
			//TODO add other options
			
			
			ProcessBuilder processServiceBuilder = new ProcessBuilder(commands);
			processServiceBuilder.redirectErrorStream(true);
			processServiceBuilder.redirectOutput();
			
//		ProcessBuilder fncsBridgeBuilder = new ProcessBuilder("python", getPath(GridServicesDConstants.FNCS_BRIDGE_PATH), simulationConfig.getSimulation_name());
//		fncsBridgeBuilder.redirectErrorStream(true);
//		fncsBridgeBuilder.redirectOutput(new File(defaultLogDir.getAbsolutePath()+File.separator+"fncs_goss_bridge.log"));
//		fncsBridgeProcess = fncsBridgeBuilder.start();
//		// Watch the process
//		watch(fncsBridgeProcess, "FNCS GOSS Bridge");
		//during watch, send stderr/out to logmanager
			
		} else if(serviceInfo.getType().equals(ServiceType.EXE)){
			List<String> commands = new ArrayList<String>();
			commands.add(serviceInfo.getExecution_path());
			commands.add(runtimeOptions);
			//TODO add other options
			
			
			ProcessBuilder processServiceBuilder = new ProcessBuilder(commands);
			processServiceBuilder.redirectErrorStream(true);
			processServiceBuilder.redirectOutput();
			
//		ProcessBuilder fncsBridgeBuilder = new ProcessBuilder("python", getPath(GridServicesDConstants.FNCS_BRIDGE_PATH), simulationConfig.getSimulation_name());
//		fncsBridgeBuilder.redirectErrorStream(true);
//		fncsBridgeBuilder.redirectOutput(new File(defaultLogDir.getAbsolutePath()+File.separator+"fncs_goss_bridge.log"));
//		fncsBridgeProcess = fncsBridgeBuilder.start();
//		// Watch the process
//		watch(fncsBridgeProcess, "FNCS GOSS Bridge");
		//during watch, send stderr/out to logmanager
			
		} else if(serviceInfo.getType().equals(ServiceType.JAVA)){
//			ProcessBuilder fncsBridgeBuilder = new ProcessBuilder("python", getPath(GridServicesDConstants.FNCS_BRIDGE_PATH), simulationConfig.getSimulation_name());
//			fncsBridgeBuilder.redirectErrorStream(true);
//			fncsBridgeBuilder.redirectOutput(new File(defaultLogDir.getAbsolutePath()+File.separator+"fncs_goss_bridge.log"));
//			fncsBridgeProcess = fncsBridgeBuilder.start();
//			// Watch the process
//			watch(fncsBridgeProcess, "FNCS GOSS Bridge");
			//during watch, send stderr/out to logmanager
				
		} else if(serviceInfo.getType().equals(ServiceType.WEB)){
//			ProcessBuilder fncsBridgeBuilder = new ProcessBuilder("python", getPath(GridServicesDConstants.FNCS_BRIDGE_PATH), simulationConfig.getSimulation_name());
//			fncsBridgeBuilder.redirectErrorStream(true);
//			fncsBridgeBuilder.redirectOutput(new File(defaultLogDir.getAbsolutePath()+File.separator+"fncs_goss_bridge.log"));
//			fncsBridgeProcess = fncsBridgeBuilder.start();
//			// Watch the process
//			watch(fncsBridgeProcess, "FNCS GOSS Bridge");
			//during watch, send stderr/out to logmanager
				
		} else {
			throw new RuntimeException("Type not recognized "+serviceInfo.getType());
		}
		
		
		//create serviceinstance object
		ServiceInstance serviceInstance = new ServiceInstance(instanceId, serviceInfo, runtimeOptions, simulationId, process);
		serviceInstance.setService_info(serviceInfo);
		
		//add to service instances map
		serviceInstances.put(instanceId, serviceInstance);
		
		
		return instanceId;
		
	}

	@Override
	public List<ServiceInstance> listRunningServices() {
		List<ServiceInstance> result = new ArrayList<ServiceInstance>();
		result.addAll(serviceInstances.values());
		return result;
	}
	
	@Override
	public List<ServiceInstance> listRunningServices(String serviceId) {
		List<ServiceInstance> result = new ArrayList<ServiceInstance>();
		for(String instanceId: serviceInstances.keySet()){
			ServiceInstance instance = serviceInstances.get(instanceId);
			if(instance.getService_info().getId().equals(serviceId)){
				result.add(instance);
			}
		}
		return result;
	}
	
	@Override
	public void stopService(String serviceId) {
		serviceId = serviceId.trim();
		for(ServiceInstance instance: listRunningServices(serviceId)){
			if(instance.getService_info().getId().equals(serviceId)){
				stopServiceInstance(instance.getInstance_id());
			}
		}
		
	}
	
	@Override
	public void stopServiceInstance(String instanceId) {
		instanceId = instanceId.trim();
		ServiceInstance instance = serviceInstances.get(instanceId);
		instance.getProcess().destroy();
		serviceInstances.remove(instanceId); 
	}

	@Override
	public void registerService(ServiceInfo appInfo, Serializable appPackage) {
		// TODO Implement this method when service registration request comes on message bus	
	}
	
	@Override
	public void deRegisterService(String service_id) {
		// TODO Auto-generated method stub
		
	}
	
	@ConfigurationDependency(pid=CONFIG_PID)
	public synchronized void updated(Dictionary<String, ?> config)  {
		this.configurationProperties = config;
	}
	
	


}
