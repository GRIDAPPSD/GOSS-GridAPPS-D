package gov.pnnl.goss.gridappsd;

import org.slf4j.Logger;

import static org.junit.Assert.*;


import static gov.pnnl.goss.gridappsd.TestConstants.*;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import gov.pnnl.goss.gridappsd.dto.ApplicationConfig;
import gov.pnnl.goss.gridappsd.dto.ApplicationObject;
import gov.pnnl.goss.gridappsd.dto.FncsBridgeResponse;
import gov.pnnl.goss.gridappsd.dto.ModelCreationConfig;
import gov.pnnl.goss.gridappsd.dto.PowerSystemConfig;
import gov.pnnl.goss.gridappsd.dto.RequestSimulation;
import gov.pnnl.goss.gridappsd.dto.SimulationConfig;
import gov.pnnl.goss.gridappsd.dto.SimulationOutput;
import gov.pnnl.goss.gridappsd.dto.SimulationOutputObject;
import pnnl.goss.core.Client;
import pnnl.goss.core.ClientFactory;

@RunWith(MockitoJUnitRunner.class)
public class DTOComponentTests {
	
	@Mock
	Logger logger;
	
	@Mock
	ClientFactory clientFactory;
	
	@Mock
	Client client;
	
	@Captor
	ArgumentCaptor<String> argCaptor;
	

	@Test
	public void powerSystemConfig_formatCheck(){
		//Verify that parsing a bad input fails
		PowerSystemConfig parseFail = null;
		try {
			parseFail = PowerSystemConfig.parse(SIMULATION_CONFIG);
		}catch(Exception e){}
		assertNull(parseFail);
		
		PowerSystemConfig parsed = PowerSystemConfig.parse(POWER_SYSTEM_CONFIG);
		assertNotNull(parsed.GeographicalRegion_name);
		assertNotNull(parsed.SubGeographicalRegion_name);
		assertNotNull(parsed.Line_name);
		
		//Create and Initialize with Values PowerSystemConfig
		PowerSystemConfig config = generatePowerSystemConfig();
		
		//Assert equal serialized PSC and comparison string
		assertEquals(config.toString(), POWER_SYSTEM_CONFIG);
				
	}
	
	
	@Test
	public void applicationObject_formatCheck(){
		//Verify that parsing a bad input fails
		ApplicationObject parseFail = null;
		try {
			parseFail = ApplicationObject.parse(SIMULATION_CONFIG);
		}catch(Exception e){}
		assertNull(parseFail);
				
		//Verify parse from string
		ApplicationObject parsed = ApplicationObject.parse(APPLICATION_OBJECT);
		assertNotNull(parsed.name);
		assertNotNull(parsed.config_string);
		
		//Create and Initialize with DTO object for serialization
		ApplicationObject config = generateApplicationObject();
		
		//Assert equal serialized object and comparison string
		assertEquals(config.toString(), APPLICATION_OBJECT_ESC);
	}
	
	
	@Test
	public void applicationConfig_formatCheck(){
		//Verify that parsing a bad input fails
		ApplicationConfig parseFail = null;
		try {
			parseFail = ApplicationConfig.parse(SIMULATION_CONFIG);
		}catch(Exception e){}
		assertNull(parseFail);
				
		//Verify parse from string
		ApplicationConfig parsed = ApplicationConfig.parse(APPLICATION_CONFIG);
		assertEquals(1, parsed.applications.length);
		
		//Create and Initialize with DTO object for serialization
		ApplicationConfig config = generateApplicationConfig();
		
		//Serialize DTO Object
		String serialized = config.toString();

		//Assert equal serialized object and comparison string
		assertEquals(serialized, APPLICATION_CONFIG_ESC);
	}
	
	
	
