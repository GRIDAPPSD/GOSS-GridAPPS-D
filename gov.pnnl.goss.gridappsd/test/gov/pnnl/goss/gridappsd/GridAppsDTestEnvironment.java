package gov.pnnl.goss.gridappsd;

import java.io.File;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Manages the Docker Compose environment for container-based integration tests.
 *
 * This class uses Testcontainers to start and manage the GridAPPS-D Docker
 * environment defined in ../gridappsd-docker/docker-compose.yml.
 *
 * Features: - Uses local docker compose command (not containerized) - Health
 * checks matching docker-compose.yml configuration - Dynamic port mapping for
 * test isolation
 *
 * Usage:
 *
 * <pre>
 * public class MyContainerTest {
 *     static GridAppsDTestEnvironment env = GridAppsDTestEnvironment.getInstance();
 *
 *     &#64;BeforeAll
 *     static void startContainers() {
 *         env.start();
 *     }
 *
 *     &#64;Test
 *     void testSomething() {
 *         String host = env.getGridAppsDHost();
 *         int port = env.getGridAppsDPort();
 *         // ... connect and test
 *     }
 * }
 * </pre>
 */
public class GridAppsDTestEnvironment {

    private static final Logger log = LoggerFactory.getLogger(GridAppsDTestEnvironment.class);

    // Docker compose file location relative to project root
    private static final String DOCKER_COMPOSE_PATH = "../gridappsd-docker/docker-compose.yml";

    // Service names as defined in docker-compose.yml
    private static final String SERVICE_GRIDAPPSD = "gridappsd";
    private static final String SERVICE_MYSQL = "mysql";
    private static final String SERVICE_BLAZEGRAPH = "blazegraph";

    // Ports as defined in docker-compose.yml
    private static final int GRIDAPPSD_OPENWIRE_PORT = 61616;
    private static final int GRIDAPPSD_STOMP_PORT = 61613;
    private static final int MYSQL_PORT = 3306;
    private static final int BLAZEGRAPH_PORT = 8080;

    // Singleton instance
    private static GridAppsDTestEnvironment instance;

    private ComposeContainer environment;
    private boolean started = false;

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
     * Start the Docker Compose environment if not already running. Uses the local
     * docker compose command for better compatibility.
     */
    public synchronized void start() {
        if (started) {
            log.info("Docker environment already started");
            return;
        }

        File composeFile = new File(DOCKER_COMPOSE_PATH);
        if (!composeFile.exists()) {
            throw new IllegalStateException(
                    "Docker compose file not found: " + composeFile.getAbsolutePath() +
                            "\nMake sure you're running from the GOSS-GridAPPS-D directory");
        }

        System.out.println("Starting Docker environment from: " + composeFile.getAbsolutePath());

        // Use ComposeContainer which uses local docker compose command
        // This avoids permission issues with files like MySQL SSL keys
        environment = new ComposeContainer(composeFile)
                // Expose and wait for MySQL
                .withExposedService(SERVICE_MYSQL, MYSQL_PORT,
                        Wait.forHealthcheck()
                                .withStartupTimeout(Duration.ofMinutes(2)))
                // Expose and wait for Blazegraph
                .withExposedService(SERVICE_BLAZEGRAPH, BLAZEGRAPH_PORT,
                        Wait.forHttp("/bigdata/namespace")
                                .forStatusCode(200)
                                .withStartupTimeout(Duration.ofMinutes(2)))
                // Expose and wait for GridAPPS-D
                .withExposedService(SERVICE_GRIDAPPSD, GRIDAPPSD_OPENWIRE_PORT,
                        Wait.forListeningPort()
                                .withStartupTimeout(Duration.ofMinutes(3)))
                .withExposedService(SERVICE_GRIDAPPSD, GRIDAPPSD_STOMP_PORT)
                // Use local compose binary
                .withLocalCompose(true);

        try {
            System.out.println("Starting containers...");
            environment.start();
            started = true;
            System.out.println("Docker environment started successfully");
            System.out.println("GridAPPS-D available at " + getGridAppsDHost() + ":" + getGridAppsDPort());
        } catch (Exception e) {
            System.err.println("Failed to start Docker environment: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to start Docker environment", e);
        }
    }

    /**
     * Stop the Docker Compose environment.
     */
    public synchronized void stop() {
        if (environment != null && started) {
            log.info("Stopping Docker environment");
            environment.stop();
            started = false;
        }
    }

    /**
     * Check if the environment is started.
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * Get the host for GridAPPS-D OpenWire connections.
     */
    public String getGridAppsDHost() {
        ensureStarted();
        return environment.getServiceHost(SERVICE_GRIDAPPSD, GRIDAPPSD_OPENWIRE_PORT);
    }

    /**
     * Get the OpenWire port for GridAPPS-D (61616 internally).
     */
    public int getGridAppsDPort() {
        ensureStarted();
        return environment.getServicePort(SERVICE_GRIDAPPSD, GRIDAPPSD_OPENWIRE_PORT);
    }

    /**
     * Get the STOMP port for GridAPPS-D (61613 internally).
     */
    public int getGridAppsDStompPort() {
        ensureStarted();
        return environment.getServicePort(SERVICE_GRIDAPPSD, GRIDAPPSD_STOMP_PORT);
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
        return environment.getServiceHost(SERVICE_MYSQL, MYSQL_PORT);
    }

    /**
     * Get the MySQL port.
     */
    public int getMySqlPort() {
        ensureStarted();
        return environment.getServicePort(SERVICE_MYSQL, MYSQL_PORT);
    }

    /**
     * Get the host for Blazegraph connections.
     */
    public String getBlazegraphHost() {
        ensureStarted();
        return environment.getServiceHost(SERVICE_BLAZEGRAPH, BLAZEGRAPH_PORT);
    }

    /**
     * Get the Blazegraph port.
     */
    public int getBlazegraphPort() {
        ensureStarted();
        return environment.getServicePort(SERVICE_BLAZEGRAPH, BLAZEGRAPH_PORT);
    }

    /**
     * Get the Blazegraph SPARQL endpoint URL.
     */
    public String getBlazegraphSparqlUrl() {
        return String.format("http://%s:%d/bigdata/namespace/kb/sparql",
                getBlazegraphHost(), getBlazegraphPort());
    }

    private void ensureStarted() {
        if (!started) {
            throw new IllegalStateException("Docker environment not started. Call start() first.");
        }
    }
}
