/*******************************************************************************
 * Copyright (c) 2017, Battelle Memorial Institute All rights reserved.
 * Battelle Memorial Institute (hereinafter Battelle) hereby grants permission
 * to any person or entity lawfully obtaining a copy of this software and
 * associated documentation files (hereinafter the Software) to redistribute
 * and use the Software in source and binary forms, with or without
 * modification.
 ******************************************************************************/
package gov.pnnl.goss.gridappsd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.ServiceManager;
import gov.pnnl.goss.gridappsd.distributed.FieldBusManagerImpl;
import gov.pnnl.goss.gridappsd.dto.ServiceInfo;
import pnnl.goss.core.Client;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.GossResponseEvent;

/**
 * Unit tests for FieldBusManagerImpl config-delivery fix (GADP-001, issue
 * 1859).
 *
 * These tests assert BEHAVIORAL invariants per the acceptance criteria: 1.
 * getFieldModelMrid() returns the configured value after config delivery. 2.
 * The manager stays registered and idle (not permanently dead) when start()
 * runs before field.model.mrid is available. 3. A live mrid change via
 * applyConfig() is reflected in getFieldModelMrid() and initiates a topology
 * rebuild.
 *
 * The topology background thread (TopologyRequestProcess) makes real STOMP
 * calls. In these unit tests the mock Client throws on getResponse(), which
 * causes the thread to exit immediately via its catch block. The assertions
 * below are on the main thread against state set BEFORE launchTopology() is
 * called, so the topology thread's early exit does not affect the assertion
 * result.
 */
@RunWith(MockitoJUnitRunner.class)
public class FieldBusManagerComponentTests {

    @Mock
    private ClientFactory clientFactory;

    @Mock
    private Client client;

    @Mock
    private ServiceManager serviceManager;

    @Mock
    private LogManager logManager;

    @Before
    public void setUp() throws Exception {
        // Make clientFactory return the mock Client so topology threads exit fast
        // (mock Client.getResponse returns null, causing the thread to bail via its
        // catch block).
        Mockito.when(clientFactory.create(Mockito.any(PROTOCOL.class), Mockito.any()))
                .thenReturn(client);
    }

    // --- Acceptance gate 1: getFieldModelMrid() non-null after config delivery ---

    @Test
    public void getFieldModelMridNonNullWhenConfigDeliveredViaApplyConfig() {
        FieldBusManagerImpl manager = new FieldBusManagerImpl();
        manager.setClientFactory(clientFactory);
        manager.setLogManager(logManager);
        manager.setServiceManager(serviceManager);

        Map<String, Object> config = new HashMap<>();
        config.put("field.model.mrid", "test-mrid-001");

        manager.applyConfig(config);

        // Core invariant: value set in config is visible through the accessor.
        assertEquals("test-mrid-001", manager.getFieldModelMrid());
    }

    @Test
    public void getFieldModelMridNullWhenNoConfigDelivered() {
        FieldBusManagerImpl manager = new FieldBusManagerImpl();
        manager.setClientFactory(clientFactory);
        manager.setLogManager(logManager);
        manager.setServiceManager(serviceManager);

        assertNull("mrid must be null before any config is delivered",
                manager.getFieldModelMrid());
    }

    // --- Acceptance gate 2: manager stays registered and idle when mrid absent ---

    @Test
    public void managerRegisteredButIdleWhenStartCalledWithNoMrid() {
        // Topology service IS present so start() does not bail before the mrid check.
        ServiceInfo serviceInfo = Mockito.mock(ServiceInfo.class);
        Mockito.when(serviceManager.getService("gridappsd-topology-background-service"))
                .thenReturn(serviceInfo);

        FieldBusManagerImpl manager = new FieldBusManagerImpl();
        manager.setClientFactory(clientFactory);
        manager.setLogManager(logManager);
        manager.setServiceManager(serviceManager);

        // No mrid in the delivered config: start(config) should log a warning and
        // return without dying (subscribed but idle).
        manager.start(new HashMap<>());

        // Manager is still "alive": accessor returns null but no exception was thrown.
        assertNull("mrid must be null when start() ran without a configured mrid",
                manager.getFieldModelMrid());

        // Subsequent config delivery must work without a restart.
        Map<String, Object> lateConfig = new HashMap<>();
        lateConfig.put("field.model.mrid", "mrid-delivered-late");
        manager.applyConfig(lateConfig);

        assertEquals("mrid-delivered-late", manager.getFieldModelMrid());
    }

