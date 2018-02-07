package gov.pnnl.goss.gridappsd.test;


import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.ConfigurationException;

import org.amdatu.testing.configurator.TestConfiguration;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.fusesource.stomp.jms.StompJmsDestination;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.northconcepts.exception.SystemException;

import gov.pnnl.goss.gridappsd.dto.AppInfo;
import gov.pnnl.goss.gridappsd.dto.PowergridModelDataRequest;
import gov.pnnl.goss.gridappsd.dto.AppInfo.AppType;
import gov.pnnl.goss.gridappsd.dto.RequestAppRegister;
import gov.pnnl.goss.gridappsd.dto.RequestAppStart;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.server.DataSourceType;
import pnnl.goss.core.server.ServerControl;
import pnnl.goss.core.Client;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.core.Request.RESPONSE_FORMAT;
import pnnl.goss.core.client.ClientServiceFactory;
import pnnl.goss.core.client.GossClient;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.DataResponse;


@RunWith(MockitoJUnitRunner.class)
public class BGPGModelManagerTest {

//	ClientFactory clientFactory;
	Client client;
	private TestConfiguration testConfig;

	public static final String APPLICATION_OBJECT_CONFIG= "{\\\"static_inputs\\\": {\\\"ieee8500\\\": {\\\"control_method\\\": \\\"ACTIVE\\\",\\\"capacitor_delay\\\": 60,\\\"regulator_delay\\\": 60,\\\"desired_pf\\\": 0.99,\\\"d_max\\\": 0.9,\\\"d_min\\\": 0.1,\\\"substation_link\\\": \\\"xf_hvmv_sub\\\",\\\"regulator_list\\\": [\\\"reg_FEEDER_REG\\\",\\\"reg_VREG2\\\",\\\"reg_VREG3\\\",\\\"reg_VREG4\\\"],\\\"regulator_configuration_list\\\": [\\\"rcon_FEEDER_REG\\\",\\\"rcon_VREG2\\\",\\\"rcon_VREG3\\\",\\\"rcon_VREG4\\\"],\\\"capacitor_list\\\": [\\\"cap_capbank0a\\\",\\\"cap_capbank0b\\\",\\\"cap_capbank0c\\\",\\\"cap_capbank1a\\\",\\\"cap_capbank1b\\\",\\\"cap_capbank1c\\\",\\\"cap_capbank2a\\\",\\\"cap_capbank2b\\\",\\\"cap_capbank2c\\\",\\\"cap_capbank3\\\"],\\\"voltage_measurements\\\": [\\\"nd_l2955047,1\\\",\\\"nd_l3160107,1\\\",\\\"nd_l2673313,2\\\",\\\"nd_l2876814,2\\\",\\\"nd_m1047574,3\\\",\\\"nd_l3254238,4\\\"],\\\"maximum_voltages\\\": 7500,\\\"minimum_voltages\\\": 6500,\\\"max_vdrop\\\": 5200,\\\"high_load_deadband\\\": 100,\\\"desired_voltages\\\": 7000,\\\"low_load_deadband\\\": 100,\\\"pf_phase\\\": \\\"ABC\\\"}}}";
	
