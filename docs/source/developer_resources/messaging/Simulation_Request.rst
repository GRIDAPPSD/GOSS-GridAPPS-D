Simulation Request
==================

Listens to topic goss/gridappsd/process/request/simulation and returns a simluationId.  

Process status will be sent on goss/gridappsd/simulation/status/<Simulation_ID>



{

	*power_system_config: the CIM model to be used in the simulation*
	
	"power_system_config": {
		"GeographicalRegion_name": "ieee8500nodecktassets_Region",
		"SubGeographicalRegion_name": "ieee8500nodecktassets_SubRegion",
		"Line_name": "ieee8500"
	},

	*simulation_config: the paramaters used by the simulation*
	"simulation_config": {
		"start_time": "2009-07-21 00:00:00",
		"duration": "120",
		"simulator": "GridLAB-D",
		"timestep_frequency": "1000",
		"timestep_increment": "1000",
		"simulation_name": "ieee8500",
		"power_flow_solver_method": "NR",
		
		*simulation_output: the objects and fields to be returned by the simulation*
		"simulation_output": {
			"output_objects": [{
				"name": "rcon_FEEDER_REG",
				"properties": ["connect_type",
				"Control",
				"control_level",
				"PT_phase",
				"band_center",
				"band_width",
				"dwell_time",
				"raise_taps",
				"lower_taps",
				"regulation"]
			},
			.....]
		},
		
		*model creation config: the paramaters used to generate the input file for the simulation*
		"model_creation_config": {
			"load_scaling_factor": "1",
			"schedule_name": "ieeezipload",
			"z_fraction": "0",
			"i_fraction": "1",
			"p_fraction": "0"
		}
	},
	
	*application config: inputs to any other applications that should run as part of the simluation, in this case the voltvar application*
	"application_config": {
		"applications": [{
			"name": "vvo",
			"config_string": "{\"static_inputs\": {\"ieee8500\" : {\"control_method\": \"ACTIVE\", \"capacitor_delay\": 60, \"regulator_delay\": 60, \"desired_pf\": 0.99, \"d_max\": 0.9, \"d_min\": 0.1,\"substation_link\": \"xf_hvmv_sub\",\"regulator_list\": [\"reg_FEEDER_REG\", \"reg_VREG2\", \"reg_VREG3\", \"reg_VREG4\"],\"regulator_configuration_list\": [\"rcon_FEEDER_REG\", \"rcon_VREG2\", \"rcon_VREG3\", \"rcon_VREG4\"],\"capacitor_list\": [\"cap_capbank0a\",\"cap_capbank0b\", \"cap_capbank0c\", \"cap_capbank1a\", \"cap_capbank1b\", \"cap_capbank1c\", \"cap_capbank2a\", \"cap_capbank2b\", \"cap_capbank2c\", \"cap_capbank3\"], \"voltage_measurements\": [\"nd_l2955047,1\", \"nd_l3160107,1\", \"nd_l2673313,2\", \"nd_l2876814,2\", \"nd_m1047574,3\", \"nd_l3254238,4\"],       \"maximum_voltages\": 7500, \"minimum_voltages\": 6500,\"max_vdrop\": 5200,\"high_load_deadband\": 100,\"desired_voltages\": 7000,   \"low_load_deadband\": 100,\"pf_phase\": \"ABC\"}}}"
		}]
	}
}

