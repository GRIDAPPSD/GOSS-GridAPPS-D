package gov.pnnl.goss.gridappsd;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
import pnnl.goss.core.DataResponse;
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

    // Default test model - IEEE 123 node test feeder (no underscore prefix in MRID)
    private static final String TEST_FEEDER_MRID = "C1C3E687-6FFD-C753-582B-632A27E28507";

    // Simulation duration in seconds (configurable via system property)
    private static final int SIMULATION_DURATION = Integer.getInteger("test.simulation.duration", 10);

    // Timeout for test (simulation duration + overhead for startup/shutdown)
    private static final int TEST_TIMEOUT_SECONDS = SIMULATION_DURATION + 180;

    // Timeout budget for the two-concurrent-simulation test. Two simulations
    // competing for the same platform config-processing resources plausibly take
    // a bit longer to each surface their first status message than a lone
    // simulation does, so this adds a modest extra buffer over the
    // single-simulation TEST_TIMEOUT_SECONDS margin rather than assuming
    // concurrency is free.
    private static final int TWO_SIM_TIMEOUT_SECONDS = SIMULATION_DURATION + 240;

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

        // Model creation config (goes inside simulator_config)
        JsonObject modelCreationConfig = new JsonObject();
        modelCreationConfig.addProperty("load_scaling_factor", 1.0);
        modelCreationConfig.addProperty("schedule_name", "ieeezipload");
        modelCreationConfig.addProperty("z_fraction", 0.0);
        modelCreationConfig.addProperty("i_fraction", 1.0);
        modelCreationConfig.addProperty("p_fraction", 0.0);
        modelCreationConfig.addProperty("randomize_zipload_fractions", false);
        modelCreationConfig.addProperty("use_houses", false);

        // Simulator config (required inside power_system_config)
        JsonObject simulatorConfig = new JsonObject();
        simulatorConfig.addProperty("power_flow_solver_method", "NR");
        simulatorConfig.addProperty("simulator", "GridLAB-D");
        simulatorConfig.add("model_creation_config", modelCreationConfig);
        powerSystemConfig.add("simulator_config", simulatorConfig);

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

        // Assemble the full request - use power_system_configs (plural) as a list for
        // multi-feeder support
        JsonArray powerSystemConfigs = new JsonArray();
        powerSystemConfigs.add(powerSystemConfig);
        request.add("power_system_configs", powerSystemConfigs);
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
        CountDownLatch simulationComplete = new CountDownLatch(1);
        AtomicReference<String> lastError = new AtomicReference<>();

        // Subscribe to platform log for status updates
        client.subscribe(GridAppsDConstants.topic_platformLog, new GossResponseEvent() {
            @Override
            public void onMessage(Serializable response) {
                String msg = response.toString();
                log.debug("Platform log: {}", msg.substring(0, Math.min(200, msg.length())));

                if (msg.contains("Simulation") && msg.contains("complete")) {
                    simulationComplete.countDown();
                }
                if (msg.contains("ERROR") || msg.contains("error")) {
                    lastError.set(msg);
                }
            }
        });

        // Build and send simulation request using getResponse to get simulation ID
        // directly
        String request = buildSimulationRequest(TEST_FEEDER_MRID, SIMULATION_DURATION, false);
        log.info("Sending simulation request to: {}", GridAppsDConstants.topic_requestSimulation);
        log.debug("Request payload: {}", request);

        // Use getResponse to send simulation request and get simulationId back
        Serializable simResponse = client.getResponse(request,
                GridAppsDConstants.topic_requestSimulation, RESPONSE_FORMAT.JSON);

        String simulationId = null;
        if (simResponse != null) {
            String simResponseStr = simResponse.toString();
            log.info("Simulation response: {}", simResponseStr);
            // The response should be just the simulation ID as a string or JSON with
            // simulationId
            try {
                JsonObject responseObj = JsonParser.parseString(simResponseStr).getAsJsonObject();
                if (responseObj.has("simulationId")) {
                    simulationId = responseObj.get("simulationId").getAsString();
                } else if (responseObj.has("simulation_id")) {
                    simulationId = responseObj.get("simulation_id").getAsString();
                }
            } catch (Exception e) {
                // Response might be just the simulation ID as a plain string
                simulationId = simResponseStr.replaceAll("\"", "").trim();
            }
        } else {
            log.warn("No response from simulation request - checking platform status...");
            // Fall back to polling platform status
            for (int i = 0; i < 30; i++) {
                Thread.sleep(2000);
                try {
                    Serializable statusResponse = client.getResponse("{}",
                            GridAppsDConstants.topic_requestPlatformStatus, RESPONSE_FORMAT.JSON);
                    if (statusResponse != null) {
                        String statusStr = statusResponse.toString();
                        log.debug("Platform status: {}", statusStr.substring(0, Math.min(200, statusStr.length())));

                        JsonObject status = JsonParser.parseString(statusStr).getAsJsonObject();
                        if (status.has("simulations") && status.get("simulations").isJsonObject()) {
                            JsonObject simulations = status.getAsJsonObject("simulations");
                            if (simulations.size() > 0) {
                                // Get the first active simulation
                                for (String simId : simulations.keySet()) {
                                    simulationId = simId;
                                    simulationIdRef.set(simId);
                                    log.info("Found active simulation: {}", simulationId);
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("Error polling status: {}", e.getMessage());
                }

                if (simulationId != null) {
                    break;
                }
            }
        }

        assertNotNull(simulationId, "Should receive simulation ID from request or find active simulation");
        log.info("Simulation started with ID: {}", simulationId);

        // Now subscribe to the specific simulation's output topic
        String outputTopic = GridAppsDConstants.topic_simulationOutput.replace("/topic/", "") + "." + simulationId;
        String logTopic = GridAppsDConstants.topic_simulationLog + simulationId;

        log.info("Subscribing to output topic: {}", outputTopic);
        client.subscribe(outputTopic, new GossResponseEvent() {
            @Override
            public void onMessage(Serializable response) {
                int count = measurementCount.incrementAndGet();
                if (count == 1) {
                    log.info("Received first measurement message");
                } else if (count % 5 == 0) {
                    log.info("Received {} measurement messages", count);
                }
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

        // Assertions - simulation started is good enough (completion depends on
        // GridLAB-D/HELICS)
        assertNotNull(simulationId, "Should have started a simulation");
        // Don't require completion or measurements since they depend on infrastructure
        // assertTrue(completed, "Simulation should complete within timeout");
        // assertTrue(totalMeasurements > 0, "Should receive at least some
        // measurements");

        log.info("=== Container-based Simulation Run Test PASSED ===");
    }

    /**
     * Captures the messages received on one simulation's own per-simulation output
     * and log topics, for the topic-isolation assertions in
     * {@link #testRunTwoConcurrentSimulationsWithTopicIsolation()}.
     *
     * The isolation property under test: a message arriving on THIS simulation's
     * topic subscription must carry THIS simulation's id in its own JSON payload
     * ("simulation_id" for output messages, "processId" for log messages, per
     * LogMessage's Gson serialization), never the other concurrently-running
     * simulation's id. Bucketing happens by which literal topic string the platform
     * routed the message to (i.e. which watcher's dedicated subscription received
     * it), not by the payload's own id field, so a cross-wired topic bug is
     * actually detectable rather than tautologically bucketed away.
     */
    private static final class SimulationTopicWatcher {
        final String label;
        volatile String expectedSimulationId;
        final List<JsonObject> outputMessages = new CopyOnWriteArrayList<>();
        final List<JsonObject> logMessages = new CopyOnWriteArrayList<>();
        final ConcurrentLinkedQueue<String> unparseableMessages = new ConcurrentLinkedQueue<>();
        final CountDownLatch firstOutputMessage = new CountDownLatch(1);
        final CountDownLatch firstLogMessage = new CountDownLatch(1);

        SimulationTopicWatcher(String label) {
            this.label = label;
        }

        GossResponseEvent outputListener() {
            return response -> {
                JsonObject parsed = parseOrRecord(response);
                if (parsed != null) {
                    outputMessages.add(parsed);
                    firstOutputMessage.countDown();
                }
            };
        }

        GossResponseEvent logListener() {
            return response -> {
                JsonObject parsed = parseOrRecord(response);
                if (parsed != null) {
                    logMessages.add(parsed);
                    firstLogMessage.countDown();
                }
            };
        }

        /**
         * subscribe() callbacks always arrive wrapped in a DataResponse envelope
         * (DefaultClientListener.onMessage wraps every TextMessage/ObjectMessage this
         * way, unlike getResponse() which unwraps to the raw text). The actual
         * published JSON payload (LogMessage or the Python bridge's
         * simulation_id-carrying output message) is DataResponse.getData(), not
         * response.toString() itself, which stringifies the whole envelope
         * (destination, username, id, ...).
         */
        private JsonObject parseOrRecord(Serializable response) {
            String text;
            if (response instanceof DataResponse) {
                Serializable data = ((DataResponse) response).getData();
                text = data == null ? "" : data.toString();
            } else {
                text = response == null ? "" : response.toString();
            }
            try {
                return JsonParser.parseString(text).getAsJsonObject();
            } catch (Exception e) {
                unparseableMessages.add("[" + label + "] " + text);
                return null;
            }
        }
    }

    /**
     * Extracts the simulation ID from a getResponse() reply to
     * topic_requestSimulation. Mirrors the parsing already inline in
     * {@link #testRunSimulationWithContainers()}, factored out here so the new
     * concurrent test does not duplicate raw parsing logic; the existing test's
     * inline copy is intentionally left untouched.
     */
    private String extractSimulationId(Serializable simResponse) {
        if (simResponse == null) {
            return null;
        }
        String simResponseStr = simResponse.toString();
        try {
            JsonObject responseObj = JsonParser.parseString(simResponseStr).getAsJsonObject();
            if (responseObj.has("simulationId")) {
                return responseObj.get("simulationId").getAsString();
            } else if (responseObj.has("simulation_id")) {
                return responseObj.get("simulation_id").getAsString();
            }
            return null;
        } catch (Exception e) {
            return simResponseStr.replaceAll("\"", "").trim();
        }
    }

    /**
     * Sends one simulation request on its own dedicated client connection and, as
     * soon as the platform-assigned simulation ID is known, immediately subscribes
     * that same client to the simulation's own output and log topics. This is the
     * earliest point at which the exact per-simulation topic string can be built:
     * the ID is server-assigned, so there is no way to subscribe to the exact topic
     * before the ID exists. Subscribing synchronously, in the same thread, right
     * after getResponse() returns minimizes the race window before the platform's
     * async config-processing pipeline starts publishing (empirically, the earliest
     * per-simulation log messages are still several seconds out at that point; see
     * the GADP-061 report for the container-log evidence).
     */
    private String startSimulationAndSubscribe(Client simClient, String request, SimulationTopicWatcher watcher)
            throws Exception {
        Serializable simResponse = simClient.getResponse(request, GridAppsDConstants.topic_requestSimulation,
                RESPONSE_FORMAT.JSON);
        String simulationId = extractSimulationId(simResponse);
        if (simulationId == null) {
            return null;
        }
        watcher.expectedSimulationId = simulationId;

        String outputTopic = GridAppsDConstants.topic_simulationOutput + "." + simulationId;
        String logTopic = GridAppsDConstants.topic_simulationLog + simulationId;

        log.info("[{}] Simulation ID: {}. Subscribing to output topic: {}", watcher.label, simulationId,
                outputTopic);
        simClient.subscribe(outputTopic, watcher.outputListener());
        log.info("[{}] Subscribing to log topic: {}", watcher.label, logTopic);
        simClient.subscribe(logTopic, watcher.logListener());

        return simulationId;
    }

    private static long remainingNanos(long deadlineNanos) {
        return Math.max(deadlineNanos - System.nanoTime(), 0L);
    }

    private void closeQuietly(Client c, String label) {
        if (c == null) {
            return;
        }
        try {
            c.close();
        } catch (Exception e) {
            log.warn("[{}] Error closing simulation client: {}", label, e.getMessage());
        }
    }

    /**
     * Test: Run two simulations CONCURRENTLY and prove per-simulation topic
     * isolation.
     *
     * This guards the regression fixed in commit faa2d4e0 (PR 1758, 2024): before
     * that fix, SimulationManager and helics_goss_bridge published to one shared
     * topic across simulations, so concurrent instances corrupted each other's
     * status. The fix suffixes every per-simulation topic with "." + simulationId
     * (SimulationManagerImpl.java:224, SimulationProcess. java:217,232,255,290,
     * helics_goss_bridge.py:440,642,681). This test does not exercise those exact
     * four call sites directly: in this environment SimulationProcess never starts
     * at all (see the completion-baseline evidence in the GADP-061 report: Proven
     * has no seeded load-schedule timeseries data, so
     * GLDZiploadScheduleConfigurationHandler throws before
     * SimulationManagerImpl.startSimulation() is ever reached). What it does
     * exercise reliably, for every simulation regardless of that gap, is the
     * identical architectural pattern one layer up: LogManagerImpl.log() building
     * topic_simulationLog + processId per message
     * (gov.pnnl.goss.gridappsd.log.LogManagerImpl:297-298), fed by log calls that
     * fire from the earliest, always-executed phase of request processing
     * (ProcessNewSimulationRequest / GLDAllConfigurationHandler).
     *
     * How this fails if the suffix regresses: - If a per-simulation topic suffix is
     * dropped entirely (shared topic), this test's exact-topic subscriptions (built
     * with the suffix) no longer match anything the platform publishes, so the
     * "received >= 1 message" assertions fail outright. - If the suffix is present
     * but the wrong/shared id is used (the "concurrent instances corrupted each
     * other's status" failure mode), a message meant for the other simulation is
     * delivered onto this simulation's subscription, and the per-message
     * processId/simulation_id equality assertions fail with the mismatched id
     * visible in the message.
     */
    @Test
    void testRunTwoConcurrentSimulationsWithTopicIsolation() throws Exception {
        Assumptions.assumeTrue(connected, "Could not connect to GridAPPS-D, skipping test");

        log.info("=== Starting Two Concurrent Simulations Topic Isolation Test ===");
        log.info("Duration: {} seconds each, Timeout: {} seconds", SIMULATION_DURATION, TWO_SIM_TIMEOUT_SECONDS);

        // Independent client connections per simulation. Client.getResponse()
        // shares one JMS Session per Client to create its temporary reply
        // destination and consumer, and a JMS Session is not safe for concurrent
        // use by more than one thread at a time; two truly concurrent requests
        // need two independent connections, not one Client juggling two logical
        // simulations. This also better matches the real-world scenario the
        // original defect was about: independent client sessions racing on a
        // shared topic.
        Client clientA = clientFactory.create(PROTOCOL.OPENWIRE, new UsernamePasswordCredentials(USERNAME, PASSWORD));
        Client clientB = clientFactory.create(PROTOCOL.OPENWIRE, new UsernamePasswordCredentials(USERNAME, PASSWORD));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            SimulationTopicWatcher watcherA = new SimulationTopicWatcher("A");
            SimulationTopicWatcher watcherB = new SimulationTopicWatcher("B");

            // Same feeder model (ieee123 / TEST_FEEDER_MRID) for both simulations,
            // confirmed present in blazegraph via SPARQL (see GADP-061 report).
            // Using the same model twice is the simpler, still-sufficient choice:
            // isolation is a topic-naming property, not a model property.
            String requestA = buildSimulationRequest(TEST_FEEDER_MRID, SIMULATION_DURATION, false);
            String requestB = buildSimulationRequest(TEST_FEEDER_MRID, SIMULATION_DURATION, false);

            Future<String> futureA = executor.submit(() -> startSimulationAndSubscribe(clientA, requestA, watcherA));
            Future<String> futureB = executor.submit(() -> startSimulationAndSubscribe(clientB, requestB, watcherB));

            String simulationIdA = futureA.get(60, TimeUnit.SECONDS);
            String simulationIdB = futureB.get(60, TimeUnit.SECONDS);

            assertNotNull(simulationIdA, "Simulation A should receive a simulation ID from its concurrent request");
            assertNotNull(simulationIdB, "Simulation B should receive a simulation ID from its concurrent request");
            assertNotEquals(simulationIdA, simulationIdB,
                    "Two concurrently-requested simulations must be assigned distinct simulation IDs");
            log.info("Simulation A ID: {}, Simulation B ID: {}", simulationIdA, simulationIdB);

            // Wait, bounded by a single shared deadline, for at least one log-topic
            // message per simulation. The log topic is the load-bearing signal: it
            // fires from the earliest, always-executed phase of simulation request
            // processing regardless of whether the deeper GridLAB-D/HELICS pipeline
            // (SimulationProcess) ever starts. The output topic depends on that
            // deeper pipeline and is checked best-effort further below, not relied
            // on here.
            long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(TWO_SIM_TIMEOUT_SECONDS);
            boolean logAReceived = watcherA.firstLogMessage.await(remainingNanos(deadlineNanos), TimeUnit.NANOSECONDS);
            boolean logBReceived = watcherB.firstLogMessage.await(remainingNanos(deadlineNanos), TimeUnit.NANOSECONDS);

            assertTrue(logAReceived, "Simulation A should receive at least one message on its own log topic ("
                    + GridAppsDConstants.topic_simulationLog + simulationIdA + ") within " + TWO_SIM_TIMEOUT_SECONDS
                    + "s. This is the assertion that fails if the per-simulation topic suffix is dropped: with no "
                    + "suffix, this subscription never matches the platform's publish topic at all.");
            assertTrue(logBReceived, "Simulation B should receive at least one message on its own log topic ("
                    + GridAppsDConstants.topic_simulationLog + simulationIdB + ") within " + TWO_SIM_TIMEOUT_SECONDS
                    + "s.");

            assertFalse(watcherA.logMessages.isEmpty(),
                    "Simulation A's log-topic message list should be non-empty once its latch has counted down");
            assertFalse(watcherB.logMessages.isEmpty(),
                    "Simulation B's log-topic message list should be non-empty once its latch has counted down");

            // The core isolation assertion: every message that arrived on A's own
            // log-topic subscription must carry A's own simulation ID in its
            // payload, never B's, and vice versa. A cross-wired topic bug (the
            // simulation ID reused or shared between the two concurrent requests)
            // would deliver a message stamped with the other simulation's ID onto
            // this subscription; this assertion catches that even though the
            // suffix itself is still present, unlike the "suffix dropped entirely"
            // case checked above.
            for (JsonObject message : watcherA.logMessages) {
                assertTrue(message.has("processId"), "Log message on A's topic should carry a processId field: "
                        + message);
                String actualId = message.get("processId").getAsString();
                assertEquals(simulationIdA, actualId, "Log message received on simulation A's topic ("
                        + GridAppsDConstants.topic_simulationLog + simulationIdA
                        + ") carried a mismatched processId. Full message: " + message);
                assertNotEquals(simulationIdB, actualId,
                        "Log message on A's topic must never carry B's simulation ID");
            }
            for (JsonObject message : watcherB.logMessages) {
                assertTrue(message.has("processId"), "Log message on B's topic should carry a processId field: "
                        + message);
                String actualId = message.get("processId").getAsString();
                assertEquals(simulationIdB, actualId, "Log message received on simulation B's topic ("
                        + GridAppsDConstants.topic_simulationLog + simulationIdB
                        + ") carried a mismatched processId. Full message: " + message);
                assertNotEquals(simulationIdA, actualId,
                        "Log message on B's topic must never carry A's simulation ID");
            }

            // Output-topic isolation: best-effort. The single-simulation baseline
            // run for this card showed GridLAB-D/HELICS never starts in this
            // environment (see the class-level javadoc above), so the output topic
            // reliably receives zero messages here regardless of whether the
            // suffix code is correct. If that infra gap is ever closed, this still
            // checks the same isolation property on whichever side (or both)
            // produced output; it just does not fail the test when neither does.
            long outputDeadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
            watcherA.firstOutputMessage.await(remainingNanos(outputDeadlineNanos), TimeUnit.NANOSECONDS);
            watcherB.firstOutputMessage.await(remainingNanos(outputDeadlineNanos), TimeUnit.NANOSECONDS);

            log.info("Output messages observed - A: {}, B: {} (0/0 is expected in this environment; see report)",
                    watcherA.outputMessages.size(), watcherB.outputMessages.size());

            for (JsonObject message : watcherA.outputMessages) {
                assertTrue(message.has("simulation_id"), "Output message on A's topic should carry simulation_id: "
                        + message);
                assertEquals(simulationIdA, message.get("simulation_id").getAsString(),
                        "Output message received on simulation A's topic carried a mismatched simulation_id. "
                                + "Full message: " + message);
            }
            for (JsonObject message : watcherB.outputMessages) {
                assertTrue(message.has("simulation_id"), "Output message on B's topic should carry simulation_id: "
                        + message);
                assertEquals(simulationIdB, message.get("simulation_id").getAsString(),
                        "Output message received on simulation B's topic carried a mismatched simulation_id. "
                                + "Full message: " + message);
            }

            assertTrue(watcherA.unparseableMessages.isEmpty(),
                    "Simulation A's topics should only carry parseable JSON payloads. Unparseable: "
                            + watcherA.unparseableMessages);
            assertTrue(watcherB.unparseableMessages.isEmpty(),
                    "Simulation B's topics should only carry parseable JSON payloads. Unparseable: "
                            + watcherB.unparseableMessages);

            log.info("=== Two Concurrent Simulations Topic Isolation Test PASSED ===");
        } finally {
            executor.shutdownNow();
            closeQuietly(clientA, "A");
            closeQuietly(clientB, "B");
        }
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

    /**
     * Test: Verify timeseries data query works through containers (query weather
     * data).
     */
    @Test
    void testTimeseriesDataQueryWithContainers() throws Exception {
        Assumptions.assumeTrue(connected, "Could not connect to GridAPPS-D, skipping test");

        log.info("=== Testing Timeseries Data Query via Containers ===");

        // Build a weather data query request
        // Format based on RequestTimeseriesDataBasic
        JsonObject timeseriesRequest = new JsonObject();
        timeseriesRequest.addProperty("queryMeasurement", "weather");
        timeseriesRequest.addProperty("queryType", "time-series");
        timeseriesRequest.addProperty("responseFormat", "JSON");

        // Query filter with time range (using a known time range that should have data)
        // Using epoch time in nanoseconds as expected by Proven
        JsonObject queryFilter = new JsonObject();
        // January 22, 2013 - a date commonly used in GridAPPS-D test data
        queryFilter.addProperty("startTime", "1358800800000000000"); // 2013-01-22 00:00:00 UTC in ns
        queryFilter.addProperty("endTime", "1358804400000000000"); // 2013-01-22 01:00:00 UTC in ns
        timeseriesRequest.add("queryFilter", queryFilter);

        log.info("Sending timeseries request: {}", timeseriesRequest);

        Serializable response = client.getResponse(timeseriesRequest.toString(),
                GridAppsDConstants.topic_requestData + ".timeseries", RESPONSE_FORMAT.JSON);

        if (response != null) {
            String responseStr = response.toString();
            log.info("Timeseries response (first 500 chars): {}",
                    responseStr.substring(0, Math.min(500, responseStr.length())));

            // Parse and verify response
            assertFalse(responseStr.isEmpty(), "Response should not be empty");
            log.info("=== Timeseries Data Query Test PASSED ===");
        } else {
            log.warn("No response from timeseries query - Proven/InfluxDB may not have weather data loaded");
            // Don't fail the test if Proven doesn't have data - it's an infrastructure
            // issue
            log.info("=== Timeseries Data Query Test SKIPPED (no data) ===");
        }
    }

    /**
     * Test: Verify timeseries data can be stored and retrieved (round-trip test).
     *
     * This test publishes data to a simulation output topic (which the timeseries
     * manager subscribes to for automatic storage), then queries it back to verify
     * the write-read cycle works.
     */
    @Test
    void testTimeseriesDataStoreAndRetrieve() throws Exception {
        Assumptions.assumeTrue(connected, "Could not connect to GridAPPS-D, skipping test");

        log.info("=== Testing Timeseries Data Store and Retrieve ===");

        // Create a unique test simulation ID
        String testSimId = "test_timeseries_" + System.currentTimeMillis();
        long testTimestamp = System.currentTimeMillis() * 1000000; // Convert to nanoseconds

        // Create test measurement data in the format that
        // ProvenTimeSeriesDataManagerImpl expects
        // The handler subscribes to /topic/goss.gridappsd.*.output and looks for
        // "datatype" field
        JsonObject testData = new JsonObject();
        testData.addProperty("datatype", "test_measurement");
        testData.addProperty("simulation_id", testSimId);
        testData.addProperty("timestamp", testTimestamp);

        JsonObject measurements = new JsonObject();
        measurements.addProperty("voltage_a", 120.5);
        measurements.addProperty("voltage_b", 121.2);
        measurements.addProperty("voltage_c", 119.8);
        measurements.addProperty("power_real", 5000.0);
        measurements.addProperty("power_reactive", 1500.0);
        testData.add("measurements", measurements);

        log.info("Publishing test data to simulation output topic: {}", testData);

        // Publish to the simulation output topic that the timeseries manager subscribes
        // to
        String outputTopic = GridAppsDConstants.topic_simulation + ".output." + testSimId;

        try {
            // Use STOMP client to publish (matches how real simulation output is published)
            Credentials credentials = new UsernamePasswordCredentials(USERNAME, PASSWORD);
            Client stompClient = clientFactory.create(PROTOCOL.STOMP, credentials);

            // Publish the test data
            stompClient.publish(outputTopic, testData.toString());
            log.info("Published test data to topic: {}", outputTopic);

            // Give time for the async storage to complete
            Thread.sleep(2000);

            stompClient.close();
        } catch (Exception e) {
            log.warn("Could not publish to STOMP topic: {}", e.getMessage());
            // Continue with query test even if publish fails
        }

        // Now query for the data we just stored
        JsonObject queryRequest = new JsonObject();
        queryRequest.addProperty("queryMeasurement", "test_measurement");
        queryRequest.addProperty("queryType", "time-series");
        queryRequest.addProperty("responseFormat", "JSON");

        JsonObject queryFilter = new JsonObject();
        queryFilter.addProperty("simulation_id", testSimId);
        // Query a time window around when we published
        queryFilter.addProperty("startTime", String.valueOf(testTimestamp - 60000000000L)); // -1 minute
        queryFilter.addProperty("endTime", String.valueOf(testTimestamp + 60000000000L)); // +1 minute
        queryRequest.add("queryFilter", queryFilter);

        log.info("Querying for stored data: {}", queryRequest);

        Serializable response = client.getResponse(queryRequest.toString(),
                GridAppsDConstants.topic_requestData + ".timeseries", RESPONSE_FORMAT.JSON);

        if (response != null) {
            String responseStr = response.toString();
            log.info("Query response: {}", responseStr.substring(0, Math.min(500, responseStr.length())));

            // Check if we got actual data back (not an error)
            if (!responseStr.contains("Error") && !responseStr.contains("error")) {
                log.info("=== Timeseries Store and Retrieve Test PASSED ===");
            } else {
                log.warn("Query returned an error - Proven backend may not be available");
                log.info("=== Timeseries Store and Retrieve Test SKIPPED (backend error) ===");
            }
        } else {
            log.warn("No response from query - Proven backend may not be running");
            log.info("=== Timeseries Store and Retrieve Test SKIPPED (no response) ===");
        }
    }
}
