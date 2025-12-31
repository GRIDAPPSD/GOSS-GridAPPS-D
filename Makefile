# GridAPPS-D Makefile
# Common build and development tasks

.PHONY: help build clean dist test test-unit test-integration test-simulation \
        run run-bg run-stop run-log docker docker-build docker-run \
        cache-clear goss goss-build goss-test commit push version release snapshot \
        release-snapshot release-release check-api bump-patch bump-minor bump-major next-snapshot \
        format format-check run-local run-local-bg

# Configuration directory: 'conf' (default, Docker) or 'local-conf' (local development)
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
	@echo "  make test-check        - Check if integration test services are available"
	@echo ""
	@echo "Run targets:"
	@echo "  make run          - Run GridAPPS-D (foreground, Docker config)"
	@echo "  make run-bg       - Run GridAPPS-D in background (Docker config)"
	@echo "  make run-local    - Run GridAPPS-D (foreground, local dev config)"
	@echo "  make run-local-bg - Run GridAPPS-D in background (local dev config)"
	@echo "  make run-stop     - Stop background GridAPPS-D process"
	@echo "  make run-log      - Tail the background log file"
	@echo ""
	@echo "Docker targets:"
	@echo "  make docker       - Build Docker image (gridappsd/gridappsd:local)"
	@echo "  make docker-run   - Run Docker container"
	@echo ""
	@echo "GOSS targets:"
	@echo "  make goss         - Build GOSS framework"
	@echo "  make goss-test    - Run GOSS tests"
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
	@echo "Repository targets (local GOSS-Repository):"
	@echo "  make release-snapshot - Build GOSS and push snapshots to ../GOSS-Repository"
	@echo "  make release-release  - Build GOSS and push releases to ../GOSS-Repository"
	@echo ""
	@echo "Utility targets:"
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
# Usage: make test-simulation [SIMULATION_DURATION=10]
SIMULATION_DURATION ?= 10
test-simulation:
	@echo "Running simulation integration test inside Docker container..."
	@echo "Duration: $(SIMULATION_DURATION) seconds"
	@echo ""
	@if ! docker ps --format '{{.Names}}' | grep -q '^gridappsd$$'; then \
		echo "Error: gridappsd container is not running."; \
		echo ""; \
		echo "To start the Docker environment:"; \
		echo "  cd ../gridappsd-docker && ./run.sh"; \
		echo ""; \
		echo "Then re-run: make test-simulation"; \
		exit 1; \
	fi
	@echo "Building test classes..."
	@./gradlew :gov.pnnl.goss.gridappsd:testClasses --quiet
	@echo "Copying test classes to container..."
	@docker cp gov.pnnl.goss.gridappsd/generated/test-classes gridappsd:/tmp/test-classes
	@docker cp gov.pnnl.goss.gridappsd/generated/classes gridappsd:/tmp/classes
	@echo "Running simulation test ($(SIMULATION_DURATION) seconds)..."
	@echo ""
	@docker exec gridappsd java -cp "/tmp/test-classes:/tmp/classes:/gridappsd/lib/*" \
		-Dtest.simulation.duration=$(SIMULATION_DURATION) \
		gov.pnnl.goss.gridappsd.SimulationRunIntegrationTest

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
	@docker ps --format "  {{.Names}}: {{.Status}}" 2>/dev/null | grep -E "mysql|blazegraph|influxdb" || echo "  No relevant containers found"
	@echo ""
	@echo "To start services: cd ../gridappsd-docker && ./run.sh"

# Run with Docker config (foreground)
run: dist
	@rm -rf build/launcher/felix-cache
	cd build/launcher && java -jar gridappsd-launcher.jar

# Run with local dev config (foreground)
run-local:
	$(MAKE) dist CONFIG=local-conf
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

# Run with local dev config in background
run-local-bg:
	$(MAKE) run-bg CONFIG=local-conf

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

docker:
	./build-gridappsd-container --base-version $(BASE_VERSION) --output-version $(DOCKER_TAG)

docker-build:
	./gradlew clean dist
	docker build \
		--build-arg GRIDAPPSD_BASE_VERSION=:$(BASE_VERSION) \
		--build-arg TIMESTAMP=$$(date +%Y%m%d%H%M%S) \
		--network=host \
		-t gridappsd/gridappsd:$(DOCKER_TAG) .

docker-run:
	cd ../gridappsd-docker && docker compose up -d gridappsd

docker-stop:
	cd ../gridappsd-docker && docker compose down

docker-logs:
	docker logs -f gridappsd

# GOSS framework targets
GOSS_DIR = ../GOSS

goss:
	cd $(GOSS_DIR) && ./gradlew build

goss-test:
	cd $(GOSS_DIR) && ./gradlew check

goss-clean:
	cd $(GOSS_DIR) && ./gradlew clean

# Utility targets
cache-clear:
	@echo "Stopping Gradle daemons..."
	./gradlew --stop || true
	cd $(GOSS_DIR) && ./gradlew --stop || true
	@echo "Clearing BND caches..."
	rm -rf ~/.bnd/cache ~/.bnd/urlcache
	rm -rf cnf/cache
	rm -rf $(GOSS_DIR)/cnf/cache
	@echo "Clearing Gradle caches..."
	rm -rf ~/.gradle/caches/modules-2/files-2.1/bnd*
	rm -rf ~/.gradle/caches/modules-2/files-2.1/biz.aQute*
	rm -rf ~/.gradle/caches/modules-2/files-2.1/com.diffplug*
	@echo "Clearing local Gradle caches..."
	rm -rf .gradle
	rm -rf $(GOSS_DIR)/.gradle
	rm -rf buildSrc/.gradle
	rm -rf $(GOSS_DIR)/buildSrc/.gradle
	@echo "Clearing build directories..."
	rm -rf build
	rm -rf */build
	rm -rf $(GOSS_DIR)/build
	rm -rf $(GOSS_DIR)/*/build
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

# Repository release targets
GOSS_REPO_DIR = ../GOSS-Repository

release-snapshot: goss
	@echo "Pushing GOSS snapshot bundles to GOSS-Repository..."
	cd $(GOSS_DIR) && python3 push-to-local-goss-repository.py --snapshot
	@echo ""
	@echo "Generating repository indexes..."
	cd $(GOSS_REPO_DIR) && ./generate-repository-index.sh snapshot

release-release: goss
	@echo "Pushing GOSS release bundles to GOSS-Repository..."
	cd $(GOSS_DIR) && python3 push-to-local-goss-repository.py --release
	@echo ""
	@echo "Generating repository indexes..."
	cd $(GOSS_REPO_DIR) && ./generate-repository-index.sh release

# API change detection
check-api:
	@python3 scripts/check-api.py

# Version bumping commands
bump-patch:
	@python3 scripts/version.py bump-patch --gridappsd-only

bump-minor:
	@python3 scripts/version.py bump-minor --gridappsd-only

bump-major:
	@python3 scripts/version.py bump-major --gridappsd-only

next-snapshot:
	@python3 scripts/version.py next-snapshot --gridappsd-only

# Code formatting targets (uses Spotless with Eclipse formatter)
format:
	@echo "Formatting Java files..."
	./gradlew spotlessApply
	@echo "Formatting complete."

format-check:
	@echo "Checking code formatting..."
	./gradlew spotlessCheck
	@echo "Format check complete."
