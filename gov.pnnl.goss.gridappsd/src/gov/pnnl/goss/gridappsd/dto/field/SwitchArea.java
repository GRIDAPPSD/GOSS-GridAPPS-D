package gov.pnnl.goss.gridappsd.dto.field;

import java.io.Serializable;
import java.util.ArrayList;

public class SwitchArea implements Serializable {

	private static final long serialVersionUID = 1L;
	
    public ArrayList<String> boundary_switches;
    public ArrayList<String> addressable_equipment;
    public ArrayList<String> unaddressable_equipment;
    public ArrayList<SecondaryArea> secondary_areas;	
    public ArrayList<String> connectivity_node;
    public String message_bus_id;

}
