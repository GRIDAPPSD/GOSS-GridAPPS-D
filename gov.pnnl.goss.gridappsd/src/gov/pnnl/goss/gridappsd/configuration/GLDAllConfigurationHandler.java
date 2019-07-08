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
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonSyntaxException;

import gov.pnnl.goss.cim2glm.CIMImporter;
import gov.pnnl.goss.cim2glm.queryhandler.QueryHandler;
import gov.pnnl.goss.gridappsd.api.ConfigurationHandler;
import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.PowergridModelDataManager;
import gov.pnnl.goss.gridappsd.data.ProvenTimeSeriesDataManagerImpl;
import gov.pnnl.goss.gridappsd.data.conversion.ProvenWeatherToGridlabdWeatherConverter;
import gov.pnnl.goss.gridappsd.data.handlers.BlazegraphQueryHandler;
import gov.pnnl.goss.gridappsd.dto.RequestTimeseriesData;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.Client;
import pnnl.goss.core.DataResponse;


@Component
public class GLDAllConfigurationHandler extends BaseConfigurationHandler implements ConfigurationHandler {

	private static Logger log = LoggerFactory.getLogger(GLDAllConfigurationHandler.class);
	Client client = null; 
	
	@ServiceDependency
	private volatile ConfigurationManager configManager;
	@ServiceDependency
	private volatile PowergridModelDataManager powergridModelManager;
	@ServiceDependency 
	volatile LogManager logManager;
	@ServiceDependency 
	volatile DataManager dataManager;
	
	
	public static final String TYPENAME = "GridLAB-D All";
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
	public static final String STARTTIME_FILTER = "startTime";
	public static final String ENDTIME_FILTER = "endTime";
	public static final int TIMEFILTER_YEAR = 2013;
	
	public static final String CONFIGTARGET = "glm";
	
	public static final String CIM2GLM_PREFIX = "model";
	public static final String BASE_FILENAME = CIM2GLM_PREFIX+"_base.glm";
	public static final String STARTUP_FILENAME = CIM2GLM_PREFIX+"_startup.glm";
	public static final String MEASUREMENTOUTPUTS_FILENAME = CIM2GLM_PREFIX+"_outputs.json";
	public static final String DICTIONARY_FILENAME = CIM2GLM_PREFIX+"_dict.json";
	public static final String WEATHER_FILENAME = CIM2GLM_PREFIX+"_weather.csv";
	
	final double sqrt3 = Math.sqrt(3);

	
	public GLDAllConfigurationHandler() {
	}
	 
