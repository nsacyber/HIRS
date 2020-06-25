package hirs.swid;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.UnmarshalException;
import javax.xml.crypto.dsig.keyinfo.*;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.namespace.QName;
import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import hirs.swid.utils.HashSwid;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.math.BigInteger;

import hirs.swid.xjc.Directory;
import hirs.swid.xjc.Entity;
import hirs.swid.xjc.Link;
import hirs.swid.xjc.Meta;
import hirs.swid.xjc.ObjectFactory;
import hirs.swid.xjc.ResourceCollection;
import hirs.swid.xjc.SoftwareIdentity;
import hirs.swid.xjc.SoftwareMeta;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.ParseException;



/**
 * This class provides interaction with the SWID Tag schema as defined in
 * http://standards.iso.org/iso/19770/-2/2015/schema.xsd
 *
 */
public class SwidTagGateway {

    private static final QName _SHA256_HASH = new QName(
            "http://www.w3.org/2001/04/xmlenc#sha256", "hash", "SHA256");
    private final ObjectFactory objectFactory = new ObjectFactory();
    private JAXBContext jaxbContext;
    private Marshaller marshaller;
    private Unmarshaller unmarshaller;
    private String attributesFile;
    private boolean defaultCredentials;
    private String pemPrivateKeyFile;
    private String pemCertificateFile;
    private String rimEventLog;

    /**
     * Default constructor initializes jaxbcontext, marshaller, and unmarshaller
     */
    public SwidTagGateway() {
        try {
            jaxbContext = JAXBContext.newInstance(SwidTagConstants.SCHEMA_PACKAGE);
            marshaller = jaxbContext.createMarshaller();
            unmarshaller = jaxbContext.createUnmarshaller();
            attributesFile = SwidTagConstants.DEFAULT_ATTRIBUTES_FILE;
            defaultCredentials = true;
            pemCertificateFile = "";
            rimEventLog = "";
        } catch (JAXBException e) {
            System.out.println("Error initializing jaxbcontext: " + e.getMessage());
        }
    }

    /**
     * Setter for String holding attributes file path
     * @param attributesFile
     */
    public void setAttributesFile(String attributesFile) {
        this.attributesFile = attributesFile;
    }

    /**
     * Setter for boolean governing signing credentials
     * @param defaultCredentials
     * @return
     */
    public void setDefaultCredentials(boolean defaultCredentials) {
        this.defaultCredentials = defaultCredentials;
    }

    /**
     * Setter for private key file in PEM format
     * @param pemPrivateKeyFile
     */
    public void setPemPrivateKeyFile(String pemPrivateKeyFile) {
        this.pemPrivateKeyFile = pemPrivateKeyFile;
    }

    /**
     * Setter for certificate file in PEM format
     * @param pemCertificateFile
     */
    public void setPemCertificateFile(String pemCertificateFile) {
        this.pemCertificateFile = pemCertificateFile;
    }

    /**
     * Setter for event log support RIM
     * @param rimEventLog
     */
    public void setRimEventLog(String rimEventLog) {
        this.rimEventLog = rimEventLog;
    }

