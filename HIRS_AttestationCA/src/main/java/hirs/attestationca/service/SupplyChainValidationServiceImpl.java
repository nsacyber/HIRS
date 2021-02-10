package hirs.attestationca.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import hirs.data.persist.BaseReferenceManifest;
import hirs.data.persist.EventLogMeasurements;
import hirs.data.persist.SupportReferenceManifest;
import hirs.data.persist.SwidResource;
import hirs.data.persist.TPMMeasurementRecord;
import hirs.data.persist.PCRPolicy;
import hirs.data.persist.ArchivableEntity;
import hirs.tpm.eventlog.TCGEventLog;
import hirs.tpm.eventlog.TpmPcrEvent;
import hirs.utils.BouncyCastleUtils;
import hirs.utils.ReferenceManifestValidator;
import hirs.validation.SupplyChainCredentialValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.LinkedList;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import hirs.appraiser.Appraiser;
import hirs.appraiser.SupplyChainAppraiser;
import hirs.data.persist.AppraisalStatus;
import hirs.data.persist.Device;
import hirs.data.persist.DeviceInfoReport;
import hirs.data.persist.SupplyChainPolicy;
import hirs.data.persist.SupplyChainValidation;
import hirs.data.persist.SupplyChainValidationSummary;
import hirs.data.persist.certificate.Certificate;
import hirs.data.persist.certificate.CertificateAuthorityCredential;
import hirs.data.persist.certificate.EndorsementCredential;
import hirs.data.persist.certificate.PlatformCredential;
import hirs.data.persist.ReferenceManifest;
import hirs.persist.AppraiserManager;
import hirs.persist.CertificateManager;
import hirs.persist.ReferenceManifestManager;
import hirs.persist.CrudManager;
import hirs.persist.DBManagerException;
import hirs.persist.PersistenceConfiguration;
import hirs.persist.PolicyManager;
import hirs.validation.CredentialValidator;

import java.util.HashMap;
import java.util.Map;

import static hirs.data.persist.AppraisalStatus.Status.FAIL;
import static hirs.data.persist.AppraisalStatus.Status.PASS;

/**
 * The main executor of supply chain verification tasks. The
 * AbstractAttestationCertificateAuthority will feed it the PC, EC, other
 * relevant certificates, and serial numbers of the provisioning task, and it
 * will then manipulate the data as necessary, retrieve useful certs, and
 * arrange for actual validation by the SupplyChainValidator.
 */
@Service
@Import(PersistenceConfiguration.class)
public class SupplyChainValidationServiceImpl implements SupplyChainValidationService {

    private PolicyManager policyManager;
    private AppraiserManager appraiserManager;
    private ReferenceManifestManager referenceManifestManager;
    private CertificateManager certificateManager;
    private CredentialValidator supplyChainCredentialValidator;
    private CrudManager<SupplyChainValidationSummary> supplyChainValidatorSummaryManager;

    private static final Logger LOGGER
            = LogManager.getLogger(SupplyChainValidationServiceImpl.class);

    /**
     * Constructor.
     *
     * @param policyManager                      the policy manager
     * @param appraiserManager                   the appraiser manager
     * @param certificateManager                 the cert manager
     * @param referenceManifestManager           the RIM manager
     * @param supplyChainValidatorSummaryManager the summary manager
     * @param supplyChainCredentialValidator     the credential validator
     */
    @Autowired
    public SupplyChainValidationServiceImpl(
            final PolicyManager policyManager, final AppraiserManager appraiserManager,
            final CertificateManager certificateManager,
            final ReferenceManifestManager referenceManifestManager,
            final CrudManager<SupplyChainValidationSummary> supplyChainValidatorSummaryManager,
            final CredentialValidator supplyChainCredentialValidator) {
        this.policyManager = policyManager;
        this.appraiserManager = appraiserManager;
        this.certificateManager = certificateManager;
        this.referenceManifestManager = referenceManifestManager;
        this.supplyChainValidatorSummaryManager = supplyChainValidatorSummaryManager;
        this.supplyChainCredentialValidator = supplyChainCredentialValidator;
    }

