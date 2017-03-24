package pnnl.goss.gridappsd.process;

import java.io.File;
import java.io.Serializable;
import java.util.Random;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.gridappsd.api.ConfigurationManager;
import pnnl.goss.gridappsd.api.ProcessManager;
import pnnl.goss.gridappsd.api.SimulationManager;
import pnnl.goss.gridappsd.api.StatusReporter;
import pnnl.goss.gridappsd.dto.RequestSimulation;
import pnnl.goss.gridappsd.utils.GridAppsDConstants;

/**
 * Process Manager subscribe to all the requests coming from Applications
 * and forward them to appropriate managers.
 * @author shar064
 *
 */
@Component
public class ProcessManagerImpl implements ProcessManager {
		
	private static Logger log = LoggerFactory.getLogger(ProcessManagerImpl.class);
	
	@ServiceDependency
	private volatile ClientFactory clientFactory;
	
	@ServiceDependency
	private volatile ConfigurationManager configurationManager;
	
	@ServiceDependency
	private volatile SimulationManager simulationManager;
	
	@ServiceDependency
	private volatile StatusReporter statusReporter;
	
	@Start
	public void start(){
		try{
			log.debug("Starting "+this.getClass().getName());
			
			Credentials credentials = new UsernamePasswordCredentials(
					GridAppsDConstants.username, GridAppsDConstants.password);
			Client client = clientFactory.create(PROTOCOL.STOMP,credentials);
			
			//TODO: subscribe to GridAppsDConstants.topic_request_prefix+/* instead of GridAppsDConstants.topic_requestSimulation
			client.subscribe(GridAppsDConstants.topic_requestSimulation, new GossResponseEvent() {
				
				@Override
				public void onMessage(Serializable message) {
					log.debug("Process manager received message ");
					DataResponse event = (DataResponse)message;
					
					statusReporter.reportStatus(String.format("Got new message in %s", getClass().getName()));
					//TODO: create registry mapping between request topics and request handlers.
					switch(event.getDestination().replace("/queue/", "")){
						case GridAppsDConstants.topic_requestSimulation : {
							log.debug("Received simulation request: "+ event.getData());
							
							//generate simulation id and reply to event's reply destination.
							int simulationId = generateSimulationId();
							client.publish(event.getReplyDestination(), simulationId);
							try{
								// TODO: validate simulation request json and create PowerSystemConfig and SimulationConfig dto objects to work with internally.
								Gson  gson = new Gson();
									
								RequestSimulation config = gson.fromJson(message.toString(), RequestSimulation.class);
								log.info("Parsed config "+config);
								if(config==null || config.getPower_system_config()==null || config.getSimulation_config()==null){
									throw new RuntimeException("Invalid configuration received");
								}
								
								
								
									
								
								//make request to configuration Manager to get power grid model file locations and names
								log.debug("Creating simulation and power grid model files for simulation Id "+ simulationId);
								File simulationFile = configurationManager.getSimulationFile(simulationId, config);
								if(simulationFile==null){
									throw new Exception("No simulation file returned for request "+config);
								}
									
									
								log.debug("Simulation and power grid model files generated for simulation Id "+ simulationId);
								
								//start simulation
								log.debug("Starting simulation for id "+ simulationId);
								simulationManager.startSimulation(simulationId, simulationFile, config.getSimulation_config());
								log.debug("Starting simulation for id "+ simulationId);
									
		//								new ProcessSimulationRequest().process(event, client, configurationManager, simulationManager); break;
							}catch (Exception e){
								e.printStackTrace();
								try {
									statusReporter.reportStatus(GridAppsDConstants.topic_simulationStatus+simulationId, "Process Initialization error: "+e.getMessage());
									log.error("Process Initialization error",e);
								} catch (Exception e1) {
									e1.printStackTrace();
								}
							}
						}
						//case GridAppsDConstants.topic_requestData : processDataRequest(); break;
						//case GridAppsDConstants.topic_requestSimulationStatus : processSimulationStatusRequest(); break;
					}
					
				}
			});
		}
		catch(Exception e){
			log.error("Error in process manager",e);
		}
		
	}
	
	

	/**
	 * Generates and returns simulation id
	 * @return simulation id
	 */
	static int generateSimulationId(){
		/*
		 * TODO: 
		 * Get the latest simulation id from database and return +1 
		 * Store the new id in database
		 */
		return Math.abs(new Random().nextInt());
	}
	
}
