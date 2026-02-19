"""
GOSS STOMP Integration Tests

Python-based integration tests that exercise the GOSS server over STOMP,
covering the same ground as the Java OSGi integration tests
(GossOSGiEndToEndTest) plus token authentication:

  Core connectivity (mirrors GossOSGiEndToEndTest):
    - Server is reachable via STOMP
    - Publish/subscribe on a topic
    - Multiple subscribers receive a broadcast
    - Client reconnection with unique sessions

  Token authentication:
    - Request a JWT token via the token topic
    - Verify the token is non-empty (regression for parseToken bug)
    - Reconnect using the token
    - Pub/sub works on a token-authenticated connection
    - Invalid credentials are rejected
    - Empty token is rejected

Requires a running GOSS/GridAPPS-D server with STOMP transport enabled.
Configure via environment variables or command-line arguments.

Usage:
    # Run inside Docker container
    python -m pytest /tmp/test_stomp_token_auth.py -v

    # Run from host against Docker (default: localhost:61613)
    pytest tests/test_stomp_token_auth.py -v

    # Via Makefile (runs inside Docker container)
    make test-stomp-token

    # Standalone (no pytest required)
    python tests/test_stomp_token_auth.py

    # Custom host/ports
    python tests/test_stomp_token_auth.py --host localhost --port 61613
"""

import argparse
import base64
import logging
import os
import sys
import threading
import time
import uuid

import stomp

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)-8s [%(name)s] %(message)s",
)
log = logging.getLogger("test_stomp_token_auth")

# ---------------------------------------------------------------------------
# Configuration defaults (overridable via env vars or CLI args)
# ---------------------------------------------------------------------------
STOMP_HOST = os.environ.get("GOSS_STOMP_HOST", "localhost")
STOMP_PORT = int(os.environ.get("GOSS_STOMP_PORT", "61613"))
USERNAME = os.environ.get("GOSS_USERNAME", "system")
PASSWORD = os.environ.get("GOSS_PASSWORD", "manager")
TOKEN_TOPIC = "/topic/pnnl.goss.token.topic"
TOKEN_TIMEOUT_S = 10
HEARTBEAT_MS = 10000


# ---------------------------------------------------------------------------
# Listener helpers
# ---------------------------------------------------------------------------
class TokenResponseListener(stomp.ConnectionListener):
    """Listens on a temporary queue for the JWT token response."""

    def __init__(self):
        self.token = None
        self.error = None
        self._event = threading.Event()

    def on_message(self, frame):
        body = frame.body if hasattr(frame, "body") else str(frame)
        log.info("Token response received (%d bytes)", len(body))
        self.token = body
        self._event.set()

    def on_error(self, frame):
        body = frame.body if hasattr(frame, "body") else str(frame)
        log.error("STOMP error during token request: %s", body)
        self.error = body
        self._event.set()

    def wait(self, timeout=TOKEN_TIMEOUT_S):
        return self._event.wait(timeout)


class PubSubListener(stomp.ConnectionListener):
    """Listens for a single message on a subscribed topic."""

    def __init__(self):
        self.received_message = None
        self.received_headers = None
        self.error = None
        self._event = threading.Event()

    def on_message(self, frame):
        self.received_headers = frame.headers if hasattr(frame, "headers") else {}
        self.received_message = frame.body if hasattr(frame, "body") else str(frame)
        log.debug("PubSub received: %s", self.received_message[:200])
        self._event.set()

    def on_error(self, frame):
        body = frame.body if hasattr(frame, "body") else str(frame)
        log.error("STOMP error in pub/sub: %s", body)
        self.error = body
        self._event.set()

    def wait(self, timeout=5):
        return self._event.wait(timeout)


class MultiMessageListener(stomp.ConnectionListener):
    """Collects multiple messages, signaling after a target count is reached."""

    def __init__(self, target_count=1):
        self.messages = []
        self._target = target_count
        self._event = threading.Event()

    def on_message(self, frame):
        body = frame.body if hasattr(frame, "body") else str(frame)
        self.messages.append(body)
        if len(self.messages) >= self._target:
            self._event.set()

    def on_error(self, frame):
        body = frame.body if hasattr(frame, "body") else str(frame)
        log.error("STOMP error: %s", body)

    def wait(self, timeout=5):
        return self._event.wait(timeout)


