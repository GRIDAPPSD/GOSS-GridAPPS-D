package gov.pnnl.goss.gridappsd.data;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gov.pnnl.goss.gridappsd.api.AppManager;
import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.api.DataManagerHandler;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.ServiceManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.api.TimeseriesDataManager;
import gov.pnnl.goss.gridappsd.data.conversion.DataFormatConverter;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.RequestTimeseriesData;
import gov.pnnl.goss.gridappsd.dto.RequestTimeseriesDataAdvanced;
import gov.pnnl.goss.gridappsd.dto.RequestTimeseriesDataBasic;
import gov.pnnl.goss.gridappsd.dto.SimulationContext;
import gov.pnnl.goss.gridappsd.dto.TimeSeriesEntryResult;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import gov.pnnl.proven.api.producer.ProvenProducer;
import gov.pnnl.proven.api.producer.ProvenResponse;
import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.core.security.SecurityConfig;

@Component
public class ProvenTimeSeriesDataManagerImpl implements TimeseriesDataManager, DataManagerHandler{
	
	@ServiceDependency
	private volatile LogManager logManager;
	
	@ServiceDependency
	private volatile DataManager dataManager;
	
	@ServiceDependency
	private volatile ClientFactory clientFactory;
	
	@ServiceDependency
	private volatile ConfigurationManager configManager;
	
	@ServiceDependency
	private volatile SimulationManager simulationManager;
	
	@ServiceDependency
	private volatile ServiceManager serviceManager;
	
	@ServiceDependency
	private volatile AppManager appManager;
	
	@ServiceDependency
	private volatile SecurityConfig securityConfig;
	
	public static final String DATA_MANAGER_TYPE = "timeseries";
	
	public static int count = 0;
	
	List<String> keywords = null;
	String requestId = null;
	Gson  gson = new Gson();
	String provenUri = null;
    String provenQueryUri = null;
    String provenAdvancedQueryUri = null;
    String provenWriteUri = null;

	ProvenProducer provenQueryProducer = new ProvenProducer();
	ProvenProducer provenWriteProducer = new ProvenProducer();
//	Credentials credentials = new UsernamePasswordCredentials(
//			GridAppsDConstants.username, GridAppsDConstants.password);
	
