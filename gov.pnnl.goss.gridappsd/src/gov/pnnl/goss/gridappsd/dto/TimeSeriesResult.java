package gov.pnnl.goss.gridappsd.dto;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class TimeSeriesResult {
	
	List<TimeSeriesMeasurementResult> measurements;
	
	public List<TimeSeriesMeasurementResult> getMeasurements() {
		return measurements;
	}

	public void setMeasurements(List<TimeSeriesMeasurementResult> measurements) {
		this.measurements = measurements;
	}
	
	public void addMeasurement(TimeSeriesMeasurementResult measurement) {
		if(this.measurements == null){
			this.measurements = new ArrayList<TimeSeriesMeasurementResult>();
		}
		this.measurements.add(measurement);
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
