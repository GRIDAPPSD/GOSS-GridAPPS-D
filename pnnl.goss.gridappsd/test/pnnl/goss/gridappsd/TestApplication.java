package pnnl.goss.gridappsd;

import java.io.Serializable;

import junit.framework.TestCase;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;

import com.google.gson.Gson;

import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.core.client.ClientServiceFactory;
import pnnl.goss.gridappsd.requests.PowerSystemConfig;
import pnnl.goss.gridappsd.requests.RequestSimulation;
import pnnl.goss.gridappsd.requests.SimulationConfig;
import pnnl.goss.gridappsd.utils.GridAppsDConstants;

public class TestApplication extends TestCase {

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
			powerSystemConfig.setGeographicalRegion_name("");
			powerSystemConfig.setSubGeographicalRegion_name("");
			powerSystemConfig.setLine_name("");
			
			SimulationConfig simulationConfig = new SimulationConfig();
			simulationConfig.setDuration("");
			simulationConfig.setOutput_object_mrid(null);
			simulationConfig.setPower_flow_solver_method("");
			simulationConfig.setSimulation_name("");
			simulationConfig.setSimulator("");
			simulationConfig.setSimulator_name(null);
			simulationConfig.setStart_time("");
			
			RequestSimulation requestSimulation = new RequestSimulation(powerSystemConfig, simulationConfig);
			
			Gson  gson = new Gson();
			String request = gson.toJson(requestSimulation); 
			
			String simulationId = client.getResponse(request, GridAppsDConstants.topic_requestSimulation, null).toString();
			
			client.subscribe(GridAppsDConstants.topic_simulationOutput+simulationId, new GossResponseEvent() {
				
				@Override
				public void onMessage(Serializable response) {
					System.out.println("simulation output is: "+response);
					
				}
			});
			
			client.subscribe(GridAppsDConstants.topic_simulationStatus+simulationId, new GossResponseEvent() {
				
				@Override
				public void onMessage(Serializable response) {
					System.out.println("simulation status is: "+response);
					
				}
			});
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	
}
