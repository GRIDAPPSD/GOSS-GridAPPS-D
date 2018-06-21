/*******************************************************************************
 * Copyright 2017, Battelle Memorial Institute All rights reserved.
 * Battelle Memorial Institute (hereinafter Battelle) hereby grants permission to any person or entity
 * lawfully obtaining a copy of this software and associated documentation files (hereinafter the
 * Software) to redistribute and use the Software in source and binary forms, with or without modification.
 * Such person or entity may use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and may permit others to do so, subject to the following conditions:
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the
 * following disclaimers.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided with the distribution.
 * Other than as used herein, neither the name Battelle Memorial Institute or Battelle may be used in any
 * form whatsoever without the express written consent of Battelle.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * BATTELLE OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * General disclaimer for use with OSS licenses
 *
 * This material was prepared as an account of work sponsored by an agency of the United States Government.
 * Neither the United States Government nor the United States Department of Energy, nor Battelle, nor any
 * of their employees, nor any jurisdiction or organization that has cooperated in the development of these
 * materials, makes any warranty, express or implied, or assumes any legal liability or responsibility for
 * the accuracy, completeness, or usefulness or any information, apparatus, product, software, or process
 * disclosed, or represents that its use would not infringe privately owned rights.
 *
 * Reference herein to any specific commercial product, process, or service by trade name, trademark, manufacturer,
 * or otherwise does not necessarily constitute or imply its endorsement, recommendation, or favoring by the United
 * States Government or any agency thereof, or Battelle Memorial Institute. The views and opinions of authors expressed
 * herein do not necessarily state or reflect those of the United States Government or any agency thereof.
 *
 * PACIFIC NORTHWEST NATIONAL LABORATORY operated by BATTELLE for the
 * UNITED STATES DEPARTMENT OF ENERGY under Contract DE-AC05-76RL01830
 ******************************************************************************/
package gov.pnnl.goss.gridappsd.simulation;

import gov.pnnl.goss.gridappsd.api.AppManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.ServiceManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.dto.FncsBridgeResponse;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.SimulationConfig;
import gov.pnnl.goss.gridappsd.dto.SimulationContext;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import gov.pnnl.goss.gridappsd.utils.RunCommandLine;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
import pnnl.goss.core.server.ServerControl;

import com.google.gson.Gson;

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
	private volatile ServiceManager serviceManager;
	
	@ServiceDependency
	private volatile AppManager appManager;
	
	@ServiceDependency
	LogManager logManager;
	
	private Map<String, SimulationContext> simContexts  = new HashMap<String, SimulationContext>();

	public SimulationManagerImpl(){ }


	public SimulationManagerImpl(ClientFactory clientFactory, ServerControl serverControl,
			LogManager logManager) {
		this.clientFactory = clientFactory;
		this.serverControl = serverControl;
		this.logManager = logManager;
		//this.configurationManager = configurationManager;
	}
	@Start
	public void start() throws Exception{
		
		Credentials credentials = new UsernamePasswordCredentials(
				GridAppsDConstants.username, GridAppsDConstants.password);
		client = clientFactory.create(PROTOCOL.STOMP,credentials);
		client.publish("goss.gridappsd.log.platform", new LogMessage(this.getClass().getSimpleName(),
				null,
				new Date().getTime(), 
				this.getClass().getSimpleName()+" Started", 
				LogLevel.INFO, 
				ProcessStatus.STARTED, 
				true).toString());
	}

	/**
	 * This method is called by Process Manager to start a simulation
	 * @param simulationId
	 * @param simulationFile
	 */
	@Override
	public void startSimulation(int simulationId, SimulationConfig simulationConfig, SimulationContext simContext){

			try {
				logManager.log(new LogMessage(this.getClass().getSimpleName(),
						Integer.toString(simulationId), 
						new Date().getTime(), 
						"Starting simulation "+simulationId, 
						LogLevel.INFO, 
						ProcessStatus.STARTING, 
						true),GridAppsDConstants.username,
						GridAppsDConstants.topic_platformLog);
			} catch (Exception e2) {
				log.warn("Error while reporting status "+e2.getMessage());
			}
			
			simContexts.put(simContext.getSimulationId(), simContext);


			Thread thread = new Thread(new Runnable() {

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
						while(!isInitialized.isInited && initAttempts<MAX_INIT_ATTEMPTS){
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

						if(initAttempts<MAX_INIT_ATTEMPTS){
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
			});

			thread.start();
	}


    class InitializedTracker {
    	public boolean isInited = false;
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


	private void sendTimesteps(GossFncsResponseEvent gossEvent, SimulationConfig simulationConfig, int simulationId) throws Exception{
		// Send fncs timestep updates for the specified duration.

		String startTimeStr = simulationConfig.getStart_time();
		Date startTime = GridAppsDConstants.SDF_GLM_CLOCK.parse(startTimeStr);
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


	/*private String getPath(String key){
		String path = configurationManager.getConfigurationProperty(key);
		if(path==null){
			log.warn("Configuration property not found, defaulting to .: "+key);
			path = ".";
		}
		return path;
	}*/



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

	
	public Map<String, SimulationContext> getSimContexts() {
		return simContexts;
	}

	@Override
	public SimulationContext getSimulationContextForId(String simulationId){
		return this.simContexts.get(simulationId);
	}
	
	
}
