#!/bin/bash
# Restart GridAPPS-D inside the running container

# Rebuild Java code first
echo "Building GridAPPS-D..."
./gradlew --parallel :gov.pnnl.goss.gridappsd:build -x test || { echo "Build failed"; exit 1; }

# Kill any existing java and python processes
echo "Stopping existing processes..."
docker exec gridappsd pkill -9 -f java 2>/dev/null
docker exec gridappsd pkill -9 -f python 2>/dev/null
docker exec gridappsd pkill -9 -f gridlabd 2>/dev/null

# Wait for port to be released
sleep 2

# Clear Felix cache to avoid stale bundle state issues
rm -rf build/launcher/felix-cache 2>/dev/null

# Start GridAPPS-D in background
echo "Starting GridAPPS-D..."
docker exec -d gridappsd bash -c "cd /gridappsd && DEBUG=1 ./run-gridappsd.sh"

echo "GridAPPS-D restarting in background. Check logs with: docker logs -f gridappsd"

# Wait for platform to be ready (check for ActiveMQ port from outside)
echo "Waiting for GridAPPS-D to start..."
for i in {1..60}; do
    if timeout 2 bash -c 'echo > /dev/tcp/localhost/61616' 2>/dev/null; then
        echo "GridAPPS-D is ready (ActiveMQ port 61616 is listening)"
        exit 0
    fi
    sleep 2
done

echo "Warning: GridAPPS-D may not have started within timeout. Check logs."
exit 1
