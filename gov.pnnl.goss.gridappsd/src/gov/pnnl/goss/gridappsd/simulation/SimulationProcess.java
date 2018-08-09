package gov.pnnl.goss.gridappsd.simulation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import gov.pnnl.goss.gridappsd.api.AppManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.ServiceManager;
import gov.pnnl.goss.gridappsd.dto.FncsBridgeResponse;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.SimulationConfig;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.SimulationContext;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import gov.pnnl.goss.gridappsd.utils.RunCommandLine;
import pnnl.goss.core.Client;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.GossResponseEvent;

public class SimulationProcess extends Thread {
	private static Logger log = LoggerFactory.getLogger(SimulationProcess.class);

	
	boolean running = true;
	
	public boolean isRunning() {
		return running;
	}
	public void setRunning(boolean running) {
		this.running = running;
	}

	
	class InitializedTracker {
		public boolean isInited = false;
	}

	SimulationContext simContext;
	ServiceManager serviceManager;
	SimulationConfig simulationConfig;
	int simulationId;
	LogManager logManager;
	AppManager appManager;
	Client client;
	
	public SimulationProcess(SimulationContext simContext, ServiceManager serviceManager,
			SimulationConfig simulationConfig, int simulationId, LogManager logManager,
			AppManager appManager, Client client){
		this.simContext = simContext;
		this.serviceManager = serviceManager;
		this.simulationConfig = simulationConfig;
		this.simulationId = simulationId;
		this.logManager = logManager;
		this.appManager = appManager;
		this.client = client;
	}
	


