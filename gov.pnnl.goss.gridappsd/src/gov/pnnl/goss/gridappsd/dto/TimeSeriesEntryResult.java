package gov.pnnl.goss.gridappsd.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class TimeSeriesEntryResult {
	List<TimeSeriesKeyValuePair> entry;

	public List<TimeSeriesKeyValuePair> getEntry() {
		if(entry==null){
			entry = new ArrayList<TimeSeriesKeyValuePair>();
		}
		return entry;
	}

	public void setEntry(List<TimeSeriesKeyValuePair> entry) {
		this.entry = entry;
	}
	
	
	public HashMap<String, String>  getEntryMap() {
		HashMap<String, String> map = new HashMap<String, String>();
		for(TimeSeriesKeyValuePair pair: getEntry()){
			map.put(pair.getKey(), pair.getValue());
		}
		return map;
		
	}
	
}
