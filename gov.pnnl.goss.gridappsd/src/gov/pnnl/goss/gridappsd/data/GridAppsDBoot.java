/*******************************************************************************
 * Copyright 2017, Battelle Memorial Institute All rights reserved.
 * Battelle Memorial Institute (hereinafter Battelle) hereby grants permission to any person or entity
 * lawfully obtaining a copy of this software and associated documentation files (hereinafter the
 * Software) to redistribute and use the Software in source and binary forms, with or without modification.
 ******************************************************************************/
package gov.pnnl.goss.gridappsd.data;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.server.ServerControl;

import gov.pnnl.goss.gridappsd.api.AppManager;
import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.api.FieldBusManager;
import gov.pnnl.goss.gridappsd.api.LogDataManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.PowergridModelDataManager;
import gov.pnnl.goss.gridappsd.api.RoleManager;
import gov.pnnl.goss.gridappsd.api.ServiceManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.api.TestManager;

import gov.pnnl.goss.gridappsd.app.AppManagerImpl;
import gov.pnnl.goss.gridappsd.configuration.ConfigurationManagerImpl;
import gov.pnnl.goss.gridappsd.data.conversion.ProvenLoadScheduleToGridlabdLoadScheduleConverter;
import gov.pnnl.goss.gridappsd.data.conversion.ProvenWeatherToGridlabdWeatherConverter;
import gov.pnnl.goss.gridappsd.distributed.FieldBusManagerImpl;
import gov.pnnl.goss.gridappsd.log.LogManagerImpl;
import gov.pnnl.goss.gridappsd.role.RoleManagerImpl;
import gov.pnnl.goss.gridappsd.service.ServiceManagerImpl;
import gov.pnnl.goss.gridappsd.simulation.SimulationManagerImpl;
import gov.pnnl.goss.gridappsd.testmanager.TestManagerImpl;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * Bootstrap component for GridAPPS-D services.
 *
 * This component is placed in the data/ package because Felix SCR has a bug
 * where it only loads components from certain packages (data/ and process/).
 * This bootstrap manually instantiates and registers all the missing manager
 * components that SCR fails to load.
 *
 * This is a workaround for: Felix SCR only loading 8 of 31 components from the
 * gridappsd bundle.
 */
@Component(immediate = true)
public class GridAppsDBoot {

    private static Logger log = LoggerFactory.getLogger(GridAppsDBoot.class);

    @Reference
    private volatile ClientFactory clientFactory;

    // ServerControl is the GOSS broker - depend on it to ensure broker is started
    // before we try to connect. This fixes "Connection refused" errors on startup.
    @Reference
    private volatile ServerControl serverControl;

    // LogDataManager is loaded by SCR (from data/ package) but has a circular
    // dependency
    // through GridAppsDataSourcesImpl -> ConfigurationManager
    // Make it optional so we can bootstrap first
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile LogDataManager logDataManager;

    // These services will become available AFTER we register ConfigurationManager
    // Use optional cardinality to break the circular dependency
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile DataManager dataManager;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile PowergridModelDataManager powergridModelManager;

    private BundleContext bundleContext;
    private List<ServiceRegistration<?>> registrations = new ArrayList<>();

    // Manually created instances
    private LogManagerImpl logManager;
    private ConfigurationManagerImpl configurationManager;
    private SimulationManagerImpl simulationManager;
    private AppManagerImpl appManager;
    private ServiceManagerImpl serviceManager;
    private TestManagerImpl testManager;
    private RoleManagerImpl roleManager;
    private FieldBusManagerImpl fieldBusManager;

    @Activate
    public void activate(BundleContext context) {
        this.bundleContext = context;
        log.info("GridAppsDBoot starting - manually bootstrapping missing manager components");

        try {
            bootstrapManagers();
            log.info("GridAppsDBoot completed - all manager components registered");
        } catch (Exception e) {
            log.error("GridAppsDBoot failed to bootstrap managers", e);
        }
    }

    @Deactivate
    public void deactivate() {
        log.info("GridAppsDBoot stopping - unregistering manually created services");
        for (ServiceRegistration<?> reg : registrations) {
            try {
                reg.unregister();
            } catch (Exception e) {
                log.warn("Error unregistering service", e);
            }
        }
        registrations.clear();
    }