    /**
     * The "main" method of supply chain validation. Takes the credentials from
     * an identity request and validates the supply chain in accordance to the
     * current supply chain policy.
     *
     * @param ec     The endorsement credential from the identity request.
     * @param pcs    The platform credentials from the identity request.
     * @param device The device to be validated.
     * @return A summary of the validation results.
     */
    @Override
    @SuppressWarnings("methodlength")
    public SupplyChainValidationSummary validateSupplyChain(final EndorsementCredential ec,
                                                            final Set<PlatformCredential> pcs,
                                                            final Device device) {
        final Appraiser supplyChainAppraiser = appraiserManager.getAppraiser(
                SupplyChainAppraiser.NAME);
        SupplyChainPolicy policy = (SupplyChainPolicy) policyManager.getDefaultPolicy(
                supplyChainAppraiser);
        boolean acceptExpiredCerts = policy.isExpiredCertificateValidationEnabled();
        PlatformCredential baseCredential = null;
        SupplyChainValidation platformScv = null;
        SupplyChainValidation basePlatformScv = null;
        boolean chkDeltas = false;
        String pcErrorMessage = "";
        List<SupplyChainValidation> validations = new LinkedList<>();
        Map<PlatformCredential, SupplyChainValidation> deltaMapping = new HashMap<>();
        SupplyChainValidation.ValidationType platformType = SupplyChainValidation
                .ValidationType.PLATFORM_CREDENTIAL;
        LOGGER.info("Validating supply chain.");

        // Validate the Endorsement Credential
        if (policy.isEcValidationEnabled()) {
            validations.add(validateEndorsementCredential(ec, acceptExpiredCerts));
            // store the device with the credential
            if (ec != null) {
                ec.setDevice(device);
                this.certificateManager.update(ec);
            }
        }

        // Validate Platform Credential signatures
        if (policy.isPcValidationEnabled()) {
            // Ensure there are platform credentials to validate
            if (pcs == null || pcs.isEmpty()) {
                LOGGER.error("There were no Platform Credentials to validate.");
                pcErrorMessage = "Platform credential(s) missing\n";
            } else {
                for (PlatformCredential pc : pcs) {
                    KeyStore trustedCa = getCaChain(pc);
                    platformScv = validatePlatformCredential(
                            pc, trustedCa, acceptExpiredCerts);

                    if (platformScv.getResult() == FAIL) {
                        pcErrorMessage = String.format("%s%s%n", pcErrorMessage,
                                platformScv.getMessage());
                    }
                    // set the base credential
                    if (pc.isBase()) {
                        baseCredential = pc;
                        basePlatformScv = platformScv;
                    } else {
                        chkDeltas = true;
                        deltaMapping.put(pc, null);
                    }
                    pc.setDevice(device);
                    this.certificateManager.update(pc);

                }

                // check that the delta certificates validity date is after
                // the base
                if (baseCredential != null) {
                    for (PlatformCredential pc : pcs) {
                        int result = baseCredential.getBeginValidity()
                                .compareTo(pc.getBeginValidity());
                        if (!pc.isBase() && (result > 0)) {
                            pcErrorMessage = String.format("%s%s%n", pcErrorMessage,
                                    "Delta Certificate's validity "
                                            + "date is not after Base");
                            break;
                        }
                    }
                } else {
                    // we don't have a base cert, fail
                    pcErrorMessage = String.format("%s%s%n", pcErrorMessage,
                            "Base Platform credential missing");
                }
            }

            if (pcErrorMessage.isEmpty()) {
                validations.add(platformScv);
            } else {
                validations.add(new SupplyChainValidation(platformType,
                        AppraisalStatus.Status.FAIL, new ArrayList<>(pcs), pcErrorMessage));
            }
        }

        // Validate Platform Credential attributes
        if (policy.isPcAttributeValidationEnabled()
                && pcErrorMessage.isEmpty()) {
            // Ensure there are platform credentials to validate
            SupplyChainValidation attributeScv = null;
            String attrErrorMessage = "";
            List<ArchivableEntity> aes = new ArrayList<>();
            // need to check if there are deltas, if not then just verify
            // components of the base
            if (baseCredential == null) {
                validations.add(buildValidationRecord(
                        SupplyChainValidation.ValidationType.PLATFORM_CREDENTIAL,
                        AppraisalStatus.Status.FAIL,
                        "Base Platform credential missing."
                                + " Cannot validate attributes",
                        null, Level.ERROR));
            } else {
                if (chkDeltas) {
                    aes.addAll(basePlatformScv.getCertificatesUsed());
                    Iterator<PlatformCredential> it = pcs.iterator();
                    while (it.hasNext()) {
                        PlatformCredential pc = it.next();
                        if (pc != null) {
                            if (!pc.isBase()) {
                                attributeScv = validateDeltaPlatformCredentialAttributes(
                                        pc, device.getDeviceInfo(),
                                        baseCredential, deltaMapping);
                                if (attributeScv.getResult() == FAIL) {
                                    attrErrorMessage = String.format("%s%s%n", attrErrorMessage,
                                            attributeScv.getMessage());
                                }
                            }
                        }
                    }
                } else {
                    aes.add(baseCredential);
                    validations.remove(platformScv);
                    // if there are no deltas, just check base credential
                    platformScv = validatePlatformCredentialAttributes(
                            baseCredential, device.getDeviceInfo(), ec);
                    validations.add(new SupplyChainValidation(
                            SupplyChainValidation.ValidationType.PLATFORM_CREDENTIAL,
                            platformScv.getResult(), aes, platformScv.getMessage()));
                }
            }
            if (!attrErrorMessage.isEmpty()) {
                //combine platform and platform attributes
                validations.remove(platformScv);
                validations.add(new SupplyChainValidation(
                        SupplyChainValidation.ValidationType.PLATFORM_CREDENTIAL,
                        attributeScv.getResult(), aes, attributeScv.getMessage()));

            }
        }

        if (policy.isFirmwareValidationEnabled()) {
            // may need to associated with device to pull the correct info
            // compare tpm quote with what is pulled from RIM associated file
            validations.add(validateFirmware(device, policy.getPcrPolicy()));
        }

        LOGGER.info("The service finished and now summarizing");
        // Generate validation summary, save it, and return it.
        SupplyChainValidationSummary summary
                = new SupplyChainValidationSummary(device, validations);
        try {
            supplyChainValidatorSummaryManager.save(summary);
        } catch (DBManagerException ex) {
            LOGGER.error("Failed to save Supply Chain summary", ex);
        }

        return summary;
    }

