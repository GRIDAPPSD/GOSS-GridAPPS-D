package gov.pnnl.goss.gridappsd.dto;

import java.io.Serializable;

import com.google.gson.Gson;

public class RequestPlatformStatus implements Serializable {

	private static final long serialVersionUID = 8110107008939698575L;
	
	boolean applications = false;
	boolean services = false;
	boolean appInstances = false;
	boolean serviceInstances = false;
	boolean field = false;
	
	public boolean isApplications() {
		return applications;
	}
	public void setApplications(boolean applications) {
		this.applications = applications;
	}
	public boolean isServices() {
		return services;
	}
	public void setServices(boolean services) {
		this.services = services;
	}
	public boolean isAppInstances() {
		return appInstances;
	}
	public void setAppInstances(boolean appInstances) {
		this.appInstances = appInstances;
	}
	public boolean isServiceInstances() {
		return serviceInstances;
	}
	public void setServiceInstances(boolean serviceInstances) {
		this.serviceInstances = serviceInstances;
	}
	public boolean isField() {
		return field;
	}
	public void setField(boolean field) {
		this.field = field;
	}
	
	public static RequestPlatformStatus parse(String jsonString){
		Gson  gson = new Gson();
		RequestPlatformStatus obj = gson.fromJson(jsonString, RequestPlatformStatus.class);
		if(!obj.appInstances & !obj.services & !obj.applications & !obj.serviceInstances & !obj.field){
			obj.applications = true;
			obj.services = true;
			obj.appInstances = true;
			obj.serviceInstances = true;
			obj.field = true;
		}
		return obj;
	}
	
}
