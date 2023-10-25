package hirs.utils;

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
     * Location of another, slightly different platform attribute cert.
     */
    public static final String TEST_PLATFORM_CERT_3 =
            "/validation/platform_credentials/Intel_pc3.cer";

    /**
     * Platform cert with comma separated baseboard and chassis serial number.
     */
    public static final String TEST_PLATFORM_CERT_4 =
            "/validation/platform_credentials/Intel_pc4.pem";

    /**
     * Another platform cert with comma separated baseboard and chassis serial number.
     */
    public static final String TEST_PLATFORM_CERT_5 =
            "/validation/platform_credentials/Intel_pc5.pem";

    /**
     * Location of another, slightly different platform attribute cert.
     */
    public static final String TEST_PLATFORM_CERT_6 =
            "/validation/platform_credentials/TPM_INTC_Platform_Cert_RSA.txt";

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

    private static final String INT_CA_CERT02 = "/certificates/fakestmtpmekint02.pem";

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
                        Paths.get(this.getClass().getResource(FAKE_ROOT_CA_FILE).toURI())
                )
        );
        assertEquals(
                certificate.getX509Certificate().getIssuerDN().getName(),
                "CN=Fake Root CA"
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
                Paths.get(this.getClass().getResource(FAKE_ROOT_CA_FILE).toURI())
        );
        assertEquals(
                certificate.getX509Certificate().getIssuerDN().getName(),
                "CN=Fake Root CA"
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
        assertEquals(getTestCertificate(FAKE_ROOT_CA_FILE).getCertificateType(),
                Certificate.CertificateType.X509_CERTIFICATE);
        assertNotEquals(getTestCertificate(FAKE_ROOT_CA_FILE).getCertificateType(),
                Certificate.CertificateType.ATTRIBUTE_CERTIFICATE);

        assertNotEquals(getTestCertificate(
                        PlatformCredential.class,
                        TEST_PLATFORM_CERT_3).getCertificateType(),
                Certificate.CertificateType.X509_CERTIFICATE);
        assertEquals(getTestCertificate(
                        PlatformCredential.class,
                        TEST_PLATFORM_CERT_3).getCertificateType(),
                Certificate.CertificateType.ATTRIBUTE_CERTIFICATE);

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
                PlatformCredential.class, TEST_PLATFORM_CERT_4
        );

        assertEquals(platformCredential.getCertificateType(),
                Certificate.CertificateType.ATTRIBUTE_CERTIFICATE);
        assertEquals(
                ((PlatformCredential) platformCredential).getPlatformSerial(),
                "GETY421001GV"
        );

        platformCredential = getTestCertificate(
                PlatformCredential.class, TEST_PLATFORM_CERT_5
        );

        assertEquals(platformCredential.getCertificateType(),
                Certificate.CertificateType.ATTRIBUTE_CERTIFICATE);
        assertEquals(
                ((PlatformCredential) platformCredential).getPlatformSerial(),
                "GETY42100160"
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

        assertEquals(rootCert.getSerialNumber(), certificate.getSerialNumber());
        assertEquals(rootCert.getIssuer(),
                certificate.getIssuerX500Principal().getName());
        assertEquals(rootCert.getSubject(),
                certificate.getSubjectX500Principal().getName());
        assertArrayEquals(rootCert.getEncodedPublicKey(),
                certificate.getPublicKey().getEncoded());
        assertArrayEquals(rootCert.getSignature(), certificate.getSignature());
        assertEquals(rootCert.getBeginValidity(), certificate.getNotBefore());
        assertEquals(rootCert.getEndValidity(), certificate.getNotAfter());
    }

    /**
     * Tests that Certificate correctly parses out non standard fields from an X509 Certificate.
     *
     * @throws IOException if there is a problem reading the cert file at the given path
     */
    @Test
    public void testX509CertificateParsingExtended() throws IOException {
        Certificate rootCert = getTestCertificate(INTEL_INT_CA_FILE);
        assertEquals(rootCert.getAuthorityInfoAccess(),
                "https://trustedservices.intel.com/"
                        + "content/TSC/certs/TSC_SS_RootCA_Certificate.cer\n");
        assertEquals(rootCert.getAuthorityKeyIdentifier(),
                "b56f72cdfd66ce839e1fdb40498f07291f5b99b7");
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
                TEST_PLATFORM_CERT_3
        );

        X509AttributeCertificateHolder attrCertHolder = new X509AttributeCertificateHolder(
                Files.readAllBytes(Paths.get(this.getClass().getResource(
                        TEST_PLATFORM_CERT_3
                ).toURI()))
        );

        assertEquals(
                platformCert.getSerialNumber(),
                attrCertHolder.getSerialNumber()
        );
        assertEquals(
                platformCert.getIssuer(),
                attrCertHolder.getIssuer().getNames()[0].toString()
        );
        assertEquals(platformCert.getSubject(), null);
        assertArrayEquals(platformCert.getEncodedPublicKey(), null);
        assertArrayEquals(platformCert.getSignature(), attrCertHolder.getSignature());
        assertEquals(platformCert.getBeginValidity(), attrCertHolder.getNotBefore());
        assertEquals(platformCert.getEndValidity(), attrCertHolder.getNotAfter());
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
            throws IOException, URISyntaxException {
        Certificate platformCert = getTestCertificate(
                PlatformCredential.class, TEST_PLATFORM_CERT_6);

        assertEquals(platformCert.getAuthorityInfoAccess(),
                "https://trustedservices.intel.com/"
                        + "content/TSC/certs/TSC_IssuingCAIKGF_TEST.cer\n");
        assertEquals(platformCert.getAuthorityKeyIdentifier(),
                "a5ecc6c07da02c6af8764d4e5c16483610a0b040");
    }

    /**
     * Tests that Certificate correctly trims out additional padding from a given certificate.
     *
     * @throws IOException        if there is a problem reading the cert file at the given path
     * @throws URISyntaxException if there is a problem constructing the file's URI
     */
    @Test
    public void testCertificateTrim() throws IOException, URISyntaxException {
        byte[] rawFileBytes = Files.readAllBytes(Paths.get(CertificateTest.class
                .getResource(EK_CERT_WITH_PADDED_BYTES).toURI()));
        byte[] expectedCertBytes = Arrays.copyOfRange(rawFileBytes, 0, 908);
        Certificate ekCert = getTestCertificate(EndorsementCredential.class,
                EK_CERT_WITH_PADDED_BYTES);
        assertEquals(ekCert.getSerialNumber(), new BigInteger("16842032579184247954"));
        assertEquals(ekCert.getIssuer(),
                "CN=Nuvoton TPM Root CA 2010+O=Nuvoton Technology Corporation+C=TW");
        assertEquals(ekCert.getSubject(), "");
        assertArrayEquals(ekCert.getRawBytes(), expectedCertBytes);
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
        byte[] rawFileBytes = Files.readAllBytes(Paths.get(CertificateTest.class
                .getResource(EK_CERT_WITH_PADDED_BYTES).toURI()));
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
        byte[] rawFileBytes = Files.readAllBytes(Paths.get(CertificateTest.class
                .getResource(EK_CERT_WITH_PADDED_BYTES).toURI()));
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
        byte[] rawFileBytes = Files.readAllBytes(Paths.get(CertificateTest.class
                .getResource(EK_CERT_WITH_PADDED_BYTES).toURI()));
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
                        Paths.get(this.getClass().getResource(FAKE_ROOT_CA_FILE).toURI())
                ),
                new CertificateAuthorityCredential(
                        Files.readAllBytes(
                                Paths.get(this.getClass().getResource(FAKE_ROOT_CA_FILE).toURI())
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
                getTestCertificate(CertificateAuthorityCredential.class, FAKE_ROOT_CA_FILE),
                null
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

        assertEquals(issuerCert.isIssuer(cert), "Certificate signature failed to verify");
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
                        Paths.get(this.getClass().getResource(FAKE_ROOT_CA_FILE).toURI())
                ).hashCode(),
                new CertificateAuthorityCredential(
                        Files.readAllBytes(
                                Paths.get(this.getClass().getResource(FAKE_ROOT_CA_FILE).toURI())
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
            throws IOException {

        Path certPath;
        try {
            certPath = Paths.get(CertificateTest.class.getResource(filename).toURI());
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
                Paths.get(CertificateTest.class.getResource(resourceName).toURI()).toFile()
        )) {
            return (X509Certificate) cf.generateCertificate(certInputStream);
        } catch (CertificateException | URISyntaxException e) {
            throw new IOException("Cannot read certificate", e);
        }
    }
}