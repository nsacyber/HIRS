package hirs.attestationca.persist.entity.userdefined;

import hirs.attestationca.persist.entity.ArchivableEntity;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import hirs.attestationca.persist.entity.userdefined.certificate.*;
import org.bouncycastle.cert.X509AttributeCertificateHolder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * This class tests functionality of the {@link Certificate} class.
 */
public class CertificateTest {
    /**
     * Location of a test (fake) root CA certificate.
     */
    public static final String FAKE_ROOT_CA_FILE = "/certificates/fakeRootCA.cer";

    /**
     * Location of a test (fake) Intel intermediate CA certificate.
     */
    public static final String FAKE_INTEL_INT_CA_FILE =
            "/certificates/fakeIntelIntermediateCA.cer";

    /**
     * Location of a test (fake) Intel intermediate CA certificate.
     */
    public static final String INTEL_INT_CA_FILE =
            "/validation/platform_credentials/intel_chain/root/intermediate2.cer";

    /**
     * Location of a test (fake) SGI intermediate CA certificate.
     */
    public static final String FAKE_SGI_INT_CA_FILE = "/certificates/fakeSGIIntermediateCA.cer";

    /**
     * Location of another test self-signed certificate.
     */
    public static final String ANOTHER_SELF_SIGNED_FILE =
            "/certificates/fakeSelfSigned.cer";

    /**
     * Location of the NUC EC.
     */
    public static final String STM_NUC1_EC = "/certificates/nuc-1/tpmcert.pem";

    /**
     * Location of the ST Micro Intermediate 02 CA certificate.
     */
    public static final String STM_INT_02_CA = "/certificates/stMicroCaCerts/stmtpmekint02.crt";

    /**
     * Location of the ST Micro Root CA certificate.
     */
    public static final String STM_ROOT_CA = "/certificates/stMicroCaCerts/stmtpmekroot.crt";

    /**
     * Location of the GlobalSign Root CA certificate.
     */
    public static final String GS_ROOT_CA = "/certificates/stMicroCaCerts/gstpmroot.crt";

    /**
     * Hex-encoded subject key identifier for the FAKE_ROOT_CA_FILE.
     */
    public static final String FAKE_ROOT_CA_SUBJECT_KEY_IDENTIFIER_HEX =
            "58ec313a1699f94c1c8c4e2c6412402b258f0177";

    /**
     * Location of a test STM endorsement credential.
     */
    public static final String TEST_EC = "/certificates/ab21ccf2-tpmcert.pem";

    /**
     * Location of a test client cert.
     */
    public static final String ISSUED_CLIENT_CERT =
            "/tpm/sample_identity_cert.cer";

    private static final String INT_CA_CERT02 = "/certificates/fakestmtpmekint02.pem";

    private static final String RDN_COMMA_SEPARATED =
            "CN=STM TPM EK Intermediate CA 02, O=STMicroelectronics NV, C=CH";
    private static final String RDN_MULTIVALUE =
            "CN=Nuvoton TPM Root CA 2010+O=Nuvoton Technology Corporation+C=TW";

    private static final String RDN_COMMA_SEPARATED_ORGANIZATION = "STMicroelectronics NV";
    private static final String RDN_MULTIVALUE_ORGANIZATION = "Nuvoton Technology Corporation";

    private static final String EK_CERT_WITH_PADDED_BYTES =
            "/certificates/ek_cert_with_padded_bytes.cer";