	public static void main(String[] args) {
		
//		
//    	String str = "{\"data\":\"{\n  \\\"head\\\": {\n    \\\"vars\\\": [ \\\"line_name\\\" , \\\"subregion_name\\\" , "
//    			+ "\\\"region_name\\\" ]\n  } ,\n  \\\"results\\\": {\n    \\\"bindings\\\": [\n      {\n        \\\"line_name\\\": "
//    			+ "{ \\\"type\\\": \\\"literal\\\" , \\\"value\\\": \\\"ieee8500\\\" } ,\n        \\\"subregion_name\\\": { \\\"type\\\": \\\"literal\\\" , "
//    			+ "\\\"value\\\": \\\"ieee8500_SubRegion\\\" } ,\n        \\\"region_name\\\": { \\\"type\\\": \\\"literal\\\" , \\\"value\\\": "
//    			+ "\\\"ieee8500_Region\\\" }\n      }\n    ]\n  }\n}\n\",\"responseComplete\":true,\"id\":\"c7d32cb0-f02f-4c31-92a9-86ca028292ce\"}";

		
//		PowergridModelDataRequest datarequest = new PowergridModelDataRequest();
//		datarequest.setRequestType("OBJECT");
//		datarequest.setModelId("1234");
		
//		String dataStr = "{\n  \"head\": {\n    \"vars\": [ \"line_name\" , \"subregion_name\" , \"region_name\" ]\n  } ,\n  \"results\": {\n    \"bindings\": [\n      {\n        \"line_name\": { \"type\": \"literal\" , \"value\": \"ieee8500\" } ,\n        \"subregion_name\": { \"type\": \"literal\" , \"value\": \"ieee8500_SubRegion\" } ,\n        \"region_name\": { \"type\": \"literal\" , \"value\": \"ieee8500_Region\" }\n      }\n    ]\n  }\n}\n";
//		DataResponse dr = new DataResponse();
//		dr.setData(dataStr);
////		dr.setData(datarequest);
//		dr.setResponseComplete(true);
//		dr.setId("TEST c7d32cb0-f02f-4c31-92a9-86ca028292ce");
//		
//		String serialized = dr.toString();
//		System.out.println(serialized);
//		DataResponse parsed = DataResponse.parse(serialized);
//		System.out.println(parsed.getId());
//		System.out.println(parsed.getReplyDestination());
//		System.out.println(parsed.isResponseComplete());
//		System.out.println(parsed.isError());
//		System.out.println(parsed.getData()+" "+parsed.getData().getClass());
		
		
//		Gson gson = new Gson();
		String dataStr = "{\n  \"head\": {\n    \"vars\": [ \"line_name\" , \"subregion_name\" , \"region_name\" ]\n  } ,\n  \"results\": {\n    \"bindings\": [\n      {\n        \"line_name\": { \"type\": \"literal\" , \"value\": \"ieee8500\" } ,\n        \"subregion_name\": { \"type\": \"literal\" , \"value\": \"ieee8500_SubRegion\" } ,\n        \"region_name\": { \"type\": \"literal\" , \"value\": \"ieee8500_Region\" }\n      }\n    ]\n  }\n}\n";
//		DataResponse dr = new DataResponse();
//		dr.setData(dataStr);
//		dr.setResponseComplete(true);
//		dr.setId("TEST c7d32cb0-f02f-4c31-92a9-86ca028292ce");
//		
//		String serialized = gson.toJson(dr);
//		System.out.println(serialized);
//		DataResponse parsed = gson.fromJson(serialized, DataResponse.class);
		
//		
//		
//		Client client;
//		try {
//			client = new BGPGModelManagerTest().getClient();
//		
//		DataResponse response = new DataResponse(dataStr);
//		response.setResponseComplete(true);
//		response.setId("MYID1234");
//		client.publish("test", response); 
////this is the publisher
//		
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		new BGPGModelManagerTest().test();
		System.exit(0);
	}
	
	
	
