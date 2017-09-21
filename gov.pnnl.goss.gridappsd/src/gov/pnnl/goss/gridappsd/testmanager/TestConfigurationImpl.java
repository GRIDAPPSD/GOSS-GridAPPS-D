package gov.pnnl.goss.gridappsd.testmanager;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import com.google.gson.Gson;

import gov.pnnl.goss.gridappsd.api.TestConfiguration;

public class TestConfigurationImpl implements TestConfiguration,Serializable {


	private static final long serialVersionUID = 1L;

	public String power_system_configuration;

	public String simulation_configuration;
	
	public Integer durations;

	public Date run_start;

	public Date run_end;

	public String region_name;

	public String subregion_name;

	public String line_name;

	public Boolean logging;
	
	public Map<String,String> logging_options;
	
	public Map<String,String> initial_conditions;
	
	public Map<String,String> default_values;
	
	public String[] outputs;

	public TestConfigurationImpl() {

	}
	
	public String getPowerSystemConfiguration(){
		return power_system_configuration;		
	}

//	@Override
//	public String toString() {
//		return "TestConfiguration \n[power_system_configuration=" + power_system_configuration
//				+ " \n, simulation_configuration=" + simulation_configuration + " \n, durations=" + durations
//				+ " \n, run_start=" + run_start + " \n, run_end=" + run_end + " \n, region_name=" + region_name
//				+ " \n, subregion_name=" + subregion_name + " \n, line_name=" + line_name + " \n, logging=" + logging
//				+ " \n, logging_options=" + logging_options + " \n, initial_conditions=" + initial_conditions
//				+ " \n, default_values=" + default_values + " \n, outputs=" + Arrays.toString(outputs) + "]";
//	}

	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}
	
	public static TestConfigurationImpl parse(String jsonString){
		Gson  gson = new Gson();
		TestConfigurationImpl obj = gson.fromJson(jsonString, TestConfigurationImpl.class);
		if(obj.power_system_configuration==null)
			throw new RuntimeException("Expected attribute power_system_configuration not found");
		return obj;
	}

}
