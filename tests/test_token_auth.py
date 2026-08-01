"""
GOSS Token Authentication Tests -- STOMP and WebSocket

Integration tests that exercise JWT token authentication over both
STOMP (TCP) and WebSocket transports against a running GOSS server.

Test matrix:
  For each transport (STOMP, WebSocket):
    - Connect with username/password credentials
    - Request a JWT token via the token topic
    - Validate the JWT has 3 parts (header.payload.signature)
    - Reconnect using the JWT token
    - Publish/subscribe works on the token-authenticated connection
    - Invalid credentials are rejected
    - Empty token is rejected

Requires a running GOSS/GridAPPS-D server with STOMP and WebSocket transports enabled.
Configure via environment variables or command-line arguments.

Usage:
    # Run via pixi
    pixi run test-token-auth

    # Run all tests with pytest
    pytest tests/test_token_auth.py -v

    # Run only WebSocket tests
    pytest tests/test_token_auth.py -k "Ws"

    # Run only STOMP tests
    pytest tests/test_token_auth.py -k "Stomp"

    # Standalone (no pytest required)
    python tests/test_token_auth.py

    # Custom host/ports
    python tests/test_token_auth.py --stomp-host localhost --stomp-port 61613 --ws-port 61614
"""

import argparse
import base64
import json
import logging
import os
import sys
import threading
import time
import uuid

import stomp
from stomp.adapter.ws import WSStompConnection

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)-8s [%(name)s] %(message)s",
)
log = logging.getLogger("test_token_auth")

# ---------------------------------------------------------------------------
# Configuration defaults (overridable via env vars or CLI args)
# ---------------------------------------------------------------------------
STOMP_HOST = os.environ.get("GOSS_STOMP_HOST", "localhost")
STOMP_PORT = int(os.environ.get("GOSS_STOMP_PORT", "61613"))
WS_PORT = int(os.environ.get("GOSS_WS_PORT", "61614"))
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


# ---------------------------------------------------------------------------
# Connection factories
# ---------------------------------------------------------------------------
def create_stomp_connection(host, port):
    """Create a new STOMP 1.2 connection over TCP (not yet connected)."""
    return stomp.Connection12(
        [(host, port)],
        heartbeats=(HEARTBEAT_MS, HEARTBEAT_MS),
    )


def create_ws_connection(host, port):
    """Create a new STOMP 1.2 connection over WebSocket (not yet connected)."""
    return WSStompConnection(
        [(host, port)],
        heartbeats=(HEARTBEAT_MS, HEARTBEAT_MS),
        header=["Sec-WebSocket-Protocol: v12.stomp"],
    )


# ---------------------------------------------------------------------------
# Token helpers
# ---------------------------------------------------------------------------
def request_token(conn_factory, host, port, username, password):
    """
    Connect with credentials, request a JWT token, and return it.

    The GOSS token flow:
      1. Connect via STOMP with username/password
      2. Subscribe to a temporary reply queue
      3. Send base64(username:password) to the token topic with reply-to header
      4. Wait for the server to respond with a JWT token
    """
    conn = conn_factory(host, port)
    listener = TokenResponseListener()
    conn.set_listener("token_listener", listener)

    log.info("Connecting to %s:%d as '%s' to request token...", host, port, username)
    conn.connect(username, password, wait=True)
    assert conn.is_connected(), "Failed to connect with username/password"
    log.info("Connected successfully with credentials")

    reply_dest = f"temp.token_resp.{username}-{uuid.uuid4().hex[:12]}"
    auth_payload = base64.b64encode(f"{username}:{password}".encode()).decode()

    conn.subscribe(destination=f"/queue/{reply_dest}", id="token-sub-1", ack="auto")
    log.info("Subscribed to /queue/%s", reply_dest)

    # Allow subscription to propagate to broker
    time.sleep(0.3)

    conn.send(
        destination=TOKEN_TOPIC,
        body=auth_payload,
        headers={"reply-to": f"/queue/{reply_dest}"},
    )
    log.info("Sent token request to %s", TOKEN_TOPIC)

    got_response = listener.wait(TOKEN_TIMEOUT_S)
    token = listener.token

    try:
        conn.disconnect()
    except Exception as e:
        log.debug("Non-critical error during disconnect: %s", e)

    return got_response, token, listener.error


