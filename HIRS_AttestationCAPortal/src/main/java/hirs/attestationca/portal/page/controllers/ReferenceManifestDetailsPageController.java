package hirs.attestationca.portal.page.controllers;

import hirs.data.persist.BaseReferenceManifest;
import hirs.data.persist.EventLogMeasurements;
import hirs.data.persist.ReferenceManifest;
import hirs.data.persist.SupportReferenceManifest;
import hirs.data.persist.SwidResource;
import hirs.data.persist.certificate.Certificate;
import hirs.data.persist.certificate.CertificateAuthorityCredential;
import hirs.persist.CertificateManager;
import hirs.persist.DBManagerException;
import hirs.persist.ReferenceManifestManager;
import hirs.tpm.eventlog.TCGEventLog;
import hirs.attestationca.portal.page.Page;
import hirs.attestationca.portal.page.PageController;
import hirs.attestationca.portal.page.PageMessages;
import hirs.attestationca.portal.page.params.ReferenceManifestDetailsPageParams;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import hirs.tpm.eventlog.TpmPcrEvent;
import hirs.utils.ReferenceManifestValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller for the Reference Manifest Details page.
 */
@Controller
@RequestMapping("/rim-details")
public class ReferenceManifestDetailsPageController
        extends PageController<ReferenceManifestDetailsPageParams> {

    private final ReferenceManifestManager referenceManifestManager;
    private final CertificateManager certificateManager;
    private static final ReferenceManifestValidator RIM_VALIDATOR
            = new ReferenceManifestValidator();
    private static final Logger LOGGER
            = LogManager.getLogger(ReferenceManifestDetailsPageController.class);

    /**
     * Constructor providing the Page's display and routing specification.
     *
     * @param referenceManifestManager the reference manifest manager.
     * @param certificateManager        the certificate manager.
     */
    @Autowired
    public ReferenceManifestDetailsPageController(
            final ReferenceManifestManager referenceManifestManager,
            final CertificateManager certificateManager) {
        super(Page.RIM_DETAILS);
        this.referenceManifestManager = referenceManifestManager;
        this.certificateManager = certificateManager;
    }

    /**
     * Returns the filePath for the view and the data model for the page.
     *
     * @param params The object to map url parameters into.
     * @param model  The data model for the request. Can contain data from
     *               redirect.
     * @return the path for the view and data model for the page.
     */
    @Override
    public ModelAndView initPage(final ReferenceManifestDetailsPageParams params,
                                 final Model model) {
        // get the basic information to render the page
        ModelAndView mav = getBaseModelAndView();
        PageMessages messages = new PageMessages();

        // Map with the rim information
        HashMap<String, Object> data = new HashMap<>();

        // Check if parameters were set
        if (params.getId() == null) {
            String typeError = "ID was not provided";
            messages.addError(typeError);
            LOGGER.error(typeError);
            mav.addObject(MESSAGES_ATTRIBUTE, messages);
        } else {
            try {
                UUID uuid = UUID.fromString(params.getId());
                data.putAll(getRimDetailInfo(uuid, referenceManifestManager, certificateManager));
            } catch (IllegalArgumentException iaEx) {
                String uuidError = "Failed to parse ID from: " + params.getId();
                messages.addError(uuidError);
                LOGGER.error(uuidError, iaEx);
            } catch (Exception ioEx) {
                LOGGER.error(ioEx);
            }
            if (data.isEmpty()) {
                String notFoundMessage = "Unable to find RIM with ID: " + params.getId();
                messages.addError(notFoundMessage);
                LOGGER.warn(notFoundMessage);
                mav.addObject(MESSAGES_ATTRIBUTE, messages);
            } else {
                mav.addObject(INITIAL_DATA, data);
            }
        }

        // return the model and view
        return mav;
    }

    /**
     * This method takes the place of an entire class for a string builder.
     * Gathers all information and returns it for displays.
     *
     * @param uuid                     database reference for the requested RIM.
     * @param referenceManifestManager the reference manifest manager.
     * @param certificateManager        the certificate manager.
     * @return mapping of the RIM information from the database.
     * @throws java.io.IOException      error for reading file bytes.
     * @throws NoSuchAlgorithmException If an unknown Algorithm is encountered.
     * @throws CertificateException     if a certificate doesn't parse.
     */
    public static HashMap<String, Object> getRimDetailInfo(final UUID uuid,
             final ReferenceManifestManager referenceManifestManager,
             final CertificateManager certificateManager) throws IOException,
            CertificateException, NoSuchAlgorithmException {
        HashMap<String, Object> data = new HashMap<>();

        BaseReferenceManifest bRim = BaseReferenceManifest.select(referenceManifestManager)
                .byEntityId(uuid).getRIM();

        if (bRim != null) {
            data.putAll(getBaseRimInfo(bRim, referenceManifestManager, certificateManager));
        }

        SupportReferenceManifest sRim = SupportReferenceManifest.select(referenceManifestManager)
                .byEntityId(uuid).getRIM();

        if (sRim != null) {
            data.putAll(getSupportRimInfo(sRim, referenceManifestManager));
        }

        EventLogMeasurements bios = EventLogMeasurements.select(referenceManifestManager)
                .byEntityId(uuid).getRIM();

        if (bios != null) {
            data.putAll(getMeasurementsRimInfo(bios, referenceManifestManager));
        }

        return data;
    }

    /**
     * This method takes the place of an entire class for a string builder.
     * Gathers all information and returns it for displays.
     *
     * @param baseRim established ReferenceManifest Type.
     * @param referenceManifestManager the reference manifest manager.
     * @param certificateManager        the certificate manager.
     * @return mapping of the RIM information from the database.
     * @throws java.io.IOException      error for reading file bytes.
     * @throws NoSuchAlgorithmException If an unknown Algorithm is encountered.
     * @throws CertificateException     if a certificate doesn't parse.
     */
    private static HashMap<String, Object> getBaseRimInfo(
            final BaseReferenceManifest baseRim,
            final ReferenceManifestManager referenceManifestManager,
            final CertificateManager certificateManager)
            throws IOException, CertificateException, NoSuchAlgorithmException {
        HashMap<String, Object> data = new HashMap<>();

        // Software Identity
        data.put("swidName", baseRim.getSwidName());
        data.put("swidVersion", baseRim.getSwidVersion());
        data.put("swidTagVersion", baseRim.getSwidTagVersion());
        if (baseRim.isSwidCorpus() == 1) {
            data.put("swidCorpus", "True");
        } else {
            data.put("swidCorpus", "False");
        }
        if (baseRim.isSwidPatch() == 1) {
            data.put("swidPatch", "True");
        } else {
            data.put("swidPatch", "False");
        }
        if (baseRim.isSwidSupplemental() == 1) {
            data.put("swidSupplemental", "True");
        } else {
            data.put("swidSupplemental", "False");
        }
        data.put("swidTagId", baseRim.getTagId());
        // Entity
        data.put("entityName", baseRim.getEntityName());
        data.put("entityRegId", baseRim.getEntityRegId());
        data.put("entityRole", baseRim.getEntityRole());
        data.put("entityThumbprint", baseRim.getEntityThumbprint());
        // Link
        data.put("linkHref", baseRim.getLinkHref());
        data.put("linkRel", baseRim.getLinkRel());
        data.put("supportRimId", "");
        data.put("supportRimTagId", "");
        data.put("platformManufacturer", baseRim.getPlatformManufacturer());
        data.put("platformManufacturerId", baseRim.getPlatformManufacturerId());
        data.put("platformModel", baseRim.getPlatformModel());
        data.put("platformVersion", baseRim.getPlatformVersion());
        data.put("payloadType", baseRim.getPayloadType());
        data.put("colloquialVersion", baseRim.getColloquialVersion());
        data.put("edition", baseRim.getEdition());
        data.put("product", baseRim.getProduct());
        data.put("revision", baseRim.getRevision());
        data.put("bindingSpec", baseRim.getBindingSpec());
        data.put("bindingSpecVersion", baseRim.getBindingSpecVersion());
        data.put("pcUriGlobal", baseRim.getPcURIGlobal());
        data.put("pcUriLocal", baseRim.getPcURILocal());
        data.put("rimLinkHash", baseRim.getRimLinkHash());
        data.put("rimType", baseRim.getRimType());

        List<SwidResource> resources = baseRim.parseResource();
        TCGEventLog logProcessor = null;
        ReferenceManifest support = null;

        if (baseRim.getAssociatedRim() == null) {
            support = SupportReferenceManifest.select(referenceManifestManager)
                    .byManufacturer(baseRim.getPlatformManufacturer())
                    .getRIM();
            if (support != null) {
                baseRim.setAssociatedRim(support.getId());
                logProcessor = new TCGEventLog(support.getRimBytes());
            }
        }
        // going to have to pull the filename and grab that from the DB
        // to get the id to make the link
        for (SwidResource swidRes : resources) {
            if (support != null && swidRes.getName()
                    .equals(support.getFileName())) {
                RIM_VALIDATOR.validateSupportRimHash(support.getRimBytes(),
                        swidRes.getHashValue());
                if (RIM_VALIDATOR.isSupportRimValid()) {
                    data.put("supportRimHashValid", true);
                } else {
                    data.put("supportRimHashValid", false);
                }
                swidRes.setPcrValues(Arrays.asList(
                        logProcessor.getExpectedPCRValues()));
                break;
            } else {
                swidRes.setPcrValues(new ArrayList<>());
            }
        }

        data.put("associatedRim", baseRim.getAssociatedRim());
        data.put("swidFiles", resources);

        RIM_VALIDATOR.validateXmlSignature(new ByteArrayInputStream(baseRim.getRimBytes()));
        data.put("signatureValid", RIM_VALIDATOR.isSignatureValid());
        if (RIM_VALIDATOR.isSignatureValid()) {
            LOGGER.info("Public key: " + RIM_VALIDATOR.getPublicKey().toString());
            try {
                Certificate certificate =
                        CertificateAuthorityCredential.select(certificateManager)
                                .byEncodedPublicKey(RIM_VALIDATOR.getPublicKey().getEncoded())
                                .getCertificate();
                data.put("issuerID", certificate.getId().toString());
            } catch (NullPointerException e) {
                LOGGER.info("Unable to get signing certificate link: " + e.getMessage());
            }
        }
        return data;
    }

    /**
     * This method takes the place of an entire class for a string builder.
     * Gathers all information and returns it for displays.
     *
     * @param support established ReferenceManifest Type.
     * @param referenceManifestManager the reference manifest manager.
     * @return mapping of the RIM information from the database.
     * @throws java.io.IOException      error for reading file bytes.
     * @throws NoSuchAlgorithmException If an unknown Algorithm is encountered.
     * @throws CertificateException     if a certificate doesn't parse.
     */
    private static HashMap<String, Object> getSupportRimInfo(
            final SupportReferenceManifest support,
            final ReferenceManifestManager referenceManifestManager)
            throws IOException, CertificateException, NoSuchAlgorithmException {
        HashMap<String, Object> data = new HashMap<>();

        if (support.getAssociatedRim() == null) {
            ReferenceManifest baseRim = BaseReferenceManifest.select(referenceManifestManager)
                    .byManufacturer(support.getPlatformManufacturer()).getRIM();
            if (baseRim != null) {
                support.setAssociatedRim(baseRim.getId());
                try {
                    referenceManifestManager.update(support);
                } catch (DBManagerException ex) {
                    LOGGER.error("Failed to update Support RIM", ex);
                }
            }
        }
        data.put("baseRim", support.getTagId());
        data.put("associatedRim", support.getAssociatedRim());
        data.put("rimType", support.getRimType());
        data.put("tagId", support.getTagId());

        TCGEventLog logProcessor = new TCGEventLog(support.getRimBytes());
        EventLogMeasurements measurements = EventLogMeasurements.select(referenceManifestManager)
                .byManufacturer(support.getPlatformManufacturer()).getRIM();

        LinkedList<TpmPcrEvent> tpmPcrEvents = new LinkedList<>();
        TCGEventLog measurementsProcess;
        if (measurements != null) {
            measurementsProcess = new TCGEventLog((measurements.getRimBytes()));
            for (TpmPcrEvent tpe : logProcessor.getEventList()) {
                if (!tpe.eventCompare(
                        measurementsProcess.getEventByNumber(
                                tpe.getEventNumber()))) {
                    tpe.setError(true);
                }
                tpmPcrEvents.add(tpe);
            }
        }

        data.put("events", tpmPcrEvents);

        return data;
    }

    /**
     * This method takes the place of an entire class for a string builder.
     * Gathers all information and returns it for displays.
     *
     * @param measurements established ReferenceManifest Type.
     * @param referenceManifestManager the reference manifest manager.
     * @return mapping of the RIM information from the database.
     * @throws java.io.IOException      error for reading file bytes.
     * @throws NoSuchAlgorithmException If an unknown Algorithm is encountered.
     * @throws CertificateException     if a certificate doesn't parse.
     */
    private static HashMap<String, Object> getMeasurementsRimInfo(
            final EventLogMeasurements measurements,
            final ReferenceManifestManager referenceManifestManager)
            throws IOException, CertificateException, NoSuchAlgorithmException {
        HashMap<String, Object> data = new HashMap<>();
        LinkedList<TpmPcrEvent> supportEvents = new LinkedList<>();
        LinkedList<TpmPcrEvent> livelogEvents = new LinkedList<>();

        data.put("supportFilename", "Blank");
        data.put("supportId", "");
        data.put("tagId", measurements.getTagId());
        data.put("baseId", "");

        data.put("rimType", measurements.getRimType());
        TCGEventLog supportLog = null;
        SupportReferenceManifest support = SupportReferenceManifest
                .select(referenceManifestManager)
                .byManufacturer(measurements
                        .getPlatformManufacturer()).getRIM();

        if (support != null) {
            supportLog = new TCGEventLog(support.getRimBytes());
            data.put("supportFilename", support.getFileName());
            data.put("supportId", support.getId());
        }

        BaseReferenceManifest base = BaseReferenceManifest
                .select(referenceManifestManager)
                .byManufacturer(measurements
                        .getPlatformManufacturer()).getRIM();

        if (base != null) {
            data.put("baseId", base.getId());
        }

        TCGEventLog measurementLog = new TCGEventLog(measurements.getRimBytes());
        if (supportLog != null) {
            TpmPcrEvent measurementEvent;
            for (TpmPcrEvent tpe : supportLog.getEventList()) {
                measurementEvent = measurementLog.getEventByNumber(tpe.getEventNumber());
                if (!tpe.eventCompare(measurementEvent)) {
                    supportEvents.add(tpe);
                    livelogEvents.add(measurementEvent);
                }
            }
        }

        data.put("supportEvents", supportEvents);
        data.put("livelogEvents", livelogEvents);

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            LOGGER.error(String.format("%s -> %s", entry.getKey(),
                    String.valueOf(entry.getValue())));
        }
        return data;
    }
}
