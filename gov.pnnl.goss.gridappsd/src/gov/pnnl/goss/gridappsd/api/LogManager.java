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
package gov.pnnl.goss.gridappsd.api;

import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.RequestLogMessage;

public interface LogManager {
	
	/**
	 * Implementation of this method should writes the message in log file. And
	 * calls LogDataManager to save the log message in data store if store_to_db
	 * is true in LogMessage object.
	 * 
	 * @param message
	 *            an Object of gov.pnnl.goss.gridappsd.dto.LogMessage
	 * @param username
	 *            username of the user logging the message
	 * @param topic
	 *            Message should be published on this topic if topic is not null. 
	 */
	//void log(LogMessage message, String username, String topic);
	
	
	public void trace(ProcessStatus processStatus, String processId, String message);

	public void debug(ProcessStatus processStatus, String processId, String message);
	
	public void info(ProcessStatus processStatus, String processId, String message);

	public void warn(ProcessStatus processStatus, String processId, String message);

	public void error(ProcessStatus processStatus, String processId, String message);

	public void fatal(ProcessStatus processStatus, String processId, String message);
	
	public void logMessageFromSource(ProcessStatus processStatus, String processId, String message, String source, LogLevel logLevel);
	/**
	 * Use platform's default username and call previous log method. 
	 * 
	 * @param message
	 *            an Object of gov.pnnl.goss.gridappsd.dto.LogMessage
	 * @param topic
	 *            Message should be published on this topic if topic is not null. 
	 */
//	void log(LogMessage message, String topic);

	/**
	 * Implementation of this method should call an implementation of
	 * LogDataManager and get the log messages from data store based on the not
	 * null values in LogMessage object.
	 */
	void get(RequestLogMessage message, String outputTopics, String LogTopic);
	
	LogDataManager getLogDataManager();
	
	LogLevel getLogLevel();

	public void setProcessType(String processId, String process_type);

}
