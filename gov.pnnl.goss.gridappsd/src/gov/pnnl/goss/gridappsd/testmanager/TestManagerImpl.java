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
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.jms.JMSException;

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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.ProcessManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.api.StatusReporter;
import gov.pnnl.goss.gridappsd.api.TestManager;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.PowerSystemConfig;
import gov.pnnl.goss.gridappsd.dto.RequestSimulation;
import gov.pnnl.goss.gridappsd.dto.RequestTest;
import gov.pnnl.goss.gridappsd.dto.SimulationConfig;
import gov.pnnl.goss.gridappsd.dto.SimulationOutput;
import gov.pnnl.goss.gridappsd.dto.SimulationOutputObject;
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
	private volatile ClientFactory clientFactory;
	
	@ServiceDependency
	private volatile SimulationManager simulationManager;
	
	@ServiceDependency
	private volatile ConfigurationManager configurationManager;
	
	@ServiceDependency
	private volatile ProcessManager processManager;
	
	@ServiceDependency
	private volatile StatusReporter statusReporter;
	
	@ServiceDependency
	private volatile LogManager logManager;

	protected int tempIndex=0;
	
	protected int rulePort;
	
	protected String topic;
	
	protected int simulationID;
	
	protected TestConfiguration testConfig;
	
	protected TestResultSeries testResultSeries = new TestResultSeries();
	
	protected TestScript testScript;
	
	protected boolean testMode=false;

	protected String expectedResultSeriesPath;
	
	public TestManagerImpl(){}
	public TestManagerImpl(ClientFactory clientFactory, 
			ConfigurationManager configurationManager,
			SimulationManager simulationManager,
			StatusReporter statusReporter,
			LogManager logManager){
		this.clientFactory = clientFactory;
		this.configurationManager = configurationManager;
		this.simulationManager = simulationManager;
		this.statusReporter = statusReporter;
		this.logManager = logManager;
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
	
	@Start
	public void start(){
		
		try{
			LogMessage logMessageObj = createLogMessage();
			
			logMessageObj.setLogMessage("Starting "+this.getClass().getName());
			logManager.log(logMessageObj,GridAppsDConstants.username, GridAppsDConstants.topic_platformLog);
			
			Credentials credentials = new UsernamePasswordCredentials(
					GridAppsDConstants.username, GridAppsDConstants.password);
			Client client = clientFactory.create(PROTOCOL.STOMP,credentials);
			
			
			//TODO: Get Simulation Results from ProcessManager
			
			//TODO: Compare Results
			
			//TODO: 
			
			// Is called directly from process manager Remove on message
			
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
					
					reqTest = RequestTest.parse(message.toString());
					
					testScript = loadTestScript(reqTest.getTestScriptPath());
					
					testConfig = loadTestConfig(reqTest.getTestConfigPath());
					
					expectedResultSeriesPath = reqTest.getExpectedResult();
					
					simulationID = reqTest.getSimulationID();
					
					rulePort = reqTest.getRulePort();
					
					topic = reqTest.getTopic();
					
					testMode=true;
					
					DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
					df.setTimeZone(TimeZone.getTimeZone("UTC"));
					
					String startDateStr= df.format(testConfig.getRun_start());
							
					String endDateStr = df.format(testConfig.getRun_end());
			
//				    System.out.println(startDateStr); 
//				    System.out.println(endDateStr);
				    
					Process rulesProcess = null;
					try {
						
						File defaultLogDir = new File(reqTest.getTestScriptPath()).getParentFile();

	//					String RULE_APP_PATH = GridAppsDConstants.RULE_APP_PATH;
//						String RULE_APP_PATH = "/Users/jsimpson/git/adms-business-rules/durable_rules/";
						String RULE_APP_PATH = defaultLogDir.getAbsolutePath();

//						String appRuleName = "app_rules.py";
						String appRuleName = testScript.getRules().get(0).name;
						
						logManager.log(new LogMessage(this.getClass().getSimpleName(),
								Integer.toString(simulationID), 
								new Date().getTime(), 
								"Calling "+"python "+RULE_APP_PATH+" "+simulationID,
								LogLevel.INFO, 
								ProcessStatus.RUNNING, 
								true),GridAppsDConstants.username, GridAppsDConstants.topic_platformLog);

						ProcessBuilder ruleAppBuilder = new ProcessBuilder("python", RULE_APP_PATH+File.separator+appRuleName,"-t","input","-p",""+rulePort,"--id", ""+simulationID);
						ruleAppBuilder.redirectErrorStream(true);
						ruleAppBuilder.redirectOutput(new File(defaultLogDir.getAbsolutePath()+File.separator+"rule_app.log"));

						rulesProcess = ruleAppBuilder.start();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					// Watch the process
					watch(rulesProcess, "Rules Application");
				}

			});
			
//		client.subscribe(GridAppsDConstants.topic_FNCS_output, new GossResponseEvent() {
		client.subscribe(GridAppsDConstants.topic_simulationLog + simulationID, new GossResponseEvent(){
				
			public void onMessage(Serializable message) {
				DataResponse event = (DataResponse)message;
				logMessageObj.setTimestamp(new Date().getTime());
				logMessageObj.setLogMessage("Recevied message: "+ event.getData() +" on topic "+event.getDestination());
				
				CompareResults compareResults = new CompareResults();
				JsonObject jsonObject = compareResults.getSimulationJson(message.toString()); 
				forwardFNCSOutput(jsonObject,rulePort,topic);
//				logManager.log(logMessageObj, GridAppsDConstants.username);
				
				String path = "/home/gridappsd/gridappsd_project/sources/GOSS-GridAPPS-D/gov.pnnl.goss.gridappsd/test/gov/pnnl/goss/gridappsd/sim_output_object.json";
//				String sim_output = "/home/gridappsd/gridappsd_project/sources/GOSS-GridAPPS-D/gov.pnnl.goss.gridappsd/test/gov/pnnl/goss/gridappsd/sim_output.json";
				String expected_output = "/home/gridappsd/gridappsd_project/sources/GOSS-GridAPPS-D/gov.pnnl.goss.gridappsd/test/gov/pnnl/goss/gridappsd/expected_output.json";
				String expected_output_series = "/home/gridappsd/gridappsd_project/sources/GOSS-GridAPPS-D/gov.pnnl.goss.gridappsd/test/gov/pnnl/goss/gridappsd/expected_output_series3.json";

				if(testMode && message != null){
					expected_output_series = expectedResultSeriesPath;
				}else{
					return;
				}
				
				logMessageObj.setTimestamp(new Date().getTime());
				logMessageObj.setLogMessage("TestManager fncs :  "+ expectedResultSeriesPath + message.toString());
				logManager.log(logMessageObj,  GridAppsDConstants.username, GridAppsDConstants.topic_platformLog);
				
//				{"output": null, "command": "isInitialized", "response": "False"}
				if( jsonObject.get("output").isJsonNull() || jsonObject.get("output") == null){
					logMessageObj.setTimestamp(new Date().getTime());
					logMessageObj.setLogMessage("TestManager fncs : null output " + jsonObject.get("output").toString());
					logManager.log(logMessageObj,GridAppsDConstants.username, GridAppsDConstants.topic_platformLog);
					return;	
				}
				
//				gridappsd_project/builds/log/karaf.log.1:TestManager fncs : not outputtruefalse{"ieee8500":{"cap_capbank0a":{"capacitor_A":400000.0,"control":"MANUAL","control_level":"BANK","dwell_time":100.0,"phases":"AN","phases_connected":"NA","pt_phase":"A","switchA":"CLOSED"},"cap_capbank0b":{"capacitor_B":400000.0,"control":"MANUAL","control_level":"BANK","dwell_time":101.0,"phases":"BN","phases_connected":"NB","pt_phase":"B","switchB":"CLOSED"},"cap_capbank0c":{"capacit
//				if( ! jsonObject.get("output").isJsonObject()){
//				JsonParser parser = new JsonParser();
//				JsonElement simOutputObject = parser.parse(jsonObject.get("output").getAsString());
//				logMessageObj.setTimestamp(new Date().getTime());
//				logMessageObj.setLog_message("TestManager fncs : not output" + simOutputObject.isJsonObject() + simOutputObject.isJsonPrimitive() +simOutputObject.getAsJsonObject().get("ieee8500"));
//				logManager.log(logMessageObj);
//				simOutputObject.getAsJsonObject().get("cap_capbank0a");				
//			}
			// The output is a string not s JSON object
				
				JsonParser parser = new JsonParser();
				JsonElement simOutputObject = parser.parse(jsonObject.get("output").getAsString());

				SimulationOutput simOutProperties = compareResults.getOutputProperties(path);
				compareResults.getProp(simOutProperties);
//				TestResults tr = compareResults.compareExpectedWithSimulation(sim_output, expected_output, simOutProperties);
			
				Map<String, JsonElement> expectedOutputMap = compareResults.getExpectedOutputMap(expected_output);
				Map<String, List<String>> propMap = simOutProperties.getOutputObjects().stream()
						.collect(Collectors.toMap(SimulationOutputObject::getName, e -> e.getProperties()));
				
				//Temp timeseries index
				String indexStr = tempIndex + "";
				tempIndex++;
//				TestResults tr = compareResults.compareExpectedWithSimulationOutput(expectedOutputMap, propMap,simOutputObject.getAsJsonObject());
				TestResults tr = compareResults.compareExpectedWithSimulationOutput(indexStr,simOutputObject.getAsJsonObject(),expected_output_series, simOutProperties);
				testResultSeries.add(indexStr, tr);
//				int count2 = compareResults.compareExpectedWithSimulationOutput(indexStr,simOutputObject.getAsJsonObject(),expected_output_series, simOutProperties).getNumberOfConflicts();
				
				logMessageObj.setTimestamp(new Date().getTime());
				logMessageObj.setLogMessage("Index: " + indexStr +" TestManager number of conflicts: "+ tr.getNumberOfConflicts() + " total "+ testResultSeries.getTotal());
				logManager.log(logMessageObj,GridAppsDConstants.username,GridAppsDConstants.topic_platformLog);
				
			}


			});
		}
		catch(Exception e){
			log.error("Error in test manager",e);
		}	
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
			System.out.println(testConfig.toString());
				
