package gov.pnnl.goss.gridappsd.testmanager;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.PriorityQueue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.dto.DifferenceMessage;
import gov.pnnl.goss.gridappsd.dto.FailureEvent;
import gov.pnnl.goss.gridappsd.dto.FaultImpedance; 
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.SimulationFault;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.Client;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.GossResponseEvent;

public class ProcessEvents {
	
    PriorityQueue<FailureEvent> pq_initiated=
            new PriorityQueue<FailureEvent>(100, (a,b) -> Long.compare(a.timeInitiated , b.timeInitiated));
    PriorityQueue<FailureEvent> pq_cleared=
            new PriorityQueue<FailureEvent>(100, (a,b) -> Long.compare(a.timeCleared , b.timeCleared));
    
    LogManager logManager;
    
    public ProcessEvents(LogManager logManager, List<FailureEvent> failureEvents){
    	this.logManager = logManager;
		addEvents(failureEvents);	
    }

	public void addEvents(List<FailureEvent> failureEvents) {
		for (FailureEvent failureEvent : failureEvents){
			pq_initiated.add(failureEvent);
			pq_cleared.add(failureEvent);
		}
	}
	
	public void processEvents(Client client, int simulationID) {
		client.subscribe("/topic/" + GridAppsDConstants.topic_simulationOutput + "." + simulationID,
		new GossResponseEvent() {
			public void onMessage(Serializable message) {

				DataResponse event = (DataResponse) message;				
				String dataStr = event.getData().toString();
				String subMsg = dataStr;
				if (subMsg.length() >= 200)
					subMsg = subMsg.substring(0, 200);
//				logMessage(this.getClass().getSimpleName() + "recevied message: " + subMsg + " on topic " + event.getDestination());
				JsonObject jsonObject = CompareResults.getSimulationJson(dataStr);
				long current_time = jsonObject.get("message").getAsJsonObject().get("timestamp").getAsLong();

	    		DifferenceMessage dm = new DifferenceMessage ();
	    		dm.difference_mrid="_"+UUID.randomUUID();
	    		dm.timestamp = new Date().getTime();
	        	while (pq_initiated.size() != 0 && pq_initiated.peek().timeInitiated <= current_time){
	        		FailureEvent temp = pq_initiated.remove();
		    		SimulationFault simFault = buildSimFault(temp);
//	        		logMessage("Adding fault " + simFault.toString());
		    		dm.forward_differences.add(simFault);
	        	}
	        	while (pq_cleared.size() != 0 && pq_cleared.peek().timeCleared <= current_time){
	        		FailureEvent temp = pq_cleared.remove();
		    		SimulationFault simFault = buildSimFault(temp);
//	        		logMessage("Remove fault " + simFault.toString());
		    		dm.reverse_differences.add(simFault);
	        	}
	    		// TODO Add difference messages and send to simulator
	    		if (! (dm.forward_differences.isEmpty() && dm.reverse_differences.isEmpty()) ){ 
	    			JsonObject command = createInputCommand(dm.toJsonElement(),simulationID);
	    			command.add("input", dm.toJsonElement());
	    			System.out.println(command.toString());
	    			logMessage("Sending command" + command.toString());
	    		}
			}
		});
	}

	public static JsonObject createInputCommand(JsonElement message, int simulationID){
		JsonObject input = new JsonObject();
		input.addProperty("simulation_id", simulationID);
		input.add("message", message);
		JsonObject command = new JsonObject();
		command.addProperty("command", "update");
		command.add("input", input);
		return command;
	}
	
	public static SimulationFault buildSimFault(FailureEvent temp) {
		SimulationFault simFault = new SimulationFault();
		simFault.FaultMRID = temp.faultMRID;
		simFault.ObjectMRID = temp.equipmentMRID;
		simFault.PhaseCode = temp.phases;
		simFault.PhaseConnectedFaultKind = temp.PhaseConnectedFaultKind;
		simFault.FaultImpedance = new FaultImpedance();
		simFault.FaultImpedance.rGround = temp.rGround;
		simFault.FaultImpedance.xGround = temp.xGround;
		simFault.FaultImpedance.rLineToLine = temp.rLineToLine;
		simFault.FaultImpedance.xLineToLine = temp.xLineToLine;
		return simFault;
	}
	
	public void logMessage(String msgStr) {
		LogMessage logMessageObj = new LogMessage();
		logMessageObj.setLogLevel(LogLevel.DEBUG);
		logMessageObj.setSource(this.getClass().getSimpleName());
		logMessageObj.setProcessStatus(ProcessStatus.RUNNING);
		logMessageObj.setStoreToDb(true);
		logMessageObj.setTimestamp(new Date().getTime());
		logMessageObj.setLogMessage(msgStr);
		logManager.log(logMessageObj,GridAppsDConstants.username,GridAppsDConstants.topic_platformLog);
	}

}
