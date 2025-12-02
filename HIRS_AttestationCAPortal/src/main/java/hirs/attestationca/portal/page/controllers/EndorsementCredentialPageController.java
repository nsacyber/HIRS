package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.FilteredRecordsList;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.service.CertificatePageService;
import hirs.attestationca.persist.service.EndorsementCredentialPageService;
import hirs.attestationca.persist.service.util.CertificateType;
import hirs.attestationca.persist.service.util.DataTablesColumn;
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
 * Controller for the Endorsement Key Credentials page.
 */
@Log4j2
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/certificate-request/endorsement-key-credentials")
public class EndorsementCredentialPageController extends PageController<NoPageParams> {
    private final EndorsementCredentialPageService endorsementCredentialPageService;
    private final CertificatePageService certificatePageService;

    /**
     * Constructor for the Endorsement Credential Page Controller.
     *
     * @param endorsementCredentialPageService endorsement credential page service
     * @param certificatePageService           certificate page service
     */
    @Autowired
    public EndorsementCredentialPageController(
            final EndorsementCredentialPageService endorsementCredentialPageService,
            final CertificatePageService certificatePageService) {
        super(Page.ENDORSEMENT_KEY_CREDENTIALS);
        this.endorsementCredentialPageService = endorsementCredentialPageService;
        this.certificatePageService = certificatePageService;
    }

    /**
     * Returns the path for the view and the data model for the Endorsement Key Credentials page.
     *
     * @param params The object to map url parameters into.
     * @param model  The data model for the request. Can contain data from
     *               redirect.
     * @return the path for the view and data model for the Endorsement Key Credentials page.
     */
    @RequestMapping
    public ModelAndView initPage(final NoPageParams params, final Model model) {
        return getBaseModelAndView(Page.ENDORSEMENT_KEY_CREDENTIALS);
    }

    /**
     * Processes the request to retrieve a list of endorsement credentials for display on the endorsement credential's
     * page.
     *
     * @param dataTableInput data table input received from the front-end
     * @return data table of endorsement credentials
     */
    @ResponseBody
    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTableResponse<EndorsementCredential> getEndorsementCredentialsTableData(
            final DataTableInput dataTableInput) {
        log.info("Received request to display list of endorsement credentials");
        log.debug("Request received a datatable input object for the endorsement "
                + "credentials page: {}", dataTableInput);

        final String globalSearchTerm = dataTableInput.getSearch().getValue();
        final Set<DataTablesColumn> columnsWithSearchCriteria =
                ControllerPagesUtils.findColumnsWithSearchCriteriaForColumnSpecificSearch(
                        dataTableInput.getColumns());

        final int currentPage = dataTableInput.getStart() / dataTableInput.getLength();
        Pageable pageable = PageRequest.of(currentPage, dataTableInput.getLength());

        FilteredRecordsList<EndorsementCredential> ekFilteredRecordsList = new FilteredRecordsList<>();
        org.springframework.data.domain.Page<EndorsementCredential> pagedResult;

        // if the user has not entered any value in either the global search box or the column search box
        if (StringUtils.isBlank(globalSearchTerm) && columnsWithSearchCriteria.isEmpty()) {
            pagedResult = this.endorsementCredentialPageService.
                    findEndorsementCredentialsByArchiveFlag(false, pageable);
        }
        // if the search term applied to the individual columns is not empty
        else if (!columnsWithSearchCriteria.isEmpty()) {
            pagedResult =
                    this.certificatePageService.findCertificatesByColumnSpecificSearchTermAndArchiveFlag(
                            EndorsementCredential.class,
                            columnsWithSearchCriteria,
                            false,
                            pageable);
        } else {
            final Set<String> searchableColumnNames =
                    ControllerPagesUtils.findSearchableColumnNamesForGlobalSearch(EndorsementCredential.class,
                            dataTableInput.getColumns());

            pagedResult = this.certificatePageService.findCertificatesByGlobalSearchTermAndArchiveFlag(
                    EndorsementCredential.class,
                    searchableColumnNames,
                    globalSearchTerm,
                    false, pageable);
        }

        if (pagedResult.hasContent()) {
            ekFilteredRecordsList.addAll(pagedResult.getContent());
        }

        ekFilteredRecordsList.setRecordsFiltered(pagedResult.getTotalElements());
        ekFilteredRecordsList.setRecordsTotal(
                this.endorsementCredentialPageService.findEndorsementCredentialRepositoryCount());

        log.info("Returning the size of the list of endorsement credentials: {}",
                ekFilteredRecordsList.getRecordsFiltered());
        return new DataTableResponse<>(ekFilteredRecordsList, dataTableInput);
    }

