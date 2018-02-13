package gov.pnnl.goss.gridappsd.api;

import java.util.List;

import org.apache.jena.query.ResultSet;

public interface PowergridModelDataManager {
	public enum ResultFormat {
	    JSON, XML, CSV
	}
	
	String query(String modelId, String query, String resultFormat ) throws Exception;
	ResultSet queryResultSet(String modelId, String query);
	
	String queryObject(String modelId, String mrid, String resultFormat) throws Exception;
	ResultSet queryObjectResultSet(String modelId, String mrid);
	
	String queryObjectTypes(String modelId, String resultFormat );
	List<String> queryObjectTypeList(String modelId);
	
	String queryModel(String modelId, String objectType, String filter, String resultFormat) throws Exception;
	ResultSet queryModelResultSet(String modelId, String objectType, String filter);
	
	String queryModelNames(String resultFormat);
	List<String> queryModelNameList();
	
	void putModel(String modelId, String model, String inputFormat);
	
	//Also will need putObject and deleteObject  (will need to support the right security permissions)

}
