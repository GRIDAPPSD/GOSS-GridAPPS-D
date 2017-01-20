package pnnl.goss.gridappsd.simulation;

import java.io.Serializable;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;

import pnnl.goss.core.Client;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.gridappsd.utils.RunCommandLine;

/**
 * SimulationEvent starts a single instance of simulation
 * @author shar064
 *
 */
@Component
public class SimulationEvent implements GossResponseEvent {
	
	//TODO: Get these paths from configuration files
	String commandFNCS = "./fncs_broker 2";
	String commandGridLABD = "gridlabd";
	String commandFNCS_GOSS_Bridge = "python ./scripts/fncs_goss_bridge.py";
	
	@ServiceDependency
	private volatile ClientFactory clientFactory;
	
	/**
	 * message is in the JSON string format
	 * {'SimulationId': 1, 'SimulationFile': '/path/name'}
	 */
	@Override
	public void onMessage(Serializable message) {
		
		try {
			Credentials credentials = new UsernamePasswordCredentials(
					GridAppsDConstants.username, GridAppsDConstants.password);
			
			Client client = clientFactory.create(PROTOCOL.STOMP,credentials);
			
			
			//Extract simulation id and simulation files from message
			//TODO: Parse message to get simulationId and simulationFile
			int simulationId = 1;
			String simulationFile = "filename";
			
			//Start FNCS
			RunCommandLine.runCommand(commandFNCS);
			
			//TODO: check if FNCS is started correctly and send publish simulation status accordingly
			client.publish(GridAppsDConstants.topic_simulationStatus+simulationId, "FNCS Co-Simulator started");
			
			//Start GridLAB-D
			RunCommandLine.runCommand(commandFNCS+" "+simulationFile+" "+simulationId);
			
			//TODO: check if GridLAB-D is started correctly and send publish simulation status accordingly
			client.publish(GridAppsDConstants.topic_simulationStatus+simulationId, "GridLAB-D started");
			
			//Start GOSS-FNCS Bridge
			RunCommandLine.runCommand(commandFNCS_GOSS_Bridge);
			
			//TODO: check if bridge is started correctly and send publish simulation status accordingly
			client.publish(GridAppsDConstants.topic_simulationStatus+simulationId, "FNCS-GOSS Bridge started");
			
			//Subscribe to GOSS FNCS Bridge output topic
			client.subscribe(GridAppsDConstants.topic_FNCS_output, new FNCSOutputEvent());
			
			//Communicate with GOSS FNCS Bride to get status and output
			client.publish(GridAppsDConstants.topic_FNCS, "isInitialized");
		
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	

}