# ---------------------------------------------------------------------------
# Test helpers
# ---------------------------------------------------------------------------
def create_connection(host, port):
    """Create a new STOMP 1.2 connection (not yet connected)."""
    return stomp.Connection12(
        [(host, port)],
        heartbeats=(HEARTBEAT_MS, HEARTBEAT_MS),
    )


def request_token(host, port, username, password):
    """
    Connect with credentials, request a JWT token, and return it.

    The GOSS token flow:
      - Connect via STOMP with username/password
      - Subscribe to a temporary reply queue
      - Send base64(username:password) to the token topic with reply-to header
      - Wait for the server to respond with a JWT token
    """
    conn = create_connection(host, port)
    listener = TokenResponseListener()
    conn.set_listener("token_listener", listener)

    log.info("Connecting to %s:%d as '%s' to request token...", host, port, username)
    conn.connect(username, password, wait=True)
    assert conn.is_connected(), "Failed to connect with username/password"
    log.info("Connected successfully with credentials")

    # Use uuid to avoid reply-queue collisions in rapid test runs
    reply_dest = f"temp.token_resp.{username}-{uuid.uuid4().hex[:12]}"
    auth_payload = base64.b64encode(f"{username}:{password}".encode()).decode()

    # Subscribe to the temp queue where the token response will arrive
    conn.subscribe(destination=f"/queue/{reply_dest}", id="token-sub-1", ack="auto")
    log.info("Subscribed to /queue/%s", reply_dest)

    # Allow subscription to propagate to broker before sending
    time.sleep(0.3)

    # Send the token request
    conn.send(
        destination=TOKEN_TOPIC,
        body=auth_payload,
        headers={"reply-to": f"/queue/{reply_dest}"},
    )
    log.info("Sent token request to %s", TOKEN_TOPIC)

    # Wait for the token
    got_response = listener.wait(TOKEN_TIMEOUT_S)
    token = listener.token

    # Disconnect the credential-based connection
    try:
        conn.disconnect()
    except Exception as e:
        log.debug("Non-critical error during disconnect: %s", e)

    return got_response, token, listener.error


def connect_with_token(host, port, token):
    """
    Connect using a JWT token as the username with an empty password.
    Returns the connected stomp.Connection12.
    """
    conn = create_connection(host, port)
    log.info("Connecting with token (%d bytes)...", len(token))
    try:
        conn.connect(token, "", wait=True)
    except stomp.exception.ConnectFailedException as e:
        raise AssertionError(
            f"Token-based STOMP connect failed. Token length={len(token)}, "
            f"token prefix={token[:40]}... Error: {e}"
        ) from e
    return conn


def verify_pubsub_with_token(conn, token):
    """
    Verify the token-based connection can publish and subscribe.
    Sends a test message on a topic and verifies receipt.
    """
    test_topic = "/topic/goss.test.stomp.token.auth"
    test_body = f'{{"test": "token_auth", "timestamp": {time.time()}}}'

    listener = PubSubListener()
    conn.set_listener("pubsub_listener", listener)
    conn.subscribe(destination=test_topic, id="pubsub-sub-1", ack="auto")
    log.info("Subscribed to %s", test_topic)

    # Allow subscription to propagate to broker
    time.sleep(0.5)

    conn.send(
        destination=test_topic,
        body=test_body,
        headers={"GOSS_HAS_SUBJECT": "true", "GOSS_SUBJECT": token},
    )
    log.info("Published test message to %s", test_topic)

    got_message = listener.wait(5)
    return got_message, listener.received_message


