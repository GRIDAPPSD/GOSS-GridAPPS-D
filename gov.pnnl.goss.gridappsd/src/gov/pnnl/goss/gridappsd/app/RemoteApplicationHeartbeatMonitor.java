package gov.pnnl.goss.gridappsd.app;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.TimerTask;

import org.apache.log4j.net.SyslogAppender;

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

public class RemoteApplicationHeartbeatMonitor implements HeartbeatTimeout {

    static HashMap<String, RemoteApplicationRegistrationResponse> remoteApps = new HashMap<String, RemoteApplicationRegistrationResponse>();
    static HashMap<String, TimerTask> remoteAppTimers = new HashMap<String, TimerTask>();
    Client client;
    LogManager logManager;

    public RemoteApplicationHeartbeatMonitor(LogManager logManager, Client client) {
        this.client = client;
        this.logManager = logManager;
        this.client.subscribe(GridAppsDConstants.topic_remoteapp_heartbeat + ".>", new HeartbeatEvent());
    }

    public void addRemoteApplication(String appId, RemoteApplicationRegistrationResponse response) {
        remoteApps.put(appId, response);
        ApplicationTimeoutTask task = new ApplicationTimeoutTask(60, appId, this);
        // Give 2 times before we throw it out
        remoteAppTimers.put(appId, task);
    }

    public void startRemoteApplication(String appId, String args) {
        if (remoteApps.containsKey(appId)) {
            RemoteApplicationRegistrationResponse controller = remoteApps.get(appId);
            RemoteApplicationExecArgs execArgs = new RemoteApplicationExecArgs();
            execArgs.command = args;
            System.out.println("Attempting to start remote app on " + controller.startControlTopic.substring(7));
            client.publish(controller.startControlTopic.substring(7), execArgs.toString());
        } else {
            throw new RuntimeException("No remote application registered for appId: " + appId);
        }
    }

    public void stopRemoteApplication(String appId) {
        if (remoteApps.containsKey(appId)) {
            System.out.println("Stopping app: " + appId);
            RemoteApplicationRegistrationResponse controller = remoteApps.get(appId);
            client.publish(controller.stopControlTopic, "");
        } else {
            throw new RuntimeException("No remote application registered for appId: " + appId);
        }
    }

    class HeartbeatEvent implements GossResponseEvent {

        @Override
        public void onMessage(Serializable message) {
            DataResponse event = (DataResponse) message;
            String appId = (String) event.getData();
            appId = appId.trim();

            RemoteApplicationRegistrationResponse resp = RemoteApplicationHeartbeatMonitor.remoteApps.get(appId.trim());

            if (RemoteApplicationHeartbeatMonitor.remoteAppTimers.containsKey(appId)) {
                ApplicationTimeoutTask task = (ApplicationTimeoutTask) RemoteApplicationHeartbeatMonitor.remoteAppTimers
                        .get(appId);
                task.cancel();
                RemoteApplicationHeartbeatMonitor.remoteAppTimers.put(appId,
                        new ApplicationTimeoutTask(60, appId, RemoteApplicationHeartbeatMonitor.this));

            } else {
                System.out.println("Unknown appid: " + appId);
            }

            // logManager.log(new LogMessage(this.getClass().getName(),
            // null,
            // new Date().getTime(),
            // "Starting "+this.getClass().getName(),
            // LogLevel.INFO,
            // ProcessStatus.RUNNING,
            // true),GridAppsDConstants.username,
            // GridAppsDConstants.topic_platformLog);

        }

    }

    @Override
    public void timeout(String appId) {
        // TODO Auto-generated method stub
        System.out.println("Unregistering " + appId);
        if (remoteAppTimers.containsKey(appId)) {
            remoteAppTimers.remove(appId);
        }

        if (remoteApps.containsKey(appId)) {
            remoteApps.remove(appId);
        }
    }

}
