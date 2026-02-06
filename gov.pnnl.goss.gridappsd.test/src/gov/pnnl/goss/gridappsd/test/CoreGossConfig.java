package gov.pnnl.goss.gridappsd.test;

import java.util.HashMap;
import java.util.Map;

import pnnl.goss.core.ClientFactory;

/**
 * Standard configuration that is required for us to use goss in integration
 * tests.
 *
 * These configuration steps can be used as a guide to building cfg files for
 * the bundles.
 *
 * @author Craig Allwardt
 *
 */
public class CoreGossConfig {

    /**
     * Get server configuration properties for testing
     *
     * @return Map of configuration properties
     */
    public static Map<String, String> getServerProperties() {
        Map<String, String> props = new HashMap<>();
        props.put("goss.openwire.uri", "tcp://localhost:6000");
        props.put("goss.stomp.uri", "stomp://localhost:6001");
        props.put("goss.ws.uri", "ws://localhost:6002");
        props.put("goss.start.broker", "true");
        props.put("goss.broker.uri", "tcp://localhost:6000");
        return props;
    }

    /**
     * Get client configuration properties for testing
     *
     * @return Map of configuration properties
     */
    public static Map<String, String> getClientProperties() {
        Map<String, String> props = new HashMap<>();
        props.put("goss.openwire.uri", "tcp://localhost:6000");
        props.put("goss.stomp.uri", "stomp://localhost:6001");
        props.put("goss.ws.uri", "ws://localhost:6002");
        return props;
    }

    /**
     * Get logging configuration properties for testing
     *
     * @return Map of configuration properties
     */
    public static Map<String, String> getLoggingProperties() {
        Map<String, String> props = new HashMap<>();
        props.put("log4j.rootLogger", "DEBUG, out, osgi:*");
        props.put("log4j.throwableRenderer", "org.apache.log4j.OsgiThrowableRenderer");
        props.put("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
        props.put("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
        props.put("log4j.appender.stdout.layout.ConversionPattern", "%-5.5p| %c{1} (%L) | %m%n");
        props.put("log4j.logger.pnnl.goss", "DEBUG, stdout");
        props.put("log4j.logger.org.apache.aries", "INFO");
        props.put("log4j.appender.out", "org.apache.log4j.RollingFileAppender");
        props.put("log4j.appender.out.layout", "org.apache.log4j.PatternLayout");
        props.put("log4j.appender.out.layout.ConversionPattern",
                "%d{ISO8601} | %-5.5p | %-16.16t | %-32.32c{1} | %X{bundle.id} - %X{bundle.name} - %X{bundle.version} | %m%n");
        props.put("log4j.appender.out.file", "felix.log");
        props.put("log4j.appender.out.append", "true");
        props.put("log4j.appender.out.maxFileSize", "1MB");
        props.put("log4j.appender.out.maxBackupIndex", "10");
        return props;
    }
}