    /**
     * This method is a sub set of the validate supply chain method and focuses
     * on the specific multibase validation check for a delta chain. This method
     * also includes the check for delta certificate CA validation as well.
     *
     * @param pc          The platform credential getting checked
     * @param platformScv The validation record
     * @return The validation record
     */
    private SupplyChainValidation validatePcPolicy(
            final PlatformCredential pc,
            final SupplyChainValidation platformScv,
            final Map<PlatformCredential, SupplyChainValidation> deltaMapping,
            final boolean acceptExpiredCerts) {
        SupplyChainValidation subPlatformScv = platformScv;

        if (pc != null) {
            // if not checked, update the map
            boolean result = checkForMultipleBaseCredentials(
                    pc.getPlatformSerial());
            // if it is, then update the SupplyChainValidation message and result
            if (result) {
                String message = "Multiple Base certificates found in chain.";
                if (!platformScv.getResult().equals(PASS)) {
                    message = String.format("%s,%n%s", platformScv.getMessage(), message);
                }
                subPlatformScv = buildValidationRecord(
                        SupplyChainValidation.ValidationType.PLATFORM_CREDENTIAL,
                        AppraisalStatus.Status.FAIL,
                        message, pc, Level.ERROR);
            }

            // only do check if this is a base certificate
            if (pc.isBase()) {
                // Grab all certs associated with this platform chain
                List<PlatformCredential> chainCertificates = PlatformCredential
                        .select(certificateManager)
                        .byBoardSerialNumber(pc.getPlatformSerial())
                        .getCertificates().stream().collect(Collectors.toList());

                SupplyChainValidation deltaScv;
                KeyStore trustedCa;
                // verify that the deltas trust chain is valid.
                for (PlatformCredential delta : chainCertificates) {
                    if (delta != null && !delta.isBase()) {
                        trustedCa = getCaChain(delta);
                        deltaScv = validatePlatformCredential(
                                delta, trustedCa, acceptExpiredCerts);
                        deltaMapping.put(delta, deltaScv);
                    }
                }
            }
        }
        return subPlatformScv;
    }

