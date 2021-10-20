package hirs.data.persist.certificate;

import hirs.persist.CertificateManager;
import hirs.persist.CertificateSelector;

import javax.persistence.Entity;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Represents an issued DevID certificate to a HIRS Client.
 * 8.1 DevID Certificate Fields Summary
 * Implementation requirements:
 *      * Key Generation
 *      * Key Insertion
 *      * Key Deletion, required if Generating and Inserting
 *      - If generating LDevID
 *      * Certificate insert
 *      * Certificate Chain insert
 *      * Certificate delete
 *      * Certificate Chain delete
 *
 *      -- RNG entropy?
 */
@Entity
public class IssuedDevIdCertificate extends IssuedCertificate {

    /**
     * This class enables the retrieval of IssuedDevIdCertificate by their attributes.
     */
    public static class Selector extends CertificateSelector<IssuedDevIdCertificate> {
        /**
         * Construct a new CertificateSelector that will use the given {@link CertificateManager} to
         * retrieve one or many IssuedDevIdCertificate.
         *
         * @param certificateManager the certificate manager to be used to retrieve certificates
         */
        public Selector(final CertificateManager certificateManager) {
            super(certificateManager, IssuedDevIdCertificate.class);
        }

        /**
         * Specify a device id that certificates must have to be considered
         * as matching.
         *
         * @param device the device id to query
         * @return this instance (for chaining further calls)
         */
        public IssuedDevIdCertificate.Selector byDeviceId(final UUID device) {
            setFieldValue(DEVICE_ID_FIELD, device);
            return this;
        }
    }

    /**
     * Default constructor for Hibernate.
     */
    protected IssuedDevIdCertificate() {
    }

    /**
     * Constructor.
     * @param certificateBytes the issued certificate bytes
     * @throws java.io.IOException if there is a problem extracting information from the certificate
     */
    public IssuedDevIdCertificate(final byte[] certificateBytes)
            throws IOException {
        super(certificateBytes, null, null);
        this.setIssuedType(ISSUED_TYPE_LDEVID);
    }

    /**
     * Constructor.
     * @param certificatePath path to certificate
     * @throws IOException if there is a problem extracting information from the certificate
     */
    public IssuedDevIdCertificate(final Path certificatePath)
            throws IOException {
        this(readBytes(certificatePath));
    }
}
