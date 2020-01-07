package gov.pnnl.goss.gridappsd.test;

import static org.amdatu.testing.configurator.TestConfigurator.cleanUp;
import static org.amdatu.testing.configurator.TestConfigurator.configure;
import static org.amdatu.testing.configurator.TestConfigurator.createServiceDependency;
import static org.junit.Assert.assertNotNull;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import org.amdatu.testing.configurator.TestConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.core.server.ServerControl;

/**
 * 
 */

@RunWith(MockitoJUnitRunner.class)
public class AppManagerTest {

	private static Logger log = LoggerFactory.getLogger(AppManagerTest.class);
   // private final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
    
	private TestConfiguration testConfig;
	private volatile ClientFactory clientFactory;
	private volatile ServerControl serverControl;
	
	
	private static final String OPENWIRE_CLIENT_CONNECTION = "tcp://localhost:6000";
	private static final String STOMP_CLIENT_CONNECTION = "stomp://localhost:6000";

	@Before
	public void before() throws InterruptedException{	
		testConfig = configure(this)
						.add(CoreGossConfig.configureServerAndClientPropertiesConfig())
						.add(createServiceDependency().setService(ClientFactory.class))
						.add(createServiceDependency().setService(Logger.class))
						.add(createServiceDependency().setService(SecurityManager.class))
						.add(createServiceDependency().setService(ServerControl.class));
		testConfig.apply();
		
		
		// Configuration update is asyncronous, so give a bit of time to catch up
		TimeUnit.MILLISECONDS.sleep(1000);
	}
	
	@Test
	public void sanity_ServerStarted() {
		log.debug("TEST: serverCanStartSuccessfully");
		System.out.println("TEST: serverCanStartSuccessfully");
		assertNotNull(serverControl);
		log.debug("TEST_END: serverCanStartSuccessfully");
	}
	
    
    /*
     * File getSimulationFile(int simulationId, RequestSimulation powerSystemConfig) throws Exception;
	String getConfigurationProperty(String key);
     */
    
    @Test
    public void testGetConfigurationProperty(){
    	//ConfigurationManager manager = getService(ConfigurationManager.class);
    	
    	//manager.getConfigurationProperty(key)
    }
    
    
    
//    <T> T getService(Class<T> clazz) throws InterruptedException {
//    	ServiceTracker<T,T> st = new ServiceTracker<>(context, clazz, null);
//    	st.open();
//    	return st.waitForService(1000);
//    }
    
	
	Client client;
    
    @Test
    public void testConnect(){
		try {
		
		//Step1: Create GOSS Client
		Credentials credentials = new UsernamePasswordCredentials(
				TestConstants.username, TestConstants.password);
		client = clientFactory.create(PROTOCOL.STOMP, credentials);
		
		//Create Request Simulation object
//		PowerSystemConfig powerSystemConfig = new PowerSystemConfig();
//		powerSystemConfig.GeographicalRegion_name = "ieee8500_Region";
//		powerSystemConfig.SubGeographicalRegion_name = "ieee8500_SubRegion";
//		powerSystemConfig.Line_name = "ieee8500";
		
		
//		Gson  gson = new Gson();
//		String request = gson.toJson(powerSystemConfig); 
//		DataRequest request = new DataRequest();
//		request.setRequestContent(powerSystemConfig);
		System.out.println(client);
		String response = client.getResponse("",GridAppsDConstants.topic_requestData, null).toString();
		
		//TODO subscribe to response
		client.subscribe(GridAppsDConstants.topic_simulationOutput+response, new GossResponseEvent() {
			
			@Override
			public void onMessage(Serializable response) {
				// TODO Auto-generated method stub
				System.out.println("RESPNOSE "+response);
			}
		});
		
		
	} catch (Exception e) {
		e.printStackTrace();
	}
    }
    
    @After
    public void after() {
      cleanUp(this);
    }
}

