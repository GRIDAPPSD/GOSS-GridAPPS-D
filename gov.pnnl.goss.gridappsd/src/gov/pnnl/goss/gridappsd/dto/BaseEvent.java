package gov.pnnl.goss.gridappsd.dto;

import java.io.Serializable;

import com.google.gson.Gson;

public abstract class BaseEvent implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4661909217634755114L;
	public String faultMRID;
	public String status;
	public Long timeInitiated;
	public Long timeCleared;

	/**
	 * Build a fault recoginizable by external elements such as the fncs_goss_bridges.py or simulator
	 * @return
	 */
	public abstract Object buildSimFault();
	
	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}

	public static BaseEvent parse(String jsonString){
		Gson  gson = new Gson();
		BaseEvent obj = gson.fromJson(jsonString, BaseEvent.class);
		if(obj.timeInitiated==null)
			throw new RuntimeException("Expected attribute simulation_id not found");
		return obj;
	}

}
