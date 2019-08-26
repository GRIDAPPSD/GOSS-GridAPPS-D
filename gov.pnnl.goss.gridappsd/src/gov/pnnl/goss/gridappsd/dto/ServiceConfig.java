package gov.pnnl.goss.gridappsd.dto;

import java.io.Serializable;
import java.util.HashMap;

public class ServiceConfig implements Serializable {
	
	private static final long serialVersionUID = -2413334775260242364L;
	
	String id;
	HashMap<String,UserInput> user_input;
	

}
