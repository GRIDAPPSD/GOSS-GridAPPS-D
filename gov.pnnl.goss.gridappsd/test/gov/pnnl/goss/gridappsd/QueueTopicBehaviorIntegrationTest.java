package gov.pnnl.goss.gridappsd;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.jms.Connection;
import jakarta.jms.Destination;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.core.client.ClientServiceFactory;

/**
 * Diagnostic test to understand how ActiveMQ routes messages between Queue and
 * Topic destinations, and how different protocols (OpenWire, STOMP) interact.
 *
 * This test documents the actual behavior to help fix the integration tests.
 *
 * Key questions to answer: 1. Does a message sent to Queue "foo" reach a Topic
 * subscriber on "foo"? 2. Does STOMP unprefixed destination default to Queue or
 * Topic? 3. How do JMS createQueue() vs createTopic() interact?
 *
 * Run with: ./gradlew :gov.pnnl.goss.gridappsd:test --tests
 * QueueTopicBehaviorIntegrationTest -PincludeIntegrationTests
 */
public class QueueTopicBehaviorIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(QueueTopicBehaviorIntegrationTest.class);

    private static final String OPENWIRE_URI = "tcp://localhost:61616";
    private static final String STOMP_URI = "stomp://localhost:61613";
    private static final String USERNAME = "system";
    private static final String PASSWORD = "manager";

    private Client gossClient;
    private ClientServiceFactory clientFactory;
    private Connection jmsConnection;
    private Session jmsSession;
    private boolean busAvailable = false;

    @Before
    public void setUp() {
        try {
            System.out.println("=== Setting up Queue/Topic behavior test ===");

            // Set up GOSS client
            Map<String, Object> properties = new HashMap<>();
            properties.put("goss.system.manager", USERNAME);
            properties.put("goss.system.manager.password", PASSWORD);
            properties.put("goss.openwire.uri", OPENWIRE_URI);
            properties.put("goss.stomp.uri", STOMP_URI);

            clientFactory = new ClientServiceFactory();
            clientFactory.updated(properties);

            Credentials credentials = new UsernamePasswordCredentials(USERNAME, PASSWORD);
            gossClient = clientFactory.create(PROTOCOL.STOMP, credentials);
            System.out.println("GOSS client created");

            // Set up raw JMS connection for direct testing
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(OPENWIRE_URI);
            factory.setUserName(USERNAME);
            factory.setPassword(PASSWORD);
            jmsConnection = factory.createConnection();
            jmsConnection.start();
            jmsSession = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            busAvailable = true;
            System.out.println("Successfully connected to message bus");

        } catch (Exception e) {
            System.out.println("Could not connect to message bus: " + e.getMessage());
            e.printStackTrace();
            busAvailable = false;
        }
    }

    @After
    public void tearDown() {
        if (gossClient != null) {
            try {
                gossClient.close();
            } catch (Exception e) {
                System.out.println("Error closing GOSS client: " + e.getMessage());
            }
        }
        if (jmsSession != null) {
            try {
                jmsSession.close();
            } catch (Exception e) {
                System.out.println("Error closing JMS session: " + e.getMessage());
            }
        }
        if (jmsConnection != null) {
            try {
                jmsConnection.close();
            } catch (Exception e) {
                System.out.println("Error closing JMS connection: " + e.getMessage());
            }
        }
    }

    /**
     * Test 1: Queue to Queue - same destination type should work
     */
    @Test(timeout = 30000)
    public void test1_QueueToQueue() throws Exception {
        if (!busAvailable) {
            System.out.println("Bus not available, skipping test");
            return;
        }

        System.out.println("\n=== TEST 1: Queue to Queue ===");
        String destName = "test.queue.to.queue." + System.currentTimeMillis();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> received = new AtomicReference<>();

        // Subscribe to Queue
        Destination queueDest = jmsSession.createQueue(destName);
        MessageConsumer consumer = jmsSession.createConsumer(queueDest);
        consumer.setMessageListener(msg -> {
            try {
                received.set(((TextMessage) msg).getText());
                System.out.println("Queue subscriber received: " + received.get());
                latch.countDown();
            } catch (Exception e) {
                System.err.println("Error in listener" + e);
            }
        });

        // Send to Queue
        MessageProducer producer = jmsSession.createProducer(queueDest);
        producer.send(jmsSession.createTextMessage("test message"));
        System.out.println("Sent to Queue: " + destName);

        boolean success = latch.await(5, TimeUnit.SECONDS);
        System.out.println("RESULT: Queue->Queue: " + (success ? "WORKS" : "FAILED"));
        consumer.close();
        producer.close();
    }

    /**
     * Test 2: Topic to Topic - same destination type should work
     */
    @Test(timeout = 30000)
    public void test2_TopicToTopic() throws Exception {
        if (!busAvailable) {
            System.out.println("Bus not available, skipping test");
            return;
        }

        System.out.println("\n=== TEST 2: Topic to Topic ===");
        String destName = "test.topic.to.topic." + System.currentTimeMillis();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> received = new AtomicReference<>();

        // Subscribe to Topic
        Destination topicDest = jmsSession.createTopic(destName);
        MessageConsumer consumer = jmsSession.createConsumer(topicDest);
        consumer.setMessageListener(msg -> {
            try {
                received.set(((TextMessage) msg).getText());
                System.out.println("Topic subscriber received: " + received.get());
                latch.countDown();
            } catch (Exception e) {
                System.err.println("Error in listener" + e);
            }
        });

        Thread.sleep(500); // Allow subscription to establish

        // Send to Topic
        MessageProducer producer = jmsSession.createProducer(topicDest);
        producer.send(jmsSession.createTextMessage("test message"));
        System.out.println("Sent to Topic: " + destName);

        boolean success = latch.await(5, TimeUnit.SECONDS);
        System.out.println("RESULT: Topic->Topic: " + (success ? "WORKS" : "FAILED"));
        consumer.close();
        producer.close();
    }

    /**
     * Test 3: Queue to Topic - does a Queue message reach Topic subscribers?
     */
    @Test(timeout = 30000)
    public void test3_QueueToTopic() throws Exception {
        if (!busAvailable) {
            System.out.println("Bus not available, skipping test");
            return;
        }

        System.out.println("\n=== TEST 3: Queue to Topic (cross-type) ===");
        String destName = "test.queue.to.topic." + System.currentTimeMillis();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> received = new AtomicReference<>();

        // Subscribe to TOPIC
        Destination topicDest = jmsSession.createTopic(destName);
        MessageConsumer consumer = jmsSession.createConsumer(topicDest);
        consumer.setMessageListener(msg -> {
            try {
                received.set(((TextMessage) msg).getText());
                System.out.println("Topic subscriber received from Queue sender: " + received.get());
                latch.countDown();
            } catch (Exception e) {
                System.err.println("Error in listener" + e);
            }
        });

        Thread.sleep(500);

        // Send to QUEUE (same name, different type)
        Destination queueDest = jmsSession.createQueue(destName);
        MessageProducer producer = jmsSession.createProducer(queueDest);
        producer.send(jmsSession.createTextMessage("test message"));
        System.out.println("Sent to Queue: " + destName + " (expecting Topic subscriber)");

        boolean success = latch.await(5, TimeUnit.SECONDS);
        System.out.println(
                "RESULT: Queue->Topic: " + (success ? "WORKS (cross-type routing)" : "FAILED (separate namespaces)"));
        consumer.close();
        producer.close();
    }

    /**
     * Test 4: Topic to Queue - does a Topic message reach Queue consumers?
     */
    @Test(timeout = 30000)
    public void test4_TopicToQueue() throws Exception {
        if (!busAvailable) {
            System.out.println("Bus not available, skipping test");
            return;
        }

        System.out.println("\n=== TEST 4: Topic to Queue (cross-type) ===");
        String destName = "test.topic.to.queue." + System.currentTimeMillis();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> received = new AtomicReference<>();

        // Subscribe to QUEUE
        Destination queueDest = jmsSession.createQueue(destName);
        MessageConsumer consumer = jmsSession.createConsumer(queueDest);
        consumer.setMessageListener(msg -> {
            try {
                received.set(((TextMessage) msg).getText());
                System.out.println("Queue subscriber received from Topic sender: " + received.get());
                latch.countDown();
            } catch (Exception e) {
                System.err.println("Error in listener" + e);
            }
        });

        Thread.sleep(500);

        // Send to TOPIC (same name, different type)
        Destination topicDest = jmsSession.createTopic(destName);
        MessageProducer producer = jmsSession.createProducer(topicDest);
        producer.send(jmsSession.createTextMessage("test message"));
        System.out.println("Sent to Topic: " + destName + " (expecting Queue subscriber)");

        boolean success = latch.await(5, TimeUnit.SECONDS);
        System.out.println(
                "RESULT: Topic->Queue: " + (success ? "WORKS (cross-type routing)" : "FAILED (separate namespaces)"));
        consumer.close();
        producer.close();
    }

    /**
     * Test 5: GOSS client.subscribe() uses Topics - verify behavior
     */
    @Test(timeout = 30000)
    public void test5_GossSubscribeIsTopic() throws Exception {
        if (!busAvailable) {
            System.out.println("Bus not available, skipping test");
            return;
        }

        System.out.println("\n=== TEST 5: GOSS subscribe() + JMS Topic sender ===");
        String destName = "test.goss.subscribe." + System.currentTimeMillis();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> received = new AtomicReference<>();

        // GOSS subscribe (should create Topic subscription)
        gossClient.subscribe(destName, new GossResponseEvent() {
            @Override
            public void onMessage(Serializable response) {
                received.set(response.toString());
                System.out.println("GOSS subscriber received: " + received.get());
                latch.countDown();
            }
        });

        Thread.sleep(500);

        // Send via JMS Topic
        Destination topicDest = jmsSession.createTopic(destName);
        MessageProducer producer = jmsSession.createProducer(topicDest);
        producer.send(jmsSession.createTextMessage("test message"));
        System.out.println("JMS sent to Topic: " + destName);

        boolean success = latch.await(5, TimeUnit.SECONDS);
        System.out.println("RESULT: JMS Topic -> GOSS subscribe: " + (success ? "WORKS" : "FAILED"));
        producer.close();
    }

    /**
     * Test 6: GOSS client.subscribe() + JMS Queue sender - cross type
     */
    @Test(timeout = 30000)
    public void test6_GossSubscribeVsJmsQueue() throws Exception {
        if (!busAvailable) {
            System.out.println("Bus not available, skipping test");
            return;
        }

        System.out.println("\n=== TEST 6: GOSS subscribe() + JMS Queue sender ===");
        String destName = "test.goss.vs.queue." + System.currentTimeMillis();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> received = new AtomicReference<>();

        // GOSS subscribe (Topic)
        gossClient.subscribe(destName, new GossResponseEvent() {
            @Override
            public void onMessage(Serializable response) {
                received.set(response.toString());
                System.out.println("GOSS subscriber received from Queue: " + received.get());
                latch.countDown();
            }
        });

        Thread.sleep(500);

        // Send via JMS Queue
        Destination queueDest = jmsSession.createQueue(destName);
        MessageProducer producer = jmsSession.createProducer(queueDest);
        producer.send(jmsSession.createTextMessage("test message"));
        System.out.println("JMS sent to Queue: " + destName + " (GOSS subscribed to Topic)");

        boolean success = latch.await(5, TimeUnit.SECONDS);
        System.out.println("RESULT: JMS Queue -> GOSS subscribe (Topic): "
                + (success ? "WORKS (cross-type)" : "FAILED (namespaces separate)"));
        producer.close();
    }

    /**
     * Test 7: GOSS getResponse() uses Queue - this is the suspected problem
     */
    @Test(timeout = 30000)
    public void test7_GossGetResponseUsesQueue() throws Exception {
        if (!busAvailable) {
            System.out.println("Bus not available, skipping test");
            return;
        }

        System.out.println("\n=== TEST 7: GOSS getResponse() destination type ===");
        System.out.println("NOTE: getResponse() uses session.createQueue() internally");
        System.out.println("If ProcessManagerImpl uses subscribe() (Topic), they won't connect!");

        // We can't easily test getResponse() without a responder,
        // but we can document what we know:
        System.out.println("");
        System.out.println("=== ANALYSIS ===");
        System.out.println("GossClient.getResponse() line 284: session.createQueue(topic)");
        System.out.println("GossClient.subscribe() line 534: session.createTopic(topicName)");
        System.out.println("ProcessManagerImpl uses: client.subscribe(topic_process_prefix + '.>')");
        System.out.println("");
        System.out.println("This means:");
        System.out.println("- Java getResponse() sends to QUEUE");
        System.out.println("- ProcessManagerImpl listens on TOPIC");
        System.out.println("- They are in separate namespaces and won't connect!");
    }

    /**
     * Test 8: GOSS publish() - what destination type does it use?
     */
    @Test(timeout = 30000)
    public void test8_GossPublishDestinationType() throws Exception {
        if (!busAvailable) {
            System.out.println("Bus not available, skipping test");
            return;
        }

        System.out.println("\n=== TEST 8: GOSS publish() + JMS Topic subscriber ===");
        String destName = "test.goss.publish." + System.currentTimeMillis();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> received = new AtomicReference<>();

        // JMS Topic subscriber
        Destination topicDest = jmsSession.createTopic(destName);
        MessageConsumer consumer = jmsSession.createConsumer(topicDest);
        consumer.setMessageListener(msg -> {
            try {
                received.set(((TextMessage) msg).getText());
                System.out.println("JMS Topic subscriber received from GOSS publish: " + received.get());
                latch.countDown();
            } catch (Exception e) {
                System.err.println("Error in listener" + e);
            }
        });

        Thread.sleep(500);

        // GOSS publish
        gossClient.publish(destName, "test message");
        System.out.println("GOSS published to: " + destName);

        boolean success = latch.await(5, TimeUnit.SECONDS);
        System.out.println("RESULT: GOSS publish -> JMS Topic: " + (success ? "WORKS (publish uses Topic)" : "FAILED"));
        consumer.close();
    }

    /**
     * Test 9: GOSS getResponse() with explicit TOPIC destination type This test
     * creates a responder that listens on a Topic and replies to the reply-to
     * destination. Note: getResponse() defaults to QUEUE (to match Python), so we
     * must explicitly specify TOPIC when communicating with Topic-based services.
     */
    @Test(timeout = 30000)
    public void test9_GossGetResponseWithTopicFix() throws Exception {
        if (!busAvailable) {
            System.out.println("Bus not available, skipping test");
            return;
        }

        System.out.println("\n=== TEST 9: GOSS getResponse() with explicit TOPIC ===");
        String destName = "test.getresponse.topic." + System.currentTimeMillis();

        // Set up a JMS Topic listener that will respond to requests
        Destination topicDest = jmsSession.createTopic(destName);
        MessageConsumer consumer = jmsSession.createConsumer(topicDest);
        consumer.setMessageListener(msg -> {
            try {
                String request = ((TextMessage) msg).getText();
                System.out.println("Responder received request: " + request);

                // Get the reply-to destination and send response
                Destination replyTo = msg.getJMSReplyTo();
                if (replyTo != null) {
                    System.out.println("Sending response to: " + replyTo);
                    MessageProducer replyProducer = jmsSession.createProducer(replyTo);
                    replyProducer.send(jmsSession.createTextMessage("Response to: " + request));
                    replyProducer.close();
                } else {
                    System.out.println("No reply-to destination!");
                }
            } catch (Exception e) {
                System.err.println("Error in responder: " + e);
            }
        });

        Thread.sleep(500);

        // Use explicit TOPIC destination type since we're sending to a Topic
        System.out.println("Sending request via GOSS getResponse() with TOPIC...");
        try {
            Serializable response = gossClient.getResponse(
                    "test request message",
                    destName,
                    pnnl.goss.core.Request.RESPONSE_FORMAT.JSON,
                    pnnl.goss.core.Client.DESTINATION_TYPE.TOPIC);
            System.out.println("RESULT: GOSS getResponse -> Topic: WORKS! Response: " + response);
        } catch (Exception e) {
            System.out.println("RESULT: GOSS getResponse -> Topic: FAILED - " + e.getMessage());
        } finally {
            consumer.close();
        }
    }

    /**
     * Main method to run all tests manually
     */
    public static void main(String[] args) {
        QueueTopicBehaviorIntegrationTest test = new QueueTopicBehaviorIntegrationTest();

        try {
            test.setUp();

            if (!test.busAvailable) {
                System.err.println("Message bus not available. Start GridAPPS-D first.");
                System.exit(1);
            }

            System.out.println("\n" + "=".repeat(60));
            System.out.println("Queue vs Topic Behavior Diagnostic Tests");
            System.out.println("=".repeat(60) + "\n");

            test.test1_QueueToQueue();
            test.test2_TopicToTopic();
            test.test3_QueueToTopic();
            test.test4_TopicToQueue();
            test.test5_GossSubscribeIsTopic();
            test.test6_GossSubscribeVsJmsQueue();
            test.test7_GossGetResponseUsesQueue();
            test.test8_GossPublishDestinationType();

            System.out.println("\n" + "=".repeat(60));
            System.out.println("Tests Complete - Review results above");
            System.out.println("=".repeat(60));

        } catch (Exception e) {
            System.err.println("Test failed:");
            e.printStackTrace();
            System.exit(1);
        } finally {
            test.tearDown();
        }
    }
}
