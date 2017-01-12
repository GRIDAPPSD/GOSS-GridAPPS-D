package pnnl.goss.gridappsd.process;

import java.io.Serializable;

import pnnl.goss.core.DataResponse;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.gridappsd.utils.GridAppsDConstants;

public class ProcessEvent implements GossResponseEvent {
	
	@Override
	public void onMessage(Serializable message) {
		try{
			DataResponse event = (DataResponse)message;
			System.out.println(event.getDestination());
			
			//TODO: create registry mapping between request topics and request handlers.
			switch(event.getDestination()){
				case GridAppsDConstants.topic_requestSimulation : new ProcessSimulationRequest().process(event); break;
				//case GridAppsDConstants.topic_requestData : processDataRequest(); break;
				//case GridAppsDConstants.topic_requestSimulationStatus : processSimulationStatusRequest(); break;
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
}
