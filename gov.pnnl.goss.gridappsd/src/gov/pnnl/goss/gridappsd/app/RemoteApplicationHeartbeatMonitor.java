package gov.pnnl.goss.gridappsd.app;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.RemoteApplicationRegistrationResponse;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.RemoteApplicationExecArgs;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.Client;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.GossResponseEvent;

public class RemoteApplicationHeartbeatMonitor {

	HashMap<String, RemoteApplicationRegistrationResponse> remoteApps = new HashMap<String, RemoteApplicationRegistrationResponse>();
	Client client;
	LogManager logManager;
	
	public RemoteApplicationHeartbeatMonitor(LogManager logManager, Client client) {
		this.client = client;
		this.logManager = logManager;
		this.client.subscribe(GridAppsDConstants.topic_remoteapp_heartbeat+".>", new HeartbeatEvent());
	}
	
	public void addRemoteApplication(String appId, RemoteApplicationRegistrationResponse response) {
		remoteApps.put(appId, response);		
//		System.out.println("Publishing foo");
//		client.publish("bar.appremote.foo", "Foobar");
	}
	
	public void startRemoteApplication(String appId, String args) {
		if (remoteApps.containsKey(appId)) {
			RemoteApplicationRegistrationResponse controller = remoteApps.get(appId);
			RemoteApplicationExecArgs execArgs = new RemoteApplicationExecArgs();
			execArgs.command = args;
			System.out.println("Attempting to start remote app on "+controller.startControlTopic.substring(7));
			client.publish(controller.startControlTopic.substring(7), execArgs.toString());
		}
		else {
			throw new RuntimeException("No remote application registered for appId: "+appId);
		}
	}
	
	public void stopRemoteApplication(String appId) {
		if (remoteApps.containsKey(appId)) {
			RemoteApplicationRegistrationResponse controller = remoteApps.get(appId);
			client.publish(controller.stopControlTopic, "");
		}
		else {
			throw new RuntimeException("No remote application registered for appId: "+appId);
		}
	}
	
	
	class HeartbeatEvent implements GossResponseEvent{

		@Override
		public void onMessage(Serializable message) {
			DataResponse event = (DataResponse)message;
			System.out.println("heartbeat detected: " + message.toString());
//			logManager.log(new LogMessage(this.getClass().getName(), 
//					null,
//					new Date().getTime(), 
//					"Starting "+this.getClass().getName(), 
//					LogLevel.INFO, 
//					ProcessStatus.RUNNING, 
//					true),GridAppsDConstants.username,
//					GridAppsDConstants.topic_platformLog);
			
		}
		
	}
	
}
