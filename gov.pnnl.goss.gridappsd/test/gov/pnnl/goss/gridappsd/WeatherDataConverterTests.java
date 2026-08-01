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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.data.conversion.ProvenWeatherToGridlabdWeatherConverter;
import gov.pnnl.goss.gridappsd.dto.TimeSeriesEntryResult;

/**
 * Tests for the ProvenWeatherToGridlabdWeatherConverter.
 *
 * These tests verify that weather data from Proven format is correctly
 * converted to GridLAB-D weather format.
 */
@RunWith(MockitoJUnitRunner.class)
public class WeatherDataConverterTests {

    @Mock
    LogManager mockLogManager;

    @Mock
    DataManager mockDataManager;

    ProvenWeatherToGridlabdWeatherConverter converter;

    // Sample weather data in the format returned by Proven/InfluxDB
    // This is the newer JSON array format that TimeSeriesEntryResult.parse expects
    private static final String SAMPLE_WEATHER_JSON = "[" +
            "{\"Diffuse\":40.006386875,\"AvgWindSpeed\":8.5,\"TowerRH\":86.8," +
            "\"long\":\"105.18 W\",\"TowerDryBulbTemp\":55.5," +
            "\"DATE\":\"1/1/2013\",\"DirectCH1\":70.0402521765," +
            "\"GlobalCM22\":21.037676152399999996,\"AvgWindDirection\":180.0," +
            "\"time\":1357016400,\"place\":\"Solar Radiation Research Laboratory\"," +
            "\"lat\":\"39.74 N\"}," +
            "{\"Diffuse\":30.005538233499999999,\"AvgWindSpeed\":4.2,\"TowerRH\":86.9," +
            "\"long\":\"105.18 W\",\"TowerDryBulbTemp\":54.8," +
            "\"DATE\":\"1/1/2013\",\"DirectCH1\":34.0395396335," +
            "\"GlobalCM22\":55.0369521827,\"AvgWindDirection\":175.0," +
            "\"time\":1357016460,\"place\":\"Solar Radiation Research Laboratory\"," +
            "\"lat\":\"39.74 N\"}" +
            "]";

    @Before
    public void setUp() {
        converter = new ProvenWeatherToGridlabdWeatherConverter(mockLogManager, mockDataManager);
    }

    // ========== Converter Registration Tests ==========

    @Test
    public void start_registersConverterWithDataManager() {
        converter.start();

        verify(mockDataManager).registerConverter(
                ProvenWeatherToGridlabdWeatherConverter.INPUT_FORMAT,
                ProvenWeatherToGridlabdWeatherConverter.OUTPUT_FORMAT,
                converter);
    }

    @Test
    public void start_logsWarningWhenDataManagerNull() {
        ProvenWeatherToGridlabdWeatherConverter converterNoDataManager = new ProvenWeatherToGridlabdWeatherConverter(
                mockLogManager, null);

        converterNoDataManager.start();

        verify(mockLogManager).warn(any(), any(), contains("No Data manager available"));
    }

    // ========== Format Constants Tests ==========

    @Test
    public void inputFormat_isProvenWeather() {
        assertEquals("PROVEN_WEATHER", ProvenWeatherToGridlabdWeatherConverter.INPUT_FORMAT);
    }

    @Test
    public void outputFormat_isGridlabdWeather() {
        assertEquals("GRIDLABD_WEATHER", ProvenWeatherToGridlabdWeatherConverter.OUTPUT_FORMAT);
    }

    // ========== String Conversion Tests ==========

    @Test
    public void convert_stringInput_producesValidOutput() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        converter.convert(SAMPLE_WEATHER_JSON, printWriter, null);

