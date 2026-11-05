package hirs.utils.dice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERUTF8String;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests fixed Caliptra certificates, independently specified DER, and invalid evidence boundaries.
 */
public class DiceTcbInfoParserTest {
    private static final String SHA256 = "2.16.840.1.101.3.4.2.1";
    private static final String SHA384 = "2.16.840.1.101.3.4.2.2";
    private static final int SHA256_LENGTH = 32;
    private static final int SHA384_LENGTH = 48;
    private static final int REGISTER_TAG = 11;
    private static final int FWIDS_TAG = 6;
    private static final int SVN_TAG = 3;
    private static final int LAYER_TAG = 4;
    private static final int INDEX_TAG = 5;
    private static final int FLAGS_TAG = 7;
    private static final int VENDOR_INFO_TAG = 8;
    private static final int TYPE_TAG = 9;
    private static final int FLAGS_MASK_TAG = 10;
    private static final int UNUSED_FLAG_BITS = 7;

    /**
     * Checks upstream golden certificates without using a HIRS evidence encoder or DICE EKUs.
     * @throws Exception if a fixture cannot be read or its signature cannot be verified
     */
    @Test
    public void testCaliptraGoldenCertificates() throws Exception {
        for (String profile : List.of("p256", "p384")) {
            try (InputStream input = getClass().getResourceAsStream("/dice/caliptra/golden_cert_" + profile + ".pem")) {
                assertNotNull(input);
                X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509")
                        .generateCertificate(input);
                cert.verify(cert.getPublicKey());
                assertNull(cert.getExtendedKeyUsage());
                List<DiceTcbInfo> records = DiceTcbInfoParser.parse(cert);
                assertEquals(1, records.size());
                DiceTcbInfo record = records.get(0);
                assertEquals(DiceTcbInfoParser.MULTI_TCB_INFO_OID, record.getExtensionOid());
                assertFalse(record.isCritical());
                assertEquals(0, record.getRecordIndex());
                assertNull(record.getVendor());
                assertNull(record.getSvn());
                assertNull(record.getLayer());
                assertNull(record.getIndex());
                assertNull(record.getFlags());
                assertNull(record.getFlagsMask());
                assertTrue(record.getIntegrityRegisters().isEmpty());
                assertEquals(1, record.getFwids().size());
                boolean p256 = profile.equals("p256");
                assertEquals(p256 ? SHA256 : SHA384, record.getFwids().get(0).getHashAlgorithmOid());
                assertArrayEquals(new byte[p256 ? SHA256_LENGTH : SHA384_LENGTH],
                        record.getFwids().get(0).getDigest());
            }
        }
    }