//			jsonReader.beginObject();
//			while (jsonReader.hasNext()) {
//
//				String name = jsonReader.nextName();
//				System.out.println(name);
//				System.out.println(jsonReader.nextString());
//				if (name.equals("test_configuration")) {
//	//				readApp(jsonReader);
//	
//				}
//			}
//			jsonReader.endObject();
			jsonReader.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
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
		TestManagerQueryFactory qf = new TestManagerQueryFactory();
		qf.getFeeder();
		PowerSystemConfig powerSystemConfig = new PowerSystemConfig();
		powerSystemConfig.GeographicalRegion_name = qf.getGeographicalRegion(); // "ieee8500_Region";
		powerSystemConfig.SubGeographicalRegion_name =  qf.getSubGeographicalRegion(); // "ieee8500_SubRegion";
		powerSystemConfig.Line_name = qf.getFeeder(); // "ieee8500";

		SimulationConfig simulationConfig = new SimulationConfig();
		simulationConfig.duration = 60;
		simulationConfig.power_flow_solver_method = "";
		simulationConfig.simulation_id = ""; //.setSimulation_name("");
		simulationConfig.simulator = ""; //.setSimulator("");

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		simulationConfig.start_time = sdf.format(new Date()); //.setStart_time("");

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
		statusReporter.reportStatus(String.format("Got new message in %s", getClass().getName()));
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
						statusReporter.reportStatus(GridAppsDConstants.topic_simulationLog+testId, "Test Initialization error: "+e.getMessage());
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
		TestManagerQueryFactory qf =  new TestManagerQueryFactory();
		qf.getFeeder();
	}
}