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

import gov.pnnl.goss.gridappsd.api.ConfigurationHandler;
import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.PowergridModelDataManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.SimulationContext;
import gov.pnnl.goss.gridappsd.dto.YBusExportResponse;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
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
	private volatile PowergridModelDataManager powergridModelManager;
	
	@ServiceDependency 
	volatile LogManager logManager;
	
	public static final String TYPENAME = "YBus Export";
	public static final String SIMULATIONID = "simulation_id";
	
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
		
		
		String simulationId = parameters.getProperty(SIMULATIONID);
		
		if(simulationId==null)
			throw new Exception("Simulation Id not provided in request paramters.");
		
		SimulationContext simulationContext = simulationManager.getSimulationContextForId(simulationId);
		
		if(simulationContext==null)
			throw new Exception("Simulation context not found for simulation_id = "+simulationId);
		
		parameters.put("i_fraction", Double.toString(simulationContext.getRequest().getSimulation_config().getModel_creation_config().getiFraction()));
		parameters.put("z_fraction", Double.toString(simulationContext.getRequest().getSimulation_config().getModel_creation_config().getzFraction()));
		parameters.put("p_fraction", Double.toString(simulationContext.getRequest().getSimulation_config().getModel_creation_config().getpFraction()));
		parameters.put("load_scaling_factor", Double.toString(simulationContext.getRequest().getSimulation_config().getModel_creation_config().getLoadScalingFactor()));
		parameters.put("schedule_name", simulationContext.getRequest().getSimulation_config().getModel_creation_config().getScheduleName());
		parameters.put("model_id", simulationContext.getRequest().getPower_system_config().getLine_name());
		parameters.put("directory",simulationContext.getSimulationDir());
		parameters.put("simulation_start_time",simulationContext.getRequest().getSimulation_config().getStart_time());
		parameters.put("simulation_duration",simulationContext.getRequest().getSimulation_config().getDuration());
		
		File simulationDir = new File(simulationContext.getSimulationDir());
		File commandFile = new File(simulationDir,"opendsscmdInput.txt");
		File dssBaseFile = new File(simulationDir,"model_base.dss");
		
		
		for(Object key: parameters.keySet().toArray()){
			log.debug(key.toString() + " = "+ parameters.getProperty(key.toString()));
		}
		
		logManager.log(new LogMessage(this.getClass().getSimpleName(), 
				simulationId, new Date().getTime(), 
				"Generating DSS base file", 
				LogLevel.DEBUG, 
				ProcessStatus.RUNNING, 
				true), username, GridAppsDConstants.topic_simulationLog+simulationId);
		
		if(!dssBaseFile.exists()) {
			//Create DSS base file if it doesn't already exist
			PrintWriter basePrintWriter = new PrintWriter(new StringWriter());
			DSSAllConfigurationHandler baseConfigurationHandler = new DSSAllConfigurationHandler(logManager,simulationManager,configManager);
			baseConfigurationHandler.generateConfig(parameters, basePrintWriter, simulationId, username);
		}
		
		
		if(!dssBaseFile.exists())
				throw new Exception("Error: Could not create DSS base file to export YBus matrix");
		
		logManager.log(new LogMessage(this.getClass().getSimpleName(), 
				simulationId, new Date().getTime(), 
				"Finished generating DSS base file", 
				LogLevel.DEBUG, 
				ProcessStatus.RUNNING, 
				true), username, GridAppsDConstants.topic_platformLog);
		
		logManager.log(new LogMessage(this.getClass().getSimpleName(), 
				simulationId, new Date().getTime(), 
				"Generating commands file for opendsscmd", 
				LogLevel.DEBUG, 
				ProcessStatus.RUNNING, 
				true), username, GridAppsDConstants.topic_simulationLog+simulationId);
		
		
		//Create file with commands for opendsscmd
		PrintWriter fileWriter = new PrintWriter(commandFile);
		fileWriter.println("redirect model_base.dss");
		// transformer winding ratios must be consistent with base voltages for state estimation
		// regulators should be at tap 0; in case LDC is active, we can not use a no-load solution
		fileWriter.println("batchedit transformer..* wdg=2 tap=1");
		fileWriter.println("batchedit regcontrol..* enabled=false");
		// remove source injections from the Y matrix on solve
		fileWriter.println("batchedit vsource..* enabled=false");
		fileWriter.println("batchedit isource..* enabled=false");
		// remove PC elements from the Y matrix on solve
		fileWriter.println("batchedit load..* enabled=false");
		fileWriter.println("batchedit generator..* enabled=false");
		fileWriter.println("batchedit pvsystem..* enabled=false");
		fileWriter.println("batchedit storage..* enabled=false");
		// solve the system in unloaded condition with regulator taps locked
		fileWriter.println("solve");
		fileWriter.println("export y triplet base_ysparse.csv");
		fileWriter.println("export ynodelist base_nodelist.csv");
		fileWriter.println("export summary base_summary.csv");
		fileWriter.flush();
		fileWriter.close();
		
		logManager.log(new LogMessage(this.getClass().getSimpleName(), 
				simulationId, new Date().getTime(), 
				"Finished generating commands file for opendsscmd", 
				LogLevel.DEBUG, 
				ProcessStatus.RUNNING, 
				true), username, GridAppsDConstants.topic_platformLog);
		
		logManager.log(new LogMessage(this.getClass().getSimpleName(), 
				simulationId, new Date().getTime(), 
				"Generating Y Bus matrix", 
				LogLevel.DEBUG, 
				ProcessStatus.RUNNING, 
				true), username, GridAppsDConstants.topic_simulationLog+simulationId);
		
		ProcessBuilder processServiceBuilder = new ProcessBuilder();
		processServiceBuilder.directory(simulationDir);
		List<String> commands = new ArrayList<String>();
		commands.add("opendsscmd");
		commands.add(commandFile.getName());
		
		processServiceBuilder.command(new ArrayList<>(Arrays.asList("opendsscmd", commandFile.getName())));
		processServiceBuilder.redirectErrorStream(true);
		processServiceBuilder.redirectOutput();
		Process process = processServiceBuilder.start();
		process.waitFor();
		
		
		YBusExportResponse response = new YBusExportResponse();
		
		File yparsePath = new File(simulationDir.getAbsolutePath()+File.separator+"base_ysparse.csv");
		File nodeListPath = new File(simulationDir.getAbsolutePath()+File.separator+"base_nodelist.csv");
		File summaryPath = new File(simulationDir.getAbsolutePath()+File.separator+"base_summary.csv");
		
		response.setyParse(Files.readAllLines(Paths.get(yparsePath.getPath())));
		response.setNodeList(Files.readAllLines(Paths.get(nodeListPath.getPath())));
		response.setSummary(Files.readAllLines(Paths.get(summaryPath.getPath())));
		
		logManager.log(new LogMessage(this.getClass().getSimpleName(), 
				simulationId, new Date().getTime(), 
				"Finished generating Y Bus matrix", 
				LogLevel.DEBUG, 
				ProcessStatus.RUNNING, 
				true), username, GridAppsDConstants.topic_simulationLog+simulationId);
			
		out.print(response);
		

	}
	
	
	
}
