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
package gov.pnnl.goss.gridappsd.process;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.google.gson.Gson;

import gov.pnnl.goss.gridappsd.api.AppManager;
import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.ServiceManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.api.TestManager;
import gov.pnnl.goss.gridappsd.configuration.DSSAllConfigurationHandler;
import gov.pnnl.goss.gridappsd.configuration.GLDAllConfigurationHandler;
import gov.pnnl.goss.gridappsd.configuration.OchreAllConfigurationHandler;
import gov.pnnl.goss.gridappsd.dto.AppInfo;
import gov.pnnl.goss.gridappsd.dto.ApplicationObject;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.ModelCreationConfig;
import gov.pnnl.goss.gridappsd.dto.PowerSystemConfig;
import gov.pnnl.goss.gridappsd.dto.RequestSimulation;
import gov.pnnl.goss.gridappsd.dto.ServiceConfig;
import gov.pnnl.goss.gridappsd.dto.ServiceInfo;
import gov.pnnl.goss.gridappsd.dto.SimulationContext;
import gov.pnnl.goss.gridappsd.dto.SimulationOutput;
import gov.pnnl.goss.gridappsd.dto.SimulationOutputObject;
import gov.pnnl.goss.gridappsd.dto.SimulatorConfig;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.DataResponse;
// TODO: Security removed in GOSS Java 21 upgrade - needs reimplementation
//import pnnl.goss.core.security.SecurityConfig;

public class ProcessNewSimulationRequest {

    public ProcessNewSimulationRequest() {
    }

    // TODO: Security removed in GOSS Java 21 upgrade - needs reimplementation
    // public ProcessNewSimulationRequest(LogManager logManager, SecurityConfig
    // securityConfig) {
    public ProcessNewSimulationRequest(LogManager logManager) {
        this.logManager = logManager;
        // TODO: Security removed in GOSS Java 21 upgrade - needs reimplementation
        // this.securityConfig = securityConfig;
    }

    private volatile LogManager logManager;
    // TODO: Security removed in GOSS Java 21 upgrade - needs reimplementation
    // private volatile SecurityConfig securityConfig;

    public void process(ConfigurationManager configurationManager,
            SimulationManager simulationManager, String simulationId,
            DataResponse event, RequestSimulation simRequest, AppManager appManager,
            ServiceManager serviceManager, TestManager testManager,
            DataManager dataManager, String username) {

        process(configurationManager, simulationManager, simulationId, simRequest,
                appManager, serviceManager, testManager, dataManager, username);
    }

