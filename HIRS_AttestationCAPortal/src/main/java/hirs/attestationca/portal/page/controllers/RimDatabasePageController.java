package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.FilteredRecordsList;
import hirs.attestationca.persist.entity.userdefined.DataTablesColumn;
import hirs.attestationca.persist.entity.userdefined.rim.ReferenceDigestValue;
import hirs.attestationca.persist.entity.userdefined.rim.SupportReferenceManifest;
import hirs.attestationca.persist.exceptions.DBManagerException;
import hirs.attestationca.persist.service.ReferenceDigestValuePageService;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.datatables.Order;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.params.NoPageParams;
import hirs.attestationca.portal.page.utils.ControllerPagesUtils;
import jakarta.validation.Valid;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.util.Set;

/**
 * Controller for the TPM Events page.
 */
@Log4j2
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/rim-database")
public class RimDatabasePageController extends PageController<NoPageParams> {
    private final ReferenceDigestValuePageService referenceDigestValuePageService;

    /**
     * Constructor for the RIM Database Page Controller.
     *
     * @param referenceDigestValuePageService reference digest value service
     */
    @Autowired
    public RimDatabasePageController(final ReferenceDigestValuePageService referenceDigestValuePageService) {
        super(Page.RIM_DATABASE);
        this.referenceDigestValuePageService = referenceDigestValuePageService;
    }

    /**
     * Returns the filePath for the view and the data model for the RIM Database page.
     *
     * @param params The object to map url parameters into.
     * @param model  The data model for the request. Can contain data from
     *               redirect.
     * @return the filePath for the view and data model for the RIM Database page.
     */
    @Override
    public ModelAndView initPage(final NoPageParams params, final Model model) {
        return getBaseModelAndView(Page.RIM_DATABASE);
    }

    /**
     * Processes the request to retrieve a list of reference digest values for display
     * on the rim database page.
     *
     * @param dataTableInput the data tables input
     * @return the data tables response, including the result set and paging
     * information
     */
    @ResponseBody
    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTableResponse<ReferenceDigestValue> getRDVTableData(
            @Valid final DataTableInput dataTableInput) {
        log.info("Received request to display list of TPM events");
        log.debug("Request received a datatable input object for the RIM database page: {}", dataTableInput);

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
                ControllerPagesUtils.findSearchableColumnNamesForGlobalSearch(ReferenceDigestValue.class,
                        dataTableInput.getColumns());

        Pageable pageable = ControllerPagesUtils.createPageableObject(
                dataTableInput.getStart(),
                dataTableInput.getLength(),
                orderColumn);

        FilteredRecordsList<ReferenceDigestValue> rdvFilteredRecordsList = getFilteredRDVList(
                globalSearchTerm,
                columnsWithSearchCriteria,
                searchableColumnNames,
                pageable);

        // might be able to get rid of this, maybe write a query that looks for not updated
        SupportReferenceManifest support;
        for (ReferenceDigestValue rdv : rdvFilteredRecordsList) {
            // We are updating the base rim ID field if necessary and
            if (rdv.getBaseRimId() == null
                    && this.referenceDigestValuePageService.doesRIMExist(rdv.getSupportRimId())) {
                support = (SupportReferenceManifest) this.referenceDigestValuePageService.findRIMById(
                        rdv.getSupportRimId());
                rdv.setBaseRimId(support.getAssociatedRim());
                try {
                    this.referenceDigestValuePageService.saveReferenceDigestValue(rdv);
                } catch (DBManagerException dbMEx) {
                    log.error("Failed to update TPM Event with Base RIM ID");
                }
            }
        }

        log.info("Returning the size of the filtered list of reference digest values: "
                + "{}", rdvFilteredRecordsList.getRecordsFiltered());
        return new DataTableResponse<>(rdvFilteredRecordsList, dataTableInput);
    }

    /**
     * Helper method that retrieves a filtered and paginated list of reference digest values based on the
     * provided search criteria.
     * The method allows filtering based on a global search term and column-specific search criteria,
     * and returns the result in a paginated format.
     *
     * <p>
     * The method handles four cases:
     * <ol>
     *     <li>If no global search term and no column-specific search criteria are provided,
     *         all reference digest values are returned.</li>
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
     * reference digest values , along with the total number of records and the number of records matching the
     * filter criteria.
     */
    private FilteredRecordsList<ReferenceDigestValue> getFilteredRDVList(
            final String globalSearchTerm,
            final Set<DataTablesColumn> columnsWithSearchCriteria,
            final Set<String> searchableColumnNames,
            final Pageable pageable) {
        org.springframework.data.domain.Page<ReferenceDigestValue> pagedResult;

        // if no value has been entered in the global search textbox and in the column search dropdown
        if (StringUtils.isBlank(globalSearchTerm) && columnsWithSearchCriteria.isEmpty()) {
            pagedResult = this.referenceDigestValuePageService.findAllReferenceDigestValues(pageable);
        } else if (!StringUtils.isBlank(globalSearchTerm) && !columnsWithSearchCriteria.isEmpty()) {
            // if a value has been entered in both the global search textbox and in the column search dropdown
            pagedResult =
                    this.referenceDigestValuePageService.findReferenceDigestValuesByGlobalAndColumnSpecificSearchTerm(
                            searchableColumnNames,
                            globalSearchTerm,
                            columnsWithSearchCriteria,
                            pageable);
        } else if (!columnsWithSearchCriteria.isEmpty()) {
            // if a value has been entered ONLY in the column search dropdown
            pagedResult =
                    this.referenceDigestValuePageService.findReferenceDigestValuesByColumnSpecificSearchTerm(
                            columnsWithSearchCriteria, pageable);
        } else {
            // if a value has been entered ONLY in the global search textbox
            pagedResult = this.referenceDigestValuePageService.findReferenceDigestValuesByGlobalSearchTerm(
                    searchableColumnNames,
                    globalSearchTerm, pageable);
        }

        FilteredRecordsList<ReferenceDigestValue> rdvFilteredRecordsList = new FilteredRecordsList<>();

        if (pagedResult.hasContent()) {
            rdvFilteredRecordsList.addAll(pagedResult.getContent());
        }

        rdvFilteredRecordsList.setRecordsFiltered(pagedResult.getTotalElements());
        rdvFilteredRecordsList.setRecordsTotal(
                this.referenceDigestValuePageService.findReferenceDigestValueRepositoryCount());

        return rdvFilteredRecordsList;
    }
}
