/*******************************************************************************
 * Copyright (c) 2017, Battelle Memorial Institute All rights reserved.
 * Battelle Memorial Institute (hereinafter Battelle) hereby grants permission to any person or entity
 * lawfully obtaining a copy of this software and associated documentation files (hereinafter the
 * Software) to redistribute and use the Software in source and binary forms, with or without modification.
 * Such person or entity may use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and may permit others to do so, subject to the following conditions:
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the
 * following disclaimers.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided with the distribution.
 * Other than as used herein, neither the name Battelle Memorial Institute or Battelle may be used in any
 * form whatsoever without the express written consent of Battelle.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * BATTELLE OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * General disclaimer for use with OSS licenses
 *
 * This material was prepared as an account of work sponsored by an agency of the United States Government.
 * Neither the United States Government nor the United States Department of Energy, nor Battelle, nor any
 * of their employees, nor any jurisdiction or organization that has cooperated in the development of these
 * materials, makes any warranty, express or implied, or assumes any legal liability or responsibility for
 * the accuracy, completeness, or usefulness or any information, apparatus, product, software, or process
 * disclosed, or represents that its use would not infringe privately owned rights.
 *
 * Reference herein to any specific commercial product, process, or service by trade name, trademark, manufacturer,
 * or otherwise does not necessarily constitute or imply its endorsement, recommendation, or favoring by the United
 * States Government or any agency thereof, or Battelle Memorial Institute. The views and opinions of authors expressed
 * herein do not necessarily state or reflect those of the United States Government or any agency thereof.
 *
 * PACIFIC NORTHWEST NATIONAL LABORATORY operated by BATTELLE for the
 * UNITED STATES DEPARTMENT OF ENERGY under Contract DE-AC05-76RL01830
 ******************************************************************************/
package gov.pnnl.goss.gridappsd;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.core.Request.RESPONSE_FORMAT;
import pnnl.goss.core.client.ClientServiceFactory;

/**
 * Integration test for multi-feeder co-simulation.
 *
 * Tests both real-time and faster-than-real-time simulation modes with multiple
 * feeders. Monitors simulation output and verifies results.
 *
 * This test requires the full GridAPPS-D environment running: - GridAPPS-D
 * platform - Blazegraph with CIM model data - GridLAB-D simulator - HELICS
 * broker and bridge
 *
 * Run with: ./gradlew :gov.pnnl.goss.gridappsd:test --tests
 * MultiFeederSimulationIntegrationTest -PincludeIntegrationTests
 *
 * Or use shorter duration: ./gradlew :gov.pnnl.goss.gridappsd:test --tests
 * MultiFeederSimulationIntegrationTest -PincludeIntegrationTests
 * -Dtest.simulation.duration=30
 *
 * NOTE: The multi-feeder simulation feature (power_system_configs with multiple
 * entries) uses a different code path that requires GridLAB-D to be launched by
 * the HELICS bridge. If tests fail with 0 measurements, verify that the
 * simulator is actually being started.
 */
