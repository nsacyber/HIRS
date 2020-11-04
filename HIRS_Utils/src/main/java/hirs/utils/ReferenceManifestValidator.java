package hirs.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.crypto.*;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Iterator;

public class ReferenceManifestValidator {
    private static final String SIGNATURE_ALGORITHM_RSA_SHA256 =
                    "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
    private static final String SCHEMA_PACKAGE = "hirs.utils.xjc";
    private static final String SCHEMA_URL = "swid_schema.xsd";
    private static final String SCHEMA_LANGUAGE = XMLConstants.W3C_XML_SCHEMA_NS_URI;
    private static final String IDENTITY_TRANSFORM = "identity_transform.xslt";
    private static final String SHA256 = "SHA-256";
    private static final Logger LOGGER = LogManager.getLogger(ReferenceManifestValidator.class);

    private Unmarshaller unmarshaller;
    private PublicKey publicKey;
    private Document document;
    private boolean signatureValid, supportRimValid;

    public boolean isSignatureValid() {
        return signatureValid;
    }

    public boolean isSupportRimValid() {
        return supportRimValid;
    }

    public PublicKey getPublicKey() { return publicKey; }

    public Document getDocument() { return document; }

    public ReferenceManifestValidator() {}

    public ReferenceManifestValidator(InputStream input) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(SCHEMA_PACKAGE);
            unmarshaller = jaxbContext.createUnmarshaller();
            signatureValid = false;
            supportRimValid = false;
            publicKey = null;
            document = unmarshallSwidTag(removeXMLWhitespace(new StreamSource(input)));
        } catch (JAXBException e) {
            LOGGER.warn("Error initializing JAXBContext: " + e.getMessage());
        } catch (IOException e) {
            LOGGER.warn("Error during unmarshalling: " + e.getMessage());
        }
    }

    public void validateSupportRimHash(byte[] input, Document doc) {
        String calculatedHash = getHashValue(input, SHA256);
        Element file = (Element) doc.getElementsByTagName("File").item(0);
        LOGGER.info("Calculated hash: " + calculatedHash
                + ", actual: " + file.getAttribute("SHA256:hash"));
        if (file.getAttribute("SHA256:hash").equals(calculatedHash)) {
            supportRimValid = true;
        } else {
            supportRimValid = false;
        }
    }

    /**
     * This method validates the signature block in the Document and stores the
     * result for public access.
     *
     * @param doc the xml data in Document form.
     */
    public void validateXmlSignature(Document doc) throws IOException {
        signatureValid = validateSignedXMLDocument(doc);
    }

    private String getHashValue(byte[] input, String sha) {
        String resultString = null;
        try {
            MessageDigest md = MessageDigest.getInstance(sha);
            byte[] bytes = md.digest(input);
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < bytes.length; i++) {
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            resultString = sb.toString();
        } catch (NoSuchAlgorithmException grex) {
            LOGGER.warn(grex.getMessage());
        }

        return resultString;
    }

    /**
     * This method validates a Document with a signature element.
     *
     * @param doc
     */
    private boolean validateSignedXMLDocument(Document doc) {
        DOMValidateContext context;
        boolean isValid = false;
        try {
            NodeList nodes = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
            if (nodes.getLength() == 0) {
                throw new Exception("Signature element not found!");
            }
            NodeList embeddedCert = doc.getElementsByTagName("X509Data");
            if (embeddedCert.getLength() > 0) {
                X509KeySelector keySelector = new ReferenceManifestValidator.X509KeySelector();
                context = new DOMValidateContext(keySelector, nodes.item(0));
                XMLSignatureFactory sigFactory = XMLSignatureFactory.getInstance("DOM");
                XMLSignature signature = sigFactory.unmarshalXMLSignature(context);
                isValid = signature.validate(context);
                publicKey = keySelector.getPublicKey();
            } else {
                LOGGER.info("Signing certificate not found for validation!");
            }
        } catch (MarshalException | XMLSignatureException e) {
            LOGGER.warn(e.getMessage());
        } catch (Exception e) {
            LOGGER.warn(e.getMessage());
            LOGGER.info(e.getMessage());
        }

        return isValid;
    }

    public class X509KeySelector extends KeySelector {
        private PublicKey publicKey;

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
                            publicKey = ((X509Certificate) object).getPublicKey();
                            if (areAlgorithmsEqual(algorithm.getAlgorithm(), publicKey.getAlgorithm())) {
                                return new ReferenceManifestValidator.X509KeySelector.RIMKeySelectorResult(publicKey);
                            }
                        }
                    }
                }
            }

            throw new KeySelectorException("No key found!");
        }

        public boolean areAlgorithmsEqual(String uri, String name) {
            if (uri.equals(SIGNATURE_ALGORITHM_RSA_SHA256) && name.equalsIgnoreCase("RSA")) {
                return true;
            } else {
                return false;
            }
        }

        public PublicKey getPublicKey() {
            return publicKey;
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
     * @param doc of the input swidtag.
     * @return document validated against the schema.
     */
    private Document unmarshallSwidTag(Document doc) {
        InputStream is = null;
        try {
            is = ReferenceManifestValidator.class
                    .getClassLoader().getResourceAsStream(SCHEMA_URL);
            SchemaFactory schemaFactory = SchemaFactory.newInstance(SCHEMA_LANGUAGE);
            Schema schema = schemaFactory.newSchema(new StreamSource(is));
            unmarshaller.setSchema(schema);
            unmarshaller.unmarshal(doc);
        } catch (SAXException e) {
            LOGGER.warn("Error setting schema for validation!");
        } catch (UnmarshalException e) {
            LOGGER.warn("Error validating swidtag file!");
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Input file empty.");
        } catch (JAXBException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    LOGGER.warn("Error closing input stream");
                }
            }
        }

        return doc;
    }

    /**
     * This method strips all whitespace from an xml file, including indents and spaces
     * added for human-readability.
     * @param source of the input xml.
     * @return Document representation of the xml.
     */
    private Document removeXMLWhitespace(StreamSource source) throws IOException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Source identitySource = new StreamSource(
                ReferenceManifestValidator.class.getClassLoader()
                            .getResourceAsStream(IDENTITY_TRANSFORM));
        Document doc = null;
        try {
            Transformer transformer = tf.newTransformer(identitySource);
            DOMResult result = new DOMResult();
            transformer.transform(source, result);
            doc = (Document) result.getNode();
        } catch (TransformerConfigurationException e) {
            LOGGER.warn("Error configuring transformer!");
            e.printStackTrace();
        } catch (TransformerException e) {
            LOGGER.warn("Error transforming input!");
            e.printStackTrace();
        }

        return doc;
    }
}
