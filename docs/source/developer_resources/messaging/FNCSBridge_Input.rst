FNCS Bridge Input/Output
-----------------
The FNCS Bridge input and output topics are the main driver behind controlling the simulation and the latest data from the simulation.
FNCS Bridge listens for input on topic **goss/gridappsd/fncs/input** and publishes responses on topic **goss/gridappsd/fncs/output**

Is Initialized Input
::

	{"command": "isInitialized"}

Is Initialized Output
::

	{"command": "isInitialized", "response":<true/false>, "output":"Any messages from FNCS regarding initialization"}

Next Time Step Input
::

	{"command": "nextTimeStep", "currentTime":<seconds from start of simulation>}
  
Next Time Step Output
::

	{"command": "isInitialized", "output":"Latest output from simulator"}
 
Update Input 
::

	{"command": "update", "message":"......"}
  
Stop Input  
::

	{"command": "stop"}