    /**
     * Tests that a certificate can be constructed from a byte array.
     *
     * @throws IOException        if there is a problem reading the cert file into a byte array
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testConstructCertFromByteArray() throws IOException, URISyntaxException {
        Certificate certificate = new CertificateAuthorityCredential(
                Files.readAllBytes(
                        Paths.get(Objects.requireNonNull(this.getClass().getResource(FAKE_ROOT_CA_FILE)).toURI())
                )
        );
        assertEquals(
                "CN=Fake Root CA",
                certificate.getX509Certificate().getIssuerX500Principal().getName()
        );
    }

    /**
     * Ensure that a Certificate cannot be created from a null byte array.
     *
     * @throws IOException          if the certificate could not be constructed properly
     * @throws CertificateException if there is a problem de/serializing the certificate
     */
    @Test
    public void testConstructCertFromNullByteArray()
            throws IOException, CertificateException {
        assertThrows(IllegalArgumentException.class, () ->
                new CertificateAuthorityCredential((byte[]) null));
    }

    /**
     * Ensure that a Certificate cannot be created from an empty byte array.
     *
     * @throws IOException          if the certificate could not be constructed properly
     * @throws CertificateException if there is a problem de/serializing the certificate
     */
    @Test
    public void testConstructCertFromEmptyByteArray()
            throws IOException, CertificateException {
        assertThrows(IllegalArgumentException.class, () ->
                new CertificateAuthorityCredential(new byte[]{}));
    }

    /**
     * Tests that a certificate can be constructed from a path.
     *
     * @throws IOException        if there is a problem reading the cert file at the given path
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testConstructCertFromPath() throws URISyntaxException, IOException {
        Certificate certificate = new CertificateAuthorityCredential(
                Paths.get(Objects.requireNonNull(this.getClass().getResource(FAKE_ROOT_CA_FILE)).toURI())
        );
        assertEquals(
                "CN=Fake Root CA",
                certificate.getX509Certificate().getIssuerX500Principal().getName()
        );
    }

    /**
     * Tests that a certificate cannot be constructed from a null path.
     *
     * @throws IOException        if there is a problem reading the cert file at the given path
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testConstructCertFromNullPath() throws URISyntaxException, IOException {
        assertThrows(IllegalArgumentException.class, () ->
                new CertificateAuthorityCredential((Path) null));
    }

    /**
     * Tests that Certificate correctly reports whether a certificate is a regular X509 cert or
     * an X509 attribute certificate.
     *
     * @throws IOException if there is a problem reading the cert file at the given path
     */
    @Test
    public void testGetCertificateType() throws IOException {
        assertEquals(
                Certificate.CertificateType.X509_CERTIFICATE,
                getTestCertificate(FAKE_ROOT_CA_FILE).getCertificateType());
        assertNotEquals(
                Certificate.CertificateType.ATTRIBUTE_CERTIFICATE,
                getTestCertificate(FAKE_ROOT_CA_FILE).getCertificateType());

        assertNotEquals(
                Certificate.CertificateType.X509_CERTIFICATE,
                getTestCertificate(
                        PlatformCredential.class,
                        PlatformCredentialTest.TEST_PLATFORM_CERT_3).getCertificateType());
        assertEquals(
                Certificate.CertificateType.ATTRIBUTE_CERTIFICATE,
                getTestCertificate(
                        PlatformCredential.class,
                        PlatformCredentialTest.TEST_PLATFORM_CERT_3).getCertificateType());

    }

    /**
     * Ensures a certificate can be parsed from a PEM file.
     * Tests both standard and attribute certificate headers.
     *
     * @throws IOException if there is a problem reading the test certificate
     */
    @Test
    public void testImportPem() throws IOException {
        Certificate platformCredential = getTestCertificate(
                PlatformCredential.class, PlatformCredentialTest.TEST_PLATFORM_CERT_4
        );

        assertEquals(
                Certificate.CertificateType.ATTRIBUTE_CERTIFICATE,
                platformCredential.getCertificateType());
        assertEquals(
                "GETY421001GV",
                ((PlatformCredential) platformCredential).getPlatformSerial()
        );

        platformCredential = getTestCertificate(
                PlatformCredential.class, PlatformCredentialTest.TEST_PLATFORM_CERT_5
        );

        assertEquals(
                Certificate.CertificateType.ATTRIBUTE_CERTIFICATE,
                platformCredential.getCertificateType());
        assertEquals(
                "GETY42100160",
                ((PlatformCredential) platformCredential).getPlatformSerial()
        );

    }

