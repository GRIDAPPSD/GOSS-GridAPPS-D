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
import com.northconcepts.exception.SystemException;

import jakarta.jms.JMSException;

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
        // Pass the delivered ConfigAdmin properties through so
        // TOPOLOGY_TIMEOUT_CONFIG_KEY (field.topology.request.timeout.seconds), if
        // the operator has set it, sizes this run's retry window instead of the
        // ~60s default.
        topology = new TopologyRequestProcess(mrid, client, logManager, configurationMap);
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
    // yet answer on the request queue at startup (a boot-order race observed to
    // last roughly 7 seconds; see GADP-051). Each instance derives its OWN attempt
    // count (effectiveMaxAttempts, below) from the resolved config-or-default
    // total-seconds window via computeMaxAttempts: that is the single source of
    // truth for "how many attempts fit in this budget". There is deliberately no
    // separate class-load-time constant here: a static MAX_TOPOLOGY_ATTEMPTS
    // computed once from DEFAULT_TOPOLOGY_TIMEOUT_SECONDS would only coincidentally
    // match the per-instance effectiveMaxAttempts that production retry actually
    // uses, and "MAX" would misrepresent a config-less run if the fixed sizing
    // constants below ever changed independently.
    //
    // Each attempt itself is bounded by TOPOLOGY_RESPONSE_TIMEOUT_MS
    // (GossClient.getResponse's underlying JMS receive() was previously unbounded,
    // so the very first attempt could block forever and the retry loop below would
    // never actually run). Worst-case total wait at the ~60s default budget:
    // effectiveMaxAttempts * TOPOLOGY_RESPONSE_TIMEOUT_MS +
    // (effectiveMaxAttempts - 1) * TOPOLOGY_RETRY_SLEEP_MS = 10*5000 + 9*1000 =
    // 59s.
    // Widened from the original 19s window (5 attempts * 3000ms + 4*1000ms) because
    // a slow host, a cold JVM, or a heavier feeder's warmup can legitimately take
    // longer than 19s to start answering topology requests; the original window
    // produced a FALSE failure (idle FieldBusManager, unpopulated topology map) on
    // those slower runs even though the topology service was still coming up, not
    // actually broken. The first successful attempt still returns immediately: this
    // budget only bounds the worst case where every earlier attempt legitimately
    // failed.
    //
    // Operator-configurable: this ~60s figure is the DEFAULT, not a hard ceiling.
    // Operators can widen (or narrow) the TOTAL window via the ConfigAdmin PID
    // pnnl.goss.gridappsd property TOPOLOGY_TIMEOUT_CONFIG_KEY below, delivered
    // through the exact same config path FieldBusManagerImpl.getFieldModelMrid()
    // already reads field.model.mrid through (FileInstall watches
    // conf/pnnl.goss.gridappsd.cfg and hot-reloads this PID with no bundle
    // restart). The operator sets a total-seconds budget because "how long
    // should this wait" is the real intent; TOPOLOGY_RESPONSE_TIMEOUT_MS and
    // TOPOLOGY_RETRY_SLEEP_MS stay fixed and the attempt count is derived from
    // the budget (computeMaxAttempts). The first successful attempt still
    // returns immediately regardless of the configured budget.
    static final long DEFAULT_TOPOLOGY_TIMEOUT_SECONDS = 60L;
    static final long TOPOLOGY_RESPONSE_TIMEOUT_MS = 5000L;
    static final long TOPOLOGY_RETRY_SLEEP_MS = 1000L;

    // Upper clamp on the operator-configured total retry-window budget, in
    // seconds (L2 remediation). Without a ceiling, a legitimate-looking but very
    // large value (e.g. an operator meaning to set a longer-than-default window
    // but fat-fingering an extra digit, such as 86400 for "one day") derives an
    // enormous effectiveMaxAttempts and leaves a retry thread spinning for
    // roughly that many hours before giving up. A malformed/oversized topology
    // response is already rejected elsewhere (GADP-051 review item 3); this is
    // the analogous guard on the CONFIGURED WINDOW SIZE itself. One hour is a
    // generous ceiling for a boot-order race that is normally seconds long.
    static final long MAX_TOPOLOGY_TIMEOUT_SECONDS = 3600L;

    // ConfigAdmin PID pnnl.goss.gridappsd property key for the operator-tunable
    // total topology-retry window, in seconds. Read from the same delivered
    // config map FieldBusManagerImpl.configurationMap already carries
    // field.model.mrid in; see resolveTopologyTimeoutSeconds below.
    static final String TOPOLOGY_TIMEOUT_CONFIG_KEY = "field.topology.request.timeout.seconds";

    String fieldModelMrid;
    Client client;
    LogManager logManager;
    Root root = null;
    Map<String, ArrayList<FieldObject>> messageBus_measIds_map = new HashMap<String, ArrayList<FieldObject>>();
    Map<String, String> measId_messageBus_map = new HashMap<String, String>();

    // The exception caught by the most recent attemptTopologyRequest call, or
    // null when the most recent attempt did not throw. Threaded through to the
    // terminal give-up warning in handleTopologyResponse so a genuine
    // auth/connection failure is reported with its true cause instead of the
    // generic "not answering" message alone (GADP-051 review item 6).
    private volatile Throwable lastAttemptFailureCause;

    // True when lastAttemptFailureCause was classified as non-retryable (see
    // isNonRetryableFailure): a failure that will not resolve itself by
    // waiting and retrying, as distinct from the benign cold-start condition
    // where the topology reply infrastructure is not wired up yet. The retry
    // loop in requestTopologyWithRetry breaks early on this rather than
    // spending the full attempt budget on a failure that cannot succeed.
    private volatile boolean lastAttemptWasNonRetryable;

    // The number of attempts this instance will actually make, derived from the
    // resolved (configured-or-default) total-seconds window. Defaults to
    // MAX_TOPOLOGY_ATTEMPTS when constructed without a config map (matches the
    // pre-existing 3-arg constructor's behavior exactly).
    final int effectiveMaxAttempts;

    public TopologyRequestProcess(String fieldModelMrid, Client client, LogManager logManager) {
        this(fieldModelMrid, client, logManager, null);
    }

    public TopologyRequestProcess(String fieldModelMrid, Client client, LogManager logManager,
            Map<String, Object> config) {
        this.fieldModelMrid = fieldModelMrid;
        this.client = client;
        this.logManager = logManager;
        long timeoutSeconds = resolveTopologyTimeoutSeconds(config, logManager);
        this.effectiveMaxAttempts = computeMaxAttempts(timeoutSeconds, TOPOLOGY_RESPONSE_TIMEOUT_MS,
                TOPOLOGY_RETRY_SLEEP_MS);
    }

    // Resolve the operator-configured total retry-window budget, in seconds,
    // from the delivered ConfigAdmin properties. Falls back to the ~60s default
    // (DEFAULT_TOPOLOGY_TIMEOUT_SECONDS) when the property is absent, blank,
    // non-numeric, or non-positive. An invalid or missing value must never
    // silently produce a degenerate (near-zero-attempt) window: that would
    // convert an operator typo into an unexplained, near-instant topology
    // failure at cold start (data-invariants Rule 2: no default that silently
    // corrupts the intended behavior). A parse or range failure is logged at
    // warn so the operator can see and fix the bad config value; an absent
    // property is expected and logged at debug only.
    static long resolveTopologyTimeoutSeconds(Map<String, Object> config, LogManager logManager) {
        if (config == null) {
            return DEFAULT_TOPOLOGY_TIMEOUT_SECONDS;
        }
        Object raw = config.get(TOPOLOGY_TIMEOUT_CONFIG_KEY);
        if (raw == null) {
            if (logManager != null) {
                logManager.debug(ProcessStatus.RUNNING, null,
                        TOPOLOGY_TIMEOUT_CONFIG_KEY + " not configured; using default "
                                + DEFAULT_TOPOLOGY_TIMEOUT_SECONDS + "s topology retry window.");
            }
            return DEFAULT_TOPOLOGY_TIMEOUT_SECONDS;
        }
        String text = raw.toString().trim();
        try {
            long parsed = Long.parseLong(text);
            if (parsed <= 0) {
                if (logManager != null) {
                    logManager.warn(ProcessStatus.RUNNING, null,
                            TOPOLOGY_TIMEOUT_CONFIG_KEY + "=" + text
                                    + " is not positive; falling back to the default "
                                    + DEFAULT_TOPOLOGY_TIMEOUT_SECONDS + "s topology retry window.");
                }
                return DEFAULT_TOPOLOGY_TIMEOUT_SECONDS;
            }
            if (parsed > MAX_TOPOLOGY_TIMEOUT_SECONDS) {
                // L2 remediation: an operator-configured window beyond the sane
                // ceiling (see MAX_TOPOLOGY_TIMEOUT_SECONDS) is clamped rather
                // than honored outright, with a WARN so the clamp itself is
                // visible rather than a silent behavior change. The default
                // (60s) and any normal small override stay well under this
                // ceiling and are returned unclamped above.
                if (logManager != null) {
                    logManager.warn(ProcessStatus.RUNNING, null,
                            TOPOLOGY_TIMEOUT_CONFIG_KEY + "=" + text
                                    + " exceeds the " + MAX_TOPOLOGY_TIMEOUT_SECONDS
                                    + "s cap; clamping to " + MAX_TOPOLOGY_TIMEOUT_SECONDS
                                    + "s.");
                }
                return MAX_TOPOLOGY_TIMEOUT_SECONDS;
            }
            return parsed;
        } catch (NumberFormatException e) {
            if (logManager != null) {
                logManager.warn(ProcessStatus.RUNNING, null,
                        TOPOLOGY_TIMEOUT_CONFIG_KEY + "=" + text
                                + " is not a valid number; falling back to the default "
                                + DEFAULT_TOPOLOGY_TIMEOUT_SECONDS + "s topology retry window.");
            }
            return DEFAULT_TOPOLOGY_TIMEOUT_SECONDS;
        }
    }

    // Derive the number of attempts that fit within the given total-seconds
    // budget, given the fixed per-attempt timeout and inter-attempt sleep.
    // attempts*(timeout+sleep) <= totalMs + sleep (the final attempt carries no
    // trailing sleep), solved for the largest integer attempts satisfying that,
    // floored at 1 so even a very small configured budget still tries once
    // rather than never attempting at all.
    static int computeMaxAttempts(long totalWindowSeconds, long perAttemptTimeoutMs, long retrySleepMs) {
        long totalMs = totalWindowSeconds * 1000L;
        long attempts = (totalMs + retrySleepMs) / (perAttemptTimeoutMs + retrySleepMs);
        return (int) Math.max(1, attempts);
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
    // service has time to become ready. Each request itself is bounded by
    // TOPOLOGY_RESPONSE_TIMEOUT_MS (GADP-051) rather than blocking indefinitely,
    // so a not-yet-answerable service on the first attempt cannot starve every
    // later attempt of the chance to retry.
    //
    // GOSS-023 composition note: client.getResponse now throws SystemException
    // (RuntimeException) on a genuine transport failure instead of coercing it
    // into the same null result as a clean timeout (see GossClient.getResponse).
    // During cold start the topology reply infrastructure is not wired up yet,
    // so an early attempt is expected to hit exactly this condition. A thrown
    // SystemException on an attempt carries the SAME retry intent as a null
    // response: log it with context, sleep, and retry, rather than letting it
    // escape and kill this thread after a single attempt.
    // GADP-051 review item 6: a genuine non-retryable cause (unambiguously
    // JMSSecurityException/JMSSecurityRuntimeException, i.e. authentication or
    // authorization rejected by the broker) breaks this loop early rather than
    // spending the full attempt budget retrying a request that cannot succeed.
    // Every OTHER caught cause (including any other JMSException/SystemException
    // shape, which cannot be reliably told apart from the benign cold-start
    // condition where the topology reply infrastructure has simply not finished
    // wiring up yet) is retried for the FULL configured budget exactly as before:
    // that is the invariant this change must preserve. Regardless of which path
    // is taken, the last attempt's failure cause is always available to
    // handleTopologyResponse's terminal give-up log via lastAttemptFailureCause,
    // so an operator debugging a give-up sees the true cause either way.
    Serializable requestTopologyWithRetry(TopologyRequest request) throws Exception {
        lastAttemptFailureCause = null;
        lastAttemptWasNonRetryable = false;
        Serializable topoResponse = attemptTopologyRequest(request, 1);
        int attempt = 1;
        while (topoResponse == null && !lastAttemptWasNonRetryable && attempt < effectiveMaxAttempts) {
            Thread.sleep(TOPOLOGY_RETRY_SLEEP_MS);
            attempt++;
            topoResponse = attemptTopologyRequest(request, attempt);
        }
        return topoResponse;
    }

    // Issue a single bounded topology request. A SystemException or JMSException
    // thrown by client.getResponse (the topology reply infrastructure not yet
    // wired up, or another genuine transport failure) is caught, logged with the
    // attempt number and cause, and treated as "not ready yet": null is returned
    // so the caller's retry loop above continues exactly as it does for a clean
    // timeout, UNLESS the cause is classified non-retryable (see
    // isNonRetryableFailure), in which case the loop above breaks early instead
    // of spending the remaining budget. This keeps the retry contract sound
    // after GOSS-023 stopped swallowing transport failures into a fast null
    // return, and after Client.getResponse's core-api signature added a
    // declared JMSException alongside SystemException.
    private Serializable attemptTopologyRequest(TopologyRequest request, int attempt) {
        try {
            Serializable result = client.getResponse(request.toString(), TOPOLOGY_REQUEST_TOPIC,
                    RESPONSE_FORMAT.JSON, TOPOLOGY_RESPONSE_TIMEOUT_MS);
            if (result != null) {
                lastAttemptFailureCause = null;
                lastAttemptWasNonRetryable = false;
            }
            return result;
        } catch (SystemException | JMSException e) {
            lastAttemptFailureCause = e;
            lastAttemptWasNonRetryable = isNonRetryableFailure(e);
            if (logManager != null) {
                logManager.warn(ProcessStatus.RUNNING, null,
                        "Topology request attempt " + attempt + "/" + effectiveMaxAttempts
                                + " for field model mrid " + sanitizeForLog(fieldModelMrid)
                                + " failed: " + e.getMessage()
                                + (lastAttemptWasNonRetryable
                                        ? "; non-retryable cause, giving up without spending remaining attempts."
                                        : "; will retry if attempts remain."));
            }
            return null;
        }
    }

    // Narrow, high-confidence classification: only an unambiguous
    // authentication/authorization rejection from the broker (surfaced as
    // JMSSecurityException, or its RuntimeException sibling
    // JMSSecurityRuntimeException, either as the caught exception itself or as
    // its cause when wrapped by SystemException.wrap) is treated as
    // non-retryable. Every other JMSException/SystemException shape is left
    // retryable: those causes cannot be reliably distinguished from the benign
    // cold-start condition (the topology reply infrastructure not yet wired
    // up), and misclassifying that cold-start case as non-retryable would
    // violate the invariant that a genuine cold-start timeout must still retry
    // the full configured budget.
    private static boolean isNonRetryableFailure(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof jakarta.jms.JMSSecurityException
                    || current instanceof jakarta.jms.JMSSecurityRuntimeException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
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
            //
            // GADP-051 review item 6: when the retry loop gave up because of an
            // actual caught failure (rather than every attempt cleanly timing
            // out with no exception at all), name the true cause here so a
            // persistent auth/connection failure is not misreported with only
            // the generic "not answering" message: an operator debugging this
            // warning needs to know whether the service never started
            // answering versus was actively rejecting the request.
            if (logManager != null) {
                Throwable cause = lastAttemptFailureCause;
                logManager.warn(ProcessStatus.RUNNING, null,
                        "Topology service returned no response for field model mrid "
                                + sanitizeForLog(fieldModelMrid)
                                + " after " + effectiveMaxAttempts
                                + " attempts; FieldBusManager subscribed but idle. "
                                + "Check that gridappsd-topology-background-service is answering "
                                + TOPOLOGY_REQUEST_TOPIC + "."
                                + (cause != null
                                        ? " Last attempt failure cause: " + cause.getMessage()
                                        : ""));
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
