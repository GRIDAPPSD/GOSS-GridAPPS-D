package gov.pnnl.goss.gridappsd.testmanager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.PriorityBlockingQueue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.dto.DifferenceMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.SimulationContext;
import gov.pnnl.goss.gridappsd.dto.events.CommOutage;
import gov.pnnl.goss.gridappsd.dto.events.Event;
import gov.pnnl.goss.gridappsd.dto.events.EventCommand;
import gov.pnnl.goss.gridappsd.dto.events.Fault;
import gov.pnnl.goss.gridappsd.dto.events.ScheduledCommandEvent;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.Client;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.GossResponseEvent;

public class ProcessEvents {
	public enum EventStatus {
	    SCHEDULED, INITIATED, CLEARED, CANCELLED
	}
	
	private Map<String,EventStatus> eventStatus = new HashMap<String, EventStatus>();
//	private Map<String,List<Event>> TestContext = new HashMap<String, List<Event>>();
    HashMap <String,Event> feStatusMap = new HashMap<String,Event>();
	private List<Event> events = new ArrayList<Event>();
	
    public List<Event> getEvents() {
		return events;
	}

	PriorityBlockingQueue<Event> pq_initiated=
            new PriorityBlockingQueue<Event>(100, (a,b) -> Long.compare(a.occuredDateTime , b.occuredDateTime));
    PriorityBlockingQueue<Event> pq_cleared=
            new PriorityBlockingQueue<Event>(100, (a,b) -> Long.compare(a.stopDateTime , b.stopDateTime));
    
	int cleared = 0;
	int initied = 0;
	
    private LogManager logManager;
    private SimulationManager simulationManager;
    private String simulationID;
	private long start_time;
	private int duration;

    public ProcessEvents(LogManager logManager, List<Event> events, long start_time, int duration){
    	this.logManager = logManager;
		addEvents(events);
    	this.duration = duration;
    	this.start_time = start_time;
    }
    
    public ProcessEvents(LogManager logManager, Client client, String simulationID,  SimulationManager simulationManager){
    	System.out.println("New " + this.getClass().getSimpleName());
    	this.logManager = logManager;
    	this.simulationID = simulationID;
    	this.simulationManager = simulationManager;
    	processEvents(client);
    }
	
	public void addEvents(List<Event> events) {
		events.forEach(event -> {addEvent(event);});
	}

	public void addEvent(Event event) {
		if(event.occuredDateTime >= event.stopDateTime){
			logMessage("Invalid command event.occuredDateTime >= event.stopDateTime.", simulationID);
			return;
		}
		
		event.setFaultMRID("_"+UUID.randomUUID());
		event.setEvent_type(event.getClass().getSimpleName());
		feStatusMap.put(event.getFaultMRID(), event);
		eventStatus.put(event.getFaultMRID(), EventStatus.SCHEDULED);
		pq_initiated.add(event);
		pq_cleared.add(event);
		events.add(event);
	}

