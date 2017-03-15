package pnnl.goss.gridappsd.utils;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pnnl.goss.core.Client;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.gridappsd.api.StatusReporter;

/**
 * The StatusReporterImpl class is a single point for writing data to the message bus.
 * 
 * During component startup the 
 *
 */
@Component
public class StatusReporterImpl implements StatusReporter {

	private static Logger log = LoggerFactory.getLogger(StatusReporterImpl.class);
	
	Client client = null; 
	
	@ServiceDependency
	private volatile ClientFactory clientFactory;
	
	
	/**
	 * Lifecycle method that connects a Client to the message bus.
	 * 
	 * An exception is thrown and the component will fail if the user name
	 * and password for connecting to the bus is incorrect.
	 * @throws Exception
	 */
	@Start
	public void start() throws Exception {
		
		getClient();
	}
	
	@Stop
	public void finish(){
		try{
			if (client != null){
				client.close();
			}
		}
		finally{
			client = null;
		}		
	}
	
	public void reportStatus(String status) {
		log.debug(status);		
	}

	public void reportStatus(String topic, String status) throws Exception{
		if(client==null){
			getClient();
		}
		
		log.debug(String.format("%s %s", topic,  status));
		client.publish(topic, status);
	}
	

	
	protected void getClient() throws Exception{
		Credentials credentials = new UsernamePasswordCredentials(
				GridAppsDConstants.username, GridAppsDConstants.password);
		client = clientFactory.create(PROTOCOL.STOMP,credentials);
	}
}
