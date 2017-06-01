.. _definitions:

Definition of Terms
-------------------
 
Process Manager - Process Manager keeps track of all the processes running on the platform. These processes may include simulators, requests, applications and other managers. It is also the starting point for a request received by the platform.

Configuration Manager - It receives simulation configuration request from Process Manager and parses it to build the necessary configuration files.

Data Manager - The data manager accesses the database to build the model files used by the simulator.

Simulation Manager - The simulation manager launches the simulator and other required applications such as the FNCS bridge, FNCS, and the VoltVar application. It is in charge of managing the timing of the simulation and reporting output from the simulation out to the simulation status topic.

FNCS-GOSS Bridge - Serves as a bridge between FNCS and Simulation Manager. 

FNCS - FNCS is a network co-simulator used to communicate between simulator  and FNCS-GOSS bridge

Platform - Refers to GridAPPS-D platform.

RC1 - Release Cycle 1. 

Simulation - A real world distribution system currently done by GridLAB-D

Simulator - In current release GridLAB-D serves as the simulator.

VoltVar Application - 

Vizualization - A web-based visualization application is developed in RC1 to view power system model with real time values from simulation result.

GOSS - Grid Optics Software System is a middleware architecture designed as a prototype future data analytics and integration platform
	
GridLAB-D - GridLAB-D is a distribution level powerflow simulator. It acts as the real world distribution system in GridAPPS-D.

Power System Model - IEEE 8500 model is used in RC1.

Model - See Power System Model

CIM - Common Information Model is a standard for representing electrical network and exchange information.
	
	
