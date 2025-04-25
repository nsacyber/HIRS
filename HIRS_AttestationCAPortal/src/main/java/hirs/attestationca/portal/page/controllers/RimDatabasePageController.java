package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.DBManagerException;
import hirs.attestationca.persist.FilteredRecordsList;
import hirs.attestationca.persist.entity.manager.ReferenceDigestValueRepository;
import hirs.attestationca.persist.entity.manager.ReferenceManifestRepository;
import hirs.attestationca.persist.entity.userdefined.rim.ReferenceDigestValue;
import hirs.attestationca.persist.entity.userdefined.rim.SupportReferenceManifest;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.params.NoPageParams;
import hirs.attestationca.portal.page.utils.ControllerPagesUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.validation.Valid;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Controller for the TPM Events page.
 */
@Log4j2
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/rim-database")
public class RimDatabasePageController extends PageController<NoPageParams> {

    private final ReferenceDigestValueRepository referenceDigestValueRepository;
    private final ReferenceManifestRepository referenceManifestRepository;
    private final EntityManager entityManager;

    /**
     * Constructor providing the Page's display and routing specification.
     *
     * @param referenceDigestValueRepository the referenceDigestValueRepository object
     * @param referenceManifestRepository    the reference manifest manager object
     * @param entityManager                  entity manager
     */
    @Autowired
    public RimDatabasePageController(final ReferenceDigestValueRepository referenceDigestValueRepository,
                                     final ReferenceManifestRepository referenceManifestRepository,
                                     final EntityManager entityManager) {
        super(Page.RIM_DATABASE);
        this.referenceDigestValueRepository = referenceDigestValueRepository;
        this.referenceManifestRepository = referenceManifestRepository;
        this.entityManager = entityManager;
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
                    this.referenceDigestValueRepository.findAll(pageable);
        } else {
            pagedResult =
                    findReferenceDigestValuesBySearchableColumns(
                            searchableColumns,
                            searchTerm, pageable);
        }

        if (pagedResult.hasContent()) {
            referenceDigestValues.addAll(pagedResult.getContent());
        }

        referenceDigestValues.setRecordsFiltered(pagedResult.getTotalElements());
        referenceDigestValues.setRecordsTotal(referenceDigestValueRepository.count());

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

        log.info("Returning the size of the list of reference digest values: "
                + "{}", pagedResult.getTotalElements());
        return new DataTableResponse<>(referenceDigestValues, input);
    }

    /**
     * Takes the provided column names, the search term that the user entered and attempts to find
     * reference digest values whose field values matches the provided search term.
     *
     * @param searchableColumns list of the searchable column name
     * @param searchTerm        text that was input in the search textbox
     * @param pageable          pageable
     * @return page full of reference digest values
     */
    private org.springframework.data.domain.Page<ReferenceDigestValue>
    findReferenceDigestValuesBySearchableColumns(
            final Set<String> searchableColumns,
            final String searchTerm,
            final Pageable pageable) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ReferenceDigestValue> query =
                criteriaBuilder.createQuery(ReferenceDigestValue.class);
        Root<ReferenceDigestValue> referenceDigestValueRoot =
                query.from(ReferenceDigestValue.class);

        List<Predicate> predicates = new ArrayList<>();

        // Dynamically add search conditions for each field that should be searchable
        if (!StringUtils.isBlank(searchTerm)) {
            // Dynamically loop through columns and create LIKE conditions for each searchable column
            for (String columnName : searchableColumns) {
                // Get the attribute type from entity root
                Path<?> fieldPath = referenceDigestValueRoot.get(columnName);

                // if the field is a string type
                if (String.class.equals(fieldPath.getJavaType())) {
                    Predicate predicate =
                            criteriaBuilder.like(
                                    criteriaBuilder.lower(referenceDigestValueRoot.get(columnName)),
                                    "%" + searchTerm.toLowerCase() + "%");
                    predicates.add(predicate);
                } else if (Integer.class.equals(fieldPath.getJavaType())) {
                    // For Integer fields, use EQUAL if the search term is numeric
                    try {
                        Integer searchInteger = Integer.valueOf(searchTerm); // Will throw if not a number
                        Predicate predicate = criteriaBuilder.equal(fieldPath, searchInteger);
                        predicates.add(predicate);
                    } catch (NumberFormatException e) {
                        // If the searchTerm is not a valid number, skip this field
                    }
                }
            }
        }

        query.where(criteriaBuilder.or(predicates.toArray(new Predicate[0])));

        // Apply pagination
        TypedQuery<ReferenceDigestValue> typedQuery = entityManager.createQuery(query);
        int totalRows = typedQuery.getResultList().size();  // Get the total count for pagination
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        // Wrap the result in a Page object to return pagination info
        List<ReferenceDigestValue> resultList = typedQuery.getResultList();
        return new PageImpl<>(resultList, pageable, totalRows);
    }
}
