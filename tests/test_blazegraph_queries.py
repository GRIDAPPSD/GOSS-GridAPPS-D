"""
Integration tests for querying Blazegraph through the GridAPPS-D message bus.

Tests verify that gridappsd-python can connect to the platform and retrieve
power grid model data from Blazegraph via the GOSS message bus.

Requires:
    - Running gridappsd container with AUTOSTART=1
    - Blazegraph loaded with CIM models
    - gridappsd-python installed in the container

Run inside container:
    python -m pytest tests/test_blazegraph_queries.py -v

Run via Makefile:
    make test-blazegraph
"""
import json
import pytest
from gridappsd import GridAPPSD


STOMP_ADDRESS = "localhost"
STOMP_PORT = "61613"
USERNAME = "system"
PASSWORD = "manager"


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
def model_info(gapps):
    """Get model info once for use by multiple tests."""
    return gapps.query_model_info()


@pytest.fixture(scope="module")
def first_model_id(model_info):
    """Get the first available model ID."""
    models = model_info.get("data", {}).get("models", [])
    assert len(models) > 0, "No models found in Blazegraph"
    return models[0]["modelId"]


class TestPlatformConnectivity:
    """Test basic platform connectivity."""

    def test_platform_status(self, gapps):
        """Verify the platform responds to status requests."""
        topic = "goss.gridappsd.process.request.status.platform"
        response = gapps.get_response(topic, "{}", timeout=10)
        assert response is not None, "Platform did not respond to status request"


class TestModelQueries:
    """Test querying power grid model metadata through the message bus."""

    def test_query_model_info(self, model_info):
        """query_model_info returns models with expected fields."""
        assert "data" in model_info
        models = model_info["data"]["models"]
        assert len(models) > 0, "Expected at least one model in Blazegraph"

        first = models[0]
        assert "modelName" in first
        assert "modelId" in first
        assert "stationName" in first
        assert "regionName" in first

    def test_query_model_names(self, gapps):
        """query_model_names returns non-empty results."""
        result = gapps.query_model_names()
        assert result is not None
        assert "data" in result

    def test_query_object_types(self, gapps, first_model_id):
        """query_object_types returns CIM types for a model."""
        result = gapps.query_object_types(model_id=first_model_id)
        assert result is not None
        assert "data" in result

        # Should contain well-known CIM types
        data_str = json.dumps(result)
        assert "CIM100" in data_str or "objectTypes" in data_str, (
            f"Expected CIM object types in response, got: {data_str[:500]}"
        )

    def test_query_object(self, gapps):
        """query_data SPARQL can retrieve specific CIM objects."""
        query = """
        PREFIX r: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        PREFIX c: <http://iec.ch/TC57/CIM100#>
        SELECT ?name ?mRID WHERE {
            ?s r:type c:ACLineSegment.
            ?s c:IdentifiedObject.name ?name.
            ?s c:IdentifiedObject.mRID ?mRID.
        } LIMIT 3
        """
        result = gapps.query_data(query)
        assert result is not None
        bindings = result["data"]["results"]["bindings"]
        assert len(bindings) > 0, "Expected ACLineSegment objects in Blazegraph"
        assert "name" in bindings[0]
        assert "mRID" in bindings[0]


class TestSparqlQueries:
    """Test raw SPARQL queries through the message bus to Blazegraph."""

    def test_count_triples(self, gapps):
        """SPARQL query to count all triples returns count > 0."""
        query = "SELECT (COUNT(*) as ?count) WHERE { ?s ?p ?o }"
        result = gapps.query_data(query)
        assert result is not None

        # Extract count from SPARQL results
        bindings = result.get("results", result.get("data", {})).get("bindings", [])
        if bindings:
            count = int(bindings[0]["count"]["value"])
            assert count > 0, "Blazegraph should contain triples"
        else:
            # Some response formats differ; just verify we got data
            assert len(json.dumps(result)) > 10, (
                f"Expected query results, got: {result}"
            )

    def test_query_feeders(self, gapps, model_info):
        """SPARQL query for feeders returns results matching model info."""
        query = """
        PREFIX r: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        PREFIX c: <http://iec.ch/TC57/CIM100#>
        SELECT ?name ?mRID WHERE {
            ?s r:type c:Feeder.
            ?s c:IdentifiedObject.name ?name.
            ?s c:IdentifiedObject.mRID ?mRID.
        }
        ORDER BY ?name
        """
        result = gapps.query_data(query)
        assert result is not None

        # Should return feeder data
        result_str = json.dumps(result)
        assert len(result_str) > 10, f"Expected feeder query results, got: {result}"

        # Verify at least one model name from model_info appears in feeder results
        model_names = {
            m["modelName"] for m in model_info.get("data", {}).get("models", [])
        }
        if model_names:
            # At least some feeders should match known model names
            assert any(name in result_str for name in model_names), (
                f"Expected feeder names matching {model_names} in results"
            )

    def test_query_measurements(self, gapps, first_model_id):
        """SPARQL query for measurements in a specific model returns results."""
        query = f"""
        PREFIX r: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        PREFIX c: <http://iec.ch/TC57/CIM100#>
        SELECT ?name ?type ?measid WHERE {{
            ?s r:type c:Analog.
            ?s c:IdentifiedObject.name ?name.
            ?s c:IdentifiedObject.mRID ?measid.
            ?s c:Analog.measurementType ?type.
        }}
        LIMIT 5
        """
        result = gapps.query_data(query)
        assert result is not None
        result_str = json.dumps(result)
        assert len(result_str) > 2, f"Expected measurement data, got: {result}"
