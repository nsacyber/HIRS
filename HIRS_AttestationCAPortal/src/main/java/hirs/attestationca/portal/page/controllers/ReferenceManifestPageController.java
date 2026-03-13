package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.entity.userdefined.DataTablesColumn;
import hirs.attestationca.persist.entity.userdefined.DownloadFile;
import hirs.attestationca.persist.entity.userdefined.FilteredRecordsList;
import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.rim.BaseReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.rim.SupportReferenceManifest;
import hirs.attestationca.persist.service.ReferenceManifestPageService;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.datatables.Order;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageMessages;
import hirs.attestationca.portal.page.params.NoPageParams;
import hirs.attestationca.portal.page.utils.ControllerPagesUtils;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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
import java.util.regex.Pattern;
import java.util.zip.ZipOutputStream;

/**
 * Controller for the Reference Manifest Page.
 */
@Log4j2
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/reference-manifests")
public class ReferenceManifestPageController extends PageController<NoPageParams> {

    private static final String BASE_RIM_FILE_PATTERN = "(\\S+(\\.(?i)swidtag)$)";
    private static final String SUPPORT_RIM_FILE_PATTERN = "(\\S+(\\.(?i)(rimpcr|rimel|bin|log))$)";

    private final ReferenceManifestPageService referenceManifestPageService;

    /**
     * Constructor for the Reference Manifest Page Controller.
     *
     * @param referenceManifestPageService reference manifest page service
     */
    @Autowired
    public ReferenceManifestPageController(final ReferenceManifestPageService referenceManifestPageService) {
        super(Page.REFERENCE_MANIFESTS);
        this.referenceManifestPageService = referenceManifestPageService;
    }

    /**
     * Returns the filePath for the view and the data model for the Reference Manifest page.
     *
     * @param params The object to map url parameters into.
     * @param model  The data model for the request. Can contain data from
     *               redirect.
     * @return the filePath for the view and data model for the Reference Manifest page.
     */
    @Override
    public ModelAndView initPage(final NoPageParams params, final Model model) {
        return getBaseModelAndView(Page.REFERENCE_MANIFESTS);
    }

