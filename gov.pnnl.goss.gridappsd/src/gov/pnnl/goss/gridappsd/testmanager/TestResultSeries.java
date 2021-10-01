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

public class TestResultSeries implements Serializable{
	
	private static final long serialVersionUID = -5368089783896803654L;
	
	public HashMap<Map<String,String>, TestResults> results = new HashMap<Map<String,String>, TestResults>();
	
	public void add(String index1, String index2, TestResults testResults){
		HashMap <String,String> index = new HashMap<String,String>();
		index.put(index1, index2);
		if(results.containsKey(index)){
			TestResults tr = results.get(index);
			for (Entry<String, HashMap<String, TestResultDetails>> entry : testResults.getObjectPropComparison().entrySet()) {
				tr.getObjectPropComparison().put(entry.getKey(), entry.getValue());
//				System.out.println(entry.getKey());
//				System.out.println(entry.getValue());
//				for (Entry<String, String[]> entry2 : entry.getValue().entrySet()) {
//					System.out.println(entry2.getKey());
//					System.out.println(entry2.getValue()[0]);
//					System.out.println(entry2.getValue()[1]);
//					tr.add(entry.getKey() , entry2.getKey(),entry2.getValue()[0], entry2.getValue()[1]);
//				}
			}
			
		}else
			results.put(index, testResults);
	}
	
	public int getTotal(){
		int total=0;
		for (Entry<Map<String,String>, TestResults> iterable_element : results.entrySet()) {
			total+=iterable_element.getValue().getNumberOfConflicts();
		}
		return total;
	}
	
	public void ppprint(){
		String temp = String.format("\t %37s %30s %10s %10s ", "object MRID", "attribute", "expected", "actual" );
		temp += String.format("%10s %37s","Type", "DiffMrid");
		System.out.println(temp);
		for (Entry<Map<String,String>, TestResults> iterable_element : results.entrySet()) {
			System.out.println(iterable_element.getKey());
			System.out.println(iterable_element.getValue().toString());
		}
	}
	
	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}
	
	public String toJson(Boolean storeMatches){
		ArrayList<TestResultFullDetails> list = new ArrayList<TestResultFullDetails>();
		for (Entry<Map<String,String>, TestResults> iterable_element : results.entrySet()) {
			Map<String, String> map = iterable_element.getKey();
			Long indexOne = Long.parseLong(map.entrySet().iterator().next().getKey()); 
			Long indexTwo =Long.parseLong(map.entrySet().iterator().next().getValue());
			TestResults tr = iterable_element.getValue();
			tr.setIndexOne(indexOne);
			tr.setIndexTwo(indexTwo);
//			if(! storeMatches && tr.)
//			list.add(tr);
		}
		for (Map<String, String> simulationTime : this.results.keySet()){
			TestResults tr = this.results.get(simulationTime);
			for (Entry<String, HashMap<String, TestResultDetails>> entry : tr.getObjectPropComparison().entrySet()) {
				HashMap<String, TestResultDetails> propMap = entry.getValue();
				for (Entry<String, TestResultDetails> prop: propMap.entrySet()){
					TestResultFullDetails trfd = new TestResultFullDetails(prop.getValue());
					trfd.setObject(entry.getKey());
					trfd.setAttribute(prop.getKey());
					Long indexOne = Long.parseLong(simulationTime.entrySet().iterator().next().getKey()); 
					Long indexTwo =Long.parseLong(simulationTime.entrySet().iterator().next().getValue());
					trfd.setIndexOne(indexOne);
					trfd.setIndexTwo(indexTwo);
					if(! storeMatches && trfd.getMatch())
						list.add(trfd);
				}
			}
		}
			
		
		Gson  gson = new Gson();
		return gson.toJson(list);
	}

}
