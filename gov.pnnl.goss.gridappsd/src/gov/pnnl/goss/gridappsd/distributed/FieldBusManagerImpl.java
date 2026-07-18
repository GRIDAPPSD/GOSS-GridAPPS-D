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
            if (!isTopologyReady()) {
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
                // Explicit null-guard on root (M-npe / GADP-005): topology.root is null
                // during the idle window before the background fetch populates it, and
                // when the topology response never arrived. Not-initialized is the
                // correct answer in both cases; do not dereference a null root.
                if (isTopologyFullyInitialized()) {
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
            if (!isTopologyReady()) {
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
                if (!isTopologyReady()) {
                    // Topology not yet initialized (or the topology response never
                    // arrived, GADP-005); simulation output cannot be routed. The
                    // fallback message-bus id below dereferences topology.root, so idle
                    // here rather than NPE.
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
        topology = new TopologyRequestProcess(mrid, client, logManager);
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

    // True once the background TopologyRequestProcess has parsed a response into
    // a non-null root. Callers that only need the root tree (get_context,
    // start_publishing, publishDeviceOutput's onMessage) use this guard rather
    // than repeating the topology/root null check at each call site.
    private boolean isTopologyReady() {
        return topology != null && topology.root != null;
    }

    // True once root AND its DistributionArea have both populated: the stricter
    // guard used by is_initilized, which reports "initialized" only when the
    // full tree that downstream callers expect to traverse is present, not just
    // a root shell.
    private boolean isTopologyFullyInitialized() {
        return isTopologyReady() && topology.root.DistributionArea != null;
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

    static final String TOPOLOGY_REQUEST_TOPIC = "goss.gridappsd.request.data.cimtopology";

    // Bounded retry: the topology background service is registered but may not
    // yet answer on the request queue at startup (a boot-order race). Give it
    // up to this many attempts, sleeping between them, before idling.
    static final int MAX_TOPOLOGY_ATTEMPTS = 6;
    static final long TOPOLOGY_RETRY_SLEEP_MS = 1000L;

    String fieldModelMrid;
    Client client;
    LogManager logManager;
    Root root = null;
    Map<String, ArrayList<FieldObject>> messageBus_measIds_map = new HashMap<String, ArrayList<FieldObject>>();
    Map<String, String> measId_messageBus_map = new HashMap<String, String>();

    public TopologyRequestProcess(String fieldModelMrid, Client client, LogManager logManager) {
        this.fieldModelMrid = fieldModelMrid;
        this.client = client;
        this.logManager = logManager;
    }

    @Override
    public void run() {
        try {

            TopologyRequest request = new TopologyRequest();
            request.mRID = fieldModelMrid;

            Serializable topoResponse = requestTopologyWithRetry(request);

            // Null-idle fail-safe (GADP-005): when the topology service never answers,
            // handleTopologyResponse leaves root null and logs an actionable warning
            // rather than dereferencing null. Parse and downstream measurement lookup
            // run only when the response actually populated root.
            if (handleTopologyResponse(topoResponse)) {
                this.getFieldMeasurementIds(fieldModelMrid);
            }

        } catch (InterruptedException e) {
            // Expected on legitimate teardown: applyConfig() calls topology.interrupt()
            // when the field model mrid changes, which unblocks Thread.sleep here.
            // Restore the interrupt flag so any caller further up the stack still
            // observes it, and log at debug/info rather than treating it as an error.
            Thread.currentThread().interrupt();
            if (logManager != null) {
                logManager.info(ProcessStatus.RUNNING, null,
                        "TopologyRequestProcess interrupted for field model mrid "
                                + sanitizeForLog(fieldModelMrid) + "; stopping.");
            }
        } catch (Exception e) {
            if (logManager != null) {
                logManager.error(ProcessStatus.ERROR, null,
                        "TopologyRequestProcess failed for field model mrid "
                                + sanitizeForLog(fieldModelMrid) + ": " + e.getMessage());
            } else {
                e.printStackTrace();
            }
        }

    }

    // Strip CR/LF from a value before it is interpolated into a log message
    // (CWE-117 log injection). fieldModelMrid originates from ConfigAdmin config
    // and is not otherwise validated, so sanitize at the point of logging rather
    // than trusting the source.
    private static String sanitizeForLog(String value) {
        return value == null ? null : value.replace("\r", "").replace("\n", "");
    }

    // Real bounded retry: request the topology, and while the response is null and
    // attempts remain, sleep BEFORE re-requesting so the background topology
    // service has time to become ready. Replaces the earlier single-shot "if"
    // that slept after its lone retry and so never actually waited for
    // initialization.
    Serializable requestTopologyWithRetry(TopologyRequest request) throws Exception {
        Serializable topoResponse = client.getResponse(request.toString(), TOPOLOGY_REQUEST_TOPIC,
                RESPONSE_FORMAT.JSON);
        int attempt = 1;
        while (topoResponse == null && attempt < MAX_TOPOLOGY_ATTEMPTS) {
            Thread.sleep(TOPOLOGY_RETRY_SLEEP_MS);
            topoResponse = client.getResponse(request.toString(), TOPOLOGY_REQUEST_TOPIC,
                    RESPONSE_FORMAT.JSON);
            attempt++;
        }
        return topoResponse;
    }

    // Parse the topology response into root. Returns true when root was populated,
    // false when the response was null (idle fail-safe: root stays null, a warning
    // is logged, and the caller must NOT proceed to parse or measurement lookup).
    // Extracted from run() so the null and valid-parse paths are unit-testable
    // without the live STOMP client.getResponse() call.
    boolean handleTopologyResponse(Serializable topoResponse) {
        if (topoResponse == null) {
            // Subscribed-but-idle: no topology available. Recoverable on a later
            // config or topology event; do not crash the bundle (GADP-001 posture).
            if (logManager != null) {
                logManager.warn(ProcessStatus.RUNNING, null,
                        "Topology service returned no response for field model mrid "
                                + sanitizeForLog(fieldModelMrid)
                                + " after " + MAX_TOPOLOGY_ATTEMPTS
                                + " attempts; FieldBusManager subscribed but idle. "
                                + "Check that gridappsd-topology-background-service is answering "
                                + TOPOLOGY_REQUEST_TOPIC + ".");
            }
            return false;
        }

        Gson gson = new Gson();
        if (topoResponse instanceof DataResponse) {
            String str = ((DataResponse) topoResponse).getData().toString();
            root = gson.fromJson(str, Root.class);
        } else {
            root = gson.fromJson(topoResponse.toString(), Root.class);
        }
        return root != null;
    }

    public void getFieldMeasurementIds(String fieldModelMrid) {

        // Idle guard (GADP-005): run() only calls this after root is populated, but
        // guard defensively so a null or partial root leaves the measurement maps
        // empty rather than dereferencing null.
        if (root == null) {
            return;
        }
        if (root.DistributionArea == null || root.DistributionArea.Substations == null) {
            // Distinct from the never-arrived case above: the topology response
            // parsed into a non-null root, but the expected DistributionArea /
            // Substations shape is missing. Warn so this malformed-but-non-null
            // condition is visible rather than silently leaving the measurement
            // maps empty.
            if (logManager != null) {
                logManager.warn(ProcessStatus.RUNNING, null,
                        "Topology response for field model mrid " + sanitizeForLog(fieldModelMrid)
                                + " parsed but did not contain the expected DistributionArea/Substations "
                                + "shape; field measurement ids not populated.");
            }
            return;
        }

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
