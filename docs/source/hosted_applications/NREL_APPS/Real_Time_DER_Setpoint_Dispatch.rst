Distribution Optimal Power Flow for Real-Time Setpoint Dispatch
---------------------------------------------------------------

Objectives
~~~~~~~~~~

This application is designed to address the problem of optimizing the
operation of aggregations of heterogeneous energy resources connected to
a distribution system. We will focus on real=time optimization method
and the power setting points of the distributed energy resources (DERs)
will be updated on a second or subsecond timescale to maximize the
operational objectives while coping with the variability of ambient
conditions and noncontrollable energy assets [1]. In order to avoid
massive measurements and overcome the limitation caused by model
inaccuracy, this application will be implemented in a distributed
manner, and only local measurements and a feedback signal from the
substation aggregator are needed to determine the optimal setpoints for
each controlled DER unit.

|nrel_OPF_image0|

**Figure 1 The conceptual framework of distribution OPF for real=time
setpoint dispatch.**

Figure 1 shows the conceptual framework of this application, and this
application is targeting at TRL 3.

Design
~~~~~~

Figure 2 describes the overall work flow of the application.
Distribution OPF algorithm requires real=time measurements, distribution
system model and power flow results, which will be obtained from
GridAPPS=D platform through GOSS/FNCS message bus. The optimization
problem formulation can be constructed using user=defined cost functions
for different controllable devices. Finally the optimal setpoints for
controllable devices will be solved based on the feedback information
from system measurements. These setpoints will be sent back to GridLab=D
grid model to update DER operations. Such a closed=loop control forms
the control iteration for the studied time point, and new setpoints for
the following time points will be determined in the same manner using
the updated model and measurements.

|nrel_OPF_image1|

**Figure 2 The workflow of real=time setpoint dispatch application and
its interaction with GridApps=D.**

Data requirements

Message schemas (UML) (Enterprise Architect software) Jeff will help
draw the UML diagram.

Testing and Validation
~~~~~~~~~~~~~~~~~~~~~~

Evaluation metrics of this application:

=  Real/reactive power at the substation

=  System loss

=  Voltages across the entire distribution grid: voltage magnitude,
   voltage fluctuation, voltage unbalance.

=  Legacy control device operations: total control actions of all
   capacitors and regulators

Scenarios:

=  Optimal Dispatch for Distributed PV Systems

=  Optimal Dispatch for Distributed PV + Energy Storage

=  Etc. (will be added when implementing the application)

Operating/Running
~~~~~~~~~~~~~~~~~

This application will be developed using Python.

References
~~~~~~~~~~

[1] E. Dall'Anese, A. Bernstein, and A. Simonetto, "Feedback=based
Projected=gradient Method for Real=time Optimization of Aggregations
of Energy Resources," IEEE Global Conference on Signal and Information
Processing (GlobalSIP), Montreal, Canada, Nov. 2017. 

.. |nrel_OPF_image0| image:: NREL_APPS/media/image1.png
.. |nrel_OPF_image1| image:: NREL_APPS/media/Distribution_OPF2.png


