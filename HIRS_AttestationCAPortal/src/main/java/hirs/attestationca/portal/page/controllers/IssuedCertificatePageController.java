package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.FilteredRecordsList;
import hirs.attestationca.persist.entity.userdefined.DataTablesColumn;
import hirs.attestationca.persist.entity.userdefined.DownloadFile;
import hirs.attestationca.persist.entity.userdefined.certificate.IssuedAttestationCertificate;
import hirs.attestationca.persist.enums.CertificateType;
import hirs.attestationca.persist.service.CertificatePageService;
import hirs.attestationca.persist.service.IssuedAttestationCertificatePageService;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.datatables.Order;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.PageMessages;
import hirs.attestationca.portal.page.params.NoPageParams;
import hirs.attestationca.portal.page.utils.ControllerPagesUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
 * Controller for the Issued Certificates page.
 */
@Log4j2
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/certificate-request/issued-certificates")
public class IssuedCertificatePageController extends PageController<NoPageParams> {
    private final IssuedAttestationCertificatePageService issuedAttestationCertificateService;
    private final CertificatePageService certificatePageService;

    /**
     * Constructor for the Issued Attestation Certificate page.
     *
     * @param issuedAttestationCertificatePageService issued certificate page service
     * @param certificatePageService                  certificate page service
     */
    @Autowired
    public IssuedCertificatePageController(
            final IssuedAttestationCertificatePageService issuedAttestationCertificatePageService,
            final CertificatePageService certificatePageService) {
        super(Page.ISSUED_CERTIFICATES);
        this.issuedAttestationCertificateService = issuedAttestationCertificatePageService;
        this.certificatePageService = certificatePageService;
    }

    /**
     * Returns the path for the view and the data model for the Issued Attestation Certificate page.
     *
     * @param params The object to map url parameters into.
     * @param model  The data model for the request. Can contain data from
     *               redirect.
     * @return the path for the view and data model for the Issued Attestation Certificate page.
     */
    @RequestMapping
    public ModelAndView initPage(final NoPageParams params, final Model model) {
        return getBaseModelAndView(Page.ISSUED_CERTIFICATES);
    }

    /**
     * Processes the request to retrieve a list of issued attestation certificates for display on the issued
     * certificates page.
     *
     * @param dataTableInput data table input received from the front-end
     * @return data table of issued certificates
     */
    @ResponseBody
    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTableResponse<IssuedAttestationCertificate> getIssuedCertificatesTableData(
            final DataTableInput dataTableInput) {
        log.info("Received request to display list of issued attestation certificates");
        log.debug("Request received a datatable input object for the issued attestation"
                + " certificate page: {}", dataTableInput);

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
                        IssuedAttestationCertificate.class,
                        dataTableInput.getColumns());

        Pageable pageable = ControllerPagesUtils.createPageableObject(
                dataTableInput.getStart(),
                dataTableInput.getLength(),
                orderColumn);

        FilteredRecordsList<IssuedAttestationCertificate> issuedCertificateFilteredRecordsList =
                getFilteredIssuedCertificateList(
                        globalSearchTerm,
                        columnsWithSearchCriteria,
                        searchableColumnNames,
                        pageable);

