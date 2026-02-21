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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;

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

    // LogDataManager is loaded by SCR (from data/ package) and depends on
    // GridAppsDataSources which blocks until MySQL is reachable.
    // Mandatory ref ensures GridAppsDBoot waits for MySQL before bootstrapping.
    @Reference
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
        // LogDataManager is guaranteed available (mandatory @Reference, waits for
        // MySQL)
        logManager = new LogManagerImpl();
        logManager.setClientFactory(clientFactory);
        logManager.setLogDataManager(logDataManager);
        logManager.start();
        registrations.add(bundleContext.registerService(LogManager.class, logManager, new Hashtable<>()));
        log.info("Registered LogManagerImpl with LogDataManager");

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
        testManager.setLogDataManager(logDataManager);
        testManager.setSimulationManager(simulationManager);
        testManager.start();
        registrations.add(bundleContext.registerService(TestManager.class, testManager, new Hashtable<>()));
        log.info("Registered TestManagerImpl");

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

        // LogDataManager is now a mandatory @Reference — already bound at bootstrap
        // time.

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

        // Load configuration from ConfigAdmin (populated by FileInstall from
        // pnnl.goss.gridappsd.cfg)
        // This is needed because we bypassed SCR and created ConfigurationManager
        // manually
        loadConfigAdminProperties();

        // Bootstrap UserRepositoryImpl from security-jwt bundle if SCR failed to
        // activate it. This component provides the token authentication service that
        // the viz UI needs to log in.
        bootstrapUserRepository();
    }

    /**
     * Load configuration properties from ConfigAdmin service. FileInstall reads
     * pnnl.goss.gridappsd.cfg and stores it in ConfigAdmin. Since we bypassed SCR,
     * we need to pull the config manually.
     */
    private void loadConfigAdminProperties() {
        try {
            // Look up ConfigurationAdmin service
            ServiceReference<?> caRef = bundleContext
                    .getServiceReference("org.osgi.service.cm.ConfigurationAdmin");
            if (caRef == null) {
                log.warn("ConfigurationAdmin service not available - configuration properties not loaded");
                return;
            }

            Object configAdmin = bundleContext.getService(caRef);
            if (configAdmin == null) {
                log.warn("ConfigurationAdmin service is null");
                return;
            }

            // Use reflection to call getConfiguration("pnnl.goss.gridappsd")
            // to avoid compile-time dependency on org.osgi.service.cm package
            java.lang.reflect.Method getConfig = configAdmin.getClass()
                    .getMethod("getConfiguration", String.class, String.class);
            Object configuration = getConfig.invoke(configAdmin, "pnnl.goss.gridappsd", null);

            if (configuration != null) {
                java.lang.reflect.Method getProps = configuration.getClass().getMethod("getProperties");
                Object propsObj = getProps.invoke(configuration);

                if (propsObj instanceof Dictionary) {
                    @SuppressWarnings("unchecked")
                    Dictionary<String, Object> props = (Dictionary<String, Object>) propsObj;
                    Map<String, Object> configMap = new HashMap<>();
                    Enumeration<String> keys = props.keys();
                    while (keys.hasMoreElements()) {
                        String key = keys.nextElement();
                        configMap.put(key, props.get(key));
                    }

                    if (!configMap.isEmpty()) {
                        configurationManager.start(configMap);
                        log.info("Loaded {} configuration properties from ConfigAdmin", configMap.size());
                    } else {
                        log.warn("ConfigAdmin has empty configuration for pnnl.goss.gridappsd");
                    }
                } else {
                    log.warn("ConfigAdmin configuration not yet available for pnnl.goss.gridappsd"
                            + " (FileInstall may not have loaded the .cfg file yet)");
                }
            }
        } catch (Exception e) {
            log.warn("Error loading configuration from ConfigAdmin: " + e.getMessage());
        }
    }

    /**
     * Ensure UserRepositoryImpl from the security-jwt bundle is activated.
     *
     * UserRepositoryImpl is a delayed DS component (no immediate=true). SCR
     * registers the service reference lazily but only instantiates and activates
     * the component when someone actually fetches the service. Since no other
     * component has a {@code @Reference UserRepository}, the activate() method
     * (which subscribes to the token topic) never runs.
     *
     * This method triggers lazy activation by fetching the service. If SCR hasn't
     * registered it at all, it falls back to manual instantiation via reflection.
     */
    private void bootstrapUserRepository() {
        try {
            // Check if SCR has registered UserRepository (possibly as a lazy/delayed
            // service)
            ServiceReference<?>[] existingRefs = bundleContext.getAllServiceReferences(
                    "pnnl.goss.core.security.jwt.UserRepository", null);
            if (existingRefs != null && existingRefs.length > 0) {
                // Trigger lazy activation by actually fetching the service.
                // This causes SCR to instantiate UserRepositoryImpl and call activate().
                Object userRepo = bundleContext.getService(existingRefs[0]);
                if (userRepo != null) {
                    log.info("Triggered lazy activation of UserRepositoryImpl (SCR delayed component)");
                    return;
                }
            }

            // Fallback: manually bootstrap if SCR didn't register the component at all
            log.info("UserRepository not registered by SCR - manually bootstrapping via reflection");

            // Find the security-jwt bundle
            Bundle securityJwtBundle = null;
            for (Bundle b : bundleContext.getBundles()) {
                if ("pnnl.goss.core.security-jwt".equals(b.getSymbolicName())) {
                    securityJwtBundle = b;
                    break;
                }
            }
            if (securityJwtBundle == null) {
                log.warn("security-jwt bundle not found - UserRepository will not be available");
                return;
            }
            if (securityJwtBundle.getState() != Bundle.ACTIVE) {
                log.warn("security-jwt bundle is not ACTIVE (state={}) - cannot bootstrap UserRepository",
                        securityJwtBundle.getState());
                return;
            }

            // Load UserRepositoryImpl class from the security-jwt bundle's classloader
            Class<?> userRepoClass = securityJwtBundle
                    .loadClass("pnnl.goss.core.security.jwt.UserRepositoryImpl");
            Object userRepo = userRepoClass.getDeclaredConstructor().newInstance();

            // Set clientFactory field
            Field cfField = userRepoClass.getDeclaredField("clientFactory");
            cfField.setAccessible(true);
            cfField.set(userRepo, clientFactory);

            // Look up and set SecurityConfig (GOSS core version)
            ServiceReference<?>[] scRefs = bundleContext.getAllServiceReferences(
                    "pnnl.goss.core.security.SecurityConfig", null);
            if (scRefs == null || scRefs.length == 0) {
                log.warn("SecurityConfig service not found - cannot bootstrap UserRepository");
                return;
            }
            Object secConfig = bundleContext.getService(scRefs[0]);
            Field scField = userRepoClass.getDeclaredField("securityConfig");
            scField.setAccessible(true);
            scField.set(userRepo, secConfig);

            // Look up and set RoleManager (GOSS core version, not GridAPPS-D version)
            ServiceReference<?>[] rmRefs = bundleContext.getAllServiceReferences(
                    "pnnl.goss.core.security.RoleManager", null);
            if (rmRefs == null || rmRefs.length == 0) {
                log.warn("RoleManager (core) service not found - cannot bootstrap UserRepository");
                return;
            }
            Object roleMgr = bundleContext.getService(rmRefs[0]);
            Field rmField = userRepoClass.getDeclaredField("roleManager");
            rmField.setAccessible(true);
            rmField.set(userRepo, roleMgr);

            // Load configuration properties for pnnl.goss.core.security.userfile
            Map<String, Object> configProps = loadConfigByPid("pnnl.goss.core.security.userfile");
            if (configProps == null || configProps.isEmpty()) {
                log.warn("Config pnnl.goss.core.security.userfile not available - cannot bootstrap UserRepository");
                return;
            }

            // Call activate(Map<String, Object> properties)
            Method activateMethod = userRepoClass.getMethod("activate", Map.class);
            activateMethod.invoke(userRepo, configProps);

            // Register the service using the interface name as string
            // (since UserRepository is a Private-Package class)
            registrations.add(bundleContext.registerService(
                    "pnnl.goss.core.security.jwt.UserRepository", userRepo, new Hashtable<>()));
            log.info("Manually bootstrapped and registered UserRepositoryImpl - token service is active");

        } catch (Exception e) {
            log.error("Failed to bootstrap UserRepository", e);
        }
    }

    /**
     * Load configuration properties from ConfigAdmin for a given PID.
     */
    private Map<String, Object> loadConfigByPid(String pid) {
        try {
            ServiceReference<?> caRef = bundleContext
                    .getServiceReference("org.osgi.service.cm.ConfigurationAdmin");
            if (caRef == null)
                return null;

            Object configAdmin = bundleContext.getService(caRef);
            if (configAdmin == null)
                return null;

            Method getConfig = configAdmin.getClass()
                    .getMethod("getConfiguration", String.class, String.class);
            Object configuration = getConfig.invoke(configAdmin, pid, null);

            if (configuration != null) {
                Method getProps = configuration.getClass().getMethod("getProperties");
                Object propsObj = getProps.invoke(configuration);

                if (propsObj instanceof Dictionary) {
                    @SuppressWarnings("unchecked")
                    Dictionary<String, Object> props = (Dictionary<String, Object>) propsObj;
                    Map<String, Object> configMap = new HashMap<>();
                    Enumeration<String> keys = props.keys();
                    while (keys.hasMoreElements()) {
                        String key = keys.nextElement();
                        configMap.put(key, props.get(key));
                    }
                    return configMap;
                }
            }
        } catch (Exception e) {
            log.warn("Error loading config for PID {}: {}", pid, e.getMessage());
        }
        return null;
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
