package hirs.swid;

import hirs.swid.utils.HashSwid;
import hirs.swid.xjc.SoftwareIdentity;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
    private Unmarshaller unmarshaller;
    private String rimEventLog;
    private String certificateFile;
    private boolean schemaValid, payloadFileValid, signatureValid;
    private SoftwareIdentity softwareIdentity;

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
        InputStream is = null;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(SwidTagConstants.SCHEMA_PACKAGE);
            unmarshaller = jaxbContext.createUnmarshaller();
            rimEventLog = "";
            certificateFile = "";
            schemaValid = false;
            payloadFileValid = false;
            signatureValid = false;
            is = SwidTagGateway.class.getClassLoader().getResourceAsStream(SwidTagConstants.SCHEMA_URL);
            SchemaFactory schemaFactory = SchemaFactory.newInstance(SwidTagConstants.SCHEMA_LANGUAGE);
            Schema schema = schemaFactory.newSchema(new StreamSource(is));
            unmarshaller.setSchema(schema);
        } catch (JAXBException e) {
            System.out.println("Error initializing JAXBContext: " + e.getMessage());
        } catch (SAXException e) {
            System.out.println("Error setting schema for validation!");
        } catch (IllegalArgumentException e) {
            System.out.println("Input file empty.");
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    System.out.println("Error closing input stream");
                }
            }
        }
    }

    /**
     * Getter for property schemaValid
     * @return
     */
    public boolean isSchemaValid() {
        return schemaValid;
    }

    /**
     * Getter for property payloadFileValid
     * @return
     */
    public boolean isPayloadFileValid() {
        return payloadFileValid;
    }

    /**
     * Getter for property signatureValid
     * @return
     */
    public boolean isSignatureValid() {
        return signatureValid;
    }

    /**
     * Getter for property softwareIdentity
     * @return
     */
    public SoftwareIdentity getSoftwareIdentity() {
        return softwareIdentity;
    }

    /**
     * This method unmarshalls a flat file to a Document object and passes it to
     * the validate method.
     * @param path to swidtag file
     * @return boolean indicating successful or failed validation
     * @throws IOException
     */
    public void validateSwidtagFile(String path) throws IOException {
        File file;
        StreamSource input;
        try {
            file = new File(path);
            if (file.length() > 0) {
                input = new StreamSource(new File(path));
                Document document = removeXMLWhitespace(input);
                validate(unmarshallSwidTag(document));
            } else {
                System.out.println("Input file is empty!");
            }
        } catch (NullPointerException e) {
            System.out.println("Path to file must be non-null!");
        }
    }

    /**
     * This method validates a swidtag uploaded into an inputstream from ACAPortal.
     * Schema validation is disabled (null) to save time.
     * @param is
     * @throws IOException
     */
    public void validateSwidtagInputStream(InputStream is) throws IOException {
        Document document = removeXMLWhitespace(new StreamSource(is));
        unmarshaller.setSchema(null);
        validate(unmarshallSwidTag(document));
    }

    /**
     * This method validates the Document parameter against the swidtag
     * schema. Three aspects are inspected, with the following output on success or failure:
     * 1. Successful schema validation results in the output of the tag's name
     * and tagId attributes, otherwise a generic error message is printed.
     * 2. Payload file validation results in a message indicating success or failure.
     * 3. Signature validation results in a message of true for success or false for failure.
     *
     * @param document containing the file to be validated
     */
    public boolean validate(Document document) throws IOException {
        Element softwareIdentity = (Element) document.getElementsByTagName("SoftwareIdentity").item(0);
        StringBuilder si = new StringBuilder("Base RIM detected:\n");
        si.append("SoftwareIdentity name: " + softwareIdentity.getAttribute("name") + "\n");
        si.append("SoftwareIdentity tagId: " + softwareIdentity.getAttribute("tagId") + "\n");
        System.out.println(si.toString());
        schemaValid = true;
        Element payloadFile = (Element) document.getElementsByTagName("File").item(0);
        try {
            payloadFileValid = validateFile(payloadFile);
        } catch (NullPointerException e) {
            System.out.println("Error validating payload file: " + e.getMessage());
        }
        if (validateSignedXMLDocument(document)) {
            System.out.println("Base RIM signature validated!");
            signatureValid = true;
        } else {
            System.out.println("Base RIM signature not valid!");
        }
        return true;
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
        System.out.println("Support rim found at " + filepath);
        if (HashSwid.get256Hash(filepath).equals(
                payloadFile.getAttribute(SwidTagConstants._SHA256_HASH.getPrefix() + ":" +
                        SwidTagConstants._SHA256_HASH.getLocalPart()))) {
            System.out.println("Support RIM hash verified!" + System.lineSeparator());
            return true;
        } else {
            System.out.println("Support RIM hash does not match Base RIM!" + System.lineSeparator());
            return false;
        }
    }

    /**
     * This method validates a Document with a signature element.
     *
     * @param doc
     */
    private boolean validateSignedXMLDocument(Document doc) {
        DOMValidateContext context = null;
        boolean isValid = false;
        try {
            NodeList nodes = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
            if (nodes.getLength() == 0) {
                throw new Exception("Signature element not found!");
            }
            NodeList embeddedCert = doc.getElementsByTagName("X509Data");
            if (embeddedCert.getLength() > 0) {
                context = new DOMValidateContext(new SwidTagValidator.X509KeySelector(), nodes.item(0));
            } else {
                CredentialParser cp = new CredentialParser();
                if (!certificateFile.isEmpty()) {
                    X509Certificate certificate = cp.parseCertFromPEM(certificateFile);
                    cp.setCertificate(certificate);
                    System.out.println(cp.getCertificateAuthorityInfoAccess());
                    context = new DOMValidateContext(certificate.getPublicKey(), nodes.item(0));
                } else {
                    System.out.println("Signing certificate not found for validation!");
                    System.exit(1);
                }
            }
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
                                return new SwidTagValidator.X509KeySelector.RIMKeySelectorResult(publicKey);
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
     * This method unmarshalls the swidtag into a Document object.
     *
     * @param document containing the input swidtag
     * @return the SoftwareIdentity element at the root of the swidtag
     */
    private Document unmarshallSwidTag(Document document) {
        try {
            JAXBElement jaxbe = (JAXBElement) unmarshaller.unmarshal(document);
            softwareIdentity = (SoftwareIdentity) jaxbe.getValue();
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
        Source source = new StreamSource(
                SwidTagGateway.class.getClassLoader().getResourceAsStream("identity_transform.xslt"));
        Document document = null;
        try {
            Transformer transformer = tf.newTransformer(source);
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
