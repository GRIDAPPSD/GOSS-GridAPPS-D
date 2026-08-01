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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.Properties;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.configuration.GLDZiploadScheduleConfigurationHandler;
import gov.pnnl.goss.gridappsd.data.conversion.ProvenLoadScheduleToGridlabdLoadScheduleConverter;
import gov.pnnl.goss.gridappsd.data.conversion.ProvenWeatherToGridlabdWeatherConverter;
import gov.pnnl.goss.gridappsd.dto.RequestTimeseriesDataBasic;
import pnnl.goss.core.DataResponse;

/**
 * Tests for verifying that configuration handlers use the correct response
 * format constants from the data converters, not hardcoded strings.
 *
 * This is critical for the converter lookup to work correctly. The converters
 * register with format strings like "GRIDLABD_WEATHER" and
 * "GRIDLABD_LOAD_SCHEDULE", so the handlers must use these exact strings (via
 * the constants) to match.
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigurationHandlerResponseFormatTests {

    @Mock
    private LogManager logManager;

    @Mock
    private DataManager dataManager;

    @Mock
    private ConfigurationManager configManager;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    /**
     * Test that GLDZiploadScheduleConfigurationHandler uses the correct
     * OUTPUT_FORMAT constant from
     * ProvenLoadScheduleToGridlabdLoadScheduleConverter.
     *
     * Expected format: "GRIDLABD_LOAD_SCHEDULE" Bug: Code was using hardcoded
     * "GridLAB-D" which doesn't match the converter registration.
     */
    @Test
    public void testGLDZiploadScheduleConfigurationHandler_usesCorrectResponseFormat() throws Exception {
        // Setup
        GLDZiploadScheduleConfigurationHandler handler = new GLDZiploadScheduleConfigurationHandler(
                logManager, dataManager);

        File tempDir = tempFolder.newFolder("simulation");
        Properties params = new Properties();
        params.setProperty("directory", tempDir.getAbsolutePath());
        params.setProperty("schedule_name", "ieeezipload");
        params.setProperty("simulation_start_time", "1546300800");
        params.setProperty("simulation_duration", "3600");
        params.setProperty("simulation_id", "12345");

        // Mock dataManager to capture the request
        ArgumentCaptor<RequestTimeseriesDataBasic> requestCaptor = ArgumentCaptor
                .forClass(RequestTimeseriesDataBasic.class);
        DataResponse mockResponse = mock(DataResponse.class);
        when(mockResponse.getData()).thenReturn("test data");
        when(dataManager.processDataRequest(
                requestCaptor.capture(),
                eq("timeseries"),
                anyString(),
                anyString(),
                anyString())).thenReturn(mockResponse);

        // Execute
        handler.generateConfig(params, null, "test-process", "test-user");

        // Verify that the request uses the correct OUTPUT_FORMAT from the converter
        RequestTimeseriesDataBasic capturedRequest = requestCaptor.getValue();
        assertEquals(
                "Response format should match converter's OUTPUT_FORMAT constant",
                ProvenLoadScheduleToGridlabdLoadScheduleConverter.OUTPUT_FORMAT,
                capturedRequest.getResponseFormat());

        // Also verify it's the expected string value
        assertEquals(
                "Response format should be GRIDLABD_LOAD_SCHEDULE",
                "GRIDLABD_LOAD_SCHEDULE",
                capturedRequest.getResponseFormat());
    }

    /**
     * Verify that the converter constants have the expected values. This test
     * documents the expected format strings.
     */
    @Test
    public void testConverterConstants_haveExpectedValues() {
        assertEquals("ProvenWeatherToGridlabdWeatherConverter.OUTPUT_FORMAT should be GRIDLABD_WEATHER",
                "GRIDLABD_WEATHER",
                ProvenWeatherToGridlabdWeatherConverter.OUTPUT_FORMAT);

        assertEquals("ProvenLoadScheduleToGridlabdLoadScheduleConverter.OUTPUT_FORMAT should be GRIDLABD_LOAD_SCHEDULE",
                "GRIDLABD_LOAD_SCHEDULE",
                ProvenLoadScheduleToGridlabdLoadScheduleConverter.OUTPUT_FORMAT);

        assertEquals("ProvenWeatherToGridlabdWeatherConverter.INPUT_FORMAT should be PROVEN_WEATHER",
                "PROVEN_WEATHER",
                ProvenWeatherToGridlabdWeatherConverter.INPUT_FORMAT);

        assertEquals("ProvenLoadScheduleToGridlabdLoadScheduleConverter.INPUT_FORMAT should be PROVEN_loadprofile",
                "PROVEN_loadprofile",
                ProvenLoadScheduleToGridlabdLoadScheduleConverter.INPUT_FORMAT);
    }

    /**
     * Test that demonstrates the mismatch between hardcoded "GridLAB-D" and the
     * actual converter OUTPUT_FORMAT constants.
     *
     * This test would have caught the bug where handlers used "GridLAB-D" instead
     * of the proper constants.
     */
    @Test
    public void testHardcodedStringDoesNotMatchConverterFormat() {
        String hardcodedValue = "GridLAB-D";

        assertNotEquals("Hardcoded 'GridLAB-D' should NOT match weather converter format",
                hardcodedValue,
                ProvenWeatherToGridlabdWeatherConverter.OUTPUT_FORMAT);

        assertNotEquals("Hardcoded 'GridLAB-D' should NOT match load schedule converter format",
                hardcodedValue,
                ProvenLoadScheduleToGridlabdLoadScheduleConverter.OUTPUT_FORMAT);
    }
}
