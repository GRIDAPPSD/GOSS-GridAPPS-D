#!/bin/bash

# Install application python requirements
for reqfile in `ls /gridappsd/services/*/requirements.txt 2>/dev/null`; do
  echo "[Entrypoint] Installing requirements $reqfile"
  pip install -r $reqfile
done
for reqfile in `ls /gridappsd/applications/*/requirements.txt 2>/dev/null`; do
  echo "[Entrypoint] Installing requirements $reqfile"
  pip install -r $reqfile
done

if [ "$1" = 'gridappsd' ]; then
  # Run tail -f /dev/null to keep the container running and waiting for connection
  echo "[Entrypoint] Waiting for connection"
  tail -f /dev/null
fi
