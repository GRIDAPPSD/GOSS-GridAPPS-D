package gov.pnnl.goss.gridappsd.dto.field;

import java.io.Serializable;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class RequestField implements Serializable {

	private static final long serialVersionUID = 3088063820447700212L;
	
	public String request_type = null;
	public String areaId = null;
	public String start_time = null;
	public String end_time = null;
	public int interval_seconds = 15;
	
	
	public static RequestField parse(String jsonString) throws JsonSyntaxException {
		Gson  gson = new Gson();
		RequestField obj = gson.fromJson(jsonString, RequestField.class);
		return obj;
	}
	
	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}

}
