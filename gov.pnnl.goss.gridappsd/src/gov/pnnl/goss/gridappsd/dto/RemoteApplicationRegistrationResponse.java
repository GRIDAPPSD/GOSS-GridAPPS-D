package gov.pnnl.goss.gridappsd.dto;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class RemoteApplicationRegistrationResponse {
	
	public String heartbeatTopic;
	public String startControlTopic;
	public String statusControlTopic;
	public String stopControlTopic;
	public String errorTopic;
	public String unregisterTopic;
	public String applicationId;

	public RemoteApplicationRegistrationResponse() {
		
	}
	
	public String getApplicationId() {
		return applicationId;
	}

	public void setApplicationId(String applicationId) {
		applicationId = applicationId;
	}

	public String getStatusControlTopic() {
		return statusControlTopic;
	}

	public void setStatusControlTopic(String statusControlTopic) {
		this.statusControlTopic = statusControlTopic;
	}

	public void setStopControlTopic(String stopControlTopic) {
		this.stopControlTopic = stopControlTopic;
	}

	public String getUnregisterTopic() {
		return unregisterTopic;
	}

	public void setUnregisterTopic(String unregisterTopic) {
		this.unregisterTopic = unregisterTopic;
	}

	public String getHeartbeatTopic() {
		return heartbeatTopic;
	}
	public void setHeartbeatTopic(String heartbeatTopic) {
		this.heartbeatTopic = heartbeatTopic;
	}
	public String getStartControlTopic() {
		return startControlTopic;
	}
	public void setStartControlTopic(String startTopic) {
		this.startControlTopic = startTopic;
	}
	public String getStopControlTopic() {
		return stopControlTopic;
	}
	public void setControlStopTopic(String stopTopic) {
		this.stopControlTopic = stopTopic;
	}
	public String getErrorTopic() {
		return errorTopic;
	}
	public void setErrorTopic(String errorTopic) {
		this.errorTopic = errorTopic;
	}
	

	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}
	
	public static RemoteApplicationRegistrationResponse parse(String jsonString){
		Gson  gson = new Gson();
		RemoteApplicationRegistrationResponse obj = gson.fromJson(jsonString, RemoteApplicationRegistrationResponse.class);
		if(obj.startControlTopic == null)
			throw new JsonSyntaxException("Expected attribute StartTopic not found");
		return obj;
	}

}
