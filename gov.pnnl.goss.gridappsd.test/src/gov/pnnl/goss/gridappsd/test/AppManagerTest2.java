package gov.pnnl.goss.gridappsd.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.jms.JMSException;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;

import com.northconcepts.exception.SystemException;

import gov.pnnl.goss.gridappsd.dto.AppInfo;
import gov.pnnl.goss.gridappsd.dto.AppInfo.AppType;
import gov.pnnl.goss.gridappsd.dto.RequestAppRegister;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.Request.RESPONSE_FORMAT;
import pnnl.goss.core.client.ClientServiceFactory;
import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;

public class AppManagerTest2 {

	ClientFactory clientFactory = new ClientServiceFactory();
	Client client;
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new AppManagerTest2().test();

	}
	
	
	
	public void test(){

		try {
			
			//Step1: Create GOSS Client
			Credentials credentials = new UsernamePasswordCredentials(
					GridAppsDConstants.username, GridAppsDConstants.password);
			client = clientFactory.create(PROTOCOL.STOMP, credentials);
			
			//Create Request Simulation object
//			PowerSystemConfig powerSystemConfig = new PowerSystemConfig();
//			powerSystemConfig.GeographicalRegion_name = "ieee8500_Region";
//			powerSystemConfig.SubGeographicalRegion_name = "ieee8500_SubRegion";
//			powerSystemConfig.Line_name = "ieee8500";
			
			
//			Gson  gson = new Gson();
//			String request = gson.toJson(powerSystemConfig); 
//			DataRequest request = new DataRequest();
//			request.setRequestContent(powerSystemConfig);
			System.out.println(client);
			
			
			registerApp(client);
			
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

	
	
	public void registerApp(Client client) throws IOException, SystemException, JMSException{
		File f = new File("");
		byte[] fileData = Files.readAllBytes(f.toPath());
		AppInfo appInfo = new AppInfo();
		appInfo.setId("vvo");
		appInfo.setCreator("pnnl");
		appInfo.setDescription("VVO app");
		appInfo.setExecution_path("");
//		appInfo.setInputs(inputs);
		appInfo.setLaunch_on_startup(false);
		appInfo.setMultiple_instances(true);
//		appInfo.setOptions(options);
//		appInfo.setPrereqs(prereqs);
		appInfo.setType(AppType.PYTHON);
		
		RequestAppRegister appRegister = new RequestAppRegister(appInfo,fileData);
		String response = client.getResponse(appInfo,GridAppsDConstants.topic_app_register, RESPONSE_FORMAT.JSON).toString();
		
		
		
		
		
	}
}
