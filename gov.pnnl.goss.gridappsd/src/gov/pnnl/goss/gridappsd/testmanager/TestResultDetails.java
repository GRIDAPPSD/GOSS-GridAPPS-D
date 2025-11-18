package gov.pnnl.goss.gridappsd.testmanager;

public class TestResultDetails {
    private String expected;
    private String actual;
    private String diffMrid;
    private String diffType;
    private Boolean match;

    public TestResultDetails(String expected, String actual, String diff_mrid, String diff_type, Boolean match) {
        this.expected = expected;
        this.actual = actual;
        this.diffMrid = diff_mrid;
        this.diffType = diff_type;
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

    public String getDiffMrid() {
        return diffMrid;
    }

    public void setDiffMrid(String diff_mrid) {
        this.diffMrid = diff_mrid;
    }

    public String getDiffType() {
        return diffType;
    }

    public void setDiffType(String diff_type) {
        this.diffType = diff_type;
    }

    public Boolean getMatch() {
        return match;
    }

    public void setMatch(Boolean match) {
        this.match = match;
    }

}
