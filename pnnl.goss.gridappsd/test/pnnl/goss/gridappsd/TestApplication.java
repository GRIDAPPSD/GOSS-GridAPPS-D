package pnnl.goss.gridappsd;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import junit.framework.TestCase;
import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.core.Request.RESPONSE_FORMAT;
import pnnl.goss.core.client.ClientServiceFactory;
import pnnl.goss.gridappsd.dto.PowerSystemConfig;
import pnnl.goss.gridappsd.dto.RequestSimulation;
import pnnl.goss.gridappsd.dto.SimulationConfig;
import pnnl.goss.gridappsd.utils.GridAppsDConstants;

public class TestApplication extends TestCase {

	private static Logger log = LoggerFactory.getLogger(TestApplication.class);
	
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
			
			SimulationConfig simulationConfig = new SimulationConfig();
			simulationConfig.duration = 60; //.setDuration("");
			// TODO: Should this be an array?
			//simulationConfig.output_object_mrid = ""; //.setOutput_object_mrid(null);
			simulationConfig.power_flow_solver_method = ""; //.setPower_flow_solver_method("");
			simulationConfig.simulation_id = ""; //.setSimulation_name("");
			simulationConfig.simulator = ""; //.setSimulator("");
			// TODO: Should this be an array?
			//simulationConfig.simulator_name  ""; //.setSimulator_name(null);
			
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			simulationConfig.start_time = sdf.format(new Date()); //.setStart_time("");
			
			RequestSimulation requestSimulation = new RequestSimulation(powerSystemConfig, simulationConfig);
			
			Gson  gson = new Gson();
			String request = gson.toJson(requestSimulation); 
			request = "{\"power_system_config\":{\"GeographicalRegion_name\":\"ieee13nodecktassets_Region\",\"SubGeographicalRegion_name\":\"ieee13nodecktassets_SubRegion\",\"Line_name\":\"ieee13nodecktassets\"}, "+
					    "\"simulation_config\":{\"start_time\":\"03/07/2017 00:00:00\",\"duration\":\"60\",\"simulator\":\"GridLAB-D\",\"simulation_name\":\"my test simulation\",\"power_flow_solver_method\":\"FBS\"}}";
			String simulationId = client.getResponse(request, GridAppsDConstants.topic_requestSimulation, RESPONSE_FORMAT.JSON).toString();
			assertNotNull(simulationId);
			log.debug("REceived simulation id  = "+simulationId);
			
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