	public void test(){

		try {

			
			
			
			
			
			
			PowergridModelDataRequest pgDataRequest = new PowergridModelDataRequest();
			String queryString = "select ?line_name ?subregion_name ?region_name WHERE {?line rdf:type cim:Line."+
	                              			 "?line cim:IdentifiedObject.name ?line_name."+
	                                         "?line cim:Line.Region ?subregion."+
	                                         "?subregion cim:IdentifiedObject.name ?subregion_name."+
	                                         "?subregion cim:SubGeographicalRegion.Region ?region."+
	                                         "?region cim:IdentifiedObject.name ?region_name"+
	                                        "}";
			pgDataRequest.setRequestType(PowergridModelDataRequest.RequestType.QUERY.toString());
			pgDataRequest.setQueryString(queryString);
			pgDataRequest.setResultFormat(PowergridModelDataRequest.ResultFormat.JSON.toString());
			pgDataRequest.setModelId(null);
			
			System.out.println(pgDataRequest);
			
			Client client = getClient();
			
			GsonBuilder gsonbuilder = new GsonBuilder();
//			gsonbuilder.registerTypeAdapter(Serializable.class, new SerializableInstanceCreator());
			Gson gson = gsonbuilder.create();
			Serializable response = client.getResponse(pgDataRequest.toString(), GridAppsDConstants.topic_requestData+".powergridmodel", RESPONSE_FORMAT.JSON);
			
			if(response instanceof String){
				String responseStr = response.toString();
				System.out.println(responseStr);
				
				DataResponse dataResponse = DataResponse.parse(responseStr);
				System.out.println(dataResponse.getData());
			} else {
				System.out.println(response);
				System.out.println(response.getClass());
			}
			
			
			
			
//			DataResponse message = new DataResponse();
//			message.setDestination(GridAppsDConstants.topic_requestData+".powergridmodel");
//			message.setData(pgDataRequest);
			
			
			
			
			
			//			Dictionary properties = new Hashtable();
//			properties.put("goss.system.manager", "system");
//			properties.put("goss.system.manager.password", "manager");
//
//			// The following are used for the core-client connection.
//			properties.put("goss.openwire.uri", "tcp://0.0.0.0:61616");
//			properties.put("goss.stomp.uri", "stomp://0.0.0.0:61613");
//			properties.put("goss.ws.uri", "ws://0.0.0.0:61614");
//			properties.put("goss.ssl.uri", "ssl://0.0.0.0:61443");
//			testConfig = configure(this)
//					.add(CoreGossConfig.configureServerAndClientPropertiesConfig())
//					.add(createServiceDependency().setService(ClientFactory.class));
//			testConfig.apply();
//			ClientServiceFactory clientFactory = new ClientServiceFactory();
//			clientFactory.updated(properties);
			
			//Step1: Create GOSS Client
//			Credentials credentials = new UsernamePasswordCredentials(
//					GridAppsDConstants.username, GridAppsDConstants.password);
//			client = clientFactory.create(PROTOCOL.OPENWIRE, credentials);
			
			//Create Request Simulation object
//			PowerSystemConfig powerSystemConfig = new PowerSystemConfig();
//			powerSystemConfig.GeographicalRegion_name = "ieee8500_Region";
//			powerSystemConfig.SubGeographicalRegion_name = "ieee8500_SubRegion";
//			powerSystemConfig.Line_name = "ieee8500";
			
			
//			Gson  gson = new Gson();
//			String request = gson.toJson(powerSystemConfig); 
//			DataRequest request = new DataRequest();
//			request.setRequestContent(powerSystemConfig);
//			System.out.println(client);
			
			
//			registerApp();
			
//			AppInfo
//			String response = client.getResponse("",GridAppsDConstants.topic_requestData, RESPONSE_FORMAT.JSON).toString();
//			
//			//TODO subscribe to response
//			client.subscribe(GridAppsDConstants.topic_simulationOutput+response, new GossResponseEvent() {
//				
//				@Override
//				public void onMessage(Serializable response) {
//					// TODO Auto-generated method stub
//					System.out.println("RESPNOSE "+response);
//				}
//			});
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	
//	public void registerApp() throws IOException, SystemException, JMSException{
//		
//		
//		
//		
//		AppInfo appInfo = new AppInfo();
//		appInfo.setId("vvo");
//		appInfo.setCreator("pnnl");
//		appInfo.setDescription("VVO app");
//		appInfo.setExecution_path("app/vvoapp.py");
//		
//		List<String> inputs = new ArrayList<String>();
//		inputs.add(GridAppsDConstants.topic_FNCS_output);
//		appInfo.setInputs(inputs);
//		
//		List<String> outputs = new ArrayList<String>();
//		outputs.add(GridAppsDConstants.topic_FNCS_input);
//		appInfo.setOutputs(outputs);
//		
//		appInfo.setLaunch_on_startup(false);
//		appInfo.setMultiple_instances(true);
//		appInfo.setOptions("SIMULATION_ID");
//		List<String> prereqs = new ArrayList<String>();
//		prereqs.add("fncs-goss-bridge");
//		appInfo.setPrereqs(prereqs);
//		appInfo.setType(AppType.PYTHON);
//		
//		System.out.println(appInfo);
//		
//		
//		File parentDir = new File(".");
//		File f = new File(parentDir.getAbsolutePath()+File.separator+"resources"+File.separator+"vvo.zip");
//		System.out.println(f.getAbsolutePath());
//		byte[] fileData = Files.readAllBytes(f.toPath());
//
//		RequestAppRegister appRegister = new RequestAppRegister(appInfo,fileData);
//		System.out.println("REGISTER"+appRegister);
//
////		DataRequest request = new DataRequest();
////		request.setRequestContent(appRegister);
////		client.publish(GridAppsDConstants.topic_requestSimulation, appRegister);
//		sendMessage(GridAppsDConstants.topic_app_register, appRegister);
////		String response = client.getResponse(request,GridAppsDConstants.topic_app_register, RESPONSE_FORMAT.JSON).toString();
//		try {
//			Thread.sleep(3000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		String runtimeOptions = "-c \""+APPLICATION_OBJECT_CONFIG+"\"";
//
//		String simulationId = "12345";
//		RequestAppStart appStart = new RequestAppStart(appInfo.getId(), runtimeOptions, simulationId);
//		sendMessage(GridAppsDConstants.topic_app_start, appStart);
//		System.out.println(appStart);
//		
//		
//		try {
//			Thread.sleep(30000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		
////		sendMessage(GridAppsDConstants.topic_app_deregister, appInfo.getId());
////		System.out.println("RESPONSE "+response);
//		
//		
//		
//	}
	
	
	private void sendMessage(String destination, Serializable message) throws JMSException{
		Gson gson = new Gson();
		StompJmsConnectionFactory connectionFactory = new StompJmsConnectionFactory();
		connectionFactory.setBrokerURI("tcp://localhost:61613");
		connectionFactory.setUsername("system");
		connectionFactory.setPassword("manager");
		Connection connection = connectionFactory.createConnection(); 
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		MessageProducer producer = session.createProducer(new StompJmsDestination(destination));
		TextMessage textMessage = null;
		if(message instanceof String){
			textMessage = session.createTextMessage(message.toString());
		} else {
			textMessage = session.createTextMessage(gson.toJson(message));
			
		}
		producer.send(textMessage);
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
	
//	private class SerializableInstanceCreator implements InstanceCreator<Serializable>{
//
//		@Override
//		public Serializable createInstance(Type type) {
//			System.out.println("type  "+type);
//			return new String();
//		}
//		
//		
//	}
	
	
	
}