    private void bootstrapManagers() {
        // Create LogManager first - many other components depend on it
        // LogDataManager may not be available yet due to circular dependencies
        logManager = new LogManagerImpl();
        logManager.setClientFactory(clientFactory);
        if (logDataManager != null) {
            logManager.setLogDataManager(logDataManager);
        }
        logManager.start();
        registrations.add(bundleContext.registerService(LogManager.class, logManager, new Hashtable<>()));
        log.info("Registered LogManagerImpl (LogDataManager may be bound later)");

        // Create RoleManager
        roleManager = new RoleManagerImpl();
        roleManager.setLogManager(logManager);
        roleManager.start();
        registrations.add(bundleContext.registerService(RoleManager.class, roleManager, new Hashtable<>()));
        log.info("Registered RoleManagerImpl");

        // Create AppManager - depends on LogManager, ClientFactory
        appManager = new AppManagerImpl();
        appManager.setClientFactory(clientFactory);
        appManager.setLogManager(logManager);
        appManager.start();
        registrations.add(bundleContext.registerService(AppManager.class, appManager, new Hashtable<>()));
        log.info("Registered AppManagerImpl");

        // Create ServiceManager - depends on LogManager, ClientFactory
        serviceManager = new ServiceManagerImpl();
        serviceManager.setClientFactory(clientFactory);
        serviceManager.setLogManager(logManager);
        serviceManager.start();
        registrations.add(bundleContext.registerService(ServiceManager.class, serviceManager, new Hashtable<>()));
        log.info("Registered ServiceManagerImpl");

        // Create SimulationManager - depends on LogManager, ServiceManager, AppManager
        simulationManager = new SimulationManagerImpl();
        simulationManager.setClientFactory(clientFactory);
        simulationManager.setLogManager(logManager);
        simulationManager.setServiceManager(serviceManager);
        simulationManager.setAppManager(appManager);
        try {
            simulationManager.start();
        } catch (Exception e) {
            log.error("Error starting SimulationManager", e);
        }
        registrations.add(bundleContext.registerService(SimulationManager.class, simulationManager, new Hashtable<>()));
        log.info("Registered SimulationManagerImpl");

        // Create ConfigurationManager EARLY - other components
        // (BGPowergridModelDataManagerImpl,
        // DataManagerImpl) depend on it. Register it now so they can be satisfied.
        // DataManager and PowergridModelManager may not be available yet due to
        // circular deps
        configurationManager = new ConfigurationManagerImpl();
        configurationManager.setClientFactory(clientFactory);
        configurationManager.setLogManager(logManager);
        configurationManager.setSimulationManager(simulationManager);
        // Note: dataManager and powergridModelManager may be null initially
        // They will be updated when they become available
        configurationManager.start(new java.util.HashMap<>());
        registrations.add(
                bundleContext.registerService(ConfigurationManager.class, configurationManager, new Hashtable<>()));
        log.info("Registered ConfigurationManagerImpl (DataManager/PowergridModelManager may be set later)");

        // Schedule late binding of optional dependencies
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Wait for other services to initialize
                lateBindDependencies();
            } catch (InterruptedException e) {
                log.warn("Late binding thread interrupted", e);
            }
        }, "GridAppsDBoot-LateBind").start();

        // Create TestManager - depends on LogManager, ClientFactory
        // DataManager/SimulationManager/LogDataManager may be set later
        testManager = new TestManagerImpl();
        testManager.setClientFactory(clientFactory);
        testManager.setLogManager(logManager);
        if (logDataManager != null) {
            testManager.setLogDataManager(logDataManager);
        }
        testManager.setSimulationManager(simulationManager);
        testManager.start();
        registrations.add(bundleContext.registerService(TestManager.class, testManager, new Hashtable<>()));
        log.info("Registered TestManagerImpl (some deps may be bound later)");

        // Create FieldBusManager - depends on LogManager, ServiceManager, ClientFactory
        fieldBusManager = new FieldBusManagerImpl();
        fieldBusManager.setClientFactory(clientFactory);
        fieldBusManager.setLogManager(logManager);
        fieldBusManager.setServiceManager(serviceManager);
        fieldBusManager.start();
        registrations.add(bundleContext.registerService(FieldBusManager.class, fieldBusManager, new Hashtable<>()));
        log.info("Registered FieldBusManagerImpl");
    }

    /**
     * Late-bind optional dependencies that become available after
     * ConfigurationManager is registered.
     */
    private void lateBindDependencies() {
        log.info("Late-binding optional dependencies...");

        // Try to get LogDataManager from the service registry
        if (logDataManager != null) {
            logManager.setLogDataManager(logDataManager);
            testManager.setLogDataManager(logDataManager);
            log.info("Late-bound LogDataManager to LogManager and TestManager");
        } else {
            // Try to look it up from the service registry
            ServiceReference<LogDataManager> ldmRef = bundleContext.getServiceReference(LogDataManager.class);
            if (ldmRef != null) {
                LogDataManager ldm = bundleContext.getService(ldmRef);
                if (ldm != null) {
                    logManager.setLogDataManager(ldm);
                    testManager.setLogDataManager(ldm);
                    log.info("Late-bound LogDataManager (from lookup) to LogManager and TestManager");
                }
            }
        }

        // Try to get DataManager from the service registry and bind LogManager to it
        DataManager dm = null;
        if (dataManager != null) {
            dm = dataManager;
        } else {
            // Try to look it up from the service registry
            ServiceReference<DataManager> dmRef = bundleContext.getServiceReference(DataManager.class);
            if (dmRef != null) {
                dm = bundleContext.getService(dmRef);
            }
        }

        if (dm != null) {
            configurationManager.setDataManager(dm);
            testManager.setDataManager(dm);
            // Also bind LogManager to DataManager if it's a DataManagerImpl
            if (dm instanceof DataManagerImpl) {
                ((DataManagerImpl) dm).setLogManager(logManager);
                log.info("Late-bound LogManager to DataManagerImpl");
            }
            log.info("Late-bound DataManager to ConfigurationManager and TestManager");

            // Register data format converters (SCR doesn't load them)
            registerDataConverters(dm);
        }

        // Try to get PowergridModelDataManager from the service registry
        if (powergridModelManager != null) {
            configurationManager.setPowergridModelManager(powergridModelManager);
            log.info("Late-bound PowergridModelDataManager to ConfigurationManager");
        } else {
            // Try to look it up from the service registry
            ServiceReference<PowergridModelDataManager> pmRef = bundleContext
                    .getServiceReference(PowergridModelDataManager.class);
            if (pmRef != null) {
                PowergridModelDataManager pm = bundleContext.getService(pmRef);
                if (pm != null) {
                    configurationManager.setPowergridModelManager(pm);
                    log.info("Late-bound PowergridModelDataManager (from lookup) to ConfigurationManager");
                }
            }
        }
    }

    /**
     * Register data format converters with the DataManager. These converters
     * transform data between formats (e.g., Proven timeseries to GridLAB-D format).
     * SCR fails to load these components, so we register them manually.
     */
    private void registerDataConverters(DataManager dm) {
        try {
            // Register weather converter: PROVEN_WEATHER -> GRIDLABD_WEATHER
            ProvenWeatherToGridlabdWeatherConverter weatherConverter = new ProvenWeatherToGridlabdWeatherConverter(
                    logManager, dm);
            dm.registerConverter(
                    ProvenWeatherToGridlabdWeatherConverter.INPUT_FORMAT,
                    ProvenWeatherToGridlabdWeatherConverter.OUTPUT_FORMAT,
                    weatherConverter);
            log.info("Registered ProvenWeatherToGridlabdWeatherConverter: {} -> {}",
                    ProvenWeatherToGridlabdWeatherConverter.INPUT_FORMAT,
                    ProvenWeatherToGridlabdWeatherConverter.OUTPUT_FORMAT);

            // Register load schedule converter: PROVEN_loadprofile ->
            // GRIDLABD_LOAD_SCHEDULE
            ProvenLoadScheduleToGridlabdLoadScheduleConverter loadScheduleConverter = new ProvenLoadScheduleToGridlabdLoadScheduleConverter(
                    logManager, dm);
            dm.registerConverter(
                    ProvenLoadScheduleToGridlabdLoadScheduleConverter.INPUT_FORMAT,
                    ProvenLoadScheduleToGridlabdLoadScheduleConverter.OUTPUT_FORMAT,
                    loadScheduleConverter);
            log.info("Registered ProvenLoadScheduleToGridlabdLoadScheduleConverter: {} -> {}",
                    ProvenLoadScheduleToGridlabdLoadScheduleConverter.INPUT_FORMAT,
                    ProvenLoadScheduleToGridlabdLoadScheduleConverter.OUTPUT_FORMAT);

        } catch (Exception e) {
            log.error("Failed to register data converters", e);
        }
    }
}
