package gov.pnnl.goss.gridappsd.api;

import java.io.PrintWriter;
import java.util.Properties;

public interface ConfigurationHandler {
	public void generateConfig(Properties parameters, PrintWriter out) throws Exception;
}
