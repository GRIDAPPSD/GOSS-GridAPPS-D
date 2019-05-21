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

import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.TestManager;
import gov.pnnl.goss.gridappsd.dto.RequestTestUpdate;
import gov.pnnl.goss.gridappsd.dto.RequestTestUpdate.RequestType;
import gov.pnnl.goss.gridappsd.dto.RuleSettings;
import gov.pnnl.goss.gridappsd.dto.SimulationContext;
import gov.pnnl.goss.gridappsd.dto.TestConfig;
import gov.pnnl.goss.gridappsd.dto.events.Event;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

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

import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.GossResponseEvent;

import com.google.gson.JsonObject;



/**
 *
 * @author jsimpson
 *
 */
@Component
public class TestManagerImpl implements TestManager {
	

	@ServiceDependency
	private volatile ClientFactory clientFactory;
	
	@ServiceDependency
	private volatile LogManager logManager;

	@ServiceDependency
	private volatile DataManager dataManger;
	
	private Hashtable<String, AtomicInteger> rulePorts = new Hashtable<String, AtomicInteger>();
	
	private Random randPort = new Random();
	
	private enum EventStatus {
	    SCHEDULED, INITIATED, CLEARED, CANCELLED
	}
	
	private Map<String,List<Event>> TestContext = new HashMap<String, List<Event>>();
	private Map<String,EventStatus> EventStatus = new HashMap<String, EventStatus>();
	private HashMap<String, ProcessEvents> processEventsMap = new HashMap<String, ProcessEvents>(10);
	

	Client client;
	
	String testOutputTopic = GridAppsDConstants.topic_simulationTestOutput;

	public TestManagerImpl(){}
	public TestManagerImpl(ClientFactory clientFactory, 
			LogManager logManager,
			DataManager dataManager){
		this.clientFactory = clientFactory;
		this.logManager = logManager;
		this.dataManger = dataManager;
	}

	
	@Start
	public void start() {

		try {

			// Log - "Starting "+this.getClass().getName());
			Credentials credentials = new UsernamePasswordCredentials(
					GridAppsDConstants.username, GridAppsDConstants.password);
			client = clientFactory.create(PROTOCOL.STOMP, credentials);
			
			client.subscribe(GridAppsDConstants.topic_simulationTestInput+".>", new GossResponseEvent() {
				
				@Override
				public void onMessage(Serializable message) {
					
					DataResponse request;
					if (message instanceof DataResponse){
						request = (DataResponse)message;
						String topic = request.getDestination();
						String simulationId = topic.substring(topic.lastIndexOf("."), topic.length());
						
						if(request.getData() instanceof RequestTestUpdate){
							
							RequestTestUpdate requestTestUpdate = RequestTestUpdate.parse(request.getData().toString());
							
							if(requestTestUpdate.getCommand() == RequestType.new_events){
								sendEventsToSimulation(requestTestUpdate.getEvents(), simulationId);
							}
							else if(requestTestUpdate.getCommand() == RequestType.update_events){
								updateEventForSimulation(requestTestUpdate.getEvents(), simulationId);
							}
							else if(requestTestUpdate.getCommand() == RequestType.query_events){
								sendEventStatus(simulationId, request.getDestination());
							}
						}
					}
				}
			});

		} catch (Exception e) {
			//TODO-log.error("Error in test manager", e);
		}
	}
	
	public void handleTestRequest(TestConfig testConfig, SimulationContext simulationContext) {
		
		//Not testing this simulation
		if(testConfig == null){
			return;
		}
		String simulationId = simulationContext.getSimulationId();
		String simulationDir = simulationContext.getSimulationDir();

		if (testConfig.getEvents() != null && testConfig.getEvents().size() > 0) {
			sendEventsToSimulation(testConfig.getEvents(), simulationId);
		}

		if (testConfig.getCompareWithSimId() != null) {
			compareSimulations(simulationId, testConfig.getCompareWithSimId());
		}
		
		if(testConfig.getExpectedResultObject() != null){
			compareWithExpectedSimOutput(simulationId, testConfig.getExpectedResultObject());
		}

		if (testConfig.getRules() != null && testConfig.getRules().size() > 0) {
			comapareSimOutputWithAppRules(simulationId, simulationDir, testConfig.getAppId(), testConfig.getRules());
		}

	}
	
	@Override
	public void sendEventsToSimulation(List<Event> events, String simulationId){
		//TODO: Add simulation and events in TestContext Map variable 
		//TODO : Update events status in EventStatus Map variable 
//		ProcessEvents pe = new ProcessEvents(logManager, events);
//		pe.processEvents(client, simulationId);
//		EventCommand eventCommand = EventCommand.parse(dataStr);	
		ProcessEvents pe = getProcessEvents(client, simulationId);
		pe.addEvents(events);
		
//		if(eventCommand.command.equalsIgnoreCase("CommEvent")){
//			pe.addEventCommandMessage(eventCommand);
//		}	
	}
	
	private ProcessEvents getProcessEvents(Client client, String simulationId) {
		ProcessEvents pe;
		if(! processEventsMap.containsKey(simulationId) ){
			pe = processEventsMap.getOrDefault(simulationId, new ProcessEvents(logManager, client, simulationId));
			processEventsMap.putIfAbsent(simulationId, pe);
	    }
		pe = processEventsMap.get(simulationId);
		return pe;
	}
	
	@Override
	public void updateEventForSimulation(List<Event> events, String simulationId){
		//TODO : Check and Update events status in EventStatus Map variable 
	}

