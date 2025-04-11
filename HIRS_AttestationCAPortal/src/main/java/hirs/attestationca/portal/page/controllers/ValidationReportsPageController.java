package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.FilteredRecordsList;
import hirs.attestationca.persist.entity.manager.SupplyChainValidationSummaryRepository;
import hirs.attestationca.persist.entity.userdefined.SupplyChainValidationSummary;
import hirs.attestationca.persist.service.ValidationSummaryReportsService;
import hirs.attestationca.portal.datatables.Column;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.params.NoPageParams;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for the Validation Reports page.
 */
@Log4j2
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/validation-reports")
public class ValidationReportsPageController extends PageController<NoPageParams> {

    private final SupplyChainValidationSummaryRepository supplyChainValidatorSummaryRepository;
    private final ValidationSummaryReportsService validationSummaryReportsService;

    /**
     * Constructor providing the Page's display and routing specification.
     *
     * @param supplyChainValidatorSummaryRepository the manager
     * @param validationSummaryReportsService       the validation summary reports service
     */
    @Autowired
    public ValidationReportsPageController(
            final SupplyChainValidationSummaryRepository supplyChainValidatorSummaryRepository,
            final ValidationSummaryReportsService validationSummaryReportsService) {
        super(Page.VALIDATION_REPORTS);
        this.supplyChainValidatorSummaryRepository = supplyChainValidatorSummaryRepository;
        this.validationSummaryReportsService = validationSummaryReportsService;
    }

    /**
     * Returns the path for the view and the data model for the page.
     *
     * @param params The object to map url parameters into.
     * @param model  The data model for the request. Can contain data from redirect.
     * @return the path for the view and data model for the page.
     */
    @Override
    @RequestMapping
    public ModelAndView initPage(final NoPageParams params, final Model model) {
        return getBaseModelAndView();
    }

    /**
     * Processes request to retrieve the collection of supply chain summary records that will be displayed
     * on the validation reports page.
     *
     * @param input the data table query.
     * @return the data table response containing the supply chain summary records
     */
    @ResponseBody
    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTableResponse<SupplyChainValidationSummary> getValidationReportsTableData(
            final DataTableInput input) {

        log.info("Received request to display list of validation reports");
        log.debug("Request received a datatable input object for the validation reports page: {}", input);

        // attempt to get the column property based on the order index.
        String orderColumnName = input.getOrderColumnName();
        log.debug("Ordering on column: {}", orderColumnName);

        final String searchText = input.getSearch().getValue();
        final List<String> searchableColumns = findSearchableColumnsNames(input.getColumns());

        FilteredRecordsList<SupplyChainValidationSummary> records = new FilteredRecordsList<>();

        int currentPage = input.getStart() / input.getLength();

        Pageable pageable = PageRequest.of(currentPage, input.getLength(), Sort.by(orderColumnName));

        org.springframework.data.domain.Page<SupplyChainValidationSummary> pagedResult;

        if (StringUtils.isBlank(searchText)) {
            pagedResult =
                    this.supplyChainValidatorSummaryRepository.findByArchiveFlagFalse(pageable);
        } else {
            pagedResult =
                    this.validationSummaryReportsService.findValidationReportsBySearchableColumnsAndArchiveFlag(
                            searchableColumns,
                            searchText,
                            false,
                            pageable);
        }

        if (pagedResult.hasContent()) {
            records.addAll(pagedResult.getContent());
            records.setRecordsTotal(pagedResult.getContent().size());
        } else {
            records.setRecordsTotal(input.getLength());
        }

        records.setRecordsFiltered(supplyChainValidatorSummaryRepository.count());

        log.info("Returning the size of the list of validation reports: {}", records.size());
        return new DataTableResponse<>(records, input);
    }

    /**
     * Processes request to download the validation summary report.
     *
     * @param request  http request
     * @param response http response
     */
    @PostMapping("/download")
    public void downloadValidationReports(final HttpServletRequest request,
                                          final HttpServletResponse response) throws IOException {
        log.info("Received request to download validation report");

        this.validationSummaryReportsService.downloadValidationReports(request, response);
    }

    /**
     * Helper method that returns a list of column names that are searchable.
     *
     * @param columns columns
     * @return searchable column names
     */
    private List<String> findSearchableColumnsNames(final List<Column> columns) {
        // Retrieve all searchable columns and collect their names into a list of strings.
        return columns.stream().filter(Column::isSearchable).map(Column::getName)
                .collect(Collectors.toList());
    }
}
