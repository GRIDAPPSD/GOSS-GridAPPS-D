package gov.pnnl.goss.gridappsd.api;

import java.io.Serializable;


public interface DataManagerHandler {

	public Serializable handle(Serializable requestContent, String processId, String username) throws Exception; 
	
}