    /**
     * This method generates a base RIM from the values in a JSON file.
     *
     * @param filename
     */
    public void generateSwidTag(final String filename) {
        SoftwareIdentity swidTag = null;
        try {
            BufferedReader jsonIn = Files.newBufferedReader(Paths.get(attributesFile), StandardCharsets.UTF_8);
            JsonObject configProperties = Json.parse(jsonIn).asObject();
            //SoftwareIdentity
            swidTag = createSwidTag(configProperties.get(SwidTagConstants.SOFTWARE_IDENTITY).asObject());
            //Entity
            JAXBElement<Entity> entity = objectFactory.createSoftwareIdentityEntity(
                    createEntity(configProperties.get(SwidTagConstants.ENTITY).asObject()));
            swidTag.getEntityOrEvidenceOrLink().add(entity);
            //Link
            JAXBElement<Link> link = objectFactory.createSoftwareIdentityLink(
                    createLink(configProperties.get(SwidTagConstants.LINK).asObject()));
            swidTag.getEntityOrEvidenceOrLink().add(link);
            //Meta
            JAXBElement<SoftwareMeta> meta = objectFactory.createSoftwareIdentityMeta(
                    createSoftwareMeta(configProperties.get(SwidTagConstants.META).asObject()));
            swidTag.getEntityOrEvidenceOrLink().add(meta);
            //File
            hirs.swid.xjc.File file = createFile(
                                configProperties.get(SwidTagConstants.PAYLOAD).asObject()
                                                .get(SwidTagConstants.DIRECTORY).asObject()
                                                .get(SwidTagConstants.FILE).asObject());
            //Directory
            Directory directory = createDirectory(
                    configProperties.get(SwidTagConstants.PAYLOAD).asObject()
                                    .get(SwidTagConstants.DIRECTORY).asObject());
            directory.getDirectoryOrFile().add(file);
            //Payload
            ResourceCollection payload = createPayload(
                    configProperties.get(SwidTagConstants.PAYLOAD).asObject());
            payload.getDirectoryOrFileOrProcess().add(directory);
            JAXBElement<ResourceCollection> jaxbPayload =
                    objectFactory.createSoftwareIdentityPayload(payload);
            swidTag.getEntityOrEvidenceOrLink().add(jaxbPayload);

        } catch (FileNotFoundException e) {
            System.out.println("File does not exist or cannot be read: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error in file reader: " + e.getMessage());
        } catch (ParseException e) {
            System.out.println("Invalid JSON detected at " + e.getLocation().toString());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        Document signedSoftwareIdentity = signXMLDocument(objectFactory.createSoftwareIdentity(swidTag));
        writeSwidTagFile(signedSoftwareIdentity, filename);
    }

   /**
     * This method validates the .swidtag file at the given filepath against the
     * schema. A successful validation results in the output of the tag's name
     * and tagId attributes, otherwise a generic error message is printed.
     *
     * @param path the location of the file to be validated
     */
    public boolean validateSwidTag(String path) throws IOException {
        Document document = unmarshallSwidTag(path);
        Element softwareIdentity = (Element) document.getElementsByTagName("SoftwareIdentity").item(0);
        StringBuilder si = new StringBuilder("Base RIM detected:\n");
        si.append("SoftwareIdentity name: " + softwareIdentity.getAttribute("name") + "\n");
        si.append("SoftwareIdentity tagId: " + softwareIdentity.getAttribute("tagId") + "\n");
        System.out.println(si.toString());
        Element file = (Element) document.getElementsByTagName("File").item(0);
        validateFile(file);
        System.out.println("Signature core validity: " + validateSignedXMLDocument(document));
        return true;
    }

    /**
     * This method writes a Document object out to the file specified by generatedFile.
     *
     * @param swidTag
     */
    public void writeSwidTagFile(Document swidTag, String output) {
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
    private SoftwareIdentity createSwidTag(JsonObject jsonObject) {
        SoftwareIdentity swidTag = objectFactory.createSoftwareIdentity();
        swidTag.setLang(SwidTagConstants.DEFAULT_ENGLISH);
        String name = jsonObject.getString(SwidTagConstants.NAME, "");
        if (!name.isEmpty()) {
            swidTag.setName(name);
        }
        String tagId = jsonObject.getString(SwidTagConstants.TAGID, "");
        if (!tagId.isEmpty()) {
            swidTag.setTagId(tagId);
        }
        swidTag.setTagVersion(new BigInteger(jsonObject.getString(SwidTagConstants.TAGVERSION, "0")));
        String version = jsonObject.getString(SwidTagConstants.VERSION, "");
        if (!version.isEmpty()) {
            swidTag.setVersion(version);
        }
        swidTag.setCorpus(jsonObject.getBoolean(SwidTagConstants.CORPUS, false));
        swidTag.setPatch(jsonObject.getBoolean(SwidTagConstants.PATCH, false));
        swidTag.setSupplemental(jsonObject.getBoolean(SwidTagConstants.SUPPLEMENTAL, false));
        if (!swidTag.isCorpus() && !swidTag.isPatch()
                && !swidTag.isSupplemental() && swidTag.getVersion() != "0.0") {
            swidTag.setVersionScheme(jsonObject.getString(SwidTagConstants.VERSION_SCHEME, "multipartnumeric"));
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
    private Entity createEntity(JsonObject jsonObject) {
        boolean isTagCreator = false;
        Entity entity = objectFactory.createEntity();
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
                //throw exception that regid is required
            } else {
                entity.setRegid(regid);
            }
        } else {
            entity.setRegid(jsonObject.getString(SwidTagConstants.REGID, "invalid.unavailable"));
        }
        String thumbprint = jsonObject.getString(SwidTagConstants.THUMBPRINT, "");
        if (!thumbprint.isEmpty()) {
            entity.setThumbprint(thumbprint);
        }
        return entity;
    }

    /**
     * Thsi method creates a Link element based on the parameters read in from a properties
     * file.
     * @param jsonObject the Properties object containing parameters from file
     * @return Link element created from the properties
     */
    private Link createLink(JsonObject jsonObject) {
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
     * @param jsonObject the Properties object containing parameters from file
     * @return the Meta element created from the properties
     */
    private SoftwareMeta createSoftwareMeta(JsonObject jsonObject) {
        SoftwareMeta softwareMeta = objectFactory.createSoftwareMeta();
        Map<QName, String> attributes = softwareMeta.getOtherAttributes();
        addNonNullAttribute(attributes, SwidTagConstants._COLLOQUIAL_VERSION, jsonObject.getString(SwidTagConstants.COLLOQUIAL_VERSION, ""));
        addNonNullAttribute(attributes, SwidTagConstants._EDITION, jsonObject.getString(SwidTagConstants.EDITION, ""));
        addNonNullAttribute(attributes, SwidTagConstants._PRODUCT, jsonObject.getString(SwidTagConstants.PRODUCT, ""));
        addNonNullAttribute(attributes, SwidTagConstants._REVISION, jsonObject.getString(SwidTagConstants.REVISION, ""));
        addNonNullAttribute(attributes, SwidTagConstants._PAYLOAD_TYPE, jsonObject.getString(SwidTagConstants.PAYLOAD_TYPE, ""));
        addNonNullAttribute(attributes, SwidTagConstants._PLATFORM_MANUFACTURER_STR, jsonObject.getString(SwidTagConstants.PLATFORM_MANUFACTURER_STR, ""));
        addNonNullAttribute(attributes, SwidTagConstants._PLATFORM_MANUFACTURER_ID, jsonObject.getString(SwidTagConstants.PLATFORM_MANUFACTURER_ID, ""));
        addNonNullAttribute(attributes, SwidTagConstants._PLATFORM_MODEL, jsonObject.getString(SwidTagConstants.PLATFORM_MODEL, ""));
        addNonNullAttribute(attributes, SwidTagConstants._PLATFORM_VERSION, jsonObject.getString(SwidTagConstants.PLATFORM_VERSION, ""));
        addNonNullAttribute(attributes, SwidTagConstants._FIRMWARE_MANUFACTURER_STR, jsonObject.getString(SwidTagConstants.FIRMWARE_MANUFACTURER_STR, ""));
        addNonNullAttribute(attributes, SwidTagConstants._FIRMWARE_MANUFACTURER_ID, jsonObject.getString(SwidTagConstants.FIRMWARE_MANUFACTURER_ID, ""));
        addNonNullAttribute(attributes, SwidTagConstants._FIRMWARE_MODEL, jsonObject.getString(SwidTagConstants.FIRMWARE_MODEL, ""));
        addNonNullAttribute(attributes, SwidTagConstants._FIRMWARE_VERSION, jsonObject.getString(SwidTagConstants.FIRMWARE_VERSION, ""));
        addNonNullAttribute(attributes, SwidTagConstants._BINDING_SPEC, jsonObject.getString(SwidTagConstants.BINDING_SPEC, ""));
        addNonNullAttribute(attributes, SwidTagConstants._BINDING_SPEC_VERSION, jsonObject.getString(SwidTagConstants.BINDING_SPEC_VERSION, ""));
        addNonNullAttribute(attributes, SwidTagConstants._PC_URI_LOCAL, jsonObject.getString(SwidTagConstants.PC_URI_LOCAL, ""));
        addNonNullAttribute(attributes, SwidTagConstants._PC_URI_GLOBAL, jsonObject.getString(SwidTagConstants.PC_URI_GLOBAL, ""));
        addNonNullAttribute(attributes, SwidTagConstants._RIM_LINK_HASH, jsonObject.getString(SwidTagConstants.RIM_LINK_HASH, ""));

        return softwareMeta;
    }

    /**
     * This method creates a Payload from the parameters read in from a properties file.
     *
     * @param jsonObject the Properties object containing parameters from file
     * @return the Payload object created
     */
    private ResourceCollection createPayload(JsonObject jsonObject) {
        ResourceCollection payload = objectFactory.createResourceCollection();
        Map<QName, String> attributes = payload.getOtherAttributes();
        addNonNullAttribute(attributes, SwidTagConstants._N8060_ENVVARPREFIX, jsonObject.getString(SwidTagConstants._N8060_ENVVARPREFIX.getLocalPart(), ""));
        addNonNullAttribute(attributes, SwidTagConstants._N8060_ENVVARSUFFIX, jsonObject.getString(SwidTagConstants._N8060_ENVVARSUFFIX.getLocalPart(), ""));
        addNonNullAttribute(attributes, SwidTagConstants._N8060_PATHSEPARATOR, jsonObject.getString(SwidTagConstants._N8060_PATHSEPARATOR.getLocalPart(), ""));

        return payload;
    }

    /**
     * This method creates a Directory from the parameters read in from a properties file.
     *
     * @param jsonObject the Properties object containing parameters from file
     * @return Directory object created from the properties
     */
    private Directory createDirectory(JsonObject jsonObject) {
        Directory directory = objectFactory.createDirectory();
        directory.setName(jsonObject.getString(SwidTagConstants.NAME, ""));
        Map<QName, String> attributes = directory.getOtherAttributes();
        addNonNullAttribute(attributes, SwidTagConstants._SUPPORT_RIM_TYPE, jsonObject.getString(SwidTagConstants.SUPPORT_RIM_TYPE, ""));
        addNonNullAttribute(attributes, SwidTagConstants._SUPPORT_RIM_FORMAT, jsonObject.getString(SwidTagConstants.SUPPORT_RIM_FORMAT, ""));
        addNonNullAttribute(attributes, SwidTagConstants._SUPPORT_RIM_URI_GLOBAL, jsonObject.getString(SwidTagConstants.SUPPORT_RIM_URI_GLOBAL, ""));

        return directory;
    }

    /**
     * This method creates a hirs.swid.xjc.File from an indirect payload type
     * using parameters read in from a properties file and then
     * calculating the hash of a given event log support RIM.
     *
     * @param jsonObject the Properties object containing parameters from file
     * @return File object created from the properties
     */
    private hirs.swid.xjc.File createFile(JsonObject jsonObject) {
        hirs.swid.xjc.File file = objectFactory.createFile();
        file.setName(jsonObject.getString(SwidTagConstants.NAME, ""));
        File rimEventLogFile = new File(rimEventLog);
        file.setSize(new BigInteger(Long.toString(rimEventLogFile.length())));
        Map<QName, String> attributes = file.getOtherAttributes();
        addNonNullAttribute(attributes, _SHA256_HASH, HashSwid.get256Hash(rimEventLog));
        addNonNullAttribute(attributes, SwidTagConstants._SUPPORT_RIM_TYPE, jsonObject.getString(SwidTagConstants.SUPPORT_RIM_TYPE, ""));
        addNonNullAttribute(attributes, SwidTagConstants._SUPPORT_RIM_FORMAT, jsonObject.getString(SwidTagConstants.SUPPORT_RIM_FORMAT, ""));
        addNonNullAttribute(attributes, SwidTagConstants._SUPPORT_RIM_URI_GLOBAL, jsonObject.getString(SwidTagConstants.SUPPORT_RIM_URI_GLOBAL, ""));

        return file;
    }

    /**
     * This method validates a hirs.swid.xjc.File from an indirect payload
     */
    private boolean validateFile(Element file) {
        String filepath;
        if (rimEventLog.isEmpty()) {
            filepath = file.getAttribute(SwidTagConstants.NAME);
        } else {
            filepath = rimEventLog;
        }
        System.out.println("Support rim found at " + filepath);
        if (HashSwid.get256Hash(filepath).equals(file.getAttribute(_SHA256_HASH.getPrefix() + ":" + _SHA256_HASH.getLocalPart()))) {
            System.out.println("Support RIM hash verified!");
            return true;
        } else {
            System.out.println("Support RIM hash does not match Base RIM!");
            return false;
        }
    }

    /**
     * This method creates a hirs.swid.xjc.File from a direct payload type.
     *
     * @param jsonObject
     * @return hirs.swid.xjc.File object from File object
     *
    private hirs.swid.xjc.File createFile(JsonObject jsonObject) {
        hirs.swid.xjc.File file = objectFactory.createFile();
        file.setName(jsonObject.getString(SwidTagConstants.NAME, ""));
        file.setSize(new BigInteger(jsonObject.getString(SwidTagConstants.SIZE, "0")));
        Map<QName, String> attributes = file.getOtherAttributes();
        addNonNullAttribute(attributes, _SHA256_HASH, jsonObject.getString(SwidTagConstants.HASH, ""));
        addNonNullAttribute(attributes, SwidTagConstants._SUPPORT_RIM_TYPE, jsonObject.getString(SwidTagConstants.SUPPORT_RIM_TYPE, ""));
        addNonNullAttribute(attributes, SwidTagConstants._SUPPORT_RIM_FORMAT, jsonObject.getString(SwidTagConstants.SUPPORT_RIM_FORMAT, ""));
        addNonNullAttribute(attributes, SwidTagConstants._SUPPORT_RIM_URI_GLOBAL, jsonObject.getString(SwidTagConstants.SUPPORT_RIM_URI_GLOBAL, ""));

        return file;
    }*/

    private void addNonNullAttribute(Map<QName, String> attributes, QName key, String value) {
        if (!value.isEmpty()) {
            attributes.put(key, value);
        }
    }

    /**
     * This method signs a SoftwareIdentity with an xmldsig in compatibility mode.
     * Current assumptions: digest method SHA256, signature method SHA256, enveloped signature
     */
    private Document signXMLDocument(JAXBElement<SoftwareIdentity> swidTag) {
        Document doc = null;
        try {
            XMLSignatureFactory sigFactory = XMLSignatureFactory.getInstance("DOM");
            Reference reference = sigFactory.newReference(
                    "",
                    sigFactory.newDigestMethod(DigestMethod.SHA256, null),
                    Collections.singletonList(sigFactory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)),
                    null,
                    null
            );
            SignedInfo signedInfo = sigFactory.newSignedInfo(
                    sigFactory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                    sigFactory.newSignatureMethod(SwidTagConstants.SIGNATURE_ALGORITHM_RSA_SHA256, null),
                    Collections.singletonList(reference)
            );
            List<XMLStructure> keyInfoElements = new ArrayList<XMLStructure>();

            KeyInfoFactory kiFactory = sigFactory.getKeyInfoFactory();
            PrivateKey privateKey;
            PublicKey publicKey;
            CredentialParser cp = new CredentialParser();
            if (defaultCredentials) {
                cp.parseJKSCredentials();
                privateKey = cp.getPrivateKey();
                publicKey = cp.getPublicKey();
            } else {
                cp.parsePEMCredentials(pemCertificateFile, pemPrivateKeyFile);
                X509Certificate certificate = cp.getCertificate();
                privateKey = cp.getPrivateKey();
                publicKey = cp.getPublicKey();
                ArrayList<Object> x509Content = new ArrayList<Object>();
                x509Content.add(certificate.getSubjectX500Principal().getName());
                x509Content.add(certificate);
                X509Data data = kiFactory.newX509Data(x509Content);
                keyInfoElements.add(data);
            }
            KeyName keyName = kiFactory.newKeyName(cp.getCertificateSubjectKeyIdentifier());
            keyInfoElements.add(keyName);
            KeyValue keyValue = kiFactory.newKeyValue(publicKey);
            keyInfoElements.add(keyValue);
            KeyInfo keyinfo = kiFactory.newKeyInfo(keyInfoElements);

            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            marshaller.marshal(swidTag, doc);
            DOMSignContext context = new DOMSignContext(privateKey, doc.getDocumentElement());
            XMLSignature signature = sigFactory.newXMLSignature(signedInfo, keyinfo);
            signature.sign(context);
        } catch (FileNotFoundException e) {
            System.out.println("Keystore not found! " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error loading keystore: " + e.getMessage());
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException |
                        ParserConfigurationException e) {
            System.out.println(e.getMessage());
        } catch (KeyException e) {
            System.out.println("Error setting public key in KeyValue: " + e.getMessage());
        } catch (CertificateException e) {
            System.out.println(e.getMessage());
        } catch (JAXBException e) {
            System.out.println("Error marshaling signed swidtag: " + e.getMessage());
        } catch (MarshalException | XMLSignatureException e) {
            System.out.println("Error while signing SoftwareIdentity: " + e.getMessage());
        }

        return doc;
    }

    /**
     * This method validates a Document with a signature element.
     *
     * @param doc
     */
    private boolean validateSignedXMLDocument(Document doc) {
        boolean isValid = false;
        try {
            NodeList nodes = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
            if (nodes.getLength() == 0) {
                throw new Exception("Signature element not found!");
            }
            DOMValidateContext context = new DOMValidateContext(new X509KeySelector(), nodes.item(0));
            XMLSignatureFactory sigFactory = XMLSignatureFactory.getInstance("DOM");
            XMLSignature signature = sigFactory.unmarshalXMLSignature(context);
            isValid = signature.validate(context);
        } catch (MarshalException | XMLSignatureException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return isValid;
    }

    public class X509KeySelector extends KeySelector {
        public KeySelectorResult select(KeyInfo keyinfo,
                                            KeySelector.Purpose purpose,
                                            AlgorithmMethod algorithm,
                                            XMLCryptoContext context) throws KeySelectorException {
            Iterator keyinfoItr = keyinfo.getContent().iterator();
            while(keyinfoItr.hasNext()) {
                XMLStructure element = (XMLStructure) keyinfoItr.next();
                if (element instanceof X509Data) {
                    X509Data data = (X509Data) element;
                    Iterator dataItr = data.getContent().iterator();
                    while (dataItr.hasNext()) {
                        Object object = dataItr.next();
                        if (object instanceof X509Certificate) {
                            final PublicKey publicKey = ((X509Certificate) object).getPublicKey();
                            if (areAlgorithmsEqual(algorithm.getAlgorithm(), publicKey.getAlgorithm())) {
                                return new RIMKeySelectorResult(publicKey);
                            }
                        }
                    }
                }
            }

            throw new KeySelectorException("No key found!");
        }

        public boolean areAlgorithmsEqual(String uri, String name) {
            if (uri.equals(SwidTagConstants.SIGNATURE_ALGORITHM_RSA_SHA256) && name.equalsIgnoreCase("RSA")) {
                return true;
            } else {
                return false;
            }
        }

        private class RIMKeySelectorResult implements KeySelectorResult {
            private Key key;

            public RIMKeySelectorResult(Key key) {
                this.key = key;
            }

            public Key getKey() {
                return key;
            }
        }
    }

    /**
     * This method unmarshalls the swidtag found at [path] into a Document object
     * and validates it according to the schema.
     *
     * @param path to the input swidtag
     * @return the SoftwareIdentity element at the root of the swidtag
     * @throws IOException if the swidtag cannot be unmarshalled or validated
     */
    private Document unmarshallSwidTag(String path) {
        InputStream is = null;
        Document document = null;
        try {
            document = removeXMLWhitespace(path);
            is = SwidTagGateway.class.getClassLoader().getResourceAsStream(SwidTagConstants.SCHEMA_URL);
            SchemaFactory schemaFactory = SchemaFactory.newInstance(SwidTagConstants.SCHEMA_LANGUAGE);
            Schema schema = schemaFactory.newSchema(new StreamSource(is));
            unmarshaller.setSchema(schema);
            unmarshaller.unmarshal(document);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (SAXException e) {
            System.out.println("Error setting schema for validation!");
        } catch (UnmarshalException e) {
            System.out.println("Error validating swidtag file!");
        } catch (IllegalArgumentException e) {
            System.out.println("Input file empty.");
        } catch (JAXBException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    System.out.println("Error closing input stream");
                }
            }
        }

        return document;
    }

    /**
     * This method strips all whitespace from an xml file, including indents and spaces
     * added for human-readability.
     * @param path
     * @return
     */
    private Document removeXMLWhitespace(String path) throws IOException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Source source = new StreamSource(
                SwidTagGateway.class.getClassLoader().getResourceAsStream("identity_transform.xslt"));
        Document document = null;
        File input = new File(path);
        if (input.length() > 0) {
            try {
                Transformer transformer = tf.newTransformer(source);
                DOMResult result = new DOMResult();
                transformer.transform(new StreamSource(input), result);
                document = (Document) result.getNode();
            } catch (TransformerConfigurationException e) {
                System.out.println("Error configuring transformer!");
                e.printStackTrace();
            } catch (TransformerException e) {
                System.out.println("Error transforming input!");
                e.printStackTrace();
            }
        } else {
            throw new IOException("Input file is empty!");
        }

        return document;
    }
}
