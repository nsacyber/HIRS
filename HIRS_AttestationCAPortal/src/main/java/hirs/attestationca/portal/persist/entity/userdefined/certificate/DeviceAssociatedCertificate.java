package hirs.attestationca.portal.persist.entity.userdefined.certificate;

import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.Device;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.IOException;
import java.nio.file.Path;

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
    @ManyToOne
    @JoinColumn(name = "device_id")
    private Device device;

    /**
     * Holds the name of the entity 'DEVICE_ID' field.
     */
    protected static final String DEVICE_ID_FIELD = "device.id";

    /**
     * Construct a new Certificate by parsing the file at the given path.  The given certificate
     * should represent either an X509 certificate or X509 attribute certificate.
     *
     * @param certificatePath the path on disk to a certificate
     * @throws java.io.IOException if there is a problem reading the file
     */
    DeviceAssociatedCertificate(final Path certificatePath) throws IOException {
        super(certificatePath);
    }

    /**
     * Construct a new Certificate given its binary contents.  The given certificate should
     * represent either an X509 certificate or X509 attribute certificate.
     *
     * @param certificateBytes the contents of a certificate file
     * @throws java.io.IOException if there is a problem extracting information from the certificate
     */
    DeviceAssociatedCertificate(final byte[] certificateBytes) throws IOException {
        super(certificateBytes);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        if (device != null) {
            sb.append(String.format("%nDevice -> %s", getDevice().toString()));
        }

        return sb.toString();
    }
}
