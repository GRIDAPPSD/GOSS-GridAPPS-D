package gov.pnnl.goss.gridappsd.data;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
//import org.openrdf.model.Statement;
//import org.openrdf.query.GraphQueryResult;
//import com.bigdata.rdf.sail.webapp.SD;
//import com.bigdata.rdf.sail.webapp.client.RemoteRepositoryManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.math3.complex.Complex;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;

import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.PowergridModelDataManager;
import gov.pnnl.goss.gridappsd.data.handlers.BlazegraphQueryHandler;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.log.LogManagerImpl;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.ClientFactory;

@Component
public class BGPowergridModelDataManagerImpl implements PowergridModelDataManager { 
	final String nsCIM = "http://iec.ch/TC57/CIM100#";
	final String nsRDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	final String nsXSD = "http://www.w3.org/2001/XMLSchema#";
	final String feederProperty = "http://iec.ch/TC57/CIM100#Feeder.NormalEnergizingSubstation";
	final String RDF_TYPE = nsRDF+"type";
	final String RDF_RESOURCE = "rdf:resource";
	final String RDF_ID = "rdf:ID";
	final String SUBJECT = "subject";
	final String PREDICATE = "predicate";
	final String OBJECT = "object";
	
	final String FEEDER_NAME = "modelName";
	final String FEEDER_ID = "modelId";
	final String STATION_NAME = "stationName";
	final String STATION_ID = "stationId";
	final String SUBREGION_NAME = "subRegionName";
	final String SUBREGION_ID = "subRegionId";
	final String REGION_NAME = "regionName";
	final String REGION_ID = "regionId";
	
	public static final String DATA_MANAGER_TYPE = "powergridmodel";
	 

	String endpointBaseURL;
	String endpointNSURL;
//	BlazegraphQueryHandler queryHandler;

	@ServiceDependency 
	private volatile ConfigurationManager configManager;
	
	@ServiceDependency 
	private volatile DataManager dataManager;
	
	@ServiceDependency
	private volatile LogManager logManager;
	
	@ServiceDependency
	private volatile ClientFactory clientFactory;
	
	HashMap<String, String> models = new HashMap<String, String>();
//	HashMap<String, String[]> phaseMapping = new HashMap<String, String[]>();
	
//	List<String> reservedModelNames = new ArrayList<String>();
	
	public BGPowergridModelDataManagerImpl() {
		
//		queryHandler = new BlazegraphQueryHandler(configManager.getConfigurationProperty(GridAppsDConstants.BLAZEGRAPH_HOST_PATH));
//		dataManager.registerDataManagerHandler(this, DATA_MANAGER_TYPE);
		
		
	}
	public BGPowergridModelDataManagerImpl(String endpoint) {
		endpointBaseURL = endpoint;
		endpointNSURL = endpoint;
		//queryHandler = new BlazegraphQueryHandler(endpoint);
//		dataManager.registerDataManagerHandler(this, DATA_MANAGER_TYPE);
		
		
		
	}
//	Repository repository;
	@Start
	public void start(){
//		System.out.println("Starting "+getClass());
		
//		System.out.println("STARTING BGPGMODELDM");
		try{

			endpointBaseURL = configManager.getConfigurationProperty(GridAppsDConstants.BLAZEGRAPH_HOST_PATH);
			endpointNSURL = configManager.getConfigurationProperty(GridAppsDConstants.BLAZEGRAPH_NS_PATH);
		}catch(Exception e){
			e.printStackTrace();
			endpointBaseURL = BlazegraphQueryHandler.DEFAULT_ENDPOINT;
			endpointNSURL = endpointBaseURL;
		}
		
//		reservedModelNames.add("kb");
		
		dataManager.registerDataManagerHandler(new BGPowergridModelDataManagerHandlerImpl(this), DATA_MANAGER_TYPE);
	}
	
	
	//update with properties??
	
