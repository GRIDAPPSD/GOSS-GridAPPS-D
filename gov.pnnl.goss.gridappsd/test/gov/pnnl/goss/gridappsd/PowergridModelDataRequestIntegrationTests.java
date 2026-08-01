package gov.pnnl.goss.gridappsd;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gov.pnnl.goss.gridappsd.dto.PowergridModelDataRequest;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.Client;
import pnnl.goss.core.Client.DESTINATION_TYPE;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.Request.RESPONSE_FORMAT;
import pnnl.goss.core.client.ClientServiceFactory;

/**
 * Integration test for PowergridModel data requests via the message bus.
 *
 * This test requires GridAPPS-D to be running and accessible. It tests the
 * complete request/response flow through: 1. GOSS Client -> ActiveMQ ->
 * ProcessManagerImpl -> ProcessEvent 2. ProcessEvent -> DataManager ->
 * BGPowergridModelDataManagerHandlerImpl 3. BGPowergridModelDataManagerImpl ->
 * Blazegraph -> Response back to client
 *
 * Run with: ./gradlew :gov.pnnl.goss.gridappsd:test --tests
 * PowergridModelDataRequestIntegrationTests
 *
 * Note: This test is skipped if GridAPPS-D is not running (connection fails).
 */
public class PowergridModelDataRequestIntegrationTests {

    private static final Logger log = LoggerFactory.getLogger(PowergridModelDataRequestIntegrationTests.class);

    private Client client;
    private ClientServiceFactory clientFactory;
    private boolean gridappsdAvailable = false;

    // Connection settings - matching what Python client uses
    private static final String OPENWIRE_URI = "tcp://localhost:61616";
    private static final String STOMP_URI = "stomp://localhost:61613";
    private static final String USERNAME = "system";
    private static final String PASSWORD = "manager";

    // Topic for powergridmodel requests
    private static final String POWERGRID_MODEL_TOPIC = GridAppsDConstants.topic_requestData + ".powergridmodel";

    @Before
    public void setUp() {
        try {
            log.info("Setting up integration test - attempting to connect to GridAPPS-D");

            Map<String, Object> properties = new HashMap<>();
            properties.put("goss.system.manager", USERNAME);
            properties.put("goss.system.manager.password", PASSWORD);
            properties.put("goss.openwire.uri", OPENWIRE_URI);
            properties.put("goss.stomp.uri", STOMP_URI);

            clientFactory = new ClientServiceFactory();
            clientFactory.updated(properties);

            Credentials credentials = new UsernamePasswordCredentials(USERNAME, PASSWORD);

            // Use OpenWire for Java clients - better JMS header support
            log.info("Attempting OpenWire connection to {}", OPENWIRE_URI);
            client = clientFactory.create(PROTOCOL.OPENWIRE, credentials);

            gridappsdAvailable = true;
            log.info("Successfully connected to GridAPPS-D via STOMP");

        } catch (Exception e) {
            log.warn("Could not connect to GridAPPS-D: {}. Skipping integration tests.", e.getMessage());
            gridappsdAvailable = false;
        }
    }

    @After
    public void tearDown() {
        if (client != null) {
            try {
                client.close();
                log.info("Client connection closed");
            } catch (Exception e) {
                log.warn("Error closing client: {}", e.getMessage());
            }
        }
    }

    /**
     * Test QUERY_MODEL_NAMES request - the same request that the Python notebook
     * sends. This is the request that was timing out.
     */
    @Test(timeout = 30000) // 30 second timeout to prevent hanging
    public void testQueryModelNames() throws Exception {
        Assume.assumeTrue("GridAPPS-D is not available, skipping test", gridappsdAvailable);

        log.info("=== Starting testQueryModelNames ===");
        log.info("Topic: {}", POWERGRID_MODEL_TOPIC);

        // Build the request - same as Python client sends
        PowergridModelDataRequest request = new PowergridModelDataRequest();
        request.setRequestType(PowergridModelDataRequest.RequestType.QUERY_MODEL_NAMES.toString());
        request.setResultFormat(PowergridModelDataRequest.ResultFormat.JSON.toString());

        String requestJson = request.toString();
        log.info("Request payload: {}", requestJson);

        // Send request and wait for response
        log.info("Sending request to topic: {}", POWERGRID_MODEL_TOPIC);
        long startTime = System.currentTimeMillis();

        Serializable response = client.getResponse(
                requestJson,
                POWERGRID_MODEL_TOPIC,
                RESPONSE_FORMAT.JSON,
                DESTINATION_TYPE.TOPIC);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Response received in {} ms", elapsed);
        log.info("Response type: {}", response != null ? response.getClass().getName() : "null");

        Assert.assertNotNull("Response should not be null", response);

        // Parse the response
        String responseStr = response.toString();
        log.info("Raw response: {}", responseStr.substring(0, Math.min(500, responseStr.length())));

        // Try to parse as DataResponse
        try {
            DataResponse dataResponse = DataResponse.parse(responseStr);
            log.info("Parsed DataResponse - responseComplete: {}", dataResponse.isResponseComplete());
            log.info("Data: {}", dataResponse.getData());

            Assert.assertTrue("Response should be complete", dataResponse.isResponseComplete());
            Assert.assertNotNull("Data should not be null", dataResponse.getData());

            // Verify it contains modelNames
            String dataStr = dataResponse.getData().toString();
            Assert.assertTrue("Response should contain modelNames",
                    dataStr.contains("modelNames") || dataStr.contains("models"));

        } catch (Exception e) {
            log.error("Failed to parse response as DataResponse: {}", e.getMessage());
            // Try parsing as raw JSON
            JsonObject json = JsonParser.parseString(responseStr).getAsJsonObject();
            log.info("Parsed as JSON: {}", json);
            Assert.assertTrue("Response should have data field", json.has("data"));
        }

        log.info("=== testQueryModelNames completed successfully ===");
    }

