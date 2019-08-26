package gov.pnnl.goss.gridappsd.dto;

import java.io.Serializable;

public class UserInput implements Serializable {

	private static final long serialVersionUID = 1L;
	
	String name;
	String help;
	String type;
	Object help_example;
	Object default_value;
	
}
