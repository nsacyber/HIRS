package hirs.swid;

import hirs.swid.utils.HashSwid;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.*;
import javax.xml.crypto.*;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Iterator;

public class SwidTagValidator {
    private static final Logger LOGGER = LogManager.getLogger(SwidTagValidator.class);
    private Unmarshaller unmarshaller;
    private String rimEventLog;
    private String certificateFile;
    private JAXBElement jaxbe;

    /**
     * Setter for rimel file path.
     * @param rimEventLog
     */
    public void setRimEventLog(String rimEventLog) {
        this.rimEventLog = rimEventLog;
    }

    public void setCertificateFile(String certificateFile) {
        this.certificateFile = certificateFile;
    }

    /**
     * This is the default constructor.  In addition to initializing properties it also
     * initializes the jaxbContext and schema-related objects for use in unmarshalling.
     */
    public SwidTagValidator() {
        InputStream schemaStream = null;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(SwidTagConstants.SCHEMA_PACKAGE);
            unmarshaller = jaxbContext.createUnmarshaller();
            rimEventLog = "";
            certificateFile = "";
            schemaStream = SwidTagGateway.class.getClassLoader()
                    .getResourceAsStream(SwidTagConstants.SCHEMA_URL);
            SchemaFactory schemaFactory = SchemaFactory.newInstance(
                                    SwidTagConstants.SCHEMA_LANGUAGE);
            Schema schema = schemaFactory.newSchema(new StreamSource(schemaStream));
            unmarshaller.setSchema(schema);
        } catch (JAXBException e) {
            System.out.println("Error initializing JAXBContext: " + e.getMessage());
        } catch (SAXException e) {
            System.out.println("Error setting schema for validation!");
        } catch (IllegalArgumentException e) {
            System.out.println("Input file empty.");
        } finally {
            if (schemaStream != null) {
                try {
                    schemaStream.close();
                } catch (IOException e) {
                    System.out.println("Error closing input stream");
                }
            }
        }
    }

    /**
     * Getter for property jaxbe.
     * @return JAXBElement containing the unmarshalled swidtag.
     */
    public JAXBElement getJaxbe() {
        return jaxbe;
    }

    /**
     * This method unmarshalls a flat file to a Document object and then validates
     * its XML signature and payload file.
     *
     * @param path to swidtag file
     * @return boolean indicating successful or failed validation
     * @throws IOException
     */
    public boolean validateSwidtagFile(String path) throws IOException {
        try {
            File file = new File(path);
            if (file.length() > 0) {
                StreamSource input = new StreamSource(new File(path));
                Document document = unmarshallDocument(input);
                Element softwareIdentity =
                        (Element) document.getElementsByTagName("SoftwareIdentity").item(0);
                StringBuilder si = new StringBuilder("Base RIM detected:\n");
                si.append("SoftwareIdentity name: "
                        + softwareIdentity.getAttribute("name") + "\n");
                si.append("SoftwareIdentity tagId: "
                        + softwareIdentity.getAttribute("tagId") + "\n");
                System.out.println(si.toString());

                Element payloadFile = (Element) document.getElementsByTagName("File").item(0);
                validateFile(payloadFile);

                if (validateSignedXMLDocument(document)) {
                    System.out.println("Base RIM signature validated!");
                    return true;
                } else {
                    System.out.println("Base RIM signature not valid!");
                    System.exit(1);
                }
            } else {
                System.out.println("Input file is empty!");
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * This method validates a hirs.swid.xjc.File from an indirect payload
     */
    private boolean validateFile(Element payloadFile) {
        String filepath;
        if (!rimEventLog.isEmpty()) {
            filepath = rimEventLog;
        } else {
            filepath = payloadFile.getAttribute(SwidTagConstants.NAME);
        }
        System.out.println("Validating support RIM " + filepath);
        if (HashSwid.get256Hash(filepath).equals(
                payloadFile.getAttribute(SwidTagConstants._SHA256_HASH.getPrefix() + ":"
                        + SwidTagConstants._SHA256_HASH.getLocalPart()))) {
            System.out.println("Support RIM hash verified!" + System.lineSeparator());
            return true;
        } else {
            System.out.println("Support RIM hash does not match Base RIM!"
                    + System.lineSeparator());
            return false;
        }
    }

    /**
     * This method validates a Document with a signature element.
     *
     * @param doc
     */
    public boolean validateSignedXMLDocument(Document doc) {
        DOMValidateContext context = null;
        boolean isValid = false;
        try {
            NodeList nodes = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
            if (nodes.getLength() == 0) {
                throw new Exception("Signature element not found!");
            }
            NodeList embeddedCert = doc.getElementsByTagName("X509Data");
            if (embeddedCert.getLength() > 0) {
                context = new DOMValidateContext(new SwidTagValidator.X509KeySelector(),
                                                                        nodes.item(0));
            } else {
                CredentialParser cp = new CredentialParser();
                if (!certificateFile.isEmpty()) {
                    X509Certificate certificate = cp.parseCertFromPEM(certificateFile);
                    cp.setCertificate(certificate);
                    System.out.println(cp.getCertificateAuthorityInfoAccess());
                    context = new DOMValidateContext(certificate.getPublicKey(),
                                                                nodes.item(0));
                } else {
                    throw new Exception("Signing certificate not found for validation!");
                }
            }
            XMLSignatureFactory sigFactory = XMLSignatureFactory.getInstance("DOM");
            XMLSignature signature = sigFactory.unmarshalXMLSignature(context);
            isValid = signature.validate(context);
        } catch (MarshalException | XMLSignatureException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            System.out.println(e.getMessage());
            LOGGER.warn("Error validating signature: " + e.getMessage());
        }

        return isValid;
    }

    public static class X509KeySelector extends KeySelector {
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
                            if (areAlgorithmsEqual(algorithm.getAlgorithm(),
                                    publicKey.getAlgorithm())) {
                                return new SwidTagValidator.X509KeySelector
                                        .RIMKeySelectorResult(publicKey);
                            }
                        }
                    }
                }
            }

            throw new KeySelectorException("No key found!");
        }

        public boolean areAlgorithmsEqual(String uri, String name) {
            if (uri.equals(SwidTagConstants.SIGNATURE_ALGORITHM_RSA_SHA256)
                    && name.equalsIgnoreCase("RSA")) {
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
     * This method unmarshalls the swidtag into
     * a JAXBElement verified against the schema.
     *
     * @param source containing the input swidtag.
     * @return JAXBElement containing the SoftwareIdentity root element.
     */
    public JAXBElement unmarshallInputStreamToJAXBElement(InputStream source) {
        try {
            jaxbe = (JAXBElement) unmarshaller.unmarshal(source);
        } catch (UnmarshalException e) {
            System.out.println("Error validating swidtag file!");
        } catch (IllegalArgumentException e) {
            System.out.println("Input file empty.");
        } catch (JAXBException e) {
            e.printStackTrace();
        }

        return jaxbe;
    }

    /**
     * This method unmarshalls the swidtag document and verifies against the schema.
     *
     * @param input containing the input swidtag.
     * @return Document containing the SoftwareIdentity root element.
     */
    public Document unmarshallDocument(StreamSource input) {
        Document document = removeXMLWhitespace(input);
        try {
            unmarshaller.unmarshal(document);
        } catch (UnmarshalException e) {
            System.out.println("Error validating swidtag file!");
        } catch (IllegalArgumentException e) {
            System.out.println("Input file empty.");
        } catch (JAXBException e) {
            e.printStackTrace();
        }

        return document;
    }

    /**
     * This method strips all whitespace from an xml file, including indents and spaces
     * added for human-readability.
     * @param input of xml data
     * @return Document object
     */
    private Document removeXMLWhitespace(StreamSource input) {
        TransformerFactory tf = TransformerFactory.newInstance();
        Source identitySource = new StreamSource(SwidTagGateway.class.getClassLoader()
                        .getResourceAsStream("identity_transform.xslt"));
        Document document = null;
        try {
            Transformer transformer = tf.newTransformer(identitySource);
            DOMResult result = new DOMResult();
            transformer.transform(input, result);
            document = (Document) result.getNode();
        } catch (TransformerConfigurationException e) {
            System.out.println("Error configuring transformer!");
            e.printStackTrace();
        } catch (TransformerException e) {
            System.out.println("Error transforming input!");
            e.printStackTrace();
        }

        return document;
    }
}
