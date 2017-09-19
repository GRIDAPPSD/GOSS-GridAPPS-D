package gov.pnnl.goss.gridappsd.api;

import gov.pnnl.goss.gridappsd.dto.LogMessage;

public interface LogManager {

	
	void log(LogMessage message);
	
	
	void get(LogMessage message);
	
}
