package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.entity.manager.ReferenceDigestValueRepository;
import hirs.attestationca.persist.entity.userdefined.rim.ReferenceDigestValue;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.params.NoPageParams;
import jakarta.validation.Valid;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

/**
 * Controller for the TPM Events page.
 */
@Log4j2
@Controller
@RequestMapping("/rim-database")
public class RimDatabasePageController extends PageController<NoPageParams> {

    private final ReferenceDigestValueRepository referenceDigestValueRepository;
//    private final ReferenceManifestServiceImpl referenceManifestManager;
//    private final ReferenceDigestValueServiceImpl referenceEventManager;

    /**
     * Constructor providing the Page's display and routing specification.
     *
     * @param referenceDigestValueRepository the referenceDigestValueRepository object
//     * @param referenceEventManager  the referenceEventManager object
     */
    @Autowired
    public RimDatabasePageController(final ReferenceDigestValueRepository referenceDigestValueRepository
//                                    , final ReferenceManifestServiceImpl referenceManifestManager,
//            final ReferenceDigestValueServiceImpl referenceEventManager
    ) {
        super(Page.RIM_DATABASE);
        this.referenceDigestValueRepository = referenceDigestValueRepository;
//        this.referenceEventManager = referenceEventManager;
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
    public List<ReferenceDigestValue> getTableData(
            @Valid final DataTableInput input) {
        log.info("Handling request for summary list: " + input);

        return this.referenceDigestValueRepository.listAll();


//        String orderColumnName = input.getOrderColumnName();
//        log.info("Ordering on column: " + orderColumnName);
//
//        // check that the alert is not archived and that it is in the specified report
//        CriteriaModifier criteriaModifier = new CriteriaModifier() {
//            @Override
//            public void modify(final Criteria criteria) {
//                criteria.add(Restrictions.isNull(Certificate.ARCHIVE_FIELD));
//            }
//        };
//
//        log.info("Querying with the following datatableinput: " + input.toString());
//
//        FilteredRecordsList<ReferenceDigestValue> referenceDigestValues =
//                OrderedListQueryDataTableAdapter.getOrderedList(
//                        ReferenceDigestValue.class,
//                        referenceEventManager,
//                        input, orderColumnName, criteriaModifier);
//
//        SupportReferenceManifest support;
//        for (ReferenceDigestValue rdv : referenceDigestValues) {
//            // We are updating the base rim ID field if necessary and
//            if (rdv.getBaseRimId() == null) {
//                support = SupportReferenceManifest.select(referenceManifestManager)
//                        .byEntityId(rdv.getSupportRimId()).getRIM();
//                if (support != null) {
//                    rdv.setBaseRimId(support.getAssociatedRim());
//                    try {
//                        referenceEventManager.updateRefDigestValue(rdv);
//                    } catch (DBManagerException e) {
//                        log.error("Failed to update TPM Event with Base RIM ID");
//                        log.error(rdv);
//                    }
//                }
//            }
//        }
//
//        return new DataTableResponse<>(referenceDigestValues, input);
    }
}
