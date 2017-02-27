package pnnl.goss.gridappsd.data;


import pnnl.goss.gridappsd.api.DataManager;
import pnnl.goss.gridappsd.api.StatusReporter;

import java.io.Serializable;
import java.util.List;
public class DataManagerImpl implements DataManager {

import pnnl.goss.core.Response;
import pnnl.goss.gridappsd.data.handlers.GridAppsDataHandler;


public interface DataManager {
	
	@ServiceDependency
	private volatile StatusReporter statusReporter;
	
	public List<GridAppsDataHandler> getHandlers(Class<?> requestClass);
	
	public GridAppsDataHandler getHandler(Class<?> requestClass, Class<?> handlerClass);
	
	public List<GridAppsDataHandler> getAllHandlers();
	
	public void registerHandler(GridAppsDataHandler handler, Class<?> requestClass);

	public Response processRequest(Serializable request);
	
	
}
