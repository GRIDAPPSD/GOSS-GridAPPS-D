# GridAPPS-D Makefile
# Common build and development tasks

.PHONY: help build clean dist test test-unit test-integration run docker docker-build docker-run \
        cache-clear goss goss-build goss-test commit push version release snapshot \
        release-snapshot release-release check-api bump-patch bump-minor bump-major next-snapshot \
        format format-check

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
	@echo "  make test-check        - Check if integration test services are available"
	@echo ""
	@echo "Run targets:"
	@echo "  make run          - Run GridAPPS-D locally (from build/launcher)"
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
	./gradlew dist

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

# Run locally
run: dist
	cd build/launcher && java -jar gridappsd-launcher.jar

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
