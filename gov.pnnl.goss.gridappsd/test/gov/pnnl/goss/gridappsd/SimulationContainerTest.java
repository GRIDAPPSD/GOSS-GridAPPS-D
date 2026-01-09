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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Container-based integration test that runs a simulation using Testcontainers.
 *
 * This test uses GridAppsDTestEnvironment to manage Docker containers via the
 * gridappsd-docker/docker-compose.yml configuration.
 *
 * Run with: ./gradlew :gov.pnnl.goss.gridappsd:containerTest
 *
 * Or via Makefile: make test-container
 *
 * Prerequisites: - Docker running - gridappsd/gridappsd:local image built (make
 * docker) - MySQL dump available in gridappsd-docker/dumps/
 */
@Tag("container")
@Tag("simulation")
public class SimulationContainerTest {

    private static final Logger log = LoggerFactory.getLogger(SimulationContainerTest.class);

    // Shared test environment (singleton manages container lifecycle)
    private static final GridAppsDTestEnvironment env = GridAppsDTestEnvironment.getInstance();

    private Client client;
    private ClientServiceFactory clientFactory;
    private boolean connected = false;

    // Credentials matching docker-compose environment
    private static final String USERNAME = "system";
    private static final String PASSWORD = "manager";

    // Default test model - IEEE 13 node test feeder
    private static final String TEST_FEEDER_MRID = "_49AD8E07-3BF9-A4E2-CB8F-C3722F837B62";

    // Simulation duration in seconds (configurable via system property)
    private static final int SIMULATION_DURATION = Integer.getInteger("test.simulation.duration", 10);

    // Timeout for test (simulation duration + overhead for startup/shutdown)
    private static final int TEST_TIMEOUT_SECONDS = SIMULATION_DURATION + 180;

    @BeforeEach
    void setUp() {
        try {
            log.info("=== Setting up Container-based Simulation Test ===");

            // Start Docker environment if not already running
            log.info("Starting Docker environment...");
            env.start();

            // Get dynamic connection URI from Testcontainers
            String openwireUri = env.getOpenWireUri();
            String stompUri = env.getStompUri();

            log.info("GridAPPS-D OpenWire URI: {}", openwireUri);
            log.info("GridAPPS-D STOMP URI: {}", stompUri);

            // Configure client factory
            Map<String, Object> properties = new HashMap<>();
            properties.put("goss.system.manager", USERNAME);
            properties.put("goss.system.manager.password", PASSWORD);
            properties.put("goss.openwire.uri", openwireUri);
            properties.put("goss.stomp.uri", stompUri);

            clientFactory = new ClientServiceFactory();
            clientFactory.updated(properties);

            // Connect to GridAPPS-D
            Credentials credentials = new UsernamePasswordCredentials(USERNAME, PASSWORD);
            log.info("Connecting to GridAPPS-D...");
            client = clientFactory.create(PROTOCOL.OPENWIRE, credentials);

            connected = true;
            log.info("Successfully connected to GridAPPS-D via Testcontainers");

        } catch (Exception e) {
            System.err.println("Failed to set up test environment: " + e.getMessage());
            e.printStackTrace();
            connected = false;
        }
    }

    @AfterEach
    void tearDown() {
        if (client != null) {
            try {
                client.close();
                log.info("Client connection closed");
            } catch (Exception e) {
                log.warn("Error closing client: {}", e.getMessage());
            }
        }
        // Note: We don't stop the environment here to allow container reuse
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
        simulationConfig.addProperty("simulation_name", "container_test_simulation");
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
     * Test: Run a simulation for the configured duration using
     * Testcontainers-managed Docker environment.
     *
     * This is a full end-to-end test that: 1. Sends a simulation request 2.
     * Receives simulation ID 3. Subscribes to simulation output 4. Waits for
     * simulation to complete 5. Verifies we received measurement data
     */
    @Test
    void testRunSimulationWithContainers() throws Exception {
        Assumptions.assumeTrue(connected, "Could not connect to GridAPPS-D, skipping test");

        log.info("=== Starting Container-based Simulation Run Test ===");
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
        Serializable response = client.getResponse(request, GridAppsDConstants.topic_requestSimulation,
                RESPONSE_FORMAT.JSON);

        assertNotNull(response, "Should receive response from simulation request");
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
        assertNotNull(simulationId, "Should have simulation ID");
        assertFalse(simulationId.isEmpty(), "Simulation ID should not be empty");
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
        log.info("=== Container-based Simulation Test Results ===");
        log.info("Simulation ID: {}", simulationId);
        log.info("Completed: {}", completed);
        log.info("Total measurements received: {}", totalMeasurements);

        if (lastError.get() != null) {
            log.warn("Last error: {}", lastError.get());
        }

        // Assertions
        assertTrue(completed, "Simulation should complete within timeout");
        assertTrue(totalMeasurements > 0, "Should receive at least some measurements (got " + totalMeasurements + ")");

        log.info("=== Container-based Simulation Run Test PASSED ===");
    }

    /**
     * Test: Verify platform status query works through containers.
     */
    @Test
    void testPlatformStatusWithContainers() throws Exception {
        Assumptions.assumeTrue(connected, "Could not connect to GridAPPS-D, skipping test");

        log.info("=== Testing Platform Status via Containers ===");

        // Query platform status
        String statusRequest = "{}";
        Serializable response = client.getResponse(statusRequest,
                GridAppsDConstants.topic_requestPlatformStatus, RESPONSE_FORMAT.JSON);

        assertNotNull(response, "Should receive platform status response");
        String responseStr = response.toString();
        log.info("Platform status: {}", responseStr);

        // Verify we got a valid JSON response
        JsonObject status = JsonParser.parseString(responseStr).getAsJsonObject();
        assertTrue(status.has("applications") || status.has("services") || status.has("simulations"),
                "Status should contain applications, services, or simulations info");

        log.info("=== Platform Status Test PASSED ===");
    }

    /**
     * Test: Verify data query works through containers (list available feeders).
     */
    @Test
    void testDataQueryWithContainers() throws Exception {
        Assumptions.assumeTrue(connected, "Could not connect to GridAPPS-D, skipping test");

        log.info("=== Testing Data Query via Containers ===");

        // Query for available feeders
        JsonObject dataRequest = new JsonObject();
        dataRequest.addProperty("requestType", "QUERY_MODEL_NAMES");
        dataRequest.addProperty("resultFormat", "JSON");

        Serializable response = client.getResponse(dataRequest.toString(),
                GridAppsDConstants.topic_requestData, RESPONSE_FORMAT.JSON);

        assertNotNull(response, "Should receive data query response");
        String responseStr = response.toString();
        log.info("Available models: {}", responseStr.substring(0, Math.min(500, responseStr.length())));

        // Should have some data
        assertFalse(responseStr.isEmpty(), "Response should not be empty");

        log.info("=== Data Query Test PASSED ===");
    }
}
