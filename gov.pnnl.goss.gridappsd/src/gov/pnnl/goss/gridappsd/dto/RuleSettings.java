package gov.pnnl.goss.gridappsd.dto;

import com.google.gson.Gson;

public class RuleSettings {
	public String name;
	
	public int port;
	
	public String topic;
	
	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}
	
	public static TestScript parse(String jsonString){
		Gson  gson = new Gson();
		TestScript obj = gson.fromJson(jsonString, TestScript.class);
		if(obj.name==null)
			throw new RuntimeException("Expected attribute name not found");
		return obj;
	}

}
