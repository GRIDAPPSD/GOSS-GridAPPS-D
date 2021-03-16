package gov.pnnl.goss.gridappsd.testmanager;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

import gov.pnnl.gridappsd.cimhub.queryhandler.QueryHandler;
import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.data.handlers.BlazegraphQueryHandler;

@Component
public class TestManagerQueryFactory {
	@ServiceDependency
	private volatile ConfigurationManager configurationManager;
	
	public TestManagerQueryFactory(){}
	public TestManagerQueryFactory(ConfigurationManager configurationManager){
		this.configurationManager = configurationManager;
	}
	

	public String getFeeder() {
		QueryHandler queryHandler = getQueryHandler();
		String lineQuery = "SELECT ?name WHERE { " + "?s r:type c:Feeder. " + "?s c:IdentifiedObject.name ?name} "
				+ "ORDER by ?name";
		ResultSet results = queryHandler.query(lineQuery, null);
		String feederName = getResultName(results);
		return feederName;
	}
	
	public String getGeographicalRegion() {
		QueryHandler queryHandler = getQueryHandler();

		String geoRegionQuery = "SELECT ?name WHERE { " + "?s r:type c:GeographicalRegion. " + "?s c:IdentifiedObject.name ?name} "
				+ "ORDER by ?name";
		ResultSet results = queryHandler.query(geoRegionQuery, null);
		String geoName = getResultName(results);
		return geoName;
	}
	
	public String getSubGeographicalRegion() {
		QueryHandler queryHandler = getQueryHandler();
		String subGeoRegionQuery = "SELECT ?name WHERE { " + "?s r:type c:SubGeographicalRegion. " + "?s c:IdentifiedObject.name ?name} "
				+ "ORDER by ?name";
		ResultSet results = queryHandler.query(subGeoRegionQuery, null);
		String subGeoName = getResultName(results);	
		return subGeoName;
	}
	
	public QueryHandler getQueryHandler() {
		// // list all the connectivity nodes (aka bus)
		// PREFIX r: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
		// PREFIX c: <http://iec.ch/TC57/2012/CIM-schema-cim16#>
		// SELECT ?name WHERE {
		// ?s r:type c:Line.
		// ?s c:IdentifiedObject.name ?name}
		// ORDER by ?name
//		String szQuery = "		PREFIX r:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
//				+ "PREFIX c:  <http://iec.ch/TC57/2012/CIM-schema-cim16#> " + "SELECT ?name WHERE { "
//				+ "?s r:type c:Line. " + "?s c:IdentifiedObject.name ?name} " + "ORDER by ?name";

//		String bgHost = configurationManager.getConfigurationProperty(GridAppsDConstants.BLAZEGRAPH_HOST_PATH);
//		if (bgHost == null || bgHost.trim().length() == 0) {
//			bgHost = "http://localhost:9999";
//		}
		String bgHost = "http://localhost:8889";
		QueryHandler queryHandler = new BlazegraphQueryHandler(bgHost+"/bigdata/namespace/kb/sparql", null, "1", "manager");
//		QueryHandler queryHandler = null;
		return queryHandler;
	}
	
	public String getResultName(ResultSet results) {
		 String feederName = "";
		if (results.hasNext()) {
			QuerySolution soln = results.next();
			feederName = soln.get("?name").toString();
			System.out.println(feederName);
		}
		return feederName;
	}
	
	public void getMeasurements(){
		QueryHandler queryHandler = getQueryHandler();
		String feeder = "_4F76A5F9-271D-9EB8-5E31-AA362D86F2C3";
		String subGeoRegionQuery = "SELECT ?name WHERE { " + "?s r:type c:SubGeographicalRegion. " + "?s c:IdentifiedObject.name ?name} "
				+ "ORDER by ?name";
		String query = "SELECT ?class ?type ?name ?bus ?phases ?eqtype ?eqname ?eqid ?trmid ?id ?ce WHERE {"+
     "VALUES ?fdrid {\""+feeder+"\"}"+
     "?eq c:Equipment.EquipmentContainer ?fdr."+
     "?fdr c:IdentifiedObject.mRID ?fdrid. "+
    "{ ?s r:type c:Discrete. bind (\"Discrete\" as ?class)}"+
     " UNION"+
    "{ ?s r:type c:Analog. bind (\"Analog\" as ?class)}"+
     "?s c:IdentifiedObject.name ?name ."+
     "?s c:IdentifiedObject.mRID ?id ."+
     "?s c:Measurement.PowerSystemResource ?eq ."+
     "?s c:Measurement.Terminal ?trm ."+
     "?trm c:Terminal.ConductingEquipment ?ce."+
     "?s c:Measurement.measurementType ?type ."+
     "?trm c:IdentifiedObject.mRID ?trmid."+
     "?eq c:IdentifiedObject.mRID ?eqid."+
     "?eq c:IdentifiedObject.name ?eqname."+
     "?eq r:type ?typeraw."+
      "bind(strafter(str(?typeraw),\"#\") as ?eqtype)"+
     "?trm c:Terminal.ConnectivityNode ?cn."+
     "?cn c:IdentifiedObject.name ?bus."+
     "?s c:Measurement.phases ?phsraw ."+
    "  {bind(strafter(str(?phsraw),\"PhaseCode.\") as ?phases)}"+
    "} ORDER BY ?class ?type ?name";
		ResultSet results = queryHandler.query(query, null);
		String subGeoName = getResultName(results);	
		System.out.println(subGeoName);
	}
}
