package gov.pnnl.goss.gridappsd.test;

import org.amdatu.testing.configurator.ConfigurationSteps;
import static org.amdatu.testing.configurator.TestConfigurator.createConfiguration;

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
     * Minimal configuration for goss including broker uri
     *
     * @return
     */
    public static ConfigurationSteps configureServerAndClientPropertiesConfig() {

        return ConfigurationSteps.create()
                .add(createConfiguration("pnnl.goss.core.server")
                        .set("goss.openwire.uri", "tcp://localhost:6000")
                        .set("goss.stomp.uri", "stomp://localhost:6001") // vm:(broker:(tcp://localhost:6001)?persistent=false)?marshal=false")
                        .set("goss.ws.uri", "ws://localhost:6002")
                        .set("goss.start.broker", "true")
                        .set("goss.broker.uri", "tcp://localhost:6000"))
                .add(createConfiguration(ClientFactory.CONFIG_PID)
                        .set("goss.openwire.uri", "tcp://localhost:6000")
                        .set("goss.stomp.uri", "stomp://localhost:6001")
                        .set("goss.ws.uri", "ws://localhost:6002"))
                .add(createConfiguration("org.ops4j.pax.logging")
                        .set("log4j.rootLogger", "DEBUG, out, osgi:*")
                        .set("log4j.throwableRenderer", "org.apache.log4j.OsgiThrowableRenderer")

                        // # CONSOLE appender not used by default
                        .set("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender")
                        .set("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout")
                        .set("log4j.appender.stdout.layout.ConversionPattern", "%-5.5p| %c{1} (%L) | %m%n")
                        // #server.core.internal.GossRequestHandlerRegistrationImpl", "DEBUG,stdout
                        .set("log4j.logger.pnnl.goss", "DEBUG, stdout")
                        .set("log4j.logger.org.apache.aries", "INFO")

                        // # File appender
                        .set("log4j.appender.out", "org.apache.log4j.RollingFileAppender")
                        .set("log4j.appender.out.layout", "org.apache.log4j.PatternLayout")
                        .set("log4j.appender.out.layout.ConversionPattern",
                                "%d{ISO8601} | %-5.5p | %-16.16t | %-32.32c{1} | %X{bundle.id} - %X{bundle.name} - %X{bundle.version} | %m%n")
                        .set("log4j.appender.out.file", "felix.log")
                        .set("log4j.appender.out.append", "true")
                        .set("log4j.appender.out.maxFileSize", "1MB")
                        .set("log4j.appender.out.maxBackupIndex", "10"));

    }
}
