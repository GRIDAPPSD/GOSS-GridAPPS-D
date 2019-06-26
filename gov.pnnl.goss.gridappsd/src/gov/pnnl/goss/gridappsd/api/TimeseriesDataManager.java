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

	void storeServiceOutput(Serializable message, String serviceId,
			String instanceId) throws Exception;

	void storeServiceInput(Serializable message, String serviceId,
			String instanceId) throws Exception;

	void storeAppOutput(Serializable message, String appId, String instanceId)
			throws Exception;

	void storeAppInput(Serializable message, String appId, String instanceId)
			throws Exception;

}
