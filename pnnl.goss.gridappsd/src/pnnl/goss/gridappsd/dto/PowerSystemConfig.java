package pnnl.goss.gridappsd.dto;

import java.io.Serializable;

// TODO change to be a dto rather than full implementation of getters and setters.
public class PowerSystemConfig implements Serializable {
	
	public static String SubGeographicalRegion_name;

	public static String GeographicalRegion_name;

	public static String Line_name;

	public static String getSubGeographicalRegion_name() {
		return SubGeographicalRegion_name;
	}

	public static void setSubGeographicalRegion_name(String subGeographicalRegion_name) {
		SubGeographicalRegion_name = subGeographicalRegion_name;
	}

	public static String getGeographicalRegion_name() {
		return GeographicalRegion_name;
	}

	public static void setGeographicalRegion_name(String geographicalRegion_name) {
		GeographicalRegion_name = geographicalRegion_name;
	}

	public static String getLine_name() {
		return Line_name;
	}

	public static void setLine_name(String line_name) {
		Line_name = line_name;
	}

//	@Override
//	public String toString() {
//		return "ClassPojo [SubGeographicalRegion_name = "
//				+ SubGeographicalRegion_name + ", GeographicalRegion_name = "
//				+ GeographicalRegion_name + ", Line_name = " + Line_name + "]";
//	}
	
	
	
	
}