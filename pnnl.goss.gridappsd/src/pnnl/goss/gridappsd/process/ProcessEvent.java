package pnnl.goss.gridappsd.process;

import java.io.Serializable;

import pnnl.goss.core.DataResponse;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.gridappsd.utils.GridAppsDConstants;

public class ProcessEvent implements GossResponseEvent {
	
	@Override
	public void onMessage(Serializable message) {
		
		DataResponse event = (DataResponse)message;
		System.out.println(event);
		
		switch(event.getDestination()){
			case GridAppsDConstants.topic_requestSimulation : processSimulationRequest(); break;
			case GridAppsDConstants.topic_requestData : processDataRequest(); break;
			case GridAppsDConstants.topic_requestSimulationStatus : processSimulationStatusRequest(); break;
			
		}

		/*
		 * TODO: 
		 * Create simulation id and send back to TestApp
		 * Persist simulationId with current status in memory
		 * Make getResponse() call to configuration Manager and get file locations
		 * Make getResponse() call to Simulation manager
		 * After each step update status on topic_simulationStatus+simulationId
		 */
		
		
		
	}
		
	private void processSimulationRequest(){
		
	}
	
	private void processDataRequest(){
		
	}
	
	private void processSimulationStatusRequest(){
		
	}
	
}
