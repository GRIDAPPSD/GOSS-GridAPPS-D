package gov.pnnl.goss.gridappsd.testmanager;

import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.dto.DifferenceMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.events.Event;
import gov.pnnl.goss.gridappsd.dto.events.Fault;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.PriorityQueue;
import java.util.UUID;

import pnnl.goss.core.Client;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.GossResponseEvent;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ProcessEvents {
	
    PriorityQueue<Event> pq_initiated=
            new PriorityQueue<Event>(100, (a,b) -> Long.compare(a.occuredDateTime , b.occuredDateTime));
    PriorityQueue<Event> pq_cleared=
            new PriorityQueue<Event>(100, (a,b) -> Long.compare(a.stopDateTime , b.stopDateTime));
    
    LogManager logManager;
    
    public ProcessEvents(LogManager logManager, List<Event> events){
    	this.logManager = logManager;
		addEvents(events);	
    }

	public void addEvents(List<Event> events) {
		for (Event event : events){
			pq_initiated.add(event);
			pq_cleared.add(event);
		}
	}
	
	public void processEvents(Client client, String simulationID) {
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
	        	while (pq_initiated.size() != 0 && pq_initiated.peek().occuredDateTime <= current_time){
	        		Event temp = pq_initiated.remove();
	        		if(temp instanceof Fault){
	        			Fault simFault = (Fault)temp;
//	        			logMessage("Adding fault " + simFault.toString());
	        			dm.forward_differences.add(simFault);
	        		}
	        	}
	        	while (pq_cleared.size() != 0 && pq_cleared.peek().stopDateTime <= current_time){
	        		Event temp = pq_cleared.remove();
	        		if(temp instanceof Fault){
	        			Fault simFault = (Fault)temp;
//	        			logMessage("Remove fault " + simFault.toString());
	        			dm.reverse_differences.add(simFault);
	        		}
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

	public static JsonObject createInputCommand(JsonElement message, String simulationID){
		JsonObject input = new JsonObject();
		input.addProperty("simulation_id", simulationID);
		input.add("message", message);
		JsonObject command = new JsonObject();
		command.addProperty("command", "update");
		command.add("input", input);
		return command;
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
