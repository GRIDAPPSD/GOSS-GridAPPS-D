#!/bin/bash

# GridAPPS-D Launcher Script
# This script launches the GridAPPS-D platform using the Felix OSGi framework

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
LAUNCHER_JAR="$SCRIPT_DIR/gridappsd-launcher.jar"

echo "GridAPPS-D Launcher"
echo "==================="

# Check if launcher JAR exists
if [ ! -f "$LAUNCHER_JAR" ]; then
    echo "Error: Launcher JAR not found at: $LAUNCHER_JAR"
    echo "Please run: ./gradlew dist"
    exit 1
fi

# Check if bundle directory exists
if [ ! -d "$SCRIPT_DIR/bundle" ]; then
    echo "Error: Bundle directory not found"
    echo "Please run: ./gradlew dist"
    exit 1
fi

# Set Java options
JAVA_OPTS="${JAVA_OPTS:--Xmx2g -Xms512m}"

# Enable debug mode if DEBUG environment variable is set
if [ "${DEBUG:-0}" != "0" ]; then
    JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=n"
    echo "Debug mode enabled on port 8000"
fi

# Enable non-interactive mode if AUTOSTART is set
if [ "${AUTOSTART:-0}" != "0" ]; then
    JAVA_OPTS="$JAVA_OPTS -Dgosh.args=--nointeractive"
    echo "Non-interactive mode enabled"
fi

echo "Starting GridAPPS-D..."
echo "Java options: $JAVA_OPTS"
echo ""

# Run the launcher
exec java $JAVA_OPTS -jar "$LAUNCHER_JAR"
