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
package gov.pnnl.goss.gridappsd.data.handlers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
//import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import gov.pnnl.goss.cim2glm.CIMImporter;
import gov.pnnl.goss.cim2glm.queryhandler.QueryHandler;
import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
//import gov.pnnl.goss.cim2glm.queryhandler.impl.HTTPBlazegraphQueryHandler;
import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.api.GridAppsDataHandler;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.dto.ApplicationObject;
import gov.pnnl.goss.gridappsd.dto.ModelCreationConfig;
import gov.pnnl.goss.gridappsd.dto.PowerSystemConfig;
import gov.pnnl.goss.gridappsd.dto.RequestSimulation;
import gov.pnnl.goss.gridappsd.dto.SimulationOutput;
import gov.pnnl.goss.gridappsd.dto.SimulationOutputObject;
import gov.pnnl.goss.gridappsd.log.LogManagerImpl;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.Response;
//import pnnl.goss.core.server.DataSourcePooledJdbc;
import pnnl.goss.core.server.DataSourceRegistry;
import pnnl.goss.core.server.DataSourceType;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;


@Component
public class GridLabDDataHandler implements GridAppsDataHandler {
	@ServiceDependency
	private volatile DataSourceRegistry datasourceRegistry;

	@ServiceDependency
	private volatile DataManager dataManager;
	
	@ServiceDependency
	private volatile ConfigurationManager configManager;
	
	CIMImporter cim2glm = new CIMImporter();
	 
	public GridLabDDataHandler(){}
	public GridLabDDataHandler(DataSourceRegistry registry, DataManager dm, ConfigurationManager cm, CIMImporter cim){
		this.datasourceRegistry = registry;
		this.dataManager = dm;
		this.configManager = cm;
		cim2glm = cim;
	}
	
	
    private Logger log = LoggerFactory.getLogger(getClass());
    private final String datasourceName = "gridappsd";
	
