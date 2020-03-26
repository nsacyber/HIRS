package hirs.data.persist;

import hirs.utils.StringValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

/**
 * This class is used to represent the OS info of a device.
 */
@Embeddable
public class OSInfo implements Serializable {
    private static final Logger LOGGER = LogManager.getLogger(OSInfo.class);

    @XmlElement
    @Column(length = DeviceInfoReport.LONG_STRING_LENGTH, nullable = false)
    private final String osName;

    @XmlElement
    @Column(length = DeviceInfoReport.LONG_STRING_LENGTH, nullable = false)
    private final String osVersion;

    @XmlElement
    @Column(length = DeviceInfoReport.SHORT_STRING_LENGTH, nullable = false)
    private final String osArch;

    @XmlElement
    @Column(length = DeviceInfoReport.SHORT_STRING_LENGTH, nullable = true)
    private final String distribution;

    @XmlElement
    @Column(length = DeviceInfoReport.SHORT_STRING_LENGTH, nullable = true)
    private final String distributionRelease;

    /**
     * Constructor used to create an OSInfo object. This constructor takes an OS
     * name (Linux | Mac OS X | Windows 7), an OS version (i.e.
     * 3.10.0-123.el7.x86_64), OS architecture (x86_64), distribution (CentOS |
     * Fedora), and distribution release (7.0.1406). Distribution only makes
     * sense for Linux, so distribution and distributionRelease may be null.
     *
     * @param osName
     *            String OS name (Linux | Mac OS X | Windows 7)
     * @param osVersion
     *            String OS version (i.e. 3.10.0-123.el7.x86_64)
     * @param osArch
     *            String OS architecture (x86_64)
     * @param distribution
     *            String distribution (CentOS | Fedora)
     * @param distributionRelease
     *            String distribution release (7.0.1406)
     */
    public OSInfo(final String osName, final String osVersion,
            final String osArch, final String distribution,
            final String distributionRelease) {
        LOGGER.debug("setting OS name information to: {}", osName);
        this.osName = StringValidator.check(osName, "osName")
                .notNull().maxLength(DeviceInfoReport.LONG_STRING_LENGTH).get();

        LOGGER.debug("setting OS version information to: {}", osVersion);
        this.osVersion = StringValidator.check(osVersion, "osVersion")
                .notNull().maxLength(DeviceInfoReport.LONG_STRING_LENGTH).get();

        LOGGER.debug("setting OS arch information to: {}", osArch);
        this.osArch = StringValidator.check(osArch, "osArch")
                .notNull().maxLength(DeviceInfoReport.SHORT_STRING_LENGTH).get();

        LOGGER.debug("setting OS distribution information to: {}", distribution);
        this.distribution = StringValidator.check(distribution, "distribution")
                .maxLength(DeviceInfoReport.SHORT_STRING_LENGTH).get();

        LOGGER.debug("setting OS distribution release information to: {}",
                distributionRelease);
        this.distributionRelease = StringValidator.check(distributionRelease, "distributionRelease")
                .maxLength(DeviceInfoReport.SHORT_STRING_LENGTH).get();
    }

    /**
     * Default constructor necessary for marshalling/unmarshalling XML objects.
     */
    public OSInfo() {
        this(DeviceInfoReport.NOT_SPECIFIED,
            DeviceInfoReport.NOT_SPECIFIED,
            DeviceInfoReport.NOT_SPECIFIED,
            DeviceInfoReport.NOT_SPECIFIED,
            DeviceInfoReport.NOT_SPECIFIED);
    }

    /**
     * Used to retrieve the OS name.
     *
     * @return a String representing the OS name
     */
    public final String getOSName() {
        return osName;
    }

    /**
     * Used to retrieve the OS version of the device.
     *
     * @return a String representing the OS version
     */
    public final String getOSVersion() {
        return osVersion;
    }

    /**
     * Used to retrieve the OS arch information of the device.
     *
     * @return a String representing the arch information
     */
    public final String getOSArch() {
        return osArch;
    }

    /**
     * Used to retrieve the distribution information set for the device.
     *
     * @return a String representing the distribution
     */
    public final String getDistribution() {
        return distribution;
    }

    /**
     * Used to retrieve the distribution release information set for the device.
     *
     * @return a String representing the distribution release
     */
    public final String getDistributionRelease() {
        return distributionRelease;
    }

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        int distributionHashCode;
        int distributionReleaseHashCode;
        if (distribution == null) {
            distributionHashCode = 0;
        } else {
            distributionHashCode = distribution.hashCode();
        }
        if (distributionRelease == null) {
            distributionReleaseHashCode = 0;
        } else {
            distributionReleaseHashCode = distributionRelease.hashCode();
        }
        result = prime * result + osName.hashCode();
        result = prime * result + osVersion.hashCode();
        result = prime * result + osArch.hashCode();
        result = prime * result + distributionHashCode;
        result = prime * result + distributionReleaseHashCode;
        return result;
    }

    @Override
    public final boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof OSInfo)) {
            return false;
        }
        OSInfo other = (OSInfo) obj;

        if (osName != null && !osName.equals(other.osName)) {
            return false;
        }
        if (osVersion != null && !osVersion.equals(other.osVersion)) {
            return false;
        }
        if (osArch != null && !osArch.equals(other.osArch)) {
            return false;
        }
        if (distribution != null && !distribution.equals(other.distribution)) {
            return false;
        }
        if (distributionRelease != null
                && !distributionRelease.equals(other.distributionRelease)) {
            return false;
        }

        return true;
    }
}
