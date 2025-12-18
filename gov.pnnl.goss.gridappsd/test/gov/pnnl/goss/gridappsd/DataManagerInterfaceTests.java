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

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.api.DataManagerHandler;
import gov.pnnl.goss.gridappsd.api.GridAppsDataHandler;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.data.DataManagerImpl;
import gov.pnnl.goss.gridappsd.data.conversion.DataFormatConverter;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.Response;

/**
 * Tests for the DataManager interface methods.
 *
 * These tests verify that the DataManagerImpl correctly implements
 * all methods defined in the DataManager interface:
 * - getHandlers
 * - getHandler
 * - getAllHandlers
 * - registerHandler
 * - registerDataManagerHandler
 * - processDataRequest
 * - registerConverter
 * - getConverter
 */
@RunWith(MockitoJUnitRunner.class)
public class DataManagerInterfaceTests {

    @Mock
    ClientFactory mockClientFactory;

    @Mock
    LogManager mockLogManager;

    @Mock
    GridAppsDataHandler mockDataHandler;

    @Mock
    DataManagerHandler mockDataManagerHandler;

    @Mock
    DataFormatConverter mockConverter;

    DataManagerImpl dataManager;

    @Before
    public void setUp() throws Exception {
        dataManager = new DataManagerImpl(mockClientFactory, mockLogManager);
    }

    // ========== getHandlers() tests ==========

    /**
     * Test that getHandlers returns null when no handlers are registered for the class.
     */
    @Test
    public void getHandlers_returnsNullWhenNoHandlersRegistered() {
        List<GridAppsDataHandler> handlers = dataManager.getHandlers(String.class);
        assertNull("getHandlers should return null when no handlers registered", handlers);
    }

    /**
     * Test that getHandlers returns handlers after registration.
     */
    @Test
    public void getHandlers_returnsHandlersAfterRegistration() {
        dataManager.registerHandler(mockDataHandler, String.class);

        List<GridAppsDataHandler> handlers = dataManager.getHandlers(String.class);
        assertNotNull("getHandlers should return non-null after registration", handlers);
        assertEquals("getHandlers should return one handler", 1, handlers.size());
        assertEquals("getHandlers should return the registered handler", mockDataHandler, handlers.get(0));
    }

    // ========== getHandler() tests ==========

    /**
     * Test that getHandler returns null (currently not implemented).
     */
    @Test
    public void getHandler_returnsNull() {
        GridAppsDataHandler handler = dataManager.getHandler(String.class, GridAppsDataHandler.class);
        assertNull("getHandler should return null (not implemented)", handler);
    }

    // ========== getAllHandlers() tests ==========

    /**
     * Test that getAllHandlers returns empty list when no handlers are registered.
     */
    @Test
    public void getAllHandlers_returnsEmptyListWhenNoHandlers() {
        List<GridAppsDataHandler> handlers = dataManager.getAllHandlers();
        assertNotNull("getAllHandlers should return non-null list", handlers);
        assertTrue("getAllHandlers should return empty list when no handlers", handlers.isEmpty());
    }

    /**
     * Test that getAllHandlers returns all registered handlers.
     */
    @Test
    public void getAllHandlers_returnsAllRegisteredHandlers() {
        GridAppsDataHandler handler1 = mock(GridAppsDataHandler.class);
        GridAppsDataHandler handler2 = mock(GridAppsDataHandler.class);

        dataManager.registerHandler(handler1, String.class);
        dataManager.registerHandler(handler2, Integer.class);

        List<GridAppsDataHandler> handlers = dataManager.getAllHandlers();
        assertNotNull("getAllHandlers should return non-null list", handlers);
        assertEquals("getAllHandlers should return all handlers", 2, handlers.size());
        assertTrue("getAllHandlers should contain handler1", handlers.contains(handler1));
        assertTrue("getAllHandlers should contain handler2", handlers.contains(handler2));
    }

    // ========== registerHandler() tests ==========

    /**
     * Test that registerHandler successfully registers a handler.
     */
    @Test
    public void registerHandler_registersHandler() {
        dataManager.registerHandler(mockDataHandler, String.class);

        List<GridAppsDataHandler> handlers = dataManager.getHandlers(String.class);
        assertNotNull("Handlers list should not be null after registration", handlers);
        assertTrue("Handlers list should contain the registered handler", handlers.contains(mockDataHandler));
    }

