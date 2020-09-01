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
package gov.pnnl.goss.gridappsd.process;

import java.util.Date;
import java.util.Hashtable;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;

import gov.pnnl.goss.gridappsd.api.AppManager;
import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.ProcessManager;
import gov.pnnl.goss.gridappsd.api.RoleManager;
import gov.pnnl.goss.gridappsd.api.ServiceManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.api.TestManager;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.security.SecurityConfig;



/**
 * Process Manager subscribe to all the requests coming from Applications
 * and forward them to appropriate managers.
 * @author shar064
 *
 */
@Component
public class ProcessManagerImpl implements ProcessManager {

	@ServiceDependency
	private volatile ClientFactory clientFactory;

	@ServiceDependency
	private volatile ConfigurationManager configurationManager;

	@ServiceDependency
	private volatile SimulationManager simulationManager;

	@ServiceDependency
	private volatile AppManager appManager;

	@ServiceDependency
	private volatile LogManager logManager;

	@ServiceDependency
	private volatile ServiceManager serviceManager;
	
	@ServiceDependency
    private volatile SecurityConfig securityConfig;

	@ServiceDependency
	private volatile DataManager dataManager;

	@ServiceDependency
	private volatile TestManager testManager;
	
	@ServiceDependency
	private volatile RoleManager roleManager;

	ProcessNewSimulationRequest newSimulationProcess = null;

	private Hashtable<Integer, AtomicInteger> simulationPorts = new Hashtable<Integer, AtomicInteger>();

	private Random randPort = new Random();

	public ProcessManagerImpl(){}
	public ProcessManagerImpl(ClientFactory clientFactory,
			ConfigurationManager configurationManager,
			SimulationManager simulationManager,
			LogManager logManager,
			AppManager appManager,
			ProcessNewSimulationRequest newSimulationProcess,
			TestManager testManager){
		this.clientFactory = clientFactory;
		this.configurationManager = configurationManager;
		this.simulationManager = simulationManager;
		this.appManager = appManager;
		this.newSimulationProcess = newSimulationProcess;
		this.logManager = logManager;
		this.testManager = testManager;
	}



	@Start
	public void start(){

		LogMessage logMessageObj = new LogMessage();

		try{
			 
			
				Credentials credentials = new UsernamePasswordCredentials(
						securityConfig.getManagerUser(), securityConfig.getManagerPassword());

//			Credentials credentials = new UsernamePasswordCredentials(
//					GridAppsDConstants.username, GridAppsDConstants.password);
			Client client = clientFactory.create(PROTOCOL.STOMP,credentials);

			logMessageObj.setLogLevel(LogLevel.DEBUG);
			logMessageObj.setSource(this.getClass().getName());
			logMessageObj.setProcessStatus(ProcessStatus.RUNNING);
			logMessageObj.setStoreToDb(true);
			logMessageObj.setLogMessage("Starting "+ this.getClass().getName());

			// Use client publish so the listeners other than the platform can get the
			// message (i.e. the viz)
			client.publish(GridAppsDConstants.topic_platformLog, logMessageObj);

			if(newSimulationProcess==null)
				newSimulationProcess = new ProcessNewSimulationRequest(this.logManager);

			logMessageObj.setTimestamp(new Date().getTime());
			logMessageObj.setLogMessage("Starting "+this.getClass().getName());
			client.publish(GridAppsDConstants.topic_platformLog, logMessageObj);


			client.subscribe(GridAppsDConstants.topic_process_prefix+".>", new ProcessEvent(this,
					client, newSimulationProcess, configurationManager, simulationManager, appManager, logManager, serviceManager, dataManager, testManager, roleManager));
		}
		catch(Exception e){
			e.printStackTrace();
			logManager.error(ProcessStatus.ERROR, null, e.getMessage());
		}

	}

	/**
	 * Generates and returns process id
	 * @return process id
	 */
	static String generateProcessId(){
		return Integer.toString(Math.abs(new Random().nextInt()));
	}

	public int assignSimulationPort(String simulationId) throws Exception {
		Integer simIdKey = new Integer(simulationId);
		if (!simulationPorts.containsKey(simIdKey)) {
			int tempPort = 49152 + randPort.nextInt(16384);
			AtomicInteger tempPortObj = new AtomicInteger(tempPort);
			while (simulationPorts.containsValue(tempPortObj)) {
				int newTempPort = 49152 + randPort.nextInt(16384);
				tempPortObj.set(newTempPort);
			}
			simulationPorts.put(simIdKey, tempPortObj);
			return tempPortObj.get();
			//TODO: test host:port is available
		} else {
			throw new Exception("The simulation id already exists. This indicates that the simulation id is part of a"
					+ "simulation in progress.");
		}
	}

}
