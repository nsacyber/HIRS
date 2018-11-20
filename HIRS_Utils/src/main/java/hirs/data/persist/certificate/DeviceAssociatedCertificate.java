package hirs.data.persist.certificate;

import hirs.data.persist.Device;

import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import java.io.IOException;
import java.nio.file.Path;

/**
 * A Certificate that is associated with a single device.
 *
 * @see Certificate
 */
@MappedSuperclass
public abstract class DeviceAssociatedCertificate extends Certificate {

    // a device can have multiple certs of this type.
    @ManyToOne
    @JoinColumn(name = "device_id")
    private Device device;

    /**
     * Holds the name of the entity 'DEVICE_ID' field.
     */
    protected static final String DEVICE_ID_FIELD = "device.id";


    /**
     * Default Constructor.
     */
    DeviceAssociatedCertificate() {
    }

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

    /**
     * Gets the device.
     * @return the device
     */
    public Device getDevice() {
        return device;
    }

    /**
     * Sets the device.
     * @param device the device
     */
    public void setDevice(final Device device) {
        this.device = device;
    }
}