    @SuppressWarnings("methodlength")
    private SupplyChainValidation validateFirmware(final Device device,
                                                   final PCRPolicy pcrPolicy) {
        boolean passed = true;
        String[] baseline = new String[Integer.SIZE];
        Level level = Level.ERROR;
        AppraisalStatus fwStatus = null;
        String manufacturer = device.getDeviceInfo()
                .getHardwareInfo().getManufacturer();
        ReferenceManifest validationObject = null;
        ReferenceManifest baseReferenceManifest = null;
        ReferenceManifest supportReferenceManifest = null;
        ReferenceManifest measurement = null;

        baseReferenceManifest = BaseReferenceManifest.select(referenceManifestManager)
                .byManufacturer(manufacturer).getRIM();
        supportReferenceManifest = SupportReferenceManifest.select(referenceManifestManager)
                .byManufacturer(manufacturer).getRIM();
        measurement = EventLogMeasurements.select(referenceManifestManager)
                .byManufacturer(manufacturer).includeArchived().getRIM();

        validationObject = baseReferenceManifest;
        String failedString = "";
        if (baseReferenceManifest == null) {
            failedString = "Base Reference Integrity Manifest\n";
            passed = false;
        }
        if (supportReferenceManifest == null) {
            failedString += "Support Reference Integrity Manifest\n";
            passed = false;
        }
        if (measurement == null) {
            failedString += "Bios measurement";
            passed = false;
        }

        if (passed) {
            List<SwidResource> resources =
                    ((BaseReferenceManifest) baseReferenceManifest).parseResource();
            fwStatus = new AppraisalStatus(PASS,
                    SupplyChainCredentialValidator.FIRMWARE_VALID);

            // verify signatures
            ReferenceManifestValidator referenceManifestValidator =
                    new ReferenceManifestValidator(
                            new ByteArrayInputStream(baseReferenceManifest.getRimBytes()));

            for (SwidResource swidRes : resources) {
                if (swidRes.getName().equals(supportReferenceManifest.getFileName())) {
                    referenceManifestValidator.validateSupportRimHash(
                            supportReferenceManifest.getRimBytes(), swidRes.getHashValue());
                }
            }

            if (!referenceManifestValidator.isSignatureValid()) {
                passed = false;
                fwStatus = new AppraisalStatus(FAIL,
                        "Firmware validation failed: Signature validation "
                                + "failed for Base RIM.");
            }

            if (passed && !referenceManifestValidator.isSupportRimValid()) {
                passed = false;
                fwStatus = new AppraisalStatus(FAIL,
                        "Firmware validation failed: Hash validation "
                                + "failed for Support RIM.");
            }

            if (passed) {
                TCGEventLog logProcessor;
                try {
                    logProcessor = new TCGEventLog(supportReferenceManifest.getRimBytes());
                    baseline = logProcessor.getExpectedPCRValues();
                } catch (CertificateException cEx) {
                    LOGGER.error(cEx);
                } catch (NoSuchAlgorithmException noSaEx) {
                    LOGGER.error(noSaEx);
                } catch (IOException ioEx) {
                    LOGGER.error(ioEx);
                }

                // part 1 of firmware validation check: PCR baseline match
                pcrPolicy.setBaselinePcrs(baseline);

                if (baseline.length > 0) {
                    String pcrContent = "";
                    pcrContent = new String(device.getDeviceInfo().getTPMInfo().getPcrValues());

                    if (pcrContent.isEmpty()) {
                        fwStatus = new AppraisalStatus(FAIL,
                                "Firmware validation failed: Client did not "
                                        + "provide pcr values.");
                        LOGGER.warn(String.format(
                                "Firmware validation failed: Client (%s) did not "
                                        + "provide pcr values.", device.getName()));
                    } else {
                        // we have a full set of PCR values
                        int algorithmLength = baseline[0].length();
                        String[] storedPcrs = buildStoredPcrs(pcrContent, algorithmLength);

                        if (storedPcrs[0].isEmpty()) {
                            // validation fail
                            fwStatus = new AppraisalStatus(FAIL,
                                    "Firmware validation failed: "
                                            + "Client provided PCR "
                                            + "values are not the same algorithm "
                                            + "as associated RIM.");
                        } else {
                            StringBuilder sb = pcrPolicy.validatePcrs(storedPcrs);
                            if (sb.length() > 0) {
                                validationObject = supportReferenceManifest;
                                level = Level.ERROR;
                                fwStatus = new AppraisalStatus(FAIL, sb.toString());
                            } else {
                                level = Level.INFO;
                            }
                        }
                        // part 2 of firmware validation check: bios measurements
                        // vs baseline tcg event log
                        // find the measurement
                        TCGEventLog tcgEventLog;
                        TCGEventLog tcgMeasurementLog;
                        LinkedList<TpmPcrEvent> tpmPcrEvents = new LinkedList<>();
                        try {
                            if (measurement.getPlatformManufacturer().equals(manufacturer)) {
                                tcgMeasurementLog = new TCGEventLog(measurement.getRimBytes());
                                tcgEventLog = new TCGEventLog(
                                        supportReferenceManifest.getRimBytes());
                                for (TpmPcrEvent tpe : tcgEventLog.getEventList()) {
                                    if (!tpe.eventCompare(
                                            tcgMeasurementLog.getEventByNumber(
                                                    tpe.getEventNumber()))) {
                                        tpmPcrEvents.add(tpe);
                                    }
                                }
                            }
                        } catch (CertificateException cEx) {
                            LOGGER.error(cEx);
                        } catch (NoSuchAlgorithmException noSaEx) {
                            LOGGER.error(noSaEx);
                        } catch (IOException ioEx) {
                            LOGGER.error(ioEx);
                        }

                        if (!tpmPcrEvents.isEmpty()) {
                            StringBuilder sb = new StringBuilder();
                            validationObject = measurement;
                            for (TpmPcrEvent tpe : tpmPcrEvents) {
                                sb.append(String.format("Event %s - %s%n",
                                        tpe.getEventNumber(),
                                        tpe.getEventTypeStr()));
                            }
                            if (fwStatus.getAppStatus().equals(FAIL)) {
                                fwStatus = new AppraisalStatus(FAIL, String.format("%s%n%s",
                                        fwStatus.getMessage(), sb.toString()));
                            } else {
                                fwStatus = new AppraisalStatus(FAIL, sb.toString());
                            }
                        }
                    }
                } else {
                    fwStatus = new AppraisalStatus(FAIL, "The RIM baseline could not be found.");
                }
            }
        } else {
            fwStatus = new AppraisalStatus(FAIL, String.format("Firmware Validation failed: "
                    + "%s for %s can not be found", failedString, manufacturer));
        }

        return buildValidationRecord(SupplyChainValidation.ValidationType.FIRMWARE,
                fwStatus.getAppStatus(), fwStatus.getMessage(), validationObject, level);
    }

