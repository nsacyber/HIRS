package hirs.attestationca.persist.entity.userdefined;

import hirs.attestationca.persist.entity.userdefined.certificate.CertificateAuthorityCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.ConformanceCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.EndorsementCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import org.bouncycastle.cert.X509AttributeCertificateHolder;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This class tests functionality of the {@link Certificate} class.
 */
public class CertificateTest extends AbstractUserdefinedEntityTest {

    /**
     * Location of a test (fake) Intel intermediate CA certificate.
     */
    public static final String INTEL_INT_CA_FILE =
            "/validation/platform_credentials/intel_chain/root/intermediate2.cer";

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

    private static X509Certificate readX509Certificate(final String resourceName)
            throws IOException {

        CertificateFactory cf;
        try {
            cf = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            throw new IOException("Cannot get X509 CertificateFactory instance", e);
        }

        try (FileInputStream certInputStream = new FileInputStream(Paths.get(
                Objects.requireNonNull(CertificateTest.class.getResource(
                        resourceName)).toURI()).toFile()
        )) {
            return (X509Certificate) cf.generateCertificate(certInputStream);
        } catch (CertificateException | URISyntaxException e) {
            throw new IOException("Cannot read certificate", e);
        }
    }

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
                        Paths.get(Objects.requireNonNull(this.getClass().getResource(
                                FAKE_ROOT_CA_FILE)).toURI())
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
     * @throws IllegalArgumentException if there is a problem de/serializing the certificate
     */
    @Test
    public void testConstructCertFromNullByteArray()
            throws IllegalArgumentException {
        assertThrows(IllegalArgumentException.class, () ->
                new CertificateAuthorityCredential((byte[]) null));
    }

    /**
     * Ensure that a Certificate cannot be created from an empty byte array.
     *
     * @throws IllegalArgumentException if there is a problem de/serializing the certificate
     */
    @Test
    public void testConstructCertFromEmptyByteArray()
            throws IllegalArgumentException {
        assertThrows(IllegalArgumentException.class, () ->
                new CertificateAuthorityCredential(new byte[] {}));
    }

    /**
     * Tests that a certificate can be constructed from a path.
     *
     * @throws IOException        if there is a problem reading the cert file at the given path
     * @throws URISyntaxException if there is a problem constructing the URI
     */
    @Test
    public void testConstructCertFromPath() throws URISyntaxException, IOException {
        final Certificate certificate = new CertificateAuthorityCredential(
                Paths.get(Objects.requireNonNull(this.getClass().getResource(
                        FAKE_ROOT_CA_FILE)).toURI())
        );

        assertEquals(
                "CN=Fake Root CA",
                certificate.getX509Certificate().getIssuerX500Principal().getName()
        );
    }

