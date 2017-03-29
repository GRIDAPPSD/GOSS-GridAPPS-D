package pnnl.goss.gridappsd.utils;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class GridAppsDConstants {
	
	//user credentials
	public static final String username = "system";
	public static final String password = "manager";
	
	//topics
	private static final String topic_prefix = "goss/gridappsd";
	
	//Process Manager topics
	public static final String topic_request_prefix = topic_prefix+"/process/request";
	public static final String topic_requestSimulation = topic_request_prefix+"/simulation";
	public static final String topic_requestData = topic_request_prefix+"/data";
	public static final String topic_requestSimulationStatus = topic_request_prefix+"/simulation/status";
	
	//Configuration Manager topics
	public static final String topic_configuration = topic_prefix+"/configuration";
	public static final String topic_configuration_powergrid = topic_configuration+"/powergrid";
	public static final String topic_configuration_simulation = topic_configuration+"/simulation";
	
	//Simulation Manager Topics
	public static final String topic_simulation = topic_prefix+"/simulation";
	public static final String topic_simulationOutput = topic_simulation+"/output/";
	public static final String topic_simulationStatus = topic_simulation+"/status/";
	
	//Data Manager Topics
	public static final String topic_getDataFilesLocation = topic_prefix+"/data/filesLocation";
	public static final String topic_getDataContent = topic_prefix+"/data/content";
	
	//FNCS GOSS Bridge Topics
	public static final String topic_FNCS = topic_prefix+"/fncs";
	public static final String topic_FNCS_input = topic_FNCS+"/input";
	public static final String topic_FNCS_output = topic_FNCS+"/output";
	
	
	public static final String FNCS_PATH = "fncs.path";
	public static final String FNCS_BRIDGE_PATH = "fncs.bridge.path";
	public static final String GRIDLABD_PATH = "gridlabd.path";
	public static final String GRIDAPPSD_TEMP_PATH = "gridappsd.temp.path";
	
	public static final SimpleDateFormat SDF_SIMULATION_REQUEST = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	public static final SimpleDateFormat SDF_GLM_CLOCK = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	
	static{
		SDF_GLM_CLOCK.setTimeZone(TimeZone.getTimeZone("UTC"));
		SDF_SIMULATION_REQUEST.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	
}
