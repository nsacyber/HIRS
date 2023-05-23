package hirs.attestationca.portal.page.controllers;

import hirs.attestationca.persist.CriteriaModifier;
import hirs.attestationca.persist.DBManagerException;
import hirs.attestationca.persist.FilteredRecordsList;
import hirs.attestationca.persist.entity.manager.ReferenceDigestValueRepository;
import hirs.attestationca.persist.entity.manager.ReferenceManifestRepository;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.rim.BaseReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.rim.ReferenceDigestValue;
import hirs.attestationca.persist.entity.userdefined.rim.SupportReferenceManifest;
import hirs.attestationca.portal.datatables.DataTableInput;
import hirs.attestationca.portal.datatables.DataTableResponse;
import hirs.attestationca.portal.datatables.OrderedListQueryDataTableAdapter;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.PageMessages;
import hirs.attestationca.portal.page.params.NoPageParams;
import hirs.utils.SwidResource;
import hirs.utils.tpm.eventlog.TCGEventLog;
import hirs.utils.tpm.eventlog.TpmPcrEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.validation.Valid;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.hibernate.Session;
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

import java.io.IOException;
import java.lang.ref.Reference;
import java.net.URISyntaxException;
import java.security.MessageDigest;
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

/**
 * Controller for the Reference Manifest page.
 */
@Log4j2
@Controller
@MultipartConfig
@RequestMapping("/reference-manifests")
public class ReferenceManifestPageController extends PageController<NoPageParams> {

    private static final String LOG_FILE_PATTERN = "([^\\s]+(\\.(?i)(rimpcr|rimel|bin|log))$)";

    @Autowired(required = false)
    private EntityManager entityManager;

    private final ReferenceManifestRepository referenceManifestRepository;
    private final ReferenceDigestValueRepository referenceDigestValueRepository;
//    private final ReferenceManifestServiceImpl referenceManifestManager;
//    private final ReferenceDigestValueServiceImpl referenceEventManager;

    /**
     * Constructor providing the Page's display and routing specification.
     *
     * @param referenceManifestRepository the reference manifest manager
     * @param referenceDigestValueRepository this is the reference event manager
     */
    @Autowired
    public ReferenceManifestPageController(final ReferenceManifestRepository referenceManifestRepository,
            final ReferenceDigestValueRepository referenceDigestValueRepository
//            final ReferenceManifestServiceImpl referenceManifestManager,
//            final ReferenceDigestValueServiceImpl referenceEventManager
    ) {
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
                null,
                input, orderColumnName, criteriaModifier);

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
            @RequestParam("files") final MultipartFile[] files,
            final RedirectAttributes attr) throws URISyntaxException, Exception {
        Map<String, Object> model = new HashMap<>();
        PageMessages messages = new PageMessages();
        String fileName;
        Pattern logPattern = Pattern.compile(LOG_FILE_PATTERN);
        Matcher matcher;
        boolean supportRIM = false;
        List<BaseReferenceManifest> baseRims = new ArrayList<>();
        List<SupportReferenceManifest> supportRims = new ArrayList<>();

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
            storeManifest(messages, rim, false);
        });
        supportRims.stream().forEach((rim) -> {
            log.info(String.format("Storing event log %s", rim.getFileName()));
            storeManifest(messages, rim, true);
        });

        // Prep a map to associated the swidtag payload hash to the swidtag.
        // pass it in to update support rims that either were uploaded
        // or already exist
        // create a map of the supports rims in case an uploaded swidtag
        // isn't one to one with the uploaded support rims.
        Map<String, SupportReferenceManifest> updatedSupportRims
                = updateSupportRimInfo(generatePayloadHashMap(baseRims));

        // look for missing uploaded support rims
