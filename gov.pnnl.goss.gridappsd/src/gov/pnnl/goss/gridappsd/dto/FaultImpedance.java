package gov.pnnl.goss.gridappsd.dto;

import com.google.gson.Gson;

public class FaultImpedance {
	public Double rGround;
	public Double xGround;
	public Double rLineToLine;
	public Double xLineToLine;
	
	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}

	public static FaultImpedance parse(String jsonString){
		Gson  gson = new Gson();
		FaultImpedance obj = gson.fromJson(jsonString, FaultImpedance.class);
//		if(obj.rGround==null)
//			throw new RuntimeException("Expected attribute FaultMRID not found");
		return obj;
	}
	
}
	