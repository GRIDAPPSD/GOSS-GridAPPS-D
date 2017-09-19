package gov.pnnl.goss.gridappsd.api;

import gov.pnnl.goss.gridappsd.dto.AppInfo;
import gov.pnnl.goss.gridappsd.dto.ServiceInstance;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

public interface ServiceManager {

	void registerService(AppInfo appInfo, Serializable appPackage);
	
	void listServices();  //Would return through message bus list of appInfo objects
	
	void getService(String service_id); //Would return through message bus appInfo object
	
	void deRegisterService(String service_id); 
	
	String startService(String service_id, String runtimeOptions);  //may also need input/output topics or simulation id
	
	String startServiceForSimultion(String service_id, String runtimeOptions, String simulationId);  //may also need input/output topics??
	
	void stopService(String service_id);  
	
	List<ServiceInstance> listRunningServices(); 

	List<ServiceInstance> listRunningServices(String serviceId);
	
}
