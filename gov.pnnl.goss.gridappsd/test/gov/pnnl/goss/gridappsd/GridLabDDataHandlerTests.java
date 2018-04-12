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

import java.text.ParseException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import gov.pnnl.goss.cim2glm.CIMImporter;
import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.data.handlers.GridLabDDataHandler;
import gov.pnnl.goss.gridappsd.dto.LogMessage.LogLevel;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import gov.pnnl.goss.gridappsd.dto.RequestSimulation;
import pnnl.goss.core.server.DataSourceRegistry;

@RunWith(MockitoJUnitRunner.class)
public class GridLabDDataHandlerTests {
	final String EXPECTED_DESCRIPTION = "Generates GridLABD config files for simulation";
	@Mock
	DataSourceRegistry registry;
	
	@Mock
	DataManager dm;
	
	@Mock
	ConfigurationManager cm;
	@Mock
	CIMImporter cimImporter; 
	
	@Captor
	ArgumentCaptor<String> argCaptor;
	@Captor
	ArgumentCaptor<Long> argLongCaptor;
	@Captor
	ArgumentCaptor<LogLevel> argLogLevelCaptor;
	@Captor
	ArgumentCaptor<ProcessStatus> argProcessStatusCaptor;
	@Captor
	ArgumentCaptor<Object> argObjectCaptor;
	@Captor
	ArgumentCaptor<Class<?>> argClassCaptor;
	
	
	
	@Test
	public void handlersRegisteredWhen_startCalled() throws ParseException{
		GridLabDDataHandler handler = new GridLabDDataHandler(registry, dm, cm, cimImporter);
		handler.start();
		//verify handlers are registered for String.class and RequestSiulation.class
		Mockito.verify(dm, Mockito.times(2)).registerHandler(Mockito.any(), argClassCaptor.capture());
	}	

	
	
	
	
	@Test
	public void verifyDescription() throws ParseException{
		GridLabDDataHandler handler = new GridLabDDataHandler(registry, dm, cm, cimImporter);
		String desc = handler.getDescription();
		assertEquals(desc, EXPECTED_DESCRIPTION);
	}
	
	@Test
	public void verifyRequestTypes() throws ParseException{
		GridLabDDataHandler handler = new GridLabDDataHandler(registry, dm, cm, cimImporter);
		List<Class<?>> types = handler.getSupportedRequestTypes();
		assertEquals(2, types.size());
		assertTrue(types.contains(String.class));
		assertTrue(types.contains(RequestSimulation.class));
	}
	
	
	
	

}
