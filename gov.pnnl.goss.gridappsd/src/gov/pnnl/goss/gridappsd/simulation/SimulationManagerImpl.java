/*******************************************************************************
 * Copyright 2017, Battelle Memorial Institute All rights reserved.
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
package gov.pnnl.goss.gridappsd.simulation;

import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.pnnl.goss.gridappsd.api.AppManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.ServiceManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.PowerSystemConfig;
import gov.pnnl.goss.gridappsd.dto.ServiceInfo;
import gov.pnnl.goss.gridappsd.dto.SimulationConfig;
import gov.pnnl.goss.gridappsd.dto.SimulationContext;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
// TODO: Security removed in GOSS Java 21 upgrade - needs reimplementation
//import pnnl.goss.core.security.SecurityConfig;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.server.ServerControl;

/**
 * This represents Internal Function 405 Simulation Control Manager. This is the
 * management function that controls the running/execution of the Distribution
 * Simulator (401).
 *
 * @author shar064
 */

@Component(service = SimulationManager.class)
public class SimulationManagerImpl implements SimulationManager {

    private static Logger log = LoggerFactory.getLogger(SimulationManagerImpl.class);
    final static int MAX_INIT_ATTEMPTS = 120;

    Client client = null;

    @Reference
    private volatile ClientFactory clientFactory;

    @Reference
    ServerControl serverControl;

    @Reference
    private volatile ServiceManager serviceManager;

    @Reference
    private volatile AppManager appManager;

    // TODO: Security removed in GOSS Java 21 upgrade - needs reimplementation
    // @Reference
    // private volatile SecurityConfig securityConfig;

    @Reference
    LogManager logManager;

    private Map<String, SimulationContext> simContexts = new HashMap<String, SimulationContext>();
    // private Map<String, SimulationProcess> simProcesses = new HashMap<String,
    // SimulationProcess>();

    private Hashtable<Integer, AtomicInteger> simulationPorts = new Hashtable<Integer, AtomicInteger>();

    private Random randPort = new Random();

    public SimulationManagerImpl() {
    }

    public SimulationManagerImpl(ClientFactory clientFactory, ServerControl serverControl,
            LogManager logManager) {
        this.clientFactory = clientFactory;
        this.serverControl = serverControl;
        this.logManager = logManager;
        // this.configurationManager = configurationManager;
    }

    // Setter methods for manual dependency injection (used by GridAppsDBoot)
    public void setClientFactory(ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public void setLogManager(LogManager logManager) {
        this.logManager = logManager;
    }

    public void setServiceManager(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    public void setAppManager(AppManager appManager) {
        this.appManager = appManager;
    }

    public void setConfigurationManager(gov.pnnl.goss.gridappsd.api.ConfigurationManager configurationManager) {
        // ConfigurationManager reference is not stored but might be needed for future
    }

    @Activate
    public void start() throws Exception {

        // TODO: Security removed in GOSS Java 21 upgrade - needs reimplementation
        // Credentials credentials = new UsernamePasswordCredentials(
        // securityConfig.getManagerUser(), securityConfig.getManagerPassword());
        Credentials credentials = new UsernamePasswordCredentials(
                "system", "manager");
        client = clientFactory.create(PROTOCOL.STOMP, credentials);
        logManager.info(ProcessStatus.STARTED, null, this.getClass().getSimpleName() + " Started");
    }

    @Deactivate
    public void stop() {
        try {
            if (client != null) {
                client.close();
            }
        } catch (Exception e) {
            // Suppress errors during shutdown
        }
        simContexts.clear();
        simulationPorts.clear();
    }

    /**
     * This method is called by Process Manager to start a simulation
     *
     * @param simulationId
     * @param simulationFile
     */
    @Override
    public void startSimulation(String simulationId, SimulationConfig simulationConfig, SimulationContext simContext,
            Map<String, Object> simulationContext, PowerSystemConfig powerSystemConfig) {
        // TODO: remove simulationContext parameter after refactoring service manager

        try {
            logManager.info(ProcessStatus.STARTING, simulationId, "Starting simulation " + simulationId);
        } catch (Exception e2) {
            log.warn("Error while reporting status " + e2.getMessage());
        }

        simContexts.put(simContext.getSimulationId(), simContext);

        // TODO: Security removed in GOSS Java 21 upgrade - needs reimplementation
        // SimulationProcess simProc = new SimulationProcess(simContext, serviceManager,
        // simulationConfig, simulationId, logManager, appManager, client,
        // securityConfig, simulationContext, powerSystemConfig);
        SimulationProcess simProc = new SimulationProcess(simContext, serviceManager,
                simulationConfig, simulationId, logManager, appManager, client, simulationContext, powerSystemConfig);
        // simProcesses.put(simContext.getSimulationId(), simProc);
        simProc.start();
    }

    @Override
    public void pauseSimulation(String simulationId) {
        // NOt implementing yet
        // client.publish(GridAppsDConstants.topic_FNCS_input, "{\"command\":
        // \"pause\"}");
    }

    @Override
    public void resumeSimulation(String simulationId) {
        // Not implementing yet
        // client.publish(GridAppsDConstants.topic_FNCS_input, "{\"command\":
        // \"resume\"}");

    }

    @Override
    public void endSimulation(String simulationId) {
        client.publish(GridAppsDConstants.topic_COSIM_input + "." + simulationId,
                "{\"command\": \"stop\"}");

    }

    public void removeSimulation(String simulationId) {
        endSimulation(simulationId);
    }

    /*
     * private String getPath(String key){ String path =
     * configurationManager.getConfigurationProperty(key); if(path==null){
     * log.warn("Configuration property not found, defaulting to .: "+key); path =
     * "."; } return path; }
     */

    public Map<String, SimulationContext> getSimContexts() {
        return simContexts;
    }

    @Override
    public SimulationContext getSimulationContextForId(String simulationId) {
        return this.simContexts.get(simulationId);
    }

    public int assignSimulationPort(String simulationId) throws Exception {
        Integer simIdKey = Integer.valueOf(simulationId);
        if (!simulationPorts.containsKey(simIdKey)) {
            int tempPort = 49152 + randPort.nextInt(16384);
            AtomicInteger tempPortObj = new AtomicInteger(tempPort);
            while (simulationPorts.containsValue(tempPortObj)) {
                int newTempPort = 49152 + randPort.nextInt(16384);
                tempPortObj.set(newTempPort);
            }
            simulationPorts.put(simIdKey, tempPortObj);
            return tempPortObj.get();
            // TODO: test host:port is available
        } else {
            throw new Exception("The simulation id already exists. This indicates that the simulation id is part of a"
                    + "simulation in progress.");
        }
    }

}
