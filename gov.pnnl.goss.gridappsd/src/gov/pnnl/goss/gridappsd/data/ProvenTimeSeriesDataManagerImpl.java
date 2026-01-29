package gov.pnnl.goss.gridappsd.data;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;

import com.google.gson.Gson;
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
 * TimeseriesDataManager implementation that uses Proven as the middleware layer
 * for storing and querying timeseries data in InfluxDB.
 *
 * This implementation uses a simple HTTP client (ProvenHttpClient) to
 * communicate with the Proven REST API, avoiding the Jersey/HK2 classloading
 * issues that occur with the proven-client library in OSGi.
 */
@Component(service = {TimeseriesDataManager.class, DataManagerHandler.class}, immediate = true)
public class ProvenTimeSeriesDataManagerImpl implements TimeseriesDataManager, DataManagerHandler {

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

    public static int count = 0;

    List<String> keywords = null;
    String requestId = null;
    Gson gson = new Gson();

    // Simple HTTP client for Proven REST API (avoids Jersey/HK2 issues)
    private ProvenHttpClient provenClient;
    private String provenBaseUri;

    @Activate
    public void start() {
        logManager.debug(ProcessStatus.RUNNING, null, "Starting " + this.getClass().getSimpleName());

        dataManager.registerDataManagerHandler(this, DATA_MANAGER_TYPE);

        // Get Proven configuration
        provenBaseUri = configManager.getConfigurationProperty(GridAppsDConstants.PROVEN_PATH);
        String provenWriteUri = configManager.getConfigurationProperty(GridAppsDConstants.PROVEN_WRITE_PATH);
        String provenQueryUri = configManager.getConfigurationProperty(GridAppsDConstants.PROVEN_QUERY_PATH);
        String provenAdvancedQueryUri = configManager
                .getConfigurationProperty(GridAppsDConstants.PROVEN_ADVANCED_QUERY_PATH);

        // Initialize simple HTTP client
        provenClient = new ProvenHttpClient();
        provenClient.setQueryUri(provenQueryUri);
        provenClient.setAdvancedQueryUri(provenAdvancedQueryUri);
        provenClient.setWriteUri(provenWriteUri);
        // Derive influxql endpoint from base URI
        String influxqlUri = provenBaseUri.replace("/provenMessage", "/influxql");
        if (!influxqlUri.endsWith("/influxql")) {
            // If base path doesn't end with provenMessage, construct from base
            influxqlUri = provenBaseUri.replaceAll("/[^/]+$", "/influxql");
        }
        provenClient.setInfluxqlUri(influxqlUri);

        // Check Proven availability
        if (ProvenHttpClient.isProvenAvailable(provenBaseUri)) {
            logManager.debug(ProcessStatus.RUNNING, null, "Proven service is available at " + provenBaseUri);
        } else {
            logManager.warn(ProcessStatus.RUNNING, null,
                    "Proven service is not available at " + provenBaseUri + " - timeseries data will not be stored");
        }

        try {
            this.subscribeAndStoreDataFromTopic("goss.gridappsd.*.output", null, null, null);
        } catch (Exception e) {
            logManager.error(ProcessStatus.RUNNING, null, "Error subscribing to output topics: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public Serializable handle(Serializable requestContent, String processId, String username) throws Exception {
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
        JsonElement response;

        try {
            // Build InfluxQL query from the request
            String influxQuery = buildInfluxQuery(requestTimeseriesData);
            logManager.debug(ProcessStatus.RUNNING, null, "Executing InfluxQL query: " + influxQuery);
            response = provenClient.sendInfluxQuery(influxQuery);
        } catch (Exception e) {
            logManager.error(ProcessStatus.RUNNING, null, "Error querying Proven: " + e.getMessage());
            throw e;
        }

        if (response == null) {
            return null;
        }

        TimeSeriesEntryResult result = TimeSeriesEntryResult.parse(response.toString());
        if (result.getData().size() == 0) {
            return null;
        }

        String origFormat = "PROVEN_" + requestTimeseriesData.getQueryMeasurement().toString();
        if (requestTimeseriesData.getOriginalFormat() != null) {
            origFormat = "PROVEN_" + requestTimeseriesData.getOriginalFormat();
        }
        String responseFormat = requestTimeseriesData.getResponseFormat();
        DataFormatConverter converter = dataManager.getConverter(origFormat, responseFormat);
        if (converter != null) {
            StringWriter sw = new StringWriter();
            converter.convert(response.toString(), new PrintWriter(sw), requestTimeseriesData);
            return sw.toString();
        }

        return response.toString();
    }

    /**
     * Build an InfluxQL query string from a RequestTimeseriesData object.
     */
    private String buildInfluxQuery(RequestTimeseriesData requestTimeseriesData) {
        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM ").append(requestTimeseriesData.getQueryMeasurement());

        // Handle basic query with filter
        if (requestTimeseriesData instanceof RequestTimeseriesDataBasic) {
            RequestTimeseriesDataBasic basicRequest = (RequestTimeseriesDataBasic) requestTimeseriesData;
            java.util.Map<String, Object> filter = basicRequest.getQueryFilter();
            if (filter != null && !filter.isEmpty()) {
                query.append(" WHERE ");
                boolean first = true;
                for (java.util.Map.Entry<String, Object> entry : filter.entrySet()) {
                    if (!first) {
                        query.append(" AND ");
                    }
                    first = false;
                    String key = entry.getKey();
                    Object value = entry.getValue();

                    // Handle time fields - detect format by digit count and convert to nanoseconds
                    // - 19+ digits: Already nanoseconds (weather format: ms + "000000") - use
                    // directly
                    // - 16-18 digits: Microseconds - multiply by 1000
                    // - 13-15 digits: Milliseconds (zipload format) - multiply by 1,000,000
                    if ("startTime".equals(key)) {
                        String timeStr = value.toString();
                        long timeNs = convertToNanoseconds(timeStr);
                        query.append("time >= ").append(timeNs);
                    } else if ("endTime".equals(key)) {
                        String timeStr = value.toString();
                        long timeNs = convertToNanoseconds(timeStr);
                        query.append("time <= ").append(timeNs);
                    } else if (value instanceof String) {
                        query.append(key).append(" = '").append(value).append("'");
                    } else {
                        query.append(key).append(" = ").append(value);
                    }
                }
            }
        }

        return query.toString();
    }

    /**
     * Convert a time string to nanoseconds based on its digit count.
     *
     * The handlers send time values in different formats: - Weather:
     * c.getTimeInMillis() + "000000" = 19+ digits (already nanoseconds) - Zipload:
     * c.getTimeInMillis() = 13 digits (milliseconds)
     *
     * InfluxDB expects nanoseconds, so we need to detect the format and convert
     * accordingly.
     *
     * @param timeStr
     *            The time string to convert
     * @return Time value in nanoseconds
     */
    private long convertToNanoseconds(String timeStr) {
        int length = timeStr.length();
        long timeValue = Long.parseLong(timeStr);

        if (length >= 19) {
            // Already nanoseconds (weather format: ms + "000000")
            return timeValue;
        } else if (length >= 16) {
            // Microseconds - multiply by 1000
            return timeValue * 1000;
        } else {
            // Milliseconds (zipload format) - multiply by 1,000,000
            return timeValue * 1_000_000;
        }
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
                    String dataString = event.getData().toString();
                    String measurementName = appOrServiceid;

                    // If no app/service specified, try to extract datatype from message
                    if (measurementName == null) {
                        JsonElement data = JsonParser.parseString(dataString);
                        if (data.isJsonObject()) {
                            JsonObject dataObj = data.getAsJsonObject();
                            if (dataObj.has("datatype") && !dataObj.get("datatype").isJsonNull()) {
                                measurementName = dataObj.get("datatype").getAsString();
                            }
                        }
                    }

                    if (measurementName != null) {
                        provenClient.sendBulkData(dataString, measurementName, instanceId, simulationId,
                                new Date().getTime());
                    }
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

    @Override
    public void storeSimulationOutput(String simulationId) throws Exception {
        subscribeAndStoreDataFromTopic(GridAppsDConstants.topic_simulation + ".output." + simulationId,
                "simulation", null, simulationId);
    }

    @Override
    public void storeSimulationInput(String simulationId) throws Exception {
        subscribeAndStoreDataFromTopic(GridAppsDConstants.topic_simulation + ".input." + simulationId,
                "simulation", null, simulationId);
    }

    @Override
    public void storeServiceOutput(String simulationId, String serviceId, String instanceId) throws Exception {
        subscribeAndStoreDataFromTopic(
                GridAppsDConstants.topic_simulation + "." + serviceId + "." + simulationId + ".output",
                serviceId, instanceId, simulationId);
    }

    @Override
    public void storeServiceInput(String simulationId, String serviceId, String instanceId) throws Exception {
        subscribeAndStoreDataFromTopic(
                GridAppsDConstants.topic_simulation + "." + serviceId + "." + simulationId + ".input",
                serviceId, instanceId, simulationId);
    }

    @Override
    public void storeAppOutput(String simulationId, String appId, String instanceId) throws Exception {
        subscribeAndStoreDataFromTopic(
                GridAppsDConstants.topic_simulation + "." + appId + "." + simulationId + ".output", appId,
                instanceId, simulationId);
    }

    @Override
    public void storeAppInput(String simulationId, String appId, String instanceId) throws Exception {
        subscribeAndStoreDataFromTopic(
                GridAppsDConstants.topic_simulation + "." + appId + "." + simulationId + ".input", appId,
                instanceId, simulationId);
    }
}
