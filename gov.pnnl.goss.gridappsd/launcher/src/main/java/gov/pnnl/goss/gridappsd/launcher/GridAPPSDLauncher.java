package gov.pnnl.goss.gridappsd.launcher;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;

/**
 * GridAPPS-D Felix OSGi Framework Launcher
 *
 * This launcher starts the Apache Felix OSGi framework and loads all bundles
 * from the configured bundle directory. It replaces the BND export mechanism
 * which is incompatible with Java 21.
 */
public class GridAPPSDLauncher {

    private static final String BUNDLE_DIR = "bundle.dir";
    private static final String CONFIG_FILE = "config.properties";

    private File baseDir;

    public static void main(String[] args) {
        GridAPPSDLauncher launcher = new GridAPPSDLauncher();
        try {
            launcher.launch();
        } catch (Exception e) {
            System.err.println("Error launching GridAPPS-D: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void launch() throws Exception {
        System.out.println("Starting GridAPPS-D OSGi Framework...");

        // Determine base directory (where the JAR file is located)
        baseDir = determineBaseDir();
        System.out.println("Base directory: " + baseDir.getAbsolutePath());

        // Load configuration
        Map<String, String> config = loadConfiguration();

        // Create and start framework
        Framework framework = createFramework(config);
        framework.start();

        System.out.println("Felix Framework started");

        // Install and start bundles
        BundleContext context = framework.getBundleContext();
        installBundles(context, config);

        System.out.println("All bundles installed and started");
        System.out.println("GridAPPS-D is running. Press Ctrl+C to stop.");

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                System.out.println("\nStopping GridAPPS-D...");
                framework.stop();
                framework.waitForStop(0);
                System.out.println("GridAPPS-D stopped");
            } catch (Exception e) {
                System.err.println("Error stopping framework: " + e.getMessage());
            }
        }));

        // Wait for framework to stop
        framework.waitForStop(0);
        System.exit(0);
    }

    /**
     * Determine the base directory where the launcher JAR is located.
     * This allows the launcher to find config.properties and bundle/ relative
     * to the JAR file location, regardless of the current working directory.
     */
    private File determineBaseDir() {
        try {
            // Get the location of this class's JAR file
            CodeSource codeSource = GridAPPSDLauncher.class.getProtectionDomain().getCodeSource();
            if (codeSource != null) {
                URL jarUrl = codeSource.getLocation();
                File jarFile = new File(jarUrl.toURI());
                if (jarFile.isFile()) {
                    // JAR file - return its parent directory
                    return jarFile.getParentFile();
                } else {
                    // Running from classes directory (development mode)
                    return jarFile;
                }
            }
        } catch (URISyntaxException e) {
            System.err.println("Warning: Could not determine JAR location: " + e.getMessage());
        }
        // Fall back to current directory
        return new File(".");
    }

    /**
     * Resolve a path relative to the base directory.
     */
    private File resolveFile(String path) {
        File file = new File(path);
        if (file.isAbsolute()) {
            return file;
        }
        return new File(baseDir, path);
    }

    private Map<String, String> loadConfiguration() throws IOException {
        Map<String, String> config = new HashMap<>();

        // Look for config.properties in base directory
        File configFile = resolveFile(CONFIG_FILE);
        if (configFile.exists()) {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
            }
            for (String key : props.stringPropertyNames()) {
                config.put(key, props.getProperty(key));
            }
            System.out.println("Loaded configuration from " + configFile.getAbsolutePath());
        } else {
            System.out.println("No config file found at " + configFile.getAbsolutePath() + ", using defaults");
        }

        // Set defaults if not specified
        if (!config.containsKey(Constants.FRAMEWORK_STORAGE)) {
            config.put(Constants.FRAMEWORK_STORAGE, "felix-cache");
        }
        if (!config.containsKey(Constants.FRAMEWORK_STORAGE_CLEAN)) {
            config.put(Constants.FRAMEWORK_STORAGE_CLEAN, "onFirstInit");
        }
        if (!config.containsKey(BUNDLE_DIR)) {
            config.put(BUNDLE_DIR, "bundle");
        }
        if (!config.containsKey("org.osgi.framework.system.packages.extra")) {
            config.put("org.osgi.framework.system.packages.extra",
                    "sun.misc" +
                    ",javax.net.ssl" +
                    ",javax.net" +
                    ",javax.security.auth.x500" +
                    ",javax.crypto" +
                    ",javax.crypto.spec" +
                    ",javax.xml.parsers" +
                    ",javax.xml.transform" +
                    ",javax.xml.transform.dom" +
                    ",javax.xml.transform.stream" +
                    ",javax.xml.namespace" +
                    ",javax.xml.xpath" +
                    ",javax.xml.datatype" +
                    ",javax.xml.validation" +
                    ",org.w3c.dom" +
                    ",org.xml.sax" +
                    ",org.xml.sax.helpers");
        }

        // Resolve relative paths to absolute paths based on baseDir
        // This ensures FileInstall and other components find files correctly
        resolveRelativePaths(config);

        // Propagate certain properties to System properties
        // Some components (like Gogo shell) read from System.getProperty()
        // rather than Felix framework configuration
        propagateSystemProperties(config);

        return config;
    }

    /**
     * Propagate certain configuration properties to System properties.
     * Some bundles (like Gogo shell) read configuration from System.getProperty()
     * rather than from Felix framework configuration.
     */
    private void propagateSystemProperties(Map<String, String> config) {
        // List of properties that should be propagated to System properties
        // if not already set via -D on the command line
        String[] systemPropKeys = {
            "gosh.args",  // Gogo shell options (--nointeractive, --noshutdown)
            "goss.activemq.host",
            "goss.openwire.port",
            "goss.stomp.port",
            "goss.ws.port",
            "goss.broker-name",
            "goss.activemq.start.broker"
        };

        for (String key : systemPropKeys) {
            String value = config.get(key);
            if (value != null && System.getProperty(key) == null) {
                System.setProperty(key, value);
                System.out.println("Set system property: " + key + "=" + value);
            }
        }
    }

    /**
     * Resolve relative paths in configuration to absolute paths based on baseDir.
     * This is necessary because the working directory may not be the same as
     * the launcher's base directory.
     */
    private void resolveRelativePaths(Map<String, String> config) {
        // Resolve FileInstall directory
        String fileInstallDir = config.get("felix.fileinstall.dir");
        if (fileInstallDir != null && !new File(fileInstallDir).isAbsolute()) {
            File resolved = resolveFileInstallDir(fileInstallDir);
            config.put("felix.fileinstall.dir", resolved.getAbsolutePath());
            System.out.println("FileInstall directory: " + resolved.getAbsolutePath());
        }

        // Resolve felix cache directory
        String cacheDir = config.get(Constants.FRAMEWORK_STORAGE);
        if (cacheDir != null && !new File(cacheDir).isAbsolute()) {
            File resolved = resolveFile(cacheDir);
            config.put(Constants.FRAMEWORK_STORAGE, resolved.getAbsolutePath());
        }
    }

    /**
     * Resolve FileInstall directory, checking multiple locations.
     * This handles both local development (conf next to launcher) and
     * Docker deployment (conf at /gridappsd/conf, launcher at /gridappsd/launcher).
     */
    private File resolveFileInstallDir(String path) {
        // First, try relative to baseDir (normal case for local development)
        File resolved = resolveFile(path);
        if (resolved.exists() && resolved.isDirectory()) {
            return resolved;
        }

        // For Docker: if baseDir is /gridappsd/launcher and path is "conf",
        // try /gridappsd/conf (parent directory + conf)
        File parentConf = new File(baseDir.getParentFile(), path);
        if (parentConf.exists() && parentConf.isDirectory()) {
            return parentConf;
        }

        // Fall back to the original resolution even if it doesn't exist
        // (FileInstall will log a warning)
        return resolved;
    }

    /**
     * Find all JAR files in a directory, including subdirectories.
     * For subdirectories, only the highest version JAR is selected.
     */
    private List<File> findBundleJars(File bundleDir) {
        List<File> bundleFiles = new ArrayList<>();

        File[] entries = bundleDir.listFiles();
        if (entries == null) {
            return bundleFiles;
        }

        for (File entry : entries) {
            if (entry.isFile() && entry.getName().endsWith(".jar")) {
                // Direct JAR file in bundle directory
                bundleFiles.add(entry);
            } else if (entry.isDirectory()) {
                // Subdirectory - find the highest version JAR
                File highestVersionJar = findHighestVersionJar(entry);
                if (highestVersionJar != null) {
                    bundleFiles.add(highestVersionJar);
                }
            }
        }

        return bundleFiles;
    }

    /**
     * Find the highest version JAR file in a directory.
     * Compares version numbers in filenames like bundle-1.0.0.jar, bundle-2.1.0.jar.
     */
    private File findHighestVersionJar(File dir) {
        File[] jars = dir.listFiles((d, name) -> name.endsWith(".jar"));
        if (jars == null || jars.length == 0) {
            return null;
        }

        File highest = jars[0];
        for (int i = 1; i < jars.length; i++) {
            if (compareVersions(jars[i].getName(), highest.getName()) > 0) {
                highest = jars[i];
            }
        }
        return highest;
    }

    /**
     * Compare two JAR filenames by their version numbers.
     * Returns positive if a > b, negative if a < b, 0 if equal.
     */
    private int compareVersions(String a, String b) {
        // Extract version from filename like "bundle-1.2.3.jar"
        String versionA = extractVersion(a);
        String versionB = extractVersion(b);

        String[] partsA = versionA.split("\\.");
        String[] partsB = versionB.split("\\.");

        int maxLen = Math.max(partsA.length, partsB.length);
        for (int i = 0; i < maxLen; i++) {
            int numA = (i < partsA.length) ? parseVersionPart(partsA[i]) : 0;
            int numB = (i < partsB.length) ? parseVersionPart(partsB[i]) : 0;
            if (numA != numB) {
                return numA - numB;
            }
        }
        return 0;
    }

    private String extractVersion(String filename) {
        // Remove .jar extension
        String name = filename.substring(0, filename.length() - 4);
        // Find the last hyphen followed by a digit
        int idx = name.length() - 1;
        while (idx > 0) {
            if (name.charAt(idx) == '-' && idx + 1 < name.length() &&
                Character.isDigit(name.charAt(idx + 1))) {
                return name.substring(idx + 1);
            }
            idx--;
        }
        return "0";
    }

    private int parseVersionPart(String part) {
        // Extract leading digits from version part (e.g., "3" from "3-SNAPSHOT")
        StringBuilder sb = new StringBuilder();
        for (char c : part.toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append(c);
            } else {
                break;
            }
        }
        if (sb.length() == 0) return 0;
        return Integer.parseInt(sb.toString());
    }

    private Framework createFramework(Map<String, String> config) throws Exception {
        // Use ServiceLoader to find FrameworkFactory
        ServiceLoader<FrameworkFactory> loader = ServiceLoader.load(FrameworkFactory.class);
        FrameworkFactory factory = null;

        for (FrameworkFactory f : loader) {
            factory = f;
            break;
        }

        if (factory == null) {
            throw new IllegalStateException("No OSGi FrameworkFactory found");
        }

        System.out.println("Using framework factory: " + factory.getClass().getName());
        return factory.newFramework(config);
    }

    private void installBundles(BundleContext context, Map<String, String> config) throws BundleException {
        String bundleDirPath = config.get(BUNDLE_DIR);
        File bundleDir = resolveFile(bundleDirPath);

        if (!bundleDir.exists() || !bundleDir.isDirectory()) {
            System.err.println("Warning: Bundle directory not found: " + bundleDir.getAbsolutePath());
            return;
        }

        System.out.println("Loading bundles from: " + bundleDir.getAbsolutePath());

        // Get list of JAR files, including those in subdirectories
        List<File> bundleFiles = findBundleJars(bundleDir);
        if (bundleFiles.isEmpty()) {
            System.err.println("Warning: No bundles found in " + bundleDir.getAbsolutePath());
            return;
        }

        // Install bundles in phases based on configuration
        List<Bundle> installedBundles = new ArrayList<>();

        // Phase 1: Install all bundles
        System.out.println("Installing " + bundleFiles.size() + " bundles...");
        for (File bundleFile : bundleFiles) {
            try {
                // Skip Felix framework JAR - it's the container, not a bundle
                if (bundleFile.getName().startsWith("org.apache.felix.framework-")) {
                    System.out.println("  Skipping framework: " + bundleFile.getName() + " (container)");
                    continue;
                }

                String location = bundleFile.toURI().toString();
                Bundle bundle = context.installBundle(location);
                installedBundles.add(bundle);
                System.out.println("  Installed: " + bundle.getSymbolicName() + " [" + bundle.getBundleId() + "]");
            } catch (BundleException e) {
                System.err.println("  Failed to install " + bundleFile.getName() + ": " + e.getMessage());
            }
        }

        // Phase 2: Start bundles that aren't fragments
        // Start Pax Logging first to avoid SLF4J "no providers found" warning
        System.out.println("Starting bundles...");

        // Priority bundles that should start first (infrastructure bundles)
        // Order matters: logging -> SPI -> config -> SCR -> fileinstall -> security
        List<String> priorityBundles = List.of(
            // Logging first (Pax Logging provides OSGi-native SLF4J with Log4j2 backend)
            "org.ops4j.pax.logging.pax-logging-api",
            "org.ops4j.pax.logging.pax-logging-log4j2",
            // SPI Fly for ServiceLoader support (Shiro 2.0 crypto modules)
            "org.apache.aries.spifly.dynamic.bundle",
            // Configuration Admin - stores/manages configurations
            "org.apache.felix.configadmin",
            // SCR - Declarative Services runtime (activates @Component classes)
            "org.apache.felix.scr",
            // FileInstall - loads .cfg files from conf/ into ConfigAdmin
            "org.apache.felix.fileinstall",
            // GOSS Core API (provides interfaces needed by security)
            "pnnl.goss.core.core-api",
            // GOSS Security - provides SecurityManager and GossPermissionResolver
            // Must start before security-ldap and security-propertyfile
            "pnnl.goss.core.goss-core-security"
        );

        // Start priority bundles first
        for (String priorityName : priorityBundles) {
            for (Bundle bundle : installedBundles) {
                if (priorityName.equals(bundle.getSymbolicName())) {
                    try {
                        if (bundle.getHeaders().get(Constants.FRAGMENT_HOST) == null) {
                            bundle.start();
                            System.out.println("  Started: " + bundle.getSymbolicName() + " (priority)");
                        }
                    } catch (BundleException e) {
                        System.err.println("  Failed to start " + bundle.getSymbolicName() + ": " + e.getMessage());
                    }
                    break;
                }
            }
        }

        // Start remaining bundles
        for (Bundle bundle : installedBundles) {
            try {
                // Skip fragment bundles
                if (bundle.getHeaders().get(Constants.FRAGMENT_HOST) != null) {
                    System.out.println("  Skipping fragment: " + bundle.getSymbolicName());
                    continue;
                }

                // Skip already started priority bundles
                if (bundle.getState() == Bundle.ACTIVE) {
                    continue;
                }

                bundle.start();
                System.out.println("  Started: " + bundle.getSymbolicName());
            } catch (BundleException e) {
                System.err.println("  Failed to start " + bundle.getSymbolicName() + ": " + e.getMessage());
                Throwable cause = e.getCause();
                while (cause != null) {
                    System.err.println("    Caused by: " + cause);
                    cause = cause.getCause();
                }
            }
        }
    }
}
