package gov.pnnl.goss.gridappsd.data;

import java.io.File;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;

import gov.pnnl.goss.cim2glm.CIMImporter;
import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.PowergridModelDataManager;
import gov.pnnl.goss.gridappsd.data.handlers.BlazegraphQueryHandler;
import gov.pnnl.goss.gridappsd.dto.ModelCreationConfig;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.Client;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.Client.PROTOCOL;

@Component
public class BGPowergridModelDataManagerImpl implements PowergridModelDataManager {
	
	BlazegraphQueryHandler queryHandler;

	@ServiceDependency
	private volatile ConfigurationManager configManager;
	
	@ServiceDependency
	private volatile LogManager logManager;
	
	@ServiceDependency
	private volatile ClientFactory clientFactory;
	
	public BGPowergridModelDataManagerImpl() {
		queryHandler = new BlazegraphQueryHandler(configManager.getConfigurationProperty(GridAppsDConstants.BLAZEGRAPH_HOST_PATH));
	}
	public BGPowergridModelDataManagerImpl(String endpoint) {
		queryHandler = new BlazegraphQueryHandler(endpoint);
	}
	
	//update with properties??
	
	public static void main(String[] args){
		String query = "select ?s ?p ?o where {?s r:type c:ConnectivityNode. ?s ?p ?o}";
//		String query = "SELECT ?key (count(?tank) as ?count) WHERE {"+
//				" ?tank c:TransformerTank.PowerTransformer ?pxf."+
//				" ?pxf c:IdentifiedObject.name ?key"+
//				"} GROUP BY ?key ORDER BY ?key";
		BGPowergridModelDataManagerImpl bg = new BGPowergridModelDataManagerImpl("http://localhost:9999/blazegraph/namespace/kb/sparql");
		bg.query(query, "XML", "", "");
		
	}
	
	
	@Override
	public void query(String query, String resultFormat, String resultTopic, String statusTopic) {
		// TODO Auto-generated method stub
		if(queryHandler==null){
			//log error status
			//throw new Exception(bg not available);
		}
		
		ResultSet rs = queryHandler.query(query);

		if(resultFormat.equals(ResultFormat.JSON.toString())){
			ResultSetFormatter.outputAsJSON(System.out, rs);

		} else if(resultFormat.equals(ResultFormat.XML.toString())){
			ResultSetFormatter.outputAsXML(System.out, rs);

		} else {
			System.out.println("UNKNOWN RESULTFORMAT");
		}
		

	}

	@Override
	public void queryObject(String mrid, String resultFormat, String outputTopic, String statusTopic) {
		// TODO Auto-generated method stub

	}

	@Override
	public void queryObjectTypeList(String modelId, String resultFormat, String resultTopic, String statusTopic) {
		// TODO Auto-generated method stub

	}

	@Override
	public void queryModel(String modelId, String objectType, String filter, String resultFormat, String outputTopic,
			String statusTopic) {
		// TODO Auto-generated method stub

	}

	@Override
	public void queryModelList(String outputTopic, String statusTopic) {
		// TODO Auto-generated method stub

	}
	
	
	protected void sendResult(String result, String resultTopic) throws Exception{
		Credentials credentials = new UsernamePasswordCredentials(
				GridAppsDConstants.username, GridAppsDConstants.password);
		Client client = clientFactory.create(PROTOCOL.STOMP,credentials);
		
	}
	
	
	protected void sendStatus(String status, String statusTopic) throws Exception{
		Credentials credentials = new UsernamePasswordCredentials(
				GridAppsDConstants.username, GridAppsDConstants.password);
		Client client = clientFactory.create(PROTOCOL.STOMP,credentials);
		
	}
	@Override
	public void putModel(String modelId, String model, String inputFormat, String resultTopic, String statusTopic) {
		// TODO Auto-generated method stub
		
	}
	

}
