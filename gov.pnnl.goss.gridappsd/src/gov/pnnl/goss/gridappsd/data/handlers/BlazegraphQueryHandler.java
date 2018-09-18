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
package gov.pnnl.goss.gridappsd.data.handlers;

import java.util.Date;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetCloseable;

import gov.pnnl.goss.cim2glm.queryhandler.QueryHandler;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;

public class BlazegraphQueryHandler implements QueryHandler {
	String endpoint;
	final String nsCIM = "http://iec.ch/TC57/2012/CIM-schema-cim17#";
	final String nsRDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	final String nsXSD = "http://www.w3.org/2001/XMLSchema#";

	public static final String DEFAULT_ENDPOINT =  "http://blazegraph:8080/bigdata/namespace/kb/sparql";
	String mRID = null;
	String processID = null;
	String username = null;
	boolean use_mRID;
	LogManager logManager;
	
//	public BlazegraphQueryHandler(String endpoint) {
//		this(endpoint, null);
//	}
	
	public BlazegraphQueryHandler(String endpoint, LogManager logManager, String processId, String userName) {
		this.endpoint = endpoint;
		this.use_mRID = false;
		this.logManager = logManager;
		this.processID = processId;
		this.username = userName;
	}
	public String getEndpoint() {
		return endpoint;
	}
	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public LogManager getLogManager() {
		return logManager;
	}
	public void setLogManager(LogManager logManager) {
		this.logManager = logManager;
	}
	@Override
	public ResultSetCloseable query(String szQuery) { 
		String qPrefix = "PREFIX r: <" + nsRDF + "> PREFIX c: <" + nsCIM + "> PREFIX rdf: <" + nsRDF + "> PREFIX cim: <" + nsCIM + "> PREFIX xsd:<" + nsXSD + "> ";
		Query query = QueryFactory.create (qPrefix + szQuery);
		GridAppsDConstants.logMessage(logManager, this.getClass().getName(), "Executing query "+szQuery, processID, username, LogLevel.DEBUG);

		long start = new Date().getTime();

		if (mRID!=null && mRID.trim().length()>0) { // try to insert a VALUES block for the feeder mRID of interest
			String insertion_point = "WHERE {";
			int idx = szQuery.lastIndexOf (insertion_point);
			if (idx >= 0) {
//				System.out.println ("\n***");
//				System.out.println (szQuery);
//				System.out.println ("***");
				StringBuilder buf = new StringBuilder (qPrefix + szQuery.substring (0, idx) + insertion_point + " VALUES ?fdrid {\"");
				buf.append (mRID + "\"} " + szQuery.substring (idx + insertion_point.length()));
//				System.out.println ("Sending " + buf.toString());
				query = QueryFactory.create (buf.toString());
			} else {
				query = QueryFactory.create (qPrefix + szQuery);
			}
		} //else {
		//	query = QueryFactory.create (qPrefix + szQuery);
		//}
		QueryExecution qexec = QueryExecutionFactory.sparqlService (endpoint, query);

		long end = new Date().getTime();
		GridAppsDConstants.logMessage(logManager, this.getClass().getName(), "Query execution took: "+(end-start)+"ms", processID, username, LogLevel.DEBUG);
		ResultSetCloseable rs=  ResultSetCloseable.closeableResultSet(qexec);
		return rs;
	}
	public boolean addFeederSelection (String mRID) {
		this.mRID = mRID;
		use_mRID = true;
		return use_mRID;
	}
	public boolean clearFeederSelections () {
		use_mRID = false;
		return use_mRID;
	}
	@Override
	public String getFeederSelection() {
		return this.mRID;
	}
	
	
}