	public static void main(String[] args){
//		String query = "SELECT ?key (count(?tank) as ?count) WHERE {"+
//				" ?tank c:TransformerTank.PowerTransformer ?pxf."+
//				" ?pxf c:IdentifiedObject.name ?key"+
//				"} GROUP BY ?key ORDER BY ?key";
//		BGPowergridModelDataManagerImpl bg = new BGPowergridModelDataManagerImpl("http://localhost:9999/blazegraph/namespace/kb/sparql");
//		BGPowergridModelDataManagerImpl bg = new BGPowergridModelDataManagerImpl("http://192.168.99.100:8889/bigdata/namespace/kb/sparql");		
		BGPowergridModelDataManagerImpl bg = new BGPowergridModelDataManagerImpl("urn:uuid");
		bg.logManager = new LogManagerImpl();

		bg.endpointNSURL = "urn:uuid";
		try {
//			String query = "select ?s ?p ?o where {?s r:type c:ConnectivityNode. ?s ?p ?o}";
//			System.out.println(bg.query("ieee13", query, "JSON"));
			
//			bg.queryObject("ieee13", "_211AEE43-D357-463C-95B9-184942ABE3E5", "JSON");
//			System.out.println(bg.queryObjectTypes("_4F76A5F9-271D-9EB8-5E31-AA362D86F2C3", "JSON", "12345", "user"));
//			System.out.println(bg.queryModelNameList("12345", "user"));
//			bg.listAllMeasurements("test", "test");
			String baseDirectory = "E:\\tmp\\measurements\\";
			bg.insertAllMeasurements("test", "test", baseDirectory);
			long start = new Date().getTime();
			String model = bg.queryModel("5B816B93-7A5F-B64C-8460-47C17D6E4B0F", "", "", "XML", "12345", "user");
//			String model = bg.queryModel("4F76A5F9-271D-9EB8-5E31-AA362D86F2C3", "", "", "XML", "12345", "user");
//			String model = bg.queryModel("503D6E20-F499-4CC7-8051-971E23D0BF79", "", "", "XML", "12345", "user");
			
			

//			models.put("acep_psil", "_77966920-E1EC-EE8A-23EE-4EFD23B205BD");
//			models.put("eprij1", "_67AB291F-DCCD-31B7-B499-338206B9828F");
//			models.put("ieee13assets", "_5B816B93-7A5F-B64C-8460-47C17D6E4B0F");
//			models.put("ieee13nodeckt", "_49AD8E07-3BF9-A4E2-CB8F-C3722F837B62");
//			models.put("ieee13ochre", "_13AD8E07-3BF9-A4E2-CB8F-C3722F837B62");
//			models.put("ieee37", "_49003F52-A359-C2EA-10C4-F4ED3FD368CC");
//			models.put("ieee123", "_C1C3E687-6FFD-C753-582B-632A27E28507");
//			models.put("ieee123pv", "_E407CBB6-8C8D-9BC9-589C-AB83FBF0826D");
//			models.put("ieee8500", "_4F76A5F9-271D-9EB8-5E31-AA362D86F2C3");
//			models.put("ieee8500enh", "_AAE94E4A-2465-6F5E-37B1-3E72183A4E44");
//			models.put("r2_12_47_2", "_9CE150A8-8CC5-A0F9-B67E-BBD8C79D3095");
//			models.put("transactive", "_503D6E20-F499-4CC7-8051-971E23D0BF79");
//			models.put("final9500node", "_EE71F6C9-56F0-4167-A14E-7F4C71F10EAA");
			
			FileOutputStream fout = new FileOutputStream(new File("xml_new_full.xml"));
			fout.write(model.getBytes());
			fout.flush();
			fout.close();
			long end = new Date().getTime();
			System.out.println("Took "+((end-start)/1000)+" sec");
//			System.out.println(bg.queryModel("_4F76A5F9-271D-9EB8-5E31-AA362D86F2C3", "", "", "XML", "12345", "user"));
//			System.out.println(bg.queryModelNames("XML"));
//			System.out.println(bg.queryModelNamesAndIds("XML", "12345", "user"));
			
//			System.out.println(bg.queryObjectIds("JSON", "4F76A5F9-271D-9EB8-5E31-AA362D86F2C3", "LoadBreakSwitch", "12345", "user"));
			//test with both object id and type
//			System.out.println(bg.queryObjectDictByType("JSON", "C1C3E687-6FFD-C753-582B-632A27E28507", "LinearShuntCompensator", "_EF2FF8C1-A6A6-4771-ADDD-A371AD929D5B", "12345", "user"));    //ieee123
			//test with only object id
//			System.out.println(bg.queryObjectDictByType("JSON", "C1C3E687-6FFD-C753-582B-632A27E28507", null, "_EF2FF8C1-A6A6-4771-ADDD-A371AD929D5B", "12345", "user"));    //ieee123
			//test with only object type
//			System.out.println(bg.queryObjectDictByType("JSON", "C1C3E687-6FFD-C753-582B-632A27E28507", "LinearShuntCompensator", null, "12345", "user"));    //ieee123
			//test with neither object or type, should fail
//			try{
//			System.out.println(bg.queryObjectDictByType("JSON", "C1C3E687-6FFD-C753-582B-632A27E28507", null, null, "12345", "user"));    //ieee123
//			}catch (Exception e) {
//				System.out.println("Expected error "+e.getMessage());
//				// TODO: handle exception
//			}
			//			System.out.println(bg.queryObjectDictByType("JSON", "4F76A5F9-271D-9EB8-5E31-AA362D86F2C3", "LinearShuntCompensator", null, "12345", "user"));   //ieee8500
//			System.out.println(bg.queryMeasurementDictByObject("JSON", "4F76A5F9-271D-9EB8-5E31-AA362D86F2C3",  null, "_7A02B3B0-2746-EB24-45A5-C3FBA8ACB88E", "12345", "user"));
//			System.out.println(bg.queryMeasurementDictByObject("JSON", "4F76A5F9-271D-9EB8-5E31-AA362D86F2C3", "LinearShuntCompensator", null, "12345", "user"));

			//			System.out.println
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
	}
	
	
	
	
	
	
	//,  String requestId/processId
	@Override
	public String query(String modelId, String query, String resultFormat, String processId, String username) throws Exception {
		
		ResultSet rs = queryResultSet(modelId, query, processId, username);
		ByteArrayOutputStream resultString = new ByteArrayOutputStream();
		if(resultFormat.equals(ResultFormat.JSON.toString())){
			ResultSetFormatter.outputAsJSON(resultString, rs);

		} else if(resultFormat.equals(ResultFormat.XML.toString())){
			ResultSetFormatter.outputAsXML(resultString, rs);

		} else {
			//PROCESS ID?  TIMESTAMP passed in or generated?? USERNAME??
			//TODO send log message 
			//logManager.log(new LogMessage(processId, timestamp, "Result Format not recognized, '"+resultFormat+"'", LogMessage.LogLevel.ERROR, LogMessage.ProcessStatus.ERROR, storeToDb), username);
		}
		
		String result = new String(resultString.toByteArray());

			//TODO
		logStatus("COMPLETE");
		
		return result;
	}
	@Override
	public ResultSet queryResultSet(String modelId, String query, String processId, String username) {
		String endpoint = getEndpointURL(modelId);
		BlazegraphQueryHandler queryHandler = new BlazegraphQueryHandler(endpoint, logManager, processId, username);
		return  queryHandler.query(query, null);
		
	}
	
	@Override
	public String queryObject(String modelId, String mrid, String resultFormat, String processId, String username) throws Exception {
		// TODO Auto-generated method stub
		
		ResultSet rs = queryObjectResultSet(modelId, mrid, processId, username);
		ByteArrayOutputStream resultString = new ByteArrayOutputStream();
		if(resultFormat.equals(ResultFormat.JSON.toString())){
			ResultSetFormatter.outputAsJSON(resultString, rs);

		} else if(resultFormat.equals(ResultFormat.XML.toString())){
			ResultSetFormatter.outputAsXML(resultString, rs);

		} else {
			//logManager.log(new LogMessage(processId, timestamp, logMessage, logLevel, processStatus, storeToDb), username);
		}
		
		String result = new String(resultString.toByteArray());

		//TODO
		logStatus("COMPLETE");
		
		return result;
	}
	@Override
	public ResultSet queryObjectResultSet(String modelId, String mrid, String processId, String username) {
		String query = "select ?property ?value where {<"+getEndpointNS(mrid)+"> ?property ?value}";

		BlazegraphQueryHandler queryHandler = new BlazegraphQueryHandler(getEndpointURL(modelId), logManager, processId, username);
		ResultSet rs = queryHandler.query(query, null);
		return rs;

	}
	
	
	
	@Override
	public String queryObjectTypes(String modelId, String resultFormat, String processId, String username) {
		return formatStringList(queryObjectTypeList(modelId, processId, username), "objectTypes", resultFormat);
	}
	@Override
	public List<String> queryObjectTypeList(String modelId, String processId, String username) {
		String query = "select DISTINCT  ?type where {?subject rdf:type ?type ";
		if(modelId!=null && modelId.trim().length()>0){
			query = query+". ?subject ?p2 <"+getEndpointNS(modelId)+"> ";

		}
		query = query + "}";
		BlazegraphQueryHandler queryHandler = new BlazegraphQueryHandler(getEndpointURL(modelId), logManager, processId, username);
		ResultSet rs = queryHandler.query(query, null);
		
		List<String> objectTypes = new ArrayList<String>(); 
		while(rs.hasNext()){
			QuerySolution binding = rs.nextSolution();
			Resource type = (Resource) binding.get("type");
		    objectTypes.add(type.getURI());
		}
		
		
		return objectTypes;
	}

	@Override
	public String queryModel(String modelId, String objectType, String filter, String resultFormat, String processId, String username) throws Exception {
		String result = null;
		
		HashSet<String> alreadySeen = new HashSet<String>();
		Queue<String> newIds = new PriorityQueue<String>();
		List<BGResult> results = new ArrayList<BGResult>();
		BlazegraphQueryHandler queryHandler = new BlazegraphQueryHandler(getEndpointURL(modelId), logManager, processId, username);
		String baseUrl = getEndpointNS(null);
		
		//Add initial ids of incoming links
		String intitialIdQuery = "CONSTRUCT {?s ?p ?o}  WHERE { { ?s ?p ?o . VALUES ?o { <"+getEndpointNS(modelId)+"> }}}";
		newIds.add(getEndpointNS(modelId));
		ResultSet rs = queryHandler.construct(intitialIdQuery);
		while(rs.hasNext()){
			QuerySolution qs = rs.nextSolution();
			String subjectUri = qs.getResource(SUBJECT).getURI();
			String propertyName = qs.getResource(PREDICATE).getURI();
			if (!alreadySeen.contains(subjectUri) && !newIds.contains(subjectUri) && !RDF_TYPE.equals(propertyName) && subjectUri.startsWith(baseUrl)){
				newIds.add(subjectUri);
				

			}
		}
		
//		rs = queryModelResultSet(modelId, objectType, filter, processId, username, true);
		
		//Tracks which subjects have been seen already and follows links to pull information on those that haven't been included yet
		while(rs.hasNext() || newIds.size()>0){
			while(rs.hasNext()){
				QuerySolution qs = rs.nextSolution();
				String subject = qs.getResource(SUBJECT).getLocalName();
				String subjectUri = qs.getResource(SUBJECT).getURI();
				String propertyName = qs.getResource(PREDICATE).getURI();
	
				String  value = "";
				if(qs.get(OBJECT).isLiteral()){
					Literal literal = qs.getLiteral(OBJECT);
					value = literal.toString();
				} else {
					Resource resource = qs.getResource(OBJECT);
					value = resource.toString();
					if(!alreadySeen.contains(value) && !newIds.contains(value) && value.startsWith(baseUrl) && !feederProperty.equals(propertyName)){
						newIds.add(value);
					}
				}
				if(!alreadySeen.contains(subjectUri)){
					alreadySeen.add(subjectUri);
					if(newIds.contains(subjectUri)){
						newIds.remove(subjectUri);
					}
				}
				BGResult r = new BGResult(subject, propertyName, value);
				results.add(r);
			}

			if(newIds.size()>0){
				//build query with new ids
				String newOutgoingIdQuery = "CONSTRUCT {?s ?p ?o} WHERE { ";
				String newIncomingIdQuery = "CONSTRUCT {?s ?p ?o}  WHERE { ";
				for(int i=0;i<100 && newIds.size()>0; i++){
					String id = newIds.poll();
					
					newOutgoingIdQuery = newOutgoingIdQuery+"{ ?s ?p ?o . VALUES ?s { <"+id+"> }}  UNION";
					newIncomingIdQuery = newIncomingIdQuery+"{ ?s ?p ?o . VALUES ?o { <"+id+"> }}  UNION";
					if(!alreadySeen.contains(id)) {
						alreadySeen.add(id);
					}
					
				}
				newOutgoingIdQuery = newOutgoingIdQuery.substring(0,newOutgoingIdQuery.length()-6);
				newOutgoingIdQuery = newOutgoingIdQuery+" }";
				newIncomingIdQuery = newIncomingIdQuery.substring(0,newIncomingIdQuery.length()-6);
				newIncomingIdQuery = newIncomingIdQuery+" }";
				
				//Get ids for the incoming links
				rs = queryHandler.construct(newIncomingIdQuery);
				while(rs.hasNext()){
					QuerySolution qs = rs.nextSolution();
					String subjectUri = qs.getResource(SUBJECT).getURI();
					String propertyName = qs.getResource(PREDICATE).getURI();

					if (!alreadySeen.contains(subjectUri) && !newIds.contains(subjectUri) && !RDF_TYPE.equals(propertyName) && !feederProperty.equals(propertyName) && subjectUri.startsWith(baseUrl)){
						newIds.add(subjectUri);
					}
				}
				

				rs = queryHandler.construct(newOutgoingIdQuery);
				
				
				
			}
		
		}
		
		if(resultFormat.equals(ResultFormat.JSON.toString())){
			result = resultSetToJson(results);
		} else if(resultFormat.equals(ResultFormat.XML.toString())){
			result = resultSetToXML(results);
		} else {
			//TODO throw error??
			//logManager.log(new LogMessage(processId, timestamp, logMessage, logLevel, processStatus, storeToDb), username);
			
			//OR JUST DEFAULT TO JSON
			result = resultSetToJson(results);
		}
		
		//TODO
		logStatus("Generation of queryModel in "+resultFormat+" is complete");
		
		return result;

	}
	@Override
	public ResultSet queryModelResultSet(String modelId, String objectType, String filter, String processId, String username) {
		return queryModelResultSet(modelId, objectType, filter, processId, username, false);
	}
		
		
	protected ResultSet queryModelResultSet(String modelId, String objectType, String filter, String processId, String username, boolean multiLevel) {
		if(modelId==null){
			throw new RuntimeException("queryModel: model id missing");
		}
		
		String query;
		if(multiLevel){
			query = "CONSTRUCT   { ?s ?p ?o . ?o ?p3 ?o3 } WHERE     { ?s ?p ?o ";
			query = query+". ?s ?p2 <"+getEndpointNS(modelId)+"> OPTIONAL {?o ?p3 ?o3 } ";
			if(objectType!=null && objectType.trim().length()>0){
				query = query+". ?s rdf:type <"+objectType+"> ";
			}
			if(filter!=null && filter.trim().length()>0){
				if(filter.startsWith(".")){
					filter = filter.substring(1);
				}
				query = query+". "+filter;
			}
			query = query+"}";
		} else {
			query = "CONSTRUCT   { ?s ?p ?o } WHERE     { ?s ?p ?o ";
			query = query+". ?s ?p2 <"+getEndpointNS(modelId)+"> ";
			if(objectType!=null && objectType.trim().length()>0){
				query = query+". ?s rdf:type <"+objectType+"> ";
			}
			if(filter!=null && filter.trim().length()>0){
				if(filter.startsWith(".")){
					filter = filter.substring(1);
				}
				query = query+". "+filter;
			}
			query = query+"}";
		}
		
		
		
		System.out.println(query);
		
		BlazegraphQueryHandler queryHandler = new BlazegraphQueryHandler(getEndpointURL(modelId), logManager, processId, username);
		ResultSet rs = queryHandler.construct(query);
		return rs;
	}
	
	
	@Override
	public String queryModelNames(String resultFormat, String processId, String username) {
		return formatStringList(queryModelNameList(processId, username), "modelNames", resultFormat);
	}
	
	
	@Override
	public List<String> queryModelNameList(String processId, String username) {
		List<String> models = new ArrayList<String>();

		String modelNameQuery = "SELECT ?feeder ?fid  WHERE {"
				+ "?s r:type c:Feeder."
				+ "?s c:IdentifiedObject.name ?feeder."
				+ "?s c:IdentifiedObject.mRID ?fid	}"
				+ " ORDER by ?fid";
		
		
		ResultSet modelNameRS = queryResultSet(null, modelNameQuery, processId, username);
		while(modelNameRS.hasNext()){
			QuerySolution qs = modelNameRS.nextSolution();
			models.add(qs.get("fid").toString());
		}

		return models;
	}
	
	@Override
	public String queryObjectIds(String resultFormat, String modelId, String objectType, String processId, String username) {
		return formatStringList(queryObjectIdsList(modelId, objectType, processId, username), "objectIds", resultFormat);

	}
	@Override
	public List<String> queryObjectIdsList(String modelId, String objectType, String processId, String username) {
		
		if(modelId==null){
			throw new RuntimeException("queryObjectIds: model id missing");
		}
		List<String> objectIds = new ArrayList<String>();
		String query = "SELECT DISTINCT ?s WHERE {"+
				  " ?s ?p <"+getEndpointNS(modelId)+"> .";
		if(objectType!=null && objectType.trim().length()>0){
			query = query + " ?s rdf:type <"+nsCIM+objectType+"> .";
		}
		query = query+ "}";
//		System.out.println(query);
		String baseUrl = getEndpointNS(null);
		ResultSet rs = queryResultSet(getEndpointURL(modelId), query, processId, username);
		while(rs.hasNext()){
			QuerySolution qs = rs.nextSolution();
			String value = qs.get("s").toString();
			if(value.startsWith(baseUrl+"#")){
				value = value.substring(baseUrl.length()+1);
			} 
			objectIds.add(value);
		}
		return objectIds;
	}
	@Override
	public String queryObjectDictByType(String resultFormat, String modelId, String objectType, String objectId, String processId, String username) throws Exception {
		String result = null;
		ResultSet rs = queryObjectDictByTypeResultSet(modelId, objectType, objectId, processId, username);
		if(resultFormat.equals(ResultFormat.JSON.toString())){
			result = resultSetToJson(rs);
		} else if(resultFormat.equals(ResultFormat.XML.toString())){
			result = resultSetToXML(rs);
//			ResultSetFormatter.outputAsXML(resultString, rs);
		} else {
			//TODO throw error
			//logManager.log(new LogMessage(processId, timestamp, logMessage, logLevel, processStatus, storeToDb), username);
		}
		
		//TODO
		logStatus("COMPLETE");
		
		return result;
	}
	
	
	@Override
	public ResultSet queryObjectDictByTypeResultSet(String modelId, String objectType,
			String objectId, String processId, String username) {

		if(modelId==null){
			throw new RuntimeException("queryObjectDict: model id missing");
		}
		
		if((objectType==null || objectType.trim().length()==0) && (objectId==null || objectId.trim().length()==0)) {
			throw new RuntimeException("queryObjectDict: both object id and object type missing, at least one required");
		}
		
		String query = "";
		String subject = "?s";
		if(objectId!=null && objectId.trim().length()>0){
			subject = "<"+getEndpointNS(objectId)+">";
		}
				
		query = "CONSTRUCT   { "+subject+" ?p ?o } WHERE     { "+ 
			  subject+" ?p ?o . "+ 
			  subject+" ?p2 <"+getEndpointNS(modelId)+"> . ";
		if((objectId==null || objectId.trim().length()==0) && objectType!=null && objectType.trim().length()>0){
			query = query + subject+" rdf:type <"+nsCIM+objectType+"> .";
		}  
		query = query+ "}";
//		System.out.println(query);
		BlazegraphQueryHandler queryHandler = new BlazegraphQueryHandler(getEndpointURL(modelId), logManager, processId, username);
		ResultSet rs = queryHandler.construct(query);
		return rs;
		
		
	}
	@Override
	public String queryMeasurementDictByObject(String resultFormat, String modelId, String objectType, String objectId, String processId, String username) throws Exception {
		String result = null;
		ResultSet rs = queryMeasurementDictByObjectResultSet(modelId, objectType, objectId, processId, username);
		if(resultFormat.equals(ResultFormat.JSON.toString())){
			JsonArray resultArr = new JsonArray();
			while( rs.hasNext()) {
				QuerySolution qs = rs.nextSolution();
				JsonObject obj = new JsonObject();
				obj.add("measid", new JsonPrimitive(qs.getLiteral("measid").getString()));
				obj.add("type", new JsonPrimitive(qs.getLiteral("type").getString()));
				obj.add("class", new JsonPrimitive(qs.getLiteral("class").getString()));
				obj.add("name", new JsonPrimitive(qs.getLiteral("name").getString()));
				obj.add("bus", new JsonPrimitive(qs.getLiteral("bus").getString()));
				obj.add("phases", new JsonPrimitive(qs.getLiteral("phases").getString()));
				obj.add("eqtype", new JsonPrimitive(qs.getLiteral("eqtype").getString()));
				obj.add("eqname", new JsonPrimitive(qs.getLiteral("eqname").getString()));
				obj.add("eqid", new JsonPrimitive(qs.getLiteral("eqid").getString()));
				obj.add("trmid", new JsonPrimitive(qs.getLiteral("trmid").getString()));
				
				resultArr.add(obj);
			}
			
			result = resultArr.toString();
		} else if(resultFormat.equals(ResultFormat.XML.toString())){
			result = resultSetToXML(rs);
//			ResultSetFormatter.outputAsXML(resultString, rs);
		} else {
			//TODO throw error
			//logManager.log(new LogMessage(processId, timestamp, logMessage, logLevel, processStatus, storeToDb), username);
		}
		
		//TODO
		logStatus("COMPLETE");
		
		return result;
	}
	@Override
	public ResultSet queryMeasurementDictByObjectResultSet( String modelId, String objectType, String objectId, String processId, String username) {
		if(modelId==null){
			throw new RuntimeException("queryMeasurementDict: model id missing");
		}
		
		String query = "";
//		String subject = "?s";
//		if(objectId!=null && objectId.trim().length()>0){
//			subject = "<"+objectId+">";
//		}
				
//		query = "CONSTRUCT   { ?s ?p ?o } WHERE     { "+ 
//			  subject+" ?p ?o . "+ 
//			  subject+" ?p2 <"+modelId+"> . ";
//		if(objectId!=null && objectId.trim().length()>0){
//			query = query + subject+" rdf:type <"+objectId+"> .";
//		}  
//		query = query+ "}";
		
		query = "SELECT ?class ?type ?name ?bus ?phases ?eqtype ?eqname ?eqid ?trmid ?measid WHERE { "+
			     "?eq c:Equipment.EquipmentContainer ?fdr. "+
			     "?fdr c:IdentifiedObject.mRID ?fdrid. "+
				 "	 { ?s r:type c:Discrete. bind (\"Discrete\" as ?class)} "+
				 "	   UNION "+
				 "	 { ?s r:type c:Analog. bind (\"Analog\" as ?class)} ";
		if(objectId!=null && objectId.trim().length()>0){			  
			query = query+"?s ?p <"+getEndpointNS(objectId)+">. ";
		}
		query = query+"	 ?s c:IdentifiedObject.name ?name . "+
					  "?s c:IdentifiedObject.mRID ?measid . "+
					  "?s c:Measurement.PowerSystemResource ?eq . "+
					  "?s c:Measurement.Terminal ?trm . "+
					  "?s c:Measurement.measurementType ?type . "+
					  "?trm c:IdentifiedObject.mRID ?trmid. "+
					  "?eq c:IdentifiedObject.mRID ?eqid. "+
					  "?eq c:IdentifiedObject.name ?eqname. "+
			          "?eq c:Equipment.EquipmentContainer <"+getEndpointNS(modelId)+">. "+
					  "?eq r:type ?typeraw. "+
					  " bind(strafter(str(?typeraw),\"#\") as ?eqtype) ";
					  
		if((objectId==null || objectId.trim().length()==0) && objectType!=null && objectType.trim().length()>0){
			query = query + "?eq r:type <"+nsCIM+objectType+"> .";
		}  
		query = query+"?trm c:Terminal.ConnectivityNode ?cn. "+
					  "?cn c:IdentifiedObject.name ?bus. "+
					  "?s c:Measurement.phases ?phsraw . "+
					  "  {bind(strafter(str(?phsraw),\"PhaseCode.\") as ?phases)} "+
					  "} ORDER BY ?class ?type ?name ";
		
		
		BlazegraphQueryHandler queryHandler = new BlazegraphQueryHandler(getEndpointURL(modelId), logManager, processId, username);
		ResultSet rs = queryHandler.construct(query);
		return rs;
	}
	
	
	protected Element getXMLElementWithPrefix(Document rootDoc, String namespace, String name){
		if(namespace==null)
			return rootDoc.createElement(name);
		
		HashMap<String, String> prefixMapping = new HashMap<String, String>();
		prefixMapping.put(nsCIM, "cim");
		prefixMapping.put(nsRDF, "rdf");
		
		if(prefixMapping.containsKey(namespace))
			name = prefixMapping.get(namespace)+":"+name;
		return rootDoc.createElementNS(namespace, name);
		
		
	}
	
	
	
//	protected void sendResult(String result, String resultTopic) throws Exception{
//		Credentials credentials = new UsernamePasswordCredentials(
//				GridAppsDConstants.username, GridAppsDConstants.password);
////		Client client = clientFactory.create(PROTOCOL.STOMP,credentials);
//		
//		 
//		
//	}
	
	
	
	protected String resultSetToJson(List<BGResult> results){
		JsonArray resultArr = new JsonArray();
		String baseUrl = getEndpointNS(null);
		HashMap<String, JsonObject> resultObjects = new HashMap<String, JsonObject>();
		for(BGResult result: results) {
			String subject = result.getSubject();
			JsonObject obj = new JsonObject();
			if(resultObjects.containsKey(subject)){
				obj = resultObjects.get(subject);
			} else {
				obj.add("id",  new JsonPrimitive(subject));
				resultObjects.put(subject, obj);
			}
			
			String propertyName = result.getProperty();
			if(propertyName.startsWith(nsCIM)){
				propertyName = propertyName.substring(nsCIM.length());
			} else if(propertyName.startsWith(nsRDF)){
				propertyName = propertyName.substring(nsRDF.length());
			}
			
			String  value = result.getObject();
			
			if(value.startsWith(baseUrl+"#")){
				value = value.substring(baseUrl.length()+1);
			} else if(value.startsWith(nsCIM)){
				value = value.substring(nsCIM.length());
			} else if(value.startsWith(nsRDF)){
				value = value.substring(nsRDF.length());
			}
//			}
			obj.add(propertyName, new JsonPrimitive(value));
			
		}
		
		for(JsonObject obj: resultObjects.values()){
			resultArr.add(obj);
		}
		
		return resultArr.toString();
	}
	
	
	protected String resultSetToJson(ResultSet rs){
		JsonArray resultArr = new JsonArray();
		String baseUrl = getEndpointNS(null);
		HashMap<String, JsonObject> resultObjects = new HashMap<String, JsonObject>();
		while( rs.hasNext()) {
			QuerySolution qs = rs.nextSolution();
			String subject = qs.getResource(SUBJECT).toString();
			if(!subject.contains("urn:uuid")){
				subject = qs.getResource(SUBJECT).getLocalName();
			} else {
				subject = subject.substring(subject.lastIndexOf(':')+1);
			}
			JsonObject obj = new JsonObject();
			if(resultObjects.containsKey(subject)){
				obj = resultObjects.get(subject);
			} else {
				obj.add("id",  new JsonPrimitive(subject));
				resultObjects.put(subject, obj);
			}
			
			String propertyName = qs.getResource(PREDICATE).getURI();
			if(propertyName.startsWith(nsCIM)){
				propertyName = propertyName.substring(nsCIM.length());
			} else if(propertyName.startsWith(nsRDF)){
				propertyName = propertyName.substring(nsRDF.length());
			}
			
			String  value = "";
			if(qs.get(OBJECT).isLiteral()){
				Literal literal = qs.getLiteral(OBJECT);
				value = literal.toString();
			} else {
				Resource resource = qs.getResource(OBJECT);
				value = resource.toString();
				if(value.startsWith(baseUrl+"#")){
					value = value.substring(baseUrl.length()+1);
				} else if(value.startsWith(nsCIM)){
					value = value.substring(nsCIM.length());
				} else if(value.startsWith(nsRDF)){
					value = value.substring(nsRDF.length());
				}
			}
//			qs.getResource("object").toString())
			obj.add(propertyName, new JsonPrimitive(value));
			
		}
		
		for(JsonObject obj: resultObjects.values()){
			resultArr.add(obj);
		}
		
		return resultArr.toString();
	}

	
	protected String resultSetToXML(List<BGResult> results) throws Exception {
		DocumentBuilderFactory factory =
		        DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		

		 DocumentBuilder builder =
		            factory.newDocumentBuilder();
		 Document rootDoc = builder.newDocument();
		 Element rootElement = getXMLElementWithPrefix(rootDoc, nsRDF, "RDF");
		 rootElement.setAttribute("xmlns:cim", nsCIM);
		 rootElement.setAttribute("xmlns:rdf", nsRDF);
		 rootDoc.appendChild(rootElement);
		 
		 
		String baseUrl = getEndpointNS(null);
		HashMap<String, List<Element>> resultObjects = new HashMap<String, List<Element>>();
		HashMap<String, String> resultTypes = new HashMap<String, String>();

		for(BGResult result: results) {
			String subject = result.getSubject();
			List<Element> objs = null;
			if(resultObjects.containsKey(subject)){
				objs = resultObjects.get(subject);
			} else {
				objs = new ArrayList<Element>();
				resultObjects.put(subject, objs);
			}
			
			String propertyName = result.getProperty();
			String  value = result.getObject();

			if(propertyName.equals(RDF_TYPE)){
				resultTypes.put(subject, value);
			} else {
				String ns = "";
				String localName = propertyName;
				if(propertyName.contains("#")){
					ns = propertyName.substring(0, propertyName.indexOf("#")+1);
					localName = propertyName.substring(propertyName.indexOf("#")+1);
//					System.out.println("GOT property NS "+ns+"   LOCAL "+localName);
				}
				Element tmp = getXMLElementWithPrefix(rootDoc, ns, localName);
				if(!isValidURI(value)){
					tmp.setTextContent(value);
				} else {
					if(value.startsWith(baseUrl+"#")){
						value = value.substring(baseUrl.length());
					}
					tmp.setAttributeNS(nsRDF, RDF_RESOURCE, value);
				}
				objs.add(tmp);
			}
			
		}
		
		//Build result elements based on types and properties
		for(String subject: resultTypes.keySet()){
//			Resource subjectRes = resultTypes.get(subject);
			String subjectType = resultTypes.get(subject);
			String ns = "";
			String localName = subjectType;
			if(subjectType.contains("#")){
				ns = subjectType.substring(0, subjectType.indexOf("#")+1);
				localName = subjectType.substring(subjectType.indexOf("#")+1);
			}
			
			
			List<Element> elements = resultObjects.get(subject);
			Element element = getXMLElementWithPrefix(rootDoc, ns, localName);
			element.setAttributeNS(nsRDF, RDF_ID, subject);
			for(Element child: elements){
				element.appendChild(child);
			}
			rootElement.appendChild(element);
		}
		
		
		TransformerFactory tranFactory = TransformerFactory.newInstance();
	    Transformer transformer = tranFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

	    StringWriter resultWriter = new StringWriter();
	    transformer.transform(new DOMSource(rootDoc), new StreamResult(resultWriter));
		return resultWriter.toString();
	}
	
	
	protected boolean isValidURI(String url){
		/* Try creating a valid URL */
        try { 
            new URL(url).toURI(); 
            return true; 
        } 
          
        // If there was an Exception 
        // while creating URL object 
        catch (Exception e) { 
            return false; 
        } 
	}
	protected String resultSetToXML(ResultSet rs) throws Exception {
		DocumentBuilderFactory factory =
		        DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		

		 DocumentBuilder builder =
		            factory.newDocumentBuilder();
		 Document rootDoc = builder.newDocument();
		 Element rootElement = getXMLElementWithPrefix(rootDoc, nsRDF, "RDF");
		 rootElement.setAttribute("xmlns:cim", nsCIM);
		 rootElement.setAttribute("xmlns:rdf", nsRDF);
		 rootDoc.appendChild(rootElement);
		 
		 
		String baseUrl = getEndpointNS(null);
		HashMap<String, List<Element>> resultObjects = new HashMap<String, List<Element>>();
		HashMap<String, Resource> resultTypes = new HashMap<String, Resource>();

		while( rs.hasNext()) {
			QuerySolution qs = rs.nextSolution();
			Resource subjectRes = qs.getResource(SUBJECT);
			String subject = subjectRes.getLocalName();
			List<Element> objs = null;
			if(resultObjects.containsKey(subject)){
				objs = resultObjects.get(subject);
			} else {
				objs = new ArrayList<Element>();
				resultObjects.put(subject, objs);
			}
			
			Resource property = qs.getResource(PREDICATE);
			if(property.getURI().equals(RDF_TYPE)){
				Resource type = qs.getResource(OBJECT);
				resultTypes.put(subject, type);
			} else {
				Element tmp = getXMLElementWithPrefix(rootDoc, property.getNameSpace(), property.getLocalName());
				String  value = "";
				if(qs.get(OBJECT).isLiteral()){
					Literal literal = qs.getLiteral(OBJECT);
					value = literal.toString();
					tmp.setTextContent(value);
				} else {
					Resource resource = qs.getResource(OBJECT);
					value = resource.toString();
					if(value.startsWith(baseUrl+"#")){
						value = value.substring(baseUrl.length());
					}
					tmp.setAttributeNS(nsRDF, RDF_RESOURCE, value);
				}
				objs.add(tmp);
			}
			
		}
		
		//Build result elements based on types and properties
		for(String subject: resultTypes.keySet()){
			Resource subjectRes = resultTypes.get(subject);
			List<Element> elements = resultObjects.get(subject);
			Element element = getXMLElementWithPrefix(rootDoc, subjectRes.getNameSpace(), subjectRes.getLocalName());
			element.setAttributeNS(nsRDF, RDF_ID, subject);
			for(Element child: elements){
				element.appendChild(child);
			}
			rootElement.appendChild(element);
		}
		
		
		TransformerFactory tranFactory = TransformerFactory.newInstance();
	    Transformer transformer = tranFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

	    StringWriter resultWriter = new StringWriter();
	    transformer.transform(new DOMSource(rootDoc), new StreamResult(resultWriter));
		return resultWriter.toString();
	}
	
	protected void logStatus(String status) throws Exception{
//		Credentials credentials = new UsernamePasswordCredentials(
//				GridAppsDConstants.username, GridAppsDConstants.password);
//		Client client = clientFactory.create(PROTOCOL.STOMP,credentials);
		
		//TODO
		//logmanager.log...
		
	}
	@Override
	public void putModel(String modelId, String model, String inputFormat, String processId, String username) {
		// TODO Auto-generated method stub
		//if model id is null throw error
		//if namespace already exists throw error 
		
	}



	private String getEndpointNS(String modelId){
		if(modelId!=null) {
			return endpointNSURL+modelId;
		}
		return endpointNSURL;
	}
	private String getEndpointURL(String modelId){
		//Originally this used a different endpoint based on the model id, with all 
		// models in the same namespace that is not necessary
//		if(endpointBaseURL==null){
			//TODO log error status
			//throw new Exception(bg endpoint not available);
//		}
//		if(modelId==null) {
//			return endpointBaseURL+"/sparql";
//		}
//		
//		return endpointBaseURL+"/namespace/"+modelId+"/sparql";
//		
		return endpointBaseURL;
	}
	
	
	private String formatStringList(List<String> values, String rootElementName, String resultFormat){
		if(resultFormat.equals(ResultFormat.JSON.toString())){
			JsonObject obj = new JsonObject();
			JsonArray resultArr = new JsonArray();
			for(String str: values){
				resultArr.add(new JsonPrimitive(str));
			}
			obj.add(rootElementName, resultArr);
			return obj.toString();

		} else if(resultFormat.equals(ResultFormat.XML.toString())){
			try{
			DocumentBuilderFactory factory =
			        DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			

			 DocumentBuilder builder =
			            factory.newDocumentBuilder();
			 Document rootDoc = builder.newDocument();
			 Element rootElement = rootDoc.createElement(rootElementName);
//			 rootElement.setAttribute("xmlns:cim", nsCIM);
//			 ROOTELEMENT.SETATTRIBUTE("XMLNS:RDF", NSRDF);
			 rootDoc.appendChild(rootElement);
			 for(String str: values){
				 rootElement.appendChild(rootDoc.createElement(str));
			 }
			 
			 TransformerFactory tranFactory = TransformerFactory.newInstance();
			    Transformer transformer = tranFactory.newTransformer();
		        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

			    StringWriter resultWriter = new StringWriter();
			    transformer.transform(new DOMSource(rootDoc), new StreamResult(resultWriter));
				return resultWriter.toString();
			}catch(Exception e){
				e.printStackTrace();
				//todo thrw a parsing error
			}
		} else if(resultFormat.equals(ResultFormat.CSV.toString())){
			return String.join(",", values);
		} else {
			//TODO send unrecognized type error
		}
		return null;
	}
	@Override
	public String queryModelNamesAndIds(String resultFormat, String processId, String username) {
		ResultSet rs = queryModelNamesAndIdsResultSet(processId, username);
		String rootElementName = "models";
		if(resultFormat.equals(ResultFormat.JSON.toString())){
			JsonObject obj = new JsonObject();
			JsonArray resultArr = new JsonArray();
			while( rs.hasNext()) {
				QuerySolution qs = rs.nextSolution();
				String feederName = qs.getLiteral(FEEDER_NAME).getString();
				String feederId = qs.getLiteral(FEEDER_ID).getString();
				String stationName = qs.getLiteral(STATION_NAME).getString();
				String stationId = qs.getLiteral(STATION_ID).getString();
				String subregionName = qs.getLiteral(SUBREGION_NAME).getString();
				String subRegionId = qs.getLiteral(SUBREGION_ID).getString();
				String regionName = qs.getLiteral(REGION_NAME).getString();
				String regionId = qs.getLiteral(REGION_ID).getString(); 
				
				JsonObject feederobj = new JsonObject();
				feederobj.add(FEEDER_NAME, new JsonPrimitive(feederName));
				feederobj.add(FEEDER_ID, new JsonPrimitive(feederId));
				feederobj.add(STATION_NAME, new JsonPrimitive(stationName));
				feederobj.add(STATION_ID, new JsonPrimitive(stationId));
				feederobj.add(SUBREGION_NAME, new JsonPrimitive(subregionName));
				feederobj.add(SUBREGION_ID, new JsonPrimitive(subRegionId));
				feederobj.add(REGION_NAME, new JsonPrimitive(regionName));
				feederobj.add(REGION_ID, new JsonPrimitive(regionId));

				resultArr.add(feederobj);
			}
			obj.add(rootElementName, resultArr);
			return obj.toString();

		} else if(resultFormat.equals(ResultFormat.XML.toString())){
			try{
			DocumentBuilderFactory factory =
			        DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			

			 DocumentBuilder builder =
			            factory.newDocumentBuilder();
			 Document rootDoc = builder.newDocument();
			 Element rootElement = rootDoc.createElement(rootElementName);
//			 rootElement.setAttribute("xmlns:cim", nsCIM);
//			 ROOTELEMENT.SETATTRIBUTE("XMLNS:RDF", NSRDF);
			 rootDoc.appendChild(rootElement);
			 while( rs.hasNext()) {
				 Element modelElement = rootDoc.createElement("model");
				
				 QuerySolution qs = rs.nextSolution();
				 String feederName = qs.getLiteral(FEEDER_NAME).getString();
				 String feederId = qs.getLiteral(FEEDER_ID).getString();
				 String stationName = qs.getLiteral(STATION_NAME).getString();
				 String stationId = qs.getLiteral(STATION_ID).getString();
				 String subregionName = qs.getLiteral(SUBREGION_NAME).getString();
				 String subRegionId = qs.getLiteral(SUBREGION_ID).getString();
				 String regionName = qs.getLiteral(REGION_NAME).getString();
				 String regionId = qs.getLiteral(REGION_ID).getString(); 
				 modelElement.setAttribute(FEEDER_NAME, feederName);
				 modelElement.setAttribute(FEEDER_ID, feederId);
				 modelElement.setAttribute(STATION_NAME, stationName);
				 modelElement.setAttribute(STATION_ID, stationId);
				 modelElement.setAttribute(SUBREGION_NAME, subregionName);
				 modelElement.setAttribute(SUBREGION_ID, subRegionId);
				 modelElement.setAttribute(REGION_NAME, regionName);
				 modelElement.setAttribute(REGION_ID, regionId);
				 rootElement.appendChild(modelElement);
			 }
			 TransformerFactory tranFactory = TransformerFactory.newInstance();
			    Transformer transformer = tranFactory.newTransformer();
		        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

			    StringWriter resultWriter = new StringWriter();
			    transformer.transform(new DOMSource(rootDoc), new StreamResult(resultWriter));
				return resultWriter.toString();
			}catch(Exception e){
				e.printStackTrace();
				//todo thrw a parsing error
			}
		} else if(resultFormat.equals(ResultFormat.CSV.toString())){
			List<String> values = new ArrayList<String>();
			while( rs.hasNext()) {
				QuerySolution qs = rs.nextSolution();
				String feederName = qs.getLiteral(FEEDER_NAME).getString();
				String feederId = qs.getLiteral(FEEDER_ID).getString();
				String stationName = qs.getLiteral(STATION_NAME).getString();
				String stationId = qs.getLiteral(STATION_ID).getString();
				String subregionName = qs.getLiteral(SUBREGION_NAME).getString();
				String subRegionId = qs.getLiteral(SUBREGION_ID).getString();
				String regionName = qs.getLiteral(REGION_NAME).getString();
				String regionId = qs.getLiteral(REGION_ID).getString(); 
				
				String value = feederName+"|"+feederId+"|"+stationName+"|"+stationId+"|"+subregionName+"|"+subRegionId+"|"+regionName+"|"+regionId;
				values.add(value);
			}
			return String.join(",", values);
		} else {
			//TODO send unrecognized type error
		}
		return null;
		
	}
	@Override
	public ResultSet queryModelNamesAndIdsResultSet(String processId, String username) {
		String modelNameQuery = "SELECT ?"+FEEDER_NAME+" ?"+FEEDER_ID+" ?"+STATION_NAME+" ?"+
				STATION_ID+" ?"+SUBREGION_NAME+" ?"+SUBREGION_ID+" ?"+REGION_NAME+" ?"+REGION_ID+" WHERE {"
				+ " ?s r:type c:Feeder."
				+ " ?s c:IdentifiedObject.name ?"+FEEDER_NAME+"."
				+ " ?s c:IdentifiedObject.mRID ?"+FEEDER_ID+"."
				+ " ?s c:Feeder.NormalEnergizingSubstation ?sub."
				+ "?sub c:IdentifiedObject.name ?"+STATION_NAME+"."
				+ " ?sub c:IdentifiedObject.mRID ?"+STATION_ID+"."
				+ " ?sub c:Substation.Region ?sgr."
				+ " ?sgr c:IdentifiedObject.name ?"+SUBREGION_NAME+"."
				+ " ?sgr c:IdentifiedObject.mRID ?"+SUBREGION_ID+"."
				+ " ?sgr c:SubGeographicalRegion.Region ?rgn."
				+ " ?rgn c:IdentifiedObject.name ?"+REGION_NAME+"."
				+ " ?rgn c:IdentifiedObject.mRID ?"+REGION_ID+"."
				+ "} ORDER by  ?"+FEEDER_NAME;
		return queryResultSet(null, modelNameQuery, processId, username);
	}
	
	public void insertAllMeasurements( String processId, String username, String baseDirectory)  throws Exception{
			//do insert measuremnt
			File tempDataPathDir = null;
			if(baseDirectory!=null){
				tempDataPathDir = new File(baseDirectory);
				if(!tempDataPathDir.exists()){
					tempDataPathDir.mkdirs();
				}
			} else {
				String simulationConfigDir = configManager.getConfigurationProperty(GridAppsDConstants.GRIDAPPSD_TEMP_PATH);
				if (simulationConfigDir == null || simulationConfigDir.trim().length()==0) {
					logManager.error(ProcessStatus.ERROR, processId, "No temporary data location returned for request "+ processId);
					throw new Exception("No temporary data location  returned for request "
							+ processId);
				}
				if(!simulationConfigDir.endsWith(File.separator)){
					simulationConfigDir = simulationConfigDir+File.separator;
				}
				simulationConfigDir = simulationConfigDir+processId+File.separator;
				tempDataPathDir = new File(simulationConfigDir);
				if(!tempDataPathDir.exists()){
					tempDataPathDir.mkdirs();
				}
			}
			
			
			ResultSet rs = queryModelNamesAndIdsResultSet(processId, username);
			while( rs.hasNext()) {
				QuerySolution qs = rs.nextSolution();
				String feederName = qs.getLiteral(FEEDER_NAME).getString();
				String feederId = qs.getLiteral(FEEDER_ID).getString();
				models.put(feederName, feederId);
			}
			
			
			// TODO Auto-generated method stub
//	        list_all_measurements to generate lines/loads/machines/etc for all models  (models in config file in conf)
//			listAllMeasurements(processId, username);
//	        drop all measurements
//			dropAllMeasurements(processId, username);
//	        iterate through list of models (acep, apriJ1, ieee123, ieee123pv, ieee13assets, ieee13ochre, ieee13node, ieee37, ieee8500, ieee9500, ieee8500enh, r2_12_47_2, transactive, final9500) 
//	             and for lines, load, machines, node_v, special, switch_i, xfmr_pq do the stuff in InsertMeasurement.py
			for (String modelName: models.keySet()){
				insertMeasurements(modelName, models.get(modelName), processId, username, tempDataPathDir.getAbsolutePath());
			}
			
//			insertAllMeasurements(modelName,  processId, username);
			
	}
	
	
	@Override
	public void insertMeasurements(String modelName, String modelId, String processId, String username, String baseDirectory) {
		int batchSize = 150;
//insert_measurements (model_name)
//list measurements for model
//        delete measurement (model_name)
//        call insert measurement for the model
		try {
			listMeasurements(modelId, modelName, baseDirectory, processId, username);
			dropMeasurements(modelId, modelName, processId, username);

			String[] fileTypes = new String[]{"_lines_pq.txt", "_loads.txt", "_machines.txt", "_node_v.txt", "_special.txt", "_xfmr_pq.txt"};
			
//			python3 $CIMHUB_UTILS/InsertMeasurements.py cimhubconfig.json ./Meas/acep_psil_lines_pq.txt  ./Meas/acep_msid.json
//			python3 $CIMHUB_UTILS/InsertMeasurements.py cimhubconfig.json ./Meas/acep_psil_loads.txt     ./Meas/acep_msid.json
//			python3 $CIMHUB_UTILS/InsertMeasurements.py cimhubconfig.json ./Meas/acep_psil_machines.txt  ./Meas/acep_msid.json
//			python3 $CIMHUB_UTILS/InsertMeasurements.py cimhubconfig.json ./Meas/acep_psil_node_v.txt    ./Meas/acep_msid.json
//			python3 $CIMHUB_UTILS/InsertMeasurements.py cimhubconfig.json ./Meas/acep_psil_special.txt   ./Meas/acep_msid.json
//			python3 $CIMHUB_UTILS/InsertMeasurements.py cimhubconfig.json ./Meas/acep_psil_xfmr_pq.txt   ./Meas/acep_msid.json
			File uuidFile = new File(baseDirectory+File.separator+modelName+"_msid.json");
			HashMap<String, String> uuidDict = new HashMap<String, String>();
			if(uuidFile.exists()){
				JsonObject obj = (JsonObject)new JsonParser().parse(new FileReader(uuidFile));
				for(Entry<String, JsonElement> entry: obj.entrySet()){
					uuidDict.put(entry.getKey(), entry.getValue().getAsString());
				}
			}
	        

			
			List<String> qTriples = new ArrayList<String>();
			for(String fileType: fileTypes) {
				File f = new File(baseDirectory+File.separator+modelName+fileType);
				if(f.exists()) {
					BufferedReader br = new BufferedReader(new FileReader(f));
					String line;
					while ((line = br.readLine()) != null) {
						// process the line.
						String[] splitStr = line.trim().split("\\s+");
						String element0 = splitStr[0];
						String element1 = splitStr[1];
						
						if("LinearShuntCompensator".equals(element0)){
							String name = "LinearShuntCompensator_" + element1;
							String key = name + ':' + splitStr[2] + ':' + splitStr[3];
							qTriples.add(createMeasurementString("Analog", getMeasurementID (key + ":PNV", uuidDict), name, splitStr[4], splitStr[5], "PNV", splitStr[3]));
							qTriples.add(createMeasurementString("Analog", getMeasurementID (key + ":VA", uuidDict), name, splitStr[4], splitStr[5], "VA", splitStr[3]));
							qTriples.add(createMeasurementString("Discrete", getMeasurementID (key + ":Pos", uuidDict), name, splitStr[4], splitStr[5], "Pos", splitStr[3]));
							
						} else if ("PowerTransformer".equals(element0) ){
							if( "RatioTapChanger".equals(element1)){
								String name = "RatioTapChanger_" + splitStr[2];
								String key = name + ':' + splitStr[4] + ':' + splitStr[5];
								qTriples.add(createMeasurementString("Discrete", getMeasurementID (key + ":Pos", uuidDict), name, splitStr[6], splitStr[7], "Pos", splitStr[5]));
							} else if ("PowerTransformerEnd".equals(element1)){
								String what = splitStr[2];
								String name = "PowerTransformer_" + splitStr[3];
								String key = name + ':' + splitStr[5] + ':' + splitStr[6];
								
								if(what.contains("v")){
									qTriples.add(createMeasurementString("Analog", getMeasurementID (key + ":PNV", uuidDict), name+ "_Voltage", splitStr[7], splitStr[8], "PNV", splitStr[6]));
								} else if(what.contains("s")){
									qTriples.add(createMeasurementString("Analog", getMeasurementID (key + ":VA", uuidDict), name+ "_Power", splitStr[7], splitStr[8], "VA", splitStr[6]));
								} else if(what.contains("i")){
									qTriples.add(createMeasurementString("Analog", getMeasurementID (key + ":A", uuidDict), name+ "_Current", splitStr[7], splitStr[8], "A", splitStr[6]));
								}
							}
						} else if ("EnergyConsumer".equals(element0)){
							String name = "EnergyConsumer_" + element1;
							String key = name + ':' + splitStr[2] + ':' + splitStr[3];
							qTriples.add(createMeasurementString("Analog", getMeasurementID (key + ":PNV", uuidDict), name, splitStr[4], splitStr[5], "PNV", splitStr[3]));
							qTriples.add(createMeasurementString("Analog", getMeasurementID (key + ":VA", uuidDict), name, splitStr[4], splitStr[5], "VA", splitStr[3]));
						} else if ("SynchronousMachine".equals(element0)) {
							String name = "SynchronousMachine_" + element1;
							String key = name + ':' + splitStr[2] + ':' + splitStr[3];
							qTriples.add(createMeasurementString("Analog", getMeasurementID (key + ":PNV", uuidDict), name, splitStr[4], splitStr[5], "PNV", splitStr[3]));
							qTriples.add(createMeasurementString("Analog", getMeasurementID (key + ":VA", uuidDict), name, splitStr[4], splitStr[5], "VA", splitStr[3]));
						} else if ("PowerElectronicsConnection".equals(element0) ){
							String phases = splitStr[5];
							if("PhotovoltaicUnit".equals(element1)) {
								String name = "PowerElectronicsConnection_PhotovoltaicUnit_" + splitStr[3];
								String key = name + ':' + splitStr[4] + ':' + splitStr[5];
								qTriples.add(createMeasurementString("Analog", getMeasurementID (key + ":PNV", uuidDict), name, splitStr[6], splitStr[7], "PNV", phases));
								qTriples.add(createMeasurementString("Analog", getMeasurementID (key + ":VA", uuidDict), name, splitStr[6], splitStr[7], "VA", phases));
							} else if ("BatteryUnit".equals(element1)){
								String name = "PowerElectronicsConnection_BatteryUnit_" + splitStr[3];
								String key = name + ':' + splitStr[4] + ':' + splitStr[5];
								if("SoC".equals(phases)){
									qTriples.add(createMeasurementString("Analog", getMeasurementID (key + ":SoC", uuidDict), name, splitStr[6], splitStr[7], "SoC", phases));
								} else {
									qTriples.add(createMeasurementString("Analog", getMeasurementID (key + ":PNV", uuidDict), name, splitStr[6], splitStr[7], "PNV", phases));
									qTriples.add(createMeasurementString("Analog", getMeasurementID (key + ":VA", uuidDict), name, splitStr[6], splitStr[7], "VA", phases));
								}
								
							}
						} else if ("ACLineSegment".equals(element0) || "LoadBreakSwitch".equals(element0) || "Breaker".equals(element0) || "Recloser".equals(element0)) {
							String name = element0 + "_" + splitStr[2];
							String key = name + ':' + element1 + ':' + splitStr[5];
							String trmid = "";
							if(element1.contains("1")){
								trmid = splitStr[7];
							} else {
								trmid = splitStr[8];
							}
							
							if(element1.contains("v")){
								qTriples.add(createMeasurementString("Analog", getMeasurementID (key + ":PNV", uuidDict), name+ "_Voltage", splitStr[6], trmid, "PNV", splitStr[5]));
							} else if(element1.contains("s")){
								qTriples.add(createMeasurementString("Analog", getMeasurementID (key + ":VA", uuidDict), name+ "_Power", splitStr[6], trmid, "VA", splitStr[5]));
							} else if(element1.contains("i")){
								qTriples.add(createMeasurementString("Analog", getMeasurementID (key + ":A", uuidDict), name+ "_Current", splitStr[6], trmid, "A", splitStr[5]));
							}
						} else {
							System.out.println("Don't know what to do with "+element0);
						}
						
						
						if(qTriples.size()>batchSize){
							postMeasurements(qTriples, modelId, processId, username);
							qTriples = new ArrayList<String>();
						}
						
						
					} //while lines.next()
					br.close();
					
					
					if(qTriples.size()>0) {
						postMeasurements(qTriples, modelId, processId, username);
						qTriples = new ArrayList<String>();
					}

					// write uuidDict back to file
					JsonObject obj = new JsonObject();
					List<String> keyNames = new ArrayList<String>();
					keyNames.addAll(uuidDict.keySet());
					Collections.sort(keyNames);
					for(String key: keyNames){
						obj.add(key, new JsonPrimitive(uuidDict.get(key)));
					}
					Gson gson = new GsonBuilder().setPrettyPrinting().create(); 
					FileWriter fw = new FileWriter(uuidFile);
					gson.toJson(obj, fw);
					fw.flush();
					fw.close();
					
					
					
				} else {   //if file.exists()
					//TODO throw error/warning
				}
			}  // for filetypes

			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	protected String getMeasurementID(String key, HashMap<String, String> uuidDict){
		if(uuidDict.containsKey(key)){
			return uuidDict.get(key);
		} else {
			String idNew = UUID.randomUUID().toString();
			//if not measid starts with _ then prepend it, this is here for consistency. 
			  // otherwise the mrids are uploaded without the initial _
			if(!idNew.startsWith("_")){
				idNew = "_"+idNew;
			}
			uuidDict.put(key, idNew);
			return idNew;
		}
	}
	protected String createMeasurementString(String meascls, String measid, String eqname, String eqid, String trmid, String meastype, String phases) {
		//if not measid starts with _ then prepend it, this is here for consistency. otherwise the mrids are uploaded without the initial _
		if (!measid.startsWith("_")){
			measid = "_"+measid;
		}
		String resource = "<" + getEndpointURL(measid) + "#" + measid + "> ";
		String equipment = "<" + getEndpointURL(measid) + "#" + eqid + "> ";
		String terminal = "<" + getEndpointURL(measid) + "#" + trmid + "> ";
		String result = resource + " a c:" + meascls + ". "+ 
				resource + " c:IdentifiedObject.mRID \"" + measid + "\". "+
				resource + " c:IdentifiedObject.name \"" + eqname + "\". "+
				resource + " c:Measurement.PowerSystemResource " + equipment + ". "+
				resource + " c:Measurement.Terminal " + terminal + ". "+
				resource + " c:Measurement.phases <" + nsCIM + "PhaseCode." + phases + ">. "+
				resource + " c:Measurement.measurementType \"" + meastype + "\".";
		
		
//		System.out.println(result);
		return result;
	}
	protected void postMeasurements(List<String> triples, String modelId, String processId, String username){
		String endpoint = getEndpointURL(modelId);
		BlazegraphQueryHandler queryHandler = new BlazegraphQueryHandler(endpoint, logManager, processId, username);
		String qstr = "INSERT DATA { " + String.join(" ",triples) +" }";
//		System.out.println(qstr);
//		System.out.println(QueryHandler.Q_PREFIX);
		queryHandler.executeUpdateQuery(qstr);
	}

	protected List<String> listAllMeasurements(String processId, String username, String baseDirectory) {

//        iterate through all model names and call list measurements, save to tmp directory
//list_measurements(model_name)
//		try{
//		FileUtils.cleanDirectory(new File(baseDirectory)); 
//		}catch (Exception e) {
//			// TODO: handle exception
//			e.printStackTrace();
//		}
		for(String modelName: models.keySet()){
			try{
				listMeasurements(models.get(modelName), modelName, baseDirectory, processId, username);
			}catch (FileNotFoundException e) {
				// TODO: handle exception
				e.printStackTrace();
			}catch (IOException e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}
		
		
		return null;
	}

	protected List<String> listMeasurements(String modelId, String modelName, String baseDirectory, String processId, String username) throws IOException {
		System.out.println("CALLING LIST MEAS "+modelName);

		//TODO What about phase ABCN?  does it go to A,B,C,N? or []
		//How are the msid.json files created?
		
		//Delete the existing measurement files for this model
		File dir = new File(baseDirectory);
		FileFilter fileFilter = new WildcardFileFilter(modelName+"_*.txt");
		File[] files = dir.listFiles(fileFilter);
		for (File file: files) {
		   file.delete();
		}
		
		FileOutputStream op = new FileOutputStream(baseDirectory+modelName+"_special.txt");
		FileOutputStream np = new FileOutputStream(baseDirectory+modelName+"_node_v.txt");
		
		
		// start by listing all the buses
		HashMap<String, HashMap<String, Boolean>> busPhases = new HashMap<String, HashMap<String, Boolean>>();
		String qstr = "SELECT ?bus WHERE { VALUES ?fdrid {\"" + modelId + "\"}"+
				"?fdr c:IdentifiedObject.mRID ?fdrid."+
				"?s c:ConnectivityNode.ConnectivityNodeContainer ?fdr."+
				"?s r:type c:ConnectivityNode."+
				"?s c:IdentifiedObject.name ?bus."+
				"}"+
				"ORDER by ?bus";
		ResultSet ret = queryResultSet(modelId, qstr, processId, username);
		while(ret.hasNext()){
			QuerySolution qs = ret.nextSolution();
			String busName = qs.getLiteral("bus").toString();
			HashMap<String, Boolean> busValues = new HashMap<String, Boolean>();
			busValues = createBusValues();
			busPhases.put(busName, busValues);
			//busphases[b['bus'].value] = {'A':False, 'B':False, 'C':False, 's1': False, 's2': False}
		}
		
		// capacitors
		qstr = "SELECT ?name ?bus ?phases ?eqid ?trmid WHERE { VALUES ?fdrid {\""+modelId+"\"}"+
				"?s c:Equipment.EquipmentContainer ?fdr."+
				"?fdr c:IdentifiedObject.mRID ?fdrid."+ 
				"?s r:type c:LinearShuntCompensator."+
				"?s c:IdentifiedObject.name ?name. "+
				"?s c:IdentifiedObject.mRID ?eqid. "+
				"?t c:Terminal.ConductingEquipment ?s. "+
				"?t c:IdentifiedObject.mRID ?trmid. "+
				"?t c:Terminal.ConnectivityNode ?cn. "+
				"?cn c:IdentifiedObject.name ?bus. "+
				"OPTIONAL {?scp c:ShuntCompensatorPhase.ShuntCompensator ?s. "+
				"?scp c:ShuntCompensatorPhase.phase ?phsraw. "+
				"  bind(strafter(str(?phsraw),\"SinglePhaseKind.\") as ?phases) } }";
		
		 ret = queryResultSet(modelId, qstr, processId, username);
		//#print ('\nLinearShuntCompensator binding keys are:',ret.variables)
		 while(ret.hasNext()){
			 QuerySolution qs = ret.nextSolution();
			 String busName = qs.getLiteral("bus").toString();
//		  bus = b['bus'].value
			 String phases = "ABC";
			 if (qs.contains("phases")){ // was OPTIONAL in the query
				 phases = getLiteral(qs, "phases");
			 }
			 if(busName.equals("684")){
				 System.out.println("PHASES "+phases);
			 }
			 String[] phaseList = mapPhases(phases);
			 HashMap<String, Boolean> busValues;
			 if(busPhases.containsKey(busName)){
				 busValues = busPhases.get(busName);
			 } else {
				 busValues = createBusValues();
				 busPhases.put(busName, busValues);
			 }
			 for(String phase: phaseList){
				 busValues.put(phase, true);
				 op.write(new String("LinearShuntCompensator "+qs.getLiteral("name").toString()+" "+busName+" "+phase+" "+qs.getLiteral("eqid").toString()+" "+qs.getLiteral("trmid").toString()+"\r\n").getBytes());
				//print ('LinearShuntCompensator',b['name'].value,bus,phs,b['eqid'].value,b['trmid'].value,file=op)
			 }
		 }
		 
		 
		 // regulators
		 qstr = "SELECT ?name ?wnum ?bus (group_concat(distinct ?phs;separator=\"\") as ?phases) ?eqid ?trmid WHERE { "+
				 "SELECT ?name ?wnum ?bus ?phs ?eqid ?trmid WHERE { VALUES ?fdrid {\""+modelId+"\"}"+
				 "?s c:Equipment.EquipmentContainer ?fdr. "+
				 "?fdr c:IdentifiedObject.mRID ?fdrid. "+
				 "?rtc r:type c:RatioTapChanger. "+
				 "?rtc c:IdentifiedObject.name ?rname. "+
				 "?rtc c:IdentifiedObject.mRID ?rtcid. "+
				 "?rtc c:RatioTapChanger.TransformerEnd ?end. "+
				 "?end c:TransformerEnd.endNumber ?wnum. "+
				 "?end c:TransformerEnd.Terminal ?trm. "+
				 "?trm c:IdentifiedObject.mRID ?trmid. "+
				 "?trm c:Terminal.ConnectivityNode ?cn. "+
				 "?cn c:IdentifiedObject.name ?bus. "+
				 "OPTIONAL {?end c:TransformerTankEnd.phases ?phsraw. "+
				 " bind(strafter(str(?phsraw),\"PhaseCode.\") as ?phs)} "+
				 "?end c:TransformerTankEnd.TransformerTank ?tank. "+
				 "?tank c:TransformerTank.PowerTransformer ?s. "+
				 "?s c:IdentifiedObject.name ?name. "+
				 "?s c:IdentifiedObject.mRID ?eqid. "+
				 "?tank c:IdentifiedObject.name ?tname. "+
				 "} ORDER BY ?name ?phs "+
				 "} "+
				 "GROUP BY ?name ?wnum ?bus ?eqid ?trmid "+
				 "ORDER BY ?name";
		 
		 ret = queryResultSet(modelId, qstr, processId, username);
		 while(ret.hasNext()){
			 QuerySolution qs = ret.nextSolution();
			 String phases = "ABC";
			 if (qs.contains("phases")){ // was OPTIONAL in the query
				 phases = qs.getLiteral("phases").toString();
			 }
			 String busName = getLiteral(qs, "bus");
			 String[] phaseList = mapPhases(phases);
			 for(String phase: phaseList){
				 op.write(new String("PowerTransformer RatioTapChanger "+qs.getLiteral("name").toString()+" "+qs.getLiteral("wnum").toString()+" "+busName+" "+phase+" "+qs.getLiteral("eqid").toString()+" "+qs.getLiteral("trmid").toString()+"\r\n").getBytes());

			 }
		 }

		 // Storage
		 qstr = "SELECT ?name ?uname ?bus (group_concat(distinct ?phs;separator=\"\") as ?phases) ?eqid ?trmid WHERE {"+
		   "SELECT ?name ?uname ?bus ?phs ?eqid ?trmid WHERE { VALUES ?fdrid {\""+modelId+"\"}"+
				 "?s c:Equipment.EquipmentContainer ?fdr. "+
				 "?fdr c:IdentifiedObject.mRID ?fdrid. "+
		  "?s r:type c:PowerElectronicsConnection. "+
		  "?s c:IdentifiedObject.name ?name. "+
		  "?s c:IdentifiedObject.mRID ?eqid. "+
		  "?peu r:type c:BatteryUnit. "+
		  "?peu c:IdentifiedObject.name ?uname. "+
		  "?s c:PowerElectronicsConnection.PowerElectronicsUnit ?peu. "+
		  "?t1 c:Terminal.ConductingEquipment ?s. "+
		  "?t1 c:IdentifiedObject.mRID ?trmid. "+
		  "?t1 c:ACDCTerminal.sequenceNumber \"1\". "+
		  "?t1 c:Terminal.ConnectivityNode ?cn1. "+
		  "?cn1 c:IdentifiedObject.name ?bus. "+
		  "OPTIONAL {?pep c:PowerElectronicsConnectionPhase.PowerElectronicsConnection ?s. "+
		  "?pep c:PowerElectronicsConnectionPhase.phase ?phsraw. "+
		  " bind(strafter(str(?phsraw),\"SinglePhaseKind.\") as ?phs) } } ORDER BY ?name ?phs "+
		  "} GROUP BY ?name ?uname ?bus ?eqid ?trmid "+
		  "ORDER BY ?name";
		 
		 ret = queryResultSet(modelId, qstr, processId, username);
		 while(ret.hasNext()){
			 QuerySolution qs = ret.nextSolution();
			 String busName = qs.getLiteral("bus").toString();
			 String phases = "ABC";
			 if (qs.contains("phases")){ // was OPTIONAL in the query
				 phases = qs.getLiteral("phases").toString();
			 }
			 String[] phaseList = mapPhases(phases);
			 op.write(new String("PowerElectronicsConnection BatteryUnit "+qs.getLiteral("name").toString()+" "+qs.getLiteral("uname").toString()+" "+busName+" SoC "+qs.getLiteral("eqid").toString()+" "+qs.getLiteral("trmid").toString()+"\r\n").getBytes());
			 for(String phase: phaseList){
				 HashMap<String, Boolean> busValues;
				 if(busPhases.containsKey(busName)){
					 busValues = busPhases.get(busName);
				 } else {
					 busValues = createBusValues();
					 busPhases.put(busName, busValues);
				 }
				 busValues.put(phase, true);
				 
				 op.write(new String("PowerElectronicsConnection BatteryUnit "+qs.getLiteral("name").toString()+" "+qs.getLiteral("uname").toString()+" "+busName+" "+phase+" "+qs.getLiteral("eqid").toString()+" "+qs.getLiteral("trmid").toString()+"\r\n").getBytes());
			 }
		 }

		 //Solar
		 qstr = "SELECT ?name ?uname ?bus (group_concat(distinct ?phs;separator=\"\") as ?phases) ?eqid ?trmid WHERE {"+
		   "SELECT ?name ?uname ?bus ?phs ?eqid ?trmid WHERE {VALUES ?fdrid {\""+modelId+"\"}"+
				 "?s c:Equipment.EquipmentContainer ?fdr. "+
				 "?fdr c:IdentifiedObject.mRID ?fdrid. "+
		  "?s r:type c:PowerElectronicsConnection. "+
		  "?s c:IdentifiedObject.name ?name. "+
		  "?s c:IdentifiedObject.mRID ?eqid. "+ 
		  "?peu r:type c:PhotovoltaicUnit. "+
		  "?peu c:IdentifiedObject.name ?uname. "+
		  "?s c:PowerElectronicsConnection.PowerElectronicsUnit ?peu. "+
		  "?t1 c:Terminal.ConductingEquipment ?s. "+
		  "?t1 c:IdentifiedObject.mRID ?trmid.  "+
		  "?t1 c:ACDCTerminal.sequenceNumber \"1\". "+
		  "?t1 c:Terminal.ConnectivityNode ?cn1.  "+
		  "?cn1 c:IdentifiedObject.name ?bus. "+
		  "OPTIONAL {?pep c:PowerElectronicsConnectionPhase.PowerElectronicsConnection ?s. "+
		  "?pep c:PowerElectronicsConnectionPhase.phase ?phsraw. "+
		   "bind(strafter(str(?phsraw),\"SinglePhaseKind.\") as ?phs) } } ORDER BY ?name ?phs "+
		  "} GROUP BY ?name ?uname ?bus ?eqid ?trmid "+
		  "ORDER BY ?name";
		 
		 ret = queryResultSet(modelId, qstr, processId, username);
		 while(ret.hasNext()){
			 QuerySolution qs = ret.nextSolution();
			 String busName = qs.getLiteral("bus").toString();
			 String phases = "ABC";
			 if (qs.contains("phases")){ // was OPTIONAL in the query
				 phases = qs.getLiteral("phases").toString();
			 }
			 String[] phaseList = mapPhases(phases);
			 for(String phase: phaseList){
				 HashMap<String, Boolean> busValues;
				 if(busPhases.containsKey(busName)){
					 busValues = busPhases.get(busName);
				 } else {
					 busValues = createBusValues();
					 busPhases.put(busName, busValues);
				 }
				 busValues.put(phase, true);
				 
				 op.write(new String("PowerElectronicsConnection PhotovoltaicUnit "+qs.getLiteral("name").toString()+" "+qs.getLiteral("uname").toString()+" "+busName+" "+phase+" "+qs.getLiteral("eqid").toString()+" "+qs.getLiteral("trmid").toString()+"\r\n").getBytes());

			 }
		 }
		 op.close();
		 //LoadBreakSwitches, Breakers and Reclosers
		 op = new FileOutputStream(baseDirectory+modelName+"_switch_i.txt");

		 qstr = "SELECT ?cimtype ?name ?bus1 ?bus2 (group_concat(distinct ?phs1;separator=\"\") as ?phases) ?eqid ?trm1id ?trm2id WHERE {"+
				  "SELECT ?cimtype ?name ?bus1 ?bus2 ?phs1 ?eqid ?trm1id ?trm2id WHERE { VALUES ?fdrid {\""+modelId+"\"}"+
				  "?s c:Equipment.EquipmentContainer ?fdr. "+
				  "?fdr c:IdentifiedObject.mRID ?fdrid. "+
				 "VALUES ?cimraw {c:LoadBreakSwitch c:Recloser c:Breaker} "+
				 "?s r:type ?cimraw. "+
				 " bind(strafter(str(?cimraw),\"#\") as ?cimtype) "+
				 "?s c:IdentifiedObject.name ?name. "+
				 "?s c:IdentifiedObject.mRID ?eqid.  "+
				 "?t1 c:Terminal.ConductingEquipment ?s. "+
				 "?t1 c:ACDCTerminal.sequenceNumber \"1\". "+
				 "?t1 c:IdentifiedObject.mRID ?trm1id.  "+
				 "?t1 c:Terminal.ConnectivityNode ?cn1.  "+
				 "?cn1 c:IdentifiedObject.name ?bus1. "+
				 "?t2 c:Terminal.ConductingEquipment ?s. "+
				 "?t2 c:ACDCTerminal.sequenceNumber \"2\". "+
				 "?t2 c:IdentifiedObject.mRID ?trm2id.  "+
				 "?t2 c:Terminal.ConnectivityNode ?cn2.  "+
				 "?cn2 c:IdentifiedObject.name ?bus2. "+
				 "OPTIONAL {?scp c:SwitchPhase.Switch ?s. "+
				 "?scp c:SwitchPhase.phaseSide1 ?phs1raw. "+
				 " bind(strafter(str(?phs1raw),\"SinglePhaseKind.\") as ?phs1) } } ORDER BY ?name ?phs1 "+
				 "} GROUP BY ?cimtype ?name ?bus1 ?bus2 ?eqid ?trm1id ?trm2id "+
				 "ORDER BY ?cimtype ?name";
		 ret = queryResultSet(modelId, qstr, processId, username);
		 while(ret.hasNext()){
			 QuerySolution qs = ret.nextSolution();
			 String busName1 = getLiteral(qs,"bus1");
			 String busName2 = getLiteral(qs,"bus2");
			 String phases = "ABC";
			 if (qs.contains("phases")){ // was OPTIONAL in the query
				 phases = getLiteral(qs,"phases");
			 }
			 String[] phaseList = mapPhases(phases);
			 for(String phase: phaseList){
				 op.write(new String(getLiteral(qs,"cimtype")+" i1 "+getLiteral(qs,"name")+" "+busName1+" "+busName2+" "+phase+" "+getLiteral(qs,"eqid")+" "+getLiteral(qs,"trm1id")+" "+getLiteral(qs,"trm2id")+"\r\n").getBytes());
				 
				 HashMap<String, Boolean> busValues;
				 if(busPhases.containsKey(busName1)){
					 busValues = busPhases.get(busName1);
					 if(!busValues.get(phase)){
						 np.write(new String(getLiteral(qs,"cimtype")+" v1 "+getLiteral(qs,"name")+" "+busName1+" "+busName2+" "+phase+" "+getLiteral(qs,"eqid")+" "+getLiteral(qs,"trm1id")+" "+getLiteral(qs,"trm2id")+"\r\n").getBytes());
						 busValues.put(phase, true);
					 }
				 } else {
					 System.out.println("BUS PHASE1 NOT FOUND "+busName1);
				 }
				 if(busPhases.containsKey(busName2)){
					 busValues = busPhases.get(busName2);
					 if(!busValues.get(phase)){
						 np.write(new String(getLiteral(qs,"cimtype")+" v2 "+getLiteral(qs,"name")+" "+busName1+" "+busName2+" "+phase+" "+getLiteral(qs,"eqid")+" "+getLiteral(qs,"trm1id")+" "+getLiteral(qs,"trm2id")+"\r\n").getBytes());
						 busValues.put(phase, true);
					 }
				 } else {
					 System.out.println("BUS PHASE2 NOT FOUND "+busName1);
				 }

			 }
		 }		
		 op.close();
		 //ACLineSegments
		 op = new FileOutputStream(baseDirectory+modelName+"_lines_pq.txt");
		 qstr = "SELECT ?name ?bus1 ?bus2 (group_concat(distinct ?phs;separator=\"\") as ?phases) ?eqid ?trm1id ?trm2id WHERE { "+
				  "SELECT ?name ?bus1 ?bus2 ?phs ?eqid ?trm1id ?trm2id WHERE {VALUES ?fdrid {\""+modelId+"\"}"+
				  "?s c:Equipment.EquipmentContainer ?fdr. "+
				  "?fdr c:IdentifiedObject.mRID ?fdrid. "+
				  "?s r:type c:ACLineSegment. "+
				  "?s c:IdentifiedObject.name ?name. "+
				  "?s c:IdentifiedObject.mRID ?eqid.  "+
				  "?t1 c:Terminal.ConductingEquipment ?s. "+
				  "?t1 c:ACDCTerminal.sequenceNumber \"1\". "+
				  "?t1 c:IdentifiedObject.mRID ?trm1id.  "+
				  "?t1 c:Terminal.ConnectivityNode ?cn1.  "+
				  "?cn1 c:IdentifiedObject.name ?bus1. "+
				  "?t2 c:Terminal.ConductingEquipment ?s. "+
				  "?t2 c:ACDCTerminal.sequenceNumber \"2\". "+
				  "?t2 c:IdentifiedObject.mRID ?trm2id.  "+
				  "?t2 c:Terminal.ConnectivityNode ?cn2.  "+
				  "?cn2 c:IdentifiedObject.name ?bus2. "+
				  "OPTIONAL {?acp c:ACLineSegmentPhase.ACLineSegment ?s. "+
				  "?acp c:ACLineSegmentPhase.phase ?phsraw. "+
				  " bind(strafter(str(?phsraw),\"SinglePhaseKind.\") as ?phs) } } ORDER BY ?name ?phs "+
				  "} GROUP BY ?name ?bus1 ?bus2 ?eqid ?trm1id ?trm2id "+
				  "ORDER BY ?name";
		 ret = queryResultSet(modelId, qstr, processId, username);
		 while(ret.hasNext()){
			 QuerySolution qs = ret.nextSolution();
			 String busName1 = getLiteral(qs,"bus1");
			 String busName2 = getLiteral(qs,"bus2");
			 String phases = "ABC";
			 if (qs.contains("phases")){ // was OPTIONAL in the query
				 phases = getLiteral(qs,"phases");
			 }
			 
			 String[] phaseList = mapPhases(phases);
			 if(phaseList==null){
				 System.out.println("NO MAPPING FOR "+phases);
			 }
			 for(String phase: phaseList){
				 op.write(new String("ACLineSegment s1 "+getLiteral(qs,"name")+" "+busName1+" "+busName2+" "+phase+" "+getLiteral(qs,"eqid")+" "+getLiteral(qs,"trm1id")+" "+getLiteral(qs,"trm2id")+"\r\n").getBytes());
				 
				 HashMap<String, Boolean> busValues;
				 if(busPhases.containsKey(busName1)){
					 busValues = busPhases.get(busName1);
					 if(!busValues.get(phase)){
						 np.write(new String("ACLineSegment v1 "+getLiteral(qs,"name")+" "+busName1+" "+busName2+" "+phase+" "+getLiteral(qs,"eqid")+" "+getLiteral(qs,"trm1id")+" "+getLiteral(qs,"trm2id")+"\r\n").getBytes());
						 busValues.put(phase, true);
					 }
				 } else {
					 System.out.println("BUS PHASE1 NOT FOUND "+busName1);
				 }
				 if(busPhases.containsKey(busName2)){
					 busValues = busPhases.get(busName2);
					 if(!busValues.get(phase)){
						 np.write(new String("ACLineSegment v2 "+getLiteral(qs,"name")+" "+busName1+" "+busName2+" "+phase+" "+getLiteral(qs,"eqid")+" "+getLiteral(qs,"trm1id")+" "+getLiteral(qs,"trm2id")+"\r\n").getBytes());
						 busValues.put(phase, true);
					 }
				 } else {
					 System.out.println("BUS PHASE2 NOT FOUND "+busName1);
				 }

			 }
		 }	
		 op.close();
		 
		 
		 // EnergyConsumer
		 op = new FileOutputStream(baseDirectory+modelName+"_loads.txt");
		 qstr = "SELECT ?name ?bus (group_concat(distinct ?phs;separator=\"\") as ?phases) ?eqid ?trmid WHERE { "+
				  "SELECT ?name ?bus ?phs ?eqid ?trmid WHERE {VALUES ?fdrid {\""+modelId+"\"}"+
				  "?s c:Equipment.EquipmentContainer ?fdr. "+
				  "?fdr c:IdentifiedObject.mRID ?fdrid. "+
				  "?s r:type c:EnergyConsumer. "+
				  "?s c:IdentifiedObject.name ?name. "+
				  "?s c:IdentifiedObject.mRID ?eqid.  "+
				  "?t1 c:Terminal.ConductingEquipment ?s. "+
				  "?t1 c:IdentifiedObject.mRID ?trmid.  "+
				  "?t1 c:ACDCTerminal.sequenceNumber \"1\". "+
				  "?t1 c:Terminal.ConnectivityNode ?cn1.  "+
				  "?cn1 c:IdentifiedObject.name ?bus. "+
				  "OPTIONAL {?acp c:EnergyConsumerPhase.EnergyConsumer ?s. "+
				  "?acp c:EnergyConsumerPhase.phase ?phsraw. "+
				  " bind(strafter(str(?phsraw),\"SinglePhaseKind.\") as ?phs) } } ORDER BY ?name ?phs "+
				  "} GROUP BY ?name ?bus ?eqid ?trmid "+
				  "ORDER BY ?name ";
		 
		 ret = queryResultSet(modelId, qstr, processId, username);
		 while(ret.hasNext()){
			 QuerySolution qs = ret.nextSolution();
			 String busName = getLiteral(qs,"bus");
			 String phases = "ABC";
			 if (qs.contains("phases")){ // was OPTIONAL in the query
				 phases = getLiteral(qs,"phases");
			 }
			 String[] phaseList = mapPhases(phases);
			 for(String phase: phaseList){
				 op.write(new String("EnergyConsumer "+getLiteral(qs,"name")+" "+busName+" "+phase+" "+getLiteral(qs,"eqid")+" "+getLiteral(qs,"trmid")+"\r\n").getBytes());
			 }
		 }
		 op.close();
		 
		 
		 // Synchronous Machines
		 op = new FileOutputStream(baseDirectory+modelName+"_machines.txt");

		 qstr = "SELECT ?name ?bus ?eqid ?trmid WHERE { VALUES ?fdrid {\""+modelId+"\"}"+
				  "?s c:Equipment.EquipmentContainer ?fdr. "+
				  "?fdr c:IdentifiedObject.mRID ?fdrid. "+
				  "?s r:type c:SynchronousMachine. "+
				  "?s c:IdentifiedObject.name ?name. "+
				  "?s c:IdentifiedObject.mRID ?eqid.  "+
				  "?t1 c:Terminal.ConductingEquipment ?s. "+
				  "?t1 c:IdentifiedObject.mRID ?trmid.  "+
				  "?t1 c:ACDCTerminal.sequenceNumber \"1\". "+
				  "?t1 c:Terminal.ConnectivityNode ?cn1.  "+
				  "?cn1 c:IdentifiedObject.name ?bus. "+
				  "} "+
				  "ORDER BY ?name";
		 ret = queryResultSet(modelId, qstr, processId, username);
		 while(ret.hasNext()){
			 QuerySolution qs = ret.nextSolution();
			 String busName = getLiteral(qs,"bus");
			 String phases = "ABC";
			 String[] phaseList = mapPhases(phases);
			 for(String phase: phaseList){
				 op.write(new String("SynchronousMachine "+getLiteral(qs,"name")+" "+busName+" "+phase+" "+getLiteral(qs,"eqid")+" "+getLiteral(qs,"trmid")+"\r\n").getBytes());
			 }
		 }
		 op.close();


		 // PowerTransformer, no tanks
		 op = new FileOutputStream(baseDirectory+modelName+"_xfmr_pq.txt");

		 qstr = "SELECT ?name ?wnum ?bus ?eqid ?trmid WHERE {VALUES ?fdrid {\""+modelId+"\"}"+
				  "?s c:Equipment.EquipmentContainer ?fdr. "+
				  "?fdr c:IdentifiedObject.mRID ?fdrid. "+
				  "?s r:type c:PowerTransformer. "+
				  "?s c:IdentifiedObject.name ?name. "+
				  "?s c:IdentifiedObject.mRID ?eqid. "+
				  "?end c:PowerTransformerEnd.PowerTransformer ?s. "+
				  "?end c:TransformerEnd.Terminal ?trm. "+
				  "?end c:TransformerEnd.endNumber ?wnum. "+
				  "?trm c:IdentifiedObject.mRID ?trmid.  "+
				  "?trm c:Terminal.ConnectivityNode ?cn.  "+
				  "?cn c:IdentifiedObject.name ?bus. "+
				  "} "+
				  "ORDER BY ?name ?wnum";
		 ret = queryResultSet(modelId, qstr, processId, username);
		 while(ret.hasNext()){
			 QuerySolution qs = ret.nextSolution();
			 String busName = getLiteral(qs,"bus");
			 String phases = "ABC";
			 String[] phaseList = mapPhases(phases);
			 for(String phase: phaseList){
				 op.write(new String("PowerTransformer PowerTransformerEnd s1 "+getLiteral(qs,"name")+" "+getLiteral(qs,"wnum")+" "+busName+" "+phase+" "+getLiteral(qs,"eqid")+" "+getLiteral(qs,"trmid")+"\r\n").getBytes());
				 HashMap<String, Boolean> busValues;
				 if(busPhases.containsKey(busName)){
					 busValues = busPhases.get(busName);
					 if(!busValues.get(phase)){
						 np.write(new String("PowerTransformer PowerTransformerEnd v1 "+getLiteral(qs,"name")+" "+getLiteral(qs,"wnum")+" "+busName+" "+phase+" "+getLiteral(qs,"eqid")+" "+getLiteral(qs,"trmid")+"\r\n").getBytes());
						 busValues.put(phase, true);
					 }
				 } else {
					 System.out.println("BUS PHASE NOT FOUND "+busName);
				 }
			 }
		 }
		 
		 // PowerTransformer, with tanks
		 qstr = "SELECT ?name ?wnum ?bus ?phases ?eqid ?trmid WHERE {VALUES ?fdrid {\""+modelId+"\"}"+
				  "?s c:Equipment.EquipmentContainer ?fdr. "+
				  "?fdr c:IdentifiedObject.mRID ?fdrid. "+
				  "?s r:type c:PowerTransformer. "+
				  "?s c:IdentifiedObject.name ?name. "+
				  "?s c:IdentifiedObject.mRID ?eqid. "+
				  "?tank c:TransformerTank.PowerTransformer ?s. "+
				  "?end c:TransformerTankEnd.TransformerTank ?tank. "+
				  "?end c:TransformerEnd.Terminal ?trm. "+
				  "?end c:TransformerEnd.endNumber ?wnum. "+
				  "?trm c:IdentifiedObject.mRID ?trmid.  "+
				  "?trm c:Terminal.ConnectivityNode ?cn.  "+
				  "?cn c:IdentifiedObject.name ?bus. "+
				  "OPTIONAL {?end c:TransformerTankEnd.phases ?phsraw. "+
				  " bind(strafter(str(?phsraw),\"PhaseCode.\") as ?phases)} "+
				  "} "+
				  "ORDER BY ?name ?wnum ?phs";
		 ret = queryResultSet(modelId, qstr, processId, username);
		 while(ret.hasNext()){
			 QuerySolution qs = ret.nextSolution();
			 String busName = getLiteral(qs,"bus");
			 String phases = getLiteral(qs,"phases");
			 String[] phaseList = mapPhases(phases);
			 for(String phase: phaseList){
				 op.write(new String("PowerTransformer TransformerTankEnd s1 "+getLiteral(qs,"name")+" "+getLiteral(qs,"wnum")+" "+busName+" "+phase+" "+getLiteral(qs,"eqid")+" "+getLiteral(qs,"trmid")+"\r\n").getBytes());
				 HashMap<String, Boolean> busValues;
				 if(busPhases.containsKey(busName)){
					 busValues = busPhases.get(busName);
					 if(!busValues.get(phase)){
						 np.write(new String("PowerTransformer TransformerTankEnd v1 "+getLiteral(qs,"name")+" "+getLiteral(qs,"wnum")+" "+busName+" "+phase+" "+getLiteral(qs,"eqid")+" "+getLiteral(qs,"trmid")+"\r\n").getBytes());
						 busValues.put(phase, true);
					 }
				 } else {
					 System.out.println("BUS PHASE NOT FOUND "+busName);
				 }
			 }
		 }

		 op.close();
		 np.close();
		 
		return null;
	}
	
	
	public void dropAllMeasurements(String processId, String username) {

//      iterate through all model names and call list measurements, save to tmp directory
//list_measurements(model_name)
		for(String modelName: models.keySet()){
			dropMeasurements(models.get(modelName), modelName, processId, username);
			
		}
		
		
	}
	
	public void dropMeasurements(String modelId, String modelName, String processId, String username)  {
		System.out.println("CALLING DROP MEAS "+modelName);
		String qstr = "DELETE { "+
				"?m a ?class. "+
				"?m c:IdentifiedObject.mRID ?uuid. "+
				"?m c:IdentifiedObject.name ?name. "+
				"?m c:Measurement.PowerSystemResource ?psr. "+
				"?m c:Measurement.Terminal ?trm. "+
				"?m c:Measurement.phases ?phases. "+
				"?m c:Measurement.measurementType ?type. "+
				"} WHERE { "+
				"VALUES ?fdrid {\""+modelId+"\"} "+
				"VALUES ?class {c:Analog c:Discrete} "+
				"?fdr c:IdentifiedObject.mRID ?fdrid.  "+
				"?eq c:Equipment.EquipmentContainer ?fdr. "+
				"?trm c:Terminal.ConductingEquipment ?eq. "+
				"?m a ?class. "+
				"?m c:IdentifiedObject.mRID ?uuid. "+
				"?m c:IdentifiedObject.name ?name. "+
				"?m c:Measurement.PowerSystemResource ?psr. "+
				"?m c:Measurement.Terminal ?trm. "+
				"?m c:Measurement.phases ?phases. "+
				"?m c:Measurement.measurementType ?type. "+
				"}";
		 try {
//			String ret = query(modelId, qstr, "JSON", processId, username);
//			System.out.println(ret);
			 String endpoint = getEndpointURL(modelId);
				BlazegraphQueryHandler queryHandler = new BlazegraphQueryHandler(endpoint, logManager, processId, username);
//			  queryHandler.query(qstr, null);
//			  queryHandler.construct(qstr);
				queryHandler.executeUpdateQuery(qstr);
			  
			  
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	
	private String getLiteral(QuerySolution qs, String name){
		try{
			return qs.getLiteral(name).toString();
		}catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			System.out.println("ERROR GETTING "+name+" "+qs.getLiteral(name));
			return "";
		}
	}
	private HashMap<String, Boolean> createBusValues(){
		HashMap<String, Boolean> busValues = new HashMap<String, Boolean>();
		busValues.put("A", false);
		busValues.put("B", false);
		busValues.put("C", false);
		busValues.put("s1", false);
		busValues.put("s2", false);
		return busValues;
	}
	private String[] mapPhases(String phases) {
		if (phases==null || phases.trim().length() < 1)
		    return new String[]{"A", "B", "C"};
		
		//Some of the phases come in different orders, fix this so that they are in alphabetical order
		if (!(phases.contains("s1") || phases.contains("s2"))){
	        char tempArray[] = phases.toCharArray();
	        Arrays.sort(tempArray);
	        phases =  new String(tempArray);
		}
		if (phases.contains("ABC"))
			return new String[]{"A", "B", "C"};
		if (phases.contains("AB"))
			return new String[]{"A", "B"};
		if (phases.contains("AC"))
			return new String[]{"A", "C"};
		if(phases.contains("BC"))
			return new String[]{"B", "C"};
		if (phases.contains("A"))
			return new String[]{"A"};
		if (phases.contains("B"))
			return new String[]{"B"};
		if (phases.contains("C"))
			return new String[]{"C"};
		if (phases.contains("s12"))
			return new String[]{"s12"};
		if (phases.contains("s1s2"))
			return new String[]{"s1", "s2"};
		if (phases.contains("s1"))
			return new String[]{"s1"};
		if (phases.contains("s2"))
			return new String[]{"s2"};
		return new String[]{};
	}
	
	@Override
	public void insertAllHouses( String processId, String username, String baseDirectory) {
		// TODO Auto-generated method stub
//	    for each model name look up settings and call insert houses
//
		for (String modelName: models.keySet()){
			dropHouses(modelName, models.get(modelName), processId, username, baseDirectory);
			insertHouses(modelName, models.get(modelName), processId, username, baseDirectory);
		}
		
//	    delete_all_houses
//	                  for each model name call delete_houses
//	    delete_houses(model_name)
//	    insert  houses (eeder mRID, region, seed value, file of persistent mRIDs, scaling factor on load)
//

	}
	
	
	@Override
	public void insertHouses(String modelName, String modelId, String processId, String username, String baseDirectory) {
		// We have to convert nominal voltages from three-phase phase-to-phase to phase
		// to ground. At the time of writing, even 240V loads use sqrt(3)...
		final double NOMVFACTOR = 3*0.5;
		// Triplex loads come back as 208V... This is wrong, but that's what's in the
		// CIM database.
		final int TRIPLEX_V = 208;
		// We'll let the user know how many 480 volt loads are in there, even though we 
				// won't be adding houses to them.
		final int COMMERCIAL_V = 480;

		// We need to define which housing properties are enumerations, as this uses 
		// a different syntax for sparql updates.
		HashMap<String, String> houseProperties = new HashMap<>();
		houseProperties.put("coolingSystem", "HouseCooling");
		houseProperties.put("heatingSystem", "HouseHeating");
		houseProperties.put("thermalIntegrity", "HouseThermalIntegrity");
		try{
			
			HashMap<String, String> uuidDict = new HashMap<String, String>();
			File uuidFile = new File(baseDirectory+File.separator+"houses"+File.separator+modelName+"_house_uuids.json");
			if(uuidFile.exists()){
				JsonObject obj = (JsonObject)new JsonParser().parse(new FileReader(uuidFile));
				for(Entry<String, JsonElement> entry: obj.entrySet()){
					uuidDict.put(entry.getKey(), entry.getValue().getAsString());
				}
			}
			
			
			 // Get the EnergyConsumers, commercial loads, and total magnitude of 
			 // residential energy consumer loads.
			/*Method to get nominal voltages from each 'EnergyConsumer.'
					  
					  Query source is Powergrid-Models/blazegraph/queries.txt, and it was 
					  modified from there.
					  
					  For now, we'll just get the 'bus' (which is really the object name in
					  the GridLAB-D model) and nominal voltage. Nominal voltage is given
					  as 3-phase phase-to-phase, even if we're talking about a split-phase.
					  I suppose that's what you get when you apply a transmission standard
					  to the secondary side of a distribution system...
					  
					  TODO: Later, we may want to act according to the load connection/phases */
					  
			String qStr = "SELECT ?name ?mrid ?bus ?basev ?p ?q ?conn (group_concat(distinct ?phs;separator=\",\") as ?phases) "+
				    "WHERE { "+
				      "?s r:type c:EnergyConsumer. "+
				      "VALUES ?fdrid {"+modelId+"} "+
				      "?s c:Equipment.EquipmentContainer ?fdr. "+
				      "?fdr c:IdentifiedObject.mRID ?fdrid. " +
				      "?s c:IdentifiedObject.name ?name. "+
				      "?s c:IdentifiedObject.mRID ?mrid. "+
				      "?s c:ConductingEquipment.BaseVoltage ?bv. "+
				      "?bv c:BaseVoltage.nominalVoltage ?basev. "+
				      "?s c:EnergyConsumer.p ?p."+
				      "?s c:EnergyConsumer.q ?q."+
				      "?s c:EnergyConsumer.phaseConnection ?connraw. "+
				      "bind(strafter(str(?connraw),\"PhaseShuntConnectionKind.\") as ?conn) "+
				      "OPTIONAL { "+
				        "?ecp c:EnergyConsumerPhase.EnergyConsumer ?s. "+
				        "?ecp c:EnergyConsumerPhase.phase ?phsraw. "+
				        "bind(strafter(str(?phsraw),\"SinglePhaseKind.\") as ?phs) "+
				      "} "+
				      "?t c:Terminal.ConductingEquipment ?s. " +
				      "?t c:Terminal.ConnectivityNode ?cn. " +
				      "?cn c:IdentifiedObject.name ?bus "+
				    "} "+
				    "GROUP BY ?name ?mrid ?bus ?basev ?p ?q ?conn "+
				    "ORDER by ?name ";
				  
			//Initialize output
			//ec = pd.DataFrame(columns=['p', 'q', 'magS', 'mrid'])		
			HashMap<String, HashMap<String, Object>> ec = new HashMap<String, HashMap<String, Object>>();
			HashMap<Float, HashMap<String, Object>> no_houses = new HashMap<Float, HashMap<String, Object>>();
	//		String[] no_houses = new String[] {};
			//no_houses = {}
			Complex j = new Complex(-1).sqrt();
			System.out.println("Square root: " + j);
			double totalRes = 0+0 * j.getImaginary();
			// Set and execute the query.
			ResultSet ret = queryResultSet(modelId, qStr, processId, username);
			while(ret.hasNext()){
				 QuerySolution qs = ret.nextSolution();
				 // grab variables
				 float v = qs.getLiteral("basev").getFloat(); // float(el['basev'].value)
				 String phs = qs.getLiteral("phases").getString(); //el['phases'].value
				 String name = qs.getLiteral("name").getString(); //name = el['name'].value
				 String mrid = qs.getLiteral("mrid").getString(); //  mrid = el['mrid'].value
				 float p = qs.getLiteral("p").getFloat(); //   p = float(el['p'].value)
				 float q = qs.getLiteral("q").getFloat(); //   q = float(el['q'].value)
	
				    // At this time, we're only adding houses to split-phase
				    // loads. In the future we will likely want to support
				    // three-phase loads.
				    if (v == TRIPLEX_V && ((phs.contains("s1")) || (phs.contains("s2")))){
				      // Triplex (split-phase) load.
				      //ec.loc[name, ['p', 'q', 'magS', 'mrid']] = [p, q, math.sqrt((p^2 + q^2)), mrid]
				    	HashMap<String, Object> subEC = new HashMap<String, Object>();
				    	if(ec.containsKey(name)){
				    		subEC = ec.get(name);
				    	}
				    	subEC.put("p", new Float(p));
				    	subEC.put("q",  new Float(q));
				    	subEC.put("magS", Math.sqrt((Math.pow(p, 2) + Math.pow(q, 2))));
				    	subEC.put("mrid", mrid);
				    	ec.put(name, subEC);
				    		  
				     // Increment counter of total residential power
				      totalRes += p + j.getImaginary()*q;
				      
				    }  else{
				      // Track other voltages/phasing
				    	HashMap<String, Object> subNH = new HashMap<String, Object>();
				    	if(no_houses.containsKey(v)){
				    		subNH = no_houses.get(v);
				    	}
	//			      try{
				        // Attempt to increment the count for this voltage level.
				    	  Integer num = 1;
				    	  Float power = (float) (p + q*j.getImaginary());
				    	  if(subNH.containsKey("num")){
				    		  num = ((Integer)subNH.get("num"))+1;
				    		  power = (float) (((Float)subNH.get("power")) + (p + q*j.getImaginary()));
				    	  } 
				    	  num++;
				    	  subNH.put("num", num);
				    	  subNH.put("power", power);
				    	no_houses.put(v, subNH);
	//			        no_houses[v]['num'] += 1
	//			      }catch(Exeption e){
	//			        // Initialize a dictionary for this voltage level.
	//			        //no_houses[v] = {'power': p + q*1j, 'num': 1}
	//			      } finally{
	//			        // Increment the total power for this voltage.
	//			        no_houses[v]['power'] += p + q*1j
	//			      }
				    }
			}
			// Update all the 'power' fields in no_houses to be strings.
			// This is a cheap hack to ensure it's json serializable.
			// for sub_dict in no_houses.values():
			//  sub_dict['power'] = '{:.2f} VA'.format(abs(sub_dict['power']))
			for(Float key: no_houses.keySet()){
				HashMap<String, Object> sub_dict = no_houses.get(key);
				Object power = sub_dict.get("power");
				power = power.toString()+" VA";
				sub_dict.put("power", power);
				no_houses.put(key, sub_dict);
			}
			
			if(ec.size()==0){
				throw new RuntimeException("There are no 120/240V split-phase loads present in the given model, so no houses will be added.");
			}
			
			
			
			
			
			
			
			
			/*'Main' function for class. Generates houses for a given set of loads
	        
	        NOTE: The intention is that this function is called by the insertHouses
	            module, and so the inputs are taken in a form they exist there.
	        
	        INPUTS:
	        loadDf: pandas dataframe, indices are names of loads, and column 'magS'
	            indicates apaprent power magnitude under peak conditions
	        magS: total magnitude of all loads [MVA]
	        scale:  scaling factor on the number of houses placed, to mitigate overloads
	        
	        OUTPUT:
	        housingDict: dictionary with nodes/loads (index from loadDf) as keys.
	            Each key maps to a two-element tuple - the first is a dataframe
	            containing housing data and the second is the chosen housing type
	            code (see eia_recs.housingData.TYPEHUQ). Using the code instead of
	            the full name for efficiency.
	            
	        note: We may want to use a different datatype other than a dictionary.
	            When we loop back over the items, this will be inefficient as 
	            the key will have to be looked up every time.
	        */
	        // First, estimate how many houses we'll be placing.
			float magS = 0;
			float scale = 0;
	        float guess = estimateTotalHouses(magS, scale);
//	        print ('Estimating {:.2f} houses for {:.2f} kVA at {:.3f} VA/ft2'.format (guess, 0.001 * magS, VA_PER_SQFT/scale))
	        
	        // Create a pandas Series to be used for updating the distribution with
	        // each housing type selection.
//	        housing = self.data['TYPEHUQ'].copy(deep=True)
//	        
//	        self.housing = housing * guess
//	        
//	        // Track how many loads we handled.
//	        int loadCount = 1;
//	        self.houseCount = pd.Series(data=np.zeros(len(self.data['TYPEHUQ'])),
//	                                    index=self.data['TYPEHUQ'].index)
//	        // Initialize flag so we aren't throwing too many warnings.
//	        self.warnFlag = False
//	        
//	        // Since the number of houses per load/node is variable, we're best
//	        // suited to use a dictionary here.
//	        housingDict = {}
//	        
//	        //Loop over each load and add houses.
//	        for loadName, data in loadDf.iterrows():
//	            
//	            // Draw a housing type from the distribution and then draw square
//	            // footages for each house that will be added to the load/xfmr.
//	            housingType, floorArea = self.typeAndSQFTForLoad(data.loc['magS'], scale)
//	            
//	            // Number of houses is the length of the floorArea
//	            n = len(floorArea)
//	            
//	            // Initialize a DataFrame for these houses. We'll be mapping 
//	            // EIA RECS codes into the codes used for the house model in 
//	            // PNNL's CIM extension.
//	            houseDf = pd.DataFrame(data=floorArea, columns=['floorArea'])
//	            
//	            // Add cooling (AC) information:
//	            coolingSystem, coolingSetpoint = self.drawAC(housingType, n)
//	            houseDf['coolingSystem'] = coolingSystem
//	            houseDf['coolingSetpoint'] = coolingSetpoint
//	            // Using the above syntax because apparently assign returns a copy,
//	            // which feels very inefficient...
////	            '''
////	            houseDf.assign(coolingSystem=coolingSystem,
////	                           coolingSetpoint=coolingSetpoint)
////	            '''
//	            
//	            // Add heating information:
//	            heatingSystem, heatingSetpoint = self.drawHeating(coolingSystem,
//	                                                              housingType, n)
//	            houseDf['heatingSystem'] = heatingSystem
//	            houseDf['heatingSetpoint'] = heatingSetpoint
//	            
//	            // Draw HVAC power system, using nan if there's neither electric 
//	            // heating nor cooling
//	            hvacPowerFactor = pd.Series(self.rand.uniform(low=HVAC_PF[0],
//	                                        high=HVAC_PF[1], size=n))
//	            // Use np.nan when both heating and cooling are absent.
//	            hvacPowerFactor[(houseDf['coolingSystem'] == 'none') & \
//	                            (houseDf['heatingSystem'] == 'none')] = np.nan
//	            // Use unity(1) when there's no cooling, but heating is resistive.
//	            hvacPowerFactor[((houseDf['coolingSystem'] == 'none') & \
//	                            (houseDf['heatingSystem'] == 'resistance'))] = 1
//	            houseDf['hvacPowerFactor'] = hvacPowerFactor
//	            
//	            // Draw number of stories.
//	            houseDf['numberOfStories'] = self.drawNumStories(housingType, n)
//	            
//	            // Draw thermal integrity.
//	            houseDf['thermalIntegrity'] = \
//	                self.drawThermalIntegrity(housingType, n)
//	            
//	            // Lookup housing code and assign houseDf and code to dictionary.
//	            hCode = TYPEHUQ_REV[housingType]
//	            housingDict[loadName] = (houseDf, hCode)
//	            
//	            self.loadCount += 1
//	            self.houseCount[housingType] += n
	            
	        // Print totals to the log. 
//	        self.log.info(('{} loads were accounted for, totaling {} '
//	                       + 'housing units').format(self.loadCount,
//	                                                 self.houseCount.sum())
//	                       )
	        
//	        # Print final distribution to the log.
//	        self.log.info('Final housing breakdown:\n{}'.format(self.houseCount))
	                        
//	        # Done!
//	        return housingDict
			
			
			
	//
			
	//	    delete_all_houses
	//	                  for each model name call delete_houses
	//	    delete_houses(model_name)
	//	    insert  houses (eeder mRID, region, seed value, file of persistent mRIDs, scaling factor on load)
	//
	
			
			// write uuidDict back to file
			JsonObject obj = new JsonObject();
			List<String> keyNames = new ArrayList<String>();
			keyNames.addAll(uuidDict.keySet());
			Collections.sort(keyNames);
			for(String key: keyNames){
				obj.add(key, new JsonPrimitive(uuidDict.get(key)));
			}
			Gson gson = new GsonBuilder().setPrettyPrinting().create(); 
			FileWriter fw = new FileWriter(uuidFile);
			gson.toJson(obj, fw);
			fw.flush();
			fw.close();
		}catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	@Override
	public void dropHouses(String modelName, String modelId, String processId, String username, String baseDirectory) {
		// TODO Auto-generated method stub
		System.out.println("CALLING DROP HOUSES "+modelName);
		String qstr = "DELETE { "+
				"?h a ?class. "+
						  " ?h c:IdentifiedObject.mRID ?uuid. "+
						  " ?h c:IdentifiedObject.name ?name. "+
						  " ?h c:House.floorArea ?floorArea. "+
						  " ?h c:House.numberOfStories ?numberOfStories. "+
						  " ?h c:House.coolingSetpoint ?coolingSetpoint. "+
						  " ?h c:House.heatingSetpoint ?heatingSetpoint. "+
						  " ?h c:House.hvacPowerFactor ?hvacPowerFactor. "+
						  " ?h c:House.coolingSystem ?coolingSystemRaw. "+
						  " ?h c:House.heatingSystem ?heatingSystemRaw. "+
						  " ?h c:House.thermalIntegrity ?thermalIntegrityRaw. "+
						  " ?h c:House.EnergyConsumer ?econ. "+
						 "} WHERE { "+
						  " VALUES ?fdrid {\""+modelId+"\"} "+
						  " VALUES ?class {c:House} "+
						 " ?fdr c:IdentifiedObject.mRID ?fdrid.  "+
						 " ?econ c:Equipment.EquipmentContainer ?fdr. "+
						 " ?h a ?class. "+
						 " ?h c:IdentifiedObject.mRID ?uuid. "+
						 " ?h c:IdentifiedObject.name ?name. "+
						 " ?h c:House.floorArea ?floorArea. "+
						 " ?h c:House.numberOfStories ?numberOfStories. "+
						 " OPTIONAL{?h c:House.coolingSetpoint ?coolingSetpoint.} "+
						 " OPTIONAL{?h c:House.heatingSetpoint ?heatingSetpoint.} "+
						 " OPTIONAL{?h c:House.hvacPowerFactor ?hvacPowerFactor.} "+
						 " ?h c:House.coolingSystem ?coolingSystemRaw. "+
						 " ?h c:House.heatingSystem ?heatingSystemRaw. "+
						 " ?h c:House.thermalIntegrity ?thermalIntegrityRaw. "+
						 " ?h c:House.EnergyConsumer ?econ. "+
						 "}";
		 try {
//			String ret = query(modelId, qstr, "JSON", processId, username);
//			System.out.println(ret);
			 String endpoint = getEndpointURL(modelId);
			 BlazegraphQueryHandler queryHandler = new BlazegraphQueryHandler(endpoint, logManager, processId, username);
//			  queryHandler.query(qstr, null);
//			  queryHandler.construct(qstr);
			 queryHandler.executeUpdateQuery(qstr);
				
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	protected float estimateTotalHouses(float magS, float scale){
		float num = 0;
		/*Estimate total number of housing units that will be generated.
        
        This is used to help ensure we're doing a good job tracking the housing
            type distribution.
            
        Rough flow:
            1) Compute mean square footage by housing type
            2) Use factor (VA_PER_SQFT/scale) to estimate peak power by housing type.
            3) Given distribution of housing types and associated average
                power, estimate how many housing units will be generated.
        
        INPUTS:
        magS: Magnitude of apparent power for all the loads in the system.
        scale:  scaling factor on the number of houses placed, to mitigate overloads
        
        OUTPUTS:
        num: Estimate of total number of housing units that will be generated.
        */
        
//        // Initialize pandas series for holding mean square footages.
//        meanSqft = np.zeros(len(self.data['TYPEHUQ']))
//        
//        // Loop over the housing types and compute the mean square footage.
//        for housingInd, housingType in enumerate(self.data['TYPEHUQ'].index):
//        	// Grab bin_edges.
//            bin_edges = np.array(\
//                self.data[housingType]['TOTSQFT_EN']['bin_edges'])
//            
//            		// Bin centers are left edge + half of the distance between bins.
//            		// We're grabbing centers because the uniform distribution is used
//            		// to pick a value within a bin.
//            bin_centers = bin_edges[0:-1] \
//                + ((bin_edges[1:] - bin_edges[0:-1]) / 2)
//                
//                // Mean square footage is the sum of the probabilities times the 
//                // values.
//            pmf = np.array(self.data[housingType]['TOTSQFT_EN']['pmf'])
//            meanSqft[housingInd] = np.sum((bin_centers * pmf))
//            
//            		// Use our (maybe trash) constant to convert square footages to power.
//        meanPower = meanSqft * VA_PER_SQFT / scale
//        
//        		// Compute the mean power for all housing types.
//        totalMean = np.sum(meanPower * self.data['TYPEHUQ'])
//        
//        		// Estimate the nubmer of houses.
//        num = magS / totalMean
        
        		// Done!
        return num;
	}
	
	@Override
	public void insertDER(String modelNames, String processId, String username, String baseDirectory) {
		// TODO Auto-generated method stub
//        list_measurements(?) for model
//        delete measurements (model_name)
//        call insertder for <model_name>_der.txt

	}
	

}
