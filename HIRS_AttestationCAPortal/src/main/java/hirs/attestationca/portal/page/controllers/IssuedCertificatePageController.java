package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.entity.userdefined.DataTablesColumn;
import hirs.attestationca.persist.entity.userdefined.DownloadFile;
import hirs.attestationca.persist.entity.userdefined.FilteredRecordsList;
import hirs.attestationca.persist.entity.userdefined.certificate.IssuedAttestationCertificate;
import hirs.attestationca.persist.enums.CertificateType;
import hirs.attestationca.persist.service.CertificatePageService;
import hirs.attestationca.persist.service.IssuedCertificatePageService;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.datatables.Order;
import hirs.attestationca.portal.page.Page;
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
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/certificate-request/issued-certificates")
@Log4j2
public class IssuedCertificatePageController extends PageController<NoPageParams> {
    private final IssuedCertificatePageService issuedCertificatePageService;
    private final CertificatePageService certificatePageService;

    /**
     * Constructor for the Issued Certificates page.
     *
     * @param issuedCertificatePageService issued certificate page service
     * @param certificatePageService       certificate page service
     */
    @Autowired
    public IssuedCertificatePageController(final IssuedCertificatePageService issuedCertificatePageService,
                                           final CertificatePageService certificatePageService) {
        super(Page.ISSUED_CERTIFICATES);
        this.issuedCertificatePageService = issuedCertificatePageService;
        this.certificatePageService = certificatePageService;
    }

    /**
     * Returns the path for the view and the data model for the Issued Certificate page.
     *
     * @param params The object to map url parameters into.
     * @param model  The data model for the request. Can contain data from redirect.
     * @return the path for the view and data model for the Issued Certificate page.
     */
    @RequestMapping
    public ModelAndView initPage(final NoPageParams params, final Model model) {
        return getBaseModelAndView(Page.ISSUED_CERTIFICATES);
    }

