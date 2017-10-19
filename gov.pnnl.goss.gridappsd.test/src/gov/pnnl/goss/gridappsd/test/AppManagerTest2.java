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
package gov.pnnl.goss.gridappsd.test;


import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;

import org.amdatu.testing.configurator.TestConfiguration;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.Gson;

import gov.pnnl.goss.gridappsd.dto.AppInfo;
import gov.pnnl.goss.gridappsd.dto.AppInfo.AppType;
import gov.pnnl.goss.gridappsd.dto.RequestAppRegister;
import gov.pnnl.goss.gridappsd.dto.RequestAppStart;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.Request.RESPONSE_FORMAT;
import pnnl.goss.core.client.ClientServiceFactory;


//@RunWith(MockitoJUnitRunner.class)
public class AppManagerTest2 {

//	ClientFactory clientFactory;
	Client client;
	private TestConfiguration testConfig;

	public static final String APPLICATION_OBJECT_CONFIG= "{\\\"static_inputs\\\": {\\\"ieee8500\\\": {\\\"control_method\\\": \\\"ACTIVE\\\",\\\"capacitor_delay\\\": 60,\\\"regulator_delay\\\": 60,\\\"desired_pf\\\": 0.99,\\\"d_max\\\": 0.9,\\\"d_min\\\": 0.1,\\\"substation_link\\\": \\\"xf_hvmv_sub\\\",\\\"regulator_list\\\": [\\\"reg_FEEDER_REG\\\",\\\"reg_VREG2\\\",\\\"reg_VREG3\\\",\\\"reg_VREG4\\\"],\\\"regulator_configuration_list\\\": [\\\"rcon_FEEDER_REG\\\",\\\"rcon_VREG2\\\",\\\"rcon_VREG3\\\",\\\"rcon_VREG4\\\"],\\\"capacitor_list\\\": [\\\"cap_capbank0a\\\",\\\"cap_capbank0b\\\",\\\"cap_capbank0c\\\",\\\"cap_capbank1a\\\",\\\"cap_capbank1b\\\",\\\"cap_capbank1c\\\",\\\"cap_capbank2a\\\",\\\"cap_capbank2b\\\",\\\"cap_capbank2c\\\",\\\"cap_capbank3\\\"],\\\"voltage_measurements\\\": [\\\"nd_l2955047,1\\\",\\\"nd_l3160107,1\\\",\\\"nd_l2673313,2\\\",\\\"nd_l2876814,2\\\",\\\"nd_m1047574,3\\\",\\\"nd_l3254238,4\\\"],\\\"maximum_voltages\\\": 7500,\\\"minimum_voltages\\\": 6500,\\\"max_vdrop\\\": 5200,\\\"high_load_deadband\\\": 100,\\\"desired_voltages\\\": 7000,\\\"low_load_deadband\\\": 100,\\\"pf_phase\\\": \\\"ABC\\\"}}}";
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new AppManagerTest2().test();
		System.exit(0);
	}
	
	
	
	public void test(){

		try {
//			Dictionary properties = new Hashtable();
//			properties.put("goss.system.manager", "system");
//			properties.put("goss.system.manager.password", "manager");
//
//			// The following are used for the core-client connection.
//			properties.put("goss.openwire.uri", "tcp://0.0.0.0:61616");
//			properties.put("goss.stomp.uri", "stomp://0.0.0.0:61613");
//			properties.put("goss.ws.uri", "ws://0.0.0.0:61614");
//			properties.put("goss.ssl.uri", "ssl://0.0.0.0:61443");
//			testConfig = configure(this)
//					.add(CoreGossConfig.configureServerAndClientPropertiesConfig())
//					.add(createServiceDependency().setService(ClientFactory.class));
//			testConfig.apply();
//			ClientServiceFactory clientFactory = new ClientServiceFactory();
//			clientFactory.updated(properties);
			
			//Step1: Create GOSS Client
//			Credentials credentials = new UsernamePasswordCredentials(
//					GridAppsDConstants.username, GridAppsDConstants.password);
//			client = clientFactory.create(PROTOCOL.OPENWIRE, credentials);
			
			//Create Request Simulation object
//			PowerSystemConfig powerSystemConfig = new PowerSystemConfig();
//			powerSystemConfig.GeographicalRegion_name = "ieee8500_Region";
//			powerSystemConfig.SubGeographicalRegion_name = "ieee8500_SubRegion";
//			powerSystemConfig.Line_name = "ieee8500";
			
			
//			Gson  gson = new Gson();
//			String request = gson.toJson(powerSystemConfig); 
//			DataRequest request = new DataRequest();
//			request.setRequestContent(powerSystemConfig);
//			System.out.println(client);
			
			
			registerApp();
			
//			AppInfo
//			String response = client.getResponse("",GridAppsDConstants.topic_requestData, RESPONSE_FORMAT.JSON).toString();
//			
//			//TODO subscribe to response
//			client.subscribe(GridAppsDConstants.topic_simulationOutput+response, new GossResponseEvent() {
//				
//				@Override
//				public void onMessage(Serializable response) {
//					// TODO Auto-generated method stub
//					System.out.println("RESPNOSE "+response);
//				}
//			});
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	
	public void registerApp() throws Exception{
		
		ClientServiceFactory clientFactory = new ClientServiceFactory();
		Dictionary properties = new Hashtable();
		properties.put("goss.system.manager", "system");
		properties.put("goss.system.manager.password", "manager");

		// The following are used for the core-client connection.
		properties.put("goss.openwire.uri", "tcp://0.0.0.0:61616");
		properties.put("goss.stomp.uri", "stomp://0.0.0.0:61613");
		properties.put("goss.ws.uri", "ws://0.0.0.0:61614");
		properties.put("goss.ssl.uri", "ssl://0.0.0.0:61443");
		clientFactory.updated(properties);
		
		//Step1: Create GOSS Client
		Credentials credentials = new UsernamePasswordCredentials(
				GridAppsDConstants.username, GridAppsDConstants.password);
		client = clientFactory.create(PROTOCOL.OPENWIRE, credentials);
		
		
//		StompJmsConnectionFactory connectionFactory = new StompJmsConnectionFactory();
//		connectionFactory.setBrokerURI("tcp://localhost:61613");
//		connectionFactory.setUsername("system");
//		connectionFactory.setPassword("manager");
//		Connection connection = connectionFactory.createConnection(); 
//		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		
		
		AppInfo appInfo = new AppInfo();
		appInfo.setId("vvo");
		appInfo.setCreator("pnnl");
		appInfo.setDescription("VVO app");
		appInfo.setExecution_path("app/vvoapp.py");
		
		List<String> inputs = new ArrayList<String>();
		inputs.add(GridAppsDConstants.topic_FNCS_output);
		appInfo.setInputs(inputs);
		
		List<String> outputs = new ArrayList<String>();
		outputs.add(GridAppsDConstants.topic_FNCS_input);
		appInfo.setOutputs(outputs);
		
		appInfo.setLaunch_on_startup(false);
		appInfo.setMultiple_instances(true);
		appInfo.setOptions("SIMULATION_ID");
		List<String> prereqs = new ArrayList<String>();
		prereqs.add("fncs-goss-bridge");
		appInfo.setPrereqs(prereqs);
		appInfo.setType(AppType.PYTHON);
		
		System.out.println(appInfo);
		
		
		File parentDir = new File(".");
		File f = new File(parentDir.getAbsolutePath()+File.separator+"resources"+File.separator+"vvo.zip");
		System.out.println(f.getAbsolutePath());
		byte[] fileData = Files.readAllBytes(f.toPath());

		RequestAppRegister appRegister = new RequestAppRegister(appInfo,fileData);
		System.out.println("REGISTER"+appRegister);

//		DataRequest request = new DataRequest();
//		request.setRequestContent(appRegister);
//		client.publish(GridAppsDConstants.topic_requestSimulation, appRegister);
		Serializable replyDest = sendMessage(GridAppsDConstants.topic_app_register, appRegister, client);
		
//		String response = client.getResponse(request,GridAppsDConstants.topic_app_register, RESPONSE_FORMAT.JSON).toString();
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String runtimeOptions = "-c \""+APPLICATION_OBJECT_CONFIG+"\"";

		String simulationId = "12345";
		RequestAppStart appStart = new RequestAppStart(appInfo.getId(), runtimeOptions, simulationId);
		replyDest = sendMessage(GridAppsDConstants.topic_app_start, appStart, client);
		System.out.println(appStart);
//		Serializable appInstanceId = recieveMessage(replyDest, client);
//		System.out.println("APP INSTANCE ID "+appInstanceId);
		
		try {
			Thread.sleep(15000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
//		 replyDest = sendMessage(GridAppsDConstants.topic_app_deregister, appInfo.getId(), session);
//		System.out.println("RESPONSE "+response);
		
		
		
	}
	
	
	private Serializable sendMessage(String destination, Serializable message, Client client) throws JMSException{
		Gson gson = new Gson();
		
			
		return client.getResponse(message, destination, RESPONSE_FORMAT.JSON);
//		MessageProducer producer = session.createProducer(new StompJmsDestination(destination));
//		TextMessage textMessage = null;
//		if(message instanceof String){
//			textMessage = session.createTextMessage(message.toString());
//		} else {
//			textMessage = session.createTextMessage(gson.toJson(message));
//			
//		}
//		Destination tmpDest = session.createTemporaryQueue();
//		System.out.println("SENDING MESSAGE WITH REPLY "+tmpDest);
//
//		textMessage.setJMSReplyTo(tmpDest);
//		producer.send(textMessage);
//		return tmpDest;
	}
	
	
	private Object recieveMessage(Destination destination, Session session) throws JMSException{
		System.out.println("RECEIVING MESSAGE ON "+destination);
		MessageConsumer consumer = session.createConsumer(destination);
		Message msg = consumer.receive(5000);
		System.out.println(msg);
		
		return msg;
	}
	
}
