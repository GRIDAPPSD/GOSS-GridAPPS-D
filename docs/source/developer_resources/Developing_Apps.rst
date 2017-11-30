
Supported Application Types
^^^^^^^^^^^^^^^^^^^^^^^^^^^
- Python
- Java (Jar)

Registering Application With Platform
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Assumptions: GOSS-GridAPPS-D repository (https://github.com/GRIDAPPSD/GOSS-GridAPPS-D.git) is cloned under [ROOT_DIR]

1. Create a [app_name].config file in JSON format with keys and values as described below. where app_name should be unique for the application.

::
	
	{
		"id":"app_name",
		"description":"This is desxription of the app",
		"creator":"orgnization name",
		"inputs":["topic.goss.gridappsd.input1", "topic.goss.gridappsd.input2", ..],
		"outputs":["topic.goss.gridappsd.output1", "topic.goss.gridappsd.output2", ..],
		"options":"space saperated command line input options",
		"execution_path":"absolute/execution/path",
		"type":"PYTHON|JAVA",
		"launch_on_startup":true|false,
		"prereqs":["other_app","other_service",..],
		"multiple_instances":true|false
	}

2. Put [app_name].config file in applications folder under cloned repository location

3. Put your application under applications/[app_name] folder under cloned repository location as shown below.

::
	
	applications
		[app_name]
			app
				Your application goes here
			test
				Test scripts for your application goes here.
				

See Using GridAPPSD section for details on starting a simulation from an application and communicating with plaform.
It also has an example in Python and Java to start a simulation.