	public GLDAllConfigurationHandler(LogManager logManager, DataManager dataManager) {
		this.logManager = logManager;
		this.dataManager = dataManager;
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

		logRunning("Generating all GridLAB-D configuration files using parameters: "+parameters, processId, username, logManager);
		
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
		if(scheduleName!=null){
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
		String simulationID = GridAppsDConstants.getStringProperty(parameters, SIMULATIONID, null);
		int simId = -1;
		if(simulationID==null || simulationID.trim().length()==0){
			logError("No "+SIMULATIONID+" parameter provided", processId, username, logManager);
			throw new Exception("Missing parameter "+SIMULATIONID);
		}
		try{
			simId = new Integer(simulationID);
		}catch (Exception e) {
			logError("Simulation ID not a valid integer "+simulationID+", defaulting to "+simId, simulationID, username, logManager);
		}
		long simulationStartTime = GridAppsDConstants.getLongProperty(parameters, SIMULATIONSTARTTIME, -1);
		if(simulationStartTime<0){
			logError("No "+SIMULATIONSTARTTIME+" parameter provided", processId, username, logManager);
			throw new Exception("Missing parameter "+SIMULATIONSTARTTIME);
		}
		long simulationDuration = GridAppsDConstants.getLongProperty(parameters, SIMULATIONDURATION, 0);
		if(simulationDuration==0){
			logError("No "+SIMULATIONDURATION+" parameter provided", processId, username, logManager);
			throw new Exception("Missing parameter "+SIMULATIONDURATION);
		}
		long simulationEndTime = simulationStartTime+(1000*simulationDuration);
		
		QueryHandler queryHandler = new BlazegraphQueryHandler(bgHost, logManager, processId, username);
		queryHandler.addFeederSelection(modelId);
		
		File dir = new File(directory);
		if(!dir.exists()){
			dir.mkdirs();
		}
		String fRoot = dir.getAbsolutePath()+File.separator+CIM2GLM_PREFIX;
		
		boolean useHouses = GridAppsDConstants.getBooleanProperty(parameters, USEHOUSES, false);
		//TODO
		boolean useClimate = true;//GridAppsDConstants.getBooleanProperty(parameters, USECLIMATE, false);
		
		//CIM2GLM utility uses 
		CIMImporter cimImporter = new CIMImporter(); 
		cimImporter.start(queryHandler, CONFIGTARGET, fRoot, scheduleName, loadScale, bWantSched, bWantZip, bWantRandomFractions, useHouses, zFraction, iFraction, pFraction);
		String tempDataPath = dir.getAbsolutePath();
		
		//If use climate, then generate gridlabd weather data file
		try {
			if(useClimate){
				RequestTimeseriesData weatherRequest = new RequestTimeseriesData();
				weatherRequest.setQueryMeasurement("weather");
				weatherRequest.setResponseFormat(ProvenWeatherToGridlabdWeatherConverter.OUTPUT_FORMAT);
				Map<String, String> queryFilter = new HashMap<String, String>();
				
				Calendar c = Calendar.getInstance();
				//For both the start and end time, set the year to the one that currently has data in the database
				//TODO either we need more weather data in the database, or make this more flexible where we only have to search by month/day
				c.setTime(new Date(simulationStartTime*1000));
				c.set(Calendar.YEAR, TIMEFILTER_YEAR);
				//Convert to UTC time until the input time is correct
				////TODO this will be changed in the future
				//c.add(Calendar.HOUR, 6);
				queryFilter.put(STARTTIME_FILTER, ""+c.getTimeInMillis()+"000");
			    c.add(Calendar.SECOND, new Long(simulationDuration).intValue());	
				queryFilter.put(ENDTIME_FILTER, ""+c.getTimeInMillis()+"000");
				weatherRequest.setQueryFilter(queryFilter);
				DataResponse resp = (DataResponse)dataManager.processDataRequest(weatherRequest, ProvenTimeSeriesDataManagerImpl.DATA_MANAGER_TYPE, simId, tempDataPath, username);
				if(resp.getData()==null){
					useClimate = false;
					throw new Exception("No weather data in time series data store. Setting useClimate = false.");
				}
				else{
					File weatherFile = new File(directory+File.separator+WEATHER_FILENAME);
					FileOutputStream fout = new FileOutputStream(weatherFile);
					fout.write(resp.getData().toString().getBytes());
					fout.flush();
					fout.close();
				}
			}
		} catch (JsonSyntaxException e) {
			logRunning("No weather data was found in proven. Running Simulation without weather data.",
					processId, username, logManager, LogLevel.WARN);
			useClimate = false;
		}catch (Exception e) {
			logRunning(e.getMessage(),
					processId, username, logManager, LogLevel.WARN);
		}
		
		//Generate startup file
		File startupFile = new File(tempDataPath+File.separator+STARTUP_FILENAME);
		PrintWriter startupFileWriter = new PrintWriter(startupFile);
		generateStartupFile(parameters, tempDataPath, startupFileWriter, modelId, processId, username, useClimate);
		
		//Generate outputs file
		PrintWriter simulationOutputs = new PrintWriter(tempDataPath+File.separator+MEASUREMENTOUTPUTS_FILENAME);
		Properties simOutputParams = new Properties();
		String dictFile = tempDataPath+File.separator+DICTIONARY_FILENAME;
		simOutputParams.setProperty(GLDSimulationOutputConfigurationHandler.DICTIONARY_FILE, dictFile);
		simOutputParams.setProperty(GLDSimulationOutputConfigurationHandler.MODELID, modelId);
		GLDSimulationOutputConfigurationHandler simulationOutputConfig = new GLDSimulationOutputConfigurationHandler(configManager, powergridModelManager, logManager);
		simulationOutputConfig.generateConfig(simOutputParams, simulationOutputs, processId, username);
		
		
		out.write(dir.getAbsolutePath());
		
		logRunning("Finished generating all GridLAB-D configuration files.", processId, username, logManager);

	}
	
	
	protected void generateStartupFile(Properties parameters, String tempDataPath, PrintWriter startupFileWriter, String modelId, String processId, String username, boolean useClimate) throws Exception{
		logRunning("Generating startup file for GridLAB-D configuration using parameters: "+parameters, processId, username, logManager);

		String simulationBrokerHost = GridAppsDConstants.getStringProperty(parameters, SIMULATIONBROKERHOST, null);
		if(simulationBrokerHost==null || simulationBrokerHost.trim().length()==0){
			logError("No "+SIMULATIONBROKERHOST+" parameter provided", processId, username, logManager);
			throw new Exception("Missing parameter "+SIMULATIONBROKERHOST);
		}
		String simulationBrokerPort = GridAppsDConstants.getStringProperty(parameters, SIMULATIONBROKERPORT, null);
		if(simulationBrokerPort==null || simulationBrokerPort.trim().length()==0){
			logError("No "+SIMULATIONBROKERPORT+" parameter provided", processId, username, logManager);
			throw new Exception("Missing parameter "+SIMULATIONBROKERPORT);
		}
		long simulationStartTime = GridAppsDConstants.getLongProperty(parameters, SIMULATIONSTARTTIME, -1);
		if(simulationStartTime<0){
			logError("No "+SIMULATIONSTARTTIME+" parameter provided", processId, username, logManager);
			throw new Exception("Missing parameter "+SIMULATIONSTARTTIME);
		}
		String simulationDuration = GridAppsDConstants.getStringProperty(parameters, SIMULATIONDURATION, null);
		if(simulationDuration==null || simulationDuration.trim().length()==0){
			logError("No "+SIMULATIONDURATION+" parameter provided", processId, username, logManager);
			throw new Exception("Missing parameter "+SIMULATIONDURATION);
		}
		String solverMethod = GridAppsDConstants.getStringProperty(parameters, SOLVERMETHOD, null);
		if(solverMethod==null || solverMethod.trim().length()==0){
			solverMethod = "NR";
		}
	
		String simulationID = GridAppsDConstants.getStringProperty(parameters, SIMULATIONID, null);
		if(simulationID==null || simulationID.trim().length()==0){
			logError("No "+SIMULATIONID+" parameter provided", processId, username, logManager);
			throw new Exception("Missing parameter "+SIMULATIONID);
		}
		String scheduleName = GridAppsDConstants.getStringProperty(parameters, SCHEDULENAME, null);
		
		double nominalv = 0;
		
		
		
		try{
			String nominalVoltageQuery = "SELECT DISTINCT ?vnom WHERE {"
					+ " ?fdr c:IdentifiedObject.mRID '"+modelId+"'. "
					+ "?s c:ConnectivityNode.ConnectivityNodeContainer|c:Equipment.EquipmentContainer ?fdr."
					+ "?s c:ConductingEquipment.BaseVoltage ?lev."
					+ " ?lev c:BaseVoltage.nominalVoltage ?vnom."
					+ "} ORDER by ?vnom";
			ResultSet rs = powergridModelManager.queryResultSet(modelId, nominalVoltageQuery, processId, username);
			QuerySolution binding = rs.nextSolution();
			String vnom = ((Literal) binding.get("vnom")).toString();
			nominalv = new Double(vnom).doubleValue()/sqrt3;
		}catch (Exception e) {
			//send error and fail in vnom not found in model or bad format
			logError("Could not find valid nominal voltage for feeder:"+modelId, processId, username, logManager);
			throw new Exception("Could not find valid nominal voltage for feeder:"+modelId);
		}
		//add an include reference to the base glm 
				String baseGLM = tempDataPath+File.separator+BASE_FILENAME;
				String brokerLocation = simulationBrokerHost;
				String brokerPort = String.valueOf(simulationBrokerPort);
				
				Calendar c = Calendar.getInstance();
				simulationStartTime = (long) simulationStartTime;
				Date startTime = new Date(simulationStartTime * 1000);  //GridAppsDConstants.SDF_GLM_CLOCK.parse(simulationStartTime);
				c.setTime(startTime);
				c.add(Calendar.SECOND, new Integer(simulationDuration));
				Date stopTime = c.getTime();
				
				
				startupFileWriter.println("clock {");
				startupFileWriter.println("     timezone \"UTC0\";");
				startupFileWriter.println("     starttime '"+GridAppsDConstants.SDF_GLM_CLOCK.format(startTime)+"';");
				startupFileWriter.println("     stoptime '"+GridAppsDConstants.SDF_GLM_CLOCK.format(stopTime)+"';");
				startupFileWriter.println("}");
				
				startupFileWriter.println("#set maximum_synctime=3600");
				
				startupFileWriter.println("#set suppress_repeat_messages=1");
				startupFileWriter.println("#set relax_naming_rules=1");
				startupFileWriter.println("#set profiler=1");
				startupFileWriter.println("#set minimum_timestep=0.1");
				
				startupFileWriter.println("module connection;");
				startupFileWriter.println("module generators;");
				startupFileWriter.println("module tape;");
				startupFileWriter.println("module powerflow {");
				startupFileWriter.println("     line_capacitance TRUE;");
				startupFileWriter.println("     solver_method "+solverMethod+";");
				startupFileWriter.println("}");
				if(useClimate) {
					startupFileWriter.println("module climate;");
				}

				startupFileWriter.println("object fncs_msg {");
				startupFileWriter.println("     name "+simulationID+";");
				startupFileWriter.println("     message_type JSON;");
				startupFileWriter.println("     configure model_outputs.json;");
				startupFileWriter.println("     option \"transport:hostname "+brokerLocation+", port "+brokerPort+"\";");
				startupFileWriter.println("}");
				startupFileWriter.println("object recorder {");
				startupFileWriter.println("     parent "+simulationID+";");
				startupFileWriter.println("     property message_type;");
				startupFileWriter.println("     file "+simulationID+".csv;");
				startupFileWriter.println("     interval 1;");
				startupFileWriter.println("}");
				/*startupFileWriter.println("object multi_recorder {");
				startupFileWriter.println("          parent "+simulationName+";");
				startupFileWriter.println("          property xf_hvmv_sub:power_in_A,xf_hvmv_sub:power_in_B,xf_hvmv_sub:power_in_C,reg_FEEDER_REG:tap_A,reg_FEEDER_REG:tap_B,reg_FEEDER_REG:tap_C,_hvmv_sub_lsb:voltage_A,_hvmv_sub_lsb:voltage_B,_hvmv_sub_lsb:voltage_C;");
				startupFileWriter.println("         file "+simulationName+"_debug_states.csv;");
				startupFileWriter.println("         interval 1;");
				startupFileWriter.println("         limit 120;");
				startupFileWriter.println("}");*/
				
				if(useClimate) {
					startupFileWriter.println("object csv_reader {");
					startupFileWriter.println(" name CSVREADER;");
					startupFileWriter.println(" filename \""+WEATHER_FILENAME+"\";");
					startupFileWriter.println("}");
					startupFileWriter.println("object climate {");
					startupFileWriter.println(" name \"Weather Data\";");
					startupFileWriter.println(" tmyfile \""+WEATHER_FILENAME+"\";");
					startupFileWriter.println(" reader CSVREADER;");
					startupFileWriter.println("}; ");
				}
				if(scheduleName!=null && scheduleName.trim().length()>0){
					startupFileWriter.println("class player {");
					startupFileWriter.println("	double value;");
					startupFileWriter.println("}");
					startupFileWriter.println("object player {");
					startupFileWriter.println("	name "+scheduleName+";");
					startupFileWriter.println("	file "+scheduleName+".player;");
					startupFileWriter.println("	loop 0;");
					startupFileWriter.println("}");
				}
				startupFileWriter.println("#define VSOURCE="+nominalv);
				startupFileWriter.println("#include \""+baseGLM+"\"");
				startupFileWriter.flush();
				startupFileWriter.close();
				
				logRunning("Finished generating startup file for GridLAB-D configuration.", processId, username, logManager);

	}
	
	
	
}