//        for (SupportReferenceManifest support : supportRims) {
//            if (!updatedSupportRims.containsKey(support.getHexDecHash())) {
//                // Make sure we are getting the db version of the file
//                updatedSupportRims.put(support.getHexDecHash(),
//                        SupportReferenceManifest
//                                .select(referenceManifestManager)
//                                .byHexDecHash(support.getHexDecHash())
//                                .getRIM());
//            }
//        }

        // pass in the updated support rims
        // and either update or add the events
        processTpmEvents(new ArrayList<SupportReferenceManifest>(updatedSupportRims.values()));

        //Add messages to the model
        model.put(MESSAGES_ATTRIBUTE, messages);

        return redirectTo(Page.REFERENCE_MANIFESTS,
                new NoPageParams(), model, attr);
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
                supportRims.add(new SupportReferenceManifest(fileName, fileBytes));
            } else {
                baseRims.add(new BaseReferenceManifest(fileName, fileBytes));
            }
        } catch (IOException ioEx) {
            final String failMessage
                    = String.format("Failed to parse uploaded file (%s): ", fileName);
            log.error(failMessage, ioEx);
            messages.addError(failMessage + ioEx.getMessage());
        }
    }

    /**
     * Stores the {@link ReferenceManifest} objects.
     *
     * @param messages message object for user display of statuses
     * @param referenceManifest the object to store
     * @param supportRim boolean flag indicating if this is a support RIM
     * process.
     */
    private void storeManifest(
            final PageMessages messages,
            final ReferenceManifest referenceManifest,
            final boolean supportRim) {

        ReferenceManifest existingManifest = null;
        String fileName = referenceManifest.getFileName();
        MessageDigest digest = null;
        String rimHash = "";
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException noSaEx) {
            log.error(noSaEx);
        }

        // look for existing manifest in the database
        try {
            if (supportRim) {
                if (digest != null) {
                    rimHash = Hex.encodeHexString(
                            digest.digest(referenceManifest.getRimBytes()));
                }
                existingManifest = referenceManifestRepository.findByHash(rimHash, ReferenceManifest.SUPPORT_RIM);
//                SupportReferenceManifest
//                        .select(referenceManifestManager)
//                        .byHexDecHash(rimHash)
//                        .includeArchived()
//                        .getRIM();
            } else {
                if (digest != null) {
                    rimHash = Base64.encodeBase64String(
                            digest.digest(referenceManifest.getRimBytes()));
                }
                existingManifest = referenceManifestRepository.findByHash(rimHash, ReferenceManifest.BASE_RIM);
//                BaseReferenceManifest
//                        .select(referenceManifestManager).byBase64Hash(rimHash)
//                        .includeArchived()
//                        .getRIM();
            }
        } catch (DBManagerException dbMEx) {
            final String failMessage = String.format("Querying for existing certificate "
                    + "failed (%s): ", fileName);
            messages.addError(failMessage + dbMEx.getMessage());
            log.error(failMessage, dbMEx);
        }

        try {
            // save the new certificate if no match is found
            if (existingManifest == null) {
                referenceManifestRepository.save(referenceManifest);

                final String successMsg = String.format("RIM successfully uploaded (%s): ",
                        fileName);
                messages.addSuccess(successMsg);
                log.info(successMsg);
            }
        } catch (DBManagerException dbmEx) {
            final String failMessage = String.format("Storing RIM failed (%s): ",
                    fileName);
            messages.addError(failMessage + dbmEx.getMessage());
            log.error(failMessage, dbmEx);
        }

        try {
            // if an identical RIM is archived, update the existing RIM to
            // unarchive it and change the creation date
            if (existingManifest != null && existingManifest.isArchived()) {
                existingManifest.restore();
                existingManifest.resetCreateTime();
                referenceManifestRepository.save(existingManifest);

                final String successMsg
                        = String.format("Pre-existing RIM found and unarchived (%s): ", fileName);
                messages.addSuccess(successMsg);
                log.info(successMsg);
            }
        } catch (DBManagerException dbmEx) {
            final String failMessage = String.format("Found an identical pre-existing RIM in the "
                    + "archive, but failed to unarchive it (%s): ", fileName);
            messages.addError(failMessage + dbmEx.getMessage());
            log.error(failMessage, dbmEx);
        }
    }

    private Map<String, BaseReferenceManifest> generatePayloadHashMap(
            final List<BaseReferenceManifest> uploadedBaseRims) {
        BaseReferenceManifest dbBaseRim;
        HashMap<String, BaseReferenceManifest> tempMap = new HashMap<>();
        for (BaseReferenceManifest base : uploadedBaseRims) {
            // this is done to make sure we have the version with the UUID
            dbBaseRim = (BaseReferenceManifest) referenceManifestRepository.findByHash(base.getBase64Hash(), ReferenceManifest.BASE_RIM);
//            BaseReferenceManifest.select(referenceManifestManager)
//                    .byBase64Hash(base.getBase64Hash()).getRIM();
            if (dbBaseRim != null) {
                for (SwidResource swid : dbBaseRim.parseResource()) {
                    tempMap.put(swid.getHashValue(), dbBaseRim);
                }
            }
        }

        return tempMap;
    }

    private Map<String, SupportReferenceManifest> updateSupportRimInfo(
            final Map<String, BaseReferenceManifest> dbBaseRims) {
        BaseReferenceManifest dbBaseRim;
        SupportReferenceManifest supportRim;
        Map<String, SupportReferenceManifest> updatedSupportRims = new HashMap<>();
        List<String> hashValues = new LinkedList<>(dbBaseRims.keySet());
        for (String supportHash : hashValues) {
            supportRim = (SupportReferenceManifest) referenceManifestRepository.findByHash(supportHash, ReferenceManifest.SUPPORT_RIM);
//            SupportReferenceManifest.select(referenceManifestManager)
//                    .byHexDecHash(supportHash).getRIM();
            // I have to assume the baseRim is from the database
            // Updating the id values, manufacturer, model
            if (supportRim != null && !supportRim.isUpdated()) {
                dbBaseRim = dbBaseRims.get(supportHash);
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
            List<BaseReferenceManifest> baseRims = this.referenceManifestRepository.getBaseByManufacturerModel(supportRim.getPlatformManufacturer(), supportRim.getPlatformModel());
//            Set<BaseReferenceManifest> baseRims = BaseReferenceManifest
//                    .select(referenceManifestManager)
//                    .byManufacturerModel(supportRim.getPlatformManufacturer(),
//                            supportRim.getPlatformModel()).getRIMs();

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

        for (SupportReferenceManifest dbSupport : dbSupportRims) {
            // So first we'll have to pull values based on support rim
            // get by support rim id NEXT

            if (dbSupport.getPlatformManufacturer() != null) {
                tpmEvents = referenceDigestValueRepository.getValuesBySupportRimId(dbSupport.getAssociatedRim());
                baseRim = findBaseRim(dbSupport);
                if (tpmEvents.isEmpty()) {
                    ReferenceDigestValue rdv;
                    try {
                        logProcessor = new TCGEventLog(dbSupport.getRimBytes());
                        for (TpmPcrEvent tpe : logProcessor.getEventList()) {
                            rdv = new ReferenceDigestValue(baseRim.getId(),
                                    dbSupport.getId(), dbSupport.getPlatformManufacturer(),
                                    dbSupport.getPlatformModel(), tpe.getPcrIndex(),
                                    tpe.getEventDigestStr(), tpe.getEventTypeStr(),
                                    false, false, true, tpe.getEventContent());

                            this.referenceDigestValueRepository.save(rdv);
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
