package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.entity.userdefined.DataTablesColumn;
import hirs.attestationca.persist.entity.userdefined.FilteredRecordsList;
import hirs.attestationca.persist.entity.userdefined.SupplyChainValidationSummary;
import hirs.attestationca.persist.service.ValidationSummaryPageService;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.datatables.Order;
import hirs.attestationca.portal.page.Page;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Controller for the Validation Summary Reports page.
 */
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/validation-reports")
@Log4j2
public class ValidationReportsPageController extends PageController<NoPageParams> {
    private final ValidationSummaryPageService validationSummaryPageService;

    /**
     * Constructor for the Validation Reports Page Controller.
     *
     * @param validationSummaryPageService the validation summary reports page service
     */
    @Autowired
    public ValidationReportsPageController(final ValidationSummaryPageService validationSummaryPageService) {
        super(Page.VALIDATION_REPORTS);
        this.validationSummaryPageService = validationSummaryPageService;
    }

    /**
     * Returns the path for the view and the data model for the Validation Reports page.
     *
     * @param params The object to map url parameters into.
     * @param model  The data model for the request. Can contain data from redirect.
     * @return the path for the view and data model validation reports page.
     */
    @Override
    @RequestMapping
    public ModelAndView initPage(final NoPageParams params, final Model model) {
        return getBaseModelAndView(Page.VALIDATION_REPORTS);
    }

    /**
     * Processes the request to retrieve a list of {@link SupplyChainValidationSummary} objects for display
     * on the Validation Reports page.
     *
     * @param dataTableInput the data table query.
     * @return the data table response containing the {@link SupplyChainValidationSummary} objects
     */
    @ResponseBody
    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTableResponse<SupplyChainValidationSummary> getValidationReportsTableData(
            final DataTableInput dataTableInput) {
        log.info("Received request to display list of validation reports");
        log.debug("Request received a datatable input object for the validation reports page: {}",
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
                        SupplyChainValidationSummary.class,
                        dataTableInput.getColumns());

        Pageable pageable = ControllerPagesUtils.createPageableObject(
                dataTableInput.getStart(),
                dataTableInput.getLength(),
                orderColumn);

        FilteredRecordsList<SupplyChainValidationSummary> reportsFilteredRecordsList =
                getFilteredValidationSummaryList(
                        globalSearchTerm,
                        columnsWithSearchCriteria,
                        searchableColumnNames,
                        pageable);

        log.info("Returning the size of the filtered list of validation reports: "
                + "{}", reportsFilteredRecordsList.getRecordsFiltered());
        return new DataTableResponse<>(reportsFilteredRecordsList, dataTableInput);
    }

    /**
     * Processes the request to download a CSV file of all the {@link SupplyChainValidationSummary} objects.
     *
     * @param response http response
     */
    @GetMapping("/download")
    public void downloadValidationReports(final HttpServletResponse response) throws IOException {
        log.info("Received request to download all Validation Summary Reports");
        final String zipFileName = "validation_report.csv";

        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + zipFileName);
        response.setContentType("text/csv");

        try (BufferedWriter bufferedWriter = new BufferedWriter(
                new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {
            validationSummaryPageService.downloadValidationReports(bufferedWriter);
            bufferedWriter.flush();
        } catch (Exception exception) {
            log.error("An exception was thrown while attempting to download a CSV file of the "
                    + "validation summary reports", exception);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Helper method that retrieves a filtered and paginated list of {@link SupplyChainValidationSummary} objects
     * based on the provided search criteria.
     * The method allows filtering based on a global search term and column-specific search criteria,
     * and returns the result in a paginated format.
     *
     * <p>
     * The method handles four cases:
     * <ol>
     *     <li>If no global search term and no column-specific search criteria are provided,
     *         all {@link SupplyChainValidationSummary} objects are returned.</li>
     *     <li>If both a global search term and column-specific search criteria are provided,
     *         {@link SupplyChainValidationSummary} objects are filtered based on both criteria.</li>
     *     <li>If only column-specific search criteria are provided, {@link SupplyChainValidationSummary} objects
     *         are filtered according to the column-specific criteria.</li>
     *     <li>If only a global search term is provided, {@link SupplyChainValidationSummary} objects
     *         are filtered according to the global search term.</li>
     * </ol>
     * </p>
     *
     * @param globalSearchTerm          A global search term that will be used to filter the
     *                                  {@link SupplyChainValidationSummary} objects by the searchable fields.
     * @param columnsWithSearchCriteria A set of columns with specific search criteria entered by the user.
     * @param searchableColumnNames     A set of searchable column names that are  for the global search term.
     * @param pageable                  pageable
     * @return A {@link FilteredRecordsList} containing the filtered and paginated list of
     * endorsement credentials, along with the total number of records and the number of records matching the
     * filter criteria.
     */
    private FilteredRecordsList<SupplyChainValidationSummary> getFilteredValidationSummaryList(
            final String globalSearchTerm,
            final Set<DataTablesColumn> columnsWithSearchCriteria,
            final Set<String> searchableColumnNames,
            final Pageable pageable) {
        org.springframework.data.domain.Page<SupplyChainValidationSummary> pagedResult;

        // if no value has been entered in the global search textbox and in the column search dropdown
        if (StringUtils.isBlank(globalSearchTerm) && columnsWithSearchCriteria.isEmpty()) {
            pagedResult = validationSummaryPageService.findValidationSummaryReportsByPageable(pageable);
        } else if (!StringUtils.isBlank(globalSearchTerm) && !columnsWithSearchCriteria.isEmpty()) {
            // if a value has been entered in both the global search textbox and in the column search dropdown
            pagedResult =
                    validationSummaryPageService.findValidationSummaryReportsByGlobalAndColumnSpecificSearchTerm(
                            searchableColumnNames,
                            globalSearchTerm,
                            columnsWithSearchCriteria,
                            false,
                            pageable);
        } else if (!columnsWithSearchCriteria.isEmpty()) {
            // if a value has been entered ONLY in the column search dropdown
            pagedResult =
                    validationSummaryPageService.findValidationSummaryReportsByColumnSpecificSearchTermAndArchiveFlag(
                            columnsWithSearchCriteria, false, pageable);
        } else {
            // if a value has been entered ONLY in the global search textbox
            pagedResult = validationSummaryPageService.findValidationReportsByGlobalSearchTermAndArchiveFlag(
                    searchableColumnNames,
                    globalSearchTerm,
                    false,
                    pageable);
        }

        FilteredRecordsList<SupplyChainValidationSummary> reportsFilteredRecordsList =
                new FilteredRecordsList<>();

        if (pagedResult.hasContent()) {
            reportsFilteredRecordsList.addAll(pagedResult.getContent());
        }

        reportsFilteredRecordsList.setRecordsFiltered(pagedResult.getTotalElements());
        reportsFilteredRecordsList.setRecordsTotal(validationSummaryPageService.findValidationSummaryRepositoryCount());

        return reportsFilteredRecordsList;
    }
}
