package hirs.attestationca.persist.validation;

import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.certificate.CertificateAuthorityCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.entity.userdefined.CertificateTest;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.AttributeCertificateHolder;
import org.bouncycastle.cert.AttributeCertificateIssuer;
import org.bouncycastle.cert.X509AttributeCertificateHolder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v2AttributeCertificateBuilder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Tests the SupplyChainValidator class.
 */
@PrepareForTest({SupplyChainCredentialValidator.class,
        PlatformCredential.class, EndorsementCredential.class })
@PowerMockIgnore({"javax.xml.parsers.*", "org.apache.xerces.jaxp.*", "org.apache.logging.log4j.*",
        "javax.security.auth.*" })
public class SupplyChainCredentialValidatorTest {

    private final SupplyChainCredentialValidator supplyChainCredentialValidator =
            new SupplyChainCredentialValidator();

    private static KeyStore keyStore;
    private static KeyStore emptyKeyStore;
    /**
     * File name used to initialize a test KeyStore.
     */
    static final String KEY_STORE_FILE_NAME = "TestKeyStore";
    /**
     * SecureRandom instance.
     */
    static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final String TEST_SIGNING_KEY = "/validation/platform_credentials/ca.pub";

    private static final String TEST_PLATFORM_CRED =
            "/validation/platform_credentials/plat_cert1.pem";

    //-------Actual ST Micro Endorsement Credential Certificate Chain!--------------
    private static final String EK_CERT = "/certificates/ab21ccf2-tpmcert.pem";
    private static final String INT_CA_CERT02 = "/certificates/fakestmtpmekint02.pem";

    //-------Generated Intel Credential Certificate Chain--------------
    private static final String INTEL_INT_CA =
            "/validation/platform_credentials/intel_chain/root/intermediate1.crt";

    //-------Actual Intel NUC Platform --------------
    private static final String INTEL_SIGNING_KEY = "/certificates/IntelSigningKey_20April2017.pem";

    private static final String NEW_NUC1 =
            "/validation/platform_credentials/Intel_pc3.cer";

    /**
     * Sets up a KeyStore for testing.
     *
     * @throws KeyStoreException
     *             if no Provider supports a KeyStoreSpi implementation for the specified type.
     * @throws NoSuchAlgorithmException
     *             if the algorithm used to check the integrity of the keystore cannot be found
     * @throws CertificateException
     *             if any of the certificates in the keystore could not be loaded
     * @throws IOException
     *             if there is an I/O or format problem with the keystore data, if a password is
     *             required but not given, or if the given password was incorrect
     */
    @BeforeAll
    public static void setUp() throws KeyStoreException, NoSuchAlgorithmException,
            CertificateException, IOException {
        Security.addProvider(new BouncyCastleProvider());

        keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        emptyKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());

        char[] password = "password".toCharArray();
        keyStore.load(null, password);

