package gov.pnnl.goss.gridappsd.test;

import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;

import java.io.Serializable;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.fusesource.stomp.jms.StompJmsDestination;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.Gson;

@RunWith(MockitoJUnitRunner.class)
public class LogManagerTest {
	
	@Test
	public void sendLogMessage() throws JMSException{
		
		String destination = "goss.gridappsd.process.log";
		
		String process_id = "test";
		long timestamp = System.currentTimeMillis();
		String log_message = "this is a test";
		LogLevel log_level = LogLevel.DEBUG;
		ProcessStatus process_status = ProcessStatus.RUNNING;
		Boolean storeToDB = true;
		LogMessage logMessage = new LogMessage(process_id, timestamp, log_message, log_level, process_status, storeToDB);
		
		sendMessage(destination, logMessage);
		
		
	}
	
	
	private void sendMessage(String destination, Serializable message) throws JMSException{
		Gson gson = new Gson();
		StompJmsConnectionFactory connectionFactory = new StompJmsConnectionFactory();
		connectionFactory.setBrokerURI("tcp://localhost:61613");
		connectionFactory.setUsername("system");
		connectionFactory.setPassword("manager");
		Connection connection = connectionFactory.createConnection(); 
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		MessageProducer producer = session.createProducer(new StompJmsDestination(destination));
		TextMessage textMessage = null;
		if(message instanceof String){
			textMessage = session.createTextMessage(message.toString());
		} else {
			textMessage = session.createTextMessage(gson.toJson(message));
			
		}
		producer.send(textMessage);
	}

}
