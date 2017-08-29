package gov.pnnl.goss.gridappsd.dto;

public class LogMessage {
	
	String process_id;
	String timestamp;
	String log_message;
	String log_level;
	String process_status;
	Boolean storeToDB = true;
	
	public String getProcess_id() {
		return process_id;
	}
	public void setProcess_id(String process_id) {
		this.process_id = process_id;
	}
	public String getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
	public String getLog_message() {
		return log_message;
	}
	public void setLog_message(String log_message) {
		this.log_message = log_message;
	}
	public String getLog_level() {
		return log_level;
	}
	public void setLog_level(String log_level) {
		this.log_level = log_level;
	}
	public String getProcess_status() {
		return process_status;
	}
	public void setProcess_status(String process_status) {
		this.process_status = process_status;
	}
	public Boolean getStoreToDB() {
		return storeToDB;
	}
	public void setStoreToDB(Boolean storeToDB) {
		this.storeToDB = storeToDB;
	}
	
	
}
