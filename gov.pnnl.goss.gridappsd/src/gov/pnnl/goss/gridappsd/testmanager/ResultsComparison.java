package gov.pnnl.goss.gridappsd.testmanager;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ResultsComparison {
	protected String name;
	protected String powerFlow;
	protected int countFalse;
	protected Map<String, String> differences = new HashMap<String,String> ();
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPowerFlow() {
		return powerFlow;
	}
	public void setPowerFlow(String powerFlow) {
		this.powerFlow = powerFlow;
	}
	public int getCountFalse() {
		return countFalse;
	}
	public void setCountFalse(int countFalse) {
		this.countFalse = countFalse;
	}
	public Map<String, String> getDifferences() {
		return differences;
	}
	public void setDifferences(Map<String, String> differences) {
		this.differences = differences;
	}
	
	public void prettyPrint(){
		System.out.println("There are " + countFalse + " differences between expected results and simulation results.");
		int count=0;
		for (Entry<String, String> entry : differences.entrySet()) {
			count++;
			System.out.println(count+": " +entry.getKey() +" not equal to " + entry.getValue());
			
		}
	}

}
