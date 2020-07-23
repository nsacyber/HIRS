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

import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

/**
 * This class parses private key, public key, and certificate for use in their respective java.security objects.
 */
public class CredentialParser {
    private static final String X509 = "X.509";
    private static final String JKS = "JKS";
    private static final String PEM = "PEM";
    private static final String PKCS1_HEADER = "-----BEGIN RSA PRIVATE KEY-----";
    private static final String PKCS1_FOOTER = "-----END RSA PRIVATE KEY-----";
    private static final String PKCS8_HEADER = "-----BEGIN PRIVATE KEY-----";
    private static final String PKCS8_FOOTER = "-----END PRIVATE KEY-----";
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

    public void parseJKSCredentials() {
        KeyStore.PrivateKeyEntry privateKeyEntry =
                parseKeystorePrivateKey(SwidTagConstants.DEFAULT_KEYSTORE_PATH,
                        SwidTagConstants.DEFAULT_PRIVATE_KEY_ALIAS,
                        SwidTagConstants.DEFAULT_KEYSTORE_PASSWORD);
        certificate = (X509Certificate) privateKeyEntry.getCertificate();
        privateKey = privateKeyEntry.getPrivateKey();
        publicKey = certificate.getPublicKey();
    }

    public void parsePEMCredentials(String certificateFile, String privateKeyFile) throws CertificateException, FileNotFoundException {
        certificate = parsePEMCertificate(certificateFile);
        if (certificate.getIssuerX500Principal().equals(certificate.getSubjectX500Principal())) {
            throw new CertificateException("Signing certificate cannot be self-signed!");
        }
        privateKey = parsePEMPrivateKey(privateKeyFile, "RSA");
        publicKey = certificate.getPublicKey();
    }

    /**
     * This method returns the X509Certificate object from a PEM certificate file.
     * @param certificateFile
     * @return
     * @throws FileNotFoundException
     */
    public X509Certificate parseCertFromPEM(String certificateFile) throws FileNotFoundException {
        return parsePEMCertificate(certificateFile);
    }

    /**
     * This method returns the X509Certificate found in a PEM file.
     * @param filename
     * @return
     * @throws FileNotFoundException
     */
    private X509Certificate parsePEMCertificate(String filename) throws FileNotFoundException {
        X509Certificate certificate = null;
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        try {
            fis = new FileInputStream(filename);
            bis = new BufferedInputStream(fis);
            CertificateFactory certificateFactory = CertificateFactory.getInstance(X509);

            while (bis.available() > 0) {
                certificate = (X509Certificate) certificateFactory.generateCertificate(bis);
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

        return certificate;
    }

    /**
     * This method extracts the private key from a PEM file.
     * Both PKCS1 and PKCS8 formats are handled.
     * Algorithm argument is present to allow handling of multiple encryption algorithms,
     * but for now it is always RSA.
     * @param filename
     * @return
     */
    private PrivateKey parsePEMPrivateKey(String filename, String algorithm) {
        PrivateKey privateKey = null;
        FileInputStream fis = null;
        DataInputStream dis = null;
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
            System.out.println("Unable to locate private key file: " + filename);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Unable to instantiate KeyFactory with algorithm: " + algorithm);
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } catch (InvalidKeySpecException e) {
            System.out.println("Error instantiating PKCS8EncodedKeySpec object: " + e.getMessage());
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
                if (dis != null) {
                    dis.close();
                }
            } catch (IOException e) {
                System.out.println("Error closing input stream: " + e.getMessage());
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
    private KeyStore.PrivateKeyEntry parseKeystorePrivateKey(String keystoreFile, String alias, String password) {
        KeyStore keystore = null;
        KeyStore.PrivateKeyEntry privateKey = null;
        try {
            keystore = KeyStore.getInstance("JKS");
            keystore.load(new FileInputStream(keystoreFile), password.toCharArray());
            privateKey = (KeyStore.PrivateKeyEntry) keystore.getEntry(alias,
                    new KeyStore.PasswordProtection(password.toCharArray()));
        } catch (FileNotFoundException e) {
            System.out.println("Cannot locate keystore " + keystoreFile);
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableEntryException | CertificateException | IOException e) {
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
     * This method returns the subjectKeyIdentifier from an X509Certificate.
     * @return
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
}
