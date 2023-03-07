package hirs.attestationca.persist.entity.userdefined.info;

import hirs.attestationca.persist.entity.userdefined.report.DeviceInfoReport;
import hirs.utils.StringValidator;
import jakarta.persistence.Column;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Used for representing the firmware info of a device, such as the BIOS information.
 */
@ToString
@EqualsAndHashCode
@Getter
public class FirmwareInfo implements Serializable {

    @XmlElement
    @Column(length = DeviceInfoReport.LONG_STRING_LENGTH, nullable = false)
    private final String biosVendor;

    @XmlElement
    @Column(length = DeviceInfoReport.LONG_STRING_LENGTH, nullable = false)
    private final String biosVersion;

    @XmlElement
    @Column(length = DeviceInfoReport.SHORT_STRING_LENGTH, nullable = false)
    private final String biosReleaseDate;

    /**
     * Constructor used to create a populated firmware info object.
     *
     * @param biosVendor String bios vendor name, i.e. Dell Inc.
     * @param biosVersion String bios version info, i.e. A11
     * @param biosReleaseDate String bios release date info, i.e. 03/12/2013
     */
    public FirmwareInfo(final String biosVendor, final String biosVersion,
                        final String biosReleaseDate) {
        this.biosVendor = StringValidator.check(biosVendor, "biosVendor")
                .notBlank().maxLength(DeviceInfoReport.LONG_STRING_LENGTH).getValue();

        this.biosVersion = StringValidator.check(biosVersion, "biosVersion")
                .notBlank().maxLength(DeviceInfoReport.LONG_STRING_LENGTH).getValue();

        this.biosReleaseDate = StringValidator.check(biosReleaseDate, "biosReleaseDate")
                .notBlank().maxLength(DeviceInfoReport.SHORT_STRING_LENGTH).getValue();
    }

    /**
     * Default constructor, useful for hibernate and marshalling and unmarshalling.
     */
    public FirmwareInfo() {
        this(DeviceInfoReport.NOT_SPECIFIED,
                DeviceInfoReport.NOT_SPECIFIED,
                DeviceInfoReport.NOT_SPECIFIED);
    }
}
