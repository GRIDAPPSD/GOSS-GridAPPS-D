As Derived from the Functional Specification 
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

This section presents a selection of GridAPPS-D domain (class) diagrams
to supplement the *OSPRREYS Functional Specification* document. The
purpose is to enhance understanding of the functional specification, by
providing graphical walkthroughs of some important use cases. The reader
should be familiar with definitions in the functional specification, and
with Universal Modeling Language (UML) diagrams.

GridAPPS-D is organized as a suite of internal function managers, twelve
of them composing the Platform Manager as shown in Figure 1. All
GridAPPS-D functions and interactions are mediated by one (or more) of
these function managers. When running, the GridAPPS-D 413 Platform
Manager will be composed of one (and only one) of each internal manager
numbered 401 – 412. These internal managers work together to accomplish
various GridAPPS-D functions.

|uml_image0|

Figure 1: Composition of the GridAPPS-D Platform Manager

Within each class block, some top-level attributes are listed with (-)
signs in the middle division, and some top-level methods are listed with
(+) signs in the lower division. For example, we already know that 401
Distribution Co-Simulator will need component simulators (i.e.
attributes) for buildings (open-source EnergyPlus), communications
(open-source ns-3), and the electric power distribution grid
(open-source GridLAB-D running in a real-time mode). It will also need
at least one method that runs the suite of simulators in a mode
emulating continuous real-time operation. Taking another example, 407
Service Manager also contains an attribute for GridLAB-D to provide
power flow calculations, but run as a service to applications.

As the design evolves, classes in Figure 1 will acquire many more
attributes and methods. The attributes themselves may reference
complicated classes and data structures. Therefore, the UML model will
expand each class into layer and sub-layer diagrams to more clearly show
these evolving details. We can still use the top-level diagrams to make
sure that the major components are in place for the important use cases.

Figure 2 illustrates the case of a user executing an application, in the
role of EF7 from the functional specification. We initially focused on
volt-var optimization (VVO), and then added a more complicated demand
response (DR) application that fits the same basic pattern. As a
prerequisite, some entity has provided both applications to GridAPPS-D
for registration and hosting, in a process detailed later. For now, we
assume the application(s) have been installed and will focus first on
running VVO.

|uml_image1|

Figure 2: Executing an application

All user interaction with GridAPPS-D occurs through a command interface,
numbered 202 when the user writes commands to GridAPPS-D, and numbered
102 when the user gets data from GridAPPS-D. To run VVO, the user will
issue 203 Model Configuration Setup and 204 Simulation Configuration
Setup to GridAPPS-D, which then delegates the commands to various
internal function managers (see Figure 1). The 203 Setup will probably
extract the feeder model of interest, set load and weather data, etc.
The 204 Setup will probably tell 401 to run GridLAB-D for a certain time
period, but not to run ns-3 or EnergyPlus. The exact composition of 203
and 204 Setups will be determined later in the design process. In a
process described later, internal functions 405 (Simulation Control
Manager) and 406 (Power System Model Manager) will transform 201, 203
and 204 into 305 and 306, which 401 can then read and run from directly.

When it runs, 401 will generate streams of data that mimic real-time
operation of the system, and these streams pass to the other parts of
GridAPPS-D as 301 Real-time Simulation Data. Some of the data streams
may also output to the user as 101 Real-time Simulation Data. The 310
VVO Application can act on this data to make decisions (e.g. switch
capacitor banks, change regulator taps, change solar inverter settings).
In this process, 310 VVO could invoke power flow calculations in
GridLAB-D via 407 Service Manager, but this is different from the way
401 Co-Simulator runs. The application may use 407 services to explore
alternatives or run contingency analysis, which could change the power
system model, but the 401 real-time simulations always take priority and
always use the “real” model.

When we considered adding the second and more complicated application,
310 DR, the structure of Figure 2 didn’t change very much. The
open-headed diamond symbols indicate that GridAPPS-D can host several
applications, which is UML aggregation. These applications may interact
via the GridAPPS-D command interface, if the applications and their
command sets have been designed for it. For example, the DR application
may use VVO to check and mitigate voltage limits.

A DR application is more likely than VVO to need EnergyPlus and ns-3 in
the co-simulation. In response, we added those attributes to 401, and
will add supporting attributes to 201, 203 and 204 as the design
evolves. It should also be recognized that more sophisticated VVO
applications might incorporate communications (ns-3) if available.

