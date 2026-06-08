package hirs.attestationca.persist.entity.userdefined.rim;

import hirs.attestationca.persist.entity.userdefined.ReferenceManifest;
import hirs.attestationca.persist.entity.userdefined.certificate.CertificateAuthorityCredential;
import hirs.utils.SwidResource;
import hirs.utils.rim.SwidTagParser;
import hirs.utils.swid.SwidTagConstants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.xml.bind.UnmarshalException;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@Log4j2
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(callSuper = true)
@Entity
public class BaseReferenceManifest extends ReferenceManifest {
    /**
     * Holds the name of the 'base64Hash' field.
     */
    public static final String BASE_64_HASH_FIELD = "base64Hash";

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
    private String firmwareManufacturer = null;

    @Column
    private String firmwareManufacturerId = null;

    @Column
    private String firmwareModel = null;

    @Column
    private String firmwareVersion = null;

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

    private List<CertificateAuthorityCredential> embeddedCertificates = new ArrayList<>();

    /**
     * Support constructor for the RIM object.
     *
     * @param rimBytes - the file content of the uploaded file.
     * @throws IOException - thrown if file cannot be read.
     */
    public BaseReferenceManifest(final byte[] rimBytes) throws IOException, ParserConfigurationException,
            SAXException, UnmarshalException {
        this("", rimBytes);
    }

    /**
     * Main constructor for the RIM object. This takes in a byte array of a
     * valid swidtag file and parses the information.
     *
     * @param fileName - string representation of the uploaded file.
     * @param rimBytes byte array representation of the RIM
     * @throws IOException if unable to read file
     */
    public BaseReferenceManifest(final String fileName, final byte[] rimBytes)
            throws IOException, ParserConfigurationException, SAXException, UnmarshalException {
        super(rimBytes);
        this.setRimType(BASE_RIM);
        this.setFileName(fileName);
        Document document = SwidTagParser.validateSwidtagSchema(SwidTagParser.convertToDocument(rimBytes));
        Element softwareIdentity;
        Element meta;
        Element entity;
        Element link;

        // begin parsing valid swid tag
        if (document != null) {
            softwareIdentity = (Element) document.getElementsByTagNameNS(
                    SwidTagConstants.SWIDTAG_NAMESPACE, SwidTagConstants.SOFTWARE_IDENTITY).item(0);
            entity = (Element) document.getElementsByTagNameNS(
                    SwidTagConstants.SWIDTAG_NAMESPACE, SwidTagConstants.ENTITY).item(0);
            link = (Element) document.getElementsByTagNameNS(
                    SwidTagConstants.SWIDTAG_NAMESPACE, SwidTagConstants.LINK).item(0);
            meta = (Element) document.getElementsByTagNameNS(
                    SwidTagConstants.SWIDTAG_NAMESPACE, SwidTagConstants.META).item(0);
            setTagId(softwareIdentity.getAttribute(SwidTagConstants.TAGID));
            this.swidName = softwareIdentity.getAttribute(SwidTagConstants.NAME);
            this.swidCorpus =
                    Boolean.parseBoolean(softwareIdentity.getAttribute(SwidTagConstants.CORPUS)) ? 1 : 0;
            this.setSwidPatch(Boolean.parseBoolean(softwareIdentity.getAttribute(SwidTagConstants.PATCH)));
            this.setSwidSupplemental(
                    Boolean.parseBoolean(softwareIdentity.getAttribute(SwidTagConstants.SUPPLEMENTAL)));
            this.setSwidVersion(softwareIdentity.getAttribute(SwidTagConstants.VERSION));
            this.setSwidTagVersion(softwareIdentity.getAttribute(SwidTagConstants.TAGVERSION));
            this.setSwidVersionScheme(softwareIdentity.getAttribute(SwidTagConstants.VERSION_SCHEME));
            parseSoftwareMeta(meta);
            parseEntity(entity);
            parseLink(link);
        }
    }

