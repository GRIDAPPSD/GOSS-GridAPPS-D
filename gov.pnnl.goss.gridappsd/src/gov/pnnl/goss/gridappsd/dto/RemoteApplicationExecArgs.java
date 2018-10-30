package gov.pnnl.goss.gridappsd.dto;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class RemoteApplicationExecArgs {
	public String command;
	
	public RemoteApplicationExecArgs() {
		
	}
	

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}
	
	public static RemoteApplicationExecArgs parse(String jsonString){
		Gson  gson = new Gson();
		RemoteApplicationExecArgs obj = gson.fromJson(jsonString, RemoteApplicationExecArgs.class);
		if(obj.command==null)
			throw new JsonSyntaxException("Expected attribute command not found");
		return obj;
	}
}
