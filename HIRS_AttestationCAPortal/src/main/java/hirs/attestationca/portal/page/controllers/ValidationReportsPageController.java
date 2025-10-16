package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.FilteredRecordsList;
import hirs.attestationca.persist.entity.userdefined.SupplyChainValidationSummary;
import hirs.attestationca.persist.service.ValidationSummaryPageService;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.params.NoPageParams;
import hirs.attestationca.portal.page.utils.ControllerPagesUtils;
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
     * @param input the data table query.
     * @return the data table response containing the supply chain summary records
     */
    @ResponseBody
    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTableResponse<SupplyChainValidationSummary> getValidationReportsTableData(final DataTableInput input) {
        log.info("Received request to display list of validation reports");
        log.debug("Request received a datatable input object for the validation reports page: {}", input);

        final String orderColumnName = input.getOrderColumnName();
        log.debug("Ordering on column: {}", orderColumnName);

        final String searchTerm = input.getSearch().getValue();
        final Set<String> searchableColumns =
                ControllerPagesUtils.findSearchableColumnsNames(SupplyChainValidationSummary.class,
                        input.getColumns());

        FilteredRecordsList<SupplyChainValidationSummary> reportsFilteredRecordsList =
                new FilteredRecordsList<>();

        final int currentPage = input.getStart() / input.getLength();

        Pageable pageable = PageRequest.of(currentPage, input.getLength(), Sort.by(orderColumnName));

        org.springframework.data.domain.Page<SupplyChainValidationSummary> pagedResult;

        if (StringUtils.isBlank(searchTerm)) {
            pagedResult = this.validationSummaryPageService.findValidationSummaryReportsByPageable(pageable);
        } else {
            pagedResult = this.validationSummaryPageService
                    .findValidationReportsBySearchableColumnsAndArchiveFlag(
                            searchableColumns,
                            searchTerm,
                            false,
                            pageable);
        }

        if (pagedResult.hasContent()) {
            reportsFilteredRecordsList.addAll(pagedResult.getContent());
        }

        reportsFilteredRecordsList.setRecordsFiltered(pagedResult.getTotalElements());
        reportsFilteredRecordsList.setRecordsTotal(
                this.validationSummaryPageService.findValidationSummaryRepositoryCount());

        log.info("Returning the size of the list of validation reports: "
                + "{}", reportsFilteredRecordsList.getRecordsFiltered());
        return new DataTableResponse<>(reportsFilteredRecordsList, input);
    }

    /**
     * Processes the request to download the selected validation summary report.
     *
     * @param request  http request
     * @param response http response
     */
    @PostMapping("/download")
    public void downloadValidationReports(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
        log.info("Received request to download validation summary reports");
        this.validationSummaryPageService.downloadValidationReports(request, response);
    }
}
