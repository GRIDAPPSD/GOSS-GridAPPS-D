package gov.pnnl.goss.gridappsd.api;

public interface PowergridModelDataManager {
	public enum ResultFormat {
	    JSON, GLM, XML 
	}
	
	void query(String query, String resultFormat, String resultTopic, String statusTopic);
	void queryObject(String mrid, String resultFormat, String outputTopic, String statusTopic);
	void queryObjectTypeList(String modelId, String resultFormat, String resultTopic, String statusTopic);
	void queryModel(String modelId, String objectType, String filter, String resultFormat, String outputTopic, String statusTopic);
	void queryModelList(String outputTopic, String statusTopic);
	void putModel(String modelId, String model, String inputFormat, String resultTopic, String statusTopic);
	
	//Also probably need putObject and deleteObject  (will need to support the right security permissions)

}
