package hirs.attestationca.persist.entity.userdefined.certificate;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

/**
 * Represents an issued attestation certificate to a HIRS Client.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class IssuedAttestationCertificate extends DeviceAssociatedCertificate {

    /**
     * AIC label that must be used.
     */
    public static final String AIC_TYPE_LABEL = "TCPA Trusted Platform Identity";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ek_id")
    private EndorsementCredential endorsementCredential;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "pc_id")
    private Set<PlatformCredential> platformCredentials;

    /**
     * This class enables the retrieval of IssuedAttestationCertificate by their attributes.
     */
//    public static class Selector extends CertificateSelector<IssuedAttestationCertificate> {
//        /**
//         * Construct a new CertificateSelector that will use the given {@link CertificateManager} to
//         * retrieve one or many IssuedAttestationCertificate.
//         *
//         * @param certificateManager the certificate manager to be used to retrieve certificates
//         */
//        public Selector(final CertificateManager certificateManager) {
//            super(certificateManager, IssuedAttestationCertificate.class);
//        }
//
//        /**
//         * Specify a device id that certificates must have to be considered
//         * as matching.
//         *
//         * @param device the device id to query
//         * @return this instance (for chaining further calls)
//         */
//        public Selector byDeviceId(final UUID device) {
//            setFieldValue(DEVICE_ID_FIELD, device);
//            return this;
//        }
//    }
//
//    /**
//     * Get a Selector for use in retrieving IssuedAttestationCertificate.
//     *
//     * @param certMan the CertificateManager to be used to retrieve persisted certificates
//     * @return a IssuedAttestationCertificate.Selector instance to use for retrieving certificates
//     */
//    public static IssuedAttestationCertificate.Selector select(final CertificateManager certMan) {
//        return new IssuedAttestationCertificate.Selector(certMan);
//    }


    /**
     * Constructor.
     * @param certificateBytes the issued certificate bytes
     * @param endorsementCredential the endorsement credential
     * @param platformCredentials the platform credentials
     * @throws IOException if there is a problem extracting information from the certificate
     */
    public IssuedAttestationCertificate(final byte[] certificateBytes,
                                        final EndorsementCredential endorsementCredential,
                                        final Set<PlatformCredential> platformCredentials)
            throws IOException {
        super(certificateBytes);
        this.endorsementCredential = endorsementCredential;
        this.platformCredentials = platformCredentials;
    }

    /**
     * Constructor.
     * @param certificatePath path to certificate
     * @param endorsementCredential the endorsement credential
     * @param platformCredentials the platform credentials
     * @throws IOException if there is a problem extracting information from the certificate
     */
    public IssuedAttestationCertificate(final Path certificatePath,
                                        final EndorsementCredential endorsementCredential,
                                        final Set<PlatformCredential> platformCredentials)
            throws IOException {
        this(readBytes(certificatePath), endorsementCredential, platformCredentials);
    }

}
