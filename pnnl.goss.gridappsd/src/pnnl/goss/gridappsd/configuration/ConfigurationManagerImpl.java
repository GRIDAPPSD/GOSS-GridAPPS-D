package pnnl.goss.gridappsd.configuration;

import java.io.File;
import java.io.Serializable;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.gridappsd.api.ConfigurationManager;
import pnnl.goss.gridappsd.api.StatusReporter;
import pnnl.goss.gridappsd.requests.RequestSimulation;
import pnnl.goss.gridappsd.utils.GridAppsDConstants;

/**
 * This class implements subset of functionalities for Internal Functions
 * 405 Simulation Manager and 406 Power System Model Manager.
 * ConfigurationManager is responsible for:
 * - subscribing to configuration topics and 
 * - converting configuration message into simulation configuration files
 *   and power grid model files.
 * @author shar064
 *
 */

@Component
public class ConfigurationManagerImpl implements ConfigurationManager{
	
	private static Logger log = LoggerFactory.getLogger(ConfigurationManagerImpl.class);
	Client client = null; 
	
	@ServiceDependency
	private volatile ClientFactory clientFactory;
	
	@ServiceDependency
	private volatile StatusReporter statusReporter;
	
	//@ServiceDependency 
	//private volatile DataManager dataManager;
	
	@Start
	public void start(){
		
		statusReporter.reportStatus(String.format("Starting %s", this.getClass().getName()));
		
//		log.debug("Starting "+this.getClass().getName());
//		
//		try{
//			Credentials credentials = new UsernamePasswordCredentials(
//					GridAppsDConstants.username, GridAppsDConstants.password);
//			client = clientFactory.create(PROTOCOL.STOMP,credentials);
//		}
//		catch(Exception e){
//				e.printStackTrace();
//		}
		
	}
	
	
	/**
	 * This method returns simulation file path with name.
	 * Return GridLAB-D file path with name for RC1.
	 * @param simulationId
	 * @param configRequest
	 * @return
	 */
	@Override
	public synchronized File getSimulationFile(int simulationId, Serializable request){
		
		Gson gson = new Gson();
		RequestSimulation requestSimulation = gson.fromJson(request.toString(), RequestSimulation.class);
		log.debug(requestSimulation.toString());
		//TODO call dataManager's method to get power grid model data and create simulation file
		//dataManager.getModelData(requestSimulation.getPower_system_config());
		
		//Update simulation status after every step, for example:
		statusReporter.reportStatus(GridAppsDConstants.topic_simulationStatus+simulationId, "Simulation files created");
		
		return new File("test");
		
	}
	
}
