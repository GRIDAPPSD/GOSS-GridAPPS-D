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
package gov.pnnl.goss.gridappsd.data;

import gov.pnnl.goss.gridappsd.api.LogDataManager;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;


@Component
public class LogDataManagerMySQL implements LogDataManager {
	
	@ServiceDependency
	GridAppsDataSources dataSources;
	
	@ServiceDependency
	ClientFactory clientFactory;
	
	private Connection connection;
	private PreparedStatement preparedStatement;
	Client client;
	
    private Logger log = LoggerFactory.getLogger(getClass());

	
	@Start
	public void start(){
		
		try {
			Credentials credentials = new UsernamePasswordCredentials(
					GridAppsDConstants.username, GridAppsDConstants.password);
			client = clientFactory.create(PROTOCOL.STOMP,credentials);
			connection = dataSources.getDataSourceByKey("gridappsd").getConnection();
			
		} catch (SQLException e) { 
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	public void store(String source, String processId, long timestamp,
			String log_message, LogLevel log_level, ProcessStatus process_status, String username) {
		
		if(connection!=null){
			try {
				
				preparedStatement = connection.prepareStatement("INSERT INTO gridappsd.log VALUES (default, ?, ?, ?, ?, ?, ?)");
				preparedStatement.setString(1, processId);
				preparedStatement.setTimestamp(2, new Timestamp(timestamp));
				preparedStatement.setString(3, log_message);
				preparedStatement.setString(4, log_level.toString());
				preparedStatement.setString(5, process_status.toString());
				preparedStatement.setString(6, username);
				
				preparedStatement.executeUpdate();
				
				
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else {
			//Need to log a warning to file, that the connection did not exist
			log.warn("Mysql connection not initialized for store");
		}
		
		
		

	}

	@Override
	public void query(String source, String processId, long timestamp, LogLevel log_level, ProcessStatus process_status,
			String username, String resultTopic, String logTopic) {
		
		if(connection!=null){
		
		try {
			String queryString = "SELECT * FROM gridappsd.log WHERE";
			if(source!=null)
				queryString+=" source="+source;
			if(processId!=null)
				queryString+=" process_id="+processId;
			if(log_level!=null)
				queryString+=" log_level="+log_level;
			if(process_status!=null)
				queryString+=" process_status="+process_status;
			if(username!=null)
				queryString+=" username="+username;
			if(timestamp!=new Long("OL"))
				queryString+=" timestamp="+timestamp;
					
			preparedStatement = connection.prepareStatement(queryString);
			
			ResultSet rs = preparedStatement.executeQuery();
			
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnsNumber = rsmd.getColumnCount();
			String rowResult="";

			while (rs.next()) {
			    for(int i = 1; i < columnsNumber; i++)
			    	rowResult = rowResult + " " + rs.getString(i);
			    client.publish(resultTopic, rowResult);
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		} else {
			//Need a way to log warning to file that connection does not exist
			log.warn("Mysql connection not initialized for query");
		}		

	}


}
