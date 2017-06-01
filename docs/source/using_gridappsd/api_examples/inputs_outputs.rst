FNCS Bridge Input/Output
-----------------
The FNCS Bridge input and output topics are the main driver behind controlling the simulation and the latest data from the simulation.
FNCS Bridge listens for input on topic **goss/gridappsd/fncs/input** and publishes responses on topic **goss/gridappsd/fncs/output**

Applications that wish to interact with the simulation can do so by subscribing to to the output topic and publishing commands to the input topic.  

Each message should contain a command field, this specifies the operation that is either sent to teh input topic or responded to on the output topic.  The available values for the command field are *isInitialized*, *nextTimeStep*, *update*, and *stop*.  These are each described in more depth below.

- isInitialized
	Input - Checks to see if the simulator is initialized, meaning that it has established a connection to both GOSS and the simulator.  This command takes no other parameters.
	::

		{"command": "isInitialized"}

	Output - Returns simulator initializtion status (true/false) and any initialization messages. 
	::

		{"command": "isInitialized", "response":<true/false>, "output":"Any messages from simulator regarding initialization"}

- nextTimeStep
	Input - Increments the simulator to the specified timestep, in a typical real-time simulation it will be incremented once per second.  The only parameter is the current time in seconds (after the start of the simulation), this command will initiate the next time step in the simulator.
	::

		{"command": "nextTimeStep", "currentTime":<seconds from start of simulation>}
  
	Output -  Returns the current state of the objects and properties in the simulator, which objects and properties are specified by the simluation output.  The is the output that applications will wish to subscribe to.  The visualization application subscribes to this output to display the latest capacitor and regulator state.  The volt-var optimization application subscribes to this output when managing voltage levels within the simulation
	::

		{"command": "nextTimeStep", "output": "{\"ieee8500\":{\"cap_capbank0a\":{\"capacitor_A\":400000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":100.0,\"phases\":\"AN\",\"phases_connected\":\"NA\",\"pt_phase\":\"A\",\"switchA\":\"CLOSED\"},\"cap_capbank0b\":{\"capacitor_B\":400000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":101.0,\"phases\":\"BN\",\"phases_connected\":\"NB\",\"pt_phase\":\"B\",\"switchB\":\"CLOSED\"},\"cap_capbank0c\":{\"capacitor_C\":400000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":102.0,\"phases\":\"CN\",\"phases_connected\":\"NC\",\"pt_phase\":\"C\",\"switchC\":\"CLOSED\"},\"cap_capbank1a\":{\"capacitor_A\":300000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":100.0,\"phases\":\"AN\",\"phases_connected\":\"NA\",\"pt_phase\":\"A\",\"switchA\":\"CLOSED\"},\"cap_capbank1b\":{\"capacitor_B\":300000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":101.0,\"phases\":\"BN\",\"phases_connected\":\"NB\",\"pt_phase\":\"B\",\"switchB\":\"CLOSED\"},\"cap_capbank1c\":{\"capacitor_C\":300000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":102.0,\"phases\":\"CN\",\"phases_connected\":\"NC\",\"pt_phase\":\"C\",\"switchC\":\"CLOSED\"},\"cap_capbank2a\":{\"capacitor_A\":300000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":100.0,\"phases\":\"AN\",\"phases_connected\":\"NA\",\"pt_phase\":\"A\",\"switchA\":\"CLOSED\"},\"cap_capbank2b\":{\"capacitor_B\":300000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":101.0,\"phases\":\"BN\",\"phases_connected\":\"NB\",\"pt_phase\":\"B\",\"switchB\":\"CLOSED\"},\"cap_capbank2c\":{\"capacitor_C\":300000.0,\"control\":\"MANUAL\",\"control_level\":\"BANK\",\"dwell_time\":102.0,\"phases\":\"CN\",\"phases_connected\":\"NC\",\"pt_phase\":\"C\",\"switchC\":\"CLOSED\"},\"cap_capbank3\":{\"capacitor_A\":300000.0,\"capacitor_B\":300000.0,\"capacitor_C\":300000.0,\"control\":\"MANUAL\",\"control_level\":\"INDIVIDUAL\",\"dwell_time\":0.0,\"phases\":\"ABCN\",\"phases_connected\":\"NCBA\",\"pt_phase\":\"\",\"switchA\":\"CLOSED\",\"switchB\":\"CLOSED\",\"switchC\":\"CLOSED\"},\"nd_190-7361\":{\"voltage_A\":\"6410.387411-4584.456974j V\",\"voltage_B\":\"-7198.592139-3270.308372j V\",\"voltage_C\":\"642.547265+7539.531175j V\"},\"nd_190-8581\":{\"voltage_A\":\"6485.244722-4692.686497j V\",\"voltage_B\":\"-7183.641237-3170.693324j V\",\"voltage_C\":\"544.875720+7443.341013j V\"},\"nd_190-8593\":{\"voltage_A\":\"6723.279162-5056.725836j V\",\"voltage_B\":\"-7494.205738-3101.034602j V\",\"voltage_C\":\"630.475857+7534.534977j V\"},\"nd__hvmv_sub_lsb\":{\"voltage_A\":\"6261.474438-3926.148203j V\",\"voltage_B\":\"-6529.409296-3466.545236j V\",\"voltage_C\":\"247.131622+7348.295282j V\"},\"nd_l2673313\":{\"voltage_A\":\"6569.522312-5003.052614j V\",\"voltage_B\":\"-7431.486583-3004.840139j V\",\"voltage_C\":\"644.553331+7464.115915j V\"},\"nd_l2876814\":{\"voltage_A\":\"6593.064915-5014.031801j V\",\"voltage_B\":\"-7430.572726-3003.995538j V\",\"voltage_C\":\"643.473396+7483.558765j V\"},\"nd_l2955047\":{\"voltage_A\":\"5850.305846-4217.166594j V\",\"voltage_B\":\"-6729.652722-2987.617376j V\",\"voltage_C\":\"535.302083+7395.127354j V\"},\"nd_l3160107\":{\"voltage_A\":\"5954.507575-4227.423005j V\",\"voltage_B\":\"-6662.357613-3055.346879j V\",\"voltage_C\":\"600.213657+7317.832960j V\"},\"nd_l3254238\":{\"voltage_A\":\"6271.490549-4631.254028j V\",\"voltage_B\":\"-7169.987847-3099.952683j V\",\"voltage_C\":\"751.609655+7519.062260j V\"},\"nd_m1047574\":{\"voltage_A\":\"6306.632406-4741.568924j V\",\"voltage_B\":\"-7214.626338-2987.055914j V\",\"voltage_C\":\"622.058711+7442.125124j V\"},\"rcon_FEEDER_REG\":{\"Control\":\"MANUAL\",\"PT_phase\":\"CBA\",\"band_center\":126.5,\"band_width\":2.0,\"connect_type\":\"WYE_WYE\",\"control_level\":\"INDIVIDUAL\",\"dwell_time\":15.0,\"lower_taps\":16,\"raise_taps\":16,\"regulation\":0.10000000000000001},\"rcon_VREG2\":{\"Control\":\"MANUAL\",\"PT_phase\":\"CBA\",\"band_center\":125.0,\"band_width\":2.0,\"connect_type\":\"WYE_WYE\",\"control_level\":\"INDIVIDUAL\",\"dwell_time\":15.0,\"lower_taps\":16,\"raise_taps\":16,\"regulation\":0.10000000000000001},\"rcon_VREG3\":{\"Control\":\"MANUAL\",\"PT_phase\":\"CBA\",\"band_center\":125.0,\"band_width\":2.0,\"connect_type\":\"WYE_WYE\",\"control_level\":\"INDIVIDUAL\",\"dwell_time\":15.0,\"lower_taps\":16,\"raise_taps\":16,\"regulation\":0.10000000000000001},\"rcon_VREG4\":{\"Control\":\"MANUAL\",\"PT_phase\":\"CBA\",\"band_center\":125.0,\"band_width\":2.0,\"connect_type\":\"WYE_WYE\",\"control_level\":\"INDIVIDUAL\",\"dwell_time\":15.0,\"lower_taps\":16,\"raise_taps\":16,\"regulation\":0.10000000000000001},\"reg_FEEDER_REG\":{\"configuration\":\"rcon_FEEDER_REG\",\"phases\":\"ABC\",\"tap_A\":2,\"tap_B\":2,\"tap_C\":1,\"to\":\"nd__hvmv_sub_lsb\"},\"reg_VREG2\":{\"configuration\":\"rcon_VREG2\",\"phases\":\"ABC\",\"tap_A\":10,\"tap_B\":6,\"tap_C\":2,\"to\":\"nd_190-8593\"},\"reg_VREG3\":{\"configuration\":\"rcon_VREG3\",\"phases\":\"ABC\",\"tap_A\":16,\"tap_B\":10,\"tap_C\":1,\"to\":\"nd_190-8581\"},\"reg_VREG4\":{\"configuration\":\"rcon_VREG4\",\"phases\":\"ABC\",\"tap_A\":12,\"tap_B\":12,\"tap_C\":5,\"to\":\"nd_190-7361\"},\"xf_hvmv_sub\":{\"power_in_A\":\"1739729.121744-774784.928874j VA\",\"power_in_B\":\"1659762.622236-785218.729252j VA\",\"power_in_C\":\"1709521.679116-849734.584017j VA\"}}}\n"}

- update
	Input - Sends an update command which can change the capacitor and regulator status within the simulator, this is used by the volt-var optimization application.  Parameters include a message field, which contains the simulation name and the desired values for the objects to be updated.
	::

		{"command": "update", "message": {"ieee8500": {"reg_FEEDER_REG": {"tap_C": -3, "tap_B": -2, "tap_A": -1}, "reg_VREG4": {"tap_C": 1, "tap_B": 8, "tap_A": 8}, "reg_VREG2": {"tap_C": -1, "tap_B": 2, "tap_A": 6}, "reg_VREG3": {"tap_C": -3, "tap_B": 6, "tap_A": 12}}}}
  
 - stop
	Input - Stops the simulator and shuts down the bridge.  No additional parameters are required
	::

		{"command": "stop"}
