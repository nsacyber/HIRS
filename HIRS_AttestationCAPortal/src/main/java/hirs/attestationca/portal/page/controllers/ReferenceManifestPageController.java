package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.CriteriaModifier;
import hirs.attestationca.persist.FilteredRecordsList;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
import hirs.attestationca.persist.service.ReferenceDigestValueService;
import hirs.attestationca.persist.service.ReferenceDigestValueServiceImpl;
import hirs.attestationca.persist.service.ReferenceManifestService;
import hirs.attestationca.persist.service.ReferenceManifestServiceImpl;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.datatables.OrderedListQueryDataTableAdapter;
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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.lang.ref.Reference;

/**
 * Controller for the Reference Manifest page.
 */
@Log4j2
@Controller
@RequestMapping("/reference-manifests")
public class ReferenceManifestPageController extends PageController<NoPageParams> {

    @Autowired(required = false)
    private EntityManager entityManager;

    private final ReferenceManifestService referenceManifestManager;
    private final ReferenceDigestValueService referenceEventManager;

    /**
     * Constructor providing the Page's display and routing specification.
     *
     * @param referenceManifestManager the reference manifest manager
     * @param referenceEventManager this is the reference event manager
     */
    @Autowired
    public ReferenceManifestPageController(
            final ReferenceManifestServiceImpl referenceManifestManager,
            final ReferenceDigestValueServiceImpl referenceEventManager) {
        super(Page.REFERENCE_MANIFESTS);
        this.referenceManifestManager = referenceManifestManager;
        this.referenceEventManager = referenceEventManager;
    }

    /**
     * Returns the filePath for the view and the data model for the page.
     *
     * @param params The object to map url parameters into.
     * @param model The data model for the request. Can contain data from
     * redirect.
     * @return the filePath for the view and data model for the page.
     */
    @Override
    public ModelAndView initPage(final NoPageParams params,
                                 final Model model) {
        return getBaseModelAndView();
    }

    /**
     * Returns the list of RIMs using the data table input for paging, ordering,
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
    public DataTableResponse<ReferenceManifest> getTableData(
            @Valid final DataTableInput input) {
        log.info("Handling request for summary list: " + input);

//        return this.referenceManifestManager.fetchReferenceManifests(input);

        String orderColumnName = input.getOrderColumnName();
        log.debug("Ordering on column: " + orderColumnName);

        // check that the alert is not archived and that it is in the specified report
        CriteriaModifier criteriaModifier = new CriteriaModifier() {
            @Override
            public void modify(final CriteriaQuery criteriaQuery) {
                Session session = entityManager.unwrap(Session.class);
                CriteriaBuilder cb = session.getCriteriaBuilder();
                Root<ReferenceManifest> rimRoot = criteriaQuery.from(Reference.class);

                criteriaQuery.select(rimRoot).distinct(true).where(cb.isNull(rimRoot.get(Certificate.ARCHIVE_FIELD)));
//                criteria.add(Restrictions.isNull(Certificate.ARCHIVE_FIELD));
            }
        };
        FilteredRecordsList<ReferenceManifest> records
                = OrderedListQueryDataTableAdapter.getOrderedList(
                ReferenceManifest.class,
                referenceManifestManager,
                input, orderColumnName, criteriaModifier);

        log.debug("Returning list of size: " + records.size());
        return new DataTableResponse<>(records, input);
    }
}
