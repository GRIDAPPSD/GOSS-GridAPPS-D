package pnnl.goss.gridappsd.data.handlers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
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
			dataManager.registerHandler(this, PowerSystemConfig.class);
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
			request = gson.fromJson((String)request, PowerSystemConfig.class);
			
		}
		
		
		if(!(request instanceof PowerSystemConfig)){
			return null;//new DataResponse(new DataError(
					//"Invalid request type specified!"));
		}
		
		
		PowerSystemConfig dataRequest = (PowerSystemConfig)request;
		
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
				sqlToRDF.outputModel(dataRequest.Line_name, rdfWriter, conn);
				rdfWriter.flush();
				
				
				//call cim to glm
//				String[] args = {"-l=0.2", "-t=y", "-e=u", "-f=60", "-v=1", "-s=1", "-q=y", 
//							"C:\\Users\\tara\\Documents\\CIM\\Powergrid-Models\\CIM\\testoutput.xml", "ieee8500"}; //8500 args
				
				String[] args = {"-l=1", "-t=y", "-e=u", "-f=60", "-v=1", "-s=1", "-q=y", 
						rdfFile.getAbsolutePath(), "ieee13"};  //13 args
				CIMDataRDFToGLM rdfToGLM = new CIMDataRDFToGLM();
				rdfToGLM.process(args);
				
				//cleanup rdf file
				rdfFile.delete();
				
				//return glm file path  (base? or busxy?)
				
				return new DataResponse(tempDataPath);
				
				
				
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
		supported.add(PowerSystemConfig.class);
		supported.add(String.class);
		return supported;
	}

}
