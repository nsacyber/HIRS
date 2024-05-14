package hirs.utils.rim;

import hirs.utils.swid.SwidTagConstants;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.UnmarshalException;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.log4j.Log4j2;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.jcajce.provider.asymmetric.X509;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.security.auth.x500.X500Principal;
import javax.xml.XMLConstants;
import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
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
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * This class handles validation functions of RIM files.
 * Currently supports validation of support RIM hashes and
 * base RIM signatures.
 */
@Log4j2
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

    private Document rim;
    private Unmarshaller unmarshaller;
    private PublicKey publicKey;
    private Schema schema;
    private String subjectKeyIdentifier;
    private String rimEventLog;
    private String trustStoreFile;
    private List<X509Certificate> trustStore;
    private boolean signatureValid, supportRimValid;

    /**
     * Setter for the RIM to be validated.  The ReferenceManifest object is converted into a
     * Document for processing.
     *
     * @param rimBytes ReferenceManifest object bytes
     */
    public void setRim(final byte[] rimBytes) {
        try {
            Document doc = validateSwidtagSchema(removeXMLWhitespace(new StreamSource(
                    new ByteArrayInputStream(rimBytes))));
            this.rim = doc;
        } catch (IOException e) {
            log.error("Error while unmarshalling rim bytes: " + e.getMessage());
        }
    }

    /**
     * Setter for the swidtag XML to be validated.  The XML is passed in via a filepath
     * and converted into a Document for processing.
     *
     * @param path String filepath
     */
    public void setRim(final String path) {
        File swidtagFile = new File(path);
        try {
            Document doc = validateSwidtagSchema(removeXMLWhitespace(new StreamSource(swidtagFile)));
            this.rim = doc;
        } catch (IOException e) {
            log.error("Error while unmarshalling rim bytes: " + e.getMessage());
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
     * Setter for the truststore file path.
     * @param trustStoreFile the file path to reference
     */
    public void setTrustStoreFile(String trustStoreFile) {
        this.trustStoreFile = trustStoreFile;
    }

    /**
     * Setter for truststore
     * @param trustStore the List of X509Certificates
     */
    public void setTrustStore(List<X509Certificate> trustStore) {
        this.trustStore = trustStore;
    }

    /**
     * Setter for rimel file path.
     * @param rimEventLog the rimel file
     */
    public void setRimEventLog(String rimEventLog) {
        this.rimEventLog = rimEventLog;
    }

    /**
     * This default constructor creates the Schema object from SCHEMA_URL immediately to save
     * time during validation calls later.
     */
    public ReferenceManifestValidator() {
        try {
            Security.addProvider(new BouncyCastleProvider());
            InputStream is = ReferenceManifestValidator.class
                    .getClassLoader().getResourceAsStream(SCHEMA_URL);
            SchemaFactory schemaFactory = SchemaFactory.newInstance(SCHEMA_LANGUAGE);
            schema = schemaFactory.newSchema(new StreamSource(is));
            rim = null;
            signatureValid = false;
            supportRimValid = false;
            publicKey = null;
            trustStoreFile = null;
            trustStore = null;
            subjectKeyIdentifier = "(not found)";
        } catch (SAXException e) {
            log.warn("Error setting schema for validation!");
        }
    }

    /**
     * This method attempts to validate the signature element of the instance's RIM
     * using a given cert.  The cert is compared to either the RIM's embedded certificate
     * or the RIM's subject key identifier.  If the cert is matched then validation proceeds,
     * otherwise validation ends.
     *
     * @param publicKey public key from the CA credential
     * @param subjectKeyIdString string version of the subjet key id of the CA credential
     * @param encodedPublicKey the encoded public key
     * @return true if the signature element is validated, false otherwise
     */
    @SuppressWarnings("magicnumber")
    public boolean validateXmlSignature(final PublicKey publicKey,
                                        final String subjectKeyIdString,
                                        final byte[] encodedPublicKey) {
        DOMValidateContext context = null;
        try {
            NodeList nodes = rim.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
            if (nodes.getLength() == 0) {
                log.error("Cannot validate RIM, signature element not found!");
                return false;
            }
            if (trustStoreFile != null && !trustStoreFile.isEmpty()) {
                trustStore = parseCertificatesFromPem(trustStoreFile);
            }
            NodeList certElement = rim.getElementsByTagName("X509Certificate");
            if (certElement.getLength() > 0) {
                X509Certificate embeddedCert = parseCertFromPEMString(
                        certElement.item(0).getTextContent());
                if (embeddedCert != null) {
                    if (isCertChainValid(embeddedCert)) {
                        context = new DOMValidateContext(new X509KeySelector(), nodes.item(0));
                    }
                }
            } else {
                subjectKeyIdentifier = getKeyName(rim);
                if (subjectKeyIdentifier.equals(subjectKeyIdString)) {
                    context = new DOMValidateContext(publicKey,
                            nodes.item(0));
                }
            }
            if (context != null) {
                this.publicKey = publicKey;
                signatureValid = validateSignedXMLDocument(context);
                return signatureValid;
            }
        } catch (IOException e) {
            log.warn("Error while parsing certificate data: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * This method validates the rim with a public key cert.
     * @param signingCertPath to the public key certificate used to sign the rim
     * @return true if both the file element and signature are valid, false otherwise
     */
    public boolean validateRim(String signingCertPath) {
        Element fileElement = (Element) rim.getElementsByTagNameNS(
                SwidTagConstants.SWIDTAG_NAMESPACE, "File").item(0);
        X509Certificate signingCert = parseCertificatesFromPem(signingCertPath).get(0);
        if (signingCert == null) {
            return failWithError("Unable to parse the signing cert from " + signingCertPath);
        }
        String subjectKeyIdentifier = "";
        try {
            subjectKeyIdentifier = getCertificateSubjectKeyIdentifier(signingCert);
        } catch (IOException e) {
            return failWithError("Error while parsing SKID: " + e.getMessage());
        }

        boolean isSignatureValid = validateXmlSignature(signingCert.getPublicKey(),
                                                        subjectKeyIdentifier,
                                                        signingCert.getPublicKey().getEncoded());
        return isSignatureValid && validateFile(fileElement);
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
            log.info("Unmatched support RIM hash! Expected: " + expected
                    + ", actual: " + calculatedHash);
        }
    }

    /**
     * This method validates a hirs.swid.xjc.File from an indirect payload
     */
    private boolean validateFile(final Element file) {
        String filepath;
        if (!rimEventLog.isEmpty()) {
            filepath = rimEventLog;
        } else {
            filepath = file.getAttribute(SwidTagConstants.NAME);
        }
        if (getHashValue(filepath, "SHA256").equals(
                file.getAttribute(SwidTagConstants._SHA256_HASH.getPrefix() + ":" +
                        SwidTagConstants._SHA256_HASH.getLocalPart()))) {
            log.info("Support RIM hash verified for " + filepath);
            return true;
        } else {
            return failWithError("Support RIM hash does not match Base RIM!");
        }
    }

    /**
     * This method pulls the signing certificate from the truststore based on the
     * SKID parsed from this instance's swidtag XML.
     *
     * @return X509Certificate signing cert
     */
    private X509Certificate getCertFromTruststore() throws IOException {
        String subjectKeyIdentifier = getKeyName(rim);
        for (X509Certificate trustedCert : trustStore) {
            String trustedSubjectKeyIdentifier = getCertificateSubjectKeyIdentifier(trustedCert);
            if (subjectKeyIdentifier.equals(trustedSubjectKeyIdentifier)) {
                return trustedCert;
            }
        }

        return null;
    }

    /**
     * This method calculates the digest of the file at filepath based on algorithm sha
     *
     * @param filepath the file to hash
     * @param sha the algorithm to use
     * @return String digest
     */
    private String getHashValue(final String filepath, final String sha) {
        try {
            MessageDigest md = MessageDigest.getInstance(sha);
            byte[] bytes = Files.readAllBytes(Paths.get(filepath));
            return getHashValue(bytes, sha);
        } catch (NoSuchAlgorithmException e) {
            log.warn(e.getMessage());
        } catch (IOException e) {
            log.warn("Error reading " + filepath + " for hashing: " + e.getMessage());
        }

        return null;
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
            log.warn(grex.getMessage());
        }

        return resultString;
    }

    private boolean validateSignedXMLDocument(final DOMValidateContext context) {
        try {
            XMLSignatureFactory sigFactory = XMLSignatureFactory.getInstance("DOM");
            XMLSignature signature = sigFactory.unmarshalXMLSignature(context);
            boolean isValid = signature.validate(context);
            if (isValid) {
                return true;
            } else {
                whySignatureInvalid(signature, context);
            }
        } catch (MarshalException e) {
            log.warn("Error while unmarshalling XML signature: " + e.getMessage());
        } catch (XMLSignatureException e) {
            log.warn("Error while validating XML signature: " + e.getMessage());
        }

        return false;
    }

    /**
     * This method attempts to gather information on why a signature failed to validate.
     * The <SignatureValue> and each <Reference> is cryptographically validated and the
     * results are logged.
     *
     * @param signature the signature that failed to validate
     * @param context the context used for validation
     * @throws XMLSignatureException
     */
    private void whySignatureInvalid(final XMLSignature signature, final DOMValidateContext context)
        throws XMLSignatureException{
        log.error("Verifying xml signature:");
        boolean cryptoValidity = signature.getSignatureValue().validate(context);
        if (cryptoValidity) {
            log.error("Signature value is valid.");
        } else {
            log.error("Signature value is invalid!");
        }
        Iterator itr = signature.getSignedInfo().getReferences().iterator();
        while (itr.hasNext()) {
            Reference reference = (Reference) itr.next();
            boolean refValidity = reference.validate(context);
            String refUri = reference.getURI();
            if (refUri.isEmpty()) {
                refUri = "whole document";
            }
            if (refValidity) {
                log.error("Reference for " + refUri + " is valid.");
            } else {
                log.error("Reference for " + refUri + " is invalid!");
            }
        }
    }

    /**
     * This method validates the cert chain for a given certificate. The truststore is iterated
     * over until a root CA is found, otherwise an error is returned.
     * @param cert the certificate at the start of the chain
     * @return true if the chain is valid
     * @throws Exception if a valid chain is not found in the truststore
     */
    private boolean isCertChainValid(final X509Certificate cert)
            throws Exception {
        if (cert == null || trustStore == null) {
            throw new Exception("Null certificate or truststore received");
        } else if (trustStore.size() == 0) {
            throw new Exception("Truststore is empty");
        }

        final String INT_CA_ERROR = "Intermediate CA found, searching for root CA";
        String errorMessage = "";
        X509Certificate chainCert = cert;
        boolean isChainCertValid;
        do {
            isChainCertValid = false;
            log.error("Validating " + chainCert.getSubjectX500Principal().getName());
            for (X509Certificate trustedCert : trustStore) {
                boolean isIssuer = areYouMyIssuer(chainCert, trustedCert);
                boolean isSigner = areYouMySigner(chainCert, trustedCert);
                if (isIssuer && isSigner) {
                    if (isSelfSigned(trustedCert)) {
                        log.info("Root CA found.");
                        return true;
                    } else {
                        chainCert = trustedCert;
                        isChainCertValid = true;
                        log.info("Intermediate CA found.");
                        break;
                    }
                } else {
                    if (!isIssuer) {
                        errorMessage = "Issuer cert not found";
                    } else if (!isSigner) {
                        errorMessage = "Signing cert not found";
                    }
                }
            }
        } while (isChainCertValid);

        log.error("CA chain validation failed to validate "
                + chainCert.getSubjectX500Principal().getName());
        return false;
    }

    /**
     * This method checks if cert's issuerDN matches issuer's subjectDN.
     * @param cert the signed certificate
     * @param issuer the signing certificate
     * @return true if they match, false if not
     * @throws Exception if either argument is null
     */
    private boolean areYouMyIssuer(final X509Certificate cert, final X509Certificate issuer)
            throws Exception {
        if (cert == null || issuer == null) {
            throw new Exception("Cannot verify issuer, null certificate received");
        }
        X500Principal issuerDN = new X500Principal(cert.getIssuerX500Principal().getName());
        return issuer.getSubjectX500Principal().equals(issuerDN);
    }

    /**
     * This method checks if cert's signature matches signer's public key.
     * @param cert the signed certificate
     * @param signer the signing certificate
     * @return true if they match
     * @throws Exception if an error occurs or there is no match
     */
    private boolean areYouMySigner(final X509Certificate cert, final X509Certificate signer)
            throws Exception {
        if (cert == null || signer == null) {
            throw new Exception("Cannot verify signature, null certificate received");
        }
        try {
            cert.verify(signer.getPublicKey(), BouncyCastleProvider.PROVIDER_NAME);
            return true;
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("Signing algorithm in signing cert not supported");
        } catch (InvalidKeyException e) {
            throw new Exception("Signing certificate key does not match signature");
        } catch (NoSuchProviderException e) {
            throw new Exception("Error with BouncyCastleProvider: " + e.getMessage());
        } catch (SignatureException e) {
            String error = "Error with signature: " + e.getMessage()
                    + System.lineSeparator()
                    + "Certificate needed for verification is missing: "
                    + signer.getSubjectX500Principal().getName();
            log.error(error);
        } catch (CertificateException e) {
            throw new Exception("Encoding error: " + e.getMessage());
        }

        return false;
    }

    /**
     * This method checks if a given certificate is self signed or not.
     * @param cert the cert to check
     * @return true if self signed, false if not
     */
    private boolean isSelfSigned(final X509Certificate cert) {
        return cert.getIssuerX500Principal().equals(cert.getSubjectX500Principal());
    }

    /**
     * This internal class handles selecting an X509 certificate embedded in a KeyInfo element.
     * It is passed as a parameter to a DOMValidateContext that uses it to validate
     * an XML signature.
     */
    public class X509KeySelector extends KeySelector {
        PublicKey publicKey;
        X509Certificate signingCert;
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
                            X509Certificate embeddedCert = (X509Certificate) object;
                            try {
                                if (isCertChainValid(embeddedCert)) {
                                    publicKey = ((X509Certificate) embeddedCert).getPublicKey();
                                    signingCert = embeddedCert;
                                    log.info("Certificate chain valid.");
                                }
                            } catch (Exception e) {
                                log.error("Certificate chain invalid: " + e.getMessage());
                            }
                        }
                    }
                } else if (element instanceof KeyValue) {
                    try {
                        PublicKey pk = ((KeyValue) element).getPublicKey();
                        if (isPublicKeyTrusted(pk)) {
                            publicKey = pk;
                            try {
                                if (isCertChainValid(signingCert)) {
                                    log.info("Certificate chain valid.");
                                } else {
                                    log.error("Certificate chain invalid.");
                                }
                            } catch (Exception e) {
                                log.error("Certificate chain invalid: " + e.getMessage());
                            }
                        }
                    } catch (KeyException e) {
                        log.error("Unable to convert KeyValue data to PK.");
                    }
                }
                if (publicKey != null) {
                    if (areAlgorithmsEqual(algorithm.getAlgorithm(), publicKey.getAlgorithm())) {
                        return new ReferenceManifestValidator.X509KeySelector
                                        .RIMKeySelectorResult(publicKey);
                    }
                }
            }
            throw new KeySelectorException("No key found!");
        }

        /**
         * This method checks that the signature and public key algorithms match.
         * @param uri to match the signature algorithm
         * @param name to match the public key algorithm
         * @return true if both match, false otherwise
         */
        public boolean areAlgorithmsEqual(String uri, String name) {
            return uri.equals(SwidTagConstants.SIGNATURE_ALGORITHM_RSA_SHA256)
                    && name.equalsIgnoreCase("RSA");
        }

        /**
         * This method compares a public key against those in the truststore.
         * @param pk a public key
         * @return true if pk is found in the trust store, false otherwise
         */
        private boolean isPublicKeyTrusted(final PublicKey pk) {
            for (X509Certificate trustedCert : trustStore) {
                if (Arrays.equals(trustedCert.getPublicKey().getEncoded(),
                        pk.getEncoded())) {
                    signingCert = trustedCert;
                    return true;
                }
            }

            return false;
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
            log.warn("Error creating CertificateFactory instance: " + e.getMessage());
        } catch (UnsupportedEncodingException e) {
            log.warn("Error while parsing cert from PEM string: " + e.getMessage());
        }

        return null;
    }

    /**
     * This method returns the X509Certificates found in a PEM file.
     * Unchecked type case warnings are suppressed because the CertificateFactory
     * implements X509Certificate objects explicitly.
     * @param filename pem file
     * @return a list containing all X509Certificates extracted
     */
    @SuppressWarnings("unchecked")
    private List<X509Certificate> parseCertificatesFromPem(String filename) {
        List<X509Certificate> certificates = null;
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        try {
            fis = new FileInputStream(filename);
            bis = new BufferedInputStream(fis);
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

            while (bis.available() > 0) {
                certificates =
                        (List<X509Certificate>) certificateFactory.generateCertificates(bis);
            }

            if (certificates.size() < 1) {
                System.out.println("ERROR: No certificates parsed from " + filename);
            }
            bis.close();
        } catch (CertificateException e) {
            log.error("Error in certificate factory: " + e.getMessage());
        } catch (IOException e) {
            log.error("Error reading from input stream: " + e.getMessage());
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
                if (bis != null) {
                    bis.close();
                }
            } catch (IOException e) {
                log.warn("Error closing input stream: " + e.getMessage());
            }
        }

        return certificates;
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
            log.warn("Error validating swidtag file!");
        } catch (IllegalArgumentException e) {
            log.warn("Input file empty.");
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
            log.warn("Error configuring transformer!");
            e.printStackTrace();
        } catch (TransformerException e) {
            log.warn("Error transforming input!");
            e.printStackTrace();
        }

        return doc;
    }

    /**
     * This method logs an error message and returns a false to signal failed validation.
     * @param errorMessage String description of what went wrong
     * @return false to represent failed validation
     */
    private boolean failWithError(String errorMessage) {
        log.error(errorMessage);
        return false;
    }
}
