package pnnl.goss.gridappsd.dto;

import java.io.Serializable;

// TODO change to be a dto rather than full implementation of getters and setters.
public class PowerSystemConfig implements Serializable {
	
	public String SubGeographicalRegion_name;

	public String GeographicalRegion_name;

	public String Line_name;

	public String getSubGeographicalRegion_name() {
		return SubGeographicalRegion_name;
	}

	public void setSubGeographicalRegion_name(String subGeographicalRegion_name) {
		SubGeographicalRegion_name = subGeographicalRegion_name;
	}

	public String getGeographicalRegion_name() {
		return GeographicalRegion_name;
	}

	public void setGeographicalRegion_name(String geographicalRegion_name) {
		GeographicalRegion_name = geographicalRegion_name;
	}

	public String getLine_name() {
		return Line_name;
	}

	public void setLine_name(String line_name) {
		Line_name = line_name;
	}

//	@Override
	public String toString() {
		return "ClassPojo [SubGeographicalRegion_name = "
				+ SubGeographicalRegion_name + ", GeographicalRegion_name = "
				+ GeographicalRegion_name + ", Line_name = " + Line_name + "]";
	}
	
	
	
	
}