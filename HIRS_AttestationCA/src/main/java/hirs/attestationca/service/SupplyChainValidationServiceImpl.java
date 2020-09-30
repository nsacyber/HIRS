package hirs.attestationca.service;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import hirs.data.persist.BaseReferenceManifest;
import hirs.data.persist.TPMMeasurementRecord;
import hirs.data.persist.SwidResource;
import hirs.data.persist.PCRPolicy;
import hirs.data.persist.ArchivableEntity;
import hirs.validation.SupplyChainCredentialValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import hirs.persist.CertificateSelector;
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
     * @param policyManager the policy manager
     * @param appraiserManager the appraiser manager
     * @param certificateManager the cert manager
     * @param referenceManifestManager the RIM manager
     * @param supplyChainValidatorSummaryManager the summary manager
     * @param supplyChainCredentialValidator the credential validator
     */
    @Autowired
    public SupplyChainValidationServiceImpl(final PolicyManager policyManager,
            final AppraiserManager appraiserManager,
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
     * @param ec The endorsement credential from the identity request.
     * @param pcs The platform credentials from the identity request.
     * @param device The device to be validated.
     * @return A summary of the validation results.
     */
    @Override
    public SupplyChainValidationSummary validateSupplyChain(final EndorsementCredential ec,
            final Set<PlatformCredential> pcs,
            final Device device) {
        final Appraiser supplyChainAppraiser = appraiserManager.getAppraiser(
                SupplyChainAppraiser.NAME);
        SupplyChainPolicy policy = (SupplyChainPolicy) policyManager.getDefaultPolicy(
                supplyChainAppraiser);
        boolean acceptExpiredCerts = policy.isExpiredCertificateValidationEnabled();
        PlatformCredential baseCredential = null;
        String componentFailures = "";
        List<SupplyChainValidation> validations = new LinkedList<>();
        Map<PlatformCredential, SupplyChainValidation> deltaMapping = new HashMap<>();
        SupplyChainValidation platformScv = null;
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
                validations.add(buildValidationRecord(
                        SupplyChainValidation.ValidationType.PLATFORM_CREDENTIAL,
                        AppraisalStatus.Status.FAIL,
                        "Platform credential(s) missing", null, Level.ERROR));
            } else {
                Iterator<PlatformCredential> it = pcs.iterator();
                while (it.hasNext()) {
                    PlatformCredential pc = it.next();
                    KeyStore trustedCa = getCaChain(pc);
                    platformScv = validatePlatformCredential(
                            pc, trustedCa, acceptExpiredCerts);

                    // check if this cert has been verified for multiple base
                    // associated with the serial number
                    if (pc != null) {
                        platformScv = validatePcPolicy(pc, platformScv,
                                deltaMapping, acceptExpiredCerts);

                        validations.add(platformScv);
                        validations.addAll(deltaMapping.values());

                        if (pc.isBase()) {
                            baseCredential = pc;
                        }
                        pc.setDevice(device);
                        this.certificateManager.update(pc);
                    }
                }
            }
        }

        // Validate Platform Credential attributes
        if (policy.isPcAttributeValidationEnabled()) {
            // Ensure there are platform credentials to validate
            if (pcs == null || pcs.isEmpty()) {
                LOGGER.error("There were no Platform Credentials to validate attributes.");
                validations.add(buildValidationRecord(
                        SupplyChainValidation.ValidationType.PLATFORM_CREDENTIAL,
                        AppraisalStatus.Status.FAIL,
                        "Platform credential(s) missing."
                        + " Cannot validate attributes",
                        null, Level.ERROR));
            } else {
                Iterator<PlatformCredential> it = pcs.iterator();
                while (it.hasNext()) {
                    PlatformCredential pc = it.next();
                    SupplyChainValidation attributeScv;

                    if (pc != null) {
                        if (pc.isDeltaChain()) {
                            // this check validates the delta changes and recompares
                            // the modified list to the original.
                            attributeScv = validateDeltaPlatformCredentialAttributes(
                                    pc, device.getDeviceInfo(),
                                    baseCredential, deltaMapping);
                        } else {
                            attributeScv = validatePlatformCredentialAttributes(
                                    pc, device.getDeviceInfo(), ec);
                        }

                        if (platformScv != null) {
                            // have to make sure the attribute validation isn't ignored and
                            // doesn't override general validation status
                            if (platformScv.getResult() == PASS
                                    && attributeScv.getResult() != PASS) {
                                // if the platform trust store validated but the attribute didn't
                                // replace
                                validations.remove(platformScv);
                                validations.add(attributeScv);
                            } else if ((platformScv.getResult() == PASS
                                    && attributeScv.getResult() == PASS)
                                    || (platformScv.getResult() != PASS
                                    && attributeScv.getResult() != PASS)) {
                                // if both trust store and attributes validated or failed
                                // combine messages
                                validations.remove(platformScv);
                                List<ArchivableEntity> aes = new ArrayList<>();
                                for (Certificate cert : platformScv.getCertificatesUsed()) {
                                    aes.add(cert);
                                }
                                validations.add(new SupplyChainValidation(
                                        platformScv.getValidationType(),
                                        platformScv.getResult(), aes,
                                        String.format("%s%n%s", platformScv.getMessage(),
                                                attributeScv.getMessage())));
                            }
                            componentFailures = attributeScv.getMessage();
                        }

                        pc.setDevice(device);
                        this.certificateManager.update(pc);
                    }
                }
            }
        }

        if (policy.isFirmwareValidationEnabled()) {
            // may need to associated with device to pull the correct info
            // compare tpm quote with what is pulled from RIM associated file
            validations.add(validateFirmware(device, policy.getPcrPolicy()));
        }

        // Generate validation summary, save it, and return it.
        SupplyChainValidationSummary summary
                = new SupplyChainValidationSummary(device, validations);
        if (baseCredential != null) {
            baseCredential.setComponentFailures(componentFailures);
            this.certificateManager.update(baseCredential);
        }
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
     * @param pc The platform credential getting checked
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

    private SupplyChainValidation validateFirmware(final Device device,
            final PCRPolicy pcrPolicy) {

        String[] baseline = new String[Integer.SIZE];
        Level level = Level.ERROR;
        AppraisalStatus fwStatus = null;
        String manufacturer = device.getDeviceInfo()
                .getHardwareInfo().getManufacturer();

        ReferenceManifest rim = ReferenceManifest.select(
                this.referenceManifestManager)
                .byManufacturer(manufacturer)
                .getRIM();

        fwStatus = new AppraisalStatus(PASS,
                SupplyChainCredentialValidator.FIRMWARE_VALID);
        if (rim instanceof BaseReferenceManifest) {
            BaseReferenceManifest bRim = (BaseReferenceManifest) rim;
            List<SwidResource> swids = bRim.parseResource();
            for (SwidResource swid : swids) {
                baseline = swid.getPcrValues()
                        .toArray(new String[swid.getPcrValues().size()]);
            }
            pcrPolicy.setBaselinePcrs(baseline);

            if (device != null) {
                String pcrContent = "";
                try {
                    pcrContent = new String(device.getDeviceInfo().getTPMInfo().getPcrValues());
                } catch (NullPointerException npEx) {
                    LOGGER.error(npEx);
                }

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
                            level = Level.ERROR;
                            fwStatus = new AppraisalStatus(FAIL, sb.toString());
                        } else {
                            level = Level.INFO;
                        }
                    }
                }
            } else {
                fwStatus = new AppraisalStatus(FAIL, "Associated Issued Attestation"
                        + " Certificate can not be found.");
            }
        } else {
            fwStatus = new AppraisalStatus(FAIL,
                    String.format("Firmware validation failed: "
                                    + "No associated RIM file could be found for %s",
                            manufacturer));
        }

        return buildValidationRecord(SupplyChainValidation.ValidationType.FIRMWARE,
                fwStatus.getAppStatus(), fwStatus.getMessage(), rim, level);
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

        // check if the policy is enabled
        if (policy.isFirmwareValidationEnabled()) {
            String[] baseline = new String[Integer.SIZE];
            String manufacturer = device.getDeviceInfo()
                    .getHardwareInfo().getManufacturer();

            // need to get pcrs
            ReferenceManifest rim = ReferenceManifest.select(
                    this.referenceManifestManager)
                    .byManufacturer(manufacturer)
                    .getRIM();
            if (rim == null) {
                fwStatus = new AppraisalStatus(FAIL,
                        String.format("Firmware Quote validation failed: "
                                        + "No associated RIM file could be found for %s",
                                manufacturer));
            } else {
                BaseReferenceManifest bRim = (BaseReferenceManifest) rim;
                List<SwidResource> swids = bRim.parseResource();
                for (SwidResource swid : swids) {
                    baseline = swid.getPcrValues()
                            .toArray(new String[swid.getPcrValues().size()]);
                }

                String pcrContent = new String(device.getDeviceInfo().getTPMInfo().getPcrValues());
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
                    fwStatus.setMessage("Firmware validation of TPM Quote failed." +
                            "\nPCR hash and Quote hash do not match.");
                }
            }

            quoteScv = buildValidationRecord(SupplyChainValidation
                            .ValidationType.FIRMWARE,
                    fwStatus.getAppStatus(), fwStatus.getMessage(), rim, level);

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

    private SupplyChainValidation validatePlatformCredential(final PlatformCredential pc,
            final KeyStore trustedCertificateAuthority,
            final boolean acceptExpiredCerts) {
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

    private SupplyChainValidation validatePlatformCredentialAttributes(final PlatformCredential pc,
            final DeviceInfoReport deviceInfoReport,
            final EndorsementCredential ec) {
        final SupplyChainValidation.ValidationType validationType
                = SupplyChainValidation.ValidationType.PLATFORM_CREDENTIAL;

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
                = SupplyChainValidation.ValidationType.PLATFORM_CREDENTIAL;

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
     * @param validationType the type of validation
     * @param result the appraisal status
     * @param message the validation message to include in the summary and log
     * @param archivableEntity the archivableEntity associated with the
     * validation
     * @param logLevel the log level
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
     *
     * Implementation notes: 1. Queries for CA certs with a subject org matching
     * the given (argument's) issuer org 2. Add that org to
     * queriedOrganizations, so we don't search for that organization again 3.
     * For each returned CA cert, add that cert to the result set, and recurse
     * with that as the argument (to go up the chain), if and only if we haven't
     * already queried for that organization (which prevents infinite loops on
     * certs with an identical subject and issuer org)
     *
     * @param credential the credential whose CA chain should be retrieved
     * @param previouslyQueriedOrganizations a list of organizations to refrain
     * from querying
     * @return a Set containing all relevant CA credentials to the given
     * certificate's organization
     */
    private Set<CertificateAuthorityCredential> getCaChainRec(
            final Certificate credential,
            final Set<String> previouslyQueriedOrganizations
    ) {
        CertificateSelector<CertificateAuthorityCredential> caSelector
                = CertificateAuthorityCredential.select(certificateManager)
                .bySubjectOrganization(credential.getIssuerOrganization());
        Set<CertificateAuthorityCredential> certAuthsWithMatchingOrg = caSelector.getCertificates();

        Set<String> queriedOrganizations = new HashSet<>(previouslyQueriedOrganizations);
        queriedOrganizations.add(credential.getIssuerOrganization());

        HashSet<CertificateAuthorityCredential> caCreds = new HashSet<>();
        for (CertificateAuthorityCredential cred : certAuthsWithMatchingOrg) {
            caCreds.add(cred);
            if (!queriedOrganizations.contains(cred.getIssuerOrganization())) {
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
