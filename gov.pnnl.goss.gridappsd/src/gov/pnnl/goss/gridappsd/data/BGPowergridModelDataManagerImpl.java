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

import org.openrdf.model.Statement;
import org.openrdf.query.GraphQueryResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.bigdata.rdf.sail.webapp.SD;
import com.bigdata.rdf.sail.webapp.client.RemoteRepositoryManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;

import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.PowergridModelDataManager;
import gov.pnnl.goss.gridappsd.data.handlers.BlazegraphQueryHandler;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.Client;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.Client.PROTOCOL;

@Component
public class BGPowergridModelDataManagerImpl implements PowergridModelDataManager {
	final String nsCIM = "http://iec.ch/TC57/2012/CIM-schema-cim16#";
	final String nsRDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	final String nsXSD = "http://www.w3.org/2001/XMLSchema#";
	final String RDF_TYPE = nsRDF+"type";
	final String RDF_RESOURCE = "rdf:resource";
	final String RDF_ID = "rdf:ID";
	final String SUBJECT = "subject";
	final String PREDICATE = "predicate";
	final String OBJECT = "object";
	
	String endpointBaseURL;
//	BlazegraphQueryHandler queryHandler;

	@ServiceDependency 
	private volatile ConfigurationManager configManager;
	
	@ServiceDependency
	private volatile LogManager logManager;
	
	@ServiceDependency
	private volatile ClientFactory clientFactory;
	
	public BGPowergridModelDataManagerImpl() {
		endpointBaseURL = configManager.getConfigurationProperty(GridAppsDConstants.BLAZEGRAPH_HOST_PATH);
//		queryHandler = new BlazegraphQueryHandler(configManager.getConfigurationProperty(GridAppsDConstants.BLAZEGRAPH_HOST_PATH));
	}
	public BGPowergridModelDataManagerImpl(String endpoint) {
		endpointBaseURL = endpoint;
		//queryHandler = new BlazegraphQueryHandler(endpoint);
	}
	
	
	//update with properties??
	
