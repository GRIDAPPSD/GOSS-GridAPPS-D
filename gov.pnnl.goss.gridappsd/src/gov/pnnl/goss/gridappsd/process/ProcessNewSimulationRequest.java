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

import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.api.StatusReporter;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.RequestSimulation;

import java.io.File;
import java.io.Serializable;
import java.util.Date;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;

import pnnl.goss.core.DataResponse;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;

@Component
public class ProcessNewSimulationRequest {
	
	public ProcessNewSimulationRequest() {
	}
	public ProcessNewSimulationRequest(LogManager logManager) {
		this.logManager = logManager;
	}
	
	

	@ServiceDependency
	private volatile LogManager logManager;
	

	public void process(ConfigurationManager configurationManager,
			SimulationManager simulationManager, StatusReporter statusReporter,
			int simulationId, DataResponse event, Serializable message) {

		try {
			RequestSimulation config = RequestSimulation.parse(message.toString());
			logManager.log(new LogMessage(new Integer(simulationId).toString(),new Date().getTime(), "Parsed config " + config, LogLevel.INFO, ProcessStatus.RUNNING, false));
			if (config == null || config.getPower_system_config() == null
					|| config.getSimulation_config() == null) {
				logManager.log(new LogMessage(new Integer(simulationId).toString(),new Date().getTime(), "No simulation file returned for request "
						+ config, LogLevel.ERROR, ProcessStatus.ERROR, false));
				throw new RuntimeException("Invalid configuration received");
			}

			// make request to configuration Manager to get power grid model
			// file locations and names
			logManager.log(new LogMessage(new Integer(simulationId).toString(),new Date().getTime(), "Creating simulation and power grid model files for simulation Id "
					+ simulationId, LogLevel.DEBUG, ProcessStatus.RUNNING, false));

			File simulationFile = configurationManager.getSimulationFile(
					simulationId, config);
			if (simulationFile == null) {
				logManager.log(new LogMessage(new Integer(simulationId).toString(),new Date().getTime(), "No simulation file returned for request "
						+ config, LogLevel.ERROR, ProcessStatus.ERROR, false));
				throw new Exception("No simulation file returned for request "
						+ config);
			}

			logManager.log(new LogMessage(new Integer(simulationId).toString(),new Date().getTime(), "Simulation and power grid model files generated for simulation Id "
					+ simulationId, LogLevel.DEBUG, ProcessStatus.RUNNING, false));

			// start simulation
			logManager.log(new LogMessage(new Integer(simulationId).toString(),new Date().getTime(), "Starting simulation for id " + simulationId, LogLevel.DEBUG, ProcessStatus.RUNNING, false));
			simulationManager.startSimulation(simulationId, simulationFile,
					config.getSimulation_config());
			logManager.log(new LogMessage(new Integer(simulationId).toString(),new Date().getTime(), "Started simulation for id " + simulationId, LogLevel.DEBUG, ProcessStatus.RUNNING, false));

		} catch (Exception e) {
			e.printStackTrace();
			try {
				statusReporter.reportStatus(
						GridAppsDConstants.topic_simulationStatus
								+ simulationId,
						"Process Initialization error: " + e.getMessage());
				logManager.log(new LogMessage(new Integer(simulationId).toString(),new Date().getTime(), "Process Initialization error: " + e.getMessage(), LogLevel.ERROR, ProcessStatus.ERROR, false));

			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}

}
