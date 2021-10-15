
package hirs.attestationca.portal.page.controllers;

import hirs.FilteredRecordsList;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.datatables.OrderedListQueryDataTableAdapter;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.params.NoPageParams;
import hirs.data.persist.ReferenceDigestRecord;
import hirs.data.persist.ReferenceDigestValue;
import hirs.data.persist.certificate.Certificate;
import hirs.persist.CriteriaModifier;
import hirs.persist.DBReferenceDigestManager;
import hirs.persist.DBReferenceEventManager;
import hirs.persist.ReferenceDigestManager;
import hirs.persist.ReferenceEventManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Controller for the TPM Events page.
 */
@Controller
@RequestMapping("/tpm-events")
public class TpmEventsPageController
        extends PageController<NoPageParams> {

    private static final String BIOS_RELEASE_DATE_FORMAT = "yyyy-MM-dd";
    private static final String LOG_FILE_PATTERN = "([^\\s]+(\\.(?i)(rimpcr|rimel|bin|log))$)";

    private final BiosDateValidator biosValidator;
    private final ReferenceDigestManager referenceDigestManager;
    private final ReferenceEventManager referenceEventManager;
    private static final Logger LOGGER
            = LogManager.getLogger(TpmEventsPageController.class);

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
        public BiosDateValidator(final String dateFormat) {
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
     * @param referenceDigestManager the ReferenceDigestManager object
     * @param referenceEventManager  the referenceEventManager object
     */
    @Autowired
    public TpmEventsPageController(
            final DBReferenceDigestManager referenceDigestManager,
            final DBReferenceEventManager referenceEventManager) {
        super(Page.TPM_EVENTS);
        this.referenceDigestManager = referenceDigestManager;
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
    @RequestMapping(value = "/list",
            produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.GET)
    public DataTableResponse<ReferenceDigestRecord> getTableData(
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
        FilteredRecordsList<ReferenceDigestRecord> referenceDigestRecords
                = OrderedListQueryDataTableAdapter.getOrderedList(
                ReferenceDigestRecord.class,
                referenceDigestManager,
                input, orderColumnName, criteriaModifier);
        LOGGER.info("ReferenceDigestManager returned: "
                + Arrays.toString(referenceDigestRecords.toArray()));
        FilteredRecordsList<HashMap<ReferenceDigestRecord, ReferenceDigestValue>>
                mappedRecordValues = mapRecordToValues(referenceDigestRecords);

        LOGGER.info("Returning list mapping: " + Arrays.toString(mappedRecordValues.toArray()));
        return new DataTableResponse<>(referenceDigestRecords, input);
    }

    /**
     * This method returns a mapping of ReferenceDigestRecord to ReferenceDigestValue objects.
     *
     * @param records the list of ReferenceDigestRecords
     * @return the collection of HashMap mappings
     */
    private FilteredRecordsList<HashMap<ReferenceDigestRecord, ReferenceDigestValue>>
    mapRecordToValues(final FilteredRecordsList<ReferenceDigestRecord> records) {

        LOGGER.info("Mapping RDRs and RDVs");
        FilteredRecordsList<HashMap<ReferenceDigestRecord, ReferenceDigestValue>> filteredList =
                new FilteredRecordsList<>();
        HashMap<ReferenceDigestRecord, ReferenceDigestValue> mappingRecordToValues =
                new HashMap<>();
        for (ReferenceDigestRecord record : records) {
            List<ReferenceDigestValue> values = referenceEventManager.getValuesByRecordId(record);
            if (values != null && !values.isEmpty()) {
                mappingRecordToValues.put(record, values.get(0));
            } else {
                mappingRecordToValues.put(record, null);
            }
            filteredList.add(new HashMap<>(mappingRecordToValues));
            mappingRecordToValues.clear();
        }

        return filteredList;
    }
}
