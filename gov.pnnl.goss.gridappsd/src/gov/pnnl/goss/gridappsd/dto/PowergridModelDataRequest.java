package gov.pnnl.goss.gridappsd.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class PowergridModelDataRequest implements Serializable{
	private static final long serialVersionUID = 8897993506912096791L;

	public enum RequestType {
	    QUERY, QUERY_OBJECT, QUERY_OBJECT_TYPES, QUERY_MODEL, QUERY_MODEL_NAMES, QUERY_MODEL_INFO, QUERY_OBJECT_IDS, QUERY_OBJECT_DICT, QUERY_OBJECT_MEASUREMENTS, INSERT_MEASUREMENTS, INSERT_ALL_MEASURMENTS, DROP_MEASUREMENTS, DROP_ALL_MEASUREMENTS, INSERT_HOUSES, INSERT_ALL_HOUSES, DROP_HOUSES, DROP_ALL_HOUSES
	}
	public enum ResultFormat {
	    JSON, XML, CSV 
	}
	
	public PowergridModelDataRequest(){}
	
	//Expected to match RequestType enum
	public String requestType;
	//For all except query model names
	public String modelId;
	//Expected to match ResultFormat enum
	public String resultFormat = "JSON";
	
	//For query
	public String queryString;
	//For query object
	public String objectId;
	//for query model
	public String filter;
	//for query model
	public String objectType;
	
	
	
	public String directory;
	public String modelName;
	
	public PowergridModelInfo model;
	public List<PowergridModelInfo> modelList;

	
	
	public String getModelId() {
		return modelId;
	}
	public void setModelId(String modelId) {
		this.modelId = modelId;
	}
	public String getResultFormat() {
		return resultFormat;
	}
	public void setResultFormat(String resultFormat) {
		this.resultFormat = resultFormat;
	}
	public String getQueryString() {
		return queryString;
	}
	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}
	public String getObjectId() {
		return objectId;
	}
	public void setObjectId(String objectId) {
		this.objectId = objectId;
	}
	public String getFilter() {
		return filter;
	}
	public void setFilter(String filter) {
		this.filter = filter;
	}
	public String getRequestType() {
		return requestType;
	}
	public void setRequestType(String requestType) {
		this.requestType = requestType;
	}

	
	
	
	public List<PowergridModelInfo> getModelList() {
		if(modelList==null){
			return new ArrayList<PowergridModelInfo>();
		}
		return modelList;
	}
	public void setModelList(List<PowergridModelInfo> modelList) {
		this.modelList = modelList;
	}
	public PowergridModelInfo getModel() {
		if(model==null){
			return new PowergridModelInfo();
		}
		return model;
	}
	public void setModel(PowergridModelInfo model) {
		this.model = model;
	}
	
	public String getObjectType() {
		return objectType;
	}
	public void setObjectType(String objectType) {
		this.objectType = objectType;
	}
	
	
	public String getDirectory() {
		return directory;
	}
	public void setDirectory(String directory) {
		this.directory = directory;
	}
	public String getModelName() {
		return modelName;
	}
	public void setModelName(String modelName) {
		this.modelName = modelName;
	}
	
	
	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}
	
	public static PowergridModelDataRequest parse(String jsonString){
		Gson  gson = new Gson();
		PowergridModelDataRequest obj = gson.fromJson(jsonString, PowergridModelDataRequest.class);
		if(obj.requestType==null)
			throw new JsonSyntaxException("Expected attribute requestType not found: "+jsonString);
		return obj;
	}
}
