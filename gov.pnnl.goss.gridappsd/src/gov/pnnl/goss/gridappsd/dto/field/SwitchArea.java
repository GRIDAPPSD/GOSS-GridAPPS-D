package gov.pnnl.goss.gridappsd.dto.field;

import java.io.Serializable;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class SwitchArea implements Serializable {

	private static final long serialVersionUID = 1L;
	
	@SerializedName("@id")
	public String id;
	
	@SerializedName("@type")
	public String type;
	
	public FieldObject feederArea;
	
	public ArrayList<FieldObject> BoundaryTerminals;
	public ArrayList<FieldObject> AddressableEquipment;
	public ArrayList<FieldObject> UnaddressableEquipment;
	public ArrayList<FieldObject> Measurements;
	public ArrayList<SecondaryArea> SecondaryAreas;	
    
	//public String message_bus_id;
    
    @Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}

}
