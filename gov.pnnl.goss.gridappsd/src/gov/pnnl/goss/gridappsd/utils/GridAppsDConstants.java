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
import java.util.TimeZone;

public class GridAppsDConstants {
	
	//user credentials
	public static final String username = "system";
	public static final String password = "manager";
	
	//topics
	private static final String topic_prefix = "goss.gridappsd";
	
	//Process Manager topics
	public static final String topic_process_prefix = topic_prefix+".process";
	
	//Process Manager Request Topics
	public static final String topic_requestSimulation = topic_process_prefix+".request.simulation";
	public static final String topic_requestData = topic_process_prefix+".request.data";
	public static final String topic_requestApp = topic_process_prefix+".request.app";
	public static final String topic_requestSimulationStatus = topic_process_prefix+".request.status.simulation";
	
	//App Request Topics
	public static final String topic_app_register = topic_requestApp+".register";
	public static final String topic_app_list = topic_requestApp+".list";
	public static final String topic_app_get = topic_requestApp+".get";
	public static final String topic_app_deregister = topic_requestApp+".deregister";
	public static final String topic_app_start = topic_requestApp+".start";
	public static final String topic_app_stop = topic_requestApp+".stop";
	public static final String topic_app_stop_instance = topic_requestApp+".stopinstance";
	
	
	//Process Manager Log Messages Topic 
	public static final String topic_log_prefix = topic_process_prefix+".log";
	public static final String topic_log_simulation = topic_log_prefix+".simulation";
	public static final String topic_log_service = topic_log_prefix+".service";
	public static final String topic_log_test = topic_log_prefix+".test";
	public static final String topic_log_app = topic_log_prefix+".app";
	
	//Configuration Manager topics
	public static final String topic_configuration = topic_prefix+".configuration";
	public static final String topic_configuration_powergrid = topic_configuration+".powergrid";
	public static final String topic_configuration_simulation = topic_configuration+".simulation";
	
	//Simulation Manager Topics
	public static final String topic_simulation = topic_prefix+".simulation";
	public static final String topic_simulationOutput = topic_simulation+".output";
	public static final String topic_simulationStatus = topic_simulation+".status.";
	
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

	public static final SimpleDateFormat SDF_SIMULATION_REQUEST = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	public static final SimpleDateFormat SDF_GLM_CLOCK = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static final SimpleDateFormat GRIDAPPSD_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	
	static{
		SDF_GLM_CLOCK.setTimeZone(TimeZone.getTimeZone("UTC"));
		SDF_SIMULATION_REQUEST.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	
}