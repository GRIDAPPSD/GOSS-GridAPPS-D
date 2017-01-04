package pnnl.goss.gridappsd.configuration;

import java.io.Serializable;
import java.nio.file.Path;

import pnnl.goss.core.GossResponseEvent;

public class ConfigurationEvent implements GossResponseEvent {
	
	@Override
	public void onMessage(Serializable response) {
		/*
		 * Received config message from ProcessManager
		 * Call getResponse() to DataManger and get data file locations and simulation file locations.
		 * response back to Process manager with file locations.
		 */
		
	}
	

	

}
