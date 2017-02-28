package pnnl.goss.gridappsd.api;

import java.io.File;
import java.io.Serializable;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;

import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.core.server.ServerControl;
import pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.gridappsd.utils.RunCommandLine;

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
	void startSimulation(int simulationId, File simulationFile);

}
