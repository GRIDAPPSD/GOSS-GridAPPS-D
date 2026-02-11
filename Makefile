# GridAPPS-D Makefile
# Common build and development tasks

.PHONY: help build clean dist test test-unit test-integration test-simulation test-container test-stomp-topics \
        run run-bg run-stop run-log docker docker-build docker-up docker-down docker-clean docker-shell docker-logs docker-status docker-versions \
        cache-clear update-dependencies commit push version release snapshot \
        check-api bump-patch bump-minor bump-major next-snapshot \
        format format-check

# Configuration directory
CONFIG ?= conf

# Default target
help:
	@echo "GridAPPS-D Build System"
	@echo "======================="
	@echo ""
	@echo "Build targets:"
	@echo "  make build        - Build the project (compile only)"
	@echo "  make dist         - Build distribution (launcher JAR + bundles)"
	@echo "  make clean        - Clean build artifacts"
	@echo "  make format       - Format all Java files using Spotless"
	@echo "  make format-check - Check formatting without making changes"
	@echo ""
	@echo "Test targets:"
	@echo "  make test              - Run all tests (unit + integration if services available)"
	@echo "  make test-unit         - Run unit tests only (no external dependencies)"
	@echo "  make test-integration  - Run integration tests (requires MySQL, Blazegraph)"
	@echo "  make test-simulation   - Run simulation test in Docker (requires running container)"
	@echo "  make test-container    - Run container-based tests using Testcontainers (auto-starts Docker)"
	@echo "  make test-stomp-topics - Run STOMP topic prefix tests in Docker (requires running container)"
	@echo "  make test-check        - Check if integration test services are available"
	@echo ""
	@echo "Run targets:"
	@echo "  make run          - Run GridAPPS-D (foreground)"
	@echo "  make run-bg       - Run GridAPPS-D in background"
	@echo "  make run-stop     - Stop background GridAPPS-D process"
	@echo "  make run-log      - Tail the background log file"
	@echo ""
	@echo "Docker targets:"
	@echo "  make docker          - Build Docker image (gridappsd/gridappsd:local)"
	@echo "  make docker BASE_VERSION=<tag>  - Build using specific base image tag"
	@echo "  make docker-up       - Start containers with auto-run (AUTOSTART=1 by default)"
	@echo "  make docker-up AUTOSTART=0         - Start containers without auto-run (wait mode)"
	@echo "  make docker-up VERSION=v2025.09.0  - Start with specific version for backport testing"
	@echo "  make docker-down     - Stop containers"
	@echo "  make docker-shell    - Open bash shell in gridappsd container"
	@echo "  make docker-logs     - Tail gridappsd logs"
	@echo "  make docker-status   - Show container status"
	@echo "  make docker-versions - List available Docker Hub versions"
	@echo ""
	@echo "Version targets:"
	@echo "  make version      - Show versions of all bundles (GridAPPS-D + GOSS)"
	@echo "  make check-api    - Analyze API changes and suggest version bump type"
	@echo "  make release VERSION=x.y.z  - Set release version"
	@echo "  make snapshot VERSION=x.y.z - Set snapshot version"
	@echo ""
	@echo "Version bumping:"
	@echo "  make next-snapshot    - Bump patch version after release (e.g., 2.0.0 -> 2.0.1-SNAPSHOT)"
	@echo "  make bump-patch       - Same as next-snapshot"
	@echo "  make bump-minor       - Bump minor version (e.g., 2.0.0 -> 2.1.0-SNAPSHOT)"
	@echo "  make bump-major       - Bump major version (e.g., 2.0.0 -> 3.0.0-SNAPSHOT)"
	@echo ""
	@echo "Utility targets:"
	@echo "  make update-dependencies - Refresh BND repository indexes to pick up new GOSS versions"
	@echo "  make cache-clear  - Clear all Gradle/BND caches and stop daemons (fixes build issues)"
	@echo "  make commit       - Stage and commit changes"
	@echo "  make push         - Push to remote"
	@echo ""

# Build targets
build:
	./gradlew build

dist:
	./gradlew dist -PconfigDir=$(CONFIG)

clean:
	./gradlew clean
	rm -rf build/launcher
	rm -rf felix-cache
	rm -rf */felix-cache

# Test targets
# Run all tests (unit tests always run, integration tests skip if services unavailable)
test:
	./gradlew test

