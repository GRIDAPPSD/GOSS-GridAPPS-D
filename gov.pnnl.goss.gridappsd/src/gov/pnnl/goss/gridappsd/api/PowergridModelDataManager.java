package gov.pnnl.goss.gridappsd.api;

import java.util.List;

import org.apache.jena.query.ResultSet;

public interface PowergridModelDataManager {
	public enum ResultFormat {
	    JSON, XML, CSV
	}
	
	String query(String modelId, String query, String resultFormat , String processId, String username) throws Exception;
	ResultSet queryResultSet(String modelId, String query, String processId, String username);
	
	String queryObject(String modelId, String mrid, String resultFormat, String processId, String username) throws Exception;
	ResultSet queryObjectResultSet(String modelId, String mrid, String processId, String username);
	
	String queryObjectTypes(String modelId, String resultFormat , String processId, String username);
	List<String> queryObjectTypeList(String modelId, String processId, String username);
	
	String queryModel(String modelId, String objectType, String filter, String resultFormat, String processId, String username) throws Exception;
	ResultSet queryModelResultSet(String modelId, String objectType, String filter, String processId, String username);
	
	String queryModelNames(String resultFormat, String processId, String username);
	List<String> queryModelNameList( String processId, String username);

	String queryModelNamesAndIds(String resultFormat, String processId, String username);
	ResultSet queryModelNamesAndIdsResultSet(String processId, String username);

	String queryObjectIds(String resultFormat, String modelId, String objectType, String processId, String username);
	List<String> queryObjectIdsList(String modelId, String objectType, String processId, String username);
	
	String queryObjectDictByType(String resultFormat, String modelId, String objectType, String objectId, String processId, String username) throws Exception ;
	ResultSet queryObjectDictByTypeResultSet(String modelId, String objectType, String objectId, String processId, String username);
	
	String queryMeasurementDictByObject(String resultFormat, String modelId, String objectType, String objectId, String processId, String username) throws Exception ;
	ResultSet queryMeasurementDictByObjectResultSet(String modelId, String objectType, String objectId, String processId, String username);
	
	
	void putModel(String modelId, String model, String inputFormat, String processId, String username);
	
	void insertAllMeasurements(String processId, String username, String baseDirectory) throws Exception;
	void insertMeasurements(String modelNames, String modelId, String processId, String username, String baseDirectory);
	void dropAllMeasurements(String processId, String username);
	void dropMeasurements(String modelNames, String modelId, String processId, String username);
	
	void insertAllHouses( String processId, String username, String baseDirectory);
	void insertHouses(String modelName, String modelId, String processId, String username, String baseDirectory);
	void dropHouses(String modelName, String modelId, String processId, String username, String baseDirectory);
	
	void insertDER(String modelNames, String processId, String username, String baseDirectory);
	
	//Also will need putObject and deleteObject  (will need to support the right security permissions)

}
