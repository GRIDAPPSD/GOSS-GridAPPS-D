.. _logging_status:

All processes should publish their log messages with process status to Process Manager. These processes include applcations, simulations, services, and test runs.

Topic:
^^^^^^^

Log message with process status should be published on the following topic. Process id should be attached to the topic name at the end.
	
.. code-block:: console

	goss.gridappsd.process.log.[process_id]

Message structure:
^^^^^^^^^^^^^^^^^^

.. code-block:: console

	{
		timestamp: "",
		status: "[started|stopped|running|error|passed|failed]",
		log_message: "",
		log_level: "[info|debug|error]"
	}




 
