package gov.pnnl.goss.gridappsd.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class TimeSeriesResult {
	
	List<MeasurementResult> measurements;
	
	public List<MeasurementResult> getMeasurements() {
		return measurements;
	}

	public void setMeasurements(List<MeasurementResult> measurements) {
		this.measurements = measurements;
	}
	
	public void addMeasurement(MeasurementResult measurement) {
		if(this.measurements == null){
			this.measurements = new ArrayList<MeasurementResult>();
		}
		this.measurements.add(measurement);
	}

	public class MeasurementResult{
		String name;
		List<RowResult> points;
		
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public List<RowResult> getPoints() {
			return points;
		}
		public void setPoints(List<RowResult> points) {
			this.points = points;
		}
	}
	
	public class RowResult{
		EntryResult row;

		public EntryResult getRow() {
			return row;
		}

		public void setRow(EntryResult row) {
			this.row = row;
		}
		
	}
	public class EntryResult {
		List<KeyValuePair> entry;

		public List<KeyValuePair> getEntry() {
			if(entry==null){
				entry = new ArrayList<KeyValuePair>();
			}
			return entry;
		}

		public void setEntry(List<KeyValuePair> entry) {
			this.entry = entry;
		}
		
		
		public HashMap<String, String>  getEntryMap() {
			HashMap<String, String> map = new HashMap<String, String>();
			for(KeyValuePair pair: getEntry()){
				map.put(pair.getKey(), pair.getValue());
			}
			return map;
			
		}
		
	}
	
	public class KeyValuePair{
		String key;
		String value;
		public String getKey() {
			return key;
		}
		public void setKey(String key) {
			this.key = key;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
		
	}
	
	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}
	
	public static TimeSeriesResult parse(String jsonString) {
		Gson  gson = new Gson();
		TimeSeriesResult obj = gson.fromJson(jsonString, TimeSeriesResult.class);
		if(obj.measurements==null)
			throw new JsonSyntaxException("Expected attribute measurements not found");
		return obj;
	}

}
