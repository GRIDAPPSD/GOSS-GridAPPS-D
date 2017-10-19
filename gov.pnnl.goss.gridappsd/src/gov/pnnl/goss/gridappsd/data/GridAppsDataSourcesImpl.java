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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pnnl.goss.core.server.DataSourceBuilder;
import pnnl.goss.core.server.DataSourcePooledJdbc;
import pnnl.goss.core.server.DataSourceRegistry;

@Component
public class GridAppsDataSourcesImpl implements GridAppsDataSources{
	private static final String CONFIG_PID = "pnnl.goss.sql.datasource.gridappsd";
//	public static final String DS_NAME = "goss.powergrids";
	private static Logger log = LoggerFactory.getLogger(GridAppsDataSourcesImpl.class);
//	private DataSource datasource;
//
//	// Eventually to hold more than one connection
//	// private Map<String, ConnectionPoolDataSource> pooledMap = new ConcurrentHashMap<>();
//	private ConnectionPoolDataSource pooledDataSource;
	
	public GridAppsDataSourcesImpl() {
	}
	public GridAppsDataSourcesImpl(Logger log, DataSourceBuilder datasourceBuilder,
			DataSourceRegistry datasourceRegistry, Properties datasourceProperties){
		GridAppsDataSourcesImpl.log = log;
		this.datasourceBuilder = datasourceBuilder;
		this.datasourceRegistry = datasourceRegistry;
		this.datasourceProperties = datasourceProperties;
	}
	

	@ServiceDependency
	private DataSourceBuilder datasourceBuilder;

	@ServiceDependency
	private DataSourceRegistry datasourceRegistry;
	
	Properties datasourceProperties;

	// These are the datasources that this module has registered.
	private List<String> registeredDatasources = new ArrayList<>();

	public List<String> getRegisteredDatasources(){
		return registeredDatasources;
	}


	@Start
	public void start(){
		log.debug("Starting "+this.getClass().getName());

		registerDataSource();
	}

	@ConfigurationDependency(pid=CONFIG_PID)
	public synchronized void updated(Dictionary<String, ?> config)  {
		Properties properties = new Properties();
		String datasourceName = (String)config.get("name");
		if(datasourceName==null){
			datasourceName = CONFIG_PID;
		}
		properties.put(DataSourceBuilder.DATASOURCE_NAME, datasourceName);
		properties.put(DataSourceBuilder.DATASOURCE_USER, config.get("username"));
		properties.put(DataSourceBuilder.DATASOURCE_PASSWORD, config.get("password"));
		properties.put(DataSourceBuilder.DATASOURCE_URL, config.get("url"));
		properties.put("driverClassName", config.get("driver"));
		if(datasourceProperties==null)
			datasourceProperties = new Properties();
		datasourceProperties.putAll(properties);
//		datasourceProperties = properties;
		
		
	}
	
	
	protected void registerDataSource(){
		
			String datasourceName = datasourceProperties.getProperty(DataSourceBuilder.DATASOURCE_NAME);
			if(datasourceName==null){
				throw new RuntimeException("No datasource name provided when registering data source");
			}
			
			if(datasourceBuilder!=null && registeredDatasources!=null){
				try {
					datasourceBuilder.create(datasourceName, datasourceProperties);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					
					//TODO use logmanager to log error
					e.printStackTrace();
				}
				registeredDatasources.add(datasourceName);
			}

		
		
	}
	
	
	@Stop
	public void stop(){
		log.debug("Stopping "+this.getClass().getName());
		for(String s: registeredDatasources){
			datasourceRegistry.remove(s);
		}
		registeredDatasources.clear();
	}

	@Override
	public Collection<String> getDataSourceKeys() {
		return this.registeredDatasources;
	}




	@Override
	public DataSourcePooledJdbc getDataSourceByKey(String datasourcekey) {
		return (DataSourcePooledJdbc) datasourceRegistry.get(datasourcekey);
	}


	@Override
	public Connection getConnectionByKey(String key) {
		
		Connection conn = null;
		try {
			conn = ((DataSourcePooledJdbc) datasourceRegistry.get(key)).getConnection();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return conn;
	}
}
