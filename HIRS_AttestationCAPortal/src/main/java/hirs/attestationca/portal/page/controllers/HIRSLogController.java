package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.params.NoPageParams;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.io.IOException;

/**
 * Controller for the HIRS Attestation Log page.
 */
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/hirs-log")
@Log4j2
public class HIRSLogController extends PageController<NoPageParams> {

    private static final String LOG_FILE_PATH = "/var/log/hirs/HIRS_AttestationCA_Portal.log";

    /**
     * Constructor for the HIRS Attestation Log page.
     */
    public HIRSLogController() {
        super(Page.HIRS_LOG);
    }

    /**
     * Returns the path for the view and the data model for the HIRS log page.
     *
     * @param params The object to map url parameters into.
     * @param model  The data model for the request. Can contain data from
     *               redirect.
     * @return model and view of the HIRS log page
     */
    @Override
    public ModelAndView initPage(final NoPageParams params, final Model model) {
        return getBaseModelAndView(Page.HIRS_LOG);
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
            final File logFile = new File(LOG_FILE_PATH);

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
}