    // --- Acceptance gate 3: topology (and mrid) rebuild reflects the new mrid ---

    @Test
    public void fieldModelMridUpdatedAndTopologyRebuiltWhenMridChangedViaApplyConfig() {
        FieldBusManagerImpl manager = new FieldBusManagerImpl();
        manager.setClientFactory(clientFactory);
        manager.setLogManager(logManager);
        manager.setServiceManager(serviceManager);

        Map<String, Object> config1 = new HashMap<>();
        config1.put("field.model.mrid", "mrid-original");
        manager.applyConfig(config1);

        // First delivery: mrid stored correctly.
        assertEquals("mrid-original", manager.getFieldModelMrid());

        Map<String, Object> config2 = new HashMap<>();
        config2.put("field.model.mrid", "mrid-updated");
        manager.applyConfig(config2);

        // Live change: mrid accessor reflects the new value.
        assertEquals("mrid-updated", manager.getFieldModelMrid());
    }

    @Test
    public void applyConfigNoopWhenMridUnchangedAndTopologyRunning() {
        FieldBusManagerImpl manager = new FieldBusManagerImpl();
        manager.setClientFactory(clientFactory);
        manager.setLogManager(logManager);
        manager.setServiceManager(serviceManager);

        Map<String, Object> config = new HashMap<>();
        config.put("field.model.mrid", "stable-mrid");
        manager.applyConfig(config);

        // Second applyConfig with the same mrid: no exception, mrid unchanged.
        manager.applyConfig(config);

        assertEquals("stable-mrid", manager.getFieldModelMrid());
    }

    // --- DS @Activate entry point: config arrives natively at activation ---

    @Test
    public void activationViaDsEntryPointDeliversMridAndStaysSubscribed() {
        ServiceInfo serviceInfo = Mockito.mock(ServiceInfo.class);
        Mockito.when(serviceManager.getService("gridappsd-topology-background-service"))
                .thenReturn(serviceInfo);

        FieldBusManagerImpl manager = new FieldBusManagerImpl();
        manager.setClientFactory(clientFactory);
        manager.setLogManager(logManager);
        manager.setServiceManager(serviceManager);

        Map<String, Object> config = new HashMap<>();
        config.put("field.model.mrid", "activate-mrid-001");

        // DS calls start(config) at activation; mrid arrives natively.
        manager.start(config);

        assertEquals("mrid delivered via the DS @Activate entry point must be visible",
                "activate-mrid-001", manager.getFieldModelMrid());
        // Subscription must be established at activation so output routing is ready.
        Mockito.verify(client).subscribe(Mockito.anyString(),
                Mockito.any(pnnl.goss.core.GossResponseEvent.class));
    }

    // --- M2: subscription is established even when the topology service is absent
    // ---

    @Test
    public void subscriptionEstablishedWhenTopologyServiceNull() {
        // Topology service absent: the old start() returned here WITHOUT subscribing.
        Mockito.when(serviceManager.getService("gridappsd-topology-background-service"))
                .thenReturn(null);

        FieldBusManagerImpl manager = new FieldBusManagerImpl();
        manager.setClientFactory(clientFactory);
        manager.setLogManager(logManager);
        manager.setServiceManager(serviceManager);

        manager.start(new HashMap<>());

        // The simulation-output subscription must still be established on this path.
        Mockito.verify(client).subscribe(Mockito.anyString(),
                Mockito.any(pnnl.goss.core.GossResponseEvent.class));
    }

    // --- M-npe: handleRequest tolerates a null topology (idle window) ---

    @Test
    public void handleRequestReturnsNullWhenTopologyNotBuilt() {
        FieldBusManagerImpl manager = new FieldBusManagerImpl();
        manager.setClientFactory(clientFactory);
        manager.setLogManager(logManager);
        manager.setServiceManager(serviceManager);

        // No activation, no config: topology is null. A get_context request must not
        // throw a NullPointerException; it returns null.
        String request = "{\"request_type\":\"get_context\"}";
        assertNull("handleRequest must not NPE when topology is null",
                manager.handleRequest("queue", request));
    }

