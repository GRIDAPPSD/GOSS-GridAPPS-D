package gov.pnnl.goss.gridappsd.api;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import gov.pnnl.goss.gridappsd.dto.ServiceInfo;
import gov.pnnl.goss.gridappsd.dto.ServiceInstance;

public interface ServiceManager {

	void registerService(ServiceInfo appInfo, Serializable appPackage);
	
	List<ServiceInfo> listServices();  //Would return through message bus list of appInfo objects
	
	ServiceInfo getService(String service_id); //Would return through message bus appInfo object
	
	void deRegisterService(String service_id); 
	
	String startService(String service_id, String runtimeOptions);  //may also need input/output topics or simulation id
	
	String startServiceForSimultion(String service_id, String runtimeOptions, String simulationId);  //may also need input/output topics??
	
	void stopService(String service_id);  
	
	List<ServiceInstance> listRunningServices(); 

	List<ServiceInstance> listRunningServices(String serviceId);

	void stopServiceInstance(String instanceId);
	
	File getServiceConfigDirectory();
}
