package gov.pnnl.goss.gridappsd.process;

import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.api.StatusReporter;
import gov.pnnl.goss.gridappsd.dto.RequestSimulation;

import java.io.File;
import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pnnl.goss.core.DataResponse;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;

import com.google.gson.Gson;

public class ProcessNewSimulationRequest {

	private static Logger log = LoggerFactory
			.getLogger(ProcessManagerImpl.class);

	public void process(ConfigurationManager configurationManager,
			SimulationManager simulationManager, StatusReporter statusReporter,
			int simulationId, DataResponse event, Serializable message) {

		try {
			// TODO: validate simulation request json and create
			// PowerSystemConfig and SimulationConfig dto objects to work with
			// internally.

			RequestSimulation config = RequestSimulation.parse(message.toString());
			log.info("Parsed config " + config);
			if (config == null || config.getPower_system_config() == null
					|| config.getSimulation_config() == null) {
				throw new RuntimeException("Invalid configuration received");
			}

			// make request to configuration Manager to get power grid model
			// file locations and names
			log.debug("Creating simulation and power grid model files for simulation Id "
					+ simulationId);
			File simulationFile = configurationManager.getSimulationFile(
					simulationId, config);
			if (simulationFile == null) {
				throw new Exception("No simulation file returned for request "
						+ config);
			}

			log.debug("Simulation and power grid model files generated for simulation Id "
					+ simulationId);

			// start simulation
			log.debug("Starting simulation for id " + simulationId);
			simulationManager.startSimulation(simulationId, simulationFile,
					config.getSimulation_config());
			log.debug("Starting simulation for id " + simulationId);

			// new ProcessSimulationRequest().process(event, client,
			// configurationManager, simulationManager); break;
		} catch (Exception e) {
			e.printStackTrace();
			try {
				statusReporter.reportStatus(
						GridAppsDConstants.topic_simulationStatus
								+ simulationId,
						"Process Initialization error: " + e.getMessage());
				log.error("Process Initialization error", e);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}

}
