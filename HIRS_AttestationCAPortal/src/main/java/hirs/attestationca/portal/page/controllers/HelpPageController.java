package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.FilteredRecordsList;
import hirs.attestationca.persist.entity.userdefined.HIRSLogger;
import hirs.attestationca.persist.service.HelpService;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.params.NoPageParams;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
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

/**
 * Controller for the Help page.
 */
@Log4j2
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/help")
public class HelpPageController extends PageController<NoPageParams> {
    private final HelpService helpService;

    private String fullLogFilePath;

    @Value("${logging.file.path}")
    private String logFilePath;

    @Value("${logging.file.name}")
    private String logFileName;

    /**
     * Constructor providing the Help Page's display and routing specification.
     *
     * @param helpService help service
     */
    @Autowired
    public HelpPageController(final HelpService helpService) {
        super(Page.HELP);
        this.helpService = helpService;
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
    @GetMapping("/hirs-log-download")
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

    /**
     * Processes the request to retrieve the main HIRS logger for display
     * on the help page.
     *
     * @param input data table input received from the front-end
     * @return data table of just the main HIRS logger
     */
    @ResponseBody
    @GetMapping(value = "/list-main-logger",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTableResponse<HIRSLogger> getMainHIRSLogger(final DataTableInput input) {

        log.info("Received request to display the main HIRS logger");
        log.debug("Request received a datatable input object for listing the main HIRS logger: "
                + "{}", input);

        FilteredRecordsList<HIRSLogger> mainHIRSLoggersFilteredRecordsList =
                new FilteredRecordsList<>();

        final HIRSLogger mainHIRSLogger = this.helpService.getMainHIRSLogger();
        mainHIRSLoggersFilteredRecordsList.add(mainHIRSLogger);
        mainHIRSLoggersFilteredRecordsList.setRecordsTotal(1);
        mainHIRSLoggersFilteredRecordsList.setRecordsFiltered(1);

        log.info("Returning the size of the list of main HIRS loggers: "
                + "{}", mainHIRSLoggersFilteredRecordsList.getRecordsFiltered());

        return new DataTableResponse<>(mainHIRSLoggersFilteredRecordsList, input);
    }


    /**
     * Processes the request that sets the log level of the selected logger.
     *
     * @param response   response that will be sent out after processing download request
     * @param loggerName logger name
     * @param logLevel   logging level
     * @return the redirection view
     * @throws IOException when writing to response output stream
     */
    @PostMapping("/setLogLevel")
    public RedirectView setLogLevel(final HttpServletResponse response,
                                    @RequestParam final String loggerName,
                                    @RequestParam final String logLevel)
            throws IOException {
        try {
            log.info("Received a request to set the log level [{}] for the provided logger [{}]", logLevel,
                    loggerName);

            this.helpService.setLoggerLevel(loggerName, logLevel);
        } catch (Exception exception) {
            log.error("An exception was thrown while attempting to set the logging level for the"
                    + " selected logger", exception);

            // send a 404 error when an exception is thrown while attempting to set the logger's log level
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }

        final String helpPageUrl = "/HIRS_AttestationCAPortal/portal/help";

        return new RedirectView(helpPageUrl);
    }
}
