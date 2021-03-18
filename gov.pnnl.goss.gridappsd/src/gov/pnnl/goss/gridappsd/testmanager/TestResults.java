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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.Gson;

/**
 * Class to keep track of the test result differences.
 * @author jsimpson
 *
 */
public class TestResults implements Serializable{
	
	private static final long serialVersionUID = -6171952609185593742L;
	private long indexOne;
	private long indexTwo;
	private long simulationTimestamp;

	private Map<String, HashMap<String, TestResultDetails>> objectPropComparison = new HashMap<String, HashMap<String, TestResultDetails>>();
	
	public long getIndexOne() {
		return indexOne;
	}

	public void setIndexOne(long indexOne) {
		this.indexOne = indexOne;
	}

	public long getIndexTwo() {
		return indexTwo;
	}

	public void setIndexTwo(long indexTwo) {
		this.indexTwo = indexTwo;
	}

	public long getSimulationTimestamp() {
		return simulationTimestamp;
	}

	public void setSimulationTimestamp(long simulationTimestamp) {
		this.simulationTimestamp = simulationTimestamp;
	}
	
	public Map<String, HashMap<String, TestResultDetails>> getObjectPropComparison() {
		return objectPropComparison;
	}

	public void setObjectPropComparison(Map<String, HashMap<String, TestResultDetails>> objectPropComparison) {
		this.objectPropComparison = objectPropComparison;
	}

	public HashMap<String, TestResultDetails> add(String obj, String prop){
		HashMap<String, TestResultDetails> prop1 ;
		if (objectPropComparison.containsKey(obj)){
			prop1  = objectPropComparison.get(obj);
		} else {
			prop1 = new HashMap<String, TestResultDetails>();
			objectPropComparison.put(obj, prop1 );
		}
		return prop1;
	}
	
	public void add(String obj, String prop, String expected, String actual){
		HashMap<String, TestResultDetails> prop1 = add(obj,prop);
//		String [] x = {expected, actual, "NA", "NA"};
		TestResultDetails trd = new TestResultDetails(expected, actual, "NA", "NA", false);
		prop1.put(prop, trd);
	}
	
	public void add(String obj, String prop, String expected, String actual, Boolean match){
		HashMap<String, TestResultDetails> prop1 = add(obj,prop);
//		String [] x = {expected, actual, "NA", "NA"};
		TestResultDetails trd = new TestResultDetails(expected, actual, "NA", "NA", match);
		prop1.put(prop, trd);
	}
	
	public void add(String obj, String prop, String expected, String actual, String diff_mrid, String diff_type ){
		HashMap<String, TestResultDetails> prop1 = add(obj,prop);
		TestResultDetails trd = new TestResultDetails(expected, actual, diff_mrid, diff_type, false);
		prop1.put(prop, trd);
	}
	
	public void add(String obj, String prop, String expected, String actual, String diff_mrid, String diff_type, Boolean match){
		HashMap<String, TestResultDetails> prop1 = add(obj,prop);
		TestResultDetails trd = new TestResultDetails(expected, actual, diff_mrid, diff_type, match);
		prop1.put(prop, trd);
	}
	
	public int getNumberOfConflicts(){
		int count = 0;
		for (Entry<String, HashMap<String, TestResultDetails>> entry : objectPropComparison.entrySet()) {
			HashMap<String, TestResultDetails> propMap = entry.getValue();
			for (Entry<String, TestResultDetails> iterable_element : propMap.entrySet()) {
				if(! iterable_element.getValue().getMatch()){
					count++;
				}
			}
		}
		return count;	
	}	
	
	public void pprint() {
		for (Entry<String, HashMap<String, TestResultDetails>> entry : objectPropComparison.entrySet()) {
			HashMap<String, TestResultDetails> propMap = entry.getValue();
			for (Entry<String, TestResultDetails> prop: propMap.entrySet()){
				System.out.println(entry.getKey() + "." + prop.getKey());
				System.out.println("    Expected:" + prop.getValue().getExpected() );
				System.out.println("    Actual  :" + prop.getValue().getActual() );
			}
		}
		System.out.println("Total conflicts "+ getNumberOfConflicts());
	}
	
	@Override
	public String toString() {
		String temp = "";
		for (Entry<String, HashMap<String, TestResultDetails>> entry : objectPropComparison.entrySet()) {
			HashMap<String, TestResultDetails> propMap = entry.getValue();
			for (Entry<String, TestResultDetails> prop: propMap.entrySet()){
//				temp+="\t"+entry.getKey() + "    " + prop.getKey()+ "    " + prop.getValue()[0] +"    " + prop.getValue()[1];
//				temp += String.format("\t %37s %10s %.3f %.3f ", entry.getKey(), prop.getKey(), Double.parseDouble(prop.getValue().getExpected() ), Double.parseDouble(prop.getValue().getActual()));
				temp += String.format("\t %37s %10s %10s %10s ", entry.getKey(), prop.getKey(), prop.getValue().getExpected(), prop.getValue().getActual());

//				if(prop.getValue().length > 3)
//					temp+="    " +prop.getValue()[2] +"    " + prop.getValue()[3];
					temp += String.format("%10s %37s", prop.getValue().getDiffMrid(), prop.getValue().getDiffType());
				temp+="\n";
			}
		}
		return temp;
	}
	
	public String toJson(Boolean storeMatches) {
//		Boolean storeMatches = true;
		TestResultFullDetails trfd = null;
			for (Entry<String, HashMap<String, TestResultDetails>> entry : getObjectPropComparison().entrySet()) {
				HashMap<String, TestResultDetails> propMap = entry.getValue();
				for (Entry<String, TestResultDetails> prop: propMap.entrySet()){
					trfd = new TestResultFullDetails(prop.getValue());
					trfd.setObject(entry.getKey());
					trfd.setAttribute(prop.getKey());
					trfd.setIndexOne(indexOne);
					trfd.setIndexTwo(indexTwo);
//					if(! storeMatches && trfd.getMatch())
//						list.add(trfd);
				}
		}
		if (storeMatches == false && trfd.getMatch() == true){
			return "";
		}
		Gson  gson = new Gson();
		return gson.toJson(trfd);
	}
	
	public String toJson() {
		TestResultFullDetails trfd = null;
			for (Entry<String, HashMap<String, TestResultDetails>> entry : getObjectPropComparison().entrySet()) {
				HashMap<String, TestResultDetails> propMap = entry.getValue();
				for (Entry<String, TestResultDetails> prop: propMap.entrySet()){
					trfd = new TestResultFullDetails(prop.getValue());
					trfd.setObject(entry.getKey());
					trfd.setAttribute(prop.getKey());
					trfd.setIndexOne(indexOne);
					trfd.setIndexTwo(indexTwo);
//					if(! storeMatches && trfd.getMatch())
//						list.add(trfd);
				}
		}
			
		Gson  gson = new Gson();
		return gson.toJson(trfd);
	}
	
	public static void main(String[] args) {
		TestResults tr = new TestResults();
//		HashMap<String, String[]> prop1 = new HashMap<String, String[]>();
//		TestResultDetails x =  new TestResultDetails(expected, actual, diff_mrid, diff_type, false);
//		prop1.put("tap_A", x );
//		tr.objectPropComparison.put("reg_VREG2",x);	
		
		tr.add("reg_VREG1", "tap_A", "3", "10");
		tr.add("reg_VREG1", "tap_B", "3", "10");
		tr.pprint();
	}

}
