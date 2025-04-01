package hirs.attestationca.persist.entity.userdefined.info;

import hirs.utils.StringValidator;
import hirs.utils.enums.DeviceInfoEnums;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * Used for representing the hardware info of a device.
 */
@ToString
@EqualsAndHashCode
@Getter
@Embeddable
public class HardwareInfo implements Serializable {

    @XmlElement
    @Column(nullable = false)
    private String manufacturer = DeviceInfoEnums.NOT_SPECIFIED;

    @XmlElement
    @Column(nullable = false)
    private String productName = DeviceInfoEnums.NOT_SPECIFIED;

    @XmlElement
    @Column(length = DeviceInfoEnums.MED_STRING_LENGTH, nullable = false)
    private String version = DeviceInfoEnums.NOT_SPECIFIED;

    @XmlElement
    @Column(nullable = false)
    private String systemSerialNumber = DeviceInfoEnums.NOT_SPECIFIED;

    @XmlElement
    @Column(nullable = false)
    private String chassisSerialNumber = DeviceInfoEnums.NOT_SPECIFIED;

    @XmlElement
    @Column(nullable = false)
    private String baseboardSerialNumber = DeviceInfoEnums.NOT_SPECIFIED;

    /**
     * Constructor used to create a populated firmware info object.
     *
     * @param manufacturer          String manufacturer name
     * @param productName           String product name info
     * @param version               String bios release date info
     * @param systemSerialNumber    String device serial number
     * @param chassisSerialNumber   String device chassis serial number
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
                    .maxLength(DeviceInfoEnums.LONG_STRING_LENGTH).getValue();
        }

        if (!StringUtils.isBlank(productName)) {
            this.productName = StringValidator.check(productName, "productName")
                    .maxLength(DeviceInfoEnums.LONG_STRING_LENGTH).getValue();
        }

        if (!StringUtils.isBlank(version)) {
            this.version = StringValidator.check(version, "version")
                    .maxLength(DeviceInfoEnums.MED_STRING_LENGTH).getValue();
        }

        if (!StringUtils.isBlank(systemSerialNumber)) {
            this.systemSerialNumber = StringValidator.check(systemSerialNumber,
                            "systemSerialNumber")
                    .maxLength(DeviceInfoEnums.LONG_STRING_LENGTH).getValue();
        }

        if (!StringUtils.isBlank(chassisSerialNumber)) {
            this.chassisSerialNumber = StringValidator.check(chassisSerialNumber,
                            "chassisSerialNumber")
                    .maxLength(DeviceInfoEnums.LONG_STRING_LENGTH).getValue();
        }

        if (!StringUtils.isBlank(baseboardSerialNumber)) {
            this.baseboardSerialNumber = StringValidator.check(
                            baseboardSerialNumber, "baseboardSerialNumber")
                    .maxLength(DeviceInfoEnums.LONG_STRING_LENGTH).getValue();
        }
    }

    /**
     * Default constructor, useful for hibernate and marshalling and unmarshalling.
     */
    public HardwareInfo() {
        this(
                DeviceInfoEnums.NOT_SPECIFIED,
                DeviceInfoEnums.NOT_SPECIFIED,
                DeviceInfoEnums.NOT_SPECIFIED,
                DeviceInfoEnums.NOT_SPECIFIED,
                DeviceInfoEnums.NOT_SPECIFIED,
                DeviceInfoEnums.NOT_SPECIFIED
        );
    }
}
