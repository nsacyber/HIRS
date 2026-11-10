package hirs.attestationca.portal.configuration;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.status.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the actual Spring Boot logging configuration without starting a server or database.
 */
public class LoggingConfigurationTest {
    @TempDir
    private Path logDirectory;

    /**
     * Tests Linux configuration, parameter substitution, stack traces and runtime level changes.
     * @throws Exception if log files cannot be read
     */
    @Test
    public void testLinuxLogging() throws Exception {
        verifyConfiguration("logback-spring.xml", null);
    }

    /**
     * Tests Windows configuration with an externally supplied log directory.
     * @throws Exception if log files cannot be read
     */
    @Test
    public void testWindowsLogging() throws Exception {
        verifyConfiguration("logback-spring.win.xml", null);
    }

    /**
     * Tests that production does not enable the default ACA INFO logger.
     * @throws Exception if log files cannot be read
     */
    @Test
    public void testProductionProfile() throws Exception {
        verifyConfiguration("logback-spring.xml", "production");
    }

    private void verifyConfiguration(final String resource, final String profile) throws Exception {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("logging.file.path", logDirectory.toString());
        if (profile != null) {
            environment.setActiveProfiles(profile);
        }
        LoggingSystem system = LoggingSystem.get(getClass().getClassLoader());
        system.beforeInitialize();
        try {
            system.initialize(new LoggingInitializationContext(environment), "classpath:" + resource, null);
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            assertFalse(context.getStatusManager().getCopyOfStatusList().stream()
                    .anyMatch(status -> status.getEffectiveLevel() == Status.ERROR));
            assertEquals(ch.qos.logback.classic.Level.ERROR, context.getLogger(Logger.ROOT_LOGGER_NAME).getLevel());
            RollingFileAppender<?> file = (RollingFileAppender<?>)
                    context.getLogger(Logger.ROOT_LOGGER_NAME).getAppender("FILE");
            assertTrue(file.isStarted());
            assertTrue(file.getRollingPolicy().isStarted());
            assertTrue(context.getLogger(Logger.ROOT_LOGGER_NAME).getAppender("STDOUT")
                    .getCopyOfAttachedFiltersList().stream().anyMatch(filter -> filter instanceof ThresholdFilter));

            Logger logger = LoggerFactory.getLogger("hirs.attestationca.logging.MigrationTest");
            assertEquals(profile == null, logger.isInfoEnabled());
            logger.info("Logging migration {}", "info");
            logger.debug("Hidden debug message");
            logger.error("Logging migration failure", new IllegalStateException("test cause"));
            logger.atLevel(Level.WARN).log("Dynamic validation level");
            system.setLogLevel(logger.getName(), LogLevel.DEBUG);
            logger.debug("Visible debug message");

            String contents = Files.readString(logDirectory.resolve("HIRS_AttestationCA_Portal.log"));
            assertEquals(profile == null, contents.contains("Logging migration info"));
            assertFalse(contents.contains("Hidden debug message"));
            assertTrue(contents.contains("Visible debug message"));
            assertTrue(contents.contains("Logging migration failure"));
            assertTrue(contents.contains("java.lang.IllegalStateException: test cause"));
            assertEquals(profile == null, contents.contains("Dynamic validation level"));
        } finally {
            system.cleanUp();
            ((LoggerContext) LoggerFactory.getILoggerFactory()).reset();
        }
    }
}
