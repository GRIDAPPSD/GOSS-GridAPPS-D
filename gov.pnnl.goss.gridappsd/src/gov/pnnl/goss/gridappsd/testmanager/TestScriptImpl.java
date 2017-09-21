package gov.pnnl.goss.gridappsd.testmanager;

import java.io.Serializable;

import com.google.gson.Gson;

import gov.pnnl.goss.gridappsd.api.TestScript;

public class TestScriptImpl implements TestScript, Serializable {

	private static final long serialVersionUID = 1L;

	public String name;

	private String test_configuration;

	private String application;
	
//	private String[] events;

	public TestScriptImpl() {

	}

//	@Override
//	public String toString() {
//		return "TestScript \n[name=" + name + " \n, test_configuration=" + test_configuration + " \n, application="
//				+ application+  "]";
//	}

	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}
	
	public static TestScriptImpl parse(String jsonString){
		Gson  gson = new Gson();
		TestScriptImpl obj = gson.fromJson(jsonString, TestScriptImpl.class);
		if(obj.name==null)
			throw new RuntimeException("Expected attribute name not found");
		return obj;
	}
}
