package gov.pnnl.goss.gridappsd.test;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.amdatu.testing.configurator.TestConfiguration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.amdatu.testing.configurator.TestConfigurator.configure;
import static org.amdatu.testing.configurator.TestConfigurator.cleanUp;
import static org.amdatu.testing.configurator.TestConfigurator.createServiceDependency;
import static org.junit.Assert.assertNotNull;

import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.dto.RequestSimulation;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.server.ServerControl;

/**
 * 
 */

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationManagerTest {

	private static Logger log = LoggerFactory.getLogger(ConfigurationManagerTest.class);
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
    
    @After
    public void after() {
      cleanUp(this);
    }
}