    /**
     * A supplemental method that handles validating just the quote post main validation.
     *
     * @param device the associated device.
     * @return True if validation is successful, false otherwise.
     */
    @Override
    public SupplyChainValidationSummary validateQuote(final Device device) {
        final Appraiser supplyChainAppraiser = appraiserManager.getAppraiser(
                SupplyChainAppraiser.NAME);
        SupplyChainPolicy policy = (SupplyChainPolicy) policyManager.getDefaultPolicy(
                supplyChainAppraiser);
        SupplyChainValidation quoteScv = null;
        SupplyChainValidationSummary summary = null;
        Level level = Level.ERROR;
        AppraisalStatus fwStatus = new AppraisalStatus(FAIL,
                SupplyChainCredentialValidator.FIRMWARE_VALID);
        SupportReferenceManifest sRim = null;

        // check if the policy is enabled
        if (policy.isFirmwareValidationEnabled()) {
            String[] baseline = new String[Integer.SIZE];
            String manufacturer = device.getDeviceInfo()
                    .getHardwareInfo().getManufacturer();

            try {
                sRim = SupportReferenceManifest.select(
                        this.referenceManifestManager)
                        .byManufacturer(manufacturer).getRIM();

                if (sRim == null) {
                    fwStatus = new AppraisalStatus(FAIL,
                            String.format("Firmware Quote validation failed: "
                                            + "No associated RIM file could be found for %s",
                                    manufacturer));
                } else {
                    baseline = sRim.getExpectedPCRList();
                    String pcrContent = new String(device.getDeviceInfo()
                            .getTPMInfo().getPcrValues());
                    String[] storedPcrs = buildStoredPcrs(pcrContent, baseline[0].length());
                    PCRPolicy pcrPolicy = policy.getPcrPolicy();
                    pcrPolicy.setBaselinePcrs(baseline);
                    // grab the quote
                    byte[] hash = device.getDeviceInfo().getTPMInfo().getTpmQuoteHash();
                    if (pcrPolicy.validateQuote(hash, storedPcrs)) {
                        level = Level.INFO;
                        fwStatus = new AppraisalStatus(PASS,
                                SupplyChainCredentialValidator.FIRMWARE_VALID);
                        fwStatus.setMessage("Firmware validation of TPM Quote successful.");

                    } else {
                        fwStatus.setMessage("Firmware validation of TPM Quote failed."
                                + "\nPCR hash and Quote hash do not match.");
                    }
                }
            } catch (Exception ex) {
                LOGGER.error(ex);
            }

            quoteScv = buildValidationRecord(SupplyChainValidation
                            .ValidationType.FIRMWARE,
                    fwStatus.getAppStatus(), fwStatus.getMessage(), sRim, level);

            // Generate validation summary, save it, and return it.
            List<SupplyChainValidation> validations = new ArrayList<>();
            SupplyChainValidationSummary previous
                    = this.supplyChainValidatorSummaryManager.get(
                    UUID.fromString(device.getSummaryId()));
            for (SupplyChainValidation scv : previous.getValidations()) {
                if (scv.getValidationType() != SupplyChainValidation.ValidationType.FIRMWARE) {
                    validations.add(buildValidationRecord(scv.getValidationType(),
                            scv.getResult(), scv.getMessage(),
                            scv.getCertificatesUsed().get(0), Level.INFO));
                }
            }
            validations.add(quoteScv);
            previous.archive();
            supplyChainValidatorSummaryManager.update(previous);
            summary = new SupplyChainValidationSummary(device, validations);

            // try removing the supply chain validation as well and resaving that
            try {
                supplyChainValidatorSummaryManager.save(summary);
            } catch (DBManagerException ex) {
                LOGGER.error("Failed to save Supply Chain Summary", ex);
            }
        }

        return summary;
    }

