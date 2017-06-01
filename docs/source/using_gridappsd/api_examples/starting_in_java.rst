**Java**

The request simulation can be called using the GOSS Client API. https://github.com/GridOPTICS/GOSS  The Client API is used to send a run configuration to the GOSS simulation request topic, once the simulation has started it listens to the FNCS output topic for the simulation data.

::

	import org.apache.http.auth.Credentials;
	import org.apache.http.auth.UsernamePasswordCredentials;
	import pnnl.goss.core.Client;
	import pnnl.goss.core.Client.PROTOCOL;
	import pnnl.goss.core.ClientFactory;
	import pnnl.goss.core.GossResponseEvent;
	import pnnl.goss.core.Request.RESPONSE_FORMAT;
	import pnnl.goss.core.client.ClientServiceFactory;
	import pnnl.goss.gridappsd.dto.PowerSystemConfig;
	import pnnl.goss.gridappsd.dto.RequestSimulation;
	import pnnl.goss.gridappsd.dto.SimulationConfig;
	import pnnl.goss.gridappsd.utils.GridAppsDConstants;

  
  
	ClientFactory clientFactory = new ClientServiceFactory();
			
	Client client;
			
	//Step1: Create GOSS Client
	Credentials credentials = new UsernamePasswordCredentials(
					username, pw);
 	client = clientFactory.create(PROTOCOL.STOMP, credentials);
  
  
  	//Create Request Simulation object, you could also just pass in a json string with the configuration
	PowerSystemConfig powerSystemConfig = new PowerSystemConfig();
	powerSystemConfig.GeographicalRegion_name = "ieee8500_Region";
	powerSystemConfig.SubGeographicalRegion_name = "ieee8500_SubRegion";
	powerSystemConfig.Line_name = "ieee8500";
			
	SimulationConfig simulationConfig = new SimulationConfig();
	simulationConfig.duration = 60;
	simulationConfig.power_flow_solver_method = "";
	simulationConfig.simulation_id = ""; //.setSimulation_name("");
	simulationConfig.simulator = ""; //.setSimulator("");
			
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	simulationConfig.start_time = sdf.format(new Date()); //.setStart_time("");
			
	RequestSimulation requestSimulation = new RequestSimulation(powerSystemConfig, simulationConfig);
			
	Gson  gson = new Gson();
	String request = gson.toJson(requestSimulation); 
  	//Step3: Send configuration to the request simulation topic
	String simulationId = client.getResponse(request, GridAppsDConstants.topic_requestSimulation, RESPONSE_FORMAT.JSON)
			
	//Subscribe to bridge output
	client.subscribe("goss/gridappsd/fncs/output", new GossResponseEvent() {					
	    public void onMessage(Serializable response) {
	      System.out.println("simulation output is: "+response);
	    }
	});
