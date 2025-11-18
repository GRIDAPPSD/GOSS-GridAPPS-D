package gov.pnnl.goss.gridappsd.launcher;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
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
    private static final String AUTO_START_LEVEL = "felix.auto.start";

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

    private Map<String, String> loadConfiguration() throws IOException {
        Map<String, String> config = new HashMap<>();

        // Load from config.properties if it exists
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
            }
            for (String key : props.stringPropertyNames()) {
                config.put(key, props.getProperty(key));
            }
            System.out.println("Loaded configuration from " + CONFIG_FILE);
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
            config.put("org.osgi.framework.system.packages.extra", "sun.misc");
        }

        return config;
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
        File bundleDir = new File(bundleDirPath);

        if (!bundleDir.exists() || !bundleDir.isDirectory()) {
            System.err.println("Warning: Bundle directory not found: " + bundleDirPath);
            return;
        }

        System.out.println("Loading bundles from: " + bundleDir.getAbsolutePath());

        // Get list of JAR files
        File[] bundleFiles = bundleDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (bundleFiles == null || bundleFiles.length == 0) {
            System.err.println("Warning: No bundles found in " + bundleDirPath);
            return;
        }

        // Install bundles in phases based on configuration
        List<Bundle> installedBundles = new ArrayList<>();

        // Phase 1: Install all bundles
        System.out.println("Installing " + bundleFiles.length + " bundles...");
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
        System.out.println("Starting bundles...");
        for (Bundle bundle : installedBundles) {
            try {
                // Skip fragment bundles
                if (bundle.getHeaders().get(Constants.FRAGMENT_HOST) != null) {
                    System.out.println("  Skipping fragment: " + bundle.getSymbolicName());
                    continue;
                }

                bundle.start();
                System.out.println("  Started: " + bundle.getSymbolicName());
            } catch (BundleException e) {
                System.err.println("  Failed to start " + bundle.getSymbolicName() + ": " + e.getMessage());
            }
        }
    }
}
