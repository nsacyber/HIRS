package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.userdefined.HIRSLogger;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.logging.LoggersEndpoint;
import org.springframework.boot.logging.LogLevel;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Service layer component that handles HIRS application logging
 * and supports various Help page-related operations.
 */
@Log4j2
@Service
public class HelpPageService {
    private static final String MAIN_HIRS_LOGGER_NAME = "hirs.attestationca";

    private static final String HIRS_ATTESTATION_CA_PORTAL_LOG_NAME = "HIRS_AttestationCA_Portal";

    private final LoggersEndpoint loggersEndpoint;

    @Value("${logging.file.path}")
    private String logFilesPath;

    /**
     * Constructor for Help Page Service.
     *
     * @param loggersEndpoint loggers endpoint
     */
    public HelpPageService(final LoggersEndpoint loggersEndpoint) {
        this.loggersEndpoint = loggersEndpoint;
    }

    /**
     * Packages a collection of HIRS Attestation log files into a zip file.
     *
     * @param zipOut zip output stream
     * @throws IOException if there are any issues packaging or downloading the zip file
     */
    public void bulkDownloadHIRSLogFiles(final ZipOutputStream zipOut) throws IOException {
        final Path logDirectory = Paths.get(logFilesPath);

        if (!Files.isDirectory(logDirectory)) {
            throw new IllegalArgumentException("Provided path is not a directory: " + logDirectory);
        }

        // Open the directory stream to iterate over files/directories inside the log directory
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(logDirectory)) {
            // Loop through each entry in the directory
            for (Path filePath : directoryStream) {
                // Skip if not a regular file
                if (!Files.isRegularFile(filePath)) {
                    log.error("Unable to process the following path [{}] since the provided file "
                            + "path is not a regular file", filePath);
                    continue;
                }

                Path fileNamePath = filePath.getFileName();

                // Skip if the filename is null
                if (fileNamePath == null) {
                    log.error("Unable to process the following path [{}] since the provided file "
                            + "path is null", filePath);
                    continue;
                }

                final String fileName = fileNamePath.toString();

                // and is a HIRS Attestation CA log file
                if (fileName.startsWith(HIRS_ATTESTATION_CA_PORTAL_LOG_NAME)
                        && fileName.endsWith(".log")) {
                    // Create a new zip entry with the file's name
                    ZipEntry zipEntry = new ZipEntry(fileName);
                    zipEntry.setTime(System.currentTimeMillis());
                    zipOut.putNextEntry(zipEntry);

                    // Open an InputStream to read the file's contents
                    try (InputStream fis = Files.newInputStream(filePath)) {
                        // Copy the file content directly into the ZipOutputStream
                        StreamUtils.copy(fis, zipOut);
                    }

                    zipOut.closeEntry();
                }
            }
        }
        zipOut.finish();
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
     * @param loggerName      name of the logger
     * @param logLevel        log level
     * @param successMessages
     * @param errorMessages
     */
    public void setLoggerLevel(final String loggerName,
                               final String logLevel,
                               final List<String> successMessages,
                               final List<String> errorMessages) {
        final LogLevel newLogLevel = LogLevel.valueOf(logLevel);

        // if a user attempts to change the log level of a logger that is not a part of the HIRS application
        if (!loggerName.startsWith(MAIN_HIRS_LOGGER_NAME)) {

            final String errorMessage = String.format("An illegal attempt has been made to change "
                    + "the selected logger [%s]'s log level. ", loggerName);

            log.error(errorMessage);
            errorMessages.add(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        loggersEndpoint.configureLogLevel(loggerName, newLogLevel);

        log.info("The logger [{}]'s level has been changed to [{}]", loggerName, newLogLevel);
    }
}
