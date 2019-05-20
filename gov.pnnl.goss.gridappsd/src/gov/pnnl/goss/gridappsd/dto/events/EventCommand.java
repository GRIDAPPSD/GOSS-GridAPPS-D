package gov.pnnl.goss.gridappsd.dto.events;

import java.io.Serializable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import gov.pnnl.goss.gridappsd.dto.BaseEventCommand;

public class EventCommand extends BaseEventCommand implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1611073142106355216L;
//	public String command;
//	public Integer simulation_id;
    public CommOutage message;


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
		if(obj.message==null)
			throw new RuntimeException("Expected attribute object not found");
		return obj;
	}
}
