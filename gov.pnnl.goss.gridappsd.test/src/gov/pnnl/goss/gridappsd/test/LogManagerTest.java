package gov.pnnl.goss.gridappsd.test;

import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;

import java.io.Serializable;

import javax.jms.JMSException;

import static org.junit.Assert.assertNotNull;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.core.client.ClientServiceFactory;

@RunWith(MockitoJUnitRunner.class)
public class LogManagerTest {
	
	Client client; 
	
	@Before
	public void setup() throws Exception{
		ClientFactory clientFactory = new ClientServiceFactory();
		Credentials credentials = new UsernamePasswordCredentials(
						"system", "manager");
	 	client = clientFactory.create(PROTOCOL.STOMP, credentials);
	}
	
	@Test
	public void sendLogMessage() throws JMSException{
		
		String destination = "goss.gridappsd.process.log";
		
		String source = "test";
		String requestId = "test request";
		long timestamp = System.currentTimeMillis();
		String log_message = "this is a test";
		LogLevel log_level = LogLevel.DEBUG;
		ProcessStatus process_status = ProcessStatus.RUNNING;
		Boolean storeToDB = true;
		LogMessage logMessage = new LogMessage(source, requestId,timestamp, log_message, log_level, process_status, storeToDB);
		
		String id = client.getResponse(logMessage, destination, null).toString();
		
		client.subscribe("goss.gridappsd.response.data."+id, new GossResponseEvent() {
			
			@Override
			public void onMessage(Serializable message) {
				DataResponse response = (DataResponse)message;
				assertNotNull(response.getData());
				
			}
		});
		
	}
	
}
