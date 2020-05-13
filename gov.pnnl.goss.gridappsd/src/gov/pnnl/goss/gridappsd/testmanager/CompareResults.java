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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.complex.ComplexFormat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;

import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.dto.SimulationOutput;
import gov.pnnl.goss.gridappsd.dto.SimulationOutputObject;
import gov.pnnl.goss.gridappsd.dto.TestConfig;
	
	/**
	*
	* @author jsimpson
	*
	*/
	public class CompareResults {
		
	//	private static Logger log = LoggerFactory.getLogger(TestManagerImpl.class);
		
		LogManager logManager;
		
		public CompareResults(){}
		public CompareResults(LogManager logManager){
			this.logManager = logManager;
		}
		
		String[] propsArray = new String[]{"connect_type", "Control", "control_level", "PT_phase", "band_center", "band_width", "dwell_time", "raise_taps", "lower_taps", "regulation"};
	
		
		private final static double EPSILON_e3 = 0.001001;
		
		private final static double EPSILON_e1 = 0.100001;
		
		private final static ComplexFormat cf = new ComplexFormat("j");
		
		public static boolean equals_e3(double a, double b){
		    return a == b ? true : Math.abs(a - b) <= EPSILON_e3;
		}
		
		public static boolean equals_e1(double a, double b){
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
	
//		public SimulationOutput getOutputProperties(String path) {
//			Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
//			JsonReader jsonReader;
//			SimulationOutput testScript = null;
//			try {
//				jsonReader = new JsonReader(new FileReader(path));
//				jsonReader.setLenient(true);
//				testScript = gson.fromJson(new FileReader(path),SimulationOutput.class);
//				jsonReader.close();
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//			
//			return testScript;	
//		}
//		
//		public SimulationOutput getOutputObjects(String path) {
//			Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
//			JsonReader jsonReader;
//			SimulationOutput testScript = null;
//			try {
//				jsonReader = new JsonReader(new FileReader(path));
//				jsonReader.setLenient(true);
//				testScript = gson.fromJson(new FileReader(path),SimulationOutput.class);
//				jsonReader.close();
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//			
//			return testScript;	
//		}
	
		
	//	private Map<Object, Object> mapConfig(Map<String, Integer> input, String prefix) {
	//	    return input.entrySet().stream()
	//	            .collect(Collectors.toMap(entry -> entry.getKey().substring(subLength), entry -> AttributeType.GetByName(entry.getValue())));
	//	}
		
//		public void loadSimResultTestCode(String path, SimulationOutput simOut) {
//			JsonParser parser = new JsonParser();
//			try {
//				BufferedReader br = new BufferedReader(new FileReader(path));
//				JsonElement elm = parser.parse(br);
//				if(elm.isJsonObject()){
//				    JsonObject jsonObject = elm.getAsJsonObject();
//				    JsonObject output = jsonObject.get("output").getAsJsonObject();
//				    JsonObject f2 = output.get(getFeeder()).getAsJsonObject();
//		    
//				    // Turn to a Map 
//				    Map<String, JsonElement> ouputsMap = f2.entrySet().stream()
//				                                    .collect(Collectors.toMap(Map.Entry::getKey,e -> e.getValue()));
//	
//					for (SimulationOutputObject out :simOut.output_objects){
//						JsonElement outputTmp = ouputsMap.get(out.name);
//						System.out.println(out.name + " " +ouputsMap.get(out.name));
//						if( output != null){
//							for (String prop : out.getProperties()) {
//								System.out.println("     "  +prop);
//								System.out.println("     "  + outputTmp.getAsJsonObject().get(prop));
//								
//							}
//						}
//						break;
//					}
//				    
//	//			    Set<Entry<String, JsonElement>> entrySet = f2.entrySet();		    
//	//			    for(Map.Entry<String,JsonElement> entry : entrySet){
//	//			    	if(entry.getValue().isJsonObject()){
//	//			    		JsonObject obj = entry.getValue().getAsJsonObject();
//	//			    		for (String prop : propsArray) {
//	//				    		if (obj.has(prop)) System.out.println(prop +  " : " + obj.get(prop));	
//	//						} 
//	//
//	//			    	}
//	//			        System.out.println("key >>> "+entry.getKey());
//	//			        System.out.println("val >>> "+entry.getValue());
//	//			    }
//	
//				}		
//			} catch (FileNotFoundException e) {
//				e.printStackTrace();
//			}
//		}
	
		
//		/**
//		 * compareExpectedWithSimulation
//		 * @param simOutputPath
//		 * @param expectedOutputPath
//		 * @param simOutProperties
//		 */
//		public TestResults compareExpectedWithSimulation(String simOutputPath, String expectedOutputPath, SimulationOutput simOutProperties) {
//			Map<String, JsonElement> expectedOutputMap = getExpectedOutputMap(expectedOutputPath);
//	
//	//		Map<String, List<String>> propMap = simOutProperties.getOutputObjects().stream()
//	//				.collect(Collectors.toMap(SimulationOutputObject::getName, e -> e.getProperties()));
//			JsonObject jsonObject = getSimulationOutputFile(simOutputPath);
//			
//			return compareExpectedWithSimulation(expectedOutputMap, jsonObject);
//		}
//		
		/**
		 * compareExpectedWithSimulation
		 * @param simOutputPath
		 * @param expectedOutputPath
		 * @param simOutProperties
		 */
		public TestResults compareExpectedWithSimulation(JsonObject jsonObject, String expectedOutputPath) {
			Map<String, JsonElement> expectedOutputMap = getExpectedOutputMap(expectedOutputPath);
			return compareExpectedWithSimulation(expectedOutputMap, jsonObject);
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
		
			return compareExpectedWithSimulationOutput(expectedOutputMap, jsonObject);
		}
		
		public TestResults compareExpectedWithSimulationInput(String timestamp, JsonObject jsonObject, JsonObject expectedInput) {
			Map<String, JsonElement> expectedForwardMap = getExpectedForwardInputMap(timestamp, expectedInput);
			Map<String, JsonElement> expectedReverseMap = getExpectedReverseInputMap(timestamp, expectedInput);
			if (expectedForwardMap == null) return new TestResults();
	//		Map<String, List<String>> propMap = simOutProperties.getOutputObjects().stream()
	//				.collect(Collectors.toMap(SimulationOutputObject::getName, e -> e.getProperties()));
			Map<String, JsonElement> forwardMap= getForwardDifferenceMap(jsonObject);
			Map<String, JsonElement> reverseMap= getReverseDifferenceMap(jsonObject);
			TestResults testResults = new TestResults();
	//		JsonObject output = jsonObject;
	//		String firstKey = getFirstKey(output);
			
//			Map<String, JsonElement> simOutputMap= getMeasurmentsMap(jsonObject);
			compareExpectedAndSim(expectedForwardMap, testResults, forwardMap);
			compareExpectedAndSim(expectedReverseMap, testResults, reverseMap);
			
			return testResults;
//			return compareExpectedWithSimulationOutput(expectedOutputMap, jsonObject);
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
	
		public TestResults compareExpectedWithSimulation(Map<String, JsonElement> expectedOutputMap, JsonObject jsonObject) {
			
			TestResults testResults = new TestResults();
			JsonObject output = jsonObject.get("output").getAsJsonObject();
			JsonObject simOutput = output.get(getFeeder()).getAsJsonObject();
			compareExpectedWithSimulation(expectedOutputMap,  testResults, simOutput);
			return testResults;
		}
		
		public String getFeeder() {
	//		TestManagerQueryFactory qf = new TestManagerQueryFactory();
	//		return qf.getFeeder();
			return "ieee8500";
		}
		
		/**
		 * Get the set of properties that matches the expected results
		 * @param testScript
		 * @param simOutProperties
		 * @return set
		 */
		public HashSet<String> getMatchedProperties(TestConfig testScript, SimulationOutput simOutProperties) {
			List<SimulationOutputObject> simOutputObjects = simOutProperties.getOutputObjects();
			Set<Entry<String, List<String>>> es = null;//testScript.getOutputs().entrySet();
			
			HashSet<String> simOONames = new HashSet<String>();
			for (SimulationOutputObject simulationOutputObject : simOutputObjects) {
				simOONames.add(simulationOutputObject.getName());
			}
			
			HashSet<String> expectedOutputNames = new HashSet<String>();
	
			for (Entry<String, List<String>> entry : es) {
				List<String> listOfValues = entry.getValue();
				for (String value : listOfValues) {
					expectedOutputNames.add(value);
				}
			}
			
	//		System.out.println(Sets.intersection(listenOutputNames, sooNames).size() +" " +Sets.intersection(listenOutputNames, sooNames));
			// Get intersection
			expectedOutputNames.retainAll(simOONames);
			System.out.println(expectedOutputNames.size() + " "  + expectedOutputNames);
			return expectedOutputNames;
		}
	
		public TestResults compareExpectedWithSimulationOutput(Map<String, JsonElement> expectedOutputMap,
				 JsonObject jsonObject) {
	
			TestResults testResults = new TestResults();
	//		JsonObject output = jsonObject;
	//		String firstKey = getFirstKey(output);
			
			Map<String, JsonElement> simOutputMap= getMeasurmentsMap(jsonObject);
			compareExpectedAndSim(expectedOutputMap, testResults, simOutputMap);
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
		
		public void compareExpectedWithSimulation(Map<String, JsonElement> expectedOutputMap, 
				TestResults testResults, JsonObject simOutput) {
	
			if (simOutput != null) { 
				Set<Entry<String, JsonElement>> simOutputSet = simOutput.entrySet();
				
				compareExpectedAndSimOld(expectedOutputMap, testResults, simOutputSet);
				
			}else{
				System.out.println("Sim output is null");
			}
	
		}
		
		public void compareExpectedAndSim(Map<String, JsonElement> expectedOutputMap, TestResults testResults,
				Map<String, JsonElement> simOutputMap) {
			int countTrue = 0;
			int countFalse = 0;
			for (Entry<String, JsonElement> entry : expectedOutputMap.entrySet()) {			
	//			System.out.println(entry);
				if (entry.getValue().isJsonObject()) {
					JsonObject expectedOutputObj = expectedOutputMap.get(entry.getKey()).getAsJsonObject();
					if ( simOutputMap.containsKey(entry.getKey()) ){
						JsonObject simOutputObj = simOutputMap.get(entry.getKey()).getAsJsonObject(); 
						// Check each property of the object
						for(Entry<String, JsonElement> simentry : simOutputMap.get(entry.getKey()).getAsJsonObject().entrySet()){
							String prop = simentry.getKey();
	//						System.out.println("\nTesting "+entry.getKey() +":"+prop);
	//						System.out.println(simOutputObj.get(prop) +  "== "+  expectedOutputObj.get(prop));
							Boolean comparison = compareObjectProperties(simOutputObj, expectedOutputObj, prop);
							if (comparison){
//								testResults.add(entry.getKey() , prop, expectedOutputObj.get(prop).toString(), simOutputObj.get(prop).toString());
								countTrue++;
							}
							else{
	//							System.out.println("\nFor "+entry.getKey() +":"+prop);
	//							System.out.println("    EXPECTED: "+ simOutputObj.get(prop) );
	//							System.out.println("    GOT:      "+ expectedOutputObj.get(prop) );
//								"hasMeasurementDifference":"FORWARD","difference_mrid":"1fae379c-d0e2-4c80-8f2c-c5d7a70ff4d4","simulation_id":"1961648576","time":1587670650
								if (simOutputObj.has("hasMeasurementDifference")){
									testResults.add(entry.getKey(),
											prop, 
											expectedOutputObj.get(prop).toString(),
											simOutputObj.get(prop).toString(),
											simOutputObj.get("hasMeasurementDifference").toString(),
											simOutputObj.get("difference_mrid").toString());
								}else{
									testResults.add(entry.getKey() , prop, expectedOutputObj.get(prop).toString(), simOutputObj.get(prop).toString());
								}
								countFalse++;
							}
						}
					} else{
						System.out.println("No property for "+ entry.getKey());
					}
				}else
					System.out.println("     Not object" + entry);
			}
			System.out.println("Number of equals : " + countTrue + " Number of not equals : " + countFalse);
		}
		
		public void compareExpectedAndSimOld(Map<String, JsonElement> expectedOutputMap, TestResults testResults,
				Set<Entry<String, JsonElement>> simOutputSet) {
			int countTrue = 0;
			int countFalse = 0;
			for (Map.Entry<String, JsonElement> simOutputElement : simOutputSet) {
				System.out.println(simOutputElement);
				if (simOutputElement.getValue().isJsonObject()) {
					JsonObject simOutputObj = simOutputElement.getValue().getAsJsonObject();
					JsonObject expectedOutputObj = expectedOutputMap.get(simOutputElement.getKey()).getAsJsonObject();
					for (Entry<String, JsonElement> entry : expectedOutputObj.entrySet()) {
						String prop = entry.getKey();				
						if (simOutputObj.has(prop)) {
	//					List<String> propsArray = propMap.get(simOutputElement.getKey());
	//					for (String prop : propsArray) {
	//						if (simOutputObj.has(prop) && expectedOutputttObj.has(prop)) {
							Boolean comparison = compareObjectProperties(simOutputObj, expectedOutputObj, prop);
							if (comparison)
								countTrue++;
							else{
	//								System.out.println("     " + prop +  " : " + simOutputObj.get(prop) + " == " +  expectedOutputObj.get(prop) + " is " + comparison);
								System.out.println("\nFor "+simOutputElement.getKey() +":"+prop);
								System.out.println("    EXPECTED: "+ simOutputObj.get(prop) );
								System.out.println("    GOT:      "+ expectedOutputObj.get(prop) );
								testResults.add(simOutputElement.getKey() , prop, expectedOutputObj.get(prop).toString(), simOutputObj.get(prop).toString());
								countFalse++;
							}
	
						} else
							System.out.println("No property");
					}
				} else
					System.out.println("     Not object" + simOutputElement);
			}
			System.out.println("Number of equals : " + countTrue + " Number of not equals : " + countFalse);
		}
		
	
//		/**
//		 * 
//		 * @param simOutputPath
//		 * @return
//		 */
//		private JsonObject getSimulationOutputFile(String simOutputPath) {
//			JsonObject jsonObject = null;
//			try {
//				JsonParser parser = new JsonParser();
//				JsonElement simOutputObject = parser.parse(new BufferedReader(new FileReader(simOutputPath)));
//	
//				if (simOutputObject.isJsonObject()) {
//					jsonObject = simOutputObject.getAsJsonObject();
//				}
//	
//			} catch (FileNotFoundException e) {
//				e.printStackTrace();
//			}
//			return jsonObject;
//		}
		
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
//				JsonObject output = jsonObject.get("expedtedResults").getAsJsonObject();  //TODO fix spelling
				
				if (output.has("output") ) output = output.getAsJsonObject("output");
				if(output.has(timestamp)){
					expectedOutputMap = getOutputMap(output.get(timestamp).getAsJsonObject());
				}else{
					System.out.println("CompareResults no index for " + timestamp);
					return null;
				}
			}
			return expectedOutputMap;
		}
		
		/**
		 * Get the map of the expected inputs
		 * @return
		 */
		public Map<String, JsonElement> getExpectedForwardInputMap(String timestamp, JsonObject expectedOutputObj) {
			Map<String, JsonElement> expectedOutputMap = null;
			if (expectedOutputObj.isJsonObject()) {
				JsonObject output = expectedOutputObj.getAsJsonObject();	
				if (output.has("input") ) output = output.getAsJsonObject("input");
				if(output.has(timestamp)){
					expectedOutputMap = getForwardDifferenceMap(output.get(timestamp).getAsJsonObject());
				}else{
					System.out.println("CompareResults no index for" + timestamp);
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
			if (expectedOutputObj.isJsonObject()) {
				JsonObject output = expectedOutputObj.getAsJsonObject();				
				
				if (output.has("input") ) output = output.getAsJsonObject("input");
				if(output.has(timestamp)){
					expectedOutputMap = getReverseDifferenceMap(output.get(timestamp).getAsJsonObject());
				}else{
					System.out.println("CompareResults no index for" + timestamp);
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
		
		public Map<String, JsonElement> getForwardDifferenceMap(JsonObject output) {
			if ( output.has("input") ) output = output.getAsJsonObject("input");
			JsonObject tempObj = output.getAsJsonObject("message");
			Map<String, JsonElement> forwardDifferenceMap = new HashMap<String,JsonElement>();
			if(tempObj.has("measurements")){
				JsonArray temp = tempObj.getAsJsonArray("measurements");
				for (JsonElement jsonElement : temp) {
					if ( jsonElement.getAsJsonObject().get("hasMeasurementDifference").getAsString().equals("FORWARD")){
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
		
		public Map<String, JsonElement> getReverseDifferenceMap(JsonObject output) {
			if ( output.has("input") ) output = output.getAsJsonObject("input");
			JsonObject tempObj = output.getAsJsonObject("message");
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
		
		public JsonObject getDifferenceMap(JsonObject output) {
//			System.out.println(output.toString());
			Map<String, JsonElement> expectedOutputMap;
//			ObjectMapper mapper = new ObjectMapper();
			if ( output.has("input") ) output = output.getAsJsonObject("input");
			JsonObject tempObj = output.getAsJsonObject("message");
//			System.out.println(output.getAsJsonObject("message").getAsString());
			if (! tempObj.get("measurements").isJsonArray() ){
	
				expectedOutputMap = tempObj.getAsJsonObject("measurements").entrySet().stream()
						.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()));
//				return expectedOutputMap;
			}
			JsonArray temp = tempObj.getAsJsonArray("measurements");
//			expectedOutputMap = new HashMap<String, JsonElement>();
			JsonObject inputObj = new JsonObject();
			Map<String, JsonElement> forwardDifferenceMap = new HashMap<String,JsonElement>();
			Map<String, JsonElement> reverseDifferenceMap = new HashMap<String,JsonElement>();
			for (JsonElement jsonElement : temp) {
	//				System.out.println(jsonElement);
				if ( jsonElement.getAsJsonObject().get("hasMeasurementDifference").getAsString().equals("FORWARD")){
					forwardDifferenceMap.put(jsonElement.getAsJsonObject().get("object").getAsString(), jsonElement);
				} else{
					reverseDifferenceMap.put(jsonElement.getAsJsonObject().get("object").getAsString(), jsonElement);
				}
			}
			return inputObj;
		}
		
		
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
	//		System.out.println("     " + prop +  " : " + simOutputObj.get(prop) + " == " +  expectedOutputObj.get(prop) + " is " + comparison);
			// Test voltage, i.e. complex number
//			if(prop.startsWith("voltage_") || prop.startsWith("power_in_") ){
//				Complex c1 = cf.parse(simOutputObj.get(prop).getAsString());
//				Complex c2 = cf.parse(expectedOutputObj.get(prop).getAsString());
//	//			System.out.println("              Complex" + c1 + c2 + equals(c1,c2));
//				comparison = equals(c1,c2);
//				return comparison;
//			}
			System.out.println(prop);
			JsonPrimitive obj1 = simOutputObj.get(prop).getAsJsonPrimitive();
			JsonPrimitive obj2 = expectedOutputObj.get(prop).getAsJsonPrimitive();
			if (obj1.isNumber() && obj2.isNumber()){
				double f1 = simOutputObj.get(prop).getAsDouble();
				double f2 = expectedOutputObj.get(prop).getAsDouble();
				if(prop.equals("value"))
					comparison = f1 == f2;
				else if(prop.equals("angle"))
					comparison = equals_e1(f1,f2);
				else
					comparison = equals_e3(f1,f2);
			}
			return comparison;
		}
//		
//		public void loadSimResult_old(String sim_output, SimulationOutput simOutProperties, String expected_output) {
//			JsonParser parser = new JsonParser();
//			try {
//				BufferedReader br = new BufferedReader(new FileReader(sim_output));
//				JsonElement elm = parser.parse(br);
//				if(elm.isJsonObject()){
//				    JsonObject jsonObject = elm.getAsJsonObject();
//	
//				    JsonObject output = jsonObject.get("output").getAsJsonObject();
//	
//				    JsonObject f2 = output.get("ieee8500").getAsJsonObject();
//	
//				    Set<Entry<String, JsonElement>> entrySet = f2.entrySet();
//				    for(Map.Entry<String,JsonElement> simOutputEle : entrySet){
//				    	if(simOutputEle.getValue().isJsonObject()){
//				    		JsonObject simOutputObj = simOutputEle.getValue().getAsJsonObject();
//				    		for (String prop : propsArray) {
//					    		if (simOutputObj.has(prop)) System.out.println(prop +  " : " + simOutputObj.get(prop));	
//							} 
//	
//				    	}
//				        System.out.println("key >>> "+simOutputEle.getKey());
//				        System.out.println("val >>> "+simOutputEle.getValue());
//				    }
//	
//	//			    if(f2.isJsonObject()){
//	//			        JsonObject f2Obj = f2.getAsJsonObject();
//	//			        JsonElement f3 = f2Obj.get("f3");
//	//			    }
//	
//				}
//				
//			} catch (FileNotFoundException e) {
//				e.printStackTrace();
//			}	
//		}
		
		public void test(){
	
			CompareResults compareResults = new CompareResults();
	//		String str_out = "{\"ieee8500\":{\"cap_capbank0a\":{\"capacitor_A\":400000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":100.0,\"phases\":\"AN\",\"phases_connected\":\"NA\",\"pt_phase\":\"A\",\"switchA\":\"CLOSED\"},\"cap_capbank0b\":{\"capacitor_B\":400000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":101.0,\"phases\":\"BN\",\"phases_connected\":\"NB\",\"pt_phase\":\"B\",\"switchB\":\"CLOSED\"},\"cap_capbank0c\":{\"capacitor_C\":400000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":102.0,\"phases\":\"CN\",\"phases_connected\":\"NC\",\"pt_phase\":\"C\",\"switchC\":\"CLOSED\"},\"cap_capbank1a\":{\"capacitor_A\":300000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":100.0,\"phases\":\"AN\",\"phases_connected\":\"NA\",\"pt_phase\":\"A\",\"switchA\":\"CLOSED\"},\"cap_capbank1b\":{\"capacitor_B\":300000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":101.0,\"phases\":\"BN\",\"phases_connected\":\"NB\",\"pt_phase\":\"B\",\"switchB\":\"CLOSED\"},\"cap_capbank1c\":{\"capacitor_C\":300000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":102.0,\"phases\":\"CN\",\"phases_connected\":\"NC\",\"pt_phase\":\"C\",\"switchC\":\"CLOSED\"},\"cap_capbank2a\":{\"capacitor_A\":300000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":100.0,\"phases\":\"AN\",\"phases_connected\":\"NA\",\"pt_phase\":\"A\",\"switchA\":\"CLOSED\"},\"cap_capbank2b\":{\"capacitor_B\":300000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":101.0,\"phases\":\"BN\",\"phases_connected\":\"NB\",\"pt_phase\":\"B\",\"switchB\":\"CLOSED\"},\"cap_capbank2c\":{\"capacitor_C\":300000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":102.0,\"phases\":\"CN\",\"phases_connected\":\"NC\",\"pt_phase\":\"C\",\"switchC\":\"CLOSED\"},\"cap_capbank3\":{\"capacitor_A\":300000.0,\"capacitor_B\":300000.0,\"capacitor_C\":300000.0,\"control\":\"MANUAL\",\"control_level\":\"INDIVIDUAL\",\"dwell_time\":0.0,\"phases\":\"ABCN\",\"phases_connected\":\"NCBA\",\"pt_phase\":\"\",\"switchA\":\"CLOSED\",\"switchB\":\"CLOSED\",\"switchC\":\"CLOSED\"},\"nd_190-7361\":{\"voltage_A\":\"4842.574418-4476.685148j V\",\"voltage_B\":\"-6188.665618-1939.201071j V\",\"voltage_C\":\"1439.949638+6505.686300j V\"},\"nd_190-8581\":{\"voltage_A\":\"4424.594646-4708.594641j V\",\"voltage_B\":\"-6385.646701-1373.465954j V\",\"voltage_C\":\"1432.367021+6738.674787j V\"},\"nd_190-8593\":{\"voltage_A\":\"3865.232091-4967.366927j V\",\"voltage_B\":\"-6370.129563-674.013528j V\",\"voltage_C\":\"1761.647774+6618.414304j V\"},\"nd__hvmv_sub_lsb\":{\"voltage_A\":\"6064.561543-3845.670885j V\",\"voltage_B\":\"-6352.552644-3361.611238j V\",\"voltage_C\":\"276.907044+7179.756922j V\"},\"nd_l2673313\":{\"voltage_A\":\"3275.878234-4690.132454j V\",\"voltage_B\":\"-6029.444730-334.685040j V\",\"voltage_C\":\"1841.889122+6312.023841j V\"},\"nd_l2876814\":{\"voltage_A\":\"3363.715890-4748.790719j V\",\"voltage_B\":\"-6029.028061-333.125547j V\",\"voltage_C\":\"1838.833947+6392.625289j V\"},\"nd_l2955047\":{\"voltage_A\":\"4044.821664-4235.230449j V\",\"voltage_B\":\"-5999.032898-1350.673025j V\",\"voltage_C\":\"1397.787052+6704.224541j V\"},\"nd_l3160107\":{\"voltage_A\":\"4563.436102-4171.693819j V\",\"voltage_B\":\"-5799.391981-1855.560357j V\",\"voltage_C\":\"1375.873493+6381.975351j V\"},\"nd_l3254238\":{\"voltage_A\":\"4354.789079-4256.496997j V\",\"voltage_B\":\"-5786.125185-1559.849649j V\",\"voltage_C\":\"1503.404103+6306.629409j V\"},\"nd_m1047574\":{\"voltage_A\":\"3637.596500-4661.598422j V\",\"voltage_B\":\"-6139.105177-656.801712j V\",\"voltage_C\":\"1736.935828+6543.527740j V\"},\"rcon_FEEDER_REG\":{\"Control\":\"MANUAL\",\"PT_phase\":\"CBA\",\"band_center\":0.0,\"band_width\":0.0,\"connect_type\":\"WYE_WYE\",\"control_level\":\"INDIVIDUAL\",\"dwell_time\":15.0,\"lower_taps\":16,\"raise_taps\":16,\"regulation\":0.10000000000000001},\"rcon_VREG2\":{\"Control\":\"MANUAL\",\"PT_phase\":\"CBA\",\"band_center\":0.0,\"band_width\":0.0,\"connect_type\":\"WYE_WYE\",\"control_level\":\"INDIVIDUAL\",\"dwell_time\":15.0,\"lower_taps\":16,\"raise_taps\":16,\"regulation\":0.10000000000000001},\"rcon_VREG3\":{\"Control\":\"MANUAL\",\"PT_phase\":\"CBA\",\"band_center\":0.0,\"band_width\":0.0,\"connect_type\":\"WYE_WYE\",\"control_level\":\"INDIVIDUAL\",\"dwell_time\":15.0,\"lower_taps\":16,\"raise_taps\":16,\"regulation\":0.10000000000000001},\"rcon_VREG4\":{\"Control\":\"MANUAL\",\"PT_phase\":\"CBA\",\"band_center\":0.0,\"band_width\":0.0,\"connect_type\":\"WYE_WYE\",\"control_level\":\"INDIVIDUAL\",\"dwell_time\":15.0,\"lower_taps\":16,\"raise_taps\":16,\"regulation\":0.10000000000000001},\"reg_FEEDER_REG\":{\"configuration\":\"rcon_FEEDER_REG\",\"phases\":\"ABC\",\"tap_A\":2,\"tap_B\":2,\"tap_C\":1,\"to\":\"nd__hvmv_sub_lsb\"},\"reg_VREG2\":{\"configuration\":\"rcon_VREG2\",\"phases\":\"ABC\",\"tap_A\":10,\"tap_B\":6,\"tap_C\":2,\"to\":\"nd_190-8593\"},\"reg_VREG3\":{\"configuration\":\"rcon_VREG3\",\"phases\":\"ABC\",\"tap_A\":16,\"tap_B\":10,\"tap_C\":1,\"to\":\"nd_190-8581\"},\"reg_VREG4\":{\"configuration\":\"rcon_VREG4\",\"phases\":\"ABC\",\"tap_A\":12,\"tap_B\":12,\"tap_C\":5,\"to\":\"nd_190-7361\"},\"xf_hvmv_sub\":{\"power_in_A\":\"6998345.083588+2349121.476580j VA\",\"power_in_B\":\"6434345.348817+1458655.625879j VA\",\"power_in_C\":\"7487531.075499+1415431.565922j VA\"}}}\n";
			String str_out = "{\"output\": {\"ieee8500\":{\"cap_capbank0a\":{\"capacitor_A\":400000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":100.0,\"phases\":\"AN\",\"phases_connected\":\"NA\",\"pt_phase\":\"A\",\"switchA\":\"OPEN\"},\"cap_capbank0b\":{\"capacitor_B\":400000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":101.0,\"phases\":\"BN\",\"phases_connected\":\"NB\",\"pt_phase\":\"B\",\"switchB\":\"OPEN\"},\"cap_capbank0c\":{\"capacitor_C\":400000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":102.0,\"phases\":\"CN\",\"phases_connected\":\"NC\",\"pt_phase\":\"C\",\"switchC\":\"CLOSED\"},\"cap_capbank1a\":{\"capacitor_A\":300000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":100.0,\"phases\":\"AN\",\"phases_connected\":\"NA\",\"pt_phase\":\"A\",\"switchA\":\"CLOSED\"},\"cap_capbank1b\":{\"capacitor_B\":300000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":101.0,\"phases\":\"BN\",\"phases_connected\":\"NB\",\"pt_phase\":\"B\",\"switchB\":\"CLOSED\"},\"cap_capbank1c\":{\"capacitor_C\":300000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":102.0,\"phases\":\"CN\",\"phases_connected\":\"NC\",\"pt_phase\":\"C\",\"switchC\":\"CLOSED\"},\"cap_capbank2a\":{\"capacitor_A\":300000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":100.0,\"phases\":\"AN\",\"phases_connected\":\"NA\",\"pt_phase\":\"A\",\"switchA\":\"CLOSED\"},\"cap_capbank2b\":{\"capacitor_B\":300000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":101.0,\"phases\":\"BN\",\"phases_connected\":\"NB\",\"pt_phase\":\"B\",\"switchB\":\"CLOSED\"},\"cap_capbank2c\":{\"capacitor_C\":300000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":102.0,\"phases\":\"CN\",\"phases_connected\":\"NC\",\"pt_phase\":\"C\",\"switchC\":\"CLOSED\"},\"cap_capbank3\":{\"capacitor_A\":300000.0,\"capacitor_B\":300000.0,\"capacitor_C\":300000.0,\"control\":\"MANUAL\",\"control_level\":\"INDIVIDUAL\",\"dwell_time\":0.0,\"phases\":\"ABCN\",\"phases_connected\":\"NCBA\",\"pt_phase\":\"\",\"switchA\":\"OPEN\",\"switchB\":\"OPEN\",\"switchC\":\"OPEN\"},\"nd_190-7361\":{\"voltage_A\":\"5990.768173-3952.329601j V\",\"voltage_B\":\"-6441.772989-3132.820514j V\",\"voltage_C\":\"500.477553+7016.663246j V\"},\"nd_190-8581\":{\"voltage_A\":\"6010.972062-4040.492774j V\",\"voltage_B\":\"-6473.150998-2928.295365j V\",\"voltage_C\":\"560.699231+7031.784680j V\"},\"nd_190-8593\":{\"voltage_A\":\"5950.964041-4083.166058j V\",\"voltage_B\":\"-6576.917479-2779.138161j V\",\"voltage_C\":\"674.850808+7108.610716j V\"},\"nd__hvmv_sub_lsb\":{\"voltage_A\":\"6153.230210-3824.632651j V\",\"voltage_B\":\"-6377.685483-3421.485972j V\",\"voltage_C\":\"218.684419+7071.000685j V\"},\"nd_l2673313\":{\"voltage_A\":\"5796.131280-4048.557299j V\",\"voltage_B\":\"-6516.129376-2687.913627j V\",\"voltage_C\":\"689.648289+7033.263880j V\"},\"nd_l2876814\":{\"voltage_A\":\"5820.549476-4056.651275j V\",\"voltage_B\":\"-6515.382040-2686.397831j V\",\"voltage_C\":\"688.293478+7053.160282j V\"},\"nd_l2955047\":{\"voltage_A\":\"5760.062780-3865.865625j V\",\"voltage_B\":\"-6314.785800-2873.229158j V\",\"voltage_C\":\"562.047666+7158.488643j V\"},\"nd_l3160107\":{\"voltage_A\":\"5857.106356-3863.688419j V\",\"voltage_B\":\"-6259.981912-3046.231363j V\",\"voltage_C\":\"497.292122+6990.999461j V\"},\"nd_l3254238\":{\"voltage_A\":\"5889.015738-3883.750230j V\",\"voltage_B\":\"-6333.193211-3054.096309j V\",\"voltage_C\":\"491.410326+6969.849946j V\"},\"nd_m1047574\":{\"voltage_A\":\"5842.733082-4007.048582j V\",\"voltage_B\":\"-6413.940334-2711.833093j V\",\"voltage_C\":\"670.025093+7066.051553j V\"},\"rcon_FEEDER_REG\":{\"Control\":\"MANUAL\",\"PT_phase\":\"CBA\",\"band_center\":126.5,\"band_width\":2.0,\"connect_type\":\"WYE_WYE\",\"control_level\":\"INDIVIDUAL\",\"dwell_time\":15.0,\"lower_taps\":16,\"raise_taps\":16,\"regulation\":0.10000000000000001},\"rcon_VREG2\":{\"Control\":\"MANUAL\",\"PT_phase\":\"CBA\",\"band_center\":125.0,\"band_width\":2.0,\"connect_type\":\"WYE_WYE\",\"control_level\":\"INDIVIDUAL\",\"dwell_time\":15.0,\"lower_taps\":16,\"raise_taps\":16,\"regulation\":0.10000000000000001},\"rcon_VREG3\":{\"Control\":\"MANUAL\",\"PT_phase\":\"CBA\",\"band_center\":125.0,\"band_width\":2.0,\"connect_type\":\"WYE_WYE\",\"control_level\":\"INDIVIDUAL\",\"dwell_time\":15.0,\"lower_taps\":16,\"raise_taps\":16,\"regulation\":0.10000000000000001},\"rcon_VREG4\":{\"Control\":\"MANUAL\",\"PT_phase\":\"CBA\",\"band_center\":125.0,\"band_width\":2.0,\"connect_type\":\"WYE_WYE\",\"control_level\":\"INDIVIDUAL\",\"dwell_time\":15.0,\"lower_taps\":16,\"raise_taps\":16,\"regulation\":0.10000000000000001},\"reg_FEEDER_REG\":{\"configuration\":\"rcon_FEEDER_REG\",\"phases\":\"ABC\",\"tap_A\":1,\"tap_B\":1,\"tap_C\":-4,\"to\":\"nd__hvmv_sub_lsb\"},\"reg_VREG2\":{\"configuration\":\"rcon_VREG2\",\"phases\":\"ABC\",\"tap_A\":3,\"tap_B\":4,\"tap_C\":1,\"to\":\"nd_190-8593\"},\"reg_VREG3\":{\"configuration\":\"rcon_VREG3\",\"phases\":\"ABC\",\"tap_A\":7,\"tap_B\":4,\"tap_C\":-3,\"to\":\"nd_190-8581\"},\"reg_VREG4\":{\"configuration\":\"rcon_VREG4\",\"phases\":\"ABC\",\"tap_A\":4,\"tap_B\":5,\"tap_C\":1,\"to\":\"nd_190-7361\"},\"xf_hvmv_sub\":{\"power_in_A\":\"1581122.900437-54039.972422j VA\",\"power_in_B\":\"1402789.071649-236163.193152j VA\",\"power_in_C\":\"1651079.408985-301389.726115j VA\"}}}\n, \"command\": \"nextTimeStep\"}";
	
	//		str_out = "{\"output\": {\"ieee8500\":{\"cap_capbank0a\":{\"capacitor_A\":400000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":100.0,\"phases\":\"AN\",\"phases_connected\":\"NA\",\"pt_phase\":\"A\",\"switchA\":\"CLOSED\"},\"cap_capbank0b\":{\"capacitor_B\":400000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":101.0,\"phases\":\"BN\",\"phases_connected\":\"NB\",\"pt_phase\":\"B\",\"switchB\":\"CLOSED\"},\"cap_capbank0c\":{\"capacitor_C\":400000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":102.0,\"phases\":\"CN\",\"phases_connected\":\"NC\",\"pt_phase\":\"C\",\"switchC\":\"CLOSED\"},\"cap_capbank1a\":{\"capacitor_A\":300000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":100.0,\"phases\":\"AN\",\"phases_connected\":\"NA\",\"pt_phase\":\"A\",\"switchA\":\"CLOSED\"},\"cap_capbank1b\":{\"capacitor_B\":300000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":101.0,\"phases\":\"BN\",\"phases_connected\":\"NB\",\"pt_phase\":\"B\",\"switchB\":\"CLOSED\"},\"cap_capbank1c\":{\"capacitor_C\":300000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":102.0,\"phases\":\"CN\",\"phases_connected\":\"NC\",\"pt_phase\":\"C\",\"switchC\":\"CLOSED\"},\"cap_capbank2a\":{\"capacitor_A\":300000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":100.0,\"phases\":\"AN\",\"phases_connected\":\"NA\",\"pt_phase\":\"A\",\"switchA\":\"CLOSED\"},\"cap_capbank2b\":{\"capacitor_B\":300000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":101.0,\"phases\":\"BN\",\"phases_connected\":\"NB\",\"pt_phase\":\"B\",\"switchB\":\"CLOSED\"},\"cap_capbank2c\":{\"capacitor_C\":300000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":102.0,\"phases\":\"CN\",\"phases_connected\":\"NC\",\"pt_phase\":\"C\",\"switchC\":\"CLOSED\"},\"cap_capbank3\":{\"capacitor_A\":300000.0,\"capacitor_B\":300000.0,\"capacitor_C\":300000.0,\"control\":\"MANUAL\",\"control_level\":\"INDIVIDUAL\",\"dwell_time\":0.0,\"phases\":\"ABCN\",\"phases_connected\":\"NCBA\",\"pt_phase\":\"\",\"switchA\":\"CLOSED\",\"switchB\":\"CLOSED\",\"switchC\":\"CLOSED\"},\"nd_190-7361\":{\"voltage_A\":\"6410.387412-4584.456974j V\",\"voltage_B\":\"-7198.592139-3270.308373j V\",\"voltage_C\":\"642.547265+7539.531175j V\"},\"nd_190-8581\":{\"voltage_A\":\"6485.244723-4692.686497j V\",\"voltage_B\":\"-7183.641236-3170.693325j V\",\"voltage_C\":\"544.875721+7443.341013j V\"},\"nd_190-8593\":{\"voltage_A\":\"6723.279163-5056.725836j V\",\"voltage_B\":\"-7494.205738-3101.034603j V\",\"voltage_C\":\"630.475858+7534.534976j V\"},\"nd__hvmv_sub_lsb\":{\"voltage_A\":\"6261.474438-3926.148203j V\",\"voltage_B\":\"-6529.409296-3466.545236j V\",\"voltage_C\":\"247.131623+7348.295282j V\"},\"nd_l2673313\":{\"voltage_A\":\"6569.522313-5003.052614j V\",\"voltage_B\":\"-7431.486583-3004.840141j V\",\"voltage_C\":\"644.553332+7464.115913j V\"},\"nd_l2876814\":{\"voltage_A\":\"6593.064916-5014.031802j V\",\"voltage_B\":\"-7430.572725-3003.995539j V\",\"voltage_C\":\"643.473398+7483.558764j V\"},\"nd_l2955047\":{\"voltage_A\":\"5850.305847-4217.166594j V\",\"voltage_B\":\"-6729.652722-2987.617377j V\",\"voltage_C\":\"535.302084+7395.127354j V\"},\"nd_l3160107\":{\"voltage_A\":\"5954.507575-4227.423005j V\",\"voltage_B\":\"-6662.357613-3055.346880j V\",\"voltage_C\":\"600.213658+7317.832959j V\"},\"nd_l3254238\":{\"voltage_A\":\"6271.490549-4631.254028j V\",\"voltage_B\":\"-7169.987847-3099.952684j V\",\"voltage_C\":\"751.609656+7519.062259j V\"},\"nd_m1047574\":{\"voltage_A\":\"6306.632407-4741.568925j V\",\"voltage_B\":\"-7214.626338-2987.055915j V\",\"voltage_C\":\"622.058712+7442.125123j V\"},\"rcon_FEEDER_REG\":{\"Control\":\"MANUAL\",\"PT_phase\":\"CBA\",\"band_center\":126.5,\"band_width\":2.0,\"connect_type\":\"WYE_WYE\",\"control_level\":\"INDIVIDUAL\",\"dwell_time\":15.0,\"lower_taps\":16,\"raise_taps\":16,\"regulation\":0.10000000000000001},\"rcon_VREG2\":{\"Control\":\"MANUAL\",\"PT_phase\":\"CBA\",\"band_center\":125.0,\"band_width\":2.0,\"connect_type\":\"WYE_WYE\",\"control_level\":\"INDIVIDUAL\",\"dwell_time\":15.0,\"lower_taps\":16,\"raise_taps\":16,\"regulation\":0.10000000000000001},\"rcon_VREG3\":{\"Control\":\"MANUAL\",\"PT_phase\":\"CBA\",\"band_center\":125.0,\"band_width\":2.0,\"connect_type\":\"WYE_WYE\",\"control_level\":\"INDIVIDUAL\",\"dwell_time\":15.0,\"lower_taps\":16,\"raise_taps\":16,\"regulation\":0.10000000000000001},\"rcon_VREG4\":{\"Control\":\"MANUAL\",\"PT_phase\":\"CBA\",\"band_center\":125.0,\"band_width\":2.0,\"connect_type\":\"WYE_WYE\",\"control_level\":\"INDIVIDUAL\",\"dwell_time\":15.0,\"lower_taps\":16,\"raise_taps\":16,\"regulation\":0.10000000000000001},\"reg_FEEDER_REG\":{\"configuration\":\"rcon_FEEDER_REG\",\"phases\":\"ABC\",\"tap_A\":2,\"tap_B\":2,\"tap_C\":1,\"to\":\"nd__hvmv_sub_lsb\"},\"reg_VREG2\":{\"configuration\":\"rcon_VREG2\",\"phases\":\"ABC\",\"tap_A\":10,\"tap_B\":6,\"tap_C\":2,\"to\":\"nd_190-8593\"},\"reg_VREG3\":{\"configuration\":\"rcon_VREG3\",\"phases\":\"ABC\",\"tap_A\":16,\"tap_B\":10,\"tap_C\":1,\"to\":\"nd_190-8581\"},\"reg_VREG4\":{\"configuration\":\"rcon_VREG4\",\"phases\":\"ABC\",\"tap_A\":12,\"tap_B\":12,\"tap_C\":5,\"to\":\"nd_190-7361\"},\"xf_hvmv_sub\":{\"power_in_A\":\"1739729.120858-774784.929430j VA\",\"power_in_B\":\"1659762.622463-785218.730666j VA\",\"power_in_C\":\"1709521.679515-849734.584043j VA\"}}}\n, \"command\": \"nextTimeStep\"}";
			JsonObject jsonObject = compareResults.getSimulationOutput(str_out);
	
	//		String path = "./applications/python/sim_output_object.json";
			String expectedOutputPath = "./applications/python/expected_output.json";
	//		SimulationOutput simOutProperties = compareResults.getOutputProperties(path);
			Map<String, JsonElement> expectedOutputMap = getExpectedOutputMap(expectedOutputPath);
	
	//		Map<String, List<String>> propMap = simOutProperties.getOutputObjects().stream()
	//				.collect(Collectors.toMap(SimulationOutputObject::getName, e -> e.getProperties()));
			compareResults.compareExpectedWithSimulation(expectedOutputMap, jsonObject);
			
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