    /**
     * Tests that a certificate cannot be constructed from a null path.
     *
     * @throws IllegalArgumentException if there is a problem constructing the URI
     */
    @Test
    public void testConstructCertFromNullPath() throws IllegalArgumentException {
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
                        TEST_PLATFORM_CERT_3).getCertificateType());
        assertEquals(
                Certificate.CertificateType.ATTRIBUTE_CERTIFICATE,
                getTestCertificate(
                        PlatformCredential.class,
                        TEST_PLATFORM_CERT_3).getCertificateType());
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

        assertEquals(
                Certificate.CertificateType.ATTRIBUTE_CERTIFICATE,
                platformCredential.getCertificateType());
        assertEquals(
                "GETY421001GV",
                ((PlatformCredential) platformCredential).getPlatformSerial()
        );

        platformCredential = getTestCertificate(
                PlatformCredential.class, TEST_PLATFORM_CERT_5
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
        final Certificate rootCert = getTestCertificate(FAKE_ROOT_CA_FILE);
        final X509Certificate certificate = readX509Certificate(FAKE_ROOT_CA_FILE);

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
     * Tests that Certificate correctly parses out non-standard fields from an X509 Certificate.
     *
     * @throws IOException if there is a problem reading the cert file at the given path
     */
    @Test
    public void testX509CertificateParsingExtended() throws IOException {
        Certificate rootCert = getTestCertificate(INTEL_INT_CA_FILE);

        final String expectedAuthorityInfo =
                "https://trustedservices.intel.com/content/TSC/certs/TSC_SS_RootCA_Certificate.cer";
        assertEquals(expectedAuthorityInfo, rootCert.getAuthorityInfoAccess().trim());
        assertEquals(
                "b56f72cdfd66ce839e1fdb40498f07291f5b99b7", rootCert.getAuthorityKeyIdentifier());
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
        final Certificate platformCert = getTestCertificate(PlatformCredential.class,
                TEST_PLATFORM_CERT_3
        );

        final X509AttributeCertificateHolder attrCertHolder = new X509AttributeCertificateHolder(
                Files.readAllBytes(Paths.get(Objects.requireNonNull(this.getClass().getResource(
                        TEST_PLATFORM_CERT_3)).toURI()))
        );

        assertEquals(
                attrCertHolder.getSerialNumber(),
                platformCert.getSerialNumber()
        );
        assertEquals(
                attrCertHolder.getIssuer().getNames()[0].toString(),
                platformCert.getIssuer()
        );
        assertNull(platformCert.getSubject());
        assertArrayEquals(null, platformCert.getEncodedPublicKey());
        assertArrayEquals(attrCertHolder.getSignature(), platformCert.getSignature());
        assertEquals(attrCertHolder.getNotBefore(), platformCert.getBeginValidity());
        assertEquals(attrCertHolder.getNotAfter(), platformCert.getEndValidity());
    }

    /**
     * Tests that Certificate correctly parses out non-standard fields from an X509 attribute
     * certificate.
     *
     * @throws IOException if there is a problem reading the cert file at the given path
     */
    @Test
    public void testX509AttributeCertificateParsingExtended()
            throws IOException {
        final Certificate platformCert = getTestCertificate(
                PlatformCredential.class, TEST_PLATFORM_CERT_6);

        final String expectedAuthorityInfo =
                "https://trustedservices.intel.com/content/TSC/certs/TSC_IssuingCAIKGF_TEST.cer";
        assertEquals(expectedAuthorityInfo, platformCert.getAuthorityInfoAccess().trim());
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
        final byte[] rawFileBytes = Files.readAllBytes(Paths.get(Objects.requireNonNull(CertificateTest.class
                .getResource(EK_CERT_WITH_PADDED_BYTES)).toURI()));

        final int finalPosition = 908;
        final byte[] expectedCertBytes = Arrays.copyOfRange(rawFileBytes, 0, finalPosition);

        final Certificate ekCert = getTestCertificate(EndorsementCredential.class,
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
        final byte[] rawFileBytes = Files.readAllBytes(Paths.get(Objects.requireNonNull(CertificateTest.class
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
        final byte[] rawFileBytes = Files.readAllBytes(Paths.get(Objects.requireNonNull(CertificateTest.class
                .getResource(EK_CERT_WITH_PADDED_BYTES)).toURI()));

        final int finalPosition = 4;
        assertThrows(IllegalArgumentException.class, () ->
                        new EndorsementCredential(Arrays.copyOfRange(rawFileBytes, 0, finalPosition)),
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
        final byte[] rawFileBytes = Files.readAllBytes(Paths.get(Objects.requireNonNull(CertificateTest.class
                .getResource(EK_CERT_WITH_PADDED_BYTES)).toURI()));

        final int finalPosition = 42;
        assertThrows(IllegalArgumentException.class, () ->
                        new EndorsementCredential(Arrays.copyOfRange(rawFileBytes, 0, finalPosition)),
                ".* Value of certificate length field extends beyond"
                        + " length of provided certificate\\.");
    }

    /**
     * Tests that the equals method on {@link Certificate} works as expected.
     *
     * @throws IOException        if the certificate could not be constructed properly
     * @throws URISyntaxException if there is a problem constructing the path to the certificate
     */
    @Test
    public void testEquals() throws IOException, URISyntaxException {
        assertEquals(
                getTestCertificate(FAKE_ROOT_CA_FILE),
                getTestCertificate(FAKE_ROOT_CA_FILE)
        );

        assertEquals(
                new CertificateAuthorityCredential(
                        Paths.get(Objects.requireNonNull(this.getClass().getResource(
                                FAKE_ROOT_CA_FILE)).toURI())
                ),
                new CertificateAuthorityCredential(
                        Files.readAllBytes(
                                Paths.get(Objects.requireNonNull(this.getClass().getResource(
                                        FAKE_ROOT_CA_FILE)).toURI())
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
     * @throws IOException if the certificate could not be constructed properly
     */
    @Test
    public void testIsIssuer() throws IOException {
        final Certificate issuerCert = getTestCertificate(FAKE_ROOT_CA_FILE);
        final Certificate cert = getTestCertificate(INT_CA_CERT02);

        assertEquals("Certificate signature failed to verify", issuerCert.isIssuer(cert));
        assertTrue(cert.isIssuer(issuerCert).isEmpty());
    }

    /**
     * Tests that the hashCode method on {@link Certificate} works as expected.
     *
     * @throws IOException        if the certificate could not be constructed properly
     * @throws URISyntaxException if there is a problem constructing the path to the certificate
     */
    @Test
    public void testHashCode() throws IOException, URISyntaxException {
        assertEquals(
                getTestCertificate(FAKE_ROOT_CA_FILE).hashCode(),
                getTestCertificate(FAKE_ROOT_CA_FILE).hashCode()
        );

        assertEquals(
                new CertificateAuthorityCredential(
                        Paths.get(Objects.requireNonNull(this.getClass().getResource(
                                FAKE_ROOT_CA_FILE)).toURI())
                ).hashCode(),
                new CertificateAuthorityCredential(
                        Files.readAllBytes(
                                Paths.get(Objects.requireNonNull(this.getClass().getResource(
                                        FAKE_ROOT_CA_FILE)).toURI())
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
}
