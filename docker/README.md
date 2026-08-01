# GridAPPS-D Docker Development Environment

This directory contains a simplified Docker Compose setup for local development and testing.

## Quick Start

```bash
# From the GOSS-GridAPPS-D directory:

# 1. Build the local GridAPPS-D image
make docker

# 2. Start all containers
make docker-up

# 3. Run tests
make test-simulation

# 4. Stop containers when done
make docker-down
```

## Version Support for Backport Testing

You can test your local code changes against different versions of the supporting containers:

```bash
# Start with specific version for backport testing
make docker-up VERSION=v2025.09.0

# List available versions
make docker-versions
```

The `VERSION` parameter controls the tag used for dependency containers (blazegraph, proven, influxdb).
The GridAPPS-D container always uses your locally-built `:local` image.

## Services

| Service | Port | Description |
|---------|------|-------------|
| gridappsd | 61613, 61614, 61616 | Main GridAPPS-D platform (STOMP, WebSocket, OpenWire) |
| mysql | 3306 | Log and data storage |
| blazegraph | 8889 | RDF/SPARQL triple store with CIM power grid models |
| redis | 6379 | Caching layer |
| influxdb | 8086 | Time-series database for Proven |
| proven | 18080 | Provenance and analytics service |

## Available Make Targets

- `make docker` - Build the local gridappsd:local image
- `make docker-up` - Start containers (default VERSION=develop)
- `make docker-up VERSION=<tag>` - Start with specific dependency version
- `make docker-down` - Stop all containers
- `make docker-logs` - Tail GridAPPS-D logs
- `make docker-status` - Show container status
- `make docker-versions` - List available Docker Hub versions

## Files

- `docker-compose.yml` - Main compose file for development
- `mysql-init.sql` - MySQL initialization script (creates schema)

## Compared to gridappsd-docker

This setup is simpler and designed for development:
- Self-contained in the GOSS-GridAPPS-D repository
- Always uses your locally-built code
- Supports version pinning for backport testing
- No viz container (use API directly for testing)

For production deployments, use the `gridappsd-docker` repository.
