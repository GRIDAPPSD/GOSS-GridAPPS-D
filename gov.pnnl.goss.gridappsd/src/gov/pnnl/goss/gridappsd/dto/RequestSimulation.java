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

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class RequestSimulation implements Serializable {
	
	private static final long serialVersionUID = 1L;

	public enum SimulationRequestType {
		   NEW, PAUSE, RESUME, STOP
		}
	
	public PowerSystemConfig power_system_config;

	public SimulationConfig simulation_config;
	
	public ApplicationConfig application_config;
	
	public SimulationRequestType simulation_request_type = SimulationRequestType.NEW;
	
	public String simulation_id;  //used for pause/resume/stop requests
	
	public RequestSimulation(){
		
	}
	
	public RequestSimulation(PowerSystemConfig power_system_config, SimulationConfig simulation_config){
		this.power_system_config = power_system_config;
		this.simulation_config = simulation_config;
	}

	public PowerSystemConfig getPower_system_config() {
		return power_system_config;
	}

	public void setPower_system_config(PowerSystemConfig power_system_config) {
		this.power_system_config = power_system_config;
	}

	public SimulationConfig getSimulation_config() {
		return simulation_config;
	}

	public void setSimulation_config(SimulationConfig simulation_config) {
		this.simulation_config = simulation_config;
	}

	public ApplicationConfig getApplication_config() {
		if(application_config==null) return new ApplicationConfig();
		return application_config;
	}

	public void setApplication_config(ApplicationConfig application_config) {
		this.application_config = application_config;
	}

	public SimulationRequestType getSimulation_request_type() {
		return simulation_request_type;
	}

	public void setSimulation_request_type(SimulationRequestType simulation_request_type) {
		this.simulation_request_type = simulation_request_type;
	}

	
	public String getSimulation_id() {
		return simulation_id;
	}

	public void setSimulation_id(String simulation_id) {
		this.simulation_id = simulation_id;
	}

	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}
	
	
	public static RequestSimulation parse(String jsonString){
		Gson  gson = new Gson();
		RequestSimulation obj = gson.fromJson(jsonString, RequestSimulation.class);
		if(obj.power_system_config==null)
			throw new JsonSyntaxException("Expected attribute power_system_config not found");
		return obj;
	}
}
