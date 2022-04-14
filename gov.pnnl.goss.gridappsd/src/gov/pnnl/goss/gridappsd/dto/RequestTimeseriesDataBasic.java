package gov.pnnl.goss.gridappsd.dto;

import java.io.IOException;

//import gov.pnnl.goss.gridappsd.api.TimeseriesDataManager.ResultFormat;

import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class RequestTimeseriesDataBasic extends RequestTimeseriesData {
	
	private static final long serialVersionUID = -820277813503252519L;
	
	String queryMeasurement;
	Map<String,Object> queryFilter;
	
	public String getQueryMeasurement() {
		return queryMeasurement;
	}

	public void setQueryMeasurement(String queryMeasurement) {
		this.queryMeasurement = queryMeasurement;
	}

	public Map<String, Object> getQueryFilter() {
		return queryFilter;
	}

	public void setQueryFilter(Map<String, Object> queryFilter) {
		this.queryFilter = queryFilter;
	}


	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}
	
	public static RequestTimeseriesDataBasic parse(String jsonString){
		ObjectMapper objectMapper = new ObjectMapper();
		RequestTimeseriesDataBasic obj = null;
		try {
			obj = objectMapper.readValue(jsonString, RequestTimeseriesDataBasic.class);
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(obj.queryMeasurement.equals("simulation"))
			if(obj.queryFilter==null || !obj.queryFilter.containsKey("simulation_id"))
				throw new JsonSyntaxException("Expected filter simulation_id not found.");
		return obj;
	}
	
}
