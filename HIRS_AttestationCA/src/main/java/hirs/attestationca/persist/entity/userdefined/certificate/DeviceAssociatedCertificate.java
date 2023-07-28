package hirs.attestationca.persist.entity.userdefined.certificate;

import hirs.attestationca.persist.entity.userdefined.Certificate;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

/**
 * A Certificate that is associated with a single device.
 *
 * @see Certificate
 */
@NoArgsConstructor(access= AccessLevel.PACKAGE)
@MappedSuperclass
public abstract class DeviceAssociatedCertificate extends Certificate {

    // a device can have multiple certs of this type.
    @Getter
    @Setter
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column
    private UUID deviceId;

    /**
     * Holds the name of the entity 'DEVICE_ID' field.
     */
    protected static final String DEVICE_ID_FIELD = "device_id";

    /**
     * Construct a new Certificate by parsing the file at the given path.  The given certificate
     * should represent either an X509 certificate or X509 attribute certificate.
     *
     * @param certificatePath the path on disk to a certificate
     * @throws IOException if there is a problem reading the file
     */
    DeviceAssociatedCertificate(final Path certificatePath) throws IOException {
        super(certificatePath);
    }

    /**
     * Construct a new Certificate given its binary contents.  The given certificate should
     * represent either an X509 certificate or X509 attribute certificate.
     *
     * @param certificateBytes the contents of a certificate file
     * @throws IOException if there is a problem extracting information from the certificate
     */
    DeviceAssociatedCertificate(final byte[] certificateBytes) throws IOException {
        super(certificateBytes);
    }
}