# Run only unit tests (no external dependencies required)
# Excludes *IntegrationTests classes which require MySQL/Blazegraph
test-unit:
	@echo "Running unit tests (excluding integration tests)..."
	./gradlew test -PexcludeIntegrationTests
	@echo ""
	@echo "Unit tests complete. To run integration tests: make test-integration"

# Run only integration tests (requires MySQL on localhost:3306 and Blazegraph on localhost:8889)
test-integration:
	@echo "Running integration tests..."
	@echo "Required services: MySQL (localhost:3306), Blazegraph (localhost:8889)"
	@echo ""
	./gradlew :gov.pnnl.goss.gridappsd:test -PonlyIntegrationTests

# Run simulation integration test inside Docker container
# This is the only practical way to run simulation tests since they require
# GridLAB-D, FNCS/HELICS bridge, and other simulators only available in Docker
# Usage: make test-simulation [SIMULATION_DURATION=10] [VERSION=develop]
# Note: Requires gridappsd/gridappsd:local image built with 'make docker'
#       Start containers with: make docker-up [VERSION=v2025.09.0]
SIMULATION_DURATION ?= 10
test-simulation:
	@echo "Running simulation integration test inside Docker container..."
	@echo "Duration: $(SIMULATION_DURATION) seconds"
	@echo ""
	@if ! docker ps --format '{{.Names}}' | grep -q '^gridappsd$$'; then \
		echo "Error: gridappsd container is not running."; \
		echo ""; \
		echo "To start the Docker environment:"; \
		echo "  1. Build local image: make docker"; \
		echo "  2. Start containers:  make docker-up"; \
		echo "     Or for backport testing: make docker-up VERSION=v2025.09.0"; \
		echo ""; \
		echo "Then re-run: make test-simulation"; \
		exit 1; \
	fi
	@echo "Building test classes..."
	@./gradlew :gov.pnnl.goss.gridappsd:clean :gov.pnnl.goss.gridappsd:testClasses --quiet
	@echo "Copying test classes and JUnit to container..."
	@docker exec gridappsd rm -rf /tmp/test-classes /tmp/classes /tmp/test-libs 2>/dev/null || true
	@docker exec gridappsd mkdir -p /tmp/test-libs
	@docker cp gov.pnnl.goss.gridappsd/bin_test gridappsd:/tmp/test-classes
	@docker cp gov.pnnl.goss.gridappsd/bin gridappsd:/tmp/classes
	@docker cp ~/.m2/repository/junit/junit/4.13.2/junit-4.13.2.jar gridappsd:/tmp/test-libs/
	@docker cp ~/.m2/repository/org/hamcrest/hamcrest/2.2/hamcrest-2.2.jar gridappsd:/tmp/test-libs/
	@echo "Running simulation test ($(SIMULATION_DURATION) seconds)..."
	@echo ""
	@docker exec gridappsd bash -c 'java -cp "/tmp/test-classes:/tmp/classes:/tmp/test-libs/*:$$(find /gridappsd/launcher/bundle -name "*-11.0.0.jar" | tr "\n" ":"):$$(find /gridappsd/launcher/bundle -maxdepth 1 -name "*.jar" | tr "\n" ":")" \
		-Dtest.simulation.duration=$(SIMULATION_DURATION) \
		gov.pnnl.goss.gridappsd.SimulationRunIntegrationTest'

# Run container-based integration tests using Testcontainers
# This automatically starts Docker containers from gridappsd-docker/docker-compose.yml
# Prerequisites:
#   - Docker running
#   - gridappsd/gridappsd:local image built (make docker)
#   - MySQL dump available in gridappsd-docker/dumps/
test-container:
	@echo "Running container-based integration tests using Testcontainers..."
	@echo ""
	@if ! docker info >/dev/null 2>&1; then \
		echo "Error: Docker is not running."; \
		echo "Please start Docker and try again."; \
		exit 1; \
	fi
	@if ! docker images --format '{{.Repository}}:{{.Tag}}' | grep -q 'gridappsd/gridappsd:local'; then \
		echo "Warning: gridappsd/gridappsd:local image not found."; \
		echo "Building it now with 'make docker'..."; \
		$(MAKE) docker; \
	fi
	./gradlew :gov.pnnl.goss.gridappsd:containerTest
	@echo ""
	@echo "Container tests complete."
	@echo "Note: Containers may still be running for reuse. Use 'docker compose down' to stop them."

