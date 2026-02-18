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

import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.core.client.ClientServiceFactory;

/**
 * Basic connectivity test to verify we can connect to, publish to, and
 * subscribe from the ActiveMQ message bus.
 *
 * Run with: ./gradlew :gov.pnnl.goss.gridappsd:test --tests
 * BasicBusConnectivityIntegrationTest -PincludeIntegrationTests
 */
public class BasicBusConnectivityIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(BasicBusConnectivityIntegrationTest.class);

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
            log.info("Setting up basic bus connectivity test");

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
     * Test 1: Can we connect to the bus?
     */
    @Test(timeout = 30000)
    public void testConnection() {
        Assume.assumeTrue("Message bus is not available, skipping test", busAvailable);

        log.info("=== Test: Connection ===");
        Assert.assertNotNull("Client should not be null", client);
        log.info("Connection test PASSED - client is connected");
    }

    /**
     * Test 2: Can we publish a message?
     */
    @Test(timeout = 30000)
    public void testPublish() {
        Assume.assumeTrue("Message bus is not available, skipping test", busAvailable);

        log.info("=== Test: Publish ===");

        String testTopic = "test.basic.publish";
        String testMessage = "Hello from test at " + System.currentTimeMillis();

        try {
            client.publish(testTopic, testMessage);
            log.info("Successfully published message to topic: {}", testTopic);
            log.info("Publish test PASSED");
        } catch (Exception e) {
            log.error("Failed to publish message", e);
            Assert.fail("Failed to publish message: " + e.getMessage());
        }
    }

    /**
     * Test 3: Can we subscribe and receive our own message?
     */
    @Test(timeout = 30000)
    public void testSubscribeAndReceive() throws Exception {
        Assume.assumeTrue("Message bus is not available, skipping test", busAvailable);

        log.info("=== Test: Subscribe and Receive ===");

        String testTopic = "test.basic.pubsub." + System.currentTimeMillis();
        String testMessage = "Test message " + System.currentTimeMillis();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> receivedMessage = new AtomicReference<>();

        // First subscribe
        log.info("Subscribing to topic: {}", testTopic);
        client.subscribe(testTopic, new GossResponseEvent() {
            @Override
            public void onMessage(Serializable response) {
                log.info("Received message: {}", response);
                receivedMessage.set(response.toString());
                latch.countDown();
            }
        });

        // Give subscription time to be established
        Thread.sleep(500);

        // Then publish
        log.info("Publishing message to topic: {}", testTopic);
        client.publish(testTopic, testMessage);

        // Wait for message
        boolean received = latch.await(10, TimeUnit.SECONDS);

        Assert.assertTrue("Should receive message within 10 seconds", received);
        Assert.assertNotNull("Received message should not be null", receivedMessage.get());
        log.info("Subscribe and receive test PASSED - got message: {}", receivedMessage.get());
    }

    /**
     * Test 4: Test STOMP vs OPENWIRE protocol connectivity
     */
    @Test(timeout = 30000)
    public void testBothProtocols() throws Exception {
        Assume.assumeTrue("Message bus is not available, skipping test", busAvailable);

        log.info("=== Test: Both Protocols ===");

        // Create an OPENWIRE client
        Credentials credentials = new UsernamePasswordCredentials(USERNAME, PASSWORD);

        log.info("Creating OPENWIRE client");
        Client openwireClient = clientFactory.create(PROTOCOL.OPENWIRE, credentials);
        Assert.assertNotNull("OPENWIRE client should not be null", openwireClient);
        log.info("OPENWIRE client created successfully");

        // Test cross-protocol communication
        String testTopic = "test.cross.protocol." + System.currentTimeMillis();
        String testMessage = "Cross protocol test " + System.currentTimeMillis();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> receivedMessage = new AtomicReference<>();

        // Subscribe with STOMP client
        log.info("STOMP client subscribing to: {}", testTopic);
        client.subscribe(testTopic, new GossResponseEvent() {
            @Override
            public void onMessage(Serializable response) {
                log.info("STOMP received: {}", response);
                receivedMessage.set(response.toString());
                latch.countDown();
            }
        });

        Thread.sleep(500);

        // Publish with OPENWIRE client
        log.info("OPENWIRE client publishing to: {}", testTopic);
        openwireClient.publish(testTopic, testMessage);

        boolean received = latch.await(10, TimeUnit.SECONDS);

        openwireClient.close();

        Assert.assertTrue("Should receive cross-protocol message within 10 seconds", received);
        log.info("Cross-protocol test PASSED");
    }

    public static void main(String[] args) {
        BasicBusConnectivityIntegrationTest test = new BasicBusConnectivityIntegrationTest();

        try {
            test.setUp();

            if (!test.busAvailable) {
                System.err.println("Message bus is not available. Make sure GridAPPS-D or ActiveMQ is running.");
                System.exit(1);
            }

            System.out.println("\n========== Running Basic Bus Connectivity Tests ==========\n");

            test.testConnection();
            System.out.println("✓ testConnection PASSED\n");

            test.testPublish();
            System.out.println("✓ testPublish PASSED\n");

            test.testSubscribeAndReceive();
            System.out.println("✓ testSubscribeAndReceive PASSED\n");

            test.testBothProtocols();
            System.out.println("✓ testBothProtocols PASSED\n");

            System.out.println("========== All Basic Tests Passed ==========");

        } catch (Exception e) {
            System.err.println("Test failed with exception:");
            e.printStackTrace();
            System.exit(1);
        } finally {
            test.tearDown();
        }
    }
}
