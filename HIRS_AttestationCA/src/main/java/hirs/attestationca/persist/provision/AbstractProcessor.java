package hirs.attestationca.persist.provision;

import com.google.protobuf.ByteString;
import hirs.attestationca.configuration.provisionerTpm2.ProvisionerTpm2;
import hirs.attestationca.persist.entity.manager.CertificateRepository;
import hirs.attestationca.persist.entity.manager.PolicyRepository;
import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.Device;
import hirs.attestationca.persist.entity.userdefined.PolicySettings;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.IssuedAttestationCertificate;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.exceptions.CertificateProcessingException;
import hirs.attestationca.persist.provision.helper.CredentialManagementHelper;
import hirs.attestationca.persist.provision.helper.IssuedCertificateAttributeHelper;
import hirs.attestationca.persist.provision.helper.ProvisionUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Contains the default properties required for a processor that communicates with the provisioner.
 * This class serves as a base for processors, providing essential configuration and functionality
 * needed to interact with the provisioning system.
 */
@Getter
@Log4j2
@NoArgsConstructor
public class AbstractProcessor {

    private int validDays;

    private PrivateKey privateKey;

    @Setter
    private PolicyRepository policyRepository;

    /**
     * Default constructor that sets main class fields.
     *
     * @param privateKey private key used for communication authentication
     * @param validDays  property value to set for issued certificates
     */
    public AbstractProcessor(final PrivateKey privateKey,
                             final int validDays) {
        this.privateKey = privateKey;
        this.validDays = validDays;
    }

    /**
     * Generates a credential using the specified public key.
     *
     * @param publicKey             cannot be null
     * @param endorsementCredential the endorsement credential
     * @param platformCredentials   the set of platform credentials
     * @param deviceName            The host name used in the subject alternative name
     * @param acaCertificate        object used to create credential
     * @return identity credential
     */
    protected X509Certificate generateCredential(final PublicKey publicKey,
                                                 final EndorsementCredential endorsementCredential,
                                                 final List<PlatformCredential> platformCredentials,
                                                 final String deviceName,
                                                 final X509Certificate acaCertificate) {
        try {
            // have the certificate expire in the configured number of days
            Calendar expiry = Calendar.getInstance();
            expiry.add(Calendar.DAY_OF_YEAR, getValidDays());

            X500Name issuer =
                    new X509CertificateHolder(acaCertificate.getEncoded()).getSubject();
            Date notBefore = new Date();
            Date notAfter = expiry.getTime();
            BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());

            SubjectPublicKeyInfo subjectPublicKeyInfo =
                    SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());

            // The subject should be left blank, per spec
            X509v3CertificateBuilder builder =
                    new X509v3CertificateBuilder(issuer, serialNumber,
                            notBefore, notAfter, null /* subjectName */, subjectPublicKeyInfo);

            Extension subjectAlternativeName =
                    IssuedCertificateAttributeHelper.buildSubjectAlternativeNameFromCerts(
                            endorsementCredential, platformCredentials, deviceName);

            Extension authKeyIdentifier = IssuedCertificateAttributeHelper
                    .buildAuthorityKeyIdentifier(acaCertificate);

            builder.addExtension(subjectAlternativeName);
            if (authKeyIdentifier != null) {
                builder.addExtension(authKeyIdentifier);
            }
            // identify cert as an AIK with this extension
            if (IssuedCertificateAttributeHelper.EXTENDED_KEY_USAGE_EXTENSION != null) {
                builder.addExtension(IssuedCertificateAttributeHelper.EXTENDED_KEY_USAGE_EXTENSION);
            } else {
                log.warn("Failed to build extended key usage extension and add to AIK");
                throw new IllegalStateException("Extended Key Usage attribute unavailable. "
                        + "Unable to issue certificates");
            }

