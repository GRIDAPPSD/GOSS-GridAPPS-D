package pnnl.goss.gridappsd.utils;

public class GridAppsDConstants {
	
	//user credentials
	public static final String username = "system";
	public static final String password = "manager";
	
	//topics
	private static final String topic_prefix = "goss/gridappsd";
	
	//Process Manager topics
	public static final String topic_request = topic_prefix+"/request/*";
	public static final String topic_requestSimulation = topic_prefix+"/request/simulation";
	public static final String topic_requestData = topic_prefix+"/request/data";
	public static final String topic_requestSimulationStatus = topic_prefix+"/request/simulation/status";
	
	//Configuration Manager topics
	public static final String topic_configuration = topic_prefix+"/configuration/*";
	
	//Simulation Manager Topics
	public static final String topic_simulationOutput = topic_prefix+"/simulation/output/";
	public static final String topic_simulationStatus = topic_prefix+"/simulation/status/";
	
	//Data Manager Topics
	public static final String topic_getDataFilesLocation = topic_prefix+"/data/filesLocation";
	public static final String topic_getDataContent = topic_prefix+"/data/content";
	
	
}
