package pnnl.goss.gridappsd.data;

import java.io.Serializable;

import pnnl.goss.core.Request;

public class DataRequest extends Request {
	String type;
	Serializable requestContent;
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Serializable getRequestContent() {
		return requestContent;
	}
	public void setRequestContent(Serializable requestContent) {
		this.requestContent = requestContent;
	}
	
	
}
