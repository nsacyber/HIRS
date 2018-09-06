package hirs.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * This class exposes a static method to reconfigure log4j dynamically.  Properties are read from
 * /etc/hirs/logging.properties if it exists.  If the file does not exist, no dynamic
 * reconfiguration will take place.  If no value for a property exists in the file, no change
 * will be applied for that property.
 *
 * Currently, this class currently only supports reconfiguring the root logging level.  The
 * property root.level can be configured with any of the five log4j logging levels.
 */
public final class LogConfigurationUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogConfigurationUtil.class);

    private static final Path LOGGING_PROPERTIES_PATH = FileSystems.getDefault()
            .getPath("/etc", "hirs", "logging.properties");

    private static final String ROOT_LOGGER = "root";

    private static final String LEVEL = ".level";

    private LogConfigurationUtil() {

    }

    /**
     * This method applies any dynamically configuration found in the properties file,
     * if it exists.
     *
     * @throws IOException if there is a problem reading the properties file
     */
    public static void applyConfiguration() throws IOException {
        if (!Files.exists(LOGGING_PROPERTIES_PATH)) {
            LOGGER.info(String.format(
                    "No file found at %s.  Logging will operate as configured at build time.",
                    LOGGING_PROPERTIES_PATH.toString()
            ));
            return;
        }

        try (InputStream loggingIs = new FileInputStream(LOGGING_PROPERTIES_PATH.toFile())) {
            Properties loggingProps = new Properties();
            loggingProps.load(loggingIs);
            setLoggingLevels(loggingProps);
        } catch (IOException e) {
            throw new IOException("Could not apply runtime logging configuration", e);
        }
    }

    private static void setLoggingLevels(final Properties loggingProperties)
            throws IOException {
        for (String loggerLevel : loggingProperties.stringPropertyNames()) {
            String logger = loggerLevel.replace(LEVEL, "");
            String level = loggingProperties.getProperty(loggerLevel, "").toUpperCase();
            if (StringUtils.isNotBlank(level)) {
                try {
                    Level levelToSet = Level.getLevel(level);
                    if (levelToSet != null) {
                        if (logger.equals(ROOT_LOGGER)) {
                            LOGGER.info("Configuring root logger with level {}...", level);
                            Configurator.setRootLevel(levelToSet);
                        } else {
                            LOGGER.info("Configuring logger {} with level {}...", logger, level);
                            Configurator.setLevel(logger, levelToSet);
                        }
                    } else {
                        throw new IllegalArgumentException(String.format(
                                "Could not set logging level for logger %s", logger
                        ));
                    }
                } catch (IllegalArgumentException e) {
                    throw new IOException(String.format("No such logging level: %s", level));
                }
                LOGGER.info("Configured logger {} to level {}.", logger, level);
            }
        }
    }
}
