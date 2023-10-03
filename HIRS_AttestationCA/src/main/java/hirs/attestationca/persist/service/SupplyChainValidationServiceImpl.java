package hirs.attestationca.persist.service;

import hirs.utils.ArchivableEntity;
import hirs.attestationca.persist.DBManagerException;
import hirs.attestationca.persist.entity.manager.CACredentialRepository;
import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.ComponentResultRepository;
import hirs.attestationca.persist.entity.manager.PolicyRepository;
import hirs.attestationca.persist.entity.manager.ReferenceDigestValueRepository;
import hirs.attestationca.persist.entity.manager.ReferenceManifestRepository;
import hirs.attestationca.persist.entity.manager.SupplyChainValidationSummaryRepository;
import hirs.utils.Certificate;
import hirs.attestationca.persist.entity.userdefined.Device;
import hirs.attestationca.persist.entity.userdefined.PolicySettings;
import hirs.attestationca.persist.entity.userdefined.SupplyChainValidation;
import hirs.attestationca.persist.entity.userdefined.SupplyChainValidationSummary;
import hirs.utils.CertificateAuthorityCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.entity.userdefined.record.TPMMeasurementRecord;
import hirs.attestationca.persist.entity.userdefined.rim.EventLogMeasurements;
import hirs.attestationca.persist.entity.userdefined.rim.SupportReferenceManifest;
import hirs.attestationca.persist.enums.AppraisalStatus;
import hirs.attestationca.persist.validation.CredentialValidator;
import hirs.attestationca.persist.validation.PcrValidator;
import hirs.attestationca.persist.validation.SupplyChainCredentialValidator;
import hirs.utils.BouncyCastleUtils;
import lombok.extern.log4j.Log4j2;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.Level;

import static hirs.attestationca.persist.enums.AppraisalStatus.Status.FAIL;
import static hirs.attestationca.persist.enums.AppraisalStatus.Status.PASS;

@Log4j2
@Service
public class SupplyChainValidationServiceImpl implements SupplyChainValidationService {

    private CACredentialRepository caCredentialRepository;
    private PolicyRepository policyRepository;
    private ReferenceManifestRepository referenceManifestRepository;
    private ReferenceDigestValueRepository referenceDigestValueRepository;
    private ComponentResultRepository componentResultRepository;
    private CertificateRepository certificateRepository;
    private CredentialValidator supplyChainCredentialValidator;
    private SupplyChainValidationSummaryRepository supplyChainValidationSummaryRepository;

    /**
     * Constructor to set just the CertificateRepository, so that cert chain validating
     * methods can be called from outside classes.
     *
     * @param certificateRepository the cert repository
     */
    public SupplyChainValidationServiceImpl(final CertificateRepository certificateRepository) {
        this.certificateRepository = certificateRepository;
    }

    /**
     * Constructor.
     *
     * @param caCredentialRepository    ca credential repository
     * @param policyRepository                      the policy manager
     * @param certificateRepository                 the cert manager
     * @param componentResultRepository             the comp result manager
     * @param referenceManifestRepository           the RIM manager
     * @param supplyChainValidationSummaryRepository the summary manager
     * @param supplyChainCredentialValidator     the credential validator
     * @param referenceDigestValueRepository              the even manager
     */
    @Autowired
    @SuppressWarnings("ParameterNumberCheck")
    public SupplyChainValidationServiceImpl(
            final CACredentialRepository caCredentialRepository,
            final PolicyRepository policyRepository,
            final CertificateRepository certificateRepository,
            final ComponentResultRepository componentResultRepository,
            final ReferenceManifestRepository referenceManifestRepository,
            final SupplyChainValidationSummaryRepository supplyChainValidationSummaryRepository,
            final CredentialValidator supplyChainCredentialValidator,
            final ReferenceDigestValueRepository referenceDigestValueRepository) {
        this.caCredentialRepository = caCredentialRepository;
        this.policyRepository = policyRepository;
        this.certificateRepository = certificateRepository;
        this.componentResultRepository = componentResultRepository;
        this.referenceManifestRepository = referenceManifestRepository;
        this.supplyChainValidationSummaryRepository = supplyChainValidationSummaryRepository;
        this.supplyChainCredentialValidator = supplyChainCredentialValidator;
        this.referenceDigestValueRepository = referenceDigestValueRepository;
    }

    @Override
    public SupplyChainValidationSummary validateSupplyChain(final EndorsementCredential ec,
                                                            final List<PlatformCredential> pc,
                                                            final Device device) {
        return null;
    }

