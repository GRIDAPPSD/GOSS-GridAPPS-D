/*******************************************************************************
 * Copyright (c) 2017, Battelle Memorial Institute All rights reserved.
 * Battelle Memorial Institute (hereinafter Battelle) hereby grants permission to any person or entity
 * lawfully obtaining a copy of this software and associated documentation files (hereinafter the
 * Software) to redistribute and use the Software in source and binary forms, with or without modification.
 * Such person or entity may use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and may permit others to do so, subject to the following conditions:
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the
 * following disclaimers.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided with the distribution.
 * Other than as used herein, neither the name Battelle Memorial Institute or Battelle may be used in any
 * form whatsoever without the express written consent of Battelle.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * BATTELLE OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * General disclaimer for use with OSS licenses
 *
 * This material was prepared as an account of work sponsored by an agency of the United States Government.
 * Neither the United States Government nor the United States Department of Energy, nor Battelle, nor any
 * of their employees, nor any jurisdiction or organization that has cooperated in the development of these
 * materials, makes any warranty, express or implied, or assumes any legal liability or responsibility for
 * the accuracy, completeness, or usefulness or any information, apparatus, product, software, or process
 * disclosed, or represents that its use would not infringe privately owned rights.
 *
 * Reference herein to any specific commercial product, process, or service by trade name, trademark, manufacturer,
 * or otherwise does not necessarily constitute or imply its endorsement, recommendation, or favoring by the United
 * States Government or any agency thereof, or Battelle Memorial Institute. The views and opinions of authors expressed
 * herein do not necessarily state or reflect those of the United States Government or any agency thereof.
 *
 * PACIFIC NORTHWEST NATIONAL LABORATORY operated by BATTELLE for the
 * UNITED STATES DEPARTMENT OF ENERGY under Contract DE-AC05-76RL01830
 ******************************************************************************/
package gov.pnnl.goss.gridappsd;

public class TestConstants {
    public static final String POWER_SYSTEM_CONFIG = "{\"SubGeographicalRegion_name\":\"ieee8500_SubRegion\",\"GeographicalRegion_name\":\"ieee8500_Region\",\"Line_name\":\"ieee8500\"}";
    // The _ESC files are because of the escaped "'s necessary in the application
    // object. there should only be a single \ when parsing, but triple \\\ when
    // comparing. This applies to anything that includes the application object
    public static final String APPLICATION_OBJECT_CONFIG_ESC = "{\\\"static_inputs\\\": {\\\"ieee8500\\\" : {\\\"control_method\\\": \\\"ACTIVE\\\", \\\"capacitor_delay\\\": 60, \\\"regulator_delay\\\": 60, \\\"desired_pf\\\": 0.99, \\\"d_max\\\": 0.9, \\\"d_min\\\": 0.1,\\\"substation_link\\\": \\\"xf_hvmv_sub\\\",\\\"regulator_list\\\": [\\\"reg_FEEDER_REG\\\", \\\"reg_VREG2\\\", \\\"reg_VREG3\\\", \\\"reg_VREG4\\\"],\\\"regulator_configuration_list\\\": [\\\"rcon_FEEDER_REG\\\", \\\"rcon_VREG2\\\", \\\"rcon_VREG3\\\", \\\"rcon_VREG4\\\"],\\\"capacitor_list\\\": [\\\"cap_capbank0a\\\",\\\"cap_capbank0b\\\", \\\"cap_capbank0c\\\", \\\"cap_capbank1a\\\", \\\"cap_capbank1b\\\", \\\"cap_capbank1c\\\", \\\"cap_capbank2a\\\", \\\"cap_capbank2b\\\", \\\"cap_capbank2c\\\", \\\"cap_capbank3\\\"], \\\"voltage_measurements\\\": [\\\"nd_l2955047,1\\\", \\\"nd_l3160107,1\\\", \\\"nd_l2673313,2\\\", \\\"nd_l2876814,2\\\", \\\"nd_m1047574,3\\\", \\\"nd_l3254238,4\\\"], \\\"maximum_voltages\\\": 7500, \\\"minimum_voltages\\\": 6500,\\\"max_vdrop\\\": 5200,\\\"high_load_deadband\\\": 100,\\\"desired_voltages\\\": 7000,   \\\"low_load_deadband\\\": 100,\\\"pf_phase\\\": \\\"ABC\\\"}}}";
    public static final String APPLICATION_OBJECT_ESC = "{\"name\":\"vvo\",\"config_string\":\""
            + APPLICATION_OBJECT_CONFIG_ESC.replaceAll("\\\\", "\\\\\\\\\\\\") + "\"}";
    public static final String APPLICATION_OBJECT = "{\"name\":\"vvo\",\"config_string\":\""
            + APPLICATION_OBJECT_CONFIG_ESC + "\"}";

