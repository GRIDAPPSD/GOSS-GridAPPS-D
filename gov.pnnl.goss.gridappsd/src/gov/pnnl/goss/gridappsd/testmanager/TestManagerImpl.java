/*******************************************************************************
 * Copyright � 2017, Battelle Memorial Institute All rights reserved.
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
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS �AS IS� AND ANY 
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

import java.io.File;
import java.io.FileReader;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.jms.JMSException;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.ProcessManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.api.StatusReporter;
import gov.pnnl.goss.gridappsd.api.TestConfiguration;
import gov.pnnl.goss.gridappsd.api.TestManager;
import gov.pnnl.goss.gridappsd.api.TestScript;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.PowerSystemConfig;
import gov.pnnl.goss.gridappsd.dto.RequestSimulation;
import gov.pnnl.goss.gridappsd.dto.SimulationConfig;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
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
	

	public static final String topic_requestTest = GridAppsDConstants.topic_process_prefix+".test";
	
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
	
	@Start
	public void start(){
		LogMessage logMessageObj = new LogMessage();
		try{
			logMessageObj.setLog_level(LogLevel.DEBUG);
			logMessageObj.setProcess_id(this.getClass().getName());
			logMessageObj.setProcess_status(ProcessStatus.RUNNING);
			logMessageObj.setStoreToDB(true);
			
			logMessageObj.setTimestamp(new Date().getTime());
			logMessageObj.setLog_message("Starting "+this.getClass().getName());
			logManager.log(logMessageObj);
			
			Credentials credentials = new UsernamePasswordCredentials(
					GridAppsDConstants.username, GridAppsDConstants.password);
			Client client = clientFactory.create(PROTOCOL.STOMP,credentials);
			
//			String path = "/Users/jsimpson/git/adms/GOSS-GridAPPS-D/gov.pnnl.goss.gridappsd/applications/python/exampleTestConfig.json";
//			TestConfiguration testConf = loadTestConfig(path);
//			path = "/Users/jsimpson/git/adms/GOSS-GridAPPS-D/gov.pnnl.goss.gridappsd/applications/python/exampleTestScript.json";
//			TestScript testScript = loadTestScript(path);
//			
//			requestSimulation(client, testConf, testScript);
			
//			TestConfigurationImpl tc = null;
//			
//			TestScriptImpl ts = null;
//			
			//TODO: Setup Figure out location of TestScripts
			
//			requestSimulation(client, tc, ts);
			
			//TODO: Collect data from data manager
				// TODO Build queries
			
			//TODO: Get Simulation Results from ProcessManager
			
			//TODO: Compare Results
			
			//TODO: 
			
			// Is called directly from process manager Remove on message
			
			//TODO: subscribe to GridAppsDConstants.topic_request_prefix+/* instead of GridAppsDConstants.topic_requestSimulation
			client.subscribe(topic_requestTest, new GossResponseEvent() {
				
				@Override
				public void onMessage(Serializable message) {
					log.debug("Test manager received message ");
					DataResponse event = (DataResponse)message;
					
					statusReporter.reportStatus(String.format("Got new message in %s", getClass().getName()));
					//TODO: create registry mapping between request topics and request handlers.
					switch(event.getDestination().replace("/queue/", "")){
						case topic_requestTest : {
							log.debug("Received test request: "+ event.getData());
							
							//generate simulation id and reply to event's reply destination.
//							int simulationId = generateSimulationId();
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
								simulationManager.startSimulation(testId, simulationFile, config.getSimulation_config());
								log.debug("Starting simulation for id "+ testId);
									
		//								new ProcessSimulationRequest().process(event, client, configurationManager, simulationManager); break;
							}catch (Exception e){
								e.printStackTrace();
								try {
									statusReporter.reportStatus(GridAppsDConstants.topic_simulationStatus+testId, "Test Initialization error: "+e.getMessage());
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
			});
		}
		catch(Exception e){
			log.error("Error in process manager",e);
		}
		
	}
	
	
	public TestConfigurationImpl loadTestConfig(String path){
//		path = "/Users/jsimpson/git/adms/GOSS-GridAPPS-D/gov.pnnl.goss.gridappsd/applications/python/exampleTestConfig2.json";
//		Gson  gson = new Gson().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").create();
		Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
		JsonReader jsonReader;
		TestConfigurationImpl testConfig = null;
		try {
			jsonReader = new JsonReader(new FileReader(path));
			jsonReader.setLenient(true);
			testConfig = gson.fromJson(new FileReader(path),TestConfigurationImpl.class);
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
			e.printStackTrace();
		}
		return testConfig;
	}
	
	public void compare(){
		JsonParser parser = new JsonParser();
		JsonElement o1 = parser.parse("{a : {a : 2}, b : 2}");
		JsonElement o2 = parser.parse("{b : 3, a : {a : 2}}");
		JsonElement o3 = parser.parse("{b : 2, a : {a : 2}}");
		System.out.println(o1.equals(o2));
		System.out.println(o1.equals(o3));
//		Assert.assertEquals(o1, o2);
	}
	
	public TestScriptImpl loadTestScript(String path){
		Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
		JsonReader jsonReader;
		TestScriptImpl testScript = null;
		try {
			jsonReader = new JsonReader(new FileReader(path));
			jsonReader.setLenient(true);
			testScript = gson.fromJson(new FileReader(path), TestScriptImpl.class);
			System.out.println(testScript.toString());
			jsonReader.close();
		} catch (Exception e) {
			e.printStackTrace();
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
	
	
	public static void main(String[] args) {
		TestManagerImpl tm = new TestManagerImpl();
		tm.compare();
		String path = "/Users/jsimpson/git/adms/GOSS-GridAPPS-D/gov.pnnl.goss.gridappsd/applications/python/exampleTestConfig.json";
		TestConfiguration testConf = tm.loadTestConfig(path);
		path = "/Users/jsimpson/git/adms/GOSS-GridAPPS-D/gov.pnnl.goss.gridappsd/applications/python/exampleTestScript.json";
		TestScript testScript = tm.loadTestScript(path);
		
//		Credentials credentials = new UsernamePasswordCredentials(
//				GridAppsDConstants.username, GridAppsDConstants.password);
//		Client client = clientFactory.create(PROTOCOL.STOMP,credentials);
//		
//		
//		requestSimulation(client, testConf, testScript);
//		
	}
}
