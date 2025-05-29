package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.FilteredRecordsList;
import hirs.attestationca.persist.entity.userdefined.HIRSLogger;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.params.NoPageParams;
import hirs.attestationca.portal.page.utils.ControllerPagesUtils;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.logging.LoggersEndpoint;
import org.springframework.boot.logging.LogLevel;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller for the Help page.
 */
@Log4j2
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/help")
public class HelpPageController extends PageController<NoPageParams> {

    private static final String ROOT_PACKAGE = "hirs";

    private final LoggersEndpoint loggersEndpoint;

    private String fullLogFilePath;

    @Value("${logging.file.path}")
    private String logFilePath;

    @Value("${logging.file.name}")
    private String logFileName;

    /**
     * Constructor providing the Help Page's display and routing specification.
     *
     * @param loggersEndpoint loggers endpoint
     */
    @Autowired
    public HelpPageController(final LoggersEndpoint loggersEndpoint) {
        super(Page.HELP);
        this.loggersEndpoint = loggersEndpoint;
    }

    /**
     * After this component has been created, combine the two application property values together
     * to create the log file's full path.
     */
    @PostConstruct
    public void initialize() {
        this.fullLogFilePath = this.logFilePath + "/" + this.logFileName;
    }

    /**
     * Returns the path for the view and the data model for the page.
     *
     * @param params The object to map url parameters into.
     * @param model  The data model for the request. Can contain data from redirect.
     * @return the path for the view and data model for the page.
     */
    @RequestMapping
    public ModelAndView initPage(final NoPageParams params, final Model model) {
        return getBaseModelAndView(Page.HELP);
    }

    /**
     * Processes the request to download the HIRS application's log file.
     *
     * @param response response that will be sent out after processing download request
     * @throws IOException when writing to response output stream
     */
    @GetMapping("/hirs-log/download")
    public void downloadHIRSLog(final HttpServletResponse response) throws IOException {

        try {
            log.info("Received request to download the HIRS Attestation application's log file");
            final File logFile = new File(this.fullLogFilePath);

            if (!logFile.exists()) {
                log.error("The log file cannot be downloaded because it does not exist");
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }

            Resource resource = new FileSystemResource(logFile);

            // Set the response headers for file download
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + logFile.getName());
            response.setContentType(
                    MediaType.APPLICATION_OCTET_STREAM_VALUE);

            // Copy the file content to the response output stream
            org.apache.commons.io.IOUtils.copy(resource.getInputStream(), response.getOutputStream());

        } catch (Exception exception) {
            log.error("An exception was thrown while attempting to download the"
                    + " HIRS Application log file", exception);

            // send a 404 error when an exception is thrown while attempting to download the
            // HIRS log file
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @ResponseBody
    @GetMapping(value = "/hirs-log/loggers-list",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTableResponse<HIRSLogger> getLoggersTable(final DataTableInput input) {

        log.info("Received request to display list of loggers");
        log.debug("Request received a datatable input object for listing hirs loggers: "
                + "{}", input);

        String orderColumnName = input.getOrderColumnName();

        log.debug("Ordering on column: {}", orderColumnName);

        final String searchTerm = input.getSearch().getValue();
        final Set<String> searchableColumns =
                ControllerPagesUtils.findSearchableColumnsNames(input.getColumns());

        FilteredRecordsList<HIRSLogger> hirsLoggerFilteredRecordsList =
                new FilteredRecordsList<>();

        final int currentPage = input.getStart() / input.getLength();
        Pageable pageable = PageRequest.of(currentPage, input.getLength(), Sort.by(orderColumnName));
        org.springframework.data.domain.Page<HIRSLogger> pagedResult;

        List<HIRSLogger> allHIRSLoggers = getAllHIRSLoggers();

        if (StringUtils.isBlank(searchTerm)) {
            pagedResult = new PageImpl<>(ControllerPagesUtils.paginate(allHIRSLoggers, pageable), pageable,
                    allHIRSLoggers.size());
        } else {
            List<HIRSLogger> filteredHIRSLoggers = getLoggersThatMatchSearchTerm(searchTerm);
            pagedResult =
                    new PageImpl<>(ControllerPagesUtils.paginate(filteredHIRSLoggers, pageable),
                            pageable, filteredHIRSLoggers.size());
        }

        if (pagedResult.hasContent()) {
            hirsLoggerFilteredRecordsList.addAll(pagedResult.getContent());
        }

        hirsLoggerFilteredRecordsList.setRecordsFiltered(pagedResult.getTotalElements());
        hirsLoggerFilteredRecordsList.setRecordsTotal(allHIRSLoggers.size());


        log.info("Returning the size of the list of hirs loggers: "
                + "{}", hirsLoggerFilteredRecordsList.size());

        return new DataTableResponse<>(hirsLoggerFilteredRecordsList, input);
    }


    /**
     * Processes the request that sets the log level of the HIRS application log file
     * based on the provided user input.
     *
     * @param response response that will be sent out after processing download request
     * @param logLevel logging level
     * @return the redirection view
     * @throws IOException when writing to response output stream
     */
    @PostMapping("/hirs-log/setLogLevel")
    public RedirectView setLogLevel(final HttpServletResponse response,
                                    @RequestParam final String logName,
                                    @RequestParam final String logLevel)
            throws IOException {
        try {
            log.info("Received a request to set the log level {} for the provided logger file {}"
                    , logLevel, logName);

            // Convert the string log level to Log4j2 Level
            final LogLevel level = LogLevel.valueOf(logLevel);

            loggersEndpoint.configureLogLevel(logName, level);

            log.info("The log file {}'s level has been changed to {}", logName, level);
        } catch (Exception exception) {
            log.error("An exception was thrown while attempting to set the logging level for the"
                    + " HIRS Application log file", exception);

            // send a 404 error when an exception is thrown while attempting to download the
            // HIRS log file
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }

        final String helpPageUrl = "/HIRS_AttestationCAPortal/portal/help";

        return new RedirectView(helpPageUrl);
    }

    /**
     * Helper method that retrieves all the HIRS application's loggers.
     *
     * @return Spring boot loggers in the form of a map
     */
    private List<HIRSLogger> getAllHIRSLoggers() {
        // retrieve all the applications' loggers
        Map<String, LoggersEndpoint.LoggerLevelsDescriptor> allLoggers =
                loggersEndpoint.loggers().getLoggers();

        // retrieve all the loggers whose logger name starts with the root package's name (hirs)
        Map<String, LoggersEndpoint.LoggerLevelsDescriptor> allHIRSLoggers = allLoggers.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(ROOT_PACKAGE))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        List<HIRSLogger> hirsLoggersList = new ArrayList<>();

        // Loop through the map, create a new object for each entry
        // that holds the log name and level, and add it to the list.
        allHIRSLoggers.forEach((loggerName, logDescriptor) -> {
            LogLevel logLevel = LogLevel.valueOf("INFO"); //todo

            hirsLoggersList.add(
                    new HIRSLogger(loggerName, logLevel));
        });

        return hirsLoggersList;
    }

    /**
     * @param searchTerm
     * @return
     */
    private List<HIRSLogger> getLoggersThatMatchSearchTerm(
            String searchTerm) {
        // grab all the hirs loggers
        List<HIRSLogger> hirsLoggers = getAllHIRSLoggers();

        // grab only the loggers whose names match the provided search term
        return hirsLoggers.stream()
                .filter(eachLogger -> eachLogger.getLoggerName().toLowerCase()
                        .contains(searchTerm.toLowerCase()))
                .collect(
                        Collectors.toList());
    }
}