# Check if integration test services are available
test-check:
	@echo "Checking integration test dependencies..."
	@echo ""
	@echo "MySQL (localhost:3306):"
	@(timeout 2 bash -c 'cat < /dev/null > /dev/tcp/localhost/3306' 2>/dev/null) && echo "  ✓ MySQL port is open" || echo "  ✗ MySQL port is NOT reachable"
	@echo ""
	@echo "Blazegraph (localhost:8889):"
	@curl -s --connect-timeout 2 http://localhost:8889/bigdata/namespace >/dev/null 2>&1 && echo "  ✓ Blazegraph is reachable" || echo "  ✗ Blazegraph is NOT reachable"
	@echo ""
	@echo "Docker containers:"
	@docker ps --format "  {{.Names}}: {{.Status}}" 2>/dev/null | grep -E "mysql|blazegraph|influxdb|proven|gridappsd" || echo "  No relevant containers found"
	@echo ""
	@echo "To start services: make docker-up"
	@echo "For backport testing: make docker-up VERSION=v2025.09.0"

# Run STOMP topic prefix tests inside Docker container
# Verifies that /topic/ prefix is required for STOMP pub/sub messaging
test-stomp-topics:
	@echo "Running STOMP topic prefix tests..."
	@if ! docker ps --format '{{.Names}}' | grep -q '^gridappsd$$'; then \
		echo "Error: gridappsd container is not running."; \
		echo "Start containers with: make docker-up"; \
		exit 1; \
	fi
	docker exec gridappsd bash -c "cd /gridappsd/services/helicsgossbridge && python -m pytest tests/test_stomp_topic_prefix.py -v"

# Run with Docker config (foreground)
run: dist
	@rm -rf build/launcher/felix-cache
	cd build/launcher && java -jar gridappsd-launcher.jar

# Run in background with logging
GRIDAPPSD_LOG ?= gridappsd.out
GRIDAPPSD_PID = .gridappsd.pid

run-bg: dist
	@if [ -f $(GRIDAPPSD_PID) ] && kill -0 $$(cat $(GRIDAPPSD_PID)) 2>/dev/null; then \
		echo "GridAPPS-D is already running (PID: $$(cat $(GRIDAPPSD_PID)))"; \
		echo "Use 'make run-stop' to stop it first"; \
		exit 1; \
	fi
	@rm -rf build/launcher/felix-cache
	@echo "Starting GridAPPS-D in background..."
	@echo "Log file: $(GRIDAPPSD_LOG)"
	@nohup sh -c 'cd build/launcher && exec java -jar gridappsd-launcher.jar' > $(GRIDAPPSD_LOG) 2>&1 & \
		PID=$$!; \
		echo $$PID > $(GRIDAPPSD_PID); \
		sleep 2; \
		if kill -0 $$PID 2>/dev/null; then \
			echo "GridAPPS-D started (PID: $$PID)"; \
			echo "Use 'make run-log' to view logs"; \
			echo "Use 'make run-stop' to stop"; \
		else \
			echo "Failed to start GridAPPS-D. Check $(GRIDAPPSD_LOG) for errors."; \
			rm -f $(GRIDAPPSD_PID); \
			exit 1; \
		fi

run-stop:
	@if [ -f $(GRIDAPPSD_PID) ]; then \
		PID=$$(cat $(GRIDAPPSD_PID)); \
		if kill -0 $$PID 2>/dev/null; then \
			echo "Stopping GridAPPS-D (PID: $$PID)..."; \
			kill $$PID; \
			sleep 2; \
			if kill -0 $$PID 2>/dev/null; then \
				echo "Process still running, sending SIGKILL..."; \
				kill -9 $$PID; \
			fi; \
			echo "GridAPPS-D stopped"; \
		else \
			echo "GridAPPS-D is not running (stale PID file)"; \
		fi; \
		rm -f $(GRIDAPPSD_PID); \
	else \
		echo "No PID file found. GridAPPS-D may not be running."; \
		echo "Looking for java processes..."; \
		pgrep -f gridappsd-launcher.jar || echo "No GridAPPS-D process found"; \
	fi

run-log:
	@if [ -f $(GRIDAPPSD_LOG) ]; then \
		tail -f $(GRIDAPPSD_LOG); \
	else \
		echo "Log file $(GRIDAPPSD_LOG) not found. Start GridAPPS-D with 'make run-bg' first."; \
	fi

# Docker targets
DOCKER_TAG ?= local
BASE_VERSION ?= rc2
VERSION ?= develop

docker:
	./build-gridappsd-container --base-version $(BASE_VERSION) --output-version $(DOCKER_TAG)

