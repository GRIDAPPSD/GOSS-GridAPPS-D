package pnnl.goss.gridappsd;


import junit.framework.TestCase;

import java.io.Serializable;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;

import com.google.gson.Gson;

import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.core.client.ClientServiceFactory;
import pnnl.goss.gridappsd.dto.PowerSystemConfig;
import pnnl.goss.gridappsd.utils.GridAppsDConstants;

public class TestDataApplication extends TestCase {

	ClientFactory clientFactory = new ClientServiceFactory();
	
	Client client;

	public void testApplication() {

		try {
			
			//Step1: Create GOSS Client
			Credentials credentials = new UsernamePasswordCredentials(
					GridAppsDConstants.username, GridAppsDConstants.password);
			client = clientFactory.create(PROTOCOL.STOMP, credentials);
			
			//Create Request Simulation object
			PowerSystemConfig powerSystemConfig = new PowerSystemConfig();
			powerSystemConfig.GeographicalRegion_name = "ieee8500_Region";
			powerSystemConfig.SubGeographicalRegion_name = "ieee8500_SubRegion";
			powerSystemConfig.Line_name = "ieee8500";
			
			
			Gson  gson = new Gson();
			String request = gson.toJson(powerSystemConfig); 
//			DataRequest request = new DataRequest();
//			request.setRequestContent(powerSystemConfig);
			String response = client.getResponse(request,GridAppsDConstants.topic_requestData, null).toString();
			
			//TODO subscribe to response
			client.subscribe(GridAppsDConstants.topic_simulationOutput+response, new GossResponseEvent() {
				
				
				@Override
				public void onMessage(Serializable response) {
					System.out.println("simulation output is: "+response);
					
				}
			});
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	
}
