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

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
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

@Component(service = FieldBusManager.class, configurationPid = "pnnl.goss.gridappsd", immediate = true)
public class FieldBusManagerImpl implements FieldBusManager {

    // Config delivery path: populated via the DS @Activate entry point at
    // activation
    // and re-applied via @Modified when ConfigAdmin updates the PID at runtime.
    private volatile Map<String, Object> configurationMap = new HashMap<>();

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
    public void start(Map<String, Object> config) {
        // DS delivers the pnnl.goss.gridappsd ConfigAdmin properties here at
        // activation. Store them before reading getFieldModelMrid(). Synchronized
        // on this so the write is ordered with respect to the applyConfig() lock
        // that the @Modified path holds (M1).
        synchronized (this) {
            if (config != null && !config.isEmpty()) {
                this.configurationMap = new HashMap<>(config);
            }
        }
        try {
            // TODO: Security removed in GOSS Java 21 upgrade - needs reimplementation
            Credentials credentials = new UsernamePasswordCredentials("system", "manager");
            client = clientFactory.create(PROTOCOL.STOMP, credentials);

            // Establish the simulation-output subscription unconditionally so output
            // routing is ready as soon as topology is built. This runs regardless of
            // whether the topology service or the mrid is available yet (M2: the old
            // topology-service-null path returned before subscribing). The onMessage
            // handler null-guards topology, so subscribing early is safe.
            this.publishDeviceOutput();

            ServiceInfo serviceInfo = serviceManager.getService("gridappsd-topology-background-service");
            if (serviceInfo == null) {
                logManager.warn(ProcessStatus.RUNNING, null,
                        "Topology daemon service is not available; FieldBusManager subscribed but idle.");
                return;
            }

            String mrid = getFieldModelMrid();
            if (mrid == null) {
                // Fail-safe: stay subscribed and idle so a later config delivery via
                // @Modified can initiate topology without a process restart. This
                // happens when activation runs before FileInstall has loaded the
                // pnnl.goss.gridappsd.cfg file into ConfigAdmin (the common boot order).
                logManager.warn(ProcessStatus.RUNNING, null,
                        "Field model mrid not available; FieldBusManager subscribed but idle. "
                                + "Check conf/pnnl.goss.gridappsd.cfg for the field.model.mrid key.");
                return;
            }

            launchTopology(mrid);

        } catch (Exception e) {
            logManager.error(ProcessStatus.ERROR, null,
                    "FieldBusManager activation failed: " + e.getMessage());
        }
    }

    @Override
    public Serializable handleRequest(String request_queue, Serializable request) {

        RequestField requestField = RequestField.parse(request.toString());

        if (requestField.request_type.equals("get_context")) {

            // Defensive guard (M-npe): topology is null during the idle window before
            // the first config delivery with a valid field.model.mrid, and root is null
            // while the background TopologyRequestProcess is still fetching the response.
            // Both states are normal; return null until topology is fully built.
            if (topology == null || topology.root == null) {
                return null;
            }

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
                if (topology != null && topology.root.DistributionArea != null) {
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

            // Defensive guard (M-npe): nothing to publish until topology is fully built.
            // topology.root is null until the background fetch completes.
            if (topology == null || topology.root == null) {
                return null;
            }

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
                if (topology == null) {
                    // Topology not yet initialized; simulation output cannot be routed.
                    return;
                }

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

    // Deliver (or re-deliver) ConfigAdmin properties to this manager.
    // Invoked by @Modified when ConfigAdmin updates the PID at runtime (and by the
    // legacy updated() path). Stores config and, when field.model.mrid is present,
    // initiates or rebuilds topology. Synchronized because the read-modify-write of
    // configurationMap and the topology teardown/rebuild must not interleave with a
    // concurrent caller off the SCR-serialized DS thread (M1).
    public synchronized void applyConfig(Map<String, Object> config) {
        if (config == null || config.isEmpty()) {
            return;
        }

        String oldMrid = getFieldModelMrid();
        this.configurationMap = new HashMap<>(config);
        String newMrid = getFieldModelMrid();

        if (newMrid == null) {
            return; // mrid still absent after config update
        }

        if (newMrid.equals(oldMrid) && topology != null) {
            return; // mrid unchanged and topology already running; no rebuild needed
        }

        // Tear down existing topology before rebuilding to apply the new mrid.
        if (topology != null) {
            topology.interrupt();
            topology = null;
            logManager.warn(ProcessStatus.RUNNING, null,
                    "Field model mrid changed; tearing down and rebuilding topology.");
        }

        // Create client if start() returned early because the topology service was
        // not yet available. Also subscribe to simulation output in that case.
        if (client == null) {
            try {
                client = clientFactory.create(PROTOCOL.STOMP,
                        new UsernamePasswordCredentials("system", "manager"));
                this.publishDeviceOutput();
            } catch (Exception e) {
                logManager.error(ProcessStatus.ERROR, null,
                        "FieldBusManager config recovery failed: " + e.getMessage());
                return;
            }
        }

        launchTopology(newMrid);
    }

    @Modified
    public void modified(Map<String, Object> config) {
        // DS invokes this when ConfigAdmin updates PID pnnl.goss.gridappsd at runtime.
        applyConfig(config);
    }

    private void launchTopology(String mrid) {
        fieldModelId = mrid;
        topology = new TopologyRequestProcess(mrid, client);
        topology.start();
    }

    // Legacy Dictionary-based config callback. Delegates to applyConfig so a
    // delivery
    // through this path rebuilds topology consistently with the DS @Modified path.
    // Previously it stored config but never rebuilt, so a mrid change arriving here
    // was silently dropped.
    public void updated(Dictionary<String, ?> config) {
        if (config == null) {
            return;
        }
        Map<String, Object> map = new HashMap<>();
        java.util.Enumeration<String> keys = config.keys();
        while (keys.hasMoreElements()) {
            String k = keys.nextElement();
            map.put(k, config.get(k));
        }
        applyConfig(map);
    }

    public String getFieldModelMrid() {
        Object value = configurationMap.get("field.model.mrid");
        return value != null ? value.toString() : null;
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
