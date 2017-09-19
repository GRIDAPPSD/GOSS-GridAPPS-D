package gov.pnnl.goss.gridappsd.service;

import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.ServiceManager;
import gov.pnnl.goss.gridappsd.dto.AppInfo;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.ServiceInfo;
import gov.pnnl.goss.gridappsd.dto.ServiceInfo.ServiceType;
import gov.pnnl.goss.gridappsd.dto.ServiceInstance;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;

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

import pnnl.goss.core.ClientFactory;

@Component
public class ServiceManagerImpl implements ServiceManager{
	
	private static final String CONFIG_PID = "pnnl.goss.gridappsd";
	
	@ServiceDependency
	LogManager logManager;
	
	@ServiceDependency
	private volatile ClientFactory clientFactory;
	
	final String CONFIG_DIR_NAME = "config";
	final String CONFIG_FILE_NAME = "serviceinfo.json";
	
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
				new Long(new Date().getTime()).toString(), 
				"Starting "+this.getClass().getName(), 
				"INFO", 
				"Running", 
				true));
		
		scanForServices();
		
		logManager.log(new LogMessage(this.getClass().getName(), 
				new Long(new Date().getTime()).toString(), 
				String.format("Found %s servicelications", services.size()), 
				"INFO", 
				"Running", 
				true));
	}
	
	protected void scanForServices(){
		//Get directory for services from the config
		File serviceConfigDir = getServiceConfigDirectory();
		
		//for each service found, parse the serviceinfo.json config file to create serviceinfo object and add to services map
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
	
	protected File getServiceConfigDirectory(){
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return serviceInfo;
		
	}

	@Override
	public void listServices() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void getService(String service_id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deRegisterService(String service_id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String startService(String service_id,
			String runtimeOptions) {
		// TODO Auto-generated method stub
		return new String();
		
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
	public void stopService(String service_id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void registerService(AppInfo appInfo, Serializable appPackage) {
		// TODO Auto-generated method stub
		
	}
	
	@ConfigurationDependency(pid=CONFIG_PID)
	public synchronized void updated(Dictionary<String, ?> config)  {
		this.configurationProperties = config;
	}
	
	


}
