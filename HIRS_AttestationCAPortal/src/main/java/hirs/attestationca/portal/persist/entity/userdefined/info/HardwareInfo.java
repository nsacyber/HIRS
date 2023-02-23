package hirs.attestationca.portal.persist.entity.userdefined.info;

import hirs.attestationca.persist.entity.userdefined.report.DeviceInfoReport;
import hirs.attestationca.utils.StringValidator;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * Used for representing the hardware info of a device.
 */
@EqualsAndHashCode
@Getter
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
            final String baseboardSerialNumber) {
        if (!StringUtils.isBlank(manufacturer)) {
            this.manufacturer = StringValidator.check(manufacturer, "manufacturer")
                    .maxLength(DeviceInfoReport.LONG_STRING_LENGTH).getValue();
        }

        if (!StringUtils.isBlank(productName)) {
            this.productName = StringValidator.check(productName, "productName")
                    .maxLength(DeviceInfoReport.LONG_STRING_LENGTH).getValue();
        }

        if (!StringUtils.isBlank(version)) {
            this.version = StringValidator.check(version, "version")
                    .maxLength(DeviceInfoReport.MED_STRING_LENGTH).getValue();
        }

        if (!StringUtils.isBlank(systemSerialNumber)) {
            this.systemSerialNumber = StringValidator.check(systemSerialNumber,
                    "systemSerialNumber")
                    .maxLength(DeviceInfoReport.LONG_STRING_LENGTH).getValue();
        }

        if (!StringUtils.isBlank(chassisSerialNumber)) {
            this.chassisSerialNumber = StringValidator.check(chassisSerialNumber,
                    "chassisSerialNumber")
                    .maxLength(DeviceInfoReport.LONG_STRING_LENGTH).getValue();
        }

        if (!StringUtils.isBlank(baseboardSerialNumber)) {
            this.baseboardSerialNumber = StringValidator.check(
                    baseboardSerialNumber, "baseboardSerialNumber")
                    .maxLength(DeviceInfoReport.LONG_STRING_LENGTH).getValue();
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
}