	public void updateEventTimes(List<Event> events) {
		for (Event event : events) {
			feStatusMap.get(event.getFaultMRID()).occuredDateTime = event.occuredDateTime;
			feStatusMap.get(event.getFaultMRID()).stopDateTime = event.stopDateTime;
		}	
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
	
	public Map<String, EventStatus> getStatus(){
		return eventStatus;
	}
	
	public JsonObject getStatusJson(){
//		JsonArray data = new JsonArray();
//		for (Event event : events) {
//			JsonElement je = gson.toJsonTree(event);
//			je.getAsJsonObject().addProperty("status", eventStatus.get(event.getFaultMRID()).toString());
//			data.add(je); 
//		}
//
//		JsonObject topElement = new JsonObject();
//		topElement.add("data", data);
		JsonObject topElement = getStatus(eventStatus,events);
		return topElement;
	}
	
	public static JsonObject getStatus(Map<String, EventStatus> eventStatus, List<Event> events) {
		Gson  gson = new Gson();
		JsonArray data = new JsonArray();
		for (Event event : events) {
			JsonElement je = gson.toJsonTree(event);
			je.getAsJsonObject().addProperty("status", eventStatus.get(event.getFaultMRID()).toString());
			data.add(je); 
		}

		JsonObject topElement = new JsonObject();
		topElement.add("data", data);
//		System.out.println(data.toString());
		return topElement;
	}
	
	public void addEventCommandMessage(EventCommand eventCommand) {
		Event baseEvent = eventCommand.message;
		addEvent(baseEvent);
	}
	
	public void processEvents(Client client) {
		client.subscribe(GridAppsDConstants.topic_FNCS_timestamp + "." + simulationID,
		new GossResponseEvent() {
			public void onMessage(Serializable message) {

				DataResponse event = (DataResponse) message;				
				String dataStr = event.getData().toString();
//				String subMsg = dataStr;
//				if (subMsg.length() >= 200)
//					subMsg = subMsg.substring(0, 200);
//				logMessage(this.getClass().getSimpleName() + "recevied message: " + subMsg + " on topic " + event.getDestination());
				JsonObject jsonObject = CompareResults.getSimulationJson(dataStr);
				long current_time = jsonObject.get("timestamp").getAsLong();
				SimulationContext simulationContext = simulationManager.getSimulationContextForId(simulationID);
				duration = simulationContext.getRequest().getSimulation_config().getDuration();
				start_time = simulationContext.getRequest().getSimulation_config().getStart_time();

				processAtTime(client, simulationID, current_time);
			}
		});
	}

	public void processAtTime(Client client, String simulationID, long current_time) {
		DifferenceMessage dm = new DifferenceMessage ();
		DifferenceMessage dmComm = new DifferenceMessage ();
		dm.difference_mrid="_"+UUID.randomUUID();
		dm.timestamp = current_time;
		dmComm.timestamp = current_time;
		
		if (! pq_initiated.isEmpty())
				System.out.println("Processing at time " + current_time + " " + pq_initiated.peek().occuredDateTime);
//		System.out.println("pq_initiated.size() " +pq_initiated.size() + " pq_cleared.size() " +pq_cleared.size());
//		
//		if(! pq_initiated.isEmpty()){
//			System.out.println(pq_initiated.size() +" pq_initiated.peek().timeInitiated " + 
//							pq_initiated.peek().occuredDateTime + " current_time " + current_time);
//		}
		while (! pq_initiated.isEmpty() && pq_initiated.peek().occuredDateTime < current_time &&
			   ! pq_cleared.isEmpty() && pq_cleared.peek().stopDateTime < current_time){
			Event temp = pq_initiated.remove();
			pq_cleared.remove();
			System.out.println("Fault event occures before the simulation start");
			logMessage("Fault event occures before the simulation start " + temp.toString(), simulationID);
    		eventStatus.put(temp.getFaultMRID(),EventStatus.CLEARED);
			initied++;
			cleared++;
		}
		long end_time = start_time + duration;
		while (! pq_initiated.isEmpty() && pq_initiated.peek().occuredDateTime > end_time){
				Event temp = pq_initiated.remove();
				pq_cleared.remove();
				System.out.println("Fault event occures after the simulation end");
				logMessage("Fault event occures after the simulation end" + temp.toString(), simulationID);
	    		eventStatus.put(temp.getFaultMRID(),EventStatus.CLEARED);
				initied++;
				cleared++;
			}
		
    	while (! pq_initiated.isEmpty() && pq_initiated.peek().occuredDateTime <= current_time){
    		Event temp = pq_initiated.remove();
//    		Object simFault = temp.buildSimFault();
//    		logMessage("Adding fault " + simFault.toString());
    		if(temp instanceof Fault){
    			Fault simFault = (Fault)temp;
    			simFault = Fault.parse(temp.toString());
    			simFault.occuredDateTime = null;
    			simFault.stopDateTime = null;
//    			logMessage("Adding fault " + simFault.toString());
    			dm.forward_differences.add(simFault);
    		}
    		if(temp instanceof CommOutage){
    			CommOutage simFault = (CommOutage)temp;
//    			logMessage("Adding fault " + simFault.toString());
    			dmComm.forward_differences.add(simFault);
    		}
    		if(temp instanceof ScheduledCommandEvent){
    			ScheduledCommandEvent simFault = (ScheduledCommandEvent)temp;
    			dm.forward_differences.addAll(simFault.getMessage().forward_differences);
    			dm.reverse_differences.addAll(simFault.getMessage().reverse_differences);
    		}
    		eventStatus.put(temp.getFaultMRID(),EventStatus.INITIATED);
    		initied++;
    	}
    	while (! pq_cleared.isEmpty() && pq_cleared.peek().stopDateTime <= current_time){
    		Event temp = pq_cleared.remove();
//    		Object simFault = temp.buildSimFault();
//    		logMessage("Remove fault " + simFault.toString());
    		if(temp instanceof Fault){
    			Fault simFault = (Fault)temp;
    			simFault = Fault.parse(temp.toString());
    			simFault.occuredDateTime = null;
    			simFault.stopDateTime = null;
//    			logMessage("Adding fault " + simFault.toString());
    			dm.reverse_differences.add(simFault);
    		}
    		if(temp instanceof CommOutage){
    			CommOutage simFault = (CommOutage)temp;
//    			logMessage("Adding fault " + simFault.toString());
    			dmComm.reverse_differences.add(simFault);
    		}
    		if(temp instanceof ScheduledCommandEvent){
    			ScheduledCommandEvent simFault = (ScheduledCommandEvent)temp;
    			// Reverse it
    			dm.forward_differences.addAll(simFault.getMessage().reverse_differences);
    			dm.reverse_differences.addAll(simFault.getMessage().forward_differences);
    		}
    		eventStatus.put(temp.getFaultMRID(),EventStatus.CLEARED);
    		cleared++;
    	}
    	
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.setPrettyPrinting();
		Gson gson = gsonBuilder.create();
    	
		JsonObject command = createDiffCommand(simulationID, dm, "update");
		if (command != null){
			System.out.println("Message to platform at time "+ current_time);
			System.out.println(gson.toJson(command));
			logMessage("Sending command to " + command.toString(), simulationID);
			client.publish(GridAppsDConstants.topic_simulationInput+"."+simulationID, command.toString());
		}
		command = createDiffCommand(simulationID, dmComm, "CommOutage");
		if (command != null) {
			command.add("input", dmComm.toJsonElement());
			System.out.println("Message to platform at time " + current_time);
			System.out.println(gson.toJson(command));
			logMessage("Sending command to " + command.toString(), simulationID);
			client.publish(GridAppsDConstants.topic_simulationInput+"."+simulationID, command.toString());
		}
	}
	
	public static JsonObject createDiffCommand(String simulationID, DifferenceMessage dm, String commandStr) {
		if (! (dm.forward_differences.isEmpty() && dm.reverse_differences.isEmpty()) ){ 
			JsonObject command = createInputCommand(dm.toJsonElement(), simulationID,commandStr);
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