	@Test
	public void fncsBridgeResponse_formatCheck(){
		//Verify that parsing a bad input fails
		FncsBridgeResponse parseFail = null;
		try {
			parseFail = FncsBridgeResponse.parse(SIMULATION_CONFIG);
		}catch(Exception e){}
		assertNull(parseFail);

		//Verify parse from string
		FncsBridgeResponse parsed = FncsBridgeResponse.parse(FNCS_BRIDGE_RESPONSE);
		assertNotNull(parsed.command);
		assertNotNull(parsed.response);
		assertNotNull(parsed.output);
						
				
		FncsBridgeResponse config = generateFncsBridgeResponse();

		//Assert equal serialized object and comparison string
		assertEquals(config.toString(), FNCS_BRIDGE_RESPONSE);		
	}
	
	@Test
	public void modelCreationConfig_formatCheck(){
		//Verify that parsing a bad input fails
		ModelCreationConfig parseFail = null;
		try {
			parseFail = ModelCreationConfig.parse(SIMULATION_CONFIG);
		}catch(Exception e){}
		assertNull(parseFail);

		//Verify parse from string
		ModelCreationConfig parsed = ModelCreationConfig.parse(MODEL_CREATION_CONFIG);
		assertNotNull(parsed.load_scaling_factor);
		assertNotNull(parsed.schedule_name);
		assertNotNull(parsed.z_fraction);
		assertNotNull(parsed.i_fraction);
		assertNotNull(parsed.p_fraction);
				
		//Create and Initialize with DTO object for serialization
		ModelCreationConfig config = generateModelCreationConfig();
				
		//Assert equal serialized object and comparison string
		assertEquals(config.toString(), MODEL_CREATION_CONFIG);
	}
	
	@Test
	public void requestSimulation_formatCheck(){
		//Verify that parsing a bad input fails
		RequestSimulation parseFail = null;
		try {
			parseFail = RequestSimulation.parse(SIMULATION_CONFIG);
		}catch(Exception e){}
		assertNull(parseFail);

		//Verify parse from string
		RequestSimulation parsed = RequestSimulation.parse(REQUEST_SIMULATION_CONFIG);
		assertNotNull(parsed.application_config);
		assertNotNull(parsed.power_system_config);
		assertNotNull(parsed.simulation_config);
				
		//Create and Initialize with DTO object for serialization
		RequestSimulation config = new RequestSimulation();
		config.power_system_config = generatePowerSystemConfig();
		config.simulation_config = generateSimulationConfig();
		config.application_config = generateApplicationConfig();
		
		//Assert equal serialized object and comparison string
		assertEquals(config.toString(), REQUEST_SIMULATION_CONFIG_ESC);
	}
	
	@Test
	public void simulationConfig_formatCheck(){
		//Verify that parsing a bad input fails
		SimulationConfig parseFail = null;
		try {
			parseFail = SimulationConfig.parse(REQUEST_SIMULATION_CONFIG);
		}catch(Exception e){}
		assertNull(parseFail);
		
		//Verify parse from string
		SimulationConfig parsed = SimulationConfig.parse(SIMULATION_CONFIG);
		assertNotNull(parsed.duration);
		assertNotNull(parsed.model_creation_config);
		assertNotNull(parsed.power_flow_solver_method);
		assertNotNull(parsed.simulation_name);
		assertNotNull(parsed.simulation_output);
		assertNotNull(parsed.simulator);
		assertNotNull(parsed.start_time);
		assertNotNull(parsed.timestep_frequency);
		assertNotNull(parsed.timestep_increment);
						
		//Create and Initialize with DTO object for serialization
		SimulationConfig config = generateSimulationConfig();
						
		//Assert equal serialized object and comparison string
		assertEquals(config.toString(), SIMULATION_CONFIG);
		
		
	}
	
	@Test
	public void simulationOutput_formatCheck(){
		//Verify that parsing a bad input fails
		SimulationOutput parseFail = null;
		try {
			parseFail = SimulationOutput.parse(SIMULATION_CONFIG);
		}catch(Exception e){}
		assertNull(parseFail);

		//Verify parse from string
		SimulationOutput parsed = SimulationOutput.parse(SIMULATION_CONFIG_OUTPUT_FULL);
		assertEquals(29, parsed.output_objects.size());
				
		
		SimulationOutput config = generateSimulationOutput();
				
		//Assert equal serialized object and comparison string
		assertEquals(config.toString(), SIMULATION_CONFIG_OUTPUT_SHORT);		
	}
	
