package pnnl.goss.gridappsd.data.handlers;

import java.io.Serializable;
import java.util.List;

import pnnl.goss.core.Response;

public interface GridAppsDataHandler {
	public Response handle(Serializable request);
	
	public String getDescription();
	
	public List<Class<?>> getSupportedRequestTypes();


}