            // Add signing extension
            builder.addExtension(
                    Extension.keyUsage,
                    true,
                    new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment)
            );

            // Basic constraints
            builder.addExtension(
                    Extension.basicConstraints,
                    true,
                    new BasicConstraints(false)
            );

            ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA")
                    .setProvider("BC").build(getPrivateKey());
            X509CertificateHolder holder = builder.build(signer);
            return new JcaX509CertificateConverter()
                    .setProvider("BC").getCertificate(holder);
        } catch (IOException | OperatorCreationException | CertificateException exception) {
            throw new CertificateProcessingException("Encountered error while generating "
                    + "identity credential: " + exception.getMessage(), exception);
        }
    }

    /**
     * Helper method to parse an Endorsement Credential from a Protobuf generated
     * IdentityClaim. Will also check if the Endorsement Credential was already uploaded.
     * Persists the Endorsement Credential if it does not already exist.
     *
     * @param identityClaim         a Protobuf generated Identity Claim object
     * @param ekPub                 the endorsement public key from the Identity Claim object
     * @param certificateRepository db connector from certificates
     * @return the Endorsement Credential, if one exists, null otherwise
     */
    protected EndorsementCredential parseEcFromIdentityClaim(
            final ProvisionerTpm2.IdentityClaim identityClaim,
            final PublicKey ekPub, final CertificateRepository certificateRepository) {
        EndorsementCredential endorsementCredential = null;

        if (identityClaim.hasEndorsementCredential()) {
            endorsementCredential = CredentialManagementHelper.storeEndorsementCredential(
                    certificateRepository,
                    identityClaim.getEndorsementCredential().toByteArray(),
                    identityClaim.getDv().getNw().getHostname());
        } else if (ekPub != null) {
            log.warn("Endorsement Cred was not in the identity claim from the client."
                    + " Checking for uploads.");
            endorsementCredential = getEndorsementCredential(ekPub, certificateRepository);
        } else {
            log.warn("No endorsement credential was received in identity claim and no EK Public"
                    + " Key was provided to check for uploaded certificates.");
        }

        return endorsementCredential;
    }

    /**
     * Helper method to parse a set of Platform Credentials from a Protobuf generated
     * IdentityClaim and Endorsement Credential. Persists the Platform Credentials if they
     * do not already exist.
     *
     * @param identityClaim         a Protobuf generated Identity Claim object
     * @param endorsementCredential an endorsement credential to check if platform credentials
     *                              exist
     * @param certificateRepository db connector from certificates
     * @return the List of Platform Credentials, if they exist, an empty set otherwise
     */
    protected List<PlatformCredential> parsePcsFromIdentityClaim(
            final ProvisionerTpm2.IdentityClaim identityClaim,
            final EndorsementCredential endorsementCredential,
            final CertificateRepository certificateRepository) {
        List<PlatformCredential> platformCredentials = new LinkedList<>();

        if (identityClaim.getPlatformCredentialCount() > 0) {

            List<ByteString> platformCredentialList = identityClaim.getPlatformCredentialList();

            for (ByteString platformCredential : platformCredentialList) {
                if (!platformCredential.isEmpty()) {
                    PlatformCredential storedPlatformCredential =
                            CredentialManagementHelper.storePlatformCredential(
                                    certificateRepository, platformCredential.toByteArray(),
                                    identityClaim.getDv().getNw().getHostname());

                    if (storedPlatformCredential != null) {
                        platformCredentials.add(storedPlatformCredential);
                    }
                }
            }
        } else if (endorsementCredential != null) {
            // if none in the identity claim, look for uploaded platform credentials
            log.warn("PC was not in the identity claim from the client. Checking for uploads.");
            platformCredentials.addAll(getPlatformCredentials(certificateRepository, endorsementCredential));
        } else {
            log.warn("No platform credential received in identity claim.");
        }

        return platformCredentials;
    }

    /**
     * Gets the Endorsement Credential from the DB given the EK public key.
     *
     * @param ekPublicKey           the EK public key
     * @param certificateRepository db store manager for certificates
     * @return the Endorsement credential, if found, otherwise null
     */
    private EndorsementCredential getEndorsementCredential(
            final PublicKey ekPublicKey,
            final CertificateRepository certificateRepository) {
        log.debug("Searching for endorsement credential based on public key: {}", ekPublicKey);

        if (ekPublicKey == null) {
            throw new IllegalArgumentException("Cannot look up an EC given a null public key");
        }

        EndorsementCredential credential = null;

        try {
            credential = certificateRepository.findByPublicKeyModulusHexValue(
                    Certificate.getPublicKeyModulus(ekPublicKey)
                            .toString(Certificate.HEX_BASE));
        } catch (IOException e) {
            log.error("Could not extract public key modulus", e);
        }

        if (credential == null) {
            log.warn("Unable to find endorsement credential for public key.");
        } else {
            log.debug("Endorsement credential found.");
        }

        return credential;
    }

    /**
     * Helper method to create an {@link IssuedAttestationCertificate} object, set its
     * corresponding device and persist it.
     *
     * @param certificateRepository            db store manager for certificates
     * @param derEncodedAttestationCertificate the byte array representing the Attestation
     *                                         certificate
     * @param endorsementCredential            the endorsement credential used to generate the AC
     * @param platformCredentials              the platform credentials used to generate the AC
     * @param device                           the device to which the attestation certificate is tied
     * @param ldevID                           whether the certificate is a ldevid
     * @return whether the certificate was saved successfully
     */
    public boolean saveAttestationCertificate(final CertificateRepository certificateRepository,
                                              final byte[] derEncodedAttestationCertificate,
                                              final EndorsementCredential endorsementCredential,
                                              final List<PlatformCredential> platformCredentials,
                                              final Device device,
                                              final boolean ldevID) {
        List<IssuedAttestationCertificate> issuedAc;
        boolean generateCertificate = true;
        PolicyRepository scp = getPolicyRepository();
        PolicySettings policySettings;
        Date currentDate = new Date();
        int days;
        try {
            // save issued certificate
            IssuedAttestationCertificate attCert = new IssuedAttestationCertificate(
                    derEncodedAttestationCertificate, endorsementCredential, platformCredentials, ldevID);

            if (scp != null) {
                policySettings = scp.findByName("Default");

                Sort sortCriteria = Sort.by(Sort.Direction.DESC, "endValidity");
                issuedAc = certificateRepository.findByDeviceIdAndLdevID(device.getId(), ldevID,
                        sortCriteria);

                generateCertificate = ldevID ? policySettings.isIssueDevIdCertificateEnabled()
                        : policySettings.isIssueAttestationCertificateEnabled();

                if (issuedAc != null && !issuedAc.isEmpty()
                        && (ldevID ? policySettings.isGenerateDevIdCertificateOnExpiration()
                        : policySettings.isGenerateAttestationCertificateOnExpiration())) {
                    if (issuedAc.get(0).getEndValidity().after(currentDate)) {
                        // so the issued AC is not expired
                        // however are we within the threshold
                        days = ProvisionUtils.daysBetween(currentDate, issuedAc.get(0).getEndValidity());
                        generateCertificate =
                                days < (ldevID ? policySettings.getDevIdReissueThreshold()
                                        : policySettings.getReissueThreshold());
                    }
                }
            }

            if (generateCertificate) {
                attCert.setDeviceId(device.getId());
                attCert.setDeviceName(device.getName());
                certificateRepository.save(attCert);
            }
        } catch (Exception e) {
            log.error("Error saving generated Attestation Certificate to database.", e);
            throw new CertificateProcessingException(
                    "Encountered error while storing Attestation Certificate: "
                            + e.getMessage(), e);
        }

        return generateCertificate;
    }

    private List<PlatformCredential> getPlatformCredentials(final CertificateRepository certificateRepository,
                                                            final EndorsementCredential ec) {
        List<PlatformCredential> credentials = null;

        if (ec == null) {
            log.warn("Cannot look for platform credential(s).  Endorsement credential was null.");
        } else {
            log.debug("Searching for platform credential(s) based on holder serial number: {}",
                    ec.getSerialNumber());
            credentials = certificateRepository.getByHolderSerialNumber(ec.getSerialNumber());
            if (credentials == null || credentials.isEmpty()) {
                log.warn("No platform credential(s) found");
            } else {
                log.debug("Platform Credential(s) found: {}", credentials.size());
            }
        }

        return credentials;
    }
}