    /**
     * Test QUERY_MODEL_INFO request - returns detailed info about all models.
     */
    @Test(timeout = 30000) // 30 second timeout to prevent hanging
    public void testQueryModelInfo() throws Exception {
        Assume.assumeTrue("GridAPPS-D is not available, skipping test", gridappsdAvailable);

        log.info("=== Starting testQueryModelInfo ===");

        PowergridModelDataRequest request = new PowergridModelDataRequest();
        request.setRequestType(PowergridModelDataRequest.RequestType.QUERY_MODEL_INFO.toString());
        request.setResultFormat(PowergridModelDataRequest.ResultFormat.JSON.toString());

        String requestJson = request.toString();
        log.info("Request: {}", requestJson);

        Serializable response = client.getResponse(
                requestJson,
                POWERGRID_MODEL_TOPIC,
                RESPONSE_FORMAT.JSON,
                DESTINATION_TYPE.TOPIC);

        Assert.assertNotNull("Response should not be null", response);
        String responseStr = response.toString();
        log.info("Response received: {}",
                responseStr.substring(0, Math.min(500, responseStr.length())));

        // Parse response as JSON to verify it's valid
        // Note: GridAPPS-D returns direct JSON data, not wrapped in DataResponse format
        com.google.gson.JsonElement responseJson = com.google.gson.JsonParser.parseString(responseStr);
        Assert.assertTrue("Response should be a JSON object or array",
                responseJson.isJsonObject() || responseJson.isJsonArray());

        log.info("=== testQueryModelInfo completed successfully ===");
    }

    /**
     * Test a simple SPARQL QUERY request.
     */
    @Test(timeout = 30000) // 30 second timeout to prevent hanging
    public void testSparqlQuery() throws Exception {
        Assume.assumeTrue("GridAPPS-D is not available, skipping test", gridappsdAvailable);

        log.info("=== Starting testSparqlQuery ===");

        // Simple query to get feeder names
        String sparqlQuery = "SELECT ?feeder ?fid WHERE { " +
                "?s r:type c:Feeder. " +
                "?s c:IdentifiedObject.name ?feeder. " +
                "?s c:IdentifiedObject.mRID ?fid " +
                "} ORDER BY ?feeder LIMIT 5";

        PowergridModelDataRequest request = new PowergridModelDataRequest();
        request.setRequestType(PowergridModelDataRequest.RequestType.QUERY.toString());
        request.setQueryString(sparqlQuery);
        request.setResultFormat(PowergridModelDataRequest.ResultFormat.JSON.toString());

        String requestJson = request.toString();
        log.info("Request: {}", requestJson);

        Serializable response = client.getResponse(
                requestJson,
                POWERGRID_MODEL_TOPIC,
                RESPONSE_FORMAT.JSON,
                DESTINATION_TYPE.TOPIC);

        Assert.assertNotNull("Response should not be null", response);
        log.info("Response received: {}",
                response.toString().substring(0, Math.min(500, response.toString().length())));

        log.info("=== testSparqlQuery completed successfully ===");
    }

    /**
     * Main method to run tests manually when GridAPPS-D is running.
     */
    public static void main(String[] args) {
        PowergridModelDataRequestIntegrationTests test = new PowergridModelDataRequestIntegrationTests();

        try {
            test.setUp();

            if (!test.gridappsdAvailable) {
                System.err.println("GridAPPS-D is not available. Make sure it is running.");
                System.err.println("Start with: cd GOSS-GridAPPS-D/build/launcher && java -jar gridappsd-launcher.jar");
                System.exit(1);
            }

            System.out.println("\n========== Running Integration Tests ==========\n");

            test.testQueryModelNames();
            System.out.println("✓ testQueryModelNames PASSED\n");

            test.testQueryModelInfo();
            System.out.println("✓ testQueryModelInfo PASSED\n");

            test.testSparqlQuery();
            System.out.println("✓ testSparqlQuery PASSED\n");

            System.out.println("========== All Tests Passed ==========");

        } catch (Exception e) {
            System.err.println("Test failed with exception:");
            e.printStackTrace();
            System.exit(1);
        } finally {
            test.tearDown();
        }
    }
}
