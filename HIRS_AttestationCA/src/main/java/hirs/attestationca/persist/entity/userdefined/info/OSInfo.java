package hirs.attestationca.persist.entity.userdefined.info;

import hirs.utils.StringValidator;
import hirs.utils.enums.DeviceInfoEnums;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.Serializable;

/**
 * This class is used to represent the OS info of a device.
 */
@EqualsAndHashCode
@Getter
@Log4j2
@Embeddable
public class OSInfo implements Serializable {

    @XmlElement
    @Column(nullable = false)
    private final String osName;

    @XmlElement
    @Column(nullable = false)
    private final String osVersion;

    @XmlElement
    @Column(length = DeviceInfoEnums.SHORT_STRING_LENGTH, nullable = false)
    private final String osArch;

    @XmlElement
    @Column(length = DeviceInfoEnums.SHORT_STRING_LENGTH)
    private final String distribution;

    @XmlElement
    @Column(length = DeviceInfoEnums.SHORT_STRING_LENGTH)
    private final String distributionRelease;

    /**
     * Constructor used to create an OSInfo object. This constructor takes an OS
     * name (Linux | Mac OS X | Windows 7), an OS version (i.e.
     * 3.10.0-123.el7.x86_64), OS architecture (x86_64), distribution (CentOS |
     * Fedora), and distribution release (7.0.1406). Distribution only makes
     * sense for Linux, so distribution and distributionRelease may be null.
     *
     * @param osName              String OS name (Linux | Mac OS X | Windows 7)
     * @param osVersion           String OS version (i.e. 3.10.0-123.el7.x86_64)
     * @param osArch              String OS architecture (x86_64)
     * @param distribution        String distribution (CentOS | Fedora)
     * @param distributionRelease String distribution release (7.0.1406)
     */
    public OSInfo(final String osName, final String osVersion,
                  final String osArch, final String distribution,
                  final String distributionRelease) {
        log.debug("setting OS name information to: {}", osName);
        this.osName = StringValidator.check(osName, "osName")
                .notNull().maxLength(DeviceInfoEnums.LONG_STRING_LENGTH).getValue();

        log.debug("setting OS version information to: {}", osVersion);
        this.osVersion = StringValidator.check(osVersion, "osVersion")
                .notNull().maxLength(DeviceInfoEnums.LONG_STRING_LENGTH).getValue();

        log.debug("setting OS arch information to: {}", osArch);
        this.osArch = StringValidator.check(osArch, "osArch")
                .notNull().maxLength(DeviceInfoEnums.SHORT_STRING_LENGTH).getValue();

        log.debug("setting OS distribution information to: {}", distribution);
        this.distribution = StringValidator.check(distribution, "distribution")
                .maxLength(DeviceInfoEnums.SHORT_STRING_LENGTH).getValue();

        log.debug("setting OS distribution release information to: {}",
                distributionRelease);
        this.distributionRelease = StringValidator.check(distributionRelease, "distributionRelease")
                .maxLength(DeviceInfoEnums.SHORT_STRING_LENGTH).getValue();
    }

    /**
     * Default constructor necessary for marshalling/unmarshalling XML objects.
     */
    public OSInfo() {
        this(DeviceInfoEnums.NOT_SPECIFIED,
                DeviceInfoEnums.NOT_SPECIFIED,
                DeviceInfoEnums.NOT_SPECIFIED,
                DeviceInfoEnums.NOT_SPECIFIED,
                DeviceInfoEnums.NOT_SPECIFIED);
    }
}
