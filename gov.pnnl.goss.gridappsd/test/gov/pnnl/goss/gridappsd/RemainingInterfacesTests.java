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
import static org.mockito.Mockito.*;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import gov.pnnl.goss.gridappsd.api.ConfigurationHandler;
import gov.pnnl.goss.gridappsd.api.DataManagerHandler;
import gov.pnnl.goss.gridappsd.api.FieldBusManager;
import gov.pnnl.goss.gridappsd.api.GridAppsDataHandler;
import gov.pnnl.goss.gridappsd.api.LogDataManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.RoleManager;
import gov.pnnl.goss.gridappsd.api.TimeseriesDataManager;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.SimulationContext;
import pnnl.goss.core.Response;

/**
 * Tests for remaining API interfaces: - RoleManager - FieldBusManager -
 * TimeseriesDataManager - LogDataManager - GridAppsDataHandler -
 * DataManagerHandler - ConfigurationHandler
 *
 * These interfaces are typically implemented by specific components or are
 * functional interfaces that require specific context. This test file verifies
 * their contracts and that mock implementations can be created.
 */
@RunWith(MockitoJUnitRunner.class)
public class RemainingInterfacesTests {

    @Mock
    LogManager mockLogManager;

    // ==================== RoleManager Interface Tests ====================

    /**
     * Test that RoleManager interface has getRoles method.
     */
    @Test
    public void roleManager_getRolesMethodExists() throws Exception {
        RoleManager mockRoleManager = mock(RoleManager.class);
        List<String> roles = new ArrayList<>();
        roles.add("admin");
        roles.add("operator");
        when(mockRoleManager.getRoles("testUser")).thenReturn(roles);

        List<String> result = mockRoleManager.getRoles("testUser");

        assertNotNull("getRoles should return a list", result);
        assertEquals("Should have 2 roles", 2, result.size());
        assertTrue("Should contain 'admin' role", result.contains("admin"));
    }

    /**
     * Test that RoleManager interface has hasRole method.
     */
    @Test
    public void roleManager_hasRoleMethodExists() throws Exception {
        RoleManager mockRoleManager = mock(RoleManager.class);
        when(mockRoleManager.hasRole("testUser", "admin")).thenReturn(true);
        when(mockRoleManager.hasRole("testUser", "superuser")).thenReturn(false);

        assertTrue("User should have admin role", mockRoleManager.hasRole("testUser", "admin"));
        assertFalse("User should not have superuser role", mockRoleManager.hasRole("testUser", "superuser"));
    }

    // ==================== FieldBusManager Interface Tests ====================

    /**
     * Test that FieldBusManager interface has handleRequest method.
     */
    @Test
    public void fieldBusManager_handleRequestMethodExists() {
        FieldBusManager mockFieldBusManager = mock(FieldBusManager.class);
        when(mockFieldBusManager.handleRequest("testQueue", "testRequest")).thenReturn("response");

        Serializable result = mockFieldBusManager.handleRequest("testQueue", "testRequest");

        assertEquals("Should return expected response", "response", result);
    }

    /**
     * Test that FieldBusManager interface has getFieldModelMrid method.
     */
    @Test
    public void fieldBusManager_getFieldModelMridMethodExists() {
        FieldBusManager mockFieldBusManager = mock(FieldBusManager.class);
        when(mockFieldBusManager.getFieldModelMrid()).thenReturn("mrid-12345");

        String result = mockFieldBusManager.getFieldModelMrid();

        assertEquals("Should return expected MRID", "mrid-12345", result);
    }

    // ==================== TimeseriesDataManager Interface Tests
    // ====================

    /**
     * Test that TimeseriesDataManager has all required methods.
     */
    @Test
    public void timeseriesDataManager_allMethodsExist() throws Exception {
        TimeseriesDataManager mockTsManager = mock(TimeseriesDataManager.class);

        // Verify all methods are accessible
        mockTsManager.query(null);
        mockTsManager.storeSimulationOutput("sim-123");
        mockTsManager.storeSimulationInput("sim-123");
        mockTsManager.storeServiceOutput("sim-123", "service-1", "instance-1");
        mockTsManager.storeServiceInput("sim-123", "service-1", "instance-1");
        mockTsManager.storeAppOutput("sim-123", "app-1", "instance-1");
        mockTsManager.storeAppInput("sim-123", "app-1", "instance-1");
        mockTsManager.storeAllData(null);

        // Verify methods were called
        verify(mockTsManager).storeSimulationOutput("sim-123");
        verify(mockTsManager).storeSimulationInput("sim-123");
    }

    /**
     * Test that TimeseriesDataManager query returns Serializable.
     */
    @Test
    public void timeseriesDataManager_queryReturnsSerializable() throws Exception {
        TimeseriesDataManager mockTsManager = mock(TimeseriesDataManager.class);
        when(mockTsManager.query(any())).thenReturn("query-result");

        Serializable result = mockTsManager.query(null);

        assertEquals("Query should return expected result", "query-result", result);
    }

    // ==================== LogDataManager Interface Tests ====================

    /**
     * Test that LogDataManager has store method.
     */
    @Test
    public void logDataManager_storeMethodExists() {
        LogDataManager mockLogDataManager = mock(LogDataManager.class);

        mockLogDataManager.store("source", "process-123", System.currentTimeMillis(),
                "Test message", LogLevel.INFO, ProcessStatus.RUNNING, "user", "SIMULATION");

        verify(mockLogDataManager).store(eq("source"), eq("process-123"), anyLong(),
                eq("Test message"), eq(LogLevel.INFO), eq(ProcessStatus.RUNNING), eq("user"), eq("SIMULATION"));
    }

