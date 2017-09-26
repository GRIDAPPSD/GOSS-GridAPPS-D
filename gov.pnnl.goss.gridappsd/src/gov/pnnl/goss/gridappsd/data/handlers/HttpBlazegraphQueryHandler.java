package gov.pnnl.goss.gridappsd.data.handlers;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;

import gov.pnnl.goss.cim2glm.queryhandler.QueryHandler;

public class HttpBlazegraphQueryHandler implements QueryHandler {
	String endpoint;
	final String nsCIM = "http://iec.ch/TC57/2012/CIM-schema-cim16#";
	final String nsRDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	final String nsXSD = "http://www.w3.org/2001/XMLSchema#";

	
	
	public HttpBlazegraphQueryHandler(String endpoint) {
		this.endpoint = endpoint;
	}
	public String getEndpoint() {
		return endpoint;
	}
	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}


	@Override
	public ResultSet query(String szQuery) { 
		String qPrefix = "PREFIX r: <" + nsRDF + "> PREFIX c: <" + nsCIM + "> PREFIX xsd:<" + nsXSD + "> ";
		Query query = QueryFactory.create (qPrefix + szQuery);
		QueryExecution qexec = QueryExecutionFactory.sparqlService (endpoint, query);
		return qexec.execSelect();
	}

}