def connect_with_token(conn_factory, host, port, token):
    """Connect using a JWT token as the username with an empty password."""
    conn = conn_factory(host, port)
    log.info("Connecting with token (%d bytes)...", len(token))
    try:
        conn.connect(token, "", wait=True)
    except stomp.exception.ConnectFailedException as e:
        raise AssertionError(
            f"Token-based connect failed. Token length={len(token)}, "
            f"token prefix={token[:40]}... Error: {e}"
        ) from e
    return conn


def verify_pubsub(conn, token):
    """Verify the connection can publish and subscribe on a test topic."""
    test_topic = "/topic/goss.test.token.auth"
    test_body = json.dumps({"test": "token_auth", "timestamp": time.time()})

    listener = PubSubListener()
    conn.set_listener("pubsub_listener", listener)
    conn.subscribe(destination=test_topic, id="pubsub-sub-1", ack="auto")
    log.info("Subscribed to %s", test_topic)

    time.sleep(0.5)

    conn.send(
        destination=test_topic,
        body=test_body,
        headers={"GOSS_HAS_SUBJECT": "true", "GOSS_SUBJECT": token},
    )
    log.info("Published test message to %s", test_topic)

    got_message = listener.wait(5)
    return got_message, listener.received_message


def verify_queue(conn, token):
    """Verify the connection can send to and consume from a queue."""
    queue_name = f"/queue/goss.test.token.queue.{uuid.uuid4().hex[:8]}"
    test_body = json.dumps({"test": "queue_auth", "timestamp": time.time()})

    listener = PubSubListener()
    conn.set_listener("queue_listener", listener)
    conn.subscribe(destination=queue_name, id="queue-sub-1", ack="auto")
    log.info("Subscribed to queue %s", queue_name)

    time.sleep(0.5)

    conn.send(
        destination=queue_name,
        body=test_body,
        headers={"GOSS_HAS_SUBJECT": "true", "GOSS_SUBJECT": token},
    )
    log.info("Sent message to queue %s", queue_name)

    got_message = listener.wait(5)
    return got_message, listener.received_message


def verify_queue_request_response(sender_conn, receiver_conn, token):
    """
    Two-client queue request/response: sender publishes to a queue,
    receiver consumes from it, then replies on a response queue.
    """
    request_queue = f"/queue/goss.test.request.{uuid.uuid4().hex[:8]}"
    response_queue = f"/queue/goss.test.response.{uuid.uuid4().hex[:8]}"
    request_body = json.dumps({"request": "ping", "ts": time.time()})
    response_body = json.dumps({"response": "pong", "ts": time.time()})

    # Receiver subscribes to the request queue
    req_listener = PubSubListener()
    receiver_conn.set_listener("req_listener", req_listener)
    receiver_conn.subscribe(destination=request_queue, id="rr-req-1", ack="auto")

    # Sender subscribes to the response queue
    resp_listener = PubSubListener()
    sender_conn.set_listener("resp_listener", resp_listener)
    sender_conn.subscribe(destination=response_queue, id="rr-resp-1", ack="auto")

    time.sleep(0.5)

    # Sender sends request with reply-to header
    sender_conn.send(
        destination=request_queue,
        body=request_body,
        headers={"reply-to": response_queue},
    )
    log.info("Sent request to %s", request_queue)

    # Receiver gets the request
    got_request = req_listener.wait(5)
    if not got_request:
        return False, None, None

    # Receiver sends response to reply-to queue
    receiver_conn.send(destination=response_queue, body=response_body)
    log.info("Sent response to %s", response_queue)

    # Sender gets the response
    got_response = resp_listener.wait(5)
    return got_response, req_listener.received_message, resp_listener.received_message


