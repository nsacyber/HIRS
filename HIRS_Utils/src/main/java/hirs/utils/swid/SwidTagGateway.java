package hirs.utils.swid;

import hirs.utils.xjc.Directory;
import hirs.utils.xjc.Entity;
import hirs.utils.xjc.FilesystemItem;
import hirs.utils.xjc.Link;
import hirs.utils.xjc.ObjectFactory;
import hirs.utils.xjc.ResourceCollection;
import hirs.utils.xjc.SoftwareIdentity;
import hirs.utils.xjc.SoftwareMeta;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.Setter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureProperties;
import javax.xml.crypto.dsig.SignatureProperty;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLObject;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyName;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * This class provides interaction with the SWID Tag schema as defined in
 * http://standards.iso.org/iso/19770/-2/2015/schema.xsd.
 */
public class SwidTagGateway {

    private final ObjectFactory objectFactory = new ObjectFactory();
    private Marshaller marshaller;
    /**
     * String holding attributes file path.
     */
    @Setter
    private String attributesFile;

    /**
     * boolean governing signing credentials.
     */
    @Setter
    private boolean defaultCredentials;

    /**
     * JKS keystore file.
     */
    @Setter
    private String jksTruststoreFile;

    /**
     * private key file in PEM format.
     */
    @Setter
    private String pemPrivateKeyFile;

    /**
     * certificate file in PEM format.
     */
    @Setter
    private String pemCertificateFile;

    /**
     * embed certificate file in signature block.
     */
    @Setter
    private boolean embeddedCert;

    /**
     * event log support RIM.
     */
    @Setter
    private String rimEventLog;

    /**
     * timestamp format in XML signature.
     */
    @Setter
    private String timestampFormat;

    /**
     * timestamp input - RFC3852 + file or RFC3339 + value.
     */
    @Setter
    private String timestampArgument;

    private String errorRequiredFields;

    private DocumentBuilderFactory dbf;

    private DocumentBuilder builder;

