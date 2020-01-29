package hirs.swid;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.UnmarshalException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
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
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import java.math.BigInteger;

import hirs.swid.utils.CsvParser;
import hirs.swid.utils.HashSwid;
import hirs.swid.xjc.BaseElement;
import hirs.swid.xjc.CanonicalizationMethodType;
import hirs.swid.xjc.DigestMethodType;
import hirs.swid.xjc.Directory;
import hirs.swid.xjc.Entity;
import hirs.swid.xjc.Link;
import hirs.swid.xjc.ObjectFactory;
import hirs.swid.xjc.ResourceCollection;
import hirs.swid.xjc.ReferenceType;
import hirs.swid.xjc.SignatureType;
import hirs.swid.xjc.SignatureValueType;
import hirs.swid.xjc.SignatureMethodType;
import hirs.swid.xjc.SignedInfoType;
import hirs.swid.xjc.SoftwareIdentity;
import hirs.swid.xjc.SoftwareMeta;
import hirs.swid.xjc.TransformType;
import hirs.swid.xjc.TransformsType;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonObject.Member;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.Location;
import com.eclipsesource.json.ParseException;



/**
 * This class provides interaction with the SWID Tag schema as defined in
 * http://standards.iso.org/iso/19770/-2/2015/schema.xsd
 *
 */
public class SwidTagGateway {

    private static final QName _DEFAULT_QNAME = new QName(
            "http://www.w3.org/2000/09/xmldsig#", "SHA256", "ds");
    private static final QName _SHA1Value_QNAME = new QName(
            "http://www.w3.org/2000/09/xmldsig#", "SHA1", "ds");
    private static final QName _SHA384Value_QNAME = new QName(
            "http://www.w3.org/2000/09/xmldsig#", "SHA384", "ds");
    private static final QName _SHA512Value_QNAME = new QName(
            "http://www.w3.org/2000/09/xmldsig#", "SHA512", "ds");
    private static final QName _SHA256_HASH = new QName(
            "http://www.w3.org/2001/04/xmlenc#sha256", "hash", "SHA256");

    private final ObjectFactory objectFactory = new ObjectFactory();
    private final File generatedFile = new File("generated_swidTag.swidtag");
    private final Path configFile = Paths.get("/etc/hirs/rim_fields.json");
    private QName hashValue = null;

    private JAXBContext jaxbContext;
    private Marshaller marshaller;
    private Unmarshaller unmarshaller;

