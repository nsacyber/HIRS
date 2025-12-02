package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.DBManagerException;
import hirs.attestationca.persist.FilteredRecordsList;
import hirs.attestationca.persist.entity.userdefined.rim.ReferenceDigestValue;
import hirs.attestationca.persist.entity.userdefined.rim.SupportReferenceManifest;
import hirs.attestationca.persist.service.ReferenceDigestValuePageService;
import hirs.attestationca.persist.service.util.DataTablesColumn;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.params.NoPageParams;
import hirs.attestationca.portal.page.utils.ControllerPagesUtils;
import jakarta.validation.Valid;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
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

        final String globalSearchTerm = dataTableInput.getSearch().getValue();
        final Set<DataTablesColumn> columnsWithSearchCriteria =
                ControllerPagesUtils.findColumnsWithSearchCriteriaForColumnSpecificSearch(
                        dataTableInput.getColumns());

        final int currentPage = dataTableInput.getStart() / dataTableInput.getLength();
        Pageable pageable = PageRequest.of(currentPage, dataTableInput.getLength());

        FilteredRecordsList<ReferenceDigestValue> rdvFilteredRecordsList = new FilteredRecordsList<>();
        org.springframework.data.domain.Page<ReferenceDigestValue> pagedResult;

        // if the user has not entered any value in either the global search box or the column search box
        if (StringUtils.isBlank(globalSearchTerm) && columnsWithSearchCriteria.isEmpty()) {
            pagedResult = this.referenceDigestValuePageService.findAllReferenceDigestValues(pageable);
        }
//        // if the user has entered a value in both the global search box and column search box
//        else if (!StringUtils.isBlank(globalSearchTerm) && !columnsWithSearchCriteria.isEmpty()) {
//
//        }
        // if the search term applied to the individual columns is not empty
        else if (!columnsWithSearchCriteria.isEmpty()) {
            pagedResult =
                    this.referenceDigestValuePageService.findReferenceDigestValuesByColumnSpecificSearchTerm(
                            columnsWithSearchCriteria, pageable);
        } else {
            final Set<String> searchableColumnNames =
                    ControllerPagesUtils.findSearchableColumnNamesForGlobalSearch(ReferenceDigestValue.class,
                            dataTableInput.getColumns());

            pagedResult = this.referenceDigestValuePageService.findReferenceDigestValuesByGlobalSearchTerm(
                    searchableColumnNames,
                    globalSearchTerm, pageable);
        }

        if (pagedResult.hasContent()) {
            rdvFilteredRecordsList.addAll(pagedResult.getContent());
        }

        rdvFilteredRecordsList.setRecordsFiltered(pagedResult.getTotalElements());
        rdvFilteredRecordsList.setRecordsTotal(
                this.referenceDigestValuePageService.findReferenceDigestValueRepositoryCount());

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

        log.info("Returning the size of the list of reference digest values: "
                + "{}", rdvFilteredRecordsList.getRecordsFiltered());
        return new DataTableResponse<>(rdvFilteredRecordsList, dataTableInput);
    }
}
