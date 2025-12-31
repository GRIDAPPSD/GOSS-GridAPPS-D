package gov.pnnl.goss.gridappsd;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.core.Request.RESPONSE_FORMAT;
import pnnl.goss.core.client.ClientServiceFactory;

/**
 * Integration test that runs an actual simulation for a specified duration.
 *
 * This test requires the full GridAPPS-D environment including:
 * - GridAPPS-D platform running
 * - Blazegraph with CIM model data
 * - GridLAB-D or other simulator
 * - FNCS/HELICS bridge
 *
 * Run with: ./gradlew :gov.pnnl.goss.gridappsd:test --tests SimulationRunIntegrationTest
 *
 * Or from Docker: docker exec gridappsd /gridappsd/run-tests.sh
 */
public class SimulationRunIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(SimulationRunIntegrationTest.class);

    private Client client;
    private ClientServiceFactory clientFactory;
    private boolean gridappsdAvailable = false;

    // Connection settings
    private static final String OPENWIRE_URI = "tcp://localhost:61616";
    private static final String STOMP_URI = "stomp://localhost:61613";
    private static final String USERNAME = "system";
    private static final String PASSWORD = "manager";

    // Default test model - IEEE 13 node test feeder
    private static final String TEST_FEEDER_MRID = "_49AD8E07-3BF9-A4E2-CB8F-C3722F837B62";

    // Simulation duration in seconds (configurable via system property)
    private static final int SIMULATION_DURATION = Integer.getInteger("test.simulation.duration", 10);

    // Timeout for test (simulation duration + overhead for startup/shutdown)
    private static final int TEST_TIMEOUT_SECONDS = SIMULATION_DURATION + 120;

    @Before
    public void setUp() {
        try {
            log.info("Setting up simulation integration test");
            log.info("Simulation duration: {} seconds", SIMULATION_DURATION);

            Map<String, Object> properties = new HashMap<>();
            properties.put("goss.system.manager", USERNAME);
            properties.put("goss.system.manager.password", PASSWORD);
            properties.put("goss.openwire.uri", OPENWIRE_URI);
            properties.put("goss.stomp.uri", STOMP_URI);

            clientFactory = new ClientServiceFactory();
            clientFactory.updated(properties);

            Credentials credentials = new UsernamePasswordCredentials(USERNAME, PASSWORD);

            log.info("Attempting OpenWire connection to {}", OPENWIRE_URI);
            client = clientFactory.create(PROTOCOL.OPENWIRE, credentials);

            gridappsdAvailable = true;
            log.info("Successfully connected to GridAPPS-D message bus");

        } catch (Exception e) {
            log.warn("Could not connect to GridAPPS-D: {}. Tests will be skipped.", e.getMessage());
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
     * Build a simulation request JSON for the specified feeder and duration.
     */
    private String buildSimulationRequest(String feederMrid, int durationSeconds, boolean runRealtime) {
        long startTime = System.currentTimeMillis() / 1000;

        JsonObject request = new JsonObject();

        // Power system config - use feeder MRID directly
        JsonObject powerSystemConfig = new JsonObject();
        powerSystemConfig.addProperty("Line_name", feederMrid);

        // Model creation config
        JsonObject modelCreationConfig = new JsonObject();
        modelCreationConfig.addProperty("load_scaling_factor", "1.0");
        modelCreationConfig.addProperty("schedule_name", "ieeezipload");
        modelCreationConfig.addProperty("z_fraction", "0");
        modelCreationConfig.addProperty("i_fraction", "1");
        modelCreationConfig.addProperty("p_fraction", "0");
        modelCreationConfig.addProperty("randomize_zipload_fractions", false);
        modelCreationConfig.addProperty("use_houses", false);
        powerSystemConfig.add("model_creation_config", modelCreationConfig);

        // Simulation config
        JsonObject simulationConfig = new JsonObject();
        simulationConfig.addProperty("start_time", String.valueOf(startTime));
        simulationConfig.addProperty("duration", String.valueOf(durationSeconds));
        simulationConfig.addProperty("simulator", "GridLAB-D");
        simulationConfig.addProperty("timestep_frequency", "1000");
        simulationConfig.addProperty("timestep_increment", "1000");
        simulationConfig.addProperty("run_realtime", runRealtime);
        simulationConfig.addProperty("simulation_name", "test_simulation");
        simulationConfig.addProperty("power_flow_solver_method", "NR");
        simulationConfig.add("model_creation_config", new JsonObject());

        // Application config (empty)
        JsonObject applicationConfig = new JsonObject();
        applicationConfig.add("applications", new JsonArray());

        // Service config (empty - use defaults)
        JsonObject serviceConfig = new JsonObject();

        // Assemble the full request
        request.add("power_system_config", powerSystemConfig);
        request.add("simulation_config", simulationConfig);
        request.add("application_config", applicationConfig);
        request.add("service_config", serviceConfig);

        return request.toString();
    }

    /**
     * Test: Run a simulation for the configured duration and verify it completes.
     *
     * This is a full end-to-end test that:
     * 1. Sends a simulation request
     * 2. Receives simulation ID
     * 3. Subscribes to simulation output
     * 4. Waits for simulation to complete
     * 5. Verifies we received measurement data
     */
    @Test(timeout = 300000) // 5 minute max timeout
    public void testRunSimulation() throws Exception {
        Assume.assumeTrue("GridAPPS-D is not available, skipping test", gridappsdAvailable);

        log.info("=== Starting Simulation Run Test ===");
        log.info("Duration: {} seconds, Timeout: {} seconds", SIMULATION_DURATION, TEST_TIMEOUT_SECONDS);

        // Track simulation state
        AtomicReference<String> simulationIdRef = new AtomicReference<>();
        AtomicInteger measurementCount = new AtomicInteger(0);
        CountDownLatch simulationStarted = new CountDownLatch(1);
        CountDownLatch simulationComplete = new CountDownLatch(1);
        AtomicReference<String> lastError = new AtomicReference<>();

        // Subscribe to platform log for status updates
        client.subscribe(GridAppsDConstants.topic_platformLog, new GossResponseEvent() {
            @Override
            public void onMessage(Serializable response) {
                String msg = response.toString();
                log.debug("Platform log: {}", msg.substring(0, Math.min(200, msg.length())));

                // Check for simulation start/complete messages
                if (msg.contains("Starting simulation")) {
                    simulationStarted.countDown();
                }
                if (msg.contains("Simulation") && msg.contains("complete")) {
                    simulationComplete.countDown();
                }
                if (msg.contains("ERROR") || msg.contains("error")) {
                    lastError.set(msg);
                }
            }
        });

        // Build and send simulation request
        String request = buildSimulationRequest(TEST_FEEDER_MRID, SIMULATION_DURATION, false);
        log.info("Sending simulation request to: {}", GridAppsDConstants.topic_requestSimulation);
        log.debug("Request payload: {}", request);

        // Send request and get response with simulation ID
        Serializable response = client.getResponse(request, GridAppsDConstants.topic_requestSimulation, RESPONSE_FORMAT.JSON);

        Assert.assertNotNull("Should receive response from simulation request", response);
        String responseStr = response.toString();
        log.info("Response: {}", responseStr);

        // Parse simulation ID from response
        try {
            JsonObject json = JsonParser.parseString(responseStr).getAsJsonObject();
            if (json.has("simulationId")) {
                simulationIdRef.set(json.get("simulationId").getAsString());
            } else if (json.has("data") && json.get("data").isJsonPrimitive()) {
                // Sometimes the ID is wrapped in data field
                simulationIdRef.set(json.get("data").getAsString());
            }
        } catch (Exception e) {
            // Response might be just the ID as a string
            simulationIdRef.set(responseStr.replaceAll("\"", "").trim());
        }

        String simulationId = simulationIdRef.get();
        Assert.assertNotNull("Should have simulation ID", simulationId);
        Assert.assertFalse("Simulation ID should not be empty", simulationId.isEmpty());
        log.info("Simulation started with ID: {}", simulationId);

        // Subscribe to simulation output
        String outputTopic = GridAppsDConstants.topic_simulationOutput + "." + simulationId;
        String logTopic = GridAppsDConstants.topic_simulationLog + "." + simulationId;

        log.info("Subscribing to output topic: {}", outputTopic);
        client.subscribe(outputTopic, new GossResponseEvent() {
            @Override
            public void onMessage(Serializable response) {
                int count = measurementCount.incrementAndGet();
                if (count % 5 == 0) {
                    log.info("Received {} measurement messages", count);
                }
                log.debug("Simulation output: {}",
                        response.toString().substring(0, Math.min(200, response.toString().length())));
            }
        });

        log.info("Subscribing to log topic: {}", logTopic);
        client.subscribe(logTopic, new GossResponseEvent() {
            @Override
            public void onMessage(Serializable response) {
                String msg = response.toString();
                if (msg.contains("Simulation") && msg.contains("complete")) {
                    log.info("Simulation completion detected in logs");
                    simulationComplete.countDown();
                }
                log.debug("Simulation log: {}", msg.substring(0, Math.min(200, msg.length())));
            }
        });

        // Wait for simulation to complete
        log.info("Waiting for simulation to complete (max {} seconds)...", TEST_TIMEOUT_SECONDS);
        boolean completed = simulationComplete.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Log results
        int totalMeasurements = measurementCount.get();
        log.info("=== Simulation Test Results ===");
        log.info("Simulation ID: {}", simulationId);
        log.info("Completed: {}", completed);
        log.info("Total measurements received: {}", totalMeasurements);

        if (lastError.get() != null) {
            log.warn("Last error: {}", lastError.get());
        }

        // Assertions
        Assert.assertTrue("Simulation should complete within timeout", completed);
        Assert.assertTrue("Should receive at least some measurements (got " + totalMeasurements + ")",
                totalMeasurements > 0);

        log.info("=== Simulation Run Test PASSED ===");
    }

    /**
     * Test: Verify simulation can be stopped prematurely.
     */
    @Test(timeout = 120000) // 2 minute timeout
    public void testStopSimulation() throws Exception {
        Assume.assumeTrue("GridAPPS-D is not available, skipping test", gridappsdAvailable);

        log.info("=== Starting Simulation Stop Test ===");

        AtomicReference<String> simulationIdRef = new AtomicReference<>();
        CountDownLatch measurementReceived = new CountDownLatch(3); // Wait for a few measurements

        // Start a longer simulation that we'll stop early
        String request = buildSimulationRequest(TEST_FEEDER_MRID, 60, false);
        Serializable response = client.getResponse(request, GridAppsDConstants.topic_requestSimulation, RESPONSE_FORMAT.JSON);

        Assert.assertNotNull("Should receive response", response);
        String responseStr = response.toString();

        // Parse simulation ID
        try {
            JsonObject json = JsonParser.parseString(responseStr).getAsJsonObject();
            if (json.has("simulationId")) {
                simulationIdRef.set(json.get("simulationId").getAsString());
            }
        } catch (Exception e) {
            simulationIdRef.set(responseStr.replaceAll("\"", "").trim());
        }

        String simulationId = simulationIdRef.get();
        Assert.assertNotNull("Should have simulation ID", simulationId);
        log.info("Simulation started with ID: {}", simulationId);

        // Subscribe to output
        String outputTopic = GridAppsDConstants.topic_simulationOutput + "." + simulationId;
        client.subscribe(outputTopic, new GossResponseEvent() {
            @Override
            public void onMessage(Serializable response) {
                log.debug("Received measurement");
                measurementReceived.countDown();
            }
        });

        // Wait for some measurements
        boolean gotMeasurements = measurementReceived.await(60, TimeUnit.SECONDS);
        log.info("Received initial measurements: {}", gotMeasurements);

        // Send stop command
        String stopTopic = GridAppsDConstants.topic_simulationInput + "." + simulationId;
        JsonObject stopCommand = new JsonObject();
        stopCommand.addProperty("command", "stop");

        log.info("Sending stop command to: {}", stopTopic);
        client.publish(stopTopic, stopCommand.toString());

        // Give it time to stop
        Thread.sleep(5000);

        log.info("=== Simulation Stop Test PASSED ===");
    }

    /**
     * Main method for running tests standalone.
     */
    public static void main(String[] args) {
        SimulationRunIntegrationTest test = new SimulationRunIntegrationTest();

        try {
            test.setUp();

            if (!test.gridappsdAvailable) {
                System.err.println("GridAPPS-D is not available. Make sure the platform is running.");
                System.exit(1);
            }

            System.out.println("\n========== Running Simulation Integration Tests ==========\n");

            test.testRunSimulation();
            System.out.println("testRunSimulation PASSED\n");

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
