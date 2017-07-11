/*******************************************************************************
 * Copyright © 2017, Battelle Memorial Institute All rights reserved.
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
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS “AS IS” AND ANY 
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
package gov.pnnl.goss.gridappsd;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import gov.pnnl.goss.gridappsd.dto.PowerSystemConfig;
import gov.pnnl.goss.gridappsd.dto.RequestSimulation;
import gov.pnnl.goss.gridappsd.dto.SimulationConfig;
import junit.framework.TestCase;
import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.core.Request.RESPONSE_FORMAT;
import pnnl.goss.core.client.ClientServiceFactory;
import pnnl.goss.gridappsd.utils.GridAppsDConstants;

public class TestApplication extends TestCase {

	private static Logger log = LoggerFactory.getLogger(TestApplication.class);
	
	ClientFactory clientFactory = new ClientServiceFactory();
	
	Client client;
	
	public void testApplication() {

		try {
			
			//Step1: Create GOSS Client
			Credentials credentials = new UsernamePasswordCredentials(
					GridAppsDConstants.username, GridAppsDConstants.password);
			client = clientFactory.create(PROTOCOL.STOMP, credentials);

			//Create Request Simulation object
			PowerSystemConfig powerSystemConfig = new PowerSystemConfig();
			powerSystemConfig.GeographicalRegion_name = "ieee8500_Region";
			powerSystemConfig.SubGeographicalRegion_name = "ieee8500_SubRegion";
			powerSystemConfig.Line_name = "ieee8500";
			
			SimulationConfig simulationConfig = new SimulationConfig();
			simulationConfig.duration = 60; //.setDuration("");
			// TODO: Should this be an array?
			//simulationConfig.output_object_mrid = ""; //.setOutput_object_mrid(null);
			simulationConfig.power_flow_solver_method = ""; //.setPower_flow_solver_method("");
			simulationConfig.simulation_id = ""; //.setSimulation_name("");
			simulationConfig.simulator = ""; //.setSimulator("");
			// TODO: Should this be an array?
			//simulationConfig.simulator_name  ""; //.setSimulator_name(null);
			
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			simulationConfig.start_time = sdf.format(new Date()); //.setStart_time("");
			
			RequestSimulation requestSimulation = new RequestSimulation(powerSystemConfig, simulationConfig);
			
			Gson  gson = new Gson();
			String request = gson.toJson(requestSimulation); 
			request = "{\"power_system_config\":{\"GeographicalRegion_name\":\"ieee13nodecktassets_Region\",\"SubGeographicalRegion_name\":\"ieee13nodecktassets_SubRegion\",\"Line_name\":\"ieee13nodecktassets\"}, "+
					    "\"simulation_config\":{\"start_time\":\"03/07/2017 00:00:00\",\"duration\":\"60\",\"simulator\":\"GridLAB-D\",\"simulation_name\":\"my test simulation\",\"power_flow_solver_method\":\"FBS\"}}";
			String simulationId = client.getResponse(request, GridAppsDConstants.topic_requestSimulation, RESPONSE_FORMAT.JSON).toString();
			assertNotNull(simulationId);
			log.debug("REceived simulation id  = "+simulationId);
			
			client.subscribe(GridAppsDConstants.topic_simulationOutput+simulationId, new GossResponseEvent() {
				
				@Override
				public void onMessage(Serializable response) {
					System.out.println("simulation output is: "+response);
					
				}
			});
			
			client.subscribe(GridAppsDConstants.topic_simulationStatus+simulationId, new GossResponseEvent() {
				
				@Override
				public void onMessage(Serializable response) {
					System.out.println("simulation status is: "+response);
					
				}
			});
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	
}
