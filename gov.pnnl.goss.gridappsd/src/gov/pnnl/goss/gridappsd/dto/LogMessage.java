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
package gov.pnnl.goss.gridappsd.dto;

import com.google.gson.Gson;

public class LogMessage {
	
	public enum LogLevel {
		TRACE, DEBUG, INFO, WARN, ERROR, FATAL
	}
	public enum ProcessStatus {
		STARTING, STARTED, RUNNING, ERROR, CLOSED
	}
	
	String process_id;
	String parent_process_id;

	long timestamp;
	String log_message;
	LogLevel log_level;
	ProcessStatus process_status;
	Boolean storeToDB = true;
	
	//I would change timestamp to a long, log level and process status to enums. and probably process id to a numeric.  and storeToDB should be store_to_db for consistency
	
	public LogMessage(){}
	public LogMessage(String process_id, long timestamp, String log_message, LogLevel log_level, ProcessStatus process_status, Boolean storeToDB){
		this.process_id = process_id;
		this.timestamp = timestamp;
		this.log_level = log_level;
		this.log_message = log_message;
		this.process_status = process_status;
		this.storeToDB = storeToDB;
	}
	
	public String getProcess_id() {
		return process_id;
	}
	public void setProcess_id(String process_id) {
		this.process_id = process_id;
	}
	public String getParent_process_id() {
		return parent_process_id;
	}
	public void setParent_process_id(String parent_process_id) {
		this.parent_process_id = parent_process_id;
	}
	public long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	public String getLog_message() {
		return log_message;
	}
	public void setLog_message(String log_message) {
		this.log_message = log_message;
	}
	public LogLevel getLog_level() {
		return log_level;
	}
	public void setLog_level(LogLevel log_level) {
		this.log_level = log_level;
	}
	public ProcessStatus getProcess_status() {
		return process_status;
	}
	public void setProcess_status(ProcessStatus process_status) {
		this.process_status = process_status;
	}
	public Boolean getStoreToDB() {
		return storeToDB;
	}
	public void setStoreToDB(Boolean storeToDB) {
		this.storeToDB = storeToDB;
	}
	
	
	public static LogMessage parse(String jsonString){
		Gson  gson = new Gson();
		LogMessage obj = gson.fromJson(jsonString, LogMessage.class);
		if(obj.log_message==null)
			throw new RuntimeException("Expected attribute log_message not found");
		return obj;
	}
	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}
}
