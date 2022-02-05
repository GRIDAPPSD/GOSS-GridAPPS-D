package gov.pnnl.goss.gridappsd.api;

import java.io.Serializable;

public interface FieldBusManager {
	
	public Serializable handleRequest(String requestQueue, Serializable request);
	
}
