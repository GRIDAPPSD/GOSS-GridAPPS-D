/*******************************************************************************
 * Copyright (c) 2017, Battelle Memorial Institute All rights reserved.
 * Battelle Memorial Institute (hereinafter Battelle) hereby grants permission to any person or entity
 * lawfully obtaining a copy of this software and associated documentation files (hereinafter the
 * Software) to redistribute and use the Software in source and binary forms, with or without modification.
 * Such person or entity may use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and may permit others to do so, subject to the following conditions:
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the
 * following disclaimers.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided with the distribution.
 * Other than as used herein, neither the name Battelle Memorial Institute or Battelle may be used in any
 * form whatsoever without the express written consent of Battelle.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * BATTELLE OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * General disclaimer for use with OSS licenses
 *
 * This material was prepared as an account of work sponsored by an agency of the United States Government.
 * Neither the United States Government nor the United States Department of Energy, nor Battelle, nor any
 * of their employees, nor any jurisdiction or organization that has cooperated in the development of these
 * materials, makes any warranty, express or implied, or assumes any legal liability or responsibility for
 * the accuracy, completeness, or usefulness or any information, apparatus, product, software, or process
 * disclosed, or represents that its use would not infringe privately owned rights.
 *
 * Reference herein to any specific commercial product, process, or service by trade name, trademark, manufacturer,
 * or otherwise does not necessarily constitute or imply its endorsement, recommendation, or favoring by the United
 * States Government or any agency thereof, or Battelle Memorial Institute. The views and opinions of authors expressed
 * herein do not necessarily state or reflect those of the United States Government or any agency thereof.
 *
 * PACIFIC NORTHWEST NATIONAL LABORATORY operated by BATTELLE for the
 * UNITED STATES DEPARTMENT OF ENERGY under Contract DE-AC05-76RL01830
 ******************************************************************************/
package gov.pnnl.goss.gridappsd.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;

import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.ServiceManager;
import gov.pnnl.goss.gridappsd.dto.EnvironmentVariable;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.UserOptions;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.ServiceInfo;
import gov.pnnl.goss.gridappsd.dto.ServiceInfo.ServiceType;
import gov.pnnl.goss.gridappsd.dto.ServiceInstance;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.ClientFactory;
// TODO: Security removed in GOSS Java 21 upgrade - needs reimplementation
//import pnnl.goss.core.security.SecurityConfig;

@Component(service = ServiceManager.class)
public class ServiceManagerImpl implements ServiceManager {

    private static final String CONFIG_PID = "pnnl.goss.gridappsd";

    @Reference
    LogManager logManager;

    @Reference
    private volatile ClientFactory clientFactory;

    // TODO: Security removed in GOSS Java 21 upgrade - needs reimplementation
    // @Reference
    // private volatile SecurityConfig securityConfig;

    private HashMap<String, ServiceInfo> services = new HashMap<String, ServiceInfo>();

    private Dictionary<String, ?> configurationProperties;

    private HashMap<String, ServiceInstance> serviceInstances = new HashMap<String, ServiceInstance>();

    // public String simulationId;
    // public String simulationPort;

    public ServiceManagerImpl() {
    }

    /**
     *
     * @param logManager
     * @param clientFactory
     */
    public ServiceManagerImpl(LogManager logManager, ClientFactory clientFactory) {
        this.logManager = logManager;
        this.clientFactory = clientFactory;

    }

