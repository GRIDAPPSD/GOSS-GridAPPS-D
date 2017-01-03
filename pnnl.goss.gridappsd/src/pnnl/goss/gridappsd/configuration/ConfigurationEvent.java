package pnnl.goss.gridappsd.configuration;

import java.io.Serializable;
import java.nio.file.Path;

import pnnl.goss.core.GossResponseEvent;

public class ConfigurationEvent implements GossResponseEvent {
	
	@Override
	public void onMessage(Serializable response) {
		//TODO: Call createSimConfigFiles() and createPowerGridModelFiles()
		//after parsing message;
		
	}
	
	/**
	 * This method creates initial simulation configuration files based on 
	 * configuration message.
	 * @return location of configuration files
	 */
	public static Path createSimConfigFiles(String configMessage){
		//TODO: implement this.
		return null;
	}
	
	/**
	 * This method creates power grid model files based on 
	 * configuration message.
	 * @return location of power grid model files
	 */
	public static Path createPowerGridModelFiles(String configMessage){
		//TODO: implement this.
		return null;
	}
	

}
