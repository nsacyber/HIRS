package hirs.data.persist;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.Type;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.xml.XMLConstants;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.UUID;

/**
 * This class represents the Reference Integrity Manifest object that will be
 * loaded into the DB and displayed in the ACA.
 */
@Entity
@Table(name = "ReferenceManifest")
@XmlRootElement(name = "ReferenceManifest")
@XmlAccessorType(XmlAccessType.FIELD)
@Access(AccessType.FIELD)
public abstract class ReferenceManifest extends ArchivableEntity {
    /**
     * String for display of a Base RIM.
     */
    public static final String BASE_RIM = "Base";
    /**
     * String for display of a Support RIM.
     */
    public static final String SUPPORT_RIM = "Support";
    /**
     * String for display of a Support RIM.
     */
    public static final String MEASUREMENT_RIM = "Measurement";

    /**
     * String for the xml schema ios standard.
     */
    public static final String SCHEMA_STATEMENT = "ISO/IEC 19770-2:2015 Schema (XSD 1.0) "
            + "- September 2015, see http://standards.iso.org/iso/19770/-2/2015/schema.xsd";
    /**
     * String for the xml schema URL file name.
     */
    public static final String SCHEMA_URL = "swid_schema.xsd";
    /**
     * String for the language type for the xml schema.
     */
    public static final String SCHEMA_LANGUAGE = XMLConstants.W3C_XML_SCHEMA_NS_URI;
    /**
     * String for the package location of the xml generated java files.
     */
    public static final String SCHEMA_PACKAGE = "hirs.utils.xjc";

    private static final Logger LOGGER = LogManager.getLogger(ReferenceManifest.class);

    /**
     * Holds the name of the 'rimHash' field.
     */
    public static final String RIM_HASH_FIELD = "rimHash";
    @Column(nullable = false)
    @JsonIgnore
    private final String rimHash;
    @Column(columnDefinition = "blob", nullable = false)
    @JsonIgnore
    private byte[] rimBytes;
    @Column(nullable = false)
    private String rimType = "Base";
    @Column
    private String tagId = null;
    @Column
    private boolean swidPatch = false;
    @Column
    private boolean swidSupplemental = false;
    @Column
    private String platformManufacturer = null;
    @Column
    private String platformManufacturerId = null;
    @Column
    private String swidTagVersion = null;
    @Column
    private String swidVersion = null;
    @Column
    private String platformModel = null;
    @Column(nullable = false)
    private String fileName = null;
    @Type(type = "uuid-char")
    @Column
    private UUID associatedRim;

    /**
     * Default constructor necessary for Hibernate.
     */
    protected ReferenceManifest() {
        super();
        this.rimBytes = null;
        this.rimHash = "";
        this.rimType = null;
        this.platformManufacturer = null;
        this.platformManufacturerId = null;
        this.platformModel = null;
        this.fileName = BASE_RIM;
        this.tagId = null;
        this.associatedRim = null;
    }