        try (FileOutputStream fos = new FileOutputStream(KEY_STORE_FILE_NAME)) {
            keyStore.store(fos, password);
        }
    }

    /**
     * Ensures the key store file is deleted after testing.
     */
    @AfterAll
    public static void tearDown() {
        File f = new File(KEY_STORE_FILE_NAME);
        if (!f.delete()) {
            fail("file was not cleaned up");
        }

    }

    /**
     * Checks if a cert can be validated against the given public key.
     *
     * @throws IOException if error occurs while reading files
     * @throws InvalidKeySpecException if error occurs while generating the PublicKey
     * @throws NoSuchAlgorithmException if error occurs while getting RSA KeyFactory
     * @throws URISyntaxException if error occurs constructing test cert path
     * @throws SupplyChainValidatorException if error occurs due to using null certificates
     */
    @Test
    public final void validateTestCertificateAgainstPublicKey()
            throws IOException, InvalidKeySpecException, NoSuchAlgorithmException,
            URISyntaxException, SupplyChainValidatorException {
        PlatformCredential credential = new PlatformCredential(Paths.get(
                Objects.requireNonNull(this.getClass().getResource(TEST_PLATFORM_CRED)).toURI()
        ));

        InputStream stream = this.getClass().getResourceAsStream(TEST_SIGNING_KEY);
        PEMParser pemParser =
                new PEMParser(new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)));
        SubjectPublicKeyInfo info = (SubjectPublicKeyInfo) pemParser.readObject();
        pemParser.close();
        PublicKey signingKey = KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(info.getEncoded()));

        assertTrue(supplyChainCredentialValidator.signatureMatchesPublicKey(
                credential.getX509AttributeCertificateHolder(), signingKey)
        );
    }

    /**
     * Negative test to check if validation against a public key can fail. Generates a random
     * key pair and attempts to validate it against the Intel cert, which is expected to fail.
     *
     * @throws IOException if error occurs while reading files
     * @throws URISyntaxException if an error occurs while constructing test resource's URI
     * @throws SupplyChainValidatorException if error occurs due to using null certificates
     */
    @Test
    public final void checkTestCertificateAgainstInvalidPublicKey()
            throws IOException, URISyntaxException, SupplyChainValidatorException {
        PlatformCredential credential = new PlatformCredential(
                Paths.get(Objects.requireNonNull(this.getClass().getResource(TEST_PLATFORM_CRED)).toURI())
        );

        PublicKey invalidPublicKey = createKeyPair().getPublic();

        assertFalse(SupplyChainCredentialValidator.signatureMatchesPublicKey(
                credential.getX509AttributeCertificateHolder(), invalidPublicKey)
        );
    }

    /**
     * Creates a self-signed "CA" cert, intermediate cert signed by the "CA", and a sample "client"
     * cert signed by the intermediate cert, and an attribute cert using the "client" cert. Attempts
     * to validate the attribute cert against a Set including the "CA" cert and the intermediate
     * cert. Validation should pass because the entire chain is included in the Set.
     *
     * @throws SupplyChainValidatorException if error occurs due to using null certificates
     */
    @Test
    public final void verifyX509AttributeCertificateAgainstIntermediate()
            throws SupplyChainValidatorException {
        KeyPair caKeyPair = createKeyPair();
        KeyPair intermediateKeyPair = createKeyPair();
        KeyPair targetKeyPair = createKeyPair();
        Set<X509Certificate> trustedCerts = new HashSet<X509Certificate>();

        X509Certificate caCert = createSelfSignedCertificate(caKeyPair);
        X509Certificate intermediateCert =
                createCertSignedByAnotherCert(intermediateKeyPair, caKeyPair.getPrivate(), caCert);
        X509Certificate targetCert =
                createCertSignedByAnotherCert(targetKeyPair, intermediateKeyPair.getPrivate(),
                        intermediateCert);
        X509AttributeCertificateHolder attrCert =
                createAttributeCert(targetCert, intermediateCert,
                        intermediateKeyPair.getPrivate());

        trustedCerts.add(caCert);
        trustedCerts.add(intermediateCert);
        boolean assertion = SupplyChainCredentialValidator.validateCertChain(attrCert,
                trustedCerts).isEmpty();

        assertTrue(assertion);

        try {
            keyStore.setCertificateEntry("CA cert", caCert);
            keyStore.setCertificateEntry("Intermediate Cert", intermediateCert);
            assertion = SupplyChainCredentialValidator.verifyCertificate(attrCert,
                    keyStore).isEmpty();
            assertTrue(assertion);
        } catch (Exception e) {
            fail("Unexpected error occurred while verifying certificate", e);
        }
    }

    /**
     * Creates a self-signed "CA" cert, intermediate cert signed by the "CA", and a sample "client"
     * cert signed by the intermediate cert, and an attribute cert based on the "client" cert.
     * Attempts to validate the attribute cert only against the self-signed "CA" cert, which fails
     * as expected.
     *
     * @throws SupplyChainValidatorException if error occurs due to using null certificates
     */
    @Test
    public final void verifyX509AttributeCertificateFailsIfSigningCertNotInList()
            throws SupplyChainValidatorException {
        KeyPair caKeyPair = createKeyPair();
        KeyPair intermediateKeyPair = createKeyPair();
        KeyPair targetKeyPair = createKeyPair();
        Set<X509Certificate> trustedCerts = new HashSet<X509Certificate>();

        X509Certificate caCert = createSelfSignedCertificate(caKeyPair);
        X509Certificate intermediateCert =
                createCertSignedByAnotherCert(intermediateKeyPair, caKeyPair.getPrivate(), caCert);
        X509Certificate targetCert =
                createCertSignedByAnotherCert(targetKeyPair, intermediateKeyPair.getPrivate(),
                        intermediateCert);
        X509AttributeCertificateHolder attrCert =
                createAttributeCert(targetCert, intermediateCert,
                        intermediateKeyPair.getPrivate());

        trustedCerts.add(caCert);

        boolean assertion = SupplyChainCredentialValidator.validateCertChain(attrCert,
                trustedCerts).isEmpty();
        assertFalse(assertion);

        try {
            keyStore.setCertificateEntry("CA cert", caCert);

            assertion = SupplyChainCredentialValidator.verifyCertificate(attrCert,
                    keyStore).isEmpty();
            assertFalse(assertion);
        } catch (Exception e) {
            fail("Unexpected error occurred while verifying certificate", e);
        }
    }

    /**
     * Tests that an X509AttributeCertificate signed by a self-signed cert will pass validation.
     *
     * @throws SupplyChainValidatorException if error occurs due to using null certificates
     */
    @Test
    public final void verifyX509AttributeCertificateAgainstCA()
            throws SupplyChainValidatorException {
        KeyPair caKeyPair = createKeyPair();
        KeyPair targetKeyPair = createKeyPair();
        Set<X509Certificate> trustedCerts = new HashSet<X509Certificate>();

        X509Certificate caCert = createSelfSignedCertificate(caKeyPair);
        X509Certificate targetCert =
                createCertSignedByAnotherCert(targetKeyPair, caKeyPair.getPrivate(), caCert);
        X509AttributeCertificateHolder attrCert =
                createAttributeCert(targetCert, caCert, caKeyPair.getPrivate());

        trustedCerts.add(caCert);

        boolean assertion = SupplyChainCredentialValidator.validateCertChain(
                attrCert, trustedCerts).isEmpty();
        assertTrue(assertion);

        try {
            keyStore.setCertificateEntry("CA cert", caCert);

            assertion = SupplyChainCredentialValidator.verifyCertificate(
                    attrCert, keyStore).isEmpty();
            assertTrue(assertion);
        } catch (Exception e) {
            fail("Unexpected error occurred while verifying certificate", e);
        }
    }

    /**
     * Creates a self-signed "CA" cert, intermediate cert signed by the "CA", and a sample "client"
     * cert signed by the intermediate cert. Attempts to validate the cert only against a Set
     * including the "CA" cert and the intermediate cert. Validation should pass because the entire
     * chain is included in the Set.
     *
     * @throws SupplyChainValidatorException if error occurs due to using null certificates
     */
    @Test
    public final void verifyX509CertificateAgainstIntermediate()
            throws SupplyChainValidatorException {
        KeyPair caKeyPair = createKeyPair();
        KeyPair intermediateKeyPair = createKeyPair();
        KeyPair targetKeyPair = createKeyPair();
        Set<X509Certificate> trustedCerts = new HashSet<X509Certificate>();

        X509Certificate caCert = createSelfSignedCertificate(caKeyPair);
        X509Certificate intermediateCert =
                createCertSignedByAnotherCert(intermediateKeyPair, caKeyPair.getPrivate(), caCert);
        X509Certificate targetCert =
                createCertSignedByAnotherCert(targetKeyPair, intermediateKeyPair.getPrivate(),
                        intermediateCert);

        trustedCerts.add(caCert);
        trustedCerts.add(intermediateCert);

        boolean assertion = SupplyChainCredentialValidator.validateCertChain(targetCert,
                trustedCerts).isEmpty();
        assertTrue(assertion);

        try {
            keyStore.setCertificateEntry("CA cert", caCert);
            keyStore.setCertificateEntry("Intermediate Cert", intermediateCert);

            assertTrue(SupplyChainCredentialValidator.verifyCertificate(targetCert,
                    keyStore));
        } catch (Exception e) {
            fail("Unexpected error occurred while verifying certificate", e);
        }
    }

    /**
     * Creates a self-signed "CA" cert, intermediate cert signed by the "CA", and a sample client
     * cert signed by the intermediate cert. Attempts to validate the cert only against the
     * self-signed cert, which fails as expected.
     *
     * @throws SupplyChainValidatorException if error occurs due to using null certificates
     */
    @Test
    public final void verifyX509CertificateFailsIfSigningCertNotInList()
            throws SupplyChainValidatorException {
        KeyPair caKeyPair = createKeyPair();
        KeyPair intermediateKeyPair = createKeyPair();
        KeyPair targetKeyPair = createKeyPair();
        Set<X509Certificate> trustedCerts = new HashSet<X509Certificate>();

        X509Certificate caCert = createSelfSignedCertificate(caKeyPair);
        X509Certificate intermediateCert =
                createCertSignedByAnotherCert(intermediateKeyPair, caKeyPair.getPrivate(), caCert);
        X509Certificate targetCert =
                createCertSignedByAnotherCert(targetKeyPair, intermediateKeyPair.getPrivate(),
                        intermediateCert);

        trustedCerts.add(caCert);

        boolean assertion = SupplyChainCredentialValidator.validateCertChain(targetCert,
                trustedCerts).isEmpty();
        assertFalse(assertion);

        try {
            keyStore.setCertificateEntry("CA cert", caCert);

            assertFalse(SupplyChainCredentialValidator.verifyCertificate(targetCert,
                    keyStore));
        } catch (Exception e) {
            fail("Unexpected error occurred while verifying certificate", e);
        }
    }

    /**
     * Tests that an X509Certificate signed by a self-signed cert will pass validation.
     *
     * @throws SupplyChainValidatorException if error occurs due to using null certificates
     */
    @Test
    public final void verifyX509CertificateAgainstCA() throws SupplyChainValidatorException {
        KeyPair caKeyPair = createKeyPair();
        KeyPair targetKeyPair = createKeyPair();
        Set<X509Certificate> trustedCerts = new HashSet<X509Certificate>();

        X509Certificate caCert = createSelfSignedCertificate(caKeyPair);
        X509Certificate targetCert =
                createCertSignedByAnotherCert(targetKeyPair, caKeyPair.getPrivate(), caCert);

        trustedCerts.add(caCert);

        boolean assertion = SupplyChainCredentialValidator.validateCertChain(targetCert,
                trustedCerts).isEmpty();
        assertTrue(assertion);

        try {
            keyStore.setCertificateEntry("CA cert", caCert);

            assertTrue(SupplyChainCredentialValidator.verifyCertificate(targetCert,
                    keyStore));
        } catch (Exception e) {
            fail("Unexpected error occurred while verifying certificate", e);
        }
    }

    /**
     * Tests that issuer/subject distinguished names can be properly verified as equal even
     * if their elements are in different orders.
     * @throws URISyntaxException failed to read certificate
     * @throws IOException failed to read certificate
     * @throws KeyStoreException failed to read key store
     * @throws SupplyChainValidatorException missing credential
     */

    @Test
    public final void testPlatformDnEquals() throws URISyntaxException, IOException,
            KeyStoreException, SupplyChainValidatorException {
        Certificate signingCert;
        signingCert = new CertificateAuthorityCredential(
                Files.readAllBytes(Paths.get(Objects.requireNonNull(getClass().getResource(INTEL_SIGNING_KEY)).toURI()))
        );

        byte[] certBytes = Files.readAllBytes(Paths.get(Objects.requireNonNull(CertificateTest.class.
                getResource(NEW_NUC1)).toURI()));

        PlatformCredential pc = new PlatformCredential(certBytes);

        X509AttributeCertificateHolder attributeCert = pc.getX509AttributeCertificateHolder();

        X509Certificate caX509 = signingCert.getX509Certificate();

        assertTrue(SupplyChainCredentialValidator.issuerMatchesSubjectDN(
                attributeCert, caX509));
    }

    /**
     * Tests that issuer/subject distinguished names can be properly verified as being unequal
     * if their elements don't match.
     * @throws URISyntaxException failed to read certificate
     * @throws IOException failed to read certificate
     * @throws KeyStoreException failed to read key store
     * @throws SupplyChainValidatorException missing credential
     */
    @Test
    public final void testPlatformDnNotEquals() throws URISyntaxException, IOException,
            KeyStoreException, SupplyChainValidatorException {
        Certificate signingCert;
        signingCert = new CertificateAuthorityCredential(
                Files.readAllBytes(Paths.get(Objects.requireNonNull(getClass().getResource(INTEL_INT_CA)).toURI()))
        );

        byte[] certBytes = Files.readAllBytes(Paths.get(Objects.requireNonNull(CertificateTest.class.
                getResource(NEW_NUC1)).toURI()));

        PlatformCredential pc = new PlatformCredential(certBytes);

        X509AttributeCertificateHolder attributeCert = pc.getX509AttributeCertificateHolder();

        X509Certificate caX509 = signingCert.getX509Certificate();

        assertFalse(SupplyChainCredentialValidator.issuerMatchesSubjectDN(
                attributeCert, caX509));
    }

    /**
     * Tests that issuer/subject distinguished names can be properly verified as equal.
     * @throws URISyntaxException failed to read certificate
     * @throws IOException failed to read certificate
     * @throws KeyStoreException failed to read key store
     * @throws SupplyChainValidatorException missing credential
     */
    @Test
    public final void testEndorsementDnEquals() throws URISyntaxException, IOException,
            KeyStoreException, SupplyChainValidatorException {
        Certificate signingCert;
        signingCert = new CertificateAuthorityCredential(
                Files.readAllBytes(Paths.get(Objects.requireNonNull(getClass().getResource(INT_CA_CERT02)).toURI()))
        );

        byte[] certBytes = Files.readAllBytes(Paths.get(Objects.requireNonNull(CertificateTest.class.
                getResource(EK_CERT)).toURI()));

        EndorsementCredential ec = new EndorsementCredential(certBytes);

        X509Certificate x509Cert = ec.getX509Certificate();

        X509Certificate caX509 = signingCert.getX509Certificate();

        assertTrue(SupplyChainCredentialValidator.issuerMatchesSubjectDN(
                x509Cert, caX509));
    }

    /**
     * Tests that issuer/subject distinguished names can be properly verified as being unequal
     * if their elements don't match.
     * @throws URISyntaxException failed to read certificate
     * @throws IOException failed to read certificate
     * @throws KeyStoreException failed to read key store
     * @throws SupplyChainValidatorException missing credential
     */
    @Test
    public final void testEndorsementDnNotEquals() throws URISyntaxException, IOException,
            KeyStoreException, SupplyChainValidatorException {
        Certificate signingCert;
        signingCert = new CertificateAuthorityCredential(
                Files.readAllBytes(Paths.get(Objects.requireNonNull(getClass().getResource(INTEL_INT_CA)).toURI()))
        );

        byte[] certBytes = Files.readAllBytes(Paths.get(Objects.requireNonNull(CertificateTest.class.
                getResource(EK_CERT)).toURI()));

        EndorsementCredential ec = new EndorsementCredential(certBytes);

        X509Certificate x509Cert = ec.getX509Certificate();

        X509Certificate caX509 = signingCert.getX509Certificate();

        assertFalse(SupplyChainCredentialValidator.issuerMatchesSubjectDN(
                x509Cert, caX509));
    }

    /**
     * Create a new X.509 attribute certificate given the holder cert, the signing cert, and the
     * signing key.
     *
     * @param targetCert
     *            X509Certificate that will be the holder of the attribute cert
     * @param signingCert
     *            X509Certificate used to sign the new attribute cert
     * @param caPrivateKey
     *            PrivateKey used to sign the new attribute cert
     * @return new X509AttributeCertificate
     */
    private static X509AttributeCertificateHolder createAttributeCert(
            final X509Certificate targetCert, final X509Certificate signingCert,
            final PrivateKey caPrivateKey) {
        X509AttributeCertificateHolder cert = null;
        try {
            final int timeRange = 50000;
            AttributeCertificateHolder holder =
                    new AttributeCertificateHolder(new X509CertificateHolder(
                            targetCert.getEncoded()));
            AttributeCertificateIssuer issuer =
                    new AttributeCertificateIssuer(new X500Name(signingCert
                            .getSubjectX500Principal().getName()));
            BigInteger serialNumber = BigInteger.ONE;
            Date notBefore = new Date(System.currentTimeMillis() - timeRange);
            Date notAfter = new Date(System.currentTimeMillis() + timeRange);
            X509v2AttributeCertificateBuilder builder =
                    new X509v2AttributeCertificateBuilder(holder, issuer, serialNumber, notBefore,
                            notAfter);

            ContentSigner signer =
                    new JcaContentSignerBuilder("SHA1WithRSA").setProvider("BC")
                            .build(caPrivateKey);

            cert = builder.build(signer);
        } catch (CertificateEncodingException | IOException | OperatorCreationException e) {
            fail("Exception occurred while creating a cert", e);
        }

        return cert;

    }

    /**
     * Creates a new RSA 1024-bit KeyPair using a Bouncy Castle Provider.
     *
     * @return new KeyPair
     */
    private static KeyPair createKeyPair() {
        final int keySize = 1024;
        KeyPairGenerator gen;
        KeyPair keyPair = null;
        try {
            gen = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
            gen.initialize(keySize, SECURE_RANDOM);
            keyPair = gen.generateKeyPair();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            fail("Error occurred while generating key pair", e);
        }
        return keyPair;
    }

    /**
     * Create a new X.509 public-key certificate signed by the given certificate.
     *
     * @param keyPair
     *            KeyPair to create the cert for
     * @param signingKey
     *            PrivateKey of the signing cert
     * @param signingCert
     *            signing cert
     * @return new X509Certificate
     */
    private static X509Certificate createCertSignedByAnotherCert(final KeyPair keyPair,
                                                                 final PrivateKey signingKey, final X509Certificate signingCert) {
        final int timeRange = 10000;
        X509Certificate cert = null;
        try {

            X500Name issuerName = new X500Name(signingCert.getSubjectX500Principal().getName());
            X500Name subjectName = new X500Name("CN=Test V3 Certificate");
            BigInteger serialNumber = BigInteger.ONE;
            Date notBefore = new Date(System.currentTimeMillis() - timeRange);
            Date notAfter = new Date(System.currentTimeMillis() + timeRange);
            X509v3CertificateBuilder builder =
                    new JcaX509v3CertificateBuilder(issuerName, serialNumber, notBefore, notAfter,
                            subjectName, keyPair.getPublic());
            ContentSigner signer =
                    new JcaContentSignerBuilder("SHA1WithRSA").setProvider("BC").build(signingKey);
            return new JcaX509CertificateConverter().setProvider("BC").getCertificate(
                    builder.build(signer));
        } catch (Exception e) {
            fail("Exception occurred while creating a cert", e);
        }
        return cert;
    }

    /**
     * Creates a self-signed X.509 public-key certificate.
     *
     * @param pair
     *            KeyPair to create the cert for
     * @return self-signed X509Certificate
     */
    private static X509Certificate createSelfSignedCertificate(final KeyPair pair) {
        Security.addProvider(new BouncyCastleProvider());
        final int timeRange = 10000;
        X509Certificate cert = null;
        try {

            X500Name issuerName = new X500Name("CN=Test Self-Signed V3 Certificate");
            X500Name subjectName = new X500Name("CN=Test Self-Signed V3 Certificate");
            BigInteger serialNumber = BigInteger.ONE;
            Date notBefore = new Date(System.currentTimeMillis() - timeRange);
            Date notAfter = new Date(System.currentTimeMillis() + timeRange);
            X509v3CertificateBuilder builder =
                    new JcaX509v3CertificateBuilder(issuerName, serialNumber, notBefore, notAfter,
                            subjectName, pair.getPublic());
            ContentSigner signer =
                    new JcaContentSignerBuilder("SHA1WithRSA").setProvider("BC").build(
                            pair.getPrivate());
            return new JcaX509CertificateConverter().setProvider("BC").getCertificate(
                    builder.build(signer));
        } catch (Exception e) {
            fail("Exception occurred while creating a cert", e);
        }
        return cert;
    }

}