# ---------------------------------------------------------------------------
# Test cases: Core STOMP functionality (mirrors GossOSGiEndToEndTest)
# ---------------------------------------------------------------------------
class TestStompCore:
    """
    Core STOMP integration tests that mirror the Java GossOSGiEndToEndTest.

    These verify the same server behaviors that the OSGi tests check, but
    from an external STOMP client rather than from inside the OSGi container.
    """

    host = STOMP_HOST
    port = STOMP_PORT
    username = USERNAME
    password = PASSWORD

    def _connect(self):
        """Helper: create and connect a STOMP client with credentials."""
        conn = create_connection(self.host, self.port)
        conn.connect(self.username, self.password, wait=True)
        assert conn.is_connected(), "Failed to connect"
        return conn

    # -- mirrors GossOSGiEndToEndTest.testServerIsRunning / testGossClientConnection --
    def test_server_reachable(self):
        """Server accepts STOMP connections (mirrors testServerIsRunning)."""
        conn = self._connect()
        log.info("PASS: server reachable at %s:%d", self.host, self.port)
        conn.disconnect()

    # -- mirrors GossOSGiEndToEndTest.testPublishSubscribe --
    def test_publish_subscribe(self):
        """Publish a message and receive it on the same topic (mirrors testPublishSubscribe)."""
        conn = self._connect()
        test_topic = "/topic/test.stomp.pubsub"
        test_message = f'{{"msg": "hello from STOMP", "ts": {time.time()}}}'

        listener = PubSubListener()
        conn.set_listener("pubsub", listener)
        conn.subscribe(destination=test_topic, id="ps-1", ack="auto")
        time.sleep(0.5)

        conn.send(destination=test_topic, body=test_message)
        assert listener.wait(5), "Should receive the published message"
        assert listener.received_message is not None
        assert "hello from STOMP" in listener.received_message, (
            f"Content mismatch: {listener.received_message}"
        )
        log.info("PASS: publish/subscribe")
        conn.disconnect()

    # -- mirrors GossOSGiEndToEndTest.testMultipleClients --
    def test_multiple_subscribers(self):
        """Two subscribers both receive a broadcast (mirrors testMultipleClients)."""
        publisher = self._connect()
        sub1 = self._connect()
        sub2 = self._connect()

        test_topic = "/topic/test.stomp.multi"
        test_message = "broadcast message"

        listener1 = PubSubListener()
        listener2 = PubSubListener()
        sub1.set_listener("sub1", listener1)
        sub2.set_listener("sub2", listener2)
        sub1.subscribe(destination=test_topic, id="m-1", ack="auto")
        sub2.subscribe(destination=test_topic, id="m-2", ack="auto")
        time.sleep(0.5)

        publisher.send(destination=test_topic, body=test_message)

        assert listener1.wait(5), "Subscriber 1 should receive the message"
        assert listener2.wait(5), "Subscriber 2 should receive the message"
        assert test_message in listener1.received_message
        assert test_message in listener2.received_message
        log.info("PASS: multiple subscribers")

        publisher.disconnect()
        sub1.disconnect()
        sub2.disconnect()

    # -- mirrors GossOSGiEndToEndTest.testClientReconnection --
    def test_client_reconnection(self):
        """Disconnect and reconnect; new session still works (mirrors testClientReconnection)."""
        # First connection
        conn1 = self._connect()
        id1 = conn1.get_listener("") or id(conn1)  # no session-id in stomp.py; use object id
        conn1.disconnect()

        # Second connection
        conn2 = self._connect()
        id2 = conn2.get_listener("") or id(conn2)
        assert id1 != id2, "Each connection should be a distinct object"

        # Verify second connection can pub/sub
        test_topic = "/topic/test.stomp.reconnect"
        listener = PubSubListener()
        conn2.set_listener("recon", listener)
        conn2.subscribe(destination=test_topic, id="r-1", ack="auto")
        time.sleep(0.3)
        conn2.send(destination=test_topic, body="after reconnect")
        assert listener.wait(5), "Should receive message after reconnect"
        assert "after reconnect" in listener.received_message
        log.info("PASS: client reconnection")
        conn2.disconnect()

    def test_publish_json_data(self):
        """Publish structured JSON and receive it intact."""
        conn = self._connect()
        test_topic = "/topic/test.stomp.json"
        import json
        payload = json.dumps({"name": "test", "value": 42, "nested": {"a": True}})

        listener = PubSubListener()
        conn.set_listener("json", listener)
        conn.subscribe(destination=test_topic, id="j-1", ack="auto")
        time.sleep(0.5)

        conn.send(destination=test_topic, body=payload)
        assert listener.wait(5), "Should receive JSON message"
        received = json.loads(listener.received_message)
        assert received["name"] == "test"
        assert received["value"] == 42
        assert received["nested"]["a"] is True
        log.info("PASS: JSON data round-trip")
        conn.disconnect()

    def test_multiple_topics(self):
        """Subscribe to two topics and receive messages on both."""
        conn = self._connect()
        topic_a = "/topic/test.stomp.topicA"
        topic_b = "/topic/test.stomp.topicB"

        listener = MultiMessageListener(target_count=2)
        conn.set_listener("multi_topic", listener)
        conn.subscribe(destination=topic_a, id="ta-1", ack="auto")
        conn.subscribe(destination=topic_b, id="tb-1", ack="auto")
        time.sleep(0.5)

        conn.send(destination=topic_a, body="msg for A")
        conn.send(destination=topic_b, body="msg for B")

        assert listener.wait(5), "Should receive messages on both topics"
        bodies = " ".join(listener.messages)
        assert "msg for A" in bodies
        assert "msg for B" in bodies
        log.info("PASS: multiple topics")
        conn.disconnect()


