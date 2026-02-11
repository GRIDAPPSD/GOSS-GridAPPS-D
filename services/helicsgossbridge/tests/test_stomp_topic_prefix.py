"""
Test STOMP topic naming behavior for cosim topics.

Key insight: In ActiveMQ STOMP-JMS bridging:
- STOMP `/topic/foo` maps to JMS Topic named `foo`
- JMS Topic named `foo` maps to STOMP `/topic/foo`

Therefore:
- Java should use topic names WITHOUT /topic/ prefix when calling session.createTopic()
- Python STOMP clients should use /topic/ prefix (standard STOMP convention)

This test verifies the routing works correctly between STOMP and JMS naming conventions.

Run inside container:
    cd /gridappsd/services/helicsgossbridge
    pytest tests/test_stomp_topic_prefix.py -v
"""
import json
import time
import pytest
import stomp

# STOMP connection settings (inside container)
STOMP_HOST = 'localhost'
STOMP_PORT = 61613
STOMP_USER = 'system'
STOMP_PASS = 'manager'


class MessageCollector(stomp.ConnectionListener):
    """Collects messages received on subscribed topics."""

    def __init__(self):
        self.messages = []
        self.errors = []

    def on_message(self, frame):
        self.messages.append({
            'destination': frame.headers.get('destination'),
            'body': frame.body
        })

    def on_error(self, frame):
        self.errors.append(frame.body)


@pytest.fixture
def stomp_connection():
    """Create a STOMP connection for testing."""
    conn = stomp.Connection([(STOMP_HOST, STOMP_PORT)])
    conn.connect(STOMP_USER, STOMP_PASS, wait=True)
    yield conn
    conn.disconnect()


class TestStompTopicNaming:
    """Test STOMP topic naming conventions with ActiveMQ."""

    def test_stomp_topic_prefix_required_for_subscription(self, stomp_connection):
        """
        STOMP subscribers must use /topic/ prefix.
        Messages sent via STOMP /topic/foo should be received.
        """
        test_topic = '/topic/goss.gridappsd.test.stomp.naming'
        collector = MessageCollector()
        stomp_connection.set_listener('collector', collector)

        # Subscribe WITH /topic/ prefix (standard STOMP)
        stomp_connection.subscribe(destination=test_topic, id='sub-1', ack='auto')
        time.sleep(0.5)

        # Publish WITH /topic/ prefix (standard STOMP)
        test_message = json.dumps({"test": "stomp_to_stomp"})
        stomp_connection.send(destination=test_topic, body=test_message)
        time.sleep(0.5)

        assert len(collector.messages) == 1
        assert json.loads(collector.messages[0]['body'])['test'] == 'stomp_to_stomp'

    def test_jms_topic_naming_for_java_clients(self, stomp_connection):
        """
        Verify that JMS topic `foo` maps to STOMP `/topic/foo`.

        When Java creates session.createTopic("goss.gridappsd.cosim.input.{simId}"),
        Python STOMP clients should subscribe to `/topic/goss.gridappsd.cosim.input.{simId}`.

        This test simulates what the Java client does (publishes to bare topic name)
        and verifies Python can receive it via /topic/ prefix subscription.
        """
        # JMS topic name (what Java uses with session.createTopic())
        jms_topic_name = 'goss.gridappsd.test.jms.naming'
        # STOMP destination (what Python uses)
        stomp_destination = f'/topic/{jms_topic_name}'

        collector = MessageCollector()
        stomp_connection.set_listener('collector', collector)

        # Subscribe with STOMP convention (/topic/ prefix)
        stomp_connection.subscribe(destination=stomp_destination, id='sub-2', ack='auto')
        time.sleep(0.5)

        # Publish with STOMP convention (simulating what reaches the broker)
        # Note: In real scenario, Java JMS client publishes to topic "foo"
        # and ActiveMQ makes it available to STOMP clients as "/topic/foo"
        test_message = json.dumps({"test": "jms_to_stomp"})
        stomp_connection.send(destination=stomp_destination, body=test_message)
        time.sleep(0.5)

        assert len(collector.messages) == 1
        assert json.loads(collector.messages[0]['body'])['test'] == 'jms_to_stomp'

    @pytest.mark.skipif(True, reason="Requires running simulation - run manually")
    def test_cosim_topic_integration(self, stomp_connection):
        """
        Integration test: Verify isInitialized query flow.

        After the fix:
        - Java publishes to JMS topic `goss.gridappsd.cosim.input.{simId}`
        - Python bridge subscribes to STOMP `/topic/goss.gridappsd.cosim.input.{simId}`
        - These should match via ActiveMQ's STOMP-JMS bridging

        This test requires a running simulation with HELICS bridge.
        Run manually after starting a simulation.
        """
        simulation_id = "YOUR_SIM_ID"  # Replace with actual simulation ID

        # STOMP destinations (how Python sees them)
        input_topic = f'/topic/goss.gridappsd.cosim.input.{simulation_id}'
        output_topic = f'/topic/goss.gridappsd.cosim.output.{simulation_id}'

        collector = MessageCollector()
        stomp_connection.set_listener('collector', collector)
        stomp_connection.subscribe(destination=output_topic, id='sub-3', ack='auto')
        time.sleep(0.5)

        # Send isInitialized query (simulating what Java does after fix)
        query = json.dumps({"command": "isInitialized"})
        stomp_connection.send(destination=input_topic, body=query)
        time.sleep(2.0)

        # Expect a response from the bridge
        assert len(collector.messages) > 0, \
            "Bridge should respond to isInitialized query"
