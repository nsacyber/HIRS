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
    private static final String HIRS_ATTESTATIONCA = "hirs.attestationca";

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
     * Retrieves all the HIRS application's loggers.
     *
     * @return list of hirs loggers
     */
    public List<HIRSLogger> getAllHIRSLoggers() {
        // retrieve all the applications' loggers
        Map<String, LoggersEndpoint.LoggerLevelsDescriptor> allLoggers =
                loggersEndpoint.loggers().getLoggers();

        // retrieve all the loggers whose logger name starts with the root package's name (hirs.attestationca)
        Map<String, LoggersEndpoint.LoggerLevelsDescriptor> allHIRSLoggers = allLoggers.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(HIRS_ATTESTATIONCA))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        List<HIRSLogger> hirsLoggersList = new ArrayList<>();

        // Loop through the map, create a new object for each entry
        // that holds the log name and level, and add it to the list.
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
     * Retrieves the HIRS loggers that match the user provided search term.
     *
     * @param searchTerm search term
     * @return list of hirs loggers that match the provided search term
     */
    public List<HIRSLogger> getHIRSLoggersThatMatchSearchTerm(
            final String searchTerm) {
        // grab all the hirs loggers
        List<HIRSLogger> hirsLoggers = getAllHIRSLoggers();

        // grab only the loggers whose names or log level match the provided search term
        return hirsLoggers.stream()
                .filter(eachLogger -> eachLogger.getLoggerName().toLowerCase()
                        .contains(searchTerm.toLowerCase())
                        || eachLogger.getLogLevel().toString().toLowerCase()
                        .contains(searchTerm.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * Sets the logger to the level that's been set by the user.
     *
     * @param loggerName name of the logger
     * @param logLevel   log level
     */
    public void setLoggerLevel(final String loggerName, final String logLevel) {
        // Convert the string log level to Log4j2 Level
        final LogLevel level = LogLevel.valueOf(logLevel);

        loggersEndpoint.configureLogLevel(loggerName, level);

        log.info("The log file {}'s level has been changed to {}", loggerName, level);
    }
}
