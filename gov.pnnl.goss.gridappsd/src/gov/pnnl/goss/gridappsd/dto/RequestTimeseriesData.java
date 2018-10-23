package gov.pnnl.goss.gridappsd.dto;

import gov.pnnl.goss.gridappsd.api.TimeseriesDataManager.ResultFormat;

import java.io.Serializable;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class RequestTimeseriesData implements Serializable {
	
	private static final long serialVersionUID = -820277813503252519L;
	
	public enum RequestType {
	    weather, simulation
	}
	
	RequestType queryMeasurement;
	Map<String,String> queryFilter;
	ResultFormat responseFormat = ResultFormat.JSON;
	private String queryType = "time-series";
	
	public RequestType getQueryMeasurement() {
		return queryMeasurement;
	}

	public void setQueryMeasurement(RequestType queryMeasurement) {
		this.queryMeasurement = queryMeasurement;
	}

	public Map<String, String> getQueryFilter() {
		return queryFilter;
	}

	public void setQueryFilter(Map<String, String> queryFilter) {
		this.queryFilter = queryFilter;
	}

	public ResultFormat getResponseFormat() {
		return responseFormat;
	}

	public void setResponseFormat(ResultFormat responseFormat) {
		this.responseFormat = responseFormat;
	}

	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}
	
	public static RequestTimeseriesData parse(String jsonString){
		Gson  gson = new Gson();
		RequestTimeseriesData obj = gson.fromJson(jsonString, RequestTimeseriesData.class);
		if(obj.queryMeasurement==RequestType.simulation && !obj.queryFilter.containsKey("simulationId"))
				throw new JsonSyntaxException("Expected attribute simulationId not found");
		return obj;
	}
	
}