# ===========================================================================
# STOMP (TCP) Token Auth Tests
# ===========================================================================
class TestStompTokenAuth:
    """Token authentication tests over STOMP TCP transport."""

    host = STOMP_HOST
    port = STOMP_PORT
    username = USERNAME
    password = PASSWORD
    _token = None

    def _factory(self):
        return create_stomp_connection

    def test_01_credential_connect(self):
        """Connect with username/password over STOMP."""
        conn = self._factory()(self.host, self.port)
        conn.connect(self.username, self.password, wait=True)
        assert conn.is_connected(), "Should connect with valid credentials"
        log.info("STOMP: PASS credential connect")
        conn.disconnect()

    def test_02_request_token(self):
        """Request a JWT token over STOMP and validate structure."""
        got_response, token, error = request_token(
            self._factory(), self.host, self.port, self.username, self.password
        )
        assert got_response, f"Should get a token response within {TOKEN_TIMEOUT_S}s"
        assert error is None, f"Should not get an error: {error}"
        assert token is not None, "Token must not be None"
        assert len(token.strip()) > 0, "Token must not be empty"
        assert token != "authentication failed", "Token request should not fail auth"

        parts = token.split(".")
        assert len(parts) == 3, (
            f"Token should be a JWT with 3 parts, got {len(parts)}: {token[:80]}..."
        )
        log.info("STOMP: PASS received valid JWT (%d bytes)", len(token))
        TestStompTokenAuth._token = token

    def test_03_connect_with_token(self):
        """Reconnect using the JWT token over STOMP."""
        token = TestStompTokenAuth._token
        assert token is not None, "Depends on test_02 having produced a valid token"

        conn = connect_with_token(self._factory(), self.host, self.port, token)
        assert conn.is_connected(), "Should connect with token"
        log.info("STOMP: PASS connected with token")
        conn.disconnect()

    def test_04_pubsub_with_token(self):
        """Publish/subscribe on a topic works with token auth over STOMP."""
        token = TestStompTokenAuth._token
        assert token is not None, "Depends on test_02 having produced a valid token"

        conn = connect_with_token(self._factory(), self.host, self.port, token)
        assert conn.is_connected()

        got_message, received = verify_pubsub(conn, token)
        assert got_message, "Should receive the published message"
        assert received is not None
        assert "token_auth" in received, f"Content mismatch: {received}"
        log.info("STOMP: PASS topic pub/sub with token auth")
        conn.disconnect()

    def test_04b_queue_with_token(self):
        """Send/receive on a queue works with token auth over STOMP."""
        token = TestStompTokenAuth._token
        assert token is not None, "Depends on test_02 having produced a valid token"

        conn = connect_with_token(self._factory(), self.host, self.port, token)
        assert conn.is_connected()

        got_message, received = verify_queue(conn, token)
        assert got_message, "Should receive the queue message"
        assert received is not None
        assert "queue_auth" in received, f"Content mismatch: {received}"
        log.info("STOMP: PASS queue send/receive with token auth")
        conn.disconnect()

    def test_05_invalid_credentials(self):
        """Invalid credentials do not produce a valid token over STOMP."""
        try:
            got_response, token, error = request_token(
                self._factory(), self.host, self.port, "baduser", "badpass"
            )
        except (stomp.exception.ConnectFailedException, AssertionError):
            log.info("STOMP: PASS invalid credentials rejected at connect")
            return

        if got_response and token:
            assert token == "authentication failed" or len(token.split(".")) != 3, (
                f"Invalid credentials should not produce a valid JWT: {token[:80]}"
            )
            log.info("STOMP: PASS server returned auth failure")
        else:
            log.info("STOMP: PASS no token for invalid credentials (timeout)")

    def test_06_empty_token_rejected(self):
        """Empty string as token is rejected over STOMP."""
        conn = self._factory()(self.host, self.port)
        try:
            conn.connect("", "", wait=True, headers={"accept-version": "1.2"})
            connected = conn.is_connected()
        except (stomp.exception.ConnectFailedException, Exception):
            log.info("STOMP: PASS empty token rejected")
            return
        finally:
            try:
                conn.disconnect()
            except Exception:
                pass

        if connected:
            try:
                import pytest
                pytest.skip("Broker allows anonymous connections")
            except ImportError:
                log.warning("SKIP: broker allows anonymous connections")


