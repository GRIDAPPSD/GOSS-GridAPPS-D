RC1 Demo Overview
^^^^^^^^^^^^^^^^^

In this procedure, we log in to a Linux machine using MobaXterm on Windows. 
 
1. Start gridappsd.
      a. Open a terminal and ssh to 172.20.128.20.
      b. Switch to the gridappsd user by typing *sudo su - gridappsd*
      c. Type *cd $HOME/gridappds_project/sources/GOSS-GridAPPS-D*
      d. Type *./run-goss-test.sh* You may not see any output and it doesn't exit.
 
2. Start the node server for the viz application.
      a. Open another terminal and ssh to 172.20.128.20.
      b. Switch to the gridappsd user by typing *sudo su - gridappsd*
      c. Type *cd $HOME/gridappsd_project/sources/viz*
      d. Start the node server by typing *node server.js* You may not see any output and it doesn't exit.
 
3. Start the viz demo. This requires a browser using an SSH tunnel, which MobaXterm establishes in the log in process.
      a. In a browser go to http://172.20.128.20:8082/ieee8500
      b. Click on the IEEE 8500 link in the top left of the webpage.
      c. Click the play button in the top right of the webpage. It will take several seconds before you see the graphs being generated. Somewhere in between 5-10 seconds.
 

|rc1_overview_image0|


.. |rc1_overview_image0| image:: rc1_demo.png


