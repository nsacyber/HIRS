package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.CriteriaModifier;
import hirs.attestationca.persist.DBManagerException;
import hirs.attestationca.persist.FilteredRecordsList;
import hirs.attestationca.persist.entity.manager.ReferenceDigestValueRepository;
import hirs.attestationca.persist.entity.manager.ReferenceManifestRepository;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.rim.ReferenceDigestValue;
import hirs.attestationca.persist.entity.userdefined.rim.SupportReferenceManifest;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.params.NoPageParams;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.validation.Valid;
import lombok.extern.log4j.Log4j2;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.lang.ref.Reference;

/**
 * Controller for the TPM Events page.
 */
@Log4j2
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/rim-database")
public class RimDatabasePageController extends PageController<NoPageParams> {

    @Autowired(required = false)
    private EntityManager entityManager;

    private final ReferenceDigestValueRepository referenceDigestValueRepository;
    private final ReferenceManifestRepository referenceManifestRepository;

    /**
     * Constructor providing the Page's display and routing specification.
     *
     * @param referenceDigestValueRepository the referenceDigestValueRepository object
     * @param referenceManifestRepository the reference manifest manager object
     */
    @Autowired
    public RimDatabasePageController(final ReferenceDigestValueRepository referenceDigestValueRepository,
                                     final ReferenceManifestRepository referenceManifestRepository) {
        super(Page.RIM_DATABASE);
        this.referenceDigestValueRepository = referenceDigestValueRepository;
        this.referenceManifestRepository = referenceManifestRepository;
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
     * Returns the list of TPM Events using the data table input for paging, ordering,
     * and filtering.
     *
     * @param input the data tables input
     * @return the data tables response, including the result set and paging
     * information
     */
    @ResponseBody
    @RequestMapping(value = "/list",
            produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.GET)
    public DataTableResponse<ReferenceDigestValue> getTableData(
            @Valid final DataTableInput input) {
        log.info("Handling request for summary list: " + input);

        String orderColumnName = input.getOrderColumnName();
        log.info("Ordering on column: " + orderColumnName);

        // check that the alert is not archived and that it is in the specified report
        CriteriaModifier criteriaModifier = new CriteriaModifier() {
            @Override
            public void modify(final CriteriaQuery criteriaQuery) {
                Session session = entityManager.unwrap(Session.class);
                CriteriaBuilder cb = session.getCriteriaBuilder();
                Root<ReferenceDigestValue> rimRoot = criteriaQuery.from(Reference.class);
                criteriaQuery.select(rimRoot).distinct(true).where(cb.isNull(rimRoot.get(Certificate.ARCHIVE_FIELD)));
            }
        };

        log.info("Querying with the following dataTableInput: " + input.toString());

        FilteredRecordsList<ReferenceDigestValue> referenceDigestValues = new FilteredRecordsList<>();

        int currentPage = input.getStart() / input.getLength();
        Pageable paging = PageRequest.of(currentPage, input.getLength(), Sort.by(orderColumnName));
        org.springframework.data.domain.Page<ReferenceDigestValue> pagedResult = referenceDigestValueRepository.findAll(paging);

        if (pagedResult.hasContent()) {
            referenceDigestValues.addAll(pagedResult.getContent());
        }
        referenceDigestValues.setRecordsTotal(input.getLength());
        referenceDigestValues.setRecordsFiltered(referenceDigestValueRepository.count());

//        FilteredRecordsList<ReferenceDigestValue> referenceDigestValues =
//                OrderedListQueryDataTableAdapter.getOrderedList(
//                        referenceDigestValueRepository,
//                        input, orderColumnName, criteriaModifier, entityManager);

        // might be able to get rid of this, maybe right a query that looks for not updated
        SupportReferenceManifest support;
        for (ReferenceDigestValue rdv : referenceDigestValues) {
            // We are updating the base rim ID field if necessary and
            if (rdv.getBaseRimId() == null) {
                support = (SupportReferenceManifest) referenceManifestRepository.getReferenceById(rdv.getSupportRimId());
                if (support != null) {
                    rdv.setBaseRimId(support.getAssociatedRim());
                    try {
                        referenceDigestValueRepository.save(rdv);
                    } catch (DBManagerException e) {
                        log.error("Failed to update TPM Event with Base RIM ID");
                    }
                }
            }
        }

        log.debug("Returning list of size: " + referenceDigestValues.size());
        return new DataTableResponse<>(referenceDigestValues, input);
    }
}
