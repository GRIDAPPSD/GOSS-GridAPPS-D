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
package gov.pnnl.goss.gridappsd.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class TestConfiguration implements Serializable {


	private static final long serialVersionUID = 1L;

	public String power_system_configuration;

	public String simulation_configuration;
	
	public Integer durations;

	public Date run_start;

	public Date run_end;

	public String region_name;

	public String subregion_name;

	public String line_name;

	public String getPower_system_configuration() {
		return power_system_configuration;
	}

	public void setPower_system_configuration(String power_system_configuration) {
		this.power_system_configuration = power_system_configuration;
	}

	public String getSimulation_configuration() {
		return simulation_configuration;
	}

	public void setSimulation_configuration(String simulation_configuration) {
		this.simulation_configuration = simulation_configuration;
	}

	public Integer getDurations() {
		return durations;
	}

	public void setDurations(Integer durations) {
		this.durations = durations;
	}

	public Date getRun_start() {
		return run_start;
	}

	public void setRun_start(Date run_start) {
		this.run_start = run_start;
	}

	public Date getRun_end() {
		return run_end;
	}

	public void setRun_end(Date run_end) {
		this.run_end = run_end;
	}

	public String getRegion_name() {
		return region_name;
	}

	public void setRegion_name(String region_name) {
		this.region_name = region_name;
	}

	public String getSubregion_name() {
		return subregion_name;
	}

	public void setSubregion_name(String subregion_name) {
		this.subregion_name = subregion_name;
	}

	public String getLine_name() {
		return line_name;
	}

	public void setLine_name(String line_name) {
		this.line_name = line_name;
	}

	public Boolean getLogging() {
		return logging;
	}

	public void setLogging(Boolean logging) {
		this.logging = logging;
	}

	public Map<String, String> getLogging_options() {
		return logging_options;
	}

	public void setLogging_options(Map<String, String> logging_options) {
		this.logging_options = logging_options;
	}

	public Map<String, String> getInitial_conditions() {
		return initial_conditions;
	}

	public void setInitial_conditions(Map<String, String> initial_conditions) {
		this.initial_conditions = initial_conditions;
	}

	public Map<String, String> getDefault_values() {
		return default_values;
	}

	public void setDefault_values(Map<String, String> default_values) {
		this.default_values = default_values;
	}

	public String[] getOutputs() {
		return outputs;
	}

	public void setOutputs(String[] outputs) {
		this.outputs = outputs;
	}

	public Boolean logging;
	
	public Map<String,String> logging_options;
	
	public Map<String,String> initial_conditions;
	
	public Map<String,String> default_values;
	
	public String[] outputs;

	public TestConfiguration() {

	}
	
	public String getPowerSystemConfiguration(){
		return power_system_configuration;		
	}

	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}
	
	public static TestConfiguration parse(String jsonString){
	    Gson  gson = new Gson();
	    TestConfiguration obj = gson.fromJson(jsonString, TestConfiguration.class);
	    if(obj.power_system_configuration==null)
	        throw new JsonSyntaxException("Expected attribute power_system_configuration not found");
	    return obj;
	}

}
