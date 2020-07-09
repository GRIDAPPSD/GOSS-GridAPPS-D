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

import gov.pnnl.goss.cim2glm.CIMImporter;
import gov.pnnl.goss.cim2glm.dto.ModelState;
import gov.pnnl.goss.cim2glm.queryhandler.QueryHandler;
import gov.pnnl.goss.gridappsd.api.ConfigurationHandler;
import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.PowergridModelDataManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.data.handlers.BlazegraphQueryHandler;
import gov.pnnl.goss.gridappsd.dto.SimulationContext;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;

import java.io.File;
import java.io.PrintWriter;
import java.util.Properties;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import pnnl.goss.core.Client;


@Component
public class DSSAllConfigurationHandler extends BaseConfigurationHandler implements ConfigurationHandler {

	private static Logger log = LoggerFactory.getLogger(DSSAllConfigurationHandler.class);
	Client client = null; 
	
	@ServiceDependency
	private volatile ConfigurationManager configManager;
	@ServiceDependency
	private volatile PowergridModelDataManager powergridModelManager;
	@ServiceDependency 
	volatile LogManager logManager;
	private volatile SimulationManager simulationManager;
	
	
	public static final String TYPENAME = "DSS All";
	public static final String DIRECTORY = "directory";
	public static final String SIMULATIONNAME = "simulation_name";
	public static final String ZFRACTION = "z_fraction";
	public static final String IFRACTION = "i_fraction";
	public static final String PFRACTION = "p_fraction";
	public static final String RANDOMIZEFRACTIONS = "randomize_zipload_fractions";
	public static final String USEHOUSES = "use_houses";
	public static final String SCHEDULENAME = "schedule_name";
	public static final String LOADSCALINGFACTOR = "load_scaling_factor";
	public static final String MODELID = "model_id";
	public static final String SOLVERMETHOD = "solver_method";
	public static final String SIMULATIONSTARTTIME = "simulation_start_time";
	public static final String SIMULATIONDURATION = "simulation_duration";
	public static final String SIMULATIONID = "simulation_id";
	public static final String SIMULATIONBROKERHOST = "simulation_broker_host";
	public static final String SIMULATIONBROKERPORT = "simulation_broker_port";
	
	public static final String CONFIGTARGET = "dss";
	
	public static final String CIM2DSS_PREFIX = "model";
	
	public DSSAllConfigurationHandler() {
	}
	 
	public DSSAllConfigurationHandler(LogManager logManager, SimulationManager simulationManager, ConfigurationManager configManager) {
		this.logManager = logManager;
		this.simulationManager = simulationManager;
		this.configManager = configManager;

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
		
		if(powergridModelManager == null){
			//TODO send log message and exception
		}
	}

	@Override
	public void generateConfig(Properties parameters, PrintWriter out, String processId, String username) throws Exception {
		boolean bWantZip = false;
		boolean bWantSched = false;
		SimulationContext simContext = null;

		logRunning("Generating all DSS configuration files using parameters: "+parameters, processId, username, logManager);
		
		
		double zFraction = GridAppsDConstants.getDoubleProperty(parameters, ZFRACTION, 0);
		if(zFraction==0) {
			zFraction = 0;
			bWantZip = true;
		}
		double iFraction = GridAppsDConstants.getDoubleProperty(parameters, IFRACTION, 0);
		if(iFraction==0){
			iFraction = 1;
			bWantZip = true;
		}
		double pFraction = GridAppsDConstants.getDoubleProperty(parameters, PFRACTION, 0);
		if(pFraction==0){
			pFraction = 0;
			bWantZip = true;
		}
		
		boolean bWantRandomFractions = GridAppsDConstants.getBooleanProperty(parameters, RANDOMIZEFRACTIONS, false);
				
		double loadScale = GridAppsDConstants.getDoubleProperty(parameters, LOADSCALINGFACTOR, 1);
		
		String scheduleName = GridAppsDConstants.getStringProperty(parameters, SCHEDULENAME, null);
		if(scheduleName!=null && scheduleName.trim().length()>0){
			bWantSched = true;
		}
		String directory = GridAppsDConstants.getStringProperty(parameters, DIRECTORY, null);
		if(directory==null || directory.trim().length()==0){
			logError("No "+DIRECTORY+" parameter provided", processId, username, logManager);
			throw new Exception("Missing parameter "+DIRECTORY);
		}
		
		String modelId = GridAppsDConstants.getStringProperty(parameters, MODELID, null);
		if(modelId==null || modelId.trim().length()==0){
			logError("No "+MODELID+" parameter provided", processId, username, logManager);
			throw new Exception("Missing parameter "+MODELID);
		}
		String bgHost = configManager.getConfigurationProperty(GridAppsDConstants.BLAZEGRAPH_HOST_PATH);
		if(bgHost==null || bgHost.trim().length()==0){
			bgHost = BlazegraphQueryHandler.DEFAULT_ENDPOINT; 
		}
		/*String simulationID = GridAppsDConstants.getStringProperty(parameters, SIMULATIONID, null);
		int simId = -1;
		if(simulationID==null || simulationID.trim().length()==0){
			logError("No "+SIMULATIONID+" parameter provided", processId, username, logManager);
			throw new Exception("Missing parameter "+SIMULATIONID);
		}
		try{
			simId = new Integer(simulationID);
		}catch (Exception e) {
			logError("Simulation ID not a valid integer "+simulationID+", defaulting to "+simId, simulationID, username, logManager);
		}*/
		
		 ModelState modelState = new ModelState();
		 String modelStateStr = GridAppsDConstants.getStringProperty(parameters, MODELSTATE, null);
		 if(modelStateStr==null || modelStateStr.trim().length()==0){
			 logRunning("No "+MODELSTATE+" parameter provided", processId, username, logManager);
		 } else {
			 Gson  gson = new Gson();
			 modelState = gson.fromJson(modelStateStr, ModelState.class);
		 }
		
		/*long simulationStartTime = GridAppsDConstants.getLongProperty(parameters, SIMULATIONSTARTTIME, -1);
		if(simulationStartTime<0){
			logError("No "+SIMULATIONSTARTTIME+" parameter provided", processId, username, logManager);
			throw new Exception("Missing parameter "+SIMULATIONSTARTTIME);
		}
		long simulationDuration = GridAppsDConstants.getLongProperty(parameters, SIMULATIONDURATION, 0);
		if(simulationDuration==0){
			logError("No "+SIMULATIONDURATION+" parameter provided", processId, username, logManager);
			throw new Exception("Missing parameter "+SIMULATIONDURATION);
		}*/
		
		QueryHandler queryHandler = new BlazegraphQueryHandler(bgHost, logManager, processId, username);
		queryHandler.addFeederSelection(modelId);
		
		File dir = new File(directory);
		if(!dir.exists()){
			dir.mkdirs();
		}
		String fRoot = dir.getAbsolutePath()+File.separator+CIM2DSS_PREFIX;
		
		boolean useHouses = GridAppsDConstants.getBooleanProperty(parameters, USEHOUSES, false);
		
		boolean bHaveEventGen = true;
		
		//TODO add climate
		
		//CIM2GLM utility uses 
		CIMImporter cimImporter = new CIMImporter(); 
		cimImporter.start(queryHandler, CONFIGTARGET, fRoot, scheduleName, loadScale, bWantSched, bWantZip, bWantRandomFractions, useHouses, zFraction, iFraction, pFraction, bHaveEventGen, modelState, false);
		
		logRunning("Finished generating all DSS configuration files.", processId, username, logManager);
		
		

	}

}
