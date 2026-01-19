package gov.pnnl.goss.gridappsd;

import java.io.IOException;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the Docker environment for container-based integration tests.
 *
 * This class expects containers to be started externally using: make docker-up
 *
 * And stopped using: make docker-down
 *
 * The containers use fixed ports as defined in docker/docker-compose.yml: -
 * GridAPPS-D OpenWire: localhost:61616 - GridAPPS-D STOMP: localhost:61613 -
 * MySQL: localhost:3306 - Blazegraph: localhost:8889
 *
 * Usage:
 *
 * <pre>
 * // Before running tests, start containers:
 * // $ make docker-up
 *
 * public class MyContainerTest {
 *     static GridAppsDTestEnvironment env = GridAppsDTestEnvironment.getInstance();
 *
 *     &#64;BeforeAll
 *     static void startContainers() {
 *         env.start(); // Verifies containers are running
 *     }
 *
 *     &#64;Test
 *     void testSomething() {
 *         String host = env.getGridAppsDHost();
 *         int port = env.getGridAppsDPort();
 *         // ... connect and test
 *     }
 * }
 *
 * // After running tests, stop containers:
 * // $ make docker-down
 * </pre>
 */
public class GridAppsDTestEnvironment {

    private static final Logger log = LoggerFactory.getLogger(GridAppsDTestEnvironment.class);

    // Fixed host for all services (containers expose ports to localhost)
    private static final String HOST = "localhost";

    // Ports as defined in docker/docker-compose.yml
    private static final int GRIDAPPSD_OPENWIRE_PORT = 61616;
    private static final int GRIDAPPSD_STOMP_PORT = 61613;
    private static final int MYSQL_PORT = 3306;
    private static final int BLAZEGRAPH_PORT = 8889;

    // Singleton instance
    private static GridAppsDTestEnvironment instance;

    private boolean verified = false;

    private GridAppsDTestEnvironment() {
        // Private constructor for singleton
    }

    /**
     * Get the singleton instance of the test environment.
     */
    public static synchronized GridAppsDTestEnvironment getInstance() {
        if (instance == null) {
            instance = new GridAppsDTestEnvironment();
        }
        return instance;
    }

    /**
     * Verify that the Docker containers are running.
     *
     * This method checks that the required services are accessible. Containers must
     * be started externally using: make docker-up
     */
    public synchronized void start() {
        if (verified) {
            log.info("Docker environment already verified");
            return;
        }

        System.out.println("Verifying Docker environment...");
        System.out.println("Note: Containers must be started with 'make docker-up' before running tests");

        // Check if GridAPPS-D OpenWire port is accessible
        if (!isPortOpen(HOST, GRIDAPPSD_OPENWIRE_PORT)) {
            throw new IllegalStateException(
                    "GridAPPS-D is not running on " + HOST + ":" + GRIDAPPSD_OPENWIRE_PORT + "\n" +
                            "Please start containers with: make docker-up\n" +
                            "Then re-run the tests.");
        }

        // Check if Blazegraph port is accessible
        if (!isPortOpen(HOST, BLAZEGRAPH_PORT)) {
            System.out.println("Warning: Blazegraph not accessible on port " + BLAZEGRAPH_PORT);
        }

        // Check if MySQL port is accessible
        if (!isPortOpen(HOST, MYSQL_PORT)) {
            System.out.println("Warning: MySQL not accessible on port " + MYSQL_PORT);
        }

        verified = true;
        System.out.println("Docker environment verified successfully");
        System.out.println("GridAPPS-D available at " + getGridAppsDHost() + ":" + getGridAppsDPort());
    }

    /**
     * Check if a port is open on the given host.
     */
    private boolean isPortOpen(String host, int port) {
        try (Socket socket = new Socket(host, port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Stop method is a no-op since containers are managed externally. Use 'make
     * docker-down' to stop containers.
     */
    public synchronized void stop() {
        log.info("Containers are managed externally. Use 'make docker-down' to stop them.");
        verified = false;
    }

    /**
     * Check if the environment has been verified.
     */
    public boolean isStarted() {
        return verified;
    }

    /**
     * Get the host for GridAPPS-D OpenWire connections.
     */
    public String getGridAppsDHost() {
        ensureStarted();
        return HOST;
    }

    /**
     * Get the OpenWire port for GridAPPS-D (61616).
     */
    public int getGridAppsDPort() {
        ensureStarted();
        return GRIDAPPSD_OPENWIRE_PORT;
    }

    /**
     * Get the STOMP port for GridAPPS-D (61613).
     */
    public int getGridAppsDStompPort() {
        ensureStarted();
        return GRIDAPPSD_STOMP_PORT;
    }

    /**
     * Get the OpenWire URI for connecting to GridAPPS-D.
     */
    public String getOpenWireUri() {
        return String.format("tcp://%s:%d", getGridAppsDHost(), getGridAppsDPort());
    }

    /**
     * Get the STOMP URI for connecting to GridAPPS-D.
     */
    public String getStompUri() {
        return String.format("stomp://%s:%d", getGridAppsDHost(), getGridAppsDStompPort());
    }

    /**
     * Get the host for MySQL connections.
     */
    public String getMySqlHost() {
        ensureStarted();
        return HOST;
    }

    /**
     * Get the MySQL port.
     */
    public int getMySqlPort() {
        ensureStarted();
        return MYSQL_PORT;
    }

    /**
     * Get the host for Blazegraph connections.
     */
    public String getBlazegraphHost() {
        ensureStarted();
        return HOST;
    }

    /**
     * Get the Blazegraph port.
     */
    public int getBlazegraphPort() {
        ensureStarted();
        return BLAZEGRAPH_PORT;
    }

    /**
     * Get the Blazegraph SPARQL endpoint URL.
     */
    public String getBlazegraphSparqlUrl() {
        return String.format("http://%s:%d/bigdata/namespace/kb/sparql",
                getBlazegraphHost(), getBlazegraphPort());
    }

    private void ensureStarted() {
        if (!verified) {
            throw new IllegalStateException(
                    "Docker environment not verified. Call start() first.\n" +
                            "Also ensure containers are running with: make docker-up");
        }
    }
}
