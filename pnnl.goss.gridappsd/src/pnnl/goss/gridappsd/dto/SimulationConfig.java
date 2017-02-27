package pnnl.goss.gridappsd.dto;

import java.io.Serializable;

//TODO change to be a dto rather than full implementation of getters and setters.
public class SimulationConfig  implements Serializable {
	public static String power_flow_solver_method;

	public static String duration;

	public static String simulation_name;

	public static String simulator;

	public static String start_time;

	public static String[] output_object_mrid;

	public static String[] simulator_name;

//	@Override
//	public String toString() {
//		return "ClassPojo [power_flow_solver_method = "
//				+ power_flow_solver_method + ", duration = " + duration
//				+ ", simulation_name = " + simulation_name + ", simulator = "
//				+ simulator + ", start_time = " + start_time
//				+ ", output_object_mrid = " + output_object_mrid
//				+ ", simulator_name = " + simulator_name + "]";
//	}
}