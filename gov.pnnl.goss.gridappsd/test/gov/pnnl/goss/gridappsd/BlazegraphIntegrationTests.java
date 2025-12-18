/*******************************************************************************
 * Copyright (c) 2017, Battelle Memorial Institute All rights reserved.
 * Battelle Memorial Institute (hereinafter Battelle) hereby grants permission to any person or entity
 * lawfully obtaining a copy of this software and associated documentation files (hereinafter the
 * Software) to redistribute and use the Software in source and binary forms, with or without modification.
 * Such person or entity may use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and may permit others to do so, subject to the following conditions:
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the
 * following disclaimers.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided with the distribution.
 * Other than as used herein, neither the name Battelle Memorial Institute or Battelle may be used in any
 * form whatsoever without the express written consent of Battelle.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * BATTELLE OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * General disclaimer for use with OSS licenses
 *
 * This material was prepared as an account of work sponsored by an agency of the United States Government.
 * Neither the United States Government nor the United States Department of Energy, nor Battelle, nor any
 * of their employees, nor any jurisdiction or organization that has cooperated in the development of these
 * materials, makes any warranty, express or implied, or assumes any legal liability or responsibility for
 * the accuracy, completeness, or usefulness or any information, apparatus, product, software, or process
 * disclosed, or represents that its use would not infringe privately owned rights.
 *
 * Reference herein to any specific commercial product, process, or service by trade name, trademark, manufacturer,
 * or otherwise does not necessarily constitute or imply its endorsement, recommendation, or favoring by the United
 * States Government or any agency thereof, or Battelle Memorial Institute. The views and opinions of authors expressed
 * herein do not necessarily state or reflect those of the United States Government or any agency thereof.
 *
 * PACIFIC NORTHWEST NATIONAL LABORATORY operated by BATTELLE for the
 * UNITED STATES DEPARTMENT OF ENERGY under Contract DE-AC05-76RL01830
 ******************************************************************************/
package gov.pnnl.goss.gridappsd;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Integration tests for Blazegraph/SPARQL operations used by PowergridModelDataManager.
 *
 * These tests require Blazegraph to be running on localhost:8889 with power grid models loaded.
 *
 * To run: Ensure Blazegraph container is running (docker ps should show blazegraph container)
 *
 * Tests are skipped if Blazegraph is not available.
 */
public class BlazegraphIntegrationTests {

    private static final String BLAZEGRAPH_URL = "http://localhost:8889/bigdata/namespace/kb/sparql";
    private static boolean blazegraphAvailable = false;

    // CIM namespace prefixes used in queries
    private static final String CIM_PREFIX = "PREFIX r: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
            "PREFIX c: <http://iec.ch/TC57/CIM100#> " +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> ";

    @BeforeClass
    public static void checkBlazegraphAvailable() {
        try {
            URL url = URI.create("http://localhost:8889/bigdata/namespace").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int responseCode = conn.getResponseCode();
            blazegraphAvailable = (responseCode == 200);
            conn.disconnect();
        } catch (Exception e) {
            System.out.println("Blazegraph not available for integration tests: " + e.getMessage());
            blazegraphAvailable = false;
        }
    }

    @Before
    public void setUp() {
        assumeTrue("Blazegraph not available - skipping integration tests", blazegraphAvailable);
    }

    // ========== Basic Connectivity Tests ==========

    @Test
    public void canConnectToBlazegraph() throws Exception {
        URL url = URI.create("http://localhost:8889/bigdata/namespace").toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        int responseCode = conn.getResponseCode();
        assertEquals("Should get 200 OK from Blazegraph", 200, responseCode);
        conn.disconnect();
    }

    @Test
    public void sparqlEndpointResponds() throws Exception {
        // Simple ASK query to verify SPARQL endpoint works
        String query = "ASK { ?s ?p ?o }";
        String result = executeSparqlQuery(query);
        assertNotNull("SPARQL result should not be null", result);
        // ASK queries return boolean
        assertTrue("Result should contain boolean", result.contains("true") || result.contains("false"));
    }

    // ========== Power Grid Model Query Tests ==========

    @Test
    public void canQueryForFeeders() throws Exception {
        // This is the query used by PowergridModelDataManager to get model names
        String query = CIM_PREFIX +
            "SELECT ?name ?mRID ?substationName ?substationID ?subregionName ?subregionID ?regionName ?regionID WHERE { " +
            "?s r:type c:Feeder. " +
            "?s c:IdentifiedObject.name ?name. " +
            "?s c:IdentifiedObject.mRID ?mRID. " +
            "OPTIONAL { " +
            "  ?s c:Feeder.NormalEnergizingSubstation ?subStation. " +
            "  ?subStation c:IdentifiedObject.name ?substationName. " +
            "  ?subStation c:IdentifiedObject.mRID ?substationID. " +
            "  ?subStation c:Substation.Region ?subRegion. " +
            "  ?subRegion c:IdentifiedObject.name ?subregionName. " +
            "  ?subRegion c:IdentifiedObject.mRID ?subregionID. " +
            "  ?subRegion c:SubGeographicalRegion.Region ?region. " +
            "  ?region c:IdentifiedObject.name ?regionName. " +
            "  ?region c:IdentifiedObject.mRID ?regionID. " +
            "} " +
            "} ORDER BY ?name";

        String result = executeSparqlQuery(query);
        assertNotNull("Query result should not be null", result);

        // Parse JSON result
        JsonObject jsonResult = JsonParser.parseString(result).getAsJsonObject();
        assertTrue("Result should have 'results' field", jsonResult.has("results"));

        JsonObject results = jsonResult.getAsJsonObject("results");
        assertTrue("Results should have 'bindings' field", results.has("bindings"));

        JsonArray bindings = results.getAsJsonArray("bindings");
        // If models are loaded, we should have at least one feeder
        if (bindings.size() > 0) {
            JsonObject firstBinding = bindings.get(0).getAsJsonObject();
            assertTrue("Feeder should have name", firstBinding.has("name"));
            assertTrue("Feeder should have mRID", firstBinding.has("mRID"));

            String feederName = firstBinding.getAsJsonObject("name").get("value").getAsString();
            String feederId = firstBinding.getAsJsonObject("mRID").get("value").getAsString();

            assertNotNull("Feeder name should not be null", feederName);
            assertNotNull("Feeder mRID should not be null", feederId);
            assertFalse("Feeder name should not be empty", feederName.isEmpty());

            System.out.println("Found feeder: " + feederName + " (ID: " + feederId + ")");
        }
    }

