package hirs.swid;

import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.DecoderException;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.List;

/**
 * This class parses private key, public key, and certificates for use in
 * their respective java.security objects.
 */
public class CredentialParser {
    private static final String X509 = "X.509";
    private static final String DEFAULT_ALGORITHM = "RSA";
    private static final String PKCS1_HEADER = "-----BEGIN RSA PRIVATE KEY-----";
    private static final String PKCS1_FOOTER = "-----END RSA PRIVATE KEY-----";
    private static final String PKCS8_HEADER = "-----BEGIN PRIVATE KEY-----";
    private static final String PKCS8_FOOTER = "-----END PRIVATE KEY-----";
    private static final String CERTIFICATE_HEADER = "-----BEGIN CERTIFICATE-----";
    private static final String CERTIFICATE_FOOTER = "-----END CERTIFICATE-----";
    private X509Certificate certificate;
    private PrivateKey privateKey;
    private PublicKey publicKey;

    public X509Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(X509Certificate certificate) {
        this.certificate = certificate;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * This method parses the X509 signing cert, private key, and public key from
     * a JKS truststore.
     * @param jksKeystore the truststore file
     */
    public void parseJKSCredentials(String jksKeystore, String alias, String password) {
        KeyStore.PrivateKeyEntry privateKeyEntry =
                parseKeystorePrivateKey(jksKeystore, alias, password);
        certificate = (X509Certificate) privateKeyEntry.getCertificate();
        privateKey = privateKeyEntry.getPrivateKey();
        publicKey = certificate.getPublicKey();
    }

    /**
     * Convenience method for parsing the cert and keys of the default JKS.
     */
    public void parseDefaultCredentials() {
        parseJKSCredentials(SwidTagConstants.DEFAULT_KEYSTORE_FILE,
                            SwidTagConstants.DEFAULT_PRIVATE_KEY_ALIAS,
                            SwidTagConstants.DEFAULT_KEYSTORE_PASSWORD);
    }

    /**
     * This method returns the X509Certificate object from a PEM truststore.
     * @param truststore the PEM truststore
     * @return a list of X509 certs
     */
    public List<X509Certificate> parseCertsFromPEM(String truststore) {
        return parsePEMCertificates(truststore);
    }

    public void parsePEMCredentials(List<X509Certificate> truststore,
                                    String privateKeyFile)
                                    throws Exception {
        byte[] challengeString = new byte[15];
        for (X509Certificate cert : truststore) {
            certificate = cert;
            privateKey = parsePEMPrivateKey(privateKeyFile, DEFAULT_ALGORITHM);
            publicKey = certificate.getPublicKey();
            SecureRandom.getInstanceStrong().nextBytes(challengeString);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(challengeString);
            byte[] signedChallenge = signature.sign();
            signature.initVerify(publicKey);
            signature.update(challengeString);
            if (signature.verify(signedChallenge)) {
                System.out.println("Matched private key to truststore certificate");
                break;
            } else {
                publicKey = null;
            }
        }
    }

    public void parsePEMCredentials(String certificateFile, String privateKeyFile)
            throws Exception {
        certificate = parsePEMCertificates(certificateFile).get(0);
        if (certificate.getIssuerX500Principal().equals(certificate.getSubjectX500Principal())) {
            throw new CertificateException("Signing certificate cannot be self-signed!");
        }
        privateKey = parsePEMPrivateKey(privateKeyFile, DEFAULT_ALGORITHM);
        publicKey = certificate.getPublicKey();
    }

    /**
     * This method extracts certificate bytes from a string. The bytes are assumed to be
     * PEM format, and a header and footer are concatenated with the input string to
     * facilitate proper parsing.
     * @param pemString the input string
     * @return an X509Certificate created from the string
     * @throws CertificateException if instantiating the CertificateFactory errors
     */
    public X509Certificate parseCertFromPEMString(String pemString) throws CertificateException {
        try {
            CertificateFactory factory = CertificateFactory.getInstance(X509);
            InputStream inputStream = new ByteArrayInputStream((CERTIFICATE_HEADER
                                                                + System.lineSeparator()
                                                                + pemString
                                                                + System.lineSeparator()
                                                                + CERTIFICATE_FOOTER).getBytes());
            return (X509Certificate) factory.generateCertificate(inputStream);
        } catch (CertificateException e) {
            throw e;
        }
    }

    /**
     * This method returns the X509Certificate found in a PEM file.
     * Unchecked typcase warnings are suppressed because the CertificateFactory
     * implements X509Certificate objects explicitly.
     * @param filename pem file
     * @return a list containing all X509Certificates extracted
     */
    @SuppressWarnings("unchecked")
    private List<X509Certificate> parsePEMCertificates(String filename) {
        List<X509Certificate> certificates = null;
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        try {
            fis = new FileInputStream(filename);
            bis = new BufferedInputStream(fis);
            CertificateFactory certificateFactory = CertificateFactory.getInstance(X509);

            while (bis.available() > 0) {
                certificates =
                        (List<X509Certificate>) certificateFactory.generateCertificates(bis);
            }

            if (certificates.size() < 1) {
                System.out.println("ERROR: No certificates parsed from " + filename);
            }
            bis.close();
        } catch (CertificateException e) {
            System.out.println("Error in certificate factory: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error reading from input stream: " + filename);
            e.printStackTrace();
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
                if (bis != null) {
                    bis.close();
                }
            } catch (IOException e) {
                System.out.println("Error closing input stream: " + e.getMessage());
            }
        }

        return certificates;
    }

