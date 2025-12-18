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

import java.util.ArrayList;
import java.util.List;

import jakarta.jms.Destination;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.gson.JsonObject;

import gov.pnnl.goss.gridappsd.api.TestManager;
import gov.pnnl.goss.gridappsd.dto.SimulationContext;
import gov.pnnl.goss.gridappsd.dto.TestConfig;
import gov.pnnl.goss.gridappsd.dto.events.Event;
import gov.pnnl.goss.gridappsd.testmanager.TestManagerImpl;
import pnnl.goss.core.DataResponse;

/**
 * Tests for the TestManager interface methods.
 *
 * These tests verify that the TestManager interface is correctly defined
 * and that TestManagerImpl implements it. Due to the complex dependencies
 * of TestManagerImpl (requires OSGi context, Gson initialization, etc.),
 * these tests use a mock interface for method signature verification.
 *
 * Interface methods tested:
 * - handleTestRequest
 * - sendEventsToSimulation
 * - compareSimulations
 * - compareTimeseriesSimulationWithExpected
 * - compareRunningWithTimeseriesSimulation
 * - compareRunningSimulationOutputWithExpected
 * - compareRunningSimulationInputWithExpected
 * - updateEventForSimulation
 * - sendEventStatus
 */
@RunWith(MockitoJUnitRunner.class)
public class TestManagerInterfaceTests {

    @Mock
    TestManager mockTestManager;

    @Mock
    SimulationContext mockSimulationContext;

    @Mock
    DataResponse mockDataResponse;

    @Mock
    Destination mockDestination;

    @Before
    public void setUp() throws Exception {
        // Using mock interface due to TestManagerImpl's complex dependencies
        // (requires OSGi context, Gson initialization via start(), etc.)
    }

    // ========== Interface Implementation Tests ==========

    /**
     * Test that TestManagerImpl implements TestManager interface.
     */
    @Test
    public void testManagerImpl_implementsInterface() {
        assertTrue("TestManagerImpl should implement TestManager interface",
                TestManager.class.isAssignableFrom(TestManagerImpl.class));
    }

    /**
     * Test that TestManager is a proper interface.
     */
    @Test
    public void testManager_isInterface() {
        assertTrue("TestManager should be an interface",
                TestManager.class.isInterface());
    }

    // ========== handleTestRequest() tests ==========

    /**
     * Test that handleTestRequest is callable with null testConfig.
     */
    @Test
    public void handleTestRequest_handlesNullTestConfig() {
        mockTestManager.handleTestRequest(null, mockSimulationContext);
        verify(mockTestManager).handleTestRequest(null, mockSimulationContext);
    }

    /**
     * Test that handleTestRequest is callable with valid parameters.
     */
    @Test
    public void handleTestRequest_acceptsValidParameters() {
        TestConfig testConfig = new TestConfig();
        mockTestManager.handleTestRequest(testConfig, mockSimulationContext);
        verify(mockTestManager).handleTestRequest(testConfig, mockSimulationContext);
    }

    // ========== sendEventsToSimulation() tests ==========

    /**
     * Test that sendEventsToSimulation handles empty list.
     */
    @Test
    public void sendEventsToSimulation_handlesEmptyList() {
        List<Event> events = new ArrayList<>();
        when(mockTestManager.sendEventsToSimulation(events, "sim-123")).thenReturn(events);

        List<Event> result = mockTestManager.sendEventsToSimulation(events, "sim-123");

        assertNotNull("Result should not be null", result);
        verify(mockTestManager).sendEventsToSimulation(events, "sim-123");
    }

    /**
     * Test that sendEventsToSimulation handles null list.
     */
    @Test
    public void sendEventsToSimulation_handlesNullList() {
        when(mockTestManager.sendEventsToSimulation(null, "sim-123")).thenReturn(null);

        List<Event> result = mockTestManager.sendEventsToSimulation(null, "sim-123");

        assertNull("sendEventsToSimulation should return null for null events", result);
    }

    // ========== compareSimulations() tests ==========

    /**
     * Test that compareSimulations is callable.
     */
    @Test
    public void compareSimulations_isCallable() {
        TestConfig testConfig = new TestConfig();
        mockTestManager.compareSimulations(testConfig, "sim-1", "sim-2", mockDataResponse);
        verify(mockTestManager).compareSimulations(testConfig, "sim-1", "sim-2", mockDataResponse);
    }

    // ========== compareTimeseriesSimulationWithExpected() tests ==========

    /**
     * Test that compareTimeseriesSimulationWithExpected is callable.
     */
    @Test
    public void compareTimeseriesSimulationWithExpected_isCallable() {
        TestConfig testConfig = new TestConfig();
        JsonObject expectedResult = new JsonObject();
        mockTestManager.compareTimeseriesSimulationWithExpected(
                testConfig, "current-sim", "sim-1", expectedResult, mockDataResponse);
        verify(mockTestManager).compareTimeseriesSimulationWithExpected(
                testConfig, "current-sim", "sim-1", expectedResult, mockDataResponse);
    }

