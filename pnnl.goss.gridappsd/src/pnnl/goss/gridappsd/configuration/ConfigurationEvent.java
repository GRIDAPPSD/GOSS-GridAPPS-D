package pnnl.goss.gridappsd.configuration;

import java.io.Serializable;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;



import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.gridappsd.utils.GridAppsDConstants;

@Component
public class ConfigurationEvent implements GossResponseEvent {
	
	@ServiceDependency
	private volatile ClientFactory clientFactory;
	
	@Override
	public void onMessage(Serializable message) {
		
		try {
			
			/*
			 * Receives config message from ProcessManager
			 * Call getResponse() to DataManger and get data file locations and simulation file locations.
			 * response back to Process manager with file locations.
			 */
			//Recieves configuration message from ProcessManager
			DataResponse event = (DataResponse)message;
			
			//Create GOSS client
			Credentials credentials = new UsernamePasswordCredentials(
					GridAppsDConstants.username, GridAppsDConstants.password);
			Client client;
			client = clientFactory.create(PROTOCOL.STOMP,credentials);
			
			
			//make request to DataManager to get power grid model files location
			Serializable response = client.getResponse(event.getData(), GridAppsDConstants.topic_getDataFilesLocation, null);
			
			
			
			
			client.publish(event.getReplyDestination(), response);
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}
	

	

}
