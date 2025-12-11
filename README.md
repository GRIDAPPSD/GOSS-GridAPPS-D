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

## Technology Stack

- **Java 21** with modern language features
- **OSGi** (Apache Felix) for modular service architecture
- **BND Tools** for OSGi bundle management
- **Apache ActiveMQ 6.x** for messaging (Jakarta EE compatible)
- **Apache Shiro 2.0** for security
- **Blazegraph** for RDF/SPARQL triple store (CIM models)
- **MySQL** for log storage

## Related Repositories

- [GOSS](https://github.com/GRIDAPPSD/GOSS) - Core messaging framework
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
