package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.FilteredRecordsList;
import hirs.attestationca.persist.entity.userdefined.certificate.IDevIDCertificate;
import hirs.attestationca.persist.service.CertificatePageService;
import hirs.attestationca.persist.service.IDevIdCertificatePageService;
import hirs.attestationca.persist.service.util.CertificateType;
import hirs.attestationca.persist.service.util.DataTablesColumn;
import hirs.attestationca.persist.util.DownloadFile;
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
 * Controller for the IDevID Certificates page.
 */
@Log4j2
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/certificate-request/idevid-certificates")
public class IDevIdCertificatePageController extends PageController<NoPageParams> {
    private final CertificatePageService certificatePageService;
    private final IDevIdCertificatePageService iDevIdCertificatePageService;

    /**
     * Constructor for the IDevID Certificate Page Controller.
     *
     * @param certificatePageService       certificate page service
     * @param iDevIdCertificatePageService iDevId certificate page service
     */
    @Autowired
    public IDevIdCertificatePageController(final CertificatePageService certificatePageService,
                                           final IDevIdCertificatePageService iDevIdCertificatePageService) {
        super(Page.IDEVID_CERTIFICATES);
        this.certificatePageService = certificatePageService;
        this.iDevIdCertificatePageService = iDevIdCertificatePageService;
    }

    /**
     * Returns the path for the view and the data model for the IDevId Certificate page.
     *
     * @param params The object to map url parameters into.
     * @param model  The data model for the request. Can contain data from
     *               redirect.
     * @return the path for the view and data model for the IDevId Certificate page.
     */
    @RequestMapping
    public ModelAndView initPage(final NoPageParams params, final Model model) {
        return getBaseModelAndView(Page.IDEVID_CERTIFICATES);
    }

