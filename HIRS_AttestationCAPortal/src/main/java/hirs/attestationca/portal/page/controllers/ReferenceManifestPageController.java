
package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.params.ReferenceManifestPageParams;

import hirs.FilteredRecordsList;
import hirs.attestationca.portal.page.PageMessages;
import hirs.data.persist.certificate.Certificate;
import hirs.persist.CertificateManager;
import hirs.persist.DBManagerException;
import hirs.persist.ReferenceManifestManager;
import hirs.data.persist.ReferenceManifest;
import java.io.IOException;
import java.net.URISyntaxException;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import hirs.utils.SwidTagGateway;
import hirs.utils.xjc.SoftwareIdentity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;


/**
 * Controller for the Reference Manifest page.
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
     * multi-threaded use. The detector has found a call to an instance
     * of DateFormat that has been obtained via a static field.
     * This looks suspicious.
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
     * Returns the list of RIMs using the data table input for paging,
     * ordering, and filtering.
     * @param input the data tables input
     * @return the data tables response, including the result set
     * and paging information
     */
    @ResponseBody
    @RequestMapping(value = "/list",
            produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.GET)
    public DataTableResponse<ReferenceManifest> getTableData(
            final DataTableInput input) {
        LOGGER.error("Made it into the list method for RIM");
        LOGGER.debug("Handling request for summary list: " + input);

        String orderColumnName = input.getOrderColumnName();
        LOGGER.debug("Ordering on column: " + orderColumnName);

        FilteredRecordsList<ReferenceManifest> records
                = new FilteredRecordsList<>();
        LOGGER.debug("Returning list of size: " + records.size());
        return new DataTableResponse<>(records, input);
    }

    /**
     * Upload and processes a reference manifest(s).
     *
     * @param files the files to process
     * @param attr the redirection attributes
     * @return the redirection view
     * @throws URISyntaxException if malformed URI
     * @throws Exception if malformed URI
     */
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    protected RedirectView upload(
            @RequestParam("file") final MultipartFile[] files,
            final RedirectAttributes attr) throws URISyntaxException, Exception {
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        // loop through the files
        for (MultipartFile file : files) {
            //Parse reference manifests
            ReferenceManifest rims = parseRIMs(file, messages);

            //Store only if it was parsed
            if (rims != null) {
                storeManifest(file.getOriginalFilename(),
                        messages,
                        rims,
                        referenceManifestManager);
            }
        }

        //Add messages to the model
        model.put(MESSAGES_ATTRIBUTE, messages);

        return redirectTo(Page.REFERENCE_MANIFESTS,
                new ReferenceManifestPageParams(), model, attr);
    }

    private ReferenceManifest parseRIMs(
            final MultipartFile file,
            final PageMessages messages) {

        byte[] fileBytes;
        String fileName = file.getOriginalFilename();

        // build the manifest from the uploaded bytes
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            final String failMessage
                    = String.format("Failed to read uploaded file (%s): ", fileName);
            LOGGER.error(failMessage, e);
            messages.addError(failMessage + e.getMessage());
            return null;
        }

        try {
            return new ReferenceManifest(fileBytes);
            // the this is a List<Object> is object is a JaxBElement that can be matched up to the QName
        } catch (IOException ioEx) {
            final String failMessage
                    = String.format("Failed to parse uploaded file (%s): ", fileName);
            LOGGER.error(failMessage, ioEx);
            messages.addError(failMessage + ioEx.getMessage());
            return null;
        }
    }

    private void storeManifest(
            final String fileName,
            final PageMessages messages,
            final ReferenceManifest referenceManifest,
            final ReferenceManifestManager referenceManifestManager) {

        ReferenceManifest existingManifest;

        // look for existing manifest in the database
        try {
            existingManifest = ReferenceManifest
                    .select(referenceManifestManager)
                    .includeArchived()
                    .byHashCode(referenceManifest.getRimHash())
                    .getRIM();
        } catch (DBManagerException e) {
            final String failMessage = String.format("Querying for existing certificate failed (%s): ", fileName);
            messages.addError(failMessage + e.getMessage());
            LOGGER.error(failMessage, e);
            return;
        }

        try {
            // save the new certificate if no match is found
            if (existingManifest == null) {
                referenceManifestManager.save(referenceManifest);

                final String successMsg = String.format("New RIM successfully uploaded (%s): ", fileName);
                messages.addSuccess(successMsg);
                LOGGER.info(successMsg);
                return;
            }
        } catch (DBManagerException dbmEx) {
            final String failMessage = String.format("Storing new RIM failed (%s): ", fileName);
            messages.addError(failMessage + dbmEx.getMessage());
            LOGGER.error(failMessage, dbmEx);
            return;
        }

        try {
            // if an identical RIM is archived, update the existing RIM to
            // unarchive it and change the creation date
            if (existingManifest.isArchived()) {
                existingManifest.restore();
                existingManifest.resetCreateTime();
                referenceManifestManager.update(existingManifest);

                final String successMsg
                        = String.format("Pre-existing RIM found and unarchived (%s): ", fileName);
                messages.addSuccess(successMsg);
                LOGGER.info(successMsg);
                return;
            }
        } catch (DBManagerException dbmEx) {
            final String failMessage = String.format("Found an identical pre-existing RIM in the "
                    + "archive, but failed to unarchive it (%s): ", fileName);
            messages.addError(failMessage + dbmEx.getMessage());
            LOGGER.error(failMessage, dbmEx);
            return;
        }
    }
}