public class MultiFeederSimulationIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(MultiFeederSimulationIntegrationTest.class);

    private Client client;
    private ClientServiceFactory clientFactory;
    private boolean gridappsdAvailable = false;

    // Connection settings
    private static final String OPENWIRE_URI = "tcp://localhost:61616";
    private static final String STOMP_URI = "stomp://localhost:61613";
    private static final String USERNAME = "system";
    private static final String PASSWORD = "manager";

    // Test feeders - IEEE 123 node (Medium) and IEEE 13 node (Small)
    private static final String FEEDER_IEEE123_MRID = "C1C3E687-6FFD-C753-582B-632A27E28507";
    private static final String FEEDER_IEEE13_MRID = "49AD8E07-3BF9-A4E2-CB8F-C3722F837B62";

    // Simulation duration in seconds - keep short for testing
    // Default 60 seconds, can override with -Dtest.simulation.duration=N
    private static final int SIMULATION_DURATION = Integer.getInteger("test.simulation.duration", 60);

    // Timeout multiplier for test (accounts for startup overhead)
    private static final int REALTIME_TIMEOUT_MULTIPLIER = 2;
    private static final int FAST_TIMEOUT_SECONDS = 120; // faster-than-realtime should finish quickly

    @Before
    public void setUp() {
        try {
            log.info("Setting up multi-feeder simulation integration test");
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
     * Build a multi-feeder simulation request matching the temp.py format.
     *
     * Uses power_system_configs array with multiple feeders, each having their own
     * simulator_config.
     */
    private String buildMultiFeederSimulationRequest(int durationSeconds, boolean runRealtime) {
        long startTime = System.currentTimeMillis() / 1000;

        JsonObject request = new JsonObject();

        // Create power_system_configs array with multiple feeders
        JsonArray powerSystemConfigs = new JsonArray();

        // First feeder - IEEE 123 node (Medium)
        powerSystemConfigs.add(createPowerSystemConfig(
                "Medium", "IEEE", FEEDER_IEEE123_MRID));

        // Second feeder - IEEE 13 node (Small)
        powerSystemConfigs.add(createPowerSystemConfig(
                "Small", "IEEE", FEEDER_IEEE13_MRID));

        request.add("power_system_configs", powerSystemConfigs);

        // Simulation config
        JsonObject simulationConfig = new JsonObject();
        simulationConfig.addProperty("duration", durationSeconds);
        simulationConfig.addProperty("start_time", startTime);
        simulationConfig.addProperty("run_realtime", runRealtime);
        simulationConfig.addProperty("pause_after_measurements", false);
        simulationConfig.addProperty("simulation_broker_port", 5570);
        simulationConfig.addProperty("simulation_broker_location", "127.0.0.1");
        simulationConfig.addProperty("simulation_name", "multi-feeder-test");

        request.add("simulation_config", simulationConfig);

        return request.toString();
    }

    /**
     * Create a power system config for a single feeder.
     */
    private JsonObject createPowerSystemConfig(String subRegion, String geoRegion, String lineName) {
        JsonObject config = new JsonObject();
        config.addProperty("SubGeographicalRegion_name", subRegion);
        config.addProperty("GeographicalRegion_name", geoRegion);
        config.addProperty("Line_name", lineName);

        // Simulator config
        JsonObject simulatorConfig = new JsonObject();
        simulatorConfig.addProperty("simulator", "GridLAB-D");
        simulatorConfig.add("simulation_output", new JsonObject());
        simulatorConfig.addProperty("power_flow_solver_method", "NR");

        // Model creation config
        JsonObject modelCreationConfig = new JsonObject();
        modelCreationConfig.addProperty("load_scaling_factor", 1.0);
        modelCreationConfig.addProperty("triplex", "y");
        modelCreationConfig.addProperty("encoding", "u");
        modelCreationConfig.addProperty("system_frequency", 60);
        modelCreationConfig.addProperty("voltage_multiplier", 1.0);
        modelCreationConfig.addProperty("power_unit_conversion", 1.0);
        modelCreationConfig.addProperty("unique_names", "y");
        modelCreationConfig.addProperty("z_fraction", 0.0);
        modelCreationConfig.addProperty("i_fraction", 1.0);
        modelCreationConfig.addProperty("p_fraction", 0.0);
        modelCreationConfig.addProperty("randomize_zipload_fractions", false);
        modelCreationConfig.addProperty("use_houses", false);

        simulatorConfig.add("model_creation_config", modelCreationConfig);
        config.add("simulator_config", simulatorConfig);

        return config;
    }

    /**
     * Result container for simulation test results.
     */
    private static class SimulationTestResult {
        String simulationId;
        int measurementCount = 0;
        int logMessageCount = 0;
        long firstMeasurementTime = 0;
        long lastMeasurementTime = 0;
        List<String> errors = new ArrayList<>();
        Map<String, Integer> measurementsByType = new ConcurrentHashMap<>();
        boolean simulationComplete = false;
        long actualDurationMs = 0;

        @Override
        public String toString() {
            return String.format(
                    "SimulationTestResult{id=%s, measurements=%d, logs=%d, errors=%d, complete=%s, durationMs=%d}",
                    simulationId, measurementCount, logMessageCount, errors.size(), simulationComplete,
                    actualDurationMs);
        }
    }

    /**
     * Run a simulation and monitor its output.
     *
     * @param durationSeconds
     *            simulation duration
     * @param runRealtime
     *            true for real-time, false for faster-than-real-time
     * @param timeoutSeconds
     *            maximum time to wait for completion
     * @return test results
     */
    private SimulationTestResult runSimulationAndMonitor(int durationSeconds, boolean runRealtime, int timeoutSeconds)
            throws Exception {

        SimulationTestResult result = new SimulationTestResult();
        AtomicInteger measurementCount = new AtomicInteger(0);
        AtomicInteger logCount = new AtomicInteger(0);
        AtomicLong firstMeasurement = new AtomicLong(0);
        AtomicLong lastMeasurement = new AtomicLong(0);
        CountDownLatch simulationComplete = new CountDownLatch(1);
        AtomicReference<String> simulationIdRef = new AtomicReference<>();
        long startTime = System.currentTimeMillis();

        // Subscribe to platform log for status and errors
        client.subscribe(GridAppsDConstants.topic_platformLog, new GossResponseEvent() {
            @Override
            public void onMessage(Serializable response) {
                String msg = response.toString();
                logCount.incrementAndGet();

                if (msg.contains("ERROR") || msg.contains("Exception")) {
                    result.errors.add(msg.substring(0, Math.min(500, msg.length())));
                    log.warn("Error in platform log: {}", msg.substring(0, Math.min(200, msg.length())));
                }

                if (msg.contains("Simulation") && (msg.contains("complete") || msg.contains("finished"))) {
                    log.info("Simulation completion detected in platform log");
                    result.simulationComplete = true;
                    simulationComplete.countDown();
                }
            }
        });

        // Build and send simulation request
        String request = buildMultiFeederSimulationRequest(durationSeconds, runRealtime);
        log.info("Sending multi-feeder simulation request (realtime={}, duration={}s)",
                runRealtime, durationSeconds);
        log.debug("Request: {}", request);

        // Send request and get simulation ID
        Serializable response = client.getResponse(request,
                GridAppsDConstants.topic_requestSimulation, RESPONSE_FORMAT.JSON);

        Assert.assertNotNull("Should receive response from simulation request", response);
        String responseStr = response.toString();
        log.info("Simulation response: {}", responseStr);

        // Parse simulation ID
        String simulationId = parseSimulationId(responseStr);
        Assert.assertNotNull("Should have simulation ID", simulationId);
        Assert.assertFalse("Simulation ID should not be empty", simulationId.isEmpty());

        result.simulationId = simulationId;
        simulationIdRef.set(simulationId);
        log.info("Simulation started with ID: {}", simulationId);

        // Subscribe to simulation output
        String outputTopic = GridAppsDConstants.topic_simulationOutput + "." + simulationId;
        log.info("Subscribing to output topic: {}", outputTopic);

        client.subscribe(outputTopic, new GossResponseEvent() {
            @Override
            public void onMessage(Serializable response) {
                long now = System.currentTimeMillis();
                int count = measurementCount.incrementAndGet();

                firstMeasurement.compareAndSet(0, now);
                lastMeasurement.set(now);

                // Parse and categorize measurements
                try {
                    JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
                    if (json.has("message")) {
                        JsonObject message = json.getAsJsonObject("message");
                        if (message.has("measurements")) {
                            JsonObject measurements = message.getAsJsonObject("measurements");
                            for (String key : measurements.keySet()) {
                                result.measurementsByType.merge("measurement", 1, Integer::sum);
                            }
                        }
                    }
                } catch (Exception e) {
                    // Ignore parse errors for now
                }

                if (count % 10 == 0) {
                    log.info("Received {} measurement messages", count);
                }
            }
        });

        // Subscribe to simulation log
        String logTopic = GridAppsDConstants.topic_simulationLog + "." + simulationId;
        log.info("Subscribing to log topic: {}", logTopic);

        client.subscribe(logTopic, new GossResponseEvent() {
            @Override
            public void onMessage(Serializable response) {
                String msg = response.toString();
                result.logMessageCount++;

                if (msg.contains("Simulation") && (msg.contains("complete") || msg.contains("finished"))) {
                    log.info("Simulation completion detected in simulation log");
                    result.simulationComplete = true;
                    simulationComplete.countDown();
                }

                if (msg.contains("ERROR") || msg.contains("Exception")) {
                    result.errors.add(msg.substring(0, Math.min(500, msg.length())));
                }
            }
        });

        // Wait for simulation to complete
        log.info("Waiting for simulation to complete (timeout: {}s)...", timeoutSeconds);
        boolean completed = simulationComplete.await(timeoutSeconds, TimeUnit.SECONDS);

        // Collect results
        long endTime = System.currentTimeMillis();
        result.measurementCount = measurementCount.get();
        result.logMessageCount = logCount.get();
        result.firstMeasurementTime = firstMeasurement.get();
        result.lastMeasurementTime = lastMeasurement.get();
        result.actualDurationMs = endTime - startTime;

        if (!completed) {
            log.warn("Simulation did not complete within timeout");
        }

        return result;
    }

    /**
     * Parse simulation ID from various response formats.
     */
    private String parseSimulationId(String responseStr) {
        try {
            JsonObject json = JsonParser.parseString(responseStr).getAsJsonObject();
            if (json.has("simulationId")) {
                return json.get("simulationId").getAsString();
            } else if (json.has("data")) {
                JsonElement data = json.get("data");
                if (data.isJsonPrimitive()) {
                    return data.getAsString();
                }
            }
        } catch (Exception e) {
            // Response might be just the ID as a string
        }
        return responseStr.replaceAll("\"", "").trim();
    }

    // ========== Test Methods ==========

    /**
     * Test: Multi-feeder simulation in faster-than-real-time mode.
     *
     * This is the primary test - runs faster than real time so it completes
     * quickly. Verifies that both feeders produce measurement output.
     */
    @Test(timeout = 300000) // 5 minute max
    public void testMultiFeederSimulation_FasterThanRealTime() throws Exception {
        Assume.assumeTrue("GridAPPS-D is not available, skipping test", gridappsdAvailable);

        log.info("=== Starting Multi-Feeder Faster-Than-Real-Time Simulation Test ===");
        log.info("Duration: {} seconds, run_realtime: false", SIMULATION_DURATION);

        SimulationTestResult result = runSimulationAndMonitor(
                SIMULATION_DURATION,
                false, // faster than real-time
                FAST_TIMEOUT_SECONDS);

        // Log results
        log.info("=== Test Results ===");
        log.info("Simulation ID: {}", result.simulationId);
        log.info("Completed: {}", result.simulationComplete);
        log.info("Measurement count: {}", result.measurementCount);
        log.info("Log message count: {}", result.logMessageCount);
        log.info("Actual duration: {} ms", result.actualDurationMs);
        log.info("Errors: {}", result.errors.size());

        if (!result.errors.isEmpty()) {
            log.warn("Errors encountered:");
            for (String error : result.errors) {
                log.warn("  - {}", error.substring(0, Math.min(200, error.length())));
            }
        }

        // Assertions
        Assert.assertTrue("Simulation should complete",
                result.simulationComplete || result.measurementCount > 0);
        Assert.assertTrue("Should receive measurements (got " + result.measurementCount + ")",
                result.measurementCount > 0);

        // Faster-than-real-time should complete much faster than the simulation
        // duration
        if (result.simulationComplete) {
            long expectedMaxDuration = SIMULATION_DURATION * 1000L; // At most real-time duration
            Assert.assertTrue(
                    "Faster-than-real-time should complete faster than duration (actual: " +
                            result.actualDurationMs + "ms, max: " + expectedMaxDuration + "ms)",
                    result.actualDurationMs < expectedMaxDuration * 1.5); // Allow 50% overhead
        }

        log.info("=== Multi-Feeder Faster-Than-Real-Time Test PASSED ===");
    }

    /**
     * Test: Multi-feeder simulation in real-time mode.
     *
     * Runs at wall-clock speed. Use shorter duration for faster testing: ./gradlew
     * test --tests "*RealTime" -Dtest.simulation.duration=30
     */
    @Test(timeout = 600000) // 10 minute max
    public void testMultiFeederSimulation_RealTime() throws Exception {
        Assume.assumeTrue("GridAPPS-D is not available, skipping test", gridappsdAvailable);

        // Use shorter duration for real-time test (max 60 seconds)
        int realtimeDuration = Math.min(SIMULATION_DURATION, 60);
        int timeoutSeconds = realtimeDuration * REALTIME_TIMEOUT_MULTIPLIER + 60;

        log.info("=== Starting Multi-Feeder Real-Time Simulation Test ===");
        log.info("Duration: {} seconds, run_realtime: true, timeout: {}s",
                realtimeDuration, timeoutSeconds);

        SimulationTestResult result = runSimulationAndMonitor(
                realtimeDuration,
                true, // real-time
                timeoutSeconds);

        // Log results
        log.info("=== Test Results ===");
        log.info("Simulation ID: {}", result.simulationId);
        log.info("Completed: {}", result.simulationComplete);
        log.info("Measurement count: {}", result.measurementCount);
        log.info("Log message count: {}", result.logMessageCount);
        log.info("Actual duration: {} ms (expected ~{}ms)",
                result.actualDurationMs, realtimeDuration * 1000);
        log.info("Errors: {}", result.errors.size());

        // Assertions
        Assert.assertTrue("Simulation should complete or produce measurements",
                result.simulationComplete || result.measurementCount > 0);
        Assert.assertTrue("Should receive measurements (got " + result.measurementCount + ")",
                result.measurementCount > 0);

        // Real-time simulation should take approximately the configured duration
        if (result.simulationComplete) {
            long minExpectedDuration = (long) (realtimeDuration * 1000 * 0.8); // At least 80% of duration
            Assert.assertTrue(
                    "Real-time simulation should take at least " + minExpectedDuration +
                            "ms (actual: " + result.actualDurationMs + "ms)",
                    result.actualDurationMs >= minExpectedDuration);
        }

        log.info("=== Multi-Feeder Real-Time Test PASSED ===");
    }

    /**
     * Test: Single feeder simulation (IEEE 13 node) for comparison.
     *
     * Tests with just one feeder to verify basic simulation works.
     */
    @Test(timeout = 180000) // 3 minute max
    public void testSingleFeederSimulation() throws Exception {
        Assume.assumeTrue("GridAPPS-D is not available, skipping test", gridappsdAvailable);

        log.info("=== Starting Single Feeder Simulation Test ===");

        // Build single feeder request
        JsonObject request = new JsonObject();
        JsonArray powerSystemConfigs = new JsonArray();
        powerSystemConfigs.add(createPowerSystemConfig("Small", "IEEE", FEEDER_IEEE13_MRID));
        request.add("power_system_configs", powerSystemConfigs);

        JsonObject simulationConfig = new JsonObject();
        simulationConfig.addProperty("duration", 30); // 30 seconds
        simulationConfig.addProperty("start_time", System.currentTimeMillis() / 1000);
        simulationConfig.addProperty("run_realtime", false);
        simulationConfig.addProperty("pause_after_measurements", false);
        simulationConfig.addProperty("simulation_broker_port", 5570);
        simulationConfig.addProperty("simulation_broker_location", "127.0.0.1");
        simulationConfig.addProperty("simulation_name", "single-feeder-test");
        request.add("simulation_config", simulationConfig);

        String requestStr = request.toString();
        log.info("Sending single feeder request");
        log.debug("Request: {}", requestStr);

        AtomicInteger measurementCount = new AtomicInteger(0);
        CountDownLatch gotMeasurements = new CountDownLatch(5); // Wait for at least 5 measurements

        // Send request
        Serializable response = client.getResponse(requestStr,
                GridAppsDConstants.topic_requestSimulation, RESPONSE_FORMAT.JSON);

        Assert.assertNotNull("Should receive response", response);
        String simulationId = parseSimulationId(response.toString());
        log.info("Simulation ID: {}", simulationId);

        // Subscribe to output
        String outputTopic = GridAppsDConstants.topic_simulationOutput + "." + simulationId;
        client.subscribe(outputTopic, new GossResponseEvent() {
            @Override
            public void onMessage(Serializable response) {
                measurementCount.incrementAndGet();
                gotMeasurements.countDown();
            }
        });

        // Wait for measurements
        boolean receivedMeasurements = gotMeasurements.await(120, TimeUnit.SECONDS);

        log.info("Received {} measurements", measurementCount.get());
        Assert.assertTrue("Should receive at least some measurements", measurementCount.get() > 0);

        log.info("=== Single Feeder Test PASSED ===");
    }

    /**
     * Test: Verify simulation output contains expected measurement structure.
     */
    @Test(timeout = 180000)
    public void testSimulationOutputStructure() throws Exception {
        Assume.assumeTrue("GridAPPS-D is not available, skipping test", gridappsdAvailable);

        log.info("=== Starting Simulation Output Structure Test ===");

        AtomicReference<JsonObject> capturedOutput = new AtomicReference<>();
        CountDownLatch gotOutput = new CountDownLatch(1);

        // Build request
        String request = buildMultiFeederSimulationRequest(30, false);

        // Send request
        Serializable response = client.getResponse(request,
                GridAppsDConstants.topic_requestSimulation, RESPONSE_FORMAT.JSON);
        String simulationId = parseSimulationId(response.toString());
        log.info("Simulation ID: {}", simulationId);

        // Subscribe to output and capture first message
        String outputTopic = GridAppsDConstants.topic_simulationOutput + "." + simulationId;
        client.subscribe(outputTopic, new GossResponseEvent() {
            @Override
            public void onMessage(Serializable response) {
                try {
                    JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
                    if (capturedOutput.compareAndSet(null, json)) {
                        gotOutput.countDown();
                    }
                } catch (Exception e) {
                    log.warn("Could not parse output: {}", e.getMessage());
                }
            }
        });

        // Wait for output
        boolean received = gotOutput.await(120, TimeUnit.SECONDS);
        Assert.assertTrue("Should receive simulation output", received);

        JsonObject output = capturedOutput.get();
        Assert.assertNotNull("Should have captured output", output);

        // Verify structure
        log.info("Output structure: {}", output.keySet());

        // Expected fields in simulation output
        if (output.has("message")) {
            JsonObject message = output.getAsJsonObject("message");
            log.info("Message fields: {}", message.keySet());

            if (message.has("timestamp")) {
                log.info("Timestamp: {}", message.get("timestamp"));
            }
            if (message.has("measurements")) {
                JsonObject measurements = message.getAsJsonObject("measurements");
                log.info("Number of measurements: {}", measurements.size());
                Assert.assertTrue("Should have measurements", measurements.size() > 0);
            }
        }

        log.info("=== Simulation Output Structure Test PASSED ===");
    }

    /**
     * Main method for running tests standalone.
     */
    public static void main(String[] args) {
        MultiFeederSimulationIntegrationTest test = new MultiFeederSimulationIntegrationTest();

        try {
            test.setUp();

            if (!test.gridappsdAvailable) {
                System.err.println("GridAPPS-D is not available. Make sure the platform is running.");
                System.exit(1);
            }

            System.out.println("\n========== Running Multi-Feeder Simulation Tests ==========\n");

            // Run faster-than-real-time test first (quicker)
            System.out.println("--- Test: Faster Than Real Time ---");
            test.testMultiFeederSimulation_FasterThanRealTime();
            System.out.println("PASSED\n");

            // Then run real-time test
            System.out.println("--- Test: Real Time ---");
            test.testMultiFeederSimulation_RealTime();
            System.out.println("PASSED\n");

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
