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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import gov.pnnl.goss.gridappsd.api.PowergridModelDataManager;

/**
 * Tests for the PowergridModelDataManager interface methods.
 *
 * These tests verify the PowergridModelDataManager interface contract using mocks.
 * The actual implementation (BGPowergridModelDataManagerImpl) requires Jena/Blazegraph
 * dependencies that are only available at runtime with the full OSGi container.
 *
 * Interface methods:
 * - query, queryResultSet
 * - queryObject, queryObjectResultSet
 * - queryObjectTypes, queryObjectTypeList
 * - queryModel, queryModelResultSet
 * - queryModelNames, queryModelNameList
 * - queryModelNamesAndIds, queryModelNamesAndIdsResultSet
 * - queryObjectIds, queryObjectIdsList
 * - queryObjectDictByType, queryObjectDictByTypeResultSet
 * - queryMeasurementDictByObject, queryMeasurementDictByObjectResultSet
 * - putModel
 */
@RunWith(MockitoJUnitRunner.class)
public class PowergridModelDataManagerInterfaceTests {

    @Mock
    PowergridModelDataManager mockDataManager;

    // ========== Interface definition tests ==========

    /**
     * Test that PowergridModelDataManager is an interface.
     */
    @Test
    public void powergridModelDataManager_isInterface() {
        assertTrue("PowergridModelDataManager should be an interface",
                PowergridModelDataManager.class.isInterface());
    }

    // ========== ResultFormat enum tests ==========

    /**
     * Test that ResultFormat enum contains expected values.
     */
    @Test
    public void resultFormat_containsExpectedValues() {
        PowergridModelDataManager.ResultFormat[] formats = PowergridModelDataManager.ResultFormat.values();
        assertEquals("Should have 3 result formats", 3, formats.length);

        // Verify each format exists
        assertNotNull(PowergridModelDataManager.ResultFormat.JSON);
        assertNotNull(PowergridModelDataManager.ResultFormat.XML);
        assertNotNull(PowergridModelDataManager.ResultFormat.CSV);
    }

    // ========== query() tests ==========

