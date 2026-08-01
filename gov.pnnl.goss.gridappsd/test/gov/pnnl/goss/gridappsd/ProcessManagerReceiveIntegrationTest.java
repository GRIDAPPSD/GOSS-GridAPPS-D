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

import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.core.client.ClientServiceFactory;

/**
 * Test to verify that ProcessManagerImpl is receiving messages on the process
 * topics.
 *
 * This test sends messages to the same topics that the ProcessEvent handler
 * subscribes to and listens for any response or log output from GridAPPS-D.
 *
 * Run with: ./gradlew :gov.pnnl.goss.gridappsd:test --tests
 * ProcessManagerReceiveIntegrationTest -PincludeIntegrationTests
 */
public class ProcessManagerReceiveIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(ProcessManagerReceiveIntegrationTest.class);

    private Client client;
    private ClientServiceFactory clientFactory;
    private boolean busAvailable = false;

    private static final String OPENWIRE_URI = "tcp://localhost:61616";
    private static final String STOMP_URI = "stomp://localhost:61613";
    private static final String USERNAME = "system";
    private static final String PASSWORD = "manager";

    @Before
    public void setUp() {
        try {
            log.info("Setting up ProcessManager receive test");

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
            e.printStackTrace();
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
     * Test: Subscribe to platform log topic and publish to process topic. If
     * ProcessManagerImpl receives our message, we should see a log entry.
     */
    @Test(timeout = 30000)
    public void testProcessTopicReception() throws Exception {
        Assume.assumeTrue("Message bus is not available, skipping test", busAvailable);

        log.info("=== Test: Process Topic Reception ===");

        // Subscribe to platform log to see if ProcessEvent logs anything
        String logTopic = GridAppsDConstants.topic_platformLog;
        log.info("Subscribing to platform log topic: {}", logTopic);

        CountDownLatch logLatch = new CountDownLatch(1);
        AtomicReference<String> logMessage = new AtomicReference<>();

        client.subscribe(logTopic, new GossResponseEvent() {
            @Override
            public void onMessage(Serializable response) {
                log.info("Received log message: {}", response);
                logMessage.set(response.toString());
                logLatch.countDown();
            }
        });

        Thread.sleep(1000); // Wait for subscription to be established

        // Send a test message to process.request topic
        String requestTopic = GridAppsDConstants.topic_process_prefix + ".request.test";
        String testMessage = "{\"test\": \"message\", \"timestamp\": " + System.currentTimeMillis() + "}";

        log.info("Publishing to process topic: {}", requestTopic);
        log.info("Message: {}", testMessage);

        client.publish(requestTopic, testMessage);

        // Wait to see if we get any log messages
        boolean gotLog = logLatch.await(5, TimeUnit.SECONDS);

        log.info("Received log message: {}", gotLog);
        if (gotLog) {
            log.info("Log content: {}", logMessage.get());
        }

        // Don't assert - we're just checking if we see any activity
        log.info("Test completed. Check GridAPPS-D logs for any trace of the message.");
    }

    /**
     * Test: Send to the specific powergridmodel topic and listen for any response.
     */
    @Test(timeout = 30000)
    public void testPowergridModelTopicPublish() throws Exception {
        Assume.assumeTrue("Message bus is not available, skipping test", busAvailable);

        log.info("=== Test: Powergridmodel Topic Publish ===");

        // The exact topic the integration test sends to
        String requestTopic = GridAppsDConstants.topic_requestData + ".powergridmodel";
        log.info("Target topic: {}", requestTopic);

        // Subscribe to multiple potential response topics
        String[] responseTopics = {
                "goss.gridappsd.response.>",
                "goss.gridappsd.platform.log",
                GridAppsDConstants.topic_platformLog
        };

        CountDownLatch responseLatch = new CountDownLatch(1);
        AtomicReference<String> responseMessage = new AtomicReference<>();

        for (String topic : responseTopics) {
            log.info("Subscribing to: {}", topic);
            client.subscribe(topic, new GossResponseEvent() {
                @Override
                public void onMessage(Serializable response) {
                    log.info("Received on {}: {}", topic, response);
                    responseMessage.set(response.toString());
                    responseLatch.countDown();
                }
            });
        }

        Thread.sleep(1000);

        // Send a PowergridModelDataRequest
        String request = "{\"requestType\":\"QUERY_MODEL_NAMES\",\"resultFormat\":\"JSON\"}";
        log.info("Publishing request: {}", request);

        client.publish(requestTopic, request);

        // Wait for any response
        boolean gotResponse = responseLatch.await(10, TimeUnit.SECONDS);

        log.info("Received response: {}", gotResponse);
        if (gotResponse) {
            log.info("Response: {}", responseMessage.get());
        }

        log.info("Test completed.");
    }

    /**
     * Test: Manually verify that ProcessManager is subscribed to the wildcard topic
     * by sending to a topic that should match the .> wildcard.
     */
    @Test(timeout = 30000)
    public void testWildcardTopicMatch() throws Exception {
        Assume.assumeTrue("Message bus is not available, skipping test", busAvailable);

        log.info("=== Test: Wildcard Topic Match ===");

        // ProcessManagerImpl subscribes to: goss.gridappsd.process.>
        // Let's verify what topics would match
        String prefix = GridAppsDConstants.topic_process_prefix;
        log.info("ProcessManager subscription prefix: {}", prefix);

        String[] testTopics = {
                prefix + ".request.data.powergridmodel",
                prefix + ".request.simulation",
                prefix + ".request.test",
                prefix + ".test"
        };

        log.info("The following topics should all be received by ProcessManager:");
        for (String topic : testTopics) {
            log.info("  - {}", topic);
        }

        // Subscribe to one of these topics ourselves to verify the client can receive
        // on them
        CountDownLatch latch = new CountDownLatch(1);
        String testTopic = prefix + ".test." + System.currentTimeMillis();

        client.subscribe(testTopic, new GossResponseEvent() {
            @Override
            public void onMessage(Serializable response) {
                log.info("Received on test topic: {}", response);
                latch.countDown();
            }
        });

        Thread.sleep(500);

        client.publish(testTopic, "test wildcard");

        boolean received = latch.await(5, TimeUnit.SECONDS);
        Assert.assertTrue("Should receive message on process.> topic", received);

        log.info("Wildcard topic matching works for client. Now check GridAPPS-D logs.");
    }

    public static void main(String[] args) {
        ProcessManagerReceiveIntegrationTest test = new ProcessManagerReceiveIntegrationTest();

        try {
            test.setUp();

            if (!test.busAvailable) {
                System.err.println("Message bus is not available.");
                System.exit(1);
            }

            System.out.println("\n=== Running ProcessManager Receive Tests ===\n");

            test.testWildcardTopicMatch();
            System.out.println("✓ testWildcardTopicMatch completed\n");

            test.testProcessTopicReception();
            System.out.println("✓ testProcessTopicReception completed\n");

            test.testPowergridModelTopicPublish();
            System.out.println("✓ testPowergridModelTopicPublish completed\n");

            System.out.println("=== All Tests Completed ===");

        } catch (Exception e) {
            System.err.println("Test failed:");
            e.printStackTrace();
            System.exit(1);
        } finally {
            test.tearDown();
        }
    }
}
