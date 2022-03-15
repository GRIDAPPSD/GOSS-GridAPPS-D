package gov.pnnl.goss.gridappsd.dto.field;

import java.io.Serializable;

import com.google.gson.Gson;

public class Root implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public Feeder feeders;
	
	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}

}
