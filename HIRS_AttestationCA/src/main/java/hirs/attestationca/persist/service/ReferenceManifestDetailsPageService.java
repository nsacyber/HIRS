package hirs.attestationca.persist.service;

import hirs.attestationca.persist.DBServiceException;
import hirs.attestationca.persist.entity.manager.CACredentialRepository;
import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.ReferenceDigestValueRepository;
import hirs.attestationca.persist.entity.manager.ReferenceManifestRepository;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.certificate.CertificateAuthorityCredential;
import hirs.attestationca.persist.entity.userdefined.rim.BaseReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.rim.EventLogMeasurements;
import hirs.attestationca.persist.entity.userdefined.rim.ReferenceDigestValue;
import hirs.attestationca.persist.entity.userdefined.rim.SupportReferenceManifest;
import hirs.attestationca.persist.validation.SupplyChainCredentialValidator;
import hirs.attestationca.persist.validation.SupplyChainValidatorException;
import hirs.utils.SwidResource;
import hirs.utils.rim.ReferenceManifestValidator;
import hirs.utils.tpm.eventlog.TCGEventLog;
import hirs.utils.tpm.eventlog.TpmPcrEvent;
import hirs.utils.tpm.eventlog.events.EvConstants;
import hirs.utils.tpm.eventlog.uefi.UefiConstants;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A service layer class responsible for encapsulating all business logic related to the Reference Manifest Details
 * Page.
 */
@Log4j2
@Service
public class ReferenceManifestDetailsPageService {
    private final ReferenceManifestRepository referenceManifestRepository;
    private final ReferenceDigestValueRepository referenceDigestValueRepository;
    private final CertificateRepository certificateRepository;
    private final CACredentialRepository caCertificateRepository;

    /**
     * Constructor for the Reference Manifest Details Page Service.
     *
     * @param referenceManifestRepository    reference manifest repository
     * @param referenceDigestValueRepository reference digest value repository
     * @param certificateRepository          certificate repository
     * @param caCertificateRepository        certificate authority credential repository
     */
    @Autowired
    public ReferenceManifestDetailsPageService(final ReferenceManifestRepository referenceManifestRepository,
                                               final ReferenceDigestValueRepository referenceDigestValueRepository,
                                               final CertificateRepository certificateRepository,
                                               final CACredentialRepository caCertificateRepository) {
        this.referenceManifestRepository = referenceManifestRepository;
        this.referenceDigestValueRepository = referenceDigestValueRepository;
        this.certificateRepository = certificateRepository;
        this.caCertificateRepository = caCertificateRepository;
    }

    /**
     * This method takes the place of an entire class for a string builder.
     * Gathers all information and returns it for displays.
     *
     * @param uuid database reference for the requested RIM.
     * @return mapping of the RIM information from the database.
     * @throws java.io.IOException      error for reading file bytes.
     * @throws NoSuchAlgorithmException If an unknown Algorithm is encountered.
     * @throws CertificateException     if a certificate doesn't parse.
     */
    public HashMap<String, Object> getRimDetailInfo(final UUID uuid) throws IOException, CertificateException,
            NoSuchAlgorithmException {
        HashMap<String, Object> data = new HashMap<>();

        BaseReferenceManifest bRim = this.referenceManifestRepository.getBaseRimEntityById(uuid);

        if (bRim != null) {
            data.putAll(getBaseRimInfo(bRim));
        }

        SupportReferenceManifest sRim = this.referenceManifestRepository.getSupportRimEntityById(uuid);

        if (sRim != null) {
            data.putAll(getSupportRimInfo(sRim));
        }

        EventLogMeasurements bios = this.referenceManifestRepository.getEventLogRimEntityById(uuid);

        if (bios != null) {
            data.putAll(getMeasurementsRimInfo(bios));
        }

        return data;
    }

