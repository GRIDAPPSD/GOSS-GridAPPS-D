package gov.pnnl.goss.gridappsd.dto;

import java.io.Serializable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class EventCommand implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1611073142106355216L;
	public String command;
	public Long simulation_id;
    public CommunicationFaultData message;


	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}
	
	public JsonElement toJsonElement() {
		Gson  gson = new Gson();
		return gson.toJsonTree(this);
	}

	public static EventCommand parse(String jsonString){
		Gson  gson = new Gson();
		EventCommand obj = gson.fromJson(jsonString, EventCommand.class);
		if(obj.command==null)
			throw new RuntimeException("Expected attribute object not found");
		return obj;
	}
}
