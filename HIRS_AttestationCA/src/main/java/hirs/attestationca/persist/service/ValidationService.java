package hirs.attestationca.persist.service;

import hirs.attestationca.persist.entity.ArchivableEntity;
import hirs.attestationca.persist.entity.manager.CACredentialRepository;
import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.ComponentAttributeRepository;
import hirs.attestationca.persist.entity.manager.ComponentResultRepository;
import hirs.attestationca.persist.entity.manager.ReferenceDigestValueRepository;
import hirs.attestationca.persist.entity.manager.ReferenceManifestRepository;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.Device;
import hirs.attestationca.persist.entity.userdefined.PolicySettings;
import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.SupplyChainValidation;
import hirs.attestationca.persist.entity.userdefined.certificate.CertificateAuthorityCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.entity.userdefined.info.ComponentInfo;
import hirs.attestationca.persist.entity.userdefined.report.DeviceInfoReport;
import hirs.attestationca.persist.enums.AppraisalStatus;
import hirs.attestationca.persist.validation.CredentialValidator;
import hirs.attestationca.persist.validation.FirmwareScvValidator;
import hirs.utils.BouncyCastleUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.bouncycastle.util.encoders.Hex;

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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Log4j2
public final class ValidationService {

    /**
     * This private constructor was created to silence checkstyle errors.
     */
    private ValidationService() {
    }

    /**
     * Evaluates the provided endorsement credential status.
     *
     * @param endorsementCredential  endorsement credential
     * @param caCredentialRepository CA Credential repository
     * @param acceptExpiredCerts     whether to accept expired certificates
     * @return a supply chain validation
     */
    public static SupplyChainValidation evaluateEndorsementCredentialStatus(
            final EndorsementCredential endorsementCredential,
            final CACredentialRepository caCredentialRepository,
            final boolean acceptExpiredCerts) {
        final SupplyChainValidation.ValidationType validationType
                = SupplyChainValidation.ValidationType.ENDORSEMENT_CREDENTIAL;
        log.info("Validating endorsement credential");
        if (endorsementCredential == null) {
            log.error("No endorsement credential to validate");
            return buildValidationRecord(validationType,
                    AppraisalStatus.Status.FAIL, "Endorsement credential is missing",
                    null, Level.ERROR);
        }

        KeyStore ecStore = getCaChain(endorsementCredential, caCredentialRepository);
        AppraisalStatus result = CredentialValidator.
                validateEndorsementCredential(endorsementCredential, ecStore, acceptExpiredCerts);
        return switch (result.getAppStatus()) {
            case PASS -> buildValidationRecord(validationType, AppraisalStatus.Status.PASS,
                    result.getMessage(), endorsementCredential, Level.INFO);
            case FAIL -> buildValidationRecord(validationType, AppraisalStatus.Status.FAIL,
                    result.getMessage(), endorsementCredential, Level.WARN);
            default -> buildValidationRecord(validationType, AppraisalStatus.Status.ERROR,
                    result.getMessage(), endorsementCredential, Level.ERROR);
        };
    }

    /**
     * Evaluates the provided platform credential status.
     *
     * @param platformCredential          platform credential
     * @param trustedCertificateAuthority trusted certificate authority
     * @param acceptExpiredCerts          whether to accept expired certificates
     * @return a supply chain validation
     */
    public static SupplyChainValidation evaluatePlatformCredentialStatus(
            final PlatformCredential platformCredential,
            final KeyStore trustedCertificateAuthority, final boolean acceptExpiredCerts) {
        final SupplyChainValidation.ValidationType validationType
                = SupplyChainValidation.ValidationType.PLATFORM_CREDENTIAL;

        if (platformCredential == null) {
            log.error("No platform credential to validate while evaluating platform credential status");
            return buildValidationRecord(validationType,
                    AppraisalStatus.Status.FAIL, "Empty Platform credential", null,
                    Level.ERROR);
        }

        log.info("Validating Platform Credential");

        AppraisalStatus result = CredentialValidator.validatePlatformCredential(platformCredential,
                trustedCertificateAuthority, acceptExpiredCerts);

        return switch (result.getAppStatus()) {
            case PASS -> buildValidationRecord(validationType, AppraisalStatus.Status.PASS,
                    result.getMessage(), platformCredential, Level.INFO);
            case FAIL -> buildValidationRecord(validationType, AppraisalStatus.Status.FAIL,
                    result.getMessage(), platformCredential, Level.WARN);
            default -> buildValidationRecord(validationType, AppraisalStatus.Status.ERROR,
                    result.getMessage(), platformCredential, Level.ERROR);
        };
    }

