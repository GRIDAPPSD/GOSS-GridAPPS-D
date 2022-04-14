package gov.pnnl.goss.gridappsd.dto;


//import gov.pnnl.goss.gridappsd.api.TimeseriesDataManager.ResultFormat;

import java.io.Serializable;

import com.google.gson.Gson;

public abstract class RequestTimeseriesData implements Serializable {
	
	private static final long serialVersionUID = -820277813503252519L;
	
	String queryMeasurement;
	//ResultFormat responseFormat = ResultFormat.JSON;
	String responseFormat ="JSON";
	private String queryType = "time-series";
	int simulationYear;
	String originalFormat = null;
	
	public String getQueryMeasurement() {
		return queryMeasurement;
	}

	public void setQueryMeasurement(String queryMeasurement) {
		this.queryMeasurement = queryMeasurement;
	}


	public String getResponseFormat() {
		return responseFormat;
	}

	public void setResponseFormat(String responseFormat) {
		this.responseFormat = responseFormat;
	}
	
	public int getSimulationYear() {
		return simulationYear;
	}

	public void setSimulationYear(int simulationYear) {
		this.simulationYear = simulationYear;
	}
	
	public String getOriginalFormat() {
		return originalFormat;
	}

	public void setOriginalFormat(String originalFormat) {
		this.originalFormat = originalFormat;
	}
	
	public String getQueryType() {
		return queryType;
	}

	public void setQueryType(String queryType) {
		this.queryType = queryType;
	}

	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}
	
	
	
}
