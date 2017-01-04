package pnnl.goss.gridappsd.process;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;

import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.server.DataSourceRegistry;
import pnnl.goss.core.server.RequestHandlerRegistry;
import pnnl.goss.core.server.ServerControl;
import pnnl.goss.gridappsd.utils.GridAppsDConstants;

/**
 * Process Manager subscribe to all the requests coming from Applications
 * and forward them to appropriate managers.
 * @author shar064
 *
 */
@Component
public class ProcessManager {
	
	@ServiceDependency
	private volatile ServerControl serverControl;
	
	@ServiceDependency
	private volatile RequestHandlerRegistry handler;
	
	@ServiceDependency
	private volatile ClientFactory clientFactory;
	
	@ServiceDependency
	private volatile DataSourceRegistry datasourceRegistry;

	
	@Start
	public void start(){
		try{
			Credentials credentials = new UsernamePasswordCredentials(
					GridAppsDConstants.username, GridAppsDConstants.password);
			Client client = clientFactory.create(PROTOCOL.STOMP,credentials);
			
			client.subscribe(GridAppsDConstants.topic_request, new ProcessEvent());
		}
		catch(Exception e){
				e.printStackTrace();
		}
		
	}
	
}
