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
package gov.pnnl.goss.gridappsd.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;


import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;

public class GridAppsDConstants {
	
	//user credentials
	public static final String username = "system";
	public static final String password = "manager";
	
	//topics
	public static final String topic_prefix = "goss.gridappsd";
	
	//Process Manager topics
	public static final String topic_process_prefix = topic_prefix+".process";
	public static final String topic_request = topic_prefix+".process";
	
	
	
	//Process Manager Request Topics
	public static final String topic_requestSimulation = topic_process_prefix+".request.simulation";
	public static final String topic_requestData = topic_process_prefix+".request.data";
	public static final String topic_requestConfig = topic_process_prefix+".request.config";
	public static final String topic_requestApp = topic_process_prefix+".request.app";
	public static final String topic_app_register_remote = topic_requestApp+".remote.register";
	public static final String topic_requestSimulationStatus = topic_process_prefix+".request.status.simulation";
	public static final String topic_requestPlatformStatus = topic_process_prefix+".request.status.platform";
	
	// Remote Application topics
	public static final String topic_remoteapp_prefix = topic_prefix+".remoteapp";
	public static final String topic_remoteapp_heartbeat = topic_remoteapp_prefix+".heartbeat";
	public static final String topic_remoteapp_start = topic_remoteapp_prefix+".start";
	public static final String topic_remoteapp_stop = topic_remoteapp_prefix+".stop";
	public static final String topic_remoteapp_status = topic_remoteapp_prefix+".status";
	
	
	public static final String topic_requestListAppsWithInstances = "goss.gridappsd.process.request.list.apps";
	public static final String topic_requestListServicesWithInstances = "goss.gridappsd.process.request.list.services";
	
	public static final String topic_responseData = topic_prefix+".response.data";
	
	public static final String topic_platformLog = topic_prefix+".platform.log";
	
	//App Request Topics
	public static final String topic_app_register = topic_requestApp+".register";
	public static final String topic_app_list = topic_requestApp+".list";
	public static final String topic_app_get = topic_requestApp+".get";
	public static final String topic_app_deregister = topic_requestApp+".deregister";
	public static final String topic_app_start = topic_requestApp+".start";
	public static final String topic_app_stop = topic_requestApp+".stop";
	public static final String topic_app_stop_instance = topic_requestApp+".stopinstance";
	
	
	//Configuration Manager topics
	public static final String topic_configuration = topic_prefix+".configuration";
	public static final String topic_configuration_powergrid = topic_configuration+".powergrid";
	public static final String topic_configuration_simulation = topic_configuration+".simulation";
	
	//Simulation Topics
	public static final String topic_simulation = topic_prefix+".simulation";
	public static final String topic_simulationInput = topic_simulation+".input";
	public static final String topic_simulationOutput = topic_simulation+".output";
	public static final String topic_simulationLog = topic_simulation+".log.";
	
	//Service Topics
	public static final String topic_service = topic_prefix+".simulation";
	public static final String topic_serviceInput = topic_service+".input";
	public static final String topic_serviceOutput = topic_service+".output";
	public static final String topic_serviceLog = topic_service+".log";
		
	//Application Topics
	public static final String topic_application = topic_prefix+".simulation";
	public static final String topic_applicationInput = topic_application+".input";
	public static final String topic_applicationOutput = topic_application+".output";
	public static final String topic_applicationLog = topic_application+".log";
	
	//Test topics
	public static final String topic_test = topic_prefix+".test";
	public static final String topic_testInput = topic_test+".input";
	public static final String topic_testOutput = topic_test+".output";
	public static final String topic_testLog = topic_test+".log";
	
	//Data Manager Topics
	public static final String topic_getDataFilesLocation = topic_prefix+".data.filesLocation";
	public static final String topic_getDataContent = topic_prefix+".data.content";
	