    /**
     * Tests that Certificate correctly parses out standard fields from an X509 Certificate.
     *
     * @throws IOException if there is a problem reading the cert file at the given path
     */
    @Test
    public void testX509CertificateParsing() throws IOException {
        Certificate rootCert = getTestCertificate(FAKE_ROOT_CA_FILE);
        X509Certificate certificate = readX509Certificate(FAKE_ROOT_CA_FILE);

        assertEquals(certificate.getSerialNumber(), rootCert.getSerialNumber());
        assertEquals(certificate.getIssuerX500Principal().getName(),
                rootCert.getIssuer());
        assertEquals(certificate.getSubjectX500Principal().getName(),
                rootCert.getSubject());
        assertArrayEquals(certificate.getPublicKey().getEncoded(),
                rootCert.getEncodedPublicKey());
        assertArrayEquals(certificate.getSignature(), rootCert.getSignature());
        assertEquals(certificate.getNotBefore(), rootCert.getBeginValidity());
        assertEquals(certificate.getNotAfter(), rootCert.getEndValidity());
    }

    /**
     * Tests that Certificate correctly parses out non standard fields from an X509 Certificate.
     *
     * @throws IOException if there is a problem reading the cert file at the given path
     */
    @Test
    public void testX509CertificateParsingExtended() throws IOException {
        Certificate rootCert = getTestCertificate(INTEL_INT_CA_FILE);
        assertEquals(
                "https://trustedservices.intel.com/"
                        + "content/TSC/certs/TSC_SS_RootCA_Certificate.cer\n",
                rootCert.getAuthorityInfoAccess());
        assertEquals(
                "b56f72cdfd66ce839e1fdb40498f07291f5b99b7",
                rootCert.getAuthorityKeyIdentifier());
    }

    /**
     * Tests that Certificate correctly parses out standard fields from an X509 attribute
     * certificate.
     *
     * @throws IOException        if there is a problem reading the cert file at the given path
     * @throws URISyntaxException if there is a problem constructing the file's URI
     */
    @Test
    public void testX509AttributeCertificateParsing() throws IOException, URISyntaxException {
        Certificate platformCert = getTestCertificate(
                PlatformCredential.class,
                PlatformCredentialTest.TEST_PLATFORM_CERT_3
        );

        X509AttributeCertificateHolder attrCertHolder = new X509AttributeCertificateHolder(
                Files.readAllBytes(Paths.get(Objects.requireNonNull(this.getClass().getResource(
                        PlatformCredentialTest.TEST_PLATFORM_CERT_3
                )).toURI()))
        );

        assertEquals(
                attrCertHolder.getSerialNumber(),
                platformCert.getSerialNumber()
        );
        assertEquals(
                attrCertHolder.getIssuer().getNames()[0].toString(),
                platformCert.getIssuer()
        );
        assertEquals(null, platformCert.getSubject());
        assertArrayEquals(null, platformCert.getEncodedPublicKey());
        assertArrayEquals(attrCertHolder.getSignature(), platformCert.getSignature());
        assertEquals(attrCertHolder.getNotBefore(), platformCert.getBeginValidity());
        assertEquals(attrCertHolder.getNotAfter(), platformCert.getEndValidity());
    }

    /**
     * Tests that Certificate correctly parses out non-standard fields from an X509 attribute
     * certificate.
     *
     * @throws IOException        if there is a problem reading the cert file at the given path
     * @throws URISyntaxException if there is a problem constructing the file's URI
     */
    @Test
    public void testX509AttributeCertificateParsingExtended()
            throws IOException, URISyntaxException, UnsupportedOperationException {
        Certificate platformCert = getTestCertificate(
                PlatformCredential.class, PlatformCredentialTest.TEST_PLATFORM_CERT_6);

        assertEquals("https://trustedservices.intel.com/"
                        + "content/TSC/certs/TSC_IssuingCAIKGF_TEST.cer\n",
                platformCert.getAuthorityInfoAccess());
        assertEquals("a5ecc6c07da02c6af8764d4e5c16483610a0b040",
                platformCert.getAuthorityKeyIdentifier());
    }

