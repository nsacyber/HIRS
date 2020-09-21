package hirs.data.persist;

import java.util.Arrays;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import hirs.persist.ReferenceManifestManager;
import hirs.persist.ReferenceManifestSelector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.persistence.Table;
import javax.xml.XMLConstants;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

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
    public static final String BASE_RIM = "Base";
    public static final String SUPPORT_RIM = "Support";

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
     * This class enables the retrieval of PlatformCredentials by their
     * attributes.
     */
    public static class Selector
            extends ReferenceManifestSelector<ReferenceManifest> {

        /**
         * Construct a new ReferenceManifestSelector that will use the given
         * {@link ReferenceManifestManager} to retrieve one or many Reference
         * Integrity Manifest.
         *
         * @param referenceManifestManager the RIM manager to be used to
         * retrieve RIMs
         */
        public Selector(final ReferenceManifestManager referenceManifestManager) {
            super(referenceManifestManager);
        }

        /**
         * Specify a manufacturer that certificates must have to be considered as matching.
         * @param rimType the manufacturer to query, not empty or null
         * @return this instance (for chaining further calls)
         */
    }

    /**
     * Holds the name of the 'rimHash' field.
     */
    public static final String RIM_HASH_FIELD = "rimHash";
    @Column(nullable = false)
    @JsonIgnore
    private final int rimHash;
    @Column(columnDefinition = "blob", nullable = false)
    @JsonIgnore
    private byte[] rimBytes;
    @Column(nullable = false)
    private String rimType = "Base";
    @Column
    private String tagId = null;
    @Column
    private String platformManufacturer = null;
    @Column
    private String platformManufacturerId = null;
    @Column
    private String platformModel = null;
    @Column(nullable = false)
    private String fileName = null;

    /**
     * Get a Selector for use in retrieving ReferenceManifest.
     *
     * @param rimMan the ReferenceManifestManager to be used to retrieve
     * persisted RIMs
     * @return a ReferenceManifest.Selector instance to use for retrieving RIMs
     */
    public static Selector select(final ReferenceManifestManager rimMan) {
        return new Selector(rimMan);
    }

    /**
     * Default constructor necessary for Hibernate.
     */
    protected ReferenceManifest() {
        super();
        this.rimBytes = null;
        this.rimHash = 0;
        this.rimType = null;
    }

    public ReferenceManifest(final byte[] rimBytes) {
        Preconditions.checkArgument(rimBytes != null,
                "Cannot construct a RIM from a null byte array");

        Preconditions.checkArgument(rimBytes.length > 0,
                "Cannot construct a RIM from an empty byte array");

        this.rimBytes = rimBytes.clone();
        this.rimHash = Arrays.hashCode(this.rimBytes);
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
    public int getRimHash() {
        return rimHash;
    }

    @Override
    public int hashCode() {
        return getRimHash();
    }
}
