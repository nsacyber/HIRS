package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.FilteredRecordsList;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.service.CertificatePageService;
import hirs.attestationca.persist.service.CertificateType;
import hirs.attestationca.persist.service.PlatformCredentialPageService;
import hirs.attestationca.persist.util.DownloadFile;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.PageMessages;
import hirs.attestationca.portal.page.params.NoPageParams;
import hirs.attestationca.portal.page.utils.ControllerPagesUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipOutputStream;

/**
 * Controller for the Platform Credentials page.
 */
@Log4j2
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/certificate-request/platform-credentials")
public class PlatformCredentialPageController extends PageController<NoPageParams> {

    private final CertificatePageService certificatePageService;
    private final PlatformCredentialPageService platformCredentialService;

    /**
     * Constructor for the Platform Credential Page Controller.
     *
     * @param certificatePageService    certificate page service
     * @param platformCredentialService platform credential service
     */
    @Autowired
    public PlatformCredentialPageController(final CertificatePageService certificatePageService,
                                            final PlatformCredentialPageService platformCredentialService) {
        super(Page.PLATFORM_CREDENTIALS);
        this.certificatePageService = certificatePageService;
        this.platformCredentialService = platformCredentialService;
    }

    /**
     * Returns the path for the view and the data model for the platform credential page.
     *
     * @param params The object to map url parameters into.
     * @param model  The data model for the request. Can contain data from
     *               redirect.
     * @return the path for the view and data model for the platform credential page.
     */
    @RequestMapping
    public ModelAndView initPage(final NoPageParams params, final Model model) {
        return getBaseModelAndView(Page.PLATFORM_CREDENTIALS);
    }

    /**
     * Processes the request to retrieve a list of platform credentials for display on the platform credentials page.
     *
     * @param input data table input received from the front-end
     * @return data table of platform credentials
     */
    @ResponseBody
    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTableResponse<PlatformCredential> getPlatformCredentialsTableData(final DataTableInput input) {
        log.info("Received request to display list of platform credentials");
        log.debug("Request received a datatable input object for the platform credentials page: {}", input);

        final String orderColumnName = input.getOrderColumnName();

        log.debug("Ordering on column: {}", orderColumnName);

        final String searchTerm = input.getSearch().getValue();

        final Set<String> searchableColumns = ControllerPagesUtils.findSearchableColumnsNames(PlatformCredential.class,
                input.getColumns());

        final int currentPage = input.getStart() / input.getLength();
        Pageable pageable = PageRequest.of(currentPage, input.getLength(), Sort.by(orderColumnName));

        FilteredRecordsList<PlatformCredential> pcFilteredRecordsList = new FilteredRecordsList<>();

        org.springframework.data.domain.Page<PlatformCredential> pagedResult;

        if (StringUtils.isBlank(searchTerm)) {
            pagedResult =
                    this.platformCredentialService.findPlatformCredentialsByArchiveFlag(false, pageable);
        } else {
            pagedResult =
                    this.certificatePageService.findCertificatesBySearchableColumnsAndArchiveFlag(
                            PlatformCredential.class,
                            searchableColumns,
                            searchTerm,
                            false, pageable);
        }

        if (pagedResult.hasContent()) {
            pcFilteredRecordsList.addAll(pagedResult.getContent());
        }

        pcFilteredRecordsList.setRecordsFiltered(pagedResult.getTotalElements());
        pcFilteredRecordsList.setRecordsTotal(this.platformCredentialService.findPlatformCredentialRepositoryCount());

        EndorsementCredential associatedEC;

        if (!pcFilteredRecordsList.isEmpty()) {
            // loop all the platform credentials
            for (PlatformCredential pc : pcFilteredRecordsList) {
                // find the EC using the PC's "holder serial number"
                associatedEC = this.platformCredentialService
                        .findECBySerialNumber(pc.getHolderSerialNumber());

                if (associatedEC != null) {
                    log.debug("EC ID for holder s/n {} = {}", pc
                            .getHolderSerialNumber(), associatedEC.getId());
                }

                pc.setEndorsementCredential(associatedEC);
            }
        }

        log.info("Returning the size of the list of platform credentials: {}",
                pcFilteredRecordsList.getRecordsFiltered());
        return new DataTableResponse<>(pcFilteredRecordsList, input);
    }

