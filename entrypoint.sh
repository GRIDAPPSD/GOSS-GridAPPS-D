#!/bin/bash

if [ "$1" = "gridappsd" ]; then
  # Install application python requirements
  for reqfile in `ls /gridappsd/services/*/requirements.txt 2>/dev/null`; do
    echo "[Entrypoint] Installing requirements $reqfile"
    sudo pip install -q --disable-pip-version-check -r $reqfile
  done
  for reqfile in `ls /gridappsd/applications/*/requirements.txt 2>/dev/null`; do
    echo "[Entrypoint] Installing requirements $reqfile"
    sudo pip install -q --disable-pip-version-check -r $reqfile
  done

  # Run tail -f /dev/null to keep the container running and waiting for connection
  echo "[Entrypoint] Waiting for connection"
  tail -f /dev/null
elif [ "$1" = "version" -o "$1" = "-v" -o "$1" = "--version" ]; then
  echo -n "version: "
  cat /gridappsd/dockerbuildversion.txt
fi
