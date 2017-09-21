package gov.pnnl.goss.gridappsd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import gov.pnnl.goss.gridappsd.api.TestManager;
import gov.pnnl.goss.gridappsd.dto.SimulationOutput;
import gov.pnnl.goss.gridappsd.dto.SimulationOutputObject;
import gov.pnnl.goss.gridappsd.testmanager.CompareResults;
import gov.pnnl.goss.gridappsd.testmanager.TestManagerImpl;
import pnnl.goss.core.Client;
import pnnl.goss.core.ClientFactory;

@RunWith(MockitoJUnitRunner.class)
public class CompareResultsTest {
	
	TestManager tm = new TestManagerImpl();
	
	@Mock
	Logger logger;
	
	@Mock
	ClientFactory clientFactory;
	
	@Mock
	Client client;
	
	@Captor
	ArgumentCaptor<String> argCaptor;
	
	protected static void getProp(SimulationOutput simOut) {
		for (SimulationOutputObject out :simOut.output_objects){
			System.out.println(out.name);
			out.getProperties();			
		}
	}
	
	@Test
	public void comapre(){	
		CompareResults compareResults = new CompareResults();
//		String path = "/Users/jsimpson/git/adms/GOSS-GridAPPS-D/gov.pnnl.goss.gridappsd/applications/python/sim_output_object.json";
		String path = "./test/gov/pnnl/goss/gridappsd/sim_output_object.json";
		SimulationOutput simOutProperties = compareResults.getOutputProperties(path);
		
//		assertEquals(simOutProperties);
		assertNotEquals(simOutProperties, null);
		getProp(simOutProperties);
		
		
//		String sim_output = "/Users/jsimpson/git/adms/GOSS-GridAPPS-D/gov.pnnl.goss.gridappsd/applications/python/sim_output.json";
//		String expected_output = "/Users/jsimpson/git/adms/GOSS-GridAPPS-D/gov.pnnl.goss.gridappsd/applications/python/expected_output.json";
		String sim_output = "./test/gov/pnnl/goss/gridappsd/sim_output.json";
		String expected_output = "./test/gov/pnnl/goss/gridappsd/expected_output.json";
		int count = compareResults.compareExpectedWithSimulation(sim_output,expected_output, simOutProperties);
		assertEquals(count, 0);
	}
	
	@Test
	public void comapre_error_1(){	
		CompareResults compareResults = new CompareResults();
		String path = "./test/gov/pnnl/goss/gridappsd/sim_output_object.json";
		SimulationOutput simOutProperties = compareResults.getOutputProperties(path);

		assertNotEquals(simOutProperties, null);
		getProp(simOutProperties);
		
		String sim_output = "./test/gov/pnnl/goss/gridappsd/sim_output.json";
		String expected_output = "./test/gov/pnnl/goss/gridappsd/expected_output_error1.json";
		int count = compareResults.compareExpectedWithSimulation(sim_output,expected_output, simOutProperties);
		assertEquals(count, 1);
	}

}
