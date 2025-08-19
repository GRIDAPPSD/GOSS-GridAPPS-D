/*******************************************************************************
 * Copyright  2017, Battelle Memorial Institute All rights reserved.
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
package gov.pnnl.goss.gridappsd.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;


public class SimulatorConfig  implements Serializable {
	private static final long serialVersionUID = -2995486912804104569L;

	//eg, forward backward sweeper
	public String power_flow_solver_method;
	//the name of the simulator that will be used to run the simulation, e.g. GridLAB-D, OpenDSS, etc.
	public String simulator;
	
	//eg "simulation_output": [{"name":"objectname", "properties": ["prop1","prop2"]},{"name":"object2name","properties":["prop1","prop2"]}]
	public SimulationOutput simulation_output = new SimulationOutput();
	
	public ModelCreationConfig model_creation_config = new ModelCreationConfig(); 
	
	//temp working folder for simulation files
	public String simulation_work_dir;
	
	public String getPower_flow_solver_method() {
		return power_flow_solver_method;
	}
	public void setPower_flow_solver_method(String power_flow_solver_method) {
		this.power_flow_solver_method = power_flow_solver_method;
	}
	
	public String getSimulator() {
		return simulator;
	}
	public void setSimulator(String simulator) {
		this.simulator = simulator;
	}
	
	public SimulationOutput getSimulation_output() {
		return simulation_output;
	}
	public void setSimulation_output(SimulationOutput simulation_output) {
		this.simulation_output = simulation_output;
	}
	public ModelCreationConfig getModel_creation_config() {
		return model_creation_config;
	}
	public void setModel_creation_config(ModelCreationConfig model_creation_config) {
		this.model_creation_config = model_creation_config;
	}
	public String getSimulation_work_dir() {
		return simulation_work_dir;
	}
	public void setSimulation_work_dir(String simulation_work_dir) {
		this.simulation_work_dir = simulation_work_dir;
	}
	
	
	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}
		
	public static SimulatorConfig parse(String jsonString){
		Gson  gson = new Gson();
		SimulatorConfig obj = gson.fromJson(jsonString, SimulatorConfig.class);
		if(obj.simulator==null)
			throw new JsonSyntaxException("Expected attribute simulator not found");
		return obj;
	}
}
