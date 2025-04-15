package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.DBManagerException;
import hirs.attestationca.persist.FilteredRecordsList;
import hirs.attestationca.persist.entity.manager.ReferenceDigestValueRepository;
import hirs.attestationca.persist.entity.manager.ReferenceManifestRepository;
import hirs.attestationca.persist.entity.userdefined.rim.ReferenceDigestValue;
import hirs.attestationca.persist.entity.userdefined.rim.SupportReferenceManifest;
import hirs.attestationca.persist.service.ReferenceDigestValueService;
import hirs.attestationca.portal.datatables.Column;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.params.NoPageParams;
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

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for the TPM Events page.
 */
@Log4j2
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/rim-database")
public class RimDatabasePageController extends PageController<NoPageParams> {

    private final ReferenceDigestValueRepository referenceDigestValueRepository;
    private final ReferenceManifestRepository referenceManifestRepository;
    private final ReferenceDigestValueService referenceDigestValueService;

    /**
     * Constructor providing the Page's display and routing specification.
     *
     * @param referenceDigestValueRepository the referenceDigestValueRepository object
     * @param referenceManifestRepository    the reference manifest manager object
     */
    @Autowired
    public RimDatabasePageController(final ReferenceDigestValueRepository referenceDigestValueRepository,
                                     final ReferenceManifestRepository referenceManifestRepository,
                                     ReferenceDigestValueService referenceDigestValueService) {
        super(Page.RIM_DATABASE);
        this.referenceDigestValueRepository = referenceDigestValueRepository;
        this.referenceManifestRepository = referenceManifestRepository;
        this.referenceDigestValueService = referenceDigestValueService;
    }

    /**
     * Returns the filePath for the view and the data model for the page.
     *
     * @param params The object to map url parameters into.
     * @param model  The data model for the request. Can contain data from
     *               redirect.
     * @return the filePath for the view and data model for the page.
     */
    @Override
    public ModelAndView initPage(final NoPageParams params,
                                 final Model model) {
        return getBaseModelAndView();
    }

    /**
     * Processes request to retrieve the collection of TPM Events that will be displayed
     * on the rim database page.
     *
     * @param input the data tables input
     * @return the data tables response, including the result set and paging
     * information
     */
    @ResponseBody
    @GetMapping(value = "/list",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public DataTableResponse<ReferenceDigestValue> getTableData(
            @Valid final DataTableInput input) {
        log.info("Received request to display list of TPM events");
        log.debug("Request received a datatable input object for the RIM database page: {}", input);

        String orderColumnName = input.getOrderColumnName();
        log.debug("Ordering on column: {}", orderColumnName);

        final String searchText = input.getSearch().getValue();
        final List<String> searchableColumns = findSearchableColumnsNames(input.getColumns());

        FilteredRecordsList<ReferenceDigestValue> referenceDigestValues = new FilteredRecordsList<>();

        int currentPage = input.getStart() / input.getLength();
        Pageable pageable = PageRequest.of(currentPage, input.getLength(), Sort.by(orderColumnName));
        org.springframework.data.domain.Page<ReferenceDigestValue> pagedResult;

        if (StringUtils.isBlank(searchText)) {
            pagedResult =
                    this.referenceDigestValueRepository.findAll(pageable);
        } else {
            pagedResult =
                    this.referenceDigestValueService.findReferenceDigestValuesBySearchableColumns(
                            searchableColumns,
                            searchText, pageable);
        }

        if (pagedResult.hasContent()) {
            referenceDigestValues.addAll(pagedResult.getContent());
            referenceDigestValues.setRecordsTotal(pagedResult.getContent().size());
        } else {
            referenceDigestValues.setRecordsTotal(input.getLength());
        }

        referenceDigestValues.setRecordsFiltered(referenceDigestValueRepository.count());

        // might be able to get rid of this, maybe right a query that looks for not updated
        SupportReferenceManifest support;
        for (ReferenceDigestValue rdv : referenceDigestValues) {
            // We are updating the base rim ID field if necessary and
            if (rdv.getBaseRimId() == null && referenceManifestRepository.existsById(rdv.getSupportRimId())) {
                support = (SupportReferenceManifest) referenceManifestRepository.getReferenceById(
                        rdv.getSupportRimId());
                rdv.setBaseRimId(support.getAssociatedRim());
                try {
                    referenceDigestValueRepository.save(rdv);
                } catch (DBManagerException dbMEx) {
                    log.error("Failed to update TPM Event with Base RIM ID");
                }
            }
        }

        log.info("Returning the size of the list of TPM events: {}", referenceDigestValues.size());
        return new DataTableResponse<>(referenceDigestValues, input);
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
