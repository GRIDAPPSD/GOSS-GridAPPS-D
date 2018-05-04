package gov.pnnl.goss.gridappsd.api;

import java.io.Serializable;

import gov.pnnl.goss.gridappsd.data.DataRequest;

public interface DataManagerHandler {

	public Serializable handle(Serializable request) throws Exception; 
	
}
