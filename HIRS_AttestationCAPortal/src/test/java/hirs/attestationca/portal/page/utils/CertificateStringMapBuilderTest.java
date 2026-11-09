package hirs.attestationca.portal.page.utils;

import hirs.attestationca.persist.entity.userdefined.Certificate;
import hirs.attestationca.persist.entity.userdefined.certificate.IDevIDCertificate;
import hirs.utils.dice.DiceTcbInfoParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.math.BigInteger;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests DICE evidence inspection across X.509 certificate categories.
 */
public class CertificateStringMapBuilderTest {
    /**
     * Supports a general X.509 certificate without requiring the IDevID entity's transient fields.
     * @throws Exception if the Caliptra fixture cannot be loaded
     */
    @Test
    public void testDiceEvidenceOnGeneralCertificate() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/dice/caliptra/golden_cert_p256.pem")) {
            assertNotNull(input);
            X509Certificate x509 = (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(input);
            Map<String, String> data = CertificateStringMapBuilder.getGeneralCertificateInfo(
                    certificate(Certificate.class, x509), null, null, List.of());
            assertEquals("1", data.get("diceTcbInfoCount"));
            assertTrue(data.get("diceTcbInfo").contains("DICE Extension: Multi TCB Info\n"));
            assertFalse(data.containsKey("diceParsingError"));
        }
    }

    /**
     * Displays decoding failure rather than hiding malformed evidence or showing a partial record list.
     * @throws Exception if certificate display fails
     */
    @Test
    public void testMalformedEvidenceIsReported() throws Exception {
        X509Certificate x509 = mock(X509Certificate.class);
        when(x509.getExtensionValue(DiceTcbInfoParser.MULTI_TCB_INFO_OID)).thenReturn(new byte[0]);
        Map<String, String> data = CertificateStringMapBuilder.getGeneralCertificateInfo(
                certificate(Certificate.class, x509), null, null, List.of());
        assertTrue(data.get("diceParsingError").contains(DiceTcbInfoParser.MULTI_TCB_INFO_OID));
        assertFalse(data.containsKey("diceTcbInfo"));
    }

    /**
     * Preserves an IDevID decoding error retained by the persistence callback.
     * @throws Exception if certificate display fails
     */
    @Test
    public void testIDevIDDecodingErrorIsReported() throws Exception {
        IDevIDCertificate certificate = certificate(IDevIDCertificate.class, mock(X509Certificate.class));
        when(certificate.getDiceParsingError()).thenReturn("Unsupported DICE field tag: 12");
        Map<String, String> data = CertificateStringMapBuilder.getGeneralCertificateInfo(
                certificate, null, null, List.of());
        assertEquals("Unsupported DICE field tag: 12", data.get("diceParsingError"));
        assertFalse(data.containsKey("diceTcbInfo"));
    }

    /**
     * Supplies ordinary persisted-certificate metadata without assigning a special DICE entity category.
     * @param <T> the persisted certificate type
     * @param type the certificate entity class
     * @param x509 the underlying X.509 certificate
     * @return a certificate with self-issued metadata, so issuer lookup is unnecessary
     * @throws Exception if the mocked X.509 accessor fails
     */
    private static <T extends Certificate> T certificate(final Class<T> type, final X509Certificate x509)
            throws Exception {
        T certificate = mock(type);
        when(certificate.getX509Certificate()).thenReturn(x509);
        when(certificate.getIssuer()).thenReturn("CN=Test");
        when(certificate.getSubject()).thenReturn("CN=Test");
        when(certificate.getSerialNumber()).thenReturn(BigInteger.ZERO);
        when(certificate.getAuthoritySerialNumber()).thenReturn(BigInteger.ZERO);
        when(certificate.getBeginValidity()).thenReturn(new Date(0));
        when(certificate.getEndValidity()).thenReturn(new Date(0));
        return certificate;
    }
}
