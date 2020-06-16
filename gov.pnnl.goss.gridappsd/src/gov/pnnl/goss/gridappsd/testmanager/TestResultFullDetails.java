package gov.pnnl.goss.gridappsd.testmanager;

public class TestResultFullDetails extends TestResultDetails {
	
	private String object;
	
	private String attribute;
	
	private long indexOne;
	
	private long indexTwo;
	
	private long simulationTimestamp;
	

	public String getObject() {
		return object;
	}


	public void setObject(String object) {
		this.object = object;
	}


	public String getAttribute() {
		return attribute;
	}


	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}


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


	public TestResultFullDetails(String expected, String actual, String diff_mrid, String diff_type, Boolean match) {
		super(expected, actual, diff_mrid, diff_type, match);
		// TODO Auto-generated constructor stub
	}


	public TestResultFullDetails(TestResultDetails value) {
		super(value.getExpected(), value.getActual(), value.getDiffMrid(), value.getDiffType(), value.getMatch());
	}

}
