package hirs.attestationca.service;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import hirs.persist.AppraiserManager;
import hirs.persist.CertificateManager;
import hirs.persist.CertificateSelector;
import hirs.persist.CrudManager;
import hirs.persist.DBManagerException;
import hirs.persist.PersistenceConfiguration;
import hirs.persist.PolicyManager;
import hirs.validation.CredentialValidator;

/**
 * The main executor of supply chain verification tasks. The AbstractAttestationCertificateAuthority
 * will feed it the PC, EC, other relevant certificates, and serial numbers of the provisioning
 * task, and it will then manipulate the data as necessary, retrieve useful certs, and arrange
 * for actual validation by the SupplyChainValidator.
 */
@Service
@Import(PersistenceConfiguration.class)
public class SupplyChainValidationServiceImpl implements SupplyChainValidationService {

    private final PolicyManager policyManager;
    private final AppraiserManager appraiserManager;
    private final CertificateManager certificateManager;
    private final CredentialValidator supplyChainCredentialValidator;
    private final CrudManager<SupplyChainValidationSummary> supplyChainValidatorSummaryManager;

    private static final Logger LOGGER =
            LogManager.getLogger(SupplyChainValidationServiceImpl.class);


    /**
     * Constructor.
     * @param policyManager the policy manager
     * @param appraiserManager the appraiser manager
     * @param certificateManager the cert manager
     * @param supplyChainValidatorSummaryManager the summary manager
     * @param supplyChainCredentialValidator the credential validator
     */
    @Autowired
    public SupplyChainValidationServiceImpl(final PolicyManager policyManager,
            final AppraiserManager appraiserManager,
            final CertificateManager certificateManager,
            final CrudManager<SupplyChainValidationSummary> supplyChainValidatorSummaryManager,
            final CredentialValidator supplyChainCredentialValidator) {
        this.policyManager = policyManager;
        this.appraiserManager = appraiserManager;
        this.certificateManager = certificateManager;
        this.supplyChainValidatorSummaryManager = supplyChainValidatorSummaryManager;
        this.supplyChainCredentialValidator = supplyChainCredentialValidator;
    }

