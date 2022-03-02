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
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS â€œAS ISâ€� AND ANY 
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

import gov.pnnl.goss.gridappsd.dto.events.Event;

import java.io.Serializable;
import java.util.List;
//import java.util.UUID;
import java.util.Random;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class TestConfig implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private Random randomNum =  new Random();
	
	public enum TestType {
		simulation_vs_expected, simulation_vs_timeseries, expected_vs_timeseries, timeseries_vs_timeseries
	}

	private List<Event> events;
	
	private List<RuleSettings> rules;
	
	private JsonObject expectedResults;
	
	private Boolean testInput = true;

	private Boolean testOutput = true;
	
	private String compareWithSimId;
	
	private String compareWithSimIdTwo;
	
	private String appId;
	
	private String testId = ""+Math.abs(randomNum.nextInt());
	
	private TestType testType = TestType.simulation_vs_expected;
	
	private Boolean storeMatches = false;
	
	public JsonObject getExpectedResultObject() {
		return expectedResults;
	}

	public void setExpectedResultObject(JsonObject expectedResults) {
		this.expectedResults = expectedResults;
	}

	public List<Event> getEvents() {
		return events;
	}

	public void setEvents(List<Event> events) {
		this.events = events;
	}

	public List<RuleSettings> getRules() {
		return rules;
	}

	public void setRules(List<RuleSettings> rules) {
		this.rules = rules;
	}
	
	public Boolean getTestInput() {
		return testInput;
	}

	public void setTestInput(Boolean testInput) {
		this.testInput = testInput;
	}

	public Boolean getTestOutput() {
		return testOutput;
	}

	public void setTestOutput(Boolean testOutput) {
		this.testOutput = testOutput;
	}
	
	public String getCompareWithSimId() {
		return compareWithSimId;
	}

	public void setCompareWithSimId(String compareWithSimId) {
		this.compareWithSimId = compareWithSimId;
	}
	
	public String getCompareWithSimIdTwo() {
		return compareWithSimIdTwo;
	}

	public void setCompareWithSimIdTwo(String compareWithSimIdTwo) {
		this.compareWithSimIdTwo = compareWithSimIdTwo;
	}
	
	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}
	

	public String getTestId() {
		return testId;
	}

	public void setTestId(String testId) {
		this.testId = testId;
	}

	public TestType getTestType() {
		return testType;
	}

	public void setTestType(TestType testType) {
		this.testType = testType;
	}

	public Boolean getStoreMatches() {
		return storeMatches;
	}

	public void setStoreMatches(Boolean storeMatches) {
		this.storeMatches = storeMatches;
	}

	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}
	
	public static TestConfig parse(String jsonString){
		Gson  gson = new Gson();
		TestConfig obj = gson.fromJson(jsonString, TestConfig.class);
		System.out.println(jsonString);
//		if(obj.events==null || obj.events.size()==0 || obj.compareWithSimId ==null)
//			throw new RuntimeException("Expected attribute events not found or is empty");
		if(obj.appId==null)
			throw new RuntimeException("Expected attribute appId not found");
		return obj;
	}
}
