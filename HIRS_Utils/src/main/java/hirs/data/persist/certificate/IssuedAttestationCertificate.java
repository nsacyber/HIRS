package hirs.data.persist.certificate;

import hirs.persist.CertificateManager;
import hirs.persist.CertificateSelector;

import javax.persistence.Entity;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

/**
 * Represents an issued attestation certificate to a HIRS Client.
 */
@Entity
public class IssuedAttestationCertificate extends IssuedCertificate {

    /**
     * This class enables the retrieval of IssuedAttestationCertificate by their attributes.
     */
    public static class Selector extends CertificateSelector<IssuedAttestationCertificate> {
        /**
         * Construct a new CertificateSelector that will use the given {@link CertificateManager} to
         * retrieve one or many IssuedAttestationCertificate.
         *
         * @param certificateManager the certificate manager to be used to retrieve certificates
         */
        public Selector(final CertificateManager certificateManager) {
            super(certificateManager, IssuedAttestationCertificate.class);
        }

        /**
         * Specify a device id that certificates must have to be considered
         * as matching.
         *
         * @param device the device id to query
         * @return this instance (for chaining further calls)
         */
        public Selector byDeviceId(final UUID device) {
            setFieldValue(DEVICE_ID_FIELD, device);
            return this;
        }
    }

    /**
     * Default constructor for Hibernate.
     */
    protected IssuedAttestationCertificate() {

    }

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
        super(certificateBytes, endorsementCredential, platformCredentials);
        this.setIssuedType(ISSUED_TYPE_AK);
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
