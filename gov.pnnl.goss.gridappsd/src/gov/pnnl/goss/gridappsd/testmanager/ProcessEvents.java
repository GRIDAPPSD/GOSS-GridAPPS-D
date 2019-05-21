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
import gov.pnnl.goss.gridappsd.dto.DifferenceMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.events.CommOutage;
import gov.pnnl.goss.gridappsd.dto.events.Event;
import gov.pnnl.goss.gridappsd.dto.events.EventCommand;
import gov.pnnl.goss.gridappsd.dto.events.Fault;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.Client;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.GossResponseEvent;

public class ProcessEvents {
	
//	PriorityBlockingQueue<BaseEvent> pq_initiated=
//            new PriorityBlockingQueue<BaseEvent>(100, (a,b) -> Long.compare(a.timeInitiated , b.timeInitiated));
//	PriorityBlockingQueue<BaseEvent> pq_cleared=
//            new PriorityBlockingQueue<BaseEvent>(100, (a,b) -> Long.compare(a.timeCleared , b.timeCleared));
    HashMap <String,Event> feStatusMap = new HashMap<String,Event>();

    PriorityBlockingQueue<Event> pq_initiated=
            new PriorityBlockingQueue<Event>(100, (a,b) -> Long.compare(a.occuredDateTime , b.occuredDateTime));
    PriorityBlockingQueue<Event> pq_cleared=
            new PriorityBlockingQueue<Event>(100, (a,b) -> Long.compare(a.stopDateTime , b.stopDateTime));
    
	int cleared =0;
	int initied=0;
	
    LogManager logManager;
    
//    public ProcessEvents(LogManager logManager, List<Event> events){
//    	this.logManager = logManager;	
//    }
    
    public ProcessEvents(LogManager logManager, List<Event> events){
    	this.logManager = logManager;
		addEvents(events);	
    }
    
    public ProcessEvents(LogManager logManager, Client client, String simulationID){
    	System.out.println("New " + this.getClass().getSimpleName());
    	this.logManager = logManager;
    	processEvents(client, simulationID);
    }

//	public void addEvents(List<? extends BaseEvent> failureEvents) {
//		for (BaseEvent failureEvent : failureEvents){
//			addEvent(failureEvent);
//		}
//	}
	
	public void addEvents(List<Event> events) {
		for (Event event : events){
			pq_initiated.add(event);
			pq_cleared.add(event);
		}
	}

