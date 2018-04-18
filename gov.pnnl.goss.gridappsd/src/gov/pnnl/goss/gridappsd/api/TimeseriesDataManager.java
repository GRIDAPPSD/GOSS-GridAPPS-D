package gov.pnnl.goss.gridappsd.api;

import gov.pnnl.goss.gridappsd.dto.RequestTimeseriesData;

public interface TimeseriesDataManager {
	public enum ResultFormat {
	    JSON, XML, CSV
	}
	
	String query(RequestTimeseriesData requestTimeseriesData) throws Exception;
	
	String store(RequestTimeseriesData requestTimeseriesData) throws Exception;
	
	String store(String requestTimeseriesData) throws Exception;

}
