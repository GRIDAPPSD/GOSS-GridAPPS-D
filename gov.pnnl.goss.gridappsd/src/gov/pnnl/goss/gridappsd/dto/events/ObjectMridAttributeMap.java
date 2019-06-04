package gov.pnnl.goss.gridappsd.dto.events;

import java.io.Serializable;

public class ObjectMridAttributeMap implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	String objectMrid;
	String attribute;
	
	public ObjectMridAttributeMap(String objectMrid, String attribute) {
		super();
		this.objectMrid = objectMrid;
		this.attribute = attribute;
	}
	
	public String getObjectMrid() {
		return objectMrid;
	}
	public void setObjectMrid(String objectMrid) {
		this.objectMrid = objectMrid;
	}
	public String getAttribute() {
		return attribute;
	}
	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}
	
	
	
}