	@Override
	public void run() {

		Process simulatorProcess = null;
		InitializedTracker isInitialized = new InitializedTracker();
		NextTimeStepTracker timeAdvanced = new NextTimeStepTracker();
		try{

			File simulationFile = new File(simContext.getStartupFile());

			if(simulationConfig!=null && simulationConfig.model_creation_config!=null && simulationConfig.model_creation_config.schedule_name!=null && simulationConfig.model_creation_config.schedule_name.trim().length()>0){
				File serviceDir = serviceManager.getServiceConfigDirectory();
				try{
					RunCommandLine.runCommand("cp "+serviceDir.getAbsolutePath()+File.separator+"etc"+File.separator+"zipload_schedule.player "+simulationFile.getParentFile().getAbsolutePath()+File.separator+simulationConfig.model_creation_config.schedule_name+".player");
				}catch(Exception e){
					log.warn("Could not copy player file to working directory");
				}
			}
			
			//Start GridLAB-D
			logManager.log(new LogMessage(this.getClass().getSimpleName(),
					Integer.toString(simulationId), 
					new Date().getTime(), 
					simContext.getSimulatorPath()+" "+simulationFile,
					LogLevel.INFO, 
					ProcessStatus.RUNNING, 
					true),GridAppsDConstants.username,
					GridAppsDConstants.topic_platformLog);
			ProcessBuilder simulatorBuilder = new ProcessBuilder(simContext.getSimulatorPath(), simulationFile.getAbsolutePath());
			simulatorBuilder.redirectErrorStream(true);
			simulatorBuilder.redirectOutput();
			//launch from directory containing simulation files
			simulatorBuilder.directory(simulationFile.getParentFile());
			simulatorProcess = simulatorBuilder.start();
			// Watch the process
			watch(simulatorProcess, "Simulator");


			//TODO: check if GridLAB-D is started correctly and send publish simulation status accordingly

			logManager.log(new LogMessage(this.getClass().getSimpleName(),
					Integer.toString(simulationId), 
					new Date().getTime(), 
					"GridLAB-D started", 
					LogLevel.INFO, 
					ProcessStatus.RUNNING, 
					true),GridAppsDConstants.username,
					GridAppsDConstants.topic_platformLog);
			
			//Subscribe to fncs-goss-bridge output topic
			GossFncsResponseEvent gossFncsResponseEvent = new GossFncsResponseEvent(logManager, isInitialized, timeAdvanced, simulationId);
			client.subscribe(GridAppsDConstants.topic_FNCS_output, gossFncsResponseEvent);

			int initAttempts = 0;
			while(!isInitialized.isInited && initAttempts<SimulationManagerImpl.MAX_INIT_ATTEMPTS){
				//Send 'isInitialized' call to fncs-goss-bridge to check initialization until it is initialized.
				//TODO add limiting how long it checks for initialized, or cancel if the fncs process exits
				//This call would return true/false for initialization and simulation output of time step 0.
				logManager.log(new LogMessage(this.getClass().getSimpleName(),
						Integer.toString(simulationId),
						new Date().getTime(), 
						"Checking fncs is initialized, currently "+isInitialized.isInited,
						LogLevel.INFO, 
						ProcessStatus.RUNNING, 
						true),GridAppsDConstants.username,
						GridAppsDConstants.topic_platformLog);
				
				client.publish(GridAppsDConstants.topic_FNCS_input, "{\"command\": \"isInitialized\"}");
				initAttempts++;
				Thread.sleep(1000);

			}

			if(initAttempts<SimulationManagerImpl.MAX_INIT_ATTEMPTS){
				logManager.log(new LogMessage(this.getClass().getSimpleName(),
						Integer.toString(simulationId), 
						new Date().getTime(), 
						"FNCS Initialized", 
						LogLevel.INFO, 
						ProcessStatus.RUNNING, 
						true),GridAppsDConstants.username,
						GridAppsDConstants.topic_platformLog);


				//Send the timesteps by second for the amount of time specified in the simulation config
                sendTimesteps(gossFncsResponseEvent, simulationConfig, simulationId);
			} else {
				logManager.log(new LogMessage(this.getClass().getSimpleName(),
						Integer.toString(simulationId), 
						new Date().getTime(), 
						"FNCS Initialization Failed", 
						LogLevel.ERROR, 
						ProcessStatus.ERROR,  
						true),GridAppsDConstants.username,
						GridAppsDConstants.topic_platformLog);

			}

            //call to stop the fncs broker
		    client.publish(GridAppsDConstants.topic_FNCS_input, "{\"command\":  \"stop\"}");
		    logManager.log(new LogMessage(this.getClass().getSimpleName(),
		    		Integer.toString(simulationId), 
					new Date().getTime(), 
					"Simulation "+simulationId+" complete", 
					LogLevel.INFO, 
					ProcessStatus.COMPLETE,
					true),GridAppsDConstants.username,
					GridAppsDConstants.topic_platformLog);
		}
		catch(Exception e){
				log.error("Error during simulation",e);
				try {
					logManager.log(new LogMessage(this.getClass().getSimpleName(),
							Integer.toString(simulationId), 
							new Date().getTime(), 
							"Simulation error: "+e.getMessage(),
							LogLevel.ERROR, 
							ProcessStatus.ERROR,
							true),GridAppsDConstants.username,
							GridAppsDConstants.topic_platformLog);
				} catch (Exception e1) {
					log.error("Error while reporting error status", e);
				}
		} finally {
			//Shut down applications and services connected with the simulation
			List<String> ids = simContext.getServiceInstanceIds();
			simulatorProcess.destroy();
			try {
				simulatorProcess.waitFor(10, TimeUnit.MILLISECONDS);
			} catch(InterruptedException ex) {
				simulatorProcess.destroyForcibly();
			}
			for(String id : ids){
				serviceManager.stopServiceInstance(id);
			}
			ids = simContext.getAppInstanceIds();
			for(String id : ids){
				appManager.stopAppInstance(id);
			}						
		}
	}

	
	private void sendTimesteps(GossFncsResponseEvent gossEvent, SimulationConfig simulationConfig, int simulationId) throws Exception{
		// Send fncs timestep updates for the specified duration.

		Date startTime = new Date(simulationConfig.getStart_time()); 
		long endTime = startTime.getTime() + (simulationConfig.getDuration()*1000);
		long currentTime = startTime.getTime(); //incrementing integer 0 ,1, 2.. representing seconds
		int seconds = 0;
		long busyWait = 10;
		Thread.sleep(simulationConfig.timestep_frequency);
		while(currentTime < endTime){
			//send next timestep to fncs bridge 
			logManager.log(new LogMessage(this.getClass().getSimpleName(),
					Integer.toString(simulationId), 
					new Date().getTime(), 
					"Sending timestep "+(seconds + 1), 
					LogLevel.INFO, 
					ProcessStatus.RUNNING, 
					true),GridAppsDConstants.username,
					GridAppsDConstants.topic_platformLog);
			String message = "{\"command\": \"nextTimeStep\", \"currentTime\": "+seconds+"}";
			client.publish(GridAppsDConstants.topic_FNCS_input, message);
			int timeStepAttempts = 0;
			while(!gossEvent.nextTimeStepTracker.isNextTimeStep) {
				timeStepAttempts++;
				if(timeStepAttempts < 100) {
					Thread.sleep(busyWait);
				} else {
					logManager.log(new LogMessage(this.getClass().getSimpleName(),
							Integer.toString(simulationId),
							new Date().getTime(),
							"FNCS_GOSS_Bridge failed to return a nextTimeStep response within"
							+ " the timestep_frequency of " + simulationConfig.timestep_frequency + " ms",
							LogLevel.INFO,
							ProcessStatus.RUNNING,
							true), GridAppsDConstants.username,
							GridAppsDConstants.topic_platformLog);
					//throw new Exception("FNCS_GOSS_Bridge failed to return a nextTimeStep response within"
					//		+ " the timestep_frequency of " + simulationConfig.timestep_frequency + " ms");
					gossEvent.nextTimeStepTracker.isNextTimeStep = true;
				}
			}
			gossEvent.nextTimeStepTracker.isNextTimeStep = false;
			logManager.log(new LogMessage(this.getClass().getSimpleName(),
					Integer.toString(simulationId),
					new Date().getTime(),
					"It took " + (timeStepAttempts * 10) + "ms for FNCS approve timestep " + (seconds + 1),
					LogLevel.INFO,
					ProcessStatus.RUNNING,
					true), GridAppsDConstants.username,
					GridAppsDConstants.topic_platformLog);
			long timeLeft = simulationConfig.timestep_frequency - (timeStepAttempts * busyWait);
			if(timeLeft < 10) {
				Thread.sleep(10);
			} else {
				Thread.sleep(timeLeft);
			}
			seconds++;
			currentTime += simulationConfig.timestep_increment;
		}
	}
	
