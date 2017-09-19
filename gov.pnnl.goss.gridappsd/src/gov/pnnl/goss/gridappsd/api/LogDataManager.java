package gov.pnnl.goss.gridappsd.api;

import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;

public interface LogDataManager {
	
	void store (String process_id, String username, long timestamp,
			String log_message, LogLevel log_level, ProcessStatus process_status);
	
	void query(String process_id, long timestamp, LogLevel log_level, ProcessStatus process_status, String username);
	
}
