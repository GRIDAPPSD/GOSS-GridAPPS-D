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
	
	long startTime;
	long endTime;
	RequestType type;
	Map<String,String> filters;
	ResultFormat responseFormat = ResultFormat.JSON;
	
	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}

	public RequestType getType() {
		return type;
	}

	public void setType(RequestType type) {
		this.type = type;
	}

	public Map<String, String> getFilters() {
		return filters;
	}

	public void setFilters(Map<String, String> filters) {
		this.filters = filters;
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
		if(obj.type==RequestType.simulation && !obj.filters.containsKey("simulationId"))
				throw new JsonSyntaxException("Expected attribute simulationId not found");
		return obj;
	}
	
}
