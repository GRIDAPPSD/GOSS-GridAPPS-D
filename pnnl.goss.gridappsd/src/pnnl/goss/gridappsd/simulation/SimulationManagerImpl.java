package pnnl.goss.gridappsd.simulation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Date;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.core.server.ServerControl;
import pnnl.goss.gridappsd.api.ConfigurationManager;
import pnnl.goss.gridappsd.api.SimulationManager;
import pnnl.goss.gridappsd.api.StatusReporter;
import pnnl.goss.gridappsd.dto.FncsBridgeResponse;
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
	final static int MAX_INIT_ATTEMPTS = 50;
	
	Client client = null; 
	
	@ServiceDependency
	private volatile ClientFactory clientFactory;
	
	@ServiceDependency
	ServerControl serverControl;
	
	@ServiceDependency
	private volatile StatusReporter statusReporter;
	
	@ServiceDependency
	private volatile ConfigurationManager configurationManager;
	
	
	@Start
	public void start() throws Exception{
		log.info("Starting simulation manager");
		//TODO
		Credentials credentials = new UsernamePasswordCredentials(
				GridAppsDConstants.username, GridAppsDConstants.password);
		client = clientFactory.create(PROTOCOL.STOMP,credentials);
		
	}
	
	/**
	 * This method is called by Process Manager to start a simulation
	 * @param simulationId
	 * @param simulationFile
	 */
	@Override
	public void startSimulation(int simulationId, File simulationFile, SimulationConfig simulationConfig){
		
			try {
				statusReporter.reportStatus(GridAppsDConstants.topic_simulationStatus+simulationId, "Starting simulation "+simulationId);
			} catch (Exception e2) {
				log.warn("Error while reporting status "+e2.getMessage());
			}

			
			Thread thread = new Thread(new Runnable() {
				
				@Override
				public void run() {
					
					Process gridlabdProcess = null;
					Process fncsProcess = null;
					Process fncsBridgeProcess = null;
					Process vvoAppProcess = null;
					InitializedTracker isInitialized = new InitializedTracker();
					try{
						
						File defaultLogDir = simulationFile.getParentFile();
					
						//Start FNCS
						//TODO, verify no errors on this
						log.info("Calling "+getPath(GridAppsDConstants.FNCS_PATH)+" 2");
//						RunCommandLine.runCommand(getPath(GridAppsDConstants.FNCS_PATH)+" 2");
						ProcessBuilder fncsBuilder = new ProcessBuilder(getPath(GridAppsDConstants.FNCS_PATH), "2");
						fncsBuilder.redirectErrorStream(true);
						fncsBuilder.redirectOutput(new File(defaultLogDir.getAbsolutePath()+File.separator+"fncs.log"));
						fncsProcess = fncsBuilder.start();
						// Watch the process
						watch(fncsProcess, "FNCS");
						//TODO: check if FNCS is started correctly and send publish simulation status accordingly
						statusReporter.reportStatus(GridAppsDConstants.topic_simulationStatus+simulationId, "FNCS Co-Simulator started");
						//client.publish(GridAppsDConstants.topic_simulationStatus+simulationId, "FNCS Co-Simulator started");
						
						//Start GridLAB-D
						log.info("Calling "+getPath(GridAppsDConstants.GRIDLABD_PATH)+" "+simulationFile);
//						RunCommandLine.runCommand(getPath(GridAppsDConstants.GRIDLABD_PATH)+" "+simulationFile);
						
						
						ProcessBuilder gridlabDBuilder = new ProcessBuilder(getPath(GridAppsDConstants.GRIDLABD_PATH), simulationFile.getAbsolutePath());
						gridlabDBuilder.redirectErrorStream(true);
						gridlabDBuilder.redirectOutput(new File(defaultLogDir.getAbsolutePath()+File.separator+"gridlabd.log"));
						//launch from directory containing simulation files
						gridlabDBuilder.directory(simulationFile.getParentFile());
						gridlabdProcess = gridlabDBuilder.start();
						// Watch the process
						watch(gridlabdProcess, "GridLABD");
						
						
						//TODO: check if GridLAB-D is started correctly and send publish simulation status accordingly
						statusReporter.reportStatus(GridAppsDConstants.topic_simulationStatus+simulationId, "GridLAB-D started");
						
						
						if(simulationConfig!=null && simulationConfig.model_creation_config!=null && simulationConfig.model_creation_config.schedule_name!=null && simulationConfig.model_creation_config.schedule_name.trim().length()>0){
							File bridgeCmd = new File(getPath(GridAppsDConstants.FNCS_BRIDGE_PATH));
							//copy zipload_schedule.player file
							try{
								RunCommandLine.runCommand("cp "+bridgeCmd.getParentFile().getAbsolutePath()+File.separator+"zipload_schedule.player "+simulationFile.getParentFile().getAbsolutePath()+File.separator+simulationConfig.model_creation_config.schedule_name+".player");
							}catch(Exception e){
								log.warn("Could not copy player file to working directory");
							}
						}
						
						
						//Start VVO Application
						//TODO filname really should be constant
						String vvoInputFile = simulationFile.getParentFile().getAbsolutePath()+File.separator+"vvo_inputs.json";
						log.info("Calling "+"python "+getPath(GridAppsDConstants.VVO_APP_PATH)+" "+simulationId+" "+vvoInputFile);
						ProcessBuilder vvoAppBuilder = new ProcessBuilder("python", getPath(GridAppsDConstants.VVO_APP_PATH), ""+simulationId, vvoInputFile);
						vvoAppBuilder.redirectErrorStream(true);
						vvoAppBuilder.redirectOutput(new File(defaultLogDir.getAbsolutePath()+File.separator+"vvo_app.log"));
						vvoAppProcess = vvoAppBuilder.start();
						// Watch the process
						watch(vvoAppProcess, "VVO Application");
						
						//TODO: check if bridge is started correctly and send publish simulation status accordingly
						statusReporter.reportStatus(GridAppsDConstants.topic_simulationStatus+simulationId, "FNCS-GOSS Bridge started");

						
						
						//Start GOSS-FNCS Bridge
						log.info("Calling "+"python "+getPath(GridAppsDConstants.FNCS_BRIDGE_PATH)+" "+simulationConfig.getSimulation_name());
						ProcessBuilder fncsBridgeBuilder = new ProcessBuilder("python", getPath(GridAppsDConstants.FNCS_BRIDGE_PATH), simulationConfig.getSimulation_name());
						fncsBridgeBuilder.redirectErrorStream(true);
						fncsBridgeBuilder.redirectOutput(new File(defaultLogDir.getAbsolutePath()+File.separator+"fncs_goss_bridge.log"));
						fncsBridgeProcess = fncsBridgeBuilder.start();
						// Watch the process
						watch(fncsBridgeProcess, "FNCS GOSS Bridge");
						
						//TODO: check if bridge is started correctly and send publish simulation status accordingly
						statusReporter.reportStatus(GridAppsDConstants.topic_simulationStatus+simulationId, "FNCS-GOSS Bridge started");
						
						
						//Subscribe to fncs-goss-bridge output topic
						client.subscribe(GridAppsDConstants.topic_FNCS_output, new GossFncsResponseEvent(statusReporter, isInitialized, simulationId));
						
						int initAttempts = 0;
						while(!isInitialized.isInited && initAttempts<MAX_INIT_ATTEMPTS){
							//Send 'isInitialized' call to fncs-goss-bridge to check initialization until it is initialized.
							//TODO add limiting how long it checks for initialized, or cancel if the fncs process exits
							//This call would return true/false for initialization and simulation output of time step 0.
							log.debug("Checking fncs is initialized, currently "+isInitialized.isInited);

							client.publish(GridAppsDConstants.topic_FNCS_input, "{\"command\": \"isInitialized\"}");
							initAttempts++;
							Thread.sleep(1000);
							
						}
						
						if(initAttempts<MAX_INIT_ATTEMPTS){
							statusReporter.reportStatus(GridAppsDConstants.topic_simulationStatus+simulationId, "FNCS Initialized");
							
							//Send the timesteps by second for the amount of time specified in the simulation config
	                        sendTimesteps(simulationConfig, simulationId); 
						} else {
							statusReporter.reportStatus(GridAppsDConstants.topic_simulationStatus+simulationId, "FNCS Initialization Failed");
						}
                        
                        //call to stop the fncs broker
					    client.publish(GridAppsDConstants.topic_FNCS_input, "{\"command\":  \"stop\"}");
					    statusReporter.reportStatus(GridAppsDConstants.topic_simulationStatus+simulationId, "Simulation "+simulationId+" complete");
					}
					catch(Exception e){
							log.error("Error during simulation",e);
							try {
								statusReporter.reportStatus(GridAppsDConstants.topic_simulationStatus+simulationId, "Simulation error: "+e.getMessage());
							} catch (Exception e1) {
								log.error("Error while reporting error status", e);
							}
					} finally {
						//shut down fncs broker and gridlabd and bridge if still running
						if(fncsProcess!=null){
							fncsProcess.destroy();
						}
						if(gridlabdProcess!=null){
							gridlabdProcess.destroy();
						}
						if(fncsBridgeProcess!=null){
							fncsBridgeProcess.destroy();
						}
					}
				}
			});
			
			thread.start();
			
		
		
		
		
	}
        class InitializedTracker {
		public boolean isInited = false;
        }	

	 class GossFncsResponseEvent implements GossResponseEvent{
		InitializedTracker initializedTracker;
		StatusReporter statusReporter;
		int simulationId;
		public GossFncsResponseEvent(StatusReporter reporter, InitializedTracker initialized, int id) {
			statusReporter = reporter;
			initializedTracker = initialized;
			simulationId = id;
		}
		 
		 
		@Override
		public void onMessage(Serializable response) {
			try{
				//Parse response
				// if it is an isInitialized response, check the value and send timesteps if true, or wait and publish another check if false
				statusReporter.reportStatus(GridAppsDConstants.topic_simulationStatus+simulationId, "FNCS-GOSS Bridge response:"+response);
				
				Gson  gson = new Gson();
				FncsBridgeResponse responseJson = gson.fromJson(response.toString(), FncsBridgeResponse.class);
				log.debug("FNCS output message: "+responseJson);
				if("isInitialized".equals(responseJson.command)){
					log.debug("FNCS Initialized response: "+responseJson);
					if("True".equals(responseJson.response)){
                        log.info("FNCS is initialized "+initializedTracker);
						initializedTracker.isInited = true;
					}
				} else {
					//System.out.println("RESPONSE COMMAND "+responseJson.command);
					//??
				}
				
				
				
			}catch (Exception e){
				e.printStackTrace();
			}
		}
	 }
	
	private void sendTimesteps(SimulationConfig simulationConfig, int simulationId) throws Exception{
		// Send fncs timestep updates for the specified duration.
		
		String startTimeStr = simulationConfig.getStart_time();
		Date startTime = GridAppsDConstants.SDF_GLM_CLOCK.parse(startTimeStr);
		long endTime = startTime.getTime() + (simulationConfig.getDuration()*1000);
		long currentTime = startTime.getTime(); //incrementing integer 0 ,1, 2.. representing seconds
		int seconds = 0;
		while(currentTime < endTime){
			//send next timestep to fncs bridge 
			statusReporter.reportStatus(GridAppsDConstants.topic_simulationStatus+simulationId, "Sending timestep "+seconds);
			String message = "{\"command\": \"nextTimeStep\", \"currentTime\": "+seconds+"}";
			client.publish(GridAppsDConstants.topic_FNCS_input, message);
			Thread.sleep(simulationConfig.timestep_frequency);
			
			seconds++;
			currentTime += simulationConfig.timestep_increment;
		}
	}

	
	private String getPath(String key){
		String path = configurationManager.getConfigurationProperty(key);
		if(path==null){
			log.warn("Configuration property not found, defaulting to .: "+key);
			path = ".";
		}
		return path;
	}
	
	
	
	private void watch(final Process process, String processName) {
	    new Thread() {
	        public void run() {
	            BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
	            String line = null; 
	            try {
	                while ((line = input.readLine()) != null) {
	                    log.info(processName+": "+line);
	                }
	            } catch (IOException e) {
	                log.error("Error on process "+processName, e);
	            }
	        }
	    }.start();
	}

}
