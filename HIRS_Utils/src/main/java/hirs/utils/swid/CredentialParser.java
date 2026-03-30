package hirs.utils.swid;

import lombok.AccessLevel;
import lombok.Getter;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.List;

/**
 * This class parses private key, public key, and certificate for use in their
 * respective java.security objects.
 */
@Getter
public final class CredentialParser {
    private static final String X509 = "X.509";
    private static final String JKS = "JKS";
    private static final String PEM = "PEM";
    private static final String PKCS1_HEADER = "-----BEGIN RSA PRIVATE KEY-----";
    private static final String PKCS1_FOOTER = "-----END RSA PRIVATE KEY-----";
    private static final String PKCS8_HEADER = "-----BEGIN PRIVATE KEY-----";
    private static final String PKCS8_FOOTER = "-----END PRIVATE KEY-----";
    private static final String CERTIFICATE_HEADER = "-----BEGIN CERTIFICATE-----";
    private static final String CERTIFICATE_FOOTER = "-----END CERTIFICATE-----";

    @Getter(AccessLevel.NONE)
    private X509Certificate certificate;

    private PrivateKey privateKey;

    private PublicKey publicKey;

    /**
     * Parses the specified JKS keystore file and extracts the certificate,
     * private key, and public key for the default alias and password.
     *
     * @param jksKeystore the path to the JKS keystore file
     */
    public void parseJKSCredentials(final String jksKeystore) {
        KeyStore.PrivateKeyEntry privateKeyEntry =
                parseKeystorePrivateKey(jksKeystore,
                        SwidTagConstants.DEFAULT_PRIVATE_KEY_ALIAS,
                        SwidTagConstants.DEFAULT_KEYSTORE_PASSWORD);
        certificate = (X509Certificate) privateKeyEntry.getCertificate();
        privateKey = privateKeyEntry.getPrivateKey();
        publicKey = certificate.getPublicKey();
    }

    /**
     * Parses the specified PEM-encoded certificate and private key files,
     * extracting the certificate, private key, and public key.
     *
     * @param certificateFile the path to the PEM-encoded certificate file
     * @param privateKeyFile  the path to the PEM-encoded private key file
     * @throws Exception if parsing fails or the certificate is self-signed
     */
    public void parsePEMCredentials(final String certificateFile, final String privateKeyFile)
            throws Exception {
        certificate = parsePEMCertificates(certificateFile).get(0);
        if (certificate.getIssuerX500Principal().equals(certificate.getSubjectX500Principal())) {
            throw new CertificateException("Signing certificate cannot be self-signed!");
        }
        privateKey = parsePEMPrivateKey(privateKeyFile, "RSA");
        publicKey = certificate.getPublicKey();
    }

    /**
     * This method extracts certificate bytes from a string. The bytes are assumed to be
     * PEM format, and a header and footer are concatenated with the input string to
     * facilitate proper parsing.
     *
     * @param pemString the input string
     * @return an X509Certificate created from the string
     * @throws CertificateException if instantiating the CertificateFactory errors
     */
    public X509Certificate parseCertFromPEMString(final String pemString) throws CertificateException {
        try {
            CertificateFactory factory = CertificateFactory.getInstance(X509);
            InputStream inputStream = new ByteArrayInputStream((CERTIFICATE_HEADER
                    + System.lineSeparator()
                    + pemString
                    + System.lineSeparator()
                    + CERTIFICATE_FOOTER).getBytes(StandardCharsets.UTF_8));
            return (X509Certificate) factory.generateCertificate(inputStream);
        } catch (CertificateException e) {
            throw e;
        }
    }

    /**
     * This method returns the X509Certificate object from a PEM certificate file.
     *
     * @param certificateFile the path to the PEM certificate file
     * @return a list of X509Certificate objects parsed from the file
     */
    public List<X509Certificate> parseCertsFromPEM(final String certificateFile) {
        return parsePEMCertificates(certificateFile);
    }

