package hirs.data.persist;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.xml.namespace.QName;
import javax.xml.bind.JAXBElement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import hirs.persist.ReferenceManifestManager;
import hirs.persist.ReferenceManifestSelector;
import hirs.utils.xjc.ResourceCollection;
import hirs.utils.xjc.SoftwareIdentity;
import hirs.utils.xjc.SoftwareMeta;
import java.io.InputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Map;
import javax.persistence.Table;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.xml.sax.SAXException;

/**
 *
 */
@Entity
@Table(name = "ReferenceManifest")
@XmlRootElement(name = "ReferenceManifest")
@XmlAccessorType(XmlAccessType.FIELD)
@Access(AccessType.FIELD)
public class ReferenceManifest extends ArchivableEntity {

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
    private static final int MAX_CERT_LENGTH_BYTES = 2048;

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
    }

    @Column
    private String platformManufacturer = null;
    @Column
    private String componentManufacturer = null;
    @Column
    private String platformModel = null;
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
        PRIMARY,
        /**
         * Supplemental Reference Integrity Manifest.
         */
        SUPPLEMENTAL,
        /**
         * Patch Reference Integrity Manifest.
         */
        PATCH;
    }

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
        this.platformManufacturer = null;
        this.componentManufacturer = null;
        this.platformModel = null;
        this.firmwareVersion = null;
        this.tagId = null;
        this.rimBytes = null;
        this.rimHash = 0;
    }

    /**
     *
     *
     *
     * @param rimBytes byte array representation of the RIM
     *
     * @throws IOException if unable to unmarshal the string
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

        SoftwareIdentity si = validateSwidTag(new ByteArrayInputStream(rimBytes));

        if (si != null) {
            this.tagId = si.getTagId();
            for (Object object : si.getEntityOrEvidenceOrLink()) {
                if (object instanceof JAXBElement) {
                    JAXBElement element = (JAXBElement) object;
                        LOGGER.error(element.toString());
                        LOGGER.error(element.getName().getNamespaceURI());
                        LOGGER.error(element.getName().getLocalPart());
                        LOGGER.error(element.getValue().toString());
                        LOGGER.error("---------------------------------");
                        String elementName = element.getName().getLocalPart();
                    if (elementName.equals("Meta")) {
                        SoftwareMeta sm = (SoftwareMeta) element.getValue();
                        if (sm != null) {
                            LOGGER.error(sm.getProduct());
                            LOGGER.error(sm.getProductFamily());
                            LOGGER.error(sm.getRevision());

                            for (Map.Entry<QName, String> entry
                                    : sm.getOtherAttributes().entrySet()) {
                                switch (entry.getKey().getLocalPart()) {
                                    case "platformManufacturerStr":
                                        this.platformManufacturer = entry.getValue();
                                        break;
                                    case "componentManufacturer":
                                        this.componentManufacturer = entry.getValue();
                                        break;
                                    case "platformModel":
                                        this.platformModel = entry.getValue();
                                        break;
                                    default:

                                }
                            }
                        }
                    }
                    switch (elementName) {
                        case "Meta":
                            SoftwareMeta sm = (SoftwareMeta) element.getValue();
                            break;
                        case "Payload":
                            ResourceCollection rc = (ResourceCollection) element.getValue();
                            break;
                        case "Link":
                        case "Entity":
                        default:
                    }
                }
            }
        }

        this.rimHash = Arrays.hashCode(this.rimBytes);
    }

    private SoftwareIdentity validateSwidTag(final InputStream fileStream) throws IOException {
        JAXBElement jaxbe = unmarshallSwidTag(fileStream);
        SoftwareIdentity swidTag = (SoftwareIdentity) jaxbe.getValue();

        LOGGER.error(String.format("SWID Tag found: %nname: %s;%ntagId:  %s%n%s",
                swidTag.getName(), swidTag.getTagId(), SCHEMA_STATEMENT));
        return swidTag;
    }

    /**
     * This method unmarshalls the swidtag found at [path] and validates it according to the
     * schema.
     *
     * @param path to the input swidtag
     * @return the SoftwareIdentity element at the root of the swidtag
     * @throws IOException if the swidtag cannot be unmarshalled or validated
     */
//    @SuppressWarnings("PMD")
    private JAXBElement unmarshallSwidTag(final InputStream stream) throws IOException {
        InputStream is = null;
        JAXBElement jaxbe = null;
        try {
            is = ReferenceManifest.class.getClassLoader().getResourceAsStream(SCHEMA_URL);
            SchemaFactory schemaFactory = SchemaFactory.newInstance(SCHEMA_LANGUAGE);
            Schema schema = schemaFactory.newSchema(new StreamSource(is));
            JAXBContext jaxbContext = JAXBContext.newInstance(SCHEMA_PACKAGE);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            unmarshaller.setSchema(schema);
            jaxbe = (JAXBElement) unmarshaller.unmarshal(stream);
        } catch (SAXException saxEx) {
            LOGGER.error(String.format("Error setting schema for validation!%n%s",
                    saxEx.getMessage()));
        } catch (UnmarshalException umEx) {
            LOGGER.error(String.format("Error validating swidtag file!%n%s%n%s",
                    umEx.getMessage(), umEx.toString()));
            for (StackTraceElement ste : umEx.getStackTrace()) {
                LOGGER.error(ste.toString());
            }
        } catch (IllegalArgumentException iaEx) {
            LOGGER.error("Input file empty.");
        } catch (JAXBException jaxEx) {
            for (StackTraceElement ste : jaxEx.getStackTrace()) {
                LOGGER.error(ste.toString());
            }
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ioEx) {
                    LOGGER.error(String.format("Error closing input stream%n%s",
                            ioEx.getMessage()));
                }
            } else {
                LOGGER.error("Input stream variable is null");
            }
        }

        if (jaxbe != null) {
            return jaxbe;
        } else {
            throw new IOException("Invalid swidtag file!");
        }
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
     * Getter for the componenetManufacturer info.
     *
     * @return string for the compononentManufacturer.
     */
    public String getComponentManufacturer() {
        return componentManufacturer;
    }

    /**
     * Setter for the componentManufacturer info.
     *
     * @param componentManufacturer passed in info.
     */
    public void setComponentManufacturer(final String componentManufacturer) {
        this.componentManufacturer = componentManufacturer;
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
     * Getter for the firmware version info.
     *
     * @return string for the firmware version
     */
    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    /**
     * Setter for the firmware version info.
     *
     * @param firmwareVersion passed in firmware version
     */
    public void setFirmwareVersion(final String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
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
     * @param timType passed in RIM Type
     */
    public void setRimType(final String timType) {
        this.rimType = timType;
    }

    /**
     * Getter for the Reference Integrity Manifest as a byte array.
     *
     * @return array of bytes
     */
    public byte[] getRimBytes() {
        return rimBytes.clone();
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
    public String toString() {
        return String.format("ReferenceManifest{platformManufacturer=%s,"
                + "componentManufacturer=%s, platformModel=%s,"
                + "firmwareVersion=%s, firmwareVersion=%s, rimHash=%d}",
                platformManufacturer, componentManufacturer,
                platformModel, firmwareVersion, tagId, rimHash);
    }
}