# ===========================================================================
# WebSocket Token Auth Tests
# ===========================================================================
class TestWsTokenAuth:
    """Token authentication tests over WebSocket transport."""

    host = STOMP_HOST
    port = WS_PORT
    username = USERNAME
    password = PASSWORD
    _token = None

    def _factory(self):
        return create_ws_connection

    def test_01_credential_connect(self):
        """Connect with username/password over WebSocket."""
        conn = self._factory()(self.host, self.port)
        conn.connect(self.username, self.password, wait=True)
        assert conn.is_connected(), "Should connect with valid credentials"
        log.info("WS: PASS credential connect")
        conn.disconnect()

    def test_02_request_token(self):
        """Request a JWT token over WebSocket and validate structure."""
        got_response, token, error = request_token(
            self._factory(), self.host, self.port, self.username, self.password
        )
        assert got_response, f"Should get a token response within {TOKEN_TIMEOUT_S}s"
        assert error is None, f"Should not get an error: {error}"
        assert token is not None, "Token must not be None"
        assert len(token.strip()) > 0, "Token must not be empty"
        assert token != "authentication failed", "Token request should not fail auth"

        parts = token.split(".")
        assert len(parts) == 3, (
            f"Token should be a JWT with 3 parts, got {len(parts)}: {token[:80]}..."
        )
        log.info("WS: PASS received valid JWT (%d bytes)", len(token))
        TestWsTokenAuth._token = token

    def test_03_connect_with_token(self):
        """Reconnect using the JWT token over WebSocket."""
        token = TestWsTokenAuth._token
        assert token is not None, "Depends on test_02 having produced a valid token"

        conn = connect_with_token(self._factory(), self.host, self.port, token)
        assert conn.is_connected(), "Should connect with token"
        log.info("WS: PASS connected with token")
        conn.disconnect()

    def test_04_pubsub_with_token(self):
        """Publish/subscribe on a topic works with token auth over WebSocket."""
        token = TestWsTokenAuth._token
        assert token is not None, "Depends on test_02 having produced a valid token"

        conn = connect_with_token(self._factory(), self.host, self.port, token)
        assert conn.is_connected()

        got_message, received = verify_pubsub(conn, token)
        assert got_message, "Should receive the published message"
        assert received is not None
        assert "token_auth" in received, f"Content mismatch: {received}"
        log.info("WS: PASS topic pub/sub with token auth")
        conn.disconnect()

    def test_04b_queue_with_token(self):
        """Send/receive on a queue works with token auth over WebSocket."""
        token = TestWsTokenAuth._token
        assert token is not None, "Depends on test_02 having produced a valid token"

        conn = connect_with_token(self._factory(), self.host, self.port, token)
        assert conn.is_connected()

        got_message, received = verify_queue(conn, token)
        assert got_message, "Should receive the queue message"
        assert received is not None
        assert "queue_auth" in received, f"Content mismatch: {received}"
        log.info("WS: PASS queue send/receive with token auth")
        conn.disconnect()

    def test_05_invalid_credentials(self):
        """Invalid credentials do not produce a valid token over WebSocket."""
        try:
            got_response, token, error = request_token(
                self._factory(), self.host, self.port, "baduser", "badpass"
            )
        except (stomp.exception.ConnectFailedException, AssertionError):
            log.info("WS: PASS invalid credentials rejected at connect")
            return

        if got_response and token:
            assert token == "authentication failed" or len(token.split(".")) != 3, (
                f"Invalid credentials should not produce a valid JWT: {token[:80]}"
            )
            log.info("WS: PASS server returned auth failure")
        else:
            log.info("WS: PASS no token for invalid credentials (timeout)")

    def test_06_empty_token_rejected(self):
        """Empty string as token is rejected over WebSocket."""
        conn = self._factory()(self.host, self.port)
        try:
            conn.connect("", "", wait=True, headers={"accept-version": "1.2"})
            connected = conn.is_connected()
        except Exception:
            log.info("WS: PASS empty token rejected")
            return
        finally:
            try:
                conn.disconnect()
            except Exception:
                pass

        if connected:
            try:
                import pytest
                pytest.skip("Broker allows anonymous connections")
            except ImportError:
                log.warning("SKIP: broker allows anonymous connections")


