package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.FilteredRecordsList;
import hirs.attestationca.persist.entity.userdefined.HIRSLogger;
import hirs.attestationca.persist.service.HelpPageService;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.PageMessages;
import hirs.attestationca.portal.page.params.NoPageParams;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
     * Constructor for the Help Page Controller.
     *
     * @param helpPageService help service
     */
    @Autowired
    public HelpPageController(final HelpPageService helpPageService) {
        super(Page.HELP);
        this.helpPageService = helpPageService;
    }

    /**
     * Returns the path for the view and the data model for the help page.
     *
     * @param params The object to map url parameters into.
     * @param model  The data model for the request. Can contain data from redirect.
     * @return the path for the view and data model for the help page.
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
        log.info(
                "Received request to download a zip file of all the HIRS Attestation application's log files");

        final String zipFileName = "HIRS_AttestationCAPortal_Logs.zip";

        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + zipFileName);
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
     * Processes the request to retrieve the main HIRS logger for display on the help page.
     *
     * @param dataTableInput data table input received from the front-end
     * @return data table of just the main HIRS logger
     */
    @ResponseBody
    @GetMapping(value = "/list-main-logger", produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTableResponse<HIRSLogger> getMainHIRSLogger(final DataTableInput dataTableInput) {

        log.info("Received request to display the main HIRS logger");
        log.debug("Request received a datatable input object for listing the main HIRS logger: {}",
                dataTableInput);

        FilteredRecordsList<HIRSLogger> mainHIRSLoggersFilteredRecordsList = new FilteredRecordsList<>();

        final HIRSLogger mainHIRSLogger = this.helpPageService.getMainHIRSLogger();
        mainHIRSLoggersFilteredRecordsList.add(mainHIRSLogger);
        mainHIRSLoggersFilteredRecordsList.setRecordsTotal(1);
        mainHIRSLoggersFilteredRecordsList.setRecordsFiltered(1);

        log.info("Returning the size of the filtered list of main HIRS loggers: "
                + "{}", mainHIRSLoggersFilteredRecordsList.getRecordsFiltered());

        return new DataTableResponse<>(mainHIRSLoggersFilteredRecordsList, dataTableInput);
    }

    /**
     * Processes the request that sets the log level of the selected logger.
     *
     * @param loggerName         logger name
     * @param logLevel           logging level
     * @param redirectAttributes RedirectAttributes used to forward data back to the original
     *                           page.
     * @return the redirection view
     * @throws URISyntaxException if any issues arise from redirecting to the Help page.
     */
    @PostMapping("/setLogLevel")
    public RedirectView setLogLevel(@RequestParam final String loggerName,
                                    @RequestParam final String logLevel,
                                    final RedirectAttributes redirectAttributes) throws URISyntaxException {
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        List<String> successMessages = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        try {
            log.info("Received a request to set the log level [{}] for the provided logger [{}]", logLevel,
                    loggerName);

            this.helpPageService.setLoggerLevel(loggerName, logLevel, successMessages, errorMessages);

            messages.addSuccessMessages(successMessages);
            messages.addErrorMessages(errorMessages);
        } catch (Exception exception) {
            final String errorMessage =
                    "An exception was thrown while attempting to set the logging level for the"
                            + " selected logger";
            log.error(errorMessage, exception);
            messages.addErrorMessage(errorMessage);
        }

        model.put(MESSAGES_ATTRIBUTE, messages);
        return redirectTo(Page.HELP, new NoPageParams(), model, redirectAttributes);
    }
}