    /**
     * Default constructor initializes jaxbcontext, marshaller, and unmarshaller
     */
    public SwidTagGateway() {
        try {
            jaxbContext = JAXBContext.newInstance(SwidTagConstants.SCHEMA_PACKAGE);
            marshaller = jaxbContext.createMarshaller();
            unmarshaller = jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {
            System.out.println("Error initializing jaxbcontext: " + e.getMessage());
        }
    }

    /**
     * default generator method that has no parameters
     */
    public void generateSwidTag() {
        generateSwidTag(generatedFile);
    }

    /**
     * This generator method is used by the create method.
     *
     * This method should be updated to incorporate the RIM fields that are implemented
     * in generateSwidTag(final File outputFile) below.
     *
     * @param inputFile - the file in csv format that is used as data
     * @param outputFile - output specific to the given file
     * @param hashType - the optional labeling of the hash type
     */
    public void generateSwidTag(final String inputFile,
            final String outputFile, final String hashType) {
        // create file instances
        File input = new File(inputFile);
        File output = new File(outputFile);
        List<String> tempList = new LinkedList<>();

        // I need to go over this again about which needs to be checked.
        if (input.exists()) {
            // parse the csv file
            CsvParser parser = new CsvParser(input);
            for (String line : parser.getContent()) {
                tempList.add(line);
            }

            if (hashType.contains("256")) {
                hashValue = _DEFAULT_QNAME;
            } else if (hashType.contains("384")) {
                hashValue = _SHA384Value_QNAME;
            } else if (hashType.contains("512")) {
                hashValue = _SHA512Value_QNAME;
            } else if (hashType.contains("1")) {
                hashValue = _SHA1Value_QNAME;
            } else {
                hashValue = _DEFAULT_QNAME;
            }

            // generate a swid tag
            Properties properties = new Properties();
            InputStream is = null;
            try {
                is = SwidTagGateway.class.getClassLoader().getResourceAsStream(SwidTagConstants.HIRS_SWIDTAG_HEADERS);
                properties.load(is);

                SoftwareIdentity swidTag = createSwidTag(new JsonObject());

                JAXBElement<Entity> entity = objectFactory.createSoftwareIdentityEntity(createEntity(new JsonObject()));
                swidTag.getEntityOrEvidenceOrLink().add(entity);

                // we should have resources, there for we need a collection
                JAXBElement<ResourceCollection> resources = objectFactory.createSoftwareIdentityPayload(createPayload(tempList, hashValue));
                swidTag.getEntityOrEvidenceOrLink().add(resources);

                JAXBElement<SoftwareIdentity> jaxbe = objectFactory.createSoftwareIdentity(swidTag);
                writeSwidTagFile(jaxbe, output);
            } catch (IOException e) {
                System.out.println("Error reading properties file: ");
                e.printStackTrace();
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ex) {
                        // ignore
                    }
                }
            }
        }
    }

    /**
     * This method generates a base RIM from the values in a JSON file.
     *
     * @param outputFile
     */
    public void generateSwidTag(final File outputFile) {
        SoftwareIdentity swidTag = null;
        try {
            BufferedReader jsonIn = Files.newBufferedReader(configFile, StandardCharsets.UTF_8);
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
        System.out.println("Signature core validity: " + validateSignedXMLDocument(signedSoftwareIdentity));
        writeSwidTagFile(signedSoftwareIdentity);
    }

   /**
     * This method validates the .swidtag file at the given filepath against the
     * schema. A successful validation results in the output of the tag's name
     * and tagId attributes, otherwise a generic error message is printed.
     *
     * @param path the location of the file to be validated
     */
    public boolean validateSwidTag(String path) throws IOException {
        JAXBElement jaxbe = unmarshallSwidTag(path);
        SoftwareIdentity swidTag = (SoftwareIdentity) jaxbe.getValue();
        String output = String.format("name: %s;\ntagId:  %s\n%s",
                swidTag.getName(), swidTag.getTagId(),
                SwidTagConstants.SCHEMA_STATEMENT);
        System.out.println("SWID Tag found: ");
        System.out.println(output);
        return true;
    }

    /**
     * This method calls the marshal() method that writes the swidtag data to the output file.
     *
     * @param jaxbe
     * @param outputFile
     */
    public void writeSwidTagFile(JAXBElement<SoftwareIdentity> jaxbe, File outputFile) {
        JAXBContext jaxbContext;
        try {
            jaxbContext = JAXBContext.newInstance(SwidTagConstants.SCHEMA_PACKAGE);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(jaxbe, outputFile);
        } catch (JAXBException e) {
            System.out.println("Error generating xml: ");
            e.printStackTrace();
        } 
    }

    /**
     * This method writes a Document object out to the file specified by generatedFile.
     *
     * @param swidTag
     */
    public void writeSwidTagFile(Document swidTag) {
        try {
            OutputStream outStream = new FileOutputStream(generatedFile);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(new DOMSource(swidTag), new StreamResult(outStream));
        } catch (FileNotFoundException e) {
            System.out.println("Unable to write to file: " + e.getMessage());
        } catch (TransformerConfigurationException e) {
            System.out.println("Error instantiating TransformerFactory class: " + e.getMessage());
        } catch (TransformerException e) {
            System.out.println("Error instantiating Transformer class: " + e.getMessage());
        }
    }
    
    /**
     * Given an input swidtag at [path] parse any PCRs in the payload into an InputStream object.
     * This method will be used in a following pull request.
     *
     * @param path
     * @return
     * @throws IOException
     */
    public ByteArrayInputStream parsePayload(String path) throws IOException {
        JAXBElement jaxbe = unmarshallSwidTag(path);
		SoftwareIdentity softwareIdentity = (SoftwareIdentity) jaxbe.getValue();
		String pcrs = "";
		if (!softwareIdentity.getEntityOrEvidenceOrLink().isEmpty()) {
			List<Object> swidtag = softwareIdentity.getEntityOrEvidenceOrLink();
			for (Object obj : swidtag) {
				try {
					JAXBElement element = (JAXBElement) obj;
					String elementName = element.getName().getLocalPart();
					if (elementName.equals(SwidTagConstants.PAYLOAD)) {
						ResourceCollection rc = (ResourceCollection) element.getValue();
						if (!rc.getDirectoryOrFileOrProcess().isEmpty()) {
							pcrs = parsePCRs(rc.getDirectoryOrFileOrProcess());
						}
					}
				} catch (ClassCastException e) {
					System.out.println("Found a non-JAXBElement object!" + e.getMessage());
					throw new IOException("Found an invalid element in the swidtag file!");
				}
			}
		}
		return new ByteArrayInputStream(pcrs.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * This method creates SoftwareIdentity element based on the parameters read in from
     * a properties file.
     *
     * @param properties the Properties object containing parameters from file
     * @return SoftwareIdentity object created from the properties
     */
    private SoftwareIdentity createSwidTag(JsonObject jsonObject) {
        SoftwareIdentity swidTag = objectFactory.createSoftwareIdentity();
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

        return swidTag;
    }

    /**
     * This method creates an Entity object based on the parameters read in from
     * a properties file.
     *
     * @param properties the Properties object containing parameters from file
     * @return Entity object created from the properties
     */
    private Entity createEntity(JsonObject jsonObject) {
        Entity entity = objectFactory.createEntity();
        String name = jsonObject.getString(SwidTagConstants.NAME, "");
        if (!name.isEmpty()) {
            entity.setName(name);
        }
        String regId = jsonObject.getString(SwidTagConstants.REGID, "");
        if (!regId.isEmpty()) {
            entity.setRegid(regId);
        }
        String[] roles = jsonObject.getString(SwidTagConstants.ROLE, "").split(",");
        for (int i = 0; i < roles.length; i++) {
            entity.getRole().add(roles[i]);
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
     * @param properties the Properties object containing parameters from file
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
     * @param properties the Properties object containing parameters from file
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
     * @param properties the Properties object containing parameters from file
     * @return the Payload object created
     */
    private ResourceCollection createPayload(JsonObject jsonObject) {
        ResourceCollection payload = objectFactory.createResourceCollection();
        Map<QName, String> attributes = payload.getOtherAttributes();
        addNonNullAttribute(attributes, SwidTagConstants._N8060_ENVVARPREFIX, jsonObject.getString(SwidTagConstants.PAYLOAD_ENVVARPREFIX, ""));
        addNonNullAttribute(attributes, SwidTagConstants._N8060_ENVVARSUFFIX, jsonObject.getString(SwidTagConstants.PAYLOAD_ENVVARSUFFIX, ""));
        addNonNullAttribute(attributes, SwidTagConstants._N8060_PATHSEPARATOR, jsonObject.getString(SwidTagConstants.PAYLOAD_PATHSEPARATOR, ""));

        return payload;
    }

    /**
     * This method creates a Directory from the parameters read in from a properties file.
     *
     * @param properties the Properties object containing parameters from file
     * @return Directory object created from the properties
     */
    private Directory createDirectory(JsonObject jsonObject) {
        Directory directory = objectFactory.createDirectory();
        directory.setName(jsonObject.getString(SwidTagConstants.NAME, ""));
        Map<QName, String> attributes = directory.getOtherAttributes();
        addNonNullAttribute(attributes, SwidTagConstants._SUPPORT_RIM_TYPE, jsonObject.getString(SwidTagConstants.SUPPORT_RIM_TYPE, ""));
        addNonNullAttribute(attributes, SwidTagConstants._SUPPORT_RIM_FORMAT, jsonObject.getString(SwidTagConstants.SUPPORT_RIM_FORMAT, ""));
        addNonNullAttribute(attributes, SwidTagConstants._SUPPORT_RIM_URI_GLOBAL, jsonObject.getString(SwidTagConstants.SUPPORT_RIM_URI_GLOBAL, ""));
/*
        directory.setLocation(jsonObject.getString(SwidTagConstants.DIRECTORY_LOCATION));
        String directoryRoot = jsonObject.getString(SwidTagConstants.DIRECTORY_ROOT);
        if (!directoryRoot.isEmpty()) {
            directory.setRoot(directoryRoot);
        }
*/
        return directory;
    }

   /**
     * This method creates a hirs.swid.xjc.File from three arguments, then calculates
     * and stores its hash as an attribute in itself.
     *
     * @param filename
     * @param location
     * @return hirs.swid.xjc.File object from File object
     */
    private hirs.swid.xjc.File createFile(JsonObject jsonObject) {
        hirs.swid.xjc.File file = objectFactory.createFile();
        file.setName(jsonObject.getString(SwidTagConstants.NAME, ""));
        file.setSize(new BigInteger(jsonObject.getString(SwidTagConstants.SIZE, "")));
        Map<QName, String> attributes = file.getOtherAttributes();
        addNonNullAttribute(attributes, _SHA256_HASH, jsonObject.getString(SwidTagConstants.HASH, ""));
        addNonNullAttribute(attributes, SwidTagConstants._SUPPORT_RIM_TYPE, jsonObject.getString(SwidTagConstants.SUPPORT_RIM_TYPE, ""));
        addNonNullAttribute(attributes, SwidTagConstants._SUPPORT_RIM_FORMAT, jsonObject.getString(SwidTagConstants.SUPPORT_RIM_FORMAT, ""));
        addNonNullAttribute(attributes, SwidTagConstants._SUPPORT_RIM_URI_GLOBAL, jsonObject.getString(SwidTagConstants.SUPPORT_RIM_URI_GLOBAL, ""));

        return file;
    }

    private void addNonNullAttribute(Map<QName, String> attributes, QName key, String value) {
        if (!value.isEmpty()) {
            attributes.put(key, value);
        }
    }

    /**
     * This method creates a Payload from a list of Strings and a hash algorithm.
     * The Strings in the list are expected to be in the form of "[PCR_NUMBER],[PCR_VALUE]"
     * and the hash algorithm is attached as the file's xml namespace identifier.
     *
     * @param populate
     * @return
     */
    private ResourceCollection createPayload(List<String> populate, QName hashStr) {
        ResourceCollection rc = objectFactory.createResourceCollection();
        hirs.swid.xjc.File xjcFile = null;
        String[] tempArray = null;

        for (String item : populate) {
            xjcFile = objectFactory.createFile();

            tempArray = item.split(",");

            xjcFile.setName(tempArray[SwidTagConstants.PCR_NUMBER]);
            xjcFile.getOtherAttributes().put(hashStr, tempArray[SwidTagConstants.PCR_VALUE]);
            rc.getDirectoryOrFileOrProcess().add(xjcFile);
        }

        return rc;
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
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(new FileInputStream(SwidTagConstants.KEYSTORE_PATH), SwidTagConstants.KEYSTORE_PASSWORD.toCharArray());
            KeyStore.PrivateKeyEntry privateKey = (KeyStore.PrivateKeyEntry) keystore.getEntry(SwidTagConstants.PRIVATE_KEY_ALIAS,
                    new KeyStore.PasswordProtection(SwidTagConstants.KEYSTORE_PASSWORD.toCharArray()));
            X509Certificate certificate = (X509Certificate) privateKey.getCertificate();
            KeyInfoFactory kiFactory = sigFactory.getKeyInfoFactory();
            ArrayList<Object> x509Content = new ArrayList<Object>();
            x509Content.add(certificate.getSubjectX500Principal().getName());
            x509Content.add(certificate);
            X509Data data = kiFactory.newX509Data(x509Content);
            KeyInfo keyinfo = kiFactory.newKeyInfo(Collections.singletonList(data));

            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            marshaller.marshal(swidTag, doc);
            DOMSignContext context = new DOMSignContext(privateKey.getPrivateKey(), doc.getDocumentElement());
            XMLSignature signature = sigFactory.newXMLSignature(signedInfo, keyinfo);
            signature.sign(context);
        } catch (FileNotFoundException e) {
            System.out.println("Keystore not found! " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error loading keystore: " + e.getMessage());
        } catch (NoSuchAlgorithmException | KeyStoreException | InvalidAlgorithmParameterException |
                        ParserConfigurationException | UnrecoverableEntryException e) {
            System.out.println(e.getMessage());
        } catch (CertificateException e) {
            System.out.println("Certificate error: " + e.getMessage());
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
     * This method traverses a hirs.swid.xjc.Directory recursively until it finds at
     * least one hirs.swid.xjc.File.  This File is expected to have an attribute of the form
     * "[hash algorithm]=[hash value]."
     *
     * @param list of swidtag elements
     * @return the hash value(s) parsed from the File object(s)
     */
    private String parsePCRs(List list) {
        final String newline = System.lineSeparator();
    	StringBuilder sb = new StringBuilder();
    	for (Object listItem : list) {
    		if (listItem instanceof Directory) {
    			Directory dir = (Directory) listItem;
    			if (!dir.getDirectoryOrFile().isEmpty()) {
    				parsePCRs(dir.getDirectoryOrFile());
    			}
    		} else if (listItem instanceof hirs.swid.xjc.File){
    			hirs.swid.xjc.File pcr = (hirs.swid.xjc.File) listItem;
    			String pcrHash = "";
    			if (!pcr.getOtherAttributes().isEmpty()) {
    				Object[] fileAttributes = pcr.getOtherAttributes().values().toArray();
    				pcrHash = (String) fileAttributes[0];
    			}
    			if (pcrHash.isEmpty()) {
    				pcrHash = "null";
    			}
    			sb.append(pcr.getName() + "," + pcrHash + newline);
    		}
    	}
    	System.out.println(sb.toString());
    	return sb.toString();
    }

    /**
     * This method unmarshalls the swidtag found at [path] and validates it according to the
     * schema.
     *
     * @param path to the input swidtag
     * @return the SoftwareIdentity element at the root of the swidtag
     * @throws IOException if the swidtag cannot be unmarshalled or validated
     */
    private JAXBElement unmarshallSwidTag(String path) throws IOException {
    	File input = null;
    	InputStream is = null;
    	JAXBElement jaxbe = null;
    	try {
    		input = new File(path);
    		is = SwidTagGateway.class.getClassLoader().getResourceAsStream(SwidTagConstants.SCHEMA_URL);
    		SchemaFactory schemaFactory = SchemaFactory.newInstance(SwidTagConstants.SCHEMA_LANGUAGE);
    		Schema schema = schemaFactory.newSchema(new StreamSource(is));
    		unmarshaller.setSchema(schema);
    		jaxbe = (JAXBElement) unmarshaller.unmarshal(input);
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
        	if (jaxbe != null) {
        	    return jaxbe;
        	} else {
        	    throw new IOException("Invalid swidtag file!");
        	}
        }
    }
}
