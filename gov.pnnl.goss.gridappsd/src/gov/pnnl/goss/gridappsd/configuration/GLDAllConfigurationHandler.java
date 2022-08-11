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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import gov.pnnl.gridappsd.cimhub.CIMImporter;
import gov.pnnl.gridappsd.cimhub.CIMQuerySetter;
import gov.pnnl.gridappsd.cimhub.dto.ModelState;
import gov.pnnl.gridappsd.cimhub.queryhandler.QueryHandler;
import gov.pnnl.goss.gridappsd.api.ConfigurationHandler;
import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.PowergridModelDataManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.data.ProvenTimeSeriesDataManagerImpl;
import gov.pnnl.goss.gridappsd.data.conversion.ProvenWeatherToGridlabdWeatherConverter;
import gov.pnnl.goss.gridappsd.data.handlers.BlazegraphQueryHandler;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.RequestTimeseriesData;
import gov.pnnl.goss.gridappsd.dto.RequestTimeseriesDataBasic;
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
	private volatile SimulationManager simulationManager;
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
	public static final String MODEL_STATE = "model_state";
	public static final String SIMULATOR = "simulator";
	public static final String SEPARATED_LOADS = "separated_loads";
	public static final int TIMEFILTER_YEAR = 2013;

//	public static final String CONFIGTARGET = "glm";
	public static final String CONFIGTARGET = "both"; //will build files for both glm and dss

	public static final String cimhub_PREFIX = "model";
	public static final String BASE_FILENAME = cimhub_PREFIX+"_base.glm";
	public static final String STARTUP_FILENAME = cimhub_PREFIX+"_startup.glm";
	public static final String SCHEDULES_FILENAME = cimhub_PREFIX+"_schedules.glm";
	public static final String MEASUREMENTOUTPUTS_FILENAME = cimhub_PREFIX+"_outputs.json";
	public static final String DICTIONARY_FILENAME = cimhub_PREFIX+"_dict.json";
	public static final String WEATHER_FILENAME = cimhub_PREFIX+"_weather.csv";

	final double sqrt3 = Math.sqrt(3);


	public GLDAllConfigurationHandler() {
	}

	public GLDAllConfigurationHandler(LogManager logManager, DataManager dataManager) {
		this.logManager = logManager;
		this.dataManager = dataManager;
	}


	@Override
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
		boolean bWantZip = true;
		boolean bWantSched = false;
		List<String> separateLoads = new ArrrayList<String>();

		logManager.info(ProcessStatus.RUNNING,processId,"Generating all GridLAB-D configuration files using parameters: "+parameters);

		double zFraction = GridAppsDConstants.getDoubleProperty(parameters, ZFRACTION, 0);
		double iFraction = GridAppsDConstants.getDoubleProperty(parameters, IFRACTION, 0);
		double pFraction = GridAppsDConstants.getDoubleProperty(parameters, PFRACTION, 0);
		
		if(zFraction == 0 && iFraction == 0 && pFraction == 0)
			bWantZip = false;
		
		boolean bWantRandomFractions = GridAppsDConstants.getBooleanProperty(parameters, RANDOMIZEFRACTIONS, false);

		double loadScale = GridAppsDConstants.getDoubleProperty(parameters, LOADSCALINGFACTOR, 1);

		String scheduleName = GridAppsDConstants.getStringProperty(parameters, SCHEDULENAME, null);
		if(scheduleName!=null && scheduleName.trim().length()>0){
			bWantSched = true;
		}
		String directory = GridAppsDConstants.getStringProperty(parameters, DIRECTORY, null);
		if(directory==null || directory.trim().length()==0){
			logManager.error(ProcessStatus.ERROR,processId,"No "+DIRECTORY+" parameter provided");
			throw new Exception("Missing parameter "+DIRECTORY);
		}

		String modelId = GridAppsDConstants.getStringProperty(parameters, MODELID, null);
		if(modelId==null || modelId.trim().length()==0){
			logManager.error(ProcessStatus.ERROR,processId,"No "+MODELID+" parameter provided");
			throw new Exception("Missing parameter "+MODELID);
		}
		String bgHost = configManager.getConfigurationProperty(GridAppsDConstants.BLAZEGRAPH_HOST_PATH);
		if(bgHost==null || bgHost.trim().length()==0){
			bgHost = BlazegraphQueryHandler.DEFAULT_ENDPOINT;
		}
		String simulationID = GridAppsDConstants.getStringProperty(parameters, SIMULATIONID, null);
		String simId = "1";
		if(simulationID==null || simulationID.trim().length()==0){
			logManager.error(ProcessStatus.ERROR,processId,"No "+SIMULATIONID+" parameter provided");
			throw new Exception("Missing parameter "+SIMULATIONID);
		}
		try{
			simId = simulationID;
		}catch (Exception e) {
			logManager.error(ProcessStatus.ERROR,simulationID,"Simulation ID not a valid "+simulationID+", defaulting to "+simId);
		}
		
		ModelState modelState = new ModelState();
		String modelStateStr = GridAppsDConstants.getStringProperty(parameters, MODELSTATE, null);
		if(modelStateStr==null || modelStateStr.trim().length()==0){
			logManager.info(ProcessStatus.RUNNING,processId,"No "+MODELSTATE+" parameter provided");
		} else {
			Gson  gson = new Gson();
			modelState = gson.fromJson(modelStateStr, ModelState.class);
		}		
		
		long simulationStartTime = GridAppsDConstants.getLongProperty(parameters, SIMULATIONSTARTTIME, -1);
		if(simulationStartTime<0){
			logManager.error(ProcessStatus.ERROR,processId,"No "+SIMULATIONSTARTTIME+" parameter provided");
			throw new Exception("Missing parameter "+SIMULATIONSTARTTIME);
		}
		long simulationDuration = GridAppsDConstants.getLongProperty(parameters, SIMULATIONDURATION, 0);
		if(simulationDuration==0){
			logManager.error(ProcessStatus.ERROR,processId,"No "+SIMULATIONDURATION+" parameter provided");
			throw new Exception("Missing parameter "+SIMULATIONDURATION);
		}
