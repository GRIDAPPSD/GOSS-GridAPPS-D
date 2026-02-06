package gov.pnnl.goss.gridappsd.distributed;

import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gov.pnnl.goss.gridappsd.api.FieldBusManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.ServiceManager;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.ServiceInfo;
import gov.pnnl.goss.gridappsd.dto.field.FieldObject;
import gov.pnnl.goss.gridappsd.dto.field.NormalEnergizedFeeder;
import gov.pnnl.goss.gridappsd.dto.field.RequestField;
import gov.pnnl.goss.gridappsd.dto.field.Root;
import gov.pnnl.goss.gridappsd.dto.field.SecondaryArea;
import gov.pnnl.goss.gridappsd.dto.field.Substation;
import gov.pnnl.goss.gridappsd.dto.field.SwitchArea;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.core.Request.RESPONSE_FORMAT;
// TODO: Security removed in GOSS Java 21 upgrade - needs reimplementation
//import pnnl.goss.core.security.SecurityConfig;

@Component(service = FieldBusManager.class)
public class FieldBusManagerImpl implements FieldBusManager {

    private static final String CONFIG_PID = "pnnl.goss.gridappsd";
    private Dictionary<String, ?> configurationProperties;

    String topology_reponse;
    String topicPrefix = "goss.gridappsd.process.request.field";

    @Reference
    ClientFactory clientFactory;

    @Reference
    private volatile ServiceManager serviceManager;

    @Reference
    private volatile LogManager logManager;

    private volatile TopologyRequestProcess topology;

    // TODO: Security removed in GOSS Java 21 upgrade - needs reimplementation
    // @Reference
    // SecurityConfig securityConfig;

    Client client;

    Map<String, List<String>> messageBus_measIds_map = new HashMap<String, List<String>>();
    // Map<String, String> measId_messageBus_map = new HashMap<String, String>();

    String fieldModelId = null;

    // FileWriter writer = null;

    public FieldBusManagerImpl() {
        System.out.println("Starting FieldBusManager");
    }

