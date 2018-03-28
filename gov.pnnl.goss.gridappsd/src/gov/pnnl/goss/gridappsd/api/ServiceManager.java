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
package gov.pnnl.goss.gridappsd.api;

import gov.pnnl.goss.gridappsd.dto.ServiceInfo;
import gov.pnnl.goss.gridappsd.dto.ServiceInstance;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface ServiceManager {

	/**
	 * Register a new service with GridAPPS-D Service manager, registered services will be persisted
	 * @param serviceInfo  Service info file that includes details about the service configuration
	 * @param servicePackage  Zip file containing files required by the service, this includes persistent config files and executable files.  
	 * 
	 */
	void registerService(ServiceInfo serviceInfo, Serializable servicePackage);
	
	/**
	 * Lists the currently registered services
	 * @return List of ServiceInfo objects describing the configurations of the registered services
	 */
	List<ServiceInfo> listServices();  //Would return through message bus list of appInfo objects
	
	/**
	 * Returns service configuration for the requested service ID
	 * @param service_id  Registered ID of the desired service
	 * @return ServiceInfo object containing service configuration
	 */
	ServiceInfo getService(String service_id); //Would return through message bus appInfo object
	
	/**
	 * Unregisters service with the requested id
	 * @param service_id  Registered ID of the service to de-register
	 */
	void deRegisterService(String service_id); 
	
	/**
	 * Start service instance
	 * @param service_id  Registered ID of the desired service
	 * @param runtimeOptions Runtime options for the service instance, in most cases these will be passed in on the command-line 
	 * @return String containing service instance ID
	 */
	String startService(String service_id, String runtimeOptions);  //may also need input/output topics or simulation id
	
	String startServiceForSimultion(String service_id, String runtimeOptions, Map<String,Object> simulationContext);  //may also need input/output topics??
	
	/**
	 * Stops all instances of the service with requested service ID
	 * @param service_id  Registered ID of the service to 
	 */
	void stopService(String service_id);  
	
	/**
	 * Lists currently running service instances
	 * @return  List of ServiceInstance objects
	 */
	List<ServiceInstance> listRunningServices(); 

	/**
	 * Lists currently running service instances for the requested service ID
	 * @param serviceId  Registered ID of the service to list
	 * @return List of ServiceInstance objects
	 */
	List<ServiceInstance> listRunningServices(String serviceId);

	/**
	 * Stops service instance
	 * @param instanceId  ID of the service instance to stop
	 */
	void stopServiceInstance(String instanceId);
	
	/**
	 * Get the directory where the service configurations are stored
	 * @return File location of the directory containing service configurations
	 */
	File getServiceConfigDirectory();
}