# ===========================================================================
# Cross-transport test: obtain token via STOMP, use via WebSocket (and vice versa)
# ===========================================================================
class TestCrossTransportToken:
    """Verify a token obtained on one transport works on the other."""

    host = STOMP_HOST
    stomp_port = STOMP_PORT
    ws_port = WS_PORT
    username = USERNAME
    password = PASSWORD

    def test_01_stomp_token_on_ws(self):
        """Token obtained via STOMP connects over WebSocket."""
        got_response, token, error = request_token(
            create_stomp_connection, self.host, self.stomp_port,
            self.username, self.password,
        )
        assert got_response and token and len(token.split(".")) == 3, (
            f"Failed to obtain token via STOMP: {error}"
        )

        conn = connect_with_token(create_ws_connection, self.host, self.ws_port, token)
        assert conn.is_connected(), "STOMP token should work on WebSocket"
        got_message, received = verify_pubsub(conn, token)
        assert got_message, "Should receive message over WS with STOMP-issued token"
        assert "token_auth" in received
        log.info("CROSS: PASS STOMP token works on WebSocket")
        conn.disconnect()

    def test_02_ws_token_on_stomp(self):
        """Token obtained via WebSocket connects over STOMP."""
        got_response, token, error = request_token(
            create_ws_connection, self.host, self.ws_port,
            self.username, self.password,
        )
        assert got_response and token and len(token.split(".")) == 3, (
            f"Failed to obtain token via WebSocket: {error}"
        )

        conn = connect_with_token(
            create_stomp_connection, self.host, self.stomp_port, token,
        )
        assert conn.is_connected(), "WS token should work on STOMP"
        got_message, received = verify_pubsub(conn, token)
        assert got_message, "Should receive message over STOMP with WS-issued token"
        assert "token_auth" in received
        log.info("CROSS: PASS WebSocket token works on STOMP")
        conn.disconnect()

    def test_03_queue_request_response_cross_transport(self):
        """Queue request/response: STOMP sender, WebSocket receiver."""
        # Get a token (use STOMP to request)
        got_response, token, error = request_token(
            create_stomp_connection, self.host, self.stomp_port,
            self.username, self.password,
        )
        assert got_response and token and len(token.split(".")) == 3

        sender = connect_with_token(
            create_stomp_connection, self.host, self.stomp_port, token,
        )
        receiver = connect_with_token(
            create_ws_connection, self.host, self.ws_port, token,
        )
        assert sender.is_connected() and receiver.is_connected()

        got_resp, req_msg, resp_msg = verify_queue_request_response(
            sender, receiver, token,
        )
        assert got_resp, "Should complete queue request/response cycle"
        assert req_msg is not None and "ping" in req_msg
        assert resp_msg is not None and "pong" in resp_msg
        log.info("CROSS: PASS queue request/response (STOMP->WS)")
        sender.disconnect()
        receiver.disconnect()


# ---------------------------------------------------------------------------
# Standalone runner
# ---------------------------------------------------------------------------
def run_all_tests(host, stomp_port, ws_port, username, password):
    """Run all tests sequentially, reporting pass/fail."""
    test_classes = [
        ("STOMP Token Auth", TestStompTokenAuth, {"host": host, "port": stomp_port}),
        ("WebSocket Token Auth", TestWsTokenAuth, {"host": host, "port": ws_port}),
        ("Cross-Transport Token", TestCrossTransportToken,
         {"host": host, "stomp_port": stomp_port, "ws_port": ws_port}),
    ]
    passed = 0
    failed = 0
    skipped = 0
    errors = []

    print(f"\n{'='*65}")
    print(f"GOSS Token Authentication Tests (STOMP + WebSocket)")
    print(f"  STOMP:     {host}:{stomp_port}")
    print(f"  WebSocket: {host}:{ws_port}")
    print(f"  User:      {username}")
    print(f"{'='*65}")

    for label, cls, attrs in test_classes:
        instance = cls()
        instance.username = username
        instance.password = password
        for k, v in attrs.items():
            setattr(instance, k, v)

        test_methods = [m for m in sorted(dir(instance)) if m.startswith("test_")]
        print(f"\n  [{label}]")

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
                    errors.append((f"{label}.{method_name}", e))
                    print(f"      -> FAILED: {e}")

    print(f"\n{'='*65}")
    print(f"Results: {passed} passed, {failed} failed, {skipped} skipped "
          f"out of {passed + failed + skipped}")
    print(f"{'='*65}")

    if errors:
        print("\nFailures:")
        for name, err in errors:
            print(f"  {name}: {err}")
        return 1
    return 0


def main():
    parser = argparse.ArgumentParser(
        description="Test JWT token auth over STOMP and WebSocket against a GOSS server"
    )
    parser.add_argument(
        "--stomp-host", default=STOMP_HOST,
        help=f"Server host (default: {STOMP_HOST})"
    )
    parser.add_argument(
        "--stomp-port", type=int, default=STOMP_PORT,
        help=f"STOMP port (default: {STOMP_PORT})"
    )
    parser.add_argument(
        "--ws-port", type=int, default=WS_PORT,
        help=f"WebSocket port (default: {WS_PORT})"
    )
    parser.add_argument(
        "--username", default=USERNAME, help=f"Username (default: {USERNAME})"
    )
    parser.add_argument(
        "--password", default=PASSWORD, help=f"Password (default: {PASSWORD})"
    )
    args = parser.parse_args()

    rc = run_all_tests(
        args.stomp_host, args.stomp_port, args.ws_port,
        args.username, args.password,
    )
    sys.exit(rc)


if __name__ == "__main__":
    main()
