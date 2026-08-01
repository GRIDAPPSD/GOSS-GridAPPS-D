"""
Integration tests for running a short simulation through GridAPPS-D.

Tests verify that gridappsd-python can start a simulation, receive log
messages, and (when co-simulation infrastructure is functional) receive
measurement output from GridLAB-D through the HELICS-GOSS bridge.

Requires:
    - Running gridappsd container with AUTOSTART=1
    - Blazegraph loaded with CIM models (IEEE 13 node or similar)
    - GridLAB-D and HELICS available in the container

Run inside container:
    python -m pytest tests/test_simulation.py -v

Run via Makefile:
    make test-simulation-python
"""
import json
import threading
import time

import pytest
from gridappsd import GridAPPSD
import gridappsd.topics as t


STOMP_ADDRESS = "localhost"
STOMP_PORT = "61613"
USERNAME = "system"
PASSWORD = "manager"

# IEEE 13 Node Test Feeder - smallest standard model
IEEE13_MRID = "49AD8E07-3BF9-A4E2-CB8F-C3722F837B62"

SIMULATION_DURATION = 120  # seconds


def build_simulation_request(model_mrid, duration=SIMULATION_DURATION):
    """Build a minimal simulation request for the given model."""
    return {
        "power_system_configs": [
            {
                "Line_name": model_mrid,
                "simulator_config": {
                    "simulator": "GridLAB-D",
                    "power_flow_solver_method": "NR",
                    "model_creation_config": {
                        "load_scaling_factor": 1.0,
                        "schedule_name": "",
                        "z_fraction": 0.0,
                        "i_fraction": 1.0,
                        "p_fraction": 0.0,
                        "randomize_zipload_fractions": False,
                        "use_houses": False,
                    },
                },
            }
        ],
        "simulation_config": {
            "start_time": "1655321830",
            "duration": duration,
            "timestep_frequency": 1000,
            "timestep_increment": 1000,
            "run_realtime": False,
            "simulation_name": "pytest_simulation_test",
        },
        "application_config": {"applications": []},
    }


def extract_sim_id(response):
    """Extract simulation ID from response, handling various formats."""
    if isinstance(response, str):
        response = json.loads(response)
    sim_id = response.get("simulationId") or response.get("data", "")
    return str(sim_id) if sim_id else None


@pytest.fixture(scope="module")
def gapps():
    """Create a GridAPPSD connection for the test module."""
    conn = GridAPPSD(
        username=USERNAME,
        password=PASSWORD,
        stomp_address=STOMP_ADDRESS,
        stomp_port=STOMP_PORT,
    )
    yield conn
    conn.disconnect()


@pytest.fixture(scope="module")
def model_mrid(gapps):
    """Get the MRID of a small model to simulate.

    Prefers IEEE 13 node if available, otherwise uses the first model.
    """
    info = gapps.query_model_info()
    models = info.get("data", {}).get("models", [])
    assert len(models) > 0, "No models found in Blazegraph"

    for m in models:
        if m["modelId"] == IEEE13_MRID:
            return IEEE13_MRID

    return models[0]["modelId"]


@pytest.fixture(scope="module")
def simulation_result(gapps, model_mrid):
    """Run a single simulation and collect logs + output for all tests.

    This fixture starts one simulation and subscribes to both log and output
    topics, sharing results across all tests in this module.
    """
    request = build_simulation_request(model_mrid)
    response = gapps.get_response(
        t.REQUEST_SIMULATION, json.dumps(request), timeout=60
    )
    assert response is not None, "No response from simulation request"

    sim_id = extract_sim_id(response)
    assert sim_id, f"Expected simulationId in response, got: {response}"

    # Subscribe to both log and output topics
    logs = []
    measurements = []
    output_event = threading.Event()
    complete_event = threading.Event()

    def on_output(headers, message):
        try:
            data = json.loads(message) if isinstance(message, str) else message
            measurements.append(data)
            output_event.set()
        except (json.JSONDecodeError, TypeError):
            pass

    def on_log(headers, message):
        try:
            data = json.loads(message) if isinstance(message, str) else message
            logs.append(data)
            msg_str = json.dumps(data).lower()
            if "simulation complete" in msg_str or "simulation finished" in msg_str:
                complete_event.set()
        except (json.JSONDecodeError, TypeError):
            pass

    output_topic = t.simulation_output_topic(sim_id)
    log_topic = t.simulation_log_topic(sim_id)
    gapps.subscribe(output_topic, on_output)
    gapps.subscribe(log_topic, on_log)

    # Wait for simulation to produce output or complete/fail
    # Non-realtime 30s sim typically takes 1-3 min wall clock
    complete_event.wait(timeout=300)

    # Give a bit more time for final messages
    time.sleep(5)

    return {
        "sim_id": sim_id,
        "logs": logs,
        "measurements": measurements,
        "got_output": output_event.is_set(),
        "completed": complete_event.is_set(),
    }


