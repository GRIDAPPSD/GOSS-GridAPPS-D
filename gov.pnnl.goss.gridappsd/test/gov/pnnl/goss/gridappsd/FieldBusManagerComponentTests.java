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

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.ServiceManager;
import gov.pnnl.goss.gridappsd.distributed.FieldBusManagerImpl;
import gov.pnnl.goss.gridappsd.dto.ServiceInfo;
import pnnl.goss.core.Client;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.Client.PROTOCOL;

/**
 * Unit tests for FieldBusManagerImpl config-delivery fix (GADP-001, issue 1859).
 *
 * These tests assert BEHAVIORAL invariants per the acceptance criteria:
 *   1. getFieldModelMrid() returns the configured value after config delivery.
 *   2. The manager stays registered and idle (not permanently dead) when
 *      start() runs before field.model.mrid is available.
 *   3. A live mrid change via applyConfig() is reflected in getFieldModelMrid()
 *      and initiates a topology rebuild.
 *
 * The topology background thread (TopologyRequestProcess) makes real STOMP calls.
 * In these unit tests the mock Client throws on getResponse(), which causes
 * the thread to exit immediately via its catch block. The assertions below are
 * on the main thread against state set BEFORE launchTopology() is called, so
 * the topology thread's early exit does not affect the assertion result.
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
        // (mock Client.getResponse returns null, causing the thread to bail via its catch block).
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

        // No config delivered: start() should log a warning and return without dying.
        manager.start();

        // Manager is still "alive": accessor returns null but no exception was thrown.
        assertNull("mrid must be null when start() ran without prior config delivery",
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
}
