package gov.pnnl.goss.gridappsd.testmanager;

public class TestResultFullDetails extends TestResultDetails {
	
	String object;
	String attribute;
	long indexOne;
	long indexTwo;
	long simulationTimestamp;
	

	public TestResultFullDetails(String expected, String actual, String diff_mrid, String diff_type, Boolean match) {
		super(expected, actual, diff_mrid, diff_type, match);
		// TODO Auto-generated constructor stub
	}


	public TestResultFullDetails(TestResultDetails value) {
		super(value.expected, value.actual, value.diff_mrid, value.diff_type, value.match);
	}

}
