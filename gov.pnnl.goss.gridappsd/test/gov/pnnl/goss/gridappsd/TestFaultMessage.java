package gov.pnnl.goss.gridappsd;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import gov.pnnl.goss.gridappsd.dto.DifferenceMessage;
//import gov.pnnl.goss.gridappsd.dto.events.SimulationFault;

public class TestFaultMessage {
    @Test
    public void testFault() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        DifferenceMessage dm = new DifferenceMessage();
        dm.difference_mrid = "_" + UUID.randomUUID();

        // SimulationFault simFault = new SimulationFault();
        // simFault.eventId = "_1f4467ee-678b-49c6-b58c-9f9462cf5ae4";
        // simFault.FaultImpedance = new FaultImpedance();
        // simFault.FaultImpedance.rGround = 0.0;
        // simFault.FaultImpedance.xGround = 0.5;
        // simFault.PhaseConnectedFaultKind="lineToGround";

        JsonArray faults = new JsonArray();
        // faults.add(simFault.toJsonElement());
        JsonObject topElement = new JsonObject();
        topElement.add("Faults", faults);
        // System.out.println(simFault.toString());
        // System.out.println(topElement.toString());

        dm.forward_differences.clear();
        dm.reverse_differences = null;
        // dm.forward_differences.add(simFault);
        System.out.println(gson.toJson(dm));

        dm = new DifferenceMessage();
        dm.difference_mrid = "_" + UUID.randomUUID();
        dm.forward_differences = null;
        JsonObject justFaultMRID = new JsonObject();
        justFaultMRID.addProperty("FaultMRID", "_1f4467ee-678b-49c6-b58c-9f9462cf5ae4");
        dm.reverse_differences.add(justFaultMRID);
        System.out.println(dm.toString());

        JsonObject input = new JsonObject();
        input.addProperty("simulation_id", 19433287);
        input.add("message", dm.toJsonElement());
        JsonObject command = new JsonObject();
        command.addProperty("command", "update");
        command.add("input", input);
        System.out.println(gson.toJson(command));
        Assert.assertEquals(command.get("command").getAsString(), "update");
        JsonObject inputObject = command.getAsJsonObject().get("input").getAsJsonObject();
        Assert.assertEquals(inputObject.get("simulation_id").getAsInt(), 19433287);
        Assert.assertEquals(inputObject.get("message").getAsJsonObject().get("reverse_differences").getAsJsonArray()
                .get(0).getAsJsonObject().get("FaultMRID").getAsString(), "_1f4467ee-678b-49c6-b58c-9f9462cf5ae4");

        // {'command': 'update', 'input': {'simulation_id': 19433287, 'message':
    }

}
