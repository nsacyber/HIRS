package hirs.attestationca.persist.entity.userdefined.certificate;

import hirs.attestationca.persist.entity.userdefined.CertificateTest;
import org.apache.commons.codec.binary.Hex;
import static org.mockito.Mockito.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import hirs.attestationca.persist.entity.manager.CertificateRepository;

/**
 * Tests that CertificateAuthorityCredential properly parses its fields.
 */
public class CertificateAuthorityCredentialTest {
    private static final CertificateRepository CERT_MAN = mock(CertificateRepository.class);

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

        assertNotNull(subjectKeyIdentifier);
        assertEquals(
                Hex.encodeHexString(subjectKeyIdentifier),
                CertificateTest.FAKE_ROOT_CA_SUBJECT_KEY_IDENTIFIER_HEX
        );
    }
}