    // Setter methods for manual dependency injection (used by GridAppsDBoot)
    public void setClientFactory(ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public void setServiceManager(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    public void setLogManager(LogManager logManager) {
        this.logManager = logManager;
    }

    @Activate
    public void start() {

        try {

            // TODO: Security removed in GOSS Java 21 upgrade - needs reimplementation
            // Credentials credentials = new
            // UsernamePasswordCredentials(securityConfig.getManagerUser(),
            // securityConfig.getManagerPassword());
            Credentials credentials = new UsernamePasswordCredentials("system",
                    "manager");
            client = clientFactory.create(PROTOCOL.STOMP, credentials);

            ServiceInfo serviceInfo = serviceManager.getService("gridappsd-topology-background-service");
            if (serviceInfo == null) {
                logManager.warn(ProcessStatus.RUNNING, null,
                        "Topology deamon service is not available. Stopping FieldBusManager.");
                return;
            }

            fieldModelId = getFieldModelMrid();
            if (fieldModelId == null) {
                logManager.warn(ProcessStatus.RUNNING, null,
                        "Field model mrid is not available. Stopping FieldBusManager. "
                                + "Check conf/pnnl.goss.gridappsd.cfg file and add field.model.mrid key with value of deployed field model mrid. ");
                return;
            }

            topology = new TopologyRequestProcess(fieldModelId, client);
            topology.start();

            this.publishDeviceOutput();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public Serializable handleRequest(String request_queue, Serializable request) {

        RequestField requestField = RequestField.parse(request.toString());

        if (requestField.request_type.equals("get_context")) {

            if (requestField.areaId == null)
                return topology.root.DistributionArea;
            else {
                for (Substation substation : topology.root.DistributionArea.Substations) {
                    if (requestField.areaId.equalsIgnoreCase(substation.id))
                        return substation;

                    for (NormalEnergizedFeeder feeder : substation.NormalEnergizedFeeder) {
                        if (requestField.areaId.equalsIgnoreCase(feeder.id))
                            return feeder.FeederArea;

                        for (SwitchArea switchArea : feeder.FeederArea.SwitchAreas) {
                            if (requestField.areaId.equalsIgnoreCase(switchArea.id))
                                return switchArea;

                            for (SecondaryArea secondaryArea : switchArea.SecondaryAreas) {
                                if (requestField.areaId.equalsIgnoreCase(secondaryArea.id))
                                    return secondaryArea;
                            }
                        }
                    }
                }
            }
        } else if (requestField.request_type.equals("is_initilized")) {

            JsonObject obj = new JsonObject();

            try {
                if (topology.root.DistributionArea != null) {
                    obj.addProperty("initialized", true);
                } else {
                    obj.addProperty("initialized", false);
                }
            } catch (NullPointerException e) {
                obj.addProperty("initialized", false);
                return obj.toString();
            }

            return obj.toString();
        } else if (requestField.request_type.equals("start_publishing")) {

            for (Substation substation : topology.root.DistributionArea.Substations) {
                String topic = "goss.gridappsd.field." + (substation.id).trim().toUpperCase();
                client.publish(topic, requestField.toString());
            }

            return "Publishing Started";
        }

        return null;
    }

    public void publishDeviceOutput() {

        client.subscribe(GridAppsDConstants.topic_simulationOutput + ".>", new GossResponseEvent() {

            @Override
            public void onMessage(Serializable response) {

                DataResponse event = (DataResponse) response;
                String simulationId = event.getDestination().substring(event.getDestination().lastIndexOf(".") + 1,
                        event.getDestination().length());
                String simOutputStr = event.getData().toString();
                JsonObject simOutputJsonObj = null;

                JsonElement simOutputObject = JsonParser.parseString(simOutputStr);

                if (simOutputObject.isJsonObject()) {
                    simOutputJsonObj = simOutputObject.getAsJsonObject();
                }

                JsonObject tempObj = simOutputJsonObj.getAsJsonObject("message");
                Map<String, JsonElement> expectedOutputMap = tempObj.getAsJsonObject("Measurements").entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()));

                try {

                    for (String measurementMrid : expectedOutputMap.keySet()) {

                        String messageBusId = null;
                        if (topology.measId_messageBus_map.get(measurementMrid) != null) {
                            messageBusId = "goss.gridappsd.field.simulation.output." + simulationId + "."
                                    + topology.measId_messageBus_map.get(measurementMrid);
                        } else {
                            messageBusId = "goss.gridappsd.field.simulation.output." + simulationId + "."
                                    + topology.root.DistributionArea.Substations.get(0).NormalEnergizedFeeder.get(0).id;
                        }

                        JsonObject obj = new JsonObject();
                        obj.add(measurementMrid, expectedOutputMap.get(measurementMrid));

                        client.publish(messageBusId, obj.toString());

                    }

                } catch (Exception e1) {
                    e1.printStackTrace();
                }

            }

        });

    }

    // TODO: @ConfigurationDependency migration - This method may need refactoring
    // to use OSGi DS configuration
    // Original: @ConfigurationDependency(pid = CONFIG_PID)
    public synchronized void updated(Dictionary<String, ?> config) {
        this.configurationProperties = config;
    }

    public String getFieldModelMrid() {
        if (this.configurationProperties != null) {
            Object value = this.configurationProperties.get("field.model.mrid");
            if (value != null)
                return value.toString();
        }
        return null;
    }

}

class TopologyRequest implements Serializable {

    private static final long serialVersionUID = 4279262793871885409L;

    String requestType = "GET_DISTRIBUTED_AREAS";
    String mRID = null;
    String resultFormat = "JSON";

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

}

class TopologyRequestProcess extends Thread {

    String fieldModelMrid;
    Client client;
    Root root = null;
    Map<String, ArrayList<FieldObject>> messageBus_measIds_map = new HashMap<String, ArrayList<FieldObject>>();
    Map<String, String> measId_messageBus_map = new HashMap<String, String>();

    public TopologyRequestProcess(String fieldModelMrid, Client client) {
        this.fieldModelMrid = fieldModelMrid;
        this.client = client;
    }

    @Override
    public void run() {
        try {

            String topologyRequestTopic = "goss.gridappsd.request.data.cimtopology";
            Gson gson = new Gson();
            TopologyRequest request = new TopologyRequest();
            request.mRID = fieldModelMrid;

            Serializable topoResponse = client.getResponse(request.toString(), topologyRequestTopic,
                    RESPONSE_FORMAT.JSON);
            int attempt = 1;
            if (topoResponse == null && attempt < 6) {
                // May have to wait for Topology processor to initialize
                topoResponse = client.getResponse(request.toString(), topologyRequestTopic,
                        RESPONSE_FORMAT.JSON);
                Thread.sleep(1000);
                attempt++;
            }
            if (topoResponse != null && (topoResponse instanceof DataResponse)) {
                String str = ((DataResponse) topoResponse).getData().toString();
                root = gson.fromJson(str, Root.class);
            } else {
                root = gson.fromJson(topoResponse.toString(), Root.class);
            }

            /*
             * feederList = root.feeders; if(root == null || feederList == null ||
             * feederList.size() == 0){ throw new
             * Exception("No Feeder available to create field message bus"); }
             */

            // NormalEnergizedFeeder feeder =
            // root.DistributionArea.Substations.get(0).NormalEnergizedFeeder.get(0);

            /*
             * feeder.message_bus_id = feeder.id;
             *
             * int switch_area_index = 0; for (SwitchArea switchArea :
             * feeder.FeederArea.SwitchAreas) { switchArea.message_bus_id = feeder.id + "."
             * + switch_area_index; int secondary_area_index = 0; for (SecondaryArea
             * secondaryArea : switchArea.SecondaryAreas) { secondaryArea.message_bus_id =
             * feeder.id + "." + switch_area_index + "." + secondary_area_index;
             * secondary_area_index++; } switch_area_index++; }
             */

            this.getFieldMeasurementIds(fieldModelMrid);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void getFieldMeasurementIds(String fieldModelMrid) {

        try {
            for (Substation substation : root.DistributionArea.Substations) {

                for (NormalEnergizedFeeder feeder : substation.NormalEnergizedFeeder) {
                    messageBus_measIds_map.put(feeder.id, feeder.FeederArea.Measurements);

                    for (SwitchArea switchArea : feeder.FeederArea.SwitchAreas) {
                        messageBus_measIds_map.put(switchArea.id, switchArea.Measurements);

                        for (SecondaryArea secondaryArea : switchArea.SecondaryAreas) {
                            messageBus_measIds_map.put(secondaryArea.id, secondaryArea.Measurements);
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