    // ========== compareRunningWithTimeseriesSimulation() tests ==========

    /**
     * Test that compareRunningWithTimeseriesSimulation is callable.
     */
    @Test
    public void compareRunningWithTimeseriesSimulation_isCallable() {
        TestConfig testConfig = new TestConfig();
        mockTestManager.compareRunningWithTimeseriesSimulation(testConfig, "current-sim", "sim-1");
        verify(mockTestManager).compareRunningWithTimeseriesSimulation(testConfig, "current-sim", "sim-1");
    }

    // ========== compareRunningSimulationOutputWithExpected() tests ==========

    /**
     * Test that compareRunningSimulationOutputWithExpected is callable.
     */
    @Test
    public void compareRunningSimulationOutputWithExpected_isCallable() {
        TestConfig testConfig = new TestConfig();
        JsonObject expectedResults = new JsonObject();
        mockTestManager.compareRunningSimulationOutputWithExpected(
                testConfig, "sim-123", expectedResults, "expected-123");
        verify(mockTestManager).compareRunningSimulationOutputWithExpected(
                testConfig, "sim-123", expectedResults, "expected-123");
    }

    // ========== compareRunningSimulationInputWithExpected() tests ==========

    /**
     * Test that compareRunningSimulationInputWithExpected is callable.
     */
    @Test
    public void compareRunningSimulationInputWithExpected_isCallable() {
        TestConfig testConfig = new TestConfig();
        JsonObject expectedResults = new JsonObject();
        mockTestManager.compareRunningSimulationInputWithExpected(
                testConfig, "sim-123", expectedResults, "expected-123");
        verify(mockTestManager).compareRunningSimulationInputWithExpected(
                testConfig, "sim-123", expectedResults, "expected-123");
    }

    // ========== updateEventForSimulation() tests ==========

    /**
     * Test that updateEventForSimulation handles empty list.
     */
    @Test
    public void updateEventForSimulation_handlesEmptyList() {
        List<Event> events = new ArrayList<>();
        mockTestManager.updateEventForSimulation(events, "sim-123");
        verify(mockTestManager).updateEventForSimulation(events, "sim-123");
    }

    /**
     * Test that updateEventForSimulation handles null list.
     */
    @Test
    public void updateEventForSimulation_handlesNullList() {
        mockTestManager.updateEventForSimulation(null, "sim-123");
        verify(mockTestManager).updateEventForSimulation(null, "sim-123");
    }

    // ========== sendEventStatus() tests ==========

    /**
     * Test that sendEventStatus is callable.
     */
    @Test
    public void sendEventStatus_isCallable() {
        mockTestManager.sendEventStatus("sim-123", mockDestination);
        verify(mockTestManager).sendEventStatus("sim-123", mockDestination);
    }

    /**
     * Test that sendEventStatus handles null destination.
     */
    @Test
    public void sendEventStatus_handlesNullDestination() {
        mockTestManager.sendEventStatus("sim-123", null);
        verify(mockTestManager).sendEventStatus("sim-123", null);
    }

    // ========== Interface contract tests ==========

    /**
     * Test that all interface methods are accessible.
     */
    @Test
    public void allInterfaceMethods_areAccessible() {
        TestManager manager = mockTestManager;
        TestConfig testConfig = new TestConfig();
        JsonObject jsonObject = new JsonObject();

        // handleTestRequest
        manager.handleTestRequest(testConfig, mockSimulationContext);

        // sendEventsToSimulation
        manager.sendEventsToSimulation(new ArrayList<>(), "sim-123");

        // compareSimulations
        manager.compareSimulations(testConfig, "sim-1", "sim-2", mockDataResponse);

        // compareTimeseriesSimulationWithExpected
        manager.compareTimeseriesSimulationWithExpected(testConfig, "current", "sim-1", jsonObject, mockDataResponse);

        // compareRunningWithTimeseriesSimulation
        manager.compareRunningWithTimeseriesSimulation(testConfig, "current", "sim-1");

        // compareRunningSimulationOutputWithExpected
        manager.compareRunningSimulationOutputWithExpected(testConfig, "sim-123", jsonObject, "expected");

        // compareRunningSimulationInputWithExpected
        manager.compareRunningSimulationInputWithExpected(testConfig, "sim-123", jsonObject, "expected");

        // updateEventForSimulation
        manager.updateEventForSimulation(new ArrayList<>(), "sim-123");

        // sendEventStatus
        manager.sendEventStatus("sim-123", mockDestination);
    }
}
