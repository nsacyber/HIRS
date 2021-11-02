package hirs.utils;

import hirs.data.persist.ReferenceManifest;
import hirs.data.persist.certificate.CertificateAuthorityCredential;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Iterator;


/**
 * This class handles validation functions of RIM files.
 * Currently supports validation of support RIM hashes and
 * base RIM signatures.
 */
public class ReferenceManifestValidator {
    private static final String SIGNATURE_ALGORITHM_RSA_SHA256 =
            "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
    private static final String SCHEMA_PACKAGE = "hirs.utils.xjc";
    private static final String SCHEMA_URL = "swid_schema.xsd";
    private static final String SCHEMA_LANGUAGE = XMLConstants.W3C_XML_SCHEMA_NS_URI;
    private static final String IDENTITY_TRANSFORM = "identity_transform.xslt";
    private static final String SHA256 = "SHA-256";
    private static final int EIGHT_BIT_MASK = 0xff;
    private static final int LEFT_SHIFT = 0x100;
    private static final int RADIX = 16;
    private static final Logger LOGGER = LogManager.getLogger(ReferenceManifestValidator.class);

    private Document rim;
    private Unmarshaller unmarshaller;
    private PublicKey publicKey;
    private Schema schema;
    private String subjectKeyIdentifier;
    private boolean signatureValid, supportRimValid;

    /**
     * Setter for the RIM to be validated.  The ReferenceManifest object is converted into a
     * Document for processing.
     *
     * @param rim ReferenceManifest object
     */
    public void setRim(final ReferenceManifest rim) {
        try {
            Document doc = validateSwidtagSchema(removeXMLWhitespace(new StreamSource(
                    new ByteArrayInputStream(rim.getRimBytes()))));
            this.rim = doc;
        } catch (IOException e) {
            LOGGER.error("Error while unmarshalling rim bytes: " + e.getMessage());
        }
    }

    /**
     * Getter for signatureValid.
     *
     * @return true if valid, false if not.
     */
    public boolean isSignatureValid() {
        return signatureValid;
    }

    /**
     * Getter for supportRimValid.
     *
     * @return true if valid, false if not.
     */
    public boolean isSupportRimValid() {
        return supportRimValid;
    }

    /**
     * Getter for certificate PublicKey.
     *
     * @return PublicKey
     */
    public PublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * Getter for subjectKeyIdentifier.
     *
     * @return subjectKeyIdentifier
     */
    public String getSubjectKeyIdentifier() {
        return subjectKeyIdentifier;
    }

    /**
     * This default constructor creates the Schema object from SCHEMA_URL immediately to save
     * time during validation calls later.
     */
    public ReferenceManifestValidator() {
        try {
            InputStream is = ReferenceManifestValidator.class
                    .getClassLoader().getResourceAsStream(SCHEMA_URL);
            SchemaFactory schemaFactory = SchemaFactory.newInstance(SCHEMA_LANGUAGE);
            schema = schemaFactory.newSchema(new StreamSource(is));
            rim = null;
            signatureValid = false;
            supportRimValid = false;
            publicKey = null;
            subjectKeyIdentifier = "(not found)";
        } catch (SAXException e) {
            LOGGER.warn("Error setting schema for validation!");
        }
    }