    /**
     * Test that registerHandler allows multiple handlers for same class.
     */
    @Test
    public void registerHandler_allowsMultipleHandlersPerClass() {
        GridAppsDataHandler handler1 = mock(GridAppsDataHandler.class);
        GridAppsDataHandler handler2 = mock(GridAppsDataHandler.class);

        dataManager.registerHandler(handler1, String.class);
        dataManager.registerHandler(handler2, String.class);

        List<GridAppsDataHandler> handlers = dataManager.getHandlers(String.class);
        assertEquals("Should have 2 handlers registered for same class", 2, handlers.size());
    }

    // ========== registerDataManagerHandler() tests ==========

    /**
     * Test that registerDataManagerHandler successfully registers a handler.
     */
    @Test
    public void registerDataManagerHandler_registersHandler() {
        // No direct way to verify registration, but should not throw
        dataManager.registerDataManagerHandler(mockDataManagerHandler, "testType");
    }

    // ========== processDataRequest() tests ==========

    /**
     * Test that processDataRequest returns null when no handlers match.
     */
    @Test
    public void processDataRequest_returnsNullWhenNoHandlers() throws Exception {
        Response response = dataManager.processDataRequest("test-request", null, "sim-123", "/tmp", "user");
        assertNull("processDataRequest should return null when no handlers match", response);
    }

    /**
     * Test that processDataRequest uses registered DataManagerHandler.
     */
    @Test
    public void processDataRequest_usesDataManagerHandler() throws Exception {
        when(mockDataManagerHandler.handle(any(), anyString(), anyString())).thenReturn("test-result");

        dataManager.registerDataManagerHandler(mockDataManagerHandler, "testType");

        Response response = dataManager.processDataRequest("test-request", "testType", "sim-123", "/tmp", "user");
        assertNotNull("processDataRequest should return response when handler matches", response);

        verify(mockDataManagerHandler).handle(eq("test-request"), eq("sim-123"), eq("user"));
    }

    // ========== registerConverter() tests ==========

    /**
     * Test that registerConverter successfully registers a converter.
     */
    @Test
    public void registerConverter_registersConverter() {
        dataManager.registerConverter("json", "xml", mockConverter);

        DataFormatConverter converter = dataManager.getConverter("json", "xml");
        assertEquals("getConverter should return registered converter", mockConverter, converter);
    }

    /**
     * Test that registerConverter is case-insensitive.
     */
    @Test
    public void registerConverter_isCaseInsensitive() {
        dataManager.registerConverter("JSON", "XML", mockConverter);

        DataFormatConverter converter = dataManager.getConverter("json", "xml");
        assertEquals("getConverter should be case-insensitive", mockConverter, converter);
    }

    // ========== getConverter() tests ==========

    /**
     * Test that getConverter returns null when no converter is registered.
     */
    @Test
    public void getConverter_returnsNullWhenNotRegistered() {
        DataFormatConverter converter = dataManager.getConverter("unknown", "format");
        assertNull("getConverter should return null when no converter registered", converter);
    }

    /**
     * Test that getConverter logs warning when converter not found.
     */
    @Test
    public void getConverter_logsWarningWhenNotFound() {
        dataManager.getConverter("unknown", "format");

        verify(mockLogManager).warn(any(), any(), contains("No Data converter available"));
    }

    // ========== Interface contract tests ==========

    /**
     * Test that DataManagerImpl implements DataManager interface.
     */
    @Test
    public void dataManagerImpl_implementsInterface() {
        assertTrue("DataManagerImpl should implement DataManager interface",
                dataManager instanceof DataManager);
    }

    /**
     * Test that all interface methods are accessible.
     */
    @Test
    public void allInterfaceMethods_areAccessible() throws Exception {
        DataManager manager = dataManager;

        // getHandlers
        manager.getHandlers(String.class);

        // getHandler
        manager.getHandler(String.class, GridAppsDataHandler.class);

        // getAllHandlers
        assertNotNull(manager.getAllHandlers());

        // registerHandler
        manager.registerHandler(mockDataHandler, String.class);

        // registerDataManagerHandler
        manager.registerDataManagerHandler(mockDataManagerHandler, "test");

        // registerConverter
        manager.registerConverter("in", "out", mockConverter);

        // getConverter
        manager.getConverter("in", "out");
    }
}
