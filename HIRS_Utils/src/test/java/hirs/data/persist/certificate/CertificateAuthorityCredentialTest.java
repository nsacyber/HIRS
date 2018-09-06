package hirs.data.persist.certificate;

import org.apache.commons.codec.binary.Hex;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import hirs.persist.CertificateManager;

/**
 * Tests that CertificateAuthorityCredential properly parses its fields.
 */
public class CertificateAuthorityCredentialTest {
    private static final CertificateManager CERT_MAN = Mockito.mock(CertificateManager.class);

    /**
     * Tests that a CertificateAuthorityCredential can be created from an X.509 certificate and
     * that the subject key identifier is correctly extracted.
     *
     * @throws IOException if the certificate could not be constructed properly
     * @throws CertificateException if there is a problem with the KeyStore or de/serializing the
     *                              certificate
     * @throws URISyntaxException if there is a problem constructing the path to the certificate
     */
    @Test
    public void testGetSubjectKeyIdentifier()
            throws CertificateException, IOException, URISyntaxException {
        Path testCertPath = Paths.get(
                this.getClass().getResource(CertificateTest.FAKE_ROOT_CA_FILE).toURI()
        );
        CertificateAuthorityCredential caCred = new CertificateAuthorityCredential(testCertPath);

        byte[] subjectKeyIdentifier = caCred.getSubjectKeyIdentifier();

        Assert.assertNotNull(subjectKeyIdentifier);
        Assert.assertEquals(
                Hex.encodeHexString(subjectKeyIdentifier),
                CertificateTest.FAKE_ROOT_CA_SUBJECT_KEY_IDENTIFIER_HEX
        );
    }

    /**
     * Test that a null subject key identifier cannot be set.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testSelectByNullSubjectKeyIdentifier() {
        CertificateAuthorityCredential.select(CERT_MAN).bySubjectKeyIdentifier(null);
    }

    /**
     * Test that an empty subject key identifier cannot be set.
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testSelectByEmptySubjectKeyIdentifier() {
        CertificateAuthorityCredential.select(CERT_MAN).bySubjectKeyIdentifier(new byte[]{});
    }
}
