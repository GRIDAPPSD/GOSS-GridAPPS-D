package gov.pnnl.goss.gridappsd;

import java.io.Serializable;

import com.google.gson.Gson;

public class Difference implements Serializable{
	
	/**
	 *  
	 */
	private static final long serialVersionUID = 7965569387061225456L;
 
	public String object;
	
	public String attribute;

	public String value;
	
	@Override
	public String toString() {
		Gson  gson = new Gson(); 
		return gson.toJson(this);
	}

	public static Difference parse(String jsonString){
		Gson  gson = new Gson();
		Difference obj = gson.fromJson(jsonString, Difference.class);
		if(obj.object==null)
			throw new RuntimeException("Expected attribute object not found");
		return obj; 
	}
}
