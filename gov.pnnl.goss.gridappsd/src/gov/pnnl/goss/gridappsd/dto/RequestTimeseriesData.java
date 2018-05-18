package gov.pnnl.goss.gridappsd.dto;

import java.io.Serializable;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import pnnl.goss.core.Request.RESPONSE_FORMAT;

public class RequestTimeseriesData implements Serializable {
	
	private static final long serialVersionUID = -820277813503252519L;
	
	
	String startTime;
	String endTime;
	String simulationId;
	String mrid;
	RESPONSE_FORMAT responseFormat = RESPONSE_FORMAT.JSON;
	
	
	public String getStartTime() {
		return startTime;
	}
	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}
	public String getEndTime() {
		return endTime;
	}
	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}
	public String getSimulationId() {
		return simulationId;
	}
	public void setSimulationId(String simulationId) {
		this.simulationId = simulationId;
	}
	public String getMrid() {
		return mrid;
	}
	public void setMrid(String mrid) {
		this.mrid = mrid;
	}
	public RESPONSE_FORMAT getResponseFormat() {
		return responseFormat;
	}
	public void setResponseFormat(RESPONSE_FORMAT responseFormat) {
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
		if(obj.simulationId==null)
			throw new JsonSyntaxException("Expected attribute simulationId not found");
		return obj;
	}
	
	
	
	

}
