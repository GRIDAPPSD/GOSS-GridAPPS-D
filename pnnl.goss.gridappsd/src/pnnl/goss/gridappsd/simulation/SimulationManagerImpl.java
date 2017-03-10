package pnnl.goss.gridappsd.simulation;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

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
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.core.server.ServerControl;
import pnnl.goss.gridappsd.api.ConfigurationManager;
import pnnl.goss.gridappsd.api.SimulationManager;
import pnnl.goss.gridappsd.api.StatusReporter;
import pnnl.goss.gridappsd.dto.SimulationConfig;
import pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.gridappsd.utils.RunCommandLine;

/**
 * This represents Internal Function 405 Simulation Control Manager.
 * This is the management function that controls the running/execution of the Distribution Simulator (401).
 * @author shar064 
 */

@Component
public class SimulationManagerImpl implements SimulationManager{
	
	private static Logger log = LoggerFactory.getLogger(SimulationManagerImpl.class);
	
	Client client = null; 
	
	@ServiceDependency
	private volatile ClientFactory clientFactory;
	
	@ServiceDependency
	ServerControl serverControl;
	
	@ServiceDependency
	private volatile StatusReporter statusReporter;
	
	@ServiceDependency
	private volatile ConfigurationManager configurationManager;
	
	//TODO: Get these paths from pnnl.goss.gridappsd.cfg file
	String commandFNCS = "fncs_broker 2";
	String commandGridLABD = "gridlabd";
	String commandFNCS_GOSS_Bridge = "fncs_goss_bridge.py";
	
	
	@Start
	public void start() throws Exception{
		System.out.println("STARTING SIMULATION MGR IMPL");
		Credentials credentials = new UsernamePasswordCredentials(
				GridAppsDConstants.username, GridAppsDConstants.password);
		client = clientFactory.create(PROTOCOL.STOMP,credentials);
//		try{ 
//			
//			log.debug("Starting "+this.getClass().getName());
//			
//			Credentials credentials = new UsernamePasswordCredentials(
//					GridAppsDConstants.username, GridAppsDConstants.password);
//			client = clientFactory.create(PROTOCOL.STOMP,credentials);
//		}
//		catch(Exception e){
//			e.printStackTrace();
//		}
		
	}
	
	/**
	 * This method is called by Process Manager to start a simulation
	 * @param simulationId
	 * @param simulationFile
	 */
	@Override
	public void startSimulation(int simulationId, File simulationFile, SimulationConfig simulationConfig){
		
			
			
			Thread thread = new Thread(new Runnable() {
				
				@Override
				public void run() {
					
					
					
					try{
					
						//Start FNCS
						RunCommandLine.runCommand(getPath(GridAppsDConstants.FNCS_PATH)+commandFNCS);
						
						//TODO: check if FNCS is started correctly and send publish simulation status accordingly
						statusReporter.reportStatus(GridAppsDConstants.topic_simulationStatus+simulationId, "FNCS Co-Simulator started");
						//client.publish(GridAppsDConstants.topic_simulationStatus+simulationId, "FNCS Co-Simulator started");
						
						//Start GridLAB-D
						RunCommandLine.runCommand(getPath(GridAppsDConstants.GRIDLABD_PATH)+commandGridLABD+" "+simulationFile);
						
						//TODO: check if GridLAB-D is started correctly and send publish simulation status accordingly
						statusReporter.reportStatus(GridAppsDConstants.topic_simulationStatus+simulationId, "GridLAB-D started");
						//client.publish(GridAppsDConstants.topic_simulationStatus+simulationId, "GridLAB-D started");
						
						//Start GOSS-FNCS Bridge
						RunCommandLine.runCommand("python "+getPath(GridAppsDConstants.FNCS_BRIDGE_PATH)+commandFNCS_GOSS_Bridge);
						
						//TODO: check if bridge is started correctly and send publish simulation status accordingly
						statusReporter.reportStatus(GridAppsDConstants.topic_simulationStatus+simulationId, "FNCS-GOSS Bridge started");
						//client.publish(GridAppsDConstants.topic_simulationStatus+simulationId, "FNCS-GOSS Bridge started");
						
						//Subscribe to fncs-goss-bridge output topic
						client.subscribe(GridAppsDConstants.topic_FNCS_output, new GossResponseEvent() {
							
							@Override
							public void onMessage(Serializable response) {
								try{
									//TODO: check response from fncs_goss_bridge
									statusReporter.reportStatus(GridAppsDConstants.topic_simulationStatus+simulationId, "FNCS-GOSS Bridge response:"+response);
									System.out.print(response);
									
									
								}catch (Exception e){
									e.printStackTrace();
								}
							}
						});
						
						//Send 'isInitialized' call to fncs-goss-bridge to check initialization.
						//This call would return true/false for initialization and simulation output of time step 0.
						client.publish(GridAppsDConstants.topic_FNCS_input, "{'command': 'isInitialized'");
						
						// Send fncs timestep updates for the specified duration.
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						String startTimeStr = simulationConfig.getStart_time();
						Date startTime = sdf.parse(startTimeStr);
						long endTime = startTime.getTime() + (simulationConfig.getDuration()*1000);
						long currentTime = startTime.getTime(); //incrementing integer 0 ,1, 2.. representing seconds
						
						while(currentTime < endTime){
							//send next timestep to fncs bridge
							String message = "{'command': 'nextTimeStep', 'currentTime': "+currentTime+"}";
							client.publish(GridAppsDConstants.topic_FNCS_input, message);
							Thread.sleep(1000);
							
							currentTime += 1000;
						}
						
						
					}
					catch(Exception e){
							e.printStackTrace();
							try {
								statusReporter.reportStatus(GridAppsDConstants.topic_simulationStatus+simulationId, "Simulation error: "+e.getMessage());
							} catch (Exception e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
					}
				}
			});
			
			thread.start();
			
		
		
		
		
	}

	
	private String getPath(String key){
		String path = configurationManager.getConfigurationProperty(key);
		if(path==null){
			log.warn("Configuration property not found, defaulting to .: "+key);
			path = ".";
		}
		if(!path.endsWith(File.separator)){
			path = path+File.separator;
		}
		return path;
	}
	

}
