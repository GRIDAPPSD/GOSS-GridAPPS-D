package gov.pnnl.goss.gridappsd.test;

import static org.amdatu.testing.configurator.TestConfigurator.cleanUp;
import static org.amdatu.testing.configurator.TestConfigurator.configure;
import static org.amdatu.testing.configurator.TestConfigurator.createServiceDependency;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.Date;
import java.util.Dictionary;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;

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
import com.northconcepts.exception.SystemException;

import gov.pnnl.goss.gridappsd.configuration.GLDBaseConfigurationHandler;
import gov.pnnl.goss.gridappsd.configuration.GLDSimulationOutputConfigurationHandler;
import gov.pnnl.goss.gridappsd.configuration.CIMDictionaryConfigurationHandler;
import gov.pnnl.goss.gridappsd.configuration.CIMFeederIndexConfigurationHandler;
import gov.pnnl.goss.gridappsd.configuration.CIMSymbolsConfigurationHandler;
import gov.pnnl.goss.gridappsd.configuration.GLDAllConfigurationHandler;
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
	private String simulationId;
	
	private static final String OPENWIRE_CLIENT_CONNECTION = "tcp://localhost:6000";
	private static final String STOMP_CLIENT_CONNECTION = "stomp://localhost:6000";

	public static void main (String[] args){
		ConfigurationManagerTest test = new ConfigurationManagerTest();
		test.simulationId = null;
		try{
//			test.simulationId = test.getClient().getResponse(TestConstants.REQUEST_SIMULATION_CONFIG_ESC, "goss.gridappasd.process.request.simulation", null).toString();
			test.simulationId = test.startSimulation();
		}catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("SIMULATION ID "+test.simulationId);
//		try {
//			Thread.sleep(35000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		test.testgetGLDAllConfiguration();
//		test.testgetCIMDictConfiguration();
//		test.testgetGLDSimulationOutputConfiguration();
//		test.testgetGLMBaseConfiguration();
//		test.testgetGLMSymbolsConfiguration();
//		test.testgetCIMFeederIndexConfiguration();
	}
	
	public String startSimulation() throws Exception{
		Client client = getClient();
		
		Serializable response = client.getResponse(TestConstants.REQUEST_SIMULATION_CONFIG_ESC, GridAppsDConstants.topic_requestSimulation, RESPONSE_FORMAT.JSON);
		
		
		return response.toString();
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
     * File getSimulationFile(String simulationId, RequestSimulation powerSystemConfig) throws Exception;
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
		long start = new Date().getTime();

		try {
			String objectMrid = "_4F76A5F9-271D-9EB8-5E31-AA362D86F2C3";

			ConfigurationRequest configRequest = new ConfigurationRequest();
			configRequest.setConfigurationType(GLDBaseConfigurationHandler.TYPENAME);
			Properties properties = new Properties();
			properties.setProperty(GLDBaseConfigurationHandler.ZFRACTION, "0.0");
			properties.setProperty(GLDBaseConfigurationHandler.IFRACTION, "1.0");
			properties.setProperty(GLDBaseConfigurationHandler.PFRACTION, "0.0");
			properties.setProperty(GLDBaseConfigurationHandler.SCHEDULENAME, "ieeezipload");
			properties.setProperty(GLDBaseConfigurationHandler.LOADSCALINGFACTOR, "1.0");
			properties.setProperty(GLDBaseConfigurationHandler.MODELID, objectMrid);
			if(simulationId!=null){
				properties.setProperty(GLDBaseConfigurationHandler.SIMULATIONID, simulationId);

			}
			configRequest.setParameters(properties);
			
			System.out.println("CONFIG BASE GLM REQUEST: "+GridAppsDConstants.topic_requestConfig);
			System.out.println(configRequest);
			System.out.println();
			System.out.println();						
			Client client = getClient();
			
			Serializable response = client.getResponse(configRequest.toString(), GridAppsDConstants.topic_requestConfig, RESPONSE_FORMAT.JSON);
			
			if(response instanceof String){
				String responseStr = response.toString();
				DataResponse dataResponse = DataResponse.parse(responseStr);
				System.out.println("Response: ");
				System.out.println(dataResponse.isError());
//				System.out.println(dataResponse.getData());

			} else {
				System.out.println(response);
				System.out.println(response.getClass());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		long end = new Date().getTime();
		System.out.println("Took "+(end-start)+" ms");
	}
    
	public void testgetGLDAllConfiguration(){
		long start = new Date().getTime();
		try {
			String objectMrid = "_4F76A5F9-271D-9EB8-5E31-AA362D86F2C3";

			ConfigurationRequest configRequest = new ConfigurationRequest();
			configRequest.setConfigurationType(GLDAllConfigurationHandler.TYPENAME);
			Properties properties = new Properties();
			String simId = "12345";
			if(simulationId!=null){
				simId = simulationId;
			}
			properties.setProperty(GLDAllConfigurationHandler.DIRECTORY, "/tmp/gridappsd_tmp/"+simId);
			properties.setProperty(GLDAllConfigurationHandler.SIMULATIONNAME, "ieee8500");
			properties.setProperty(GLDAllConfigurationHandler.ZFRACTION, "0.0");
			properties.setProperty(GLDAllConfigurationHandler.IFRACTION, "1.0");
			properties.setProperty(GLDAllConfigurationHandler.PFRACTION, "0.0");
			properties.setProperty(GLDAllConfigurationHandler.SCHEDULENAME, "ieeezipload");
			properties.setProperty(GLDAllConfigurationHandler.LOADSCALINGFACTOR, "1.0");
			properties.setProperty(GLDAllConfigurationHandler.SOLVERMETHOD, "NR");
			properties.setProperty(GLDAllConfigurationHandler.SIMULATIONSTARTTIME, "2018-02-18 00:00:00");
			properties.setProperty(GLDAllConfigurationHandler.SIMULATIONDURATION, "60");
			properties.setProperty(GLDAllConfigurationHandler.SIMULATIONID, simId);
			properties.setProperty(GLDAllConfigurationHandler.SIMULATIONBROKERHOST, "localhost");
			properties.setProperty(GLDAllConfigurationHandler.SIMULATIONBROKERPORT, "61616");


			properties.setProperty(GLDAllConfigurationHandler.MODELID, objectMrid);
			
			configRequest.setParameters(properties);
			
			System.out.println("CONFIG BASE ALL REQUEST: "+GridAppsDConstants.topic_requestConfig);
			System.out.println(configRequest);
			System.out.println();
			System.out.println();						
			Client client = getClient();
			
			Serializable response = client.getResponse(configRequest.toString(), GridAppsDConstants.topic_requestConfig, RESPONSE_FORMAT.JSON);
			
			if(response instanceof String){
				String responseStr = response.toString();
//				System.out.println("Response: "+responseStr);

				DataResponse dataResponse = DataResponse.parse(responseStr);
				System.out.println(dataResponse.getData());
			} else {
				System.out.println("Response: "+response);
				System.out.println(response.getClass());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		long end = new Date().getTime();
		System.out.println("Took "+(end-start)+" ms");
	}
	
	public void testgetGLDSimulationOutputConfiguration(){
		long start = new Date().getTime();
		try {
			String objectMrid = "_4F76A5F9-271D-9EB8-5E31-AA362D86F2C3";

			ConfigurationRequest configRequest = new ConfigurationRequest();
			configRequest.setConfigurationType(GLDSimulationOutputConfigurationHandler.TYPENAME);
			Properties properties = new Properties();
			
			properties.setProperty(GLDSimulationOutputConfigurationHandler.MODELID, objectMrid);
			properties.setProperty(GLDSimulationOutputConfigurationHandler.DICTIONARY_FILE, "");
			if(simulationId!=null){
				properties.setProperty(GLDBaseConfigurationHandler.SIMULATIONID, simulationId);

			}
			configRequest.setParameters(properties);
			
			System.out.println("CONFIG SIM OUTPUT REQUEST: "+GridAppsDConstants.topic_requestConfig);
			System.out.println(configRequest);
			System.out.println();
			System.out.println();						
			Client client = getClient();
			
			Serializable response = client.getResponse(configRequest.toString(), GridAppsDConstants.topic_requestConfig, RESPONSE_FORMAT.JSON);
			
			if(response instanceof String){
				String responseStr = response.toString();
//				System.out.println("Response: "+responseStr);

				DataResponse dataResponse = DataResponse.parse(responseStr);
				System.out.println(dataResponse.isError());

//				System.out.println(dataResponse.getData());
			} else {
				System.out.println(response);
				System.out.println(response.getClass());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		long end = new Date().getTime();
		System.out.println("Took "+(end-start)+" ms");
	}
    
	public void testgetGLMSymbolsConfiguration(){
		long start = new Date().getTime();
		String objectMrid = "_4F76A5F9-271D-9EB8-5E31-AA362D86F2C3";

		testConfig(CIMSymbolsConfigurationHandler.TYPENAME, objectMrid, simulationId);
		long end = new Date().getTime();
		System.out.println("Took "+(end-start)+" ms");
	}
	public void testgetCIMDictConfiguration(){
		long start = new Date().getTime();
		String objectMrid = "_4F76A5F9-271D-9EB8-5E31-AA362D86F2C3";

		testConfig(CIMDictionaryConfigurationHandler.TYPENAME, objectMrid, simulationId);
		long end = new Date().getTime();
		System.out.println("Took "+(end-start)+" ms");
	}
	public void testgetCIMFeederIndexConfiguration(){
		long start = new Date().getTime();
		String objectMrid = "_4F76A5F9-271D-9EB8-5E31-AA362D86F2C3";

		testConfig(CIMFeederIndexConfigurationHandler.TYPENAME, objectMrid, simulationId);
		long end = new Date().getTime();
		System.out.println("Took "+(end-start)+" ms");
	}
	
	
	void testConfig (String type, String modelId, String simulationId){
		try {

			ConfigurationRequest configRequest = new ConfigurationRequest();
			configRequest.setConfigurationType(type);
			Properties properties = new Properties();
			properties.setProperty(GLDAllConfigurationHandler.MODELID, modelId);
			if(simulationId!=null){
				properties.setProperty(GLDAllConfigurationHandler.SIMULATIONID, simulationId);

			}
			configRequest.setParameters(properties);
			
			System.out.println("CONFIG "+type+" REQUEST: "+GridAppsDConstants.topic_requestConfig);
			System.out.println(configRequest);
			System.out.println();
			System.out.println();						
			Client client = getClient();
			
			Serializable response = client.getResponse(configRequest.toString(), GridAppsDConstants.topic_requestConfig, RESPONSE_FORMAT.JSON);
			
			if(response instanceof String){
				String responseStr = response.toString();
				DataResponse dataResponse = DataResponse.parse(responseStr);
				System.out.println("Response: ");
//				System.out.println(dataResponse.getData());
				System.out.println(dataResponse.isError());

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
					TestConstants.username, TestConstants.password);
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

