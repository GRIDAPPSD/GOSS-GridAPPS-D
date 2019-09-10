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

import gov.pnnl.goss.gridappsd.api.DataManagerHandler;
import gov.pnnl.goss.gridappsd.api.LogDataManager;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.RequestLogMessage;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DataTruncation;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import pnnl.goss.core.Client;
import pnnl.goss.core.Client.PROTOCOL;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.security.SecurityConfig;


@Component
public class LogDataManagerMySQL implements LogDataManager, DataManagerHandler {
	
	@ServiceDependency
	GridAppsDataSources dataSources;

	@ServiceDependency
	ClientFactory clientFactory;
	
	@ServiceDependency
	SecurityConfig securityConfig;
	
	private Connection connection;
	private PreparedStatement preparedStatement;
	Client client;
	
	
	
	public static final String DATA_MANAGER_TYPE = "log";
	
	public LogDataManagerMySQL(){
		System.out.println("CREATING LOG DATA MGR MYSQL");
	}
	
    private Logger log = LoggerFactory.getLogger(getClass());
	
	@Start
	public void start(){
		
		try {
			Credentials credentials = new UsernamePasswordCredentials(
					securityConfig.getManagerUser(), securityConfig.getManagerPassword());
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
			String log_message, LogLevel log_level, ProcessStatus process_status, String username, String process_type) {
		
		if(connection!=null){
			try {
				
				preparedStatement = connection.prepareStatement("INSERT INTO gridappsd.log ("
						+ "id, "
						+ "source, "
						+ "process_id, "
						+ "timestamp, "
						+ "log_message, "
						+ "log_level, "
						+ "process_status, "
						+ "username, "
						+ "process_type) "
						+ "VALUES (default, ?, ?, ?, ?, ?, ?,?,?)");
				preparedStatement.setString(1, source);
				preparedStatement.setString(2, processId);
				preparedStatement.setTimestamp(3, new Timestamp(timestamp));
				preparedStatement.setString(4, log_message);
				preparedStatement.setString(5, log_level.toString());
				preparedStatement.setString(6, process_status.toString());
				preparedStatement.setString(7, username);
				preparedStatement.setString(8, process_type);
				
				
				preparedStatement.executeUpdate();
				
			} catch (Exception e) {
				log.error("Error while storing log:");
				log.error("error = " + e.getMessage());
				log.error("source = " + source);
				log.error("message = " + log_message);
			}
		} else {
			//Need to log a warning to file, that the connection did not exist
			log.warn("Mysql connection not initialized for store");
		}
		
		
		

	}
	
	@Override
	public void storeExpectedResults(String test_id, String processId, long simulation_time,
			String mrid, String property, String expected, String actual) {
		
		if(connection!=null){
			try {
				
				preparedStatement = connection.prepareStatement("INSERT INTO gridappsd.expected_results VALUES (default, ?, ?, ?, ?, ?, ?,?)");
				preparedStatement.setString(1, test_id);
				preparedStatement.setString(2, processId);
				preparedStatement.setString(3, mrid);
				preparedStatement.setString(4, property);
				preparedStatement.setString(5, expected);
				preparedStatement.setString(6, actual);
				preparedStatement.setTimestamp(7, new Timestamp(simulation_time));
				
				preparedStatement.executeUpdate();
				
			} catch (DataTruncation e) {
				log.error("Error while storing log:");
				log.error("error = " + e.getMessage());
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else {
			//Need to log a warning to file, that the connection did not exist
			log.warn("Mysql connection not initialized for store");
		}

	}

	@Override
	public Serializable query(String source, String processId, long timestamp, LogLevel log_level, ProcessStatus process_status,
			String username, String process_type) {
		
		if(connection==null){
			try {
				connection = dataSources.getDataSourceByKey("gridappsd").getConnection();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if(connection!=null){
		
		try {
			String queryString = "SELECT * FROM gridappsd.log WHERE";
			
			boolean where = false;
			
			if(source!=null){
				queryString+=" source=\'"+source+"\'";
				where = true;
			}
				
			if(processId!=null)
				if(where)
					queryString+=" and process_id=\'"+processId+"\'";
				else{
					queryString+=" process_id=\'"+processId+"\'";
					where = true;
				}
			
			if(log_level!=null)
				if(where)
					queryString+=" and log_level=\'"+log_level+"\'";
				else{
					queryString+=" log_level=\'"+log_level+"\'";
					where = true;
				}
			
			if(process_status!=null)
				if(where)
					queryString+=" and process_status=\'"+process_status+"\'";
				else{
				queryString+=" process_status=\'"+process_status+"\'";
					where = true;
				}
			
			if(username!=null)
				if(where)
					queryString+=" and username=\'"+username+"\'";
				else{
					queryString+=" username=\'"+username+"\'";
					where = true;
				}
			
			if(timestamp!=new Long("0"))
				if(where)
					queryString+=" and timestamp="+timestamp;
				else{
					queryString+=" timestamp="+timestamp;
				}

			preparedStatement = connection.prepareStatement(queryString);
			
			ResultSet rs = preparedStatement.executeQuery();
			
			return this.getJSONFromResultSet(rs);
			
			
			/*ResultSetMetaData rsmd = rs.getMetaData();
			int columnsNumber = rsmd.getColumnCount();
			String rowResult="";

			while (rs.next()) {
			    for(int i = 1; i < columnsNumber; i++)
			    	rowResult = rowResult + " " + rs.getString(i);
			    //client.publish(resultTopic, rowResult);
			}*/
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		} else {
			//Need a way to log warning to file that connection does not exist
			log.warn("Mysql connection not initialized for query");
		}	
		
		return null;

	}
	

	@Override
	public Serializable query(String queryString){
		if(connection==null){
			try {
				connection = dataSources.getDataSourceByKey("gridappsd").getConnection();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if(connection!=null){
			try{
				preparedStatement = connection.prepareStatement(queryString);
				ResultSet rs = preparedStatement.executeQuery();
				return this.getJSONFromResultSet(rs);
		}catch (SQLException e) {
			e.printStackTrace();
		}
		} else {
			//Need a way to log warning to file that connection does not exist
			log.warn("Mysql connection not initialized for query");
		}	
		
		return null;
	}

	@Override
	public Serializable handle(Serializable requestContent, String processId,
			String username) throws Exception {
		
		RequestLogMessage request;
		
		if(requestContent instanceof RequestLogMessage)
			 request = (RequestLogMessage) requestContent;
		else 
			 request = RequestLogMessage.parse(requestContent.toString());
		
		if(request.getQuery()!=null)
			return this.query(request.getQuery());
		else
			return this.query(request.getSource(), request.getProcessId(), request.getTimestamp(), request.getLogLevel(),request.getProcessStatus(), request.getUsername(), request.getProcess_type());

	}
	
	public Serializable getJSONFromResultSet(ResultSet rs) {
	    List<Map<String,Object>> list = new ArrayList<Map<String,Object>>();
	    if(rs!=null)
	    {
	        try {
	            ResultSetMetaData metaData = rs.getMetaData();
	            while(rs.next())
	            {
	                Map<String,Object> columnMap = new HashMap<String, Object>();
	                for(int columnIndex=1;columnIndex<=metaData.getColumnCount();columnIndex++)
	                {
	                    if(rs.getString(metaData.getColumnName(columnIndex))!=null)
	                        columnMap.put(metaData.getColumnLabel(columnIndex),     rs.getString(metaData.getColumnName(columnIndex)));
	                    else
	                        columnMap.put(metaData.getColumnLabel(columnIndex), "");
	                }
	                list.add(columnMap);
	            }
	        } catch (SQLException e) {
	            e.printStackTrace();
	        }
	     }
	     return new Gson().toJson(list);
	}


}
