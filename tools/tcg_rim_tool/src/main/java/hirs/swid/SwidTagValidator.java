package hirs.swid;

import hirs.swid.utils.HashSwid;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
import java.io.File;
import java.io.FileNotFoundException;
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

    public SwidTagValidator() {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(SwidTagConstants.SCHEMA_PACKAGE);
            unmarshaller = jaxbContext.createUnmarshaller();
            rimEventLog = "";
            certificateFile = "";
        } catch (JAXBException e) {
            System.out.println("Error initializing JAXBContext: " + e.getMessage());
        }
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
     * This method validates a hirs.swid.xjc.File from an indirect payload
     */
    private boolean validateFile(Element file) {
        String filepath;
        if (!rimEventLog.isEmpty()) {
            filepath = rimEventLog;
        } else {
            filepath = file.getAttribute(SwidTagConstants.NAME);
        }
        System.out.println("Support rim found at " + filepath);
        if (HashSwid.get256Hash(filepath).equals(
                file.getAttribute(SwidTagConstants._SHA256_HASH.getPrefix() + ":" +
                        SwidTagConstants._SHA256_HASH.getLocalPart()))) {
            System.out.println("Support RIM hash verified!");
            return true;
        } else {
            System.out.println("Support RIM hash does not match Base RIM!");
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
                    context = new DOMValidateContext(cp.parseKeyFromPEMCertificate(certificateFile), nodes.item(0));
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
