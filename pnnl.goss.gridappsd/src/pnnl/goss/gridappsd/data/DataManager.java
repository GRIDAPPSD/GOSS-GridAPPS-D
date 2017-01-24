package pnnl.goss.gridappsd.data;



import java.io.Serializable;
import java.util.List;

import pnnl.goss.core.Response;
import pnnl.goss.gridappsd.data.handlers.GridAppsDataHandler;


public interface DataManager {
	
	public List<GridAppsDataHandler> getHandlers(Class<?> requestClass);
	
	public GridAppsDataHandler getHandler(Class<?> requestClass, Class<?> handlerClass);
	
	public List<GridAppsDataHandler> getAllHandlers();
	
	public void registerHandler(GridAppsDataHandler handler, Class<?> requestClass);

	public Response processRequest(Serializable request);
	
	
}
