package com.pnnl.goss.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.GradleException

/**
 * Gradle plugin that creates generic OSGi runners from .bndrun files.
 *
 * This plugin provides a task factory that generates executable JAR files
 * from BND runtime specification files (.bndrun), using updated dependencies
 * for Java 21, Jakarta EE, ActiveMQ 6.x, and Apache Shiro 2.0.
 *
 * Usage:
 *   apply plugin: BndRunnerPlugin
 *
 *   bndRunner {
 *       // Optional: specify additional bundle directories
 *       bundleDirs = [file('generated'), file('../GOSS/pnnl.goss.core/generated')]
 *
 *       // Optional: specify configuration directory
 *       configDir = file('conf')
 *   }
 *
 * Task Usage:
 *   ./gradlew buildRunner.goss-core      # Builds from goss-core.bndrun
 *   ./gradlew buildRunner.<name>         # Builds from <name>.bndrun
 */
class BndRunnerPlugin implements Plugin<Project> {

    void apply(Project project) {
        // Create extension for configuration
        def extension = project.extensions.create('bndRunner', BndRunnerExtension)

        // Set defaults
        extension.bundleDirs = [
            project.file('generated'),
            project.file('../pnnl.goss.core/generated')
        ]
        extension.configDir = project.file('conf')
        extension.outputDir = project.file("${project.buildDir}/runners")

        // Create configurations for dependencies
        project.configurations {
            felixRuntime
            gossRuntime
        }

        // Add dependencies
        project.dependencies {
            // Felix Framework
            felixRuntime 'org.apache.felix:org.apache.felix.framework:7.0.5'
            felixRuntime 'org.apache.felix:org.apache.felix.main:7.0.5'

            // Core OSGi services - updated versions
            gossRuntime 'org.apache.felix:org.apache.felix.scr:2.2.12'
            gossRuntime 'org.apache.felix:org.apache.felix.configadmin:1.9.26'
            gossRuntime 'org.apache.felix:org.apache.felix.gogo.runtime:1.1.6'
            gossRuntime 'org.apache.felix:org.apache.felix.gogo.shell:1.1.4'
            gossRuntime 'org.apache.felix:org.apache.felix.gogo.command:1.1.2'

            // Logging - latest versions
            gossRuntime 'org.slf4j:slf4j-api:2.0.16'
            gossRuntime 'org.slf4j:slf4j-simple:2.0.16'

            // ActiveMQ 6.x with Jakarta JMS and Shiro 2.0
            gossRuntime 'org.apache.activemq:activemq-osgi:6.2.0'
            gossRuntime 'org.apache.activemq:activemq-shiro:6.2.0'
            gossRuntime 'org.apache.shiro:shiro-core:2.0.0'
            gossRuntime 'org.apache.shiro:shiro-lang:2.0.0'
            gossRuntime 'org.apache.shiro:shiro-cache:2.0.0'
            gossRuntime 'org.apache.shiro:shiro-event:2.0.0'
            gossRuntime 'org.apache.shiro:shiro-crypto-core:2.0.0'
            gossRuntime 'org.apache.shiro:shiro-crypto-hash:2.0.0'
            gossRuntime 'org.apache.shiro:shiro-crypto-cipher:2.0.0'
            gossRuntime 'org.apache.shiro:shiro-config-core:2.0.0'
            gossRuntime 'org.apache.shiro:shiro-config-ogdl:2.0.0'

            // Jakarta EE APIs (Java 21 compatible)
            gossRuntime 'jakarta.jms:jakarta.jms-api:3.1.0'
            gossRuntime 'jakarta.annotation:jakarta.annotation-api:2.1.1'
            gossRuntime 'jakarta.resource:jakarta.resource-api:2.1.0'
            gossRuntime 'jakarta.transaction:jakarta.transaction-api:2.0.1'
            gossRuntime 'jakarta.inject:jakarta.inject-api:2.0.1'
            gossRuntime 'jakarta.xml.bind:jakarta.xml.bind-api:4.0.2'
            gossRuntime 'jakarta.activation:jakarta.activation-api:2.1.3'

            // Basic dependencies - latest versions
            gossRuntime 'org.apache.httpcomponents:httpclient-osgi:4.5.14'
            gossRuntime 'org.apache.httpcomponents:httpcore-osgi:4.4.16'
            gossRuntime 'commons-io:commons-io:2.18.0'
            gossRuntime 'commons-logging:commons-logging:1.2'
            gossRuntime 'org.apache.commons:commons-pool2:2.12.0'
            gossRuntime 'com.thoughtworks.xstream:xstream:1.4.20'
            gossRuntime 'com.google.code.gson:gson:2.11.0'
            gossRuntime 'com.h2database:h2:2.1.214'

            // Apache Aries SPI Fly for ServiceLoader support
            gossRuntime 'org.apache.aries.spifly:org.apache.aries.spifly.dynamic.framework.extension:1.3.7'
            gossRuntime 'org.apache.aries.spifly:org.apache.aries.spifly.dynamic.bundle:1.3.7'
            gossRuntime 'org.ow2.asm:asm:9.7.1'
            gossRuntime 'org.ow2.asm:asm-commons:9.7.1'
            gossRuntime 'org.ow2.asm:asm-util:9.7.1'

            // OSGi utilities
            gossRuntime 'org.osgi:org.osgi.util.promise:1.3.0'
            gossRuntime 'org.osgi:org.osgi.util.function:1.2.0'
        }

        // Add task rule for building runners from .bndrun files
        project.tasks.addRule('Pattern: buildRunner.<name>: Build OSGi runner from <name>.bndrun using Java 21/Jakarta/ActiveMQ 6.x') { String taskName ->
            if (taskName.startsWith('buildRunner.')) {
                def bndrunName = taskName - 'buildRunner.'
                def bndrunFile = project.file("${bndrunName}.bndrun")

                project.task(taskName, type: Jar) {
                    description = "Build OSGi runner from ${bndrunName}.bndrun with Java 21 / Jakarta EE / ActiveMQ 6.x"
                    group = 'build'

                    archiveBaseName = "${bndrunName}-runner"
                    archiveVersion = ''
                    destinationDirectory = extension.outputDir

                    // Validate .bndrun file exists
                    doFirst {
                        if (!bndrunFile.exists()) {
                            throw new GradleException("BNDrun file not found: ${bndrunFile}")
                        }
                        logger.lifecycle("Building OSGi runner from: ${bndrunFile.name}")
                        logger.lifecycle("Using updated dependencies: ActiveMQ 6.2.0, Shiro 2.0.0, Jakarta JMS 3.1.0")
                        logger.lifecycle("Bundle directories: ${extension.bundleDirs}")
                    }

                    // Main class: Felix launcher
                    manifest {
                        attributes(
                            'Main-Class': 'org.apache.felix.main.Main',
                            'Bundle-SymbolicName': "goss.${bndrunName}.runner",
                            'Bundle-Version': '2.0.0',
                            'Created-By': 'BndRunnerPlugin',
                            'BNDrun-Source': bndrunFile.name
                        )
                    }

                    // Include Felix framework classes
                    from {
                        project.configurations.felixRuntime.collect {
                            it.isDirectory() ? it : project.zipTree(it)
                        }
                    }

                    // Include all bundles from configured directories
                    into('bundle') {
                        extension.bundleDirs.each { dir ->
                            if (dir.exists()) {
                                from project.fileTree(dir: dir, include: '*.jar')
                            }
                        }
                    }

                    // Include updated runtime dependencies (ActiveMQ 6.x, Jakarta, Shiro 2.0)
                    into('bundle') {
                        from project.configurations.gossRuntime
                    }

                    // Include configuration files if they exist
                    if (extension.configDir.exists()) {
                        into('conf') {
                            from project.fileTree(dir: extension.configDir, include: '**/*')
                        }
                    }

                    // Copy the source .bndrun file for reference
                    into('META-INF') {
                        from bndrunFile
                    }

                    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

                    doLast {
                        logger.lifecycle("✓ Created: ${archiveFile.get().asFile}")
                        logger.lifecycle("  Size: ${String.format('%.1f', archiveFile.get().asFile.length() / (1024*1024))} MB")
                        logger.lifecycle("  Run with: java -jar ${archiveFile.get().asFile.name}")
                    }
                }
            }
        }
    }
}

/**
 * Extension for configuring the BndRunner plugin.
 */
class BndRunnerExtension {
    List<File> bundleDirs = []
    File configDir
    File outputDir
}