    /**
     * This is a helper method that parses the SoftwareMeta tag and stores the
     * information in the class fields.
     *
     * @param softwareMeta The object to parse.
     */
    private void parseSoftwareMeta(final Element softwareMeta) {
        if (softwareMeta != null) {
            this.colloquialVersion = softwareMeta.getAttribute(SwidTagConstants.COLLOQUIAL_VERSION_STR);
            this.product = softwareMeta.getAttribute(SwidTagConstants.PRODUCT_STR);
            this.revision = softwareMeta.getAttribute(SwidTagConstants.REVISION_STR);
            this.edition = softwareMeta.getAttribute(SwidTagConstants.EDITION_STR);
            this.rimLinkHash = softwareMeta.getAttribute(SwidTagConstants.RIM_LINK_HASH_STR);
            this.bindingSpec = softwareMeta.getAttribute(SwidTagConstants.BINDING_SPEC_STR);
            this.bindingSpecVersion = softwareMeta.getAttribute(SwidTagConstants.BINDING_SPEC_VERSION_STR);
            this.setPlatformManufacturerId(
                    softwareMeta.getAttribute(SwidTagConstants.PLATFORM_MANUFACTURER_ID_STR));
            this.setPlatformManufacturer(
                    softwareMeta.getAttribute(SwidTagConstants.PLATFORM_MANUFACTURER_FULL_STR));
            this.setPlatformModel(softwareMeta.getAttribute(SwidTagConstants.PLATFORM_MODEL_STR));
            this.platformVersion = softwareMeta.getAttribute(SwidTagConstants.PLATFORM_VERSION_STR);
            this.setPayloadType(softwareMeta.getAttribute(SwidTagConstants.PAYLOAD_TYPE_STR));
            this.firmwareManufacturer = softwareMeta.getAttribute(SwidTagConstants.FIRMWARE_MANUFACTURER_FULL_STR);
            this.firmwareManufacturerId = softwareMeta.getAttribute(SwidTagConstants.FIRMWARE_MANUFACTURER_ID_STR);
            this.firmwareModel = softwareMeta.getAttribute(SwidTagConstants.FIRMWARE_MODEL_STR);
            this.firmwareVersion = softwareMeta.getAttribute(SwidTagConstants.FIRMWARE_VERSION_STR);
            this.pcURIGlobal = softwareMeta.getAttribute(SwidTagConstants.PC_URI_GLOBAL_STR);
            this.pcURILocal = softwareMeta.getAttribute(SwidTagConstants.PC_URI_LOCAL_STR);
        } else {
            log.warn("SoftwareMeta Tag not found.");
        }
    }

    /**
     * This is a helper method that parses the Entity tag and stores the
     * information in the class fields.
     *
     * @param entity The object to parse.
     */
    private void parseEntity(final Element entity) {
        if (entity != null) {
            this.entityName = entity.getAttribute(SwidTagConstants.NAME);
            this.entityRegId = entity.getAttribute(SwidTagConstants.REGID);
            this.entityRole = entity.getAttribute(SwidTagConstants.ROLE);
            this.entityThumbprint = entity.getAttribute(SwidTagConstants.THUMBPRINT);
        } else {
            log.warn("Entity Tag not found.");
        }
    }

    /**
     * This is a helper method that parses the Link tag and stores the
     * information in the class fields.
     *
     * @param link The object to parse.
     */
    private void parseLink(final Element link) {
        if (link != null) {
            this.linkHref = link.getAttribute(SwidTagConstants.HREF);
            this.linkRel = link.getAttribute(SwidTagConstants.REL);
        } else {
            log.warn("Link Tag not found.");
        }
    }

    /**
     * This method validates the .swidtag file at the given filepath against the
     * schema. A successful validation results in the output of the tag's name
     * and tagId attributes, otherwise a generic error message is printed.
     *
     * @param rimBytes byte array representation of the RIM
     * @return an element
     */
    private Element getDirectoryTag(final byte[] rimBytes) {
        if (rimBytes == null || rimBytes.length == 0) {
            return getDirectoryTag(new ByteArrayInputStream(getRimBytes()));
        } else {
            return getDirectoryTag(new ByteArrayInputStream(rimBytes));
        }
    }