	//FNCS GOSS Bridge Topics
	public static final String topic_FNCS = topic_prefix+".fncs";
	public static final String topic_FNCS_input = topic_FNCS+".input";
	public static final String topic_FNCS_output = topic_FNCS+".output";
	
	
	public static final String FNCS_PATH = "fncs.path";
	public static final String FNCS_BRIDGE_PATH = "fncs.bridge.path";
	public static final String VVO_APP_PATH = "vvo.app.path";
	public static final String GRIDLABD_PATH = "gridlabd.path";
	public static final String GRIDAPPSD_TEMP_PATH = "gridappsd.temp.path";
	public static final String APPLICATIONS_PATH = "applications.path";
	public static final String SERVICES_PATH = "services.path";
	public static final String BLAZEGRAPH_HOST_PATH = "blazegraph.host.path";
	public static final String PROVEN_PATH = "proven.path";

	public static final SimpleDateFormat SDF_SIMULATION_REQUEST = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	public static final SimpleDateFormat SDF_GLM_CLOCK = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static final SimpleDateFormat GRIDAPPSD_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	
	static{
		SDF_GLM_CLOCK.setTimeZone(TimeZone.getTimeZone("UTC"));
		SDF_SIMULATION_REQUEST.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	
	
	/**
	 * Helper method to get double value from properties, if not found returns default
	 * @param props
	 * @param keyName
	 * @param defaultValue
	 * @return
	 */
	public static double getDoubleProperty(Properties props, String keyName, double defaultValue){
		System.out.println(props);
		if(props.containsKey(keyName)){
			String val = props.getProperty(keyName);
			return new Double(val).doubleValue();
		}
		
		return defaultValue;
	}
	/**
	 * Helper method to get boolean value from properties, if not found returns default
	 * @param props
	 * @param keyName
	 * @param defaultValue
	 * @return
	 */
	public static boolean getBooleanProperty(Properties props, String keyName, boolean defaultValue){
		System.out.println(props);
		if(props.containsKey(keyName)){
			String val = props.getProperty(keyName);
			return new Boolean(val).booleanValue();
		}
		
		return defaultValue;
	}
	
	/**
	 * Helper method to get String value from properties, if not found returns default
	 * @param props
	 * @param keyName
	 * @param defaultValue
	 * @return
	 */
	public static String getStringProperty(Properties props, String keyName, String defaultValue){
		if(props.containsKey(keyName)){
			return props.getProperty(keyName);
		}
		
		return defaultValue;
	}
	
	
	/**
	 * Helper method to get String value from properties, if not found returns default
	 * @param props
	 * @param keyName
	 * @param defaultValue
	 * @return
	 */
	public static long getLongProperty(Properties props, String keyName, long defaultValue){
		
		if(props.containsKey(keyName)){
			Object val = props.get(keyName);
			if(val instanceof Long){
				return ((Long)val).longValue();
			} else if (val instanceof Integer){
				return ((Integer)val).longValue();
			} else if (val instanceof String){
				return new Long((String)val).longValue();
			} else {
				throw new RuntimeException("Unrecognized type when reading "+keyName+", for parameter value "+val.getClass());
			}
//			return new Long(props.getProperty(keyName)).longValue();
		}
		
		return defaultValue;
	}
	
	
	public static void logMessage(LogManager logManager, String fromClass, String message, String simulationID, String username, LogLevel logLevel){
		
		if(logManager!=null){
			if(logLevel==LogLevel.ERROR){
				logManager.log(
						new LogMessage(fromClass, simulationID, new Date().getTime(),
								message, LogLevel.ERROR,
								ProcessStatus.ERROR, false), username,
						GridAppsDConstants.topic_platformLog);
			} else {
				logManager.log(
						new LogMessage(fromClass, simulationID, new Date().getTime(),
								message, logLevel,
								ProcessStatus.RUNNING, false), username,
						GridAppsDConstants.topic_platformLog);
			}
			
		} else {
			//???  what to do if they didn't set a log manager?
		}
	}
}