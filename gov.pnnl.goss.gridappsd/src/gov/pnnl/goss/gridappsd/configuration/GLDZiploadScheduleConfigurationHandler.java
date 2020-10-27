/*******************************************************************************
 * Copyright  2017, Battelle Memorial Institute All rights reserved.
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
package gov.pnnl.goss.gridappsd.configuration;

import gov.pnnl.goss.gridappsd.api.ConfigurationHandler;
import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.data.ProvenTimeSeriesDataManagerImpl;
import gov.pnnl.goss.gridappsd.data.conversion.ProvenLoadScheduleToGridlabdLoadScheduleConverter;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.RequestTimeseriesData;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;

import pnnl.goss.core.Client;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.security.SecurityConfig;

@Component
public class GLDZiploadScheduleConfigurationHandler extends
		BaseConfigurationHandler implements ConfigurationHandler {

	Client client = null;

	@ServiceDependency
	private volatile ConfigurationManager configManager;
	@ServiceDependency
	volatile LogManager logManager;
	@ServiceDependency
	volatile DataManager dataManager;
	@ServiceDependency
	volatile SecurityConfig securityConfig;

	public static final String TYPENAME = "GridLAB-D Zipload Schedule";
	public static final String DIRECTORY = "directory";
	public static final String SIMULATIONNAME = "simulation_name";
	public static final String SCHEDULENAME = "schedule_name";
	public static final String SIMULATIONSTARTTIME = "simulation_start_time";
	public static final String SIMULATIONDURATION = "simulation_duration";
	public static final String SIMULATIONID = "simulation_id";
	public static final String STARTTIME_FILTER = "startTime";
	public static final String ENDTIME_FILTER = "endTime";
	public static final int TIMEFILTER_YEAR = 2018;

	public static final String CIM2GLM_PREFIX = "model";
	
	final double sqrt3 = Math.sqrt(3);

	public GLDZiploadScheduleConfigurationHandler() {
	}

	public GLDZiploadScheduleConfigurationHandler(LogManager logManager,
			DataManager dataManager) {
		this.logManager = logManager;
		this.dataManager = dataManager;
	}

	@Override
	@Start
	public void start() {
		if (configManager != null) {
			configManager.registerConfigurationHandler(TYPENAME, this);
		} else {
			logManager.error(ProcessStatus.ERROR, null, "No Config manager avilable for " + getClass().getSimpleName());
		}
	}

	@Override
	public void generateConfig(Properties parameters, PrintWriter out,
			String processId, String username) throws Exception {

		logManager.info(ProcessStatus.RUNNING, processId, "Generating zipload schedule GridLAB-D configuration files using parameters: "
						+ parameters);

		String scheduleName = GridAppsDConstants.getStringProperty(parameters,
				SCHEDULENAME, null);
		String directory = GridAppsDConstants.getStringProperty(parameters,
				DIRECTORY, null);
		if (directory == null || directory.trim().length() == 0) {
			logManager.error(ProcessStatus.ERROR, processId, "No " + DIRECTORY + " parameter provided");
			throw new Exception("Missing parameter " + DIRECTORY);
		}

		long simulationStartTime = GridAppsDConstants.getLongProperty(
				parameters, SIMULATIONSTARTTIME, -1);
		if (simulationStartTime < 0) {
			logManager.error(ProcessStatus.ERROR, processId,"No " + SIMULATIONSTARTTIME + " parameter provided");
			throw new Exception("Missing parameter " + SIMULATIONSTARTTIME);
		}
		long simulationDuration = GridAppsDConstants.getLongProperty(
				parameters, SIMULATIONDURATION, 0);
		if (simulationDuration == 0) {
			logManager.error(ProcessStatus.ERROR, processId,"No " + SIMULATIONDURATION + " parameter provided");
			throw new Exception("Missing parameter " + SIMULATIONDURATION);
		}
		File dir = new File(directory);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		String tempDataPath = dir.getAbsolutePath();
		
		String simulationID = GridAppsDConstants.getStringProperty(parameters, SIMULATIONID, null);
		String loadprofile = GridAppsDConstants.getStringProperty(parameters, SCHEDULENAME, "ieeezipload");
		String simId = "1";
		if(simulationID==null || simulationID.trim().length()==0){
			logManager.error(ProcessStatus.ERROR, simulationID,"No "+SIMULATIONID+" parameter provided");
			throw new Exception("Missing parameter "+SIMULATIONID);
		}
		try{
			simId = simulationID;
		}catch (Exception e) {
			logManager.error(ProcessStatus.ERROR, simulationID,"Simulation ID not a valid  "+simulationID+", defaulting to "+simId);
		}
				
		RequestTimeseriesData request = new RequestTimeseriesData();
		request.setQueryMeasurement(loadprofile);
		request.setResponseFormat(ProvenLoadScheduleToGridlabdLoadScheduleConverter.OUTPUT_FORMAT);
		Map<String, Object> queryFilter = new HashMap<String, Object>();

		Calendar c = Calendar.getInstance();
		c.setTime(new Date(simulationStartTime*1000));
		int simulationYear = c.get(Calendar.YEAR);
		c.set(Calendar.YEAR, TIMEFILTER_YEAR);
		queryFilter.put(STARTTIME_FILTER, "" + c.getTimeInMillis());
		c.add(Calendar.SECOND, new Long(simulationDuration).intValue());
		queryFilter.put(ENDTIME_FILTER, "" + c.getTimeInMillis());
		request.setQueryFilter(queryFilter);
		request.setSimulationYear(simulationYear);
		request.setOriginalFormat("loadprofile");
		DataResponse resp = (DataResponse) dataManager.processDataRequest(
				request,
				ProvenTimeSeriesDataManagerImpl.DATA_MANAGER_TYPE, simId,
				tempDataPath, username);
		if (resp.getData() == null) {
			throw new Exception(
					"No load schedule data in time series data store. Setting useClimate = false.");
		} else {
			File loadScheduleFile = new File(directory + File.separator+ scheduleName+".player");
			FileOutputStream fout = new FileOutputStream(loadScheduleFile);
			fout.write(resp.getData().toString().getBytes());
			fout.flush();
			fout.close();
		}

		logManager.info(ProcessStatus.RUNNING, processId, "Finished generating all GridLAB-D configuration files.");

	}

}
