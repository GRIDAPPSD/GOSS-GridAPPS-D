#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONTAINER_NAME="gridappsd"
NETWORK_NAME="gridappsd-docker_default"
IMAGE_NAME="${GRIDAPPSD_IMAGE:-gridappsd/gridappsd:local}"

# Build GridAPPS-D distribution
echo "Building GridAPPS-D distribution..."
./gradlew clean dist

# Function to start the gridappsd container
start_container() {
    echo "Starting gridappsd container..."

    # Check if container exists but is stopped
    if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
        echo "Removing existing stopped container..."
        docker rm -f ${CONTAINER_NAME} 2>/dev/null || true
    fi

    # Check if the network exists
    if ! docker network ls --format '{{.Name}}' | grep -q "^${NETWORK_NAME}$"; then
        echo "Creating network ${NETWORK_NAME}..."
        docker network create ${NETWORK_NAME}
    fi

    # Start container with the built artifacts mounted to /gridappsd/launcher/
    # Note: Using /gridappsd/launcher to avoid overriding /gridappsd/lib which contains GridLAB-D modules
    echo "Starting container with local build mounted..."
    docker run -d \
        --name ${CONTAINER_NAME} \
        --network ${NETWORK_NAME} \
        -p 61613:61613 \
        -p 61614:61614 \
        -p 61616:61616 \
        -v "${SCRIPT_DIR}/build/launcher/gridappsd-launcher.jar:/gridappsd/launcher/gridappsd-launcher.jar:ro" \
        -v "${SCRIPT_DIR}/build/launcher/bundle:/gridappsd/launcher/bundle:ro" \
        -v "${SCRIPT_DIR}/build/launcher/config.properties:/gridappsd/launcher/config.properties:ro" \
        -e "GRIDAPPSD_BLAZEGRAPH_HOST=blazegraph" \
        -e "GRIDAPPSD_MYSQL_HOST=mysql" \
        -e "GRIDAPPSD_INFLUXDB_HOST=influxdb" \
        -e "GRIDAPPSD_REDIS_HOST=redis" \
        ${IMAGE_NAME} \
        gridappsd

    echo "Container started. Waiting for it to be ready..."
    sleep 3

    # Verify container is still running
    if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
        echo "ERROR: Container failed to start. Logs:"
        docker logs ${CONTAINER_NAME}
        exit 1
    fi
}

# Check if gridappsd container is running
if docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "Container is running. Updating artifacts..."

    # Copy built artifacts to running container's launcher/ directory
    echo "Copying launcher JAR to container..."
    docker cp build/launcher/gridappsd-launcher.jar ${CONTAINER_NAME}:/gridappsd/launcher/gridappsd-launcher.jar

    echo "Copying bundles to container..."
    docker cp build/launcher/bundle/. ${CONTAINER_NAME}:/gridappsd/launcher/bundle/

    echo "Copying config.properties to container..."
    docker cp build/launcher/config.properties ${CONTAINER_NAME}:/gridappsd/launcher/config.properties
else
    # Container not running - start it with mounts
    start_container
fi

echo ""
echo "============================================"
echo "Build and deployment complete!"
echo "============================================"
echo ""
echo "To start GridAPPS-D inside the container:"
echo "  docker exec -it ${CONTAINER_NAME} bash"
echo "  ./run-gridappsd.sh"
echo ""
echo "Or run directly:"
echo "  docker exec -it ${CONTAINER_NAME} ./run-gridappsd.sh"
echo ""