    /**
     * This method extracts the private key from a PEM file.
     * Both PKCS1 and PKCS8 formats are handled.
     * Algorithm argument is present to allow handling of multiple encryption algorithms,
     * but for now it is always RSA.
     * @param filename
     * @return
     */
    private PrivateKey parsePEMPrivateKey(String filename, String algorithm) throws Exception {
        PrivateKey privateKey = null;
        FileInputStream fis = null;
        DataInputStream dis = null;
        String errorMessage = "";
        try {
            File file = new File(filename);
            fis = new FileInputStream(file);
            dis = new DataInputStream(fis);
            byte[] key = new byte[(int) file.length()];
            dis.readFully(key);
            dis.close();

            String privateKeyStr = new String(key);
            if (privateKeyStr.contains(PKCS1_HEADER)) {
                privateKey = getPKCS1KeyPair(filename).getPrivate();
            } else if (privateKeyStr.contains(PKCS8_HEADER)) {
                privateKeyStr = privateKeyStr.replace(PKCS8_HEADER, "");
                privateKeyStr = privateKeyStr.replace(PKCS8_FOOTER, "");

                byte[] decodedKey = Base64.decode(privateKeyStr);
                PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decodedKey);
                KeyFactory keyFactory = KeyFactory.getInstance(algorithm);

                privateKey = keyFactory.generatePrivate(spec);
            }
        } catch (FileNotFoundException e) {
            errorMessage += "Unable to locate private key file: " + filename;
        } catch (DecoderException e) {
            errorMessage += "Failed to parse uploaded pem file: " + e.getMessage();
        } catch (NoSuchAlgorithmException e) {
            errorMessage += "Unable to instantiate KeyFactory with algorithm: " + algorithm;
        } catch (IOException e) {
            errorMessage += "IOException: " + e.getMessage();
        } catch (InvalidKeySpecException e) {
            errorMessage += "Error instantiating PKCS8EncodedKeySpec object: " + e.getMessage();
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
                if (dis != null) {
                    dis.close();
                }
            } catch (IOException e) {
                errorMessage += "Error closing input stream: " + e.getMessage();
            }
            if (!errorMessage.isEmpty()) {
                throw new Exception("Error parsing private key: " + errorMessage);
            }
        }

        return privateKey;
    }

    /**
     * This method reads a PKCS1 keypair from a PEM file.
     * @param filename
     * @return
     */
    private KeyPair getPKCS1KeyPair(String filename) throws IOException {
        Security.addProvider(new BouncyCastleProvider());
        PEMParser pemParser = new PEMParser(new FileReader(filename));
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
        KeyPair keyPair = converter.getKeyPair((PEMKeyPair) pemParser.readObject());

        return keyPair;
    }

    /**
     * This method returns the private key from a JKS keystore.
     * @param keystoreFile
     * @param alias
     * @param password
     * @return KeyStore.PrivateKeyEntry
     */
    private KeyStore.PrivateKeyEntry parseKeystorePrivateKey(String keystoreFile,
                                                             String alias,
                                                             String password) {
        KeyStore keystore = null;
        KeyStore.PrivateKeyEntry privateKey = null;
        try {
            keystore = KeyStore.getInstance("JKS");
            keystore.load(new FileInputStream(keystoreFile), password.toCharArray());
            privateKey = (KeyStore.PrivateKeyEntry) keystore.getEntry(alias,
                    new KeyStore.PasswordProtection(password.toCharArray()));
        } catch (FileNotFoundException e) {
            System.out.println("Cannot locate keystore " + keystoreFile);
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableEntryException |
                CertificateException | IOException e) {
            e.printStackTrace();
        }

        return privateKey;
    }

    /**
     * This method returns the authorityInfoAccess from an X509Certificate.
     * @return
     * @throws IOException
     */
    public String getCertificateAuthorityInfoAccess() throws IOException {
        StringBuilder sb = new StringBuilder("Authority Info Access:\n");
        byte[] extension = certificate.getExtensionValue(Extension.authorityInfoAccess.getId());
        if (extension != null && extension.length > 0) {
            AuthorityInformationAccess aia = AuthorityInformationAccess.getInstance(
                                    JcaX509ExtensionUtils.parseExtensionValue(extension));
            for (AccessDescription ad : aia.getAccessDescriptions()) {
                if (ad.getAccessMethod().toString().equals(SwidTagConstants.CA_ISSUERS)) {
                    sb.append("CA issuers - ");
                }
                if (ad.getAccessLocation().getTagNo() == GeneralName.uniformResourceIdentifier) {
                    sb.append("URI:" + ad.getAccessLocation().getName().toString() + "\n");
                }
            }
        }
        return sb.toString();
    }

    /**
     * This method returns the subjectKeyIdentifier from the local X509Certificate.
     * @return the String representation of the subjectKeyIdentifier
     * @throws IOException
     */
    public String getCertificateSubjectKeyIdentifier() throws IOException {
        String decodedValue = null;
        byte[] extension = certificate.getExtensionValue(Extension.subjectKeyIdentifier.getId());
        if (extension != null && extension.length > 0) {
            decodedValue = JcaX509ExtensionUtils.parseExtensionValue(extension).toString();
        }
        return decodedValue.substring(1);//Drop the # at the beginning of the string
    }

    /**
     * This method returns the subjectKeyIdentifier from a given X509Certificate.
     * @param certificate the cert to pull the subjectKeyIdentifier from
     * @return the String representation of the subjectKeyIdentifier
     * @throws IOException
     */
    public String getCertificateSubjectKeyIdentifier(X509Certificate certificate) throws IOException {
        String decodedValue = null;
        byte[] extension = certificate.getExtensionValue(Extension.subjectKeyIdentifier.getId());
        if (extension != null && extension.length > 0) {
            decodedValue = JcaX509ExtensionUtils.parseExtensionValue(extension).toString();
        }
        return decodedValue.substring(1);//Drop the # at the beginning of the string
    }
}
