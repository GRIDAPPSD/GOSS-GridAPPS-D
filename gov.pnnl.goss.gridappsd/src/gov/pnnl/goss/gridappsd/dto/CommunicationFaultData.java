package gov.pnnl.goss.gridappsd.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class CommunicationFaultData extends BaseEvent implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2908489734896007494L;
	public List<Object> inputList = new ArrayList<Object>();
	public List<Object> outputList = new ArrayList<Object>();
	public Boolean filterAllInputs;
	public Boolean filterAllOutputs;
    
	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}
	
	public JsonElement toJsonElement() {
		Gson  gson = new Gson();
		return gson.toJsonTree(this);
	}

	public static CommunicationFaultData parse(String jsonString){
		Gson  gson = new Gson();
		CommunicationFaultData obj = gson.fromJson(jsonString, CommunicationFaultData.class);
		if(obj.filterAllInputs==null)
			throw new RuntimeException("Expected attribute object not found");
		return obj;
	}

	@Override
	public Object buildSimFault() {
		// TODO Auto-generated method stub
		return this;
	}
}
