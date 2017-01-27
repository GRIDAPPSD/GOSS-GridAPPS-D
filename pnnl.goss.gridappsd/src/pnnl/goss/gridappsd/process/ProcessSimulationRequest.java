package pnnl.goss.gridappsd.process;

import java.util.Random;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.DataResponse;
import pnnl.goss.gridappsd.configuration.ConfigurationManager;
import pnnl.goss.gridappsd.data.DataManager;
import pnnl.goss.gridappsd.simulation.SimulationManager;
import pnnl.goss.gridappsd.utils.GridAppsDConstants;

@Component
public class ProcessSimulationRequest {
	
	@ServiceDependency
	private volatile ClientFactory clientFactory;
	
	@ServiceDependency
	private SimulationManager simulationManager;
	
	@ServiceDependency
	private DataManager dataManager;
	
	@ServiceDependency
	private ConfigurationManager configurationManager;
	
	private static Logger log = LoggerFactory.getLogger(ProcessSimulationRequest.class);
	
	public void process(DataResponse event, Client client){
		
		log.debug("Received simulation request: "+ event.getData());
		
		//generate simulation id and reply to event's reply destination.
		int simulationId = generateSimulationId();
		client.publish(event.getReplyDestination(), simulationId);
		
		//make request to configuration Manager to get power grid model file locations and names
		log.debug("Creating simulation and power grid model files for simulation Id "+ simulationId);
		//File simulationFile = configurationManager.createSimulationFiles(simulationId, event.getData());
		//dataManager.createModelFiles(simulationId, event.getData());
		log.debug("Simulation and power grid model files generated for simulation Id "+ simulationId);
		
		//start simulation
		//simulationManager.startSimulation(simulationId, simulationFilePathWithName);
		
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
