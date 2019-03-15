package gov.pnnl.goss.gridappsd.dto;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class SimulationFault {
	

	public String FaultMRID;
	public FaultImpedance FaultImpedance;
	public String PhaseConnectedFaultKind;
    public String PhaseCode ;
    public String ObjectMRID;

	
	
	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}
	

	public JsonElement toJsonElement() {
		Gson  gson = new Gson();
		return gson.toJsonTree(this);
	}


	public static SimulationFault parse(String jsonString){
		Gson  gson = new Gson();
		SimulationFault obj = gson.fromJson(jsonString, SimulationFault.class);
		if(obj.FaultMRID==null)
			throw new RuntimeException("Expected attribute FaultMRID not found");
		return obj;
	}
}
