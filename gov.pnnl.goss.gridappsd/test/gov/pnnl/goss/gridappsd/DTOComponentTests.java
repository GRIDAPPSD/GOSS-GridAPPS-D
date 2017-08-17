package gov.pnnl.goss.gridappsd;

import org.slf4j.Logger;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import gov.pnnl.goss.gridappsd.api.StatusReporter;
import gov.pnnl.goss.gridappsd.dto.PowerSystemConfig;
import pnnl.goss.core.Client;
import pnnl.goss.core.ClientFactory;
import gov.pnnl.goss.gridappsd.utils.StatusReporterImpl;

@RunWith(MockitoJUnitRunner.class)
public class DTOComponentTests {
	
	@Mock
	Logger logger;
	
	@Mock
	ClientFactory clientFactory;
	
	@Mock
	Client client;
	
	@Captor
	ArgumentCaptor<String> argCaptor;
	
	@Test
	public void powerSystemConfig_formatCheck(){
		
		// ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
		//Create and Initialize with Values PowerSystemConfig
		PowerSystemConfig config = new PowerSystemConfig();
		config.GeographicalRegion_name = "ieee8500_Region";
		config.Line_name = "ieee8500";
		config.SubGeographicalRegion_name = "ieee8500_SubRegion";
		
		
		//Serialize PowerSystemConfig
		String pscSerialized = config.toString();
		
		//What the serialized config should look like
		String request = 	"{\"SubGeographicalRegion_name\":\"ieee8500_SubRegion\",\"GeographicalRegion_name\":\"ieee8500_Region\",\"Line_name\":\"ieee8500\"}";
		
		//Assert equal serialized PSC and comparison string
		assertEquals(pscSerialized, request);
				
	}
	
	@Test
	public void applicationConfig_formatCheck(){
		
	}
	
	@Test
	public void applicationObject_formatCheck(){
		
	}
	
	@Test
	public void fncsBridgeResponse_formatCheck(){
		
	}
	
	@Test
	public void modelCreationConfig_formatCheck(){
		
	}
	
	@Test
	public void requestSimulation_formatCheck(){
		
	}
	
	@Test
	public void simulationConfig_formatCheck(){
		
	}
	
	@Test
	public void simulationOutput_formatCheck(){
		
	}
	
	@Test
	public void simulationOutputObject_formatCheck(){
		
	}
	
	//TODO also add failure for each, parsing string that is non-compliant
	//TODO also check parsing
	
	
	
//	@Test
//	public void whenReportStatusOnTopic_clientPublishCalled(){
//
//		// ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
//		
//		try {
//			Mockito.when(clientFactory.create(Mockito.any(), Mockito.any())).thenReturn(client);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		StatusReporter statusReporter = new StatusReporterImpl(clientFactory, logger) ;
//		
//		try {
//			statusReporter.reportStatus("big/status", "Things are good");
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		Mockito.verify(client).publish(argCaptor.capture(), argCaptor.capture());
//		
//		List<String> allValues = argCaptor.getAllValues();
//		assertEquals(2, allValues.size());
//		assertEquals("big/status", allValues.get(0));
//		assertEquals("Things are good", allValues.get(1));
//				
//	}
	
	

}
