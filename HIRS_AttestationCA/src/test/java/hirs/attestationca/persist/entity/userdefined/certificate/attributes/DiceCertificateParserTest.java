package hirs.attestationca.persist.entity.userdefined.certificate.attributes;

import hirs.attestationca.persist.entity.userdefined.certificate.IDevIDCertificate;
import hirs.utils.dice.DiceTcbInfoParser;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.X509CertificateHolder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests DICE recognition alongside the shared TCB evidence decoder.
 */
public class DiceCertificateParserTest {
    /**
     * Recognizes TCB evidence without EKUs and populates transient fields on initial import.
     * @throws Exception if the Caliptra fixture cannot be read
     */
    @Test
    public void testCaliptraEvidenceAndInitialImport() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/dice/caliptra/golden_cert_p384.pem")) {
            assertNotNull(input);
            byte[] contents = input.readAllBytes();
            IDevIDCertificate imported = new IDevIDCertificate(contents);
            DiceCertificateInfo info = imported.getDiceCertificateInfo();
            assertNotNull(info);
            assertEquals(DiceProfileType.UNKNOWN, info.getProfileType());
            assertEquals(Set.of(), info.getDiceKeyPurposes());
            assertEquals(1, info.getTcbInfo().size());
            assertEquals(DiceTcbInfoParser.MULTI_TCB_INFO_OID, info.getTcbInfo().get(0).getExtensionOid());
        }
    }

    /**
     * Leaves ordinary certificates unclassified and preserves combined recognized DICE key purposes.
     * @throws Exception if a mock certificate cannot be parsed
     */
    @Test
    public void testRecognitionAndExistingProfiles() throws Exception {
        X509Certificate certificate = mock(X509Certificate.class);
        assertNull(DiceCertificateParser.parse(certificate));
        when(certificate.getExtendedKeyUsage()).thenReturn(List.of("1.3.6.1.5.5.7.3.1"));
        assertNull(DiceCertificateParser.parse(certificate));
        when(certificate.getExtendedKeyUsage()).thenReturn(List.of(DiceKeyPurpose.ATTEST_LOC.getOid(),
                DiceKeyPurpose.IDENTITY_LOC.getOid()));
        DiceCertificateInfo info = DiceCertificateParser.parse(certificate);
        assertEquals(DiceProfileType.ATTESTATION, info.getProfileType());
        assertEquals(Set.of(DiceKeyPurpose.ATTEST_LOC, DiceKeyPurpose.IDENTITY_LOC), info.getDiceKeyPurposes());
        assertEquals(List.of(), info.getTcbInfo());
    }

    /**
     * Propagates malformed TCB evidence rather than returning a successful certificate classification.
     * @throws Exception if test input cannot be encoded
     */
    @Test
    public void testMalformedEvidenceFails() throws Exception {
        X509Certificate certificate = mock(X509Certificate.class);
        when(certificate.getExtendedKeyUsage()).thenReturn(List.of(DiceKeyPurpose.ATTEST_LOC.getOid()));
        when(certificate.getExtensionValue(DiceTcbInfoParser.MULTI_TCB_INFO_OID))
                .thenReturn(new DEROctetString(HexFormat.of().parseHex("3000")).getEncoded());
        assertThrows(IOException.class, () -> DiceCertificateParser.parse(certificate));
    }

    /**
     * Keeps malformed evidence inspectable on import instead of making persistence reload fail.
     * The altered certificate's old signature is intentionally not valid; this test only checks decoding behavior.
     * @throws Exception if the fixture cannot be read or rewritten
     */
    @Test
    public void testMalformedEvidenceRemainsInspectable() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/dice/caliptra/golden_cert_p256.pem")) {
            assertNotNull(input);
            IDevIDCertificate original = new IDevIDCertificate(input.readAllBytes());
            X509CertificateHolder holder = new X509CertificateHolder(original.getX509Certificate().getEncoded());
            Extensions extensions = holder.getExtensions();
            List<Extension> altered = new ArrayList<>();
            for (var oid : extensions.getExtensionOIDs()) {
                altered.add(oid.getId().equals(DiceTcbInfoParser.MULTI_TCB_INFO_OID)
                        ? new Extension(oid, false, new DEROctetString(new DERSequence().getEncoded()))
                        : extensions.getExtension(oid));
            }
            ASN1Sequence tbs = ASN1Sequence.getInstance(holder.toASN1Structure().getTBSCertificate().toASN1Primitive());
            ASN1Encodable[] fields = tbs.toArray();
            final int extensionsTag = 3;
            fields[fields.length - 1] = new DERTaggedObject(true, extensionsTag,
                    new Extensions(altered.toArray(Extension[]::new)));
            byte[] changed = new DERSequence(new ASN1Encodable[]{new DERSequence(fields),
                    holder.toASN1Structure().getSignatureAlgorithm(), holder.toASN1Structure().getSignature()})
                    .getEncoded();
            IDevIDCertificate imported = new IDevIDCertificate(changed);
            assertNull(imported.getDiceCertificateInfo());
            assertNotNull(imported.getDiceParsingError());
            assertThrows(IOException.class, () -> DiceCertificateParser.parse(imported.getX509Certificate()));
        }
    }
}
