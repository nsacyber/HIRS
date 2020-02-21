package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;

import hirs.FilteredRecordsList;
import hirs.attestationca.portal.datatables.OrderedListQueryDataTableAdapter;
import hirs.attestationca.portal.page.PageMessages;
import hirs.attestationca.portal.page.params.NoPageParams;
import hirs.persist.DBManagerException;
import hirs.persist.ReferenceManifestManager;
import hirs.persist.CriteriaModifier;
import hirs.data.persist.ReferenceManifest;
import hirs.data.persist.certificate.Certificate;
import java.io.IOException;
import java.net.URISyntaxException;

//import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.http.HttpServletResponse;

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
        extends PageController<NoPageParams> {

    private static final String BIOS_RELEASE_DATE_FORMAT = "yyyy-MM-dd";

    private final BiosDateValidator biosValidator;
    private final ReferenceManifestManager referenceManifestManager;
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
     * @param model The data model for the request. Can contain data from
     * redirect.
     * @return the path for the view and data model for the page.
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
        List<MultipartFile> rims = new ArrayList<>();
        String fileName;
        String uploadDirStr = System.getProperty("catalina.base")
                        + "/webapps/HIRS_AttestationCAPortal/upload/";
        Path pathDir = Paths.get(uploadDirStr);
        Path path;

        for (MultipartFile file : files) {
            fileName = file.getOriginalFilename();
            if (fileName.toLowerCase().endsWith("swidtag")) {
                rims.add(file);
            } else {
                path = Paths.get(uploadDirStr + file.getOriginalFilename());
                if (Files.notExists(pathDir)) {
                    Files.createDirectory(pathDir);
                }
                if (Files.notExists(path)) {
                    Files.createFile(path);
                }

                Files.write(path, file.getBytes());

                String uploadCompletedMessage = String.format(
                        "%s successfully uploaded", file.getOriginalFilename());
                messages.addSuccess(uploadCompletedMessage);
                LOGGER.info(uploadCompletedMessage);
            }
        }

        for (MultipartFile file : rims) {
            //Parse reference manifests
            ReferenceManifest rim = parseRIM(file, messages);

            //Store only if it was parsed
            if (rim != null) {
                storeManifest(file.getOriginalFilename(),
                        messages,
                        rim,
                        referenceManifestManager);
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
                fileName.append(referenceManifest.getSwidName());
                fileName.append("_[");
                fileName.append(referenceManifest.getRimHash());
                fileName.append("]");
                fileName.append(".swidTag\"");

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
     * This method takes the parameter and looks for this information in the
     * Database.
     *
     * @param id of the RIM
     * @return the associated RIM from the DB
     * @throws IllegalArgumentException
     */
    private ReferenceManifest getRimFromDb(final String id) throws IllegalArgumentException {
        UUID uuid = UUID.fromString(id);

        return ReferenceManifest
                .select(referenceManifestManager)
                .byEntityId(uuid).getRIM();
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
     * @param referenceManifestManager the class that handles the storage
     * process.
     */
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
            final String failMessage = String.format("Querying for existing certificate "
                    + "failed (%s): ", fileName);
            messages.addError(failMessage + e.getMessage());
            LOGGER.error(failMessage, e);
            return;
        }

        try {
            // save the new certificate if no match is found
            if (existingManifest == null) {
                referenceManifestManager.save(referenceManifest);

                final String successMsg = String.format("New RIM successfully uploaded (%s): ",
                        fileName);
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
            }
        } catch (DBManagerException dbmEx) {
            final String failMessage = String.format("Found an identical pre-existing RIM in the "
                    + "archive, but failed to unarchive it (%s): ", fileName);
            messages.addError(failMessage + dbmEx.getMessage());
            LOGGER.error(failMessage, dbmEx);
        }
    }

//    private void unarchiveZip(final MultipartFile zipUpload) {
////        byte[] buffer = new byte[Integer.SIZE * Integer.SIZE];
//        ZipInputStream zis;
//        FileOutputStream fos;
//        ZipFile zipFile = null;
//        String uploadDirStr = "/etc/hirs/upload/";
//
//        try {
//            File uploadDirFile = new File(uploadDirStr);
//            if (uploadDirFile.mkdir()) {
//                LOGGER.error("FUSTA - Directory created");
//                Path path = Paths.get(uploadDirStr + zipUpload.getOriginalFilename());
//                Files.write(path, zipUpload.getBytes());
//
//                if (Files.exists(path)) {
//                    LOGGER.error(path.getFileName());
//                    zipFile = new ZipFile(new File(path.toUri()));
//                }
//            }
//
//            if (zipFile != null) {
//                Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
//                while (zipEntries.hasMoreElements()) {
//                    ZipEntry zipEntry = zipEntries.nextElement();
//                    LOGGER.error(zipEntry.getName());
//                    try (InputStream is = zipFile.getInputStream(zipEntry);) {
//                        String fileName = zipEntry.getName().toLowerCase();
//                        if (fileName.endsWith("swidtag")) {
//                            // parse as tag
//                            int i = 1 + 2;
//                            System.out.print(i);
//                        }
//                    }
//                }
//                zipFile.close();
//            }
//
//        } catch (FileNotFoundException fnfEx) {
//            LOGGER.error(fnfEx);
//        } catch (IOException ioEx) {
//            LOGGER.error(ioEx);
//        }
//
//    }
}
