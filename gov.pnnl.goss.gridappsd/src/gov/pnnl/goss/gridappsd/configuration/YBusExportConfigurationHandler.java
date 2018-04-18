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
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import gov.pnnl.goss.gridappsd.api.ConfigurationHandler;
import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.SimulationContext;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;


@Component
public class YBusExportConfigurationHandler implements ConfigurationHandler {

	private static Logger log = LoggerFactory.getLogger(YBusExportConfigurationHandler.class);
	
	@ServiceDependency
	private volatile ConfigurationManager configManager;
	
	@ServiceDependency
	private volatile SimulationManager simulationManager;
	
	@ServiceDependency
	private volatile DataManager dataManager;
	
	@ServiceDependency 
	volatile LogManager logManager;
	
	public static final String TYPENAME = "YBus Export";
	
	public YBusExportConfigurationHandler() {
	}
	 
	public YBusExportConfigurationHandler(LogManager logManager) {

	}
	
	
	@Start
	public void start(){
		if(configManager!=null) {
			configManager.registerConfigurationHandler(TYPENAME, this);
		}
		else { 
			//TODO send log message and exception
			log.warn("No Config manager avilable for "+getClass());
		}
	}

	@Override
	public void generateConfig(Properties parameters, PrintWriter out, String processId, String username) throws Exception {
		
		
		String simulationId = parameters.getProperty("simulationId");
		SimulationContext simulationContext = simulationManager.getSimulationContextForId(simulationId);
		parameters.remove("simulationId");
		
		parameters.put("i_fraction", Double.toString(simulationContext.getRequest().getSimulation_config().getModel_creation_config().getiFraction()));
		parameters.put("z_fraction", Double.toString(simulationContext.getRequest().getSimulation_config().getModel_creation_config().getzFraction()));
		parameters.put("p_fraction", Double.toString(simulationContext.getRequest().getSimulation_config().getModel_creation_config().getpFraction()));
		parameters.put("model_id", simulationContext.getRequest().getPower_system_config().getLine_name());
		parameters.put("load_scaling_factor", Double.toString(simulationContext.getRequest().getSimulation_config().getModel_creation_config().getLoadScalingFactor()));
		parameters.put("schedule_name", simulationContext.getRequest().getSimulation_config().getModel_creation_config().getScheduleName());
		
		File simulationDir = new File(simulationContext.getSimulationDir());
		File commandFile = new File(simulationDir,"opendsscmdInput.txt");
		File dssBaseFile = new File(simulationDir,"model_base.dss");
		
		
		for(Object key: parameters.keySet().toArray()){
			log.debug(key.toString() + " = "+ parameters.getProperty(key.toString()));
		}
		
		logManager.log(new LogMessage(this.getClass().getSimpleName(), 
				processId, new Date().getTime(), 
				"Generating DSS base file", 
				LogLevel.DEBUG, 
				ProcessStatus.RUNNING, 
				true), username, GridAppsDConstants.topic_platformLog);
		
		//Create DSS base file
		PrintWriter basePrintWriter = new PrintWriter(dssBaseFile);
		DSSBaseConfigurationHandler baseConfigurationHandler = new DSSBaseConfigurationHandler(logManager,configManager);
		baseConfigurationHandler.generateConfig(parameters, basePrintWriter, processId, username);
		
		if(!dssBaseFile.exists()){
			logManager.log(new LogMessage(this.getClass().getSimpleName(), 
					processId, new Date().getTime(), 
					"Error: Could not create DSS base file to export YBus matrix", 
					LogLevel.ERROR, 
					ProcessStatus.ERROR, 
					true), username, GridAppsDConstants.topic_platformLog);
			throw new Exception("Error: Could not create DSS base file to export YBus matrix");
		}
		
		logManager.log(new LogMessage(this.getClass().getSimpleName(), 
				processId, new Date().getTime(), 
				"Generated DSS base file", 
				LogLevel.DEBUG, 
				ProcessStatus.RUNNING, 
				true), username, GridAppsDConstants.topic_platformLog);
		
		logManager.log(new LogMessage(this.getClass().getSimpleName(), 
				processId, new Date().getTime(), 
				"Generating commands file for opendsscmd for simulation Id : "+processId, 
				LogLevel.DEBUG, 
				ProcessStatus.RUNNING, 
				true), username, GridAppsDConstants.topic_platformLog);
		
		
		//Create file with commands for opendsscmd
		PrintWriter fileWriter = new PrintWriter(commandFile);
		fileWriter.println("redirect model_base.dss");
		fileWriter.println("solve");
		fileWriter.println("export y triplet base_ysparse.csv");
		fileWriter.println("export ynodelist base_nodelist.csv");
		fileWriter.println("export summary base_summary.csv");
		fileWriter.flush();
		fileWriter.close();
		
		logManager.log(new LogMessage(this.getClass().getSimpleName(), 
				processId, new Date().getTime(), 
				"Generated commands file for opendsscmd for simulation Id : "+processId, 
				LogLevel.DEBUG, 
				ProcessStatus.RUNNING, 
				true), username, GridAppsDConstants.topic_platformLog);
		
		logManager.log(new LogMessage(this.getClass().getSimpleName(), 
				processId, new Date().getTime(), 
				"Generating Y Bus matrix for simulation Id : "+processId, 
				LogLevel.DEBUG, 
				ProcessStatus.RUNNING, 
				true), username, GridAppsDConstants.topic_platformLog);
		
		ProcessBuilder processServiceBuilder = new ProcessBuilder();
		processServiceBuilder.directory(simulationDir);
		List<String> commands = new ArrayList<String>();
		commands.add("opendsscmd");
		commands.add(commandFile.getName());
		
		processServiceBuilder.command(new ArrayList<>(Arrays.asList("opendsscmd", commandFile.getName())));
		processServiceBuilder.start();
		
		logManager.log(new LogMessage(this.getClass().getSimpleName(), 
				processId, new Date().getTime(), 
				"Generating Y Bus matrix for simulation Id : "+processId, 
				LogLevel.DEBUG, 
				ProcessStatus.RUNNING, 
				true), username, GridAppsDConstants.topic_platformLog);
	
		YBusExportResponse response = new YBusExportResponse();
		response.yParseFilePath = simulationDir.getAbsolutePath()+File.separator+"base_ysparse.csv";
		response.nodeListFilePath = simulationDir.getAbsolutePath()+File.separator+"base_nodelist.csv";
		response.summaryFilePath = simulationDir.getAbsolutePath()+File.separator+"base_summary.csv";
		
		out.write(response.toString());
		

	}
	
class YBusExportResponse implements Serializable{
	
	private static final long serialVersionUID = 1L;

	String yParseFilePath;
	String nodeListFilePath;
	String summaryFilePath;
	
	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}
	
}
	
	
	
	
}
