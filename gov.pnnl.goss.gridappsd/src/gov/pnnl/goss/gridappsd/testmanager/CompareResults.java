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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.complex.ComplexFormat;
import org.apache.felix.dm.annotation.api.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import gov.pnnl.goss.gridappsd.dto.SimulationOutput;
import gov.pnnl.goss.gridappsd.dto.SimulationOutputObject;

/**
*
* @author jsimpson
*
*/
@Component
public class CompareResults {
	String[] propsArray = new String[]{"connect_type", "Control", "control_level", "PT_phase", "band_center", "band_width", "dwell_time", "raise_taps", "lower_taps", "regulation"};

	private final static double EPSILON = 0.01;
	
	private final static ComplexFormat cf = new ComplexFormat("j");

	public static boolean equals(float a, float b){
	    return a == b ? true : Math.abs(a - b) < EPSILON;
	}
	
	public static boolean equals(Complex a, Complex b){
	    return a.equals(b) ? true : (a.subtract(b)).abs() < EPSILON;
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

	public SimulationOutput getOutputProperties(String path) {
		Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
		JsonReader jsonReader;
		SimulationOutput testScript = null;
		try {
			jsonReader = new JsonReader(new FileReader(path));
			jsonReader.setLenient(true);
			testScript = gson.fromJson(new FileReader(path),SimulationOutput.class);
			jsonReader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return testScript;	
	}
	
//	private Map<Object, Object> mapConfig(Map<String, Integer> input, String prefix) {
//	    return input.entrySet().stream()
//	            .collect(Collectors.toMap(entry -> entry.getKey().substring(subLength), entry -> AttributeType.GetByName(entry.getValue())));
//	}
	
	public void loadSimResultTestCode(String path, SimulationOutput simOut) {
		JsonParser parser = new JsonParser();
		try {
			BufferedReader br = new BufferedReader(new FileReader(path));
			JsonElement elm = parser.parse(br);
			if(elm.isJsonObject()){
			    JsonObject jsonObject = elm.getAsJsonObject();
			    JsonObject output = jsonObject.get("output").getAsJsonObject();
			    JsonObject f2 = output.get("ieee8500").getAsJsonObject();
	    
			    // Turn to a Map 
			    Map<String, JsonElement> ouputsMap = f2.entrySet().stream()
			                                    .collect(Collectors.toMap(Map.Entry::getKey,e -> e.getValue()));

				for (SimulationOutputObject out :simOut.output_objects){
					JsonElement outputTmp = ouputsMap.get(out.name);
					System.out.println(out.name + " " +ouputsMap.get(out.name));
					// TODO Compare here since I can look at each output and property
					if( output != null){
						for (String prop : out.getProperties()) {
							System.out.println("     "  +prop);
							System.out.println("     "  + outputTmp.getAsJsonObject().get(prop));
						}
					}
					break;
				}
			    
//			    Set<Entry<String, JsonElement>> entrySet = f2.entrySet();		    
//			    for(Map.Entry<String,JsonElement> entry : entrySet){
//			    	if(entry.getValue().isJsonObject()){
//			    		JsonObject obj = entry.getValue().getAsJsonObject();
//			    		for (String prop : propsArray) {
//				    		if (obj.has(prop)) System.out.println(prop +  " : " + obj.get(prop));	
//						} 
//
//			    	}
//			        System.out.println("key >>> "+entry.getKey());
//			        System.out.println("val >>> "+entry.getValue());
//			    }

			}		
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	/**
	 * compareExpectedWithSimulation
	 * @param simOutputPath
	 * @param expectedOutputPath
	 * @param simOutProperties
	 */
	public int compareExpectedWithSimulation(String simOutputPath, String expectedOutputPath, SimulationOutput simOutProperties) {
		int countTrue = 0;
		int countFalse = 0;

		Map<String, JsonElement> expectedOutputMap = getExpectedOutputMap(expectedOutputPath);

		Map<String, List<String>> propMap = simOutProperties.getOutputObjects().stream()
				.collect(Collectors.toMap(SimulationOutputObject::getName, e -> e.getProperties()));
		JsonObject jsonObject = getSimulationOutput(simOutputPath);
		JsonObject output = jsonObject.get("output").getAsJsonObject();
		JsonObject simOutput = output.get("ieee8500").getAsJsonObject();

		if (jsonObject != null) {
			Set<Entry<String, JsonElement>> simOutputSet = simOutput.entrySet();
			for (Map.Entry<String, JsonElement> simOutputElement : simOutputSet) {
				System.out.println(simOutputElement);
				if (simOutputElement.getValue().isJsonObject()) {
					JsonObject simOutputObj = simOutputElement.getValue().getAsJsonObject();
					JsonObject expectedOutputttObj = expectedOutputMap.get(simOutputElement.getKey()).getAsJsonObject();
					List<String> propsArray = propMap.get(simOutputElement.getKey());
					for (String prop : propsArray) {
						if (simOutputObj.has(prop) && expectedOutputttObj.has(prop)) {
							Boolean comparison = compareObjectProperties(simOutputObj, expectedOutputttObj, prop);
							if (comparison)
								countTrue++;
							else
								countFalse++;

						} else
							System.out.println("No property");
					}
				} else
					System.out.println("     Not object" + simOutputElement);
			}
		}
		System.out.println("Number of equals : " + countTrue + " Number of not equals : " + countFalse);
		return countFalse;
	}

	/**
	 * 
	 * @param simOutputPath
	 * @return
	 */
	private JsonObject getSimulationOutput(String simOutputPath) {
		JsonObject jsonObject = null;
		try {
			JsonParser parser = new JsonParser();
			JsonElement simOutputObject = parser.parse(new BufferedReader(new FileReader(simOutputPath)));

			if (simOutputObject.isJsonObject()) {
				jsonObject = simOutputObject.getAsJsonObject();
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return jsonObject;
	}

	/**
	 * Get the map of the expected outputs
	 * @param expectedOutputPath
	 * @return
	 * @throws FileNotFoundException
	 */
	private Map<String, JsonElement> getExpectedOutputMap(String expectedOutputPath) {
		Map<String, JsonElement> expectedOutputMap = null;
		try {
			JsonParser parser = new JsonParser();
			JsonElement expectedOutputObj = parser.parse(new BufferedReader(new FileReader(expectedOutputPath)));
			if (expectedOutputObj.isJsonObject()) {
				JsonObject jsonObject = expectedOutputObj.getAsJsonObject();
				JsonObject output = jsonObject.get("expected_output").getAsJsonObject();
				JsonObject outputs = output.get("ieee8500").getAsJsonObject();
				expectedOutputMap = outputs.entrySet().stream()
						.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()));
				System.out.println("     Lookup expected" + expectedOutputMap.get("rcon_VREG3"));

			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return expectedOutputMap;
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
		System.out.println("     " + prop +  " : " + simOutputObj.get(prop) + " == " +  expectedOutputObj.get(prop) + " is " + comparison);
		// Test voltage, i.e. complex number
		if(prop.startsWith("voltage_") || prop.startsWith("power_in_") ){
			Complex c1 = cf.parse(simOutputObj.get(prop).getAsString());
			Complex c2 = cf.parse(expectedOutputObj.get(prop).getAsString());
			System.out.println("              Complex" + c1 + c2 + equals(c1,c2));
			comparison = equals(c1,c2);
		}
		
		return comparison;
	}
	
	public void loadSimResult_old(String sim_output, SimulationOutput simOutProperties, String expected_output) {
		JsonParser parser = new JsonParser();
		try {
			BufferedReader br = new BufferedReader(new FileReader(sim_output));
			JsonElement elm = parser.parse(br);
			if(elm.isJsonObject()){
			    JsonObject jsonObject = elm.getAsJsonObject();

			    JsonObject output = jsonObject.get("output").getAsJsonObject();

			    JsonObject f2 = output.get("ieee8500").getAsJsonObject();

			    Set<Entry<String, JsonElement>> entrySet = f2.entrySet();
			    for(Map.Entry<String,JsonElement> simOutputEle : entrySet){
			    	if(simOutputEle.getValue().isJsonObject()){
			    		JsonObject simOutputObj = simOutputEle.getValue().getAsJsonObject();
			    		for (String prop : propsArray) {
				    		if (simOutputObj.has(prop)) System.out.println(prop +  " : " + simOutputObj.get(prop));	
						} 

			    	}
			        System.out.println("key >>> "+simOutputEle.getKey());
			        System.out.println("val >>> "+simOutputEle.getValue());
			    }

			    if(f2.isJsonObject()){
			        JsonObject f2Obj = f2.getAsJsonObject();
			        JsonElement f3 = f2Obj.get("f3");
			    }

			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		
		CompareResults compareResults = new CompareResults();

		SimulationOutput simOutProperties = compareResults.getOutputProperties(path);
		compareResults.getProp(simOutProperties);
		compareResults.compareExpectedWithSimulation(sim_output, expected_output, simOutProperties);
	}


	private void getProp(SimulationOutput simOut) {
		for (SimulationOutputObject out :simOut.output_objects){
			System.out.println(out.name);
			out.getProperties();			
		}
	}

}
