package gov.pnnl.goss.gridappsd.dto;

import java.util.HashMap;

public class TimeSeriesKeyValuePair{
	
	HashMap<String,Object> keyValuePair;

	public HashMap<String, Object> getKeyValuePair() {
		return keyValuePair;
	}

	public void setKeyValuePair(HashMap<String, Object> keyValuePair) {
		this.keyValuePair = keyValuePair;
	}
	
	public Object getValue(String key){
		return keyValuePair.get(key);
	}
	
}
