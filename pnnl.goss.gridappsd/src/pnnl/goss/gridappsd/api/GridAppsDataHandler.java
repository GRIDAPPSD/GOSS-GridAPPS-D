package pnnl.goss.gridappsd.api;

import java.io.Serializable;
import java.util.List;

import pnnl.goss.core.Response;

public interface GridAppsDataHandler {
	public Response handle(Serializable request, int simulationId, String tempDataPath) throws Exception;
	
	public String getDescription();
	
	public List<Class<?>> getSupportedRequestTypes();


}