    /**
     * Processes the request to retrieve a list of RIMs for display on the RIM page.
     *
     * @param dataTableInput data table input
     * @return data table of RIMs
     */
    @ResponseBody
    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTableResponse<ReferenceManifest> getRIMTableData(@Valid final DataTableInput dataTableInput) {
        log.info("Received request to display list of reference manifests");
        log.debug("Request received a datatable input object for the reference manifest page "
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
                ControllerPagesUtils.findSearchableColumnNamesForGlobalSearch(ReferenceManifest.class,
                        dataTableInput.getColumns());

        Pageable pageable = ControllerPagesUtils.createPageableObject(
                dataTableInput.getStart(),
                dataTableInput.getLength(),
                orderColumn);

        FilteredRecordsList<ReferenceManifest> rimFilteredRecordsList =
                getFilteredReferenceManifestList(
                        globalSearchTerm,
                        columnsWithSearchCriteria,
                        searchableColumnNames,
                        pageable);

        log.info("Returning the size of the filtered list of reference manifests: {}",
                rimFilteredRecordsList.getRecordsFiltered());
        return new DataTableResponse<>(rimFilteredRecordsList, dataTableInput);
    }

    /**
     * Processes the request to upload one or more reference manifest(s) to the ACA.
     *
     * @param files              the files to process
     * @param redirectAttributes RedirectAttributes used to forward data back to the original page.
     * @return the redirection view
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("/upload")
    protected RedirectView uploadRIMs(@RequestParam("file") final MultipartFile[] files,
                                      final RedirectAttributes redirectAttributes)
            throws URISyntaxException {
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();
        List<String> successMessages = new ArrayList<>();

        final Pattern baseRimPattern = Pattern.compile(BASE_RIM_FILE_PATTERN);
        final Pattern supportRimPattern = Pattern.compile(SUPPORT_RIM_FILE_PATTERN);

        List<BaseReferenceManifest> baseRims = new ArrayList<>();
        List<SupportReferenceManifest> supportRims = new ArrayList<>();

        log.info("Processing {} uploaded files", files.length);

        for (MultipartFile file : files) {
            String fileName = file.getOriginalFilename();
            List<String> errorMessages = new ArrayList<>();

            if (fileName == null) {
                log.warn("File with empty or null name skipped");
                continue;  // Skip processing this file
            }

            final boolean isBaseRim = baseRimPattern.matcher(fileName).matches();
            final boolean isSupportRim = !isBaseRim && supportRimPattern.matcher(fileName).matches();

            if (isBaseRim) {
                final BaseReferenceManifest baseReferenceManifest =
                        this.referenceManifestPageService.parseBaseRIM(errorMessages, file);
                baseRims.add(baseReferenceManifest);
                messages.addErrorMessages(errorMessages);
            } else if (isSupportRim) {
                final SupportReferenceManifest supportReferenceManifest =
                        this.referenceManifestPageService.parseSupportRIM(errorMessages, file);
                supportRims.add(supportReferenceManifest);
                messages.addErrorMessages(errorMessages);
            } else {
                String errorString = "The file extension of " + fileName + " was not recognized."
                        + " Base RIMs support the extension \".swidtag\", and support RIMs support "
                        + "\".rimpcr\", \".rimel\", \".bin\", and \".log\". "
                        + "Please verify your upload and retry.";
                log.error("File extension in {} not recognized as base or support RIM.", fileName);
                messages.addErrorMessage(errorString);
            }
        }

        this.referenceManifestPageService.storeRIMS(successMessages, baseRims, supportRims);

        messages.addSuccessMessages(successMessages);

        model.put(MESSAGES_ATTRIBUTE, messages);
        return redirectTo(Page.REFERENCE_MANIFESTS, new NoPageParams(), model, redirectAttributes);
    }

    /**
     * Processes the request to download the RIM .
     *
     * @param id       the UUID of the rim to download
     * @param response the response object (needed to update the header with the
     *                 file name)
     * @throws java.io.IOException when writing to response output stream
     */
    @GetMapping("/download")
    public void downloadRIM(@RequestParam final String id, final HttpServletResponse response)
            throws IOException {
        log.info("Received request to download RIM id {}", id);

        try {
            final DownloadFile downloadFile =
                    this.referenceManifestPageService.downloadRIM(UUID.fromString(id));
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment;" + "filename=\"" + downloadFile.getFileName());
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.getOutputStream().write(downloadFile.getFileBytes());

        } catch (Exception exception) {
            log.error("An exception was thrown while attempting to download the "
                    + " specified RIM", exception);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Processes the request to bulk download RIMs .
     *
     * @param response the response object (needed to update the header with the
     *                 file name)
     * @throws IOException when writing to response output stream
     */
    @GetMapping("/bulk-download")
    public void bulkDownloadRIMs(final HttpServletResponse response) throws IOException {
        log.info("Handling request to download all Reference Integrity Manifests");
        final String zipFileName = "rims.zip";

        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + zipFileName);
        response.setContentType("application/zip");

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            this.referenceManifestPageService.bulkDownloadRIMS(zipOut);
        } catch (Exception exception) {
            log.error("An exception was thrown while attempting to bulk download all the "
                    + "reference integrity manifests", exception);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Processes the request to archive/soft delete the provided Reference Integrity Manifest.
     *
     * @param id                 the UUID of the rim to delete
     * @param redirectAttributes RedirectAttributes used to forward data back to the original
     *                           page.
     * @return redirect to this page
     * @throws URISyntaxException if malformed URI
     */
    @PostMapping("/delete")
    public RedirectView deleteRIM(@RequestParam final String id, final RedirectAttributes redirectAttributes)
            throws URISyntaxException {
        log.info("Received request to delete RIM id {}", id);

        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        List<String> successMessages = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        try {
            this.referenceManifestPageService.deleteRIM(UUID.fromString(id), successMessages, errorMessages);
            messages.addSuccessMessages(successMessages);
            messages.addErrorMessages(errorMessages);
        } catch (Exception exception) {
            final String errorMessage = "An exception was thrown while attempting to delete the"
                    + " specified RIM";
            messages.addErrorMessage(errorMessage);
            log.error(errorMessage, exception);
        }

        model.put(MESSAGES_ATTRIBUTE, messages);
        return redirectTo(Page.REFERENCE_MANIFESTS, new NoPageParams(), model, redirectAttributes);
    }

    /**
     * Processes the request to delete multiple RIMs.
     *
     * @param ids                the list of UUIDs of the RIMs to be deleted
     * @param redirectAttributes used to pass data back to the original page after the operation
     * @return a redirect to the trust chain certificate page
     * @throws URISyntaxException if the URI is malformed
     */
    @PostMapping("/bulk-delete")
    public RedirectView bulkDeleteRIMs(@RequestParam final List<String> ids,
                                       final RedirectAttributes redirectAttributes)
            throws URISyntaxException {
        log.info("Received request to delete multiple RIMs");

        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        List<String> successMessages = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        try {
            this.referenceManifestPageService.bulkDeleteRIMs(ids, successMessages,
                    errorMessages);
            messages.addSuccessMessages(successMessages);
            messages.addErrorMessages(errorMessages);
        } catch (Exception exception) {
            final String errorMessage = "An exception was thrown while attempting to delete"
                    + " multiple RIMs";
            messages.addErrorMessage(errorMessage);
            log.error(errorMessage, exception);
        }

        model.put(MESSAGES_ATTRIBUTE, messages);
        return redirectTo(Page.REFERENCE_MANIFESTS, new NoPageParams(), model, redirectAttributes);
    }

    /**
     * Helper method that retrieves a filtered and paginated list of reference manifests based on the
     * provided search criteria.
     * The method allows filtering based on a global search term and column-specific search criteria,
     * and returns the result in a paginated format.
     *
     * <p>
     * The method handles four cases:
     * <ol>
     *     <li>If no global search term and no column-specific search criteria are provided,
     *         all reference manifests are returned.</li>
     *     <li>If both a global search term and column-specific search criteria are provided,
     *         it performs filtering on both.</li>
     *     <li>If only column-specific search criteria are provided, it filters based on the column-specific
     *         criteria.</li>
     *     <li>If only a global search term is provided, it filters based on the global search term.</li>
     * </ol>
     * </p>
     *
     * @param globalSearchTerm          A global search term that will be used to filter the endorsement
     *                                  credentials by the searchable fields.
     * @param columnsWithSearchCriteria A set of columns with specific search criteria entered by the user.
     * @param searchableColumnNames     A set of searchable column names that are  for the global search term.
     * @param pageable                  pageable
     * @return A {@link FilteredRecordsList} containing the filtered and paginated list of
     * reference manifests, along with the total number of records and the number of records matching the
     * filter criteria.
     */
    private FilteredRecordsList<ReferenceManifest> getFilteredReferenceManifestList(
            final String globalSearchTerm,
            final Set<DataTablesColumn> columnsWithSearchCriteria,
            final Set<String> searchableColumnNames,
            final Pageable pageable) {
        org.springframework.data.domain.Page<ReferenceManifest> pagedResult;

        // if no value has been entered in the global search textbox and in the column search dropdown
        if (StringUtils.isBlank(globalSearchTerm) && columnsWithSearchCriteria.isEmpty()) {
            pagedResult = this.referenceManifestPageService.findAllBaseAndSupportRIMSByPageable(pageable);
        } else if (!StringUtils.isBlank(globalSearchTerm) && !columnsWithSearchCriteria.isEmpty()) {
            // if a value has been entered in both the global search textbox and in the column search dropdown
            pagedResult =
                    this.referenceManifestPageService.findRIMSByGlobalAndColumnSpecificSearchTerm(
                            searchableColumnNames,
                            globalSearchTerm,
                            columnsWithSearchCriteria,
                            false,
                            pageable);
        } else if (!columnsWithSearchCriteria.isEmpty()) {
            // if a value has been entered ONLY in the column search dropdown
            pagedResult =
                    this.referenceManifestPageService.
                            findRIMSByColumnSpecificSearchTermAndArchiveFlag(
                                    columnsWithSearchCriteria,
                                    false,
                                    pageable);
        } else {
            // if a value has been entered ONLY in the global search textbox
            pagedResult = this.referenceManifestPageService.
                    findRIMSByGlobalSearchTermAndArchiveFlag(searchableColumnNames,
                            globalSearchTerm,
                            false,
                            pageable);
        }

        FilteredRecordsList<ReferenceManifest> rimFilteredRecordsList = new FilteredRecordsList<>();

        if (pagedResult.hasContent()) {
            rimFilteredRecordsList.addAll(pagedResult.getContent());
        }

        rimFilteredRecordsList.setRecordsFiltered(pagedResult.getTotalElements());
        rimFilteredRecordsList.setRecordsTotal(this.referenceManifestPageService.findRIMRepositoryCount());

        return rimFilteredRecordsList;
    }
}
