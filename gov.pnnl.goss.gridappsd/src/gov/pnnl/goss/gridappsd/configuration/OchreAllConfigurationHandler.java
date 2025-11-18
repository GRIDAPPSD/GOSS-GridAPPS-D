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
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Properties;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.pnnl.goss.gridappsd.api.ConfigurationHandler;
import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.PowergridModelDataManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.data.handlers.BlazegraphQueryHandler;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.SimulationContext;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import gov.pnnl.goss.gridappsd.utils.RunCommandLine;
import gov.pnnl.gridappsd.cimhub.CIMImporter;
import gov.pnnl.gridappsd.cimhub.queryhandler.QueryHandler;
import pnnl.goss.core.Client;

@Component(service = ConfigurationHandler.class)
public class OchreAllConfigurationHandler extends BaseConfigurationHandler implements ConfigurationHandler {// implements
                                                                                                            // ConfigurationManager{

    private static Logger log = LoggerFactory.getLogger(OchreAllConfigurationHandler.class);
    Client client = null;

    @Reference
    private volatile ConfigurationManager configManager;
    @Reference
    private volatile SimulationManager simulationManager;
    @Reference
    private volatile PowergridModelDataManager powergridModelManager;
    @Reference
    private volatile LogManager logManager;

    public static final String TYPENAME = "OCHRE";
    public static final String DIRECTORY = "directory";
    public static final String MODELID = "model_id";
    public static final String SIMULATIONID = "simulation_id";
    public static final String CONFIGTARGET = "ochre"; // will build files for both glm and ochre
    public static final String CONFIG_FILENAME = "ochre_helics_config.json";
    public static final String SIMULATIONBROKERHOST = "simulation_broker_host";
    public static final String SIMULATIONBROKERPORT = "simulation_broker_port";
    public static final String MODEL_ID = "model_id";

    public OchreAllConfigurationHandler() {
    }

    public OchreAllConfigurationHandler(LogManager logManager, DataManager dataManager) {

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
                "Generating OCHRE HELICS config file using parameters: " + parameters);

        String directory = GridAppsDConstants.getStringProperty(parameters, DIRECTORY, null);
        if (directory == null || directory.trim().length() == 0) {
            logManager.error(ProcessStatus.ERROR, processId, "No " + DIRECTORY + " parameter provided");
            throw new Exception("Missing parameter " + DIRECTORY);
        }
        File dir = new File(directory);
        if (!dir.exists())
            dir.mkdirs();
        String tempDataPath = dir.getAbsolutePath();

        String simulationBrokerHost = GridAppsDConstants.getStringProperty(parameters, SIMULATIONBROKERHOST, null);
        if (simulationBrokerHost == null || simulationBrokerHost.trim().length() == 0) {
            logManager.error(ProcessStatus.ERROR, processId, "No " + SIMULATIONBROKERHOST + " parameter provided");
            throw new Exception("Missing parameter " + SIMULATIONBROKERHOST);
        }

        String simulationBrokerPort = GridAppsDConstants.getStringProperty(parameters, SIMULATIONBROKERPORT, null);
        if (simulationBrokerPort == null || simulationBrokerPort.trim().length() == 0) {
            logManager.error(ProcessStatus.ERROR, processId, "No " + SIMULATIONBROKERPORT + " parameter provided");
            throw new Exception("Missing parameter " + SIMULATIONBROKERPORT);
        }

        String model_id = GridAppsDConstants.getStringProperty(parameters, MODEL_ID, null);
        if (model_id == null || model_id.trim().length() == 0) {
            logManager.error(ProcessStatus.ERROR, processId, "No " + MODEL_ID + " parameter provided");
            throw new Exception("Missing parameter " + MODEL_ID);
        }

        String separated_loads_file = GridAppsDConstants.getStringProperty(parameters,
                GLDAllConfigurationHandler.SEPARATED_LOADS_FILE, null);
        if (separated_loads_file == null || separated_loads_file.trim().length() == 0) {
            logManager.error(ProcessStatus.ERROR, processId,
                    "No " + GLDAllConfigurationHandler.SEPARATED_LOADS_FILE + " parameter provided");
            throw new Exception("Missing parameter " + GLDAllConfigurationHandler.SEPARATED_LOADS_FILE);
        }

        try {
            File tmpDir = new File(tempDataPath);
            RunCommandLine.runCommand("cp -r /gridappsd/services/gridappsd-ochre/inputs/ " + tempDataPath);
            RunCommandLine.runCommand("cp -r /gridappsd/services/gridappsd-ochre/agents/ " + tempDataPath);

            simulationBrokerHost = "localhost";

            RunCommandLine.runCommand("python /gridappsd/services/gridappsd-ochre/bin/make_config_file.py " +
                    simulationBrokerHost + " " +
                    tempDataPath + " " +
                    CONFIG_FILENAME + " " +
                    simulationBrokerPort + " " +
                    processId + " " +
                    model_id + " " +
                    separated_loads_file);
            logManager.info(ProcessStatus.RUNNING, processId,
                    "python /gridappsd/services/gridappsd-ochre/bin/make_config_file.py " +
                            simulationBrokerHost + " " +
                            tempDataPath + " " +
                            CONFIG_FILENAME + " " +
                            simulationBrokerPort + " " +
                            processId + " " +
                            model_id + " " +
                            separated_loads_file);

        } catch (Exception e) {
            log.warn("Could not create OCHRE HELICS config file");
        }

        logManager.info(ProcessStatus.RUNNING, processId, "Finished generating OCHRE HELICS config file.");

    }

}
