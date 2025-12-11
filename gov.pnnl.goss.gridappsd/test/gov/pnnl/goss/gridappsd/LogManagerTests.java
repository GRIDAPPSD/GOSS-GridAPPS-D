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
package gov.pnnl.goss.gridappsd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.text.ParseException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import gov.pnnl.goss.gridappsd.api.LogDataManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.dto.LogMessage;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.RequestLogMessage;
import gov.pnnl.goss.gridappsd.log.LogManagerImpl;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;

/**
 * Tests for LogManager functionality.
 *
 * The LogManager in GridAPPS-D works by: 1. Log methods (debug, info, etc.)
 * create a LogMessage and publish to a topic 2. A subscription receives these
 * messages and calls logToConsole 3. logToConsole stores to DB if storeToDb is
 * true
 *
 * These tests verify: - LogMessage creation and parsing - LogDataManager query
 * functionality - LogManager configuration
 */
@RunWith(MockitoJUnitRunner.class)
public class LogManagerTests {

    @Mock
    LogDataManager logDataManager;

    @Captor
    ArgumentCaptor<String> argCaptor;
    @Captor
    ArgumentCaptor<Long> argLongCaptor;
    @Captor
    ArgumentCaptor<LogLevel> argLogLevelCaptor;
    @Captor
    ArgumentCaptor<ProcessStatus> argProcessStatusCaptor;

    /**
     * Test that LogMessage can be created and parsed correctly. This verifies the
     * data transfer object works as expected.
     */
    @Test
    public void logMessage_canBeCreatedAndParsed() {
        LogMessage message = new LogMessage(
                "test.source",
                "process_123",
                System.currentTimeMillis(),
                "Test log message",
                LogLevel.INFO,
                ProcessStatus.RUNNING,
                true,
                "simulation");

        assertNotNull(message);
        assertEquals("test.source", message.getSource());
        assertEquals("process_123", message.getProcessId());
        assertEquals("Test log message", message.getLogMessage());
        assertEquals(LogLevel.INFO, message.getLogLevel());
        assertEquals(ProcessStatus.RUNNING, message.getProcessStatus());
        assertEquals(true, message.getStoreToDb());
        assertEquals("simulation", message.getProcess_type());

        // Test serialization and parsing
        String json = message.toString();
        LogMessage parsed = LogMessage.parse(json);

        assertEquals(message.getSource(), parsed.getSource());
        assertEquals(message.getProcessId(), parsed.getProcessId());
        assertEquals(message.getLogMessage(), parsed.getLogMessage());
        assertEquals(message.getLogLevel(), parsed.getLogLevel());
        assertEquals(message.getProcessStatus(), parsed.getProcessStatus());
    }

    /**
     * Test that LogManager can be instantiated with LogDataManager dependency.
     */
    @Test
    public void logManager_canBeCreatedWithLogDataManager() {
        LogManager logManager = new LogManagerImpl(logDataManager);
        assertNotNull(logManager);
        assertEquals(logDataManager, logManager.getLogDataManager());
    }

    /**
     * Test that get() method calls LogDataManager.query() with correct parameters
     * when using a RequestLogMessage object.
     */
    @Test
    public void queryCalledWhen_getLogCalledWithObject() throws ParseException {

        LogManager logManager = new LogManagerImpl(logDataManager);

        RequestLogMessage message = new RequestLogMessage();
        message.setLogLevel(LogLevel.DEBUG);
        message.setSource(this.getClass().getName());
        message.setProcessStatus(ProcessStatus.RUNNING);
        message.setTimestamp(GridAppsDConstants.SDF_SIMULATION_REQUEST.parse("11/11/11 11:11:11").getTime());

        String resultTopic = "goss.gridappsd.data.output";
        String logTopic = "goss.gridappsd.data.log";

        logManager.get(message, resultTopic, logTopic);

        // Verify that query was called on the logDataManager
        Mockito.verify(logDataManager).query(
                Mockito.eq(this.getClass().getName()),
                Mockito.isNull(),
                Mockito.eq(GridAppsDConstants.SDF_SIMULATION_REQUEST.parse("11/11/11 11:11:11").getTime()),
                Mockito.eq(LogLevel.DEBUG),
                Mockito.eq(ProcessStatus.RUNNING),
                Mockito.eq("system"),
                Mockito.isNull());
    }

    /**
     * Test that get() method calls LogDataManager.query() with a custom query
     * string.
     */
    @Test
    public void queryCalledWhen_getLogCalledWithCustomQuery() {

        LogManager logManager = new LogManagerImpl(logDataManager);

        RequestLogMessage message = new RequestLogMessage();
        message.setQuery("SELECT * FROM log WHERE process_id = 'test_123'");

        String resultTopic = "goss.gridappsd.data.output";
        String logTopic = "goss.gridappsd.data.log";

        logManager.get(message, resultTopic, logTopic);

        // Verify that query was called with the custom query string
        Mockito.verify(logDataManager).query(Mockito.eq("SELECT * FROM log WHERE process_id = 'test_123'"));
    }

    /**
     * Test that RequestLogMessage can properly hold query parameters.
     */
    @Test
    public void requestLogMessage_holdsCorrectParameters() throws ParseException {
        RequestLogMessage message = new RequestLogMessage();
        message.setLogLevel(LogLevel.ERROR);
        message.setSource("test.source");
        message.setProcessStatus(ProcessStatus.ERROR);
        message.setProcessId("sim_456");
        message.setTimestamp(GridAppsDConstants.SDF_SIMULATION_REQUEST.parse("08/14/17 02:22:22").getTime());

        assertEquals(LogLevel.ERROR, message.getLogLevel());
        assertEquals("test.source", message.getSource());
        assertEquals(ProcessStatus.ERROR, message.getProcessStatus());
        assertEquals("sim_456", message.getProcessId());
    }

}
