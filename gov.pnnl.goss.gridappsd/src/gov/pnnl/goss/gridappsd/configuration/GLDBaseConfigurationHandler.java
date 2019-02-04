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
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Properties;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.pnnl.goss.cim2glm.CIMImporter;
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
import pnnl.goss.core.Client;


@Component
public class GLDBaseConfigurationHandler extends BaseConfigurationHandler implements ConfigurationHandler {

	private static Logger log = LoggerFactory.getLogger(GLDBaseConfigurationHandler.class);
	Client client = null; 
	@ServiceDependency
	private volatile LogManager logManager;
	@ServiceDependency
	private volatile ConfigurationManager configManager;
	@ServiceDependency
	private volatile SimulationManager simulationManager;
	@ServiceDependency
	private volatile PowergridModelDataManager powergridModelManager;
	
	public static final String TYPENAME = "GridLAB-D Base GLM";
	public static final String ZFRACTION = "z_fraction";
	public static final String IFRACTION = "i_fraction";
	public static final String PFRACTION = "p_fraction";
	public static final String SCHEDULENAME = "schedule_name";
	public static final String LOADSCALINGFACTOR = "load_scaling_factor";
	public static final String MODELID = "model_id";
	public static final String SIMULATIONID = "simulation_id";
	public static final String USEHOUSES = "use_houses";

	public GLDBaseConfigurationHandler() {
	}
	 
	public GLDBaseConfigurationHandler(LogManager logManager, DataManager dataManager) {

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
		logRunning("Generating Base GridLAB-D configuration file using parameters: "+parameters, processId, username, logManager);

		String simulationId = GridAppsDConstants.getStringProperty(parameters, SIMULATIONID, null);
		File configFile = null;
		if(simulationId!=null){
			SimulationContext simulationContext = simulationManager.getSimulationContextForId(simulationId);
			if(simulationContext!=null){
				configFile = new File(simulationContext.getSimulationDir()+File.separator+GLDAllConfigurationHandler.BASE_FILENAME);
				//If the config file already has been created for this simulation then return it
				if(configFile.exists()){
					printFileToOutput(configFile, out);
					logRunning("Dictionary GridLAB-D base file for simulation "+simulationId+" already exists.", processId, username, logManager);
					return;
				}
			} else {
				logRunning("No simulation context found for simulation_id: "+simulationId, processId, username, logManager, LogLevel.WARN);
			}
		}
		
		boolean bWantZip = false;
		boolean bWantSched = false;

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
		boolean bWantRandomFractions = GridAppsDConstants.getBooleanProperty(parameters, GLDAllConfigurationHandler.RANDOMIZEFRACTIONS, false);

		double loadScale = GridAppsDConstants.getDoubleProperty(parameters, LOADSCALINGFACTOR, 1);
		
		String scheduleName = GridAppsDConstants.getStringProperty(parameters, SCHEDULENAME, null);
		if(scheduleName!=null){
			bWantSched = true;
		}
		
		String modelId = GridAppsDConstants.getStringProperty(parameters, MODELID, null);
		if(modelId==null || modelId.trim().length()==0){
			throw new Exception("Missing parameter "+MODELID);
		}
		
		
		String bgHost = configManager.getConfigurationProperty(GridAppsDConstants.BLAZEGRAPH_HOST_PATH);
		if(bgHost==null || bgHost.trim().length()==0){
			bgHost = BlazegraphQueryHandler.DEFAULT_ENDPOINT; 
		}
		
		//TODO write a query handler that uses the built in powergrid model data manager that talks to blazegraph internally
		QueryHandler queryHandler = new BlazegraphQueryHandler(bgHost, logManager, processId, username);
		queryHandler.addFeederSelection(modelId);
		
		boolean useHouses = GridAppsDConstants.getBooleanProperty(parameters, USEHOUSES, false);
		
		CIMImporter cimImporter = new CIMImporter(); 
		
		//If the simulation info is available also write to file
		if(configFile!=null){
			cimImporter.generateGLMFile(queryHandler, new PrintWriter(new FileWriter(configFile)), scheduleName, loadScale, bWantSched, bWantZip, bWantRandomFractions, useHouses, zFraction, iFraction, pFraction);
		} else {
			cimImporter.generateGLMFile(queryHandler, out, scheduleName, loadScale, bWantSched, bWantZip, bWantRandomFractions, useHouses,zFraction, iFraction, pFraction);
		}
		if(configFile!=null){
			//config was written to file, so return that
			printFileToOutput(configFile, out);
		}
		logRunning("Finished generating Base GridLAB-D configuration file.", processId, username, logManager);

	}
	
	
	
	
}
