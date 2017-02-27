package pnnl.goss.gridappsd.data;

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
	private static final String CONFIG_PID = "pnnl.goss.sql.datasource.powergrids.adms";
//	public static final String DS_NAME = "goss.powergrids";
	private static final Logger log = LoggerFactory.getLogger(GridAppsDataSourcesImpl.class);
//	private DataSource datasource;
//
//	// Eventually to hold more than one connection
//	// private Map<String, ConnectionPoolDataSource> pooledMap = new ConcurrentHashMap<>();
//	private ConnectionPoolDataSource pooledDataSource;

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
		
		datasourceProperties = properties;
		
		
	}
	
	
	protected void registerDataSource(){
		try {
			String datasourceName = datasourceProperties.getProperty(DataSourceBuilder.DATASOURCE_NAME);
			if(datasourceBuilder!=null && registeredDatasources!=null){
				datasourceBuilder.create(datasourceName, datasourceProperties);
				registeredDatasources.add(datasourceName);
			}

		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
