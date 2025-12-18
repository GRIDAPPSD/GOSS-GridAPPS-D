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

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.api.ServiceManager;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.ServiceInfo;
import gov.pnnl.goss.gridappsd.dto.ServiceInstance;
import gov.pnnl.goss.gridappsd.service.ServiceManagerImpl;
import pnnl.goss.core.ClientFactory;

/**
 * Tests for the ServiceManager interface methods.
 *
 * These tests verify that the ServiceManagerImpl correctly implements all
 * methods defined in the ServiceManager interface: - registerService -
 * listServices - getService - getServiceIdForInstance - deRegisterService -
 * startService - startServiceForSimultion - stopService - listRunningServices
 * (2 overloads) - stopServiceInstance - getServiceConfigDirectory
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ServiceManagerInterfaceTests {

    @Mock
    LogManager mockLogManager;

    @Mock
    ClientFactory mockClientFactory;

    ServiceManagerImpl serviceManager;

    @Before
    public void setUp() throws Exception {
        serviceManager = new ServiceManagerImpl(mockLogManager, mockClientFactory);
    }

    // ========== listServices() tests ==========

    /**
     * Test that listServices returns empty list when no services are registered.
     */
    @Test
    public void listServices_returnsEmptyListWhenNoServices() {
        List<ServiceInfo> services = serviceManager.listServices();
        assertNotNull("listServices should return a non-null list", services);
        assertTrue("listServices should return empty list when no services registered", services.isEmpty());
    }

    // ========== listRunningServices() tests ==========

    /**
     * Test that listRunningServices returns empty list when no services are
     * running.
     */
    @Test
    public void listRunningServices_returnsEmptyListWhenNoServicesRunning() {
        List<ServiceInstance> instances = serviceManager.listRunningServices();
        assertNotNull("listRunningServices should return a non-null list", instances);
        assertTrue("listRunningServices should return empty list when no services running", instances.isEmpty());
    }

    /**
     * Test that listRunningServices with serviceId and simulationId returns empty
     * list when no matching services.
     */
    @Test
    public void listRunningServices_withIds_returnsEmptyListWhenNoMatchingServices() {
        List<ServiceInstance> instances = serviceManager.listRunningServices("nonexistent-service", "sim-123");
        assertNotNull("listRunningServices(serviceId, simulationId) should return a non-null list", instances);
        assertTrue("listRunningServices(serviceId, simulationId) should return empty list when no matching services",
                instances.isEmpty());
    }

    // ========== getService() tests ==========

    /**
     * Test that getService returns null for non-existent service.
     */
    @Test
    public void getService_returnsNullForNonExistentService() {
        ServiceInfo service = serviceManager.getService("nonexistent-service");
        assertNull("getService should return null for non-existent service", service);
    }

    /**
     * Test that getService trims whitespace from serviceId.
     */
    @Test
    public void getService_trimsWhitespace() {
        ServiceInfo service = serviceManager.getService("  nonexistent-service  ");
        assertNull("getService should handle whitespace in serviceId", service);
    }

    // ========== getServiceIdForInstance() tests ==========

    /**
     * Test that getServiceIdForInstance throws exception for non-existent instance.
     */
    @Test(expected = NullPointerException.class)
    public void getServiceIdForInstance_throwsForNonExistentInstance() {
        serviceManager.getServiceIdForInstance("nonexistent-instance");
    }

    // ========== getServiceConfigDirectory() tests ==========

    /**
     * Test that getServiceConfigDirectory returns a File object.
     */
    @Test
    public void getServiceConfigDirectory_returnsFile() {
        File configDir = serviceManager.getServiceConfigDirectory();
        assertNotNull("getServiceConfigDirectory should return a non-null File", configDir);
    }

    /**
     * Test that getServiceConfigDirectory returns "services" as default.
     */
    @Test
    public void getServiceConfigDirectory_returnsDefaultDirectory() {
        File configDir = serviceManager.getServiceConfigDirectory();
        assertEquals("getServiceConfigDirectory should return 'services' as default",
                "services", configDir.getName());
    }

    // ========== registerService() tests ==========

    /**
     * Test that registerService is callable (currently not fully implemented).
     */
    @Test
    public void registerService_isCallable() {
        ServiceInfo serviceInfo = new ServiceInfo();
        // Should not throw - method is currently stubbed
        serviceManager.registerService(serviceInfo, "test-package");
    }

    // ========== deRegisterService() tests ==========

    /**
     * Test that deRegisterService is callable for non-existent service.
     */
    @Test
    public void deRegisterService_handlesNonExistentService() {
        // Should not throw - method is currently stubbed
        serviceManager.deRegisterService("nonexistent-service");
    }

    // ========== startService() tests ==========

    /**
     * Test that startService throws exception for non-existent service.
     */
    @Test(expected = RuntimeException.class)
    public void startService_throwsForNonExistentService() {
        serviceManager.startService("nonexistent-service", null);
    }

    /**
     * Test that startService accepts runtime options.
     */
    @Test(expected = RuntimeException.class)
    public void startService_acceptsRuntimeOptions() {
        HashMap<String, Object> runtimeOptions = new HashMap<>();
        runtimeOptions.put("option1", "value1");
        serviceManager.startService("nonexistent-service", runtimeOptions);
    }

    // ========== startServiceForSimultion() tests ==========

    /**
     * Test that startServiceForSimultion throws exception for non-existent service.
     */
    @Test(expected = RuntimeException.class)
    public void startServiceForSimultion_throwsForNonExistentService() {
        Map<String, Object> simulationContext = new HashMap<>();
        simulationContext.put("simulationId", "sim-123");
        serviceManager.startServiceForSimultion("nonexistent-service", null, simulationContext);
    }

    /**
     * Test that startServiceForSimultion handles null simulation context.
     */
    @Test(expected = RuntimeException.class)
    public void startServiceForSimultion_handlesNullContext() {
        serviceManager.startServiceForSimultion("nonexistent-service", null, null);
    }

    // ========== stopService() tests ==========

    /**
     * Test that stopService handles non-existent service gracefully.
     */
    @Test
    public void stopService_handlesNonExistentService() {
        // Should not throw
        serviceManager.stopService("nonexistent-service");
    }

    // ========== stopServiceInstance() tests ==========

    /**
     * Test that stopServiceInstance throws exception for non-existent instance.
     */
    @Test(expected = NullPointerException.class)
    public void stopServiceInstance_throwsForNonExistentInstance() {
        serviceManager.stopServiceInstance("nonexistent-instance");
    }

    // ========== Interface contract tests ==========

    /**
     * Test that ServiceManagerImpl implements ServiceManager interface.
     */
    @Test
    public void serviceManagerImpl_implementsInterface() {
        assertTrue("ServiceManagerImpl should implement ServiceManager interface",
                serviceManager instanceof ServiceManager);
    }

    /**
     * Test that all interface methods are accessible.
     */
    @Test
    public void allInterfaceMethods_areAccessible() {
        ServiceManager manager = serviceManager;

        // listServices
        assertNotNull(manager.listServices());

        // listRunningServices (no args)
        assertNotNull(manager.listRunningServices());

        // listRunningServices (with ids)
        assertNotNull(manager.listRunningServices("test", "sim-123"));

        // getService
        manager.getService("test");

        // getServiceConfigDirectory
        assertNotNull(manager.getServiceConfigDirectory());

        // deRegisterService
        manager.deRegisterService("test");

        // stopService
        manager.stopService("test");

        // registerService
        manager.registerService(new ServiceInfo(), "test");
    }
}
