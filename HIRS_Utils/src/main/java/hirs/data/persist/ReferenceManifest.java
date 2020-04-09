package hirs.data.persist;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.xml.namespace.QName;
import javax.xml.bind.JAXBElement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import hirs.persist.DBReferenceManifestManager;
import hirs.persist.ReferenceManifestManager;
import hirs.persist.ReferenceManifestSelector;
import hirs.utils.xjc.BaseElement;
import hirs.utils.xjc.ResourceCollection;
import hirs.utils.xjc.SoftwareIdentity;
import hirs.utils.xjc.SoftwareMeta;
import hirs.utils.xjc.Meta;
import hirs.utils.xjc.Directory;
import hirs.utils.xjc.FilesystemItem;
import java.io.InputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.persistence.Table;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.validation.Schema;

/**
 * This class represents the Reference Integrity Manifest object that will be
 * loaded into the DB and displayed in the ACA.
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
    private static JAXBContext jaxbContext;

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
    private String swidName = null;
    @Column
    private String swidVersion = null;
    @Column
    private boolean swidCorpus = false;
    @Column
    private boolean swidPatch = false;
    @Column
    private boolean swidSupplemental = false;
    @Column
    private String platformManufacturer = null;
    @Column
    private String platformManufacturerId = null;
    @Column
    private String platformModel = null;
    @Column
    private String firmwareVersion = null;
    @Column
    private String tagId = null;
    @Column
    private String rimType = null;
    @Column
    private String colloquialVersion = null;
    @Column
    private String product = null;
    @Column
    private String revision = null;
    @Column
    private String edition = null;
    @Column
    private String rimLinkHash = null;
    @Column
    private String bindingSpec = null;
    @Column
    private String bindingSpecVersion = null;
    @Column
    private String platformVersion = null;
    @Column
    private String payloadType = null;
    @Column
    private String pcURIGlobal = null;
    @Column
    private String pcURILocal = null;
    @Column(columnDefinition = "blob", nullable = false)
    @JsonIgnore
    private byte[] rimBytes;
    /**
     * Holds the name of the 'rimHash' field.
     */
    public static final String RIM_HASH_FIELD = "rimHash";
    @Column(nullable = false)
    @JsonIgnore
    private final int rimHash;

    private String entityName = null;
    private String entityRegId = null;
    private String entityRole = null;
    private String entityThumbprint = null;
    private String linkHref = null;
    private String linkRel = null;

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
        this.swidName = null;
        this.swidVersion = null;
        this.swidVersion = null;
        this.swidCorpus = false;
        this.swidPatch = false;
        this.swidSupplemental = false;
        this.platformManufacturer = null;
        this.platformManufacturerId = null;
        this.platformModel = null;
        this.firmwareVersion = null;
        this.tagId = null;
        this.rimBytes = null;
        this.rimHash = 0;
        this.linkHref = null;
        this.linkRel = null;
        this.entityName = null;
        this.entityRegId = null;
        this.entityRole = null;
        this.entityThumbprint = null;
        this.colloquialVersion = null;
        this.product = null;
        this.revision = null;
        this.edition = null;
        this.rimLinkHash = null;
        this.bindingSpec = null;
        this.bindingSpecVersion = null;
        this.platformVersion = null;
        this.payloadType = null;
        this.pcURIGlobal = null;
        this.pcURILocal = null;
    }

    /**
     * Main constructor for the RIM object. This takes in a byte array of a
     * valid swidtag file and parses the information.
     *
     * @param rimBytes byte array representation of the RIM
     * @throws IOException if unable to unmarshal the string
     */
    public ReferenceManifest(final byte[] rimBytes) throws IOException {
        Preconditions.checkArgument(rimBytes != null,
                "Cannot construct a RIM from a null byte array");

        Preconditions.checkArgument(rimBytes.length > 0,
                "Cannot construct a RIM from an empty byte array");

        this.rimBytes = rimBytes.clone();

        SoftwareIdentity si = validateSwidTag(new ByteArrayInputStream(rimBytes));

        // begin parsing valid swid tag
        if (si != null) {
            this.tagId = si.getTagId();
            this.swidName = si.getName();
            this.swidCorpus = si.isCorpus();
            this.swidPatch = si.isPatch();
            this.swidSupplemental = si.isSupplemental();
            this.swidVersion = si.getVersion();

            for (Object object : si.getEntityOrEvidenceOrLink()) {
                if (object instanceof JAXBElement) {
                    JAXBElement element = (JAXBElement) object;
                    String elementName = element.getName().getLocalPart();
                    switch (elementName) {
                        case "Meta":
                            parseSoftwareMeta((SoftwareMeta) element.getValue());
                            break;
                        case "Entity":
                            hirs.utils.xjc.Entity entity
                                    = (hirs.utils.xjc.Entity) element.getValue();
                            if (entity != null) {
                                this.entityName = entity.getName();
                                this.entityRegId = entity.getRegid();
                                StringBuilder sb = new StringBuilder();
                                for (String role : entity.getRole()) {
                                    sb.append(String.format("%s%n", role));
                                }
                                this.entityRole = sb.toString();
                                this.entityThumbprint = entity.getThumbprint();
                            }
                            break;
                        case "Link":
                            hirs.utils.xjc.Link link
                                    = (hirs.utils.xjc.Link) element.getValue();
                            if (link != null) {
                                this.linkHref = link.getHref();
                                this.linkRel = link.getRel();
                            }
                            break;
                        case "Payload":
                            parseResource((ResourceCollection) element.getValue());
                            break;
                        case "Signature":
                        // left blank for a followup issue enhancement
                        default:
                    }
                }
            }
        }

        this.rimHash = Arrays.hashCode(this.rimBytes);
    }

    /**
     * This method and code is pulled and adopted from the TCG Tool. Since this
     * is taking in an file stored in memory through http, this was changed from
     * a file to a stream as the input.
     *
     * @param fileStream stream of the swidtag file.
     * @return a {@link SoftwareIdentity} object
     * @throws IOException Thrown by the unmarhsallSwidTag method.
     */
    private SoftwareIdentity validateSwidTag(final InputStream fileStream) throws IOException {
        JAXBElement jaxbe = unmarshallSwidTag(fileStream);
        SoftwareIdentity swidTag = (SoftwareIdentity) jaxbe.getValue();

        LOGGER.info(String.format("SWID Tag found: %nname: %s;%ntagId:  %s%n%s",
                swidTag.getName(), swidTag.getTagId(), SCHEMA_STATEMENT));
        return swidTag;
    }

    /**
     * Helper method that is used to parse a specific element of the SwidTag
     * based on an already established and stored byte array.
     *
     * @param elementName string of an xml tag in the file.
     * @return the object value of the element, if it exists
     */
    private BaseElement getBaseElementFromBytes(final String elementName) {
        BaseElement baseElement = null;

        if (rimBytes != null && elementName != null) {
            try {
                SoftwareIdentity si = validateSwidTag(new ByteArrayInputStream(this.rimBytes));
                JAXBElement element;
                for (Object object : si.getEntityOrEvidenceOrLink()) {
                    if (object instanceof JAXBElement) {
                        element = (JAXBElement) object;
                        if (element.getName().getLocalPart().equals(elementName)) {
                            // found the element
                            baseElement = (BaseElement) element.getValue();
                        }
                    }
                }

            } catch (IOException ioEx) {
                LOGGER.error("Failed to parse Swid Tag bytes.", ioEx);
            }
        }

        return baseElement;
    }

    /**
     * This is a helper method that parses the SoftwareMeta tag and stores the
     * information in the class fields.
     *
     * @param softwareMeta The object to parse.
     */
    private void parseSoftwareMeta(final SoftwareMeta softwareMeta) {
        if (softwareMeta != null) {
            for (Map.Entry<QName, String> entry
                    : softwareMeta.getOtherAttributes().entrySet()) {
                switch (entry.getKey().getLocalPart()) {
                    case "colloquialVersion":
                        this.colloquialVersion = entry.getValue();
                        break;
                    case "product":
                        this.product = entry.getValue();
                        break;
                    case "revision":
                        this.revision = entry.getValue();
                        break;
                    case "edition":
                        this.edition = entry.getValue();
                        break;
                    case "rimLinkHash":
                        this.rimLinkHash = entry.getValue();
                        break;
                    case "bindingSpec":
                        this.bindingSpec = entry.getValue();
                        break;
                    case "bindingSpecVersion":
                        this.bindingSpecVersion = entry.getValue();
                        break;
                    case "platformManufacturerId":
                        this.platformManufacturerId = entry.getValue();
                        break;
                    case "platformModel":
                        this.platformModel = entry.getValue();
                        break;
                    case "platformManufacturerStr":
                        this.platformManufacturer = entry.getValue();
                        break;
                    case "platformVersion":
                        this.platformVersion = entry.getValue();
                        break;
                    case "payloadType":
                        this.payloadType = entry.getValue();
                        break;
                    case "pcURIGlobal":
                        this.pcURIGlobal = entry.getValue();
                        break;
                    case "pcURILocal":
                        this.pcURILocal = entry.getValue();
                        break;
                    default:
                }
            }
        }
    }

    /**
     * Default method for parsing the payload element.
     *
     * @return a collection of payload objects.
     */
    public final List<SwidResource> parseResource() {
        return parseResource((ResourceCollection) this.getBaseElementFromBytes("Payload"));
    }

    /**
     * This method parses the payload method of a {@link ResourceCollection}.
     *
     * @param rc Resource Collection object.
     * @return a collection of payload objects.
     */
    public final List<SwidResource> parseResource(final ResourceCollection rc) {
        List<SwidResource> resources = new ArrayList<>();

        try {
            if (rc != null) {
                for (Meta meta : rc.getDirectoryOrFileOrProcess()) {
                    if (meta != null) {
                        if (meta instanceof Directory) {
                            Directory directory = (Directory) meta;
                            for (FilesystemItem fsi : directory.getDirectoryOrFile()) {
                                if (fsi != null) {
                                    resources.add(new SwidResource(
                                            (hirs.utils.xjc.File) fsi, null));
                                }
                            }
                        } else if (meta instanceof hirs.utils.xjc.File) {
                            resources.add(new SwidResource((hirs.utils.xjc.File) meta, null));
                        }
                    }
                }
            }
        } catch (ClassCastException ccEx) {
            LOGGER.error(ccEx);
            LOGGER.error("At this time, the code does not support the "
                    + "particular formatting of this SwidTag's Payload.");
        }

        return resources;
    }

    /**
     * This method unmarshalls the swidtag found at [path] and validates it
     * according to the schema.
     *
     * @param stream to the input swidtag
     * @return the SoftwareIdentity element at the root of the swidtag
     * @throws IOException if the swidtag cannot be unmarshalled or validated
     */
    private JAXBElement unmarshallSwidTag(final InputStream stream) throws IOException {
        JAXBElement jaxbe = null;
        Schema schema;

        try {
            schema = DBReferenceManifestManager.getSchemaObject();
            if (jaxbContext == null) {
                jaxbContext = JAXBContext.newInstance(SCHEMA_PACKAGE);
            }
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            unmarshaller.setSchema(schema);
            jaxbe = (JAXBElement) unmarshaller.unmarshal(stream);
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
        }

        if (jaxbe != null) {
            return jaxbe;
        } else {
            throw new IOException("Invalid swidtag file!");
        }
    }

    /**
     * Getter for the SWID name parameter.
     *
     * @return string representation of the SWID name
     */
    public String getSwidName() {
        return swidName;
    }

    /**
     * Setter for the SWID name parameter.
     *
     * @param swidName string of the name
     */
    public void setSwidName(final String swidName) {
        this.swidName = swidName;
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
     * Getter for the corpus flag.
     *
     * @return boolean flag for corpus
     */
    public boolean isSwidCorpus() {
        return swidCorpus;
    }

    /**
     * Setter for the corpus flag.
     *
     * @param swidCorpus boolean value
     */
    public void setSwidCorpus(final boolean swidCorpus) {
        this.swidCorpus = swidCorpus;
    }

    /**
     * Getter for the patch flag.
     *
     * @return boolean flag for the patch flag
     */
    public boolean isSwidPatch() {
        return swidPatch;
    }

    /**
     * Setter for the patch flag.
     *
     * @param swidPatch boolean value
     */
    public void setSwidPatch(final boolean swidPatch) {
        this.swidPatch = swidPatch;
    }

    /**
     * Getter for the supplemental flag.
     *
     * @return boolean flag for the supplemental flag
     */
    public boolean isSwidSupplemental() {
        return swidSupplemental;
    }

    /**
     * Setter for the supplemental flag.
     *
     * @param swidSupplemental boolean value
     */
    public void setSwidSupplemental(final boolean swidSupplemental) {
        this.swidSupplemental = swidSupplemental;
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

    /**
     * Getter for the Entity Name.
     *
     * @return string of the entity name.
     */
    public String getEntityName() {
        return entityName;
    }

    /**
     * Setter for the Entity Name.
     *
     * @param entityName string of the entity name.
     */
    public void setEntityName(final String entityName) {
        this.entityName = entityName;
    }

    /**
     * Getter for the Entity Reg ID.
     *
     * @return string of the entity reg id.
     */
    public String getEntityRegId() {
        return entityRegId;
    }

    /**
     * Setter for the Entity Reg ID.
     *
     * @param entityRegId string of the entity reg id.
     */
    public void setEntityRegId(final String entityRegId) {
        this.entityRegId = entityRegId;
    }

    /**
     * Getter for the Entity Role.
     *
     * @return string of the entity role.
     */
    public String getEntityRole() {
        return entityRole;
    }

    /**
     * Setter for the Entity Role.
     *
     * @param entityRole string of the entity role .
     */
    public void setEntityRole(final String entityRole) {
        this.entityRole = entityRole;
    }

    /**
     * Getter for the Entity thumbprint.
     *
     * @return string of the entity thumbprint.
     */
    public String getEntityThumbprint() {
        return entityThumbprint;
    }

    /**
     * Setter for the Entity Thumbprint.
     *
     * @param entityThumbprint string of the entity thumbprint.
     */
    public void setEntityThumbprint(final String entityThumbprint) {
        this.entityThumbprint = entityThumbprint;
    }

    /**
     * Getter for the Link Href.
     *
     * @return string of the link href.
     */
    public String getLinkHref() {
        return linkHref;
    }

    /**
     * Setter for the Link href.
     *
     * @param linkHref in string representation.
     */
    public void setLinkHref(final String linkHref) {
        this.linkHref = linkHref;
    }

    /**
     * Getter for the Link Rel.
     *
     * @return string of the link rel
     */
    public String getLinkRel() {
        return linkRel;
    }

    /**
     * Setter for the Link Rel.
     *
     * @param linkRel in string representation.
     */
    public void setLinkRel(final String linkRel) {
        this.linkRel = linkRel;
    }

    /**
     * Getter for Colloquial Version.
     *
     * @return string of the colloquial version.
     */
    public String getColloquialVersion() {
        return colloquialVersion;
    }

    /**
     * Setter for Colloquial Version.
     *
     * @param colloquialVersion in string representation.
     */
    public void setColloquialVersion(final String colloquialVersion) {
        this.colloquialVersion = colloquialVersion;
    }

    /**
     * Getter for Product.
     *
     * @return string of the product information
     */
    public String getProduct() {
        return product;
    }

    /**
     * Setter for the Product.
     *
     * @param product in string representation.
     */
    public void setProduct(final String product) {
        this.product = product;
    }

    /**
     * Getter for the Revision string.
     *
     * @return string of revision information.
     */
    public String getRevision() {
        return revision;
    }

    /**
     * Setter for the Revision.
     *
     * @param revision in string representation.
     */
    public void setRevision(final String revision) {
        this.revision = revision;
    }

    /**
     * Getter for the Edition.
     *
     * @return string of edition information.
     */
    public String getEdition() {
        return edition;
    }

    /**
     * Setter for the Edition string.
     *
     * @param edition in string representation.
     */
    public void setEdition(final String edition) {
        this.edition = edition;
    }

    /**
     * Getter for the RIM Link Hash.
     *
     * @return string of the RIM link hash.
     */
    public String getRimLinkHash() {
        return rimLinkHash;
    }

    /**
     * Setter for the RIM link hash.
     *
     * @param rimLinkHash in string representation.
     */
    public void setRimLinkHash(final String rimLinkHash) {
        this.rimLinkHash = rimLinkHash;
    }

    /**
     * Getter for the Binding Spec.
     *
     * @return string of Binding spec.
     */
    public String getBindingSpec() {
        return bindingSpec;
    }

    /**
     * Setter for the Binding Spec.
     *
     * @param bindingSpec in string representation.
     */
    public void setBindingSpec(final String bindingSpec) {
        this.bindingSpec = bindingSpec;
    }

    /**
     * Getter for the Binding Spec Version.
     *
     * @return string of binding spec version.
     */
    public String getBindingSpecVersion() {
        return bindingSpecVersion;
    }

    /**
     * Setter for the binding spec version.
     *
     * @param bindingSpecVersion in string representation.
     */
    public void setBindingSpecVersion(final String bindingSpecVersion) {
        this.bindingSpecVersion = bindingSpecVersion;
    }

    /**
     * Getter for the Platform Version.
     *
     * @return string of platform version.
     */
    public String getPlatformVersion() {
        return platformVersion;
    }

    /**
     * Setter for the Platform Version.
     *
     * @param platformVersion in string representation.
     */
    public void setPlatformVersion(final String platformVersion) {
        this.platformVersion = platformVersion;
    }

    /**
     * Getter for the Payload Type.
     *
     * @return string of payload type.
     */
    public String getPayloadType() {
        return payloadType;
    }

    /**
     * Setter for the Payload type.
     *
     * @param payloadType in string representation.
     */
    public void setPayloadType(final String payloadType) {
        this.payloadType = payloadType;
    }

    /**
     * Getter for the PC URI Global.
     *
     * @return string of Pc URI Global.
     */
    public String getPcURIGlobal() {
        return pcURIGlobal;
    }

    /**
     * Setter for the PC URI Global.
     *
     * @param pcURIGlobal in string representation.
     */
    public void setPcURIGlobal(final String pcURIGlobal) {
        this.pcURIGlobal = pcURIGlobal;
    }

    /**
     * Getter for the PC URI Local.
     *
     * @return string of PC URI Local.
     */
    public String getPcURILocal() {
        return pcURILocal;
    }

    /**
     * Setter for the PC URI Local.
     *
     * @param pcURILocal in string representation.
     */
    public void setPcURILocal(final String pcURILocal) {
        this.pcURILocal = pcURILocal;
    }

    @Override
    public String toString() {
        return String.format("ReferenceManifest{swidName=%s,"
                + "platformManufacturer=%s,"
                + " platformModel=%s,"
                + "firmwareVersion=%s, firmwareVersion=%s, rimHash=%d}",
                swidName, platformManufacturer,
                platformModel, firmwareVersion, tagId, rimHash);
    }
}
