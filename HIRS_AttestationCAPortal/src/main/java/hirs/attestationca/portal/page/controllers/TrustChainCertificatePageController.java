package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.entity.manager.CACredentialRepository;
import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.userdefined.DataTablesColumn;
import hirs.attestationca.persist.entity.userdefined.DownloadFile;
import hirs.attestationca.persist.entity.userdefined.FilteredRecordsList;
import hirs.attestationca.persist.entity.userdefined.certificate.CertificateAuthorityCredential;
import hirs.attestationca.persist.enums.CertificateType;
import hirs.attestationca.persist.service.CertificatePageService;
import hirs.attestationca.persist.service.TrustChainCertificatePageService;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.datatables.Order;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.PageMessages;
import hirs.attestationca.portal.page.params.NoPageParams;
import hirs.attestationca.portal.page.utils.CertificateStringMapBuilder;
import hirs.attestationca.portal.page.utils.ControllerPagesUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipOutputStream;

/**
 * Controller for the Trust Chain Certificates page.
 */
@Log4j2
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/certificate-request/trust-chain")
public class TrustChainCertificatePageController extends PageController<NoPageParams> {
    /**
     * Model attribute name used by initPage for the aca cert info.
     */
    static final String ACA_CERT_DATA = "acaCertData";

    private final CertificateRepository certificateRepository;
    private final CACredentialRepository caCredentialRepository;
    private final CertificatePageService certificatePageService;
    private final List<CertificateAuthorityCredential> certificateAuthorityCredentials;
    private final TrustChainCertificatePageService trustChainCertificatePageService;

    /**
     * Constructor for the Trust Chain Certificate Page Controller.
     *
     * @param certificateRepository            certificate repository
     * @param caCredentialRepository           caCredential repository
     * @param certificatePageService           certificate page service
     * @param trustChainCertificatePageService trust chain certificate page service
     * @param acaTrustChainCertificates        ACA Trust Chain certificates
     */
    @Autowired
    public TrustChainCertificatePageController(final CertificateRepository certificateRepository,
                                               final CACredentialRepository caCredentialRepository,
                                               final CertificatePageService certificatePageService,
                                               final TrustChainCertificatePageService trustChainCertificatePageService,
                                               @Qualifier("acaTrustChainCerts") final X509Certificate[]
                                                       acaTrustChainCertificates) {
        super(Page.TRUST_CHAIN);
        this.certificateRepository = certificateRepository;
        this.caCredentialRepository = caCredentialRepository;
        this.certificatePageService = certificatePageService;
        this.trustChainCertificatePageService = trustChainCertificatePageService;
        this.certificateAuthorityCredentials = new ArrayList<>();

        try {
            for (X509Certificate eachCert : acaTrustChainCertificates) {
                this.certificateAuthorityCredentials.add(
                        new CertificateAuthorityCredential(eachCert.getEncoded()));
            }
        } catch (IOException ioEx) {
            log.error("Failed to read ACA certificate", ioEx);
        } catch (CertificateEncodingException ceEx) {
            log.error("Error getting encoded ACA certificate", ceEx);
        }
    }

    /**
     * Returns the path for the view and the data model for the Trust Chain certificate page.
     *
     * @param params The object to map url parameters into.
     * @param model  The data model for the request. Can contain data from
     *               redirect.
     * @return the path for the view and data model for the Trust Chain certificate page.
     */
    @RequestMapping
    public ModelAndView initPage(final NoPageParams params, final Model model) {

        ModelAndView mav = getBaseModelAndView(Page.TRUST_CHAIN);

        mav.addObject(ACA_CERT_DATA,
                new HashMap<>(CertificateStringMapBuilder.getCertificateAuthorityInformation(
                        this.certificateAuthorityCredentials, this.certificateRepository,
                        this.caCredentialRepository)));

        return mav;
    }