    /**
     * Tests that Certificate correctly trims out additional padding from a given certificate.
     *
     * @throws IOException        if there is a problem reading the cert file at the given path
     * @throws URISyntaxException if there is a problem constructing the file's URI
     */
    @Test
    public void testCertificateTrim() throws IOException, URISyntaxException {
        byte[] rawFileBytes = Files.readAllBytes(Paths.get(Objects.requireNonNull(CertificateTest.class
                .getResource(EK_CERT_WITH_PADDED_BYTES)).toURI()));
        byte[] expectedCertBytes = Arrays.copyOfRange(rawFileBytes, 0, 908);
        Certificate ekCert = getTestCertificate(EndorsementCredential.class,
                EK_CERT_WITH_PADDED_BYTES);
        assertEquals(new BigInteger("16842032579184247954"), ekCert.getSerialNumber());
        assertEquals("CN=Nuvoton TPM Root CA 2010+O=Nuvoton Technology Corporation+C=TW",
                ekCert.getIssuer());
        assertEquals("", ekCert.getSubject());
        assertArrayEquals(expectedCertBytes, ekCert.getRawBytes());
    }

    /**
     * Tests that Certificate correctly throws IllegalArgumentException when no length field is
     * found in the provided byte array.
     *
     * @throws IOException        if there is a problem reading the cert file at the given path
     * @throws URISyntaxException if there is a problem constructing the file's URI
     */
    @Test
    public void testCertificateTrimThrowsWhenNoLengthFieldFound() throws IOException,
            URISyntaxException {
        byte[] rawFileBytes = Files.readAllBytes(Paths.get(Objects.requireNonNull(CertificateTest.class
                .getResource(EK_CERT_WITH_PADDED_BYTES)).toURI()));
        assertThrows(IllegalArgumentException.class, () ->
                        new EndorsementCredential(Arrays.copyOfRange(rawFileBytes, 0, 2)),
                ".* No certificate length field could be found\\.");
    }

    /**
     * Tests that Certificate correctly throws IllegalArgumentException when the byte array only
     * contains a header for an ASN.1 Sequence.
     *
     * @throws IOException        if there is a problem reading the cert file at the given path
     * @throws URISyntaxException if there is a problem constructing the file's URI
     */
    @Test
    public void testCertificateTrimThrowsWhenOnlyASN1Sequence() throws IOException,
            URISyntaxException {
        byte[] rawFileBytes = Files.readAllBytes(Paths.get(Objects.requireNonNull(CertificateTest.class
                .getResource(EK_CERT_WITH_PADDED_BYTES)).toURI()));
        assertThrows(IllegalArgumentException.class, () ->
                        new EndorsementCredential(Arrays.copyOfRange(rawFileBytes, 0, 4)),
                ".* Certificate is nothing more than ASN.1 Sequence\\\\.");
    }

    /**
     * Tests that Certificate correctly throws IllegalArgumentException when the provided
     * Certificate has a length that extends beyond the byte array as a whole.
     *
     * @throws IOException        if there is a problem reading the cert file at the given path
     * @throws URISyntaxException if there is a problem constructing the file's URI
     */
    @Test
    public void testCertificateTrimThrowsWhenLengthIsTooLarge() throws IOException,
            URISyntaxException {
        byte[] rawFileBytes = Files.readAllBytes(Paths.get(Objects.requireNonNull(CertificateTest.class
                .getResource(EK_CERT_WITH_PADDED_BYTES)).toURI()));
        assertThrows(IllegalArgumentException.class, () ->
                        new EndorsementCredential(Arrays.copyOfRange(rawFileBytes, 0, 42)),
                ".* Value of certificate length field extends beyond"
                        + " length of provided certificate\\.");
    }

