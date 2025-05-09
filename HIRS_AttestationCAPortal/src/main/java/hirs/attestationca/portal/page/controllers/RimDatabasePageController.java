package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.DBManagerException;
import hirs.attestationca.persist.FilteredRecordsList;
import hirs.attestationca.persist.entity.userdefined.rim.ReferenceDigestValue;
import hirs.attestationca.persist.entity.userdefined.rim.SupportReferenceManifest;
import hirs.attestationca.persist.service.ReferenceDigestValueService;
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
import org.springframework.data.domain.Sort;
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
    private final ReferenceDigestValueService referenceDigestValueService;

    /**
     * Constructor providing the Page's display and routing specification.
     *
     * @param referenceDigestValueService reference digest value service
     */
    @Autowired
    public RimDatabasePageController(
            final ReferenceDigestValueService referenceDigestValueService) {
        super(Page.RIM_DATABASE);
        this.referenceDigestValueService = referenceDigestValueService;
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
    public ModelAndView initPage(final NoPageParams params,
                                 final Model model) {
        return getBaseModelAndView(Page.RIM_DATABASE);
    }

    /**
     * Processes the request to retrieve a list of reference digest values for display
     * on the rim database page.
     *
     * @param input the data tables input
     * @return the data tables response, including the result set and paging
     * information
     */
    @ResponseBody
    @GetMapping(value = "/list",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTableResponse<ReferenceDigestValue> getRDVTableData(
            @Valid final DataTableInput input) {
        log.info("Received request to display list of TPM events");
        log.debug("Request received a datatable input object for the RIM database page: {}", input);

        String orderColumnName = input.getOrderColumnName();
        log.debug("Ordering on column: {}", orderColumnName);

        final String searchTerm = input.getSearch().getValue();
        final Set<String> searchableColumns =
                ControllerPagesUtils.findSearchableColumnsNames(ReferenceDigestValue.class,
                        input.getColumns());

        FilteredRecordsList<ReferenceDigestValue> referenceDigestValues = new FilteredRecordsList<>();

        final int currentPage = input.getStart() / input.getLength();
        Pageable pageable = PageRequest.of(currentPage, input.getLength(), Sort.by(orderColumnName));
        org.springframework.data.domain.Page<ReferenceDigestValue> pagedResult;

        if (StringUtils.isBlank(searchTerm)) {
            pagedResult =
                    this.referenceDigestValueService.findAllReferenceDigestValues(pageable);
        } else {
            pagedResult =
                    this.referenceDigestValueService.findReferenceDigestValuesBySearchableColumns(
                            searchableColumns,
                            searchTerm, pageable);
        }

        if (pagedResult.hasContent()) {
            referenceDigestValues.addAll(pagedResult.getContent());
        }

        referenceDigestValues.setRecordsFiltered(pagedResult.getTotalElements());
        referenceDigestValues.setRecordsTotal(
                this.referenceDigestValueService.findReferenceDigestValueRepositoryCount());

        // might be able to get rid of this, maybe write a query that looks for not updated
        SupportReferenceManifest support;
        for (ReferenceDigestValue rdv : referenceDigestValues) {
            // We are updating the base rim ID field if necessary and
            if (rdv.getBaseRimId() == null &&
                    this.referenceDigestValueService.doesRIMExist(rdv.getSupportRimId())) {
                support = (SupportReferenceManifest) this.referenceDigestValueService.findRIMById(
                        rdv.getSupportRimId());
                rdv.setBaseRimId(support.getAssociatedRim());
                try {
                    referenceDigestValueService.saveReferenceDigestValue(rdv);
                } catch (DBManagerException dbMEx) {
                    log.error("Failed to update TPM Event with Base RIM ID");
                }
            }
        }

        log.info("Returning the size of the list of reference digest values: "
                + "{}", pagedResult.getTotalElements());
        return new DataTableResponse<>(referenceDigestValues, input);
    }
}
