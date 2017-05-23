.. using


Using GridAPPS-D
================

Overview
--------

.. include:: rc1_overview.rst

Run Configuration
-----------------

.. include:: run_configuration.rst

Starting Simulation Using API
-----------------------------

GridAPPS-D communicates over a publish subscribe architecture implemented in ActiveMQ.  A number of communication protocols are supported, including Openwire, STOMP, and websockets.  Many programming languages support communication over these protocols, below are three examples.

.. include:: api_examples/starting_in_java.rst
.. include:: api_examples/starting_in_websockets.rst
.. include:: api_examples/starting_in_python.rst

Starting Simulation Using Viz Application
-----------------------------------------

.. include:: starting_in_viz.rst
