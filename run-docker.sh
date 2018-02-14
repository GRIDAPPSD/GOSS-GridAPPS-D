#!/bin/bash

# Setup the path for running the gridappsd framework
export PATH=/gridappsd/services/fncsgossbridge/service:$PATH

cd /gridappsd

# If the DEBUG environmental variable is set and is not 0
# then expose the port for remote debugging.
if [ "${DEBUG:-0}" != "0" ]; then
	java -agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=n -jar lib/run.bnd.jar 
else
	java -jar lib/run.bnd.jar
fi