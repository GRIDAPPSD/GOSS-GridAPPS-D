package pnnl.goss.gridappsd.simulation;

import java.io.Serializable;

import pnnl.goss.core.GossResponseEvent;

/**
 *  1. Start FNCS
 *	2. Start GridLAB-D with input file location and name
 *	3. Start GOSS-FNCS Bridge
 *	4. Call FNCS IsInitialized()
 *	5. Publish 'Simulation Initialized' on 'simulation/[id]/status' once IsInitialized() returns.
 *		If IsInitialized() does not return in given time then publish error on 'simulation/[id]/status' and send 'die' message to GOSS-FNCS topic simulation/[id]/input
 * @author shar064
 *
 */
public class SimulationEvent implements GossResponseEvent {
	
	@Override
	public void onMessage(Serializable message) {
		
		/*  Parse message. message is in JSON string.
		 *  create and return response as simulation id
		 *  
		 *  make synchronous call to DataManager and receive file location
		 *  
		 *  Start FNCS
		 *	Start GridLAB-D with input file location and name
		 *	Start GOSS-FNCS Bridge
		 *	Call FNCS IsInitialized()
		 *  
		 *	Publish 'Simulation Initialized' on 'simulation/[id]/status' once IsInitialized() returns.
		 *		If IsInitialized() does not return in given time then publish error on 'simulation/[id]/status' and send 'die' message to GOSS-FNCS topic simulation/[id]/input
		*/
		
		
	}
	

}
