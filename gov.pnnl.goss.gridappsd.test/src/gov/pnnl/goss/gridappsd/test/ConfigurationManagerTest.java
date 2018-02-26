package gov.pnnl.goss.gridappsd.test;

import static org.amdatu.testing.configurator.TestConfigurator.cleanUp;
import static org.amdatu.testing.configurator.TestConfigurator.configure;
import static org.amdatu.testing.configurator.TestConfigurator.createServiceDependency;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.Dictionary;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.amdatu.testing.configurator.TestConfiguration;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import gov.pnnl.goss.gridappsd.configuration.handlers.GridlabDBaseConfigHandler;
import gov.pnnl.goss.gridappsd.dto.ConfigurationRequest;
import gov.pnnl.goss.gridappsd.dto.PowergridModelDataRequest;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.Client;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.Request.RESPONSE_FORMAT;
import pnnl.goss.core.client.ClientServiceFactory;
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
	private Client client;
	
	private static final String OPENWIRE_CLIENT_CONNECTION = "tcp://localhost:6000";
	private static final String STOMP_CLIENT_CONNECTION = "stomp://localhost:6000";

	public static void main (String[] args){
		ConfigurationManagerTest test = new ConfigurationManagerTest();
		test.testgetGLMBaseConfiguration();
		
	}
	
	
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
    
    
	public void testgetGLMBaseConfiguration(){

		try {

			ConfigurationRequest configRequest = new ConfigurationRequest();
			configRequest.setConfigurationType(GridlabDBaseConfigHandler.TYPENAME);
			Properties properties = new Properties();
			properties.setProperty(GridlabDBaseConfigHandler.ZFRACTION, "0.0");
			properties.setProperty(GridlabDBaseConfigHandler.IFRACTION, "1.0");
			properties.setProperty(GridlabDBaseConfigHandler.PFRACTION, "0.0");
			properties.setProperty(GridlabDBaseConfigHandler.SCHEDULENAME, "ieeezipload");
			properties.setProperty(GridlabDBaseConfigHandler.LOADSCALINGFACTOR, "1.0");
			properties.setProperty(GridlabDBaseConfigHandler.NAMESPACE, "ieee13");
			configRequest.setParameters(properties);
			
			System.out.println(configRequest);
			
			Client client = getClient();
			
			Serializable response = client.getResponse(configRequest.toString(), GridAppsDConstants.topic_requestConfig, RESPONSE_FORMAT.JSON);
			
			if(response instanceof String){
				String responseStr = response.toString();
				System.out.println(responseStr);
				
				DataResponse dataResponse = DataResponse.parse(responseStr);
				System.out.println(dataResponse.getData());
			} else {
				System.out.println(response);
				System.out.println(response.getClass());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
    
    
    
    @After
    public void after() {
      cleanUp(this);
    }
    
    
    
    Client getClient() throws Exception{
		if(client==null){
			Dictionary properties = new Properties();
			properties.put("goss.system.manager", "system");
			properties.put("goss.system.manager.password", "manager");
	
			// The following are used for the core-client connection.
			properties.put("goss.openwire.uri", "tcp://0.0.0.0:61616");
			properties.put("goss.stomp.uri", "stomp://0.0.0.0:61613");
			properties.put("goss.ws.uri", "ws://0.0.0.0:61614");
			properties.put("goss.ssl.uri", "ssl://0.0.0.0:61443");
			ClientServiceFactory clientFactory = new ClientServiceFactory();
			clientFactory.updated(properties);
			
			//Step1: Create GOSS Client
			Credentials credentials = new UsernamePasswordCredentials(
					GridAppsDConstants.username, GridAppsDConstants.password);
	//		client = clientFactory.create(PROTOCOL.OPENWIRE, credentials);
			client = clientFactory.create(PROTOCOL.STOMP, credentials);
		}
		return client;
	}
	
	@Override
		protected void finalize() throws Throwable {
			// TODO Auto-generated method stub
			super.finalize();
			if(client!=null){
				client.close();
			}
		}
}

