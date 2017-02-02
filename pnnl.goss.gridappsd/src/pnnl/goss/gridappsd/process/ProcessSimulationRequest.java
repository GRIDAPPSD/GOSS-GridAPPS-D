package pnnl.goss.gridappsd.process;

import java.io.File;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pnnl.goss.core.Client;
import pnnl.goss.core.DataResponse;
import pnnl.goss.gridappsd.api.ConfigurationManager;
import pnnl.goss.gridappsd.api.SimulationManager;

public class ProcessSimulationRequest {
	
	private static Logger log = LoggerFactory.getLogger(ProcessSimulationRequest.class);
	
	public void process(DataResponse event, Client client, ConfigurationManager configurationManager, SimulationManager simulationManager){
		
		log.debug("Received simulation request: "+ event.getData());
		
		//generate simulation id and reply to event's reply destination.
		int simulationId = generateSimulationId();
		client.publish(event.getReplyDestination(), simulationId);
		
		//make request to configuration Manager to get power grid model file locations and names
		log.debug("Creating simulation and power grid model files for simulation Id "+ simulationId);
		File simulationFile = configurationManager.getSimulationFile(simulationId, event.getData());
		log.debug("Simulation and power grid model files generated for simulation Id "+ simulationId);
		
		//start simulation
		log.debug("Starting simulation for id "+ simulationId);
		simulationManager.startSimulation(simulationId, simulationFile);
		log.debug("Starting simulation for id "+ simulationId);
		
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