    /**
     * Default constructor for ingesting the bytes of the file content.
     * @param rimBytes - file contents.
     */
    public ReferenceManifest(final byte[] rimBytes) {
        Preconditions.checkArgument(rimBytes != null,
                "Cannot construct a RIM from a null byte array");

        Preconditions.checkArgument(rimBytes.length > 0,
                "Cannot construct a RIM from an empty byte array");

        this.rimBytes = rimBytes.clone();

        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException noSaEx) {
            LOGGER.error(noSaEx);
        }
        if (digest == null) {
            this.rimHash = "";
        } else {
            this.rimHash = Hex.encodeHexString(
                    digest.digest(rimBytes));
        }
    }

    /**
     * Getter for the file name of the data that was uploaded.
     * @return the file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Setter for the file name of the data that was uploaded.
     * @param fileName file name to associate
     */
    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    /**
     * Getter for the platformManufacturer info.
     *
     * @return string for the platformManufacturer
     */
    public String getPlatformManufacturer() {
        return platformManufacturer;
    }

    /**
     * Setter for the platformManufacturer info.
     *
     * @param platformManufacturer passed in info.
     */
    public void setPlatformManufacturer(final String platformManufacturer) {
        this.platformManufacturer = platformManufacturer;
    }

    /**
     * Getter for the platform manufacturer ID.
     *
     * @return string for the platform manufacturer id.
     */
    public String getPlatformManufacturerId() {
        return platformManufacturerId;
    }

    /**
     * Setter for the platform manufacturer ID.
     *
     * @param platformManufacturerId passed in info
     */
    public void setPlatformManufacturerId(final String platformManufacturerId) {
        this.platformManufacturerId = platformManufacturerId;
    }

    /**
     * Getter for the platformModel info.
     *
     * @return string for the platformModel
     */
    public String getPlatformModel() {
        return platformModel;
    }

    /**
     * Setter for the platformModel info.
     *
     * @param platformModel passed in platformModel
     */
    public void setPlatformModel(final String platformModel) {
        this.platformModel = platformModel;
    }

    /**
     * Getter for the RIM Type (Primary, Supplemental, Patch).
     *
     * @return string for the RIM Type
     */
    public String getRimType() {
        return rimType;
    }

    /**
     * Setter for the RIM Type.
     *
     * @param rimType passed in RIM Type
     */
    public void setRimType(final String rimType) {
        this.rimType = rimType;
    }

    /**
     * Getter for the SWID tag version.
     *
     * @return string of the tag version number
     */
    public String getSwidTagVersion() {
        return swidTagVersion;
    }

    /**
     * Setter for the SWID tag version.
     *
     * @param swidTagVersion string of the version
     */
    public void setSwidTagVersion(final String swidTagVersion) {
        this.swidTagVersion = swidTagVersion;
    }

    /**
     * Getter for the SWID version.
     *
     * @return string of the version number
     */
    public String getSwidVersion() {
        return swidVersion;
    }

    /**
     * Setter for the SWID version.
     *
     * @param swidVersion string of the version
     */
    public void setSwidVersion(final String swidVersion) {
        this.swidVersion = swidVersion;
    }

    /**
     * Getter for the RIM Tag ID.
     *
     * @return string for the RIM tag id
     */
    public String getTagId() {
        return tagId;
    }

    /**
     * Setter for the RIM Tag ID.
     *
     * @param tagId passed in RIM Tag ID
     */
    public void setTagId(final String tagId) {
        this.tagId = tagId;
    }

    /**
     * Getter for the patch flag.
     *
     * @return int flag for the patch flag
     */
    public boolean isSwidPatch() {
        return swidPatch;
    }

    /**
     * Setter for the patch flag.
     *
     * @param swidPatch int value
     */
    public void setSwidPatch(final boolean swidPatch) {
        this.swidPatch = swidPatch;
    }

    /**
     * Getter for the supplemental flag.
     *
     * @return int flag for the supplemental flag
     */
    public boolean isSwidSupplemental() {
        return swidSupplemental;
    }

    /**
     * Setter for the supplemental flag.
     *
     * @param swidSupplemental int value
     */
    public void setSwidSupplemental(final boolean swidSupplemental) {
        this.swidSupplemental = swidSupplemental;
    }

    /**
     * Getter for the associated RIM DB ID.
     * @return UUID for the rim
     */
    public UUID getAssociatedRim() {
        return associatedRim;
    }

    /**
     * Setter for the associated RIM DB ID.
     * @param associatedRim UUID for the rim
     */
    public void setAssociatedRim(final UUID associatedRim) {
        this.associatedRim = associatedRim;
    }

    /**
     * Getter for the Reference Integrity Manifest as a byte array.
     *
     * @return array of bytes
     */
    @JsonIgnore
    public byte[] getRimBytes() {
        if (this.rimBytes != null) {
            return this.rimBytes.clone();
        }
        return null;
    }

    /**
     * Getter for the Reference Integrity Manifest hash value.
     *
     * @return int representation of the hash value
     */
    public String getRimHash() {
        return rimHash;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.rimBytes);
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        if (!super.equals(object)) {
            return false;
        }
        ReferenceManifest that = (ReferenceManifest) object;
        return rimHash == that.rimHash
                && Arrays.equals(rimBytes, that.rimBytes)
                && rimType.equals(that.rimType)
                && tagId.equals(that.tagId)
                && platformManufacturer.equals(that.platformManufacturer)
                && platformManufacturerId.equals(that.platformManufacturerId)
                && platformModel.equals(that.platformModel)
                && fileName.equals(that.fileName);
    }

    @Override
    public String toString() {
        return String.format("Filename->%s%nPlatform Manufacturer->%s%n"
                + "Platform Model->%s%nRIM Type->%s%nRIM Hash->%s", this.getFileName(),
                this.platformManufacturer, this.platformModel, this.getRimType(),
                this.getRimHash());
    }
}
