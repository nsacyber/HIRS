package hirs.data.persist;

import com.fasterxml.jackson.annotation.JsonIgnore;
import hirs.persist.DBReferenceManifestManager;
import hirs.persist.ReferenceManifestManager;
import hirs.persist.ReferenceManifestSelector;
import hirs.utils.xjc.BaseElement;
import hirs.utils.xjc.Directory;
import hirs.utils.xjc.FilesystemItem;
import hirs.utils.xjc.Meta;
import hirs.utils.xjc.ResourceCollection;
import hirs.utils.xjc.SoftwareIdentity;
import hirs.utils.xjc.SoftwareMeta;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.validation.Schema;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 *
 */
@Entity
public class BaseReferenceManifest extends ReferenceManifest {
    private static final Logger LOGGER = LogManager.getLogger(BaseReferenceManifest.class);
    /**
     * Holds the name of the 'base64Hash' field.
     */
    public static final String BASE_64_HASH_FIELD = "base64Hash";
    /**
     * Holds the name of the 'base64Hash' field.
     */
    public static final String RIM_LINK_FIELD = "rimLinkHash";

    private static JAXBContext jaxbContext;

    @Column
    @JsonIgnore
    private String base64Hash = "";
    @Column
    private String swidName = null;
    @Column
    private int swidCorpus = 0;
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

    private String entityName = null;
    private String entityRegId = null;
    private String entityRole = null;
    private String entityThumbprint = null;
    private String linkHref = null;
    private String linkRel = null;

    /**
     * This class enables the retrieval of BaseReferenceManifest by their attributes.
     */
    public static class Selector extends ReferenceManifestSelector<BaseReferenceManifest> {
        /**
         * Construct a new ReferenceManifestSelector that will use
         * the given (@link ReferenceManifestManager}
         * to retrieve one or may BaseReferenceManifest.
         *
         * @param referenceManifestManager the reference manifest manager to be used to retrieve
         * reference manifests.
         */
        public Selector(final ReferenceManifestManager referenceManifestManager) {
            super(referenceManifestManager, BaseReferenceManifest.class);
        }

        /**
         * Specify the platform manufacturer that rims must have to be considered
         * as matching.
         * @param manufacturer string for the manufacturer
         * @return this instance
         */
        public Selector byManufacturer(final String manufacturer) {
            setFieldValue(PLATFORM_MANUFACTURER, manufacturer);
            return this;
        }

        /**
         * Specify the platform model that rims must have to be considered
         * as matching.
         * @param model string for the model
         * @return this instance
         */
        public Selector byModel(final String model) {
            setFieldValue(PLATFORM_MODEL, model);
            return this;
        }

        /**
         * Specify the platform manufacturer/model that rims must have to be considered
         * as matching.
         * @param manufacturer string for the manufacturer
         * @param model string for the model
         * @return this instance
         */
        public Selector byManufacturerModel(final String manufacturer, final String model) {
            setFieldValue(PLATFORM_MANUFACTURER, manufacturer);
            setFieldValue(PLATFORM_MODEL, model);
            return this;
        }

        /**
         * Specify the platform manufacturer/model/base flag that rims must have to be considered
         * as matching.
         * @param manufacturer string for the manufacturer
         * @param model string for the model
         * @return this instance
         */
        public Selector byManufacturerModelBase(final String manufacturer, final String model) {
            setFieldValue(PLATFORM_MANUFACTURER, manufacturer);
            setFieldValue(PLATFORM_MODEL, model);
            setFieldValue("swidPatch", false);
            setFieldValue("swidSupplemental", false);
            //setFieldValue("", false); //corpus?
            return this;
        }

        /**
         * Specify the device name that rims must have to be considered
         * as matching.
         * @param deviceName string for the deviceName
         * @return this instance
         */
        public Selector byDeviceName(final String deviceName) {
            setFieldValue("deviceName", deviceName);
            return this;
        }

        /**
         * Specify the RIM hash associated with the base RIM.
         * @param base64Hash the hash of the file associated with the rim
         * @return this instance
         */
        public Selector byBase64Hash(final String base64Hash) {
            setFieldValue(BASE_64_HASH_FIELD, base64Hash);
            return this;
        }

        /**
         * Specify the RIM hash associated with the base RIM.
         * @param rimLinkHash the hash of the file associated with the rim
         * @return this instance
         */
        public Selector byRimLink(final String rimLinkHash) {
            setFieldValue(RIM_LINK_FIELD, rimLinkHash);
            return this;
        }

