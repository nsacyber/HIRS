
package hirs.attestationca.portal.page.controllers;

import hirs.FilteredRecordsList;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.datatables.OrderedListQueryDataTableAdapter;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.params.NoPageParams;
import hirs.data.persist.ReferenceDigestValue;
import hirs.data.persist.SupportReferenceManifest;
import hirs.data.persist.certificate.Certificate;
import hirs.persist.CriteriaModifier;
import hirs.persist.DBManagerException;
import hirs.attestationca.persist.DBReferenceEventManager;
import hirs.attestationca.persist.DBReferenceManifestManager;
import hirs.persist.ReferenceEventManager;
import hirs.persist.ReferenceManifestManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Controller for the TPM Events page.
 */
@RestController
@RequestMapping(path = "/rim-database")
public class RimDatabasePageController
        extends PageController<NoPageParams> {

    private static final String BIOS_RELEASE_DATE_FORMAT = "yyyy-MM-dd";

    private final BiosDateValidator biosValidator;
    private final ReferenceManifestManager referenceManifestManager;
    private final ReferenceEventManager referenceEventManager;
    private static final Logger LOGGER
            = LogManager.getLogger(RimDatabasePageController.class);

    /**
     * This class was created for the purposes of avoiding findbugs message: As
     * the JavaDoc states, DateFormats are inherently unsafe for multi-threaded
     * use. The detector has found a call to an instance of DateFormat that has
     * been obtained via a static field. This looks suspicious.
     * <p>
     * This class can have uses elsewhere but for now it will remain here.
     */
    private static final class BiosDateValidator {

        private final String dateFormat;

        /**
         * Default constructor that sets the format to parse against.
         *
         * @param dateFormat
         */
        BiosDateValidator(final String dateFormat) {
            this.dateFormat = dateFormat;
        }

        /**
         * Validates a date by attempting to parse based on format provided.
         *
         * @param date string of the given date
         * @return true if the format matches
         */
        public boolean isValid(final String date) {
            DateFormat validFormat = new SimpleDateFormat(this.dateFormat);
            boolean result = true;
            validFormat.setLenient(false);

            try {
                validFormat.parse(date);
            } catch (ParseException pEx) {
                result = false;
            }

            return result;
        }
    }

    /**
     * Constructor providing the Page's display and routing specification.
     *
     * @param referenceManifestManager the ReferenceManifestManager object
     * @param referenceEventManager  the referenceEventManager object
     */
    @Autowired
    public RimDatabasePageController(
            final DBReferenceManifestManager referenceManifestManager,
            final DBReferenceEventManager referenceEventManager) {
        super(Page.RIM_DATABASE);
        this.referenceManifestManager = referenceManifestManager;
        this.referenceEventManager = referenceEventManager;
        this.biosValidator = new BiosDateValidator(BIOS_RELEASE_DATE_FORMAT);
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
    @GetMapping
    @RequestMapping(value = "/list",
            produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.GET)
    public DataTableResponse<ReferenceDigestValue> getTableData(
            final DataTableInput input) {
        LOGGER.info("Handling request for summary list: " + input);

        String orderColumnName = input.getOrderColumnName();
        LOGGER.info("Ordering on column: " + orderColumnName);

        // check that the alert is not archived and that it is in the specified report
        CriteriaModifier criteriaModifier = new CriteriaModifier() {
            @Override
            public void modify(final Criteria criteria) {
                criteria.add(Restrictions.isNull(Certificate.ARCHIVE_FIELD));
            }
        };

        LOGGER.info("Querying with the following datatableinput: " + input.toString());

        FilteredRecordsList<ReferenceDigestValue> referenceDigestValues =
                OrderedListQueryDataTableAdapter.getOrderedList(
                ReferenceDigestValue.class,
                referenceEventManager,
                input, orderColumnName, criteriaModifier);

        SupportReferenceManifest support;
        for (ReferenceDigestValue rdv : referenceDigestValues) {
            // We are updating the base rim ID field if necessary and
            if (rdv.getBaseRimId() == null) {
                support = SupportReferenceManifest.select(referenceManifestManager)
                        .byEntityId(rdv.getSupportRimId()).getRIM();
                if (support != null) {
                    rdv.setBaseRimId(support.getAssociatedRim());
                    try {
                        referenceEventManager.updateEvent(rdv);
                    } catch (DBManagerException e) {
                        LOGGER.error("Failed to update TPM Event with Base RIM ID");
                        LOGGER.error(rdv);
                    }
                }
            }
        }

        return new DataTableResponse<>(referenceDigestValues, input);
    }
}