    public static final String APPLICATION_CONFIG_ESC = "{\"applications\":["
            + APPLICATION_OBJECT.replaceAll("\\\\", "\\\\\\\\\\\\") + "]}";
    public static final String APPLICATION_CONFIG = "{\"applications\":[" + APPLICATION_OBJECT + "]}";

    public static final String MODEL_CREATION_CONFIG = "{\"load_scaling_factor\":1.0,\"triplex\":\"y\",\"encoding\":\"u\",\"system_frequency\":60,\"voltage_multiplier\":1.0,\"power_unit_conversion\":1.0,\"unique_names\":\"y\",\"schedule_name\":\"ieeezipload\",\"z_fraction\":0.0,\"i_fraction\":1.0,\"p_fraction\":0.0}";

    public static final String SIMULATION_CONFIG_OUTPUT_OBJECT_1 = "{\"name\":\"rcon_FEEDER_REG\",\"properties\":[\"connect_type\",\"Control\",\"control_level\",\"PT_phase\",\"band_center\",\"band_width\",\"dwell_time\",\"raise_taps\",\"lower_taps\",\"regulation\"]}";
    public static final String SIMULATION_CONFIG_OUTPUT_OBJECT_2 = "{\"name\":\"rcon_VREG2\",\"properties\":[\"connect_type\",\"Control\",\"control_level\",\"PT_phase\",\"band_center\",\"band_width\",\"dwell_time\",\"raise_taps\",\"lower_taps\",\"regulation\"]}";
    public static final String SIMULATION_CONFIG_OUTPUT_FULL = "{\"output_objects\":[{\"name\":\"rcon_FEEDER_REG\",\"properties\":[\"connect_type\",\"Control\",\"control_level\",\"PT_phase\",\"band_center\",\"band_width\",\"dwell_time\",\"raise_taps\",\"lower_taps\",\"regulation\"]},{\"name\":\"rcon_VREG2\",\"properties\":[\"connect_type\",\"Control\",\"control_level\",\"PT_phase\",\"band_center\",\"band_width\",\"dwell_time\",\"raise_taps\",\"lower_taps\",\"regulation\"]},{\"name\":\"rcon_VREG3\",\"properties\":[\"connect_type\",\"Control\",\"control_level\",\"PT_phase\",\"band_center\",\"band_width\",\"dwell_time\",\"raise_taps\",\"lower_taps\",\"regulation\"]},{\"name\":\"rcon_VREG4\",\"properties\":[\"connect_type\",\"Control\",\"control_level\",\"PT_phase\",\"band_center\",\"band_width\",\"dwell_time\",\"raise_taps\",\"lower_taps\",\"regulation\"]},{\"name\":\"reg_FEEDER_REG\",\"properties\":[\"configuration\",\"phases\",\"to\",\"tap_A\",\"tap_B\",\"tap_C\"]},{\"name\":\"reg_VREG2\",\"properties\":[\"configuration\",\"phases\",\"to\",\"tap_A\",\"tap_B\",\"tap_C\"]},{\"name\":\"reg_VREG3\",\"properties\":[\"configuration\",\"phases\",\"to\",\"tap_A\",\"tap_B\",\"tap_C\"]},{\"name\":\"reg_VREG4\",\"properties\":[\"configuration\",\"phases\",\"to\",\"tap_A\",\"tap_B\",\"tap_C\"]},{\"name\":\"cap_capbank0a\",\"properties\":[\"phases\",\"pt_phase\",\"phases_connected\",\"control\",\"control_level\",\"capacitor_A\",\"dwell_time\",\"switchA\"]},{\"name\":\"cap_capbank1a\",\"properties\":[\"phases\",\"pt_phase\",\"phases_connected\",\"control\",\"control_level\",\"capacitor_A\",\"dwell_time\",\"switchA\"]},{\"name\":\"cap_capbank2a\",\"properties\":[\"phases\",\"pt_phase\",\"phases_connected\",\"control\",\"control_level\",\"capacitor_A\",\"dwell_time\",\"switchA\"]},{\"name\":\"cap_capbank0b\",\"properties\":[\"phases\",\"pt_phase\",\"phases_connected\",\"control\",\"control_level\",\"capacitor_B\",\"dwell_time\",\"switchB\"]},{\"name\":\"cap_capbank1b\",\"properties\":[\"phases\",\"pt_phase\",\"phases_connected\",\"control\",\"control_level\",\"capacitor_B\",\"dwell_time\",\"switchB\"]},{\"name\":\"cap_capbank2b\",\"properties\":[\"phases\",\"pt_phase\",\"phases_connected\",\"control\",\"control_level\",\"capacitor_B\",\"dwell_time\",\"switchB\"]},{\"name\":\"cap_capbank0c\",\"properties\":[\"phases\",\"pt_phase\",\"phases_connected\",\"control\",\"control_level\",\"capacitor_C\",\"dwell_time\",\"switchC\"]},{\"name\":\"cap_capbank1c\",\"properties\":[\"phases\",\"pt_phase\",\"phases_connected\",\"control\",\"control_level\",\"capacitor_C\",\"dwell_time\",\"switchC\"]},{\"name\":\"cap_capbank2c\",\"properties\":[\"phases\",\"pt_phase\",\"phases_connected\",\"control\",\"control_level\",\"capacitor_C\",\"dwell_time\",\"switchC\"]},{\"name\":\"cap_capbank3\",\"properties\":[\"phases\",\"pt_phase\",\"phases_connected\",\"control\",\"control_level\",\"capacitor_A\",\"capacitor_B\",\"capacitor_C\",\"dwell_time\",\"switchA\",\"switchB\",\"switchC\"]},{\"name\":\"xf_hvmv_sub\",\"properties\":[\"power_in_A\",\"power_in_B\",\"power_in_C\"]},{\"name\":\"nd_l2955047\",\"properties\":[\"voltage_A\",\"voltage_B\",\"voltage_C\"]},{\"name\":\"nd_l2673313\",\"properties\":[\"voltage_A\",\"voltage_B\",\"voltage_C\"]},{\"name\":\"nd_l3160107\",\"properties\":[\"voltage_A\",\"voltage_B\",\"voltage_C\"]},{\"name\":\"nd_l2876814\",\"properties\":[\"voltage_A\",\"voltage_B\",\"voltage_C\"]},{\"name\":\"nd_l3254238\",\"properties\":[\"voltage_A\",\"voltage_B\",\"voltage_C\"]},{\"name\":\"nd_m1047574\",\"properties\":[\"voltage_A\",\"voltage_B\",\"voltage_C\"]},{\"name\":\"nd__hvmv_sub_lsb\",\"properties\":[\"voltage_A\",\"voltage_B\",\"voltage_C\"]},{\"name\":\"nd_190-8593\",\"properties\":[\"voltage_A\",\"voltage_B\",\"voltage_C\"]},{\"name\":\"nd_190-8581\",\"properties\":[\"voltage_A\",\"voltage_B\",\"voltage_C\"]},{\"name\":\"nd_190-7361\",\"properties\":[\"voltage_A\",\"voltage_B\",\"voltage_C\"]}]}";
    public static final String SIMULATION_CONFIG_OUTPUT_SHORT = "{\"output_objects\":["
            + SIMULATION_CONFIG_OUTPUT_OBJECT_1 + "," + SIMULATION_CONFIG_OUTPUT_OBJECT_2 + "]}";
    public static final String SIMULATION_CONFIG = "{\"power_flow_solver_method\":\"NR\",\"duration\":120,\"simulation_id\":\"12345\",\"simulation_name\":\"ieee8500\",\"simulator\":\"GridLAB-D\",\"start_time\":\"2009-07-21 00:00:00\",\"run_realtime\":true,\"simulation_output\":"
            + SIMULATION_CONFIG_OUTPUT_SHORT +
            ",\"model_creation_config\":" + MODEL_CREATION_CONFIG
            + ",\"simulation_broker_port\":5570,\"simulation_broker_location\":\"127.0.0.1\"}";
    public static final String REQUEST_SIMULATION_CONFIG = "{\"power_system_config\":" + POWER_SYSTEM_CONFIG
            + ",\"simulation_config\":" + SIMULATION_CONFIG + ",\"application_config\":" + APPLICATION_CONFIG + "}";
    public static final String REQUEST_SIMULATION_CONFIG_ESC = "{\"power_system_config\":" + POWER_SYSTEM_CONFIG
            + ",\"simulation_config\":" + SIMULATION_CONFIG + ",\"application_config\":" + APPLICATION_CONFIG_ESC + "}";

    public static final String FNCS_BRIDGE_RESPONSE = "{\"timestamp\":0,\"command\":\"isInitialized\",\"response\":\"true\",\"output\":\"Any messages from simulator regarding initialization\"}";

    public static final String SYSTEM_USER_NAME = "system";

}
