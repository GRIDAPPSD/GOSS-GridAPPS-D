# GridAPPS-D Platform

GridAPPS-D (Grid Application Platform for Planning and Simulation with Distribution) is an open-source platform for developing and deploying advanced distribution system applications. Built on the GOSS (GridOPTICS Software System) messaging framework, it provides a standardized environment for power grid simulation, data management, and application integration.

## Documentation

Full documentation is available at: http://gridappsd.readthedocs.io/

## Prerequisites

- **Java 21** (OpenJDK recommended)
- **Docker** and **docker-compose** (for containerized deployment)
- **Gradle 8.10+** (included via wrapper)

## Quick Start

### Building from Source

```bash
# Clone the repository
git clone https://github.com/GRIDAPPSD/GOSS-GridAPPS-D.git
cd GOSS-GridAPPS-D

# Build the platform
./gradlew build

# Build the distribution (creates launcher JAR with all bundles)
./gradlew dist
```

### Running GridAPPS-D

```bash
# Run the launcher
cd build/launcher
java -jar gridappsd-launcher.jar
```

### Docker Deployment

For production deployment, use the [gridappsd-docker](https://github.com/GRIDAPPSD/gridappsd-docker) repository:

```bash
git clone https://github.com/GRIDAPPSD/gridappsd-docker.git
cd gridappsd-docker
./run.sh
```

## Project Structure

```
GOSS-GridAPPS-D/
├── gov.pnnl.goss.gridappsd/       # Main GridAPPS-D platform bundle
│   ├── src/                       # Source code
│   │   └── gov/pnnl/goss/gridappsd/
│   │       ├── api/               # Manager interfaces
│   │       ├── app/               # Application management
│   │       ├── configuration/     # Configuration handlers
│   │       ├── data/              # Data managers and handlers
│   │       ├── dto/               # Data transfer objects
│   │       ├── process/           # Request processing
│   │       ├── service/           # Service management
│   │       ├── simulation/        # Simulation orchestration
│   │       └── testmanager/       # Test framework
│   ├── test/                      # Unit tests
│   └── launcher/                  # OSGi launcher
├── gov.pnnl.goss.gridappsd.test/  # Integration tests
├── gridappsd-jena/                # Apache Jena wrapper bundle
├── gridappsd-poi/                 # Apache POI wrapper bundle
├── gridappsd-proven/              # Proven timeseries wrapper bundle
├── cnf/                           # BND workspace configuration
└── build.gradle                   # Gradle build configuration
```

## Architecture

GridAPPS-D is built as an OSGi application on top of the GOSS messaging framework:

### Core Managers

| Manager | Description |
|---------|-------------|
| **ProcessManager** | Central request router for all platform operations |
| **SimulationManager** | Orchestrates simulation lifecycle (start/stop/pause/resume) |
| **ConfigurationManager** | Generates simulator-specific configuration files |
| **AppManager** | Application registration and lifecycle management |
| **ServiceManager** | Service registration and lifecycle management |
| **DataManager** | Routes data requests to registered handlers |
| **TestManager** | Injects events into simulations and validates results |
| **LogManager** | Centralized logging for all platform components |

### Supported Simulators

- **GridLAB-D** - Distribution power flow simulator
- **OpenDSS** - Alternative distribution simulator
- **OCHRE** - Building/load simulator

### Co-Simulation Bridges

- **HELICS** - Hierarchical Engine for Large-scale Infrastructure Co-Simulation
- **FNCS** - Framework for Network Co-Simulation (legacy)

## Message Bus Topics

GridAPPS-D uses ActiveMQ for message-based communication:

### Request Topics
- `goss.gridappsd.process.request.simulation` - Simulation control
- `goss.gridappsd.process.request.data` - Data queries
- `goss.gridappsd.process.request.config` - Configuration generation
- `goss.gridappsd.process.request.app` - Application management

### Simulation Topics
- `goss.gridappsd.simulation.input.<simId>` - Control inputs
- `goss.gridappsd.simulation.output.<simId>` - Measurement outputs
- `goss.gridappsd.simulation.log.<simId>` - Simulation logs

## Development

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew :gov.pnnl.goss.gridappsd:test --tests SimulationManagerTests
```

### Building Docker Image

```bash
# Build and copy to Docker container
./build-docker.sh
```

### Version Management

GridAPPS-D uses [Semantic Versioning](https://semver.org/) with automated API change detection:

| Change Type | Version Bump | Example |
|-------------|--------------|---------|
| **MAJOR** | X.0.0 | Interface changes, removed public methods, breaking changes |
| **MINOR** | x.Y.0 | New public methods on classes, new classes (backward compatible) |
| **PATCH** | x.y.Z | Implementation-only changes, bug fixes |

#### Basic Commands

```bash
make version              # Show versions of all bundles
make build                # Build all bundles
make test                 # Run tests
make clean                # Clean build artifacts
```

#### API Change Detection

Before bumping versions, analyze your changes to determine the appropriate version bump:

```bash
make check-api            # Analyze API changes and get recommendation
```

#### Version Bumping Commands

```bash
# Automatic version bumping (reads current version, increments appropriately)
make bump-major           # 2.0.0 -> 3.0.0-SNAPSHOT (breaking changes)
make bump-minor           # 2.0.0 -> 2.1.0-SNAPSHOT (new features)
make bump-patch           # 2.0.0 -> 2.0.1-SNAPSHOT (bug fixes)
make next-snapshot        # Same as bump-patch (use after release)

# Manual version setting
make release VERSION=2.0.0    # Set exact release version (removes -SNAPSHOT)
make snapshot VERSION=2.1.0   # Set exact snapshot version (adds -SNAPSHOT)
```

#### Complete Release Workflow

```bash
# 1. Analyze changes to determine version bump type
make check-api

# 2. If currently on snapshot, set release version
make version                      # Verify: 2.0.0-SNAPSHOT
make release VERSION=2.0.0        # Changes to: 2.0.0

# 3. Build and test
make build && make test

# 4. Tag and commit release
git add -A && git commit -m "Release 2.0.0"
git tag v2.0.0
git push && git push --tags

# 5. Start next development cycle
make next-snapshot                # Bumps to: 2.0.1-SNAPSHOT
git add -A && git commit -m "Start 2.0.1-SNAPSHOT development"
git push
```

#### API Compatibility Guidelines

**MAJOR version bump required:**
- Adding or removing methods from an **interface** (breaks implementors)
- Removing public methods from a class
- Changing method signatures (parameters, return types)
- Changing class hierarchy (superclass, implemented interfaces)

**MINOR version bump required:**
- Adding new public methods to a **class** (not interface)
- Adding new classes
- Adding new packages

**PATCH version bump required:**
- Bug fixes with no API changes
- Performance improvements
- Internal refactoring
- Documentation updates

## Technology Stack

- **Java 21** with modern language features
- **OSGi** (Apache Felix) for modular service architecture
- **BND Tools** for OSGi bundle management
- **Apache ActiveMQ 6.x** for messaging (Jakarta EE compatible)
- **Apache Shiro 2.0** for security
- **Blazegraph** for RDF/SPARQL triple store (CIM models)
- **MySQL** for log storage

## Related Repositories

- [GOSS](https://github.com/GridOPTICS/GOSS) - Core messaging framework
- [GOSS-Repository](https://github.com/GridOPTICS/GOSS-Repository) - OSGi bundle repository
- [gridappsd-docker](https://github.com/GRIDAPPSD/gridappsd-docker) - Docker deployment
- [gridappsd-python](https://github.com/GRIDAPPSD/gridappsd-python) - Python client library
- [CIMHub](https://github.com/GRIDAPPSD/CIMHub) - CIM model utilities

## License

This project is licensed under the BSD-3-Clause License. See [LICENSE](LICENSE) for details.

## Contributing

Contributions are welcome! Please read our contributing guidelines and submit pull requests to the `develop` branch.

## Support

- **Documentation**: http://gridappsd.readthedocs.io/
- **Issues**: https://github.com/GRIDAPPSD/GOSS-GridAPPS-D/issues
