package pnnl.goss.gridappsd.dto;

import java.util.ArrayList;
import java.util.List;

public class SimulationOutput {

	List<SimulationOutputObject> output_objects = new ArrayList<SimulationOutputObject>();

	


	public List<SimulationOutputObject> getOutputObjects() {
		return output_objects;
	}

	public void setOutputObjects(List<SimulationOutputObject> outputObjects) {
		this.output_objects = outputObjects;
	}
	
	
}
