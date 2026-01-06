/*******************************************************************************
 * Copyright  2017, Battelle Memorial Institute All rights reserved.
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
package gov.pnnl.goss.gridappsd.configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

import gov.pnnl.gridappsd.cimhub.CIMImporter;
import gov.pnnl.gridappsd.cimhub.dto.ModelState;
import gov.pnnl.gridappsd.cimhub.queryhandler.QueryHandler;
import gov.pnnl.goss.gridappsd.api.ConfigurationHandler;
import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.PowergridModelDataManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.data.handlers.BlazegraphQueryHandler;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.SimulationContext;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.Client;

@Component(service = ConfigurationHandler.class)
public class GLDSimulationOutputConfigurationHandler extends BaseConfigurationHandler implements ConfigurationHandler {// implements
                                                                                                                       // ConfigurationManager{

    private static Logger log = LoggerFactory.getLogger(GLDSimulationOutputConfigurationHandler.class);
    Client client = null;

    @Reference
    private volatile ConfigurationManager configManager;
    @Reference
    private volatile SimulationManager simulationManager;
    @Reference
    private volatile PowergridModelDataManager powergridModelManager;
    @Reference
    volatile LogManager logManager;

    // Setter methods for manual dependency injection (workaround for SCR not
    // loading components)
    public void setConfigManager(ConfigurationManager configManager) {
        this.configManager = configManager;
    }

    public void setSimulationManager(SimulationManager simulationManager) {
        this.simulationManager = simulationManager;
    }

    public void setPowergridModelManager(PowergridModelDataManager powergridModelManager) {
        this.powergridModelManager = powergridModelManager;
    }

    public void setLogManager(LogManager logManager) {
        this.logManager = logManager;
    }

    public static final String TYPENAME = "GridLAB-D Simulation Output";
    public static final String MODELID = "model_id";
    public static final String DICTIONARY_FILE = "dictionary_file";
    public static final String SIMULATIONID = "simulation_id";
    public static final String USEHOUSES = "use_houses";
    public static final String SIMULATIONBROKERHOST = "simulation_broker_host";
    public static final String SIMULATIONBROKERPORT = "simulation_broker_port";

    public static final String HELICS_PREFIX = "{\n"
            + "\t\"name\": \"MODEL_ID\",\n"
            + "\t\"log_level\": \"DATA\",\n"
            + "\t\"period\": 1.0,\n"
            + "\t\"broker\": \"BROKER_LOCATION:BROKER_PORT\",\n"
            + "\t\"endpoints\": [\n"
            + "\t\t{\n"
            + "\t\t\t\"name\": \"helics_input\",\n"
            + "\t\t\t\"global\": false,\n"
            + "\t\t\t\"type\": \"string\",\n"
            + "\t\t\t\"info\": \"This is the endpoint which recieves CIM commands from the HELICS GOSS bridge.\"\n"
            + "\t\t},\n"
            + "\t\t{\n"
            + "\t\t\t\"name\": \"helics_output\",\n"
            + "\t\t\t\"global\": false,\n"
            + "\t\t\t\"type\": \"string\",\n"
            + "\t\t\t\"destination\": \"HELICS_GOSS_Bridge_PROCESS_ID/helics_output\",\n"
            + "\t\t\t\"info\": ";
    public static final String HELICS_SUFFIX = "\t\t}\n\t]\n}";

    public GLDSimulationOutputConfigurationHandler() {
    }

    public GLDSimulationOutputConfigurationHandler(ConfigurationManager configManager,
            PowergridModelDataManager powergridModelManager, LogManager logManager) {
        this.configManager = configManager;
        this.powergridModelManager = powergridModelManager;
        this.logManager = logManager;
    }

    @Activate
    public void start() {
        if (configManager != null) {
            configManager.registerConfigurationHandler(TYPENAME, this);
        } else {
            // TODO send log message and exception
            log.warn("No Config manager avilable for " + getClass());
        }

        if (powergridModelManager == null) {
            // TODO send log message and exception
        }
    }

    @Override
    public void generateConfig(Properties parameters, PrintWriter out, String processId, String username)
            throws Exception {
        logManager.info(ProcessStatus.RUNNING, processId,
                "Generating simulation output configuration file using parameters: " + parameters);
        File dictFile = null;
        String simulationId = GridAppsDConstants.getStringProperty(parameters, SIMULATIONID, null);
        boolean useHouses = GridAppsDConstants.getBooleanProperty(parameters, USEHOUSES, false);
        File configFile = null;
        if (simulationId != null) {
            SimulationContext simulationContext = simulationManager.getSimulationContextForId(simulationId);
            if (simulationContext != null) {
                configFile = new File(simulationContext.getSimulationDir() + File.separator
                        + GLDAllConfigurationHandler.MEASUREMENTOUTPUTS_FILENAME);
                dictFile = new File(simulationContext.getSimulationDir() + File.separator
                        + GLDAllConfigurationHandler.DICTIONARY_FILENAME);
                // If the config file already has been created for this simulation then return
                // it
                if (configFile.exists()) {
                    printFileToOutput(configFile, out);
                    logManager.info(ProcessStatus.RUNNING, processId,
                            "Dictionary GridLAB-D simulation outputs file for simulation " + simulationId
                                    + " already exists.");
                    return;
                }
            } else {
                logManager.warn(ProcessStatus.RUNNING, processId,
                        "No simulation context found for simulation_id: " + simulationId);
            }
        }

        String gldInterface = GridAppsDConstants.getStringProperty(parameters, GridAppsDConstants.GRIDLABD_INTERFACE,
                GridAppsDConstants.GRIDLABD_INTERFACE_FNCS);

        StringWriter parameters_writer = new StringWriter();
        parameters.list(new PrintWriter(parameters_writer));
        String parameters_list = parameters_writer.getBuffer().toString();
        String modelId = GridAppsDConstants.getStringProperty(parameters, MODELID, null);
        if (modelId == null || modelId.trim().length() == 0) {
            logManager.error(ProcessStatus.RUNNING, processId,
                    "No " + MODELID + " parameter provided.\nSimulationParameters: " + parameters_list);
            throw new Exception("Missing parameter " + MODELID + "\nSimulation Parameters: " + parameters_list);
        }

        ModelState modelState = new ModelState();
        String modelStateStr = GridAppsDConstants.getStringProperty(parameters, MODELSTATE, null);
        if (modelStateStr == null || modelStateStr.trim().length() == 0) {
            logManager.info(ProcessStatus.RUNNING, processId,
                    "No " + MODELSTATE + " parameter provided.\nSimulationParameters: " + parameters_list);
        } else {
            Gson gson = new Gson();
            modelState = gson.fromJson(modelStateStr, ModelState.class);
        }
        // If passed in, use location of dictionary file, otherwise it will attempt to
        // generate it
        String dictFilePath = GridAppsDConstants.getStringProperty(parameters, DICTIONARY_FILE, null);
        if (dictFilePath != null) {
            dictFile = new File(dictFilePath);
        }

        String bgHost = configManager.getConfigurationProperty(GridAppsDConstants.BLAZEGRAPH_HOST_PATH);
        if (bgHost == null || bgHost.trim().length() == 0) {
            bgHost = BlazegraphQueryHandler.DEFAULT_ENDPOINT;
        }

        Reader measurementFileReader;

        if (dictFile != null && dictFile.getName().length() > 0 && dictFile.exists()) {
            measurementFileReader = new FileReader(dictFile);
        } else {
            // TODO write a query handler that uses the built in powergrid model data
            // manager that talks to blazegraph internally
            QueryHandler queryHandler = new BlazegraphQueryHandler(bgHost, logManager, processId, username);
            queryHandler.addFeederSelection(modelId);
            CIMImporter cimImporter = new CIMImporter();
            StringWriter dictionaryStringOutput = new StringWriter();
            PrintWriter dictionaryOutput = new PrintWriter(dictionaryStringOutput);

            cimImporter.generateDictionaryFile(queryHandler, dictionaryOutput, useHouses, modelState);
            String dictOut = dictionaryStringOutput.toString();
            measurementFileReader = null;
            if (dictFile != null && dictFile.getName().length() > 0 && !dictFile.exists()) {
                FileWriter fw = new FileWriter(dictFile);
                fw.write(dictOut);
                fw.close();
            }
            measurementFileReader = new StringReader(dictOut);
        }

        String result = CreateGldPubs(measurementFileReader, processId, username);

        // if it is for helics wrap it in the helix endpoint definition
        if (GridAppsDConstants.GRIDLABD_INTERFACE_HELICS.equals(gldInterface)) {
            // Escape the json and embed it in the helics config file
            String simulationBrokerHost = GridAppsDConstants.getStringProperty(parameters, SIMULATIONBROKERHOST, null);
            if (simulationBrokerHost == null || simulationBrokerHost.trim().length() == 0) {
                logManager.error(ProcessStatus.ERROR, processId, "No " + SIMULATIONBROKERHOST
                        + " parameter provided.\nSimulationParameters: " + parameters_list);
                throw new Exception(
                        "Missing parameter " + SIMULATIONBROKERHOST + "\nSimulation Parameters: " + parameters_list);
            }
            String simulationBrokerPort = GridAppsDConstants.getStringProperty(parameters, SIMULATIONBROKERPORT, null);
            if (simulationBrokerPort == null || simulationBrokerPort.trim().length() == 0) {
                logManager.error(ProcessStatus.ERROR, processId, "No " + SIMULATIONBROKERPORT
                        + " parameter provided.\nSimulationParameters: " + parameters_list);
                throw new Exception(
                        "Missing parameter " + SIMULATIONBROKERPORT + "\nSimulation Parameters: " + parameters_list);
            }
            String brokerLocation = simulationBrokerHost;
            String brokerPort = String.valueOf(simulationBrokerPort);
            String HELICS_PREFIX1 = HELICS_PREFIX.replaceAll("BROKER_LOCATION", brokerLocation);
            String HELICS_PREFIX2 = HELICS_PREFIX1.replaceAll("BROKER_PORT", brokerPort);
            result = HELICS_PREFIX2.replaceAll("PROCESS_ID", processId).replaceAll("MODEL_ID", modelId)
                    + result.replaceAll("    ", "\t\t\t\t\t").replaceAll("  ", "\t\t\t\t").replaceAll("}", "\t\t\t}\n")
                    + HELICS_SUFFIX;
        }

        if (configFile != null) {
            FileWriter fw = new FileWriter(configFile);
            fw.write(result);
            fw.close();
        }
        // return result;
        out.write(result);
        out.flush();

        logManager.info(ProcessStatus.RUNNING, processId, "Finished generating simulation output configuration file.");

    }

    String CreateGldPubs(Reader measurementFileReader, String processId, String username) throws FileNotFoundException {
        String jsonObjStr = "";
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject gldConfigObj = new JsonObject();
        // JsonObject gldPublications = new JsonObject();

        try {
            JsonObject jsonObj = gson.fromJson(measurementFileReader, JsonObject.class);
            JsonArray feeders = (JsonArray) jsonObj.get("feeders");
            Iterator<JsonElement> iter = feeders.iterator();
            while (iter.hasNext()) {
                JsonObject feederInfo = (JsonObject) iter.next();
                JsonArray feederMeasurements = (JsonArray) feederInfo.get("measurements");
                Iterator<JsonElement> feederMeasurementsIter = feederMeasurements.iterator();
                Map<String, JsonArray> measurements = new HashMap<String, JsonArray>();
                while (feederMeasurementsIter.hasNext()) {
                    JsonObject feederMeasurement = (JsonObject) feederMeasurementsIter.next();
                    parseMeasurement(measurements, feederMeasurement);
                }
                for (Map.Entry<String, JsonArray> entry : measurements.entrySet()) {
                    gldConfigObj.add(entry.getKey(), entry.getValue());
                }
                JsonArray globalsArr = new JsonArray();
                globalsArr.add(new JsonPrimitive("clock"));
                gldConfigObj.add("globals", globalsArr);
                measurements.clear();
            }

            // gldConfigObj.add("publications", gldPublications);
            jsonObjStr = gson.toJson(gldConfigObj);

        } catch (JsonIOException e) {
            logManager.error(ProcessStatus.RUNNING, processId,
                    "Error while generating simulation output: " + e.getMessage());
            throw e;
        } catch (JsonParseException e) {
            logManager.error(ProcessStatus.RUNNING, processId,
                    "Error while generating simulation output: " + e.getMessage());
            throw e;
        }
        return jsonObjStr;
    }

    void parseMeasurement(Map<String, JsonArray> measurements, JsonObject measurement) throws JsonParseException {
        String objectName;
        String propertyName;
        String measurementType;
        String phases;
        String conductingEquipmentType;
        String conductingEquipmentName;
        String connectivityNode;
        if (!measurement.has("measurementType") || !measurement.has("phases")
                || !measurement.has("ConductingEquipment_type") || !measurement.has("ConductingEquipment_name")
                || !measurement.has("ConnectivityNode")) {
            throw new JsonParseException(
                    "CimMeasurementsToGldPubs::parseMeasurement: The JsonObject measurements must have the following keys: measurementType, phases, ConductingEquipment_type,ConductingEquipment_name, and ConnectivityNode.");
        }
        measurementType = measurement.get("measurementType").getAsString();
        phases = measurement.get("phases").getAsString();
        if (phases.equals("s1")) {
            phases = "1";
        } else if (phases.equals("s2")) {
            phases = "2";
        }
        conductingEquipmentType = measurement.get("name").getAsString();
        conductingEquipmentName = measurement.get("SimObject").getAsString();
        connectivityNode = measurement.get("ConnectivityNode").getAsString();
        if (conductingEquipmentType.contains("LinearShuntCompensator")) {
            if (measurementType.equals("VA")) {
                objectName = conductingEquipmentName;
                propertyName = "shunt_" + phases;
            } else if (measurementType.equals("Pos")) {
                objectName = conductingEquipmentName;
                propertyName = "switch" + phases;
            } else if (measurementType.equals("PNV")) {
                objectName = conductingEquipmentName;
                propertyName = "voltage_" + phases;
            } else {
                throw new JsonParseException(String.format(
                        "CimMeasurementsToGldPubs::parseMeasurement: The value of measurementType is not a valid type.\nValid types for LinearShuntCompensators are VA, Pos, and PNV.\nmeasurementType = %s.",
                        measurementType));
            }
        } else if (conductingEquipmentType.contains("PowerTransformer")
                || conductingEquipmentType.contains("TransformerTank")) {
            if (measurementType.equals("VA")) {
                objectName = conductingEquipmentName;
                propertyName = "power_in_" + phases;
            } else if (measurementType.equals("PNV")) {
                objectName = connectivityNode;
                propertyName = "voltage_" + phases;
            } else if (measurementType.equals("A")) {
                objectName = conductingEquipmentName;
                propertyName = "current_in_" + phases;
            } else {
                throw new JsonParseException(String.format(
                        "CimMeasurementsToGldPubs::parseMeasurement: The value of measurementType is not a valid type.\nValid types for PowerTransformers and TransformerTanks are VA, PNV, and A.\nmeasurementType = %s.",
                        measurementType));
            }
        } else if (conductingEquipmentType.contains("RatioTapChanger")) {
            if (measurementType.equals("VA")) {
                objectName = conductingEquipmentName;
                propertyName = "power_in_" + phases;
            } else if (measurementType.equals("PNV")) {
                objectName = connectivityNode;
                propertyName = "voltage_" + phases;
            } else if (measurementType.equals("A")) {
                objectName = conductingEquipmentName;
                propertyName = "current_in_" + phases;
            } else if (measurementType.equals("Pos")) {
                objectName = conductingEquipmentName;
                propertyName = "tap_" + phases;
            } else {
                throw new JsonParseException(String.format(
                        "CimMeasurementsToGldPubs::parseMeasurement: The value of measurementType is not a valid type.\nValid types for RatioTapChanger are VA, PNV, A, and Pos.\nmeasurementType = %s.",
                        measurementType));
            }
        } else if (conductingEquipmentType.contains("ACLineSegment")) {
            if (measurementType.equals("VA")) {
                objectName = conductingEquipmentName;
                if (phases.equals("1")) {
                    propertyName = "power_in_" + "A";
                } else if (phases.equals("2")) {
                    propertyName = "power_in_" + "B";
                } else {
                    propertyName = "power_in_" + phases;
                }
            } else if (measurementType.equals("PNV")) {
                objectName = connectivityNode;
                propertyName = "voltage_" + phases;
            } else if (measurementType.equals("A")) {
                objectName = conductingEquipmentName;
                propertyName = "current_in_" + phases;
            } else {
                throw new JsonParseException(String.format(
                        "CimMeasurementsToGldPubs::parseMeasurement: The value of measurementType is not a valid type.\nValid types for ACLineSegments are VA, A, and PNV.\nmeasurementType = %s.",
                        measurementType));
            }
        } else if (conductingEquipmentType.contains("LoadBreakSwitch") || conductingEquipmentType.contains("Recloser")
                || conductingEquipmentType.contains("Breaker")) {
            if (measurementType.equals("VA")) {
                objectName = conductingEquipmentName;
                propertyName = "power_in_" + phases;
            } else if (measurementType.equals("PNV")) {
                objectName = connectivityNode;
                propertyName = "voltage_" + phases;
            } else if (measurementType.equals("Pos")) {
                objectName = conductingEquipmentName;
                propertyName = "status";
            } else if (measurementType.equals("A")) {
                objectName = conductingEquipmentName;
                if (phases.equals("1")) {
                    propertyName = "current_in_A";
                } else if (phases.equals("2")) {
                    propertyName = "current_in_B";
                } else {
                    propertyName = "current_in_" + phases;
                }
            } else {
                throw new JsonParseException(String.format(
                        "CimMeasurementsToGldPubs::parseMeasurement: The value of measurementType is not a valid type.\nValid types for LoadBreakSwitch are VA, A, and PNV.\nmeasurementType = %s.",
                        measurementType));
            }
        } else if (conductingEquipmentType.contains("EnergyConsumer")) {
            if (measurementType.equals("VA")) {
                objectName = conductingEquipmentName;
                if (phases.equals("1") || phases.equals("2")) {
                    propertyName = "indiv_measured_power_" + phases;
                } else {
                    propertyName = "measured_power_" + phases;
                }
            } else if (measurementType.equals("PNV")) {
                objectName = connectivityNode;
                propertyName = "voltage_" + phases;
            } else if (measurementType.equals("A")) {
                objectName = connectivityNode;
                propertyName = "measured_current_" + phases;
            } else {
                throw new JsonParseException(String.format(
                        "CimMeasurementsToGldPubs::parseMeasurement: The value of measurementType is not a valid type.\nValid types for EnergyConsumer are VA, A, and PNV.\nmeasurementType = %s.",
                        measurementType));
            }
        } else if (conductingEquipmentType.contains("PowerElectronicsConnection")) {
            if (measurementType.equals("VA")) {
                objectName = conductingEquipmentName;
                if (phases.equals("1") || phases.equals("2")) {
                    propertyName = "indiv_measured_power_" + phases;
                } else {
                    propertyName = "measured_power_" + phases;
                }
            } else if (measurementType.equals("PNV")) {
                objectName = conductingEquipmentName;
                propertyName = "voltage_" + phases;
            } else if (measurementType.equals("A")) {
                objectName = conductingEquipmentName;
                propertyName = "measured_current_" + phases;
            } else if (measurementType.equals("SoC")) {
                objectName = conductingEquipmentName;
                propertyName = "state_of_charge";
            } else {
                throw new JsonParseException(String.format(
                        "CimMeasurementsToGldPubs::parseMeasurement: The value of measurementType is not a valid type.\nValid types for PowerElectronicsConnection are VA, A, PNV, and SoC.\nmeasurementType = %s.",
                        measurementType));
            }
        } else if (conductingEquipmentType.contains("SynchronousMachine")) {
            if (measurementType.equals("VA")) {
                objectName = conductingEquipmentName;
                propertyName = "measured_power_" + phases;
            } else if (measurementType.equals("PNV")) {
                objectName = connectivityNode;
                propertyName = "voltage_" + phases;
            } else if (measurementType.equals("A")) {
                objectName = conductingEquipmentName;
                propertyName = "measured_current_" + phases;
            } else {
                throw new JsonParseException(String.format(
                        "CimMeasurementsToGldPubs::parseMeasurement: The value of measurementType is not a valid type.\nValid types for SynchronousMachine are VA, A, and PNV.\nmeasurementType = %s.",
                        measurementType));
            }
        } else {
            throw new JsonParseException(String.format(
                    "CimMeasurementsToGldPubs::parseMeasurement: The value of ConductingEquipment_type is not a recognized object type.\nValid types are ACLineSegment, LinearShuntCompesator, RatioTapChanger, LoadBreakSwitch, EnergyConsumer, PowerElectronicsConnection, TransformerTank, and PowerTransformer.\nConductingEquipment_type = %s.",
                    conductingEquipmentType));

        }
        if (measurements.containsKey(objectName)) {
            JsonPrimitive p = new JsonPrimitive(propertyName);
            if (!measurements.get(objectName).contains(p)) {
                measurements.get(objectName).add(new JsonPrimitive(propertyName));
            }
        } else {
            JsonArray newMeasurements = new JsonArray();
            newMeasurements.add(new JsonPrimitive(propertyName));
            measurements.put(objectName, newMeasurements);
        }
    }
}