    /**
     * This method returns the X509Certificate found in a PEM file.
     * Unchecked typcase warnings are suppressed because the CertificateFactory
     * implements X509Certificate objects explicitly.
     *
     * @param filename pem file
     * @return a list containing all X509Certificates extracted
     */
    private List<X509Certificate> parsePEMCertificates(final String filename) {
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
            System.out.println("Error reading from input stream: " + e.getMessage());
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
     *
     * @param filename  the path to the PEM private key file
     * @param algorithm the encryption algorithm for the private key
     * @return the extracted PrivateKey object
     */
    private PrivateKey parsePEMPrivateKey(final String filename, final String algorithm) throws Exception {

        String errorMessage = "";
        File file = new File(filename);
        byte[] key;

        try (FileInputStream fis = new FileInputStream(file);
             DataInputStream dis = new DataInputStream(fis)) {

            key = new byte[(int) file.length()];
            dis.readFully(key);

            String privateKeyStr = new String(key, StandardCharsets.UTF_8);
            if (privateKeyStr.contains(PKCS1_HEADER)) {
                return getPKCS1KeyPair(filename).getPrivate();
            } else if (privateKeyStr.contains(PKCS8_HEADER)) {
                privateKeyStr = privateKeyStr
                        .replace(PKCS8_HEADER, "")
                        .replace(PKCS8_FOOTER, "");

                byte[] decodedKey = Base64.decode(privateKeyStr);
                PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decodedKey);
                KeyFactory keyFactory = KeyFactory.getInstance(algorithm);

                return keyFactory.generatePrivate(spec);
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
        }
        if (!errorMessage.isEmpty()) {
            throw new Exception("Error parsing private key: " + errorMessage);
        }
        return null;
    }

    /**
     * This method reads a PKCS1 keypair from a PEM file.
     *
     * @param filename the path to the PEM file containing the PKCS#1 key pair
     * @return the extracted KeyPair object
     */
    private KeyPair getPKCS1KeyPair(final String filename) throws IOException {
        Security.addProvider(new BouncyCastleProvider());
        try (FileInputStream fis = new FileInputStream(filename);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             PEMParser pemParser = new PEMParser(isr)) {
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
            return converter.getKeyPair((PEMKeyPair) pemParser.readObject());
        }
    }

    /**
     * This method returns the private key from a JKS keystore.
     *
     * @param keystoreFile the path to the JKS keystore file
     * @param alias        the alias of the key entry in the keystore
     * @param password     the password for the keystore and key entry
     * @return the PrivateKeyEntry containing the private key and certificate
     */
    private KeyStore.PrivateKeyEntry parseKeystorePrivateKey(final String keystoreFile,
                                                             final String alias, final String password) {
        KeyStore.PrivateKeyEntry privateKeyEntry = null;
        try (FileInputStream fis = new FileInputStream(keystoreFile)) {
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(fis, password.toCharArray());
            privateKeyEntry = (KeyStore.PrivateKeyEntry) keystore.getEntry(alias,
                    new KeyStore.PasswordProtection(password.toCharArray()));
        } catch (FileNotFoundException e) {
            System.out.println("Cannot locate keystore " + keystoreFile);
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableEntryException
                 | CertificateException | IOException e) {
            e.printStackTrace();
        }
        return privateKeyEntry;
    }

    /**
     * This method returns the authorityInfoAccess from an X509Certificate.
     *
     * @return a formatted string listing the AIA information from the certificate
     * @throws IOException if an I/O error occurs while trying to retrieve the certificate authority info
     *                     access.
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
     *
     * @return the String representation of the subjectKeyIdentifier
     * @throws IOException if an I/O error occurs while retrieving the certificate subject key identifier.
     */
    public String getCertificateSubjectKeyIdentifier() throws IOException {
        byte[] extension = certificate.getExtensionValue(Extension.subjectKeyIdentifier.getId());
        if (extension == null || extension.length == 0) {
            throw new IOException("Subject Key Identifier extension not found in certificate.");
        }
        String decodedValue = JcaX509ExtensionUtils.parseExtensionValue(extension).toString();
        return decodedValue.substring(1); // Drop the # at the beginning of the string
    }

    /**
     * This method returns the subjectKeyIdentifier from a given X509Certificate.
     *
     * @param certificate the cert to pull the subjectKeyIdentifier from
     * @return the String representation of the subjectKeyIdentifier
     * @throws IOException if an I/O error occurs while retrieving the certificate subject key identifier.
     */
    public String getCertificateSubjectKeyIdentifier(final X509Certificate certificate) throws IOException {
        byte[] extension = certificate.getExtensionValue(Extension.subjectKeyIdentifier.getId());
        if (extension == null || extension.length == 0) {
            throw new IOException("Subject Key Identifier extension not found in certificate.");
        }
        String decodedValue = JcaX509ExtensionUtils.parseExtensionValue(extension).toString();
        return decodedValue.substring(1); // Drop the # at the beginning of the string
    }

    /**
     * Returns a copy of the stored X509Certificate.
     *
     * @return cloned X509Certificate
     */
    public X509Certificate getCertificate() {
        try {
            return (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(certificate.getEncoded()));
        } catch (CertificateException e) {
            throw new RuntimeException("Failed to clone certificate", e);
        }
    }

    /**
     * Sets the stored certificate by creating a copy of the provided X509Certificate.
     *
     * @param cert the certificate to store
     */
    public void setCertificate(final X509Certificate cert) {
        if (cert == null) {
            this.certificate = null;
            return;
        }
        try {
            this.certificate = (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(cert.getEncoded()));
        } catch (CertificateException e) {
            throw new IllegalArgumentException("Failed to copy certificate", e);
        }
    }
}
