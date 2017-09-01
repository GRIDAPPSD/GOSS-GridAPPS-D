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

import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.api.StatusReporter;
import gov.pnnl.goss.gridappsd.dto.FncsBridgeResponse;
import gov.pnnl.goss.gridappsd.dto.SimulationConfig;
import gov.pnnl.goss.gridappsd.simulation.SimulationManagerImpl;
import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.core.server.ServerControl;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import gov.pnnl.goss.gridappsd.utils.RunCommandLine;

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
	
	
	@Test
	public void clientFactoryCreateCalledWhen_simulationManagerStartCalled() {
		SimulationManagerImpl simulationManager = new SimulationManagerImpl(mockClientFactory,mockServerControl,
				mockStatusReporter,mockConfigurationManager);
		try {
			simulationManager.start();
			Mockito.verify(mockClientFactory).create(protocol, credentials);
		}
	}
}
