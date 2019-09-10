package gov.pnnl.goss.gridappsd.dto;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class TimeSeriesResult {
	
	List<TimeSeriesMeasurementResult> data;
	
	public List<TimeSeriesMeasurementResult> getMeasurements() {
		return data;
	}

	public void setMeasurements(List<TimeSeriesMeasurementResult> data) {
		this.data = data;
	}
	
	public void addMeasurement(TimeSeriesMeasurementResult data) {
		if(this.data == null){
			this.data = new ArrayList<TimeSeriesMeasurementResult>();
		}
		this.data.add(data);
	}
	
	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}
	
	public static TimeSeriesResult parse(String jsonString) {
		Gson  gson = new Gson();
		TimeSeriesResult obj = gson.fromJson(jsonString, TimeSeriesResult.class);
		if(obj.data==null)
			throw new JsonSyntaxException("Expected attribute measurements not found");
		return obj;
	}

}
