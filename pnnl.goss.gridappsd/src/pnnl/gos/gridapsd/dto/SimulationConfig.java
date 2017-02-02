package pnnl.gos.gridapsd.dto;

//TODO change to be a dto rather than full implementation of getters and setters.
public class SimulationConfig {
	private String power_flow_solver_method;

	private String duration;

	private String simulation_name;

	private String simulator;

	private String start_time;

	private String[] output_object_mrid;

	private String[] simulator_name;

	public String getPower_flow_solver_method() {
		return power_flow_solver_method;
	}

	public void setPower_flow_solver_method(String power_flow_solver_method) {
		this.power_flow_solver_method = power_flow_solver_method;
	}

	public String getDuration() {
		return duration;
	}

	public void setDuration(String duration) {
		this.duration = duration;
	}

	public String getSimulation_name() {
		return simulation_name;
	}

	public void setSimulation_name(String simulation_name) {
		this.simulation_name = simulation_name;
	}

	public String getSimulator() {
		return simulator;
	}

	public void setSimulator(String simulator) {
		this.simulator = simulator;
	}

	public String getStart_time() {
		return start_time;
	}

	public void setStart_time(String start_time) {
		this.start_time = start_time;
	}

	public String[] getOutput_object_mrid() {
		return output_object_mrid;
	}

	public void setOutput_object_mrid(String[] output_object_mrid) {
		this.output_object_mrid = output_object_mrid;
	}

	public String[] getSimulator_name() {
		return simulator_name;
	}

	public void setSimulator_name(String[] simulator_name) {
		this.simulator_name = simulator_name;
	}

	@Override
	public String toString() {
		return "ClassPojo [power_flow_solver_method = "
				+ power_flow_solver_method + ", duration = " + duration
				+ ", simulation_name = " + simulation_name + ", simulator = "
				+ simulator + ", start_time = " + start_time
				+ ", output_object_mrid = " + output_object_mrid
				+ ", simulator_name = " + simulator_name + "]";
	}
}