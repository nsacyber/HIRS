package hirs.attestationca.persist.validation;

import com.google.protobuf.ByteString;
import hirs.attestationca.configuration.provisionerTpm2.ProvisionerTpm2;
import hirs.attestationca.persist.entity.userdefined.certificate.ComponentEvidenceCheck;
import hirs.attestationca.persist.entity.userdefined.certificate.ComponentEvidenceResult;
import hirs.attestationca.persist.entity.userdefined.certificate.PlatformCredential;
import hirs.attestationca.persist.entity.userdefined.certificate.attributes.V2.ComponentIdentifierV2;
import hirs.attestationca.persist.provision.helper.EpscBootlogEvidence;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks HP EpSC evidence captured from an HP ZBook against a paccor platform certificate (v1.1) that lists the EpSC.
 */
public class ComponentEvidenceValidatorTest {
    private static final String RESOURCES = "/validation/epsc/";
    private static final String PLATFORM_UUID = "01b5ed98-a5d0-439e-a88b-b8e15625e167";
    // SMBIOS byte order: the first three fields are little endian
    private static final byte[] PLATFORM_UUID_BYTES = Hex.decode("98edb501d0a59e43a88bb8e15625e167");
    private static final byte[] OTHER_UUID_BYTES = Hex.decode("98edb501d0a59e43a88bb8e15625e168");
    private static final int CHECK_COUNT = 9;
    private static final int MEASUREMENT_COUNT = 8;
    private static final int SIGNED_ECR0_BYTE = 20;

    private static byte[] evidence;
    private static byte[] deviceId;
    private static byte[] attestation;
    private static byte[] nonce;
    private static byte[] platformCertIdentifier;

    /**
     * Loads the sample and reads the EpSC component's certificate identifier from the platform certificate.
     *
     * @throws Exception if a resource cannot be read
     */
    @BeforeAll
    public static void loadSample() throws Exception {
        evidence = read("evidence.bin");
        deviceId = read("deviceid.cer");
        attestation = read("attestation.cer");
        nonce = Hex.decode(new String(read("nonce.hex"), StandardCharsets.US_ASCII).trim());
        PlatformCredential platformCredential = new PlatformCredential(resource("platform_cert.pem"));
        ComponentIdentifierV2 epsc = platformCredential.getPlatformConfigurationV2().getComponentIdentifiers()
                .stream()
                .filter(component -> component.getComponentPlatformCert() != null)
                .findFirst()
                .orElseThrow();
        assertEquals("HP EpSC 24", epsc.getComponentModel().toString());
        assertEquals("2.16.840.1.101.3.4.2.2", epsc.getComponentPlatformCert().getHashAlgorithm());
        platformCertIdentifier = epsc.getComponentPlatformCert().getHashOverSignatureValue();
    }

    /**
     * Evidence from the device passes every check.
     */
    @Test
    public void testSampleEvidencePasses() {
        ComponentEvidenceResult result = validate(evidence, List.of(attestation, deviceId), nonce,
                PLATFORM_UUID_BYTES);
        assertTrue(result.isPassed(), "Failed: " + failedChecks(result));
        assertEquals(CHECK_COUNT, result.getChecks().size());
        assertArrayEquals(deviceId, result.getDeviceIdCertificate());
    }

