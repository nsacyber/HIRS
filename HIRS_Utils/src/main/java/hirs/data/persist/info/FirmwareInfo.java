package hirs.data.persist.info;

import hirs.utils.StringValidator;

import javax.persistence.Column;
import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

import static hirs.data.persist.info.HardwareInfo.LONG_STRING_LENGTH;
import static hirs.data.persist.info.HardwareInfo.SHORT_STRING_LENGTH;
import static hirs.data.persist.info.HardwareInfo.NOT_SPECIFIED;

/**
 * Used for representing the firmware info of a device, such as the BIOS information.
 */
public class FirmwareInfo implements Serializable {

    @XmlElement
    @Column(length = LONG_STRING_LENGTH, nullable = false)
    private final String biosVendor;

    @XmlElement
    @Column(length = LONG_STRING_LENGTH, nullable = false)
    private final String biosVersion;

    @XmlElement
    @Column(length = SHORT_STRING_LENGTH, nullable = false)
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
                .notBlank().maxLength(LONG_STRING_LENGTH).get();

        this.biosVersion = StringValidator.check(biosVersion, "biosVersion")
                .notBlank().maxLength(LONG_STRING_LENGTH).get();

        this.biosReleaseDate = StringValidator.check(biosReleaseDate, "biosReleaseDate")
                .notBlank().maxLength(SHORT_STRING_LENGTH).get();
    }

    /**
     * Default constructor, useful for hibernate and marshalling and unmarshalling.
     */
    public FirmwareInfo() {
        this(NOT_SPECIFIED, NOT_SPECIFIED, NOT_SPECIFIED);
    }

    /**
     * Retrieves the BIOS biosVendor info.
     *
     * @return biosVendor info, cannot be null
     */
    public final String getBiosVendor() {
        return this.biosVendor;
    }

    /**
     * Retrieves the BIOS biosVersion info.
     *
     * @return biosVersion info, cannot be null
     */
    public final String getBiosVersion() {
        return this.biosVersion;
    }

    /**
     * Retrieves the BIOS release date info.
     *
     * @return release date info, cannot be null
     */
    public final String getBiosReleaseDate() {
        return this.biosReleaseDate;
    }

    @Override
    public String toString() {
        return String.format("%s, %s, %s", biosVendor, biosVersion, biosReleaseDate);
    }

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + biosVendor.hashCode();
        result = prime * result + biosVersion.hashCode();
        result = prime * result + biosReleaseDate.hashCode();
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
        if (!(obj instanceof FirmwareInfo)) {
            return false;
        }
        FirmwareInfo other = (FirmwareInfo) obj;

        if (biosVendor != null && !biosVendor.equals(other.biosVendor)) {
            return false;
        }
        if (biosVersion != null && !biosVersion.equals(other.biosVersion)) {
            return false;
        }
        if (biosReleaseDate != null && !biosReleaseDate.equals(other.biosReleaseDate)) {
            return false;
        }

        return true;
    }
}