    /**
     * Processes the request to download the selected platform credential.
     *
     * @param id       the UUID of the platform credential to download
     * @param response the response object (needed to update the header with the
     *                 file name)
     * @throws IOException when writing to response output stream
     */
    @GetMapping("/download")
    public void downloadPlatformCredential(@RequestParam final String id, final HttpServletResponse response)
            throws IOException {
        log.info("Received request to download platform credential id {}", id);

        try {
            final DownloadFile downloadFile = this.certificatePageService.downloadCertificate(PlatformCredential.class,
                    UUID.fromString(id));
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;" + downloadFile.getFileName());
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.getOutputStream().write(downloadFile.getFileBytes());
        } catch (Exception exception) {
            log.error("An exception was thrown while attempting to download the"
                    + " specified platform credential", exception);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Processes the request to bulk download all the platform credentials.
     *
     * @param response the response object (needed to update the header with the
     *                 file name)
     * @throws IOException when writing to response output stream
     */
    @GetMapping("/bulk-download")
    public void bulkDownloadPlatformCredentials(final HttpServletResponse response) throws IOException {
        log.info("Received request to download all platform credentials");

        final String zipFileName = "platform_certificates.zip";
        final String singleFileName = "Platform_Certificate";

        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + zipFileName);
        response.setContentType("application/zip");

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            this.certificatePageService.bulkDownloadCertificates(zipOut, CertificateType.PLATFORM_CREDENTIALS,
                    singleFileName);
        } catch (Exception exception) {
            log.error("An exception was thrown while attempting to bulk download all the"
                    + "platform credentials", exception);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Processes the request to upload one or more platform credentials to the ACA.
     *
     * @param files the files to process
     * @param attr  the redirection attributes
     * @return the redirection view
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("/upload")
    protected RedirectView uploadPlatformCredentials(
            @RequestParam("file") final MultipartFile[] files,
            final RedirectAttributes attr) throws URISyntaxException {
        log.info("Received request to upload one or more platform credentials");

        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        for (MultipartFile file : files) {
            List<String> errorMessages = new ArrayList<>();
            List<String> successMessages = new ArrayList<>();

            PlatformCredential parsedPlatformCredential = this.platformCredentialService.parsePlatformCredential(file,
                    errorMessages);

            if (parsedPlatformCredential != null) {
                certificatePageService.storeCertificate(
                        CertificateType.PLATFORM_CREDENTIALS,
                        file.getOriginalFilename(),
                        successMessages, errorMessages, parsedPlatformCredential);
            }

            messages.addSuccessMessages(successMessages);
            messages.addErrorMessages(errorMessages);
        }

        model.put(MESSAGES_ATTRIBUTE, messages);
        return redirectTo(Page.PLATFORM_CREDENTIALS, new NoPageParams(), model, attr);
    }

    /**
     * Processes the request to archive/soft delete the provided platform credential.
     *
     * @param id   the UUID of the platform credential to delete
     * @param attr RedirectAttributes used to forward data back to the original
     *             page.
     * @return redirect to this page
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("/delete")
    public RedirectView deletePlatformCredential(@RequestParam final String id,
                                                 final RedirectAttributes attr) throws URISyntaxException {
        log.info("Received request to delete platform credential id {}", id);

        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        List<String> successMessages = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        try {
            this.certificatePageService.deleteCertificate(UUID.fromString(id),
                    successMessages, errorMessages);

            messages.addSuccessMessages(successMessages);
            messages.addErrorMessages(errorMessages);
        } catch (Exception exception) {
            final String errorMessage = "An exception was thrown while attempting to delete"
                    + " the specified platform credential";
            messages.addErrorMessage(errorMessage);
            log.error(errorMessage, exception);
        }

        model.put(MESSAGES_ATTRIBUTE, messages);
        return redirectTo(Page.PLATFORM_CREDENTIALS, new NoPageParams(), model, attr);
    }
}