    /**
     * The platform certificate identifies the Device ID certificate by SHA-384 over its signature value.
     */
    @Test
    public void testPlatformCertificateIdentifiesDeviceId() throws Exception {
        X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(deviceId));
        assertArrayEquals(ComponentEvidenceValidator.sha384(certificate.getSignature()), platformCertIdentifier);
    }

    /**
     * The ACA verifies the platform certificate's ML-DSA-87 signature with the test CA, as in platform credential
     * validation.
     *
     * @throws Exception if a resource cannot be read
     */
    @Test
    public void testPlatformCertificateSignedByTestCa() throws Exception {
        PlatformCredential platformCredential = new PlatformCredential(resource("platform_cert.pem"));
        X509Certificate testCa = (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(read("test_ca_mldsa87.pem")));
        assertTrue(SupplyChainCredentialValidator.signatureMatchesPublicKey(
                platformCredential.getX509AttributeCertificateHolder(), testCa));
    }

    /**
     * Evidence over another nonce fails only the nonce check.
     */
    @Test
    public void testOtherNonceFails() {
        byte[] otherNonce = nonce.clone();
        otherNonce[0] ^= 1;
        ComponentEvidenceResult result = validate(evidence, List.of(attestation, deviceId), otherNonce,
                PLATFORM_UUID_BYTES);
        assertEquals(List.of("Evidence contains the ACA nonce"), failedChecks(result));
    }

    /**
     * Evidence from another platform fails only the UUID check.
     */
    @Test
    public void testOtherPlatformFails() {
        ComponentEvidenceResult result = validate(evidence, List.of(attestation, deviceId), nonce,
                OTHER_UUID_BYTES);
        assertEquals(List.of("Evidence UUID matches the platform UUID"), failedChecks(result));
    }

    /**
     * Changed measurements break the evidence signature.
     */
    @Test
    public void testTamperedEvidenceFails() {
        byte[] tampered = evidence.clone();
        tampered[SIGNED_ECR0_BYTE] ^= 1;
        ComponentEvidenceResult result = validate(tampered, List.of(attestation, deviceId), nonce,
                PLATFORM_UUID_BYTES);
        assertEquals(List.of("Attestation key signed the evidence"), failedChecks(result));
    }

    /**
     * Evidence without its certificate chain cannot be verified.
     */
    @Test
    public void testMissingChainFails() {
        ComponentEvidenceResult result = validate(evidence, List.of(attestation), nonce, PLATFORM_UUID_BYTES);
        assertFalse(result.isPassed());
        assertEquals(List.of("Certificate chain [Attestation, Device ID]"), failedChecks(result));
    }

    /**
     * The evidence parser reads the UUID in SMBIOS byte order.
     */
    @Test
    public void testEvidenceUuid() {
        EpscBootlogEvidence parsed = EpscBootlogEvidence.parse(evidence).orElseThrow();
        assertEquals(PLATFORM_UUID, EpscBootlogEvidence.formatSmbiosUuid(parsed.uuid()));
        assertArrayEquals(nonce, parsed.nonce());
        assertEquals(MEASUREMENT_COUNT, parsed.measurements().size());
    }

    private static ComponentEvidenceResult validate(final byte[] rawEvidence, final List<byte[]> chain,
                                                    final byte[] issuedNonce, final byte[] platformUuid) {
        ComponentEvidenceResult result = new ComponentEvidenceResult(null, null, "test",
                ProvisionerTpm2.EvidenceFormat.HP_EPSC_BOOTLOG.name(), new byte[0], issuedNonce);
        ProvisionerTpm2.ExternalEvidenceResult answer = ProvisionerTpm2.ExternalEvidenceResult.newBuilder()
                .setEvidenceFormat(ProvisionerTpm2.EvidenceFormat.HP_EPSC_BOOTLOG)
                .setEvidenceVersion("1")
                .setEvidence(ByteString.copyFrom(rawEvidence))
                .addAllCertificateChain(chain.stream().map(ByteString::copyFrom).toList())
                .build();
        ComponentEvidenceValidator.validateEpscBootlog(result, platformCertIdentifier, platformUuid, answer);
        return result;
    }

    private static List<String> failedChecks(final ComponentEvidenceResult result) {
        return result.getChecks().stream()
                .filter(check -> !check.isPassed())
                .map(ComponentEvidenceCheck::getName)
                .toList();
    }

    private static Path resource(final String name) throws URISyntaxException {
        return Paths.get(ComponentEvidenceValidatorTest.class.getResource(RESOURCES + name).toURI());
    }

    private static byte[] read(final String name) throws IOException, URISyntaxException {
        return Files.readAllBytes(resource(name));
    }
}