    @Test
    public void canQueryObjectTypes() throws Exception {
        // Query to get distinct object types in the model
        String query = CIM_PREFIX +
            "SELECT DISTINCT ?type WHERE { " +
            "?s r:type ?type. " +
            "FILTER(STRSTARTS(STR(?type), 'http://iec.ch/TC57/CIM100#')) " +
            "} ORDER BY ?type LIMIT 20";

        String result = executeSparqlQuery(query);
        assertNotNull("Query result should not be null", result);

        JsonObject jsonResult = JsonParser.parseString(result).getAsJsonObject();
        JsonArray bindings = jsonResult.getAsJsonObject("results").getAsJsonArray("bindings");

        if (bindings.size() > 0) {
            System.out.println("Found " + bindings.size() + " CIM object types");
            for (JsonElement binding : bindings) {
                String type = binding.getAsJsonObject().getAsJsonObject("type").get("value").getAsString();
                assertTrue("Type should be CIM type", type.contains("CIM100#"));
            }
        }
    }

    @Test
    public void canQueryMeasurements() throws Exception {
        // Query for measurements - similar to what PowergridModelDataManager does
        String query = CIM_PREFIX +
            "SELECT ?name ?type ?phases ?measid ?eqid ?trmid WHERE { " +
            "?s r:type c:Analog. " +
            "?s c:IdentifiedObject.name ?name. " +
            "?s c:IdentifiedObject.mRID ?measid. " +
            "?s c:Analog.measurementType ?type. " +
            "?s c:Measurement.phases ?phases. " +
            "?s c:Measurement.Terminal ?trm. " +
            "?trm c:IdentifiedObject.mRID ?trmid. " +
            "?trm c:Terminal.ConductingEquipment ?eq. " +
            "?eq c:IdentifiedObject.mRID ?eqid. " +
            "} LIMIT 10";

        String result = executeSparqlQuery(query);
        assertNotNull("Query result should not be null", result);

        JsonObject jsonResult = JsonParser.parseString(result).getAsJsonObject();
        JsonArray bindings = jsonResult.getAsJsonObject("results").getAsJsonArray("bindings");

        // Measurements may or may not exist depending on model
        System.out.println("Found " + bindings.size() + " analog measurements");
    }

    @Test
    public void canCountTriples() throws Exception {
        // Count total triples in the store
        String query = "SELECT (COUNT(*) as ?count) WHERE { ?s ?p ?o }";

        String result = executeSparqlQuery(query);
        assertNotNull("Query result should not be null", result);

        JsonObject jsonResult = JsonParser.parseString(result).getAsJsonObject();
        JsonArray bindings = jsonResult.getAsJsonObject("results").getAsJsonArray("bindings");

        assertTrue("Should have count result", bindings.size() > 0);

        String countStr = bindings.get(0).getAsJsonObject().getAsJsonObject("count").get("value").getAsString();
        long count = Long.parseLong(countStr);

        System.out.println("Total triples in Blazegraph: " + count);
        assertTrue("Should have some triples", count > 0);
    }

    @Test
    public void queryResultsAreValidJson() throws Exception {
        String query = CIM_PREFIX + "SELECT ?s ?name WHERE { ?s c:IdentifiedObject.name ?name } LIMIT 5";

        String result = executeSparqlQuery(query);
        assertNotNull("Result should not be null", result);

        // Should be parseable as JSON
        JsonElement element = JsonParser.parseString(result);
        assertTrue("Result should be JSON object", element.isJsonObject());

        JsonObject obj = element.getAsJsonObject();
        assertTrue("Should have 'head' field", obj.has("head"));
        assertTrue("Should have 'results' field", obj.has("results"));
    }

    // ========== Helper Methods ==========

    private String executeSparqlQuery(String query) throws Exception {
        URL url = URI.create(BLAZEGRAPH_URL).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Accept", "application/sparql-results+json");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);

        String encodedQuery = "query=" + URLEncoder.encode(query, StandardCharsets.UTF_8.toString());

        try (OutputStream os = conn.getOutputStream()) {
            os.write(encodedQuery.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            StringBuilder errorResponse = new StringBuilder();
            String line;
            while ((line = errorReader.readLine()) != null) {
                errorResponse.append(line);
            }
            fail("SPARQL query failed with code " + responseCode + ": " + errorResponse.toString());
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        conn.disconnect();

        return response.toString();
    }
}
