package gov.pnnl.goss.gridappsd.data;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gov.pnnl.goss.gridappsd.api.AppManager;
import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.api.DataManagerHandler;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.ServiceManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.api.TimeseriesDataManager;
import gov.pnnl.goss.gridappsd.data.conversion.DataFormatConverter;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.RequestTimeseriesData;
import gov.pnnl.goss.gridappsd.dto.RequestTimeseriesDataAdvanced;
import gov.pnnl.goss.gridappsd.dto.RequestTimeseriesDataBasic;
import gov.pnnl.goss.gridappsd.dto.SimulationContext;
import gov.pnnl.goss.gridappsd.dto.TimeSeriesEntryResult;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.GossResponseEvent;

/**
 * InfluxDB-based implementation of TimeseriesDataManager.
 *
 * This implementation writes time series data directly to InfluxDB using the
 * HTTP API, bypassing the Proven REST API middleware to avoid Jersey/HK2
 * classloader issues in OSGi environments.
 *
 * Disabled in favor of ProvenTimeSeriesDataManagerImpl which uses the Proven
 * middleware.
 */
// @Component(service = {TimeseriesDataManager.class, DataManagerHandler.class},
// immediate = true)
public class InfluxTimeSeriesDataManagerImpl implements TimeseriesDataManager, DataManagerHandler {

    @Reference
    private volatile LogManager logManager;

    @Reference
    private volatile DataManager dataManager;

    @Reference
    private volatile ClientFactory clientFactory;

    @Reference
    private volatile ConfigurationManager configManager;

    @Reference
    private volatile SimulationManager simulationManager;

    @Reference
    private volatile ServiceManager serviceManager;

    @Reference
    private volatile AppManager appManager;

    public static final String DATA_MANAGER_TYPE = "timeseries";

    private static final String DEFAULT_INFLUXDB_URL = "http://influxdb:8086";
    private static final String DEFAULT_INFLUXDB_DATABASE = "proven";

    private CloseableHttpClient httpClient;
    private String influxDbUrl;
    private String influxDbDatabase;
    private String influxDbUsername;
    private String influxDbPassword;

    Gson gson = new Gson();