    private SupplyChainValidation validateEndorsementCredential(final EndorsementCredential ec,
                                                                final boolean acceptExpiredCerts) {
        final SupplyChainValidation.ValidationType validationType
                = SupplyChainValidation.ValidationType.ENDORSEMENT_CREDENTIAL;
        LOGGER.info("Validating endorsement credential");
        if (ec == null) {
            LOGGER.error("No endorsement credential to validate");
            return buildValidationRecord(validationType,
                    AppraisalStatus.Status.FAIL, "Endorsement credential is missing",
                    null, Level.ERROR);
        }

        KeyStore ecStore = getCaChain(ec);
        AppraisalStatus result = supplyChainCredentialValidator.
                validateEndorsementCredential(ec, ecStore, acceptExpiredCerts);
        switch (result.getAppStatus()) {
            case PASS:
                return buildValidationRecord(validationType, PASS,
                        result.getMessage(), ec, Level.INFO);
            case FAIL:
                return buildValidationRecord(validationType, AppraisalStatus.Status.FAIL,
                        result.getMessage(), ec, Level.WARN);
            case ERROR:
            default:
                return buildValidationRecord(validationType, AppraisalStatus.Status.ERROR,
                        result.getMessage(), ec, Level.ERROR);
        }
    }

    private SupplyChainValidation validatePlatformCredential(
            final PlatformCredential pc,
            final KeyStore trustedCertificateAuthority, final boolean acceptExpiredCerts) {
        final SupplyChainValidation.ValidationType validationType
                = SupplyChainValidation.ValidationType.PLATFORM_CREDENTIAL;

        if (pc == null) {
            LOGGER.error("No platform credential to validate");
            return buildValidationRecord(validationType,
                    AppraisalStatus.Status.FAIL, "Empty Platform credential", null, Level.ERROR);
        }
        LOGGER.info("Validating Platform Credential");
        AppraisalStatus result = supplyChainCredentialValidator.validatePlatformCredential(pc,
                trustedCertificateAuthority, acceptExpiredCerts);
        switch (result.getAppStatus()) {
            case PASS:
                return buildValidationRecord(validationType, PASS,
                        result.getMessage(), pc, Level.INFO);
            case FAIL:
                return buildValidationRecord(validationType, AppraisalStatus.Status.FAIL,
                        result.getMessage(), pc, Level.WARN);
            case ERROR:
            default:
                return buildValidationRecord(validationType, AppraisalStatus.Status.ERROR,
                        result.getMessage(), pc, Level.ERROR);
        }
    }

    private SupplyChainValidation validatePlatformCredentialAttributes(
            final PlatformCredential pc, final DeviceInfoReport deviceInfoReport,
            final EndorsementCredential ec) {
        final SupplyChainValidation.ValidationType validationType
                = SupplyChainValidation.ValidationType.PLATFORM_CREDENTIAL_ATTRIBUTES;

        if (pc == null) {
            LOGGER.error("No platform credential to validate");
            return buildValidationRecord(validationType,
                    AppraisalStatus.Status.FAIL, "Platform credential is missing",
                    null, Level.ERROR);
        }
        LOGGER.info("Validating platform credential attributes");
        AppraisalStatus result = supplyChainCredentialValidator.
                validatePlatformCredentialAttributes(pc, deviceInfoReport, ec);
        switch (result.getAppStatus()) {
            case PASS:
                return buildValidationRecord(validationType, PASS,
                        result.getMessage(), pc, Level.INFO);
            case FAIL:
                if (!result.getAdditionalInfo().isEmpty()) {
                    pc.setComponentFailures(result.getAdditionalInfo());
                    this.certificateManager.update(pc);
                }
                return buildValidationRecord(validationType, AppraisalStatus.Status.FAIL,
                        result.getMessage(), pc, Level.WARN);
            case ERROR:
            default:
                return buildValidationRecord(validationType, AppraisalStatus.Status.ERROR,
                        result.getMessage(), pc, Level.ERROR);
        }
    }

