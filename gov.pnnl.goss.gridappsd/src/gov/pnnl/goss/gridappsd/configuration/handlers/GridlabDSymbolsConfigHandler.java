package gov.pnnl.goss.gridappsd.configuration.handlers;

import java.io.PrintWriter;
import java.util.Properties;

import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;

import aQute.bnd.annotation.component.Component;
import gov.pnnl.goss.cim2glm.CIMImporter;
import gov.pnnl.goss.cim2glm.queryhandler.QueryHandler;
import gov.pnnl.goss.gridappsd.api.ConfigurationHandler;
import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.PowergridModelDataManager;
import gov.pnnl.goss.gridappsd.data.handlers.BlazegraphQueryHandler;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;

@Component
public class GridlabDSymbolsConfigHandler implements ConfigurationHandler {

	@ServiceDependency 
	private volatile ConfigurationManager configManager;
	
	@ServiceDependency
	private volatile PowergridModelDataManager powergridModelManager;
	
	public static final String TYPENAME = "GridLAB-D Symbols GLM";
	public static final String NAMESPACE = "namespace";
	@Start
	public void start(){
		if(configManager!=null) {
			configManager.registerConfigurationHandler(TYPENAME, this);
		}
		else { 
			//TODO send log message and exception
//			log.warn("No Config manager avilable for "+getClass());
		}
		
		if(powergridModelManager == null){
			//TODO send log message and exception
		}
	}
	
	public String generateConfig(Properties parameters, PrintWriter out) throws Exception {
		
		String namespace = GridAppsDConstants.getStringProperty(parameters, NAMESPACE, null);
		if(namespace!=null){
			throw new Exception("Missing parameter "+NAMESPACE);
		}
		
		
		String bgHost = configManager.getConfigurationProperty(GridAppsDConstants.BLAZEGRAPH_HOST_PATH);
		if(bgHost==null || bgHost.trim().length()==0){
			bgHost = "http://localhost:9999"; 
		}
		String bgNS = configManager.getConfigurationProperty(GridAppsDConstants.BLAZEGRAPH_HOST_PATH);
		if(bgNS==null || bgNS.trim().length()==0){
			bgNS = "blazegraph"; 
		}
		//TODO write a query handler that uses the built in powergrid model data manager that talks to blazegraph internally
		QueryHandler queryHandler = new BlazegraphQueryHandler(bgHost+"/"+bgNS+"/namespace/"+namespace+"/sparql");
		
	    
		CIMImporter cimImporter = new CIMImporter();
		cimImporter.generateJSONSymbolFile(queryHandler, out);
		
		return out.toString();
	}
	
	

}
