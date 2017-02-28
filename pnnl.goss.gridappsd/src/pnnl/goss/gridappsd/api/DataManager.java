package pnnl.goss.gridappsd.api;

import java.io.Serializable;
import java.util.List;

import pnnl.goss.core.Response;

public interface DataManager {
	
	List<GridAppsDataHandler> getHandlers(Class<?> requestClass);
	
	GridAppsDataHandler getHandler(Class<?> requestClass, Class<?> handlerClass);
	
	List<GridAppsDataHandler> getAllHandlers();
	
	void registerHandler(GridAppsDataHandler handler, Class<?> requestClass);
	
	Response processDataRequest(Serializable request);
}