//		long simulationEndTime = simulationStartTime+(1000*simulationDuration);

		QueryHandler queryHandler = new BlazegraphQueryHandler(bgHost, logManager, processId, username);
		queryHandler.addFeederSelection(modelId);

		File dir = new File(directory);
		if(!dir.exists()){
			dir.mkdirs();
		}
		String fRoot = dir.getAbsolutePath()+File.separator+cimhub_PREFIX;

		boolean useHouses = GridAppsDConstants.getBooleanProperty(parameters, USEHOUSES, false);
		//TODO
		boolean useClimate = true;//GridAppsDConstants.getBooleanProperty(parameters, USECLIMATE, false);
		
		boolean bHaveEventGen = true;
		
		String separatedLoadsFile = GridAppsDConstants.getStringProperty(parameters, SEPARATED_LOADS, null);
		//TODO parse xlsx spreadsheet specified in separatedLoadsFile
		//if(separatedLoadsFile!=null) {
			
		//}

		//cimhub utility uses
		CIMImporter cimImporter = new CIMImporter();
		CIMQuerySetter qs = new CIMQuerySetter();
		cimImporter.start(queryHandler, qs, CONFIGTARGET, fRoot, scheduleName, loadScale, bWantSched, bWantZip, bWantRandomFractions, useHouses, zFraction, iFraction, pFraction, bHaveEventGen, modelState, false, separatedLoads);
		String tempDataPath = dir.getAbsolutePath();

		//If use climate, then generate gridlabd weather data file
		try {
			if(useClimate){
				RequestTimeseriesDataBasic weatherRequest = new RequestTimeseriesDataBasic();
				weatherRequest.setQueryMeasurement("weather");
				weatherRequest.setResponseFormat(ProvenWeatherToGridlabdWeatherConverter.OUTPUT_FORMAT);
				Map<String, Object> queryFilter = new HashMap<String, Object>();

				Calendar c = Calendar.getInstance();
				//For both the start and end time, set the year to the one that currently has data in the database
				//TODO either we need more weather data in the database, or make this more flexible where we only have to search by month/day
				c.setTime(new Date(simulationStartTime*1000));
				c.set(Calendar.YEAR, TIMEFILTER_YEAR);
				c.add(Calendar.MINUTE, -1);
				//Convert to UTC time until the input time is correct
				////TODO this will be changed in the future
				//c.add(Calendar.HOUR, 6);
				queryFilter.put(STARTTIME_FILTER, ""+c.getTimeInMillis()+"000000");
				simulationDuration = simulationDuration+60;
				c.add(Calendar.SECOND, new Long(simulationDuration).intValue());
				queryFilter.put(ENDTIME_FILTER, ""+c.getTimeInMillis()+"000000");
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
			logManager.warn(ProcessStatus.RUNNING,processId,"No weather data was found in proven. Running Simulation without weather data.");
			useClimate = false;
		}catch (Exception e) {
			logManager.warn(ProcessStatus.RUNNING,processId,e.getMessage());
		}
		
		//Generate zip load profile player file
		if(scheduleName!=null && scheduleName.trim().length()>0) {
			GLDZiploadScheduleConfigurationHandler ziploadScheduleConfigurationHandler = new GLDZiploadScheduleConfigurationHandler(logManager, dataManager);
			ziploadScheduleConfigurationHandler.generateConfig(parameters, null, processId, username);
		}
		
		//Generate startup file
		File startupFile = new File(tempDataPath+File.separator+STARTUP_FILENAME);
		PrintWriter startupFileWriter = new PrintWriter(startupFile);
		generateStartupFile(parameters, tempDataPath, startupFileWriter, modelId, processId, username, useClimate, useHouses);

		//Generate outputs file
		PrintWriter simulationOutputs = new PrintWriter(tempDataPath+File.separator+MEASUREMENTOUTPUTS_FILENAME);
		Properties simOutputParams = new Properties();
		String dictFile = tempDataPath+File.separator+DICTIONARY_FILENAME;
		simOutputParams.setProperty(GLDSimulationOutputConfigurationHandler.DICTIONARY_FILE, dictFile);
		simOutputParams.setProperty(GLDSimulationOutputConfigurationHandler.MODELID, modelId);
		simOutputParams.setProperty(GLDSimulationOutputConfigurationHandler.USEHOUSES, Boolean.toString(useHouses));
		simOutputParams.setProperty(SIMULATIONBROKERHOST, parameters.getProperty(SIMULATIONBROKERHOST,"127.0.0.1"));
		simOutputParams.setProperty(SIMULATIONBROKERPORT, parameters.getProperty(SIMULATIONBROKERPORT,"5570"));
		simOutputParams.setProperty(GridAppsDConstants.GRIDLABD_INTERFACE, parameters.getProperty(GridAppsDConstants.GRIDLABD_INTERFACE,GridAppsDConstants.GRIDLABD_INTERFACE_FNCS));
		GLDSimulationOutputConfigurationHandler simulationOutputConfig = new GLDSimulationOutputConfigurationHandler(configManager, powergridModelManager, logManager);
		simulationOutputConfig.generateConfig(simOutputParams, simulationOutputs, processId, username);


		out.write(dir.getAbsolutePath());

		logManager.info(ProcessStatus.RUNNING,processId,"Finished generating all GridLAB-D configuration files.");

	}


	protected void generateStartupFile(Properties parameters, String tempDataPath, PrintWriter startupFileWriter, String modelId, String processId, String username, boolean useClimate, boolean useHouses) throws Exception{
		logManager.info(ProcessStatus.RUNNING,processId,"Generating startup file for GridLAB-D configuration using parameters: "+parameters);
		
		
		String gldInterface = GridAppsDConstants.getStringProperty(parameters, GridAppsDConstants.GRIDLABD_INTERFACE, GridAppsDConstants.GRIDLABD_INTERFACE_FNCS);
		String simulator = GridAppsDConstants.getStringProperty(parameters, SIMULATOR, null);
		
		String simulationBrokerHost = GridAppsDConstants.getStringProperty(parameters, SIMULATIONBROKERHOST, null);
		if(simulationBrokerHost==null || simulationBrokerHost.trim().length()==0){
			logManager.error(ProcessStatus.ERROR,processId,"No "+SIMULATIONBROKERHOST+" parameter provided");
			throw new Exception("Missing parameter "+SIMULATIONBROKERHOST);
		}
		String simulationBrokerPort = GridAppsDConstants.getStringProperty(parameters, SIMULATIONBROKERPORT, null);
		if(simulationBrokerPort==null || simulationBrokerPort.trim().length()==0){
			logManager.error(ProcessStatus.ERROR,processId,"No "+SIMULATIONBROKERPORT+" parameter provided");
			throw new Exception("Missing parameter "+SIMULATIONBROKERPORT);
		}
		long simulationStartTime = GridAppsDConstants.getLongProperty(parameters, SIMULATIONSTARTTIME, -1);
		if(simulationStartTime<0){
			logManager.error(ProcessStatus.ERROR,processId,"No "+SIMULATIONSTARTTIME+" parameter provided");
			throw new Exception("Missing parameter "+SIMULATIONSTARTTIME);
		}
		String simulationDuration = GridAppsDConstants.getStringProperty(parameters, SIMULATIONDURATION, null);
		if(simulationDuration==null || simulationDuration.trim().length()==0){
			logManager.error(ProcessStatus.ERROR,processId,"No "+SIMULATIONDURATION+" parameter provided");
			throw new Exception("Missing parameter "+SIMULATIONDURATION);
		}
		String solverMethod = GridAppsDConstants.getStringProperty(parameters, SOLVERMETHOD, null);
		if(solverMethod==null || solverMethod.trim().length()==0){
			solverMethod = "NR";
		}

		String simulationID = GridAppsDConstants.getStringProperty(parameters, SIMULATIONID, null);
		if(simulationID==null || simulationID.trim().length()==0){
			logManager.error(ProcessStatus.ERROR,processId,"No "+SIMULATIONID+" parameter provided");
			throw new Exception("Missing parameter "+SIMULATIONID);
		}
		String scheduleName = GridAppsDConstants.getStringProperty(parameters, SCHEDULENAME, null);

		double nominalv = 0;

		try{
			String nominalVoltageQuery = "SELECT (MAX(xsd:float(?vnom)) AS ?vnomvoltage) WHERE {"
					+ "?fdr c:IdentifiedObject.mRID '" + modelId + "'. "
					+ "?s c:ConnectivityNode.ConnectivityNodeContainer|c:Equipment.EquipmentContainer ?fdr. "
					+ "?s c:ConductingEquipment.BaseVoltage ?lev. " + "?lev c:BaseVoltage.nominalVoltage ?vnom."
					+ "}";
			// ORDER by DESC(?vnom)";
			ResultSet rs = powergridModelManager.queryResultSet(modelId, nominalVoltageQuery, processId, username);
			QuerySolution binding = rs.nextSolution();
			Double vnom = binding.getLiteral("vnomvoltage").getDouble();
			nominalv = vnom / sqrt3;
		}catch (Exception e) {
			//send error and fail in vnom not found in model or bad format
			logManager.error(ProcessStatus.ERROR,processId,"Could not find valid nominal voltage for feeder:"+modelId);
			// Throw the real exception because its could just be a problem with the query
			// itself.
			throw e;
		}
		//add an include reference to the base glm
		String baseGLM = tempDataPath+File.separator+BASE_FILENAME;
		String schedulesFile = tempDataPath+File.separator+SCHEDULES_FILENAME;

		String brokerLocation = simulationBrokerHost;
		String brokerPort = String.valueOf(simulationBrokerPort);

		Calendar c = Calendar.getInstance();
//		simulationStartTime = simulationStartTime;
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
		if (useHouses) {
			startupFileWriter.println("module residential {");
			startupFileWriter.println("     implicit_enduses NONE;");
			startupFileWriter.println("}");
		}
		startupFileWriter.println("module connection;");
		startupFileWriter.println("module generators;");
		startupFileWriter.println("module tape;");
		startupFileWriter.println("module reliability {");
	    startupFileWriter.println("    report_event_log false;");
	    startupFileWriter.println("};");
		startupFileWriter.println("module powerflow {");
		startupFileWriter.println("     line_capacitance TRUE;");
		startupFileWriter.println("     solver_method "+solverMethod+";");
		startupFileWriter.println("}");
		if(useClimate) {
			startupFileWriter.println("module climate;");
		}
		startupFileWriter.println("module reliability;");

		if(GridAppsDConstants.GRIDLABD_INTERFACE_HELICS.equals(gldInterface)){
			startupFileWriter.println("object helics_msg {");
			startupFileWriter.println("      name "+simulationID+";");
			if(simulator.equalsIgnoreCase("gridlab-d"))
				startupFileWriter.println("      message_type JSON;");
			startupFileWriter.println("      publish_period 3;");
			startupFileWriter.println("      configure model_outputs.json;");
			startupFileWriter.println("}");

		} else {
			startupFileWriter.println("object fncs_msg {");
			startupFileWriter.println("     name "+simulationID+";");
			startupFileWriter.println("     message_type JSON;");
			startupFileWriter.println("     configure model_outputs.json;");
			startupFileWriter.println("     option \"transport:hostname "+brokerLocation+", port "+brokerPort+"\";");
			startupFileWriter.println("}");
		}
		
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

		startupFileWriter.println("object fault_check {");
		startupFileWriter.println("     name fault_check_object;");
		startupFileWriter.println("     check_mode ONCHANGE;");
		startupFileWriter.println("     eventgen_object external_event_handler;");
		startupFileWriter.println("     output_filename fault_check_output.txt;");
		startupFileWriter.println("     strictly_radial FALSE;");
		//TODO: Remove if condition and set grid_association to TRUE once issue with GridLAB-D is resolved
		if(modelId.equals("_5DB4FA23-623B-4DBE-BA59-B99ECF44DABA"))
			startupFileWriter.println("     grid_association FALSE;");
		else
			startupFileWriter.println("     grid_association TRUE;");
		startupFileWriter.println("}");
		startupFileWriter.println("object eventgen {");
		startupFileWriter.println("     name external_event_handler;");
		startupFileWriter.println("     use_external_faults TRUE;");
		startupFileWriter.println("}");

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
		startupFileWriter.println("#include \""+schedulesFile+"\"");
		startupFileWriter.println("#include \""+baseGLM+"\"");
		
		startupFileWriter.flush();
		startupFileWriter.close();

		logManager.info(ProcessStatus.RUNNING,processId,"Finished generating startup file for GridLAB-D configuration.");

	}
	
	private static List<String> getSaperatedLoadNames(String fileName) {
		
		List<String> loadNames = new ArrayList<String>();
		boolean isHeader = true;
		
		try {
			FileInputStream fis = new FileInputStream(fileName);
			Workbook workbook = null;
			if(fileName.toLowerCase().endsWith("xlsx")){
				workbook = new XSSFWorkbook(fis);
			}else if(fileName.toLowerCase().endsWith("xls")){
				workbook = new HSSFWorkbook(fis);
			}
			
			Sheet sheet = workbook.getSheetAt(0);
			Iterator<Row> rowIterator = sheet.iterator();
			while (rowIterator.hasNext()) 
	        {
				
				Row row = rowIterator.next();
				if(!isHeader){
					loadNames.add(row.getCell(5).getStringCellValue());
					System.out.println(row.getCell(5).getStringCellValue());
				}
				isHeader=false;
	        }
			fis.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return loadNames;
	}
	
}
