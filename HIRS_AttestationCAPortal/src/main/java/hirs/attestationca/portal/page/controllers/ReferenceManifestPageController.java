package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.DBManagerException;
import hirs.attestationca.persist.FilteredRecordsList;
import hirs.attestationca.persist.entity.manager.ReferenceDigestValueRepository;
import hirs.attestationca.persist.entity.manager.ReferenceManifestRepository;
import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.rim.BaseReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.rim.ReferenceDigestValue;
import hirs.attestationca.persist.entity.userdefined.rim.SupportReferenceManifest;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.PageMessages;
import hirs.attestationca.portal.page.params.NoPageParams;
import hirs.utils.tpm.eventlog.TCGEventLog;
import hirs.utils.tpm.eventlog.TpmPcrEvent;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Controller for the Reference Manifest page.
 */
@Log4j2
@Controller
@RequestMapping("/HIRS_AttestationCAPortal/portal/reference-manifests")
public class ReferenceManifestPageController extends PageController<NoPageParams> {

    private static final String LOG_FILE_PATTERN = "([^\\s]+(\\.(?i)(rimpcr|rimel|bin|log))$)";

    @Autowired(required = false)
    private EntityManager entityManager;

    private final ReferenceManifestRepository referenceManifestRepository;
    private final ReferenceDigestValueRepository referenceDigestValueRepository;