# ---------------------------------------------------------------------------
# Test cases: Token authentication
# ---------------------------------------------------------------------------
class TestStompTokenAuth:
    """Integration tests for STOMP token authentication against a live GOSS server."""

    host = STOMP_HOST
    port = STOMP_PORT
    username = USERNAME
    password = PASSWORD

    def test_01_credential_connect(self):
        """Verify basic STOMP connection with username/password works."""
        conn = create_connection(self.host, self.port)
        conn.connect(self.username, self.password, wait=True)
        assert conn.is_connected(), "Should connect with valid credentials"
        log.info("PASS: credential connect")
        conn.disconnect()

    def test_02_token_request_returns_nonempty_token(self):
        """Request a token and verify it is returned non-empty (the core bug fix)."""
        got_response, token, error = request_token(
            self.host, self.port, self.username, self.password
        )
        assert got_response, f"Should get a token response within {TOKEN_TIMEOUT_S}s"
        assert error is None, f"Should not get an error: {error}"
        assert token is not None, "Token must not be None"
        assert len(token.strip()) > 0, "Token must not be empty"
        assert token != "authentication failed", "Token request should not fail auth"
        # JWT tokens have 3 dot-separated parts (header.payload.signature)
        parts = token.split(".")
        assert len(parts) == 3, (
            f"Token should be a JWT with 3 parts (header.payload.signature), "
            f"got {len(parts)} parts: {token[:80]}..."
        )
        log.info("PASS: received valid JWT token (%d bytes, 3 parts)", len(token))
        # Stash for dependent tests
        TestStompTokenAuth._token = token

    def test_03_connect_with_token(self):
        """Reconnect using the JWT token as credentials."""
        token = getattr(TestStompTokenAuth, "_token", None)
        assert token is not None, (
            "test_03 depends on test_02 having produced a valid token. "
            "Run tests sequentially: pytest -v (tests are ordered by name)."
        )

        conn = connect_with_token(self.host, self.port, token)
        assert conn.is_connected(), "Should connect with token"
        log.info("PASS: connected with token")
        conn.disconnect()

    def test_04_pubsub_with_token(self):
        """Verify publish/subscribe works on a token-authenticated connection."""
        token = getattr(TestStompTokenAuth, "_token", None)
        assert token is not None, (
            "test_04 depends on test_02 having produced a valid token. "
            "Run tests sequentially: pytest -v (tests are ordered by name)."
        )

        conn = connect_with_token(self.host, self.port, token)
        assert conn.is_connected(), "Should connect with token"

        got_message, received = verify_pubsub_with_token(conn, token)
        assert got_message, "Should receive the published message"
        assert received is not None, "Received message should not be None"
        assert "token_auth" in received, f"Message content mismatch: {received}"
        log.info("PASS: pub/sub works with token auth")
        conn.disconnect()

    def test_05_invalid_credentials_no_token(self):
        """Verify that invalid credentials do not produce a valid token."""
        try:
            got_response, token, error = request_token(
                self.host, self.port, "baduser", "badpass"
            )
        except (stomp.exception.ConnectFailedException, AssertionError):
            # Connection refused with bad credentials -- expected
            log.info("PASS: invalid credentials rejected at STOMP connect")
            return

        # If connection succeeded, verify no valid JWT was issued
        if got_response and token:
            assert token == "authentication failed" or len(token.split(".")) != 3, (
                f"Invalid credentials should not produce a valid JWT, got: {token[:80]}"
            )
            log.info("PASS: server returned auth failure message")
        else:
            # No response within timeout -- also acceptable (server ignored bad creds)
            log.info("PASS: no token response for invalid credentials (timeout)")

    def test_06_empty_token_rejected(self):
        """Verify that connecting with an empty string as token fails."""
        conn = create_connection(self.host, self.port)
        connected = False
        try:
            conn.connect("", "", wait=True, headers={"accept-version": "1.2"})
            connected = conn.is_connected()
        except (stomp.exception.ConnectFailedException, Exception):
            log.info("PASS: empty token connection correctly refused")
            return
        finally:
            try:
                conn.disconnect()
            except Exception:
                pass

        if connected:
            # Broker allows anonymous -- this test is not meaningful in
            # that configuration, so skip rather than false-pass.
            try:
                import pytest
                pytest.skip(
                    "Broker allows anonymous connections; "
                    "empty-token rejection cannot be verified."
                )
            except ImportError:
                log.warning(
                    "SKIP: broker allows anonymous connections; "
                    "empty-token rejection cannot be verified."
                )


