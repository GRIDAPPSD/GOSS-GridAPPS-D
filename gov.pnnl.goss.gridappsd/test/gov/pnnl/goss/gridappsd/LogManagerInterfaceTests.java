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

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import gov.pnnl.goss.gridappsd.api.LogDataManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.log.LogManagerImpl;

/**
 * Tests for the LogManager interface methods.
 *
 * These tests verify that the LogManagerImpl correctly implements all methods
 * defined in the LogManager interface: - trace, debug, info, warn, error, fatal
 * - logMessageFromSource - getLogDataManager - getLogLevel - setProcessType
 *
 * Note: LogManagerImpl publishes to a message broker which requires a client.
 * These tests verify method signatures and basic behavior without a connected
 * client.
 */
@RunWith(MockitoJUnitRunner.class)
public class LogManagerInterfaceTests {

    @Mock
    LogDataManager mockLogDataManager;

    LogManagerImpl logManager;

    @Before
    public void setUp() throws Exception {
        logManager = new LogManagerImpl(mockLogDataManager);
    }

    // ========== Interface Implementation Tests ==========

    /**
     * Test that LogManagerImpl implements LogManager interface.
     */
    @Test
    public void logManagerImpl_implementsLogManagerInterface() {
        assertTrue("LogManagerImpl should implement LogManager interface",
                logManager instanceof LogManager);
    }

    // ========== getLogDataManager() tests ==========

    /**
     * Test that getLogDataManager returns the injected LogDataManager.
     */
    @Test
    public void getLogDataManager_returnsInjectedLogDataManager() {
        LogDataManager result = logManager.getLogDataManager();
        assertEquals("getLogDataManager should return the injected LogDataManager",
                mockLogDataManager, result);
    }

    // ========== getLogLevel() tests ==========

    /**
     * Test that getLogLevel returns a value based on SLF4J logger level. When
     * logLevel field is not explicitly set, it falls back to detecting the SLF4J
     * logger's enabled level.
     */
    @Test
    public void getLogLevel_returnsLogLevelBasedOnSlf4jLogger() {
        LogLevel result = logManager.getLogLevel();
        // Returns a log level based on SLF4J logger configuration, not null
        // In test environment, this is typically DEBUG or INFO
        assertNotNull("getLogLevel should return a LogLevel based on SLF4J logger", result);
    }

    // ========== trace() method signature tests ==========

    /**
     * Test that trace method is callable with correct signature. Note: Will not
     * actually publish without a client connection.
     */
    @Test
    public void trace_methodIsCallable() {
        // Should not throw - method is callable even without client
        logManager.trace(ProcessStatus.RUNNING, "process-123", "Test trace message");
    }

    /**
     * Test that trace handles null processId.
     */
    @Test
    public void trace_handlesNullProcessId() {
        logManager.trace(ProcessStatus.RUNNING, null, "Test message");
    }

    // ========== debug() method signature tests ==========

    /**
     * Test that debug method is callable with correct signature.
     */
    @Test
    public void debug_methodIsCallable() {
        logManager.debug(ProcessStatus.RUNNING, "process-123", "Test debug message");
    }

    /**
     * Test that debug handles null processId.
     */
    @Test
    public void debug_handlesNullProcessId() {
        logManager.debug(ProcessStatus.RUNNING, null, "Test debug message");
    }

    // ========== info() method signature tests ==========

    /**
     * Test that info method is callable with correct signature.
     */
    @Test
    public void info_methodIsCallable() {
        logManager.info(ProcessStatus.RUNNING, "process-123", "Test info message");
    }

    /**
     * Test that info handles empty message.
     */
    @Test
    public void info_handlesEmptyMessage() {
        logManager.info(ProcessStatus.RUNNING, "process-123", "");
    }

    // ========== warn() method signature tests ==========

    /**
     * Test that warn method is callable with correct signature.
     */
    @Test
    public void warn_methodIsCallable() {
        logManager.warn(ProcessStatus.RUNNING, "process-123", "Test warn message");
    }

    // ========== error() method signature tests ==========

    /**
     * Test that error method is callable with correct signature.
     */
    @Test
    public void error_methodIsCallable() {
        logManager.error(ProcessStatus.ERROR, "process-123", "Test error message");
    }

    /**
     * Test that error handles long message.
     */
    @Test
    public void error_handlesLongMessage() {
        StringBuilder longMessage = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longMessage.append("This is a very long error message. ");
        }
        logManager.error(ProcessStatus.ERROR, "process-123", longMessage.toString());
    }

    // ========== fatal() method signature tests ==========

    /**
     * Test that fatal method is callable with correct signature.
     */
    @Test
    public void fatal_methodIsCallable() {
        logManager.fatal(ProcessStatus.ERROR, "process-123", "Test fatal message");
    }

    // ========== logMessageFromSource() tests ==========

    /**
     * Test that logMessageFromSource method is callable with correct signature.
     */
    @Test
    public void logMessageFromSource_methodIsCallable() {
        logManager.logMessageFromSource(ProcessStatus.RUNNING, "process-123",
                "Test message", "TestSource", LogLevel.INFO);
    }

    /**
     * Test that logMessageFromSource handles all log levels.
     */
    @Test
    public void logMessageFromSource_handlesAllLogLevels() {
        for (LogLevel level : LogLevel.values()) {
            logManager.logMessageFromSource(ProcessStatus.RUNNING, "process-123",
                    "Test message for " + level, "TestSource", level);
        }
    }

    // ========== setProcessType() tests ==========

    /**
     * Test that setProcessType method is callable with correct signature.
     */
    @Test
    public void setProcessType_methodIsCallable() {
        logManager.setProcessType("process-123", "SIMULATION");
    }

    // ========== ProcessStatus enum tests ==========

    /**
     * Test that all ProcessStatus values are supported.
     */
    @Test
    public void allProcessStatuses_areSupported() {
        for (ProcessStatus status : ProcessStatus.values()) {
            // All should be callable without throwing
            logManager.info(status, "process-123", "Testing status: " + status);
        }
    }

    // ========== LogLevel enum tests ==========

    /**
     * Test that all LogLevel values are defined.
     */
    @Test
    public void allLogLevels_areDefined() {
        LogLevel[] levels = LogLevel.values();
        assertTrue("Should have multiple log levels", levels.length >= 5);

        // Verify common log levels exist
        assertNotNull(LogLevel.TRACE);
        assertNotNull(LogLevel.DEBUG);
        assertNotNull(LogLevel.INFO);
        assertNotNull(LogLevel.WARN);
        assertNotNull(LogLevel.ERROR);
        assertNotNull(LogLevel.FATAL);
    }

    // ========== Interface method accessibility tests ==========

    /**
     * Test that all interface methods are accessible via interface reference.
     */
    @Test
    public void allInterfaceMethods_areAccessible() {
        LogManager manager = logManager;

        // All these should be callable
        manager.trace(ProcessStatus.RUNNING, null, "test");
        manager.debug(ProcessStatus.RUNNING, null, "test");
        manager.info(ProcessStatus.RUNNING, null, "test");
        manager.warn(ProcessStatus.RUNNING, null, "test");
        manager.error(ProcessStatus.ERROR, null, "test");
        manager.fatal(ProcessStatus.ERROR, null, "test");
        manager.logMessageFromSource(ProcessStatus.RUNNING, null, "test", "source", LogLevel.INFO);
        manager.setProcessType("process", "type");
        manager.getLogDataManager();
        manager.getLogLevel();
    }
}
