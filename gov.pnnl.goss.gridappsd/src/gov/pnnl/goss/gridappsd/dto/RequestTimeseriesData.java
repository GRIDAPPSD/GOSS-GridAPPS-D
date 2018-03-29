package gov.pnnl.goss.gridappsd.dto;

import java.io.Serializable;
import java.util.List;

import pnnl.goss.core.Request.RESPONSE_FORMAT;

public class RequestTimeseriesData implements Serializable {
	
	private static final long serialVersionUID = -820277813503252519L;
	
	
	String startTime;
	String endTime;
	String simulationId;
	List<String> objectMrids;
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
	public List<String> getObjectMrids() {
		return objectMrids;
	}
	public void setObjectMrids(List<String> objectMrids) {
		this.objectMrids = objectMrids;
	}
	public RESPONSE_FORMAT getResponseFormat() {
		return responseFormat;
	}
	public void setResponseFormat(RESPONSE_FORMAT responseFormat) {
		this.responseFormat = responseFormat;
	}
	
	
	
	
	

}
