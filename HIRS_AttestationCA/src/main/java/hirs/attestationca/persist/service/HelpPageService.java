package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.userdefined.HIRSLogger;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.actuate.logging.LoggersEndpoint;
import org.springframework.boot.logging.LogLevel;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * Service layer component that handles HIRS application logging
 * and supports various Help page-related operations.
 */
@Log4j2
@Service
public class HelpPageService {
    private static final String MAIN_HIRS_LOGGER_NAME = "hirs.attestationca";

    private final LoggersEndpoint loggersEndpoint;

    /**
     * Constructor for Help Page service.
     *
     * @param loggersEndpoint loggers endpoint
     */
    public HelpPageService(final LoggersEndpoint loggersEndpoint) {
        this.loggersEndpoint = loggersEndpoint;
    }

    /**
     * Retrieves the main HIRS application's logger.
     *
     * @return main HIRS logger
     */
    public HIRSLogger getMainHIRSLogger() {
        // retrieve all the applications' loggers
        Map<String, LoggersEndpoint.LoggerLevelsDescriptor> allLoggers =
                loggersEndpoint.loggers().getLoggers();

        // retrieve the ONE main HIRS Logger from the list of loggers
        Optional<Map.Entry<String, LoggersEndpoint.LoggerLevelsDescriptor>> mainLoggerEntry =
                allLoggers.entrySet().stream()
                        .filter(entry ->
                                entry.getKey().equalsIgnoreCase(MAIN_HIRS_LOGGER_NAME))
                        .findFirst();

        HIRSLogger hirsLogger = null;

        // if we are able to retrieve the main hirs logger from the list of loggers
        if (mainLoggerEntry.isPresent()) {
            // grab the main HIRS logger's name and description
            final String loggerName = mainLoggerEntry.get().getKey();
            final LoggersEndpoint.LoggerLevelsDescriptor loggerLevelsDescriptor =
                    mainLoggerEntry.get().getValue();

            // set the log level of the HIRS logger based on the configured level
            LogLevel logLevel;

            // if the log level has already been configured, find the enum equivalent of that
            // configured log level
            if (loggerLevelsDescriptor.getConfiguredLevel() != null) {
                logLevel = LogLevel.valueOf(loggerLevelsDescriptor.getConfiguredLevel());
            } else {
                // if the log level has not been configured (current configured log level is null),
                // set the log level to info
                logLevel = LogLevel.INFO;
            }

            // create a new logger POJO with the logger name and configured log level
            hirsLogger = new HIRSLogger(loggerName, logLevel);
        }

        return hirsLogger;
    }

    /**
     * Sets the selected logger to the user provided log level.
     *
     * @param loggerName name of the logger
     * @param logLevel   log level
     */
    public void setLoggerLevel(final String loggerName, final String logLevel) {
        final LogLevel newLogLevel = LogLevel.valueOf(logLevel);

        // if a user attempts to change the log level of a logger that is not a part of the HIRS application
        if (!loggerName.startsWith(MAIN_HIRS_LOGGER_NAME)) {

            final String errorMessage = String.format(
                    "An illegal attempt has been made to change the selected logger [%s]'s log level. ",
                    loggerName);

            log.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        loggersEndpoint.configureLogLevel(loggerName, newLogLevel);

        log.info("The logger [{}]'s level has been changed to [{}]", loggerName, newLogLevel);
    }
}
