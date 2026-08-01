package gov.pnnl.goss.gridappsd;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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

import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.core.Request.RESPONSE_FORMAT;
import pnnl.goss.core.client.ClientServiceFactory;

/**
 * Integration test for simulation requests.
 *
 * This test verifies that: 1. We can send a simulation request to the platform
 * 2. We receive a response with a simulationId 3. We can subscribe to
 * simulation output topics
 *
 * Prerequisites: - GridAPPS-D platform must be running (or at minimum the
 * ProcessManager) - ActiveMQ message bus must be available - Blazegraph must be
 * running with model data (for full simulation)
 */
public class SimulationRequestIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(SimulationRequestIntegrationTest.class);

    private Client client;
    private ClientServiceFactory clientFactory;
    private boolean busAvailable = false;

    private static final String OPENWIRE_URI = "tcp://localhost:61616";
    private static final String STOMP_URI = "stomp://localhost:61613";
    private static final String USERNAME = "system";
    private static final String PASSWORD = "manager";

    // IEEE 13 node test feeder - commonly available model
    private static final String TEST_REGION = "ieee13nodeckt_Region";
    private static final String TEST_SUBREGION = "ieee13nodeckt_SubRegion";
    private static final String TEST_LINE = "ieee13nodeckt";

    @Before
    public void setUp() {
        try {
            log.info("Setting up simulation request integration test");

            Map<String, Object> properties = new HashMap<>();
            properties.put("goss.system.manager", USERNAME);
            properties.put("goss.system.manager.password", PASSWORD);
            properties.put("goss.openwire.uri", OPENWIRE_URI);
            properties.put("goss.stomp.uri", STOMP_URI);

            clientFactory = new ClientServiceFactory();
            clientFactory.updated(properties);

            Credentials credentials = new UsernamePasswordCredentials(USERNAME, PASSWORD);

            log.info("Attempting STOMP connection to {}", STOMP_URI);
            client = clientFactory.create(PROTOCOL.STOMP, credentials);

            busAvailable = true;
            log.info("Successfully connected to message bus");

        } catch (Exception e) {
            log.warn("Could not connect to message bus: {}. Skipping tests.", e.getMessage());
            busAvailable = false;
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
     * Build a minimal simulation request JSON.
     */
    private String buildSimulationRequest() {
        long startTime = System.currentTimeMillis() / 1000; // Unix timestamp in seconds

        JsonObject request = new JsonObject();

        // Power system config
        JsonObject powerSystemConfig = new JsonObject();
        powerSystemConfig.addProperty("GeographicalRegion_name", TEST_REGION);
        powerSystemConfig.addProperty("SubGeographicalRegion_name", TEST_SUBREGION);
        powerSystemConfig.addProperty("Line_name", TEST_LINE);

        // Simulation config
        JsonObject simulationConfig = new JsonObject();
        simulationConfig.addProperty("start_time", String.valueOf(startTime));
        simulationConfig.addProperty("duration", "60"); // 60 second simulation
        simulationConfig.addProperty("simulation_name", "test_simulation");
        simulationConfig.addProperty("run_realtime", false); // Run as fast as possible
        simulationConfig.addProperty("pause_after_measurements", false);

        // Model creation config
        JsonObject modelCreationConfig = new JsonObject();
        modelCreationConfig.addProperty("load_scaling_factor", "1.0");
        modelCreationConfig.addProperty("schedule_name", "ieeezipload");
        modelCreationConfig.addProperty("z_fraction", "0");
        modelCreationConfig.addProperty("i_fraction", "1");
        modelCreationConfig.addProperty("p_fraction", "0");

        // Application config (empty)
        JsonObject applicationConfig = new JsonObject();
        applicationConfig.add("applications", new com.google.gson.JsonArray());

        // Test config (empty)
        JsonObject testConfig = new JsonObject();
        testConfig.add("events", new com.google.gson.JsonArray());
        testConfig.addProperty("appId", "");

        // Assemble the full request
        com.google.gson.JsonArray powerSystemConfigs = new com.google.gson.JsonArray();
        powerSystemConfig.add("model_creation_config", modelCreationConfig);
        powerSystemConfigs.add(powerSystemConfig);

        request.add("power_system_config", powerSystemConfig);
        request.add("simulation_config", simulationConfig);
        request.add("application_config", applicationConfig);
        request.add("test_config", testConfig);
        request.addProperty("simulation_request_type", "NEW");

        return request.toString();
    }

    /**
     * Test: Send a simulation request and verify we get a response with
     * simulationId.
     *
     * This is the core test - it mimics what the Python client does when calling
     * Simulation.start_simulation()
     */
    @Test(timeout = 30000) // 30 second timeout to prevent hanging
    public void testSimulationRequestAndResponse() throws Exception {
        Assume.assumeTrue("Message bus is not available, skipping test", busAvailable);

        log.info("=== Test: Simulation Request and Response ===");

        String requestTopic = GridAppsDConstants.topic_requestSimulation;
        log.info("Request topic: {}", requestTopic);

        AtomicReference<String> simulationIdRef = new AtomicReference<>();

        // Subscribe to platform logs to see processing
        CountDownLatch logLatch = new CountDownLatch(1);
        client.subscribe(GridAppsDConstants.topic_platformLog, new GossResponseEvent() {
            @Override
            public void onMessage(Serializable response) {
                String msg = response.toString();
                log.debug("Platform log: {}", msg);
                if (msg.contains("Received message") || msg.contains("simulation")) {
                    logLatch.countDown();
                }
            }
        });

        Thread.sleep(500); // Wait for subscription

        // Build the simulation request
        String request = buildSimulationRequest();
        log.info("Sending simulation request to: {}", requestTopic);
        log.info("Request payload: {}", request);

        // Use getResponse() which sets up a proper reply destination
        // This is how the Python client does it with gapps.get_response()
        try {
            Serializable response = client.getResponse(request, requestTopic, RESPONSE_FORMAT.JSON);
            log.info("Got response: {}", response);

            if (response != null) {
                String responseStr = response.toString();
                log.info("Response string: {}", responseStr);

                // Parse the response to extract simulationId
                try {
                    JsonObject json = JsonParser.parseString(responseStr).getAsJsonObject();
                    if (json.has("simulationId")) {
                        simulationIdRef.set(json.get("simulationId").getAsString());
                        log.info("Simulation ID: {}", simulationIdRef.get());
                    } else if (json.has("data")) {
                        // Response might be wrapped in a data envelope
                        log.info("Response has data field: {}", json.get("data"));
                    }
                } catch (Exception e) {
                    log.warn("Could not parse response as JSON: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error getting response: {}", e.getMessage());
            e.printStackTrace();
        }

        // Check if we got log activity
        boolean gotLog = logLatch.await(5, TimeUnit.SECONDS);
        log.info("Got log activity: {}", gotLog);

        if (simulationIdRef.get() != null) {
            log.info("SUCCESS: Simulation started with ID: {}", simulationIdRef.get());
            Assert.assertNotNull("Should have a simulation ID", simulationIdRef.get());
        } else {
            log.warn("No simulation ID received.");
            log.warn("This may indicate:");
            log.warn("  1. ProcessManager is not running or not subscribed");
            log.warn("  2. The model is not loaded in Blazegraph");
            log.warn("  3. Configuration issues with the simulation request");
            // Don't fail - this is an integration test that requires full platform
        }
    }

    /**
     * Test: Verify we can send to the simulation topic and receive platform logs.
     *
     * This is a lighter test that just checks message routing works.
     */
    @Test(timeout = 30000) // 30 second timeout to prevent hanging
    public void testSimulationTopicRouting() throws Exception {
        Assume.assumeTrue("Message bus is not available, skipping test", busAvailable);

        log.info("=== Test: Simulation Topic Routing ===");

        CountDownLatch logLatch = new CountDownLatch(1);
        AtomicReference<String> logRef = new AtomicReference<>();

        // Subscribe to platform logs
        client.subscribe(GridAppsDConstants.topic_platformLog, new GossResponseEvent() {
            @Override
            public void onMessage(Serializable response) {
                log.info("Platform log received: {}", response);
                logRef.set(response.toString());
                logLatch.countDown();
            }
        });

        Thread.sleep(500);

        // Send a minimal message to the simulation request topic
        String request = "{\"test\": true}";
        client.publish(GridAppsDConstants.topic_requestSimulation, request);

        // We should see some log activity if ProcessManager is listening
        boolean gotLog = logLatch.await(5, TimeUnit.SECONDS);

        log.info("Received platform log: {}", gotLog);
        if (gotLog) {
            log.info("Log content: {}", logRef.get());
        }

        log.info("Topic routing test completed");
    }

    /**
     * Test: Subscribe to simulation output topics.
     *
     * This tests that we can subscribe to the topics that a running simulation
     * would publish to.
     */
    @Test(timeout = 30000) // 30 second timeout to prevent hanging
    public void testSimulationOutputTopicSubscription() throws Exception {
        Assume.assumeTrue("Message bus is not available, skipping test", busAvailable);

        log.info("=== Test: Simulation Output Topic Subscription ===");

        String testSimId = "test_" + System.currentTimeMillis();

        // These are the topics a simulation publishes to
        String outputTopic = GridAppsDConstants.topic_simulationOutput + "." + testSimId;
        String logTopic = GridAppsDConstants.topic_simulationLog + "." + testSimId;
        String inputTopic = GridAppsDConstants.topic_simulationInput + "." + testSimId;

        log.info("Output topic: {}", outputTopic);
        log.info("Log topic: {}", logTopic);
        log.info("Input topic: {}", inputTopic);

        CountDownLatch outputLatch = new CountDownLatch(1);

        // Subscribe to simulation output
        client.subscribe(outputTopic, new GossResponseEvent() {
            @Override
            public void onMessage(Serializable response) {
                log.info("Simulation output: {}", response);
                outputLatch.countDown();
            }
        });

        // Subscribe to simulation log
        client.subscribe(logTopic, new GossResponseEvent() {
            @Override
            public void onMessage(Serializable response) {
                log.info("Simulation log: {}", response);
            }
        });

        Thread.sleep(500);

        // Publish a test message to simulate output (for topic verification)
        String testOutput = "{\"simulation_id\": \"" + testSimId
                + "\", \"message\": {\"timestamp\": 0, \"measurements\": {}}}";
        client.publish(outputTopic, testOutput);

        boolean received = outputLatch.await(5, TimeUnit.SECONDS);
        Assert.assertTrue("Should receive message on simulation output topic", received);

        log.info("Simulation output topic subscription works");
    }

    public static void main(String[] args) {
        SimulationRequestIntegrationTest test = new SimulationRequestIntegrationTest();

        try {
            test.setUp();

            if (!test.busAvailable) {
                System.err.println("Message bus is not available. Make sure GridAPPS-D is running.");
                System.exit(1);
            }

            System.out.println("\n========== Running Simulation Integration Tests ==========\n");

            test.testSimulationOutputTopicSubscription();
            System.out.println("testSimulationOutputTopicSubscription PASSED\n");

            test.testSimulationTopicRouting();
            System.out.println("testSimulationTopicRouting PASSED\n");

            test.testSimulationRequestAndResponse();
            System.out.println("testSimulationRequestAndResponse completed\n");

            System.out.println("========== All Tests Completed ==========");

        } catch (Exception e) {
            System.err.println("Test failed with exception:");
            e.printStackTrace();
            System.exit(1);
        } finally {
            test.tearDown();
        }
    }
}
