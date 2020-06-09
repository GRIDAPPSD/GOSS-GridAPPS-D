package gov.pnnl.goss.gridappsd.testmanager;

public class TestResultDetails {
	public String expected;
	public String actual;
	public String diff_mrid;
	public String diff_type;
	public Boolean match;
	
	public TestResultDetails(String expected, String actual, String diff_mrid, String diff_type, Boolean match) {
		this.expected = expected;
		this.actual = actual;
		this.diff_mrid = diff_mrid;
		this.diff_type = diff_type;
		this.match = match;
	}
	
	public String getExpected() {
		return expected;
	}
	public void setExpected(String expected) {
		this.expected = expected;
	}
	public String getActual() {
		return actual;
	}
	public void setActual(String actual) {
		this.actual = actual;
	}
	public String getDiff_mrid() {
		return diff_mrid;
	}
	public void setDiff_mrid(String diff_mrid) {
		this.diff_mrid = diff_mrid;
	}
	public String getDiff_type() {
		return diff_type;
	}
	public void setDiff_type(String diff_type) {
		this.diff_type = diff_type;
	}
	public Boolean getMatch() {
		return match;
	}
	public void setMatch(Boolean match) {
		this.match = match;
	}


}
