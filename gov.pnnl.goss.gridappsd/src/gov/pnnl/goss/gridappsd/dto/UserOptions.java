package gov.pnnl.goss.gridappsd.dto;

import java.io.Serializable;

import com.google.gson.Gson;

public class UserOptions implements Serializable {

	private static final long serialVersionUID = 1L;
	
	String name;
	String help;
	String type;
	Object help_example;
	Object default_value;
	
	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}
	
}
