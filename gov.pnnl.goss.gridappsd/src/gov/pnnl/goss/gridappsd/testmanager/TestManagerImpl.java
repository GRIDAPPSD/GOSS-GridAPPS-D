/*******************************************************************************
 * Copyright (c) 2017, Battelle Memorial Institute All rights reserved.
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
package gov.pnnl.goss.gridappsd.testmanager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.jms.JMSException;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.Map.Entry;
import java.util.stream.IntStream;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

import gov.pnnl.goss.gridappsd.api.AppManager;
import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.ProcessManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.api.TestManager;
import gov.pnnl.goss.gridappsd.data.GridAppsDataSources;
import gov.pnnl.goss.gridappsd.dto.AppInfo;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.PowerSystemConfig;	
import gov.pnnl.goss.gridappsd.dto.RequestSimulation;
import gov.pnnl.goss.gridappsd.dto.RequestTest;
import gov.pnnl.goss.gridappsd.dto.SimulationConfig;
import gov.pnnl.goss.gridappsd.dto.TestConfiguration;
import gov.pnnl.goss.gridappsd.dto.TestScript;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.core.Request.RESPONSE_FORMAT;



/**
 *
 * @author jsimpson
 *
 */
@Component
public class TestManagerImpl implements TestManager {
	

	public static final String topic_requestTest = "goss.gridappsd" +".test";
	
	private static Logger log = LoggerFactory.getLogger(TestManagerImpl.class);
	
	@ServiceDependency
	private volatile AppManager appManager;
	
	@ServiceDependency
	private volatile ClientFactory clientFactory;
	
	@ServiceDependency
	private volatile SimulationManager simulationManager;
	
	@ServiceDependency
	private volatile ConfigurationManager configurationManager;
	
	@ServiceDependency
	private volatile ProcessManager processManager;
	
	@ServiceDependency
	private volatile LogManager logManager;
	
	@ServiceDependency
	GridAppsDataSources dataSources;

	protected int tempIndex=0;
	
	protected int rulePort;
	
	protected String topic;
	
	protected int simulationID;
	
	protected TestConfiguration testConfig;
	
	protected TestResultSeries testResultSeries = new TestResultSeries();
	
	protected TestScript testScript;
	
	protected boolean testMode = false;

	protected String expectedResultSeriesPath;
	
	protected Process rulesProcess = null;
	
	protected boolean processExpectedResults = false;

