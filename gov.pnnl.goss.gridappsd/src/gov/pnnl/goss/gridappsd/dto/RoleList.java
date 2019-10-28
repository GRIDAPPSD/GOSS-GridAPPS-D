package gov.pnnl.goss.gridappsd.dto;

import java.util.List;

import com.google.gson.Gson;

public class RoleList {
	public List<String> roles;

	public List<String> getRoles() {
		return roles;
	}

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}
	
	
	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}
	
	
	public static RoleList parse(String jsonString){
		Gson  gson = new Gson();
		RoleList obj = gson.fromJson(jsonString, RoleList.class);
		return obj;
	}
}