	private void addEvent(Event event) {
		pq_initiated.add(event);
		pq_cleared.add(event);
//		event.status = "in queue";
		feStatusMap.put(event.getFaultMRID(), event);
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
	
	public Collection<Event> getStatus(){
		return feStatusMap.values();
	}
	
	public void addEventCommandMessage(EventCommand eventCommand) {
		Event baseEvent = eventCommand.message;
		addEvent(baseEvent);
	}
	
	public void processEvents(Client client, String simulationID) {
		client.subscribe("/topic/" + GridAppsDConstants.topic_simulationOutput + "." + simulationID,
		new GossResponseEvent() {
			public void onMessage(Serializable message) {

				DataResponse event = (DataResponse) message;				
				String dataStr = event.getData().toString();
//				String subMsg = dataStr;
//				if (subMsg.length() >= 200)
//					subMsg = subMsg.substring(0, 200);
//				logMessage(this.getClass().getSimpleName() + "recevied message: " + subMsg + " on topic " + event.getDestination());
				JsonObject jsonObject = CompareResults.getSimulationJson(dataStr);
				long current_time = jsonObject.get("message").getAsJsonObject().get("timestamp").getAsLong();

	    		DifferenceMessage dm = new DifferenceMessage ();
	    		DifferenceMessage dmComm = new DifferenceMessage ();
	    		dm.difference_mrid="_"+UUID.randomUUID();
	    		dm.timestamp = new Date().getTime();
	    		System.out.println("pq_initiated.size() " +pq_initiated.size() + " pq_cleared.size() " +pq_cleared.size());
	    		
	    		if(! pq_initiated.isEmpty()){
	    			System.out.println(pq_initiated.size() +" pq_initiated.peek().timeInitiated " + 
	    							pq_initiated.peek().occuredDateTime + " current_time " + current_time);
	    		}
	        	while (! pq_initiated.isEmpty() && pq_initiated.peek().occuredDateTime <= current_time){
	        		Event temp = pq_initiated.remove();
//		    		Object simFault = temp.buildSimFault();
//	        		logMessage("Adding fault " + simFault.toString());
	        		if(temp instanceof Fault){
	        			Fault simFault = (Fault)temp;
	        			simFault = Fault.parse(temp.toString());
	        			simFault.occuredDateTime = null;
	        			simFault.stopDateTime = null;
//	        			logMessage("Adding fault " + simFault.toString());
	        			dm.forward_differences.add(simFault);
	        		}
	        		if(temp instanceof CommOutage){
	        			CommOutage simFault = (CommOutage)temp;
//	        			logMessage("Adding fault " + simFault.toString());
	        			dmComm.forward_differences.add(simFault);
	        		}
//		    		dm.forward_differences.add(simFault);
//		    		feStatusMap.get(temp.faultMRID).status = "initiated";
		    		initied++;
	        	}
	        	while (! pq_cleared.isEmpty() && pq_cleared.peek().stopDateTime <= current_time){
	        		Event temp = pq_cleared.remove();
//	        		Object simFault = temp.buildSimFault();
//	        		logMessage("Remove fault " + simFault.toString());
	        		if(temp instanceof Fault){
	        			Fault simFault = (Fault)temp;
	        			simFault = Fault.parse(temp.toString());
	        			simFault.occuredDateTime = null;
	        			simFault.stopDateTime = null;
//	        			logMessage("Adding fault " + simFault.toString());
	        			dm.reverse_differences.add(simFault);
	        		}
	        		if(temp instanceof CommOutage){
	        			CommOutage simFault = (CommOutage)temp;
//	        			logMessage("Adding fault " + simFault.toString());
	        			dmComm.reverse_differences.add(simFault);
	        		}
//		    		dm.reverse_differences.add(simFault);
//		    		feStatusMap.get(temp.faultMRID).status = "cleared";
		    		cleared++;
	        	}
	        	System.out.println("initied " +initied + " cleared " + cleared);
	        	System.out.println(getStatus().toString());
	        	
	    		JsonObject command = createDiffCommand(simulationID, dm, "update");
	    		if (command != null){
	    			logMessage("Sending command to " + command.toString(), simulationID);
	    			client.publish(GridAppsDConstants.topic_FNCS_input, command.toString());
	    		}
	    		command = createDiffCommand(simulationID, dmComm, "CommOutage");
	    		if (command != null) {
	    			logMessage("Sending command to " + command.toString(), simulationID);
	    			client.publish(GridAppsDConstants.FNCS_BRIDGE_PATH, command.toString());
	    		}
			}
		});
	}

	public static JsonObject createDiffCommand(String simulationID, DifferenceMessage dm, String commandStr) {
		// TODO Add difference messages and send to simulator
		if (! (dm.forward_differences.isEmpty() && dm.reverse_differences.isEmpty()) ){ 
			JsonObject command = createInputCommand(dm.toJsonElement(), simulationID,commandStr);
			command.add("input", dm.toJsonElement());
//			System.out.println(command.toString());
			return command;
		}
		return null;
	}
	
	public static JsonObject createInputCommand(JsonElement message, String simulationID, String commandStr){
		if (commandStr == null) commandStr = "update";
		JsonObject input = new JsonObject();
		input.addProperty("simulation_id", simulationID);
		input.add("message", message);
		JsonObject command = new JsonObject();
		command.addProperty("command", commandStr);
		command.add("input", input);
		return command;
	}
	
//	public static SimulationFault buildSimFault(FailureEvent temp) {
//		SimulationFault simFault = new SimulationFault();
//		simFault.eventId = temp.eventId;
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
	
	public void logMessage(String msgStr, String simulationId) {
		LogMessage logMessageObj = new LogMessage();
		logMessageObj.setProcessId(simulationId);
		logMessageObj.setLogLevel(LogLevel.DEBUG);
		logMessageObj.setSource(this.getClass().getSimpleName());
		logMessageObj.setProcessStatus(ProcessStatus.RUNNING);
		logMessageObj.setStoreToDb(true);
		logMessageObj.setTimestamp(new Date().getTime());
		logMessageObj.setLogMessage(msgStr);
		logManager.log(logMessageObj,GridAppsDConstants.username,GridAppsDConstants.topic_platformLog);
	}

}
