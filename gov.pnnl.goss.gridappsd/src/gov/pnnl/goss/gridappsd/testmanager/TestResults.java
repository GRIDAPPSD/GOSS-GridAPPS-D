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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Class to keep track of the test result differences.
 * @author jsimpson
 *
 */
public class TestResults implements Serializable{
	
	private static final long serialVersionUID = -6171952609185593742L;
	
	long simulationTimestamp;
	
	public Map<String, HashMap<String, String[]>> objectPropComparison = new HashMap<String, HashMap<String, String[]>>();
	
	public HashMap<String, String[]> add(String obj, String prop){
		HashMap<String, String[]> prop1 ;
		if (objectPropComparison.containsKey(obj)){
			prop1  = objectPropComparison.get(obj);
		} else {
			prop1 = new HashMap<String, String[]>();
			objectPropComparison.put(obj, prop1 );
		}
		return prop1;
	}
	
	public void add(String obj, String prop, String expected, String actual){
		HashMap<String, String[]> prop1 = add(obj,prop);
		String [] x = {expected, actual, "NA", "NA"};
		prop1.put(prop, x);
	}
	
	public void add(String obj, String prop, String expected, String actual, String diff_mrid, String diff_type ){
		HashMap<String, String[]> prop1 = add(obj,prop);
		String [] x = {expected, actual, diff_mrid, diff_type};
		prop1.put(prop, x);
	}
	
	public int getNumberOfConflicts(){
		int count = 0;
		for (Entry<String, HashMap<String, String[]>> entry : objectPropComparison.entrySet()) {
			HashMap<String, String[]> propMap = entry.getValue();
			count+=propMap.size();
		}
		return count;	
	}	
	
	public void pprint() {
		for (Entry<String, HashMap<String, String[]>> entry : objectPropComparison.entrySet()) {
			HashMap<String, String[]> propMap = entry.getValue();
			for (Entry<String, String[]> prop: propMap.entrySet()){
				System.out.println(entry.getKey() + "." + prop.getKey());
				System.out.println("    Expected:" + prop.getValue()[0] );
				System.out.println("    Actual  :" + prop.getValue()[1] );
			}
		}
		System.out.println("Total conflicts "+ getNumberOfConflicts());
	}
	
	@Override
	public String toString() {
		String temp = "";
		for (Entry<String, HashMap<String, String[]>> entry : objectPropComparison.entrySet()) {
			HashMap<String, String[]> propMap = entry.getValue();
			for (Entry<String, String[]> prop: propMap.entrySet()){
//				temp+="\t"+entry.getKey() + "    " + prop.getKey()+ "    " + prop.getValue()[0] +"    " + prop.getValue()[1];
				temp += String.format("\t %37s %10s %.3f %.3f ", entry.getKey(), prop.getKey(), Double.parseDouble(prop.getValue()[0]), Double.parseDouble(prop.getValue()[1]));
				if(prop.getValue().length > 3)
//					temp+="    " +prop.getValue()[2] +"    " + prop.getValue()[3];
					temp += String.format("%10s %37s", prop.getValue()[2], prop.getValue()[3]);
				temp+="\n";
			}
		}
		return temp;
	}
	
	public static void main(String[] args) {
		TestResults tr = new TestResults();
		HashMap<String, String[]> prop1 = new HashMap<String, String[]>();
		String [] x = {"3","10"};
		prop1.put("tap_A", x );
		tr.objectPropComparison.put("reg_VREG2",prop1);	
		
		tr.add("reg_VREG1", "tap_A", "3", "10");
		tr.add("reg_VREG1", "tap_B", "3", "10");
		tr.pprint();
	}

}
