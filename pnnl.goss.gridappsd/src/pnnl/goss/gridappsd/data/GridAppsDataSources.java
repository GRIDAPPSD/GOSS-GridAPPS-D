package pnnl.goss.gridappsd.data;

import java.sql.Connection;
import java.util.Collection;

import pnnl.goss.core.server.DataSourcePooledJdbc;

public interface GridAppsDataSources {
	/**
	 * Returns the keys that can query against the @{link: DataSourceRegistry}
	 * 
	 * @return
	 */
	public Collection<String> getDataSourceKeys();
	
	/**
	 * Returns an @{link: DataSourcePooledJdbc} object by key.
	 * 
	 * @param mrid
	 * @return
	 */
	public DataSourcePooledJdbc getDataSourceByKey(String datasourcekey);
	
	/**
	 * Returns an @{link: Connection} object to where the key is located.
	 * 
	 * @param mrid
	 * @return
	 */
	public Connection getConnectionByKey(String mrid);
}