	@Override
	public void sendEventStatus(String simulationId, String replyDestination){
		//TODO: Get events for simulationId from TestContext Map variable 
		//TODO : Check and Send events status from EventStatus Map variable 
	}
	
	@Override
	public void compareSimulations(String simulationIdOne, String simulationIdTwo){
		
		HistoricalComparison hc = new HistoricalComparison(dataManger);
		//TODO: Remove expected results from this method
		TestResultSeries testResultsSeries = null; //hc.test_proven(simulationIdTwo, expectedResultObject);
		client.publish(testOutputTopic+simulationIdOne, testResultsSeries);
		for (String key : testResultsSeries.results.keySet()) {
			client.publish(testOutputTopic+simulationIdOne, "Index: " + key + " TestManager number of conflicts: "
					+ " total " + testResultsSeries.getTotal());
		}
	}
	
	@Override
	public void compareWithExpectedSimOutput(String simulationId, JsonObject expectedResults) {
		client.subscribe("/topic/" + GridAppsDConstants.topic_simulationOutput + "." + simulationId,

		new GossResponseEvent() {
			public void onMessage(Serializable message) {
				
				DataResponse event = (DataResponse) message;
				String simOutputStr = event.getData().toString();
//				if (simOutputStr.length() >= 200)
//					simOutputStr = simOutputStr.substring(0, 200);
				//TODO: Log debug - "TestManager received message: " + simOutput + " on topic " + event.getDestination()
				
				CompareResults compareResults = new CompareResults();
				JsonObject simOutputJsonObj = CompareResults.getSimulationJson(simOutputStr);

				if ( ! simOutputJsonObj.has("message")) {
					//TODO: Log error - "TestManager received empty message key in simulation output"
					return;
				}
				
				String simulationTimestamp = simOutputJsonObj.getAsJsonObject().get("message").getAsJsonObject().get("timestamp").getAsString();
				TestResults testResults = compareResults.compareExpectedWithSimulationOutput(simulationTimestamp,
						simOutputJsonObj, expectedResults);
				if (testResults != null) {
					client.publish(testOutputTopic+simulationId, testResults);
					//TODO: Store results in timeseries store.
				}
								
			}

		});
	}
	
	//TODO: cross check this port with port used for FNCS
	private int assignTestPort(String simulationId) throws Exception {
		if (!rulePorts.containsKey(simulationId)) {
			int tempPort = 49152 + randPort.nextInt(16384);
			AtomicInteger tempPortObj = new AtomicInteger(tempPort);
			while (rulePorts.containsValue(tempPortObj)) {
				int newTempPort = 49152 + randPort.nextInt(16384);
				tempPortObj.set(newTempPort);
			}
			rulePorts.put(simulationId, tempPortObj);
			return tempPortObj.get();
			//TODO: test host:port is available
		} else {
			throw new Exception("RulePort already assigned to the simulation id : "+simulationId);
		}
	}

	private void forwardSimInputToRuleEngine(Client client, String simulationID, int rulePort) {
		client.subscribe("/topic/" + GridAppsDConstants.topic_simulationInput +"."+ simulationID, new GossResponseEvent(){
			public void onMessage(Serializable message) {
				JsonObject jsonObject = CompareResults.getSimulationJson(message.toString()); 
				jsonObject = CompareResults.getSimulationJson(jsonObject.get("data").getAsString());
				JsonObject forwardObject = jsonObject.get("input").getAsJsonObject();
				forwardSimOutputToRuleEngine(forwardObject,rulePort,"output","localhost");
			}
		});
	}
	
	private void forwardSimOutputToRuleEngine(JsonObject jsonObject, int port, String topic, String host) {
		CloseableHttpClient httpClient = HttpClientBuilder.create().build();
		try {
			
		    HttpPost request = new HttpPost("http://"+host+":"+port+"/"+topic+"/events");
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
		    //TODO: Log error - handle exception here
		} finally {
		    try {
				httpClient.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	/**
	 * This method comapred simulation output with more complex rules 
	 * using a rule engine. This is still in test mode will be available
	 * in later release. 
	 */
	public void comapareSimOutputWithAppRules(String simulationId, String simulationDir, String appId, List<RuleSettings> rules) {
		
		
		
		Process rulesProcess=null;
			try {
				int rulePort = assignTestPort(simulationId);
				String appRuleName = rules.get(0).name;
				//TODO: Log info - "Calling python "+appDirectory+File.separator+appRuleName+" "+simulationID
				ProcessBuilder ruleAppBuilder = new ProcessBuilder("python", simulationDir+File.separator+appRuleName,"-t","input","-p",""+rulePort,"--id", ""+simulationId);
				ruleAppBuilder.redirectErrorStream(true);
				ruleAppBuilder.redirectOutput(new File(simulationDir+File.separator+"rule_app.log"));
				rulesProcess = ruleAppBuilder.start();
				//TODO: Add this ruleProcess to simulationContext so it can be stopped when simulation stops 
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if ( ! rulesProcess.isAlive()){
					//TODO: log error - "Process " + appDirectory+File.separator+appRuleName+" " +"did not start check rule script and that redis is running."
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			// Watch the process
			if(rulesProcess!=null)
				watch(rulesProcess, "RulesApp"+simulationId);
	}
	
	private void watch(final Process process, String processName) {
		new Thread() {
	        public void run() {
	            BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
	            String line = null;
	            try {
	                while ((line = input.readLine()) != null) {
	                	//TODO: debug - processName+":"+line
	                }
	            } catch (IOException e) {
	            	//TODO:log error - "Error on process "+ processName, e
	            }
	        }
	    }.start();
	}
	
}
