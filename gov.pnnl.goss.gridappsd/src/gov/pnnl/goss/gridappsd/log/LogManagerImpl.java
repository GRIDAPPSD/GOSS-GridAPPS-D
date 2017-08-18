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

package gov.pnnl.goss.gridappsd.log;

import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.dto.LogMessage;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Start;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * This class implements functionalities for Internal Function 409 Log Manager.
 * LogManager is responsible for logging messages coming from platform and other
 * processes in log file/stream as well as data store.
 * 
 * @author shar064
 *
 */
@Component
public class LogManagerImpl implements LogManager {
	
	private static Logger log = LoggerFactory.getLogger(LogManagerImpl.class);

	@ServiceDependency 
	private volatile DataManager dataManager;
	
	public public LogManagerImpl() { }
	
	public public LogManagerImpl(DataManager dataManager) {
		this.dataManager = dataManager;
	}
	
	@Start
	public void start() {
		
		log.debug("Starting "+this.getClass().getName());

	}

	@Override
	public void log(String message) {
		
		Gson gson = new Gson();
		LogMessage obj = gson.fromJson(message, LogMessage.class); 
		String process_id = obj.getProcess_id();
		String timestamp = obj.getTimestamp();
		String log_message = obj.getLog_message();
		String log_level = obj.getLog_level();
		String process_status = obj.getProcess_status();
		Boolean storeToDB = obj.getStoreToDB();
		String username = "system";
		
		log.debug(String.format("%s|%s|%s|%s|%s\n%s\n", timestamp, process_id,
				process_status, username, log_level, log_message));	
		
		if(storeToDB)
			store(process_id, username, timestamp, log_message, log_level, process_status);
	}
	
	@Override
	public void log(LogMessage message) {
		
		String process_id = message.getProcess_id();
		String timestamp = message.getTimestamp();
		String log_message = message.getLog_message();
		String log_level = message.getLog_level();
		String process_status = message.getProcess_status();
		Boolean storeToDB = message.getStoreToDB();
		String username = "system";
		
		log.debug(String.format("%s|%s|%s|%s|%s\n%s\n", timestamp, process_id,
				process_status, username, log_level, log_message));	

		if(storeToDB)
			store(process_id,username,timestamp,log_message,log_level,process_status);
		
	}
	
	private void store(String process_id, String username, String timestamp,
			String log_message, String log_level, String process_status) {
		
		//TODO: Save log in data store using DataManager
		log.debug("log saved");

	}

}
