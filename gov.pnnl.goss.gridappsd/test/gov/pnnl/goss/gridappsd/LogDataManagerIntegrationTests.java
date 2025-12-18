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
import static org.junit.Assume.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;

/**
 * Integration tests for LogDataManager that test actual MySQL database operations.
 *
 * These tests require MySQL to be running on localhost:3306 with:
 * - Database: gridappsd
 * - Username: gridappsd
 * - Password: gridappsd1234
 *
 * To run: Ensure MySQL container is running (docker ps should show mysql container)
 *
 * Tests are skipped if database is not available.
 */
public class LogDataManagerIntegrationTests {

    private static final String MYSQL_URL = "jdbc:mysql://localhost:3306/gridappsd";
    private static final String MYSQL_USER = "gridappsd";
    private static final String MYSQL_PASSWORD = "gridappsd1234";

    private static boolean databaseAvailable = false;
    private Connection connection;

    // Test-specific process ID to avoid conflicts with real data
    private static final String TEST_PROCESS_ID = "test-integration-" + System.currentTimeMillis();

    @BeforeClass
    public static void checkDatabaseAvailable() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD)) {
                databaseAvailable = conn.isValid(5);
            }
        } catch (Exception e) {
            System.out.println("MySQL not available for integration tests: " + e.getMessage());
            databaseAvailable = false;
        }
    }

    @Before
    public void setUp() throws Exception {
        assumeTrue("MySQL database not available - skipping integration tests", databaseAvailable);
        connection = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD);
    }

    @After
    public void tearDown() throws Exception {
        if (connection != null) {
            // Clean up test data
            try {
                PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM gridappsd.log WHERE process_id LIKE 'test-integration-%'");
                ps.executeUpdate();
            } catch (SQLException e) {
                // Ignore cleanup errors
            }
            connection.close();
        }
    }

    // ========== Database Schema Tests ==========

    @Test
    public void logTableExists() throws Exception {
        PreparedStatement ps = connection.prepareStatement(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'gridappsd' AND table_name = 'log'");
        ResultSet rs = ps.executeQuery();
        assertTrue("Result set should have data", rs.next());
        assertEquals("log table should exist", 1, rs.getInt(1));
    }

    @Test
    public void logTableHasRequiredColumns() throws Exception {
        PreparedStatement ps = connection.prepareStatement(
            "SELECT column_name FROM information_schema.columns WHERE table_schema = 'gridappsd' AND table_name = 'log'");
        ResultSet rs = ps.executeQuery();

        java.util.Set<String> columns = new java.util.HashSet<>();
        while (rs.next()) {
            columns.add(rs.getString(1).toLowerCase());
        }

        assertTrue("log table should have 'source' column", columns.contains("source"));
        assertTrue("log table should have 'process_id' column", columns.contains("process_id"));
        assertTrue("log table should have 'timestamp' column", columns.contains("timestamp"));
        assertTrue("log table should have 'log_message' column", columns.contains("log_message"));
        assertTrue("log table should have 'log_level' column", columns.contains("log_level"));
        assertTrue("log table should have 'process_status' column", columns.contains("process_status"));
        assertTrue("log table should have 'username' column", columns.contains("username"));
    }

    // ========== Direct SQL Tests (Testing what LogDataManagerMySQL does) ==========

    @Test
    public void canInsertLogEntry() throws Exception {
        String source = "TestSource";
        String logMessage = "Test log message";
        long timestamp = System.currentTimeMillis();

        PreparedStatement ps = connection.prepareStatement(
            "INSERT INTO gridappsd.log (id, source, process_id, timestamp, log_message, log_level, process_status, username, process_type) " +
            "VALUES (default, ?, ?, ?, ?, ?, ?, ?, ?)");
        ps.setString(1, source);
        ps.setString(2, TEST_PROCESS_ID);
        ps.setTimestamp(3, new java.sql.Timestamp(timestamp));
        ps.setString(4, logMessage);
        ps.setString(5, LogLevel.INFO.toString());
        ps.setString(6, ProcessStatus.RUNNING.toString());
        ps.setString(7, "test-user");
        ps.setString(8, "TEST");

        int rowsAffected = ps.executeUpdate();
        assertEquals("Should insert 1 row", 1, rowsAffected);
    }

    @Test
    public void canQueryLogEntries() throws Exception {
        // First insert a log entry
        String source = "QueryTestSource";
        String logMessage = "Query test message";
        long timestamp = System.currentTimeMillis();

        PreparedStatement insertPs = connection.prepareStatement(
            "INSERT INTO gridappsd.log (id, source, process_id, timestamp, log_message, log_level, process_status, username, process_type) " +
            "VALUES (default, ?, ?, ?, ?, ?, ?, ?, ?)");
        insertPs.setString(1, source);
        insertPs.setString(2, TEST_PROCESS_ID);
        insertPs.setTimestamp(3, new java.sql.Timestamp(timestamp));
        insertPs.setString(4, logMessage);
        insertPs.setString(5, LogLevel.DEBUG.toString());
        insertPs.setString(6, ProcessStatus.STARTED.toString());
        insertPs.setString(7, "test-user");
        insertPs.setString(8, "TEST");
        insertPs.executeUpdate();

        // Now query for it
        PreparedStatement queryPs = connection.prepareStatement(
            "SELECT * FROM gridappsd.log WHERE process_id = ? AND source = ?");
        queryPs.setString(1, TEST_PROCESS_ID);
        queryPs.setString(2, source);
        ResultSet rs = queryPs.executeQuery();

        assertTrue("Should find the inserted log entry", rs.next());
        assertEquals("Source should match", source, rs.getString("source"));
        assertEquals("Process ID should match", TEST_PROCESS_ID, rs.getString("process_id"));
        assertEquals("Log message should match", logMessage, rs.getString("log_message"));
        assertEquals("Log level should match", LogLevel.DEBUG.toString(), rs.getString("log_level"));
    }

    @Test
    public void canQueryByLogLevel() throws Exception {
        // Insert entries with different log levels
        insertTestLog("Source1", "Debug message", LogLevel.DEBUG);
        insertTestLog("Source2", "Info message", LogLevel.INFO);
        insertTestLog("Source3", "Error message", LogLevel.ERROR);

        // Query by log level
        PreparedStatement ps = connection.prepareStatement(
            "SELECT * FROM gridappsd.log WHERE process_id = ? AND log_level = ?");
        ps.setString(1, TEST_PROCESS_ID);
        ps.setString(2, LogLevel.ERROR.toString());
        ResultSet rs = ps.executeQuery();

        assertTrue("Should find ERROR log entry", rs.next());
        assertEquals("Source should be Source3", "Source3", rs.getString("source"));
        assertFalse("Should only find one ERROR entry", rs.next());
    }

    @Test
    public void canQueryByProcessStatus() throws Exception {
        // Insert entries with different statuses
        insertTestLogWithStatus("Source1", "Started message", ProcessStatus.STARTED);
        insertTestLogWithStatus("Source2", "Running message", ProcessStatus.RUNNING);
        insertTestLogWithStatus("Source3", "Complete message", ProcessStatus.COMPLETE);

        // Query by process status
        PreparedStatement ps = connection.prepareStatement(
            "SELECT * FROM gridappsd.log WHERE process_id = ? AND process_status = ?");
        ps.setString(1, TEST_PROCESS_ID);
        ps.setString(2, ProcessStatus.RUNNING.toString());
        ResultSet rs = ps.executeQuery();

        assertTrue("Should find RUNNING log entry", rs.next());
        assertEquals("Source should be Source2", "Source2", rs.getString("source"));
    }

    @Test
    public void resultSetCanBeConvertedToJson() throws Exception {
        // Insert some test data
        insertTestLog("JsonSource", "Json test message", LogLevel.INFO);

        // Query and convert to JSON (similar to what LogDataManagerMySQL.getJSONFromResultSet does)
        PreparedStatement ps = connection.prepareStatement(
            "SELECT source, process_id, log_message, log_level FROM gridappsd.log WHERE process_id = ?");
        ps.setString(1, TEST_PROCESS_ID);
        ResultSet rs = ps.executeQuery();

        java.util.List<java.util.Map<String, Object>> list = new java.util.ArrayList<>();
        java.sql.ResultSetMetaData metaData = rs.getMetaData();

        while (rs.next()) {
            java.util.Map<String, Object> columnMap = new java.util.HashMap<>();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                String value = rs.getString(metaData.getColumnName(i));
                columnMap.put(metaData.getColumnLabel(i), value != null ? value : "");
            }
            list.add(columnMap);
        }

        String json = new Gson().toJson(list);
        assertNotNull("JSON should not be null", json);
        assertTrue("JSON should contain source", json.contains("JsonSource"));
        assertTrue("JSON should be valid JSON array", json.startsWith("["));

        // Verify it's parseable
        JsonElement element = JsonParser.parseString(json);
        assertTrue("Should parse as JSON array", element.isJsonArray());
        JsonArray array = element.getAsJsonArray();
        assertTrue("Array should have at least one element", array.size() >= 1);
    }

    @Test
    public void canHandleSpecialCharactersInLogMessage() throws Exception {
        String specialMessage = "Test with 'quotes' and \"double quotes\" and \\ backslash";
        insertTestLog("SpecialCharSource", specialMessage, LogLevel.INFO);

        PreparedStatement ps = connection.prepareStatement(
            "SELECT log_message FROM gridappsd.log WHERE process_id = ? AND source = ?");
        ps.setString(1, TEST_PROCESS_ID);
        ps.setString(2, "SpecialCharSource");
        ResultSet rs = ps.executeQuery();

        assertTrue("Should find entry", rs.next());
        assertEquals("Message with special chars should be stored correctly", specialMessage, rs.getString("log_message"));
    }

    @Test
    public void canHandleLongLogMessages() throws Exception {
        // Create a long message (but within reasonable limits)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("This is line ").append(i).append(" of a long log message. ");
        }
        String longMessage = sb.toString();

        insertTestLog("LongMsgSource", longMessage.substring(0, Math.min(longMessage.length(), 10000)), LogLevel.INFO);

        PreparedStatement ps = connection.prepareStatement(
            "SELECT log_message FROM gridappsd.log WHERE process_id = ? AND source = ?");
        ps.setString(1, TEST_PROCESS_ID);
        ps.setString(2, "LongMsgSource");
        ResultSet rs = ps.executeQuery();

        assertTrue("Should find entry with long message", rs.next());
        String retrieved = rs.getString("log_message");
        assertNotNull("Retrieved message should not be null", retrieved);
        assertTrue("Retrieved message should be substantial", retrieved.length() > 100);
    }

    // ========== Helper Methods ==========

    private void insertTestLog(String source, String message, LogLevel level) throws SQLException {
        insertTestLogWithStatus(source, message, level, ProcessStatus.RUNNING);
    }

    private void insertTestLogWithStatus(String source, String message, ProcessStatus status) throws SQLException {
        insertTestLogWithStatus(source, message, LogLevel.INFO, status);
    }

    private void insertTestLogWithStatus(String source, String message, LogLevel level, ProcessStatus status) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
            "INSERT INTO gridappsd.log (id, source, process_id, timestamp, log_message, log_level, process_status, username, process_type) " +
            "VALUES (default, ?, ?, ?, ?, ?, ?, ?, ?)");
        ps.setString(1, source);
        ps.setString(2, TEST_PROCESS_ID);
        ps.setTimestamp(3, new java.sql.Timestamp(System.currentTimeMillis()));
        ps.setString(4, message);
        ps.setString(5, level.toString());
        ps.setString(6, status.toString());
        ps.setString(7, "test-user");
        ps.setString(8, "TEST");
        ps.executeUpdate();
    }
}
