package gov.pnnl.goss.gridappsd;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import gov.pnnl.goss.gridappsd.dto.DifferenceMessage;
import gov.pnnl.goss.gridappsd.dto.events.EventCommand;
import gov.pnnl.goss.gridappsd.dto.events.Fault;
import gov.pnnl.goss.gridappsd.dto.events.FaultCommand;

/**
 * Tests for communication fault and event command parsing/serialization.
 */
public class TestCommunicationFault {

    /**
     * Test FaultCommand creation and serialization.
     */
    @Test
    public void testFaultCommand_canBeCreatedAndSerialized() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting();
        Gson gson = gsonBuilder.create();

        Fault simFault = new Fault();
        simFault.faultMRID = "_1f4467ee-678b-49c6-b58c-9f9462cf5ae4";

        FaultCommand faultCommand = new FaultCommand();
        faultCommand.command = "FaultEvent";
        faultCommand.simulation_id = 9999999;
        faultCommand.message = simFault;

        String json = gson.toJson(faultCommand);

        Assert.assertNotNull("FaultCommand JSON should not be null", json);
        Assert.assertTrue("JSON should contain FaultEvent", json.contains("FaultEvent"));
        Assert.assertTrue("JSON should contain simulation_id", json.contains("9999999"));
        Assert.assertTrue("JSON should contain faultMRID", json.contains("_1f4467ee-678b-49c6-b58c-9f9462cf5ae4"));
    }

    /**
     * Test FaultCommand parsing from JSON string.
     */
    @Test
    public void testFaultCommand_canBeParsedFromJson() {
        String faultCommandString = "{\"message\":{\"rGround\":0.0,\"xGround\":0.5,\"rLineToLine\":0.0,\"xLineToLine\":0.0,\"PhaseConnectedFaultKind\":\"lineToGround\",\"faultMRID\":\"_1f4467ee-678b-49c6-b58c-9f9462cf5ae4\"},\"command\":\"FaultEvent\",\"simulation_id\":9999999}";

        FaultCommand testEventCommand = FaultCommand.parse(faultCommandString);

        Assert.assertNotNull("Parsed FaultCommand should not be null", testEventCommand);
        Assert.assertEquals("Command should be FaultEvent", "FaultEvent", testEventCommand.command);
        Assert.assertEquals("Simulation ID should be 9999999", 9999999L, (long) testEventCommand.simulation_id);
        Assert.assertNotNull("Message (Fault) should not be null", testEventCommand.message);
    }

    /**
     * Test EventCommand parsing from JSON string.
     */
    @Test
    public void testEventCommand_canBeParsedFromJson() {
        String eventCommandString = "{\"type\":\"EventCommand\",\"command\": \"CommEvent\", \"simulation_id\": 9999999, \"message\": {\"inputList\": [{\"objectMRID\": \"UU123214\", \"attribute\": \"RegulatingControl.mode\"}], \"outputList\": [\"UU12323\"], \"filterAllInputs\": false, \"filterAllOutputs\": false, \"timeInitiated\": 1248156005, \"timeCleared\": 1248156008}}";

        EventCommand testEventCommand = EventCommand.parse(eventCommandString);

        Assert.assertNotNull("Parsed EventCommand should not be null", testEventCommand);
        Assert.assertEquals("Command should be CommEvent", "CommEvent", testEventCommand.command);
        Assert.assertEquals("Simulation ID should be 9999999", 9999999L, (long) testEventCommand.simulation_id);
    }

    /**
     * Test DifferenceMessage creation and serialization.
     */
    @Test
    public void testDifferenceMessage_canBeCreatedAndSerialized() {
        DifferenceMessage dm = new DifferenceMessage();
        dm.difference_mrid = "_test-mrid-1234";

        String json = dm.toString();

        Assert.assertNotNull("DifferenceMessage JSON should not be null", json);
        Assert.assertTrue("JSON should contain difference_mrid", json.contains("_test-mrid-1234"));
    }

    /**
     * Test building a command JsonObject with DifferenceMessage.
     */
    @Test
    public void testCommandJsonObject_canBeBuilt() {
        DifferenceMessage dm = new DifferenceMessage();
        dm.difference_mrid = "_test-mrid-5678";

        JsonObject input = new JsonObject();
        input.addProperty("simulation_id", 1231234567);
        input.addProperty("timestamp", 1374498000);
        input.add("message", dm.toJsonElement());

        JsonObject command = new JsonObject();
        command.addProperty("command", "CommEvent");
        command.add("input", input);

        Assert.assertEquals("Command should be CommEvent", "CommEvent", command.get("command").getAsString());

        JsonObject inputObject = command.getAsJsonObject().get("input").getAsJsonObject();
        Assert.assertEquals("Simulation ID should be 1231234567", 1231234567,
                inputObject.get("simulation_id").getAsInt());
        Assert.assertEquals("Timestamp should be 1374498000", 1374498000, inputObject.get("timestamp").getAsInt());

        // Verify the message contains the difference_mrid
        JsonObject messageObject = inputObject.get("message").getAsJsonObject();
        Assert.assertEquals("Difference MRID should match", "_test-mrid-5678",
                messageObject.get("difference_mrid").getAsString());
    }

}