    private void getEventSummary(final HashMap<String, Object> data,
                                 final Collection<TpmPcrEvent> eventList) {
        boolean crtm = false;
        boolean bootManager = false;
        boolean osLoader = false;
        boolean osKernel = false;
        boolean acpiTables = false;
        boolean smbiosTables = false;
        boolean gptTable = false;
        boolean bootOrder = false;
        boolean defaultBootDevice = false;
        boolean secureBoot = false;
        boolean pk = false;
        boolean kek = false;
        boolean sigDb = false;
        boolean forbiddenDbx = false;

        String contentStr;
        for (TpmPcrEvent tpe : eventList) {
            contentStr = tpe.getEventContentStr();
            // check for specific events
            if (contentStr.contains("CRTM")
                || contentStr.contains("IBB")) {
                crtm = true;
            } else if (contentStr.contains("shimx64.efi")
                    || contentStr.contains("bootmgfw.efi")) {
                bootManager = true;
            } else if (contentStr.contains("grubx64.efi")
                    || contentStr.contains("winload.efi")) {
                osLoader = true;
            } else if (contentStr.contains("vmlinuz")
                    || contentStr.contains("ntoskrnl.exe")) {
                osKernel = true;
            } else if (contentStr.contains("ACPI")) {
                acpiTables = true;
            } else if (contentStr.contains("SMBIOS")) {
                smbiosTables = true;
            } else if (contentStr.contains("GPT")) {
                gptTable = true;
            } else if (contentStr.contains("BootOrder")) {
                bootOrder = true;
            } else if (contentStr.contains(UefiConstants.UEFI_VARIABLE_LABEL + " PK")) {
                pk = true;
            } else if (contentStr.contains(UefiConstants.UEFI_VARIABLE_LABEL + " KEK")) {
                kek = true;
            } else if (contentStr.contains(UefiConstants.UEFI_VARIABLE_LABEL + " db")) {
                if (contentStr.contains("dbx")) {
                    forbiddenDbx = true;
                } else {
                    sigDb = true;
                }
            } else if (contentStr.contains("Secure Boot is enabled")) {
                secureBoot = true;
            }
        }

        data.put("crtm", crtm);
        data.put("bootManager", bootManager);
        data.put("osLoader", osLoader);
        data.put("osKernel", osKernel);
        data.put("acpiTables", acpiTables);
        data.put("smbiosTables", smbiosTables);
        data.put("gptTable", gptTable);
        data.put("bootOrder", bootOrder);
        data.put("defaultBootDevice", defaultBootDevice);
        data.put("secureBoot", secureBoot);
        data.put("pk", pk);
        data.put("kek", kek);
        data.put("sigDb", sigDb);
        data.put("forbiddenDbx", forbiddenDbx);
    }

