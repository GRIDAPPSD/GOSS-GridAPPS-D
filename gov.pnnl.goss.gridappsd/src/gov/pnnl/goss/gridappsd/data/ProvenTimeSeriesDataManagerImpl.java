package gov.pnnl.goss.gridappsd.data;

import java.io.Serializable;
import java.util.Date;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;

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
import pnnl.goss.core.ClientFactory;

@Component
public class ProvenTimeSeriesDataManagerImpl implements TimeseriesDataManager, DataManagerHandler{
	
	@ServiceDependency
	LogManager logManager;
	
	@ServiceDependency
	DataManager dataManager;
	
	@ServiceDependency
	ClientFactory clientFactory;
	
	public static final String DATA_MANAGER_TYPE = "timeseries";
	
	@Start
	public void start(){
		this.logDebug(null, "Starting "+this.getClass().getSimpleName());
		
		dataManager.registerDataManagerHandler(this, DATA_MANAGER_TYPE);
	}
	
	
	@Override
	public Serializable handle(Serializable request) throws Exception {
	
		if(request instanceof RequestTimeseriesData){
			return query((RequestTimeseriesData)request);
		}
		else if(request instanceof String){
			
		}
		
		return null;
	}

	@Override
	public String query(RequestTimeseriesData requestTimeseriesData) throws Exception {
		
		ProvenProducer provenProducer = new ProvenProducer();
		provenProducer.sendMessage(null, requestTimeseriesData.toString());
		
		return null;
	}
	
	@Override
	public String store(RequestTimeseriesData requestTimeseriesData) throws Exception {
		
		return null;
	}
	
	@Override
	public String store(String requestTimeseriesData) throws Exception {
		
		return null;
	}
	
	private void logDebug(String processId, String message) {

		LogMessage logMessage = new LogMessage();
		logMessage.setSource(this.getClass().getSimpleName());
		logMessage.setProcessId(processId);
		logMessage.setLogLevel(LogLevel.DEBUG);
		logMessage.setProcessStatus(ProcessStatus.RUNNING);
		logMessage.setLogMessage(message);
		logMessage.setStoreToDb(true);
		logMessage.setTimestamp(new Date().getTime());

		logManager.log(logMessage, GridAppsDConstants.topic_platformLog);	

	}

	private void logError(String processId, String message) {

		LogMessage logMessage = new LogMessage();
		logMessage.setSource(this.getClass().getSimpleName());
		logMessage.setProcessId(processId);
		logMessage.setLogLevel(LogLevel.ERROR);
		logMessage.setProcessStatus(ProcessStatus.ERROR);
		logMessage.setLogMessage(message);
		logMessage.setStoreToDb(true);
		logMessage.setTimestamp(new Date().getTime());

		logManager.log(logMessage, GridAppsDConstants.topic_platformLog);	

	}

	

}
