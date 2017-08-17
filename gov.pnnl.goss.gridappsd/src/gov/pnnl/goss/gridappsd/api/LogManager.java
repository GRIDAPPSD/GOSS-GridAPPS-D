package gov.pnnl.goss.gridappsd.api;

public interface LogManager {

	void log(String process_id, String username, String timestamp,
			String log_message, String log_level, String process_status);
	
	void log(String message);
	
	public void store(String process_id, String username, String timestamp,
			String log_message, String log_level, String process_status);
}
