package gov.pnnl.goss.gridappsd.dto;

import java.io.Serializable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class CommunicationFaultObjectPair implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9134665254072433609L;
	public String objectMRID;
	public String attribute;

	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}
	
	public JsonElement toJsonElement() {
		Gson  gson = new Gson();
		return gson.toJsonTree(this);
	}

	public static CommunicationFaultObjectPair parse(String jsonString){
		Gson  gson = new Gson();
		CommunicationFaultObjectPair obj = gson.fromJson(jsonString, CommunicationFaultObjectPair.class);
		if(obj.objectMRID==null)
			throw new RuntimeException("Expected attribute object not found");
		return obj;
	}
}
