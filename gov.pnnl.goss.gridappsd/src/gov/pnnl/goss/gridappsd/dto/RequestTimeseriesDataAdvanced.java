package gov.pnnl.goss.gridappsd.dto;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;


import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class RequestTimeseriesDataAdvanced extends RequestTimeseriesData {
	
	private static final long serialVersionUID = -820277813503252512L;
	
	List<Object> queryFilter = new ArrayList<Object>();
 
	List<String> selectCriteria = new ArrayList<String>();
	Integer last;
	Integer first;

	public List<Object> getQueryFilter() {
		return queryFilter;
	}

	public void setQueryFilter(List<Object> advancedQueryFilter) {
		this.queryFilter = advancedQueryFilter;
	}
	
	

	public List<String> getSelectCriteria() {
		return selectCriteria;
	}
	public void setSelectCriteria(List<String> selectCriteria) {
		this.selectCriteria = selectCriteria;
	}
	

	public Integer getLast() {
		return last;
	}

	public void setLast(Integer last) {
		this.last = last;
	}

	public Integer getFirst() {
		return first;
	}

	public void setFirst(Integer first) {
		this.first = first;
	}


	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}
	
	public static RequestTimeseriesDataAdvanced parse(String jsonString){
		ObjectMapper objectMapper = new ObjectMapper();
		RequestTimeseriesDataAdvanced obj = null;
		String error = "";
		try {
			obj = objectMapper.readValue(jsonString, RequestTimeseriesDataAdvanced.class);
		} catch (JsonParseException e) {
			e.printStackTrace();
			error = e.getMessage();
		} catch (JsonMappingException e) {
			e.printStackTrace();
			error = e.getMessage();
		} catch (IOException e) {
			e.printStackTrace();
			error = e.getMessage();
		}
		if(obj==null){
			throw new JsonSyntaxException("Request time series data request could not be parsed: "+error);
		}
		
//		if(obj!=null && obj.queryMeasurement.equals("simulation")){
			//if(obj.queryFilter==null || !obj.queryFilter.containsKey("simulation_id"))
			//	throw new JsonSyntaxException("Expected filter simulation_id not found.");
		//TODO iterate through and look for key = simulation_id
		return obj;
	}
	
	
	
}
