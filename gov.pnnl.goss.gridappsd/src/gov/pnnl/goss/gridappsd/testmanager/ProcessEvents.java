package gov.pnnl.goss.gridappsd.testmanager;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.PriorityBlockingQueue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.dto.BaseEvent;
import gov.pnnl.goss.gridappsd.dto.DifferenceMessage;
import gov.pnnl.goss.gridappsd.dto.EventCommand;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.Client;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.GossResponseEvent;

public class ProcessEvents {
	
	PriorityBlockingQueue<BaseEvent> pq_initiated=
            new PriorityBlockingQueue<BaseEvent>(100, (a,b) -> Long.compare(a.timeInitiated , b.timeInitiated));
	PriorityBlockingQueue<BaseEvent> pq_cleared=
            new PriorityBlockingQueue<BaseEvent>(100, (a,b) -> Long.compare(a.timeCleared , b.timeCleared));
    HashMap <String,BaseEvent> feStatusMap = new HashMap<String,BaseEvent>();

    
	int cleared =0;
	int initied=0;
	
    LogManager logManager;
    
    public ProcessEvents(LogManager logManager){
    	this.logManager = logManager;	
    }
    
    public ProcessEvents(LogManager logManager, List<BaseEvent> failureEvents){
    	this(logManager);
		addEvents(failureEvents);	
    }
    
    public ProcessEvents(LogManager logManager, Client client, int simulationID){
    	System.out.println("New " + this.getClass().getSimpleName());
    	this.logManager = logManager;
    	processEvents(client, simulationID);
    }

	public void addEvents(List<? extends BaseEvent> failureEvents) {
		for (BaseEvent failureEvent : failureEvents){
			addEvent(failureEvent);
		}
	}

	private void addEvent(BaseEvent failureEvent) {
		pq_initiated.add(failureEvent);
		pq_cleared.add(failureEvent);
		failureEvent.status = "in queue";
		feStatusMap.put(failureEvent.faultMRID, failureEvent);
	}
	
//	private void addEvent(BaseEvent failureEvent) {
//		pq_initiated.add(failureEvent);
//		pq_cleared.add(failureEvent);
//		StatusEvent se = new StatusEvent();
//		se.faultMRID ="_"+UUID.randomUUID();
//		se.event = failureEvent;
//		se.timeInitiated = failureEvent.timeInitiated;
//		se.timeCleared = failureEvent.timeCleared;
//		if(failureEvent instanceof FailureEvent){
//			se.faultMRID = ((FailureEvent)failureEvent).faultMRID;
//		}
//		se.status = "in queue";
//		feStatusMap.put(se.faultMRID, se);
//	}
	
	public Collection<BaseEvent> getStatus(){
		return feStatusMap.values();
	}
	
	public void addEventCommandMessage(EventCommand eventCommand) {
		BaseEvent baseEvent = eventCommand.message;
		addEvent(baseEvent);
	}
	
	private void processEvents(Client client, int simulationID) {
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
	    		System.out.println("pq_initiated.size() " +pq_initiated.size() + " pq_cleared.size() " +pq_cleared.size());
	    		
	    		if(! pq_initiated.isEmpty()){
	    			System.out.println(pq_initiated.size() +" pq_initiated.peek().timeInitiated " + 
	    							pq_initiated.peek().timeInitiated + "current_time " + current_time);
	    		}
	        	while (! pq_initiated.isEmpty() && pq_initiated.peek().timeInitiated <= current_time){
	        		BaseEvent temp = pq_initiated.remove();
		    		Object simFault = temp.buildSimFault();
//	        		logMessage("Adding fault " + simFault.toString());
		    		dm.forward_differences.add(simFault);
		    		feStatusMap.get(temp.faultMRID).status = "initiated";
		    		initied++;
	        	}
	        	while (! pq_cleared.isEmpty() && pq_cleared.peek().timeCleared <= current_time){
	        		BaseEvent temp = pq_cleared.remove();
	        		Object simFault = temp.buildSimFault();
//	        		logMessage("Remove fault " + simFault.toString());
		    		dm.reverse_differences.add(simFault);
		    		feStatusMap.get(temp.faultMRID).status = "cleared";
		    		cleared++;
	        	}
	        	System.out.println("initied " +initied + " cleared " + cleared);
	        	System.out.println(getStatus().toString());
	        	
	    		// TODO Add difference messages and send to simulator
	    		if (! (dm.forward_differences.isEmpty() && dm.reverse_differences.isEmpty()) ){ 
	    			JsonObject command = createInputCommand(dm.toJsonElement(), simulationID);
	    			command.add("input", dm.toJsonElement());
	    			System.out.println(command.toString());
	    			logMessage("Sending command to " + command.toString(), simulationID);
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
	
//	public static SimulationFault buildSimFault(FailureEvent temp) {
//		SimulationFault simFault = new SimulationFault();
//		simFault.FaultMRID = temp.faultMRID;
//		simFault.ObjectMRID = temp.equipmentMRID;
//		simFault.PhaseCode = temp.phases;
//		simFault.PhaseConnectedFaultKind = temp.PhaseConnectedFaultKind;
//		simFault.FaultImpedance = new FaultImpedance();
//		simFault.FaultImpedance.rGround = temp.rGround;
//		simFault.FaultImpedance.xGround = temp.xGround;
//		simFault.FaultImpedance.rLineToLine = temp.rLineToLine;
//		simFault.FaultImpedance.xLineToLine = temp.xLineToLine;
//		return simFault;
//	}
	
	public void logMessage(String msgStr, int simulationId) {
		LogMessage logMessageObj = new LogMessage();
		logMessageObj.setProcessId(""+simulationId);
		logMessageObj.setLogLevel(LogLevel.DEBUG);
		logMessageObj.setSource(this.getClass().getSimpleName());
		logMessageObj.setProcessStatus(ProcessStatus.RUNNING);
		logMessageObj.setStoreToDb(true);
		logMessageObj.setTimestamp(new Date().getTime());
		logMessageObj.setLogMessage(msgStr);
		logManager.log(logMessageObj,GridAppsDConstants.username,GridAppsDConstants.topic_platformLog);
	}

}