Figure 3 depicts the process of managing power system models, including
the schema and repository within 201 Distribution System Model. Because
it’s based on standards (e.g. IEC 61968) and open-source tools (e.g.
MySQL), the model can be created and maintained from outside GridAPPS-D,
directly by EF 21, the Model Manager. This is shown at the top of Figure
3. This process is out of GridAPPS-D scope but within project scope, and
it can leverage existing tools like Cimphony, Cimdesk, EA, etc.

For use by and within GridAPPS-D, all model configuration commands will
pass from EF21 through the command interface to function 406, the Power
System Model Manager. This function reads the base power system model
data from 201, and configures it into a three-phase load flow model for
solution in 106/306. The Distribution Co-Simulator uses 306, but the
user might want 106 for off-line use. Working with 404 Data Manager, the
406 Power System Model Manager may also write additional data (i.e. not
used in the load flow calculation) to 104/304. In this case, the 102
Model Output function will collect that data from both 104 and 106 for
reporting to the user, EF7, via the command interface. Note that the
base data, in 201, is not modified through this process. Instead, the
base data is treated as input to GridAPPS-D.

|uml_image2|

Figure 3: Internal model management

Figure 4 shows the internal Platform Manager flow when running
application tests. Compared to the case of normal usage in Figure 2,
this example shows additional control and output for testing. The test
commands include 203 and 204, as in Figure 2, but they also include:

-  205 Test Scripts, for the sequence of steps to perform

-  206 Test Configuration Setup, including initial conditions, etc.

-  207 Expected Results, for comparison to the actual output

-  210 Application Metadata, for information to run and instrument the
   application

The 403 Test Manager orchestrates the steps to run the application and
collect results. As part of 103 Test Results, it will compare the
real-time data (101/301) to the expected results in 207. If the testing
user, EF8, requested logging, then the 409 Log Manager will create
109/309 System Logs for collection by 403 Test Manager. Logging is
optional, and should have been requested as part of the 206 Test Config
Setup or 204 Model Config Setup (this is not spelled out in the
functional specification).

|uml_image3|

Figure 4: Testing an application or the platform

Figure 5 shows some of the internal 413 Platform Manager detail when a
user, EF7, runs an application in debugging mode. Compared to Figure 2,
there is much more internal output. The 212 Debug Configuration will
include such things as breakpoints, watch variables, and logging
requests. When run in debug mode, the 408 Debug Manager will collect the
internal inputs and intermediate results from a variety of GridAPPS-D
modules, including the simulator, services in use, model data, and
access violations. The 404 Data Manager mediates most of this data
collection (and with a change to the specification it could also mediate
101/301). The 408 Debug Manager combines this into 108 Intermediate
Results, with 109 System Logs, for output to the user via the command
interface. Depending on the implementation of GridAPPS-D, interactive
debugging may also be supported, but is not shown in Figure 5.

|uml_image4|

Figure 5: Debugging an application

Figure 6 shows the process of registering or updating an application to
use with GridAPPS-D. The developer, in the role of EF13, must provide
the application itself (211) along with the application data schema
(208) and metadata (210). The data schema includes input and output
parameters. The metadata includes a user-friendly name, description,
calling parameters, command syntax, API functions used, etc. Using this
information, 410 Application Hosting Manager will install and register
the application, and its data, with 407 Service Manager and 404 Data
Manager. After completing these steps, 412 Version Manager will output
the current version information via the command interface; the current
version includes information about which applications are installed
along with the application versions.

In order to perform application management, EF13 also needs to provide
user credentials to be checked against the 209 Access Control List. If
these credentials are valid, the 411 SAC Manager will create 311 Access
Permission Verification for all of the internal Platform Manager
components. In Figure 6, the 410 Application Hosting Manager can pass
311 to 404, 407 and 412 as needed. Although not shown earlier, SAC is
actually incorporated into all GridAPPS-D processes this way.

|uml_image5|

Figure 6: Hosting an application

.. |uml_image0| image:: UML_Diagrams/media/uml_Platform.png
.. |uml_image1| image:: UML_Diagrams/media/uml_VVO.png
.. |uml_image2| image:: UML_Diagrams/media/uml_ModelManagement.png
.. |uml_image3| image:: UML_Diagrams/media/uml_Testing.png
.. |uml_image4| image:: UML_Diagrams/media/uml_Debugging.png
.. |uml_image5| image:: UML_Diagrams/media/uml_Hosting.png
