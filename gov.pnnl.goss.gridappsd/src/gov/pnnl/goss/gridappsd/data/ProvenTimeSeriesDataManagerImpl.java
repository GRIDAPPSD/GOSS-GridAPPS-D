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

import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.api.DataManagerHandler;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.TimeseriesDataManager;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.RequestTimeseriesData;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import gov.pnnl.proven.api.producer.ProvenProducer;
import gov.pnnl.proven.api.producer.ProvenResponse;
import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.GossResponseEvent;

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
	
	public static final String DATA_MANAGER_TYPE = "timeseries";
	
	List<String> keywords = null;
	String requestId = null;
	Gson  gson = new Gson();
	String provenUri = null;
	ProvenProducer provenProducer = new ProvenProducer();
	
	@Start
	public void start(){
		
		
		logManager.log(new LogMessage(this.getClass().getSimpleName(), null, 
				new Date().getTime(), "Starting "+this.getClass().getSimpleName(), 
				LogLevel.DEBUG, ProcessStatus.RUNNING, true), 
				GridAppsDConstants.topic_platformLog);
		
		dataManager.registerDataManagerHandler(this, DATA_MANAGER_TYPE);
		provenUri = configManager.getConfigurationProperty(GridAppsDConstants.PROVEN_PATH);
		
		try{
		
			Credentials credentials = new UsernamePasswordCredentials(
			GridAppsDConstants.username, GridAppsDConstants.password);
			Client client = clientFactory.create(PROTOCOL.STOMP,credentials);
			
			client.subscribe("/topic/"+GridAppsDConstants.topic_simulation+".>", new GossResponseEvent() {
				@Override
				public void onMessage(Serializable message) {
					DataResponse event = (DataResponse)message;
					try{
						if(event.getDestination().contains("output"))
								storeSimulationOutput(event.getData());
						else if(event.getDestination().contains("input"))
							storeSimulationInput(event.getData());
					}catch(Exception e){
						
						StringWriter sw = new StringWriter();
						PrintWriter pw = new PrintWriter(sw);
						e.printStackTrace(pw);
						String sStackTrace = sw.toString(); // stack trace as a string
						System.out.println(sStackTrace);
						logManager.log(new LogMessage(this.getClass().getSimpleName(), null, 
								new Date().getTime(), "Error storing timeseries data for message at "+event.getDestination()+" : "+sStackTrace, 
								LogLevel.DEBUG, ProcessStatus.RUNNING, true), 
								GridAppsDConstants.topic_platformLog);
					}
				}
			});
		}catch(Exception e){
			
		}
	}
	
	@Override
	public Serializable handle(Serializable requestContent, String processId,
			String username) throws Exception {
		if(requestContent instanceof RequestTimeseriesData){
			return query((RequestTimeseriesData)requestContent);
		}
		else if(requestContent instanceof String){
			RequestTimeseriesData timeSeriesRequest = RequestTimeseriesData.parse((String)requestContent);
			return query(timeSeriesRequest);
		}
		
		return null;
	}
	
	@Override
	public String query(RequestTimeseriesData requestTimeseriesData) throws Exception {
		
		provenProducer.restProducer(provenUri, null, null);
		provenProducer.setMessageInfo("GridAPPSD", "QUERY", this.getClass().getSimpleName(), keywords);
		
		
		QueryFilter queryFilter = new QueryFilter();
		
		if(requestTimeseriesData.getSimulationId()!=null)
			queryFilter.hasSimulationId = requestTimeseriesData.getSimulationId();
		if(requestTimeseriesData.getMrid()!=null)
			queryFilter.hasMrid = requestTimeseriesData.getMrid();
		if(requestTimeseriesData.getStartTime()!=null)
			queryFilter.hasMrid = requestTimeseriesData.getStartTime();
		if(requestTimeseriesData.getEndTime()!=null)
			queryFilter.hasMrid = requestTimeseriesData.getEndTime();
		ProvenQuery provenQuery = new ProvenQuery();
		provenQuery.queryFilter = queryFilter;
		
		ProvenResponse response = provenProducer.sendMessage(provenQuery.toString(), requestId);
		return response.toString();
		
	}
	
	@Override
	public void storeSimulationOutput(Serializable message) throws Exception {
		
		provenProducer.restProducer(provenUri, null, null);
		provenProducer.setMessageInfo("GridAPPSD", "SimulationOutput", this.getClass().getSimpleName(), keywords);
		provenProducer.sendMessage(message.toString(), requestId);
	}
	
	
	
	@Override
	public void storeSimulationInput(Serializable message) throws Exception {
		
		provenProducer.restProducer(provenUri, null, null);
		provenProducer.setMessageInfo("GridAPPSD", "SimulationInput", this.getClass().getSimpleName(), keywords);
		provenProducer.sendMessage(message.toString(), requestId);
	}




}


class ProvenQuery implements Serializable{
	
	String queryMeasurement = "simulation";
	String queryType = "time-series"; 
	QueryFilter queryFilter;	
	
	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}

}

class QueryFilter implements Serializable{
	 	String hasSimulationId;
	    String hasSimulationMessageType;
	    String hasMrid;
	    String startTime;
	    String endTime;
	    
	    @Override
		public String toString() {
			Gson  gson = new Gson();
			return gson.toJson(this);
		}
}