    /**
     * Preserves optional metadata, integer precision, unknown algorithm OIDs, masks, and named registers.
     * @throws Exception if test evidence cannot be encoded
     */
    @Test
    public void testMetadataAndCorrectedOptionalFields() throws Exception {
        final int integerBits = 80;
        BigInteger largeSvn = BigInteger.ONE.shiftLeft(integerBits);
        byte[] state = {1, 2};
        byte[] type = HexFormat.of().parseHex("01020304");
        DERBitString flags = new DERBitString(HexFormat.of().parseHex("80"), UNUSED_FLAG_BITS);
        DERSequence register = sequence(tag(0, new DERIA5String("journey")),
                tag(1, new ASN1Integer(0)), tag(2, sequence(fwid(SHA384, new byte[SHA384_LENGTH]))));
        DERSequence record = sequence(tag(0, new DERUTF8String("OEM")), tag(1, new DERUTF8String("Module")),
                tag(2, new DERUTF8String("1.0")), tag(SVN_TAG, new ASN1Integer(largeSvn)),
                tag(LAYER_TAG, new ASN1Integer(1)), tag(INDEX_TAG, new ASN1Integer(0)),
                tag(FWIDS_TAG, sequence(fwid(SHA256, new byte[SHA256_LENGTH]), fwid("1.2.3.4.5", state))),
                tag(FLAGS_TAG, flags), tag(VENDOR_INFO_TAG, new DEROctetString(state)),
                tag(TYPE_TAG, new DEROctetString(type)), tag(FLAGS_MASK_TAG, flags),
                tag(REGISTER_TAG, sequence(register)));
        DiceTcbInfo parsed = parseMulti(sequence(record).getEncoded()).get(0);
        assertEquals("OEM", parsed.getVendor());
        assertEquals("Module", parsed.getModel());
        assertEquals("1.0", parsed.getVersion());
        assertEquals(largeSvn, parsed.getSvn());
        assertEquals(BigInteger.ONE, parsed.getLayer());
        assertEquals(BigInteger.ZERO, parsed.getIndex());
        assertEquals("1.2.3.4.5", parsed.getFwids().get(1).getHashAlgorithmOid());
        assertArrayEquals(state, parsed.getFwids().get(1).getDigest());
        assertArrayEquals(type, parsed.getType());
        assertArrayEquals(state, parsed.getVendorInfo());
        assertEquals(UNUSED_FLAG_BITS, parsed.getFlags().getPadBits());
        assertEquals(parsed.getFlags(), parsed.getFlagsMask());
        DiceIntegrityRegister ir = parsed.getIntegrityRegisters().get(0);
        assertEquals("journey", ir.getRegisterName());
        assertEquals(BigInteger.ZERO, ir.getRegisterNum());
        assertEquals(SHA384, ir.getRegisterDigests().get(0).getHashAlgorithmOid());
        assertArrayEquals(new byte[SHA384_LENGTH], ir.getRegisterDigests().get(0).getDigest());
        assertTrue(parsed.toString().contains("Security version: " + largeSvn));
        assertTrue(parsed.toString().contains("Integrity register: name=journey"));

        // Certificate entities can also be returned through Jackson-backed portal endpoints.
        var json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(parsed));
        assertEquals(UNUSED_FLAG_BITS, json.get("flags").get("padBits").intValue());
        assertEquals("1.2.3.4.5", json.get("fwids").get(1).get("hashAlgorithmOid").textValue());

        parsed.getFlags().getBytes()[0] = 0;
        parsed.getType()[0] = 0;
        parsed.getVendorInfo()[0] = 0;
        parsed.getFwids().get(1).getDigest()[0] = 0;
        byte[] encoded = parsed.getEncoded();
        encoded[0] = 0;
        assertArrayEquals(type, parsed.getType());
        assertArrayEquals(state, parsed.getVendorInfo());
        assertArrayEquals(state, parsed.getFwids().get(1).getDigest());
        assertArrayEquals(record.getEncoded(), parsed.getEncoded());
        assertArrayEquals(HexFormat.of().parseHex("80"), parsed.getFlags().getBytes());

        assertThrows(UnsupportedOperationException.class, () -> parsed.getFwids().clear());
        assertThrows(UnsupportedOperationException.class, () -> parsed.getIntegrityRegisters().clear());
        assertThrows(UnsupportedOperationException.class, () -> ir.getRegisterDigests().clear());
    }

    /**
     * Supplies the OCTET STRING wrapper returned by the Java X.509 API.
     * @param contents the extension payload
     * @return parsed records
     * @throws IOException if decoding fails
     */
    private static List<DiceTcbInfo> parseMulti(final byte[] contents) throws IOException {
        X509Certificate certificate = mock(X509Certificate.class);
        when(certificate.getExtensionValue(DiceTcbInfoParser.MULTI_TCB_INFO_OID))
                .thenReturn(new DEROctetString(contents).getEncoded());
        when(certificate.getCriticalExtensionOIDs()).thenReturn(Set.of(DiceTcbInfoParser.MULTI_TCB_INFO_OID));
        return DiceTcbInfoParser.parse(certificate);
    }

    /**
     * Creates independent test structures.
     * @param values sequence contents
     * @return a DER sequence
     */
    private static DERSequence sequence(final ASN1Encodable... values) {
        return new DERSequence(values);
    }

    /**
     * Creates an implicitly tagged test field.
     * @param number tag number
     * @param value field value
     * @return a context-specific field
     */
    private static DERTaggedObject tag(final int number, final ASN1Encodable value) {
        return new DERTaggedObject(false, number, value);
    }

    /**
     * Creates a test FWID independently of the HIRS decoder.
     * @param oid algorithm OID
     * @param digest digest bytes
     * @return a FWID sequence
     */
    private static DERSequence fwid(final String oid, final byte[] digest) {
        return sequence(new ASN1ObjectIdentifier(oid), new DEROctetString(digest));
    }
}