    /**
     * This method validates the .swidtag file at the given filepath against the
     * schema. A successful validation results in the output of the tag's name
     * and tagId attributes, otherwise a generic error message is printed.
     *
     * @param byteArrayInputStream the location of the file to be validated
     * @return an element
     */
    private Element getDirectoryTag(final ByteArrayInputStream byteArrayInputStream) {
        Document document = null;
        try {
            document = SwidTagParser.validateSwidtagSchema(SwidTagParser.convertToDocument(
                    byteArrayInputStream.readAllBytes()));
        } catch (IOException e) {
            log.error(e.getMessage());
        } catch (ParserConfigurationException e) {
            log.error("Error encountered setting up to parse rim bytes: {}", e.getMessage());
            throw new RuntimeException(e);
        } catch (SAXException | UnmarshalException e) {
            log.error("Error while parsing Base RIM: {}", e.getMessage());
        }
        if (document != null) {
            Element softwareIdentity =
                    (Element) document.getElementsByTagNameNS(
                            SwidTagConstants.SWIDTAG_NAMESPACE, "SoftwareIdentity").item(0);
            if (softwareIdentity != null) {
                Element directory = (Element) document.getElementsByTagNameNS(
                        SwidTagConstants.SWIDTAG_NAMESPACE, "Directory").item(0);

                return directory;
            } else {
                log.error("Invalid xml for validation, please verify ");
            }
        }

        return null;
    }

    /**
     * This method iterates over the list of File elements under the directory.
     *
     * @return a list of swid resources
     */
    public List<SwidResource> getFileResources() {
        return getFileResources(getRimBytes());
    }

    /**
     * This method iterates over the list of File elements under the directory.
     *
     * @param rimBytes the bytes to find the files
     * @return a list of swid resources
     */
    public List<SwidResource> getFileResources(final byte[] rimBytes) {
        Element directoryTag = getDirectoryTag(rimBytes);
        List<SwidResource> validHashes = new ArrayList<>();
        NodeList fileNodeList = directoryTag.getChildNodes();
        Element file = null;
        SwidResource swidResource = null;
        for (int i = 0; i < fileNodeList.getLength(); i++) {
            file = (Element) fileNodeList.item(i);
            swidResource = new SwidResource();
            swidResource.setName(file.getAttribute(SwidTagConstants.NAME));
            swidResource.setSize(file.getAttribute(SwidTagConstants.SIZE));
            swidResource.setHashValue(file.getAttribute(SwidTagConstants.SHA_256_HASH.getPrefix() + ":"
                    + SwidTagConstants.SHA_256_HASH.getLocalPart()));
            swidResource.setRimFormat(
                    file.getAttributeNS(
                            SwidTagConstants.QNAME_SUPPORT_RIM_FORMAT.getNamespaceURI(),
                            SwidTagConstants.QNAME_SUPPORT_RIM_FORMAT.getLocalPart()));
            swidResource.setRimType(
                    file.getAttributeNS(
                            SwidTagConstants.QNAME_SUPPORT_RIM_TYPE.getNamespaceURI(),
                            SwidTagConstants.QNAME_SUPPORT_RIM_TYPE.getLocalPart()));
            swidResource.setRimUriGlobal(
                    file.getAttributeNS(
                            SwidTagConstants.QNAME_SUPPORT_RIM_URI_GLOBAL.getNamespaceURI(),
                            SwidTagConstants.QNAME_SUPPORT_RIM_URI_GLOBAL.getLocalPart()));
            validHashes.add(swidResource);
        }

        return validHashes;
    }

    /**
     * Creates a string representation of the Base Reference Manifest object.
     *
     * @return a string representation of the Base Reference Manifest object.
     */
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