    /**
     * This method attempts to validate the signature element of the instance's RIM
     * using a given cert.  The cert is compared to either the RIM's embedded certificate
     * or the RIM's subject key identifier.  If the cert is matched then validation proceeds,
     * otherwise validation ends.
     *
     * @param cert the cert to be checked against the RIM
     * @return true if the signature element is validated, false otherwise
     */
    @SuppressWarnings("magicnumber")
    public boolean validateXmlSignature(final CertificateAuthorityCredential cert) {
        DOMValidateContext context = null;
        try {
            NodeList nodes = rim.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
            if (nodes.getLength() == 0) {
                LOGGER.error("Cannot validate RIM, signature element not found!");
                return false;
            }
            NodeList certElement = rim.getElementsByTagName("X509Certificate");
            if (certElement.getLength() > 0) {
                X509Certificate embeddedCert = parseCertFromPEMString(
                        certElement.item(0).getTextContent());
                if (embeddedCert != null) {
                    subjectKeyIdentifier = getCertificateSubjectKeyIdentifier(embeddedCert);
                    if (Arrays.equals(embeddedCert.getPublicKey().getEncoded(),
                            cert.getEncodedPublicKey())) {
                        context = new DOMValidateContext(new X509KeySelector(), nodes.item(0));
                    }
                }
            } else {
                subjectKeyIdentifier = getKeyName(rim);
                if (subjectKeyIdentifier.equals(cert.getSubjectKeyIdString().substring(8))) {
                    context = new DOMValidateContext(cert.getX509Certificate().getPublicKey(),
                            nodes.item(0));
                }
            }
            if (context != null) {
                publicKey = cert.getX509Certificate().getPublicKey();
                signatureValid = validateSignedXMLDocument(context);
                return signatureValid;
            }
        } catch (IOException e) {
            LOGGER.warn("Error while parsing certificate data: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * This method calculates the SHA256 hash of the input byte array and compares it against
     * the value passed in.
     *
     * @param input    byte array to hash.
     * @param expected value to compare against.
     */
    public void validateSupportRimHash(final byte[] input, final String expected) {
        String calculatedHash = getHashValue(input, SHA256);
        supportRimValid = calculatedHash.equals(expected);
        if (!supportRimValid) {
            LOGGER.info("Unmatched support RIM hash! Expected: " + expected + ", actual: " + calculatedHash);
        }
    }

    /**
     * This method calculates the digest of a byte array based on the hashing algorithm passed in.
     *
     * @param input byte array.
     * @param sha   hash algorithm.
     * @return String digest.
     */
    private String getHashValue(final byte[] input, final String sha) {
        String resultString = null;
        try {
            MessageDigest md = MessageDigest.getInstance(sha);
            byte[] bytes = md.digest(input);
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < bytes.length; i++) {
                sb.append(Integer.toString((bytes[i] & EIGHT_BIT_MASK)
                        + LEFT_SHIFT, RADIX).substring(1));
            }
            resultString = sb.toString();
        } catch (NoSuchAlgorithmException grex) {
            LOGGER.warn(grex.getMessage());
        }

        return resultString;
    }

    private boolean validateSignedXMLDocument(final DOMValidateContext context) {
        try {
            XMLSignatureFactory sigFactory = XMLSignatureFactory.getInstance("DOM");
            XMLSignature signature = sigFactory.unmarshalXMLSignature(context);
            return signature.validate(context);
        } catch (MarshalException e) {
            LOGGER.warn("Error while unmarshalling XML signature: " + e.getMessage());
        } catch (XMLSignatureException e) {
            LOGGER.warn("Error while validating XML signature: " + e.getMessage());
        }

        return false;
    }

    /**
     * This internal class handles selecting an X509 certificate embedded in a KeyInfo element.
     * It is passed as a parameter to a DOMValidateContext that uses it to validate
     * an XML signature.
     */
    public static class X509KeySelector extends KeySelector {
        /**
         * This method selects a public key for validation.
         * PKs are parsed preferentially from the following elements:
         * - X509Data
         * - KeyValue
         * The parsed PK is then verified based on the provided algorithm before
         * being returned in a KeySelectorResult.
         *
         * @param keyinfo   object containing the cert.
         * @param purpose   purpose.
         * @param algorithm algorithm.
         * @param context   XMLCryptoContext.
         * @return KeySelectorResult holding the PublicKey.
         * @throws KeySelectorException exception.
         */
        public KeySelectorResult select(final KeyInfo keyinfo,
                                        final KeySelector.Purpose purpose,
                                        final AlgorithmMethod algorithm,
                                        final XMLCryptoContext context)
                throws KeySelectorException {
            Iterator keyinfoItr = keyinfo.getContent().iterator();
            while (keyinfoItr.hasNext()) {
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
                                return new ReferenceManifestValidator.X509KeySelector
                                        .RIMKeySelectorResult(publicKey);
                            }
                        }
                    }
                }
            }
            throw new KeySelectorException("No key found!");
        }

        /**
         * This method checks if two strings refer to the same algorithm.
         *
         * @param uri  string 1
         * @param name string 2
         * @return true if equal, false if not
         */
        public boolean areAlgorithmsEqual(final String uri, final String name) {
            return uri.equals(SIGNATURE_ALGORITHM_RSA_SHA256) && name.equalsIgnoreCase("RSA");
        }

        /**
         * This internal class creates a KeySelectorResult from the public key.
         */
        private static class RIMKeySelectorResult implements KeySelectorResult {
            private Key key;

            RIMKeySelectorResult(final Key key) {
                this.key = key;
            }

            public Key getKey() {
                return key;
            }
        }
    }

    /**
     * This method extracts certificate bytes from a string. The bytes are assumed to be
     * PEM format, and a header and footer are concatenated with the input string to
     * facilitate proper parsing.
     *
     * @param pemString the input string
     * @return an X509Certificate created from the string, or null
     * @throws Exception if certificate cannot be successfully parsed
     */
    private X509Certificate parseCertFromPEMString(final String pemString) throws Exception {
        String certificateHeader = "-----BEGIN CERTIFICATE-----";
        String certificateFooter = "-----END CERTIFICATE-----";
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            InputStream inputStream = new ByteArrayInputStream((certificateHeader
                    + System.lineSeparator()
                    + pemString
                    + System.lineSeparator()
                    + certificateFooter).getBytes("UTF-8"));
            return (X509Certificate) factory.generateCertificate(inputStream);
        } catch (CertificateException e) {
            LOGGER.warn("Error creating CertificateFactory instance: " + e.getMessage());
        } catch (UnsupportedEncodingException e) {
            LOGGER.warn("Error while parsing cert from PEM string: " + e.getMessage());
        }

        return null;
    }

    /**
     * This method returns the subjectKeyIdentifier from a given X509Certificate.
     *
     * @param certificate the cert to pull the subjectKeyIdentifier from
     * @return the String representation of the subjectKeyIdentifier
     * @throws IOException
     */
    private String getCertificateSubjectKeyIdentifier(final X509Certificate certificate)
                                                                    throws IOException {
        String decodedValue;
        byte[] extension = certificate.getExtensionValue(Extension.subjectKeyIdentifier.getId());
        if (extension != null && extension.length > 0) {
            decodedValue = JcaX509ExtensionUtils.parseExtensionValue(extension).toString();
        } else {
            decodedValue = " "; //Unlikely that a proper X509Certificate does not have a skid
        }
        return decodedValue.substring(1); //Drop the # at the beginning of the string
    }

    /**
     * This method parses the subject key identifier from the KeyName element of a signature.
     *
     * @param doc
     * @return SKID if found, or an empty string.
     */
    private String getKeyName(final Document doc) {
        NodeList keyName = doc.getElementsByTagName("KeyName");
        if (keyName.getLength() > 0) {
            return keyName.item(0).getTextContent();
        } else {
            return null;
        }
    }

    /**
     * This method validates the Document against the schema.
     *
     * @param doc of the input swidtag.
     * @return document validated against the schema.
     */
    private Document validateSwidtagSchema(final Document doc) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(SCHEMA_PACKAGE);
            unmarshaller = jaxbContext.createUnmarshaller();
            unmarshaller.setSchema(schema);
            unmarshaller.unmarshal(doc);
        } catch (UnmarshalException e) {
            LOGGER.warn("Error validating swidtag file!");
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Input file empty.");
        } catch (JAXBException e) {
            e.printStackTrace();
        }

        return doc;
    }

    /**
     * This method strips all whitespace from an xml file, including indents and spaces
     * added for human-readability.
     *
     * @param source of the input xml.
     * @return Document representation of the xml.
     */
    private Document removeXMLWhitespace(final StreamSource source) throws IOException {
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