        /**
         * Specify the RIM hash associated with the base RIM.
         * @param hexDecHash the hash of the file associated with the rim
         * @return this instance
         */
        public Selector byHexDecHash(final String hexDecHash) {
            setFieldValue(HEX_DEC_HASH_FIELD, hexDecHash);
            return this;
        }
    }

    /**
     * Support constructor for the RIM object.
     *
     * @param fileName - string representation of the uploaded file.
     * @param rimBytes - the file content of the uploaded file.
     * @throws IOException - thrown if the file is invalid.
     */
    public BaseReferenceManifest(final String fileName, final byte[] rimBytes) throws IOException {
        this(rimBytes);
        this.setFileName(fileName);
    }

    /**
     * Main constructor for the RIM object. This takes in a byte array of a
     * valid swidtag file and parses the information.
     *
     * @param rimBytes byte array representation of the RIM
     * @throws IOException if unable to unmarshal the string
     */
    @SuppressWarnings("checkstyle:AvoidInlineConditionals")
    public BaseReferenceManifest(final byte[] rimBytes) throws IOException {
        super(rimBytes);
        this.setRimType(BASE_RIM);
        this.setFileName("");
        SoftwareIdentity si = validateSwidTag(new ByteArrayInputStream(rimBytes));

        MessageDigest digest = null;
        this.base64Hash = "";
        try {
            digest = MessageDigest.getInstance("SHA-256");
            this.base64Hash = Base64.getEncoder().encodeToString(
                    digest.digest(rimBytes));
        } catch (NoSuchAlgorithmException noSaEx) {
            LOGGER.error(noSaEx);
        }

        // begin parsing valid swid tag
        if (si != null) {
            setTagId(si.getTagId());
            this.swidName = si.getName();
            this.swidCorpus = si.isCorpus() ? 1 : 0;
            this.setSwidPatch(si.isPatch());
            this.setSwidSupplemental(si.isSupplemental());
            this.setSwidVersion(si.getVersion());
            if (si.getTagVersion() != null) {
                this.setSwidTagVersion(si.getTagVersion().toString());
            }

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
    }

    /**
     * Default constructor necessary for Hibernate.
     */
    protected BaseReferenceManifest() {

    }

    /**
     * Get a Selector for use in retrieving ReferenceManifest.
     *
     * @param rimMan the ReferenceManifestManager to be used to retrieve
     * persisted RIMs
     * @return a Selector instance to use for retrieving RIMs
     */
    public static Selector select(final ReferenceManifestManager rimMan) {
        return new Selector(rimMan);
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

        if (getRimBytes() != null && elementName != null) {
            try {
                SoftwareIdentity si = validateSwidTag(new ByteArrayInputStream(getRimBytes()));
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
            throw new IOException("Invalid Base RIM, swidtag format expected.");
        }
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
                        this.setPlatformManufacturerId(entry.getValue());
                        break;
                    case "platformModel":
                        this.setPlatformModel(entry.getValue());
                        break;
                    case "platformManufacturerStr":
                        this.setPlatformManufacturer(entry.getValue());
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
     * Getter for the corpus flag.
     *
     * @return int flag for corpus
     */
    public int isSwidCorpus() {
        return swidCorpus;
    }

    /**
     * Setter for the corpus flag.
     *
     * @param swidCorpus int value
     */
    public void setSwidCorpus(final int swidCorpus) {
        this.swidCorpus = swidCorpus;
    }

    /**
     * The assumed requirement for being the initial swidtag.
     * @return flag for the status
     */
    public boolean isBase() {
        return !this.isSwidPatch() && !this.isSwidSupplemental()
                && (this.isSwidCorpus() == 0);
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

    /**
     * Getter for the Reference Integrity Manifest hash value.
     *
     * @return int representation of the hash value
     */
    public String getBase64Hash() {
        return base64Hash;
    }

    @Override
    public String toString() {
        return String.format("ReferenceManifest{swidName=%s,"
                        + "platformManufacturer=%s,"
                        + " platformModel=%s,"
                        + "tagId=%s, rimHash=%s}",
                swidName, this.getPlatformManufacturer(),
                this.getPlatformModel(), getTagId(), this.getBase64Hash());
    }
}
