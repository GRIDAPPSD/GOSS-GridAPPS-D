package gov.pnnl.goss.gridappsd.dto;

import java.io.Serializable;
import java.util.HashMap;

public class ServiceConfig implements Serializable {
	
	private static final long serialVersionUID = -2413334775260242364L;
	
	String id;
	HashMap<String,UserInput> user_input;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public HashMap<String, UserInput> getUser_input() {
		return user_input;
	}
	public void setUser_input(HashMap<String, UserInput> user_input) {
		this.user_input = user_input;
	}
	
	
	

}
