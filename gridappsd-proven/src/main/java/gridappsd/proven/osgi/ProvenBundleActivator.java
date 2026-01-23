package gridappsd.proven.osgi;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * OSGi Bundle Activator for the gridappsd-proven bundle.
 *
 * This activator initializes the Jersey JAX-RS client with the correct
 * classloader to ensure ServiceLoader can find the shaded HK2
 * InjectionManagerFactory.
 */
public class ProvenBundleActivator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        System.out.println("gridappsd-proven: Initializing Jersey client with bundle classloader...");
        try {
            JerseyClassLoaderHelper.initialize();
            System.out.println("gridappsd-proven: Jersey client initialized successfully");
        } catch (Exception e) {
            System.err.println("gridappsd-proven: Failed to initialize Jersey client: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        // Nothing to clean up
    }
}
