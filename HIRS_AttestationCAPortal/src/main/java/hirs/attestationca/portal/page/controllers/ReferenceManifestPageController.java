
package hirs.attestationca.portal.page.controllers;

import hirs.FilteredRecordsList;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.datatables.OrderedListQueryDataTableAdapter;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.PageMessages;
import hirs.attestationca.portal.page.params.NoPageParams;
import hirs.data.persist.BaseReferenceManifest;
import hirs.data.persist.EventLogMeasurements;
import hirs.data.persist.ReferenceDigestValue;
import hirs.data.persist.ReferenceManifest;
import hirs.data.persist.SupportReferenceManifest;
import hirs.data.persist.SwidResource;
import hirs.data.persist.certificate.Certificate;
import hirs.persist.CriteriaModifier;
import hirs.persist.DBManagerException;
import hirs.persist.ReferenceEventManager;
import hirs.persist.ReferenceManifestManager;
import hirs.tpm.eventlog.TCGEventLog;
import hirs.tpm.eventlog.TpmPcrEvent;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Controller for the Reference Manifest page.
 */
@Controller
@RequestMapping("/reference-manifests")
public class ReferenceManifestPageController
        extends PageController<NoPageParams> {

    private static final String BIOS_RELEASE_DATE_FORMAT = "yyyy-MM-dd";
    private static final String LOG_FILE_PATTERN = "([^\\s]+(\\.(?i)(rimpcr|rimel|bin|log))$)";

    private final BiosDateValidator biosValidator;
    private final ReferenceManifestManager referenceManifestManager;
    private final ReferenceEventManager referenceEventManager;
    private static final Logger LOGGER
            = LogManager.getLogger(ReferenceManifestPageController.class);

    /**
     * This class was created for the purposes of avoiding findbugs message: As
     * the JavaDoc states, DateFormats are inherently unsafe for multi-threaded
     * use. The detector has found a call to an instance of DateFormat that has
     * been obtained via a static field. This looks suspicious.
     *
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
     * @param referenceManifestManager the reference manifest manager
     * @param referenceEventManager this is the reference event manager
     */
    @Autowired
    public ReferenceManifestPageController(
            final ReferenceManifestManager referenceManifestManager,
            final ReferenceEventManager referenceEventManager) {
        super(Page.REFERENCE_MANIFESTS);
        this.referenceManifestManager = referenceManifestManager;
        this.referenceEventManager = referenceEventManager;
        this.biosValidator = new BiosDateValidator(BIOS_RELEASE_DATE_FORMAT);
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
            final DataTableInput input) {
        LOGGER.debug("Handling request for summary list: " + input);

        String orderColumnName = input.getOrderColumnName();
        LOGGER.debug("Ordering on column: " + orderColumnName);

        // check that the alert is not archived and that it is in the specified report
        CriteriaModifier criteriaModifier = new CriteriaModifier() {
            @Override
            public void modify(final Criteria criteria) {
                criteria.add(Restrictions.isNull(Certificate.ARCHIVE_FIELD));
            }
        };
        FilteredRecordsList<ReferenceManifest> records
                = OrderedListQueryDataTableAdapter.getOrderedList(
                        ReferenceManifest.class,
                        referenceManifestManager,
                        input, orderColumnName, criteriaModifier);

        SupportReferenceManifest support;
//        List<ReferenceDigestValue> events;
//        for (ReferenceManifest rim : records) {
//            if (rim instanceof SupportReferenceManifest) {
//                support = (SupportReferenceManifest) rim;
//                events = referenceEventManager.getValuesByRimId(support);
//
//                for (ReferenceDigestValue rdv : events) {
//                    // the selector isn't giving me what I want
//                    if (support.getPlatformManufacturer() != null) {
//                        rdv.setManufacturer(support.getPlatformManufacturer());
//                    }
//                    if (support.getPlatformModel() != null) {
//                        rdv.setModel(support.getPlatformModel());
//                    }
//                    if (support.getAssociatedRim() != null) {
//                        rdv.setBaseRimId(support.getAssociatedRim());
//                    }
//                    referenceEventManager.updateRecord(rdv);
//                }
//            }
//        }

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
        String fileName;
        Pattern logPattern = Pattern.compile(LOG_FILE_PATTERN);
        Matcher matcher;
        boolean supportRIM = false;
        BaseReferenceManifest base;
        SupportReferenceManifest support;

        // loop through the files
        for (MultipartFile file : files) {
            fileName = file.getOriginalFilename();
            matcher = logPattern.matcher(fileName);
            supportRIM = matcher.matches();

            //Parse reference manifests
            ReferenceManifest rim = parseRIM(file, supportRIM, messages);
            // store first then update
            ReferenceManifest  referenceManifest = storeManifest(file.getOriginalFilename(),
                    messages,
                    rim,
                    supportRIM);
            //Store only if it was parsed
            if (rim != null) {
                if (supportRIM) {
                    // look for associated base/support
                    // if I am the support rim, my hash is in the meta data of the swidtag
                    Set<BaseReferenceManifest> rims = BaseReferenceManifest
                            .select(referenceManifestManager).getRIMs();
                    support = (SupportReferenceManifest) rim;
                    // update information for associated support rim
                    for (BaseReferenceManifest bRim : rims) {
                        for (SwidResource swid : bRim.parseResource()) {
                            if (support.getHexDecHash().equals(swid.getHashValue())) {
                                updateSupportRimInfo(bRim, support);
                                referenceManifestManager.update(support);
                            }
                        }
                        if (support.isUpdated()) {
                            for (ReferenceDigestValue rdv : referenceEventManager
                                    .getValuesByRimId(support)) {
                                rdv.updateInfo(support);
                                referenceEventManager.updateRecord(rdv);
                            }
                            break;
                        }
                    }
                } else {
                    base = (BaseReferenceManifest) referenceManifest;
                    // the base can find the support rim by the meta data hash
                    for (SwidResource swid : base.parseResource()) {
                        support = SupportReferenceManifest.select(referenceManifestManager)
                                .byHexDecHash(swid.getHashValue()).getRIM();
                        if (support != null) {
                            base.setAssociatedRim(support.getId());
                            if (support.isUpdated()) {
                                // this is separate because I want to break if we found it
                                // instead of finding it, it is uptodate but still search
                                break;
                            } else {
                                updateSupportRimInfo(base, support);
                                updateTpmEvents(support);
                                try {
                                    referenceManifestManager.update(support);
                                } catch (DBManagerException dbmEx) {
                                    LOGGER.warn("Failed to update Support RIM");
                                }
                            }
                        }
                    }
                }
            }
        }

        //Add messages to the model
        model.put(MESSAGES_ATTRIBUTE, messages);

        return redirectTo(Page.REFERENCE_MANIFESTS,
                new NoPageParams(), model, attr);
    }

    /**
     * Archives (soft delete) the Reference Integrity Manifest entry.
     *
     * @param id the UUID of the rim to delete
     * @param attr RedirectAttributes used to forward data back to the original
     * page.
     * @return redirect to this page
     * @throws URISyntaxException if malformed URI
     */
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public RedirectView delete(@RequestParam final String id,
            final RedirectAttributes attr) throws URISyntaxException {
        LOGGER.info("Handling request to delete " + id);

        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        try {
            ReferenceManifest referenceManifest = getRimFromDb(id);

            if (referenceManifest == null) {
                String notFoundMessage = "Unable to locate RIM with ID: " + id;
                messages.addError(notFoundMessage);
                LOGGER.warn(notFoundMessage);
            } else {
                referenceManifest.archive();
                referenceManifestManager.update(referenceManifest);

                String deleteCompletedMessage = "RIM successfully deleted";
                messages.addInfo(deleteCompletedMessage);
                LOGGER.info(deleteCompletedMessage);

                // if support rim, update associated events
                if (referenceManifest instanceof SupportReferenceManifest) {
                    List<ReferenceDigestValue> rdvs = referenceEventManager
                            .getValuesByRimId(referenceManifest);

                    for (ReferenceDigestValue rdv : rdvs) {
                       rdv.archive("Support RIM was deleted");
                       referenceEventManager.updateRecord(rdv);
                    }
                }
            }
        } catch (IllegalArgumentException ex) {
            String uuidError = "Failed to parse ID from: " + id;
            messages.addError(uuidError);
            LOGGER.error(uuidError, ex);
        } catch (DBManagerException ex) {
            String dbError = "Failed to archive cert: " + id;
            messages.addError(dbError);
            LOGGER.error(dbError, ex);
        }

        model.put(MESSAGES_ATTRIBUTE, messages);
        return redirectTo(Page.REFERENCE_MANIFESTS, new NoPageParams(), model, attr);
    }

    /**
     * Handles request to download the rim by writing it to the response stream
     * for download.
     *
     * @param id the UUID of the rim to download
     * @param response the response object (needed to update the header with the
     * file name)
     * @throws java.io.IOException when writing to response output stream
     */
    @RequestMapping(value = "/download", method = RequestMethod.GET)
    public void download(@RequestParam final String id,
            final HttpServletResponse response)
            throws IOException {
        LOGGER.info("Handling RIM request to download " + id);

        try {
            ReferenceManifest referenceManifest = getRimFromDb(id);

            if (referenceManifest == null) {
                String notFoundMessage = "Unable to locate RIM with ID: " + id;
                LOGGER.warn(notFoundMessage);
                // send a 404 error when invalid Reference Manifest
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else {
                StringBuilder fileName = new StringBuilder("filename=\"");
                fileName.append(referenceManifest.getFileName());
                // Set filename for download.
                response.setHeader("Content-Disposition", "attachment;" + fileName);
                response.setContentType("application/octet-stream");

                // write cert to output stream
                response.getOutputStream().write(referenceManifest.getRimBytes());
            }
        } catch (IllegalArgumentException ex) {
            String uuidError = "Failed to parse ID from: " + id;
            LOGGER.error(uuidError, ex);
            // send a 404 error when invalid certificate
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Handles request to download bulk of RIMs by writing it to the response stream
     * for download in bulk.
     *
     * @param response the response object (needed to update the header with the
     * file name)
     * @throws java.io.IOException when writing to response output stream
     */
    @RequestMapping(value = "/bulk", method = RequestMethod.GET)
    public void bulk(final HttpServletResponse response)
            throws IOException {
        LOGGER.info("Handling request to download all Reference Integrity Manifests");
        String fileName = "rims.zip";
        String zipFileName;

        // Set filename for download.
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        response.setContentType("application/zip");

        List<ReferenceManifest> referenceManifestList = new LinkedList<>();
        referenceManifestList.addAll(BaseReferenceManifest
                .select(referenceManifestManager).getRIMs());
        referenceManifestList.addAll(SupportReferenceManifest
                .select(referenceManifestManager).getRIMs());

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            // get all files
            for (ReferenceManifest rim : referenceManifestList) {
                if (rim.getFileName().isEmpty()) {
                    zipFileName = "";
                } else {
                    // configure the zip entry, the properties of the 'file'
                    zipFileName = rim.getFileName();
                }
                ZipEntry zipEntry = new ZipEntry(zipFileName);
                zipEntry.setSize((long) rim.getRimBytes().length * Byte.SIZE);
                zipEntry.setTime(System.currentTimeMillis());
                zipOut.putNextEntry(zipEntry);
                // the content of the resource
                StreamUtils.copy(rim.getRimBytes(), zipOut);
                zipOut.closeEntry();
            }
            zipOut.finish();
            // write cert to output stream
        } catch (IllegalArgumentException ex) {
            String uuidError = "Failed to parse ID from: ";
            LOGGER.error(uuidError, ex);
            // send a 404 error when invalid certificate
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * This method takes the parameter and looks for this information in the
     * Database.
     *
     * @param id of the RIM
     * @return the associated RIM from the DB
     * @throws IllegalArgumentException
     */
    private ReferenceManifest getRimFromDb(final String id) throws IllegalArgumentException {
        UUID uuid = UUID.fromString(id);
        ReferenceManifest rim = BaseReferenceManifest.select(referenceManifestManager)
                .byEntityId(uuid).getRIM();

        if (rim == null) {
            rim = SupportReferenceManifest.select(referenceManifestManager)
                    .byEntityId(uuid).getRIM();
        }

        if (rim == null) {
            rim = EventLogMeasurements.select(referenceManifestManager)
                    .byEntityId(uuid).getRIM();
        }

        return rim;
    }

    /**
     * Takes the rim files provided and returns a {@link ReferenceManifest}
     * object.
     *
     * @param file the provide user file via browser.
     * @param messages the object that handles displaying information to the
     * user.
     * @return a single or collection of reference manifest files.
     */
    private ReferenceManifest parseRIM(
            final MultipartFile file, final boolean supportRIM,
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
            if (supportRIM) {
                return new SupportReferenceManifest(fileName, fileBytes);
            } else {
                return new BaseReferenceManifest(fileName, fileBytes);
            }
            // the this is a List<Object> is object is a JaxBElement that can
            // be matched up to the QName
        } catch (IOException ioEx) {
            final String failMessage
                    = String.format("Failed to parse uploaded file (%s): ", fileName);
            LOGGER.error(failMessage, ioEx);
            messages.addError(failMessage + ioEx.getMessage());
            return null;
        }
    }

    /**
     * Stores the {@link ReferenceManifest} objects.
     *
     * @param fileName name of the file given
     * @param messages message object for user display of statuses
     * @param referenceManifest the object to store
     * @param supportRim boolean flag indicating if this is a support RIM
     * process.
     */
    private ReferenceManifest storeManifest(
            final String fileName,
            final PageMessages messages,
            final ReferenceManifest referenceManifest,
            final boolean supportRim) {

        ReferenceManifest existingManifest;

        MessageDigest digest = null;
        String rimHash = "";
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException noSaEx) {
            LOGGER.error(noSaEx);
        }

        // look for existing manifest in the database
        try {
            if (supportRim) {
                if (digest != null) {
                    rimHash = Hex.encodeHexString(
                            digest.digest(referenceManifest.getRimBytes()));
                }
                existingManifest = SupportReferenceManifest
                        .select(referenceManifestManager)
                        .byHexDecHash(rimHash)
                        .includeArchived()
                        .getRIM();
            } else {
                if (digest != null) {
                    rimHash = Base64.encodeBase64String(
                            digest.digest(referenceManifest.getRimBytes()));
                }
                existingManifest = BaseReferenceManifest
                        .select(referenceManifestManager).byBase64Hash(rimHash)
                        .includeArchived()
                        .getRIM();
            }
        } catch (DBManagerException e) {
            final String failMessage = String.format("Querying for existing certificate "
                    + "failed (%s): ", fileName);
            messages.addError(failMessage + e.getMessage());
            LOGGER.error(failMessage, e);
            return null;
        }

        try {
            // save the new certificate if no match is found
            if (existingManifest == null) {
                saveTpmEvents(referenceManifestManager.save(referenceManifest));

                final String successMsg = String.format("RIM successfully uploaded (%s): ",
                        fileName);
                messages.addSuccess(successMsg);
                LOGGER.info(successMsg);

                return referenceManifest;
            }
        } catch (DBManagerException dbmEx) {
            final String failMessage = String.format("Storing RIM failed (%s): ", fileName);
            messages.addError(failMessage + dbmEx.getMessage());
            LOGGER.error(failMessage, dbmEx);
            return null;
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
                return existingManifest;
            }
        } catch (DBManagerException dbmEx) {
            final String failMessage = String.format("Found an identical pre-existing RIM in the "
                    + "archive, but failed to unarchive it (%s): ", fileName);
            messages.addError(failMessage + dbmEx.getMessage());
            LOGGER.error(failMessage, dbmEx);

            return null;
        }

        return referenceManifest;
    }

    private void updateSupportRimInfo(final BaseReferenceManifest dbBaseRim,
                                      final SupportReferenceManifest supportRim) {
        // I have to assume the baseRim is from the database
        // Updating the id values, manufacturer, model
        if (supportRim != null) {
            supportRim.setSwidTagVersion(dbBaseRim.getSwidTagVersion());
            supportRim.setPlatformManufacturer(dbBaseRim.getPlatformManufacturer());
            supportRim.setPlatformModel(dbBaseRim.getPlatformModel());
            supportRim.setTagId(dbBaseRim.getTagId());
            supportRim.setAssociatedRim(dbBaseRim.getId());
            supportRim.setUpdated(true);
        }
    }

    private void updateTpmEvents(final ReferenceManifest referenceManifest) {
        String manufacturer;
        String model;
        if (referenceManifest.getPlatformManufacturer() == null) {
            manufacturer = "";
        } else {
            manufacturer = referenceManifest.getPlatformManufacturer();
        }

        if (referenceManifest.getPlatformModel() == null) {
            model = "";
        } else {
            model = referenceManifest.getPlatformModel();
        }

        List<ReferenceDigestValue> rdvs = referenceEventManager
                .getValuesByRimId(referenceManifest);

        for (ReferenceDigestValue rdv : rdvs) {
            rdv.setModel(model);
            rdv.setManufacturer(manufacturer);
            rdv.setBaseRimId(referenceManifest.getAssociatedRim());
            referenceEventManager.updateRecord(rdv);
        }
    }

    private void saveTpmEvents(final ReferenceManifest referenceManifest) {
        SupportReferenceManifest dbSupport;
        String manufacturer;
        String model;
        if (referenceManifest instanceof SupportReferenceManifest) {
            dbSupport = (SupportReferenceManifest) referenceManifest;
        } else {
            return;
        }
        TCGEventLog logProcessor = null;
        if (dbSupport.getPlatformManufacturer() == null) {
            manufacturer = "";
        } else {
            manufacturer = dbSupport.getPlatformManufacturer();
        }

        if (dbSupport.getPlatformModel() == null) {
            model = "";
        } else {
            model = dbSupport.getPlatformModel();
        }
        try {
            logProcessor = new TCGEventLog(dbSupport.getRimBytes());
            ReferenceDigestValue rdv;
            for (TpmPcrEvent tpe : logProcessor.getEventList()) {
                rdv = new ReferenceDigestValue(dbSupport.getAssociatedRim(),
                        dbSupport.getId(), manufacturer,
                        model, tpe.getPcrIndex(),
                        tpe.getEventDigestStr(), tpe.getEventTypeStr(),
                        false, false, tpe.getEventContent());
                this.referenceEventManager.saveValue(rdv);
            }
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
