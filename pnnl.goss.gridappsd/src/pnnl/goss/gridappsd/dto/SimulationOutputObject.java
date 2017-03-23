package pnnl.goss.gridappsd.dto;

import java.util.List;

public class SimulationOutputObject {
	
	String name;
	List<String> properties;
	
	
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getProperties() {
		return properties;
	}

	public void setProperties(List<String> properties) {
		this.properties = properties;
	}
	

}
