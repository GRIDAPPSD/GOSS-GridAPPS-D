package pnnl.goss.gridappsd.testmanager;

import java.io.Serializable;
import java.util.Arrays;

import gov.pnnl.goss.gridappsd.api.TestScript;

public class TestScriptImpl implements TestScript, Serializable {

	private static final long serialVersionUID = 1L;

	public String name;

	private String test_configuration;

	private String application;
	
	private String[] events;

	public TestScriptImpl() {

	}

	@Override
	public String toString() {
		return "TestScript \n[name=" + name + " \n, test_configuration=" + test_configuration + " \n, application="
				+ application + " \n, events=" + Arrays.toString(events) + "]";
	}

}
