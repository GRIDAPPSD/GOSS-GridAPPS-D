package gov.pnnl.goss.gridappsd.testmanager;

import java.util.HashMap;
import java.util.Map.Entry;

public class TestResultSeries {
	public HashMap<String, TestResults> results = new HashMap<String, TestResults>();
	
	public void add(String index, TestResults testResults){
		results.put(index, testResults);
	}
	
	public int getTotal(){
		int total=0;
		for (Entry<String, TestResults> iterable_element : results.entrySet()) {
			total+=iterable_element.getValue().getNumberOfConflicts();
		}
		return total;
	}

}