    /**
     * Processes the request to retrieve a list of {@link IssuedAttestationCertificate} objects for display on the
     * Issued Certificates page.
     *
     * @param dataTableInput data table input received from the front-end
     * @return data table of {@link IssuedAttestationCertificate} objects
     */
    @ResponseBody
    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTableResponse<IssuedAttestationCertificate> getIssuedCertificatesTableData(
            final DataTableInput dataTableInput) {
        log.info("Received request to display list of issued certificates");
        log.debug("Request received a datatable input object for the Issued Certificates"
                + " page: {}", dataTableInput);

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
     * Processes the request to download the specified {@link IssuedAttestationCertificate} object.
     *
     * @param id       the UUID of the {@link IssuedAttestationCertificate} object to download
     * @param response the response object (needed to update the header with the file name)
     * @throws IOException when writing to response output stream
     */
    @GetMapping("/download")
    public void downloadIssuedCertificate(@RequestParam final String id, final HttpServletResponse response)
            throws IOException {
        log.info("Received request to download issued certificate id {}", id);

        try {
            final DownloadFile downloadFile =
                    certificatePageService.downloadCertificate(IssuedAttestationCertificate.class, UUID.fromString(id));
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;" + downloadFile.getFileName());
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.getOutputStream().write(downloadFile.getFileBytes());
        } catch (Exception exception) {
            log.error("An exception was thrown while attempting to download the"
                    + " specified issued certificate", exception);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Processes the request to bulk download all the {@link IssuedAttestationCertificate} objects.
     *
     * @param response the response object (needed to update the header with the file name)
     * @throws IOException when writing to response output stream
     */
    @GetMapping("/bulk-download")
    public void bulkDownloadIssuedCertificates(final HttpServletResponse response) throws IOException {
        log.info("Received request to download all issued certificates");

        final String singleFileName = "Issued_Certificate";
        final String zipFileName = "issued_certificates.zip";

        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + zipFileName);
        response.setContentType("application/zip");

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            certificatePageService.bulkDownloadCertificates(zipOut, CertificateType.ISSUED_CERTIFICATE,
                    singleFileName);
        } catch (Exception exception) {
            log.error("An exception was thrown while attempting to bulk download all the "
                    + "issued certificates", exception);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Processes the request to archive/soft delete the specified {@link IssuedAttestationCertificate} object.
     *
     * @param id                 the UUID of the {@link IssuedAttestationCertificate} object to delete
     * @param redirectAttributes RedirectAttributes used to forward data back to the original page.
     * @return redirect to the Issued Certificates page
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("/delete")
    public RedirectView deleteIssuedCertificate(@RequestParam final String id,
                                                final RedirectAttributes redirectAttributes)
            throws URISyntaxException {
        log.info("Received request to delete issued certificate with id {}", id);

        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        List<String> successMessages = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        try {
            certificatePageService.deleteCertificate(UUID.fromString(id), successMessages, errorMessages);
            messages.addSuccessMessages(successMessages);
            messages.addErrorMessages(errorMessages);
        } catch (Exception exception) {
            final String errorMessage = "An exception was thrown while attempting to delete"
                    + " the specified issued certificate";
            messages.addErrorMessage(errorMessage);
            log.error(errorMessage, exception);
        }

        model.put(MESSAGES_ATTRIBUTE, messages);
        return redirectTo(Page.ISSUED_CERTIFICATES, new NoPageParams(), model, redirectAttributes);
    }

    /**
     * Processes the request to delete multiple {@link IssuedAttestationCertificate} objects.
     *
     * @param ids                the list of UUIDs of the {@link IssuedAttestationCertificate} objects to be deleted
     * @param redirectAttributes used to pass data back to the original page after the operation
     * @return a redirect to the Issued Certificates page
     * @throws URISyntaxException if the URI is malformed
     */
    @PostMapping("/bulk-delete")
    public RedirectView bulkDeleteIssuedCertificates(@RequestParam final List<String> ids,
                                                     final RedirectAttributes redirectAttributes)
            throws URISyntaxException {
        log.info("Received request to delete multiple issued certificates");

        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        List<String> successMessages = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        try {
            certificatePageService.bulkDeleteCertificates(ids, successMessages, errorMessages);
            messages.addSuccessMessages(successMessages);
            messages.addErrorMessages(errorMessages);
        } catch (Exception exception) {
            final String errorMessage = "An exception was thrown while attempting to delete"
                    + " multiple issued certificates";
            messages.addErrorMessage(errorMessage);
            log.error(errorMessage, exception);
        }

        model.put(MESSAGES_ATTRIBUTE, messages);
        return redirectTo(Page.ISSUED_CERTIFICATES, new NoPageParams(), model, redirectAttributes);
    }

    /**
     * Helper method that retrieves a filtered and paginated list of {@link IssuedAttestationCertificate} objects
     * based on the provided search criteria.
     * <p>
     * The method allows filtering based on a global search term and column-specific search criteria,
     * and returns the result in a paginated format.
     *
     * <p>
     * The method handles four cases:
     * <ol>
     *     <li>If no global search term and no column-specific search criteria are provided,
     *         all {@link IssuedAttestationCertificate} objects are returned.</li>
     *     <li>If both a global search term and column-specific search criteria are provided,
     *         {@link IssuedAttestationCertificate} objects are filtered based on both criteria.</li>
     *     <li>If only column-specific search criteria are provided, {@link IssuedAttestationCertificate} objects
     *         are filtered according to the column-specific criteria.</li>
     *     <li>If only a global search term is provided, {@link IssuedAttestationCertificate} objects
     *         are filtered according to the global search term.</li>
     * </ol>
     * </p>
     *
     * @param globalSearchTerm          A global search term that will be used to filter the
     *                                  {@link IssuedAttestationCertificate} objects  by the searchable fields.
     * @param columnsWithSearchCriteria A set of columns with specific search criteria entered by the user.
     * @param searchableColumnNames     A set of searchable column names that are  for the global search term.
     * @param pageable                  pageable
     * @return A {@link FilteredRecordsList} containing the filtered and paginated list of
     * {@link IssuedAttestationCertificate} objects, along with the total number of records and the number of records
     * matching the filter criteria.
     */
    private FilteredRecordsList<IssuedAttestationCertificate> getFilteredIssuedCertificateList(
            final String globalSearchTerm,
            final Set<DataTablesColumn> columnsWithSearchCriteria,
            final Set<String> searchableColumnNames,
            final Pageable pageable) {
        org.springframework.data.domain.Page<IssuedAttestationCertificate> pagedResult;

        // if no value has been entered in the global search textbox and in the column search dropdown
        if (StringUtils.isBlank(globalSearchTerm) && columnsWithSearchCriteria.isEmpty()) {
            pagedResult =
                    issuedCertificatePageService.findIssuedCertificatesByArchiveFlag(false, pageable);
        } else if (!StringUtils.isBlank(globalSearchTerm) && !columnsWithSearchCriteria.isEmpty()) {
            // if a value has been entered in both the global search textbox and in the column search dropdown
            pagedResult =
                    certificatePageService.findCertificatesByGlobalAndColumnSpecificSearchTerm(
                            IssuedAttestationCertificate.class,
                            searchableColumnNames,
                            globalSearchTerm,
                            columnsWithSearchCriteria,
                            false,
                            pageable);
        } else if (!columnsWithSearchCriteria.isEmpty()) {
            // if a value has been entered ONLY in the column search dropdown
            pagedResult =
                    certificatePageService.findCertificatesByColumnSpecificSearchTermAndArchiveFlag(
                            IssuedAttestationCertificate.class,
                            columnsWithSearchCriteria,
                            false,
                            pageable);
        } else {
            // if a value has been entered ONLY in the global search textbox
            pagedResult = certificatePageService.findCertificatesByGlobalSearchTermAndArchiveFlag(
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
                issuedCertificatePageService.findIssuedCertificateRepoCount());

        return issuedCertificateFilteredRecordsList;
    }
}
