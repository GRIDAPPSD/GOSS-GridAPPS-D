package gov.pnnl.goss.gridappsd.dto.events;

import gov.pnnl.goss.gridappsd.dto.FaultImpedance;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class SimulationFault extends Event{
	
	private static final long serialVersionUID = 2638861907618920676L;
	
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
		if(obj.faultMRID==null)
			throw new RuntimeException("Expected attribute faultMRID not found");
		return obj;
	}
}
