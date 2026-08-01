package gov.pnnl.goss.gridappsd.test;

import static org.junit.Assert.assertNotNull;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;

/**
 *
 */

@RunWith(MockitoJUnitRunner.class)
public class SimulationStartTest {

    private volatile ClientFactory clientFactory;
    Client client;

    @Before
    public void beforeTest() throws Exception {
        Credentials credentials = new UsernamePasswordCredentials("system", "manager");
        client = clientFactory.create(PROTOCOL.STOMP, credentials);
    }

    @Test
    public void testGridappsd() throws Exception {

        String simulationId = client.getResponse(TestConstants.REQUEST_SIMULATION_CONFIG_ESC,
                "goss.gridappasd.process.request.simulation", null).toString();

        assertNotNull(simulationId);

    }
}
