/*******************************************************************************
 * Copyright  2017, Battelle Memorial Institute All rights reserved.
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
package gov.pnnl.goss.gridappsd.configuration;

import java.io.File;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Properties;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.pnnl.goss.gridappsd.api.ConfigurationHandler;
import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.RequestSimulation;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import pnnl.goss.core.Client;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.Response;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;

/**
 * This class implements subset of functionalities for Internal Functions
 * 405 Simulation Manager and 406 Power System Model Manager.
 * ConfigurationManager is responsible for:
 * - subscribing to configuration topics and 
 * - converting configuration message into simulation configuration files
 *   and power grid model files.
 * @author shar064
 *
 */

@Component
public class ConfigurationManagerImpl implements ConfigurationManager{
	private static final String CONFIG_PID = "pnnl.goss.gridappsd";

	private static Logger log = LoggerFactory.getLogger(ConfigurationManagerImpl.class);
	Client client = null; 
	
	@ServiceDependency
	private volatile ClientFactory clientFactory;
	
	@ServiceDependency 
	private volatile LogManager logManager;
	
	@ServiceDependency 
	private volatile DataManager dataManager;
	
	private Dictionary<String, ?> configurationProperties;
	
	private HashMap<String, ConfigurationHandler> configHandlers = new HashMap<String, ConfigurationHandler>();
	
	
	public ConfigurationManagerImpl() {
	}
	 
	public ConfigurationManagerImpl(LogManager logManager, DataManager dataManager) {
		this.dataManager = dataManager;

	}
	
	
	@Start
	public void start(){
		//TODO send log "Starting configuration manager
		
	}
	
	
	/**
	 * This method returns simulation file path with name.
	 * Return GridLAB-D file path with name for RC1.
	 * @param simulationId
	 * @param configRequest
	 * @return
	 */
	@Override
	public synchronized File getSimulationFile(int simulationId, RequestSimulation powerSystemConfig) throws Exception{
		logManager.log(
				new LogMessage(this.getClass().getName(), new Integer(
						simulationId).toString(), new Date().getTime(),
						"ConfigurationManager.getSimulationFile will be deprecated", LogLevel.WARN,
						ProcessStatus.RUNNING, false), "",
				GridAppsDConstants.topic_platformLog);
		log.debug(powerSystemConfig.toString());
		//TODO call dataManager's method to get power grid model data and create simulation file
		Response resp = dataManager.processDataRequest(powerSystemConfig, null, simulationId, getConfigurationProperty(GridAppsDConstants.GRIDAPPSD_TEMP_PATH));
		
		if(resp!=null && (resp instanceof DataResponse) && (((DataResponse)resp).getData())!=null && (((DataResponse)resp).getData() instanceof File)){
			//Update simulation status after every step, for example:
//			statusReporter.reportStatus(GridAppsDConstants.topic_simulationLog+simulationId, "Simulation files created");
			return (File)((DataResponse)resp).getData();
		}
		
		return null;
		
	}
	
	@ConfigurationDependency(pid=CONFIG_PID)
	public synchronized void updated(Dictionary<String, ?> config)  {
		this.configurationProperties = config;
	}
	
	public String getConfigurationProperty(String key){
		if(this.configurationProperties!=null){
			Object value = this.configurationProperties.get(key);
			if(value!=null)
				return value.toString();
		}
		return null;
	}

	@Override
	public void registerConfigurationHandler(String type, ConfigurationHandler handler) {
		//TODO send to log mgr
		log.info("Registring config "+type+" "+handler.getClass());
		configHandlers.put(type, handler);
	}

	@Override
	public void generateConfiguration(String type, Properties parameters, PrintWriter out, String processId, String username) throws Exception {
		if(configHandlers.containsKey(type) && configHandlers.get(type)!=null){
			configHandlers.get(type).generateConfig(parameters, out, processId, username);
		} else {
			logManager.log(
					new LogMessage(this.getClass().getName(), new Integer(
							processId).toString(), new Date().getTime(),
							"No configuration handler registered for '"+type+"'", LogLevel.ERROR,
							ProcessStatus.ERROR, false), "",
					GridAppsDConstants.topic_platformLog);			
			throw new Exception("No configuration handler registered for '"+type+"'");
		}
	}
	
}
