package gov.pnnl.goss.gridappsd.test;

import static org.amdatu.testing.configurator.TestConfigurator.cleanUp;
import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.pnnl.goss.gridappsd.api.TestConfiguration;
import gov.pnnl.goss.gridappsd.api.TestManager;
import pnnl.goss.core.Client;
import pnnl.goss.core.ClientFactory;

@RunWith(MockitoJUnitRunner.class)
public class TestTestManager {
	
	private final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
	private static Logger log = LoggerFactory.getLogger(TestTestManager.class);
	private TestConfiguration tm;
	
	private static final String OPENWIRE_CLIENT_CONNECTION = "tcp://localhost:6000";
	private static final String STOMP_CLIENT_CONNECTION = "stomp://localhost:6000";
	
//	@Before
//	public void before() throws InterruptedException{	
//		tm = configure(this)
//						.add(CoreGossConfig.configureServerAndClientPropertiesConfig())
//						.add(createServiceDependency().setService(ClientFactory.class))
//						.add(createServiceDependency().setService(Logger.class))
//						.add(createServiceDependency().setService(SecurityManager.class))
//						.add(createServiceDependency().setService(ServerControl.class));
//		tm.apply();
//		
//		// Configuration update is asyncronous, so give a bit of time to catch up
//		TimeUnit.MILLISECONDS.sleep(1000);
//	}
	
	@Mock
	Logger logger;
	
	@Mock
	ClientFactory clientFactory;
	
	@Mock
	Client client;
	
	@Captor
	ArgumentCaptor<String> argCaptor;
    @Test
    public void testEval() throws Exception {
    	Assert.assertNotNull(context);
    	Assert.assertNotNull(getService(TestManager.class));
    }
    
	@Test
	public void testLoadConfig(){	
		TestManager manager = null;
		try {
			manager = getService(TestManager.class);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String path = "/Users/jsimpson/git/adms/GOSS-GridAPPS-D/gov.pnnl.goss.gridappsd/applications/python/exampleTestConfig.json";
		TestConfiguration testConfig = manager.loadTestConfig(path);
		assertEquals(testConfig.getPowerSystemConfiguration(),"ieee8500");
	}

	<T> T getService(Class<T> clazz) throws InterruptedException {
		ServiceTracker<T,T> st = new ServiceTracker<>(context, clazz, null);
		st.open();
		return st.waitForService(1000);
	}
//	@Test
//	public void testLoadScript(){	
//		String path = "/Users/jsimpson/git/adms/GOSS-GridAPPS-D/gov.pnnl.goss.gridappsd/applications/python/exampleTestConfig.json";
//		TestScript testScript = tm.loadTestScript(path);
//		assertEquals(testScript.name,"VVO");
//	}
//	
    @After
    public void after() {
      cleanUp(this);
    }

}