# ---------------------------------------------------------------------------
# Standalone runner
# ---------------------------------------------------------------------------
def run_all_tests(host, port, username, password):
    """Run all tests sequentially, reporting pass/fail."""
    test_classes = [TestStompCore, TestStompTokenAuth]
    passed = 0
    failed = 0
    skipped = 0
    errors = []

    print(f"\n{'='*60}")
    print(f"GOSS STOMP Integration Tests")
    print(f"Server: {host}:{port}  User: {username}")
    print(f"{'='*60}")

    for cls in test_classes:
        instance = cls()
        instance.host = host
        instance.port = port
        instance.username = username
        instance.password = password

        test_methods = [m for m in sorted(dir(instance)) if m.startswith("test_")]
        print(f"\n  [{cls.__name__}]")

        for method_name in test_methods:
            method = getattr(instance, method_name)
            doc = method.__doc__ or method_name
            print(f"    {method_name}: {doc.strip()}")
            try:
                method()
                passed += 1
                print(f"      -> PASSED")
            except Exception as e:
                if "SKIP" in str(e) or "skip" in type(e).__name__.lower():
                    skipped += 1
                    print(f"      -> SKIPPED: {e}")
                else:
                    failed += 1
                    errors.append((f"{cls.__name__}.{method_name}", e))
                    print(f"      -> FAILED: {e}")

    print(f"\n{'='*60}")
    print(f"Results: {passed} passed, {failed} failed, {skipped} skipped "
          f"out of {passed + failed + skipped}")
    print(f"{'='*60}")

    if errors:
        print("\nFailures:")
        for name, err in errors:
            print(f"  {name}: {err}")
        return 1
    return 0


def main():
    parser = argparse.ArgumentParser(
        description="Test STOMP token authentication against a GOSS server"
    )
    parser.add_argument(
        "--host", default=STOMP_HOST, help=f"STOMP host (default: {STOMP_HOST})"
    )
    parser.add_argument(
        "--port", type=int, default=STOMP_PORT,
        help=f"STOMP port (default: {STOMP_PORT})"
    )
    parser.add_argument(
        "--username", default=USERNAME, help=f"Username (default: {USERNAME})"
    )
    parser.add_argument(
        "--password", default=PASSWORD, help=f"Password (default: {PASSWORD})"
    )
    args = parser.parse_args()

    rc = run_all_tests(args.host, args.port, args.username, args.password)
    sys.exit(rc)


if __name__ == "__main__":
    main()