	public static void main(String[] args) {
		
		
		PowerSystemConfig config = new PowerSystemConfig();
		config.GeographicalRegion_name = "ieee8500_Region";
		config.Line_name = "ieee8500";
		config.SubGeographicalRegion_name = "ieee8500_SubRegion";
		
		String request = 	"{\"power_system_config\":{\"GeographicalRegion_name\":\"ieee8500nodecktassets_Region\",\"SubGeographicalRegion_name\":\"ieee8500nodecktassets_SubRegion\",\"Line_name\":\"ieee8500\"}, "+
				"\"simulation_config\":{\"start_time\":\"03/07/2017 00:00:00\",\"duration\":\"60\",\"simulator\":\"GridLAB-D\",\"simulation_name\":\"ieee8500\",\"power_flow_solver_method\":\"NR\","
				+ "        \"simulation_output\": {\"output_objects\":[{\"name\":\"swt_a8869_48332_sw\", \"properties\": [\"switch\"]},{\"name\":\"swt_l9407_48332_sw\",\"properties\":[\"switch\"]},{\"name\":\"cap_capbank0c\",\"properties\":[\"parent\",\"cap_nominal_voltage\"]},{\"name\":\"cap_capbank2a\",\"properties\":[\"parent\",\"cap_nominal_voltage\"]}]},"
				+ "        \"model_creation_config\":{\"load_scaling_factor\":\".2\",\"schedule_name\":\"ieeezipload\",\"z_fraction\":\".3\",\"i_fraction\":\".3\",\"p_fraction\":\".4\"}}}";

		
		
		try {
			GridAppsDataHandler handler = new GridLabDDataHandler();
			
			
			
			handler.handle(request, 12345, "d:\\tmp\\gridlabd-tmp\\", new LogManagerImpl());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	@Start
	public void start(){
		if(dataManager!=null) {
			dataManager.registerHandler(this, RequestSimulation.class);
			dataManager.registerHandler(this, String.class);
		}
		else { 
			log.warn("No Data manager avilable for "+getClass());
		}
	}
	
	
	@Override
	public Response handle(Serializable request, int simulationId, String tempDataPath, LogManager logManager) throws Exception {
//		statusReporter.reportStatus(GridAppsDConstants.topic_simulationLog+simulationId, "Generating GridLABD simulation files");
		//TODO log message
		
		//TODO check content in the request for validity
		if(request instanceof String){
			request = RequestSimulation.parse((String)request);
		}
		
		
		if(!(request instanceof RequestSimulation)){
			return null;//new DataResponse(new DataError(
					//"Invalid request type specified!"));
		}
		
		
		RequestSimulation dataRequest = (RequestSimulation)request;
		
		Map<String, DataSourceType> datasources = datasourceRegistry.getAvailable();
		
//		DataSourcePooledJdbc jdbcPool = (DataSourcePooledJdbc)datasourceRegistry.get(datasourceName);
//		if(jdbcPool!=null){
			BufferedWriter rdfWriter = null;
			FileWriter rdfOut = null;
			try {
//				Connection conn = jdbcPool.getConnection();
				
				//[SubGeographicalRegion_name = "
				//+ SubGeographicalRegion_name + ", GeographicalRegion_name = "
				// + GeographicalRegion_name + ", Line_name = " + Line_name + "]
				//TODO parse data request string??? for line and sub region name or just get them out of request object
				
				//call sql to cimrdf
				//TODO get temp directory
				if(!tempDataPath.endsWith(File.separator)){
					tempDataPath = tempDataPath+File.separator;
				}
				tempDataPath = tempDataPath+simulationId+File.separator;
				File tempDataPathDir = new File(tempDataPath);
				if(!tempDataPathDir.exists()){
					tempDataPathDir.mkdirs();
				}
				
				
//				File rdfFile = new File(tempDataPath+"rdfOut"+new Date().getTime()+".rdf");
//				rdfOut = new FileWriter(rdfFile);
//				rdfWriter = new BufferedWriter(rdfOut);
//				CIMDataSQLtoRDF sqlToRDF = new CIMDataSQLtoRDF();
//				sqlToRDF.outputModel(dataRequest.getPower_system_config().Line_name, rdfWriter, conn);
//				rdfWriter.flush();
				
				String simulationName = dataRequest.getSimulation_config().simulation_name;
				//call cim to glm
//				String[] args = {"-l=0.2", "-t=y", "-e=u", "-f=60", "-v=1", "-s=1", "-q=y", 
//							"C:\\Users\\tara\\Documents\\CIM\\Powergrid-Models\\CIM\\testoutput.xml", "ieee8500"}; //8500 args
				
				
				ModelCreationConfig modelConfig = dataRequest.getSimulation_config().model_creation_config;
                //RunCommandLine.runCommand("cp /tmp/ieee8500_base.glm "+tempDataPath);				
				//generate simulation base file
				//-l=0.2 -t=y -e=u -f=60 -v=1 -s=1 -q=y ieee8500.xml ieee8500
//				String[] args = {"-l=0.2", "-t=y", "-e=u", "-f=60", "-v=1", "-s=1", "-q=y", "-n=zipload_schedule", "-z=0.3", "-i=0.3", "-p=0.4",
				
				String bgHost = configManager.getConfigurationProperty(GridAppsDConstants.BLAZEGRAPH_HOST_PATH);
				if(bgHost==null || bgHost.trim().length()==0){
					bgHost = BlazegraphQueryHandler.DEFAULT_ENDPOINT;;
				}
				//TODO write a query handler that uses the built in powergrid model data manager that talks to blazegraph internally
				QueryHandler queryHandler = new BlazegraphQueryHandler(bgHost+"/blazegraph/namespace/kb/sparql");
				CIMImporter cim2glm = new CIMImporter();
				//Generate GLM using zipload
				boolean bWantSched = false;
				boolean bWantZip = false;
				String outBaseFile = tempDataPathDir.getAbsolutePath()+File.separator+simulationName+"_base.glm";
				String fXY = tempDataPathDir.getAbsolutePath()+File.separator+simulationName+"_symbols.json";
				
				if(modelConfig.schedule_name!=null && modelConfig.schedule_name.trim().length()>0){
					double zFraction = modelConfig.z_fraction;
					if(zFraction==0) {
						zFraction = 0;
						bWantZip = true;
					}
					double iFraction = modelConfig.i_fraction;
					if(iFraction==0){
						iFraction = 1;
						bWantZip = true;
					}
					double pFraction = modelConfig.p_fraction; 
					if(pFraction==0){
						pFraction = 0;
						bWantZip = true;
					}
					if(modelConfig.schedule_name!=null){
						bWantSched = true;
					}
					
					
					//cim2glm.start(queryHandler, outBaseFile, modelConfig.schedule_name, 
					//		modelConfig.load_scaling_factor, bWantSched, bWantZip, zFraction, iFraction, pFraction, fXY);
					
					cim2glm.start(queryHandler, "glm", tempDataPathDir.getAbsolutePath()+File.separator+simulationName, modelConfig.schedule_name, 
							modelConfig.load_scaling_factor, bWantSched, bWantZip, zFraction, iFraction, pFraction); 
//					String[] args = {"-l="+modelConfig.load_scaling_factor,"-t="+modelConfig.triplex, "-e="+modelConfig.encoding, "-f="+modelConfig.system_frequency,
//										"-v="+modelConfig.voltage_multiplier, "-s="+modelConfig.power_unit_conversion, "-q="+modelConfig.unique_names, "-n="+modelConfig.schedule_name, 
//										"-z="+zFraction, "-i="+iFraction, "-p="+pFraction,		
//										rdfFile.getAbsolutePath(), tempDataPath+simulationName};  //13 args
//					log.debug("Generating GLM file with args "+args);
//					CIMDataRDFToGLM rdfToGLM = new CIMDataRDFToGLM();
//					rdfToGLM.process(args);
					
					
				
				} else {
					//Generate GLM, no zipload
//					String[] args = {"-l="+modelConfig.load_scaling_factor,"-t="+modelConfig.triplex, "-e="+modelConfig.encoding, "-f="+modelConfig.system_frequency,
//							"-v="+modelConfig.voltage_multiplier, "-s="+modelConfig.power_unit_conversion, "-q="+modelConfig.unique_names,		
//						rdfFile.getAbsolutePath(), tempDataPath+simulationName};  //13 args
//					log.debug("Generating GLM file with args "+args);
//					CIMDataRDFToGLM rdfToGLM = new CIMDataRDFToGLM();
//					rdfToGLM.process(args);
					
					cim2glm.start(queryHandler, "glm", tempDataPathDir.getAbsolutePath()+File.separator+simulationName, modelConfig.schedule_name, 
							modelConfig.load_scaling_factor, bWantSched, bWantZip, 0, 0, 0); 
				
				}
//				statusReporter.reportStatus(GridAppsDConstants.topic_simulationLog+simulationId, "GridLABD base file generated");
				//cleanup rdf file
//				rdfFile.delete();
				
				//generate vvo application file if the simulation contains an application config with the name vvo
				String vvoAppFile = "vvo_inputs.json";
				ApplicationObject[] appConfigs = dataRequest.getApplication_config().applications;
				for(ApplicationObject appConfig: appConfigs){
					if("vvo".equals(appConfig.getName())){
						String config = appConfig.getConfig_string();
						FileOutputStream fout = new FileOutputStream(tempDataPath+vvoAppFile);
						fout.write(config.getBytes());
						fout.flush();
						fout.close();
					}
				}
				
				
				//generate simulation config json file
				String configFileName = "configfile.json";
//				String configFileValue = "{\"swt_g9343_48332_sw\": [\"status\"],\"swt_l5397_48332_sw\": [\"status\"],\"swt_a8869_48332_sw\": [\"status\"]}";
				String configFileValue = generateConfigValue(dataRequest.getSimulation_config().simulation_output);
				//TODO change this to be obtained from Tom's conversion script
				//MRID->grid labd names
				
				FileOutputStream configFileOut = new FileOutputStream(tempDataPath+configFileName);
				configFileOut.write(configFileValue.getBytes());
				configFileOut.flush();
				configFileOut.close();
//				statusReporter.reportStatus(GridAppsDConstants.topic_simulationLog+simulationId, "GridLABD output config file generated");

				
				//generate simulation config startup file
				File startupFile = new File(tempDataPath+simulationName+"_startup.glm");
				PrintWriter startupFileWriter = new PrintWriter(startupFile);
				//add an include reference to the base glm 
				String baseGLM = tempDataPath+simulationName+"_base.glm";
				String brokerLocation = dataRequest.getSimulation_config().getSimulation_broker_location();
				String brokerPort = String.valueOf(dataRequest.getSimulation_config().getSimulation_broker_port());
				
				Calendar c = Calendar.getInstance();
				Date startTime = GridAppsDConstants.SDF_GLM_CLOCK.parse(dataRequest.getSimulation_config().start_time);
				c.setTime(startTime);
				c.add(Calendar.SECOND, dataRequest.getSimulation_config().duration);
				Date stopTime = c.getTime();
				
				
				startupFileWriter.println("clock {");
				startupFileWriter.println("     timezone \"UTC0\";");
				startupFileWriter.println("     starttime '"+GridAppsDConstants.SDF_GLM_CLOCK.format(startTime)+"';");
				startupFileWriter.println("     stoptime '"+GridAppsDConstants.SDF_GLM_CLOCK.format(stopTime)+"';");
				startupFileWriter.println("}");
				
				startupFileWriter.println("#set suppress_repeat_messages=1");
				startupFileWriter.println("#set relax_naming_rules=1");
				startupFileWriter.println("#set profiler=1");
				startupFileWriter.println("#set minimum_timestep=0.1");
				
				startupFileWriter.println("module connection;");
				startupFileWriter.println("module tape;");
				startupFileWriter.println("module powerflow {");
				startupFileWriter.println("     line_capacitance TRUE;");
				startupFileWriter.println("     solver_method "+dataRequest.getSimulation_config().power_flow_solver_method+";");
				startupFileWriter.println("}");
				
				startupFileWriter.println("object fncs_msg {");
				startupFileWriter.println("     name "+simulationName+";");
				startupFileWriter.println("     message_type JSON;");
				startupFileWriter.println("     configure configfile.json;");
				startupFileWriter.println("     option \"transport:hostname "+brokerLocation+", port "+brokerPort+"\";");
				startupFileWriter.println("}");
				startupFileWriter.println("object recorder {");
				startupFileWriter.println("     parent "+simulationName+";");
				startupFileWriter.println("     property message_type;");
				startupFileWriter.println("     file "+simulationName+".csv;");
				startupFileWriter.println("     interval 60;");
				startupFileWriter.println("}");
				startupFileWriter.println("object multi_recorder {");
				startupFileWriter.println("          parent "+simulationName+";");
				startupFileWriter.println("          property xf_hvmv_sub:power_in_A,xf_hvmv_sub:power_in_B,xf_hvmv_sub:power_in_C,reg_FEEDER_REG:tap_A,reg_FEEDER_REG:tap_B,reg_FEEDER_REG:tap_C,nd__hvmv_sub_lsb:voltage_A,nd__hvmv_sub_lsb:voltage_B,nd__hvmv_sub_lsb:voltage_C;");
				startupFileWriter.println("         file "+simulationName+"_debug_states.csv;");
				startupFileWriter.println("         interval 1;");
				startupFileWriter.println("         limit 120;");
				startupFileWriter.println("}");
				if(modelConfig.schedule_name!=null && modelConfig.schedule_name.trim().length()>0){
					startupFileWriter.println("class player {");
					startupFileWriter.println("	double value;");
					startupFileWriter.println("}");
					startupFileWriter.println("object player {");
					startupFileWriter.println("	name "+modelConfig.schedule_name+";");
					startupFileWriter.println("	file "+modelConfig.schedule_name+".player;");
					startupFileWriter.println("	loop 0;");
					startupFileWriter.println("}");
				}
				
				

				startupFileWriter.println("#include \""+baseGLM+"\"");

				startupFileWriter.flush();
				startupFileWriter.close();
				
//				statusReporter.reportStatus(GridAppsDConstants.topic_simulationLog+simulationId, "GridLABD startup file generated");

				
				return new DataResponse(startupFile);
				
				
				
//			} catch (SQLException e) {
//				log.error("Error while generating GridLABD config files", e);
//				throw new Exception("SQL error while generating GLM configuration",e);
			} catch (IOException e) {
				log.error("Error while generating GridLABD config files", e);
				throw new Exception("IO error while generating GLM configuration",e);
			}finally {
				try {
					if(rdfWriter!=null) rdfWriter.close();
					if(rdfOut!=null) rdfOut.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			}
//		} else {
//			throw new Exception("No jdbc pool avialable for "+datasourceName);
//		}
		
		
	}


	@Override
	public String getDescription() {
		return "Generates GridLABD config files for simulation";
	}

	/**
	 * Create configfile.json string, should look something like 
	 *   "{\"swt_g9343_48332_sw\": [\"status\"],\"swt_l5397_48332_sw\": [\"status\"],\"swt_a8869_48332_sw\": [\"status\"]}";
	 * @param simulationOutput
	 * @return
	 */
	protected String generateConfigValue(SimulationOutput simulationOutput){
		StringBuffer configStr = new StringBuffer();
		boolean isFirst = true;
		configStr.append("{");
		for(SimulationOutputObject obj: simulationOutput.getOutputObjects()){
			if(!isFirst){
				configStr.append(",");
			}
			isFirst = false;
			
			configStr.append("\""+obj.getName()+"\": [");
			boolean isFirstProp = true;
			for(String property: obj.getProperties()){
				if(!isFirstProp){
					configStr.append(",");
				}
				isFirstProp = false;
				configStr.append("\""+property+"\"");
			}
			configStr.append("]");
		}
		
		configStr.append("}");
		
		return configStr.toString();
	}

	@Override
	public List<Class<?>> getSupportedRequestTypes() {
		List<Class<?>> supported = new ArrayList<Class<?>>();
		supported.add(RequestSimulation.class);
		supported.add(String.class);
		return supported;
	}

}
