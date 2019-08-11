package gov.pnnl.goss.gridappsd.dto;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.jena.ext.com.google.common.reflect.TypeToken;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;


public class TimeSeriesEntryResult {
	ArrayList<HashMap<String,Object>> data;

	public ArrayList<HashMap<String,Object>> getData() {
		if(data==null){
			data = new ArrayList<HashMap<String,Object>>();
		}
		return data;
	}

	public void setData(ArrayList<HashMap<String,Object>> data) {
		this.data = data;
	}
	
	
	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}
	
	public static TimeSeriesEntryResult parse(String jsonString) {
		Gson  gson = new Gson();
		TimeSeriesEntryResult obj = gson.fromJson(jsonString, TimeSeriesEntryResult.class);
		if(obj.data==null)
			throw new JsonSyntaxException("Expected attribute measurements not found");
		return obj;
	}
	
	public static void main(String[] args){
		
		Type listType = new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType();
		
		String jsonString = "{\"data\":[{\"Diffuse\":19.958636,\"AvgWindSpeed\":7.9434,\"TowerRH\":31.61,\"long\":\"105.18 W\",\"MST\":\"13:44\",\"TowerDryBulbTemp\":74.534,\"DATE\":\"892013\",\"DirectCH1\":-0.0531206845,\"GlobalCM22\":20.2478337,\"AvgWindDirection\":359.3,\"time\":1376077440,\"place\":\"Solar Radiation Research Laboratory\",\"lat\":\"39.74 N\"}]}";
		Gson  gson = new Gson();
		//jsonString = jsonString.substring(8, jsonString.length()-1);
		
		System.out.println(jsonString);
		
		//ArrayList<HashMap<String, Object>> obj = gson.fromJson(jsonString, listType);
		TimeSeriesEntryResult obj = gson.fromJson(jsonString, TimeSeriesEntryResult.class);
		System.out.println(obj);
		
		
		
		
		
		
		
	}
	
}
