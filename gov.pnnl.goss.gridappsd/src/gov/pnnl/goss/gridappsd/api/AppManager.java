package gov.pnnl.goss.gridappsd.api;

import java.io.Serializable;
import java.util.List;

import gov.pnnl.goss.gridappsd.dto.AppInfo;
import gov.pnnl.goss.gridappsd.dto.AppInstance;
import pnnl.goss.core.DataResponse;

public interface AppManager {
	
	void process(StatusReporter statusReporter,
			int processId, DataResponse event, Serializable message) throws Exception;

	void registerApp(AppInfo appInfo, byte[] appPackage) throws Exception;
	
	List<AppInfo> listApps();  
	
	List<AppInstance> listRunningApps(); 

	List<AppInstance> listRunningApps(String appId);
	
	AppInfo getApp(String appId); //Would return through message bus appInfo object
	
	void deRegisterApp(String appId); 
	
	String startApp(String appId, String runtimeOptions, String requestId);  //may also need input/output topics or simulation id, would return app instance id
	
	String startAppForSimultion(String appId, String runtimeOptions, String simulationId, String requestId);  //may also need input/output topics??, would return app instance id
	
	void stopApp(String appId);  

	void stopAppInstance(String instanceId);

	
}
