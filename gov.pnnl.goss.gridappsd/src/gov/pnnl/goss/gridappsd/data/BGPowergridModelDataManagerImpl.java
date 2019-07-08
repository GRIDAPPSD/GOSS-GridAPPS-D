package gov.pnnl.goss.gridappsd.data;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
//import org.openrdf.model.Statement;
//import org.openrdf.query.GraphQueryResult;
//import com.bigdata.rdf.sail.webapp.SD;
//import com.bigdata.rdf.sail.webapp.client.RemoteRepositoryManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

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
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.ClientFactory;

@Component
public class BGPowergridModelDataManagerImpl implements PowergridModelDataManager { 
	final String nsCIM = "http://iec.ch/TC57/CIM100#";
	final String nsRDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	final String nsXSD = "http://www.w3.org/2001/XMLSchema#";
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
		//BGPowergridModelDataManagerImpl bg = new BGPowergridModelDataManagerImpl("http://localhost:9999/blazegraph/namespace/kb/sparql");
		BGPowergridModelDataManagerImpl bg = new BGPowergridModelDataManagerImpl("http://192.168.99.100:8889/bigdata/namespace/kb/sparql");
		bg.endpointNSURL = "http://localhost:8889/bigdata/sparql";
		try {
//			String query = "select ?s ?p ?o where {?s r:type c:ConnectivityNode. ?s ?p ?o}";
//			System.out.println(bg.query("ieee13", query, "JSON"));
			
//			bg.queryObject("ieee13", "_211AEE43-D357-463C-95B9-184942ABE3E5", "JSON");
//			System.out.println(bg.queryObjectTypes("_4F76A5F9-271D-9EB8-5E31-AA362D86F2C3", "JSON", "12345", "user"));
//			System.out.println(bg.queryModelNameList("12345", "user"));
//			System.out.println(bg.queryModel("_4F76A5F9-271D-9EB8-5E31-AA362D86F2C3", "http://iec.ch/TC57/2012/CIM-schema-cim17#PowerTransformer", "?s c:IdentifiedObject.name 't5138260a'", "JSON", "12345", "user"));
//			System.out.println(bg.queryModelNames("XML"));
//			System.out.println(bg.queryModelNamesAndIds("XML", "12345", "user"));
			
//			System.out.println(bg.queryObjectIds("JSON", "_4F76A5F9-271D-9EB8-5E31-AA362D86F2C3", "LoadBreakSwitch", "12345", "user"));
			//test with both object id and type
			System.out.println(bg.queryObjectDictByType("JSON", "_C1C3E687-6FFD-C753-582B-632A27E28507", "LinearShuntCompensator", "_EF2FF8C1-A6A6-4771-ADDD-A371AD929D5B", "12345", "user"));    //ieee123
			//test with only object id
			System.out.println(bg.queryObjectDictByType("JSON", "_C1C3E687-6FFD-C753-582B-632A27E28507", null, "_EF2FF8C1-A6A6-4771-ADDD-A371AD929D5B", "12345", "user"));    //ieee123
			//test with only object type
			System.out.println(bg.queryObjectDictByType("JSON", "_C1C3E687-6FFD-C753-582B-632A27E28507", "LinearShuntCompensator", null, "12345", "user"));    //ieee123
			//test with neither object or type, should fail
			try{
			System.out.println(bg.queryObjectDictByType("JSON", "_C1C3E687-6FFD-C753-582B-632A27E28507", null, null, "12345", "user"));    //ieee123
			}catch (Exception e) {
				System.out.println("Expected error "+e.getMessage());
				// TODO: handle exception
			}
			//			System.out.println(bg.queryObjectDictByType("JSON", "_4F76A5F9-271D-9EB8-5E31-AA362D86F2C3", "LinearShuntCompensator", null, "12345", "user"));   //ieee8500
//			System.out.println(bg.queryMeasurementDictByObject("JSON", "_4F76A5F9-271D-9EB8-5E31-AA362D86F2C3", null, "12345", "user"));
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
		return  queryHandler.query(query);
		
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
		ResultSet rs = queryHandler.query(query);
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
		ResultSet rs = queryHandler.query(query);
		
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
		ResultSet rs = queryModelResultSet(modelId, objectType, filter, processId, username);
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
	public ResultSet queryModelResultSet(String modelId, String objectType, String filter, String processId, String username) {
		if(modelId==null){
			throw new RuntimeException("queryModel: model id missing");
		}
		
		String query = "CONSTRUCT   { ?s ?p ?o } WHERE     { ?s ?p ?o ";
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
//		System.out.println(query);
		
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
	public String queryMeasurementDictByObject(String resultFormat, String modelId, String objectId, String processId, String username) throws Exception {
		String result = null;
		ResultSet rs = queryMeasurementDictByObjectResultSet(modelId, objectId, processId, username);
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
	public ResultSet queryMeasurementDictByObjectResultSet( String modelId, String objectId, String processId, String username) {
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
					  " bind(strafter(str(?typeraw),\"#\") as ?eqtype) "+
					  "?trm c:Terminal.ConnectivityNode ?cn. "+
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
	
	
	protected String resultSetToJson(ResultSet rs){
		JsonArray resultArr = new JsonArray();
		String baseUrl = getEndpointNS(null);
		HashMap<String, JsonObject> resultObjects = new HashMap<String, JsonObject>();
		while( rs.hasNext()) {
			QuerySolution qs = rs.nextSolution();
			String subject = qs.getResource(SUBJECT).getLocalName();
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
			return endpointNSURL+"#"+modelId;
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
	

}