        log.info("Returning the size of the filtered list of issued certificates: "
                + "{}", issuedCertificateFilteredRecordsList.getRecordsFiltered());
        return new DataTableResponse<>(issuedCertificateFilteredRecordsList, dataTableInput);
    }

    /**
     * Processes the request to download the specified issued attestation certificate.
     *
     * @param id       the UUID of the issued attestation certificate to download
     * @param response the response object (needed to update the header with the
     *                 file name)
     * @throws IOException when writing to response output stream
     */
    @GetMapping("/download")
    public void downloadIssuedCertificate(@RequestParam final String id, final HttpServletResponse response)
            throws IOException {
        log.info("Received request to download issued certificate id {}", id);

        try {
            final DownloadFile downloadFile =
                    this.certificatePageService.downloadCertificate(IssuedAttestationCertificate.class,
                            UUID.fromString(id));
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;" + downloadFile.getFileName());
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.getOutputStream().write(downloadFile.getFileBytes());
        } catch (Exception exception) {
            log.error("An exception was thrown while attempting to download the"
                    + " specified issued attestation certificate", exception);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Processes the request to bulk download all the issued attestation certificates.
     *
     * @param response the response object (needed to update the header with the
     *                 file name)
     * @throws IOException when writing to response output stream
     */
    @GetMapping("/bulk-download")
    public void bulkDownloadIssuedCertificates(final HttpServletResponse response)
            throws IOException {
        log.info("Received request to download all issued certificates");

        final String singleFileName = "Issued_Certificate";
        final String zipFileName = "issued_certificates.zip";

        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + zipFileName);
        response.setContentType("application/zip");

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            this.certificatePageService.bulkDownloadCertificates(zipOut, CertificateType.ISSUED_CERTIFICATES,
                    singleFileName);
        } catch (Exception exception) {
            log.error("An exception was thrown while attempting to bulk download all the "
                    + "issued attestation certificates", exception);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Processes the request to archive/soft delete the specified issued attestation certificate.
     *
     * @param id                 the UUID of the issued attestation certificate to delete
     * @param redirectAttributes RedirectAttributes used to forward data back to the original
     *                           page.
     * @return redirect to this page
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("/delete")
    public RedirectView deleteIssuedCertificate(@RequestParam final String id,
                                                final RedirectAttributes redirectAttributes)
            throws URISyntaxException {
        log.info("Received request to delete issued attestation certificate id {}", id);

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
                    + " the specified issued attestation certificate";
            messages.addErrorMessage(errorMessage);
            log.error(errorMessage, exception);
        }

        model.put(MESSAGES_ATTRIBUTE, messages);
        return redirectTo(Page.ISSUED_CERTIFICATES, new NoPageParams(), model, redirectAttributes);
    }

    /**
     * Processes the request to delete multiple issued attestation certificates.
     *
     * @param ids                the list of UUIDs of the issued attestation certificates to be deleted
     * @param redirectAttributes used to pass data back to the original page after the operation
     * @return a redirect to the issued attestation certificate page
     * @throws URISyntaxException if the URI is malformed
     */
    @PostMapping("/bulk-delete")
    public RedirectView bulkDeleteIssuedCertificates(@RequestParam final List<String> ids,
                                                     final RedirectAttributes redirectAttributes)
            throws URISyntaxException {
        log.info("Received request to delete multiple issued attestation certificates");

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
                    + " multiple issued attestation certificates";
            messages.addErrorMessage(errorMessage);
            log.error(errorMessage, exception);
        }

        model.put(MESSAGES_ATTRIBUTE, messages);
        return redirectTo(Page.ISSUED_CERTIFICATES, new NoPageParams(), model, redirectAttributes);
    }

    /**
     * Helper method that retrieves a filtered and paginated list of issued certificates based on the
     * provided search criteria.
     * The method allows filtering based on a global search term and column-specific search criteria,
     * and returns the result in a paginated format.
     *
     * <p>
     * The method handles four cases:
     * <ol>
     *     <li>If no global search term and no column-specific search criteria are provided,
     *         all issued certificates are returned.</li>
     *     <li>If both a global search term and column-specific search criteria are provided,
     *         it performs filtering on both.</li>
     *     <li>If only column-specific search criteria are provided, it filters based on the column-specific
     *         criteria.</li>
     *     <li>If only a global search term is provided, it filters based on the global search term.</li>
     * </ol>
     * </p>
     *
     * @param globalSearchTerm          A global search term that will be used to filter the issued certificates
     *                                  by the searchable fields.
     * @param columnsWithSearchCriteria A set of columns with specific search criteria entered by the user.
     * @param searchableColumnNames     A set of searchable column names that are  for the global search term.
     * @param pageable                  pageable
     * @return A {@link FilteredRecordsList} containing the filtered and paginated list of
     * issued certificates, along with the total number of records and the number of records matching the
     * filter criteria.
     */
    private FilteredRecordsList<IssuedAttestationCertificate> getFilteredIssuedCertificateList(
            final String globalSearchTerm,
            final Set<DataTablesColumn> columnsWithSearchCriteria,
            final Set<String> searchableColumnNames,
            final Pageable pageable) {
        org.springframework.data.domain.Page<IssuedAttestationCertificate> pagedResult;

        // if no value has been entered in the global search textbox and in the column search dropdown
        if (StringUtils.isBlank(globalSearchTerm) && columnsWithSearchCriteria.isEmpty()) {
            pagedResult = this.issuedAttestationCertificateService.
                    findIssuedCertificatesByArchiveFlag(false, pageable);
        } else if (!StringUtils.isBlank(globalSearchTerm) && !columnsWithSearchCriteria.isEmpty()) {
            // if a value has been entered in both the global search textbox and in the column search dropdown
            pagedResult =
                    this.certificatePageService.findCertificatesByGlobalAndColumnSpecificSearchTerm(
                            IssuedAttestationCertificate.class,
                            searchableColumnNames,
                            globalSearchTerm,
                            columnsWithSearchCriteria,
                            false,
                            pageable);
        } else if (!columnsWithSearchCriteria.isEmpty()) {
            // if a value has been entered ONLY in the column search dropdown
            pagedResult =
                    this.certificatePageService.findCertificatesByColumnSpecificSearchTermAndArchiveFlag(
                            IssuedAttestationCertificate.class,
                            columnsWithSearchCriteria,
                            false,
                            pageable);
        } else {
            // if a value has been entered ONLY in the global search textbox
            pagedResult = this.certificatePageService.findCertificatesByGlobalSearchTermAndArchiveFlag(
                    IssuedAttestationCertificate.class,
                    searchableColumnNames,
                    globalSearchTerm,
                    false,
                    pageable);
        }

        FilteredRecordsList<IssuedAttestationCertificate> issuedCertificateFilteredRecordsList =
                new FilteredRecordsList<>();

        if (pagedResult.hasContent()) {
            issuedCertificateFilteredRecordsList.addAll(pagedResult.getContent());
        }

        issuedCertificateFilteredRecordsList.setRecordsFiltered(pagedResult.getTotalElements());
        issuedCertificateFilteredRecordsList.setRecordsTotal(
                this.issuedAttestationCertificateService.findIssuedCertificateRepoCount());

        return issuedCertificateFilteredRecordsList;
    }
}