        String output = stringWriter.toString();
        assertNotNull("Output should not be null", output);
        assertFalse("Output should not be empty", output.isEmpty());
    }

    @Test
    public void convert_stringInput_includesHeader() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        converter.convert(SAMPLE_WEATHER_JSON, printWriter, null);

        String output = stringWriter.toString();
        assertTrue("Output should contain header comment", output.contains("#"));
        assertTrue("Output should contain column headers",
                output.contains("temperature,humidity,wind_speed,solar_dir,solar_diff,solar_global"));
    }

    @Test
    public void convert_stringInput_includesWeatherData() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        converter.convert(SAMPLE_WEATHER_JSON, printWriter, null);

        String output = stringWriter.toString();
        String[] lines = output.split("\n");

        // Should have header lines plus data lines
        assertTrue("Should have multiple lines", lines.length > 4);

        // Last data lines should have comma-separated values
        String lastLine = lines[lines.length - 1].trim();
        String[] values = lastLine.split(",");
        assertEquals("Data line should have 7 values (timestamp + 6 weather fields)", 7, values.length);
    }

    // ========== InputStream Conversion Tests ==========

    @Test
    public void convert_inputStream_producesValidOutput() throws Exception {
        InputStream inputStream = new ByteArrayInputStream(
                SAMPLE_WEATHER_JSON.getBytes(StandardCharsets.UTF_8));
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        converter.convert(inputStream, printWriter, null);

        String output = stringWriter.toString();
        assertNotNull("Output should not be null", output);
        assertFalse("Output should not be empty", output.isEmpty());
    }

    @Test
    public void convert_inputStream_matchesStringConversion() throws Exception {
        // Convert via string
        StringWriter stringWriter1 = new StringWriter();
        PrintWriter printWriter1 = new PrintWriter(stringWriter1);
        converter.convert(SAMPLE_WEATHER_JSON, printWriter1, null);

        // Convert via input stream
        InputStream inputStream = new ByteArrayInputStream(
                SAMPLE_WEATHER_JSON.getBytes(StandardCharsets.UTF_8));
        StringWriter stringWriter2 = new StringWriter();
        PrintWriter printWriter2 = new PrintWriter(stringWriter2);
        converter.convert(inputStream, printWriter2, null);

        assertEquals("String and InputStream conversion should produce same output",
                stringWriter1.toString(), stringWriter2.toString());
    }

    // ========== Weather Data Field Tests ==========

    @Test
    public void convert_includesTemperature() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        converter.convert(SAMPLE_WEATHER_JSON, printWriter, null);

        String output = stringWriter.toString();
        // Temperature 55.5 should appear in output
        assertTrue("Output should contain temperature value", output.contains("55.5"));
    }

    @Test
    public void convert_includesHumidity() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        converter.convert(SAMPLE_WEATHER_JSON, printWriter, null);

        String output = stringWriter.toString();
        // Humidity 86.8% should be converted to 0.868 (divided by 100)
        assertTrue("Output should contain normalized humidity", output.contains("0.868"));
    }

    @Test
    public void convert_includesWindSpeed() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        converter.convert(SAMPLE_WEATHER_JSON, printWriter, null);

        String output = stringWriter.toString();
        // Wind speed 8.5 should appear in output
        assertTrue("Output should contain wind speed", output.contains("8.5"));
    }

    @Test
    public void convert_includesSolarValues() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        converter.convert(SAMPLE_WEATHER_JSON, printWriter, null);

        String output = stringWriter.toString();
        // Check for solar direct, diffuse, and global values
        assertTrue("Output should contain solar direct value",
                output.contains("70.0402521765") || output.contains("34.0395396335"));
        assertTrue("Output should contain solar diffuse value",
                output.contains("40.006386875") || output.contains("30.005538233"));
    }

    // ========== TimeSeriesEntryResult Parsing Tests ==========

    @Test
    public void timeSeriesEntryResult_parsesJsonArray() {
        TimeSeriesEntryResult result = TimeSeriesEntryResult.parse(SAMPLE_WEATHER_JSON);

        assertNotNull("Result should not be null", result);
        assertNotNull("Data should not be null", result.getData());
        assertEquals("Should have 2 weather records", 2, result.getData().size());
    }

    @Test
    public void timeSeriesEntryResult_containsExpectedFields() {
        TimeSeriesEntryResult result = TimeSeriesEntryResult.parse(SAMPLE_WEATHER_JSON);

        var firstRecord = result.getData().get(0);
        assertTrue("Record should contain TowerDryBulbTemp", firstRecord.containsKey("TowerDryBulbTemp"));
        assertTrue("Record should contain TowerRH", firstRecord.containsKey("TowerRH"));
        assertTrue("Record should contain AvgWindSpeed", firstRecord.containsKey("AvgWindSpeed"));
        assertTrue("Record should contain DirectCH1", firstRecord.containsKey("DirectCH1"));
        assertTrue("Record should contain Diffuse", firstRecord.containsKey("Diffuse"));
        assertTrue("Record should contain GlobalCM22", firstRecord.containsKey("GlobalCM22"));
        assertTrue("Record should contain time", firstRecord.containsKey("time"));
    }

    // ========== Edge Case Tests ==========

    @Test
    public void convert_handlesMultipleRecords() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        converter.convert(SAMPLE_WEATHER_JSON, printWriter, null);

        String output = stringWriter.toString();
        String[] lines = output.split("\n");

        // Count data lines (lines that start with a date pattern MM:dd:HH:mm:ss)
        int dataLineCount = 0;
        for (String line : lines) {
            if (line.matches("^\\d{2}:\\d{2}:\\d{2}:\\d{2}:\\d{2},.*")) {
                dataLineCount++;
            }
        }
        assertEquals("Should have 2 data lines for 2 records", 2, dataLineCount);
    }

    @Test
    public void convert_singleRecord_producesValidOutput() throws Exception {
        String singleRecordJson = "[" +
                "{\"Diffuse\":40.0,\"AvgWindSpeed\":8.5,\"TowerRH\":86.8," +
                "\"long\":\"105.18 W\",\"TowerDryBulbTemp\":55.5," +
                "\"DATE\":\"1/1/2013\",\"DirectCH1\":70.0," +
                "\"GlobalCM22\":21.0,\"time\":1357016400," +
                "\"place\":\"Test Location\",\"lat\":\"39.74 N\"}" +
                "]";

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        converter.convert(singleRecordJson, printWriter, null);

        String output = stringWriter.toString();
        assertNotNull("Output should not be null", output);
        assertTrue("Output should contain header", output.contains("temperature,humidity"));
    }

    @Test(expected = Exception.class)
    public void convert_invalidJson_throwsException() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        converter.convert("not valid json", printWriter, null);
    }

    @Test(expected = Exception.class)
    public void convert_emptyArray_throwsException() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        converter.convert("[]", printWriter, null);
    }
}
