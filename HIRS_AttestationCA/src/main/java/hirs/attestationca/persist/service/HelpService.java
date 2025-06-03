package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.userdefined.HIRSLogger;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.actuate.logging.LoggersEndpoint;
import org.springframework.boot.logging.LogLevel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service layer component that handles HIRS application logging
 * and supports various Help page-related operations.
 */
@Log4j2
@Service
public class HelpService {
    private static final String MAIN_HIRS_LOGGER_NAME = "hirs.attestationca";

    private final LoggersEndpoint loggersEndpoint;

    /**
     * Constructor for Help service.
     *
     * @param loggersEndpoint loggers endpoint
     */
    public HelpService(final LoggersEndpoint loggersEndpoint) {
        this.loggersEndpoint = loggersEndpoint;
    }

    /**
     * Retrieves the list of main HIRS application's loggers.
     *
     * @return list of main HIRS loggers
     */
    public List<HIRSLogger> getMainHIRSLoggers() {
        // retrieve all the applications' loggers
        Map<String, LoggersEndpoint.LoggerLevelsDescriptor> allLoggers =
                loggersEndpoint.loggers().getLoggers();

        // retrieve just the main logger
        Map<String, LoggersEndpoint.LoggerLevelsDescriptor> allHIRSLoggers = allLoggers.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(MAIN_HIRS_LOGGER_NAME))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        List<HIRSLogger> hirsLoggersList = new ArrayList<>();

        // Loop through the map, create a new object for each entry
        // that holds the log name and log level, and add it to the list.
        allHIRSLoggers.forEach((loggerName, logDescriptor) -> {
            LogLevel logLevel;
            if (logDescriptor.getConfiguredLevel() != null) {
                logLevel = LogLevel.valueOf(logDescriptor.getConfiguredLevel());
            } else {
                logLevel = LogLevel.INFO;
            }
            hirsLoggersList.add(
                    new HIRSLogger(loggerName, logLevel));
        });

        return hirsLoggersList;
    }

    /**
     * Retrieves the main HIRS loggers that match the user provided search term.
     *
     * @param searchTerm search term
     * @return list of main hirs loggers that match the provided search term
     */
    public List<HIRSLogger> getMainHIRSLoggersThatMatchSearchTerm(
            final String searchTerm) {
        // grab all the main hirs loggers
        List<HIRSLogger> hirsLoggers = getMainHIRSLoggers();

        // grab only the main loggers whose names or log level match the provided search term
        return hirsLoggers.stream()
                .filter(eachLogger -> eachLogger.getLoggerName().toLowerCase()
                        .contains(searchTerm.toLowerCase())
                        || eachLogger.getLogLevel().toString().toLowerCase()
                        .contains(searchTerm.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * Sets the selected main HIRS logger to the user provided log level.
     *
     * @param loggerName name of the main HIRS logger
     * @param logLevel   log level
     */
    public void setMainHIRSLoggerLevel(final String loggerName, final String logLevel) {
        final LogLevel newLogLevel = LogLevel.valueOf(logLevel);

        loggersEndpoint.configureLogLevel(loggerName, newLogLevel);

        log.info("The main HIRS logger [{}]'s level has been changed to [{}]", loggerName, newLogLevel);
    }
}
