package gov.pnnl.goss.gridappsd.api;

import gov.pnnl.goss.gridappsd.dto.LogMessage;

public interface LogDataManager {
	
	void store (String process_id, String username, String timestamp,
			String log_message, String log_level, String process_status);
	
	void query(String process_id, String timestamp, String log_level, String process_status);
	
}
