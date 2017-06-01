.. _definitions:

Definition of Terms
-------------------
 
Process Manager - The workflow begins when a simulation request is sent to the request topic monitored by the Process Manager, the process manager gathers the necessary configurations from the Configuration Manager. Then sends the configuration to the simulation manager to run the simulation.

Configuration Manager - The configuration manager parses the request and builds the necessary configuration files. It also uses the data manager to pull the model data from the CIM database.

Data Manager - The data manager accesses the CIM database to build the model files used by the simulator.

Simulation Manager - The simulation manager launches the simulator and other required applications such as the FNCS bridge, FNCS, and the VoltVar application. It is in charge of managing the timing of the simulation and reporting output from the simulation out to the simulation status topic.

FNCS Bridge - Serves as input and output from the simulator to the rest of GridAPPS-D, receives initialization, timestep, update, and finalize requests from the simulation manager and other applications, such as voltvar. It also publishes output from the simulator on a pre-defined topic for the simulation manager and other applications to subscribe to.

Simulator - In this case GridLAB-D serves as the simulator.

VoltVar Application -

Vizualization - The vizualization

GOSS
	Grid Optics Software System is a ...
	
GridLAB-D
	GridLAB-D is a distribution level powerflow simulator. It acts as the real world distribution system in GridAPPS-D.
	
Anonther Terms
	Another definition
	
	
