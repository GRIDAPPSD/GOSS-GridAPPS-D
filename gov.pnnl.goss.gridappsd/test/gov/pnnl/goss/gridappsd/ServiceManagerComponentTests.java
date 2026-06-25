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

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.service.ServiceManagerImpl;
import pnnl.goss.core.ClientFactory;

/**
 * Unit tests for ServiceManagerImpl config-delivery fix (GADP-001, issue 1859).
 *
 * These tests assert BEHAVIORAL invariants per the acceptance criteria:
 *   1. getFieldModelMrid() returns the configured value after config delivery.
 *   2. getFieldModelMrid() returns null before any config is delivered (fail-safe).
 *   3. A live config change via modified() (the DS @Modified callback) is
 *      reflected in getFieldModelMrid() without a restart.
 *   4. getConfigurationProperty() also reads from the delivered config, so
 *      services.path is available after delivery.
 *
 * ServiceManager does NOT rebuild topology; mrid is used lazily in
 * startServiceForSimultion() for (field_model_mrid) placeholder substitution.
 * These tests cover the delivery path only, not the launch path.
 */
@RunWith(MockitoJUnitRunner.class)
public class ServiceManagerComponentTests {

    @Mock
    private LogManager logManager;

    @Mock
    private ClientFactory clientFactory;

    // --- Acceptance gate 1: getFieldModelMrid() non-null after config delivery ---

    @Test
    public void getFieldModelMridNonNullAfterApplyConfig() {
        ServiceManagerImpl manager = new ServiceManagerImpl();
        manager.setLogManager(logManager);
        manager.setClientFactory(clientFactory);

        Map<String, Object> config = new HashMap<>();
        config.put("field.model.mrid", "svc-test-mrid-001");

        manager.applyConfig(config);

        // Core invariant: value set in config is visible through the accessor.
        assertEquals("svc-test-mrid-001", manager.getFieldModelMrid());
    }

    @Test
    public void getFieldModelMridNullBeforeAnyConfig() {
        ServiceManagerImpl manager = new ServiceManagerImpl();
        manager.setLogManager(logManager);
        manager.setClientFactory(clientFactory);

        assertNull("mrid must be null before any config is delivered",
                manager.getFieldModelMrid());
    }

    // --- Acceptance gate 2: fail-safe when mrid is absent in delivered config ---

    @Test
    public void applyConfigDoesNotThrowWhenMridAbsent() {
        ServiceManagerImpl manager = new ServiceManagerImpl();
        manager.setLogManager(logManager);
        manager.setClientFactory(clientFactory);

        Map<String, Object> configWithoutMrid = new HashMap<>();
        configWithoutMrid.put("services.path", "/opt/gridappsd/services");

        // Must not throw; manager must stay usable for a later delivery.
        manager.applyConfig(configWithoutMrid);

        assertNull("mrid must remain null when not present in delivered config",
                manager.getFieldModelMrid());
    }

    @Test
    public void lateConfigDeliveryMakesMridAvailable() {
        ServiceManagerImpl manager = new ServiceManagerImpl();
        manager.setLogManager(logManager);
        manager.setClientFactory(clientFactory);

        // No config yet: mrid is null.
        assertNull(manager.getFieldModelMrid());

        // Late delivery (simulates GridAppsDBoot.loadConfigAdminProperties() call).
        Map<String, Object> lateConfig = new HashMap<>();
        lateConfig.put("field.model.mrid", "mrid-delivered-late");
        manager.applyConfig(lateConfig);

        assertEquals("mrid-delivered-late", manager.getFieldModelMrid());
    }

    // --- Acceptance gate 3: @Modified delegates to applyConfig ---

    @Test
    public void modifiedDelegatesToApplyConfig() {
        ServiceManagerImpl manager = new ServiceManagerImpl();
        manager.setLogManager(logManager);
        manager.setClientFactory(clientFactory);

        Map<String, Object> config = new HashMap<>();
        config.put("field.model.mrid", "mrid-via-modified");

        // @Modified callback must behave identically to a direct applyConfig call.
        manager.modified(config);

        assertEquals("mrid-via-modified", manager.getFieldModelMrid());
    }

    @Test
    public void secondApplyConfigOverridesMrid() {
        ServiceManagerImpl manager = new ServiceManagerImpl();
        manager.setLogManager(logManager);
        manager.setClientFactory(clientFactory);

        Map<String, Object> config1 = new HashMap<>();
        config1.put("field.model.mrid", "mrid-original");
        manager.applyConfig(config1);
        assertEquals("mrid-original", manager.getFieldModelMrid());

        Map<String, Object> config2 = new HashMap<>();
        config2.put("field.model.mrid", "mrid-updated");
        manager.applyConfig(config2);

        // Live change: accessor reflects the new value.
        assertEquals("mrid-updated", manager.getFieldModelMrid());
    }

    // --- getConfigurationProperty also reads from the delivered config ---

    @Test
    public void getConfigurationPropertyReturnsValueAfterApplyConfig() {
        ServiceManagerImpl manager = new ServiceManagerImpl();
        manager.setLogManager(logManager);
        manager.setClientFactory(clientFactory);

        Map<String, Object> config = new HashMap<>();
        config.put("services.path", "/opt/gridappsd/services");
        config.put("field.model.mrid", "some-mrid");

        manager.applyConfig(config);

        assertEquals("/opt/gridappsd/services",
                manager.getConfigurationProperty("services.path"));
        assertEquals("some-mrid",
                manager.getConfigurationProperty("field.model.mrid"));
    }

    @Test
    public void getConfigurationPropertyNullBeforeConfig() {
        ServiceManagerImpl manager = new ServiceManagerImpl();
        manager.setLogManager(logManager);
        manager.setClientFactory(clientFactory);

        assertNull(manager.getConfigurationProperty("services.path"));
    }

    // --- DS @Activate entry point: config arrives natively at activation ---

    @Test
    public void activationViaDsEntryPointDeliversConfig() throws Exception {
        // A fresh temp services dir keeps scanForServices() side-effect free.
        File tmpServices = Files.createTempDirectory("svc-mgr-test").toFile();
        tmpServices.deleteOnExit();

        ServiceManagerImpl manager = new ServiceManagerImpl();
        manager.setLogManager(logManager);
        manager.setClientFactory(clientFactory);

        Map<String, Object> config = new HashMap<>();
        config.put("services.path", tmpServices.getAbsolutePath());
        config.put("field.model.mrid", "activate-mrid-001");

        // DS calls start(config) at activation; config is delivered here, not pushed
        // manually by GridAppsDBoot. After activation the accessors see the values.
        manager.start(config);

        assertEquals("mrid delivered via the DS @Activate entry point must be visible",
                "activate-mrid-001", manager.getFieldModelMrid());
        assertEquals("services.path delivered via @Activate must be visible",
                tmpServices.getAbsolutePath(),
                manager.getConfigurationProperty("services.path"));
    }
}
