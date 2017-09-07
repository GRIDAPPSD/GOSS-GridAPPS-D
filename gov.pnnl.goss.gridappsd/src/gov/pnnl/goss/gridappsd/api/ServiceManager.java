package gov.pnnl.goss.gridappsd.api;

import java.io.Serializable;
import java.util.HashMap;

import gov.pnnl.goss.gridappsd.dto.AppInfo;

public interface ServiceManager {

	void registerService(AppInfo appInfo, Serializable appPackage);
	
	void listServices();  //Would return through message bus list of appInfo objects
	
	void getService(String service_id); //Would return through message bus appInfo object
	
	void deRegisterService(String service_id); 
	
	void startService(String service_id, HashMap<String, String> runtimeOptions);  //may also need input/output topics or simulation id
	
	void startServiceForSimultion(String service_id, HashMap<String, String> runtimeOptions, long simulationId);  //may also need input/output topics??
	
	void stopService(String service_id);  
	
}