    /**
     * Processes the request to download the specified endorsement credential.
     *
     * @param id       the UUID of the endorsement credential to download
     * @param response the response object (needed to update the header with the
     *                 file name)
     * @throws IOException when writing to response output stream
     */
    @GetMapping("/download")
    public void downloadEndorsementCredential(@RequestParam final String id,
                                              final HttpServletResponse response)
            throws IOException {
        log.info("Received request to download endorsement credential id {}", id);

        try {
            final DownloadFile downloadFile =
                    this.certificatePageService.downloadCertificate(EndorsementCredential.class,
                            UUID.fromString(id));
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;" + downloadFile.getFileName());
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.getOutputStream().write(downloadFile.getFileBytes());
        } catch (Exception exception) {
            log.error("An exception was thrown while attempting to download the"
                    + " specified endorsement credential", exception);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Processes the request to bulk download all the endorsement credentials.
     *
     * @param response the response object (needed to update the header with the
     *                 file name)
     * @throws IOException when writing to response output stream
     */
    @GetMapping("/bulk-download")
    public void bulkDownloadEndorsementCredentials(final HttpServletResponse response) throws IOException {
        log.info("Received request to download all endorsement credentials");

        final String zipFileName = "endorsement_certificates.zip";
        final String singleFileName = "Endorsement_Certificates";

        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + zipFileName);
        response.setContentType("application/zip");

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            this.certificatePageService.bulkDownloadCertificates(zipOut,
                    CertificateType.ENDORSEMENT_CREDENTIALS,
                    singleFileName);
        } catch (Exception exception) {
            log.error("An exception was thrown while attempting to bulk download all the "
                    + "endorsement credentials", exception);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Processes the request to upload one or more endorsement credentials to the ACA.
     *
     * @param files the files to process
     * @param attr  the redirection attributes
     * @return the redirection view
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("/upload")
    protected RedirectView uploadEndorsementCredential(@RequestParam("file") final MultipartFile[] files,
                                                       final RedirectAttributes attr)
            throws URISyntaxException {
        log.info("Received request to upload one or more endorsement credentials");

        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        for (MultipartFile file : files) {
            List<String> errorMessages = new ArrayList<>();
            List<String> successMessages = new ArrayList<>();

            EndorsementCredential parsedEndorsementCredential =
                    this.endorsementCredentialPageService.parseEndorsementCredential(file, errorMessages);

            if (parsedEndorsementCredential != null) {
                this.certificatePageService.storeCertificate(CertificateType.ENDORSEMENT_CREDENTIALS,
                        file.getOriginalFilename(),
                        successMessages, errorMessages, parsedEndorsementCredential);
            }

            messages.addSuccessMessages(successMessages);
            messages.addErrorMessages(errorMessages);
        }

        model.put(MESSAGES_ATTRIBUTE, messages);
        return redirectTo(Page.ENDORSEMENT_KEY_CREDENTIALS, new NoPageParams(), model, attr);
    }

    /**
     * Processes the request to archive/soft delete the specified endorsement credential.
     *
     * @param id   the UUID of the endorsement certificate to delete
     * @param attr RedirectAttributes used to forward data back to the original
     *             page.
     * @return redirect to this page
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("/delete")
    public RedirectView deleteEndorsementCredential(@RequestParam final String id,
                                                    final RedirectAttributes attr)
            throws URISyntaxException {
        log.info("Received request to delete endorsement credential id {}", id);

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
                    + " endorsement credential";
            messages.addErrorMessage(errorMessage);
            log.error(errorMessage, exception);
        }

        model.put(MESSAGES_ATTRIBUTE, messages);
        return redirectTo(Page.ENDORSEMENT_KEY_CREDENTIALS, new NoPageParams(), model, attr);
    }
}
