
package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.params.ReferenceManifestPageParams;

import hirs.FilteredRecordsList;
import hirs.persist.ReferenceManifestManager;
import hirs.data.persist.ReferenceManifest;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;


/**
 * Controller for the Certificate Details page.
 */
@Controller
@RequestMapping("/reference-manifests")
public class ReferenceManifestPageController
extends PageController<ReferenceManifestPageParams> {

    private static final String BIOS_RELEASE_DATE_FORMAT = "yyyy-MM-dd";

    private final BiosDateValidator biosValidator;
    private final ReferenceManifestManager referenceManifestManager;
    private static final Logger LOGGER =
            LogManager.getLogger(ReferenceManifestPageController.class);

    /**
     * This class was created for the purposes of avoiding findbugs message:
     * As the JavaDoc states, DateFormats are inherently unsafe for
     * multithreaded use. The detector has found a call to an instance
     * of DateFormat that has been obtained via a static field.
     * This looks suspicous.
     *
     * This class can have uses elsewhere but for now it will remain here.
     */
    private static final class BiosDateValidator {
        private final String dateFormat;

        /**
         * Default constructor that sets the format to parse against.
         * @param dateFormat
         */
        public BiosDateValidator(final String dateFormat) {
            this.dateFormat = dateFormat;
        }

        /**
         * Validates a date by attempting to parse based on format provided.
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
     * @param referenceManifestManager the reference manifest manager
     */
    @Autowired
    public ReferenceManifestPageController(
            final ReferenceManifestManager referenceManifestManager) {
        super(Page.REFERENCE_MANIFESTS);
        this.referenceManifestManager = referenceManifestManager;
        this.biosValidator = new BiosDateValidator(BIOS_RELEASE_DATE_FORMAT);
    }

    /**
     * Returns the path for the view and the data model for the page.
     *
     * @param params The object to map url parameters into.
     * @param model The data model for the request. Can contain data from redirect.
     * @return the path for the view and data model for the page.
     */
    @Override
    public ModelAndView initPage(final ReferenceManifestPageParams params,
            final Model model) {
        return getBaseModelAndView();
    }

    /**
     * Returns the list of RIMs using the datatable input for paging,
     * ordering, and filtering.
     * @param input the data tables input
     * @return the data tables response, including the result set
     * and paging information
     */
    @ResponseBody
    @RequestMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.GET)
    public DataTableResponse<ReferenceManifest> getTableData(
            final DataTableInput input) {
        LOGGER.debug("Handling request for summary list: " + input);

        String orderColumnName = input.getOrderColumnName();
        LOGGER.debug("Ordering on column: " + orderColumnName);

        ReferenceManifest rm = new ReferenceManifest();
        rm.setManufacturer("Ford");
        rm.setModel("Mach-E");
        rm.setFirmwareVersion("1.1");
        rm.setTagId("0000-60000");
        rm.setRimType("primary");
        FilteredRecordsList<ReferenceManifest> records
                = new FilteredRecordsList<>();
//                = OrderedListQueryDataTableAdapter.getOrderedList(
//                        ReferenceManifest.class, referenceManifestManager,
//                        input, orderColumnName);


        records.add(rm);
        LOGGER.debug("Returning list of size: " + records.size());
        return new DataTableResponse<>(records, input);
    }
}