    /**
     * The "main" method of supply chain validation. Takes the credentials from an identity
     * request and validates the supply chain in accordance to the current supply chain
     * policy.
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
        HashMap<PlatformCredential, SupplyChainValidation> credentialMap = new HashMap<>();
        PlatformCredential baseCredential = null;
        List<SupplyChainValidation> validations = new LinkedList<>();
        Map<String, Boolean> multiBaseCheckMap = new HashMap<>();

        // validate all supply chain pieces. Potentially, a policy setting could be made
        // to dictate stopping after the first validation failure.

        // Validate the Endorsement Credential
        if (policy.isEcValidationEnabled()) {
            validations.add(validateEndorsementCredential(ec, acceptExpiredCerts));
            // store the device with the credential
            if (null != ec) {
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
                    SupplyChainValidation platformScv = validatePlatformCredential(
                            pc, trustedCa, acceptExpiredCerts);

                    // check if this cert has been verified for multiple base associated
                    // with the serial number
                    if (pc != null) {
                        boolean checked = multiBaseCheckMap.containsKey(pc.getPlatformSerial());
                        if (!checked) {
                            // if not checked, update the map
                            boolean result = checkForMultipleBaseCredentials(
                                    pc.getPlatformSerial());
                            multiBaseCheckMap.put(pc.getPlatformSerial(), result);
                            // if it is, then update the SupplyChainValidation message and result
                            if (result) {
                                String message = "Multiple Base certificates found in chain.";
                                if (!platformScv.getResult()
                                        .equals(AppraisalStatus.Status.PASS)) {
                                    message = String.format("%s,%n%s",
                                            platformScv.getMessage(), message);
                                }
                                platformScv = buildValidationRecord(
                                        SupplyChainValidation.ValidationType.PLATFORM_CREDENTIAL,
                                        AppraisalStatus.Status.FAIL,
                                        message, pc, Level.ERROR);
                            }
                        }
                    }
                    validations.add(platformScv);
                    if (pc != null) {
                        pc.setDevice(device);
                        this.certificateManager.update(pc);
                        credentialMap.put(pc, platformScv);
                        /*
                         * This method will be added to PlatformCredential to return whether a given
                         * object is a base or a delta credential.
                         */
/*                        if (pc.isBase()) {
                            baseCredential = pc;
                        }
*/
                    }
                }

                // iterate through base credentials found

            }
        }

        // Validate Platform Credential attributes
        if (policy.isPcAttributeValidationEnabled()) {
            // Ensure there are platform credentials to validate
            if (pcs == null || pcs.isEmpty()) {
                LOGGER.error("There were no Platform Credentials to validate attributes.");
                validations.add(buildValidationRecord(
                        SupplyChainValidation.ValidationType.PLATFORM_CREDENTIAL_ATTRIBUTES,
                        AppraisalStatus.Status.FAIL,
                        "Platform credential(s) missing. Cannot validate attributes",
                        null, Level.ERROR));
            } else {
                Iterator<PlatformCredential> it = pcs.iterator();
                while (it.hasNext()) {
                    PlatformCredential pc = it.next();
                    SupplyChainValidation attributeScv;
                    if (pc == baseCredential || baseCredential == null) {
                        attributeScv = validatePlatformCredentialAttributes(
                            pc, device.getDeviceInfo(), ec);
                    } else {
                        attributeScv = validateDeltaPlatformCredentialAttributes(
                            pc, device.getDeviceInfo(), baseCredential);
                    }

                    SupplyChainValidation platformScv = credentialMap.get(pc);
                    if (platformScv != null) {
                        if (platformScv.getResult() == AppraisalStatus.Status.FAIL
                                || platformScv.getResult() == AppraisalStatus.Status.ERROR) {
                            if (attributeScv != null
                                    && attributeScv.getResult() ==  AppraisalStatus.Status.PASS) {
                                validations.add(buildValidationRecord(
                                        SupplyChainValidation.ValidationType
                                                .PLATFORM_CREDENTIAL_ATTRIBUTES,
                                        AppraisalStatus.Status.FAIL,
                                        platformScv.getMessage(), pc, Level.WARN));
                            }
                        } else {
                            validations.add(attributeScv);
                        }
                    }

                    if (pc != null) {
                        pc.setDevice(device);
                        this.certificateManager.update(pc);
                    }
                }
            }
        }

        // Generate validation summary, save it, and return it.
        SupplyChainValidationSummary summary =
                new SupplyChainValidationSummary(device, validations);
        try {
            supplyChainValidatorSummaryManager.save(summary);
        } catch (DBManagerException ex) {
            LOGGER.error("Failed to save Supply chain summary", ex);
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
                return buildValidationRecord(validationType, AppraisalStatus.Status.PASS,
                        result.getMessage(), ec, Level.INFO);
            case FAIL:
                return buildValidationRecord(validationType, AppraisalStatus.Status.FAIL,
                        result.getMessage(), ec, Level.WARN);
            case ERROR:
                return buildValidationRecord(validationType, AppraisalStatus.Status.ERROR,
                        result.getMessage(), ec, Level.ERROR);
            default:
                return buildValidationRecord(validationType, AppraisalStatus.Status.ERROR,
                        result.getMessage(), ec, Level.ERROR);
        }
    }

    private SupplyChainValidation validatePlatformCredential(final PlatformCredential pc,
                                                             final KeyStore
                                                                     trustedCertificateAuthority,
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
                return buildValidationRecord(validationType, AppraisalStatus.Status.PASS,
                        result.getMessage(), pc, Level.INFO);
            case FAIL:
                return buildValidationRecord(validationType, AppraisalStatus.Status.FAIL,
                        result.getMessage(), pc, Level.WARN);
            case ERROR:
                return buildValidationRecord(validationType, AppraisalStatus.Status.ERROR,
                        result.getMessage(), pc, Level.ERROR);
            default:
                return buildValidationRecord(validationType, AppraisalStatus.Status.ERROR,
                        result.getMessage(), pc, Level.ERROR);
        }
    }

    private SupplyChainValidation validatePlatformCredentialAttributes(final PlatformCredential pc,
           final DeviceInfoReport deviceInfoReport,
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
                return buildValidationRecord(validationType, AppraisalStatus.Status.PASS,
                        result.getMessage(), pc, Level.INFO);
            case FAIL:
                return buildValidationRecord(validationType, AppraisalStatus.Status.FAIL,
                        result.getMessage(), pc, Level.WARN);
            case ERROR:
                return buildValidationRecord(validationType, AppraisalStatus.Status.ERROR,
                        result.getMessage(), pc, Level.ERROR);
            default:
                return buildValidationRecord(validationType, AppraisalStatus.Status.ERROR,
                        result.getMessage(), pc, Level.ERROR);
        }
    }

    private SupplyChainValidation validateDeltaPlatformCredentialAttributes(
            final PlatformCredential delta,
            final DeviceInfoReport deviceInfoReport,
            final PlatformCredential base) {
    /*
     * Do we need a new ValidationType for deltas?
     */
        final SupplyChainValidation.ValidationType validationType =
                SupplyChainValidation.ValidationType.DELTA_PLATFORM_CREDENTIAL_ATTRIBUTES;

        if (delta == null) {
            LOGGER.error("No delta credential to validate");
            return buildValidationRecord(validationType,
                    AppraisalStatus.Status.FAIL, "Platform credential is missing",
                    null, Level.ERROR);
        }
        LOGGER.info("Validating platform credential attributes");
        AppraisalStatus result = supplyChainCredentialValidator.
                validateDeltaPlatformCredentialAttributes(delta, deviceInfoReport, base);
        switch (result.getAppStatus()) {
            case PASS:
                return buildValidationRecord(validationType, AppraisalStatus.Status.PASS,
                        result.getMessage(), delta, Level.INFO);
            case FAIL:
                return buildValidationRecord(validationType, AppraisalStatus.Status.FAIL,
                        result.getMessage(), delta, Level.WARN);
            case ERROR:
                return buildValidationRecord(validationType, AppraisalStatus.Status.ERROR,
                        result.getMessage(), delta, Level.ERROR);
            default:
                return buildValidationRecord(validationType, AppraisalStatus.Status.ERROR,
                        result.getMessage(), delta, Level.ERROR);
        }
    }

    /**
     * Creates a supply chain validation record and logs the validation
     * message at the specified log level.
     * @param validationType the type of validation
     * @param result the appraisal status
     * @param message the validation message to include in the summary and log
     * @param certificate the certificate associated with the validation
     * @param logLevel the log level
     * @return a SupplyChainValidation
     */
    private SupplyChainValidation buildValidationRecord(
            final SupplyChainValidation.ValidationType validationType,
            final AppraisalStatus.Status result, final String message,
            final Certificate certificate, final Level logLevel) {

        List<Certificate> certificateList = new ArrayList<>();
        if (certificate != null) {
            certificateList.add(certificate);
        }

        LOGGER.log(logLevel, message);
        return new SupplyChainValidation(validationType, result, certificateList, message);
    }

    /**
     * This method is used to retrieve the entire CA chain (up to a
     * trusted self-signed certificate) for the given certificate.  This method will look up
     * CA certificates that have a matching issuer organization as the given certificate, and will
     * perform that operation recursively until all certificates for all relevant organizations
     * have been retrieved.  For that reason, the returned set of certificates may be larger
     * than the the single trust chain for the queried certificate, but is guaranteed to include
     * the trust chain if it exists in this class' CertificateManager.
     * Returns the certificate authority credentials in a KeyStore.
     *
     * @param credential the credential whose CA chain should be retrieved
     * @return A keystore containing all relevant CA credentials to the given certificate's
     * organization or null if the keystore can't be assembled
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
     * This is a recursive method which is used to retrieve the entire CA chain (up to a
     * trusted self-signed certificate) for the given certificate.  This method will look up
     * CA certificates that have a matching issuer organization as the given certificate, and will
     * perform that operation recursively until all certificates for all relevant organizations
     * have been retrieved.  For that reason, the returned set of certificates may be larger
     * than the the single trust chain for the queried certificate, but is guaranteed to include
     * the trust chain if it exists in this class' CertificateManager.
     *
     * Implementation notes:
     * 1. Queries for CA certs with a subject org matching the given (argument's) issuer org
     * 2. Add that org to queriedOrganizations, so we don't search for that organization again
     * 3. For each returned CA cert, add that cert to the result set, and recurse with that as the
     *      argument (to go up the chain), if and only if we haven't already queried for that
     *      organization (which prevents infinite loops on certs with an identical subject and
     *      issuer org)
     *
     * @param credential the credential whose CA chain should be retrieved
     * @param previouslyQueriedOrganizations a list of organizations to refrain from querying
     * @return a Set containing all relevant CA credentials to the given certificate's organization
     */
    private Set<CertificateAuthorityCredential> getCaChainRec(
            final Certificate credential,
            final Set<String> previouslyQueriedOrganizations
    ) {
        CertificateSelector<CertificateAuthorityCredential> caSelector =
                CertificateAuthorityCredential.select(certificateManager)
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
}
