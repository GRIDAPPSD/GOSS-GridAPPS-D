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

import static gov.pnnl.goss.gridappsd.TestConstants.REQUEST_SIMULATION_CONFIG;
import static org.junit.Assert.assertNotNull;

import java.util.Random;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import gov.pnnl.goss.gridappsd.api.AppManager;
import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.ServiceManager;
import gov.pnnl.goss.gridappsd.api.SimulationManager;
import gov.pnnl.goss.gridappsd.api.TestManager;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.dto.RequestSimulation;
import gov.pnnl.goss.gridappsd.process.ProcessNewSimulationRequest;
import pnnl.goss.core.DataResponse;

/**
 * Tests for ProcessNewSimulationRequest component.
 *
 * These tests verify that the ProcessNewSimulationRequest correctly: - Parses
 * and validates simulation requests - Logs appropriate messages during
 * processing - Handles error conditions gracefully
 */
@RunWith(MockitoJUnitRunner.class)
public class ProcessNewSimulationRequestComponentTests {

    @Captor
    ArgumentCaptor<String> stringCaptor;

    @Captor
    ArgumentCaptor<ProcessStatus> statusCaptor;

    @Mock
    LogManager logManager;
    @Mock
    ConfigurationManager configurationManager;
    @Mock
    SimulationManager simulationManager;
    @Mock
    DataResponse event;
    @Mock
    AppManager appManager;
    @Mock
    ServiceManager serviceManager;
    @Mock
    TestManager testManager;
    @Mock
    DataManager dataManager;

    /**
     * Test that a valid simulation request is parsed and logged correctly. Verifies
     * that info logging is called when processing starts.
     */
    @Test
    public void callsMadeWhen_processStarted() throws Exception {
        String simulationId = Integer.toString(Math.abs(new Random().nextInt()));
        ProcessNewSimulationRequest request = new ProcessNewSimulationRequest(logManager);
        RequestSimulation requestSimulation = RequestSimulation.parse(REQUEST_SIMULATION_CONFIG);

        assertNotNull("RequestSimulation should be parsed successfully", requestSimulation);
        assertNotNull("Power system configs should not be null", requestSimulation.getPower_system_config());

        request.process(configurationManager, simulationManager, simulationId, event, requestSimulation, appManager,
                serviceManager, testManager, dataManager, TestConstants.SYSTEM_USER_NAME);

        // Verify that info logging was called with the simulation request
        Mockito.verify(logManager, Mockito.atLeastOnce()).info(
                Mockito.eq(ProcessStatus.RUNNING),
                Mockito.eq(simulationId),
                Mockito.contains("Parsed simulation request"));
    }

    /**
     * Test that an error is logged when power system config has invalid data.
     */
    @Test
    public void callsMadeWhen_processError() throws Exception {
        String simulationId = Integer.toString(Math.abs(new Random().nextInt()));
        ProcessNewSimulationRequest request = new ProcessNewSimulationRequest(logManager);
        RequestSimulation requestSimulation = RequestSimulation.parse(REQUEST_SIMULATION_CONFIG);

        // Modify the config to cause an error (invalid region name)
        requestSimulation.getPower_system_config().get(0).setGeographicalRegion_name("Bad");

        request.process(configurationManager, simulationManager, simulationId, event, requestSimulation, appManager,
                serviceManager, testManager, dataManager, TestConstants.SYSTEM_USER_NAME);

        // Verify that error logging was called
        Mockito.verify(logManager, Mockito.atLeastOnce()).error(
                Mockito.eq(ProcessStatus.ERROR),
                Mockito.eq(simulationId),
                Mockito.anyString());
    }

    /**
     * Test that an error is logged when null config is passed.
     */
    @Test
    public void callsMadeWhen_processErrorBecauseNullConfig() throws Exception {
        String simulationId = Integer.toString(Math.abs(new Random().nextInt()));
        ProcessNewSimulationRequest request = new ProcessNewSimulationRequest(logManager);

        // Pass null config to trigger error
        request.process(configurationManager, simulationManager, simulationId, event, null, appManager, serviceManager,
                testManager, dataManager, TestConstants.SYSTEM_USER_NAME);

        // Verify that info logging was called with "Invalid simulation request"
        Mockito.verify(logManager, Mockito.atLeastOnce()).info(
                Mockito.eq(ProcessStatus.RUNNING),
                Mockito.eq(simulationId),
                Mockito.contains("Invalid simulation request"));
    }

    /**
     * Test that an error is logged when GRIDAPPSD_TEMP_PATH is not configured. When
     * configurationManager.getSimulationFile() returns null, the simulation cannot
     * create its working directory.
     */
    @Test
    public void callsMadeWhen_processErrorBecauseNullSimulationFile() throws Exception {
        String simulationId = Integer.toString(Math.abs(new Random().nextInt()));
        ProcessNewSimulationRequest request = new ProcessNewSimulationRequest(logManager);
        RequestSimulation requestSimulation = RequestSimulation.parse(REQUEST_SIMULATION_CONFIG);

        request.process(configurationManager, simulationManager, simulationId, event, requestSimulation, appManager,
                serviceManager, testManager, dataManager, TestConstants.SYSTEM_USER_NAME);

        // Verify that error logging was called for GRIDAPPSD_TEMP_PATH not configured
        Mockito.verify(logManager, Mockito.atLeastOnce()).error(
                Mockito.eq(ProcessStatus.ERROR),
                Mockito.eq(simulationId),
                Mockito.contains("GRIDAPPSD_TEMP_PATH not configured"));
    }

    /**
     * Test that RequestSimulation can be properly parsed from JSON.
     */
    @Test
    public void requestSimulation_canBeParsedFromJson() {
        RequestSimulation requestSimulation = RequestSimulation.parse(REQUEST_SIMULATION_CONFIG);

        assertNotNull("RequestSimulation should not be null", requestSimulation);
        assertNotNull("Simulation config should not be null", requestSimulation.getSimulation_config());
        assertNotNull("Power system configs should not be null", requestSimulation.getPower_system_config());
        assertNotNull("Application config should not be null", requestSimulation.getApplication_config());
    }

}
