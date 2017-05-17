.. image:: RC1_Tasks/media/RC1_workflow.png
    
Process Manager - The workflow begins when a simulation request is sent to the request topic monitored by the Process Manager, the process manager gathers the necessary configurations from the Configuration Manager.  Then sends the configuration to the simulation manager to run the simulation.

Configuration Manager - The configuration manager parses the request and builds the necessary configuration files.  It also uses the data manager to pull the model data from the CIM database.

Data Manager - The data manager accesses the CIM database to build the model files used by the simulator.

Simulation Manager

FNCS Bridge

Simulator

VoltVar Application

Vizualization
