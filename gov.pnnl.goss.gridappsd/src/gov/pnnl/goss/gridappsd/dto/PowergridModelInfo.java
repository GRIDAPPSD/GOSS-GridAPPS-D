package gov.pnnl.goss.gridappsd.dto;

import java.io.Serializable;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class PowergridModelInfo implements Serializable{
	private static final long serialVersionUID = 8897993526912036791L;

	
	public PowergridModelInfo(){}
	public PowergridModelInfo(String modelId, String modelName)
	{
		this.modelId = modelId;
		this.modelName = modelName;
	}
	public PowergridModelInfo(String modelId, String modelName, String region, double scale, double seed)
	{
		this.modelId = modelId;
		this.modelName = modelName;
		this.region = region;
		this.scale = scale;
		this.seed = seed;
	}
	
	//Expected to match RequestType enum
	public String requestType;
	//For all except query model names
	public String modelId;
	
	public String modelName;
	
	public String region = "3";
	
	public double scale = 1.0;
	
	public double seed = 0;

	
	
	
	public String getRequestType() {
		return requestType;
	}

	public void setRequestType(String requestType) {
		this.requestType = requestType;
	}

	public String getModelId() {
		return modelId;
	}

	public void setModelId(String modelId) {
		this.modelId = modelId;
	}

	public String getModelName() {
		return modelName;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public double getScale() {
		return scale;
	}

	public void setScale(double scale) {
		this.scale = scale;
	}

	public double getSeed() {
		return seed;
	}

	public void setSeed(double seed) {
		this.seed = seed;
	}

	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}
	
	public static PowergridModelInfo parse(String jsonString){
		Gson  gson = new Gson();
		PowergridModelInfo obj = gson.fromJson(jsonString, PowergridModelInfo.class);
		if(obj.requestType==null)
			throw new JsonSyntaxException("Expected attribute requestType not found: "+jsonString);
		return obj;
	}
}
