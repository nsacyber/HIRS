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
import hirs.attestationca.persist.validation.CertificateAttributeScvValidator;
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
public class ValidationService {

    public static SupplyChainValidation evaluateEndorsementCredentialStatus(
            final EndorsementCredential ec,
            final CACredentialRepository caCredentialRepository,
            final boolean acceptExpiredCerts) {
        final SupplyChainValidation.ValidationType validationType
                = SupplyChainValidation.ValidationType.ENDORSEMENT_CREDENTIAL;
        log.info("Validating endorsement credential");
        if (ec == null) {
            log.error("No endorsement credential to validate");
            return buildValidationRecord(validationType,
                    AppraisalStatus.Status.FAIL, "Endorsement credential is missing",
                    null, Level.ERROR);
        }

        KeyStore ecStore = getCaChain(ec, caCredentialRepository);
        AppraisalStatus result = CredentialValidator.
                validateEndorsementCredential(ec, ecStore, acceptExpiredCerts);
        switch (result.getAppStatus()) {
            case PASS:
                return buildValidationRecord(validationType, AppraisalStatus.Status.PASS,
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

    public static SupplyChainValidation evaluatePlatformCredentialStatus(
            final PlatformCredential pc,
            final KeyStore trustedCertificateAuthority, final boolean acceptExpiredCerts) {
        final SupplyChainValidation.ValidationType validationType
                = SupplyChainValidation.ValidationType.PLATFORM_CREDENTIAL;

        if (pc == null) {
            log.error("No platform credential to validate");
            return buildValidationRecord(validationType,
                    AppraisalStatus.Status.FAIL, "Empty Platform credential", null, Level.ERROR);
        }
        log.info("Validating Platform Credential");
        AppraisalStatus result = CredentialValidator.validatePlatformCredential(pc,
                trustedCertificateAuthority, acceptExpiredCerts);
        switch (result.getAppStatus()) {
            case PASS:
                return buildValidationRecord(validationType, AppraisalStatus.Status.PASS,
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

    public static SupplyChainValidation evaluatePCAttributesStatus(
            final PlatformCredential pc, final DeviceInfoReport deviceInfoReport,
            final EndorsementCredential ec,
            final CertificateRepository certificateRepository,
            final ComponentResultRepository componentResultRepository,
            final ComponentAttributeRepository componentAttributeRepository,
            final List<ComponentInfo> componentInfos,
            final UUID provisionSessionId, final boolean ignoreRevisionAttribute) {
        final SupplyChainValidation.ValidationType validationType
                = SupplyChainValidation.ValidationType.PLATFORM_CREDENTIAL_ATTRIBUTES;

        if (pc == null) {
            log.error("No platform credential to validate");
            return buildValidationRecord(validationType,
                    AppraisalStatus.Status.FAIL, "Platform credential is missing",
                    null, Level.ERROR);
        }
        log.info("Validating platform credential attributes");
        AppraisalStatus result = CredentialValidator.
                validatePlatformCredentialAttributes(pc, deviceInfoReport, ec,
                        componentResultRepository, componentAttributeRepository,
                        componentInfos, provisionSessionId, ignoreRevisionAttribute);
        switch (result.getAppStatus()) {
            case PASS:
                return buildValidationRecord(validationType, AppraisalStatus.Status.PASS,
                        result.getMessage(), pc, Level.INFO);
            case FAIL:
                if (!result.getAdditionalInfo().isEmpty()) {
                    pc.setComponentFailures(result.getAdditionalInfo());
                    pc.setComponentFailureMessage(result.getMessage());
                    certificateRepository.save(pc);
                }
                return buildValidationRecord(validationType, AppraisalStatus.Status.FAIL,
                        result.getMessage(), pc, Level.WARN);
            case ERROR:
            default:
                return buildValidationRecord(validationType, AppraisalStatus.Status.ERROR,
                        result.getMessage(), pc, Level.ERROR);
        }
    }

    public static SupplyChainValidation evaluateDeltaAttributesStatus(
            final DeviceInfoReport deviceInfoReport,
            final PlatformCredential base,
            final Map<PlatformCredential, SupplyChainValidation> deltaMapping,
            final CertificateRepository certificateRepository,
            final ComponentResultRepository componentResultRepository,
            final ComponentAttributeRepository componentAttributeRepository,
            final List<ComponentInfo> componentInfos,
            final UUID provisionSessionId, final boolean ignoreRevisionAttribute) {
        final SupplyChainValidation.ValidationType validationType
                = SupplyChainValidation.ValidationType.PLATFORM_CREDENTIAL_ATTRIBUTES;

        log.info("Validating delta platform certificate attributes");
        AppraisalStatus result = CredentialValidator.
                validateDeltaPlatformCredentialAttributes(deviceInfoReport,
                        base, deltaMapping, componentInfos,
                        componentResultRepository, componentAttributeRepository,
                        provisionSessionId, ignoreRevisionAttribute);
        switch (result.getAppStatus()) {
            case PASS:
                return buildValidationRecord(validationType, AppraisalStatus.Status.PASS,
                        result.getMessage(), base, Level.INFO);
            case FAIL:
                if (!result.getAdditionalInfo().isEmpty()) {
                    base.setComponentFailures(result.getAdditionalInfo());
                    base.setComponentFailureMessage(result.getMessage());
                    certificateRepository.save(base);
                }
                // we are adding things to componentFailures
//                certificateRepository.save(delta);
                return buildValidationRecord(validationType, AppraisalStatus.Status.FAIL,
                        result.getMessage(), base, Level.WARN);
            case ERROR:
            default:
                return buildValidationRecord(validationType, AppraisalStatus.Status.ERROR,
                        result.getMessage(), base, Level.ERROR);
        }
    }

    public static SupplyChainValidation evaluateFirmwareStatus(
            final Device device,
            final PolicySettings policySettings, final ReferenceManifestRepository rimRepo,
            final ReferenceDigestValueRepository rdvRepo,
            final CACredentialRepository caRepo) {
        final SupplyChainValidation.ValidationType validationType
                = SupplyChainValidation.ValidationType.FIRMWARE;

        List<ReferenceManifest> rims = rimRepo.findByDeviceName(device.getName());
        ReferenceManifest baseRim = null;
        for (ReferenceManifest rim : rims) {
            if (rim.getRimType().equals(ReferenceManifest.BASE_RIM)) {
                baseRim = rim;
            }
        }
        AppraisalStatus result = FirmwareScvValidator.validateFirmware(device, policySettings,
                rimRepo, rdvRepo, caRepo);
        Level logLevel;

        switch (result.getAppStatus()) {
            case PASS:
                logLevel = Level.INFO;
                break;
            case FAIL:
                logLevel = Level.WARN;
                break;
            case ERROR:
            default:
                logLevel = Level.ERROR;
        }
        return buildValidationRecord(validationType, result.getAppStatus(),
                result.getMessage(), baseRim, logLevel);
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
     * @param certificate the credential whose CA chain should be retrieved
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
                caCreds.addAll(getCaChainRec(cred, queriedOrganizations, caCredentialRepository));
            }
        }
        return caCreds;
    }

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
