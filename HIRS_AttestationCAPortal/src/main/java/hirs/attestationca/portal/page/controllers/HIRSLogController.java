package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.params.NoPageParams;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
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
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;

/**
 * Controller for the HIRS Attestation Log. The REST endpoints are specifically for actions that
 * are taken on the Help page.
 */
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/hirs-log")
@Log4j2
public class HIRSLogController extends PageController<NoPageParams> {
    @Value("${logging.file.path}")
    private String logFilePath;

    @Value("${logging.file.name}")
    private String logFileName;

    /**
     *
     */
    public HIRSLogController() {
        super(Page.HELP);
    }

    /**
     * Returns the path for the view and the data model for the Help page.
     *
     * @param params The object to map url parameters into.
     * @param model  The data model for the request. Can contain data from
     *               redirect.
     * @return the path for the view and data model for the Help page.
     */
    @Override
    public ModelAndView initPage(final NoPageParams params, final Model model) {
        return getBaseModelAndView(Page.HELP);
    }

    /**
     * Processes the request to download the HIRS application's log file.
     *
     * @param response response that will be sent out after processing download request
     * @throws IOException when writing to response output stream
     */
    @GetMapping("/download")
    public void downloadHIRSLog(final HttpServletResponse response) throws IOException {

        try {
            log.info("Received request to download the HIRS Attestation application's log file");
            final File logFile = new File(logFilePath + "/" + logFileName);

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
     * Processes the request that sets the log level of the HIRS application log file
     * based on the provided user input.
     *
     * @param response           response that will be sent out after processing download request
     * @param logLevel           logging level
     * @param redirectAttributes attributes needed for the redirect
     * @return the redirection view
     * @throws IOException when writing to response output stream
     */
    @PostMapping("/setLogLevel")
    public RedirectView setLogLevel(final HttpServletResponse response,
                                    @RequestParam final String logLevel,
                                    final RedirectAttributes redirectAttributes)
            throws IOException, URISyntaxException {
        try {
            log.info("Received a request to set the log level for the HIRS Application"
                    + " log file");
            // Convert the string log level to Log4j2 Level
            Level level = Level.getLevel(logLevel);

            log.info("The HIRS Application log file log level has been changed to {}", level);
        } catch (Exception exception) {
            log.error("An exception was thrown while attempting to set the logging level for the"
                    + " HIRS Application log file", exception);

            // send a 404 error when an exception is thrown while attempting to download the
            // HIRS log file
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }

        return redirectTo(Page.HELP, new NoPageParams(), new HashMap<>(), redirectAttributes);
    }
}
