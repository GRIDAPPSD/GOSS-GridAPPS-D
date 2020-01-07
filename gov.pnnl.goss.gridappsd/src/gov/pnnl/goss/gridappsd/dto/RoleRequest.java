package gov.pnnl.goss.gridappsd.dto;

import java.io.Serializable;
import com.google.gson.Gson;

public class RoleRequest implements Serializable{
	
	private static final long serialVersionUID = -3277794121736133832L;


	public RoleRequest(){}
	
	public String user;
	
	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}
	
	public static RoleRequest parse(String jsonString){
		Gson  gson = new Gson();
		RoleRequest obj = gson.fromJson(jsonString, RoleRequest.class);
		return obj;
	}
}
