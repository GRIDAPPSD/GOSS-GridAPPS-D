/*******************************************************************************
 * Copyright (c) 2017, Battelle Memorial Institute All rights reserved.
 * Battelle Memorial Institute (hereinafter Battelle) hereby grants permission to any person or entity 
 * lawfully obtaining a copy of this software and associated documentation files (hereinafter the 
 * Software) to redistribute and use the Software in source and binary forms, with or without modification. 
 * Such person or entity may use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Software, and may permit others to do so, subject to the following conditions:
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the 
 * following disclaimers.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and 
 * the following disclaimer in the documentation and/or other materials provided with the distribution.
 * Other than as used herein, neither the name Battelle Memorial Institute or Battelle may be used in any 
 * form whatsoever without the express written consent of Battelle.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL 
 * BATTELLE OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, 
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE 
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED 
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED 
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * General disclaimer for use with OSS licenses
 * 
 * This material was prepared as an account of work sponsored by an agency of the United States Government. 
 * Neither the United States Government nor the United States Department of Energy, nor Battelle, nor any 
 * of their employees, nor any jurisdiction or organization that has cooperated in the development of these 
 * materials, makes any warranty, express or implied, or assumes any legal liability or responsibility for 
 * the accuracy, completeness, or usefulness or any information, apparatus, product, software, or process 
 * disclosed, or represents that its use would not infringe privately owned rights.
 * 
 * Reference herein to any specific commercial product, process, or service by trade name, trademark, manufacturer, 
 * or otherwise does not necessarily constitute or imply its endorsement, recommendation, or favoring by the United 
 * States Government or any agency thereof, or Battelle Memorial Institute. The views and opinions of authors expressed 
 * herein do not necessarily state or reflect those of the United States Government or any agency thereof.
 * 
 * PACIFIC NORTHWEST NATIONAL LABORATORY operated by BATTELLE for the 
 * UNITED STATES DEPARTMENT OF ENERGY under Contract DE-AC05-76RL01830
 ******************************************************************************/
package gov.pnnl.goss.gridappsd.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;

import pnnl.goss.core.Client;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.api.DataManagerHandler;
import gov.pnnl.goss.gridappsd.api.GridAppsDataHandler;
import gov.pnnl.goss.gridappsd.api.LogManager;

/**
 * This represents Internal Function 405 Simulation Control Manager.
 * This is the management function that controls the running/execution of the Distribution Simulator (401).
 * @author shar064 
 */

@Component
public class DataManagerImpl implements DataManager {
	
	private Map<Class<?>, List<GridAppsDataHandler>> handlers = new HashMap<Class<?>, List<GridAppsDataHandler>>();
	private Map<String, DataManagerHandler> dataManagers = new HashMap<String, DataManagerHandler>();
    private Logger log = LoggerFactory.getLogger(getClass());
    
	Client client = null; 
	
	@ServiceDependency
	private volatile ClientFactory clientFactory;
	@ServiceDependency
	private volatile LogManager logManager;
	
	public DataManagerImpl(ClientFactory clientFactory, LogManager logManager){
		this.clientFactory = clientFactory;
		this.logManager = logManager;
	}
	public DataManagerImpl(){
		
	}
	
	@Start
	public void start(){
		log.info("Starting "+getClass());
//		try{
//			Credentials credentials = new UsernamePasswordCredentials(
//					GridAppsDConstants.username, GridAppsDConstants.password);
//			client = clientFactory.create(PROTOCOL.STOMP,credentials);
//			
//			client.subscribe(GridAppsDConstants.topic_requestData, new DataEvent(this));
//			
//			
//		}
//		catch(Exception e){
//				e.printStackTrace();
//		}
		
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
	public void registerDataManagerHandler(DataManagerHandler handler, String name){
		//TODO Should it support multiple handlers per request type???
		log.info("Data Manager Handler Registration: "+handler.getClass()+" - "+name);
		this.dataManagers.put(name, handler);
	}

	@Override
	public Response processDataRequest(Serializable request, String type, int simulationId, String tempDataPath) throws Exception {
				
		
		if(request!=null && type!=null){
			DataResponse r = new DataResponse();
			Serializable responseData = null;
			if(dataManagers.containsKey(type)){
				responseData = dataManagers.get(type).handle(request);
			} else {
				System.out.println("TYPE NOT SUPPORTED");
				//TODO throw error that type not supported
			}
			r.setData(responseData);
			r.setResponseComplete(true);
			return r;
		}
			
		
		//TODO this will be phased out
		List<GridAppsDataHandler> handlers = getHandlers(request.getClass());
		if(handlers!=null){
			//iterate through all handlers until we get one with a result
			for(GridAppsDataHandler handler: handlers){
				//datahandler.handle
				Response r = handler.handle(request, simulationId, tempDataPath, logManager);
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
