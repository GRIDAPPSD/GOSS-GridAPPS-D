package pnnl.goss.gridappsd.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;

import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.Response;
import pnnl.goss.gridappsd.api.DataManager;
import pnnl.goss.gridappsd.api.GridAppsDataHandler;
import pnnl.goss.gridappsd.api.StatusReporter;
import pnnl.goss.gridappsd.utils.GridAppsDConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This represents Internal Function 405 Simulation Control Manager.
 * This is the management function that controls the running/execution of the Distribution Simulator (401).
 * @author shar064 
 */

@Component
public class DataManagerImpl implements DataManager {
	
	private Map<Class<?>, List<GridAppsDataHandler>> handlers = new HashMap<Class<?>, List<GridAppsDataHandler>>();
    private Logger log = LoggerFactory.getLogger(getClass());
    
	Client client = null; 
	
	@ServiceDependency
	private volatile ClientFactory clientFactory;
	
	
	@ServiceDependency
	private volatile StatusReporter statusReporter;
	
	@Start
	public void start(){
		System.out.println("STARTING DATA MANAGER");
		try{
			Credentials credentials = new UsernamePasswordCredentials(
					GridAppsDConstants.username, GridAppsDConstants.password);
			client = clientFactory.create(PROTOCOL.STOMP,credentials);
			
			client.subscribe(GridAppsDConstants.topic_requestData, new DataEvent(this));
			
			
		}
		catch(Exception e){
				e.printStackTrace();
		}
		
	}

	@Override
	public void registerHandler(GridAppsDataHandler handler, Class<?> requestClass){
		//TODO Should it support multiple handlers per request type???
		log.info("Data Manager Registration: "+handler.getClass()+" - "+requestClass);
		List<GridAppsDataHandler> typeHandlers = null;
		if(handlers.containsKey(requestClass)){
			typeHandlers = handlers.get(requestClass);
		} else {
			typeHandlers = new ArrayList<GridAppsDataHandler>();
			handlers.put(requestClass, typeHandlers);
		}
		typeHandlers.add(handler);
	}


	@Override
	public Response processDataRequest(Serializable request) {
		List<GridAppsDataHandler> handlers = getHandlers(request.getClass());
		if(handlers!=null){
			//iterate through all handlers until we get one with a result
			for(GridAppsDataHandler handler: handlers){
				//datahandler.handle
				Response r = handler.handle(request);
				if(r!=null){
					return r;
				}
				//Return result from handler
			}
		} 
		//return null if no valid results
		return null;
	}


	@Override
	public List<GridAppsDataHandler> getHandlers(Class<?> requestClass) {
		if(handlers.containsKey(requestClass)){
			log.debug("Data handler "+handlers.get(requestClass)+" found for "+requestClass);
			return handlers.get(requestClass);
		}
		log.warn("No data handler found for request type "+requestClass);
		return null;
	}


	@Override
	public List<GridAppsDataHandler> getAllHandlers() {
		List<GridAppsDataHandler> allHandlers = new ArrayList<GridAppsDataHandler>();
		for(List<GridAppsDataHandler>typeHandlers: handlers.values()){
			allHandlers.addAll(typeHandlers);
		}
		
		return allHandlers;
	}


	@Override
	public GridAppsDataHandler getHandler(Class<?> requestClass, Class<?> handlerClass) {
		// TODO Auto-generated method stub
		return null;
	}

}
