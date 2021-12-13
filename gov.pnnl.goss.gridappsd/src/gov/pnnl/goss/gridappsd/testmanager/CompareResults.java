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
package gov.pnnl.goss.gridappsd.testmanager;
	
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import gov.pnnl.goss.gridappsd.dto.SimulationOutput;
import gov.pnnl.goss.gridappsd.dto.SimulationOutputObject;
import gov.pnnl.goss.gridappsd.dto.TestConfig;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.Client;
	
	/**
	*
	* @author jsimpson
	*
	*/
	public class CompareResults {
				
		private Client client;
		
		private String testId; 
		
		private String testOutputTopic = GridAppsDConstants.topic_simulationTestOutput;
		
		private final static double EPSILON_e3 = 0.001001;
		
		private final static double EPSILON_e1 = 0.100001;
		
		private Set<String> propSet = new HashSet<String>();

		private TestConfig testConfig;
		
		protected boolean debug = false;
		
		protected String[] propsArray = new String[]{"connect_type", "Control", "control_level", "PT_phase", "band_center", "band_width", "dwell_time", "raise_taps", "lower_taps", "regulation"};
		
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
		
//		public CompareResults(Client client){
//			this.client = client;
//			propSet.add("value");
//			propSet.add("angle");
//			propSet.add("magnitude");
//		}
		
		public CompareResults(Client client, TestConfig testConfig){
			this.client = client;
			this.testConfig = testConfig;
			this.testId = testConfig.getTestId();
			propSet.add("value");
			propSet.add("angle");
			propSet.add("magnitude");
		}
				
		public static boolean equalsE3(double a, double b){
		    return a == b ? true : Math.abs(a - b) <= EPSILON_e3;
		}
		
		public static boolean equalsE1(double a, double b){
		    return a == b ? true : Math.abs(a - b) < EPSILON_e1;
		}
		
		public static boolean equals(Complex a, Complex b){
		    return a.equals(b) ? true : (a.subtract(b)).abs() <= EPSILON_e3;
		}
		
		public void compare(){
			JsonParser parser = new JsonParser();
			JsonElement o1 = parser.parse("{a : {a : 2}, b : 2}");
			JsonElement o2 = parser.parse("{b : 3, a : {a : 2}}");
			JsonElement o3 = parser.parse("{b : 2, a : {a : 2}}");
			System.out.println(o1.equals(o2));
			System.out.println(o1.equals(o3));
	//		Assert.assertEquals(o1, o2);
		}

		public void publishTestResults(String id, TestResults testResults, Boolean storeMatches) {
			String tr  = testResults.toJson(storeMatches);
//			System.out.println(storeMatches);
//			System.out.println(tr);
			if (! tr.isEmpty()){
				if(debug){System.out.println(tr);}
	//			System.out.println(testOutputTopic+id);
				client.publish(testOutputTopic+id,tr);
			}
		}
		
		public void publish(String timestamp, String key, String prop, String expectedOutputObjstring, String simOutputObjstring2, boolean match) {
			TestResults temp = new TestResults();
			temp.add(key, prop, expectedOutputObjstring, simOutputObjstring2, match);
			temp.setIndexOne(Long.parseLong(timestamp));
			temp.setIndexTwo(Long.parseLong(timestamp));
			publishTestResults(testId, temp, testConfig.getStoreMatches());
		}
		
		public void publish(String timestamp, String obj, String prop, String expected, String actual, String diff_mrid, String diff_type, Boolean match) {
			TestResults temp = new TestResults();
			temp.add( obj, prop, expected, actual, diff_mrid, diff_type, match);
			temp.setIndexOne(Long.parseLong(timestamp));
			temp.setIndexTwo(Long.parseLong(timestamp));
			publishTestResults(testId, temp, testConfig.getStoreMatches());	
		}	
		
	//	/**
	//	 * Look up the expected with a timestamp
	//	 * @param timestamp
	//	 * @param jsonObject
	//	 * @param expectedOutputPath
	//	 * @param simOutProperties
	//	 * @return
	//	 */
	//	public TestResults compareExpectedWithSimulationOutput(String timestamp, JsonObject jsonObject, String expectedOutputPath) {
	//		Map<String, JsonElement> expectedOutputMap = getExpectedOutputMap(timestamp, expectedOutputPath);
	//		if (expectedOutputMap == null) return null;
	////		Map<String, List<String>> propMap = simOutProperties.getOutputObjects().stream()
	////				.collect(Collectors.toMap(SimulationOutputObject::getName, e -> e.getProperties()));
	//	
	//		return compareExpectedWithSimulationOutput(expectedOutputMap, jsonObject);
	//	}
		
		/**
		 * Look up the expected with a timestamp
		 * @param timestamp
		 * @param jsonObject
		 * @param expectedOutputPath
		 * @param simOutProperties
		 * @return
		 */
		public TestResults compareExpectedWithSimulationOutput(String timestamp, JsonObject jsonObject, JsonObject expectedOutput) {
			Map<String, JsonElement> expectedOutputMap = getExpectedOutputMap(timestamp, expectedOutput);
			if (expectedOutputMap == null) return new TestResults();
	//		Map<String, List<String>> propMap = simOutProperties.getOutputObjects().stream()
	//				.collect(Collectors.toMap(SimulationOutputObject::getName, e -> e.getProperties()));
		
			return compareExpectedWithSimulationOutput(timestamp, expectedOutputMap, jsonObject);
		}
		
		/**
		 * Compare expected with simulation output
		 * @param timestamp1 actual time stamp
		 * @param timestamp2 expected time stamp
		 * @param simultaionOutput
		 * @param expectedOutput
		 * @param start_time
		 * @param end_time
		 * @return
		 */
		public TestResults compareExpectedWithSimulationOutput(String timestamp1, String timestamp2, JsonObject simultaionOutput, JsonObject expectedOutput, long start_time, long end_time) {
			Map<String, JsonElement> expectedOutputMap = getExpectedOutputMap(timestamp2, expectedOutput);
			if(debug){
				System.out.println("expectedOutput");
				if(expectedOutput!=null){
					System.out.println(StringUtils.left(expectedOutput.toString(),300));
				}else{
					System.out.println("No expectedOutput");
				}
				System.out.println("simultaionOutput");
				System.out.println(StringUtils.left(simultaionOutput.toString(),300));
			}
			Map<String, JsonElement> outputMap = getExpectedOutputMap(timestamp1, simultaionOutput);
			if(debug){
				System.out.println("simultaionInput outputMap");
				System.out.println("simultaionInput expectedOutputMap is null " + Boolean.valueOf(expectedOutputMap==null).toString());
				System.out.println("simultaionInput outputMap is null " + Boolean.valueOf(outputMap==null).toString());
				if(expectedOutputMap!=null){
					System.out.println("expectedOutputMap");
					System.out.println(StringUtils.left(gson.toJson(expectedOutputMap),600));
				}else{
					System.out.println("No expectedOutputMap");
				}
				if(outputMap!=null){
					System.out.println("outputMap");
					System.out.println(StringUtils.left(gson.toJson(outputMap),600));
				}else{
					System.out.println("No outputMap");
				}
			}
			
//			System.out.println(forwardMap.toString());
			TestResults testResults = new TestResults();
			if(testConfig.getTestWithAllSimulationOutput()){  // This will generate a lot of data if true
				checkIfExpectedOutputIsNull(timestamp1, start_time, end_time, expectedOutputMap, outputMap,
						testResults);
			}
			checkIfSimulationOutputIsNull(timestamp1, start_time, end_time, expectedOutputMap, outputMap, testResults);

				
			if (expectedOutputMap == null || outputMap == null){
				return testResults;
			}
			compareExpectedAndSim(timestamp1, expectedOutputMap, testResults, outputMap);
			
			return testResults;
		}

		/**
		 * checkIfSimulationOutputIsNull and create a result message with NA for the expected value
		 * @param timestamp1
		 * @param start_time
		 * @param end_time
		 * @param expectedOutputMap
		 * @param outputMap
		 * @param testResults
		 */
		private void checkIfSimulationOutputIsNull(String timestamp1, long start_time, long end_time,
				Map<String, JsonElement> expectedOutputMap, Map<String, JsonElement> outputMap,
				TestResults testResults) {
			if (outputMap == null){			
				for (Entry<String, JsonElement> entry : expectedOutputMap.entrySet()) {
					long time = Long.parseLong(timestamp1);
					if(time>=start_time && time < end_time){
						if (entry.getValue().isJsonObject()) {
							System.out.println("Case no forwardMap");
							JsonObject expectedOutputObj = expectedOutputMap.get(entry.getKey()).getAsJsonObject();
							for(Entry<String, JsonElement> simentry : expectedOutputObj.entrySet()){
								String prop1 = simentry.getKey();
								if( ! propSet.contains(prop1))
									continue;
								publish(timestamp1, entry.getKey(), prop1, simentry.getValue().getAsString(), "NA", false);
								testResults.add(entry.getKey(), prop1, simentry.getValue().getAsString(), "NA", false);
							}
						}
					}
				}
			}
		}

		/***
		 * checkIfExpectedOutputIsNull and create a result message with NA for the actual value
		 * @param timestamp1
		 * @param start_time
		 * @param end_time
		 * @param expectedOutputMap
		 * @param outputMap
		 * @param testResults
		 */
		private void checkIfExpectedOutputIsNull(String timestamp1, long start_time, long end_time,
				Map<String, JsonElement> expectedOutputMap, Map<String, JsonElement> outputMap,
				TestResults testResults) {
			if (expectedOutputMap == null){
				for (Entry<String, JsonElement> entry : outputMap.entrySet()) {
					long time = Long.parseLong(timestamp1);
					if(time>=start_time && time < end_time){
//					System.out.println(entry);
						if (entry.getValue().isJsonObject()) {
							System.out.println("Case no expectedOutputMap");
							JsonObject expectedOutputObj = outputMap.get(entry.getKey()).getAsJsonObject();
							for(Entry<String, JsonElement> simentry : expectedOutputObj.entrySet()){
								String prop1 = simentry.getKey();
								if( ! propSet.contains(prop1))
									continue;
								publish(timestamp1, entry.getKey(), prop1, "NA", simentry.getValue().getAsString(), false);
								testResults.add(entry.getKey(), prop1, "NA",simentry.getValue().getAsString(), false);
							}
						}
					}
				}
			}
		}
		 
		/**
		 * Compare expect with simulation input
		 * Only 
		 * @param timestamp1 actual time stamp
		 * @param timestamp2 expected time stamp
		 * @param simultaionInput
		 * @param expectedInput
		 * @param start_time
		 * @param end_time
		 * @return
		 */
		public TestResults compareExpectedWithSimulationInput(String timestamp1, String timestamp2, JsonObject simultaionInput, JsonObject expectedInput, long start_time, long end_time) {
			Map<String, JsonElement> expectedForwardMap = getExpectedForwardInputMap(timestamp2, expectedInput);
			Map<String, JsonElement> expectedReverseMap = getExpectedReverseInputMap(timestamp2, expectedInput);
			if(debug){
				System.out.println("expectedInput");
				if(expectedInput!=null){
					System.out.println(StringUtils.left(expectedInput.toString(),300));
				}else{
					System.out.println("No expectedInput");
				}
				System.out.println("simultaionInput");
				System.out.println(StringUtils.left(simultaionInput.toString(),300));
			}

			Map<String, JsonElement> forwardMap = getExpectedForwardInputMap(timestamp1, simultaionInput);
			Map<String, JsonElement> reverseMap = getExpectedReverseInputMap(timestamp1, simultaionInput);
			if(debug){
				System.out.println("simultaionInput forwardMap");
				System.out.println("simultaionInput expectedForwardMap is null " + Boolean.valueOf(expectedForwardMap==null).toString());
				System.out.println("simultaionInput forwardMap is null " + Boolean.valueOf(forwardMap==null).toString());
				if(expectedForwardMap!=null){
					System.out.println("expectedForwardMap");
					System.out.println(StringUtils.left(gson.toJson(expectedForwardMap),600));
				}else{
					System.out.println("No expectedForwardMap");
				}
				if(forwardMap!=null){
					System.out.println("forwardMap");
					System.out.println(StringUtils.left(gson.toJson(forwardMap),600));
				}else{
					System.out.println("No forwardMap");
				}
			}
			
//			System.out.println(forwardMap.toString());
			TestResults testResults = new TestResults();
			checkIfExpectedForwardInputIsNull(timestamp1, start_time, end_time, expectedForwardMap, forwardMap,
					testResults);
			checkIfExpectedReverseInputIsNull(timestamp1, start_time, end_time, expectedReverseMap, reverseMap,
					testResults);
			checkIfSimulationForwardInputIsNull(timestamp1, start_time, end_time, expectedForwardMap, forwardMap,
					testResults);
			checkIfSimulationReverseInputIsNull(timestamp1, start_time, end_time, expectedReverseMap, reverseMap,
					testResults);
				
			if (expectedForwardMap == null || forwardMap == null){
				return testResults;
			}
			
//			if (expectedForwardMap == null) return new TestResults();
//			Map<String, JsonElement> forwardMap = getExpectedForwardInputMap(timestamp1, simultaionInput);
//			Map<String, JsonElement> reverseMap = getExpectedReverseInputMap(timestamp1, simultaionInput);
			
	//		Map<String, List<String>> propMap = simOutProperties.getOutputObjects().stream()
	//				.collect(Collectors.toMap(SimulationOutputObject::getName, e -> e.getProperties()));
//			Map<String, JsonElement> forwardMap= getForwardDifferenceMap(jsonObject);
//			Map<String, JsonElement> reverseMap= getReverseDifferenceMap(jsonObject);
//			TestResults testResults = new TestResults();
	//		JsonObject output = jsonObject;
	//		String firstKey = getFirstKey(output);
			
//			Map<String, JsonElement> simOutputMap= getMeasurmentsMap(jsonObject);
			compareExpectedAndSim(timestamp1, expectedForwardMap, testResults, forwardMap);
			compareExpectedAndSim(timestamp1, expectedReverseMap, testResults, reverseMap);
			
			return testResults;
//			return compareExpectedWithSimulationOutput(expectedOutputMap, jsonObject);
		}

		protected void checkIfSimulationReverseInputIsNull(String timestamp1, long start_time, long end_time,
				Map<String, JsonElement> expectedReverseMap, Map<String, JsonElement> reverseMap,
				TestResults testResults) {
			if (reverseMap == null){			
				for (Entry<String, JsonElement> entry : expectedReverseMap.entrySet()) {
					long time = Long.parseLong(timestamp1);
					if(time>=start_time && time < end_time){
	//					System.out.println(entry);
						if (entry.getValue().isJsonObject()) {
							if(debug){System.out.println("Case no reverseMap");}
							JsonObject expectedOutputObj = expectedReverseMap.get(entry.getKey()).getAsJsonObject();
							String objectMRID = expectedOutputObj.get("object").getAsString();
							String attr = expectedOutputObj.get("attribute").getAsString();
							String value = expectedOutputObj.get("value").toString();
//							System.out.println("no index for "+ timestamp2);
							testResults.add(entry.getKey(), attr, value, "NA", expectedOutputObj.get("difference_mrid").getAsString(), "REVERSE", false);
							publish(timestamp1, objectMRID, attr, value, "NA", expectedOutputObj.get("difference_mrid").getAsString(), "REVERSE", false);
						}
					}
				}
			}
		}

		protected void checkIfSimulationForwardInputIsNull(String timestamp1, long start_time, long end_time,
				Map<String, JsonElement> expectedForwardMap, Map<String, JsonElement> forwardMap,
				TestResults testResults) {
			if (forwardMap == null){			
				for (Entry<String, JsonElement> entry : expectedForwardMap.entrySet()) {
					long time = Long.parseLong(timestamp1);
					if(time>=start_time && time < end_time){
	//					System.out.println(entry);
						if (entry.getValue().isJsonObject()) {
							if(debug){System.out.println("Case no forwardMap");}
							JsonObject expectedOutputObj = expectedForwardMap.get(entry.getKey()).getAsJsonObject();
							String objectMRID = expectedOutputObj.get("object").getAsString();
							String attr = expectedOutputObj.get("attribute").getAsString();
							String value = expectedOutputObj.get("value").toString();
//							System.out.println("no index for "+ timestamp2);
							testResults.add(entry.getKey(), attr, value, "NA", expectedOutputObj.get("difference_mrid").getAsString(), "FORWARD", false);
							publish(timestamp1, objectMRID, attr, value, "NA", expectedOutputObj.get("difference_mrid").getAsString(), "FORWARD", false);
						}
					}
				}
			}
		}

		protected void checkIfExpectedReverseInputIsNull(String timestamp1, long start_time, long end_time,
				Map<String, JsonElement> expectedReverseMap, Map<String, JsonElement> reverseMap,
				TestResults testResults) {
			if (expectedReverseMap == null){
				for (Entry<String, JsonElement> entry : reverseMap.entrySet()) {
					long time = Long.parseLong(timestamp1);
					if(time>=start_time && time < end_time){
	//					System.out.println(entry);
						if (entry.getValue().isJsonObject()) {
							if(debug){System.out.println("Case no expectedReverseMap");}
							JsonObject expectedOutputObj = reverseMap.get(entry.getKey()).getAsJsonObject();
							String objectMRID = expectedOutputObj.get("object").getAsString();
							String attr = expectedOutputObj.get("attribute").getAsString();
							String value = expectedOutputObj.get("value").toString();
//							System.out.println("no index for "+ timestamp2);
							testResults.add(entry.getKey(), attr, "NA", value, expectedOutputObj.get("difference_mrid").getAsString(), "REVERSE", false);
							publish(timestamp1, objectMRID, attr, "NA", value, expectedOutputObj.get("difference_mrid").getAsString(), "REVERSE", false);
						}
					}
				}
			}
		}

		protected void checkIfExpectedForwardInputIsNull(String timestamp1, long start_time, long end_time,
				Map<String, JsonElement> expectedForwardMap, Map<String, JsonElement> forwardMap,
				TestResults testResults) {
			if (expectedForwardMap == null){
				for (Entry<String, JsonElement> entry : forwardMap.entrySet()) {
					long time = Long.parseLong(timestamp1);
					if(time>=start_time && time < end_time){
	//					System.out.println(entry);
						if (entry.getValue().isJsonObject()) {
							System.out.println("Case no expectedForwardMap");
							JsonObject expectedOutputObj = forwardMap.get(entry.getKey()).getAsJsonObject();
							String objectMRID = expectedOutputObj.get("object").getAsString();
							String attr = expectedOutputObj.get("attribute").getAsString();
							String value = expectedOutputObj.get("value").toString();
//							System.out.println("no index for "+ timestamp2);
							testResults.add(entry.getKey(), attr, "NA", value, expectedOutputObj.get("difference_mrid").getAsString(), "FORWARD", false);
							publish(timestamp1, objectMRID, attr, "NA", value, expectedOutputObj.get("difference_mrid").getAsString(), "FORWARD", false);
						}
					}
				}
			}
		}
		
	//	/**
	//	 * compareExpectedWithSimulation
	//	 * @param simOutputPath
	//	 * @param expectedOutputPath
	//	 * @param simOutProperties
	//	 */
	//	public TestResults compareExpectedWithSimulationOutput(JsonObject jsonObject, String expectedOutputPath) {
	//		Map<String, JsonElement> expectedOutputMap = getExpectedOutputMap(expectedOutputPath);
	//	
	//		return compareExpectedWithSimulationOutput(expectedOutputMap,  jsonObject);
	//	}
	
//		public TestResults compareExpectedWithSimulation(Map<String, JsonElement> expectedOutputMap, JsonObject jsonObject) {
//			
//			TestResults testResults = new TestResults();
//			JsonObject output = jsonObject.get("output").getAsJsonObject();
//			JsonObject simOutput = output.get(getFeeder()).getAsJsonObject();
//			compareExpectedWithSimulation(expectedOutputMap,  testResults, simOutput);
//			return testResults;
//		}
		
//		public String getFeeder() {
//	//		TestManagerQueryFactory qf = new TestManagerQueryFactory();
//	//		return qf.getFeeder();
//			return "ieee8500";
//		}
//		
//		/**
//		 * Get the set of properties that matches the expected results
//		 * @param testScript
//		 * @param simOutProperties
//		 * @return set
//		 */
//		public HashSet<String> getMatchedProperties(TestConfig testScript, SimulationOutput simOutProperties) {
//			List<SimulationOutputObject> simOutputObjects = simOutProperties.getOutputObjects();
//			Set<Entry<String, List<String>>> es = null;//testScript.getOutputs().entrySet();
//			
//			HashSet<String> simOONames = new HashSet<String>();
//			for (SimulationOutputObject simulationOutputObject : simOutputObjects) {
//				simOONames.add(simulationOutputObject.getName());
//			}
//			
//			HashSet<String> expectedOutputNames = new HashSet<String>();
//	
//			for (Entry<String, List<String>> entry : es) {
//				List<String> listOfValues = entry.getValue();
//				for (String value : listOfValues) {
//					expectedOutputNames.add(value);
//				}
//			}
//			
//	//		System.out.println(Sets.intersection(listenOutputNames, sooNames).size() +" " +Sets.intersection(listenOutputNames, sooNames));
//			// Get intersection
//			expectedOutputNames.retainAll(simOONames);
//			System.out.println(expectedOutputNames.size() + " "  + expectedOutputNames);
//			return expectedOutputNames;
//		}
//	
		public TestResults compareExpectedWithSimulationOutput(String timestamp,Map<String, JsonElement> expectedOutputMap,
				 JsonObject jsonObject) {
	
			TestResults testResults = new TestResults();
	//		JsonObject output = jsonObject;
	//		String firstKey = getFirstKey(output);
			
			Map<String, JsonElement> simOutputMap= getMeasurmentsMap(jsonObject);
			compareExpectedAndSim(timestamp, expectedOutputMap, testResults, simOutputMap);
			return testResults;
	
	//		JsonObject simOutput = null;
	//
	////		if (output.get(firstKey).isJsonObject()){
	//		if ( ! firstKey.equals("output") ){
	//			simOutput = output.get(firstKey).getAsJsonObject();
	//		} else { 
	//			// CIM: new sim output 
	//			simOutput = output.get(firstKey).getAsJsonObject();
	//			Map<String, JsonElement> simOutputMap= getMeasurmentsMap(simOutput);
	//			compareExpectedAndSim(expectedOutputMap, testResults, simOutputMap);
	//			return testResults;
	//		}	
	//		compareExpectedWithSimulation(expectedOutputMap, testResults, simOutput);
	//		return testResults;
		}
		
		/**
		 * 
		 * @param timestamp
		 * @param expectedOutputMap
		 * @param testResults
		 * @param simOutputMap
		 */
		public void compareExpectedAndSim(String timestamp, Map<String, JsonElement> expectedOutputMap, TestResults testResults,
				Map<String, JsonElement> simOutputMap) {
			// Get all keys
			Set<String> allObjectKeys = new HashSet<String>();
			allObjectKeys.addAll(expectedOutputMap.keySet());
			allObjectKeys.addAll(simOutputMap.keySet());
			for (Iterator<String> iterator = allObjectKeys.iterator(); iterator.hasNext();) {
				String key = (String) iterator.next();
//				System.out.println(key);
				compareMaps(timestamp, expectedOutputMap, testResults, simOutputMap, key);
			}
//			System.out.println("Number of equals : " + countTrue + " Number of not equals : " + countFalse);
		}

		/**
		 * 
		 * @param timestamp
		 * @param expectedOutputMap
		 * @param testResults
		 * @param simOutputMap
		 * @param key
		 */
		private void compareMaps(String timestamp, Map<String, JsonElement> expectedOutputMap, TestResults testResults,
				Map<String, JsonElement> simOutputMap, String key) {
			if (expectedOutputMap.containsKey(key) && simOutputMap.containsKey(key)) {
				JsonObject expectedOutputObj = expectedOutputMap.get(key).getAsJsonObject();
//				if ( simOutputMap.containsKey(key) ){
				JsonObject simOutputObj = simOutputMap.get(key).getAsJsonObject(); 
				// Check each property of the object
				for(Entry<String, JsonElement> simentry : simOutputMap.get(key).getAsJsonObject().entrySet()){
					String prop = simentry.getKey();
//						System.out.println("\nTesting "+entry.getKey() +":"+prop);
//						System.out.println(simOutputObj.get(prop) +  "== "+  expectedOutputObj.get(prop));
					if( ! propSet.contains(prop))
						continue;
//					String propOrAttr = prop;
//					if(simOutputObj.has("attribute")){
//						propOrAttr = simOutputObj.get("attribute").getAsString();
//					}
					Boolean comparisonProperty = compareObjectProperties(simOutputObj, expectedOutputObj, prop);
					// There is a match for that object and property
					if (comparisonProperty){
						if (simOutputObj.has("hasMeasurementDifference")){
							testResults.add(simOutputObj.get("object").getAsString(),
									simOutputObj.get("attribute").getAsString(), 
									expectedOutputObj.get(prop).toString(),
									simOutputObj.get(prop).toString(),
									simOutputObj.get("difference_mrid").getAsString(),
									simOutputObj.get("hasMeasurementDifference").getAsString(),
									true);
							publish(timestamp, simOutputObj.get("object").getAsString(),
									simOutputObj.get("attribute").getAsString(), 
									expectedOutputObj.get(prop).toString(),
									simOutputObj.get(prop).toString(),
									simOutputObj.get("difference_mrid").getAsString(),
									simOutputObj.get("hasMeasurementDifference").getAsString(),
									true);
						}else{
							testResults.add(key, prop, expectedOutputObj.get(prop).toString(), simOutputObj.get(prop).toString(), true);
							publish(timestamp, key, prop, expectedOutputObj.get(prop).toString(), simOutputObj.get(prop).toString(), true);
						}
					//	There is NOT a match for that object property
					} else {
//							System.out.println("\nFor "+entry.getKey() +":"+prop);
//							System.out.println("    EXPECTED: "+ simOutputObj.get(prop) );
//							System.out.println("    GOT:      "+ expectedOutputObj.get(prop) );
//								"hasMeasurementDifference":"FORWARD","difference_mrid":"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4","simulation_id":"1961648576","time":1587670650
						if (simOutputObj.has("hasMeasurementDifference")){
							testResults.add(simOutputObj.get("object").getAsString(),
									simOutputObj.get("attribute").getAsString(), 
									expectedOutputObj.get(prop).toString(),
									simOutputObj.get(prop).toString(),
									simOutputObj.get("difference_mrid").getAsString(),
									simOutputObj.get("hasMeasurementDifference").getAsString(),
									false);
							publish(timestamp, simOutputObj.get("object").getAsString(),
									simOutputObj.get("attribute").getAsString(), 
									expectedOutputObj.get(prop).toString(),
									simOutputObj.get(prop).toString(),
									simOutputObj.get("difference_mrid").getAsString(),
									simOutputObj.get("hasMeasurementDifference").getAsString(),
									false);
						}else{
							testResults.add(key, prop, expectedOutputObj.get(prop).toString(), simOutputObj.get(prop).toString(), false);
							publish(timestamp, key, prop, expectedOutputObj.get(prop).toString(), simOutputObj.get(prop).toString(), false);
						}
					}
				}
			}
			if(! simOutputMap.containsKey(key)){
				JsonObject expectedOutputObj = expectedOutputMap.get(key).getAsJsonObject();
				if(debug){
					System.out.println("No property for " + key +" "+ expectedOutputObj);
				}
				String prop = "value";
				if(expectedOutputObj.has("hasMeasurementDifference") ){
					if(debug){System.out.println("If compareExpectedAndSim no hasMeasurementDifference");}
					testResults.add(expectedOutputObj.get("object").getAsString(),
							expectedOutputObj.get("attribute").getAsString(), 
							expectedOutputObj.get(prop).toString(),
							"NA",
							expectedOutputObj.get("difference_mrid").getAsString(),
							expectedOutputObj.get("hasMeasurementDifference").getAsString(),
							false);
					publish(timestamp, expectedOutputObj.get("object").getAsString(),
							expectedOutputObj.get("attribute").getAsString(), 
							expectedOutputObj.get(prop).toString(),
							"NA",
							expectedOutputObj.get("difference_mrid").getAsString(),
							expectedOutputObj.get("hasMeasurementDifference").getAsString(),
							false);
				}else{
					if(debug){System.out.println("Else compareExpectedAndSim no hasMeasurementDifference");}
					for(Entry<String, JsonElement> simentry : expectedOutputObj.entrySet()){
						String prop1 = simentry.getKey();
						if( ! propSet.contains(prop1))
							continue;
						publish(timestamp, key, prop1, simentry.getValue().getAsString(), "NA" , false);
						testResults.add(key, prop1, simentry.getValue().getAsString(), "NA", false);
					}
				}					
			}
			if(! expectedOutputMap.containsKey(key)){
				JsonObject expectedOutputObj = simOutputMap.get(key).getAsJsonObject();
				if(debug){
//					System.out.println("No property for "+ key);
					System.out.println("No property for " + key +" "+ expectedOutputObj);
				}
				String prop = "value";
				if(expectedOutputObj.has("hasMeasurementDifference") ){
					if(debug){System.out.println("If compareExpectedAndSim no hasMeasurementDifference");}
					testResults.add(expectedOutputObj.get("object").getAsString(),
							expectedOutputObj.get("attribute").getAsString(), 
							"NA",
							expectedOutputObj.get(prop).toString(),
							expectedOutputObj.get("difference_mrid").getAsString(),
							expectedOutputObj.get("hasMeasurementDifference").getAsString(),
							false);
					publish(timestamp, expectedOutputObj.get("object").getAsString(),
							expectedOutputObj.get("attribute").getAsString(),
							"NA",
							expectedOutputObj.get(prop).toString(),
							expectedOutputObj.get("difference_mrid").getAsString(),
							expectedOutputObj.get("hasMeasurementDifference").getAsString(),
							false);
				}else{
					if(testConfig.getTestWithAllSimulationOutput()){
						if(debug){System.out.println("Else compareExpectedAndSim no hasMeasurementDifference");}
						for(Entry<String, JsonElement> simentry : expectedOutputObj.entrySet()){
							String prop1 = simentry.getKey();
							if( ! propSet.contains(prop1))
								continue;
							publish(timestamp, key, prop1, "NA",simentry.getValue().getAsString(), false);
							testResults.add(key, prop1, "NA",simentry.getValue().getAsString(), false);
						}
					}
				}					
			}
		}
		
		@Deprecated
		public void compareExpectedAndSimOld(String timestamp, Map<String, JsonElement> expectedOutputMap, TestResults testResults,
				Map<String, JsonElement> simOutputMap) {
			for (Entry<String, JsonElement> entry : expectedOutputMap.entrySet()) {			
//				System.out.println(entry);
				if (entry.getValue().isJsonObject()) {
					JsonObject expectedOutputObj = expectedOutputMap.get(entry.getKey()).getAsJsonObject();
					if ( simOutputMap.containsKey(entry.getKey()) ){
						JsonObject simOutputObj = simOutputMap.get(entry.getKey()).getAsJsonObject(); 
						// Check each property of the object
						for(Entry<String, JsonElement> simentry : simOutputMap.get(entry.getKey()).getAsJsonObject().entrySet()){
							String prop = simentry.getKey();
	//						System.out.println("\nTesting "+entry.getKey() +":"+prop);
	//						System.out.println(simOutputObj.get(prop) +  "== "+  expectedOutputObj.get(prop));
							if( ! propSet.contains(prop))
								continue;
//							String propOrAttr = prop;
//							if(simOutputObj.has("attribute")){
//								propOrAttr = simOutputObj.get("attribute").getAsString();
//							}
							Boolean comparisonProperty = compareObjectProperties(simOutputObj, expectedOutputObj, prop);
							// There is a match for that object and property
							if (comparisonProperty){
								if (simOutputObj.has("hasMeasurementDifference")){
									testResults.add(simOutputObj.get("object").getAsString(),
											simOutputObj.get("attribute").getAsString(), 
											expectedOutputObj.get(prop).toString(),
											simOutputObj.get(prop).toString(),
											simOutputObj.get("difference_mrid").getAsString(),
											simOutputObj.get("hasMeasurementDifference").getAsString(),
											true);
									publish(timestamp, simOutputObj.get("object").getAsString(),
											simOutputObj.get("attribute").getAsString(), 
											expectedOutputObj.get(prop).toString(),
											simOutputObj.get(prop).toString(),
											simOutputObj.get("difference_mrid").getAsString(),
											simOutputObj.get("hasMeasurementDifference").getAsString(),
											true);
								}else{
									testResults.add(entry.getKey(), prop, expectedOutputObj.get(prop).toString(), simOutputObj.get(prop).toString(), true);
									publish(timestamp, entry.getKey(), prop, expectedOutputObj.get(prop).toString(), simOutputObj.get(prop).toString(), true);
								}
							//	There is NOT a match for that object property
							} else {
	//							System.out.println("\nFor "+entry.getKey() +":"+prop);
	//							System.out.println("    EXPECTED: "+ simOutputObj.get(prop) );
	//							System.out.println("    GOT:      "+ expectedOutputObj.get(prop) );
//								"hasMeasurementDifference":"FORWARD","difference_mrid":"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4","simulation_id":"1961648576","time":1587670650
								if (simOutputObj.has("hasMeasurementDifference")){
									testResults.add(simOutputObj.get("object").getAsString(),
											simOutputObj.get("attribute").getAsString(), 
											expectedOutputObj.get(prop).toString(),
											simOutputObj.get(prop).toString(),
											simOutputObj.get("difference_mrid").getAsString(),
											simOutputObj.get("hasMeasurementDifference").getAsString(),
											false);
									publish(timestamp, simOutputObj.get("object").getAsString(),
											simOutputObj.get("attribute").getAsString(), 
											expectedOutputObj.get(prop).toString(),
											simOutputObj.get(prop).toString(),
											simOutputObj.get("difference_mrid").getAsString(),
											simOutputObj.get("hasMeasurementDifference").getAsString(),
											false);
								}else{
									testResults.add(entry.getKey(), prop, expectedOutputObj.get(prop).toString(), simOutputObj.get(prop).toString(), false);
									publish(timestamp, entry.getKey(), prop, expectedOutputObj.get(prop).toString(), simOutputObj.get(prop).toString(), false);
								}
							}
						}
					} else{
						System.out.println("No property for "+ entry.getKey());
						System.out.println("No property for "+ entry);
						System.out.println("No property for "+ expectedOutputObj);
//						String prop = entry.getKey();
//						publish(timestamp, entry.getKey(), prop, simentry.getValue().getAsString(), "NA" , false);
						String prop = "value";
						if(expectedOutputObj.has("hasMeasurementDifference") ){
							System.out.println("If compareExpectedAndSim no hasMeasurementDifference");
							testResults.add(expectedOutputObj.get("object").getAsString(),
									expectedOutputObj.get("attribute").getAsString(), 
									expectedOutputObj.get(prop).toString(),
									"NA",
									expectedOutputObj.get("difference_mrid").getAsString(),
									expectedOutputObj.get("hasMeasurementDifference").getAsString(),
									false);
							publish(timestamp, expectedOutputObj.get("object").getAsString(),
									expectedOutputObj.get("attribute").getAsString(), 
									expectedOutputObj.get(prop).toString(),
									"NA",
									expectedOutputObj.get("difference_mrid").getAsString(),
									expectedOutputObj.get("hasMeasurementDifference").getAsString(),
									false);
						}else{
							System.out.println("Else compareExpectedAndSim no hasMeasurementDifference");
							for(Entry<String, JsonElement> simentry : expectedOutputObj.entrySet()){
								String prop1 = simentry.getKey();
		//						System.out.println("\nTesting "+entry.getKey() +":"+prop);
		//						System.out.println(simOutputObj.get(prop) +  "== "+  expectedOutputObj.get(prop));
								if( ! propSet.contains(prop1))
									continue;
								publish(timestamp, entry.getKey(), prop1, simentry.getValue().getAsString(), "NA" , false);
								testResults.add(entry.getKey(), prop1, simentry.getValue().getAsString(), "NA", false);
							}
							// old why did I do this?  It was a problem with the attribute )"ShuntCompensator" versus "value,magnitude,angle" prop right?
//							testResults.add(entry.getKey(), expectedOutputObj.get("attribute").getAsString(), expectedOutputObj.get(prop).toString(), "NA", false);
//							publish(timestamp, entry.getKey(), expectedOutputObj.get("attribute").getAsString(), expectedOutputObj.get(prop).toString(), "NA", false);
						}
							
						
//						for(Entry<String, JsonElement> simentry : expectedOutputObj.entrySet()){
//							String prop1 = simentry.getKey();
//	//						System.out.println("\nTesting "+entry.getKey() +":"+prop);
//	//						System.out.println(simOutputObj.get(prop) +  "== "+  expectedOutputObj.get(prop));
//							if( ! propSet.contains(prop1))
//								continue;
//							publish(timestamp, entry.getKey(), prop1, simentry.getValue().getAsString(), "NA" , false);
//						}
						
					}
				}else
					System.out.println("     Not object" + entry);
			}
//			System.out.println("Number of equals : " + countTrue + " Number of not equals : " + countFalse);
		}
		
		/**
		 * 
		 * @param simOutputPath
		 * @return
		 */
		public JsonObject getSimulationOutput(String simOutput) {
			JsonObject jsonObject = null;
	
			JsonParser parser = new JsonParser();
			JsonElement simOutputObject = parser.parse(simOutput);
	
			if (simOutputObject.isJsonObject()) {
				jsonObject = simOutputObject.getAsJsonObject();
			}
			return jsonObject;
		}
		
		/**
		 * Get the JsonObject from a string
		 * @param simOutput
		 * @return
		 */
		public static JsonObject getSimulationJson(String simOutput) {
			JsonObject jsonObject = null;
	
			JsonParser parser = new JsonParser();
			JsonElement simOutputObject = parser.parse(simOutput);
	
			if (simOutputObject.isJsonObject()) {
				jsonObject = simOutputObject.getAsJsonObject();
			}
	
			return jsonObject;
		}
	
		/**
		 * Get the map of the expected outputs
		 * @param expectedOutputPath
		 * @return
		 * @throws FileNotFoundException
		 */
		public Map<String, JsonElement> getExpectedOutputMap(String expectedOutputPath) {
			Map<String, JsonElement> expectedOutputMap = null;
			try {
				JsonParser parser = new JsonParser();
				JsonElement expectedOutputObj = parser.parse(new BufferedReader(new FileReader(expectedOutputPath)));
				if (expectedOutputObj.isJsonObject()) {
					JsonObject jsonObject = expectedOutputObj.getAsJsonObject();
					JsonObject output = jsonObject.get("expected_output").getAsJsonObject();
					expectedOutputMap = getOutputMap(output);
	
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			return expectedOutputMap;
		}
		
		/**
		 * Get the map of the expected outputs
		 * @param expectedOutputPath
		 * @return
		 * @throws FileNotFoundException
		 */
		public Map<String, JsonElement> getExpectedOutputMap(String timestamp, JsonObject expectedOutputObj) {
			Map<String, JsonElement> expectedOutputMap = null;
			if (expectedOutputObj.isJsonObject()) {
				JsonObject output = expectedOutputObj.getAsJsonObject();				
			
				if (output.has("output") ) output = output.getAsJsonObject("output");
				if(output.has(timestamp)){
					expectedOutputMap = getOutputMap(output.get(timestamp).getAsJsonObject());
				}else{
					if(debug) System.out.println("CompareResults output no index for " + timestamp);
					return null;
				}
			}
			return expectedOutputMap;
		}
		
		/**
		 * Get the map of the expected forward inputs for the timestamp
		 * @param timestamp
		 * @param expectedOutputObj
		 * @return
		 */
		public Map<String, JsonElement> getExpectedForwardInputMap(String timestamp, JsonObject expectedOutputObj) {
			Map<String, JsonElement> expectedOutputMap = null;
			if(expectedOutputObj == null){
				return null;
			}
//			System.out.println("input map");
//			System.out.println(expectedOutputObj.toString());
			if (expectedOutputObj.isJsonObject()) {
				JsonObject output = expectedOutputObj.getAsJsonObject();	
				if (output.has("input") ) output = output.getAsJsonObject("input");

				if(output.has(timestamp)){
					expectedOutputMap = getForwardDifferenceMap(output.get(timestamp).getAsJsonObject());
				}else{
					System.out.println("CompareResults forward input no index for " + timestamp);
					return null;
				}
			}
			return expectedOutputMap;
		}
		
		/**
		 * Get the map of the expected inputs
		 * @return
		 */
		public Map<String, JsonElement> getExpectedReverseInputMap(String timestamp, JsonObject expectedOutputObj) {
			Map<String, JsonElement> expectedOutputMap = null;
			if(expectedOutputObj == null){
				return null;
			}
			if (expectedOutputObj.isJsonObject()) {
				JsonObject output = expectedOutputObj.getAsJsonObject();				
				
				if (output.has("input") ) output = output.getAsJsonObject("input");
				if(output.has(timestamp)){
					expectedOutputMap = getReverseDifferenceMap(output.get(timestamp).getAsJsonObject());
				}else{
					System.out.println("CompareResults reverse input no index for " + timestamp);
					return null;
				}
			}
			return expectedOutputMap;
		}
		
		/**
		 * Get the map of the expected outputs
		 * @param expectedOutputPath
		 * @return
		 * @throws FileNotFoundException
		 */
		public Map<String, JsonElement> getExpectedOutputMap(String timestamp, String expectedOutputPath) {
			Map<String, JsonElement> expectedOutputMap = null;
			try {
				JsonParser parser = new JsonParser();
				JsonElement expectedOutputObj = parser.parse(new BufferedReader(new FileReader(expectedOutputPath)));
				if (expectedOutputObj.isJsonObject()) {
					JsonObject jsonObject = expectedOutputObj.getAsJsonObject();
	//				JsonArray outputs = jsonObject.get("expected_outputs").getAsJsonArray();
	//				for (JsonElement output : outputs) {
	//					expectedOutputMap = getOutputMap(output.getAsJsonObject());
	//				}
	//				
					JsonObject outputs = jsonObject.get("expected_outputs").getAsJsonObject();
					if(outputs.has(timestamp)){
						expectedOutputMap = getOutputMap(outputs.get(timestamp).getAsJsonObject());
					}else{
						return null;
					}
	
	
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			return expectedOutputMap;
		}
	
		public Map<String, JsonElement> getOutputMap(JsonObject output) {
			Map<String, JsonElement> expectedOutputMap;
			String firstKey = getFirstKey(output);
			// CIM
			if (output.has("message") ){
				return getMeasurmentsMap(output);
				
			}
			JsonObject outputs = output.get(firstKey).getAsJsonObject();
			expectedOutputMap = outputs.entrySet().stream()
					.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()));
			return expectedOutputMap;
		}
		
		/**
		 * Build a map of all forward difference messages with object id as the key.
		 * Example return map:
		 * {_307E4291-5FEA-4388-B2E0-2B3D22FE8183={"hasMeasurementDifference":"FORWARD",
		 * 										   "difference_mrid":"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4",
		 * 										   "attribute":"ShuntCompensator.sections",
		 * 										   "value":0.0,"object":
		 * 										   "_307E4291-5FEA-4388-B2E0-2B3D22FE8183"}}"
		 * @param output
		 * @return
		 */
		public Map<String, JsonElement> getForwardDifferenceMap(JsonObject output) {
			JsonObject tempOutput = output;
			if ( tempOutput.has("input") ) tempOutput = tempOutput.getAsJsonObject("input");
			JsonObject tempObj = tempOutput.getAsJsonObject("message");
			Map<String, JsonElement> forwardDifferenceMap = new HashMap<String,JsonElement>();
			
			
			if(tempObj.has("measurements")){
				JsonArray temp = tempObj.getAsJsonArray("measurements");
				for (JsonElement jsonElement : temp) {
					if ( jsonElement.getAsJsonObject().get("hasMeasurementDifference").getAsString().equals("FORWARD")){
//						System.out.println(jsonElement);
						forwardDifferenceMap.put(jsonElement.getAsJsonObject().get("object").getAsString(), jsonElement);
					} 
				}
			} else if (tempObj.has("forward_differences")) {
				JsonParser parser = new JsonParser();
				JsonArray temp = tempObj.getAsJsonArray("forward_differences");
				for (JsonElement jsonElement : temp) {
					jsonElement.getAsJsonObject().add("difference_mrid",tempObj.get("difference_mrid"));
					jsonElement.getAsJsonObject().add("hasMeasurementDifference",parser.parse("FORWARD"));
					forwardDifferenceMap.put(jsonElement.getAsJsonObject().get("object").getAsString(), jsonElement);
				}
			}
			return forwardDifferenceMap;
		}
		
		/**
		 * Build a map of all reverse difference messages
		 * Example return map:
		 * {_307E4291-5FEA-4388-B2E0-2B3D22FE8183={"hasMeasurementDifference":"REVERSE",
		 *                                         "difference_mrid":"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4",
		 *                                         "simulation_id":"1961648576",
		 *                                         "time":1248156014,
		 *                                         "attribute":"ShuntCompensator.sections",
		 *                                         "value":0.0,
		 *                                         "object":"_307E4291-5FEA-4388-B2E0-2B3D22FE8183"}}
		 * @param output
		 * @return
		 */
		public Map<String, JsonElement> getReverseDifferenceMap(JsonObject output) {
			JsonObject tempOutput = output;
			if ( tempOutput.has("input") ) tempOutput = tempOutput.getAsJsonObject("input");
			JsonObject tempObj = tempOutput.getAsJsonObject("message");
			Map<String, JsonElement> reverseDifferenceMap = new HashMap<String,JsonElement>();
			
			if(tempObj.has("measurements")){
				JsonArray temp = tempObj.getAsJsonArray("measurements");
				for (JsonElement jsonElement : temp) {
					if ( jsonElement.getAsJsonObject().get("hasMeasurementDifference").getAsString().equals("REVERSE")){
						reverseDifferenceMap.put(jsonElement.getAsJsonObject().get("object").getAsString(), jsonElement);
					}
				}
			}else if (tempObj.has("reverse_differences")) {
				JsonArray temp = tempObj.getAsJsonArray("reverse_differences");
				JsonParser parser = new JsonParser();
				for (JsonElement jsonElement : temp) {
					jsonElement.getAsJsonObject().add("difference_mrid",tempObj.get("difference_mrid"));
					jsonElement.getAsJsonObject().add("hasMeasurementDifference",parser.parse("REVERSE"));
					reverseDifferenceMap.put(jsonElement.getAsJsonObject().get("object").getAsString(), jsonElement);
				}
			}
			return reverseDifferenceMap;
		}
		
//		public JsonObject getDifferenceMap(JsonObject output) {
////			System.out.println(output.toString());
////			ObjectMapper mapper = new ObjectMapper();
//			if ( output.has("input") ) output = output.getAsJsonObject("input");
//			JsonObject tempObj = output.getAsJsonObject("message");
////			System.out.println(output.getAsJsonObject("message").getAsString());
//			if (! tempObj.get("measurements").isJsonArray() ){
//	
//				Map<String, JsonElement> expectedOutputMap = tempObj.getAsJsonObject("measurements").entrySet().stream()
//						.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()));
////				return expectedOutputMap;
//			}
//			JsonArray temp = tempObj.getAsJsonArray("measurements");
////			expectedOutputMap = new HashMap<String, JsonElement>();
//			JsonObject inputObj = new JsonObject();
//			Map<String, JsonElement> forwardDifferenceMap = new HashMap<String,JsonElement>();
//			Map<String, JsonElement> reverseDifferenceMap = new HashMap<String,JsonElement>();
//			for (JsonElement jsonElement : temp) {
//	//				System.out.println(jsonElement);
//				if ( jsonElement.getAsJsonObject().get("hasMeasurementDifference").getAsString().equals("FORWARD")){
//					forwardDifferenceMap.put(jsonElement.getAsJsonObject().get("object").getAsString(), jsonElement);
//				} else{
//					reverseDifferenceMap.put(jsonElement.getAsJsonObject().get("object").getAsString(), jsonElement);
//				}
//			}
//			return inputObj;
//		}
		
		
		public Map<String, JsonElement> getMeasurmentsMap(JsonObject output) {
//			System.out.println(output.toString());
			Map<String, JsonElement> expectedOutputMap;
//			ObjectMapper mapper = new ObjectMapper();
			if ( output.has("output") ) output = output.getAsJsonObject("output");
			JsonObject tempObj = output.getAsJsonObject("message");
//			System.out.println(output.getAsJsonObject("message").getAsString());
			if (! tempObj.get("measurements").isJsonArray() ){
	
				expectedOutputMap = tempObj.getAsJsonObject("measurements").entrySet().stream()
						.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()));
				return expectedOutputMap;
				
//				String jsonMap = tempObj.get("measurements").toString();
//				try {
//					expectedOutputMap = mapper.readValue(jsonMap, new TypeReference<Map<String, Object>>(){});
//					return expectedOutputMap;
//				} catch (JsonParseException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (JsonMappingException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
			}
			JsonArray temp = tempObj.getAsJsonArray("measurements");
			expectedOutputMap = new HashMap<String, JsonElement>();
			for (JsonElement jsonElement : temp) {
	//				System.out.println(jsonElement);
				expectedOutputMap.put(jsonElement.getAsJsonObject().get("measurement_mrid").getAsString(), jsonElement);
			}
			return expectedOutputMap;
		}
		
		public static String getFirstKey(String json_string) {
			JsonParser parser = new JsonParser();
			JsonElement simOutputObject = parser.parse(json_string);
			String firstKey = CompareResults.getFirstKey(simOutputObject.getAsJsonObject());
			return firstKey;
		}
		
		public static String getFirstKey(JsonObject output) {
			String firstKey = null;
			List<String> keys = output.getAsJsonObject().entrySet()
				    .stream()
				    .map(i -> i.getKey())
				    .collect(Collectors.toCollection(ArrayList::new));
	
	//		keys.forEach(System.out::println);
			firstKey = keys.get(0);
			return firstKey;
		}
	
		/**
		 * Compare two json objects individual property
		 * 
		 * @param simOutputObj
		 * @param expectedOutputObj
		 * @param prop
		 * @return
		 */
		public Boolean compareObjectProperties(JsonObject simOutputObj, JsonObject expectedOutputObj, String prop) {
			Boolean comparison = simOutputObj.get(prop).equals(expectedOutputObj.get(prop));
			JsonPrimitive obj1 = simOutputObj.get(prop).getAsJsonPrimitive();
			JsonPrimitive obj2 = expectedOutputObj.get(prop).getAsJsonPrimitive();
			if (obj1.isNumber() && obj2.isNumber()){
				double f1 = simOutputObj.get(prop).getAsDouble();
				double f2 = expectedOutputObj.get(prop).getAsDouble();
				if("value".equals(prop))
					comparison = f1 == f2;
				else if("angle".equals(prop))
					comparison = equalsE1(f1,f2);
				else
					comparison = equalsE3(f1,f2);
			}
			return comparison;
		}		
	
		void getProp(SimulationOutput simOut) {
			for (SimulationOutputObject out :simOut.output_objects){
				System.out.println(out.name);
				out.getProperties();			
			}
		}
		
		
		public static void main(String[] args) {
			String path = "./applications/python/sim_output_object.json";
			String sim_output = "./applications/python/sim_output.json";
			String expected_output = "./applications/python/expected_output.json";
			if(args.length == 3){
				path=args[0];
				sim_output=args[1];
				expected_output=args[2];
				System.out.println("Arguments are: " + path + " " + sim_output + " " + expected_output);
			} else{
				System.out.println("Command line usage: CompareResults.java simulation_output_object_path sim_output_path expected_output_path");
				System.out.println("Using defaults: " + path + " " + sim_output + " " + expected_output);
			}
			
//			CompareResults compareResults = new CompareResults();
//			SimulationOutput simOutProperties = compareResults.getOutputProperties(path);
//			compareResults.getProp(simOutProperties);
//			compareResults.compareExpectedWithSimulation(sim_output, expected_output, simOutProperties);
//			
//			compareResults.test();
			
			
	//		String testScriptPath = "./applications/python/exampleTestScript.json";
	//		TestScript testScript = loadTestScript(testScriptPath);
	
	//		SimulationOutput simOut = SimulationOutput.parse(str_out);
		}
	
	}