    /**
     * Evaluates PC Attributes status.
     *
     * @param platformCredential           platform credential
     * @param deviceInfoReport             device information report
     * @param endorsementCredential        endorsement credential
     * @param certificateRepository        certificate repository
     * @param componentResultRepository    component result repository
     * @param componentAttributeRepository component attribute repository
     * @param componentInfos               list of component information
     * @param provisionSessionId           uuid representation of the provision session id
     * @param ignoreRevisionAttribute      whether to ignore revision attribute
     * @param ignorePcieVpdAttribute       whether to ignore the pcie vpd attribute
     * @return a supply chain validation
     */
    public static SupplyChainValidation evaluatePCAttributesStatus(
            final PlatformCredential platformCredential, final DeviceInfoReport deviceInfoReport,
            final EndorsementCredential endorsementCredential,
            final CertificateRepository certificateRepository,
            final ComponentResultRepository componentResultRepository,
            final ComponentAttributeRepository componentAttributeRepository,
            final List<ComponentInfo> componentInfos,
            final UUID provisionSessionId,
            final boolean ignoreRevisionAttribute,
            final boolean ignorePcieVpdAttribute) throws IOException {
        final SupplyChainValidation.ValidationType validationType
                = SupplyChainValidation.ValidationType.PLATFORM_CREDENTIAL_ATTRIBUTES;

        if (platformCredential == null) {
            log.error("No platform credential to validate while evaluating platform credential attributes "
                    + "status");
            return buildValidationRecord(validationType,
                    AppraisalStatus.Status.FAIL, "Platform credential is missing",
                    null, Level.ERROR);
        }
        log.info("Validating platform credential attributes");

        AppraisalStatus result = CredentialValidator.
                validatePlatformCredentialAttributes(platformCredential, deviceInfoReport,
                        endorsementCredential,
                        componentResultRepository, componentAttributeRepository,
                        componentInfos, provisionSessionId, ignoreRevisionAttribute, ignorePcieVpdAttribute);

        return switch (result.getAppStatus()) {
            case PASS -> buildValidationRecord(validationType, AppraisalStatus.Status.PASS,
                    result.getMessage(), platformCredential, Level.INFO);
            case FAIL -> {
                if (!result.getAdditionalInfo().isEmpty()) {
                    platformCredential.setComponentFailures(result.getAdditionalInfo());
                    platformCredential.setComponentFailureMessage(result.getMessage());
                    certificateRepository.save(platformCredential);
                }
                yield buildValidationRecord(validationType, AppraisalStatus.Status.FAIL,
                        result.getMessage(), platformCredential, Level.WARN);
            }
            default -> buildValidationRecord(validationType, AppraisalStatus.Status.ERROR,
                    result.getMessage(), platformCredential, Level.ERROR);
        };
    }

    /**
     * Evaluates delta attributes status.
     *
     * @param deviceInfoReport             device information report
     * @param base                         base platform credential
     * @param deltaMapping                 delta mapping
     * @param certificateRepository        certificate repository
     * @param componentResultRepository    component result repository
     * @param componentAttributeRepository component attribute repository
     * @param componentInfos               list of component information
     * @param provisionSessionId           uuid representation of the provision session ID
     * @param ignoreRevisionAttribute      whether to ignore the revision attribute
     * @param ignorePcieVpdAttribute       whether to ignore the pcie vpd attribute
     * @return a supply chain validation
     */
    public static SupplyChainValidation evaluateDeltaAttributesStatus(
            final DeviceInfoReport deviceInfoReport,
            final PlatformCredential base,
            final Map<PlatformCredential, SupplyChainValidation> deltaMapping,
            final CertificateRepository certificateRepository,
            final ComponentResultRepository componentResultRepository,
            final ComponentAttributeRepository componentAttributeRepository,
            final List<ComponentInfo> componentInfos,
            final UUID provisionSessionId,
            final boolean ignoreRevisionAttribute,
            final boolean ignorePcieVpdAttribute) {
        final SupplyChainValidation.ValidationType validationType
                = SupplyChainValidation.ValidationType.PLATFORM_CREDENTIAL_ATTRIBUTES;

        log.info("Validating delta platform certificate attributes");
        AppraisalStatus result = CredentialValidator.
                validateDeltaPlatformCredentialAttributes(deviceInfoReport,
                        base, deltaMapping, componentInfos,
                        componentResultRepository, componentAttributeRepository,
                        provisionSessionId, ignoreRevisionAttribute, ignorePcieVpdAttribute);

        return switch (result.getAppStatus()) {
            case PASS -> buildValidationRecord(validationType, AppraisalStatus.Status.PASS,
                    result.getMessage(), base, Level.INFO);
            case FAIL -> {
                if (!result.getAdditionalInfo().isEmpty()) {
                    base.setComponentFailures(result.getAdditionalInfo());
                    base.setComponentFailureMessage(result.getMessage());
                    certificateRepository.save(base);
                }
                // we are adding things to componentFailures
//                certificateRepository.save(delta);
                yield buildValidationRecord(validationType, AppraisalStatus.Status.FAIL,
                        result.getMessage(), base, Level.WARN);
                // we are adding things to componentFailures
//                certificateRepository.save(delta);
            }
            default -> buildValidationRecord(validationType, AppraisalStatus.Status.ERROR,
                    result.getMessage(), base, Level.ERROR);
        };
    }

