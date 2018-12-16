Transactive Systems Application
-------------------------------

Transactive energy is a method of controlling loads and resources on the
distribution system, combining both market and electrical principles
[1]. One reason for including this application in DOE-funded GridAPPS-D
is that PNNL has made several technical contributions and led several
demonstration projects in transactive systems, also funded by DOE [2].

**Application structure**

This transactive systems application is to be implemented as a
modularized 2-layer 3-level structure, as seen from Figure 3. The layer
decomposition helps the control of various groups, with limited
information flow between different layers. With the predefined functions
in each agent type (Agent A, B, and C) in each level, the existing
transactive system related work can be conveniently integrated into the
application, and the new control features can be added into specific
control function in each type of the agent easily.

|TransactiveSystemAppStructure|

Figure 3: The structure of the modularized 2-layer 3-level transactive
system application

The modularized agents opens the door for integrating different control
mechanisms into the application. Users need to consider which level
their control algorithm fits into, and fill in the control function of
the Agent class in that level, without worrying about communications
between the agents. In each level, the same type of the agent may have
various control functions, which help combining benefits of different
control schemes together.

Agent A, B and C will be implemented as VOLTTRON applications. VOLTTRON
is an application platform for distributed sensing and control
applications [3]. With the capability of hardware-in-the-loop (HIL)
testing through VOLTTRON, the transactive systems application will be
tested using the actual devices. A GOSS-VOLTTRON Bridge is to be
implemented, for the communication between GridAPPS-D and the VOLTTRON
agents in the transactive systems application.

**Application test cases**


The hierarchical control framework introduced in [4] for integrated
coordination between distributed energy resources and demand response
will be implemented into the application. In addition, [4] has not
considered the power losses or power constrains, which will be taken
into consideration in this test case. The two-layer control mechanism,
including the coordination layer and device layer, fits the proposed
structure of the application well. The control in each level will be
implemented into corresponding function in each type of the agent. The
IEEE 123-node test feeder built in GridLAB-D will be used for testing
the application.

**CIM extension for the Application**

The latest versions of GridAPPS-D has used a reduced-order CIM to
support feeder modeling. With transactive system application included
into GridAPPS-D platform, more objects, such as house air conditioner
and water heater, need to be defined in CIM. Before the definition in
CIM, a simplified version of the house object and water heater object
are to be implemented in GridLAB-D.

References
~~~~~~~~~~

[1] Gridwise Architecture Council. (2017). *Transactive Energy*. Available: http://www.gridwiseac.org/about/transactive\_energy.aspx

[2] Pacific Northwest National Laboratory. (2017). *Transactive Energy Simulation Platform (TESP)*. Available: http://tesp.readthedocs.io/en/latest/

[3] S. Katipamula, J. Haack, G. Hernandez, B. Akyol, and J. Hagerman, "VOLTTRON: An Open-Source Software Platform of the Future," *IEEE Electrification Magazine,* vol. 4, pp. 15-22, 2016.

[4] Di Wu, Jianming Lian, Yannan Sun, Tao Yang, Jacob Hansen, "Hierarchical control framework for integrated coordination between distributed energy resources and demand response," Electric Power Systems Research, pp. 45-54, May 2017.

.. |TransactiveSystemAppStructure| image:: PNNL_Apps/media/TransactiveSystemAppStructure.png

