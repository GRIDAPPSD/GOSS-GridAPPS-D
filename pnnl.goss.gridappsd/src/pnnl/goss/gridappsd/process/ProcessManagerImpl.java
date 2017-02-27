package pnnl.goss.gridappsd.process;

import java.io.Serializable;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.gridappsd.api.ConfigurationManager;
import pnnl.goss.gridappsd.api.SimulationManager;
import pnnl.goss.gridappsd.api.StatusReporter;
import pnnl.goss.gridappsd.utils.GridAppsDConstants;

/**
 * Process Manager subscribe to all the requests coming from Applications
 * and forward them to appropriate managers.
 * @author shar064
 *
 */
@Component
public class ProcessManagerImpl {
		
	private static Logger log = LoggerFactory.getLogger(ProcessManagerImpl.class);
	
	@ServiceDependency
	private volatile ClientFactory clientFactory;
	
	@ServiceDependency
	private volatile ConfigurationManager configurationManager;
	
	@ServiceDependency
	private volatile SimulationManager simulationManager;
	
	@ServiceDependency
	private volatile StatusReporter statusReporter;
	
	@Start
	public void start(){
		try{
			
			log.debug("Starting "+this.getClass().getName());
			
			Credentials credentials = new UsernamePasswordCredentials(
					GridAppsDConstants.username, GridAppsDConstants.password);
			Client client = clientFactory.create(PROTOCOL.STOMP,credentials);
			
			//TODO: subscribe to GridAppsDConstants.topic_request_prefix+/* instead of GridAppsDConstants.topic_requestSimulation
			client.subscribe(GridAppsDConstants.topic_requestSimulation, new GossResponseEvent() {
				
				@Override
				public void onMessage(Serializable message) {
					DataResponse event = (DataResponse)message;
					
					statusReporter.reportStatus(String.format("Got new message in %s", getClass().getName()));
					//TODO: create registry mapping between request topics and request handlers.
					switch(event.getDestination().replace("/queue/", "")){
						case GridAppsDConstants.topic_requestSimulation : new ProcessSimulationRequest().process(event, client, configurationManager, simulationManager); break;
						//case GridAppsDConstants.topic_requestData : processDataRequest(); break;
						//case GridAppsDConstants.topic_requestSimulationStatus : processSimulationStatusRequest(); break;
					}
					
				}
			});
		}
		catch(Exception e){
				e.printStackTrace();
		}
		
	}
	
}