    /**
     * Processes the request to retrieve a list of trust chain certificates that will be
     * displayed on the trust chain certificates page.
     *
     * @param dataTableInput data table input received from the front-end
     * @return data table of trust chain certificates
     */
    @ResponseBody
    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTableResponse<CertificateAuthorityCredential> getTrustChainCertificatesTableData(
            final DataTableInput dataTableInput) {
        log.info("Received request to display list of trust chain certificates");
        log.debug("Request received a datatable input object for the trust chain certificates page: {}",
                dataTableInput);

        // grab the column to which ordering has been applied
        final Order orderColumn = dataTableInput.getOrderColumn();

        // grab the value that was entered in the global search textbox
        final String globalSearchTerm = dataTableInput.getSearch().getValue();

        // find all columns that have a value that's been entered in column search dropdown
        final Set<DataTablesColumn> columnsWithSearchCriteria =
                ControllerPagesUtils.findColumnsWithSearchCriteriaForColumnSpecificSearch(
                        dataTableInput.getColumns());

        // find all columns that are considered searchable
        final Set<String> searchableColumnNames =
                ControllerPagesUtils.findSearchableColumnNamesForGlobalSearch(
                        CertificateAuthorityCredential.class,
                        dataTableInput.getColumns());

        Pageable pageable = ControllerPagesUtils.createPageableObject(
                dataTableInput.getStart(),
                dataTableInput.getLength(),
                orderColumn);

        FilteredRecordsList<CertificateAuthorityCredential> caFilteredRecordsList =
                getFilteredTrustChainsList(
                        globalSearchTerm,
                        columnsWithSearchCriteria,
                        searchableColumnNames,
                        pageable);

        log.info("Returning the size of the filtered list of trust chain certificates: "
                + " {}", caFilteredRecordsList.getRecordsFiltered());
        return new DataTableResponse<>(caFilteredRecordsList, dataTableInput);
    }

