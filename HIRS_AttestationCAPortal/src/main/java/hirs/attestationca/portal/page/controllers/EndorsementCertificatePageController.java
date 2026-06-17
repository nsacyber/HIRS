package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.entity.userdefined.DataTablesColumn;
import hirs.attestationca.persist.entity.userdefined.DownloadFile;
import hirs.attestationca.persist.entity.userdefined.FilteredRecordsList;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.enums.CertificateType;
import hirs.attestationca.persist.service.CertificatePageService;
import hirs.attestationca.persist.service.EndorsementCertificatePageService;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.datatables.Order;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.persist.dto.PageMessages;
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
 * Controller for the Endorsement Key Certificates page.
 */
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/certificate-request/endorsement-key-certificates")
@Log4j2
public class EndorsementCertificatePageController extends PageController<NoPageParams> {
    private final EndorsementCertificatePageService endorsementCertificatePageService;
    private final CertificatePageService certificatePageService;

    /**
     * Constructor for the Endorsement Certificate Page Controller.
     *
     * @param endorsementCertificatePageService endorsement certificate page service
     * @param certificatePageService            certificate page service
     */
    @Autowired
    public EndorsementCertificatePageController(
            final EndorsementCertificatePageService endorsementCertificatePageService,
            final CertificatePageService certificatePageService) {
        super(Page.ENDORSEMENT_KEY_CERTIFICATES);
        this.endorsementCertificatePageService = endorsementCertificatePageService;
        this.certificatePageService = certificatePageService;
    }

    /**
     * Returns the path for the view and the data model for the Endorsement Key Certificates page.
     *
     * @param params The object to map url parameters into.
     * @param model  The data model for the request. Can contain data from redirect.
     * @return the path for the view and data model for the Endorsement Key Certificates page.
     */
    @RequestMapping
    public ModelAndView initPage(final NoPageParams params, final Model model) {
        return getBaseModelAndView(Page.ENDORSEMENT_KEY_CERTIFICATES);
    }

    /**
     * Processes the request to retrieve a list of {@link EndorsementCredential} objects for display on the
     * Endorsement Certificates page.
     *
     * @param dataTableInput data table input received from the front-end
     * @return data table of {@link EndorsementCredential} objects
     */
    @ResponseBody
    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTableResponse<EndorsementCredential> getEndorsementCertificatesTableData(
            final DataTableInput dataTableInput) {
        log.info("Received request to display list of endorsement certificates");
        log.debug("Request received a datatable input object for the endorsement "
                + "certificates page: {}", dataTableInput);

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
                ControllerPagesUtils.findSearchableColumnNamesForGlobalSearch(EndorsementCredential.class,
                        dataTableInput.getColumns());

        Pageable pageable = ControllerPagesUtils.createPageableObject(
                dataTableInput.getStart(),
                dataTableInput.getLength(),
                orderColumn);

        FilteredRecordsList<EndorsementCredential> ekFilteredRecordsList =
                getFilteredEndorsementCertificateList(
                        globalSearchTerm,
                        columnsWithSearchCriteria,
                        searchableColumnNames,
                        pageable);

        log.info("Returning the size of the filtered list of endorsement certificates: {}",
                ekFilteredRecordsList.getRecordsFiltered());
        return new DataTableResponse<>(ekFilteredRecordsList, dataTableInput);
    }