    @Activate
    public void start() {
        logManager.debug(ProcessStatus.RUNNING, null, "Starting " + this.getClass().getSimpleName());

        dataManager.registerDataManagerHandler(this, DATA_MANAGER_TYPE);

        // Get InfluxDB configuration from properties
        influxDbUrl = configManager.getConfigurationProperty(GridAppsDConstants.INFLUXDB_URL);
        if (influxDbUrl == null || influxDbUrl.isEmpty()) {
            influxDbUrl = DEFAULT_INFLUXDB_URL;
        }

        influxDbDatabase = configManager.getConfigurationProperty(GridAppsDConstants.INFLUXDB_DATABASE);
        if (influxDbDatabase == null || influxDbDatabase.isEmpty()) {
            influxDbDatabase = DEFAULT_INFLUXDB_DATABASE;
        }

        influxDbUsername = configManager.getConfigurationProperty(GridAppsDConstants.INFLUXDB_USERNAME);
        influxDbPassword = configManager.getConfigurationProperty(GridAppsDConstants.INFLUXDB_PASSWORD);

        // Initialize HTTP client
        httpClient = HttpClients.createDefault();

        // Create database if it doesn't exist
        try {
            executeInfluxQuery("CREATE DATABASE " + influxDbDatabase);
            logManager.info(ProcessStatus.RUNNING, null,
                    "Connected to InfluxDB at " + influxDbUrl + ", database: " + influxDbDatabase);
        } catch (Exception e) {
            logManager.error(ProcessStatus.ERROR, null,
                    "Failed to connect to InfluxDB: " + e.getMessage());
            e.printStackTrace();
        }

        // Subscribe to simulation output topics
        try {
            this.subscribeAndStoreDataFromTopic("/topic/goss.gridappsd.*.output", null, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Deactivate
    public void stop() {
        if (httpClient != null) {
            try {
                httpClient.close();
                logManager.info(ProcessStatus.STOPPED, null, "InfluxDB HTTP client closed");
            } catch (Exception e) {
                logManager.error(ProcessStatus.ERROR, null,
                        "Error closing InfluxDB HTTP client: " + e.getMessage());
            }
        }
    }

    @Override
    public Serializable handle(Serializable requestContent, String processId,
            String username) throws Exception {
        if (requestContent instanceof SimulationContext) {
            storeAllData((SimulationContext) requestContent);
        }
        if (requestContent instanceof RequestTimeseriesData) {
            return query((RequestTimeseriesData) requestContent);
        } else if (requestContent instanceof String) {
            // First try to parse the query as the new format, if that fails try the old
            RequestTimeseriesData timeSeriesRequest;
            try {
                timeSeriesRequest = RequestTimeseriesDataAdvanced.parse((String) requestContent);
            } catch (Exception e) {
                try {
                    timeSeriesRequest = RequestTimeseriesDataBasic.parse((String) requestContent);
                } catch (Exception e2) {
                    throw new Exception("Failed to parse time series data request");
                }
            }
            return query(timeSeriesRequest);
        }
        return null;
    }

    @Override
    public Serializable query(RequestTimeseriesData requestTimeseriesData) throws Exception {
        String measurement = requestTimeseriesData.getQueryMeasurement();
        String influxQuery = buildInfluxQuery(requestTimeseriesData);

        logManager.debug(ProcessStatus.RUNNING, null, "Executing InfluxDB query: " + influxQuery);

        String jsonResponse = executeInfluxQuery(influxQuery);

        // Convert InfluxDB JSON response to TimeSeriesEntryResult format
        ArrayList<HashMap<String, Object>> data = parseInfluxResponse(jsonResponse);

        if (data.isEmpty()) {
            return null;
        }

        TimeSeriesEntryResult result = new TimeSeriesEntryResult();
        result.setData(data);

        // Check if format conversion is needed
        String origFormat = "PROVEN_" + measurement;
        if (requestTimeseriesData.getOriginalFormat() != null) {
            origFormat = "PROVEN_" + requestTimeseriesData.getOriginalFormat();
        }
        String responseFormat = requestTimeseriesData.getResponseFormat();
        DataFormatConverter converter = dataManager.getConverter(origFormat, responseFormat);

        if (converter != null) {
            StringWriter sw = new StringWriter();
            converter.convert(gson.toJson(data), new PrintWriter(sw), requestTimeseriesData);
            return sw.toString();
        }

        return gson.toJson(data);
    }

    /**
     * Execute an InfluxQL query via HTTP API.
     */
    private String executeInfluxQuery(String query) throws Exception {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
        String url = influxDbUrl + "/query?db=" + influxDbDatabase + "&q=" + encodedQuery;

        HttpGet request = new HttpGet(url);
        request.setHeader("Accept", "application/json");

        if (influxDbUsername != null && !influxDbUsername.isEmpty()) {
            String auth = influxDbUsername + ":" + (influxDbPassword != null ? influxDbPassword : "");
            String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            request.setHeader("Authorization", "Basic " + encodedAuth);
        }

        HttpResponse response = httpClient.execute(request);
        HttpEntity entity = response.getEntity();

        if (entity != null) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8))) {
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                return result.toString();
            }
        }
        return "{}";
    }

    /**
     * Write data points to InfluxDB via HTTP API using line protocol.
     */
    private void writeToInfluxDB(String lineProtocol) throws Exception {
        String url = influxDbUrl + "/write?db=" + influxDbDatabase + "&precision=ms";

        HttpPost request = new HttpPost(url);
        request.setHeader("Content-Type", "text/plain");

        if (influxDbUsername != null && !influxDbUsername.isEmpty()) {
            String auth = influxDbUsername + ":" + (influxDbPassword != null ? influxDbPassword : "");
            String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            request.setHeader("Authorization", "Basic " + encodedAuth);
        }

        request.setEntity(new StringEntity(lineProtocol, StandardCharsets.UTF_8));

        HttpResponse response = httpClient.execute(request);
        int statusCode = response.getStatusLine().getStatusCode();

        if (statusCode < 200 || statusCode >= 300) {
            HttpEntity entity = response.getEntity();
            String errorBody = "";
            if (entity != null) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8))) {
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    errorBody = result.toString();
                }
            }
            throw new Exception("InfluxDB write failed with status " + statusCode + ": " + errorBody);
        }
    }

    /**
     * Build an InfluxQL query from RequestTimeseriesData.
     */
    private String buildInfluxQuery(RequestTimeseriesData request) {
        StringBuilder query = new StringBuilder();
        String measurement = request.getQueryMeasurement();

        query.append("SELECT * FROM \"").append(escapeInfluxString(measurement)).append("\"");

        // Build WHERE clause from query filter
        List<String> whereConditions = new ArrayList<>();

        if (request instanceof RequestTimeseriesDataBasic) {
            RequestTimeseriesDataBasic basicRequest = (RequestTimeseriesDataBasic) request;
            Map<String, Object> filter = basicRequest.getQueryFilter();
            if (filter != null) {
                addFilterConditions(filter, whereConditions);
            }
        } else if (request instanceof RequestTimeseriesDataAdvanced) {
            RequestTimeseriesDataAdvanced advRequest = (RequestTimeseriesDataAdvanced) request;
            List<Object> filters = advRequest.getQueryFilter();
            if (filters != null) {
                for (Object filterObj : filters) {
                    if (filterObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> filter = (Map<String, Object>) filterObj;
                        addFilterConditions(filter, whereConditions);
                    }
                }
            }
        }

        if (!whereConditions.isEmpty()) {
            query.append(" WHERE ").append(String.join(" AND ", whereConditions));
        }

        // Add LIMIT clause for advanced queries
        if (request instanceof RequestTimeseriesDataAdvanced) {
            RequestTimeseriesDataAdvanced advRequest = (RequestTimeseriesDataAdvanced) request;
            if (advRequest.getFirst() != null) {
                query.append(" LIMIT ").append(advRequest.getFirst());
            } else if (advRequest.getLast() != null) {
                query.append(" ORDER BY time DESC LIMIT ").append(advRequest.getLast());
            }
        }

        return query.toString();
    }

    /**
     * Add filter conditions to the WHERE clause.
     */
    private void addFilterConditions(Map<String, Object> filter, List<String> whereConditions) {
        for (Map.Entry<String, Object> entry : filter.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if ("startTime".equals(key)) {
                // InfluxDB expects nanoseconds, but we use milliseconds precision
                long timestamp = Long.parseLong(value.toString());
                whereConditions.add("time >= " + timestamp + "ms");
            } else if ("endTime".equals(key)) {
                long timestamp = Long.parseLong(value.toString());
                whereConditions.add("time <= " + timestamp + "ms");
            } else if (value instanceof String) {
                whereConditions
                        .add("\"" + escapeInfluxString(key) + "\" = '" + escapeInfluxString(value.toString()) + "'");
            } else if (value instanceof Number) {
                whereConditions.add("\"" + escapeInfluxString(key) + "\" = " + value);
            }
        }
    }

    /**
     * Parse InfluxDB JSON response into TimeSeriesEntryResult format.
     */
    private ArrayList<HashMap<String, Object>> parseInfluxResponse(String jsonResponse) {
        ArrayList<HashMap<String, Object>> data = new ArrayList<>();

        try {
            JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();
            JsonArray results = root.getAsJsonArray("results");

            if (results == null || results.size() == 0) {
                return data;
            }

            JsonObject firstResult = results.get(0).getAsJsonObject();
            if (firstResult.has("error")) {
                logManager.error(ProcessStatus.ERROR, null,
                        "InfluxDB query error: " + firstResult.get("error").getAsString());
                return data;
            }

            JsonArray series = firstResult.getAsJsonArray("series");
            if (series == null || series.size() == 0) {
                return data;
            }

            for (JsonElement seriesElement : series) {
                JsonObject seriesObj = seriesElement.getAsJsonObject();
                String name = seriesObj.has("name") ? seriesObj.get("name").getAsString() : null;
                JsonArray columns = seriesObj.getAsJsonArray("columns");
                JsonArray values = seriesObj.getAsJsonArray("values");

                // Get tags if present
                JsonObject tags = seriesObj.has("tags") ? seriesObj.getAsJsonObject("tags") : null;

                if (values == null) {
                    continue;
                }

                for (JsonElement rowElement : values) {
                    JsonArray row = rowElement.getAsJsonArray();
                    HashMap<String, Object> entry = new HashMap<>();

                    for (int i = 0; i < columns.size(); i++) {
                        String column = columns.get(i).getAsString();
                        JsonElement value = row.get(i);
                        if (!value.isJsonNull()) {
                            if (value.isJsonPrimitive()) {
                                if (value.getAsJsonPrimitive().isNumber()) {
                                    entry.put(column, value.getAsNumber());
                                } else if (value.getAsJsonPrimitive().isBoolean()) {
                                    entry.put(column, value.getAsBoolean());
                                } else {
                                    entry.put(column, value.getAsString());
                                }
                            } else {
                                entry.put(column, value.toString());
                            }
                        }
                    }

                    // Add measurement name
                    if (name != null) {
                        entry.put("measurement", name);
                    }

                    // Add tags
                    if (tags != null) {
                        for (Map.Entry<String, JsonElement> tagEntry : tags.entrySet()) {
                            entry.put(tagEntry.getKey(), tagEntry.getValue().getAsString());
                        }
                    }

                    data.add(entry);
                }
            }
        } catch (Exception e) {
            logManager.error(ProcessStatus.ERROR, null,
                    "Error parsing InfluxDB response: " + e.getMessage());
            e.printStackTrace();
        }

        return data;
    }

    /**
     * Escape special characters in InfluxDB strings.
     */
    private String escapeInfluxString(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("'", "\\'");
    }

    /**
     * Escape special characters for InfluxDB line protocol tag/field keys.
     */
    private String escapeLineProtocolKey(String str) {
        if (str == null) {
            return "";
        }
        return str.replace(" ", "\\ ")
                .replace(",", "\\,")
                .replace("=", "\\=");
    }

    /**
     * Escape special characters for InfluxDB line protocol string field values.
     */
    private String escapeLineProtocolFieldValue(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    @Override
    public void storeAllData(SimulationContext simulationContext) throws Exception {
        String simulationId = simulationContext.getSimulationId();

        storeSimulationInput(simulationId);
        storeSimulationOutput(simulationId);

        for (String instanceId : simulationContext.getServiceInstanceIds()) {
            String serviceId = serviceManager.getServiceIdForInstance(instanceId);
            storeServiceInput(simulationId, serviceId, instanceId);
            storeServiceOutput(simulationId, serviceId, instanceId);
        }

        for (String instanceId : simulationContext.getAppInstanceIds()) {
            String appId = appManager.getAppIdForInstance(instanceId);
            storeAppInput(simulationId, appId, instanceId);
            storeAppOutput(simulationId, appId, instanceId);
        }
    }

    private void subscribeAndStoreDataFromTopic(String topic, String appOrServiceid, String instanceId,
            String simulationId) throws Exception {
        Credentials credentials = new UsernamePasswordCredentials("system", "manager");
        Client inputClient = clientFactory.create(PROTOCOL.STOMP, credentials);

        inputClient.subscribe(topic, new GossResponseEvent() {
            @Override
            public void onMessage(Serializable message) {
                DataResponse event = (DataResponse) message;
                try {
                    writeMessageToInfluxDB(event, appOrServiceid, instanceId, simulationId);
                } catch (Exception e) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    String sStackTrace = sw.toString();
                    System.out.println(sStackTrace);
                    logManager.error(ProcessStatus.RUNNING, null, "Error storing timeseries data for message at "
                            + event.getDestination() + " : " + sStackTrace);
                }
            }
        });
    }

    /**
     * Write a message to InfluxDB using line protocol.
     */
    private void writeMessageToInfluxDB(DataResponse event, String appOrServiceid, String instanceId,
            String simulationId) {
        try {
            String dataStr = event.getData().toString();
            JsonElement data = JsonParser.parseString(dataStr);

            if (!data.isJsonObject()) {
                return;
            }

            JsonObject dataObj = data.getAsJsonObject();
            String datatype = appOrServiceid;

            // Get datatype from message if not provided
            if (datatype == null && dataObj.has("datatype")) {
                datatype = dataObj.get("datatype").getAsString();
            }

            if (datatype == null) {
                return;
            }

            // Get timestamp from message or use current time
            long timestamp = new Date().getTime();
            if (dataObj.has("timestamp")) {
                timestamp = dataObj.get("timestamp").getAsLong();
            }

            // Get simulation_id from message or use provided
            String simId = simulationId;
            if (simId == null && dataObj.has("simulation_id")) {
                simId = dataObj.get("simulation_id").getAsString();
            }

            // Build InfluxDB line protocol
            StringBuilder lineProtocol = new StringBuilder();
            lineProtocol.append(escapeLineProtocolKey(datatype));

            // Add tags
            List<String> tags = new ArrayList<>();
            if (simId != null) {
                tags.add("simulation_id=" + escapeLineProtocolKey(simId));
            }
            if (instanceId != null) {
                tags.add("instance_id=" + escapeLineProtocolKey(instanceId));
            }
            tags.add("source=" + escapeLineProtocolKey(event.getDestination()));

            if (!tags.isEmpty()) {
                lineProtocol.append(",").append(String.join(",", tags));
            }

            lineProtocol.append(" ");

            // Add fields
            List<String> fields = new ArrayList<>();

            for (Map.Entry<String, JsonElement> entry : dataObj.entrySet()) {
                String key = entry.getKey();
                JsonElement value = entry.getValue();

                // Skip known metadata fields that are stored as tags
                if ("datatype".equals(key) || "simulation_id".equals(key) || "timestamp".equals(key)) {
                    continue;
                }

                if (value.isJsonPrimitive()) {
                    if (value.getAsJsonPrimitive().isNumber()) {
                        fields.add(escapeLineProtocolKey(key) + "=" + value.getAsDouble());
                    } else if (value.getAsJsonPrimitive().isString()) {
                        fields.add(escapeLineProtocolKey(key) + "=\"" +
                                escapeLineProtocolFieldValue(value.getAsString()) + "\"");
                    } else if (value.getAsJsonPrimitive().isBoolean()) {
                        fields.add(escapeLineProtocolKey(key) + "=" + value.getAsBoolean());
                    }
                } else if (value.isJsonObject() && "measurements".equals(key)) {
                    // Handle nested measurements object
                    JsonObject measurements = value.getAsJsonObject();
                    for (Map.Entry<String, JsonElement> mEntry : measurements.entrySet()) {
                        JsonElement mValue = mEntry.getValue();
                        if (mValue.isJsonPrimitive() && mValue.getAsJsonPrimitive().isNumber()) {
                            fields.add(escapeLineProtocolKey(mEntry.getKey()) + "=" + mValue.getAsDouble());
                        }
                    }
                } else if (!value.isJsonNull()) {
                    // Store complex objects as JSON strings
                    fields.add(escapeLineProtocolKey(key) + "=\"" +
                            escapeLineProtocolFieldValue(gson.toJson(value)) + "\"");
                }
            }

            // Only write if there are fields
            if (!fields.isEmpty()) {
                lineProtocol.append(String.join(",", fields));
                lineProtocol.append(" ").append(timestamp);

                writeToInfluxDB(lineProtocol.toString());
            }

        } catch (Exception e) {
            logManager.error(ProcessStatus.ERROR, null,
                    "Error writing to InfluxDB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void storeSimulationOutput(String simulationId) throws Exception {
        subscribeAndStoreDataFromTopic("/topic/" + GridAppsDConstants.topic_simulation + ".output." + simulationId,
                "simulation", null, simulationId);
    }

    @Override
    public void storeSimulationInput(String simulationId) throws Exception {
        subscribeAndStoreDataFromTopic("/topic/" + GridAppsDConstants.topic_simulation + ".input." + simulationId,
                "simulation", null, simulationId);
    }

    @Override
    public void storeServiceOutput(String simulationId, String serviceId, String instanceId) throws Exception {
        subscribeAndStoreDataFromTopic(
                "/topic/" + GridAppsDConstants.topic_simulation + "." + serviceId + "." + simulationId + ".output",
                serviceId, instanceId, simulationId);
    }

    @Override
    public void storeServiceInput(String simulationId, String serviceId, String instanceId) throws Exception {
        subscribeAndStoreDataFromTopic(
                "/topic/" + GridAppsDConstants.topic_simulation + "." + serviceId + "." + simulationId + ".input",
                serviceId, instanceId, simulationId);
    }

    @Override
    public void storeAppOutput(String simulationId, String appId, String instanceId) throws Exception {
        subscribeAndStoreDataFromTopic(
                "/topic/" + GridAppsDConstants.topic_simulation + "." + appId + "." + simulationId + ".output", appId,
                instanceId, simulationId);
    }

    @Override
    public void storeAppInput(String simulationId, String appId, String instanceId) throws Exception {
        subscribeAndStoreDataFromTopic(
                "/topic/" + GridAppsDConstants.topic_simulation + "." + appId + "." + simulationId + ".input", appId,
                instanceId, simulationId);
    }
}
