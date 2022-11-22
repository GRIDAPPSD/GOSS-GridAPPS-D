package gov.pnnl.goss.gridappsd.dto.field;

import java.io.Serializable;
import java.util.ArrayList;

import com.google.gson.Gson;

public class SecondaryArea implements Serializable{
	
    private static final long serialVersionUID = 1L;
	
    public ArrayList<String> distribution_transformer;
    public ArrayList<String> addressable_equipment;
    public ArrayList<String> unaddressable_equipment;
    public ArrayList<String> connectivity_node;
    public String message_bus_id;
    
    @Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}
    
}