    /**
     * This method takes the place of an entire class for a string builder.
     * Gathers all information and returns it for displays.
     *
     * @param baseRim established ReferenceManifest Type.
     * @return mapping of the RIM information from the database.
     * @throws java.io.IOException error for reading file bytes.
     */
    private HashMap<String, Object> getBaseRimInfo(final BaseReferenceManifest baseRim) throws IOException {
        HashMap<String, Object> data = new HashMap<>();

        // Software Identity
        data.put("swidName", baseRim.getSwidName());
        data.put("swidVersion", baseRim.getSwidVersion());
        data.put("swidTagVersion", baseRim.getSwidTagVersion());

        if (baseRim.getSwidCorpus() == 1) {
            data.put("swidCorpus", "True");
        } else {
            data.put("swidCorpus", "False");
        }

        if (baseRim.isSwidPatch()) {
            data.put("swidPatch", "True");
        } else {
            data.put("swidPatch", "False");
        }

        if (baseRim.isSwidSupplemental()) {
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
        data.put("linkHrefLink", "");

        List<BaseReferenceManifest> baseReferenceManifests =
                this.referenceManifestRepository.findAllBaseRims();

        for (BaseReferenceManifest bRim : baseReferenceManifests) {
            if (baseRim.getLinkHref().contains(bRim.getTagId())) {
                data.put("linkHrefLink", bRim.getId());
            }
        }

        data.put("linkRel", baseRim.getLinkRel());
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
        if (baseRim.getRimLinkHash() != null) {
            ReferenceManifest rim = this.referenceManifestRepository.findByHexDecHashAndRimType(
                    baseRim.getRimLinkHash(), ReferenceManifest.BASE_RIM);
            if (rim != null) {
                data.put("rimLinkId", rim.getId());
                data.put("linkHashValid", true);
            } else {
                data.put("linkHashValid", false);
            }
        }
        data.put("rimType", baseRim.getRimType());

        List<SwidResource> resources = baseRim.getFileResources();
        SupportReferenceManifest support = null;

        ReferenceManifestValidator referenceManifestValidator = new ReferenceManifestValidator();

        // going to have to pull the filename and grab that from the DB
        // to get the id to make the link
        referenceManifestValidator.setRim(baseRim.getRimBytes());
        for (SwidResource swidRes : resources) {
            support = (SupportReferenceManifest) this.referenceManifestRepository.findByHexDecHashAndRimType(
                    swidRes.getHashValue(), ReferenceManifest.SUPPORT_RIM);

            if (support != null && swidRes.getHashValue().equalsIgnoreCase(support.getHexDecHash())) {
                baseRim.setAssociatedRim(support.getId());
                referenceManifestValidator.validateSupportRimHash(support.getRimBytes(),
                        swidRes.getHashValue());
                if (referenceManifestValidator.isSupportRimValid()) {
                    data.put("supportRimHashValid", true);
                } else {
                    data.put("supportRimHashValid", false);
                }
                break;
            }
        }

        data.put("associatedRim", baseRim.getAssociatedRim());
        data.put("swidFiles", resources);
        if (support != null && (!baseRim.isSwidSupplemental()
                && !baseRim.isSwidPatch())) {
            data.put("pcrList", support.getExpectedPCRList());
        }

        List<Certificate> certificates = certificateRepository.findByType("CertificateAuthorityCredential");

        CertificateAuthorityCredential caCert;

        //Report invalid signature unless referenceManifestValidator validates it and cert path is valid
        data.put("signatureValid", false);

        for (Certificate certificate : certificates) {
            caCert = (CertificateAuthorityCredential) certificate;
            KeyStore keystore = ValidationService.getCaChain(caCert, caCertificateRepository);
            try {
                List<X509Certificate> truststore =
                        convertCACsToX509Certificates(ValidationService.getCaChainRec(caCert,
                                Collections.emptySet(),
                                caCertificateRepository));
                referenceManifestValidator.setTrustStore(truststore);
            } catch (IOException e) {
                log.error("Error building CA chain for {}: {}", caCert.getSubjectKeyIdentifier(),
                        e.getMessage());
            }

            if (referenceManifestValidator.validateXmlSignature(caCert.getX509Certificate().getPublicKey(),
                    caCert.getSubjectKeyIdString())) {
                try {
                    if (SupplyChainCredentialValidator.verifyCertificate(
                            caCert.getX509Certificate(), keystore)) {
                        data.replace("signatureValid", true);
                        break;
                    }
                } catch (SupplyChainValidatorException scvEx) {
                    log.error("Error verifying cert chain: {}", scvEx.getMessage());
                }
            }
        }

        data.put("skID", referenceManifestValidator.getSubjectKeyIdentifier());
        try {
            if (referenceManifestValidator.getPublicKey() != null) {
                for (Certificate certificate : certificates) {
                    caCert = (CertificateAuthorityCredential) certificate;
                    if (Arrays.equals(caCert.getEncodedPublicKey(),
                            referenceManifestValidator.getPublicKey().getEncoded())) {
                        data.put("issuerID", caCert.getId().toString());
                    }
                }
            }
        } catch (Exception npEx) {
            log.warn("Unable to link signing certificate: {}", npEx.getMessage());
        }

        return data;
    }

    /**
     * This method takes the place of an entire class for a string builder.
     * Gathers all information and returns it for displays.
     *
     * @param supportReferenceManifest established ReferenceManifest Type.
     * @return mapping of the RIM information from the database.
     * @throws java.io.IOException      error for reading file bytes.
     * @throws NoSuchAlgorithmException If an unknown Algorithm is encountered.
     * @throws CertificateException     if a certificate doesn't parse.
     */
    private HashMap<String, Object> getSupportRimInfo(final SupportReferenceManifest supportReferenceManifest)
            throws IOException, CertificateException, NoSuchAlgorithmException {
        HashMap<String, Object> data = new HashMap<>();
        EventLogMeasurements measurements;

        if (supportReferenceManifest.getAssociatedRim() == null) {
            List<BaseReferenceManifest> baseRims = this.referenceManifestRepository.findAllBaseRims();

            for (BaseReferenceManifest baseRim : baseRims) {
                if (baseRim != null && baseRim.getAssociatedRim() != null
                        && baseRim.getAssociatedRim().equals(supportReferenceManifest.getId())) {
                    supportReferenceManifest.setAssociatedRim(baseRim.getId());
                    try {
                        this.referenceManifestRepository.save(supportReferenceManifest);
                    } catch (DBServiceException ex) {
                        log.error("Failed to update Support RIM", ex);
                    }
                    break;
                }
            }
        }

        // testing this independent of the above if statement because the above
        // starts off checking if associated rim is null; that is irrelevant for
        // this statement.
        measurements =
                (EventLogMeasurements) this.referenceManifestRepository.findByHexDecHashAndRimTypeUnarchived(
                        supportReferenceManifest.getHexDecHash(),
                        ReferenceManifest.MEASUREMENT_RIM);

        if (supportReferenceManifest.isSwidPatch()) {
            data.put("swidPatch", "True");
        } else {
            data.put("swidPatch", "False");
        }

        if (supportReferenceManifest.isSwidSupplemental()) {
            data.put("swidSupplemental", "True");
        } else {
            data.put("swidSupplemental", "False");
        }

        data.put("swidBase", (!supportReferenceManifest.isSwidPatch()
                && !supportReferenceManifest.isSwidSupplemental()));
        data.put("baseRim", supportReferenceManifest.getTagId());
        data.put("associatedRim", supportReferenceManifest.getAssociatedRim());
        data.put("rimType", supportReferenceManifest.getRimType());
        data.put("tagId", supportReferenceManifest.getTagId());

        TCGEventLog logProcessor = new TCGEventLog(supportReferenceManifest.getRimBytes());
        LinkedList<TpmPcrEvent> tpmPcrEvents = new LinkedList<>();
        TCGEventLog measurementsProcess;
        if (measurements != null) {
            measurementsProcess = new TCGEventLog((measurements.getRimBytes()));
            HashMap<String, TpmPcrEvent> digestMap = new HashMap<>();
            for (TpmPcrEvent tpe : logProcessor.getEventList()) {
                digestMap.put(tpe.getEventDigestStr(), tpe);
                if (!supportReferenceManifest.isSwidSupplemental()
                        && !tpe.eventCompare(
                        measurementsProcess.getEventByNumber(
                                tpe.getEventNumber()))) {
                    tpe.setError(true);
                }
                tpmPcrEvents.add(tpe);
            }
            for (TpmPcrEvent tpe : logProcessor.getEventList()) {
                tpe.setError(!digestMap.containsKey(tpe.getEventDigestStr()));
            }
            data.put("events", tpmPcrEvents);
        } else {
            data.put("events", logProcessor.getEventList());
        }

        getEventSummary(data, logProcessor.getEventList());
        return data;
    }

    /**
     * This method takes the place of an entire class for a string builder.
     * Gathers all information and returns it for displays.
     *
     * @param measurements established ReferenceManifest Type.
     * @return mapping of the RIM information from the database.
     * @throws java.io.IOException      error for reading file bytes.
     * @throws NoSuchAlgorithmException If an unknown Algorithm is encountered.
     * @throws CertificateException     if a certificate doesn't parse.
     */
    private HashMap<String, Object> getMeasurementsRimInfo(final EventLogMeasurements measurements)
            throws IOException, CertificateException, NoSuchAlgorithmException {
        HashMap<String, Object> data = new HashMap<>();
        LinkedList<TpmPcrEvent> unmatchedAttestationEvents = new LinkedList<>();
        BaseReferenceManifest base;
        List<SupportReferenceManifest> supports = new ArrayList<>();
        SupportReferenceManifest baseSupport = null;

        data.put("supportFilename", "Blank");
        data.put("supportId", "");
        data.put("associatedRim", "");
        data.put("rimType", measurements.getRimType());
        data.put("hostName", measurements.getDeviceName());
        data.put("validationResult", measurements.getOverallValidationResult());
        data.put("swidBase", true);

        List<ReferenceDigestValue> assertions = new LinkedList<>();
        if (measurements.getDeviceName() != null) {
            supports.addAll(this.referenceManifestRepository.getSupportByManufacturerModel(
                    measurements.getPlatformManufacturer(), measurements.getPlatformModel()));
            for (SupportReferenceManifest support : supports) {
                if (support.isBaseSupport()) {
                    baseSupport = support;
                }
            }

            if (baseSupport != null) {
                data.put("supportFilename", baseSupport.getFileName());
                data.put("supportId", baseSupport.getId());
                data.put("tagId", baseSupport.getTagId());

                base = this.referenceManifestRepository.getBaseRimEntityById(baseSupport.getAssociatedRim());
                if (base != null) {
                    data.put("associatedRim", base.getId());
                }

                assertions.addAll(
                        this.referenceDigestValueRepository.findBySupportRimId(baseSupport.getId()));
            }
        }

        TCGEventLog measurementLog = new TCGEventLog(measurements.getRimBytes());
        Map<String, ReferenceDigestValue> referenceValueMap = new HashMap<>();

        for (ReferenceDigestValue record : assertions) {
            referenceValueMap.put(record.getDigestValue(), record);
        }
        for (TpmPcrEvent attestationEvent : measurementLog.getEventList()) {
            if (!referenceValueMap.containsKey(attestationEvent.getEventDigestStr())) {
                unmatchedAttestationEvents.add(attestationEvent);
            }
        }

        if (!supports.isEmpty()) {
            Map<String, List<TpmPcrEvent>> baselineLogEvents = new HashMap<>();
            List<TpmPcrEvent> matchedEvents = null;
            List<TpmPcrEvent> referenceEventValues = new LinkedList<>();
            for (SupportReferenceManifest support : supports) {
                referenceEventValues.addAll(support.getEventLog());
            }
            String bootVariable;
            Pattern variableName = Pattern.compile("Variable Name: (\\w+)");
            Matcher matcher;

            for (TpmPcrEvent attestationEvent : unmatchedAttestationEvents) {
                matchedEvents = new ArrayList<>();
                for (TpmPcrEvent referenceEvent : referenceEventValues) {
                    if ((referenceEvent.getEventType() == attestationEvent.getEventType())
                            && (referenceEvent.getPcrIndex() == attestationEvent.getPcrIndex())) {
                        if (eventIsType(attestationEvent.getEventType())) {
                            matcher = variableName.matcher(attestationEvent.getEventContentStr());
                            if (matcher.find()) {
                                log.debug("Event variable name: {}", matcher.group(1));
                                bootVariable = matcher.group(1);
                                if (referenceEvent.getEventContentStr().contains(bootVariable)) {
                                    matchedEvents.add(referenceEvent);
                                }
                            }
                        } else {
                            matchedEvents.add(referenceEvent);
                        }
                    }
                }
                baselineLogEvents.put(attestationEvent.getEventDigestStr(), matchedEvents);
            }
            data.put("eventTypeMap", baselineLogEvents);
        }

        TCGEventLog logProcessor = new TCGEventLog(measurements.getRimBytes());
        data.put("livelogEvents", unmatchedAttestationEvents);
        data.put("events", logProcessor.getEventList());
        getEventSummary(data, logProcessor.getEventList());

        return data;
    }


    /**
     * This method checks if the given event is of the below event types.
     *
     * @param eventType to check for event type
     * @return true if the below types are matched, otherwise false
     */
    private boolean eventIsType(final long eventType) {
        return eventType == EvConstants.EV_EFI_VARIABLE_AUTHORITY
                || eventType == EvConstants.EV_EFI_VARIABLE_BOOT
                || eventType == EvConstants.EV_EFI_VARIABLE_DRIVER_CONFIG
                || eventType == EvConstants.EV_EFI_SPDM_DEVICE_AUTHORITY
                || eventType == EvConstants.EV_EFI_SPDM_DEVICE_POLICY;
    }

    /**
     * This method converts a Set of CertificateAuthorityCredentials to a List of X509Certificates.
     *
     * @param set of CACs to convert
     * @return list of X509Certificates
     */
    private List<X509Certificate> convertCACsToX509Certificates(final Set<CertificateAuthorityCredential> set)
            throws IOException {
        List<X509Certificate> certs = new ArrayList<>(set.size());
        for (CertificateAuthorityCredential cac : set) {
            certs.add(cac.getX509Certificate());
        }
        return certs;
    }

}
