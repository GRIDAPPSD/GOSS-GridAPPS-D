package pnnl.goss.gridappsd.configuration;

import java.io.File;
import java.io.Serializable;
import java.util.Dictionary;
import java.util.Properties;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
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
import pnnl.goss.core.Response;
import pnnl.goss.gridappsd.api.ConfigurationManager;
import pnnl.goss.gridappsd.api.DataManager;
import pnnl.goss.gridappsd.api.StatusReporter;
import pnnl.goss.gridappsd.dto.PowerSystemConfig;
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
	private static final String CONFIG_PID = "pnnl.goss.gridappsd";

	private static Logger log = LoggerFactory.getLogger(ConfigurationManagerImpl.class);
	Client client = null; 
	
	@ServiceDependency
	private volatile ClientFactory clientFactory;
	
	@ServiceDependency
	private volatile StatusReporter statusReporter;
	
	@ServiceDependency 
	private volatile DataManager dataManager;
	
	private Dictionary<String, ?> configurationProperties;
	
	@Start
	public void start(){
		System.out.println("STARTING CONFIGURATION MANAGER");
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
	public synchronized File getSimulationFile(int simulationId, PowerSystemConfig powerSystemConfig) throws Exception{
		
		log.debug(powerSystemConfig.toString());
		//TODO call dataManager's method to get power grid model data and create simulation file
		Response resp = dataManager.processDataRequest(powerSystemConfig, simulationId, getConfigurationProperty(GridAppsDConstants.GRIDAPPSD_TEMP_PATH));
//		resp.f
		//Update simulation status after every step, for example:
		statusReporter.reportStatus(GridAppsDConstants.topic_simulationStatus+simulationId, "Simulation files created");
		
		return new File("test");
		
	}
	
	@ConfigurationDependency(pid=CONFIG_PID)
	public synchronized void updated(Dictionary<String, ?> config)  {
		this.configurationProperties = config;
	}
	
	public String getConfigurationProperty(String key){
		if(this.configurationProperties!=null){
			Object value = this.configurationProperties.get(key);
			if(value!=null)
				return value.toString();
		}
		return null;
	}
	
}
