package gov.pnnl.goss.gridappsd;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import gov.pnnl.goss.gridappsd.api.LogDataManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.log.LogManagerImpl;

@RunWith(MockitoJUnitRunner.class)
public class LogManagerTests {
	
	@Mock
	LogDataManager logDataManager;
	
	@Captor
	ArgumentCaptor<String> argCaptor;
	
	@Test
	public void storeCalledWhen_logStoreToDBTrueInObject(){
		
		LogManager logManager = new LogManagerImpl(logDataManager);
		
		LogMessage message = new LogMessage();
		message.setLog_level("debug");
		message.setLog_message("Process manager received message "+ message);
		message.setProcess_id(this.getClass().getName());
		message.setProcess_status("running");
		message.setStoreToDB(true);
		message.setTimestamp("11/11/11 11:11:11");
		
		logManager.log(message);
		
		
		Mockito.verify(logDataManager).store(argCaptor.capture(), argCaptor.capture(),
				argCaptor.capture(), argCaptor.capture(),
				argCaptor.capture(), argCaptor.capture());
		
		List<String> allValues = argCaptor.getAllValues();
		assertEquals(6, allValues.size());
		assertEquals(message.getProcess_id(), allValues.get(0));
		//TODO: User test user for this instead of system
		assertEquals("system", allValues.get(1));
		assertEquals(message.getTimestamp(), allValues.get(2));
		assertEquals(message.getLog_level(), allValues.get(3));
		assertEquals(message.getLog_message(), allValues.get(4));
		assertEquals(message.getProcess_status(), allValues.get(5));
	
	}
	
	@Test
	public void storeCalledWhen_logStoreToDBTrueInString(){
		
		
		LogManager logManager = new LogManagerImpl(logDataManager);
		String message = "{"
				+ "\"process_id\":\"app_123\","
				+ "\"process_status\":\"started\","
				+ "\"log_level\":\"debug\","
				+ "\"log_message\":\"Testing LogManager\","
				+ "\"timestamp\": \"8\14\17 2:22:22\"}";
		
		logManager.log(message);
		
		
		Mockito.verify(logDataManager).store(argCaptor.capture(), argCaptor.capture(),
				argCaptor.capture(), argCaptor.capture(),
				argCaptor.capture(), argCaptor.capture());
		
		List<String> allValues = argCaptor.getAllValues();
		assertEquals(6, allValues.size());
		assertEquals("app_123", allValues.get(0));
		//TODO: User test user for this instead of system
		assertEquals("system", allValues.get(1));
		assertEquals("8\14\17 2:22:22", allValues.get(2));
		assertEquals("debug", allValues.get(3));
		assertEquals("Testing LogManager", allValues.get(4));
		assertEquals("started", allValues.get(5));
		
		
	}
	
	

}
