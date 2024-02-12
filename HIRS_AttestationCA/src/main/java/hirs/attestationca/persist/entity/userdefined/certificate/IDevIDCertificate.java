package hirs.attestationca.persist.entity.userdefined.certificate;

import hirs.attestationca.persist.entity.userdefined.Certificate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;

@Entity
@Log4j2
public class IDevIDCertificate extends Certificate {

    // Undefined expiry date, as specified in 802.1AR
    private static final long UNDEFINED_EXPIRY_DATE = 253402300799000L;

    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    private static final String SUBJECT_ALTERNATIVE_NAME_EXTENSION = "2.5.29.17";

    @Column
    private final byte[] subjectAltName;

    @Getter
    @Column
    private String subjectAltNameString;

    private void checkIDevIDValidity() throws IOException {
        // Detection of IDevID certificates
        if (this.getEndValidity() == null || !this.getEndValidity().equals(new Date(UNDEFINED_EXPIRY_DATE))) {
            throw new IOException("Invalid IDevID certificate detected"); // TODO: CHANGE

            // MUST fields: serialNumber, Validity (notBefore/notAfter), Subject
        }
    }

    /**
     * Construct a new IDevIDCertificate given its binary contents. The given
     * certificate should represent a valid X.509 certificate.
     *
     * @param certificateBytes the contents of a certificate file
     * @throws IOException if there is a problem extracting information from the certificate
     */
    public IDevIDCertificate(final byte[] certificateBytes)
            throws IOException {
        super(certificateBytes);

        // TODO: Extract device serial number (or identifier) from DN
        // TODO: Instantiate DevID class object and/or unique fields
        // TODO: Support for subjectAltName extension
        this.subjectAltName =
                getX509Certificate().getExtensionValue(SUBJECT_ALTERNATIVE_NAME_EXTENSION);

        if (this.subjectAltName != null) {
            // TODO: Finish
            this.subjectAltNameString = Hex.encodeHexString(this.subjectAltName);
        }
    }

    /**
     * Construct a new IDevIDCertificate by parsing the file at the given path.
     * The given certificate should represent a valid X.509 certificate.
     *
     * @param certificatePath the path on disk to a certificate
     * @throws IOException if there is a problem reading the file
     */
    public IDevIDCertificate(final Path certificatePath)
            throws IOException {
        super(certificatePath);

        // TODO: Extract device serial number (or identifier) from DN
        // TODO: Instantiate DevID class object and/or unique fields
        // TODO: Support for subjectAltName extension
        this.subjectAltName =
                getX509Certificate().getExtensionValue(SUBJECT_ALTERNATIVE_NAME_EXTENSION);

        if (this.subjectAltName != null) {
            // TODO: Finish
            this.subjectAltNameString = Hex.encodeHexString(this.subjectAltName);
        }
    }

    /**
     * Default constructor for Hibernate.
     */
    protected IDevIDCertificate() {
        subjectAltName = null;
    }

    @Override
    public int hashCode() {
        // TODO: Change
        return Arrays.hashCode(this.getRawBytes());
    }

    @Override
    public boolean equals(final Object o) {
        // TODO: Change
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Certificate that = (Certificate) o;

        return Arrays.equals(this.getRawBytes(), that.getRawBytes());
    }
}
