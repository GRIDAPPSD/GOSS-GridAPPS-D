#!/bin/bash
# Restart GridAPPS-D inside the running container

# Kill any existing java process
docker exec gridappsd pkill -9 -f java 2>/dev/null

# Wait for port to be released
sleep 2

# Clear Felix cache to avoid stale bundle state issues
rm -rf build/launcher/felix-cache 2>/dev/null

# Start GridAPPS-D
docker exec gridappsd bash -c "cd /gridappsd && DEBUG=1 ./run-gridappsd.sh"
