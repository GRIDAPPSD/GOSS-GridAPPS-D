package gov.pnnl.goss.gridappsd.api;

import gov.pnnl.goss.gridappsd.dto.RequestTimeseriesData;

import java.io.Serializable;

public interface TimeseriesDataManager {
	public enum ResultFormat {
	    JSON, XML, CSV
	}
	
	String query(RequestTimeseriesData requestTimeseriesData) throws Exception;
	
	void storeSimulationOutput(Serializable simulationOutput) throws Exception;
	
	void storeSimulationInput(Serializable simulationIput) throws Exception;

}