	@Start
	public void start(){
		
		
		logManager.debug(ProcessStatus.RUNNING, null, "Starting "+this.getClass().getSimpleName());
		
		dataManager.registerDataManagerHandler(this, DATA_MANAGER_TYPE);
		provenUri = configManager.getConfigurationProperty(GridAppsDConstants.PROVEN_PATH);
		provenWriteUri = configManager.getConfigurationProperty(GridAppsDConstants.PROVEN_WRITE_PATH);
        provenQueryUri = configManager.getConfigurationProperty(GridAppsDConstants.PROVEN_QUERY_PATH);	
        provenAdvancedQueryUri = configManager.getConfigurationProperty(GridAppsDConstants.PROVEN_ADVANCED_QUERY_PATH);	
        provenQueryProducer.restProducer(provenQueryUri, null, null);
        provenWriteProducer.restProducer(provenWriteUri, null, null);
        
        try {
			this.subscribeAndStoreDataFromTopic("/topic/goss.gridappsd.*.output", null, null, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
        
	}
	
	
	@Override
	public Serializable handle(Serializable requestContent, String processId,
			String username) throws Exception {
		if(requestContent instanceof SimulationContext){
			storeAllData((SimulationContext)requestContent);
		}
		if(requestContent instanceof RequestTimeseriesData){
			return query((RequestTimeseriesData)requestContent);
		}
		else if(requestContent instanceof String){
			//First try to parse the query as the new format, if that fails try the old
			RequestTimeseriesData timeSeriesRequest;
			try{
				timeSeriesRequest = RequestTimeseriesDataAdvanced.parse((String)requestContent);
			}catch (Exception e) {
				// TODO: handle exception
				try{
					timeSeriesRequest = RequestTimeseriesDataBasic.parse((String)requestContent);
				}catch (Exception e2) {
					throw new Exception("Failed to parse time series data request");
				}
			}
			
			
			return query(timeSeriesRequest);
		}
		
		return null;
	}
	
	@Override
	public Serializable query(RequestTimeseriesData requestTimeseriesData) throws Exception {
		
		ProvenResponse response = null;
		
		if(requestTimeseriesData instanceof RequestTimeseriesDataAdvanced){
			provenQueryProducer.restProducer(provenAdvancedQueryUri, null, null);
			provenQueryProducer.setMessageInfo("GridAPPSD", "QUERY", this.getClass().getSimpleName(), keywords);
			response = provenQueryProducer.getAdvancedTsQuery(requestTimeseriesData.toString(), requestId);
		}else {
			provenQueryProducer.restProducer(provenQueryUri, null, null);
			provenQueryProducer.setMessageInfo("GridAPPSD", "QUERY", this.getClass().getSimpleName(), keywords);
			response = provenQueryProducer.sendMessage(requestTimeseriesData.toString(), requestId);
			}
		TimeSeriesEntryResult result = TimeSeriesEntryResult.parse(response.data.toString());
		if(result.getData().size()==0)
			return null;
		String origFormat = "PROVEN_"+requestTimeseriesData.getQueryMeasurement().toString();
		if(requestTimeseriesData.getOriginalFormat()!=null) {
			origFormat = "PROVEN_"+requestTimeseriesData.getOriginalFormat();
		}
		String responseFormat = requestTimeseriesData.getResponseFormat();
		DataFormatConverter converter = dataManager.getConverter(origFormat, responseFormat);
		if(converter!=null){
			StringWriter sw = new StringWriter();			
			converter.convert(response.data.toString(), new PrintWriter(sw), requestTimeseriesData);
			return sw.toString();
		}
				
		return response.data;
		
	}
	
	
	@Override
    public void storeAllData(SimulationContext simulationContext) throws Exception{
    	
    	String simulationId = simulationContext.getSimulationId();
    	
    	storeSimulationInput(simulationId);
    	storeSimulationOutput(simulationId);
        
        for(String instanceId: simulationContext.getServiceInstanceIds()){
        	String serviceId = serviceManager.getServiceIdForInstance(instanceId);
        	storeServiceInput(simulationId, serviceId, instanceId);
        	storeServiceOutput(simulationId, serviceId, instanceId);
        	
        }
        
        for(String instanceId: simulationContext.getAppInstanceIds()){
        	String appId = appManager.getAppIdForInstance(instanceId);
        	storeAppInput(simulationId, appId, instanceId);
        	storeAppOutput(simulationId, appId, instanceId);
        }
    }
    
    
    
    private void subscribeAndStoreDataFromTopic(String topic, String appOrServiceid, String instanceId, String simulationId) throws Exception{
    	
    	Credentials credentials = new UsernamePasswordCredentials(
				securityConfig.getManagerUser(), securityConfig.getManagerPassword());
        Client inputClient = clientFactory.create(PROTOCOL.STOMP,credentials,true);
        inputClient.subscribe(topic, new GossResponseEvent() {
            @Override
            public void onMessage(Serializable message) {
            	DataResponse event = (DataResponse)message;
            	for(String str : event.getDestination().split(".")){
            		System.out.println(str);
            	}
            	//String appOrServiceid = event.getDestination().split("[.]")[2];
            	try{
            		//TODO: Remove if block once changes made in proven cluster to get measurement name from datatype
            		if(appOrServiceid==null){
            			JsonParser parser = new JsonParser();
        				JsonElement data = parser.parse(event.getData().toString());
        				if (data.isJsonObject()) {
        					JsonObject dataObj = data.getAsJsonObject();
        					if(dataObj.get("datatype")!=null){
        						String datatype = dataObj.get("datatype").getAsString();
        						if(datatype!=null)
        							provenWriteProducer.sendBulkMessage(event.getData().toString(), datatype, instanceId, simulationId, new Date().getTime());
        					}
        				}
            		}
            		else{
            			provenWriteProducer.sendBulkMessage(event.getData().toString(), appOrServiceid, instanceId, simulationId, new Date().getTime());
            		}
                }catch(Exception e){
                    
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    String sStackTrace = sw.toString(); // stack trace as a string
                    System.out.println(sStackTrace);
                    logManager.error(ProcessStatus.RUNNING, null, "Error storing timeseries data for message at "+event.getDestination()+" : "+sStackTrace);
                }
            }
        });
    }
    
    @Override
	public void storeSimulationOutput(String simulationId) throws Exception {
		subscribeAndStoreDataFromTopic("/topic/"+GridAppsDConstants.topic_simulation+".output."+simulationId,"simulation",null,simulationId);
	}
	
	@Override
	public void storeSimulationInput(String simulationId) throws Exception {
		subscribeAndStoreDataFromTopic("/topic/"+GridAppsDConstants.topic_simulation+".input."+simulationId,"simulation",null, simulationId);
	}
	
	@Override
	public void storeServiceOutput(String simulationId, String serviceId, String instanceId) throws Exception {
	        subscribeAndStoreDataFromTopic("/topic/"+GridAppsDConstants.topic_simulation+"."+serviceId+"."+simulationId+".output", serviceId, instanceId, simulationId);
	}
	
	@Override
	public void storeServiceInput(String simulationId, String serviceId, String instanceId) throws Exception {
		subscribeAndStoreDataFromTopic("/topic/"+GridAppsDConstants.topic_simulation+"."+serviceId+"."+simulationId+".input", serviceId, instanceId, simulationId);
	}
	
	@Override
	public void storeAppOutput(String simulationId, String appId, String instanceId) throws Exception {
		subscribeAndStoreDataFromTopic("/topic/"+GridAppsDConstants.topic_simulation+"."+appId+"."+simulationId+".output", appId, instanceId, simulationId);
	}
	
	@Override
	public void storeAppInput(String simulationId, String appId, String instanceId) throws Exception {
    	subscribeAndStoreDataFromTopic("/topic/"+GridAppsDConstants.topic_simulation+"."+appId+"."+simulationId+".input", appId, instanceId, simulationId);
	}

}