    private SupplyChainValidation validateDeltaPlatformCredentialAttributes(
            final PlatformCredential delta,
            final DeviceInfoReport deviceInfoReport,
            final PlatformCredential base,
            final Map<PlatformCredential, SupplyChainValidation> deltaMapping) {
        final SupplyChainValidation.ValidationType validationType
                = SupplyChainValidation.ValidationType.PLATFORM_CREDENTIAL_ATTRIBUTES;

        if (delta == null) {
            LOGGER.error("No delta certificate to validate");
            return buildValidationRecord(validationType,
                    AppraisalStatus.Status.FAIL, "Delta platform certificate is missing",
                    null, Level.ERROR);
        }
        LOGGER.info("Validating delta platform certificate attributes");
        AppraisalStatus result = supplyChainCredentialValidator.
                validateDeltaPlatformCredentialAttributes(delta, deviceInfoReport,
                        base, deltaMapping);
        switch (result.getAppStatus()) {
            case PASS:
                return buildValidationRecord(validationType, PASS,
                        result.getMessage(), delta, Level.INFO);
            case FAIL:
                if (!result.getAdditionalInfo().isEmpty()) {
                    base.setComponentFailures(result.getAdditionalInfo());
                    this.certificateManager.update(base);
                }
                return buildValidationRecord(validationType, AppraisalStatus.Status.FAIL,
                        result.getMessage(), delta, Level.WARN);
            case ERROR:
            default:
                return buildValidationRecord(validationType, AppraisalStatus.Status.ERROR,
                        result.getMessage(), delta, Level.ERROR);
        }
    }

    /**
     * Creates a supply chain validation record and logs the validation message
     * at the specified log level.
     *
     * @param validationType   the type of validation
     * @param result           the appraisal status
     * @param message          the validation message to include in the summary and log
     * @param archivableEntity the archivableEntity associated with the
     *                         validation
     * @param logLevel         the log level
     * @return a SupplyChainValidation
     */
    private SupplyChainValidation buildValidationRecord(
            final SupplyChainValidation.ValidationType validationType,
            final AppraisalStatus.Status result, final String message,
            final ArchivableEntity archivableEntity, final Level logLevel) {
        List<ArchivableEntity> aeList = new ArrayList<>();
        if (archivableEntity != null) {
            aeList.add(archivableEntity);
        }

        LOGGER.log(logLevel, message);
        return new SupplyChainValidation(validationType, result, aeList, message);
    }

    /**
     * This method is used to retrieve the entire CA chain (up to a trusted
     * self-signed certificate) for the given certificate. This method will look
     * up CA certificates that have a matching issuer organization as the given
     * certificate, and will perform that operation recursively until all
     * certificates for all relevant organizations have been retrieved. For that
     * reason, the returned set of certificates may be larger than the the
     * single trust chain for the queried certificate, but is guaranteed to
     * include the trust chain if it exists in this class' CertificateManager.
     * Returns the certificate authority credentials in a KeyStore.
     *
     * @param credential the credential whose CA chain should be retrieved
     * @return A keystore containing all relevant CA credentials to the given
     * certificate's organization or null if the keystore can't be assembled
     */
    public KeyStore getCaChain(final Certificate credential) {
        KeyStore caKeyStore = null;
        try {
            caKeyStore = caCertSetToKeystore(getCaChainRec(credential, Collections.emptySet()));
        } catch (KeyStoreException | IOException e) {
            LOGGER.error("Unable to assemble CA keystore", e);
        }
        return caKeyStore;
    }