    /**
     * Test that LogDataManager has query methods.
     */
    @Test
    public void logDataManager_queryMethodsExist() {
        LogDataManager mockLogDataManager = mock(LogDataManager.class);
        when(mockLogDataManager.query("SELECT * FROM logs")).thenReturn("query-result");

        // Test string query
        Serializable result1 = mockLogDataManager.query("SELECT * FROM logs");
        assertEquals("Query should return result", "query-result", result1);

        // Test parameterized query
        mockLogDataManager.query("source", "process", 0L, LogLevel.INFO, ProcessStatus.RUNNING, "user", "type");
        verify(mockLogDataManager).query("source", "process", 0L, LogLevel.INFO, ProcessStatus.RUNNING, "user", "type");
    }

    /**
     * Test that LogDataManager has storeExpectedResults method.
     */
    @Test
    public void logDataManager_storeExpectedResultsMethodExists() {
        LogDataManager mockLogDataManager = mock(LogDataManager.class);

        mockLogDataManager.storeExpectedResults("app-1", "test-1", "process-1", "process-2",
                1000L, 2000L, "mrid-123", "property", "expected", "actual", "up", "diff-mrid", true);

        verify(mockLogDataManager).storeExpectedResults("app-1", "test-1", "process-1", "process-2",
                1000L, 2000L, "mrid-123", "property", "expected", "actual", "up", "diff-mrid", true);
    }

    // ==================== GridAppsDataHandler Interface Tests ====================

    /**
     * Test that GridAppsDataHandler has handle method.
     */
    @Test
    public void gridAppsDataHandler_handleMethodExists() throws Exception {
        GridAppsDataHandler mockHandler = mock(GridAppsDataHandler.class);
        Response mockResponse = mock(Response.class);
        when(mockHandler.handle(any(), anyString(), anyString(), any())).thenReturn(mockResponse);

        Response result = mockHandler.handle("request", "sim-123", "/tmp", mockLogManager);

        assertNotNull("Handle should return a response", result);
    }

    /**
     * Test that GridAppsDataHandler has getDescription method.
     */
    @Test
    public void gridAppsDataHandler_getDescriptionMethodExists() {
        GridAppsDataHandler mockHandler = mock(GridAppsDataHandler.class);
        when(mockHandler.getDescription()).thenReturn("Test handler description");

        String description = mockHandler.getDescription();

        assertEquals("Should return description", "Test handler description", description);
    }

    /**
     * Test that GridAppsDataHandler has getSupportedRequestTypes method.
     */
    @Test
    public void gridAppsDataHandler_getSupportedRequestTypesMethodExists() {
        GridAppsDataHandler mockHandler = mock(GridAppsDataHandler.class);
        List<Class<?>> types = new ArrayList<>();
        types.add(String.class);
        when(mockHandler.getSupportedRequestTypes()).thenReturn(types);

        List<Class<?>> result = mockHandler.getSupportedRequestTypes();

        assertNotNull("Should return list of types", result);
        assertEquals("Should have one type", 1, result.size());
    }

    // ==================== DataManagerHandler Interface Tests ====================

    /**
     * Test that DataManagerHandler has handle method.
     */
    @Test
    public void dataManagerHandler_handleMethodExists() throws Exception {
        DataManagerHandler mockHandler = mock(DataManagerHandler.class);
        when(mockHandler.handle(any(), anyString(), anyString())).thenReturn("handled-result");

        Serializable result = mockHandler.handle("request-content", "process-123", "user");

        assertEquals("Handle should return result", "handled-result", result);
    }

    /**
     * Test that DataManagerHandler handle can throw Exception.
     */
    @Test(expected = Exception.class)
    public void dataManagerHandler_handleCanThrowException() throws Exception {
        DataManagerHandler mockHandler = mock(DataManagerHandler.class);
        when(mockHandler.handle(any(), anyString(), anyString())).thenThrow(new Exception("Test error"));

        mockHandler.handle("request", "process", "user");
    }

    // ==================== ConfigurationHandler Interface Tests
    // ====================

    /**
     * Test that ConfigurationHandler has generateConfig method.
     */
    @Test
    public void configurationHandler_generateConfigMethodExists() throws Exception {
        ConfigurationHandler mockHandler = mock(ConfigurationHandler.class);

        Properties params = new Properties();
        params.setProperty("key", "value");
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        mockHandler.generateConfig(params, pw, "process-123", "user");

        verify(mockHandler).generateConfig(params, pw, "process-123", "user");
    }

    /**
     * Test that ConfigurationHandler can throw Exception.
     */
    @Test(expected = Exception.class)
    public void configurationHandler_generateConfigCanThrowException() throws Exception {
        ConfigurationHandler mockHandler = mock(ConfigurationHandler.class);
        doThrow(new Exception("Config generation failed")).when(mockHandler)
                .generateConfig(any(), any(), anyString(), anyString());

        mockHandler.generateConfig(new Properties(), new PrintWriter(new StringWriter()), "process", "user");
    }

    // ==================== Interface Contract Tests ====================

    /**
     * Test that all interfaces are properly defined as interfaces.
     */
    @Test
    public void allTargetTypes_areInterfaces() {
        assertTrue("RoleManager should be an interface", RoleManager.class.isInterface());
        assertTrue("FieldBusManager should be an interface", FieldBusManager.class.isInterface());
        assertTrue("TimeseriesDataManager should be an interface", TimeseriesDataManager.class.isInterface());
        assertTrue("LogDataManager should be an interface", LogDataManager.class.isInterface());
        assertTrue("GridAppsDataHandler should be an interface", GridAppsDataHandler.class.isInterface());
        assertTrue("DataManagerHandler should be an interface", DataManagerHandler.class.isInterface());
        assertTrue("ConfigurationHandler should be an interface", ConfigurationHandler.class.isInterface());
    }
}
