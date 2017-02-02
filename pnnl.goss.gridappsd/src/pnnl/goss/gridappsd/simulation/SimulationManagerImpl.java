package pnnl.goss.gridappsd.simulation;

import java.io.File;
import java.io.Serializable;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.core.server.ServerControl;
import pnnl.goss.gridappsd.api.SimulationManager;
import pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.gridappsd.utils.RunCommandLine;

/**
 * This represents Internal Function 405 Simulation Control Manager.
 * This is the management function that controls the running/execution of the Distribution Simulator (401).
 * @author shar064 
 */

@Component
public class SimulationManagerImpl implements SimulationManager{
	
	private static Logger log = LoggerFactory.getLogger(SimulationManagerImpl.class);
	
	Client client = null; 
	
	@ServiceDependency
	private volatile ClientFactory clientFactory;
	
	@ServiceDependency
	ServerControl serverControl;
	
	//TODO: Get these paths from pnnl.goss.gridappsd.cfg file
	String commandFNCS = "./fncs_broker 2";
	String commandGridLABD = "gridlabd";
	String commandFNCS_GOSS_Bridge = "python ./scripts/fncs_goss_bridge.py";
	
	
	@Start
	public void start(){
		try{ 
			
			log.debug("Starting "+this.getClass().getName());
			
			Credentials credentials = new UsernamePasswordCredentials(
					GridAppsDConstants.username, GridAppsDConstants.password);
			client = clientFactory.create(PROTOCOL.STOMP,credentials);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	/**
	 * This method is called by Process Manager to start a simulation
	 * @param simulationId
	 * @param simulationFile
	 */
	@Override
	public void startSimulation(int simulationId, File simulationFile){
		try{
			
			
			Thread thread = new Thread(new Runnable() {
				
				@Override
				public void run() {
					
					int currentTime = 0; //incrementing integer 0 ,1, 2.. representing seconds
					
					//Start FNCS
					RunCommandLine.runCommand(commandFNCS);
					
					//TODO: check if FNCS is started correctly and send publish simulation status accordingly
					client.publish(GridAppsDConstants.topic_simulationStatus+simulationId, "FNCS Co-Simulator started");
					
					//Start GridLAB-D
					RunCommandLine.runCommand(commandGridLABD+" "+simulationFile);
					
					//TODO: check if GridLAB-D is started correctly and send publish simulation status accordingly
					client.publish(GridAppsDConstants.topic_simulationStatus+simulationId, "GridLAB-D started");
					
					//Start GOSS-FNCS Bridge
					RunCommandLine.runCommand(commandFNCS_GOSS_Bridge);
					
					//TODO: check if bridge is started correctly and send publish simulation status accordingly
					client.publish(GridAppsDConstants.topic_simulationStatus+simulationId, "FNCS-GOSS Bridge started");
					
					//Subscribe to fncs-goss-bridge output topic
					client.subscribe(GridAppsDConstants.topic_FNCS_output, new GossResponseEvent() {
						
						@Override
						public void onMessage(Serializable response) {
							
							//TODO: check response from fncs_goss_bridge
							System.out.print(response);
							
							//Send message to fncs_goss_bridge to get output of next time step
							String message = "{'command': 'nextTimeStep', 'currentTime': "+currentTime+"}";
							client.publish(GridAppsDConstants.topic_FNCS_input, message);
							
						}
					});
					
					//Send 'isInitialized' call to fncs-goss-bridge to check initialization.
					//This call would return true/false for initialization and simulation output of time step 0.
					client.publish(GridAppsDConstants.topic_FNCS_input, "{'command': 'isInitialized'");

					
				}
			});
			
			thread.start();
			
			
		}
		catch(Exception e){
				e.printStackTrace();
		}
		
	}


}
