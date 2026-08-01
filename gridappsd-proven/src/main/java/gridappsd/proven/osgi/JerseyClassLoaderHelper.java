package gridappsd.proven.osgi;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * Helper class to properly initialize Jersey JAX-RS client in an OSGi
 * environment with shaded dependencies.
 *
 * The issue is that Jersey uses ServiceLoader to discover its
 * InjectionManagerFactory, but ServiceLoader relies on the Thread Context
 * ClassLoader (TCCL). In OSGi, the TCCL may not have access to the shaded
 * service files inside this bundle.
 *
 * This helper sets the TCCL to this bundle's classloader before creating Jersey
 * clients.
 *
 * Note: We use reflection to avoid direct references to the shaded classes,
 * which allows this code to compile before the shadow plugin runs.
 */
public class JerseyClassLoaderHelper {

    private static volatile boolean initialized = false;
    // Use the shaded package name since the shadow plugin relocates javax.ws.rs
    private static final String CLIENT_BUILDER_CLASS = "gridappsd.proven.shaded.javax.ws.rs.client.ClientBuilder";

    /**
     * Initialize Jersey with the correct classloader. This should be called once
     * when the bundle starts.
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        ClassLoader bundleClassLoader = JerseyClassLoaderHelper.class.getClassLoader();
        ClassLoader originalTccl = Thread.currentThread().getContextClassLoader();

        try {
            // Set TCCL to bundle classloader so ServiceLoader can find shaded services
            Thread.currentThread().setContextClassLoader(bundleClassLoader);

            // Use reflection to load and invoke ClientBuilder.newClient()
            // This forces Jersey to initialize and discover the HK2 InjectionManagerFactory
            Class<?> clientBuilderClass = bundleClassLoader.loadClass(CLIENT_BUILDER_CLASS);
            Method newClientMethod = clientBuilderClass.getMethod("newClient");
            Object client = newClientMethod.invoke(null);

            // Close the test client
            if (client != null) {
                Method closeMethod = client.getClass().getMethod("close");
                closeMethod.invoke(client);
            }

            initialized = true;
            System.out.println("gridappsd-proven: Jersey InjectionManager initialized successfully");
        } catch (Exception e) {
            System.err.println("gridappsd-proven: Failed to initialize Jersey client: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Restore original TCCL
            Thread.currentThread().setContextClassLoader(originalTccl);
        }
    }

    /**
     * Execute a runnable with the bundle's classloader as TCCL. Use this to wrap
     * any code that uses JAX-RS/Jersey.
     */
    public static void withBundleClassLoader(Runnable runnable) {
        ClassLoader bundleClassLoader = JerseyClassLoaderHelper.class.getClassLoader();
        ClassLoader originalTccl = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(bundleClassLoader);
            runnable.run();
        } finally {
            Thread.currentThread().setContextClassLoader(originalTccl);
        }
    }

    /**
     * Execute a callable with the bundle's classloader as TCCL and return the
     * result. Use this to wrap any code that uses JAX-RS/Jersey and returns a
     * value.
     *
     * @param <T>
     *            the return type
     * @param callable
     *            the callable to execute
     * @return the result of the callable
     * @throws Exception
     *             if the callable throws an exception
     */
    public static <T> T callWithBundleClassLoader(Callable<T> callable) throws Exception {
        ClassLoader bundleClassLoader = JerseyClassLoaderHelper.class.getClassLoader();
        ClassLoader originalTccl = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(bundleClassLoader);
            return callable.call();
        } finally {
            Thread.currentThread().setContextClassLoader(originalTccl);
        }
    }
}
