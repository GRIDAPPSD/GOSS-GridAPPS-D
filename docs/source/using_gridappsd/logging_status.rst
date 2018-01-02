.. _logging_status:

All processes should publish their log messages with process status to Process Manager. These processes include applications, simulations, services, and test runs.

Topic:
^^^^^^^

Log message with process status should be published on the following topic. Process id should be attached to the topic name at the end.
	
.. code-block:: console

	goss.gridappsd.process.log.simulation.[simulation_id]
	goss.gridappsd.process.log.service.[service_id]
	goss.gridappsd.process.log.application.[app_id]
	goss.gridappsd.process.log.test.[test_id]

Message structure:
^^^^^^^^^^^^^^^^^^

.. code-block:: console

	{
		"process_id": ""
		"timestamp": "",
		"process_status": "[started|stopped|running|error|passed|failed]",
		"log_message": "",
		"log_level": "[info|debug|error]",
		"store_to_db": [true|false]
	}

Receving multiple logs:
^^^^^^^^^^^^^^^^^^^^^^^

User can either receive individual process's log by subcribing to topics mentioned above or recevie all logs of a type by subcribing to following topics.

.. code-block:: console

	goss.gridappsd.process.log.simulation.*
	goss.gridappsd.process.log.service.*
	goss.gridappsd.process.log.application.*
	goss.gridappsd.process.log.test.*

Similarly, to receive to all logs subscribe to following topic:

.. code-block:: console

	goss.gridappsd.process.log.>

 