    // Setter methods for manual dependency injection (used by GridAppsDBoot)
    public void setClientFactory(ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public void setLogManager(LogManager logManager) {
        this.logManager = logManager;
    }

    /**
     *
     */
    @Activate
    public void start() {
        // statusReporter.reportStatus(String.format("Starting %s",
        // this.getClass().getName()));
        try {
            logManager.info(ProcessStatus.RUNNING, null, "Starting " + this.getClass().getName());

            scanForServices();

            logManager.info(ProcessStatus.RUNNING, null, String.format("Found %s services", services.size()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     */
    protected void scanForServices() {
        // Get directory for services from the config
        File serviceConfigDir = getServiceConfigDirectory();
        // for each service found, parse the [service].config file to create serviceinfo
        // object and add to services map
        File[] serviceconfigFiles = serviceConfigDir.listFiles(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                if (pathname.isFile() && pathname.getName().endsWith(".config"))
                    return true;
                else
                    return false;
            }
        });
        for (File serviceConfigFile : serviceconfigFiles) {
            ServiceInfo serviceInfo = parseServiceInfo(serviceConfigFile);
            services.put(serviceInfo.getId(), serviceInfo);
            if (serviceInfo.isLaunch_on_startup()) {
                startService(serviceInfo.getId(), null);
            }
        }
    }

    /**
     *
     */
    public File getServiceConfigDirectory() {
        String configDirStr = getConfigurationProperty(GridAppsDConstants.SERVICES_PATH);
        if (configDirStr == null) {
            configDirStr = "services";
        }

        File configDir = new File(configDirStr);
        if (!configDir.exists()) {
            configDir.mkdirs();
            if (!configDir.exists()) {
                throw new RuntimeException(
                        "Services directory " + configDir.getAbsolutePath() + " does not exist and cannot be created.");
            }
        }

        return configDir;

    }

    /**
     *
     * @param key
     * @return
     */
    public String getConfigurationProperty(String key) {
        if (this.configurationProperties != null) {
            Object value = this.configurationProperties.get(key);
            if (value != null)
                return value.toString();
        }
        return null;
    }

    /**
     *
     * @param serviceConfigFile
     * @return
     */
    protected ServiceInfo parseServiceInfo(File serviceConfigFile) {
        ServiceInfo serviceInfo = null;
        String serviceConfigStr;
        try {
            serviceConfigStr = new String(Files.readAllBytes(serviceConfigFile.toPath()));
            serviceInfo = ServiceInfo.parse(serviceConfigStr);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return serviceInfo;

    }

    @Override
    public List<ServiceInfo> listServices() {
        List<ServiceInfo> result = new ArrayList<ServiceInfo>();
        result.addAll(services.values());
        return result;

    }

    @Override
    public ServiceInfo getService(String service_id) {
        service_id = service_id.trim();
        return services.get(service_id);
    }

    @Override
    public String getServiceIdForInstance(String serviceInstanceId) {
        serviceInstanceId = serviceInstanceId.trim();
        return serviceInstances.get(serviceInstanceId).getService_info().getId();
    }

    @Override
    public String startService(String serviceId,
            HashMap<String, Object> runtimeOptions) {
        return startServiceForSimultion(serviceId, runtimeOptions, null);
    }

    @Override
    public String startServiceForSimultion(String serviceId, HashMap<String, Object> runtimeOptions,
            Map<String, Object> simulationContext) {

        String simulationId = null;

        if (simulationContext != null && simulationContext.get("simulationId") != null) {
            simulationId = simulationContext.get("simulationId").toString();
        }

        String instanceId = serviceId + "-" + new Date().getTime();

        logManager.info(ProcessStatus.RUNNING, simulationId, "Calling start service: " + instanceId);

        // get execution path
        ServiceInfo serviceInfo = services.get(serviceId);
        if (serviceInfo == null) {
            // TODO: publish error on status topic
            throw new RuntimeException("Service not found: " + serviceId);
        }

        if (simulationId != null && serviceInfo.isLaunch_on_startup()) {
            logManager.warn(ProcessStatus.RUNNING, simulationId,
                    serviceId + " service is already running and multiple instances are not allowed");
            return null;
        }

        // are multiple allowed? if not check to see if it is already running, if it is
        // then send warning message
        if (!serviceInfo.isMultiple_instances() && listRunningServices(serviceId, simulationId).size() > 0) {
            logManager.warn(ProcessStatus.RUNNING, simulationId, serviceId
                    + " service is already running and multiple instances are not allowed for single simulation");
            return null;
        }

        File serviceDirectory = new File(getServiceConfigDirectory().getAbsolutePath()
                + File.separator + serviceId);

        ProcessBuilder processServiceBuilder = new ProcessBuilder();
        Process process = null;
        List<String> commands = new ArrayList<String>();
        Map<String, String> envVars = processServiceBuilder.environment();

        // set environment variables
        List<EnvironmentVariable> envVarList = serviceInfo.getEnvironmentVariables();
        for (EnvironmentVariable envVar : envVarList) {
            String value = envVar.getEnvValue();
            // Right now this depends on having the simulationContext set, so don't try it
            // if the simulation context is null
            if (simulationContext != null) {
                if (value.contains("(")) {
                    String[] replaceValue = StringUtils.substringsBetween(envVar.getEnvValue(), "(", ")");
                    for (String args : replaceValue) {
                        value = value.replace("(" + args + ")", simulationContext.get(args).toString());
                    }
                }
            }
            envVars.put(envVar.getEnvName(), value);
        }
        // TODO: Security removed in GOSS Java 21 upgrade - needs reimplementation
        // envVars.put("GRIDAPPSD_USER", securityConfig.getManagerUser());
        // envVars.put("GRIDAPPSD_PASSWORD", securityConfig.getManagerPassword());
        envVars.put("GRIDAPPSD_USER", "system");
        envVars.put("GRIDAPPSD_PASSWORD", "manager");
        envVars.put("GRIDAPPSD_LOG_LEVEL", logManager.getLogLevel().toString());

        // add executation command
        commands.add(serviceInfo.getExecution_path());

        // Check if static args contain any replacement values
        List<String> staticArgsList = serviceInfo.getStatic_args();
        for (String staticArg : staticArgsList) {
            if (staticArg != null) {
                // Right now this depends on having the simulationContext set, so don't try it
                // if the simulation context is null
                if (simulationContext != null) {
                    if (staticArg.contains("(")) {
                        String[] replaceArgs = StringUtils.substringsBetween(staticArg, "(", ")");
                        for (String args : replaceArgs) {
                            staticArg = staticArg.replace("(" + args + ")", simulationContext.get(args).toString());
                        }
                    }
                } else {
                    if (staticArg.contains("(field_model_mrid")) {
                        staticArg = staticArg.replace("(field_model_mrid)", this.getFieldModelMrid());
                    }
                }
                commands.add(staticArg);
            }
        }

        if (runtimeOptions != null) {
            commands.add(runtimeOptions.toString());
        }

        try {
            if (serviceInfo.getType().equals(ServiceType.PYTHON)) {

                commands.add(0, "python");
                processServiceBuilder.command(commands);
                if (serviceDirectory.exists())
                    processServiceBuilder.directory(serviceDirectory);
                processServiceBuilder.redirectErrorStream(true);
                processServiceBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);

                logManager.info(ProcessStatus.RUNNING, simulationId,
                        "Starting service with command " + String.join(" ", commands));
                process = processServiceBuilder.start();

            } else if (serviceInfo.getType().equals(ServiceType.EXE)) {

                processServiceBuilder.command(commands);
                if (serviceDirectory.exists())
                    processServiceBuilder.directory(serviceDirectory);
                processServiceBuilder.redirectErrorStream(true);
                processServiceBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                logManager.info(ProcessStatus.RUNNING, simulationId,
                        "Starting service with command " + String.join(" ", commands));
                process = processServiceBuilder.start();

            } else if (serviceInfo.getType().equals(ServiceType.JAVA)) {

                commands.add(0, "java -jar");
                processServiceBuilder.command(commands);
                if (serviceDirectory.exists())
                    processServiceBuilder.directory(serviceDirectory);
                processServiceBuilder.redirectErrorStream(true);
                processServiceBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                logManager.info(ProcessStatus.RUNNING, simulationId,
                        "Starting service with command " + String.join(" ", commands));
                process = processServiceBuilder.start();

            } else if (serviceInfo.getType().equals(ServiceType.WEB)) {

            } else {
                throw new RuntimeException("Type not recognized " + serviceInfo.getType());
            }

        } catch (IOException e) {

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String sStackTrace = sw.toString(); // stack trace as a string
            System.out.println(sStackTrace);

            StringBuilder commandString = new StringBuilder();
            for (String s : commands) {
                commandString.append(s);
                commandString.append(" ");
            }

            logManager.error(ProcessStatus.ERROR, simulationId, "Error running command + " + commandString);
            logManager.error(ProcessStatus.ERROR, simulationId, sStackTrace);
        }

        // create serviceinstance object
        ServiceInstance serviceInstance = new ServiceInstance(instanceId, serviceInfo, runtimeOptions, simulationId,
                process);
        serviceInstance.setService_info(serviceInfo);

        // add to service instances map
        serviceInstances.put(instanceId, serviceInstance);
        logManager.info(ProcessStatus.RUNNING, simulationId, "Started service: " + instanceId);

        watch(serviceInstance, simulationId);

        return instanceId;

    }

    @Override
    public List<ServiceInstance> listRunningServices() {
        List<ServiceInstance> result = new ArrayList<ServiceInstance>();
        result.addAll(serviceInstances.values());
        return result;
    }

    @Override
    public List<ServiceInstance> listRunningServices(String serviceId, String simulationId) {
        List<ServiceInstance> result = new ArrayList<ServiceInstance>();
        for (String instanceId : serviceInstances.keySet()) {
            ServiceInstance instance = serviceInstances.get(instanceId);
            if (instance.getService_info().getId().equals(serviceId)) {
                if (simulationId != null && instance.getSimulation_id().equals(simulationId))
                    result.add(instance);
            }
        }
        return result;
    }

    @Override
    public void stopService(String serviceId) {
        serviceId = serviceId.trim();
        for (ServiceInstance instance : listRunningServices(serviceId, null)) {
            if (instance.getService_info().getId().equals(serviceId)) {
                stopServiceInstance(instance.getInstance_id());
            }
        }

    }

    @Override
    public void stopServiceInstance(String instanceId) {
        instanceId = instanceId.trim();
        ServiceInstance instance = serviceInstances.get(instanceId);
        instance.getProcess().destroy();
        try {
            instance.getProcess().waitFor(10, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            instance.getProcess().destroyForcibly();
        }
        serviceInstances.remove(instanceId);
    }

    @Override
    public void registerService(ServiceInfo serviceInfo, Serializable servicePackage) {
        // TODO Implement this method when service registration request comes on message
        // bus
    }

    @Override
    public void deRegisterService(String service_id) {
        // TODO Auto-generated method stub

    }

    // TODO: @ConfigurationDependency migration - This method may need refactoring
    // to use OSGi DS configuration
    // Original: @ConfigurationDependency(pid=CONFIG_PID)
    public synchronized void updated(Dictionary<String, ?> config) {
        this.configurationProperties = config;
    }

    private void watch(final ServiceInstance serviceInstance, String simulationId) {
        System.out.println("WATCHING " + serviceInstance.getInstance_id());
        new Thread() {
            public void run() {
                BufferedReader input = new BufferedReader(
                        new InputStreamReader(serviceInstance.getProcess().getInputStream()));
                String line = null;
                try {
                    while ((line = input.readLine()) != null) {
                        logManager.logMessageFromSource(ProcessStatus.RUNNING, simulationId, line,
                                serviceInstance.getInstance_id(), LogLevel.DEBUG);
                    }
                } catch (IOException e) {
                    if (!(e.getMessage().contains("Stream closed"))) {
                        e.printStackTrace();
                        logManager.logMessageFromSource(ProcessStatus.ERROR, simulationId,
                                serviceInstance.getInstance_id() + " : " + e.getMessage(),
                                serviceInstance.getInstance_id(), LogLevel.ERROR);
                    }
                }
            }
        }.start();
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