    /**
     * Tests that the equals method on {@link Certificate} works as expected.
     *
     * @throws IOException          if the certificate could not be constructed properly
     * @throws CertificateException if there is a problem with the KeyStore or de/serializing the
     *                              certificate
     * @throws URISyntaxException   if there is a problem constructing the path to the certificate
     */
    @Test
    public void testEquals() throws CertificateException, IOException, URISyntaxException {
        assertEquals(
                getTestCertificate(FAKE_ROOT_CA_FILE),
                getTestCertificate(FAKE_ROOT_CA_FILE)
        );

        assertEquals(
                new CertificateAuthorityCredential(
                        Paths.get(Objects.requireNonNull(this.getClass().getResource(FAKE_ROOT_CA_FILE)).toURI())
                ),
                new CertificateAuthorityCredential(
                        Files.readAllBytes(
                                Paths.get(Objects.requireNonNull(this.getClass().getResource(FAKE_ROOT_CA_FILE)).toURI())
                        )
                )
        );

        assertNotEquals(
                getTestCertificate(CertificateAuthorityCredential.class, FAKE_ROOT_CA_FILE),
                getTestCertificate(CertificateAuthorityCredential.class, FAKE_INTEL_INT_CA_FILE)
        );

        assertNotEquals(
                getTestCertificate(CertificateAuthorityCredential.class, FAKE_ROOT_CA_FILE),
                getTestCertificate(ConformanceCredential.class, FAKE_ROOT_CA_FILE)
        );

        assertNotEquals(
                null,
                getTestCertificate(CertificateAuthorityCredential.class, FAKE_ROOT_CA_FILE)
       );
    }

    /**
     * Tests that the isIssuer method on {@link Certificate} works as expected.
     *
     * @throws IOException             if the certificate could not be constructed properly
     * @throws CertificateException    if there is a problem with the KeyStore or de/serializing the
     *                                 certificate
     * @throws NoSuchProviderException if the Bouncy Castle security provider is unavailable
     * @throws URISyntaxException      if there is a problem constructing the path to the certificate
     */
    @Test
    public void testIsIssuer() throws CertificateException, IOException, NoSuchProviderException,
            URISyntaxException {
        Certificate issuerCert = getTestCertificate(FAKE_ROOT_CA_FILE);
        Certificate cert = getTestCertificate(INT_CA_CERT02);

        assertEquals("Certificate signature failed to verify", issuerCert.isIssuer(cert));
        assertTrue(cert.isIssuer(issuerCert).isEmpty());
    }

    /**
     * Tests that the hashCode method on {@link Certificate} works as expected.
     *
     * @throws IOException          if the certificate could not be constructed properly
     * @throws CertificateException if there is a problem with the KeyStore or de/serializing the
     *                              certificate
     * @throws URISyntaxException   if there is a problem constructing the path to the certificate
     */
    @Test
    public void testHashCode() throws CertificateException, IOException, URISyntaxException {
        assertEquals(
                getTestCertificate(FAKE_ROOT_CA_FILE).hashCode(),
                getTestCertificate(FAKE_ROOT_CA_FILE).hashCode()
        );

        assertEquals(
                new CertificateAuthorityCredential(
                        Paths.get(Objects.requireNonNull(this.getClass().getResource(FAKE_ROOT_CA_FILE)).toURI())
                ).hashCode(),
                new CertificateAuthorityCredential(
                        Files.readAllBytes(
                                Paths.get(Objects.requireNonNull(this.getClass().getResource(FAKE_ROOT_CA_FILE)).toURI())
                        )
                ).hashCode()
        );

        assertNotEquals(
                getTestCertificate(
                        CertificateAuthorityCredential.class, FAKE_ROOT_CA_FILE
                ).hashCode(),
                getTestCertificate(
                        CertificateAuthorityCredential.class, FAKE_INTEL_INT_CA_FILE
                ).hashCode()
        );
    }