    /**
     * This is a recursive method which is used to retrieve the entire CA chain
     * (up to a trusted self-signed certificate) for the given certificate. This
     * method will look up CA certificates that have a matching issuer
     * organization as the given certificate, and will perform that operation
     * recursively until all certificates for all relevant organizations have
     * been retrieved. For that reason, the returned set of certificates may be
     * larger than the the single trust chain for the queried certificate, but
     * is guaranteed to include the trust chain if it exists in this class'
     * CertificateManager.
     * <p>
     * Implementation notes: 1. Queries for CA certs with a subject org matching
     * the given (argument's) issuer org 2. Add that org to
     * queriedOrganizations, so we don't search for that organization again 3.
     * For each returned CA cert, add that cert to the result set, and recurse
     * with that as the argument (to go up the chain), if and only if we haven't
     * already queried for that organization (which prevents infinite loops on
     * certs with an identical subject and issuer org)
     *
     * @param credential                the credential whose CA chain should be retrieved
     * @param previouslyQueriedSubjects a list of organizations to refrain
     *                                  from querying
     * @return a Set containing all relevant CA credentials to the given
     * certificate's organization
     */
    private Set<CertificateAuthorityCredential> getCaChainRec(
            final Certificate credential,
            final Set<String> previouslyQueriedSubjects) {
        CertificateAuthorityCredential skiCA = null;
        Set<CertificateAuthorityCredential> certAuthsWithMatchingIssuer = new HashSet<>();
        if (credential.getAuthKeyId() != null
                && !credential.getAuthKeyId().isEmpty()) {
            byte[] bytes = Hex.decode(credential.getAuthKeyId());
            skiCA = CertificateAuthorityCredential
                    .select(certificateManager)
                    .bySubjectKeyIdentifier(bytes).getCertificate();
        }

        if (skiCA == null) {
            if (credential.getIssuerSorted() == null
                    || credential.getIssuerSorted().isEmpty()) {
                certAuthsWithMatchingIssuer = CertificateAuthorityCredential
                        .select(certificateManager)
                        .bySubject(credential.getIssuer())
                        .getCertificates();
            } else {
                //Get certificates by subject organization
                certAuthsWithMatchingIssuer = CertificateAuthorityCredential
                        .select(certificateManager)
                        .bySubjectSorted(credential.getIssuerSorted())
                        .getCertificates();
            }
        } else {
            certAuthsWithMatchingIssuer.add(skiCA);
        }
        Set<String> queriedOrganizations = new HashSet<>(previouslyQueriedSubjects);
        queriedOrganizations.add(credential.getIssuer());

        HashSet<CertificateAuthorityCredential> caCreds = new HashSet<>();
        for (CertificateAuthorityCredential cred : certAuthsWithMatchingIssuer) {
            caCreds.add(cred);
            if (!BouncyCastleUtils.x500NameCompare(cred.getIssuer(),
                    cred.getSubject())) {
                caCreds.addAll(getCaChainRec(cred, queriedOrganizations));
            }
        }
        return caCreds;
    }

    private KeyStore caCertSetToKeystore(final Set<CertificateAuthorityCredential> certs)
            throws KeyStoreException, IOException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try {
            keyStore.load(null, "".toCharArray());
            for (Certificate cert : certs) {
                keyStore.setCertificateEntry(cert.getId().toString(), cert.getX509Certificate());
            }
        } catch (IOException | CertificateException | NoSuchAlgorithmException e) {
            throw new IOException("Could not create and populate keystore", e);
        }

        return keyStore;
    }

    private boolean checkForMultipleBaseCredentials(final String platformSerialNumber) {
        boolean multiple = false;
        PlatformCredential baseCredential = null;

        if (platformSerialNumber != null) {
            List<PlatformCredential> chainCertificates = PlatformCredential
                    .select(certificateManager)
                    .byBoardSerialNumber(platformSerialNumber)
                    .getCertificates().stream().collect(Collectors.toList());

            for (PlatformCredential pc : chainCertificates) {
                if (baseCredential != null && pc.isBase()) {
                    multiple = true;
                } else if (pc.isBase()) {
                    baseCredential = pc;
                }
            }
        }

        return multiple;
    }

    private String[] buildStoredPcrs(final String pcrContent, final int algorithmLength) {
        // we have a full set of PCR values
        String[] pcrSet = pcrContent.split("\\n");
        String[] storedPcrs = new String[TPMMeasurementRecord.MAX_PCR_ID + 1];

        // we need to scroll through the entire list until we find
        // a matching hash length
        int offset = 1;

        for (int i = 0; i < pcrSet.length; i++) {
            if (pcrSet[i].contains("sha")) {
                // entered a new set, check size
                if (pcrSet[i + offset].split(":")[1].trim().length()
                        == algorithmLength) {
                    // found the matching set
                    for (int j = 0; j <= TPMMeasurementRecord.MAX_PCR_ID; j++) {
                        storedPcrs[j] = pcrSet[++i].split(":")[1].trim();
                    }
                    break;
                }
            }
        }

        return storedPcrs;
    }
}
