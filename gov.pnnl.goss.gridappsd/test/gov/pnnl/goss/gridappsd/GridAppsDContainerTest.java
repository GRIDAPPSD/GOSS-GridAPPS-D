package gov.pnnl.goss.gridappsd;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ComposeContainer;

import pnnl.goss.core.Client;
import pnnl.goss.core.Client.DESTINATION_TYPE;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.core.client.ClientServiceFactory;

/**
 * Container-based integration tests for GridAPPS-D using Testcontainers.
 *
 * These tests verify: 1. GridAPPS-D starts and exposes ports 61613 (STOMP),
 * 61614 (WebSocket), 61616 (OpenWire) 2. Queue messaging - send and receive
 * with point-to-point semantics 3. Topic messaging - publish and subscribe with
 * broadcast semantics
 *
 * Prerequisites: - Build local gridappsd image: make docker - Docker daemon
 * running
 *
 * Run with: ./gradlew :gov.pnnl.goss.gridappsd:containerTest
 */
@Tag("container")
@Tag("messaging")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GridAppsDContainerTest {

    private static final Logger log = LoggerFactory.getLogger(GridAppsDContainerTest.class);

    private static final String USERNAME = "system";
    private static final String PASSWORD = "manager";

    // Docker compose container - starts the full GridAPPS-D stack
    private ComposeContainer environment;

    private Client client;
    private ClientServiceFactory clientFactory;
    private String openwireUri;
    private String stompUri;
    private String host;
    private int openwirePort;
    private int stompPort;
    private int wsPort;

    @BeforeAll
    void startContainers() {
        log.info("=== Starting GridAPPS-D containers ===");

        // Find docker-compose.yml - it's at the project root, not in the subproject
        // Try multiple paths to handle different working directory scenarios
        File composeFile = findComposeFile();
        log.info("Using docker-compose.yml at: {}", composeFile.getAbsolutePath());

        environment = new ComposeContainer(composeFile)
                .withEnv("GRIDAPPSD_TAG", ":develop")
                // Use local compose - this means ports are mapped as defined in
                // docker-compose.yml
                // and we use localhost:port directly (no dynamic port mapping)
                .withLocalCompose(true);

        try {
            environment.start();
            log.info("=== Docker Compose started, waiting for GridAPPS-D to be ready ===");

            // With localCompose=true, ports are mapped directly as defined in
            // docker-compose.yml
            // (no ambassador containers, so we use localhost with fixed ports)
            host = "localhost";
            openwirePort = 61616;
            stompPort = 61613;
            wsPort = 61614;

            // Wait for GridAPPS-D ActiveMQ broker to be ready
            // The docker-compose healthcheck uses start_period=60s and interval=30s,
            // so we need to be patient here
            waitForBroker(host, openwirePort, Duration.ofMinutes(5));

            openwireUri = "tcp://" + host + ":" + openwirePort;
            stompUri = "stomp://" + host + ":" + stompPort;

            log.info("=== Containers started successfully ===");
            log.info("GridAPPS-D OpenWire: {}", openwireUri);
            log.info("GridAPPS-D STOMP: stomp://{}:{}", host, stompPort);
            log.info("GridAPPS-D WebSocket: ws://{}:{}", host, wsPort);

        } catch (Exception e) {
            log.error("Failed to start containers", e);
            if (environment != null) {
                try {
                    environment.stop();
                } catch (Exception stopEx) {
                    log.warn("Error stopping containers after failure", stopEx);
                }
            }
            fail("Failed to start containers: " + e.getMessage());
        }
    }

    /**
     * Wait for the GridAPPS-D broker to be ready. This checks not just if the port
     * is open, but if ActiveMQ is actually accepting connections.
     */
    private void waitForBroker(String host, int port, Duration timeout) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        log.info("Waiting for ActiveMQ broker at {}:{} to be ready (timeout: {})", host, port, timeout);

        Exception lastException = null;
        while (System.currentTimeMillis() < deadline) {
            // First check if port is open
            if (!isPortOpen(host, port)) {
                log.info("Port {}:{} not yet open, waiting...", host, port);
                Thread.sleep(5000);
                continue;
            }

            // Port is open, try to actually connect to the broker
            try {
                String brokerUrl = "tcp://" + host + ":" + port;
                org.apache.activemq.ActiveMQConnectionFactory factory = new org.apache.activemq.ActiveMQConnectionFactory(
                        brokerUrl);
                factory.setUserName(USERNAME);
                factory.setPassword(PASSWORD);
                // Short connection timeout
                factory.setSendTimeout(5000);
                factory.setConnectResponseTimeout(5000);

                try (jakarta.jms.Connection conn = factory.createConnection()) {
                    conn.start();
                    log.info("Successfully connected to ActiveMQ broker at {}:{}", host, port);
                    return;
                }
            } catch (Exception e) {
                lastException = e;
                log.info("Broker not ready yet: {} - retrying in 5 seconds...", e.getMessage());
                Thread.sleep(5000);
            }
        }

        String msg = "Timeout waiting for ActiveMQ broker at " + host + ":" + port + " after " + timeout;
        if (lastException != null) {
            msg += ". Last error: " + lastException.getMessage();
        }
        throw new RuntimeException(msg);
    }

    @AfterAll
    void stopContainers() {
        if (environment != null) {
            log.info("=== Stopping GridAPPS-D containers ===");
            environment.stop();
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        // Configure client factory with dynamic URIs
        Map<String, Object> properties = new HashMap<>();
        properties.put("goss.system.manager", USERNAME);
        properties.put("goss.system.manager.password", PASSWORD);
        properties.put("goss.openwire.uri", openwireUri);
        properties.put("goss.stomp.uri", stompUri);

        clientFactory = new ClientServiceFactory();
        clientFactory.updated(properties);

        // Create GOSS client
        Credentials credentials = new UsernamePasswordCredentials(USERNAME, PASSWORD);
        client = clientFactory.create(PROTOCOL.OPENWIRE, credentials);

        log.info("GOSS client connected successfully");
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
    }

    /**
     * Test that all three GridAPPS-D messaging ports are open and accessible. -
     * 61613: STOMP protocol - 61614: WebSocket transport - 61616: OpenWire protocol
     * (primary JMS)
     */
    @Test
    void testGridAppsDPortsAreOpen() {
        log.info("=== Testing GridAPPS-D ports are open ===");

        // Test OpenWire port (61616)
        assertTrue(isPortOpen(host, openwirePort),
                "OpenWire port (61616) should be accessible at " + host + ":" + openwirePort);
        log.info("OpenWire port {} is open", openwirePort);

        // Test STOMP port (61613)
        assertTrue(isPortOpen(host, stompPort),
                "STOMP port (61613) should be accessible at " + host + ":" + stompPort);
        log.info("STOMP port {} is open", stompPort);

        // Test WebSocket port (61614)
        assertTrue(isPortOpen(host, wsPort),
                "WebSocket port (61614) should be accessible at " + host + ":" + wsPort);
        log.info("WebSocket port {} is open", wsPort);

        log.info("=== All GridAPPS-D ports verified ===");
    }

    /**
     * Test basic queue send and receive. Messages sent to a queue should be
     * received by a queue subscriber.
     */
    @Test
    void testQueueSendReceive() throws Exception {
        log.info("=== Testing Queue Send/Receive ===");

        String queueName = "test.queue." + System.currentTimeMillis();
        String testMessage = "Hello Queue " + System.currentTimeMillis();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> received = new AtomicReference<>();

        // Subscribe to queue
        client.subscribe(queueName, new GossResponseEvent() {
            @Override
            public void onMessage(Serializable response) {
                String msg = response.toString();
                log.info("Queue received: {}", msg);
                received.set(msg);
                latch.countDown();
            }
        }, DESTINATION_TYPE.QUEUE);

        // Small delay to ensure subscription is established
        Thread.sleep(500);

        // Send message to queue
        client.publish(queueName, testMessage, DESTINATION_TYPE.QUEUE);
        log.info("Sent to queue: {}", testMessage);

        // Wait for message
        boolean messageReceived = latch.await(10, TimeUnit.SECONDS);

        assertTrue(messageReceived, "Should receive message from queue within timeout");
        assertNotNull(received.get(), "Received message should not be null");
        assertTrue(received.get().contains(testMessage) || received.get().equals(testMessage),
                "Received message should contain sent message");

        log.info("=== Queue Send/Receive PASSED ===");
    }

    /**
     * Test basic topic send and receive. Messages published to a topic should be
     * received by topic subscribers.
     */
    @Test
    void testTopicSendReceive() throws Exception {
        log.info("=== Testing Topic Send/Receive ===");

        String topicName = "test.topic." + System.currentTimeMillis();
        String testMessage = "Hello Topic " + System.currentTimeMillis();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> received = new AtomicReference<>();

        // Subscribe to topic (default destination type is TOPIC)
        client.subscribe(topicName, new GossResponseEvent() {
            @Override
            public void onMessage(Serializable response) {
                String msg = response.toString();
                log.info("Topic received: {}", msg);
                received.set(msg);
                latch.countDown();
            }
        });

        // Small delay to ensure subscription is established
        Thread.sleep(500);

        // Publish message to topic (default destination type is TOPIC)
        client.publish(topicName, testMessage);
        log.info("Published to topic: {}", testMessage);

        // Wait for message
        boolean messageReceived = latch.await(10, TimeUnit.SECONDS);

        assertTrue(messageReceived, "Should receive message from topic within timeout");
        assertNotNull(received.get(), "Received message should not be null");
        assertTrue(received.get().contains(testMessage) || received.get().equals(testMessage),
                "Received message should contain sent message");

        log.info("=== Topic Send/Receive PASSED ===");
    }

    /**
     * Test queue semantics: messages are delivered to only ONE consumer. When
     * multiple consumers listen on a queue, each message goes to exactly one
     * consumer.
     */
    @Test
    void testQueueDeliveredToOneConsumer() throws Exception {
        log.info("=== Testing Queue delivers to ONE consumer ===");

        String queueName = "test.queue.semantics." + System.currentTimeMillis();

        AtomicInteger consumer1Count = new AtomicInteger(0);
        AtomicInteger consumer2Count = new AtomicInteger(0);
        CountDownLatch messageLatch = new CountDownLatch(1);

        // Create second client for second consumer
        Credentials credentials = new UsernamePasswordCredentials(USERNAME, PASSWORD);
        Client client2 = clientFactory.create(PROTOCOL.OPENWIRE, credentials);

        try {
            // Consumer 1 subscribes to queue
            client.subscribe(queueName, new GossResponseEvent() {
                @Override
                public void onMessage(Serializable response) {
                    int count = consumer1Count.incrementAndGet();
                    log.info("Consumer 1 received message #{}", count);
                    messageLatch.countDown();
                }
            }, DESTINATION_TYPE.QUEUE);

            // Consumer 2 subscribes to same queue
            client2.subscribe(queueName, new GossResponseEvent() {
                @Override
                public void onMessage(Serializable response) {
                    int count = consumer2Count.incrementAndGet();
                    log.info("Consumer 2 received message #{}", count);
                    messageLatch.countDown();
                }
            }, DESTINATION_TYPE.QUEUE);

            // Allow subscriptions to establish
            Thread.sleep(500);

            // Send one message to queue
            client.publish(queueName, "Test message", DESTINATION_TYPE.QUEUE);
            log.info("Sent message to queue");

            // Wait for message to be received
            boolean received = messageLatch.await(10, TimeUnit.SECONDS);
            assertTrue(received, "At least one consumer should receive the message");

            // Give time for any additional message delivery
            Thread.sleep(500);

            // Verify queue semantics: message delivered to exactly ONE consumer
            int totalReceived = consumer1Count.get() + consumer2Count.get();
            assertEquals(1, totalReceived,
                    "Queue message should be delivered to exactly ONE consumer, but was delivered to " + totalReceived);

            log.info("Consumer 1 received: {}, Consumer 2 received: {}",
                    consumer1Count.get(), consumer2Count.get());
            log.info("=== Queue semantics verified: message delivered to ONE consumer ===");

        } finally {
            client2.close();
        }
    }

    /**
     * Test topic semantics: messages are delivered to ALL subscribers. When
     * multiple subscribers listen on a topic, each message goes to all of them.
     */
    @Test
    void testTopicDeliveredToAllSubscribers() throws Exception {
        log.info("=== Testing Topic delivers to ALL subscribers ===");

        String topicName = "test.topic.semantics." + System.currentTimeMillis();

        AtomicInteger subscriber1Count = new AtomicInteger(0);
        AtomicInteger subscriber2Count = new AtomicInteger(0);
        CountDownLatch bothReceived = new CountDownLatch(2);

        // Create second client for second subscriber
        Credentials credentials = new UsernamePasswordCredentials(USERNAME, PASSWORD);
        Client client2 = clientFactory.create(PROTOCOL.OPENWIRE, credentials);

        try {
            // Subscriber 1 subscribes to topic
            client.subscribe(topicName, new GossResponseEvent() {
                @Override
                public void onMessage(Serializable response) {
                    int count = subscriber1Count.incrementAndGet();
                    log.info("Subscriber 1 received message #{}", count);
                    bothReceived.countDown();
                }
            }, DESTINATION_TYPE.TOPIC);

            // Subscriber 2 subscribes to same topic
            client2.subscribe(topicName, new GossResponseEvent() {
                @Override
                public void onMessage(Serializable response) {
                    int count = subscriber2Count.incrementAndGet();
                    log.info("Subscriber 2 received message #{}", count);
                    bothReceived.countDown();
                }
            }, DESTINATION_TYPE.TOPIC);

            // Allow subscriptions to establish
            Thread.sleep(500);

            // Publish one message to topic
            client.publish(topicName, "Test message", DESTINATION_TYPE.TOPIC);
            log.info("Published message to topic");

            // Wait for both subscribers to receive the message
            boolean received = bothReceived.await(10, TimeUnit.SECONDS);
            assertTrue(received, "Both subscribers should receive the message");

            // Verify topic semantics: message delivered to ALL subscribers
            assertEquals(1, subscriber1Count.get(),
                    "Subscriber 1 should receive exactly 1 message");
            assertEquals(1, subscriber2Count.get(),
                    "Subscriber 2 should receive exactly 1 message");

            log.info("Subscriber 1 received: {}, Subscriber 2 received: {}",
                    subscriber1Count.get(), subscriber2Count.get());
            log.info("=== Topic semantics verified: message delivered to ALL subscribers ===");

        } finally {
            client2.close();
        }
    }

    /**
     * Check if a TCP port is open on the given host.
     */
    private boolean isPortOpen(String host, int port) {
        try (Socket socket = new Socket(host, port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Find docker-compose.yml by checking multiple possible paths. Handles
     * different working directory scenarios (IDE vs Gradle).
     */
    private File findComposeFile() {
        // Possible locations relative to different working directories
        String[] possiblePaths = {
                "docker/docker-compose.yml", // From project root
                "../docker/docker-compose.yml", // From subproject directory
                "../../docker/docker-compose.yml", // From nested directory
        };

        for (String path : possiblePaths) {
            File file = new File(path);
            if (file.exists()) {
                return file;
            }
        }

        // Log current working directory for debugging
        File cwd = new File(".");
        log.error("Current working directory: {}", cwd.getAbsolutePath());
        log.error("Looking for docker-compose.yml in these locations:");
        for (String path : possiblePaths) {
            File file = new File(path);
            log.error("  {} -> {}", path, file.getAbsolutePath());
        }

        fail("docker-compose.yml not found. Make sure you're running from the project root.");
        return null; // Never reached
    }
}
