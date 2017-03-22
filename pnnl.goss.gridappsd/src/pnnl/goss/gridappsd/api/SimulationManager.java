package pnnl.goss.gridappsd.api;

import java.io.File;
import pnnl.goss.gridappsd.dto.SimulationConfig;

/**
 * This represents Internal Function 405 Simulation Control Manager.
 * This is the management function that controls the running/execution of the Distribution Simulator (401).
 * @author shar064 
 */

public interface SimulationManager {
	
	/**
	 * This method is called by Process Manager to start a simulation
	 * @param simulationId
	 * @param simulationFile
	 */
	void startSimulation(int simulationId, File simulationFile, SimulationConfig simulationConfig);

}
