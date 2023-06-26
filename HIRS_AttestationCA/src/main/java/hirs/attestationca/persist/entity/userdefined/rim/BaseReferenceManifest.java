package hirs.attestationca.persist.entity.userdefined.rim;

import com.fasterxml.jackson.annotation.JsonIgnore;
import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
import hirs.attestationca.persist.service.ReferenceManifestServiceImpl;
import hirs.utils.SwidResource;
import hirs.utils.xjc.BaseElement;
import hirs.utils.xjc.Directory;
import hirs.utils.xjc.File;
import hirs.utils.xjc.FilesystemItem;
import hirs.utils.xjc.Link;
import hirs.utils.xjc.Meta;
import hirs.utils.xjc.ResourceCollection;
import hirs.utils.xjc.SoftwareIdentity;
import hirs.utils.xjc.SoftwareMeta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.UnmarshalException;
import jakarta.xml.bind.Unmarshaller;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

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
@Log4j2
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class BaseReferenceManifest extends ReferenceManifest {
    /**
     * Holds the name of the 'base64Hash' field.
     */
    public static final String BASE_64_HASH_FIELD = "base64Hash";

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
            log.error(noSaEx);
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
                            Link link
                                    = (Link) element.getValue();
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

        log.debug(String.format("SWID Tag found: %nname: %s;%ntagId:  %s%n%s",
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
                log.error("Failed to parse Swid Tag bytes.", ioEx);
            }
        }

        return baseElement;
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
            schema = ReferenceManifestServiceImpl.getSchemaObject();
            if (jaxbContext == null) {
                jaxbContext = JAXBContext.newInstance(SCHEMA_PACKAGE);
            }
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            unmarshaller.setSchema(schema);
            jaxbe = (JAXBElement) unmarshaller.unmarshal(stream);
        } catch (UnmarshalException umEx) {
            log.error(String.format("Error validating swidtag file!%n%s%n%s",
                    umEx.getMessage(), umEx.toString()));
            for (StackTraceElement ste : umEx.getStackTrace()) {
                log.error(ste.toString());
            }
        } catch (IllegalArgumentException iaEx) {
            log.error("Input file empty.");
        } catch (JAXBException jaxEx) {
            for (StackTraceElement ste : jaxEx.getStackTrace()) {
                log.error(ste.toString());
            }
        }

        if (jaxbe != null) {
            return jaxbe;
        } else {
            throw new IOException("Invalid Base RIM, swidtag format expected.");
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

        log.error("Parsing stuff");
        try {
            if (rc != null) {
                for (Meta meta : rc.getDirectoryOrFileOrProcess()) {
                    if (meta instanceof Directory) {
                        Directory directory = (Directory) meta;
                        for (FilesystemItem fsi : directory.getDirectoryOrFile()) {
                            if (fsi != null) {
                                resources.add(new SwidResource(
                                        (File) fsi, null));
                            } else {
                                log.error("fsi is negative");
                            }
                        }
                    } else if (meta instanceof File) {
                        resources.add(new SwidResource((File) meta, null));
                    }
                }
            } else {
                log.error("ResourceCollection is negative");
            }
        } catch (ClassCastException ccEx) {
            log.error(ccEx);
            log.error("At this time, the code does not support the "
                    + "particular formatting of this SwidTag's Payload.");
        }

        return resources;
    }

    @Override
    public String toString() {
        return String.format("ReferenceManifest{swidName=%s,"
                        + "platformManufacturer=%s,"
                        + " platformModel=%s,"
                        + "tagId=%s, base64Hash=%s}",
                swidName, this.getPlatformManufacturer(),
                this.getPlatformModel(), getTagId(), this.getBase64Hash());
    }
}
