#!/bin/bash

echo " "
if [ "$1" = "version" -o "$1" = "-v" -o "$1" = "--version" ]; then
  if [ -f /gridappsd/dockerbuildversion.txt ]; then
    echo -n "version: "
    cat /gridappsd/dockerbuildversion.txt
  else
    echo "Error: can't find version"
  fi
  echo " "
  exit 0
fi

cd /gridappsd

# clean up log files
if [ -d /gridappsd/log ]; then
  /bin/rm -rf /gridappsd/log/* 2 > /dev/null
fi

JAVA_OPTIONS=""
if [ "${AUTOSTART:-0}" != "0" ]; then
  JAVA_OPTIONS=" -Dgosh.args=--nointeractive "
fi

# If the DEBUG environmental variable is set and is not 0
# then expose the port for remote debugging.
# Note: address=*:8000 is required for Java 9+ to accept connections from outside the container
if [ "${DEBUG:-0}" != "0" ]; then
	java ${JAVA_OPTIONS} -agentlib:jdwp=transport=dt_socket,server=y,address=*:8000,suspend=n -jar launcher/gridappsd-launcher.jar
else
	java ${JAVA_OPTIONS} -jar launcher/gridappsd-launcher.jar
fi