    /**
     * Processes the request to download the selected trust chain certificate.
     *
     * @param id       the UUID of the trust chain certificate to download
     * @param response the response object (needed to update the header with the
     *                 file name)
     * @throws IOException when writing to response output stream
     */
    @GetMapping("/download")
    public void downloadTrustChainCertificate(@RequestParam final String id,
                                              final HttpServletResponse response)
            throws IOException {
        log.info("Received request to download trust chain certificate {}", id);

        try {
            final DownloadFile downloadFile =
                    this.certificatePageService.downloadCertificate(CertificateAuthorityCredential.class,
                            UUID.fromString(id));
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;" + downloadFile.getFileName());
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.getOutputStream().write(downloadFile.getFileBytes());
        } catch (Exception exception) {
            log.error("An exception was thrown while attempting to download the"
                    + " specified trust chain certificate", exception);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Processes the request to download the ACA trust chain certificates.
     *
     * @param response the response object (needed to update the header with the
     *                 file name)
     * @throws IOException when writing to response output stream
     */
    @ResponseBody
    @GetMapping("/download-aca-cert-chain")
    public void downloadACATrustChain(final HttpServletResponse response) throws IOException {
        log.info("Received request to download the ACA server trust chain certificates");

        // Get the output stream of the response
        try (OutputStream outputStream = response.getOutputStream()) {
            // PEM file of the leaf certificate, intermediate certificate and root certificate (in that order)
            final String fullChainPEM = ControllerPagesUtils.convertCertificateArrayToPem(
                    new CertificateAuthorityCredential[] {certificateAuthorityCredentials.get(0),
                            certificateAuthorityCredentials.get(1),
                            certificateAuthorityCredentials.get(2)});

            final String pemFileName = "hirs-aca-trust_chain.pem ";

            response.setContentType("application/x-pem-file");  // MIME type for PEM files
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + pemFileName);
            response.setContentLength(fullChainPEM.length());

            outputStream.write(fullChainPEM.getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        } catch (Exception exception) {
            log.error("An exception was thrown while attempting to download the"
                    + "aca trust chain", exception);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Processes the request to bulk download all the trust chain certificates.
     *
     * @param response the response object (needed to update the header with the
     *                 file name)
     * @throws IOException when writing to response output stream
     */
    @GetMapping("/bulk-download")
    public void bulkDownloadTrustChainCertificates(final HttpServletResponse response) throws IOException {
        log.info("Received request to download all trust chain certificates");
        final String zipFileName = "trust-chain.zip";
        final String singleFileName = "ca-certificates";

        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + zipFileName);
        response.setContentType("application/zip");

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            this.certificatePageService.bulkDownloadCertificates(zipOut, CertificateType.TRUST_CHAIN,
                    singleFileName);
        } catch (Exception exception) {
            log.error("An exception was thrown while attempting to bulk download all the"
                    + "trust chain certificates", exception);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Processes the request to upload one or more trust chain certificates.
     *
     * @param files              the files to process
     * @param redirectAttributes Redirect Attributes used to forward data back to the original
     *                           page.
     * @return redirect to the trust chain certificate page
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("/upload")
    protected RedirectView uploadTrustChainCertificate(@RequestParam("file") final MultipartFile[] files,
                                                       final RedirectAttributes redirectAttributes)
            throws URISyntaxException {

        log.info("Received request to upload one or more trust chain certificates");

        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        for (MultipartFile file : files) {
            List<String> errorMessages = new ArrayList<>();
            List<String> successMessages = new ArrayList<>();

            CertificateAuthorityCredential parsedTrustChainCertificate =
                    this.certificatePageService.parseTrustChainCertificate(file, successMessages,
                            errorMessages);

            if (parsedTrustChainCertificate != null) {
                certificatePageService.storeCertificate(
                        CertificateType.TRUST_CHAIN,
                        file.getOriginalFilename(),
                        successMessages, errorMessages, parsedTrustChainCertificate);
            }

            messages.addSuccessMessages(successMessages);
            messages.addErrorMessages(errorMessages);
        }

        model.put(MESSAGES_ATTRIBUTE, messages);
        return redirectTo(Page.TRUST_CHAIN, new NoPageParams(), model, redirectAttributes);
    }

    /**
     * Processes the request to archive/soft delete the provided trust chain certificate.
     *
     * @param id                 the UUID of the trust chain certificate to delete
     * @param redirectAttributes Redirect Attributes used to forward data back to the original
     *                           page.
     * @return redirect to the trust chain certificate page
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("/delete")
    public RedirectView deleteTrustChainCertificate(@RequestParam final String id,
                                                    final RedirectAttributes redirectAttributes)
            throws URISyntaxException {
        log.info("Received request to delete trust chain certificate id {}", id);

        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        List<String> successMessages = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        try {
            this.certificatePageService.deleteCertificate(UUID.fromString(id), successMessages,
                    errorMessages);
            messages.addSuccessMessages(successMessages);
            messages.addErrorMessages(errorMessages);
        } catch (Exception exception) {
            final String errorMessage = "An exception was thrown while attempting to delete"
                    + " the specified trust chain certificate";
            messages.addErrorMessage(errorMessage);
            log.error(errorMessage, exception);
        }

        model.put(MESSAGES_ATTRIBUTE, messages);
        return redirectTo(Page.TRUST_CHAIN, new NoPageParams(), model, redirectAttributes);
    }

    /**
     * Processes the request to delete multiple trust chain certificates.
     *
     * @param ids                the list of UUIDs of the trust chain certificates to be deleted
     * @param redirectAttributes used to pass data back to the original page after the operation
     * @return a redirect to the trust chain certificate page
     * @throws URISyntaxException if the URI is malformed
     */
    @PostMapping("/bulk-delete")
    public RedirectView bulkDeleteTrustChainCertificates(@RequestParam final List<String> ids,
                                                         final RedirectAttributes redirectAttributes)
            throws URISyntaxException {
        log.info("Received request to delete multiple trust chain certificates");

        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        List<String> successMessages = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        try {
            this.certificatePageService.bulkDeleteCertificates(ids, successMessages,
                    errorMessages);
            messages.addSuccessMessages(successMessages);
            messages.addErrorMessages(errorMessages);
        } catch (Exception exception) {
            final String errorMessage = "An exception was thrown while attempting to delete"
                    + " multiple trust chain certificates";
            messages.addErrorMessage(errorMessage);
            log.error(errorMessage, exception);
        }

        model.put(MESSAGES_ATTRIBUTE, messages);
        return redirectTo(Page.TRUST_CHAIN, new NoPageParams(), model, redirectAttributes);
    }

    /**
     * Helper method that retrieves a filtered and paginated list of trust chain certificates based on the
     * provided search criteria.
     * The method allows filtering based on a global search term and column-specific search criteria,
     * and returns the result in a paginated format.
     *
     * <p>
     * The method handles four cases:
     * <ol>
     *     <li>If no global search term and no column-specific search criteria are provided,
     *         all trust chain certificates are returned.</li>
     *     <li>If both a global search term and column-specific search criteria are provided,
     *         it performs filtering on both.</li>
     *     <li>If only column-specific search criteria are provided, it filters based on the column-specific
     *         criteria.</li>
     *     <li>If only a global search term is provided, it filters based on the global search term.</li>
     * </ol>
     * </p>
     *
     * @param globalSearchTerm          A global search term that will be used to filter the trust chain
     *                                  certificates by the searchable fields.
     * @param columnsWithSearchCriteria A set of columns with specific search criteria entered by the user.
     * @param searchableColumnNames     A set of searchable column names that are  for the global search term.
     * @param pageable                  pageable
     * @return A {@link FilteredRecordsList} containing the filtered and paginated list of
     * trust chain certificates, along with the total number of records and the number of records matching the
     * filter criteria.
     */
    private FilteredRecordsList<CertificateAuthorityCredential> getFilteredTrustChainsList(
            final String globalSearchTerm,
            final Set<DataTablesColumn> columnsWithSearchCriteria,
            final Set<String> searchableColumnNames,
            final Pageable pageable) {
        org.springframework.data.domain.Page<CertificateAuthorityCredential> pagedResult;

        // if no value has been entered in the global search textbox and in the column search dropdown
        if (StringUtils.isBlank(globalSearchTerm) && columnsWithSearchCriteria.isEmpty()) {
            pagedResult =
                    this.trustChainCertificatePageService.
                            findCACredentialsByArchiveFlag(false, pageable);
        } else if (!StringUtils.isBlank(globalSearchTerm) && !columnsWithSearchCriteria.isEmpty()) {
            // if a value has been entered in both the global search textbox and in the column search dropdown
            pagedResult =
                    this.certificatePageService.findCertificatesByGlobalAndColumnSpecificSearchTerm(
                            CertificateAuthorityCredential.class,
                            searchableColumnNames,
                            globalSearchTerm,
                            columnsWithSearchCriteria,
                            false,
                            pageable);
        } else if (!columnsWithSearchCriteria.isEmpty()) {
            // if a value has been entered ONLY in the column search dropdown
            pagedResult =
                    this.certificatePageService.findCertificatesByColumnSpecificSearchTermAndArchiveFlag(
                            CertificateAuthorityCredential.class,
                            columnsWithSearchCriteria,
                            false,
                            pageable);
        } else {
            pagedResult = this.certificatePageService.findCertificatesByGlobalSearchTermAndArchiveFlag(
                    // if a value has been entered ONLY in the global search textbox
                    CertificateAuthorityCredential.class,
                    searchableColumnNames,
                    globalSearchTerm,
                    false, pageable);
        }

        FilteredRecordsList<CertificateAuthorityCredential> caFilteredRecordsList =
                new FilteredRecordsList<>();

        if (pagedResult.hasContent()) {
            caFilteredRecordsList.addAll(pagedResult.getContent());
        }

        caFilteredRecordsList.setRecordsFiltered(pagedResult.getTotalElements());
        caFilteredRecordsList.setRecordsTotal(
                this.trustChainCertificatePageService.findTrustChainCertificateRepoCount());

        return caFilteredRecordsList;
    }
}