    /**
     * A supplemental method that handles validating just the quote post main validation.
     *
     * @param device the associated device.
     * @return True if validation is successful, false otherwise.
     */
    @Override
    public SupplyChainValidationSummary validateQuote(final Device device) {
        SupplyChainValidation quoteScv = null;
        SupplyChainValidationSummary summary = null;
        Level level = Level.ERROR;
        AppraisalStatus fwStatus = new AppraisalStatus(FAIL,
                "Unknown exception caught during quote validation.");
        SupportReferenceManifest sRim = null;
        EventLogMeasurements eventLog = null;

        // check if the policy is enabled
        if (getPolicySettings().isFirmwareValidationEnabled()) {
            String[] baseline = new String[Integer.SIZE];
            String deviceName = device.getDeviceInfo()
                    .getNetworkInfo().getHostname();

            try {
                List<SupportReferenceManifest> supportRims = referenceManifestRepository.getSupportByManufacturerModel(
                        device.getDeviceInfo().getHardwareInfo().getManufacturer(),
                        device.getDeviceInfo().getHardwareInfo().getProductName());
                for (SupportReferenceManifest support : supportRims) {
                    if (support.isBaseSupport()) {
                        sRim = support;
                    }
                }
                eventLog = (EventLogMeasurements) referenceManifestRepository
                        .findByHexDecHash(sRim.getEventLogHash());

                if (sRim == null) {
                    fwStatus = new AppraisalStatus(FAIL,
                            String.format("Firmware Quote validation failed: "
                                            + "No associated Support RIM file "
                                            + "could be found for %s",
                                    deviceName));
                } else if (eventLog == null) {
                    fwStatus = new AppraisalStatus(FAIL,
                            String.format("Firmware Quote validation failed: "
                                            + "No associated Client Log file "
                                            + "could be found for %s",
                                    deviceName));
                } else {
                    baseline = sRim.getExpectedPCRList();
                    String[] storedPcrs = eventLog.getExpectedPCRList();
                    PcrValidator pcrValidator = new PcrValidator(baseline);
                    // grab the quote
                    byte[] hash = device.getDeviceInfo().getTpmInfo().getTpmQuoteHash();
                    if (pcrValidator.validateQuote(hash, storedPcrs, getPolicySettings())) {
                        level = Level.INFO;
                        fwStatus = new AppraisalStatus(PASS,
                                SupplyChainCredentialValidator.FIRMWARE_VALID);
                        fwStatus.setMessage("Firmware validation of TPM Quote successful.");
                    } else {
                        fwStatus.setMessage("Firmware validation of TPM Quote failed."
                                + "\nPCR hash and Quote hash do not match.");
                    }
                    eventLog.setOverallValidationResult(fwStatus.getAppStatus());
                    this.referenceManifestRepository.save(eventLog);
                }
            } catch (Exception ex) {
                log.error(ex);
            }

            quoteScv = buildValidationRecord(SupplyChainValidation
                            .ValidationType.FIRMWARE,
                    fwStatus.getAppStatus(), fwStatus.getMessage(), eventLog, level);

            // Generate validation summary, save it, and return it.
            List<SupplyChainValidation> validations = new ArrayList<>();
            SupplyChainValidationSummary previous
                    = this.supplyChainValidationSummaryRepository.findByDevice(deviceName);
            for (SupplyChainValidation scv : previous.getValidations()) {
                if (scv.getValidationType() != SupplyChainValidation.ValidationType.FIRMWARE) {
                    validations.add(buildValidationRecord(scv.getValidationType(),
                            scv.getValidationResult(), scv.getMessage(),
                            scv.getCertificatesUsed().get(0), Level.INFO));
                }
            }
            validations.add(quoteScv);
            previous.archive();
            supplyChainValidationSummaryRepository.save(previous);
            summary = new SupplyChainValidationSummary(device, validations);

            // try removing the supply chain validation as well and resaving that
            try {
                supplyChainValidationSummaryRepository.save(summary);
            } catch (DBManagerException dbEx) {
                log.error("Failed to save Supply Chain Summary", dbEx);
            }
        }

        return summary;
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

        log.log(logLevel, message);
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
            log.error("Unable to assemble CA keystore", e);
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
        List<CertificateAuthorityCredential> certAuthsWithMatchingIssuer = new LinkedList<>();
        if (credential.getAuthorityKeyIdentifier() != null
                && !credential.getAuthorityKeyIdentifier().isEmpty()) {
            byte[] bytes = Hex.decode(credential.getAuthorityKeyIdentifier());
            // CYRUS is SKI unique?
            skiCA = caCredentialRepository.findBySubjectKeyIdentifier(bytes);
        }

        if (skiCA == null) {
            if (credential.getIssuerSorted() == null
                    || credential.getIssuerSorted().isEmpty()) {
                certAuthsWithMatchingIssuer = caCredentialRepository.findBySubject(credential.getIssuer());
            } else {
                //Get certificates by subject organization
                certAuthsWithMatchingIssuer = caCredentialRepository.findBySubjectSorted(credential.getIssuerSorted());
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

    /**
     * Helper function to get a fresh load of the default policy from the DB.
     *
     * @return The default Supply Chain Policy
     */
    private PolicySettings getPolicySettings() {
        PolicySettings defaultSettings = this.policyRepository.findByName("Default");

        if (defaultSettings == null) {
            defaultSettings = new PolicySettings("Default", "Settings are configured for no validation flags set.");
        }
        return defaultSettings;
    }
}
