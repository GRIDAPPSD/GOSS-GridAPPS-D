package pnnl.goss.gridappsd.data;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pnnl.goss.core.DataResponse;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.core.Response;
import pnnl.goss.gridappsd.api.DataManager;

/**
 *  1. Start FNCS
 *	2. Start GridLAB-D with input file location and name
 *	3. Start GOSS-FNCS Bridge
 *	4. Call FNCS IsInitialized()
 *	5. Publish 'Simulation Initialized' on 'simulation/[id]/status' once IsInitialized() returns.
 *		If IsInitialized() does not return in given time then publish error on 'simulation/[id]/status' and send 'die' message to GOSS-FNCS topic simulation/[id]/input
 * @author shar064
 *
 */
public class DataEvent implements GossResponseEvent {
	
	private volatile DataManager dataManager;
    private Logger log = LoggerFactory.getLogger(getClass());

	
	public DataEvent(DataManager manager){
		this.dataManager = manager;
	}
	
	
//	@Override
	public void onMessage(Serializable message) {
		
		/*  Parse message. message is in JSON string.
		 *  create and return response as simulation id
		 *  
		 *  make synchronous call to DataManager and receive file location
		 *  
		 *  Start FNCS
		 *	Start GridLAB-D with input file location and name
		 *	Start GOSS-FNCS Bridge
		 *	Call FNCS IsInitialized()
		 *  
		 *	Publish 'Simulation Initialized' on 'simulation/[id]/status' once IsInitialized() returns.
		 *		If IsInitialized() does not return in given time then publish error on 'simulation/[id]/status' and send 'die' message to GOSS-FNCS topic simulation/[id]/input
		*/

		Serializable requestData = null;
		
		if(message instanceof DataRequest){
			requestData = ((DataRequest)message).getRequestContent();
		} else if(message instanceof DataResponse){
			//TODO figure out why it is double nested in dataresponse
			if(((DataResponse)message).getData() instanceof DataResponse){
				requestData = ((DataResponse)((DataResponse)message).getData()).getData();
			}else{
				requestData = ((DataResponse)message).getData();
			}
		} else {
			requestData = message;
		}
		
		Response r = dataManager.processDataRequest(requestData);
		//TODO create client and send response on it
		
		
	}
	

}