    public void process(ConfigurationManager configurationManager,
            SimulationManager simulationManager, String simulationId,
            RequestSimulation simRequest, AppManager appManager,
            ServiceManager serviceManager, TestManager testManager, DataManager dataManager, String username) {

        try {

            // 1. check if simulation request is valid
            logManager.info(ProcessStatus.RUNNING, simulationId, "Parsed simulation request: " + simRequest);
            if (simRequest == null || simRequest.getPower_system_config() == null
                    || simRequest.getSimulation_config() == null) {
                logManager.info(ProcessStatus.RUNNING, simulationId,
                        "Invalid simulation request received " + simRequest);
                throw new RuntimeException("Invalid simulation request received");
            }

            // 2. Create simulation working folders for each model in power system config as
            // /tmp/simulationId/modelId
            logManager.info(ProcessStatus.RUNNING, simulationId,
                    "Creating simulation working folders for simulation Id " + simulationId);
            String tmpWorkingDir = configurationManager
                    .getConfigurationProperty(GridAppsDConstants.GRIDAPPSD_TEMP_PATH);
            if (tmpWorkingDir == null || tmpWorkingDir.trim().length() == 0) {
                logManager.error(ProcessStatus.ERROR, simulationId, "GRIDAPPSD_TEMP_PATH not configured correectly ");
                throw new Exception("GRIDAPPSD_TEMP_PATH not configured correectly");
            }
            if (!tmpWorkingDir.endsWith(File.separator)) {
                tmpWorkingDir = tmpWorkingDir + File.separator;
            }

            // Create simulation working directory
            File simulationWorkingDir = new File(tmpWorkingDir + simulationId);
            if (!simulationWorkingDir.exists()) {
                simulationWorkingDir.mkdirs();
            }

            // Create model working directories for each model in request
            for (PowerSystemConfig powerSystemConfig : simRequest.power_system_configs) {
                // Initialize simulator_config if null (can happen when not provided in request)
                if (powerSystemConfig.simulator_config == null) {
                    powerSystemConfig.simulator_config = new SimulatorConfig();
                }
                SimulatorConfig simulatorConfig = powerSystemConfig.simulator_config;
                // Default to GridLAB-D if no simulator specified
                if (simulatorConfig.getSimulator() == null || simulatorConfig.getSimulator().trim().isEmpty()) {
                    simulatorConfig.setSimulator("GridLAB-D");
                }
                File modelWorkingDir = new File(simulationWorkingDir, powerSystemConfig.getLine_name());
                simulatorConfig.simulation_work_dir = modelWorkingDir.getAbsolutePath();
                if (!modelWorkingDir.exists()) {
                    modelWorkingDir.mkdirs();
                }
            }

            // 3. Assign a port for simulation broker
            simRequest.simulation_config.simulation_broker_port = simulationManager.assignSimulationPort(simulationId);

            // 4. Set up simulation context that will be passed to managers, services and
            // apps
            Map<String, Object> simulationContext = new HashMap<String, Object>();
            simulationContext.put("request", simRequest);
            simulationContext.put("simulationId", simulationId);
            simulationContext.put("simulationHost", "127.0.0.1");
            simulationContext.put("simulationPort", simRequest.simulation_config.simulation_broker_port);
            simulationContext.put("simulationDir", simulationWorkingDir);

            SimulationContext simContext = new SimulationContext();
            simContext.setRequest(simRequest);
            simContext.simulationId = simulationId;
            simContext.simulationPort = simRequest.simulation_config.simulation_broker_port;
            simContext.simulationDir = simulationWorkingDir.getAbsolutePath();
            /*
             * if(simRequest.getSimulation_config().getSimulator_configs().getSimulator().
             * equals("GridLAB-D")) simContext.startupFile =
             * tempDataPathDir.getAbsolutePath()+File.separator+"model_startup.glm"; else
             * if(simRequest.getSimulation_config().getSimulator().equals("OCHRE"))
             * simContext.startupFile =
             * tempDataPathDir.getAbsolutePath()+File.separator+"ochre_helics_config.json";
             */
            simContext.simulationUser = username;
            /*
             * (try{ simContext.simulatorPath =
             * serviceManager.getService(simRequest.getSimulation_config().getSimulator()).
             * getExecution_path(); }catch(NullPointerException e){
             * if(serviceManager.getService(simRequest.getSimulation_config().getSimulator()
             * ) == null){ logManager.error(ProcessStatus.ERROR,
             * simulationId,"Cannot find service with id ="+simRequest.getSimulation_config(
             * ).getSimulator()); }else
             * if(serviceManager.getService(simRequest.getSimulation_config().getSimulator()
             * ).getExecution_path() == null){ logManager.error(ProcessStatus.ERROR,
             * simulationId,"Cannot find execution path for service ="+simRequest.
             * getSimulation_config().getSimulator()); } e.printStackTrace(); }
             */

            // 5. Check GridLAB-D interface to use fncs or helics
            String gldInterface = null;
            ServiceInfo gldService = serviceManager.getService("GridLAB-D");
            if (gldService != null) {
                List<String> deps = gldService.getService_dependencies();
                gldInterface = GridAppsDConstants.getGLDInterface(deps);
            }

            // 6. Generate configuration files for the requested simulator

            int numFederates = simRequest.power_system_configs.size() + 1;

            for (PowerSystemConfig powerSystemConfig : simRequest.power_system_configs) {

                logManager.info(ProcessStatus.RUNNING, simulationId,
                        "Creating simulation and power grid model files for simulation Id " + simulationId
                                + " and model id " + powerSystemConfig.Line_name);
                SimulatorConfig simulatorConfig = powerSystemConfig.simulator_config;

                String simulator = simulatorConfig.getSimulator();

                // generate config files for requested simulator
                // if requested simulator is opendss
                if (simulator.equalsIgnoreCase(DSSAllConfigurationHandler.CONFIGTARGET)) {
                    Properties simulationParams = generateSimulationParameters(simRequest, powerSystemConfig);
                    simulationParams.put(DSSAllConfigurationHandler.SIMULATIONID, simulationId);
                    simulationParams.put(DSSAllConfigurationHandler.DIRECTORY,
                            simulatorConfig.getSimulation_work_dir());
                    if (gldInterface != null) {
                        simulationParams.put(GridAppsDConstants.GRIDLABD_INTERFACE, gldInterface);
                    }
                    configurationManager.generateConfiguration(DSSAllConfigurationHandler.TYPENAME, simulationParams,
                            new PrintWriter(new StringWriter()), simulationId, username);
                } else if (simulator.equalsIgnoreCase(OchreAllConfigurationHandler.TYPENAME)) {
                    Properties simulationParams = generateSimulationParameters(simRequest, powerSystemConfig);
                    simulationParams.put(DSSAllConfigurationHandler.SIMULATIONID, simulationId);
                    simulationParams.put(DSSAllConfigurationHandler.DIRECTORY,
                            simulatorConfig.getSimulation_work_dir());

                    if (simulatorConfig.model_creation_config.separated_loads_file != null) {
                        numFederates = getSeparatedLoadNames(simulatorConfig.model_creation_config.separated_loads_file)
                                .size() + numFederates;
                        simulationParams.put(GLDAllConfigurationHandler.SEPARATED_LOADS_FILE,
                                simulatorConfig.model_creation_config.separated_loads_file);
                    } else {
                        logManager.error(ProcessStatus.ERROR, simulationId,
                                "No " + GLDAllConfigurationHandler.SEPARATED_LOADS_FILE + " parameter provided");
                        throw new Exception("Missing parameter " + GLDAllConfigurationHandler.SEPARATED_LOADS_FILE);
                    }

                    if (gldInterface != null) {
                        simulationParams.put(GridAppsDConstants.GRIDLABD_INTERFACE, gldInterface);
                    }
                    configurationManager.generateConfiguration(GLDAllConfigurationHandler.TYPENAME, simulationParams,
                            new PrintWriter(new StringWriter()), simulationId, username);
                    configurationManager.generateConfiguration(OchreAllConfigurationHandler.TYPENAME, simulationParams,
                            new PrintWriter(new StringWriter()), simulationId, username);
                } else { // otherwise use gridlabd
                    Properties simulationParams = generateSimulationParameters(simRequest, powerSystemConfig);
                    simulationParams.put(GLDAllConfigurationHandler.SIMULATIONID, simulationId);
                    simulationParams.put(GLDAllConfigurationHandler.DIRECTORY,
                            simulatorConfig.getSimulation_work_dir());
                    if (gldInterface != null) {
                        simulationParams.put(GridAppsDConstants.GRIDLABD_INTERFACE, gldInterface);
                    }
                    configurationManager.generateConfiguration(GLDAllConfigurationHandler.TYPENAME, simulationParams,
                            new PrintWriter(new StringWriter()), simulationId, username);
                }

                logManager.debug(ProcessStatus.RUNNING, simulationId,
                        "Simulation and power grid model files generated for simulation Id for model id "
                                + powerSystemConfig.Line_name);

            }

            // Start Apps and Services

            simulationContext.put("numFederates", numFederates);
            simContext.numFederates = numFederates;

            /*
             * if(simRequest.getSimulation_config().getSimulator().equals("GridLAB-D"))
             * simulationContext.put("simulationFile",tempDataPathDir.getAbsolutePath()+File
             * .separator+"model_startup.glm"); else
             * if(simRequest.getSimulation_config().getSimulator().equals("OCHRE"))
             * simulationContext.put("simulationFile",tempDataPathDir.getAbsolutePath()+File
             * .separator+"ochre_helics_config.json");
             */
            simulationContext.put("logLevel", logManager.getLogLevel());
            // TODO: Security removed in GOSS Java 21 upgrade - needs reimplementation
            // simulationContext.put("username", securityConfig.getManagerUser());
            // simulationContext.put("password", securityConfig.getManagerPassword());
            simulationContext.put("username", "system");
            simulationContext.put("password", "manager");
            /*
             * try{
             * simulationContext.put("simulatorPath",serviceManager.getService(simRequest.
             * getSimulation_config().getSimulator()).getExecution_path());
             * }catch(NullPointerException e){
             * if(serviceManager.getService(simRequest.getSimulation_config().getSimulator()
             * ) == null){ logManager.error(ProcessStatus.ERROR,
             * simulationId,"Cannot find service with id ="+simRequest.getSimulation_config(
             * ).getSimulator()); }else
             * if(serviceManager.getService(simRequest.getSimulation_config().getSimulator()
             * ).getExecution_path() == null){ logManager.error(ProcessStatus.ERROR,
             * simulationId,"Cannot find execution path for service ="+simRequest.
             * getSimulation_config().getSimulator()); } e.printStackTrace(); }
             */

            List<String> connectServiceInstanceIds = new ArrayList<String>();
            List<String> connectServiceIds = new ArrayList<String>();
            List<String> connectedAppInstanceIds = new ArrayList<String>();
            logManager.info(ProcessStatus.RUNNING, simulationId, "Service configs " + simRequest.service_configs);
            if (simRequest.service_configs == null) {
                logManager.warn(ProcessStatus.RUNNING, simulationId,
                        "No services found in simulation request  =" + simRequest.simulation_id);
            } else {
                for (ServiceConfig serviceConfig : simRequest.service_configs) {
                    logManager.info(ProcessStatus.RUNNING, simulationId, "Starting service" + serviceConfig.getId());

                    String serviceInstanceId = serviceManager.startServiceForSimultion(serviceConfig.getId(), null,
                            simulationContext);
                    if (serviceInstanceId != null) {
                        connectServiceInstanceIds.add(serviceInstanceId);
                        connectServiceIds.add(serviceConfig.getId());
                    }
                }
            }

            if (simRequest.application_config == null) {
                logManager.warn(ProcessStatus.RUNNING, simulationId,
                        "No applications found in simulation request  =" + simRequest.simulation_id);
            } else {
                for (ApplicationObject app : simRequest.application_config
                        .getApplications()) {
                    AppInfo appInfo = appManager.getApp(app.getName());
                    if (appInfo == null) {
                        logManager.error(ProcessStatus.ERROR, simulationId,
                                "Cannot start application " + app.getName() + ". Application not available");
                        throw new RuntimeException(
                                "Cannot start application " + app.getName() + ". Application not available");

                    }

                    List<String> prereqsList = appManager.getApp(app.getName())
                            .getPrereqs();
                    for (String prereqs : prereqsList) {

                        if (!connectServiceIds.contains(prereqs)) {
                            String serviceInstanceId = serviceManager.startServiceForSimultion(prereqs, null,
                                    simulationContext);
                            if (serviceInstanceId != null) {
                                connectServiceInstanceIds.add(serviceInstanceId);
                                logManager.info(ProcessStatus.RUNNING, simulationId,
                                        "Started " + prereqs + " with instance id " + serviceInstanceId);
                            }
                        }
                    }

                    String appInstanceId = appManager.startAppForSimultion(app
                            .getName(), app.getConfig_string(), simulationContext);
                    connectedAppInstanceIds.add(appInstanceId);
                    logManager.info(ProcessStatus.RUNNING, simulationId,
                            "Started " + app.getName() + " with instance id " + appInstanceId);

                }
            }

            simulationContext.put("connectedServiceInstanceIds", connectServiceInstanceIds);
            simulationContext.put("connectedAppInstanceIds", connectedAppInstanceIds);
            simContext.serviceInstanceIds = connectServiceInstanceIds;
            simContext.appInstanceIds = connectedAppInstanceIds;

            for (PowerSystemConfig powerSystemConfig : simRequest.power_system_configs) {
                ServiceInfo simulationServiceInfo = serviceManager
                        .getService(powerSystemConfig.simulator_config.simulator);
                List<String> serviceDependencies = simulationServiceInfo.getService_dependencies();
                for (String service : serviceDependencies) {
                    String serviceInstanceId = serviceManager.startServiceForSimultion(service, null,
                            simulationContext);
                    if (serviceInstanceId != null)
                        simContext.addServiceInstanceIds(serviceInstanceId);
                }
            }

            dataManager.processDataRequest(simContext, "timeseries", simulationId, null, username);

            // start test if requested
            testManager.handleTestRequest(simRequest.getTest_config(), simContext);

            // start simulation
            for (PowerSystemConfig powerSystemConfig : simRequest.power_system_configs) {
                logManager.debug(ProcessStatus.RUNNING, simulationId, "Starting simulation for simulation id "
                        + simulationId + " and model id " + powerSystemConfig.Line_name);
                simulationManager.startSimulation(simulationId, simRequest.getSimulation_config(), simContext,
                        simulationContext, powerSystemConfig);
                logManager.info(ProcessStatus.RUNNING, simulationId,
                        "Started simulation for id " + simulationId + " and model id " + powerSystemConfig.Line_name);
            }

        } catch (Exception e) {
            e.printStackTrace();
            try {
                logManager.error(ProcessStatus.ERROR, simulationId,
                        "Failed to start simulation correctly: " + e.getMessage());

            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    Properties generateSimulationParameters(RequestSimulation requestSimulation, PowerSystemConfig powerSystemConfig) {
        Properties params = new Properties();

        // TODO where to get feeder id?
        params.put(GLDAllConfigurationHandler.MODELID, powerSystemConfig.Line_name);

        SimulatorConfig simConfig = powerSystemConfig.simulator_config;
        ModelCreationConfig modelConfig = simConfig.model_creation_config;
        if (modelConfig == null) {
            modelConfig = new ModelCreationConfig();
            simConfig.model_creation_config = modelConfig;
        }

        double zFraction = modelConfig.z_fraction;
        double iFraction = modelConfig.i_fraction;
        double pFraction = modelConfig.p_fraction;

        params.put(GLDAllConfigurationHandler.ZFRACTION, Double.toString(zFraction));
        params.put(GLDAllConfigurationHandler.IFRACTION, Double.toString(iFraction));
        params.put(GLDAllConfigurationHandler.PFRACTION, Double.toString(pFraction));
        params.put(GLDAllConfigurationHandler.LOADSCALINGFACTOR, Double.toString(modelConfig.load_scaling_factor));
        params.put(GLDAllConfigurationHandler.RANDOMIZEFRACTIONS, modelConfig.randomize_zipload_fractions);
        params.put(GLDAllConfigurationHandler.USEHOUSES, modelConfig.use_houses);

        if (modelConfig.schedule_name != null) {
            params.put(GLDAllConfigurationHandler.SCHEDULENAME, modelConfig.schedule_name);
        } else {
            params.put(GLDAllConfigurationHandler.SCHEDULENAME, "");
        }
        params.put(GLDAllConfigurationHandler.SIMULATIONNAME, requestSimulation.getSimulation_config().simulation_name);

        // Default power_flow_solver_method if not specified
        String solverMethod = simConfig.power_flow_solver_method;
        if (solverMethod == null || solverMethod.trim().isEmpty()) {
            solverMethod = "NR";
        }
        params.put(GLDAllConfigurationHandler.SOLVERMETHOD, solverMethod);

        params.put(GLDAllConfigurationHandler.SIMULATIONBROKERHOST,
                requestSimulation.getSimulation_config().getSimulation_broker_location());
        params.put(GLDAllConfigurationHandler.SIMULATIONBROKERPORT,
                Integer.toString(requestSimulation.getSimulation_config().getSimulation_broker_port()));

        params.put(GLDAllConfigurationHandler.SIMULATIONSTARTTIME, requestSimulation.getSimulation_config().start_time);
        params.put(GLDAllConfigurationHandler.SIMULATIONDURATION,
                Integer.toString(requestSimulation.getSimulation_config().duration));

        if (modelConfig.getModel_state() != null) {
            Gson gson = new Gson();
            params.put(GLDAllConfigurationHandler.MODEL_STATE, gson.toJson(modelConfig.getModel_state()));
        }

        params.put(GLDAllConfigurationHandler.SIMULATOR, simConfig.simulator);
        params.put(GLDAllConfigurationHandler.RUN_REALTIME, requestSimulation.getSimulation_config().run_realtime);

        if (modelConfig.separated_loads_file != null) {
            params.put(GLDAllConfigurationHandler.SEPARATED_LOADS_FILE, modelConfig.separated_loads_file);
        } else {
            params.put(GLDAllConfigurationHandler.SEPARATED_LOADS_FILE, "");
        }
        return params;
    }

    private List<String> getSeparatedLoadNames(String fileName) {

        List<String> loadNames = new ArrayList<String>();
        boolean isHeader = true;

        try {
            FileInputStream fis = new FileInputStream(fileName);
            Workbook workbook = null;
            if (fileName.toLowerCase().endsWith("xlsx")) {
                workbook = new XSSFWorkbook(fis);
            } else if (fileName.toLowerCase().endsWith("xls")) {
                workbook = new HSSFWorkbook(fis);
            }

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            while (rowIterator.hasNext()) {

                Row row = rowIterator.next();
                if (!isHeader) {
                    loadNames.add(row.getCell(5).getStringCellValue());
                    System.out.println(row.getCell(5).getStringCellValue());
                }
                isHeader = false;
            }
            fis.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return loadNames;
    }

    /**
     * Create configfile.json string, should look something like
     * "{\"swt_g9343_48332_sw\": [\"status\"],\"swt_l5397_48332_sw\":
     * [\"status\"],\"swt_a8869_48332_sw\": [\"status\"]}";
     *
     * @param simulationOutput
     * @return
     */
    protected void generateConfigFile(File configFile, SimulationOutput simulationOutput) {
        StringBuffer configStr = new StringBuffer();
        boolean isFirst = true;
        configStr.append("{");
        for (SimulationOutputObject obj : simulationOutput.getOutputObjects()) {
            if (!isFirst) {
                configStr.append(",");
            }
            isFirst = false;

            configStr.append("\"" + obj.getName() + "\": [");
            boolean isFirstProp = true;
            for (String property : obj.getProperties()) {
                if (!isFirstProp) {
                    configStr.append(",");
                }
                isFirstProp = false;
                configStr.append("\"" + property + "\"");
            }
            configStr.append("]");
        }

        configStr.append("}");

        FileWriter fOut;
        try {
            fOut = new FileWriter(configFile);
            fOut.write(configStr.toString());

            fOut.flush();
            fOut.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
