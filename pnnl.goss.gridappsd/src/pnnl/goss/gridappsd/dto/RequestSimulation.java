package pnnl.goss.gridappsd.dto;

import java.io.Serializable;

public class RequestSimulation implements Serializable {
	
	private static final long serialVersionUID = 1L;

	private PowerSystemConfig power_system_config;

	private SimulationConfig simulation_config;
	
	private ApplicationConfig application_config;
	
	public RequestSimulation(){
		
	}
	
	public RequestSimulation(PowerSystemConfig power_system_config, SimulationConfig simulation_config){
		this.power_system_config = power_system_config;
		this.simulation_config = simulation_config;
	}

	public PowerSystemConfig getPower_system_config() {
		return power_system_config;
	}

	public void setPower_system_config(PowerSystemConfig power_system_config) {
		this.power_system_config = power_system_config;
	}

	public SimulationConfig getSimulation_config() {
		return simulation_config;
	}

	public void setSimulation_config(SimulationConfig simulation_config) {
		this.simulation_config = simulation_config;
	}

	public ApplicationConfig getApplication_config() {
		if(application_config==null) return new ApplicationConfig();
		return application_config;
	}

	public void setApplication_config(ApplicationConfig application_config) {
		this.application_config = application_config;
	}

	@Override
	public String toString() {
		return "ClassPojo [power_system_config = " + power_system_config
				+ ", simulation_config = " + simulation_config + "]";
	}
}