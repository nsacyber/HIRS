package hirs.attestationca.persist.util;

import hirs.attestationca.persist.entity.userdefined.certificate.CertificateAuthorityCredential;
import org.bouncycastle.util.io.pem.PemReader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests methods in the {@link CredentialHelper} utility class.
 */
public class CredentialHelperTest {

    private static final String DN_WITH_ESCAPED_COMMA =
            "CN=PCTEST\\, FANCY.,L=Here,ST=There,C=US";

    private static final String INTEL_INTERMEDIATE_CA = "/certificates/fakeIntelIntermediateCA.pem";

    private static final String SGI_INTERMEDIATE_CA = "/certificates/fakeSGIIntermediateCA.pem";

    private static final String PLATFORM_CERT =
            "/validation/platform_credentials_2/basic_plat_cert.pem";

    private static final String LEADING_TEXT =
            "Certificate:\n    Data:\n        Version: 3 (0x2)\n        Serial Number: 1\n";

    /**
     * Tests that sorting a DN preserves escaped commas inside RDN values.
     */
    @Test
    public void testParseSortDNsPreservesEscapedComma() {
        String sortedDn = CredentialHelper.parseSortDNs(DN_WITH_ESCAPED_COMMA);

        assertEquals("c=us,cn=pctest\\, fancy.,l=here,st=there", sortedDn);
    }

    /**
     * Tests that a single PEM certificate is decoded.
     *
     * @throws IOException        if a test resource cannot be read
     * @throws URISyntaxException if a test resource path cannot be built
     */
    @Test
    public void testStripPemHeaderFooterSingleCertificate() throws IOException, URISyntaxException {
        String pem = readResource(INTEL_INTERMEDIATE_CA);

        assertArrayEquals(decodeFirstPem(pem), CredentialHelper.stripPemHeaderFooter(pem));
    }

    /**
     * Tests that only the first certificate is decoded when a PEM file contains several X.509
     * certificates (for example, a certificate followed by its chain).
     *
     * @throws IOException        if a test resource cannot be read
     * @throws URISyntaxException if a test resource path cannot be built
     */
    @Test
    public void testStripPemHeaderFooterMultipleCertificates() throws IOException, URISyntaxException {
        String first = readResource(INTEL_INTERMEDIATE_CA);
        String second = readResource(SGI_INTERMEDIATE_CA);

        assertArrayEquals(decodeFirstPem(first), CredentialHelper.stripPemHeaderFooter(first + second));
        assertArrayEquals(decodeFirstPem(second), CredentialHelper.stripPemHeaderFooter(second + first));
    }

    /**
     * Tests that text before and after the PEM block is ignored.
     *
     * @throws IOException        if a test resource cannot be read
     * @throws URISyntaxException if a test resource path cannot be built
     */
    @Test
    public void testStripPemHeaderFooterSurroundingText() throws IOException, URISyntaxException {
        String cert = readResource(INTEL_INTERMEDIATE_CA);
        String platformCert = readResource(PLATFORM_CERT);

        assertArrayEquals(decodeFirstPem(cert),
                CredentialHelper.stripPemHeaderFooter(LEADING_TEXT + cert + "\ntrailing text\n"));
        assertArrayEquals(decodeFirstPem(platformCert),
                CredentialHelper.stripPemHeaderFooter(LEADING_TEXT + platformCert + "\ntrailing text\n"));
    }

    /**
     * Tests that an attribute certificate alone is decoded.
     *
     * @throws IOException        if a test resource cannot be read
     * @throws URISyntaxException if a test resource path cannot be built
     */
    @Test
    public void testStripPemHeaderFooterAttributeCertificate() throws IOException, URISyntaxException {
        String platformCert = readResource(PLATFORM_CERT);

        assertArrayEquals(decodeFirstPem(platformCert), CredentialHelper.stripPemHeaderFooter(platformCert));
    }

    /**
     * Tests that an X.509 certificate is preferred over an attribute certificate in the same
     * file, regardless of order. Platform credentials isolate the attribute certificate first
     * with {@link CredentialHelper#isolateAttributeCertificatePem(byte[])}.
     *
     * @throws IOException        if a test resource cannot be read
     * @throws URISyntaxException if a test resource path cannot be built
     */
    @Test
    public void testStripPemHeaderFooterPrefersX509OverAttributeCertificate()
            throws IOException, URISyntaxException {
        String cert = readResource(INTEL_INTERMEDIATE_CA);
        String platformCert = readResource(PLATFORM_CERT);

        assertArrayEquals(decodeFirstPem(cert), CredentialHelper.stripPemHeaderFooter(platformCert + cert));
        assertArrayEquals(decodeFirstPem(cert), CredentialHelper.stripPemHeaderFooter(cert + platformCert));
    }

    /**
     * Tests that isolating the attribute certificate works regardless of the order of the blocks
     * and ignores surrounding text.
     *
     * @throws IOException        if a test resource cannot be read
     * @throws URISyntaxException if a test resource path cannot be built
     */
    @Test
    public void testIsolateAttributeCertificatePem() throws IOException, URISyntaxException {
        String cert = readResource(INTEL_INTERMEDIATE_CA);
        String platformCert = readResource(PLATFORM_CERT);
        byte[] expected = decodeFirstPem(platformCert);

        for (String pem : new String[]{platformCert + cert, cert + platformCert, LEADING_TEXT + platformCert}) {
            byte[] isolated = CredentialHelper.isolateAttributeCertificatePem(
                    pem.getBytes(StandardCharsets.UTF_8));
            assertArrayEquals(expected,
                    CredentialHelper.stripPemHeaderFooter(new String(isolated, StandardCharsets.UTF_8)));
        }

        byte[] noAttributeCert = cert.getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(noAttributeCert, CredentialHelper.isolateAttributeCertificatePem(noAttributeCert));
    }

    /**
     * Tests that a certificate entity (not only a platform credential) can be built from a PEM
     * file containing several certificates; the first certificate is used.
     *
     * @throws IOException        if a test resource cannot be read
     * @throws URISyntaxException if a test resource path cannot be built
     */
    @Test
    public void testCertificateFromMultiplePemCertificates() throws IOException, URISyntaxException {
        String first = readResource(INTEL_INTERMEDIATE_CA);
        String second = readResource(SGI_INTERMEDIATE_CA);

        CertificateAuthorityCredential expected =
                new CertificateAuthorityCredential(first.getBytes(StandardCharsets.UTF_8));
        CertificateAuthorityCredential actual =
                new CertificateAuthorityCredential((first + second).getBytes(StandardCharsets.UTF_8));

        assertEquals(expected.getSubject(), actual.getSubject());
        assertEquals(expected.getSerialNumber(), actual.getSerialNumber());
        assertArrayEquals(expected.getRawBytes(), actual.getRawBytes());
    }

    private String readResource(final String resource) throws IOException, URISyntaxException {
        return new String(Files.readAllBytes(Paths.get(getClass().getResource(resource).toURI())),
                StandardCharsets.UTF_8);
    }

    private static byte[] decodeFirstPem(final String pem) throws IOException {
        try (PemReader reader = new PemReader(new StringReader(pem))) {
            return reader.readPemObject().getContent();
        }
    }
}
