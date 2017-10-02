package gov.pnnl.goss.gridappsd;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
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
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.log.LogManagerImpl;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;

@RunWith(MockitoJUnitRunner.class)
public class LogManagerTests {
	
	@Mock
	LogDataManager logDataManager;
	
	@Captor
	ArgumentCaptor<String> argCaptor;
	@Captor
	ArgumentCaptor<Long> argLongCaptor;
	@Captor
	ArgumentCaptor<LogLevel> argLogLevelCaptor;
	@Captor
	ArgumentCaptor<ProcessStatus> argProcessStatusCaptor;
	
	
	@Test
	public void storeCalledWhen_logStoreToDBTrueInObject() throws ParseException{
		
		LogManager logManager = new LogManagerImpl(logDataManager);
		
		LogMessage message = new LogMessage();
		message.setLog_level(LogLevel.DEBUG);
		message.setLog_message("Process manager received message "+ message);
		message.setProcess_id(this.getClass().getName());
		message.setProcess_status(ProcessStatus.RUNNING);
		message.setStoreToDB(true);
		message.setTimestamp(GridAppsDConstants.SDF_SIMULATION_REQUEST.parse("11/11/11 11:11:11").getTime());
		
		logManager.log(message);
		
		
		
		Mockito.verify(logDataManager).store(argCaptor.capture(), argCaptor.capture(),
				argLongCaptor.capture(), argCaptor.capture(),
				argLogLevelCaptor.capture(), argProcessStatusCaptor.capture());
		
		List<String> allStringValues = argCaptor.getAllValues();
		assertEquals(3, allStringValues.size());
		assertEquals(message.getProcess_id(), allStringValues.get(0));
		//TODO: User test user for this instead of system
		assertEquals("system", allStringValues.get(1));
		assertEquals(new Long(message.getTimestamp()), argLongCaptor.getValue());
		assertEquals(message.getLog_level(), argLogLevelCaptor.getValue());
		assertEquals(message.getLog_message(), allStringValues.get(2));
		assertEquals(message.getProcess_status(), argProcessStatusCaptor.getValue());
	
	}
	
	@Test
	public void storeCalledWhen_logStoreToDBTrueInString() throws ParseException{
		
		
		LogManager logManager = new LogManagerImpl(logDataManager);
		String message = "{"
				+ "\"process_id\":\"app_123\","
				+ "\"process_status\":\"STARTED\","
				+ "\"log_level\":\"DEBUG\","
				+ "\"log_message\":\"Testing LogManager\","
				+ "\"timestamp\": "+GridAppsDConstants.SDF_SIMULATION_REQUEST.parse("8/14/17 2:22:22").getTime()+"}";
		
		logManager.log(LogMessage.parse(message));
		
		
		
		
		Mockito.verify(logDataManager).store(argCaptor.capture(), argCaptor.capture(),
				argLongCaptor.capture(), argCaptor.capture(),
				argLogLevelCaptor.capture(), argProcessStatusCaptor.capture());
		
		List<String> allStringValues = argCaptor.getAllValues();
		assertEquals(3, allStringValues.size());
		assertEquals("app_123", allStringValues.get(0));
		//TODO: User test user for this instead of system
		assertEquals("system", allStringValues.get(1));
		assertEquals(new Long(GridAppsDConstants.SDF_SIMULATION_REQUEST.parse("8/14/17 2:22:22").getTime()), argLongCaptor.getValue());
		assertEquals(LogLevel.DEBUG, argLogLevelCaptor.getValue());
		assertEquals("Testing LogManager", allStringValues.get(2));
		assertEquals(ProcessStatus.STARTED, argProcessStatusCaptor.getValue());
		

	}
	
	@Test
	public void queryCalledWhen_getLogCalledWithObject() throws ParseException{
		
		LogManager logManager = new LogManagerImpl(logDataManager);
		
		LogMessage message = new LogMessage();
		message.setLog_level(LogLevel.DEBUG);
		message.setProcess_id(this.getClass().getName());
		message.setProcess_status(ProcessStatus.RUNNING);
		message.setTimestamp(GridAppsDConstants.SDF_SIMULATION_REQUEST.parse("11/11/11 11:11:11").getTime());
		
		logManager.get(message);
		
		
//		Mockito.verify(logDataManager).query(argCaptor.capture(), argCaptor.capture(),
//				argCaptor.capture(), argCaptor.capture(), argCaptor.capture());
//		
//		List<String> allValues = argCaptor.getAllValues();
//		assertEquals(5, allValues.size());
//		assertEquals(message.getProcess_id(), allValues.get(0));
//		assertEquals(message.getTimestamp(), allValues.get(1));
//		assertEquals(message.getLog_level(), allValues.get(2));
//		assertEquals(message.getProcess_status(), allValues.get(3));
//		//TODO: User test user for this instead of system
//		assertEquals("system", allValues.get(4));
	}
	
	@Test
	public void queryCalledWhen_getLogCalledWithString() throws ParseException{
		
		
		LogManager logManager = new LogManagerImpl(logDataManager);
		String message = "{"
				+ "\"process_id\":\"app_123\","
				+ "\"process_status\":\"started\","
				+ "\"log_level\":\"debug\","
				+ "\"log_message\":\"something happened\","
				+ "\"timestamp\": "+GridAppsDConstants.SDF_SIMULATION_REQUEST.parse("8/14/17 2:22:22").getTime()+"}";
		
		logManager.get(LogMessage.parse(message));
		
		
//		Mockito.verify(logDataManager).query(argCaptor.capture(), argCaptor.capture(),
//				argCaptor.capture(), argCaptor.capture(), argCaptor.capture());
//		
//		List<String> allValues = argCaptor.getAllValues();
//		assertEquals(5, allValues.size());
//		assertEquals("app_123", allValues.get(0));
//		assertEquals("8\14\17 2:22:22", allValues.get(1));
//		assertEquals("debug", allValues.get(2));
//		assertEquals("started", allValues.get(3));
//		//TODO: User test user for this instead of system
//		assertEquals("system", allValues.get(4));
				

	}
	
	

}
