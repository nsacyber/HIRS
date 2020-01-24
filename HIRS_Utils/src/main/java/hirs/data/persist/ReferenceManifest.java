package hirs.data.persist;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
//import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import hirs.persist.ReferenceManifestManager;
import hirs.persist.ReferenceManifestSelector;
import hirs.utils.SwidTagGateway;
import hirs.utils.xjc.SoftwareIdentity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 */
@Entity
@Access(AccessType.FIELD)
public class ReferenceManifest extends ArchivableEntity  {

    private static final int MAX_CERT_LENGTH_BYTES = 2048;
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * This class enables the retrieval of PlatformCredentials by their attributes.
     */
    public static class Selector extends ReferenceManifestSelector<ReferenceManifest> {
        /**
         * Construct a new ReferenceManifestSelector that will use the given {@link ReferenceManifestManager} to
         * retrieve one or many Reference Integrity Manifest.
         *
         * @param referenceManifestManager the RIM manager to be used to retrieve RIMs
         */
        public Selector(final ReferenceManifestManager referenceManifestManager) {
            super(referenceManifestManager);
        }
    }

    @Column
    private String manufacturer = null;
    @Column
    private String model = null;
    @Column
    private String firmwareVersion = null;
    @Column
    private String tagId = null;
    @Column
    private String rimType = null;
    @Column(length = MAX_CERT_LENGTH_BYTES, nullable = false)
    @JsonIgnore
    private byte[] rimBytes;
    /**
     * Holds the name of the 'rimHash' field.
     */
    public static final String RIM_HASH_FIELD = "rimHash";
    @Column(nullable = false)
    @JsonIgnore
    private final int rimHash;

    /**
     * Holds the different RIM types.
     */
    public enum RimType {
        /**
        * Primary Reference Integrity Manifest.
        */
        PRIMARY_RIM("Primary"),
        /**
        * Supplemental Reference Integrity Manifest.
        */
        SUPPLEMENTAL_RIM("Supplemental"),
        /**
        * Patch Reference Integrity Manifest.
        */
        PATCH_RIM("Patch");

        private String type;

        /**
         * Default constructor.
         * @param type a string for the type.
         */
        RimType(final String type) {
            this.type = type;
        }

        /**
         * Assessor for RIM Type.
         * @return string for type
         */
        public String getType() {
            return type;
        }
    }

    /**
     * Get a Selector for use in retrieving ReferenceManifest.
     *
     * @param rimMan the ReferenceManifestManager to be used to retrieve persisted RIMs
     * @return a ReferenceManifest.Selector instance to use for retrieving RIMs
     */
    public static Selector select(final ReferenceManifestManager rimMan) {
        return new Selector(rimMan);
    }

    /**
     * Default constructor of given name.
     */
    protected ReferenceManifest() {
        super();
        this.manufacturer = null;
        this.model = null;
        this.firmwareVersion = null;
        this.tagId = null;
        this.rimBytes = null;
        this.rimHash = 0;
    }

    /**
     *
     *
     * @param rimBytes byte array representation of the RIM     *
     * @throws JAXBException
     *             if unable to unmarshal the string
     */
    public ReferenceManifest(final byte[] rimBytes) throws IOException {
        Preconditions.checkArgument(
                rimBytes != null,
                "Cannot construct a RIM from a null byte array"
        );

        Preconditions.checkArgument(
                rimBytes.length > 0,
                "Cannot construct a RIM from an empty byte array"
        );

        this.rimBytes = rimBytes.clone();


        SoftwareIdentity si = (new SwidTagGateway()).validateSwidTag(
                new ByteArrayInputStream(rimBytes));

        if (si != null) {
            for (Object object : si.getEntityOrEvidenceOrLink()) {
                if (object instanceof JAXBElement) {
                    JAXBElement element = (JAXBElement) object;
                }
            }
        }

        this.rimHash = Arrays.hashCode(this.rimBytes);
    }

    /**
     * Getter for the manufacturer info.
     * @return string for the manufacturer
     */
    public String getManufacturer() {
        return manufacturer;
    }

    /**
     * Setter for the manufacturer info.
     * @param manufacturer passed in info.
     */
    public void setManufacturer(final String manufacturer) {
        this.manufacturer = manufacturer;
    }

    /**
     * Getter for the model info.
     * @return string for the model
     */
    public String getModel() {
        return model;
    }

    /**
     * Setter for the Model info.
     * @param model passed in model
     */
    public void setModel(final String model) {
        this.model = model;
    }

    /**
     * Getter for the firmware version info.
     * @return string for the firmware version
     */
    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    /**
     * Setter for the firmware version info.
     * @param firmwareVersion passed in firmware version
     */
    public void setFirmwareVersion(final String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    /**
     * Getter for the RIM Tag ID.
     * @return string for the RIM tag id
     */
    public String getTagId() {
        return tagId;
    }

    /**
     * Setter for the RIM Tag ID.
     * @param tagId passed in RIM Tag ID
     */
    public void setTagId(final String tagId) {
        this.tagId = tagId;
    }

    /**
     * Getter for the RIM Type (Primary, Supplemental, Patch).
     * @return string for the RIM Type
     */
    public String getRimType() {
        return rimType;
    }

    /**
     * Setter for the RIM Type.
     * @param type passed in RIM Type
     */
    public void setRimType(final String type) {
        this.rimType = type;
    }

    /**
     * Getter for the Reference Integrity Manifest as a byte array
     * @return
     */
    public byte[] getRimBytes() {
        return rimBytes;
    }

    /**
     * Setter for the Reference Integrity Manifest as a byte array
     * @param rimBytes
     */
    public void setRimBytes(byte[] rimBytes) {
        this.rimBytes = rimBytes;
    }

    /**
     * Getter for the Reference Integrity Manifest hash value.
     * @return int representation of the hash value
     */
    public int getRimHash() {
        return rimHash;
    }
}
