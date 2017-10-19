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
package gov.pnnl.goss.gridappsd.test;
import org.amdatu.testing.configurator.ConfigurationSteps;
import static org.amdatu.testing.configurator.TestConfigurator.createConfiguration;

import pnnl.goss.core.ClientFactory;

/**
 * Standard configuration that is required for us to use goss in integration tests.
 * 
 * These configuration steps can be used as a guide to building cfg files
 * for the bundles.
 * 
 * @author Craig Allwardt
 *
 */
public class CoreGossConfig {
	
	/**
	 * Minimal configuration for goss including broker uri
	 * @return
	 */
	public static ConfigurationSteps configureServerAndClientPropertiesConfig(){
		
		return ConfigurationSteps.create()
				.add(createConfiguration("pnnl.goss.core.server")
					.set("goss.openwire.uri", "tcp://localhost:6000")
					.set("goss.stomp.uri",  "stomp://localhost:6001") //vm:(broker:(tcp://localhost:6001)?persistent=false)?marshal=false")
					.set("goss.ws.uri", "ws://localhost:6002")
					.set("goss.start.broker", "true")
					.set("goss.broker.uri", "tcp://localhost:6000"))
				.add(createConfiguration(ClientFactory.CONFIG_PID)
					.set("goss.openwire.uri", "tcp://localhost:6000")
					.set("goss.stomp.uri",  "stomp://localhost:6001")
					.set("goss.ws.uri", "ws://localhost:6002"))
				.add(createConfiguration("org.ops4j.pax.logging")
					.set("log4j.rootLogger", "DEBUG, out, osgi:*")
					.set("log4j.throwableRenderer", "org.apache.log4j.OsgiThrowableRenderer")

					//# CONSOLE appender not used by default
					.set("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender")
					.set("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout")
					.set("log4j.appender.stdout.layout.ConversionPattern", "%-5.5p| %c{1} (%L) | %m%n")
					//#server.core.internal.GossRequestHandlerRegistrationImpl", "DEBUG,stdout
					.set("log4j.logger.pnnl.goss", "DEBUG, stdout")
					.set("log4j.logger.org.apache.aries", "INFO")

					//# File appender
					.set("log4j.appender.out", "org.apache.log4j.RollingFileAppender")
					.set("log4j.appender.out.layout", "org.apache.log4j.PatternLayout")
					.set("log4j.appender.out.layout.ConversionPattern", "%d{ISO8601} | %-5.5p | %-16.16t | %-32.32c{1} | %X{bundle.id} - %X{bundle.name} - %X{bundle.version} | %m%n")
					.set("log4j.appender.out.file", "felix.log")
					.set("log4j.appender.out.append", "true")
					.set("log4j.appender.out.maxFileSize", "1MB")
					.set("log4j.appender.out.maxBackupIndex", "10"));
		
	}
}