    /**
     * Processes the request to download the specified {@link EndorsementCredential} object.
     *
     * @param id       the UUID of the specified {@link EndorsementCredential} object to download
     * @param response the response object (needed to update the header with the file name)
     * @throws IOException when writing to response output stream
     */
    @GetMapping("/download")
    public void downloadEndorsementCertificate(@RequestParam final String id, final HttpServletResponse response)
            throws IOException {
        log.info("Received request to download endorsement certificate id {}", id);

        try {
            final DownloadFile downloadFile = certificatePageService.downloadCertificate(EndorsementCredential.class,
                    UUID.fromString(id));
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;" + downloadFile.getFileName());
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.getOutputStream().write(downloadFile.getFileBytes());
        } catch (Exception exception) {
            log.error("An exception was thrown while attempting to download the"
                    + " specified endorsement certificate", exception);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Processes the request to bulk download all the {@link EndorsementCredential} objects.
     *
     * @param response the response object (needed to update the header with the file name)
     * @throws IOException when writing to response output stream
     */
    @GetMapping("/bulk-download")
    public void bulkDownloadEndorsementCertificates(final HttpServletResponse response) throws IOException {
        log.info("Received request to download all endorsement certificates");

        final String zipFileName = "endorsement_certificates.zip";
        final String singleFileName = "Endorsement_Certificates";

        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + zipFileName);
        response.setContentType("application/zip");

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            certificatePageService.bulkDownloadCertificates(zipOut, CertificateType.ENDORSEMENT_CERTIFICATE,
                    singleFileName);
        } catch (Exception exception) {
            log.error("An exception was thrown while attempting to bulk download all the "
                    + "endorsement certificates", exception);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Processes the request to upload one or more {@link EndorsementCredential} objects to the ACA.
     *
     * @param files              the files to process
     * @param redirectAttributes RedirectAttributes used to forward data back to the original page.
     * @return redirect to the Endorsement Certificates page
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("/upload")
    protected RedirectView uploadEndorsementCertificate(@RequestParam("file") final MultipartFile[] files,
                                                        final RedirectAttributes redirectAttributes)
            throws URISyntaxException {
        log.info("Received request to upload one or more endorsement certificates");

        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        for (MultipartFile file : files) {
            List<String> errorMessages = new ArrayList<>();
            List<String> successMessages = new ArrayList<>();

            EndorsementCredential parsedEndorsementCertificate =
                    endorsementCertificatePageService.parseEndorsementCertificate(file, errorMessages);

            if (parsedEndorsementCertificate != null) {
                certificatePageService.storeCertificate(CertificateType.ENDORSEMENT_CERTIFICATE,
                        file.getOriginalFilename(),
                        successMessages, errorMessages, parsedEndorsementCertificate);
            }

            messages.addSuccessMessages(successMessages);
            messages.addErrorMessages(errorMessages);
        }

        model.put(MESSAGES_ATTRIBUTE, messages);
        return redirectTo(Page.ENDORSEMENT_KEY_CERTIFICATES, new NoPageParams(), model, redirectAttributes);
    }

    /**
     * Processes the request to archive/soft delete the specified {@link EndorsementCredential} object.
     *
     * @param id                 the UUID of the specified {@link EndorsementCredential} object to delete
     * @param redirectAttributes RedirectAttributes used to forward data back to the original page.
     * @return redirect to the Endorsement Certificates page
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("/delete")
    public RedirectView deleteEndorsementCertificate(@RequestParam final String id,
                                                     final RedirectAttributes redirectAttributes)
            throws URISyntaxException {
        log.info("Received request to delete endorsement certificate id {}", id);

        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        List<String> successMessages = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        try {
            certificatePageService.deleteCertificate(UUID.fromString(id), successMessages, errorMessages);

            messages.addSuccessMessages(successMessages);
            messages.addErrorMessages(errorMessages);
        } catch (Exception exception) {
            final String errorMessage = "An exception was thrown while attempting to delete the specified"
                    + " endorsement certificate";
            messages.addErrorMessage(errorMessage);
            log.error(errorMessage, exception);
        }

        model.put(MESSAGES_ATTRIBUTE, messages);
        return redirectTo(Page.ENDORSEMENT_KEY_CERTIFICATES, new NoPageParams(), model, redirectAttributes);
    }

    /**
     * Processes the request to delete multiple {@link EndorsementCredential} objects.
     *
     * @param ids                the list of UUIDs of the {@link EndorsementCredential} objects to be deleted
     * @param redirectAttributes used to pass data back to the original page after the operation
     * @return a redirect to the Endorsement Certificates page
     * @throws URISyntaxException if the URI is malformed
     */
    @PostMapping("/bulk-delete")
    public RedirectView bulkDeleteEndorsementCertificates(@RequestParam final List<String> ids,
                                                          final RedirectAttributes redirectAttributes)
            throws URISyntaxException {
        log.info("Received request to delete multiple endorsement certificates");

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
                    + " multiple endorsement certificates";
            messages.addErrorMessage(errorMessage);
            log.error(errorMessage, exception);
        }

        model.put(MESSAGES_ATTRIBUTE, messages);
        return redirectTo(Page.ENDORSEMENT_KEY_CERTIFICATES, new NoPageParams(), model, redirectAttributes);
    }

    /**
     * Helper method that retrieves a filtered and paginated list of {@link EndorsementCredential} objects based on the
     * provided search criteria.
     * <p>
     * The method allows filtering based on a global search term and column-specific search criteria,
     * and returns the result in a paginated format.
     * <p>
     * The method handles four cases:
     * <ol>
     *     <li>If no global search term and no column-specific search criteria are provided,
     *         all {@link EndorsementCredential} objects are returned.</li>
     *     <li>If both a global search term and column-specific search criteria are provided,
     *         {@link EndorsementCredential} objects are filtered based on both criteria.</li>
     *     <li>If only column-specific search criteria are provided, {@link EndorsementCredential} objects
     *         are filtered according to the column-specific criteria.</li>
     *     <li>If only a global search term is provided, {@link EndorsementCredential} objects
     *         are filtered according to the global search term.</li>
     * </ol>
     * </p>
     *
     * @param globalSearchTerm          A global search term that will be used to filter the
     *                                  {@link EndorsementCredential} objects by the searchable fields.
     * @param columnsWithSearchCriteria A set of columns with specific search criteria entered by the user.
     * @param searchableColumnNames     A set of searchable column names that are  for the global search term.
     * @param pageable                  pageable
     * @return A {@link FilteredRecordsList} containing the filtered and paginated list of
     * {@link EndorsementCredential} objects, along with the total number of records and the number of records
     * matching the filter criteria.
     */
    private FilteredRecordsList<EndorsementCredential> getFilteredEndorsementCertificateList(
            final String globalSearchTerm,
            final Set<DataTablesColumn> columnsWithSearchCriteria,
            final Set<String> searchableColumnNames,
            final Pageable pageable) {
        org.springframework.data.domain.Page<EndorsementCredential> pagedResult;

        // if no value has been entered in the global search textbox and in the column search dropdown
        if (StringUtils.isBlank(globalSearchTerm) && columnsWithSearchCriteria.isEmpty()) {
            pagedResult =
                    endorsementCertificatePageService.findEndorsementCertificatesByArchiveFlag(false, pageable);
        } else if (!StringUtils.isBlank(globalSearchTerm) && !columnsWithSearchCriteria.isEmpty()) {
            // if a value has been entered in both the global search textbox and in the column search dropdown
            pagedResult = certificatePageService.findCertificatesByGlobalAndColumnSpecificSearchTerm(
                    EndorsementCredential.class,
                    searchableColumnNames,
                    globalSearchTerm,
                    columnsWithSearchCriteria,
                    false,
                    pageable);
        } else if (!columnsWithSearchCriteria.isEmpty()) {
            // if a value has been entered ONLY in the column search dropdown
            pagedResult = certificatePageService.findCertificatesByColumnSpecificSearchTermAndArchiveFlag(
                    EndorsementCredential.class,
                    columnsWithSearchCriteria,
                    false,
                    pageable);
        } else {
            // if a value has been entered ONLY in the global search textbox
            pagedResult = certificatePageService.findCertificatesByGlobalSearchTermAndArchiveFlag(
                    EndorsementCredential.class,
                    searchableColumnNames,
                    globalSearchTerm,
                    false, pageable);
        }

        FilteredRecordsList<EndorsementCredential> ekFilteredRecordsList = new FilteredRecordsList<>();

        if (pagedResult.hasContent()) {
            ekFilteredRecordsList.addAll(pagedResult.getContent());
        }

        ekFilteredRecordsList.setRecordsFiltered(pagedResult.getTotalElements());
        ekFilteredRecordsList.setRecordsTotal(
                endorsementCertificatePageService.findEndorsementCertificateRepositoryCount());

        return ekFilteredRecordsList;
    }
}
