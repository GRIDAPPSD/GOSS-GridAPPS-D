package pnnl.goss.gridappsd.simulation;

import java.io.Serializable;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;

import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.gridappsd.utils.GridAppsDConstants;

/**
 * FNCSOutputEvent processes all messages coming from fncs-goss-bridge.py
 * @author shar064
 *
 */
@Component
public class FNCSOutputEvent implements GossResponseEvent {
	
	@ServiceDependency
	private volatile ClientFactory clientFactory;
	
	/**
	 * message is in the JSON string format
	 * {}
	 */
	@Override
	public void onMessage(Serializable message) {
		
		try {
			Credentials credentials = new UsernamePasswordCredentials(
					GridAppsDConstants.username, GridAppsDConstants.password);
			
			Client client = clientFactory.create(PROTOCOL.STOMP,credentials);
			
			
			
			
			//TODO: Parse message and update simulation status or communicate with bridge accordingly
			client.publish(GridAppsDConstants.topic_FNCS_input, "test message");
					
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	

}
