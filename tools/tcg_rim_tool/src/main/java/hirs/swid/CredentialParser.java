package hirs.swid;

import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.util.encoders.Base64;

import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public class CredentialParser {
    private static final String X509 = "X.509";
    private static final String JKS = "JKS";
    private static final String PEM = "PEM";
    private X509Certificate certificate;
    private PrivateKey privateKey;
    private PublicKey publicKey;

    public X509Certificate getCertificate() {
        return certificate;
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

    public void parsePEMCredentials(String certificateFile, String privateKeyFile) throws FileNotFoundException {
        certificate = parsePEMCertificate(certificateFile);

        /*User input on algorithm???*/
        privateKey = parsePEMPrivateKey(privateKeyFile, "RSA");

        publicKey = certificate.getPublicKey();
    }

    /**
     * This method returns the X509Certificate found in a PEM file.
     * @param filename
     * @return
     * @throws FileNotFoundException
     */
    private X509Certificate parsePEMCertificate(String filename) throws FileNotFoundException {
        X509Certificate certificate = null;
        try {
            FileInputStream fis = new FileInputStream(filename);
            BufferedInputStream bis = new BufferedInputStream(fis);
            CertificateFactory certificateFactory = CertificateFactory.getInstance(X509);

            while (bis.available() > 0) {
                certificate = (X509Certificate) certificateFactory.generateCertificate(bis);
            }


        } catch (CertificateException e) {
            System.out.println("Error in certificate factory: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error reading from input stream: " + e.getMessage());
        }

        return certificate;
    }

    /**
     * This method extracts the private key from a PEM file.
     * @param filename
     * @return
     */
    private PrivateKey parsePEMPrivateKey(String filename, String algorithm) {
        PrivateKey privateKey = null;
        try {
            File file = new File(filename);
            FileInputStream fis = new FileInputStream(file);
            DataInputStream dis = new DataInputStream(fis);
            byte[] key = new byte[(int) file.length()];
            dis.readFully(key);
            dis.close();

            String privateKeyStr = new String(key);
            privateKeyStr = privateKeyStr.replace("-----BEGIN PRIVATE KEY-----\n", "");
            privateKeyStr = privateKeyStr.replace("-----END PRIVATE KEY-----", "");

            Base64 base64 = new Base64();
            byte[] decodedKey = base64.decode(privateKeyStr);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decodedKey);
            KeyFactory keyFactory = KeyFactory.getInstance(algorithm);

            privateKey = keyFactory.generatePrivate(spec);
        } catch (FileNotFoundException e) {
            System.out.println("Unable to locate private key file: " + filename);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Unable to instantiate KeyFactory with algorithm: " + algorithm);
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } catch (InvalidKeySpecException e) {
            System.out.println("Error instantiating PKCS8EncodedKeySpec object: " + e.getMessage());
        }

        return privateKey;
    }

    /**
     * This method returns the private key in a JKS keystore.
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
     * Utility method for extracting the subjectKeyIdentifier from an X509Certificate.
     * The subjectKeyIdentifier is stored as a DER-encoded octet and will be converted to a String.
     * @return
     */
    public String getCertificateSubjectKeyIdentifier() throws IOException {
        String decodedValue = null;
        byte[] extension = certificate.getExtensionValue(SwidTagConstants.CERTIFICATE_SUBJECT_KEY_IDENTIFIER);
        if (extension != null) {
            decodedValue = JcaX509ExtensionUtils.parseExtensionValue(extension).toString();
        }

        return decodedValue;
    }
}