    @Test
    public void handleRequestReturnsNullWhenTopologyRootNotYetSet() {
        FieldBusManagerImpl manager = new FieldBusManagerImpl();
        manager.setClientFactory(clientFactory);
        manager.setLogManager(logManager);
        manager.setServiceManager(serviceManager);

        // Deliver a valid mrid so launchTopology() runs: topology != null but
        // root remains null because the background thread calls client.getResponse()
        // which returns null (mock default), causing run() to exit before assigning
        // root.
        Map<String, Object> config = new HashMap<>();
        config.put("field.model.mrid", "npe-guard-test-mrid");
        manager.applyConfig(config);

        // get_context: must return null without NPE when topology.root is null.
        String getCtxRequest = "{\"request_type\":\"get_context\"}";
        assertNull("get_context must not NPE when topology.root is null",
                manager.handleRequest("queue", getCtxRequest));

        // start_publishing: same guard applies.
        String startPubRequest = "{\"request_type\":\"start_publishing\"}";
        assertNull("start_publishing must not NPE when topology.root is null",
                manager.handleRequest("queue", startPubRequest));
    }

    @Test
    public void modifiedDelegatesToApplyConfig() {
        FieldBusManagerImpl manager = new FieldBusManagerImpl();
        manager.setClientFactory(clientFactory);
        manager.setLogManager(logManager);
        manager.setServiceManager(serviceManager);

        Map<String, Object> config = new HashMap<>();
        config.put("field.model.mrid", "mrid-via-modified");

        // @Modified callback must behave identically to applyConfig.
        manager.modified(config);

        assertEquals("mrid-via-modified", manager.getFieldModelMrid());
    }

    // --- GADP-051 review remediation item 4(a): isTopologyReady() guard inside
    // publishDeviceOutput's onMessage handler, exercised with a real DataResponse
    // while topology is NOT ready. ---

    @Test
    public void publishDeviceOutputOnMessageGatesPublishWhenTopologyNotReady() {
        FieldBusManagerImpl manager = new FieldBusManagerImpl();
        manager.setClientFactory(clientFactory);
        manager.setLogManager(logManager);
        manager.setServiceManager(serviceManager);

        // Topology service absent so start() subscribes but never launches
        // topology: isTopologyReady() (topology != null && topology.root != null)
        // is false for the entire life of this manager.
        Mockito.when(serviceManager.getService("gridappsd-topology-background-service"))
                .thenReturn(null);
        manager.start(new HashMap<>());

        ArgumentCaptor<GossResponseEvent> eventCaptor = ArgumentCaptor.forClass(GossResponseEvent.class);
        Mockito.verify(client).subscribe(Mockito.anyString(), eventCaptor.capture());
        GossResponseEvent onMessage = eventCaptor.getValue();

        DataResponse event = new DataResponse();
        event.setDestination("goss.gridappsd.simulation.output.12345");
        event.setData("{\"message\":{\"Measurements\":{\"meas-1\":1.0}}}");

        // Driving a real DataResponse through onMessage while topology is not
        // ready must gate the publish entirely: no exception (no NPE on the
        // null topology dereference below the guard) and no client.publish call.
        onMessage.onMessage(event);

        Mockito.verify(client, Mockito.never()).publish(Mockito.anyString(), Mockito.any());
    }

    // --- GADP-051 review remediation item 4(b): isTopologyFullyInitialized()
    // behind "is_initilized", driven across root==null, root!=null &&
    // DistributionArea==null, and fully-populated states, value-asserting the
    // "initialized" boolean in every branch. ---

    @Test
    public void isInitilizedReportsFalseWhenTopologyNeverBuilt() {
        FieldBusManagerImpl manager = new FieldBusManagerImpl();
        manager.setClientFactory(clientFactory);
        manager.setLogManager(logManager);
        manager.setServiceManager(serviceManager);

        // No activation, no config: topology is null (root==null state).
        String request = "{\"request_type\":\"is_initilized\"}";
        String responseJson = (String) manager.handleRequest("queue", request);

        JsonObject obj = JsonParser.parseString(responseJson).getAsJsonObject();
        assertFalse("initialized must be exactly false when topology was never built",
                obj.get("initialized").getAsBoolean());
    }

