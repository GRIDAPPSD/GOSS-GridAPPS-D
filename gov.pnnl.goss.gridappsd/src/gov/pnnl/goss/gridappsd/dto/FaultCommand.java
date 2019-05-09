package gov.pnnl.goss.gridappsd.dto;

import java.io.Serializable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import gov.pnnl.goss.gridappsd.dto.events.FailureEvent;

public class FaultCommand extends BaseEventCommand implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1611073142106355216L;
	public FailureEvent message;


	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}
	
	public JsonElement toJsonElement() {
		Gson  gson = new Gson();
		return gson.toJsonTree(this);
	}

	public static FaultCommand parse(String jsonString){
		Gson  gson = new Gson();
		FaultCommand obj = gson.fromJson(jsonString, FaultCommand.class);
		if(obj.command==null)
			throw new RuntimeException("Expected attribute object not found");
		return obj;
	}
}