    /**
     * Evaluates the firmware status.
     *
     * @param device         device
     * @param policySettings policy settings
     * @param rimRepo        reference manifest repository
     * @param rdvRepo        reference digest value repository
     * @param caRepo         CA Credential repository
     * @return a supply chain validation
     */
    public static SupplyChainValidation evaluateFirmwareStatus(
            final Device device,
            final PolicySettings policySettings, final ReferenceManifestRepository rimRepo,
            final ReferenceDigestValueRepository rdvRepo,
            final CACredentialRepository caRepo) {
        final SupplyChainValidation.ValidationType validationType
                = SupplyChainValidation.ValidationType.FIRMWARE;

        AppraisalStatus result = FirmwareScvValidator.validateFirmware(device, policySettings,
                rimRepo, rdvRepo, caRepo);
        Level logLevel;
        List<ReferenceManifest> rims = rimRepo.findByDeviceName(device.getName());
        ReferenceManifest referenceManifest = null;
        String rimType = "";
        if (result.getAdditionalInfo().equals(ReferenceManifest.MEASUREMENT_RIM)) {
            rimType = ReferenceManifest.MEASUREMENT_RIM;
        } else {
            rimType = ReferenceManifest.BASE_RIM;
        }
        for (ReferenceManifest rim : rims) {
            if (rim.getRimType().equals(rimType)) {
                referenceManifest = rim;
            }
        }

        logLevel = switch (result.getAppStatus()) {
            case PASS -> Level.INFO;
            case FAIL -> Level.WARN;
            default -> Level.ERROR;
        };

        return buildValidationRecord(validationType, result.getAppStatus(),
                result.getMessage(), referenceManifest, logLevel);
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
    public static SupplyChainValidation buildValidationRecord(
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
     * @param certificate            the credential whose CA chain should be retrieved
     * @param caCredentialRepository db service to get CA Certs
     * @return A keystore containing all relevant CA credentials to the given
     * certificate's organization or null if the keystore can't be assembled
     */
    public static KeyStore getCaChain(final Certificate certificate,
                                      final CACredentialRepository caCredentialRepository) {
        KeyStore caKeyStore = null;
        try {
            caKeyStore = caCertSetToKeystore(getCaChainRec(certificate, Collections.emptySet(),
                    caCredentialRepository));
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
     * @param caCredentialRepository    CA Credential repository
     * @return a Set containing all relevant CA credentials to the given
     * certificate's organization
     */
    public static Set<CertificateAuthorityCredential> getCaChainRec(
            final Certificate credential,
            final Set<String> previouslyQueriedSubjects,
            final CACredentialRepository caCredentialRepository) {
        CertificateAuthorityCredential skiCA = null;
        List<CertificateAuthorityCredential> certAuthsWithMatchingIssuer = new LinkedList<>();
        if (credential.getAuthorityKeyIdentifier() != null
                && !credential.getAuthorityKeyIdentifier().isEmpty()) {
            byte[] bytes = Hex.decode(credential.getAuthorityKeyIdentifier());
            skiCA = caCredentialRepository.findBySubjectKeyIdentifier(bytes);
        }

        if (skiCA == null) {
            if (credential.getIssuerSorted() == null
                    || credential.getIssuerSorted().isEmpty()) {
                certAuthsWithMatchingIssuer = caCredentialRepository.findBySubject(credential.getIssuer());
            } else {
                //Get certificates by subject organization
                certAuthsWithMatchingIssuer =
                        caCredentialRepository.findBySubjectSorted(credential.getIssuerSorted());
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
                caCreds.addAll(getCaChainRec(cred, queriedOrganizations, caCredentialRepository));
            }
        }
        return caCreds;
    }

    /**
     * Creates a key store using the provided set of certificate authority credentials.
     *
     * @param certs set of certificate authority credentials
     * @return a keystore
     * @throws KeyStoreException if there is an issue creating a key store
     * @throws IOException       if there is an issue creating a key store
     */
    public static KeyStore caCertSetToKeystore(final Set<CertificateAuthorityCredential> certs)
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
}