	public static void main(String[] args){
//		String query = "SELECT ?key (count(?tank) as ?count) WHERE {"+
//				" ?tank c:TransformerTank.PowerTransformer ?pxf."+
//				" ?pxf c:IdentifiedObject.name ?key"+
//				"} GROUP BY ?key ORDER BY ?key";
		BGPowergridModelDataManagerImpl bg = new BGPowergridModelDataManagerImpl("http://localhost:9999/blazegraph");///namespace/kb/sparql");
		try {
//			String query = "select ?s ?p ?o where {?s r:type c:ConnectivityNode. ?s ?p ?o}";
//			System.out.println(bg.query("ieee13", query, "JSON"));
			
//			bg.queryObject("ieee13", "_211AEE43-D357-463C-95B9-184942ABE3E5", "JSON");
//			System.out.println(bg.queryObjectTypes("ieee13", "JSON"));
//			System.out.println(bg.queryModelNameList());
			System.out.println(bg.queryModel("ieee8500", null, null, "JSON"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
	}
	
	//,  String requestId/processId
	@Override
	public String query(String modelId, String query, String resultFormat) throws Exception {
		
		ResultSet rs = queryResultSet(modelId, query);
		ByteArrayOutputStream resultString = new ByteArrayOutputStream();
		if(resultFormat.equals(ResultFormat.JSON.toString())){
			ResultSetFormatter.outputAsJSON(resultString, rs);

		} else if(resultFormat.equals(ResultFormat.XML.toString())){
			ResultSetFormatter.outputAsXML(resultString, rs);

		} else {
			//PROCESS ID?  TIMESTAMP passed in or generated?? USERNAME??
			
			//logManager.log(new LogMessage(processId, timestamp, "Result Format not recognized, '"+resultFormat+"'", LogMessage.LogLevel.ERROR, LogMessage.ProcessStatus.ERROR, storeToDb), username);
		}
		
		String result = new String(resultString.toByteArray());

			//TODO
		sendStatus("COMPLETE");
		
		return result;
	}
	@Override
	public ResultSet queryResultSet(String modelId, String query) {
		BlazegraphQueryHandler queryHandler = new BlazegraphQueryHandler(getEndpointURL(modelId));
		ResultSet rs = queryHandler.query(query);
		return rs;
	}
	
	@Override
	public String queryObject(String modelId, String mrid, String resultFormat) throws Exception {
		// TODO Auto-generated method stub
		
		ResultSet rs = queryObjectResultSet(modelId, mrid);
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
		sendStatus("COMPLETE");
		
		return result;
	}
	@Override
	public ResultSet queryObjectResultSet(String modelId, String mrid) {
		String query = "select ?property ?value where {<"+getEndpointURL(modelId)+"#"+mrid+"> ?property ?value}";

		BlazegraphQueryHandler queryHandler = new BlazegraphQueryHandler(getEndpointURL(modelId));
		ResultSet rs = queryHandler.query(query);
		return rs;

	}
	
	
	
	@Override
	public String queryObjectTypes(String modelId, String resultFormat) {
		// TODO Auto-generated method stub
		return queryObjectTypeList(modelId).toString();
	}
	@Override
	public List<String> queryObjectTypeList(String modelId) {
		String query = "select DISTINCT  ?type where {?subject rdf:type ?type}";

		BlazegraphQueryHandler queryHandler = new BlazegraphQueryHandler(getEndpointURL(modelId));
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
	public String queryModel(String modelId, String objectType, String filter, String resultFormat) throws Exception {
		String result = null;
		ResultSet rs = queryModelResultSet(modelId, objectType, filter);
		ByteArrayOutputStream resultString = new ByteArrayOutputStream();
		if(resultFormat.equals(ResultFormat.JSON.toString())){
			JsonArray resultArr = new JsonArray();
			String baseUrl = getEndpointURL(modelId);
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
				String  value = "";
				if(qs.get(OBJECT).isLiteral()){
					Literal literal = qs.getLiteral(OBJECT);
					value = literal.toString();
				} else {
					Resource resource = qs.getResource(OBJECT);
					value = resource.toString();
					if(value.startsWith(baseUrl+"#")){
						value = value.substring(baseUrl.length()+1);
					}
				}
//				qs.getResource("object").toString())
				obj.add(propertyName, new JsonPrimitive(value));
				
			}
			
			for(JsonObject obj: resultObjects.values()){
				resultArr.add(obj);
			}
			
//			ResultSetFormatter.outputAsJSON(resultString, rs);
			result = resultArr.toString();
		} else if(resultFormat.equals(ResultFormat.XML.toString())){
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
			 
			 
			String baseUrl = getEndpointURL(modelId);
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
			result = resultWriter.toString();
//			ResultSetFormatter.outputAsXML(resultString, rs);

		} else {
			//logManager.log(new LogMessage(processId, timestamp, logMessage, logLevel, processStatus, storeToDb), username);
		}
		
		//TODO
		sendStatus("COMPLETE");
		
		return result;

	}
	@Override
	public ResultSet queryModelResultSet(String modelId, String objectType, String filter) {
		String query = "CONSTRUCT   { ?s ?p ?o } WHERE     { ?s ?p ?o ";
		if(objectType!=null && objectType.trim().length()>0){
			query = query+". ?s rdf:type <"+objectType+">";
		}
		if(filter!=null && filter.trim().length()>0){
			if(filter.startsWith(".")){
				filter = filter.substring(1);
			}
			query = query+". "+filter;
		}
		query = query+"}";
		
		BlazegraphQueryHandler queryHandler = new BlazegraphQueryHandler(getEndpointURL(modelId));
		ResultSet rs = queryHandler.query(query);
		return rs;
	}
	
	
	@Override
	public String queryModelNames(String resultFormat) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public List<String> queryModelNameList() {
		List<String> models = new ArrayList<String>();
		RemoteRepositoryManager repo = new RemoteRepositoryManager(
				endpointBaseURL, false /* useLBS */);
		try{
			GraphQueryResult res = repo.getRepositoryDescriptions();
	
			while (res.hasNext()) {
				Statement stmt = res.next();
				if (stmt.getPredicate()
						.toString()
						.equals(SD.KB_NAMESPACE.stringValue())) {
					models.add(stmt.getObject().stringValue());
				}
				
			}	
			res.close();
		}catch(Exception e){
			e.printStackTrace();
			//TODO log message
		} finally {
			try {
				repo.close();
			} catch (Exception e) {
			}
		}
		return models;
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
	
	protected void sendResult(String result, String resultTopic) throws Exception{
		Credentials credentials = new UsernamePasswordCredentials(
				GridAppsDConstants.username, GridAppsDConstants.password);
//		Client client = clientFactory.create(PROTOCOL.STOMP,credentials);
		
		 
		
	}
	
	
	protected void sendStatus(String status) throws Exception{
//		Credentials credentials = new UsernamePasswordCredentials(
//				GridAppsDConstants.username, GridAppsDConstants.password);
//		Client client = clientFactory.create(PROTOCOL.STOMP,credentials);
		
		//TODO
		//logmanager.log...
		
	}
	@Override
	public void putModel(String modelId, String model, String inputFormat) {
		// TODO Auto-generated method stub
		//if model id is null throw error
		//if namespace already exists throw error 
		
	}
	
	private String getEndpointURL(String modelId){
		if(endpointBaseURL==null){
			//TODO log error status
			//throw new Exception(bg endpoint not available);
		}
		if(modelId==null) {
			return endpointBaseURL+"/sparql";
		}
		
		return endpointBaseURL+"/namespace/"+modelId+"/sparql";
		
	}
	
	
	

}
