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
public class GridlabDBaseConfigHandler implements ConfigurationHandler {

	@ServiceDependency 
	private volatile ConfigurationManager configManager;
	
	@ServiceDependency
	private volatile PowergridModelDataManager powergridModelManager;
	
	public static final String TYPENAME = "GridLAB-D Base GLM";
	public static final String ZFRACTION = "z_fraction";
	public static final String IFRACTION = "i_fraction";
	public static final String PFRACTION = "p_fraction";
	public static final String SCHEDULENAME = "schedule_name";
	public static final String LOADSCALINGFACTOR = "load_scaling_factor";
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
		
		double loadScale = GridAppsDConstants.getDoubleProperty(parameters, LOADSCALINGFACTOR, 0);
		
		String scheduleName = GridAppsDConstants.getStringProperty(parameters, SCHEDULENAME, null);
		if(scheduleName!=null){
			bWantSched = true;
		}
		
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
		cimImporter.generateGLMFile(queryHandler, out, scheduleName, loadScale, bWantSched, bWantZip, zFraction, iFraction, pFraction);
		
		return out.toString();
	}
	
	

}
