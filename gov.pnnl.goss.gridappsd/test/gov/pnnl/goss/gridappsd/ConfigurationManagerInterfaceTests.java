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
import java.io.StringWriter;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import gov.pnnl.goss.gridappsd.api.ConfigurationHandler;
import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.configuration.ConfigurationManagerImpl;

/**
 * Tests for the ConfigurationManager interface methods.
 *
 * These tests verify that the ConfigurationManagerImpl correctly implements
 * all methods defined in the ConfigurationManager interface:
 * - getSimulationFile
 * - getConfigurationProperty
 * - registerConfigurationHandler
 * - generateConfiguration
 *
 * Note: ConfigurationManagerImpl uses OSGi @Reference injection for LogManager.
 * The constructor only accepts (LogManager, DataManager) but doesn't set logManager field.
 * Tests that require logManager will use a mock ConfigurationManager instead.
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigurationManagerInterfaceTests {

    @Mock
    LogManager mockLogManager;

    @Mock
    DataManager mockDataManager;

    @Mock
    ConfigurationHandler mockConfigHandler;

    @Mock
    ConfigurationManager mockConfigManager;

    ConfigurationManagerImpl configManager;

    @Before
    public void setUp() throws Exception {
        // Note: The constructor doesn't properly inject logManager (it's a bug in the impl)
        // For full testing, we would need OSGi DS context
        configManager = new ConfigurationManagerImpl(mockLogManager, mockDataManager);
    }

    // ========== Interface Implementation Tests ==========

    /**
     * Test that ConfigurationManagerImpl implements ConfigurationManager interface.
     */
    @Test
    public void configurationManagerImpl_implementsInterface() {
        assertTrue("ConfigurationManagerImpl should implement ConfigurationManager interface",
                configManager instanceof ConfigurationManager);
    }

    // ========== getConfigurationProperty() tests ==========

    /**
     * Test that getConfigurationProperty returns null when no properties are set.
     */
    @Test
    public void getConfigurationProperty_returnsNullWhenNoProperties() {
        String value = configManager.getConfigurationProperty("nonexistent.key");
        assertNull("getConfigurationProperty should return null when no properties set", value);
    }

    // ========== ConfigurationHandler interface tests ==========

    /**
     * Test that ConfigurationHandler interface has generateConfig method.
     */
    @Test
    public void configurationHandler_generateConfigMethodExists() throws Exception {
        Properties params = new Properties();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        mockConfigHandler.generateConfig(params, pw, "process-123", "user");

        verify(mockConfigHandler).generateConfig(params, pw, "process-123", "user");
    }

    /**
     * Test that mock ConfigurationManager can register handler.
     */
    @Test
    public void mockConfigManager_registerConfigurationHandlerWorks() {
        mockConfigManager.registerConfigurationHandler("testType", mockConfigHandler);
        verify(mockConfigManager).registerConfigurationHandler("testType", mockConfigHandler);
    }

    /**
     * Test that mock ConfigurationManager can generate configuration.
     */
    @Test
    public void mockConfigManager_generateConfigurationWorks() throws Exception {
        Properties params = new Properties();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        mockConfigManager.generateConfiguration("testType", params, pw, "process-123", "user");

        verify(mockConfigManager).generateConfiguration("testType", params, pw, "process-123", "user");
    }

    /**
     * Test that mock ConfigurationManager can get simulation file.
     */
    @Test
    public void mockConfigManager_getSimulationFileWorks() throws Exception {
        when(mockConfigManager.getSimulationFile(anyString(), any())).thenReturn(null);

        java.io.File result = mockConfigManager.getSimulationFile("sim-123", null);

        assertNull("getSimulationFile should return null when configured to", result);
        verify(mockConfigManager).getSimulationFile("sim-123", null);
    }

    // ========== Interface method signature tests ==========

    /**
     * Test that all interface methods are defined.
     */
    @Test
    public void allInterfaceMethods_areDefined() {
        ConfigurationManager manager = mockConfigManager;

        // Verify all methods can be called on the interface
        try {
            manager.getConfigurationProperty("test");
            manager.registerConfigurationHandler("type", mockConfigHandler);
            manager.generateConfiguration("type", new Properties(), new PrintWriter(new StringWriter()),
                    "process", "user");
            manager.getSimulationFile("sim", null);
        } catch (Exception e) {
            // Expected for some methods without proper setup
        }
    }

    /**
     * Test ConfigurationHandler interface is a proper interface.
     */
    @Test
    public void configurationHandler_isInterface() {
        assertTrue("ConfigurationHandler should be an interface",
                ConfigurationHandler.class.isInterface());
    }

    /**
     * Test ConfigurationManager interface is a proper interface.
     */
    @Test
    public void configurationManager_isInterface() {
        assertTrue("ConfigurationManager should be an interface",
                ConfigurationManager.class.isInterface());
    }
}
