package gov.pnnl.goss.gridappsd.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class PowergridModelList implements Serializable{
	private static final long serialVersionUID = 8897993506312096791L;

	
	public PowergridModelList(){}
	
	public List<PowergridModelInfo> modelList;

	
	
	
	public List<PowergridModelInfo> getModelList() {
		if(modelList==null){
			return new ArrayList<PowergridModelInfo>();
		}
		return modelList;
	}
	public void setModelList(List<PowergridModelInfo> modelList) {
		this.modelList = modelList;
	}
	
	
	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}
	
	public static PowergridModelList parse(String jsonString){
		Gson  gson = new Gson();
		PowergridModelList obj = gson.fromJson(jsonString, PowergridModelList.class);
		if(obj.modelList==null)
			throw new JsonSyntaxException("Expected attribute modelList not found: "+jsonString);
		return obj;
	}
}
