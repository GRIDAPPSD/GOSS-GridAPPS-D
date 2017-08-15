package gov.pnnl.goss.gridappsd;

import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import junit.framework.TestCase;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.client.ClientServiceFactory;

public class TestLogManager extends TestCase {
	
private static Logger log = LoggerFactory.getLogger(TestLogManager.class);
	
	ClientFactory clientFactory = new ClientServiceFactory();
	
	Client client;
	
	public void testLogging() throws Exception {

			
			//Step1: Create GOSS Client
			Credentials credentials = new UsernamePasswordCredentials(
					GridAppsDConstants.username, GridAppsDConstants.password);
			client = clientFactory.create(PROTOCOL.STOMP, credentials);
			
			String message = "{"
					+ "\"process_id\":\"app_123\","
					+ "\"process_status\":\"started\","
					+ "\"log_level\":\"debug\","
					+ "\"log_message\":\"Testing LogManager\","
					+ "\"timestamp\": \"8\14\17 2:22:22\"}";
			client.publish("goss.gridappsd.process.log", message);
			
	}
	
	public void testLogStore() throws Exception {

		
		//Step1: Create GOSS Client
		Credentials credentials = new UsernamePasswordCredentials(
				GridAppsDConstants.username, GridAppsDConstants.password);
		client = clientFactory.create(PROTOCOL.STOMP, credentials);
		
		String message = "{"
				+ "\"process_id\":\"app_123\","
				+ "\"process_status\":\"started\","
				+ "\"log_level\":\"debug\","
				+ "\"log_message\":\"Testing LogManager\","
				+ "\"timestamp\": \"8\14\17 2:22:22\"}";
		client.publish("goss.gridappsd.process.log", message);
		
		
		
}	
	

}