	@Test
	public void simulationOutputObject_formatCheck(){
		//Verify that parsing a bad input fails
		SimulationOutputObject parseFail = null;
		try {
			parseFail = SimulationOutputObject.parse(SIMULATION_CONFIG);
		}catch(Exception e){}
		assertNull(parseFail);

		//Verify parse from string
		SimulationOutputObject parsed = SimulationOutputObject.parse(SIMULATION_CONFIG_OUTPUT_OBJECT_1);
		assertNotNull(parsed.name);
		assertEquals(10, parsed.properties.size());
				
		//Create and Initialize with DTO object for serialization
		SimulationOutputObject config = generateSimulationOutputObject("rcon_FEEDER_REG");
				
		//Assert equal serialized object and comparison string
		assertEquals(config.toString(), SIMULATION_CONFIG_OUTPUT_OBJECT_1);		
		
	}
	
	
	
	private SimulationConfig generateSimulationConfig(){
		SimulationOutput configOutput = generateSimulationOutput();
		SimulationConfig config = new SimulationConfig();
		config.start_time = "2009-07-21 00:00:00";
		config.duration = 120;
		config.simulator = "GridLAB-D";
		config.timestep_frequency = 1000;
		config.timestep_increment = 1000;
		config.simulation_name = "ieee8500";
		config.power_flow_solver_method = "NR";
		
		config.simulation_output = configOutput;
		config.model_creation_config = generateModelCreationConfig();

		return config;
	}
	
	private PowerSystemConfig generatePowerSystemConfig(){
		PowerSystemConfig config = new PowerSystemConfig();
		config.GeographicalRegion_name = "ieee8500_Region";
		config.Line_name = "ieee8500";
		config.SubGeographicalRegion_name = "ieee8500_SubRegion";
		return config;
	}
	
	private ModelCreationConfig generateModelCreationConfig(){
		ModelCreationConfig config = new ModelCreationConfig();
		config.load_scaling_factor = 1;
		config.schedule_name = "ieeezipload";
		config.z_fraction = 0;
		config.i_fraction = 1;
		config.p_fraction = 0;
		
		return config;
	}
	
	private SimulationOutput generateSimulationOutput(){
		
		SimulationOutputObject configObj1 = generateSimulationOutputObject("rcon_FEEDER_REG");
		SimulationOutputObject configObj2 = generateSimulationOutputObject("rcon_VREG2");
		
		//Create and Initialize with DTO object for serialization
		SimulationOutput config = new SimulationOutput();
		config.getOutputObjects().add(configObj1);
		config.getOutputObjects().add(configObj2);
		
		return config;
	}
	
	private SimulationOutputObject generateSimulationOutputObject(String name){
		String[] propsArray = new String[]{"connect_type", "Control", "control_level", "PT_phase", "band_center", "band_width", "dwell_time", "raise_taps", "lower_taps", "regulation"};
		SimulationOutputObject config = new SimulationOutputObject();
		config.name = name;
		config.properties = Arrays.asList(propsArray);
		return config;
	}
	
	
	
	private ApplicationObject generateApplicationObject(){
		ApplicationObject obj = new ApplicationObject();
		obj.config_string = APPLICATION_OBJECT_CONFIG_ESC.replaceAll("\\\\\\\\", "\\");
		obj.name = "vvo";
		return obj;
	}
	
	private ApplicationConfig generateApplicationConfig(){
		ApplicationConfig config = new ApplicationConfig();
		config.applications = new ApplicationObject[]{generateApplicationObject()};
		return config;
	}
	
	private FncsBridgeResponse generateFncsBridgeResponse(){
		FncsBridgeResponse config = new FncsBridgeResponse();
		config.command = "isInitialized";
		config.output = "Any messages from simulator regarding initialization";
		config.response = "true";
		return config;
	}

}
