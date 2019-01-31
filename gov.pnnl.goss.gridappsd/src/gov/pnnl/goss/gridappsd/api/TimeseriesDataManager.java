package gov.pnnl.goss.gridappsd.api;

import java.io.Serializable;

import gov.pnnl.goss.gridappsd.dto.RequestTimeseriesData;

public interface TimeseriesDataManager {
//	public enum ResultFormat {
//	    JSON, XML, CSV
//	}
	
	Serializable query(RequestTimeseriesData requestTimeseriesData) throws Exception;
	
	void storeSimulationOutput(Serializable simulationOutput) throws Exception;
	
	void storeSimulationInput(Serializable simulationIput) throws Exception;

}
