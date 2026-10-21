package hirs.attestationca.persist.provision.helper;

import org.bouncycastle.util.encoders.Hex;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * HP Endpoint Security Controller (EpSC) bootlog evidence.
 * The layout comes from analysis of sample evidence, not from an HP specification:
 * <pre>
 * version (1) | ECR0 record | ECR1 record | nonce (32) | attestation certificate digest (48) | system UUID (16)
 *   | ECDSA P-384 signature r||s (96)
 * </pre>
 * Each record starts with a 4 byte tag and a 2 byte little endian size that includes the tag and size.
 * The signature covers every byte before it.
 *
 * @param version               evidence version
 * @param ecr0                  ECR0 record, including its header
 * @param ecr1                  ECR1 record, including its header
 * @param nonce                 the verifier's nonce
 * @param attestationCertDigest SHA-384 over the whole Attestation certificate
 * @param uuid                  system UUID in SMBIOS byte order
 * @param signedData            every byte before the signature
 * @param signature             ECDSA P-384 signature, r||s
 */
public record EpscBootlogEvidence(int version, byte[] ecr0, byte[] ecr1, byte[] nonce, byte[] attestationCertDigest,
                                  byte[] uuid, byte[] signedData, byte[] signature) {
    /**
     * The EpSC requires a nonce of exactly this size.
     */
    public static final int NONCE_SIZE = 32;

    private static final int SUPPORTED_VERSION = 1;
    private static final int SHA384_SIZE = 48;
    private static final int SHA512_SIZE = 64;
    private static final int UUID_SIZE = 16;
    private static final int SIGNATURE_SIZE = 96;
    private static final int RECORD_HEADER_SIZE = 6;
    private static final int TAG_SIZE = 4;
    private static final int TRAILER_SIZE = NONCE_SIZE + SHA384_SIZE + UUID_SIZE + SIGNATURE_SIZE;
    private static final int ECR0_ROM_HASH_START = RECORD_HEADER_SIZE + 2; // version, life cycle
    // version, auth config, header version, 3 reserved bytes
    private static final int ECR1_DEVICE_ID_HASH_START = RECORD_HEADER_SIZE + 6;
    private static final int UUID_CLOCK_SEQ_START = 8;
    private static final int UUID_CLOCK_SEQ_SIZE = 2;
    private static final int UUID_NODE_START = 10;
    private static final int UUID_NODE_SIZE = 6;
    private static final int UINT16_MASK = 0xFFFF;
    private static final byte[] ECR0_TAG = {0x4B, (byte) 0xA5, (byte) 0xF0, (byte) 0xE9};
    private static final byte[] ECR1_TAG = {0x5C, 0x36, 0x35, 0x39};
    private static final String[] ECR1_DIGESTS = {"Device ID certificate", "L1 firmware", "RSA key 1", "RSA key 2",
            "HSS key 1", "HSS key 2"};

    /**
     * Parses evidence as returned by the EpSC.
     *
     * @param evidence raw evidence
     * @return the parsed evidence, or empty if it does not have the expected layout
     */
    public static Optional<EpscBootlogEvidence> parse(final byte[] evidence) {
        if (evidence == null || evidence.length == 0 || evidence[0] != SUPPORTED_VERSION) {
            return Optional.empty();
        }
        int ecr1Start = 1 + recordSize(evidence, 1, ECR0_TAG);
        int trailerStart = ecr1Start + recordSize(evidence, ecr1Start, ECR1_TAG);
        if (ecr1Start == 1 || trailerStart == ecr1Start
                || trailerStart - ecr1Start < ECR1_DEVICE_ID_HASH_START + SHA384_SIZE
                || evidence.length - trailerStart != TRAILER_SIZE) {
            return Optional.empty();
        }
        int digestStart = trailerStart + NONCE_SIZE;
        int uuidStart = digestStart + SHA384_SIZE;
        int signatureStart = uuidStart + UUID_SIZE;
        return Optional.of(new EpscBootlogEvidence(evidence[0],
                Arrays.copyOfRange(evidence, 1, ecr1Start),
                Arrays.copyOfRange(evidence, ecr1Start, trailerStart),
                Arrays.copyOfRange(evidence, trailerStart, digestStart),
                Arrays.copyOfRange(evidence, digestStart, uuidStart),
                Arrays.copyOfRange(evidence, uuidStart, signatureStart),
                Arrays.copyOfRange(evidence, 0, signatureStart),
                Arrays.copyOfRange(evidence, signatureStart, evidence.length)));
    }

    /**
     * Formats a UUID given in SMBIOS byte order, where the first three fields are little endian.
     *
     * @param smbiosUuid 16 bytes
     * @return the UUID in its usual text form, or hex if it is not 16 bytes
     */
    public static String formatSmbiosUuid(final byte[] smbiosUuid) {
        if (smbiosUuid == null || smbiosUuid.length != UUID_SIZE) {
            return smbiosUuid == null ? "" : Hex.toHexString(smbiosUuid);
        }
        ByteBuffer buffer = ByteBuffer.wrap(smbiosUuid).order(ByteOrder.LITTLE_ENDIAN);
        return String.format("%08x-%04x-%04x-%s-%s", buffer.getInt(), buffer.getShort(), buffer.getShort(),
                Hex.toHexString(smbiosUuid, UUID_CLOCK_SEQ_START, UUID_CLOCK_SEQ_SIZE),
                Hex.toHexString(smbiosUuid, UUID_NODE_START, UUID_NODE_SIZE));
    }

    /**
     * The Device ID certificate digest in ECR1: SHA-384 over the whole Device ID certificate.
     *
     * @return the digest
     */
    public byte[] deviceIdCertHash() {
        return Arrays.copyOfRange(ecr1, ECR1_DEVICE_ID_HASH_START, ECR1_DEVICE_ID_HASH_START + SHA384_SIZE);
    }

    /**
     * Measurements reported in ECR0 and ECR1, for display or comparison with reference values.
     *
     * @return measurement name to hex digest, in evidence order
     */
    public Map<String, String> measurements() {
        Map<String, String> measurements = new LinkedHashMap<>();
        measurements.put("ROM firmware", hex(ecr0, ECR0_ROM_HASH_START, SHA512_SIZE));
        measurements.put("L0 firmware", hex(ecr0, ECR0_ROM_HASH_START + SHA512_SIZE, SHA512_SIZE));
        for (int i = 0; i < ECR1_DIGESTS.length; i++) {
            measurements.put(ECR1_DIGESTS[i], hex(ecr1, ECR1_DEVICE_ID_HASH_START + i * SHA384_SIZE, SHA384_SIZE));
        }
        return measurements;
    }

    private static String hex(final byte[] record, final int start, final int length) {
        return record.length < start + length ? "" : Hex.toHexString(record, start, length);
    }

    /**
     * @return the record size, or 0 if no record with the tag starts at pos
     */
    private static int recordSize(final byte[] evidence, final int pos, final byte[] tag) {
        if (evidence.length - pos < RECORD_HEADER_SIZE
                || !Arrays.equals(evidence, pos, pos + TAG_SIZE, tag, 0, TAG_SIZE)) {
            return 0;
        }
        int size = ByteBuffer.wrap(evidence, pos + TAG_SIZE, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & UINT16_MASK;
        return size < RECORD_HEADER_SIZE || evidence.length - pos < size ? 0 : size;
    }
}