    /**
     * Processes the request to retrieve a list of idevid certificates for display on the idevid certificates page.
     *
     * @param dataTableInput data table input received from the front-end
     * @return data table of idevid certificates
     */
    @ResponseBody
    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTableResponse<IDevIDCertificate> getIDevIdCertificatesTableData(
            final DataTableInput dataTableInput) {
        log.info("Received request to display list of idevid certificates");
        log.debug("Request received a datatable input object for the idevid certificates page: {}",
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
                ControllerPagesUtils.findSearchableColumnNamesForGlobalSearch(IDevIDCertificate.class,
                        dataTableInput.getColumns());

        final int currentPage = dataTableInput.getStart() / dataTableInput.getLength();

        // If pageSize is -1 (Show All), set a very large page size
        // otherwise keep the original page size
        final int pageSize = dataTableInput.getLength() != -1 ?
                dataTableInput.getLength() : Integer.MAX_VALUE;

        Pageable pageable = PageRequest.of(currentPage, pageSize);

        FilteredRecordsList<IDevIDCertificate> idevidFilteredRecordsList =
                getFilteredIDevIdCertificateList(globalSearchTerm,
                        columnsWithSearchCriteria,
                        searchableColumnNames,
                        pageable);

        log.info("Returning the size of the list of IDevId certificates: "
                + "{}", idevidFilteredRecordsList.getRecordsFiltered());
        return new DataTableResponse<>(idevidFilteredRecordsList, dataTableInput);
    }

    /**
     * Processes the request to download the specified IDevId certificate.
     *
     * @param id       the UUID of the idevid certificate to download
     * @param response the response object (needed to update the header with the
     *                 file name)
     * @throws IOException when writing to response output stream
     */
    @GetMapping("/download")
    public void downloadIDevIdCertificate(@RequestParam final String id, final HttpServletResponse response)
            throws IOException {
        log.info("Received request to download idevid certificate id {}", id);

        try {
            final DownloadFile downloadFile =
                    this.certificatePageService.downloadCertificate(IDevIDCertificate.class,
                            UUID.fromString(id));
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;" + downloadFile.getFileName());
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.getOutputStream().write(downloadFile.getFileBytes());
        } catch (Exception exception) {
            log.error("An exception was thrown while attempting to download the specified idevid certificate",
                    exception);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Processes the request to bulk download all the IDevID Certificates.
     *
     * @param response the response object (needed to update the header with the
     *                 file name)
     * @throws IOException when writing to response output stream
     */
    @GetMapping("/bulk-download")
    public void bulkDownloadIDevIdCertificates(final HttpServletResponse response) throws IOException {
        log.info("Received request to download all idevid certificates");

        final String zipFileName = "idevid_certificates.zip";
        final String singleFileName = "IDevID_Certificates";

        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + zipFileName);
        response.setContentType("application/zip");

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            this.certificatePageService.bulkDownloadCertificates(zipOut, CertificateType.IDEVID_CERTIFICATES,
                    singleFileName);
        } catch (Exception exception) {
            log.error("An exception was thrown while attempting to bulk download all the idevid certificates",
                    exception);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Processes the request to upload one or more idevid certificates to the ACA.
     *
     * @param files the files to process
     * @param attr  the redirection attributes
     * @return the redirection view
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("/upload")
    protected RedirectView uploadIDevIdCertificate(@RequestParam("file") final MultipartFile[] files,
                                                   final RedirectAttributes attr) throws URISyntaxException {
        log.info("Received request to upload one or more idevid certificates");

        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        for (MultipartFile file : files) {
            List<String> errorMessages = new ArrayList<>();
            List<String> successMessages = new ArrayList<>();

            IDevIDCertificate parsedIDevIDCertificate =
                    this.iDevIdCertificatePageService.parseIDevIDCertificate(file, errorMessages);

            if (parsedIDevIDCertificate != null) {
                certificatePageService.storeCertificate(CertificateType.IDEVID_CERTIFICATES,
                        file.getOriginalFilename(),
                        successMessages, errorMessages, parsedIDevIDCertificate);
            }

            messages.addSuccessMessages(successMessages);
            messages.addErrorMessages(errorMessages);
        }

        model.put(MESSAGES_ATTRIBUTE, messages);
        return redirectTo(Page.IDEVID_CERTIFICATES, new NoPageParams(), model, attr);
    }

    /**
     * Processes the request to archive/soft delete the provided idevid certificate.
     *
     * @param id   the UUID of the idevid certificate to delete
     * @param attr RedirectAttributes used to forward data back to the original
     *             page.
     * @return redirect to this page
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("/delete")
    public RedirectView deleteIdevIdCertificate(@RequestParam final String id, final RedirectAttributes attr)
            throws URISyntaxException {
        log.info("Received request to delete idevid certificate id {}", id);

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
            final String errorMessage =
                    "An exception was thrown while attempting to delete the specified idevid "
                            + "certificate";
            messages.addErrorMessage(errorMessage);
            log.error(errorMessage, exception);
        }

        model.put(MESSAGES_ATTRIBUTE, messages);
        return redirectTo(Page.IDEVID_CERTIFICATES, new NoPageParams(), model, attr);
    }

    /**
     * Helper method that retrieves a filtered and paginated list of idevid certificates based on the
     * provided search criteria.
     * The method allows filtering based on a global search term and column-specific search criteria,
     * and returns the result in a paginated format.
     *
     * <p>
     * The method handles four cases:
     * <ol>
     *     <li>If no global search term and no column-specific search criteria are provided,
     *         all idevid certificates are returned.</li>
     *     <li>If both a global search term and column-specific search criteria are provided,
     *         it performs filtering on both.</li>
     *     <li>If only column-specific search criteria are provided, it filters based on the column-specific
     *         criteria.</li>
     *     <li>If only a global search term is provided, it filters based on the global search term.</li>
     * </ol>
     * </p>
     *
     * @param globalSearchTerm          A global search term that will be used to filter the idevid certificates by the
     *                                  searchable fields.
     * @param columnsWithSearchCriteria A set of columns with specific search criteria entered by the user.
     * @param searchableColumnNames     A set of searchable column names that are  for the global search term.
     * @param pageable                  pageable
     * @return A {@link FilteredRecordsList} containing the filtered and paginated list of
     * idevid certificates, along with the total number of records and the number of records matching the
     * filter criteria.
     */
    private FilteredRecordsList<IDevIDCertificate> getFilteredIDevIdCertificateList(
            final String globalSearchTerm,
            final Set<DataTablesColumn> columnsWithSearchCriteria,
            final Set<String> searchableColumnNames,
            final Pageable pageable) {
        org.springframework.data.domain.Page<IDevIDCertificate> pagedResult;

        // if no value has been entered in the global search textbox and in the column search dropdown
        if (StringUtils.isBlank(globalSearchTerm) && columnsWithSearchCriteria.isEmpty()) {
            pagedResult = this.iDevIdCertificatePageService.
                    findIDevCertificatesByArchiveFlag(false, pageable);
        } else if (!StringUtils.isBlank(globalSearchTerm) && !columnsWithSearchCriteria.isEmpty()) {
            // if a value has been entered in both the global search textbox and in the column search dropdown
            pagedResult =
                    this.certificatePageService.findCertificatesByGlobalAndColumnSpecificSearchTerm(
                            IDevIDCertificate.class,
                            searchableColumnNames,
                            globalSearchTerm,
                            columnsWithSearchCriteria,
                            false,
                            pageable);
        } else if (!columnsWithSearchCriteria.isEmpty()) {
            // if a value has been entered ONLY in the column search dropdown
            pagedResult =
                    this.certificatePageService.findCertificatesByColumnSpecificSearchTermAndArchiveFlag(
                            IDevIDCertificate.class,
                            columnsWithSearchCriteria,
                            false,
                            pageable);
        } else {
            // if a value has been entered ONLY in the global search textbox
            pagedResult = this.certificatePageService.findCertificatesByGlobalSearchTermAndArchiveFlag(
                    IDevIDCertificate.class,
                    searchableColumnNames,
                    globalSearchTerm,
                    false, pageable);
        }
        FilteredRecordsList<IDevIDCertificate> idevidFilteredRecordsList = new FilteredRecordsList<>();

        if (pagedResult.hasContent()) {
            idevidFilteredRecordsList.addAll(pagedResult.getContent());
        }

        idevidFilteredRecordsList.setRecordsFiltered(pagedResult.getTotalElements());
        idevidFilteredRecordsList.setRecordsTotal(
                this.iDevIdCertificatePageService.findIDevIdCertificateRepositoryCount());

        return idevidFilteredRecordsList;
    }
}
