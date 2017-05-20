**Websockets/Javascript**

In order to call the simulation API from javascript you will need to install `stomp.js <http://jmesnil.net/stomp-websocket/doc/>`_   
In order to start the simulation through the websocket API you will need to send the configuration to the gridappsd simulation topic in the format descibed on `the Simulation Request page #simulation-request_`   

::

  <script src='js/jquery-2.1.4.min.js'></script>
  <script src="js/stomp.js" type="text/javascript"></script>
  configString = "...........  See developer resources"
  simulationTopic = "/queue/goss/gridappsd/process/request/simulation";
  gossHost = "gridappsdhost";
  //Create client
  var client = Stomp.client( "ws://"+gossHost+":61614");
  client.heartbeat.incoming=0;
  client.heartbeat.outgoing=0;
  
  var connect_error_callback = function(error) {
     $("#debug").append("Error "+error + "\n");	   
  };	
  var outputCallback = function(message){
     $("#debug").append("Output "+message.body + "\n");
  }
  //Make connection with server
  client.connect( "username", "pw", connect_callback, connect_error_callback);

  var request = JSON.stringify(JSON.parse(configField));
  client.send(simulationTopic, {"reply-to" :"/temp-queue/response-queue"}, request);
	client.subscribe("/temp-queue/response-queue", function(message) {
	    var simulationId = JSON.parse(message.body);
	    $("#debug").append("Received Simulation ID: " +simulationId + "\n");
	    client.subscribe("/topic/goss/gridappsd/simulation/status/"+simulationId, statusCallback);
	});
  client.subscribe("/topic/goss/gridappsd/fncs/output", outputCallback);
    
    
