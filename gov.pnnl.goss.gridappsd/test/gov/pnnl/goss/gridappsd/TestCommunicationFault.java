package gov.pnnl.goss.gridappsd;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import gov.pnnl.goss.gridappsd.dto.BaseEvent;
import gov.pnnl.goss.gridappsd.dto.BaseEventCommand;
import gov.pnnl.goss.gridappsd.dto.CommunicationFault;
import gov.pnnl.goss.gridappsd.dto.CommunicationFaultData;
import gov.pnnl.goss.gridappsd.dto.CommunicationFaultObjectPair;
import gov.pnnl.goss.gridappsd.dto.DifferenceMessage;
import gov.pnnl.goss.gridappsd.dto.EventCommand;
import gov.pnnl.goss.gridappsd.dto.FailureEvent;
import gov.pnnl.goss.gridappsd.dto.FaultCommand;
//import gov.pnnl.goss.gridappsd.dto.RuntimeTypeAdapterFactory;
import gov.pnnl.goss.gridappsd.dto.FaultImpedance;
import gov.pnnl.goss.gridappsd.dto.SimulationFault;


public class TestCommunicationFault {
	@Test
	public void testFault() {

		GsonBuilder gsonBuilder = new GsonBuilder();
		
//		RuntimeTypeAdapterFactory<BaseEventCommand> commandAdapterFactory = RuntimeTypeAdapterFactory.of(BaseEventCommand.class, "type")
//				.registerSubtype(FaultCommand.class,"FaultCommand")
//		    .registerSubtype(EventCommand.class, "EventCommand");
		
//		RuntimeTypeAdapterFactory<BaseEvent> eventAdapterFactory = RuntimeTypeAdapterFactory.of(BaseEvent.class, "type")
//				.registerSubtype(FailureEvent.class,"FailureEvent")
//		    .registerSubtype(CommunicationFaultData.class, "CommunicationFaultData");
		
//		gsonBuilder.registerTypeAdapterFactory(commandAdapterFactory);
//		gsonBuilder.registerTypeAdapterFactory(eventAdapterFactory);
		gsonBuilder.setPrettyPrinting();
		Gson gson = gsonBuilder.create();
		
		DifferenceMessage dm = new DifferenceMessage ();
		dm.difference_mrid="1234";

		CommunicationFault commFault = new CommunicationFault();
		commFault.object = "UU1234";
		commFault.attribute = "FilterObject";
		commFault.value = new CommunicationFaultData();
		CommunicationFaultObjectPair tempCFOP = new CommunicationFaultObjectPair();
		tempCFOP.objectMRID = "UU123214";
		tempCFOP.attribute = "RegulatingControl.mode";
		commFault.value.inputList.add(tempCFOP); 
		commFault.value.outputList.add("UU12323");
		commFault.value.filterAllInputs = false;
		commFault.value.filterAllOutputs = false;
		commFault.value.timeInitiated = 1248156005L;
		commFault.value.timeCleared = 1248156008L;

		
		System.out.println(new String (commFault.toString()));
		String tempStr = "{\"inputList\":[{\"objectMRID\":\"UU123214\",\"attribute\":\"RegulatingControl.mode\"}],\"outputList\":[\"UU12323\"],\"filterAllInputs\":false,\"filterAllOutputs\":false,\"timeInitiated\":1248156005,\"timeCleared\":1248156008}";
		CommunicationFaultData tempComm = CommunicationFaultData.parse(tempStr);
		System.out.println(gson.toJson(tempComm));
		System.out.println("************************");
		System.out.println(gson.toJson(tempComm.buildSimFault()));
		System.out.println("************************");
		
		EventCommand eventCommand = new EventCommand();
		eventCommand.command = "CommEvent";
		eventCommand.simulation_id = 9999999; 
		eventCommand.message = tempComm;
		System.out.println(gson.toJson(eventCommand, EventCommand.class));
		
		
		FailureEvent simFault = new FailureEvent();
		simFault.faultMRID = "_1f4467ee-678b-49c6-b58c-9f9462cf5ae4";
		simFault.rGround = 0.0;
		simFault.xGround = 0.5;
		simFault.PhaseConnectedFaultKind="lineToGround";
		FaultCommand faultCommand = new FaultCommand();
		faultCommand.command = "FaultEvent";
		faultCommand.simulation_id = 9999999; 
		faultCommand.message = simFault;
		System.out.println(gson.toJson(faultCommand));
		
		String faultCommandString = "{\"message\":{\"rGround\":0.0,\"xGround\":0.5,\"rLineToLine\":0.0,\"xLineToLine\":0.0,\"PhaseConnectedFaultKind\":\"lineToGround\",\"faultMRID\":\"_1f4467ee-678b-49c6-b58c-9f9462cf5ae4\"},\"command\":\"FaultEvent\",\"simulation_id\":9999999}";
		
		
		String eventCommandString = "{\"type\":\"EventCommand\",\"command\": \"CommEvent\", \"simulation_id\": 9999999, \"message\": {\"inputList\": [{\"objectMRID\": \"UU123214\", \"attribute\": \"RegulatingControl.mode\"}], \"outputList\": [\"UU12323\"], \"filterAllInputs\": false, \"filterAllOutputs\": false, \"timeInitiated\": 1248156005, \"timeCleared\": 1248156008}}";

		
		if(eventCommandString.contains("CommEvent")){
			EventCommand testEventCommand = EventCommand.parse(eventCommandString);
			System.out.println(gson.toJson(testEventCommand));
		} else if(eventCommandString.contains("FaultEvent")){
			FaultCommand testEventCommand = FaultCommand.parse(eventCommandString);
			System.out.println(gson.toJson(testEventCommand));
		}
			
//		BaseEventCommand testEventCommand2 = gson.fromJson(tempString, BaseEventCommand.class);
//		System.out.println(gson.toJson(testEventCommand2));
		
		System.out.println(gson.toJson(commFault));
		
		dm.forward_differences.clear();
		dm.reverse_differences = null;
		dm.forward_differences.add(commFault);
		System.out.println(dm.toString());
		
		dm = new DifferenceMessage ();
		dm.difference_mrid="_"+UUID.randomUUID();
		dm.forward_differences.add(commFault);
//		dm.reverse_differences.add();
		System.out.println(dm.toString());
		
		JsonObject input = new JsonObject();
		input.addProperty("simulation_id", 1231234567);
		input.addProperty("timestamp", 1374498000);
		input.add("message", dm.toJsonElement());
		JsonObject command = new JsonObject();
		command.addProperty("command", "CommEvent");
		command.add("input", input);
		System.out.println(command.toString());
		System.out.println(gson.toJson(command));
		
		Assert.assertEquals(command.get("command").getAsString(), "CommEvent");
		JsonObject inputObject = command.getAsJsonObject().get("input").getAsJsonObject();
		
		Assert.assertEquals(inputObject.get("simulation_id").getAsInt(), 1231234567);
		JsonObject firstForwardObject = inputObject.get("message").getAsJsonObject().get("forward_differences").getAsJsonArray().get(0).getAsJsonObject();
		Assert.assertEquals(firstForwardObject.get("attribute").getAsString(), "FilterObject");

		Assert.assertEquals(firstForwardObject.get("value").getAsJsonObject().get("inputList").getAsJsonArray().get(0).getAsJsonObject().get("attribute").getAsString(), "RegulatingControl.mode");

		
//		HashMap<Integer, ProcessEvents> processEventsMap = new HashMap<Integer, ProcessEvents>(10);
//		ProcessEvents pe;
//		if(! processEventsMap.containsKey(123) ){
//			pe = processEventsMap.getOrDefault(123, new ProcessEvents(null,null, 123));
//			processEventsMap.putIfAbsent(123, pe);
//		}
//		pe = processEventsMap.get(123);
//		System.out.println(pe);
//		System.out.println(pe);
	}
}