    /**
     * Construct a CertificateAuthorityCredential from the given parameters.
     *
     * @param filename the location of the certificate to be used
     * @return the newly-constructed Certificate
     * @throws IOException if there is a problem constructing the test certificate
     */
    public static Certificate getTestCertificate(
            final String filename) throws IOException {
        return getTestCertificate(CertificateAuthorityCredential.class, filename);
    }


    /**
     * Construct a test certificate from the given parameters.
     *
     * @param <T>              the type of Certificate that will be created
     * @param certificateClass the class of certificate to generate
     * @param filename         the location of the certificate to be used
     * @return the newly-constructed Certificate
     * @throws IOException if there is a problem constructing the test certificate
     */
    public static <T extends ArchivableEntity> Certificate getTestCertificate(
            final Class<T> certificateClass, final String filename)
            throws IOException {
        return getTestCertificate(certificateClass, filename, null, null);
    }

    /**
     * Construct a test certificate from the given parameters.
     *
     * @param <T>                   the type of Certificate that will be created
     * @param certificateClass      the class of certificate to generate
     * @param filename              the location of the certificate to be used
     * @param endorsementCredential the endorsement credentials (can be null)
     * @param platformCredentials   the platform credentials (can be null)
     * @return the newly-constructed Certificate
     * @throws IOException if there is a problem constructing the test certificate
     */
    public static <T extends ArchivableEntity> Certificate getTestCertificate(
            final Class<T> certificateClass, final String filename,
            final EndorsementCredential endorsementCredential,
            final List<PlatformCredential> platformCredentials)
            throws IOException, UnsupportedOperationException {

        Path certPath;
        try {
            certPath = Paths.get(Objects.requireNonNull(CertificateTest.class.getResource(filename)).toURI());
        } catch (URISyntaxException e) {
            throw new IOException("Could not resolve path URI", e);
        }

        switch (certificateClass.getSimpleName()) {
            case "CertificateAuthorityCredential":
                return new CertificateAuthorityCredential(certPath);
            case "ConformanceCredential":
                return new ConformanceCredential(certPath);
            case "EndorsementCredential":
                return new EndorsementCredential(certPath);
            case "PlatformCredential":
                return new PlatformCredential(certPath);
            case "IssuedAttestationCertificate":
                return new IssuedAttestationCertificate(certPath,
                        endorsementCredential, platformCredentials);
            default:
                throw new IllegalArgumentException(
                        String.format("Unknown certificate class %s", certificateClass.getName())
                );
        }
    }

    /**
     * Return a list of all test certificates.
     *
     * @return a list of all test certificates
     * @throws IOException if there is a problem deserializing certificates
     */
    public static List<ArchivableEntity> getAllTestCertificates() throws IOException {
        return Arrays.asList(
                getTestCertificate(CertificateAuthorityCredential.class, FAKE_SGI_INT_CA_FILE),
                getTestCertificate(CertificateAuthorityCredential.class, FAKE_INTEL_INT_CA_FILE),
                getTestCertificate(CertificateAuthorityCredential.class, FAKE_ROOT_CA_FILE)
        );
    }

    private static X509Certificate readX509Certificate(final String resourceName)
            throws IOException {

        CertificateFactory cf;
        try {
            cf = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            throw new IOException("Cannot get X509 CertificateFactory instance", e);
        }

        try (FileInputStream certInputStream = new FileInputStream(
                Paths.get(Objects.requireNonNull(CertificateTest.class.getResource(resourceName)).toURI()).toFile()
        )) {
            return (X509Certificate) cf.generateCertificate(certInputStream);
        } catch (CertificateException | URISyntaxException e) {
            throw new IOException("Cannot read certificate", e);
        }
    }
}