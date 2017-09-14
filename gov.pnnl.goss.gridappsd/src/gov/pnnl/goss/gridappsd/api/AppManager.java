package gov.pnnl.goss.gridappsd.api;

import java.io.Serializable;
import java.util.HashMap;

import gov.pnnl.goss.gridappsd.dto.AppInfo;

public interface AppManager {

	void registerApp(AppInfo appInfo, Serializable appPackage);
	
	void listApps();  //Would return through message bus list of appInfo objects
	
	void getApp(String app_id); //Would return through message bus appInfo object
	
	void deRegisterApp(String app_id); 
	
	void startApp(String app_id, HashMap<String, String> runtimeOptions);  //may also need input/output topics or simulation id
	
	void startAppForSimultion(String app_id, HashMap<String, String> runtimeOptions, long simulationId);  //may also need input/output topics??
	
	void stopApp(String app_id);  
	
}