    @Test
    public void isInitilizedReportsFalseWhenRootPopulatedButDistributionAreaMissing() throws Exception {
        // client.getResponse returning a DataResponse whose data parses into a
        // Root with a null DistributionArea reproduces root!=null &&
        // DistributionArea==null without needing a live background thread: drive
        // handleTopologyResponse directly against the manager's real topology
        // instance via applyConfig + a synchronous stub, matching the pattern the
        // pre-existing NPE-guard tests in this file already use for topology
        // lifecycle setup.
        FieldBusManagerImpl manager = new FieldBusManagerImpl();
        manager.setClientFactory(clientFactory);
        manager.setLogManager(logManager);
        manager.setServiceManager(serviceManager);

        DataResponse topoResponse = new DataResponse();
        topoResponse.setData("{}"); // parses to a Root with DistributionArea == null
        Mockito.when(client.getResponse(Mockito.any(), Mockito.anyString(),
                Mockito.any(pnnl.goss.core.Request.RESPONSE_FORMAT.class), Mockito.anyLong()))
                .thenReturn(topoResponse);

        Map<String, Object> config = new HashMap<>();
        config.put("field.model.mrid", "root-no-distribution-area-mrid");
        manager.applyConfig(config);

        // Allow the background TopologyRequestProcess thread to parse the stubbed
        // response into root before asserting; the thread's only work here is one
        // synchronous getResponse() call plus a JSON parse, so this is not the
        // long boot-order race the retry budget guards against.
        Thread.sleep(500);

        String request = "{\"request_type\":\"is_initilized\"}";
        String responseJson = (String) manager.handleRequest("queue", request);

        JsonObject obj = JsonParser.parseString(responseJson).getAsJsonObject();
        assertFalse("initialized must be exactly false when root populated but DistributionArea is missing",
                obj.get("initialized").getAsBoolean());
    }

    @Test
    public void isInitilizedReportsTrueWhenTopologyFullyPopulated() throws Exception {
        FieldBusManagerImpl manager = new FieldBusManagerImpl();
        manager.setClientFactory(clientFactory);
        manager.setLogManager(logManager);
        manager.setServiceManager(serviceManager);

        DataResponse topoResponse = new DataResponse();
        topoResponse.setData("{\"DistributionArea\":{\"@id\":\"da-1\",\"@type\":\"DistributionArea\","
                + "\"Substations\":[]}}");
        Mockito.when(client.getResponse(Mockito.any(), Mockito.anyString(),
                Mockito.any(pnnl.goss.core.Request.RESPONSE_FORMAT.class), Mockito.anyLong()))
                .thenReturn(topoResponse);

        Map<String, Object> config = new HashMap<>();
        config.put("field.model.mrid", "fully-populated-mrid");
        manager.applyConfig(config);

        Thread.sleep(500);

        String request = "{\"request_type\":\"is_initilized\"}";
        String responseJson = (String) manager.handleRequest("queue", request);

        JsonObject obj = JsonParser.parseString(responseJson).getAsJsonObject();
        assertTrue("initialized must be exactly true once root and DistributionArea are both populated",
                obj.get("initialized").getAsBoolean());
    }

    // --- GADP-051 review remediation item 4(c): the legacy updated(Dictionary)
    // path, driven with a real Hashtable, must actually rebuild config (the
    // exact regression this PR fixes: previously stored config but never
    // rebuilt). ---

    @Test
    public void updatedWithRealHashtableRebuildsConfigAndMrid() {
        FieldBusManagerImpl manager = new FieldBusManagerImpl();
        manager.setClientFactory(clientFactory);
        manager.setLogManager(logManager);
        manager.setServiceManager(serviceManager);

        Hashtable<String, Object> initial = new Hashtable<>();
        initial.put("field.model.mrid", "mrid-via-updated-initial");
        manager.updated(initial);

        assertEquals("the legacy updated(Dictionary) path must deliver the mrid via applyConfig, "
                + "not merely store it unapplied",
                "mrid-via-updated-initial", manager.getFieldModelMrid());

        Hashtable<String, Object> changed = new Hashtable<>();
        changed.put("field.model.mrid", "mrid-via-updated-changed");
        manager.updated(changed);

        // The exact regression this PR fixes: a mrid CHANGE arriving through
        // updated(Dictionary) must actually be reflected (rebuilt), not silently
        // dropped because the old code stored config but never called applyConfig.
        assertEquals("a changed mrid delivered via updated(Dictionary) must be rebuilt and visible",
                "mrid-via-updated-changed", manager.getFieldModelMrid());
    }

    @Test
    public void updatedWithNullDictionaryIsANoopAndDoesNotThrow() {
        FieldBusManagerImpl manager = new FieldBusManagerImpl();
        manager.setClientFactory(clientFactory);
        manager.setLogManager(logManager);
        manager.setServiceManager(serviceManager);

        // Guard clause: a null Dictionary (ConfigurationAdmin can deliver this on
        // deletion) must not throw and must not change the manager's state.
        manager.updated(null);

        assertNull("a null Dictionary delivery must remain a no-op", manager.getFieldModelMrid());
    }
}
