"""
WebSocket STOMP Power Grid Model Query Test

Integration test that exercises the full flow a JavaScript frontend would use:
  1. Connect via STOMP over WebSocket
  2. Obtain a JWT token
  3. Send a power grid model query with the JWT in GOSS_SUBJECT
  4. Verify the response contains feeder model data

This mirrors the exact STOMP frame a browser client sends:

    >>> SEND
    destination:goss.gridappsd.process.request.data.powergridmodel
    GOSS_HAS_SUBJECT:true
    GOSS_SUBJECT:<jwt-token>
    reply-to:feeder-models
    content-length:701

Requires a running GridAPPS-D platform with Blazegraph loaded with CIM models.

Usage:
    pytest tests/test_ws_powergrid_query.py -v
    python tests/test_ws_powergrid_query.py
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
log = logging.getLogger("test_ws_powergrid_query")

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
STOMP_HOST = os.environ.get("GOSS_STOMP_HOST", "localhost")
STOMP_PORT = int(os.environ.get("GOSS_STOMP_PORT", "61613"))
WS_PORT = int(os.environ.get("GOSS_WS_PORT", "61614"))
USERNAME = os.environ.get("GOSS_USERNAME", "system")
PASSWORD = os.environ.get("GOSS_PASSWORD", "manager")
TOKEN_TOPIC = "/topic/pnnl.goss.token.topic"
POWERGRID_TOPIC = "goss.gridappsd.process.request.data.powergridmodel"
TOKEN_TIMEOUT_S = 10
QUERY_TIMEOUT_S = 15
HEARTBEAT_MS = 10000

# The SPARQL query for feeder models (same as sent by the JS frontend)
FEEDER_QUERY = json.dumps({
    "requestType": "QUERY",
    "resultFormat": "JSON",
    "queryString": (
        "SELECT ?name ?mRID ?substationName ?substationID "
        "?subregionName ?subregionID ?regionName ?regionID WHERE {"
        "?s r:type c:Feeder."
        "?s c:IdentifiedObject.name ?name."
        "?s c:IdentifiedObject.mRID ?mRID."
        "?s c:Feeder.NormalEnergizingSubstation ?subStation."
        "?subStation c:IdentifiedObject.name ?substationName."
        "?subStation c:IdentifiedObject.mRID ?substationID."
        "?subStation c:Substation.Region ?subRegion."
        "?subRegion c:IdentifiedObject.name ?subregionName."
        "?subRegion c:IdentifiedObject.mRID ?subregionID."
        "?subRegion c:SubGeographicalRegion.Region ?region."
        "?region c:IdentifiedObject.name ?regionName."
        "?region c:IdentifiedObject.mRID ?regionID."
        "}  ORDER by ?name "
    ),
})


# ---------------------------------------------------------------------------
# Listeners
# ---------------------------------------------------------------------------
class ResponseListener(stomp.ConnectionListener):
    """Listens for a response message on a reply-to destination."""

    def __init__(self):
        self.response = None
        self.headers = None
        self.error = None
        self._event = threading.Event()

    def on_message(self, frame):
        self.headers = frame.headers if hasattr(frame, "headers") else {}
        self.response = frame.body if hasattr(frame, "body") else str(frame)
        log.info("Response received (%d bytes)", len(self.response))
        self._event.set()

    def on_error(self, frame):
        body = frame.body if hasattr(frame, "body") else str(frame)
        log.error("STOMP error: %s", body)
        self.error = body
        self._event.set()

    def wait(self, timeout):
        return self._event.wait(timeout)


# ---------------------------------------------------------------------------
# Connection factories
# ---------------------------------------------------------------------------
def create_stomp_connection(host, port):
    """Create a STOMP 1.2 connection over TCP."""
    return stomp.Connection12(
        [(host, port)],
        heartbeats=(HEARTBEAT_MS, HEARTBEAT_MS),
    )


def create_ws_connection(host, port):
    """Create a STOMP 1.2 connection over WebSocket."""
    return WSStompConnection(
        [(host, port)],
        heartbeats=(HEARTBEAT_MS, HEARTBEAT_MS),
        header=["Sec-WebSocket-Protocol: v12.stomp"],
    )


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
def request_token(conn_factory, host, port, username, password):
    """Connect with credentials, request a JWT token, return it."""
    conn = conn_factory(host, port)
    listener = ResponseListener()
    conn.set_listener("token_listener", listener)

    conn.connect(username, password, wait=True)
    assert conn.is_connected(), "Failed to connect with credentials"

    reply_dest = f"temp.token_resp.{username}-{uuid.uuid4().hex[:12]}"
    auth_payload = base64.b64encode(f"{username}:{password}".encode()).decode()

    conn.subscribe(destination=f"/queue/{reply_dest}", id="token-sub-1", ack="auto")
    time.sleep(0.3)

    conn.send(
        destination=TOKEN_TOPIC,
        body=auth_payload,
        headers={"reply-to": f"/queue/{reply_dest}"},
    )
    log.info("Sent token request to %s", TOKEN_TOPIC)

    got_response = listener.wait(TOKEN_TIMEOUT_S)
    token = listener.response

    try:
        conn.disconnect()
    except Exception:
        pass

    return got_response, token, listener.error


def send_powergrid_query(conn_factory, host, port, token, query_body,
                         reply_to="feeder-models"):
    """
    Send a power grid model query using JWT auth, return the response.

    This replicates the exact STOMP frame a JS frontend sends:
        destination: goss.gridappsd.process.request.data.powergridmodel
        GOSS_HAS_SUBJECT: true
        GOSS_SUBJECT: <jwt-token>
        reply-to: <reply-destination>
    """
    conn = conn_factory(host, port)
    listener = ResponseListener()
    conn.set_listener("query_listener", listener)

    # Connect with the JWT token
    conn.connect(token, "", wait=True)
    assert conn.is_connected(), "Failed to connect with JWT token"
    log.info("Connected with JWT token")

    # Subscribe to the reply destination
    reply_dest = f"/queue/{reply_to}-{uuid.uuid4().hex[:8]}"
    conn.subscribe(destination=reply_dest, id="query-sub-1", ack="auto")
    time.sleep(0.3)

    # Send the query - same headers as the JS frontend
    conn.send(
        destination=POWERGRID_TOPIC,
        body=query_body,
        headers={
            "GOSS_HAS_SUBJECT": "true",
            "GOSS_SUBJECT": token,
            "reply-to": reply_dest,
        },
    )
    log.info("Sent powergrid query to %s (reply-to: %s)", POWERGRID_TOPIC, reply_dest)

    got_response = listener.wait(QUERY_TIMEOUT_S)
    response = listener.response

    try:
        conn.disconnect()
    except Exception:
        pass

    return got_response, response, listener.error


# ===========================================================================
# Test cases: WebSocket STOMP power grid model queries
# ===========================================================================
class TestWsPowergridQuery:
    """Power grid model queries over WebSocket STOMP with JWT auth."""

    host = STOMP_HOST
    port = WS_PORT
    username = USERNAME
    password = PASSWORD
    _token = None

    def test_01_obtain_token_via_ws(self):
        """Obtain a JWT token over WebSocket for subsequent queries."""
        got_response, token, error = request_token(
            create_ws_connection, self.host, self.port, self.username, self.password
        )
        assert got_response, f"Should get token response within {TOKEN_TIMEOUT_S}s"
        assert error is None, f"Token request error: {error}"
        assert token is not None and len(token.strip()) > 0, "Token must not be empty"
        parts = token.split(".")
        assert len(parts) == 3, f"Expected JWT with 3 parts, got: {token[:80]}..."
        log.info("WS: obtained JWT token (%d bytes)", len(token))
        TestWsPowergridQuery._token = token

    def test_02_query_feeder_models_via_ws(self):
        """Send feeder model SPARQL query over WebSocket and get results."""
        token = TestWsPowergridQuery._token
        assert token is not None, "Depends on test_01 for JWT token"

        got_response, response, error = send_powergrid_query(
            create_ws_connection, self.host, self.port, token, FEEDER_QUERY,
        )
        assert got_response, f"Should get query response within {QUERY_TIMEOUT_S}s"
        assert error is None, f"Query error: {error}"
        assert response is not None, "Response must not be None"

        # Parse the response as JSON
        data = json.loads(response)
        log.info("WS: received powergrid response: %s", json.dumps(data)[:500])

        # Verify response structure - should contain feeder data
        assert "data" in data or "results" in data or "head" in data, (
            f"Expected query results in response, got keys: {list(data.keys())}"
        )

    def test_03_query_response_contains_feeders(self):
        """Feeder query response contains model names and mRIDs."""
        token = TestWsPowergridQuery._token
        assert token is not None, "Depends on test_01 for JWT token"

        got_response, response, error = send_powergrid_query(
            create_ws_connection, self.host, self.port, token, FEEDER_QUERY,
        )
        assert got_response and response is not None

        data = json.loads(response)
        response_str = json.dumps(data)

        # The response should contain feeder-related data
        assert "name" in response_str or "mRID" in response_str or "modelName" in response_str, (
            f"Expected feeder data (name/mRID) in response: {response_str[:500]}"
        )
        log.info("WS: feeder query response contains expected fields")

    def test_04_query_object_types(self):
        """Query object types via WebSocket returns CIM types."""
        token = TestWsPowergridQuery._token
        assert token is not None, "Depends on test_01 for JWT token"

        query = json.dumps({
            "requestType": "QUERY_OBJECT_TYPES",
            "resultFormat": "JSON",
            "modelId": "",
        })

        got_response, response, error = send_powergrid_query(
            create_ws_connection, self.host, self.port, token, query,
            reply_to="object-types",
        )
        assert got_response, f"Should get response within {QUERY_TIMEOUT_S}s"
        assert error is None, f"Query error: {error}"
        assert response is not None

        data = json.loads(response)
        log.info("WS: object types response: %s", json.dumps(data)[:500])

    def test_05_query_model_info(self):
        """Query model info via WebSocket returns available models."""
        token = TestWsPowergridQuery._token
        assert token is not None, "Depends on test_01 for JWT token"

        query = json.dumps({
            "requestType": "QUERY_MODEL_INFO",
            "resultFormat": "JSON",
        })

        got_response, response, error = send_powergrid_query(
            create_ws_connection, self.host, self.port, token, query,
            reply_to="model-info",
        )
        assert got_response, f"Should get response within {QUERY_TIMEOUT_S}s"
        assert error is None, f"Query error: {error}"
        assert response is not None

        data = json.loads(response)
        log.info("WS: model info response: %s", json.dumps(data)[:500])

        # Should contain model information
        response_str = json.dumps(data)
        assert "models" in response_str or "modelName" in response_str or "data" in data, (
            f"Expected model info in response: {response_str[:500]}"
        )


# ===========================================================================
# Test cases: STOMP TCP power grid model queries (same tests, different transport)
# ===========================================================================
class TestStompPowergridQuery:
    """Power grid model queries over STOMP TCP with JWT auth."""

    host = STOMP_HOST
    port = STOMP_PORT
    username = USERNAME
    password = PASSWORD
    _token = None

    def test_01_obtain_token_via_stomp(self):
        """Obtain a JWT token over STOMP TCP for subsequent queries."""
        got_response, token, error = request_token(
            create_stomp_connection, self.host, self.port, self.username, self.password
        )
        assert got_response, f"Should get token response within {TOKEN_TIMEOUT_S}s"
        assert error is None, f"Token request error: {error}"
        assert token is not None and len(token.strip()) > 0
        assert len(token.split(".")) == 3, f"Expected JWT, got: {token[:80]}..."
        log.info("STOMP: obtained JWT token (%d bytes)", len(token))
        TestStompPowergridQuery._token = token

    def test_02_query_feeder_models_via_stomp(self):
        """Send feeder model SPARQL query over STOMP TCP and get results."""
        token = TestStompPowergridQuery._token
        assert token is not None, "Depends on test_01 for JWT token"

        got_response, response, error = send_powergrid_query(
            create_stomp_connection, self.host, self.port, token, FEEDER_QUERY,
        )
        assert got_response, f"Should get query response within {QUERY_TIMEOUT_S}s"
        assert error is None, f"Query error: {error}"
        assert response is not None

        data = json.loads(response)
        log.info("STOMP: received powergrid response: %s", json.dumps(data)[:500])

        assert "data" in data or "results" in data or "head" in data, (
            f"Expected query results, got keys: {list(data.keys())}"
        )

    def test_03_query_model_names(self):
        """Query model names over STOMP TCP returns results."""
        token = TestStompPowergridQuery._token
        assert token is not None, "Depends on test_01 for JWT token"

        query = json.dumps({
            "requestType": "QUERY_MODEL_NAMES",
            "resultFormat": "JSON",
            "modelId": "",
        })

        got_response, response, error = send_powergrid_query(
            create_stomp_connection, self.host, self.port, token, query,
            reply_to="model-names",
        )
        assert got_response, f"Should get response within {QUERY_TIMEOUT_S}s"
        assert error is None, f"Query error: {error}"
        assert response is not None
        log.info("STOMP: model names response: %s", json.dumps(json.loads(response))[:500])


# ===========================================================================
# Cross-transport: token from STOMP, query via WebSocket
# ===========================================================================
class TestCrossTransportPowergridQuery:
    """Obtain token on one transport, query on another."""

    host = STOMP_HOST
    stomp_port = STOMP_PORT
    ws_port = WS_PORT
    username = USERNAME
    password = PASSWORD

    def test_01_stomp_token_ws_query(self):
        """Token from STOMP TCP works for WebSocket power grid query."""
        # Get token via STOMP
        got_response, token, error = request_token(
            create_stomp_connection, self.host, self.stomp_port,
            self.username, self.password,
        )
        assert got_response and token and len(token.split(".")) == 3, (
            f"Failed to obtain token via STOMP: {error}"
        )

        # Query via WebSocket using STOMP-issued token
        got_response, response, error = send_powergrid_query(
            create_ws_connection, self.host, self.ws_port, token, FEEDER_QUERY,
        )
        assert got_response, "Should get query response via WS with STOMP token"
        assert response is not None
        data = json.loads(response)
        log.info("CROSS: STOMP token -> WS query succeeded: %s", json.dumps(data)[:300])

    def test_02_ws_token_stomp_query(self):
        """Token from WebSocket works for STOMP TCP power grid query."""
        # Get token via WebSocket
        got_response, token, error = request_token(
            create_ws_connection, self.host, self.ws_port,
            self.username, self.password,
        )
        assert got_response and token and len(token.split(".")) == 3, (
            f"Failed to obtain token via WebSocket: {error}"
        )

        # Query via STOMP using WS-issued token
        got_response, response, error = send_powergrid_query(
            create_stomp_connection, self.host, self.stomp_port, token, FEEDER_QUERY,
        )
        assert got_response, "Should get query response via STOMP with WS token"
        assert response is not None
        data = json.loads(response)
        log.info("CROSS: WS token -> STOMP query succeeded: %s", json.dumps(data)[:300])


# ---------------------------------------------------------------------------
# Standalone runner
# ---------------------------------------------------------------------------
def run_all_tests(host, stomp_port, ws_port, username, password):
    """Run all tests sequentially, reporting pass/fail."""
    test_classes = [
        ("WebSocket Powergrid Query", TestWsPowergridQuery,
         {"host": host, "port": ws_port}),
        ("STOMP Powergrid Query", TestStompPowergridQuery,
         {"host": host, "port": stomp_port}),
        ("Cross-Transport Powergrid Query", TestCrossTransportPowergridQuery,
         {"host": host, "stomp_port": stomp_port, "ws_port": ws_port}),
    ]
    passed = 0
    failed = 0
    errors = []

    print(f"\n{'='*65}")
    print(f"Power Grid Model Query Tests (STOMP + WebSocket)")
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
                failed += 1
                errors.append((f"{label}.{method_name}", e))
                print(f"      -> FAILED: {e}")

    print(f"\n{'='*65}")
    print(f"Results: {passed} passed, {failed} failed out of {passed + failed}")
    print(f"{'='*65}")

    if errors:
        print("\nFailures:")
        for name, err in errors:
            print(f"  {name}: {err}")
        return 1
    return 0


def main():
    parser = argparse.ArgumentParser(
        description="Test power grid model queries over STOMP and WebSocket"
    )
    parser.add_argument("--stomp-host", default=STOMP_HOST, help=f"Host (default: {STOMP_HOST})")
    parser.add_argument("--stomp-port", type=int, default=STOMP_PORT, help=f"STOMP port (default: {STOMP_PORT})")
    parser.add_argument("--ws-port", type=int, default=WS_PORT, help=f"WebSocket port (default: {WS_PORT})")
    parser.add_argument("--username", default=USERNAME, help=f"Username (default: {USERNAME})")
    parser.add_argument("--password", default=PASSWORD, help=f"Password (default: {PASSWORD})")
    args = parser.parse_args()

    rc = run_all_tests(args.stomp_host, args.stomp_port, args.ws_port, args.username, args.password)
    sys.exit(rc)


if __name__ == "__main__":
    main()