    /**
     * Test that query method is defined in interface.
     */
    @Test
    public void query_methodIsDefined() throws Exception {
        when(mockDataManager.query(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("{\"results\":[]}");

        String result = mockDataManager.query("modelId", "SELECT * WHERE {}", "JSON", "process-123", "user");

        assertNotNull("query should return a result", result);
        verify(mockDataManager).query("modelId", "SELECT * WHERE {}", "JSON", "process-123", "user");
    }

    // ========== queryObject() tests ==========

    /**
     * Test that queryObject method is defined in interface.
     */
    @Test
    public void queryObject_methodIsDefined() throws Exception {
        when(mockDataManager.queryObject(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("{\"object\":{}}");

        String result = mockDataManager.queryObject("modelId", "mrid-123", "JSON", "process-123", "user");

        assertNotNull("queryObject should return a result", result);
    }

    // ========== queryObjectTypes() tests ==========

    /**
     * Test that queryObjectTypes method is defined in interface.
     */
    @Test
    public void queryObjectTypes_methodIsDefined() {
        when(mockDataManager.queryObjectTypes(anyString(), anyString(), anyString(), anyString()))
                .thenReturn("{\"types\":[]}");

        String result = mockDataManager.queryObjectTypes("modelId", "JSON", "process-123", "user");

        assertNotNull("queryObjectTypes should return a result", result);
    }

    // ========== queryObjectTypeList() tests ==========

    /**
     * Test that queryObjectTypeList method is defined in interface.
     */
    @Test
    public void queryObjectTypeList_methodIsDefined() {
        List<String> types = new ArrayList<>();
        types.add("ACLineSegment");
        types.add("PowerTransformer");
        when(mockDataManager.queryObjectTypeList(anyString(), anyString(), anyString()))
                .thenReturn(types);

        List<String> result = mockDataManager.queryObjectTypeList("modelId", "process-123", "user");

        assertNotNull("queryObjectTypeList should return a list", result);
        assertEquals("Should return 2 types", 2, result.size());
    }

    // ========== queryModel() tests ==========

    /**
     * Test that queryModel method is defined in interface.
     */
    @Test
    public void queryModel_methodIsDefined() throws Exception {
        when(mockDataManager.queryModel(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("{\"model\":{}}");

        String result = mockDataManager.queryModel("modelId", "objectType", "filter", "JSON", "process-123", "user");

        assertNotNull("queryModel should return a result", result);
    }

    // ========== queryModelNames() tests ==========

    /**
     * Test that queryModelNames method is defined in interface.
     */
    @Test
    public void queryModelNames_methodIsDefined() {
        when(mockDataManager.queryModelNames(anyString(), anyString(), anyString()))
                .thenReturn("{\"names\":[\"ieee8500\",\"ieee123\"]}");

        String result = mockDataManager.queryModelNames("JSON", "process-123", "user");

        assertNotNull("queryModelNames should return a result", result);
    }

    // ========== queryModelNameList() tests ==========

    /**
     * Test that queryModelNameList method is defined in interface.
     */
    @Test
    public void queryModelNameList_methodIsDefined() {
        List<String> names = new ArrayList<>();
        names.add("ieee8500");
        names.add("ieee123");
        when(mockDataManager.queryModelNameList(anyString(), anyString()))
                .thenReturn(names);

        List<String> result = mockDataManager.queryModelNameList("process-123", "user");

        assertNotNull("queryModelNameList should return a list", result);
        assertEquals("Should return 2 names", 2, result.size());
    }

    // ========== queryModelNamesAndIds() tests ==========

    /**
     * Test that queryModelNamesAndIds method is defined in interface.
     */
    @Test
    public void queryModelNamesAndIds_methodIsDefined() {
        when(mockDataManager.queryModelNamesAndIds(anyString(), anyString(), anyString()))
                .thenReturn("{\"models\":[]}");

        String result = mockDataManager.queryModelNamesAndIds("JSON", "process-123", "user");

        assertNotNull("queryModelNamesAndIds should return a result", result);
    }

    // ========== queryObjectIds() tests ==========

    /**
     * Test that queryObjectIds method is defined in interface.
     */
    @Test
    public void queryObjectIds_methodIsDefined() {
        when(mockDataManager.queryObjectIds(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("{\"ids\":[]}");

        String result = mockDataManager.queryObjectIds("JSON", "modelId", "objectType", "process-123", "user");

        assertNotNull("queryObjectIds should return a result", result);
    }

    // ========== queryObjectIdsList() tests ==========

    /**
     * Test that queryObjectIdsList method is defined in interface.
     */
    @Test
    public void queryObjectIdsList_methodIsDefined() {
        List<String> ids = new ArrayList<>();
        ids.add("id-1");
        ids.add("id-2");
        when(mockDataManager.queryObjectIdsList(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(ids);

        List<String> result = mockDataManager.queryObjectIdsList("modelId", "objectType", "process-123", "user");

        assertNotNull("queryObjectIdsList should return a list", result);
    }

    // ========== queryObjectDictByType() tests ==========

    /**
     * Test that queryObjectDictByType method is defined in interface.
     */
    @Test
    public void queryObjectDictByType_methodIsDefined() throws Exception {
        when(mockDataManager.queryObjectDictByType(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("{\"dict\":{}}");

        String result = mockDataManager.queryObjectDictByType("JSON", "modelId", "objectType", "objectId", "process-123", "user");

        assertNotNull("queryObjectDictByType should return a result", result);
    }

    // ========== queryMeasurementDictByObject() tests ==========

    /**
     * Test that queryMeasurementDictByObject method is defined in interface.
     */
    @Test
    public void queryMeasurementDictByObject_methodIsDefined() throws Exception {
        when(mockDataManager.queryMeasurementDictByObject(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("{\"measurements\":[]}");

        String result = mockDataManager.queryMeasurementDictByObject("JSON", "modelId", "objectType", "objectId", "process-123", "user");

        assertNotNull("queryMeasurementDictByObject should return a result", result);
    }

    // ========== putModel() tests ==========

    /**
     * Test that putModel method is defined in interface.
     */
    @Test
    public void putModel_methodIsDefined() {
        mockDataManager.putModel("modelId", "<rdf>model</rdf>", "XML", "process-123", "user");

        verify(mockDataManager).putModel("modelId", "<rdf>model</rdf>", "XML", "process-123", "user");
    }

    // ========== All interface methods accessible test ==========

    /**
     * Test that all interface methods are accessible.
     */
    @Test
    public void allInterfaceMethods_areAccessible() throws Exception {
        PowergridModelDataManager manager = mockDataManager;

        // All methods should be callable
        manager.query("m", "q", "JSON", "p", "u");
        manager.queryResultSet("m", "q", "p", "u");
        manager.queryObject("m", "mrid", "JSON", "p", "u");
        manager.queryObjectResultSet("m", "mrid", "p", "u");
        manager.queryObjectTypes("m", "JSON", "p", "u");
        manager.queryObjectTypeList("m", "p", "u");
        manager.queryModel("m", "type", "filter", "JSON", "p", "u");
        manager.queryModelResultSet("m", "type", "filter", "p", "u");
        manager.queryModelNames("JSON", "p", "u");
        manager.queryModelNameList("p", "u");
        manager.queryModelNamesAndIds("JSON", "p", "u");
        manager.queryModelNamesAndIdsResultSet("p", "u");
        manager.queryObjectIds("JSON", "m", "type", "p", "u");
        manager.queryObjectIdsList("m", "type", "p", "u");
        manager.queryObjectDictByType("JSON", "m", "type", "id", "p", "u");
        manager.queryObjectDictByTypeResultSet("m", "type", "id", "p", "u");
        manager.queryMeasurementDictByObject("JSON", "m", "type", "id", "p", "u");
        manager.queryMeasurementDictByObjectResultSet("m", "type", "id", "p", "u");
        manager.putModel("m", "model", "XML", "p", "u");
    }
}
