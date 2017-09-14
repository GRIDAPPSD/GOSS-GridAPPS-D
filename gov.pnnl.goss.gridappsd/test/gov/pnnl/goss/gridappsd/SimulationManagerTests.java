package gov.pnnl.goss.gridappsd;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.api.StatusReporter;
import gov.pnnl.goss.gridappsd.dto.FncsBridgeResponse;
import gov.pnnl.goss.gridappsd.dto.SimulationConfig;
import gov.pnnl.goss.gridappsd.simulation.SimulationManagerImpl;
import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.GossCoreContants;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.core.server.ServerControl;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import gov.pnnl.goss.gridappsd.utils.RunCommandLine;
import junit.framework.Assert;

@RunWith(MockitoJUnitRunner.class)
public class SimulationManagerTests {
	@Mock
	ConfigurationManager mockConfigurationManager;
	
	@Mock
	StatusReporter mockStatusReporter;
	
	@Mock
	FncsBridgeResponse mockFncsBridgeResponse;
	
	@Mock
	ClientFactory mockClientFactory;
	
	@Mock
	ServerControl mockServerControl;
	
	@Captor
	ArgumentCaptor<PROTOCOL> protocalCapture;
	
	@Captor
	ArgumentCaptor<Credentials> credentialCapture;
	
	
	Credentials creds;
	
	@Test
	public void correctCredsWhenStarted(){
		
		SimulationManagerImpl manager = new SimulationManagerImpl(mockClientFactory, mockServerControl, mockStatusReporter, mockConfigurationManager);
		
		try {
			manager.start();
			
			Mockito.verify(mockClientFactory).create(protocalCapture.capture(), credentialCapture.capture());	
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		
		assertEquals(PROTOCOL.STOMP, protocalCapture.getValue());
		

		assertEquals(GridAppsDConstants.username, credentialCapture.getValue().getUserPrincipal().getName());
		assertEquals(GridAppsDConstants.password, credentialCapture.getValue().getPassword());
		
	}
	
	
//	@Captor
//	ArgumentCaptor<PROTOCOL> captProtocol;
//
//	@Captor
//	ArgumentCaptor<Credentials> captCreds;
//	
//	@Test
//	public void clientFactoryCreateCalledWhen_simulationManagerStartCalled() {
//		
//		creds = new UsernamePasswordCredentials("foo", "bar");
//		
//		SimulationManagerImpl simulationManager = new SimulationManagerImpl(mockClientFactory,mockServerControl,
//				mockStatusReporter,mockConfigurationManager);
//		try {
//			simulationManager.start();
//			//Mockito.verify(mockClientFactory).create(PROTOCOL.OPENWIRE, creds);
//		}
//	}
}
