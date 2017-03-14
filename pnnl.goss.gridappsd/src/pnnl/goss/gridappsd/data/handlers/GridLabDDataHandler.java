package pnnl.goss.gridappsd.data.handlers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.Connection;
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

import com.google.gson.Gson;

import pnnl.goss.core.DataResponse;
import pnnl.goss.core.Response;
import pnnl.goss.core.server.DataSourcePooledJdbc;
import pnnl.goss.core.server.DataSourceRegistry;
import pnnl.goss.core.server.DataSourceType;
import pnnl.goss.gridappsd.api.DataManager;
import pnnl.goss.gridappsd.api.GridAppsDataHandler;
import pnnl.goss.gridappsd.dto.PowerSystemConfig;
import pnnl.goss.gridappsd.dto.RequestSimulation;
import pnnl.goss.gridappsd.utils.GridAppsDConstants;


@Component
public class GridLabDDataHandler implements GridAppsDataHandler {
	@ServiceDependency
	private volatile DataSourceRegistry datasourceRegistry;

	@ServiceDependency
	private volatile DataManager dataManager;
	 
    private Logger log = LoggerFactory.getLogger(getClass());
    private final String datasourceName = "gridappsd";
	
	public static void main(String[] args) {
		PowerSystemConfig config = new PowerSystemConfig();
		config.GeographicalRegion_name = "ieee8500_Region";
		config.Line_name = "ieee8500";
		config.SubGeographicalRegion_name = "ieee8500_SubRegion";
		try {
			new GridLabDDataHandler().handle(config, 12345, "d:\\tmp\\gridlabd-tmp\\");
		} catch (Exception e) {
			// TODO Auto-generated catch block
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
	public Response handle(Serializable request, int simulationId, String tempDataPath) throws Exception {
		//TODO check content in the request for validity
		if(request instanceof String){
			Gson  gson = new Gson();
			request = gson.fromJson((String)request, RequestSimulation.class);
			
		}
		
		
		if(!(request instanceof RequestSimulation)){
			return null;//new DataResponse(new DataError(
					//"Invalid request type specified!"));
		}
		
		
		RequestSimulation dataRequest = (RequestSimulation)request;
		
		Map<String, DataSourceType> datasources = datasourceRegistry.getAvailable();
		
		DataSourcePooledJdbc jdbcPool = (DataSourcePooledJdbc)datasourceRegistry.get(datasourceName);
		if(jdbcPool!=null){
			BufferedWriter rdfWriter = null;
			FileWriter rdfOut = null;
			try {
				Connection conn = jdbcPool.getConnection();
				
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
				
				
				File rdfFile = new File(tempDataPath+"rdfOut"+new Date().getTime()+".rdf");
				rdfOut = new FileWriter(rdfFile);
				rdfWriter = new BufferedWriter(rdfOut);
				CIMDataSQLtoRDF sqlToRDF = new CIMDataSQLtoRDF();
				sqlToRDF.outputModel(dataRequest.getPower_system_config().Line_name, rdfWriter, conn);
				rdfWriter.flush();
				
				String simulationName = dataRequest.getSimulation_config().simulation_name;
				//call cim to glm
//				String[] args = {"-l=0.2", "-t=y", "-e=u", "-f=60", "-v=1", "-s=1", "-q=y", 
//							"C:\\Users\\tara\\Documents\\CIM\\Powergrid-Models\\CIM\\testoutput.xml", "ieee8500"}; //8500 args
				
				
				//generate simulation base file
				String[] args = {"-l=1", "-t=y", "-e=u", "-f=60", "-v=1", "-s=1", "-q=y", 
						rdfFile.getAbsolutePath(), tempDataPath+simulationName};  //13 args
				CIMDataRDFToGLM rdfToGLM = new CIMDataRDFToGLM();
				rdfToGLM.process(args);
				
				//cleanup rdf file
				rdfFile.delete();
				
				
				
				//generate simulation config json file
				String configFileName = "configfile.json";
				String configFileValue = "{\"ROOT\": [\"nominal_voltage\",\"distribution_load\",\"positive_sequence_voltage\"],"
						+ " \"regconfig6506321\":[\"tap_pos_A\",\"tap_pos_B\",\"tap_pos_C\",\"regulation\",\"time_delay\"]}";
				FileOutputStream configFileOut = new FileOutputStream(tempDataPath+configFileName);
				configFileOut.write(configFileValue.getBytes());
				configFileOut.flush();
				configFileOut.close();
				
				
				//generate simulation config startup file
				File startupFile = new File(tempDataPath+simulationName+"_startup.glm");
				PrintWriter startupFileWriter = new PrintWriter(startupFile);
				//add an include reference to the base glm 
				String baseGLM = tempDataPath+simulationName+"_base.glm";

				Calendar c = Calendar.getInstance();
				Date startTime = GridAppsDConstants.SDF_SIMULATION_REQUEST.parse(dataRequest.getSimulation_config().start_time);
				c.setTime(startTime);
				c.add(Calendar.SECOND, dataRequest.getSimulation_config().duration);
				Date stopTime = c.getTime();
				
				startupFileWriter.println("#include "+baseGLM);
				
				startupFileWriter.println("clock {");
				startupFileWriter.println("     startime '"+GridAppsDConstants.SDF_GLM_CLOCK.format(startTime)+"';");
				startupFileWriter.println("     stoptime '"+GridAppsDConstants.SDF_GLM_CLOCK.format(stopTime)+"';");
				startupFileWriter.println("}");
				
				startupFileWriter.println("#set suppress_repeat_messages=1");
				startupFileWriter.println("#set relax_naming_rules=1");
				startupFileWriter.println("#set profiler=1");
				startupFileWriter.println("#set double_format=%+.12lg");
				startupFileWriter.println("#set complex_format=%+.12lg%+.12lg%c");
				startupFileWriter.println("#set minimum_timestep=0.1");
				
				startupFileWriter.println("module connection;");
				startupFileWriter.println("module powerflow {");
				startupFileWriter.println("     line_capacitance TRUE;");
				startupFileWriter.println("     solver_method "+dataRequest.getSimulation_config().power_flow_solver_method+";");
				startupFileWriter.println("}");
				startupFileWriter.flush();
				startupFileWriter.close();
				
				return new DataResponse(startupFile);
				
				
				
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new Exception("SQL error while generating GLM configuration",e);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new Exception("IO error while generating GLM configuration",e);
			}finally {
				try {
					if(rdfWriter!=null) rdfWriter.close();
					if(rdfOut!=null) rdfOut.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		} else {
			throw new Exception("No jdbc pool avialable for "+datasourceName);
		}
		
		
	}


	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public List<Class<?>> getSupportedRequestTypes() {
		List<Class<?>> supported = new ArrayList<Class<?>>();
		supported.add(RequestSimulation.class);
		supported.add(String.class);
		return supported;
	}

}