    /**
     * Default constructor initializes jaxbcontext, marshaller, and unmarshaller.
     */
    public SwidTagGateway() {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(SwidTagConstants.SCHEMA_PACKAGE);
            marshaller = jaxbContext.createMarshaller();
            attributesFile = "";
            defaultCredentials = true;
            pemCertificateFile = "";
            embeddedCert = false;
            rimEventLog = "";
            timestampFormat = "";
            timestampArgument = "";
            errorRequiredFields = "";
            dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            builder = dbf.newDocumentBuilder();
        } catch (JAXBException e) {
            System.out.println("Error initializing jaxbcontext: " + e.getMessage());
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Error instantiating Document object for parsing swidtag: "
                    + e.getMessage(), e);
        }
    }

    /**
     * This method generates a base RIM from the values in a JSON file.
     *
     * @param filename
     */
    public void generateSwidTag(final String filename) {
        Document swidtag = builder.newDocument();
        SoftwareIdentity softwareIdentity = null;
        try {
            InputStream is = new FileInputStream(attributesFile);
            JsonReader reader = Json.createReader(is);
            JsonObject configProperties = reader.readObject();
            reader.close();
            //SoftwareIdentity
            softwareIdentity = createSwidTag(
                    configProperties.getJsonObject(SwidTagConstants.SOFTWARE_IDENTITY));
            //Entity
            JAXBElement<Entity> entity = objectFactory.createSoftwareIdentityEntity(
                    createEntity(configProperties.getJsonObject(SwidTagConstants.ENTITY)));
            softwareIdentity.getEntityOrEvidenceOrLink().add(entity);
            //Link
            JAXBElement<Link> link = objectFactory.createSoftwareIdentityLink(
                    createLink(configProperties.getJsonObject(SwidTagConstants.LINK)));
            softwareIdentity.getEntityOrEvidenceOrLink().add(link);
            //Meta
            JAXBElement<SoftwareMeta> meta = objectFactory.createSoftwareIdentityMeta(
                    createSoftwareMeta(configProperties.getJsonObject(SwidTagConstants.META)));
            softwareIdentity.getEntityOrEvidenceOrLink().add(meta);

            swidtag = convertToDocument(objectFactory.createSoftwareIdentity(softwareIdentity));
            Element rootElement = swidtag.getDocumentElement();

            Node payloadNode = swidtag.importNode(
                    assembleCompositePayload(configProperties).getDocumentElement(), true);
            rootElement.appendChild(payloadNode);

            //Signature
            if (errorRequiredFields.isEmpty()) {
                Document signedSoftwareIdentity = signXMLDocument(swidtag);
                writeSwidTagFile(signedSoftwareIdentity, filename);
            } else {
                throw new RuntimeException("The following fields cannot be empty or null: "
                        + errorRequiredFields.substring(0, errorRequiredFields.length() - 2));
            }
        } catch (JsonException e) {
            throw new RuntimeException("Error reading JSON attributes: " + e.getMessage());
        } catch (FileNotFoundException e) {
            throw new RuntimeException("File does not exist or cannot be read: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("File does not exist or cannot be read: " + e.getMessage());
        }
    }

    private Document assembleCompositePayload(final JsonObject configProperties) throws Exception {
        //Directory
        Directory directory = createDirectory(
                configProperties.getJsonObject(SwidTagConstants.PAYLOAD)
                        .getJsonObject(SwidTagConstants.DIRECTORY));
        JAXBElement<FilesystemItem> jaxbDirectory = objectFactory.createPayloadDirectory(directory);
        Document dirDoc = convertToDocument(jaxbDirectory);

        //File(s)
        JsonArray files = configProperties.getJsonObject(SwidTagConstants.PAYLOAD)
                .getJsonObject(SwidTagConstants.DIRECTORY).getJsonArray(SwidTagConstants.FILE);
        Iterator itr = files.iterator();
        while (itr.hasNext()) {
            hirs.utils.xjc.File file = createFile((JsonObject) itr.next());
            JAXBElement<FilesystemItem> jaxbFile = objectFactory.createDirectoryFile(file);
            Document fileDoc = convertToDocument(jaxbFile);
            Node fileNode = dirDoc.importNode(fileDoc.getDocumentElement(), true);
            dirDoc.getDocumentElement().appendChild(fileNode);
        }

        //Payload
        ResourceCollection payload = createPayload(
                configProperties.getJsonObject(SwidTagConstants.PAYLOAD));
        JAXBElement<ResourceCollection> jaxbPayload =
                objectFactory.createSoftwareIdentityPayload(payload);
        Document payloadDoc = convertToDocument(jaxbPayload);
        Node dirNode = payloadDoc.importNode(dirDoc.getDocumentElement(), true);
        payloadDoc.getDocumentElement().appendChild(dirNode);

        return payloadDoc;
    }

    /**
     * This method writes a Document object out to the file specified by generatedFile.
     *
     * @param swidTag the XML representing the SWID tag to write
     * @param output the file path to write the SWID tag to
     */
    public void writeSwidTagFile(final Document swidTag, final String output) {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            Source source = new DOMSource(swidTag);
            if (output.isEmpty()) {
                transformer.transform(source, new StreamResult(System.out));
            } else {
                transformer.transform(source, new StreamResult(new FileOutputStream(output)));
            }
        } catch (FileNotFoundException e) {
            System.out.println("Unable to write to file: " + e.getMessage());
        } catch (TransformerConfigurationException e) {
            System.out.println("Error instantiating TransformerFactory class: " + e.getMessage());
        } catch (TransformerException e) {
            System.out.println("Error instantiating Transformer class: " + e.getMessage());
        }
    }

    /**
     * This method creates SoftwareIdentity element based on the parameters read in from
     * a properties file.
     *
     * @param jsonObject the Properties object containing parameters from file
     * @return SoftwareIdentity object created from the properties
     */
    private SoftwareIdentity createSwidTag(final JsonObject jsonObject) {
        SoftwareIdentity swidTag = objectFactory.createSoftwareIdentity();
        if (jsonObject == null) {
            errorRequiredFields += SwidTagConstants.SOFTWARE_IDENTITY + ", ";
        } else {
            swidTag.setLang(SwidTagConstants.DEFAULT_ENGLISH);
            String name = jsonObject.getString(SwidTagConstants.NAME, "");
            if (!name.isEmpty()) {
                swidTag.setName(name);
            }
            String tagId = jsonObject.getString(SwidTagConstants.TAGID, "");
            if (!tagId.isEmpty()) {
                swidTag.setTagId(tagId);
            }
            swidTag.setTagVersion(new BigInteger(
                    jsonObject.getString(SwidTagConstants.TAGVERSION, "0")));
            swidTag.setVersion(jsonObject.getString(SwidTagConstants.VERSION, "0.0"));
            swidTag.setCorpus(jsonObject.getBoolean(SwidTagConstants.CORPUS, false));
            swidTag.setPatch(jsonObject.getBoolean(SwidTagConstants.PATCH, false));
            swidTag.setSupplemental(jsonObject.getBoolean(SwidTagConstants.SUPPLEMENTAL, false));
            if (!swidTag.isCorpus() && !swidTag.isPatch()
                    && !swidTag.isSupplemental() && swidTag.getVersion() != "0.0") {
                swidTag.setVersionScheme(
                        jsonObject.getString(SwidTagConstants.VERSION_SCHEME, "multipartnumeric"));
            }
        }

        return swidTag;
    }

    /**
     * This method creates an Entity object based on the parameters read in from
     * a properties file.
     *
     * @param jsonObject the Properties object containing parameters from file
     * @return Entity object created from the properties
     */
    private Entity createEntity(final JsonObject jsonObject) {
        boolean isTagCreator = false;
        Entity entity = objectFactory.createEntity();
        if (jsonObject == null) {
            errorRequiredFields += SwidTagConstants.ENTITY + ", ";
        } else {
            String name = jsonObject.getString(SwidTagConstants.NAME, "");
            if (!name.isEmpty()) {
                entity.setName(name);
            }
            String[] roles = jsonObject.getString(SwidTagConstants.ROLE, "").split(",");
            for (int i = 0; i < roles.length; i++) {
                entity.getRole().add(roles[i]);
                if (roles[i].equals("tagCreator")) {
                    isTagCreator = true;
                }
            }
            if (isTagCreator) {
                String regid = jsonObject.getString(SwidTagConstants.REGID, "");
                if (regid.isEmpty()) {
                    throw new IllegalArgumentException("regid is required");
                } else {
                    entity.setRegid(regid);
                }
            } else {
                entity.setRegid(jsonObject.getString(SwidTagConstants.REGID,
                        "invalid.unavailable"));
            }
            String thumbprint = jsonObject.getString(SwidTagConstants.THUMBPRINT, "");
            if (!thumbprint.isEmpty()) {
                entity.setThumbprint(thumbprint);
            }
        }
        return entity;
    }

    /**
     * Thsi method creates a Link element based on the parameters read in from a properties
     * file.
     *
     * @param jsonObject the Properties object containing parameters from file
     * @return Link element created from the properties
     */
    private Link createLink(final JsonObject jsonObject) {
        Link link = objectFactory.createLink();
        String href = jsonObject.getString(SwidTagConstants.HREF, "");
        if (!href.isEmpty()) {
            link.setHref(href);
        }
        String rel = jsonObject.getString(SwidTagConstants.REL, "");
        if (!rel.isEmpty()) {
            link.setRel(rel);
        }

        return link;
    }

    /**
     * This method creates a Meta element based on the parameters read in from a properties
     * file.
     *
     * @param jsonObject the Properties object containing parameters from file
     * @return the Meta element created from the properties
     */
    private SoftwareMeta createSoftwareMeta(final JsonObject jsonObject) {
        SoftwareMeta softwareMeta = objectFactory.createSoftwareMeta();
        Map<QName, String> attributes = softwareMeta.getOtherAttributes();
        addNonNullAttribute(attributes, SwidTagConstants.QNAME_COLLOQUIAL_VERSION,
                jsonObject.getString(SwidTagConstants.COLLOQUIAL_VERSION, ""));
        addNonNullAttribute(attributes, SwidTagConstants.QNAME_EDITION,
                jsonObject.getString(SwidTagConstants.EDITION, ""));
        addNonNullAttribute(attributes, SwidTagConstants.QNAME_PRODUCT,
                jsonObject.getString(SwidTagConstants.PRODUCT, ""));
        addNonNullAttribute(attributes, SwidTagConstants.QNAME_REVISION,
                jsonObject.getString(SwidTagConstants.REVISION, ""));
        addNonNullAttribute(attributes, SwidTagConstants.QNAME_PAYLOAD_TYPE,
                jsonObject.getString(SwidTagConstants.PAYLOAD_TYPE, ""));
        addNonNullAttribute(attributes, SwidTagConstants.QNAME_PLATFORM_MANUFACTURER_STR,
                jsonObject.getString(SwidTagConstants.PLATFORM_MANUFACTURER_STR, ""), true);
        addNonNullAttribute(attributes, SwidTagConstants.QNAME_PLATFORM_MANUFACTURER_ID,
                jsonObject.getString(SwidTagConstants.PLATFORM_MANUFACTURER_ID, ""), true);
        addNonNullAttribute(attributes, SwidTagConstants.QNAME_PLATFORM_MODEL,
                jsonObject.getString(SwidTagConstants.PLATFORM_MODEL, ""), true);
        addNonNullAttribute(attributes, SwidTagConstants.QNAME_PLATFORM_VERSION,
                jsonObject.getString(SwidTagConstants.PLATFORM_VERSION, ""));
        addNonNullAttribute(attributes, SwidTagConstants.QNAME_FIRMWARE_MANUFACTURER_STR,
                jsonObject.getString(SwidTagConstants.FIRMWARE_MANUFACTURER_STR, ""));
        addNonNullAttribute(attributes, SwidTagConstants.QNAME_FIRMWARE_MANUFACTURER_ID,
                jsonObject.getString(SwidTagConstants.FIRMWARE_MANUFACTURER_ID, ""));
        addNonNullAttribute(attributes, SwidTagConstants.QNAME_FIRMWARE_MODEL,
                jsonObject.getString(SwidTagConstants.FIRMWARE_MODEL, ""));
        addNonNullAttribute(attributes, SwidTagConstants.QNAME_FIRMWARE_VERSION,
                jsonObject.getString(SwidTagConstants.FIRMWARE_VERSION, ""));
        addNonNullAttribute(attributes, SwidTagConstants.QNAME_BINDING_SPEC,
                jsonObject.getString(SwidTagConstants.BINDING_SPEC, ""));
        addNonNullAttribute(attributes, SwidTagConstants.QNAME_BINDING_SPEC_VERSION,
                jsonObject.getString(SwidTagConstants.BINDING_SPEC_VERSION, ""));
        addNonNullAttribute(attributes, SwidTagConstants.QNAME_PC_URI_LOCAL,
                jsonObject.getString(SwidTagConstants.PC_URI_LOCAL, ""));
        addNonNullAttribute(attributes, SwidTagConstants.QNAME_PC_URI_GLOBAL,
                jsonObject.getString(SwidTagConstants.PC_URI_GLOBAL, ""));
        addNonNullAttribute(attributes, SwidTagConstants.QNAME_RIM_LINK_HASH,
                jsonObject.getString(SwidTagConstants.RIM_LINK_HASH, ""));

        return softwareMeta;
    }

    /**
     * This method creates a Payload from the parameters read in from a properties file.
     *
     * @param jsonObject the Properties object containing parameters from file
     * @return the Payload object created
     */
    private ResourceCollection createPayload(final JsonObject jsonObject) {
        ResourceCollection payload = objectFactory.createResourceCollection();
        Map<QName, String> attributes = payload.getOtherAttributes();
        if (jsonObject == null) {
            errorRequiredFields += SwidTagConstants.PAYLOAD + ", ";
        } else {
            addNonNullAttribute(attributes, SwidTagConstants.QNAME_N8060_ENVVARPREFIX,
                    jsonObject.getString(SwidTagConstants.QNAME_N8060_ENVVARPREFIX.getLocalPart(), ""));
            addNonNullAttribute(attributes, SwidTagConstants.QNAME_N8060_ENVVARSUFFIX,
                    jsonObject.getString(SwidTagConstants.QNAME_N8060_ENVVARSUFFIX.getLocalPart(), ""));
            addNonNullAttribute(attributes, SwidTagConstants.QNAME_N8060_PATHSEPARATOR,
                    jsonObject.getString(SwidTagConstants.QNAME_N8060_PATHSEPARATOR.getLocalPart(), ""));
        }

        return payload;
    }

    /**
     * This method creates a Directory from the parameters read in from a properties file.
     *
     * @param jsonObject the Properties object containing parameters from file
     * @return Directory object created from the properties
     */
    private Directory createDirectory(final JsonObject jsonObject) {
        Directory directory = objectFactory.createDirectory();
        directory.setName(jsonObject.getString(SwidTagConstants.NAME, ""));
        directory.setLocation(jsonObject.getString(SwidTagConstants.LOCATION, ""));
        Map<QName, String> attributes = directory.getOtherAttributes();
        String supportRimFormat = jsonObject.getString(SwidTagConstants.SUPPORT_RIM_FORMAT,
                SwidTagConstants.SUPPORT_RIM_FORMAT_MISSING);
        if (!supportRimFormat.equals(SwidTagConstants.SUPPORT_RIM_FORMAT_MISSING)) {
            if (supportRimFormat.isEmpty()) {
                attributes.put(SwidTagConstants.QNAME_SUPPORT_RIM_FORMAT,
                        SwidTagConstants.TCG_EVENTLOG_ASSERTION);
            } else {
                attributes.put(SwidTagConstants.QNAME_SUPPORT_RIM_FORMAT, supportRimFormat);
            }
        }
        addNonNullAttribute(attributes, SwidTagConstants.QNAME_SUPPORT_RIM_TYPE,
                jsonObject.getString(SwidTagConstants.SUPPORT_RIM_TYPE, ""));
        addNonNullAttribute(attributes, SwidTagConstants.QNAME_SUPPORT_RIM_URI_GLOBAL,
                jsonObject.getString(SwidTagConstants.SUPPORT_RIM_URI_GLOBAL, ""));

        return directory;
    }

    /**
     * This method creates a hirs.utils.xjc.File from an indirect payload type
     * using parameters read in from a properties file and then
     * calculating the hash of a given event log support RIM.
     *
     * @param jsonObject the Properties object containing parameters from file
     * @return File object created from the properties
     */
    private hirs.utils.xjc.File createFile(final JsonObject jsonObject) throws Exception {
        hirs.utils.xjc.File file = objectFactory.createFile();
        file.setName(jsonObject.getString(SwidTagConstants.NAME, ""));
        file.setSize(new BigInteger(jsonObject.getString(SwidTagConstants.SIZE, "0")));
        Map<QName, String> attributes = file.getOtherAttributes();
        String fileHash;
        if (rimEventLog.isEmpty()) {
            fileHash = jsonObject.getString(SwidTagConstants.SHA_256_HASH.getLocalPart());
        } else {
            fileHash = jsonObject.getString(SwidTagConstants.SHA_256_HASH.getLocalPart(),
                    HashSwid.get256Hash(rimEventLog));
        }
        addNonNullAttribute(attributes, SwidTagConstants.SHA_256_HASH, fileHash, true);
        String supportRimFormat = jsonObject.getString(SwidTagConstants.SUPPORT_RIM_FORMAT,
                SwidTagConstants.SUPPORT_RIM_FORMAT_MISSING);
        if (!supportRimFormat.equals(SwidTagConstants.SUPPORT_RIM_FORMAT_MISSING)) {
            if (supportRimFormat.isEmpty()) {
                attributes.put(SwidTagConstants.QNAME_SUPPORT_RIM_FORMAT,
                        SwidTagConstants.TCG_EVENTLOG_ASSERTION);
            } else {
                attributes.put(SwidTagConstants.QNAME_SUPPORT_RIM_FORMAT, supportRimFormat);
            }
        }
        addNonNullAttribute(attributes, SwidTagConstants.QNAME_SUPPORT_RIM_TYPE,
                jsonObject.getString(SwidTagConstants.SUPPORT_RIM_TYPE, ""));
        addNonNullAttribute(attributes, SwidTagConstants.QNAME_SUPPORT_RIM_URI_GLOBAL,
                jsonObject.getString(SwidTagConstants.SUPPORT_RIM_URI_GLOBAL, ""));

        return file;
    }

    private void addNonNullAttribute(final Map<QName, String> attributes, final QName key,
                                     final String value, final boolean required) {
        if (required && value.isEmpty()) {
            errorRequiredFields += key.getLocalPart() + ", ";
        } else {
            addNonNullAttribute(attributes, key, value);
        }
    }

    /**
     * This utility method checks if an attribute value is empty before adding it to the map.
     *
     * @param attributes
     * @param key
     * @param value
     */
    private void addNonNullAttribute(final Map<QName, String> attributes,
                                     final QName key, final String value) {
        if (!value.isEmpty()) {
            attributes.put(key, value);
        }
    }

    /**
     * This method converts a JAXBElement object generated from the hirs.utils.xjc package into
     * a Document object.
     *
     * @param element to convert
     * @return a Document object
     */
    private Document convertToDocument(final JAXBElement element) {
        Document doc = null;
        try {
            doc = builder.newDocument();
            marshaller.marshal(element, doc);
        } catch (JAXBException e) {
            throw new RuntimeException("Error while marshaling swidtag:" + e.getMessage(), e);
        }

        return doc;
    }

    /**
     * This method signs a SoftwareIdentity with an xmldsig in compatibility mode.
     * Current assumptions: digest method SHA256, signature method SHA256, enveloped signature
     * @param doc the XML {@link Document} representing the SoftwareIdentity to sign
     * @return the signed XML {@link Document}
     */
    private Document signXMLDocument(final Document doc) {
        XMLSignatureFactory sigFactory = XMLSignatureFactory.getInstance("DOM");
        List xmlObjectList = null;
        String signatureId = null;

        Reference documentRef = null;
        try {
            documentRef = sigFactory.newReference(
                    "",
                    sigFactory.newDigestMethod(DigestMethod.SHA256, null),
                    Collections.singletonList(sigFactory.newTransform(Transform.ENVELOPED,
                            (TransformParameterSpec) null)),
                    null,
                    null
            );
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException("Error while creating enveloped signature Reference: "
                    + e.getMessage(), e);
        }

        List<Reference> refList = new ArrayList<Reference>();
        refList.add(documentRef);

        if (!timestampFormat.isEmpty()) {
            Reference timestampRef = null;
            try {
                timestampRef = sigFactory.newReference(
                        "#TST",
                        sigFactory.newDigestMethod(DigestMethod.SHA256, null)
                );
            } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
                throw new RuntimeException("Error while creating timestamp Reference: " + e.getMessage(), e);
            }
            refList.add(timestampRef);
            xmlObjectList = Collections.singletonList(createXmlTimestamp(doc, sigFactory));
            signatureId = "RimSignature";
        }
        SignedInfo signedInfo = null;
        try {
            signedInfo = sigFactory.newSignedInfo(
                    sigFactory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE,
                            (C14NMethodParameterSpec) null),
                    sigFactory.newSignatureMethod(SwidTagConstants.SIGNATURE_ALGORITHM_RSA_SHA256,
                            null),
                    refList
            );
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException("Error while creating timestamp Reference: " + e.getMessage(), e);
        }
        List<XMLStructure> keyInfoElements = new ArrayList<XMLStructure>();

        KeyInfoFactory kiFactory = sigFactory.getKeyInfoFactory();
        PrivateKey privateKey;
        CredentialParser cp = new CredentialParser();
        if (defaultCredentials) {
            cp.parseJKSCredentials(jksTruststoreFile);
            privateKey = cp.getPrivateKey();
        } else {
            try {
                cp.parsePEMCredentials(pemCertificateFile, pemPrivateKeyFile);
            } catch (Exception e) {
                throw new RuntimeException("Error while parsing PEM files: " + e.getMessage(), e);
            }
            X509Certificate certificate = cp.getCertificate();
            privateKey = cp.getPrivateKey();
            if (embeddedCert) {
                ArrayList<Object> x509Content = new ArrayList<Object>();
                x509Content.add(certificate.getSubjectX500Principal().getName());
                x509Content.add(certificate);
                X509Data data = kiFactory.newX509Data(x509Content);
                keyInfoElements.add(data);
            } else {
                try {
                    keyInfoElements.add(kiFactory.newKeyValue(certificate.getPublicKey()));
                } catch (KeyException e) {
                    System.out.println("Error while creating KeyValue: " + e.getMessage());
                }
            }
        }
        try {
            KeyName keyName = kiFactory.newKeyName(cp.getCertificateSubjectKeyIdentifier());
            keyInfoElements.add(keyName);
        } catch (IOException e) {
            throw new RuntimeException("Error while getting SKID: " + e.getMessage(), e);
        }
        KeyInfo keyinfo = kiFactory.newKeyInfo(keyInfoElements);

        DOMSignContext context = new DOMSignContext(privateKey, doc.getDocumentElement());
        XMLSignature signature = sigFactory.newXMLSignature(
                signedInfo,
                keyinfo,
                xmlObjectList,
                signatureId,
                null
        );
        try {
            signature.sign(context);
        } catch (MarshalException | XMLSignatureException e) {
            System.out.println("Error while signing the swidtag: " + e.getMessage());
        }

        return doc;
    }

    /**
     * This method creates a timestamp element and populates it with data according to
     * the RFC format set in timestampFormat.  The element is returned within an XMLObject.
     *
     * @param doc        the Document representing the XML to be signed
     * @param sigFactory the SignatureFactory object
     * @return an XMLObject containing the timestamp element
     */
    private XMLObject createXmlTimestamp(final Document doc, final XMLSignatureFactory sigFactory) {
        Element timeStampElement = null;
        switch (timestampFormat.toUpperCase()) {
            case "RFC3852":
                try {
                    byte[] counterSignature = Base64.getEncoder().encode(
                            Files.readAllBytes(Paths.get(timestampArgument)));
                    timeStampElement = doc.createElementNS(SwidTagConstants.RFC3852_NS,
                            SwidTagConstants.RFC3852_PFX + ":TimeStamp");
                    timeStampElement.setAttributeNS("http://www.w3.org/2000/xmlns/",
                            "xmlns:" + SwidTagConstants.RFC3852_PFX,
                            SwidTagConstants.RFC3852_NS);
                    timeStampElement.setAttributeNS(SwidTagConstants.RFC3852_NS,
                            SwidTagConstants.RFC3852_PFX + ":" + SwidTagConstants.DATETIME,
                            new String(counterSignature, StandardCharsets.UTF_8));
                } catch (IOException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
                break;
            case "RFC3339":
                timeStampElement = doc.createElementNS(SwidTagConstants.RFC3339_NS,
                        SwidTagConstants.RFC3339_PFX + ":TimeStamp");
                timeStampElement.setAttributeNS("http://www.w3.org/2000/xmlns/",
                        "xmlns:" + SwidTagConstants.RFC3339_PFX, SwidTagConstants.RFC3339_NS);
                if (timestampArgument.isEmpty()) {
                    timeStampElement.setAttributeNS(SwidTagConstants.RFC3339_NS,
                            SwidTagConstants.RFC3339_PFX + ":" + SwidTagConstants.DATETIME,
                            LocalDateTime.now().toString());
                } else {
                    timeStampElement.setAttributeNS(SwidTagConstants.RFC3339_NS,
                            SwidTagConstants.RFC3339_PFX + ":" + SwidTagConstants.DATETIME,
                            timestampArgument);
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported timestamp format: " + timestampFormat);
        }
        DOMStructure timestampObject = new DOMStructure(timeStampElement);
        SignatureProperty signatureProperty = sigFactory.newSignatureProperty(
                Collections.singletonList(timestampObject), "RimSignature", "TST"
        );
        SignatureProperties signatureProperties = sigFactory.newSignatureProperties(
                Collections.singletonList(signatureProperty), null);
        XMLObject xmlObject = sigFactory.newXMLObject(
                Collections.singletonList(signatureProperties), null, null, null);

        return xmlObject;
    }
}
