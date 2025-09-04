package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.FilteredRecordsList;
import hirs.attestationca.persist.entity.userdefined.HIRSLogger;
import hirs.attestationca.persist.service.HelpPageService;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.params.NoPageParams;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.zip.ZipOutputStream;

/**
 * Controller for the Help page.
 */
@Log4j2
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/help")
public class HelpPageController extends PageController<NoPageParams> {
    private final HelpPageService helpPageService;

    /**
     * Constructor providing the Help Page's display and routing specification.
     *
     * @param helpPageService help service
     */
    @Autowired
    public HelpPageController(final HelpPageService helpPageService) {
        super(Page.HELP);
        this.helpPageService = helpPageService;
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
     * Processes the request to download a zip file of the HIRS application's log files.
     *
     * @param response response that will be sent out after processing download request
     * @throws IOException when writing to response output stream
     */
    @GetMapping("/hirs-logs-download")
    public void downloadHIRSLogs(final HttpServletResponse response) throws IOException {
        log.info("Received request to download a zip file of all the"
                + " HIRS Attestation application's log files");

        final String fileName = "HIRS_AttestationCAPortal_Logs.zip";

        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
        response.setContentType("application/zip");

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            this.helpPageService.bulkDownloadHIRSLogFiles(zipOut);
        } catch (Exception exception) {
            log.error("An exception was thrown while attempting to bulk download all the "
                    + "HIRS Attestation Logs", exception);
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

        final HIRSLogger mainHIRSLogger = this.helpPageService.getMainHIRSLogger();
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

            this.helpPageService.setLoggerLevel(loggerName, logLevel);
        } catch (Exception exception) {
            log.error("An exception was thrown while attempting to set the logging level for the"
                    + " selected logger", exception);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }

        final String helpPageUrl = "/HIRS_AttestationCAPortal/portal/help";

        return new RedirectView(helpPageUrl);
    }
}
