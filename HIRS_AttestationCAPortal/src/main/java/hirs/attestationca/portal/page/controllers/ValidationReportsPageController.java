package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.FilteredRecordsList;
import hirs.attestationca.persist.entity.userdefined.SupplyChainValidationSummary;
import hirs.attestationca.persist.service.ValidationSummaryPageService;
import hirs.attestationca.persist.service.util.DataTablesColumn;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.datatables.Order;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.params.NoPageParams;
import hirs.attestationca.portal.page.utils.ControllerPagesUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.Set;

/**
 * Controller for the Validation Summary Reports page.
 */
@Log4j2
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/validation-reports")
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
     * Returns the path for the view and the data model for the validation reports page.
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
     * Processes the request to retrieve a list of supply chain summary records for display
     * on the validation reports page.
     *
     * @param dataTableInput the data table query.
     * @return the data table response containing the supply chain summary records
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

        Pageable pageable = ControllerPagesUtils.getPageable(
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
     * Processes the request to download the selected validation summary report.
     *
     * @param request  http request
     * @param response http response
     */
    @PostMapping("/download")
    public void downloadValidationReports(final HttpServletRequest request,
                                          final HttpServletResponse response)
            throws IOException {
        log.info("Received request to download validation summary reports");
        this.validationSummaryPageService.downloadValidationReports(request, response);
    }

    /**
     * Helper method that retrieves a filtered and paginated list of endorsement credentials based on the
     * provided search criteria.
     * The method allows filtering based on a global search term and column-specific search criteria,
     * and returns the result in a paginated format.
     *
     * <p>
     * The method handles four cases:
     * <ol>
     *     <li>If no global search term and no column-specific search criteria are provided,
     *         all endorsement credentials are returned.</li>
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
            pagedResult = this.validationSummaryPageService.findValidationSummaryReportsByPageable(pageable);
        } else if (!StringUtils.isBlank(globalSearchTerm) && !columnsWithSearchCriteria.isEmpty()) {
            // if a value has been entered in both the global search textbox and in the column search dropdown
            pagedResult =
                    this.validationSummaryPageService.findValidationSummaryReportsByGlobalAndColumnSpecificSearchTerm(
                            searchableColumnNames,
                            globalSearchTerm,
                            columnsWithSearchCriteria,
                            false,
                            pageable);
        } else if (!columnsWithSearchCriteria.isEmpty()) {
            // if a value has been entered ONLY in the column search dropdown
            pagedResult = this.validationSummaryPageService
                    .findValidationSummaryReportsByColumnSpecificSearchTermAndArchiveFlag(
                            columnsWithSearchCriteria, false, pageable);
        } else {
            // if a value has been entered ONLY in the global search textbox
            pagedResult = this.validationSummaryPageService
                    .findValidationReportsByGlobalSearchTermAndArchiveFlag(
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
        reportsFilteredRecordsList.setRecordsTotal(
                this.validationSummaryPageService.findValidationSummaryRepositoryCount());

        return reportsFilteredRecordsList;
    }
}