    /**
     * Constructor providing the Page's display and routing specification.
     *
     * @param referenceManifestRepository the reference manifest manager
     * @param referenceDigestValueRepository this is the reference event manager
     */
    @Autowired
    public ReferenceManifestPageController(final ReferenceManifestRepository referenceManifestRepository,
            final ReferenceDigestValueRepository referenceDigestValueRepository) {
        super(Page.REFERENCE_MANIFESTS);
        this.referenceManifestRepository = referenceManifestRepository;
        this.referenceDigestValueRepository = referenceDigestValueRepository;
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
        log.debug("Handling request for summary list: " + input);

        String orderColumnName = input.getOrderColumnName();
        log.info("Ordering on column: " + orderColumnName);

        log.info("Querying with the following dataTableInput: " + input.toString());

        FilteredRecordsList<ReferenceManifest> records = new FilteredRecordsList<>();
        int itemCount = 0;
        int currentPage = input.getStart() / input.getLength();
        Pageable paging = PageRequest.of(currentPage, input.getLength(), Sort.by(orderColumnName));
        org.springframework.data.domain.Page<ReferenceManifest> pagedResult = referenceManifestRepository.findAll(paging);

        if (pagedResult.hasContent()) {
            for (ReferenceManifest manifest : pagedResult.getContent()) {
                if (!manifest.getRimType().equals(ReferenceManifest.MEASUREMENT_RIM)) {
                    records.add(manifest);
                    itemCount++;
                }
            }
        }
        records.setRecordsTotal(referenceManifestRepository.count());
        records.setRecordsFiltered(itemCount);

        log.debug("Returning list of size: " + records.size());
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
        List<BaseReferenceManifest> baseRims = new ArrayList<>();
        List<SupportReferenceManifest> supportRims = new ArrayList<>();
        log.info(String.format("Processing %s uploaded files", files.length));

        // loop through the files
        for (MultipartFile file : files) {
            fileName = file.getOriginalFilename();
            matcher = logPattern.matcher(fileName);
            supportRIM = matcher.matches();

            //Parse reference manifests
            parseRIM(file, supportRIM, messages, baseRims, supportRims);
        }
        baseRims.stream().forEach((rim) -> {
            log.info(String.format("Storing swidtag %s", rim.getFileName()));
            this.referenceManifestRepository.save(rim);
        });
        supportRims.stream().forEach((rim) -> {
            log.info(String.format("Storing event log %s", rim.getFileName()));
            this.referenceManifestRepository.save(rim);
        });

        // Prep a map to associated the swidtag payload hash to the swidtag.
        // pass it in to update support rims that either were uploaded
        // or already exist
        // create a map of the supports rims in case an uploaded swidtag
        // isn't one to one with the uploaded support rims.
        Map<String, SupportReferenceManifest> updatedSupportRims
                = updateSupportRimInfo(referenceManifestRepository.findAllSupportRims());

        // pass in the updated support rims
        // and either update or add the events
        processTpmEvents(new ArrayList<SupportReferenceManifest>(updatedSupportRims.values()));

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
        log.info("Handling request to delete " + id);

        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();

        try {
            ReferenceManifest referenceManifest = getRimFromDb(id);
            List<ReferenceDigestValue> values = new LinkedList<>();

            if (referenceManifest == null) {
                String notFoundMessage = "Unable to locate RIM with ID: " + id;
                messages.addError(notFoundMessage);
                log.warn(notFoundMessage);
            } else {
                // if support rim, update associated events
                values = referenceDigestValueRepository.findBySupportRimHash(
                        referenceManifest.getHexDecHash());

                for (ReferenceDigestValue value : values) {
                    referenceDigestValueRepository.delete(value);
                }

                referenceManifestRepository.delete(referenceManifest);
                String deleteCompletedMessage = "RIM successfully deleted";
                messages.addInfo(deleteCompletedMessage);
                log.info(deleteCompletedMessage);
            }
        } catch (IllegalArgumentException iaEx) {
            String uuidError = "Failed to parse ID from: " + id;
            messages.addError(uuidError);
            log.error(uuidError, iaEx);
        } catch (DBManagerException dbmEx) {
            String dbError = "Failed to archive cert: " + id;
            messages.addError(dbError);
            log.error(dbError, dbmEx);
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
        log.info("Handling RIM request to download " + id);

        try {
            ReferenceManifest referenceManifest = getRimFromDb(id);

            if (referenceManifest == null) {
                String notFoundMessage = "Unable to locate RIM with ID: " + id;
                log.warn(notFoundMessage);
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
            log.error(uuidError, ex);
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
        log.info("Handling request to download all Reference Integrity Manifests");
        String fileName = "rims.zip";
        String zipFileName;

        // Set filename for download.
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        response.setContentType("application/zip");

        List<ReferenceManifest> referenceManifestList = new LinkedList<>();
        for (ReferenceManifest rim : referenceManifestRepository.findAll()) {
            if ((rim instanceof BaseReferenceManifest)
                    || (rim instanceof SupportReferenceManifest)) {
                referenceManifestList.add(rim);
            }
        }

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
            log.error(uuidError, ex);
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
//        ReferenceManifest rim = BaseReferenceManifest.select(referenceManifestManager)
//                .byEntityId(uuid).getRIM();
//
//        if (rim == null) {
//            rim = SupportReferenceManifest.select(referenceManifestManager)
//                    .byEntityId(uuid).getRIM();
//        }
//
//        if (rim == null) {
//            rim = EventLogMeasurements.select(referenceManifestManager)
//                    .byEntityId(uuid).getRIM();
//        }

        return this.referenceManifestRepository.getReferenceById(uuid);
    }

    /**
     * Takes the rim files provided and returns a {@link ReferenceManifest}
     * object.
     *
     * @param file the provide user file via browser.
     * @param supportRIM matcher result
     * @param messages the object that handles displaying information to the
     * user.
     * @param baseRims object to store multiple files
     * @param supportRims object to store multiple files
     * @return a single or collection of reference manifest files.
     */
    private void parseRIM(
            final MultipartFile file, final boolean supportRIM,
            final PageMessages messages, final List<BaseReferenceManifest> baseRims,
            final List<SupportReferenceManifest> supportRims) {

        byte[] fileBytes = new byte[0];
        String fileName = file.getOriginalFilename();
        BaseReferenceManifest baseRim;
        SupportReferenceManifest supportRim;

        // build the manifest from the uploaded bytes
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            final String failMessage
                    = String.format("Failed to read uploaded file (%s): ", fileName);
            log.error(failMessage, e);
            messages.addError(failMessage + e.getMessage());
        }

        try {
            if (supportRIM) {
                supportRim = new SupportReferenceManifest(fileName, fileBytes);
                if (referenceManifestRepository.findByHexDecHashAndRimType(supportRim.getHexDecHash(),
                        supportRim.getRimType()) == null) {
                    supportRims.add(supportRim);
                    messages.addInfo("Saved Reference Manifest " + fileName);
                }
            } else {
                baseRim = new BaseReferenceManifest(fileName, fileBytes);
                if (referenceManifestRepository.findByHexDecHashAndRimType(baseRim.getHexDecHash(),
                        baseRim.getRimType()) == null) {
                    baseRims.add(baseRim);
                }
            }
        } catch (IOException ioEx) {
            final String failMessage
                    = String.format("Failed to parse uploaded file (%s): ", fileName);
            log.error(failMessage, ioEx);
            messages.addError(failMessage + ioEx.getMessage());
        }
    }

    private Map<String, SupportReferenceManifest> updateSupportRimInfo(
            final List<SupportReferenceManifest> dbSupportRims) {
        SupportReferenceManifest supportRim;
        String fileString;
        Map<String, SupportReferenceManifest> updatedSupportRims = new HashMap<>();
        Map<String, SupportReferenceManifest> hashValues = new HashMap<>();
        for (SupportReferenceManifest support : dbSupportRims) {
            hashValues.put(support.getHexDecHash(), support);
        }

        for (BaseReferenceManifest dbBaseRim : referenceManifestRepository.findAllBaseRims()) {
            for (String supportHash : hashValues.keySet()) {
                fileString = new String(dbBaseRim.getRimBytes(), StandardCharsets.UTF_8);

                if (fileString.contains(supportHash)) {
                    supportRim = hashValues.get(supportHash);
                    // I have to assume the baseRim is from the database
                    // Updating the id values, manufacturer, model
                    if (supportRim != null && !supportRim.isUpdated()) {
                        supportRim.setSwidTagVersion(dbBaseRim.getSwidTagVersion());
                        supportRim.setPlatformManufacturer(dbBaseRim.getPlatformManufacturer());
                        supportRim.setPlatformModel(dbBaseRim.getPlatformModel());
                        supportRim.setTagId(dbBaseRim.getTagId());
                        supportRim.setAssociatedRim(dbBaseRim.getId());
                        supportRim.setUpdated(true);
                        referenceManifestRepository.save(supportRim);
                        updatedSupportRims.put(supportHash, supportRim);
                    }
                }
            }
        }

        return updatedSupportRims;
    }

    /**
     * If the support rim is a supplemental or base, this method looks for the
     * original oem base rim to associate with each event.
     * @param supportRim assumed db object
     * @return reference to the base rim
     */
    private ReferenceManifest findBaseRim(final SupportReferenceManifest supportRim) {
        if (supportRim != null && (supportRim.getId() != null
                && !supportRim.getId().toString().equals(""))) {
            List<BaseReferenceManifest> baseRims = new LinkedList<>();
            baseRims.add(this.referenceManifestRepository
                    .getBaseByManufacturerModel(supportRim.getPlatformManufacturer(),
                            supportRim.getPlatformModel()));

            for (BaseReferenceManifest base : baseRims) {
                if (base.isBase()) {
                    // there should be only one
                    return base;
                }
            }
        }
        return null;
    }

    private void processTpmEvents(final List<SupportReferenceManifest> dbSupportRims) {
        List<ReferenceDigestValue> tpmEvents;
        TCGEventLog logProcessor = null;
        ReferenceManifest baseRim;
        ReferenceDigestValue newRdv;

        for (SupportReferenceManifest dbSupport : dbSupportRims) {
            // So first we'll have to pull values based on support rim
            // get by support rim id NEXT
            if (dbSupport.getPlatformManufacturer() != null) {
                tpmEvents = referenceDigestValueRepository.findBySupportRimId(dbSupport.getId());
                baseRim = findBaseRim(dbSupport);
                if (tpmEvents.isEmpty()) {
                    try {
                        logProcessor = new TCGEventLog(dbSupport.getRimBytes());
                        for (TpmPcrEvent tpe : logProcessor.getEventList()) {
                            newRdv = new ReferenceDigestValue(baseRim.getId(),
                                    dbSupport.getId(), dbSupport.getPlatformManufacturer(),
                                    dbSupport.getPlatformModel(), tpe.getPcrIndex(),
                                    tpe.getEventDigestStr(), dbSupport.getHexDecHash(),
                                    tpe.getEventTypeStr(),false, false,
                                    true, tpe.getEventContent());

                            this.referenceDigestValueRepository.save(newRdv);
                        }
                    } catch (CertificateException e) {
                        e.printStackTrace();
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    for (ReferenceDigestValue rdv : tpmEvents) {
                        if (!rdv.isUpdated()) {
                            rdv.updateInfo(dbSupport, baseRim.getId());
                            this.referenceDigestValueRepository.save(rdv);
                        }
                    }
                }
            }
        }
    }
}
