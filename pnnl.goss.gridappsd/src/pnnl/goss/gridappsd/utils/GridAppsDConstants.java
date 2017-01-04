package pnnl.goss.gridappsd.utils;

public class GridAppsDConstants {
	
	//user credentials
	public static final String username = "system";
	public static final String password = "manager";
	
	//topics
	private static final String topic_prefix = "/goss/gridappsd";
	public static final String topic_request = topic_prefix+"/request/*";
	public static final String topic_requestSimulation = topic_prefix+"/request/simulation";
	public static final String topic_requestData = topic_prefix+"/request/data";
	public static final String topic_requestSimulationStatus = topic_prefix+"/request/simulation/status";
	public static final String topic_configuration = topic_prefix+"/configuration";
	public static final String topic_simulationOutput = topic_prefix+"/simulation/output/";
	public static final String topic_simulationStatus = topic_prefix+"/simulation/status/";
	
	
}
