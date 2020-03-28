package hirs.data.persist.info;

import hirs.data.persist.DeviceInfoReport;
import hirs.utils.StringValidator;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;
import java.util.Objects;

/**
 * Used for representing the hardware info of a device.
 */
@Embeddable
public class HardwareInfo implements Serializable {

    @XmlElement
    @Column(length = DeviceInfoReport.LONG_STRING_LENGTH, nullable = false)
    private String manufacturer = DeviceInfoReport.NOT_SPECIFIED;

    @XmlElement
    @Column(length = DeviceInfoReport.LONG_STRING_LENGTH, nullable = false)
    private String productName = DeviceInfoReport.NOT_SPECIFIED;

    @XmlElement
    @Column(length = DeviceInfoReport.MED_STRING_LENGTH, nullable = false)
    private String version = DeviceInfoReport.NOT_SPECIFIED;

    @XmlElement
    @Column(length = DeviceInfoReport.LONG_STRING_LENGTH, nullable = false)
    private String systemSerialNumber = DeviceInfoReport.NOT_SPECIFIED;

    @XmlElement
    @Column(length = DeviceInfoReport.LONG_STRING_LENGTH, nullable = false)
    private String chassisSerialNumber = DeviceInfoReport.NOT_SPECIFIED;

    @XmlElement
    @Column(length = DeviceInfoReport.LONG_STRING_LENGTH, nullable = false)
    private String baseboardSerialNumber = DeviceInfoReport.NOT_SPECIFIED;

    /**
     * Constructor used to create a populated firmware info object.
     *
     * @param manufacturer String manufacturer name
     * @param productName String product name info
     * @param version String bios release date info
     * @param systemSerialNumber String device serial number
     * @param chassisSerialNumber String device chassis serial number
     * @param baseboardSerialNumber String device baseboard serial number
     */
    public HardwareInfo(
            final String manufacturer,
            final String productName,
            final String version,
            final String systemSerialNumber,
            final String chassisSerialNumber,
            final String baseboardSerialNumber
    ) {
        if (!StringUtils.isBlank(manufacturer)) {
            this.manufacturer = StringValidator.check(manufacturer, "manufacturer")
                    .maxLength(DeviceInfoReport.LONG_STRING_LENGTH).get();
        }

        if (!StringUtils.isBlank(productName)) {
            this.productName = StringValidator.check(productName, "productName")
                    .maxLength(DeviceInfoReport.LONG_STRING_LENGTH).get();
        }

        if (!StringUtils.isBlank(version)) {
            this.version = StringValidator.check(version, "version")
                    .maxLength(DeviceInfoReport.MED_STRING_LENGTH).get();
        }

        if (!StringUtils.isBlank(systemSerialNumber)) {
            this.systemSerialNumber = StringValidator.check(systemSerialNumber,
                    "systemSerialNumber")
                    .maxLength(DeviceInfoReport.LONG_STRING_LENGTH).get();
        }

        if (!StringUtils.isBlank(chassisSerialNumber)) {
            this.chassisSerialNumber = StringValidator.check(chassisSerialNumber,
                    "chassisSerialNumber")
                    .maxLength(DeviceInfoReport.LONG_STRING_LENGTH).get();
        }

        if (!StringUtils.isBlank(baseboardSerialNumber)) {
            this.baseboardSerialNumber = StringValidator.check(
                    baseboardSerialNumber, "baseboardSerialNumber")
                    .maxLength(DeviceInfoReport.LONG_STRING_LENGTH).get();
        }
    }

    /**
     * Default constructor, useful for hibernate and marshalling and unmarshalling.
     */
    public HardwareInfo() {
        this(
                DeviceInfoReport.NOT_SPECIFIED,
                DeviceInfoReport.NOT_SPECIFIED,
                DeviceInfoReport.NOT_SPECIFIED,
                DeviceInfoReport.NOT_SPECIFIED,
                DeviceInfoReport.NOT_SPECIFIED,
                DeviceInfoReport.NOT_SPECIFIED
        );
    }

    /**
     * Retrieves the manufacturer info.
     *
     * @return manufacturer info, cannot be null
     */
    public final String getManufacturer() {
        return this.manufacturer;
    }

    /**
     * Retrieves the product name info.
     *
     * @return product name info, cannot be null
     */
    public final String getProductName() {
        return this.productName;
    }

    /**
     * Retrieves the version info.
     *
     * @return version info, cannot be null
     */
    public final String getVersion() {
        return this.version;
    }

    /**
     * Retrieves the serial number of the device.
     *
     * @return a String representing the serial number of the device
     */
    public final String getSystemSerialNumber() {
        return systemSerialNumber;
    }

    /**
     * Retrieves the chassis serial number of the device.
     *
     * @return a String representing the chassis serial number of the device
     */
    public final String getChassisSerialNumber() {
        return chassisSerialNumber;
    }

    /**
     * Retrieves the baseboard serial number of the device.
     *
     * @return a String representing the baseboard serial number of the device
     */
    public String getBaseboardSerialNumber() {
        return baseboardSerialNumber;
    }

    @Override
    public String toString() {
        return "HardwareInfo{"
                + "manufacturer='" + manufacturer + '\''
                + ", productName='" + productName + '\''
                + ", version='" + version + '\''
                + ", systemSerialNumber='" + systemSerialNumber + '\''
                + ", chassisSerialNumber='" + chassisSerialNumber + '\''
                + ", baseboardSerialNumber='" + baseboardSerialNumber + '\''
                + '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HardwareInfo that = (HardwareInfo) o;
        return Objects.equals(manufacturer, that.manufacturer)
                && Objects.equals(productName, that.productName)
                && Objects.equals(version, that.version)
                && Objects.equals(systemSerialNumber, that.systemSerialNumber)
                && Objects.equals(chassisSerialNumber, that.chassisSerialNumber)
                && Objects.equals(baseboardSerialNumber, that.baseboardSerialNumber);
    }

    @Override
    public int hashCode() {

        return Objects.hash(manufacturer, productName, version, systemSerialNumber,
                chassisSerialNumber, baseboardSerialNumber);
    }
}
