package pnnl.goss.gridappsd.configuration;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;

import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;
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
public class ConfigurationManager {
	
	Client client = null; 
	
	@ServiceDependency
	private volatile ClientFactory clientFactory;
	
	@Start
	public void start(){
		try{
			Credentials credentials = new UsernamePasswordCredentials(
					GridAppsDConstants.username, GridAppsDConstants.password);
			client = clientFactory.create(PROTOCOL.STOMP,credentials);
			
			client.subscribe(GridAppsDConstants.topic_configuration, new ConfigurationEvent());
		}
		catch(Exception e){
				e.printStackTrace();
		}
		
	}
	
}
