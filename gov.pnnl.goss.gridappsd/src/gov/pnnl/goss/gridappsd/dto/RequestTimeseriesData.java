package gov.pnnl.goss.gridappsd.dto;

import java.io.IOException;

//import gov.pnnl.goss.gridappsd.api.TimeseriesDataManager.ResultFormat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.activemq.console.filter.QueryFilter;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class RequestTimeseriesData implements Serializable {
	
	private static final long serialVersionUID = -820277813503252519L;
	
	String queryMeasurement;
	//Map<String,Object> queryFilter;
	List<QueryFilter> queryFilter = new ArrayList<QueryFilter>();

	//ResultFormat responseFormat = ResultFormat.JSON;
	List<String> selectCriteria = new ArrayList<String>();
	int last = 1;
	String responseFormat ="JSON";
	private String queryType = "time-series";
	int simulationYear;
	String originalFormat = null;
	
	public String getQueryMeasurement() {
		return queryMeasurement;
	}

	public void setQueryMeasurement(String queryMeasurement) {
		this.queryMeasurement = queryMeasurement;
	}

	public List<QueryFilter> getQueryFilter() {
		return queryFilter;
	}

	public void setQueryFilter(List<QueryFilter> queryFilter) {
		this.queryFilter = queryFilter;
	}

	public String getResponseFormat() {
		return responseFormat;
	}

	public void setResponseFormat(String responseFormat) {
		this.responseFormat = responseFormat;
	}
	
	public int getSimulationYear() {
		return simulationYear;
	}

	public void setSimulationYear(int simulationYear) {
		this.simulationYear = simulationYear;
	}
	
	public String getOriginalFormat() {
		return originalFormat;
	}

	public void setOriginalFormat(String originalFormat) {
		this.originalFormat = originalFormat;
	}

	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}
	
	public static RequestTimeseriesData parse(String jsonString){
		ObjectMapper objectMapper = new ObjectMapper();
		RequestTimeseriesData obj = null;
		try {
			obj = objectMapper.readValue(jsonString, RequestTimeseriesData.class);
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(obj.queryMeasurement.equals("simulation")){
			//if(obj.queryFilter==null || !obj.queryFilter.containsKey("simulation_id"))
			//	throw new JsonSyntaxException("Expected filter simulation_id not found.");
		//TODO iterate through and look for key = simulation_id
		}
			return obj;
	}
	
	
	

	private class QueryFilter {
		String key;
		String eq;
		String ge;
		String le;
		String gt;
		String lt;
		public String getKey() {
			return key;
		}
		public void setKey(String key) {
			this.key = key;
		}
		public String getEq() {
			return eq;
		}
		public void setEq(String eq) {
			this.eq = eq;
		}
		public String getGe() {
			return ge;
		}
		public void setGe(String ge) {
			this.ge = ge;
		}
		public String getLe() {
			return le;
		}
		public void setLe(String le) {
			this.le = le;
		}
		public String getGt() {
			return gt;
		}
		public void setGt(String gt) {
			this.gt = gt;
		}
		public String getLt() {
			return lt;
		}
		public void setLt(String lt) {
			this.lt = lt;
		}
		
		
	}	
}
