#!/bin/bash

if [ "$1" = "gridappsd" ]; then
  # Copy user-provided configuration overrides into /gridappsd/conf
  # This preserves defaults while letting users override specific files
  if [ -d /conf ] && [ "$(ls -A /conf 2>/dev/null)" ]; then
    echo "[Entrypoint] Copying configuration overrides from /conf to /gridappsd/conf"
    cp -rv /conf/* /gridappsd/conf/
  fi

  # Install application python requirements
  for reqfile in `ls /gridappsd/services/*/requirements.txt 2>/dev/null`; do
    echo "[Entrypoint] Installing requirements $reqfile"
    sudo pip install -q --disable-pip-version-check --root-user-action=ignore -r $reqfile
  done
  for reqfile in `ls /gridappsd/applications/*/requirements.txt 2>/dev/null`; do
    echo "[Entrypoint] Installing requirements $reqfile"
    sudo pip install -q --disable-pip-version-check --root-user-action=ignore -r $reqfile
  done


  if [ "${AUTOSTART:-0}" != "0" ]; then
    /gridappsd/run-gridappsd.sh
  else
    # Run tail -f /dev/null to keep the container running and waiting for connection
    echo "[Entrypoint] Waiting for connection"
     tail -f /dev/null
  fi
elif [ "$1" = "version" -o "$1" = "-v" -o "$1" = "--version" ]; then
  echo -n "version: "
  cat /gridappsd/dockerbuildversion.txt
fi
