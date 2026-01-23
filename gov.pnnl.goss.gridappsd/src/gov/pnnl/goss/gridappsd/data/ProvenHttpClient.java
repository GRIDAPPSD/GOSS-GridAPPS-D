package gov.pnnl.goss.gridappsd.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Simple HTTP client for communicating with the Proven REST API.
 *
 * This class replaces the ProvenProducer from proven-client which uses
 * Jersey/HK2 and causes classloading issues in OSGi due to shaded dependency
 * conflicts. Using standard HttpURLConnection avoids these issues while
 * maintaining Proven as the middleware layer for timeseries data.
 */
public class ProvenHttpClient {

    private String queryUri;
    private String advancedQueryUri;
    private String writeUri;
    private String influxqlUri;

    public ProvenHttpClient() {
    }

    /**
     * Configure the Proven query endpoint.
     *
     * @param uri
     *            The base URI for queries (e.g.,
     *            http://proven:8080/hybrid/rest/v1/repository/influxql)
     */
    public void setQueryUri(String uri) {
        this.queryUri = uri;
    }

    /**
     * Configure the Proven advanced query endpoint.
     *
     * @param uri
     *            The URI for advanced time series queries
     */
    public void setAdvancedQueryUri(String uri) {
        this.advancedQueryUri = uri;
    }

    /**
     * Configure the Proven write endpoint.
     *
     * @param uri
     *            The URI for writing time series data
     */
    public void setWriteUri(String uri) {
        this.writeUri = uri;
    }

    /**
     * Configure the Proven InfluxQL endpoint.
     *
     * @param uri
     *            The URI for InfluxQL queries (e.g.,
     *            http://proven:8080/hybrid/rest/v1/repository/influxql)
     */
    public void setInfluxqlUri(String uri) {
        this.influxqlUri = uri;
    }

    /**
     * Send a query to Proven and return the response.
     *
     * @param queryJson
     *            The query as a JSON string
     * @return The response data as a JsonElement
     * @throws IOException
     *             if the request fails
     */
    public JsonElement sendQuery(String queryJson) throws IOException {
        return doPost(queryUri, queryJson, "application/json");
    }

    /**
     * Send an advanced time series query to Proven.
     *
     * @param queryJson
     *            The advanced query as a JSON string
     * @return The response data as a JsonElement
     * @throws IOException
     *             if the request fails
     */
    public JsonElement sendAdvancedQuery(String queryJson) throws IOException {
        return doPost(advancedQueryUri, queryJson, "application/json");
    }

    /**
     * Send an InfluxQL query to Proven.
     *
     * @param influxQuery
     *            The InfluxQL query string
     * @return The response data as a JsonElement
     * @throws IOException
     *             if the request fails
     */
    public JsonElement sendInfluxQuery(String influxQuery) throws IOException {
        return doPost(influxqlUri, influxQuery, "text/plain");
    }

    /**
     * Send bulk time series data to Proven.
     *
     * @param dataJson
     *            The data payload as JSON
     * @param measurementName
     *            The measurement name
     * @param instanceId
     *            The instance ID (optional)
     * @param simulationId
     *            The simulation ID (optional)
     * @param timestamp
     *            The timestamp
     * @return The response from Proven
     * @throws IOException
     *             if the request fails
     */
    public JsonElement sendBulkData(String dataJson, String measurementName, String instanceId, String simulationId,
            long timestamp) throws IOException {
        // Build query parameters
        StringBuilder urlBuilder = new StringBuilder(writeUri);
        urlBuilder.append("?measurementName=").append(measurementName);
        if (instanceId != null) {
            urlBuilder.append("&instanceId=").append(instanceId);
        }
        if (simulationId != null) {
            urlBuilder.append("&simulationId=").append(simulationId);
        }
        urlBuilder.append("&timestamp=").append(timestamp);

        return doPost(urlBuilder.toString(), dataJson, "application/json");
    }

    /**
     * Perform an HTTP POST request and parse the JSON response.
     */
    private JsonElement doPost(String urlString, String body, String contentType) throws IOException {
        URL url = URI.create(urlString).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", contentType);
            // Don't set Accept header - Proven's influxql endpoint rejects application/json
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);

            // Write request body
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = body.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();

            // Read response
            String response;
            if (responseCode >= 200 && responseCode < 300) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    response = br.lines().collect(Collectors.joining("\n"));
                }
            } else {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    response = br.lines().collect(Collectors.joining("\n"));
                }
                throw new IOException("HTTP " + responseCode + ": " + response);
            }

            // Parse JSON response
            if (response != null && !response.isEmpty()) {
                return JsonParser.parseString(response);
            }
            return null;

        } finally {
            conn.disconnect();
        }
    }

    /**
     * Check if Proven is available by calling the state endpoint.
     *
     * @param baseUri
     *            The base Proven URI (e.g., http://proven:8080/hybrid/rest/v1)
     * @return true if Proven is available and enabled
     */
    public static boolean isProvenAvailable(String baseUri) {
        try {
            URL url = URI.create(baseUri + "/repository/state").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String response = br.lines().collect(Collectors.joining("\n"));
                    JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                    return json.has("isEnabled") && json.get("isEnabled").getAsBoolean();
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
