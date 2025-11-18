# GridAPPS-D Felix Launcher

## Overview

GridAPPS-D now uses a custom Apache Felix launcher instead of the BND export mechanism. This change was necessary due to compatibility issues between BND's export task and Java 21.

The launcher provides a Java 21-compatible way to run GridAPPS-D as a standalone OSGi application using the Apache Felix framework.

## Building the Distribution

To build the complete GridAPPS-D distribution with the launcher:

```bash
./gradlew dist
```

This will:
1. Compile all GridAPPS-D OSGi bundles
2. Collect all dependency bundles from GOSS
3. Download Felix framework and essential Felix bundles
4. Compile the custom launcher
5. Package everything into `build/launcher/`

The distribution includes:
- `gridappsd-launcher.jar` - The launcher application
- `bundle/` - Directory containing all OSGi bundles (89 bundles)
- `config.properties` - Felix framework configuration

## Running GridAPPS-D

### Standard Run

```bash
cd build/launcher
java -jar gridappsd-launcher.jar
```

### Debug Mode

To enable remote debugging on port 8000:

```bash
DEBUG=1 java -jar gridappsd-launcher.jar
```

Or manually:

```bash
java -agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=n -jar gridappsd-launcher.jar
```

### Non-Interactive Mode

For automated/headless deployments:

```bash
java -Dgosh.args=--nointeractive -jar gridappsd-launcher.jar
```

### Memory Configuration

Adjust JVM memory settings:

```bash
java -Xmx4g -Xms1g -jar gridappsd-launcher.jar
```

## Configuration

The `config.properties` file controls the Felix framework and GridAPPS-D settings:

### Framework Settings

```properties
# OSGi framework storage
org.osgi.framework.storage=felix-cache
org.osgi.framework.storage.clean=onFirstInit

# Bundle directory
bundle.dir=bundle

# System packages exported from JVM
org.osgi.framework.system.packages.extra=sun.misc
```

### GridAPPS-D Settings

```properties
# GOSS/GridAPPS-D configuration
goss.activemq.host=localhost
goss.openwire.port=61616
goss.stomp.port=61613
goss.ws.port=61614
goss.broker-name=broker
goss.activemq.start.broker=true
```

## Docker Deployment

For Docker deployment, the launcher can be packaged instead of the old BND export JAR:

```dockerfile
# Copy launcher distribution
COPY build/launcher /gridappsd

# Run GridAPPS-D
WORKDIR /gridappsd
CMD ["java", "-jar", "gridappsd-launcher.jar"]
```

## Architecture

The launcher follows the Apache Felix recommended pattern:

1. **Launcher Class** (`GridAPPSDLauncher.java`)
   - Loads configuration from `config.properties`
   - Creates Felix framework using `FrameworkFactory` service
   - Installs all bundles from the `bundle/` directory
   - Starts bundles (skipping fragments)
   - Handles graceful shutdown

2. **Bundle Directory**
   - All OSGi bundles are placed in `bundle/`
   - Bundles are loaded and started automatically
   - No manual bundle management required

3. **Configuration**
   - Java properties file format
   - Supports Felix framework configuration
   - Supports GridAPPS-D/GOSS system properties

## Comparison with BND Export

### Old Approach (BND Export)
- ❌ **Broken on Java 21** - `addClose()` API incompatibility
- Uses `.bndrun` files for configuration
- Creates single executable JAR with embedded launcher
- Requires working BND export task

### New Approach (Felix Launcher)
- ✅ **Works with Java 21**
- Uses `config.properties` for configuration
- Launcher JAR + bundle directory structure
- Standard Apache Felix pattern
- More flexible and maintainable

## Troubleshooting

### Bundle Not Starting

Check the console output for bundle start errors. Common issues:
- Missing dependencies
- Fragment bundles (these are attached to host bundles automatically)
- Configuration issues

### Configuration Not Applied

Ensure `config.properties` is in the same directory as the launcher JAR.

### Out of Memory

Increase heap size:
```bash
java -Xmx4g -jar gridappsd-launcher.jar
```

### Port Conflicts

Change ports in `config.properties`:
```properties
goss.openwire.port=61616
goss.stomp.port=61613
goss.ws.port=61614
```

## Development

### Adding New Bundles

1. Place JAR files in `bundle/` directory
2. Restart the launcher
3. Bundles will be automatically installed and started

### Modifying Configuration

Edit `config.properties` and restart the launcher.

### Rebuilding

```bash
./gradlew clean dist
```

## Technical Details

- **Java Version**: 21
- **OSGi Framework**: Apache Felix 7.0.5
- **Bundle Count**: 89 (including GOSS and GridAPPS-D bundles)
- **Launcher Size**: ~6KB (main launcher JAR)
- **Total Distribution**: ~200MB (including all bundles)

## References

- [Apache Felix Framework](https://felix.apache.org/documentation/subprojects/apache-felix-framework/apache-felix-framework-usage-documentation.html)
- [OSGi Core Specification](https://docs.osgi.org/specification/)
- [BND Tools](https://bnd.bndtools.org/)
