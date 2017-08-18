package gov.pnnl.goss.gridappsd;

import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.log.LogManagerImpl;
import pnnl.goss.core.Client;
import pnnl.goss.core.ClientFactory;

@RunWith(MockitoJUnitRunner.class)
public class LogManagerTests {
	
	@Mock
	DataManager dataManager;
	
	@Captor
	ArgumentCaptor<String> argCaptor;
	
	@Test 
	public void storeCalledWhen_logStoreToDBTrue(){
		
		LogManager logManager = new LogManagerImpl(dataManager);
		
		LogMessage message = new LogMessage();
		message.setLog_level("debug");
		message.setLog_message("Process manager received message "+ message);
		message.setProcess_id(this.getClass().getName());
		message.setProcess_status("running");
		message.setStoreToDB(true);
		message.setTimestamp("11/11/11 11:11:11");
		
		logManager.log(message);
		
		Mockito.verify(dataManager).store(argCaptor.capture());
		
		assertEquals(message, argCaptor.getValue());
	}
	
	

}
