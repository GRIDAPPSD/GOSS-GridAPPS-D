package gov.pnnl.goss.gridappsd.dto;

import java.io.Serializable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class CommunicationFault implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1752104296052902945L;
	public String object;
	public String attribute;
    public CommunicationFaultData value ;


	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}
	
	public JsonElement toJsonElement() {
		Gson  gson = new Gson();
		return gson.toJsonTree(this);
	}

	public static CommunicationFault parse(String jsonString){
		Gson  gson = new Gson();
		CommunicationFault obj = gson.fromJson(jsonString, CommunicationFault.class);
		if(obj.object==null)
			throw new RuntimeException("Expected attribute object not found");
		return obj;
	}
}