docker-build:
	./gradlew clean dist
	docker build \
		--build-arg GRIDAPPSD_BASE_VERSION=:$(BASE_VERSION) \
		--build-arg TIMESTAMP=$$(date +%Y%m%d%H%M%S) \
		--network=host \
		-t gridappsd/gridappsd:$(DOCKER_TAG) .

# New Docker management targets using docker_manager.py
# These use the local docker/docker-compose.yml for self-contained development
AUTOSTART ?= 1
docker-up:
ifeq ($(AUTOSTART),0)
	python3 scripts/docker_manager.py up --version $(VERSION)
else
	python3 scripts/docker_manager.py up --version $(VERSION) --autostart
endif

docker-down:
	python3 scripts/docker_manager.py down

# Remove Docker volumes (mysql-data, redis-data) for a clean start
# Use this when you need to reset database state
docker-clean:
	python3 scripts/docker_manager.py down
	docker volume rm docker_mysql-data docker_redis-data 2>/dev/null || true
	@echo "Docker volumes removed. Next 'make docker-up' will initialize fresh databases."

docker-shell:
	docker exec -it gridappsd bash

docker-logs:
	python3 scripts/docker_manager.py logs

docker-status:
	python3 scripts/docker_manager.py status

docker-versions:
	python3 scripts/docker_manager.py versions

# Legacy targets for gridappsd-docker compatibility
docker-run:
	cd ../gridappsd-docker && docker compose up -d gridappsd

docker-stop:
	cd ../gridappsd-docker && docker compose down

# Refresh BND repository indexes to discover new GOSS bundle versions
# Clears the URL cache (index metadata) and GOSS JAR cache, then rebuilds
update-dependencies:
	@echo "Clearing BND URL cache (forces re-fetch of repository indexes)..."
	rm -rf ~/.bnd/urlcache
	@echo "Clearing cached GOSS bundles..."
	rm -rf "cnf/cache/7.1.0/GOSS Release"
	rm -rf "cnf/cache/7.1.0/GOSS Dependencies"
	@echo "Refreshing dependencies..."
	./gradlew build --refresh-dependencies
	@echo "Dependencies updated."

# Utility targets
cache-clear:
	@echo "Stopping Gradle daemons..."
	./gradlew --stop || true
	@echo "Clearing BND caches..."
	rm -rf ~/.bnd/cache ~/.bnd/urlcache
	rm -rf cnf/cache
	@echo "Clearing Gradle caches..."
	rm -rf ~/.gradle/caches/modules-2/files-2.1/bnd*
	rm -rf ~/.gradle/caches/modules-2/files-2.1/biz.aQute*
	rm -rf ~/.gradle/caches/modules-2/files-2.1/com.diffplug*
	@echo "Clearing local Gradle caches..."
	rm -rf .gradle
	rm -rf buildSrc/.gradle
	@echo "Clearing build directories..."
	rm -rf build
	rm -rf */build
	@echo "Caches cleared. Run './gradlew build' to rebuild."

# Git targets
commit:
	@if [ -z "$(MSG)" ]; then \
		echo "Usage: make commit MSG=\"Your commit message\""; \
		exit 1; \
	fi
	git add -A
	git commit -m "$(MSG)"

push:
	git push

# Full rebuild (clean + dist)
rebuild: clean dist

# Development workflow - rebuild and run
dev: rebuild run

# Version targets
version:
	@python3 scripts/version.py show

release:
ifndef VERSION
	$(error VERSION is required. Usage: make release VERSION=x.y.z)
endif
	@python3 scripts/version.py release $(VERSION)

snapshot:
ifndef VERSION
	$(error VERSION is required. Usage: make snapshot VERSION=x.y.z)
endif
	@python3 scripts/version.py snapshot $(VERSION)

# API change detection
check-api:
	@python3 scripts/check-api.py

# Version bumping commands
bump-patch:
	@python3 scripts/version.py bump-patch
bump-minor:
	@python3 scripts/version.py bump-minor
bump-major:
	@python3 scripts/version.py bump-major
next-snapshot:
	@python3 scripts/version.py next-snapshot
# Code formatting targets (uses Spotless with Eclipse formatter)
format:
	@echo "Formatting Java files..."
	./gradlew spotlessApply
	@echo "Formatting complete."

format-check:
	@echo "Checking code formatting..."
	./gradlew spotlessCheck
	@echo "Format check complete."
