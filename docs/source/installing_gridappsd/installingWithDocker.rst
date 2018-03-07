
Requirements
------------

* git
* docker version 17.12 or higher
* docker-compose version 1.16.1 or higher

Docker and prerequisite install on OS X
----------------------------------------

* git
   * OS X requires xcode


.. code-block:: bash

        xcode-select --install
..

Clone or download the repository
--------------------------------

.. code-block:: bash

  git clone https://github.com/GRIDAPPSD/gridappsd-docker
  cd gridappsd-docker

..

Install Docker on Ubuntu
------------------------------------------

  * run the docker-ce installation script

.. code-block:: bash

     ./docker_install_ubuntu.sh
..
  * log out of your Ubuntu session and log back in to make the docker groups change active

Start the docker container services
-----------------------------------

.. code-block:: bash

  ./run.sh

..

The run.sh does the following
 *  download the mysql dump file
 *  download the blazegraph data
 *  start the docker containers
 *  ingest the blazegraph data
 *  connect to the gridappsd container

Start gridappsd
---------------

Now we are inside the executing container

.. code-block:: bash

  root@737c30c82df7:/gridappsd# ./run-docker.sh

..


Open your browser to http://localhost:8080/


Exiting the container and stopping the containers
-------------------------------------------------

.. code-block:: bash

  Use Ctrl+C to stop gridappsd from running
  exit
  ./stop.sh

.. 

Restarting the containers
-------------------------

.. code-block:: bash

  ./run.sh

.. 

Reconnecting to the running gridappsd container

.. code-block:: bash

  user@foo>docker exec -it gridappsddocker_gridappsd_1 bash

..

Future enhancements    
-------------------
  *  open a web browser to the viz container http://localhost:8080