class TestSimulationStartup:
    """Test that simulations can be requested and the platform responds."""

    def test_start_simulation_returns_id(self, gapps, model_mrid):
        """Start a simulation and verify it returns a simulation ID."""
        request = build_simulation_request(model_mrid)
        response = gapps.get_response(
            t.REQUEST_SIMULATION, json.dumps(request), timeout=60
        )
        assert response is not None, "No response from simulation request"

        sim_id = extract_sim_id(response)
        assert sim_id, f"Expected simulationId in response, got: {response}"
        assert sim_id.isdigit(), f"Simulation ID should be numeric, got: {sim_id}"


class TestSimulationExecution:
    """Test simulation execution, log output, and measurement data."""

    def test_simulation_produces_logs(self, simulation_result):
        """Verify the simulation produces log messages."""
        logs = simulation_result["logs"]
        assert len(logs) > 0, (
            f"No log messages received for simulation {simulation_result['sim_id']}"
        )

        # Should see configuration generation logs at minimum
        log_sources = set()
        for log in logs:
            if isinstance(log, dict):
                log_sources.add(log.get("source", "unknown"))

        assert len(log_sources) > 0, "Expected log messages from platform components"

    def test_simulation_starts_services(self, simulation_result):
        """Verify that required services (HELICS, bridge) are started."""
        logs = simulation_result["logs"]
        log_text = " ".join(
            json.dumps(l) if isinstance(l, dict) else str(l) for l in logs
        )

        # HELICS broker and bridge should be started
        assert "helics" in log_text.lower(), (
            "Expected HELICS-related log messages during simulation startup"
        )

    def test_gridlabd_starts(self, simulation_result):
        """Verify that GridLAB-D simulator process is started."""
        logs = simulation_result["logs"]
        log_text = " ".join(
            json.dumps(l) if isinstance(l, dict) else str(l) for l in logs
        )

        assert "gridlab" in log_text.lower() or "simulator" in log_text.lower(), (
            "Expected GridLAB-D startup log messages"
        )

    def test_simulation_produces_measurements(self, simulation_result):
        """Verify measurement output is received from the simulation.

        This test requires fully functional co-simulation infrastructure
        (HELICS broker, HELICS-GOSS bridge, GridLAB-D with HELICS).
        If the co-simulation fails to initialize, the test reports
        the failure details from the simulation logs.
        """
        if not simulation_result["got_output"]:
            # Check logs for known failure patterns
            logs = simulation_result["logs"]
            errors = []
            for log in logs:
                if isinstance(log, dict):
                    level = log.get("logLevel", "")
                    msg = log.get("logMessage", "")
                    if level == "ERROR" or "FATAL" in str(msg):
                        errors.append(msg[:200])

            error_detail = "\n".join(errors) if errors else "No error details in logs"
            pytest.fail(
                f"No measurement output received for simulation "
                f"{simulation_result['sim_id']}.\n"
                f"Simulation errors:\n{error_detail}"
            )

        measurements = simulation_result["measurements"]
        assert len(measurements) > 0, "Measurement list is empty"

        # Verify measurement structure
        first = measurements[0]
        msg = json.dumps(first)
        assert len(msg) > 10, f"Unexpected measurement format: {msg[:500]}"