	private void watch(final Process process, String processName) {
	    new Thread() {
	        public void run() {
	            BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
	            String line = null;
	            try {
	                while ((line = input.readLine()) != null) {
	                    log.info(processName+": "+line.substring(0,200));
	                }
	            } catch (IOException e) {
	                log.error("Error on process "+processName, e);
	            }
	        }
	    }.start();
	}
	
    class NextTimeStepTracker {
    	public boolean isNextTimeStep = false;
    }

	
	class GossFncsResponseEvent implements GossResponseEvent{
		InitializedTracker initializedTracker;
		volatile NextTimeStepTracker nextTimeStepTracker;
		LogManager logManager;
		int simulationId;
		public GossFncsResponseEvent(LogManager logManager, InitializedTracker initialized, NextTimeStepTracker timeAdvanced, int id) {
			this.logManager = logManager;
			initializedTracker = initialized;
			nextTimeStepTracker = timeAdvanced;
			simulationId = id;
		}


		@Override
		public void onMessage(Serializable response) {
			try{
				//Parse response
				// if it is an isInitialized response, check the value and send timesteps if true, or wait and publish another check if false
				
				DataResponse dataResponse = (DataResponse)response;
				
				logManager.log(new LogMessage(this.getClass().getSimpleName(),
						Integer.toString(simulationId), 
						new Date().getTime(), 
						 "FNCS-GOSS Bridge response:"+dataResponse.getData(), 
							LogLevel.INFO, 
							ProcessStatus.RUNNING,
						true),GridAppsDConstants.username,
						GridAppsDConstants.topic_platformLog);

				Gson  gson = new Gson();
				
				FncsBridgeResponse responseJson = gson.fromJson(dataResponse.getData().toString(), FncsBridgeResponse.class);
				log.debug("FNCS output message: "+responseJson);
				if("isInitialized".equals(responseJson.command)){
					log.debug("FNCS Initialized response: "+responseJson);
					if("True".equals(responseJson.response)){
                        log.info("FNCS is initialized "+initializedTracker);
						initializedTracker.isInited = true;
					}
				} else if("nextTimeStep".equals(responseJson.command)){
					log.debug("FNCS nextTimeStep response: "+responseJson);
					if("True".equals(responseJson.response)){
						log.info("FNCS timestep successful");
						nextTimeStepTracker.isNextTimeStep = true;
					}
				}




			}catch (Exception e){
				e.printStackTrace();
			}
		}
	}
}
