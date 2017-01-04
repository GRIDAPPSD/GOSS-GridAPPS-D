package pnnl.goss.gridappsd.process;

import java.io.Serializable;

import pnnl.goss.core.GossResponseEvent;

public class ProcessEvent implements GossResponseEvent {
	
	@Override
	public void onMessage(Serializable message) {
		
		System.out.println(message);
		/*
		 * TODO: 
		 * Create simulation id and send back to TestApp
		 * Persist simulationId with current status in memory
		 * Make getResponse() call to configuration Manager and get file locations
		 * Make getResponse() call to Simulation manager
		 * After each step update status on topic_simulationStatus+simulationId
		 */
		
		
		
	}
	
}