	public TestManagerImpl(){}
	public TestManagerImpl(AppManager appManager,
			ClientFactory clientFactory, 
			ConfigurationManager configurationManager,
			SimulationManager simulationManager,
			LogManager logManager){
		this.appManager = appManager;
		this.clientFactory = clientFactory;
		this.configurationManager = configurationManager;
		this.simulationManager = simulationManager;
		this.logManager = logManager;
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
	
	@Start
	public void start(){
		
		try{
			LogMessage logMessageObj = createLogMessage();
			
			logMessageObj.setLogMessage("Starting "+this.getClass().getName());
			logManager.log(logMessageObj,GridAppsDConstants.username, GridAppsDConstants.topic_platformLog);
			
			Credentials credentials = new UsernamePasswordCredentials(
					GridAppsDConstants.username, GridAppsDConstants.password);
			Client client = clientFactory.create(PROTOCOL.STOMP,credentials);
			//TODO: subscribe to GridAppsDConstants.topic_request_prefix+/* instead of GridAppsDConstants.topic_requestSimulation
			client.subscribe(topic_requestTest, new GossResponseEvent() {

				private RequestTest reqTest;
				/*
				 * Need:
				 * TestConfig
				 * TestScript
				 * ExpectedResults
				 * SimuationID
				 * TestID
				 * @see pnnl.goss.core.GossResponseEvent#onMessage(java.io.Serializable)
				 */
				 
				@Override
				public void onMessage(Serializable message) {
					DataResponse event = (DataResponse)message;
					logMessageObj.setTimestamp(new Date().getTime());
					logMessageObj.setLogMessage("Recevied message: "+ event.getData() +" on topic "+event.getDestination());
					logManager.log(logMessageObj,GridAppsDConstants.username,GridAppsDConstants.topic_platformLog);
					
					System.out.println("TestManager got message " + message.toString());
					
					reqTest = RequestTest.parse(event.getData().toString());
					
					testScript = loadTestScript(reqTest.getTestScriptPath());
					
					testConfig = loadTestConfig(reqTest.getTestConfigPath());
					
					expectedResultSeriesPath = reqTest.getExpectedResult();
					
					simulationID = reqTest.getSimulationID();
					
					rulePort = reqTest.getRulePort();
					
					topic = reqTest.getTopic();
					
					testMode=true;
					
					DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
					df.setTimeZone(TimeZone.getTimeZone("UTC"));
					
//					String startDateStr= df.format(testConfig.getRun_start());
//							
//					String endDateStr = df.format(testConfig.getRun_end());
			
//				    System.out.println(startDateStr); 
//				    System.out.println(endDateStr);
					
					processExpectedResults=true;
					
					if (expectedResultSeriesPath == null || expectedResultSeriesPath.isEmpty()){
						logMessageObj.setTimestamp(new Date().getTime());
						logMessageObj.setLogMessage("TestManager expected output is null or empty. Skipping test.");
						logManager.log(logMessageObj,GridAppsDConstants.username, GridAppsDConstants.topic_platformLog);
						processExpectedResults=false;
					}else{
						File testFile = new File(expectedResultSeriesPath);
						if(!testFile.exists() || testFile.isDirectory()) {
							logMessageObj.setTimestamp(new Date().getTime());
							logMessageObj.setLogMessage("TestManager expected output does not exist:  "+ expectedResultSeriesPath);
							logManager.log(logMessageObj,GridAppsDConstants.username, GridAppsDConstants.topic_platformLog);
							processExpectedResults=false;
						}
					}
					
					forwardSimulationInput(client, simulationID);
					
					processSimulationOutput(logMessageObj, client, simulationID);

					try {
						
						File defaultLogDir = new File(reqTest.getTestScriptPath()).getParentFile();
						
						String appRuleName = testScript.getRules().get(0).name;
						
						AppInfo appInfo = null;
						for (AppInfo appInfoTemp : appManager.listApps()) {
							if (appInfoTemp.getId().equals(testScript.getApplication())){
								appInfo = appInfoTemp;
								break;
							}
						}
						
						if(appInfo == null){
							logManager.log(new LogMessage(this.getClass().getSimpleName(),
									Integer.toString(simulationID), 
									new Date().getTime(), 
									"Application not found for " + appRuleName,
									LogLevel.ERROR, 
									ProcessStatus.RUNNING, 
									true),GridAppsDConstants.username, GridAppsDConstants.topic_platformLog);
			                return;
												
						}
						
						
						File appDirectory = new File(appManager.getAppConfigDirectory().getAbsolutePath()
								+ File.separator + appInfo.getId() + File.separator + "tests");
						
						logManager.log(new LogMessage(this.getClass().getSimpleName(),
								Integer.toString(simulationID), 
								new Date().getTime(), 
								"Calling python "+appDirectory+File.separator+appRuleName+" "+simulationID,
								LogLevel.INFO, 
								ProcessStatus.RUNNING, 
								true),GridAppsDConstants.username, GridAppsDConstants.topic_platformLog);

						ProcessBuilder ruleAppBuilder = new ProcessBuilder("python", appDirectory+File.separator+appRuleName,"-t","input","-p",""+rulePort,"--id", ""+simulationID);
						ruleAppBuilder.redirectErrorStream(true);
						ruleAppBuilder.redirectOutput(new File(defaultLogDir.getAbsolutePath()+File.separator+"rule_app.log"));

						rulesProcess = ruleAppBuilder.start();
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						System.out.println("TestMan rule " + rulesProcess.isAlive());
						if ( ! rulesProcess.isAlive()){
							logManager.log(new LogMessage(this.getClass().getSimpleName(),
									Integer.toString(simulationID), 
									new Date().getTime(), 
									"Process " + appDirectory+File.separator+appRuleName+" " +"did not start check rule script and that redis is running." ,
									LogLevel.INFO, 
									ProcessStatus.RUNNING, 
									true),GridAppsDConstants.username, GridAppsDConstants.topic_platformLog);
			
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					// Watch the process
					watch(rulesProcess, "Rules Application");
				}
			});
			
			
//			 /topic/goss.gridappsd.platform.log
//			{"data":" {\"log_level\": \"DEBUG\", \"timestamp\": 158812, \"process_id\": \"fncs_goss_bridge-1454543646\", \"proces_status\": \"STARTED\", \"log_message\": \"received message {\\\"command\\\": \\\"nextTimeStep\\\", \\\"currentTime\\\": 29}\"}","responseComplete":false,
//			"destination":"/queue/goss.gridappsd.process.log.simulation.1454543646","id":"a5f879e2-7126-405c-8022-a18c63271d64"}


		   client.subscribe("/topic/"+GridAppsDConstants.topic_FNCS_input, new GossResponseEvent(){
//			   client.subscribe("/topic/goss.gridappsd.fncs.input", new GossResponseEvent(){  
	
				public void onMessage(Serializable message) {

					DataResponse event = (DataResponse)message;
					String str = event.getData().toString();
//					System.out.println("TestMana: Stopping 4 " + str);
					
					JsonObject jsonObject = CompareResults.getSimulationJson(str);
					if ( jsonObject.has("command") && 
							jsonObject.get("command").getAsString().toLowerCase().equals("stop")){
//						System.out.println("TestMana: Stopping 5 " + jsonObject.has("command"));

						if(rulesProcess !=null){
							logMessageObj.setLogMessage("Stopping rules process");
							logManager.log(logMessageObj,GridAppsDConstants.username, GridAppsDConstants.topic_platformLog);							
							rulesProcess.destroy();
						}
					}

				}
			});


		}
		catch(Exception e){
			log.error("Error in test manager",e);
		}	
	}
	
	public void processSimulationOutput(LogMessage logMessageObj, Client client, int simulationID) {
		client.subscribe("/topic/" + GridAppsDConstants.topic_simulationOutput + "." + simulationID,
		new GossResponseEvent() {
			public void onMessage(Serializable message) {
				String expected_output_series = "/home/gridappsd/gridappsd_project/sources/GOSS-GridAPPS-D/gov.pnnl.goss.gridappsd/test/gov/pnnl/goss/gridappsd/expected_output_series3.json";

				if (testMode && message != null) {
					expected_output_series = expectedResultSeriesPath;
				} else {
					return;
				}

				DataResponse event = (DataResponse) message;
				logMessageObj.setTimestamp(new Date().getTime());
				String subMsg = event.getData().toString();
				if (subMsg.length() >= 200)
					subMsg = subMsg.substring(0, 200);
				logMessageObj.setLogMessage("TestManager recevied message: " + subMsg + " on topic " + event.getDestination());

				CompareResults compareResults = new CompareResults();
				JsonObject jsonObject = CompareResults.getSimulationJson(message.toString());
				jsonObject = CompareResults.getSimulationJson(jsonObject.get("data").getAsString());

				if (jsonObject.get("output") == null || jsonObject.get("output").isJsonNull()) {
					logMessageObj.setTimestamp(new Date().getTime());
					if (jsonObject.get("output") == null)
						logMessageObj.setLogMessage("TestManager output is null.");
					else
						logMessageObj.setLogMessage("TestManager output is Json null" + jsonObject.get("output").toString());
					
					logManager.log(logMessageObj, GridAppsDConstants.username,
							GridAppsDConstants.topic_platformLog);
					return;
				}
				
				// Break up measurements to send to rules app
				JsonObject temp = CompareResults.getSimulationJson(message.toString());
				temp = CompareResults.getSimulationJson(temp.get("data").getAsString());
				JsonObject forwardObject = temp.get("output").getAsJsonObject();

                int meas_len = forwardObject.get("message").getAsJsonObject().get("measurements").getAsJsonArray().size();
                JsonArray tarray = forwardObject.get("message").getAsJsonObject().get("measurements").getAsJsonArray();
                int chunk_size = 500;
                IntStream.range(0, (meas_len-1) / chunk_size).forEachOrdered(end -> {
//                	System.out.println("TestManager range " + end*chunk_size + " " + ((end+1)*chunk_size-1));
                	JsonArray slice = getArraySlice(tarray, end*chunk_size, (end+1)*chunk_size); 
					forwardObject.get("message").getAsJsonObject().add("measurements", slice);
	            	forwardFNCSOutput(forwardObject,rulePort, topic);
                });
//                System.out.println("TestManager range " + ((meas_len-1) / chunk_size)*chunk_size + " " + (meas_len-1));
            	JsonArray slice = getArraySlice(tarray, ((meas_len-1) / chunk_size)*chunk_size, meas_len); 
				forwardObject.get("message").getAsJsonObject().add("measurements", slice);
				forwardFNCSOutput(forwardObject, rulePort, topic);

				if (!processExpectedResults) {
					return;
				}

				JsonElement simOutputObject = jsonObject.getAsJsonObject();
				String firstKey = CompareResults.getFirstKey(simOutputObject.getAsJsonObject());
				System.out.println("TestMan compare key " + firstKey);

				// Temp timeseries index
				String indexStr = tempIndex + "";
				tempIndex++;
				TestResults tr = compareResults.compareExpectedWithSimulationOutput(indexStr,
						simOutputObject.getAsJsonObject(), expected_output_series);
				if (tr != null) {
					testResultSeries.add(indexStr, tr);
				}
				 
				String test_id = testScript.getApplication();
				String simulation_time = simOutputObject.getAsJsonObject().get("output").getAsJsonObject().get("message").getAsJsonObject().get("timestamp").getAsString();
				for (Entry<String, HashMap<String, String[]>> entry : tr.objectPropComparison.entrySet()){
					HashMap<String, String[]> propMap = entry.getValue();
					for (Entry<String, String[]> prop: propMap.entrySet()){
						logManager.getLogDataManager().storeExpectedResults(test_id, ""+simulationID, java.sql.Timestamp.valueOf(simulation_time).getTime() , entry.getKey(), prop.getKey(), prop.getValue()[0], prop.getValue()[1]);
					}
				}
				
				logMessageObj.setTimestamp(new Date().getTime());
				logMessageObj.setLogMessage("Index: " + indexStr + " TestManager number of conflicts: "
						+ " total " + testResultSeries.getTotal());
				logManager.log(logMessageObj, GridAppsDConstants.username,
						GridAppsDConstants.topic_platformLog);
			}

			public JsonArray getArraySlice(JsonArray tarray, int start, int end) {
				JsonArray childJsonArray1 = new JsonArray();
				IntStream.range(start , end).forEachOrdered(ii -> {
				        JsonElement rec = tarray.get(ii);
				        childJsonArray1.add(rec); 		
				});
				return childJsonArray1;
			}

		});
	}
	
	public void forwardSimulationInput(Client client, int simulationID) {
		client.subscribe("/topic/" + GridAppsDConstants.topic_simulationInput +"."+ simulationID, new GossResponseEvent(){
			public void onMessage(Serializable message) {
				if( !(testMode && message != null)){
					return;
				}
				JsonObject jsonObject = CompareResults.getSimulationJson(message.toString()); 
				jsonObject = CompareResults.getSimulationJson(jsonObject.get("data").getAsString());
				JsonObject forwardObject = jsonObject.get("input").getAsJsonObject();
				forwardFNCSOutput(forwardObject,rulePort,topic);
			}
		});
	}
	
	public void forwardFNCSOutput(JsonObject jsonObject, int port, String topic) {
		CloseableHttpClient httpClient = HttpClientBuilder.create().build();
		try {
		    HttpPost request = new HttpPost("http://localhost:"+port+"/"+topic+"/events");
//			String str_json= "{\"simulation_id\" : \"12ae2345\", \"message\" : { \"timestamp\" : \"YYYY-MMssZ\", \"difference_mrid\" : \"123a456b-789c-012d-345e-678f901a234\", \"reverse_difference\" : { \"attribute\" : \"Switch.open\", \"value\" : \"0\" }, \"forward_difference\" : { \"attribute\" : \"Switch.open\", \"value\" : \"1\" } }}";
			
		    StringEntity params = new StringEntity(jsonObject.toString());
		    request.addHeader(HTTP.CONTENT_TYPE, "application/json");
		    request.addHeader("Accept","application/json");
		    request.setEntity(params);
		    CloseableHttpResponse response = httpClient.execute(request);	    
            /*Checking response */
            if (response != null) {
                InputStream in = response.getEntity().getContent(); //Get the data in the entity
            }

		} catch (Exception ex) {
		    // handle exception here
		} finally {
		    try {
				httpClient.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	
	private LogMessage createLogMessage() {
		LogMessage logMessageObj = new LogMessage();

//		logMessageObj.setProcessId(this.getClass().getSimpleName());
		logMessageObj.setSource(this.getClass().getSimpleName());

		logMessageObj.setLogLevel(LogLevel.DEBUG);
		logMessageObj.setSource(this.getClass().getSimpleName());
		logMessageObj.setProcessStatus(ProcessStatus.RUNNING);
		logMessageObj.setStoreToDb(true);
		logMessageObj.setTimestamp(new Date().getTime());
		return logMessageObj;
	}
	
	
	public TestConfiguration loadTestConfig(String path){
		LogMessage logMessageObj = createLogMessage();
		logMessageObj.setLogMessage("Loading TestCofiguration from:" + path);
		logManager.log(logMessageObj,GridAppsDConstants.username, GridAppsDConstants.topic_platformLog);
//		path = "/Users/jsimpson/git/adms/GOSS-GridAPPS-D/gov.pnnl.goss.gridappsd/applications/python/exampleTestConfig2.json";
//		Gson  gson = new Gson().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").create();
		Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
		JsonReader jsonReader;
		TestConfiguration testConfig = null;
		try {
			jsonReader = new JsonReader(new FileReader(path));
			jsonReader.setLenient(true);
			testConfig = gson.fromJson(new FileReader(path),TestConfiguration.class);
//			System.out.println(testConfig.toString());
			jsonReader.close();
		} catch (Exception e) {
//			e.printStackTrace();
			logMessageObj.setTimestamp(new Date().getTime());
			logMessageObj.setLogMessage("Error" + e.getMessage());
			logManager.log(logMessageObj,GridAppsDConstants.username,GridAppsDConstants.topic_platformLog);
		}
		return testConfig;
	}
	
	public TestScript loadTestScript(String path){
//		LogMessage logMessageObj = createLogMessage();
//		logMessageObj.setLogMessage("Loading TestScript from:" + path);
//		logManager.log(logMessageObj,GridAppsDConstants.username, GridAppsDConstants.topic_platformLog);
		Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
		JsonReader jsonReader;
		TestScript testScript = null;
		try {
			jsonReader = new JsonReader(new FileReader(path));
			jsonReader.setLenient(true);
			testScript = gson.fromJson(new FileReader(path), TestScript.class);
//			System.out.println(testScript.toString());
			jsonReader.close();
		} catch (Exception e) {
			e.printStackTrace();
//			logMessageObj.setTimestamp(new Date().getTime());
//			logMessageObj.setLogMessage("Error" + e.getMessage());
//			logManager.log(logMessageObj,GridAppsDConstants.username, GridAppsDConstants.topic_platformLog);
		}
		return testScript;
	}


	public void requestSimulation(Client client, TestConfiguration testConfiguration, TestScript ts) throws JMSException {
		//TODO: Request Simulation
			//TODO: 1 PowerSystemConfig
			//TODO: 2 SimulationConfig simulation_config
			//TODO: 3 Build/Set ApplicationConfig 
		//Create Request Simulation object, you could also just pass in a json string with the configuration
		PowerSystemConfig powerSystemConfig = new PowerSystemConfig();
		powerSystemConfig.GeographicalRegion_name = "ieee8500_Region";
		powerSystemConfig.SubGeographicalRegion_name = "ieee8500_SubRegion";
		powerSystemConfig.Line_name = "ieee8500";

		SimulationConfig simulationConfig = new SimulationConfig();
		simulationConfig.duration = 60;
		simulationConfig.power_flow_solver_method = "";
		simulationConfig.simulation_id = ""; //.setSimulation_name("");
		simulationConfig.simulator = ""; //.setSimulator("");

		simulationConfig.start_time = new Date().getTime(); //.setStart_time("");

		RequestSimulation requestSimulation = new RequestSimulation(powerSystemConfig, simulationConfig);

		Gson  gson = new Gson();
		String request = gson.toJson(requestSimulation);
		//Step3: Send configuration to the request simulation topic
		log.debug("Request simulation");
		log.debug("Client is:" + client);
		Serializable simulationId = client.getResponse(request, GridAppsDConstants.topic_requestSimulation, RESPONSE_FORMAT.JSON);
		log.debug("simulation id is: "+simulationId);
		//Subscribe to bridge output
		client.subscribe("goss/gridappsd/fncs/output", new GossResponseEvent() {
		    public void onMessage(Serializable response) {
		      log.debug("simulation output is: "+response);
		      System.out.println("simulation output is: "+response);
		      //TODO capture stream and save
		      
		    }
		});
	}
	
	private void requestSimOld(Client client, Serializable message, DataResponse event) {
//		statusReporter.reportStatus(String.format("Got new message in %s", getClass().getName()));
		//TODO: create registry mapping between request topics and request handlers.
		switch(event.getDestination().replace("/queue/", "")){
			case topic_requestTest : {
				log.debug("Received test request: "+ event.getData());
				
				//generate simulation id and reply to event's reply destination.
//				int simulationId = generateSimulationId();
				int testId = 1234;
				client.publish(event.getReplyDestination(), testId);
				try{
					// TODO: validate simulation request json and create PowerSystemConfig and SimulationConfig dto objects to work with internally.
					Gson  gson = new Gson();
						
					RequestSimulation config = gson.fromJson(message.toString(), RequestSimulation.class);
					log.info("Parsed config "+config);
					if(config==null || config.getPower_system_config()==null || config.getSimulation_config()==null){
						throw new RuntimeException("Invalid configuration received");
					}

					//make request to configuration Manager to get power grid model file locations and names
					log.debug("Creating simulation and power grid model files for simulation Id "+ testId);
					File simulationFile = configurationManager.getSimulationFile(testId, config);
					if(simulationFile==null){
						throw new Exception("No simulation file returned for request "+config);
					}
						
						
					log.debug("Simulation and power grid model files generated for simulation Id "+ testId);
					
					//start simulation
					log.debug("Starting simulation for id "+ testId);
					simulationManager.startSimulation(testId, config.getSimulation_config(),null);
					log.debug("Starting simulation for id "+ testId);
						
//								new ProcessSimulationRequest().process(event, client, configurationManager, simulationManager); break;
				}catch (Exception e){
					e.printStackTrace();
					try {
//						statusReporter.reportStatus(GridAppsDConstants.topic_simulationLog+testId, "Test Initialization error: "+e.getMessage());
						log.error("Test Initialization error",e);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
			}
			//case GridAppsDConstants.topic_requestData : processDataRequest(); break;
			//case GridAppsDConstants.topic_requestSimulationStatus : processSimulationStatusRequest(); break;
		}
	}
	
	
	public static void main(String[] args) {

	}
}
