package gov.pnnl.goss.gridappsd.dto.field;

import java.io.Serializable;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class Substation implements Serializable {

	private static final long serialVersionUID = 1L;
	
	@SerializedName("@id")
	public String id;
	
	@SerializedName("@type")
	public String type;
	
	public ArrayList<NormalEnergizedFeeder> NormalEnergizedFeeder;
	
	public String message_bus_id;
	
	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}

}
