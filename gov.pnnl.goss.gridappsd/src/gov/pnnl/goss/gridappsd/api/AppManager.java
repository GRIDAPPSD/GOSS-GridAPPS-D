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
package gov.pnnl.goss.gridappsd.api;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import gov.pnnl.goss.gridappsd.dto.AppInfo;
import gov.pnnl.goss.gridappsd.dto.AppInstance;
import pnnl.goss.core.DataResponse;

public interface AppManager {
	/**
	 * Process application request
	 * @param statusReporter
	 * @param processId
	 * @param event
	 * @param message
	 * @throws Exception
	 */
	void process(StatusReporter statusReporter,
			int processId, DataResponse event, Serializable message) throws Exception;

	/**
	 * Register a new app with GridAPPS-D app manager, registered apps will be persisted
	 * @param appInfo App info file that includes details about the app configuration
	 * @param appPackage Zip file containing files required by the app, this includes persistent config files and executable files.
	 * @throws Exception
	 */
	void registerApp(AppInfo appInfo, byte[] appPackage) throws Exception;
	
	/**
	 * Lists the currently registered apps
	 * @return List of AppInfo objects describing the configurations of the registered apps
	 */
	List<AppInfo> listApps();  
	
	/**
	 * Lists currently running app instances
	 * @return List of AppInstance objects
	 */
	List<AppInstance> listRunningApps(); 

	/**
	 * Lists currently running app instances
	 * @param appId Registered ID of the desired app
	 * @return  List of AppInstance objects
	 */
	List<AppInstance> listRunningApps(String appId);
	
	/**
	 * Returns app configuration for the requested app ID
	 * @param appId Registered ID of the desired app
	 * @return AppInfo object containing app configuration
	 */
	AppInfo getApp(String appId); //Would return through message bus appInfo object
	
	/**
	 * Unregisters app with the requested id
	 * @param appId Registered ID of the app to de-register
	 */
	void deRegisterApp(String appId); 
	
	/**
	 * Start app instance
	 * @param appId Registered ID of the desired app
	 * @param runtimeOptions Runtime options for the app instance, in most cases these will be passed in on the command-line
	 * @param requestId 
	 * @return String containing app instance ID
	 */
	String startApp(String appId, String runtimeOptions, String requestId);  //may also need input/output topics or simulation id, would return app instance id
	
	/**
	 * Start app instance
	 * @param appId Registered ID of the desired app
	 * @param runtimeOptions Runtime options for the app instance, in most cases these will be passed in on the command-line
	 * @param simulationId  Associated simulation Id
	 * @param requestId
	 * @return String containing app instance ID
	 */
	String startAppForSimultion(String appId, String runtimeOptions, String simulationId, String requestId);  //may also need input/output topics??, would return app instance id
	
	/**
	 * Stops all instances of the app with requested app ID
	 * @param appId Registered ID of the app to
	 */
	void stopApp(String appId);  

	/**
	 * Stops app instance
	 * @param instanceId ID of the app instance to stop
	 */
	void stopAppInstance(String instanceId);

	/**
	 * Get the directory where the app configurations are stored
	 * @return File location of the directory containing app configurations
	 */
	File getAppConfigDirectory();
	//TODO add isRunning and is Runningforsimulation
	
}
