package pnnl.goss.gridappsd.process;

import java.util.Random;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;

import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.DataResponse;
import pnnl.goss.gridappsd.utils.GridAppsDConstants;

@Component
public class ProcessSimulationRequest {
	
	@ServiceDependency
	private volatile ClientFactory clientFactory;
	
	public void process(DataResponse event) throws Exception{
		
		/*
		 * Make getResponse() call to configuration Manager and get file locations
		 * MAke Asynchronous call to Simulation manager with simulation id and files path
		 * After each step update status on topic_simulationStatus+simulationId
		 */
		
		Credentials credentials = new UsernamePasswordCredentials(
				GridAppsDConstants.username, GridAppsDConstants.password);
		Client client = clientFactory.create(PROTOCOL.STOMP,credentials);
		
		//generate simulation id and reply to event's reply destination.
		int simulationId = generateSimulationId();
		client.publish(event.getReplyDestination(), simulationId);
		
		//make request to configuration Manager to get power grid model file locations and names
		String simulationFilePathWithName = client.getResponse(event.getData(), GridAppsDConstants.topic_configuration, null).toString();
		System.out.println("Response from Config Manager: "+simulationFilePathWithName);
		
		
		String message = "{'SimulationId':"+simulationId+", 'SimulationFile': '"+simulationFilePathWithName+"'}";
		client.publish(GridAppsDConstants.topic_simulation,message);
		
		
		
		
		
	}
	
	/**
	 * Generates and returns simulation id
	 * @return simulation id
	 */
	public static int generateSimulationId(){
		/*
		 * TODO: 
		 * Get the latest simulation id from database and return +1 
		 * Store the new id in database
		 */
		return new Random().nextInt();
	}
	
	

	
}
