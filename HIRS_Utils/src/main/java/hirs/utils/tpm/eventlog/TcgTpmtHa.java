package hirs.utils.tpm.eventlog;

import hirs.utils.HexUtils;
import lombok.AccessLevel;
import lombok.Getter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;

/**
 * Class to for the TCG defined TPMT_HA structure used to support the Crypto Agile Log format.
 * <p>
 * typedef struct {
 * .     TPMI_ALG_HASH hashAlg;
 * .     TPMU_HA       digest;
 * } TPMT_HA;
 */
public class TcgTpmtHa {
    /** TCG ID for SHA1. */
    public static final int TPM_ALG_SHA1 = 0x04;
    /** TCG Name for SHA1. */
    public static final String TPM_ALG_SHA1_STR = "TPM_ALG_SHA1";
    /** TCG ID for SHA56. */
    public static final int TPM_ALG_SHA256 = 0x0B;
    /** TCG Name for SHA56. */
    public static final String TPM_ALG_SHA256_STR = "TPM_ALG_SHA256";
    /** TCG ID for SHA 384. */
    public static final int TPM_ALG_SHA384 = 0x0C;
    /** TCG ID for SHA 384. */
    public static final String  TPM_ALG_SHA384_STR = "TPM_ALG_SHA384";
    /** TCG ID for SHA512. */
    public static final int TPM_ALG_SHA512 = 0x0D;
    /** TCG ID for SHA512. */
    public static final String TPM_ALG_SHA512_STR = "TPM_ALG_SHA512";

    /**
     * TCG ID for Null algorithm.
     */
    public static final int TPM_ALG_NULL = 0x10;

    /**
     * TCG ID for SHA1.
     */
    public static final int TPM_ALG_SHA1_LENGTH = 20;

    /**
     * TCG ID for SHA1.
     */
    public static final int TPM_ALG_SHA256_LENGTH = 32;

    /**
     * TCG ID for SHA 384.
     */
    public static final int TPM_ALG_SHA384_LENGTH = 48;

    /**
     * TCG ID for SHA512.
     */
    public static final int TPM_ALG_SHA512_LENGTH = 64;

    /**
     * TCG ID for Null algorithm.
     */
    public static final int TPM_ALG_NULL_LENGTH = 0;

    /**
     * TCG Defined Algorithm Identifiers.
     */
    @Getter
    private int hashAlgId = 0;

    /**
     * Length of the  hash.
     */
    @Getter
    private int hashLength = 0;

    /**
     * Human-readable name of the hash algorithm.
     */
    @Getter
    private String hashName = "";

    /**
     * Hash data.
     */
    @Getter(value = AccessLevel.PROTECTED)
    private byte[] digest = null;

    /**
     * buffer to hold the structure.
     */
    private byte[] buffer = null;

    /**
     * Constructor.
     *
     * @param is ByteArrayInputStream holding the TcgTPMT_HA structured data
     * @throws java.io.IOException if TPMT_HA structure cannot be parsed
     */
    public TcgTpmtHa(final ByteArrayInputStream is) throws IOException {
        byte[] algID = new byte[2];
        is.read(algID);
        byte[] rAlgID = HexUtils.leReverseByte(algID);
        hashAlgId = new BigInteger(rAlgID).intValue();
        hashName = tcgAlgIdToString(algID[0]);
        hashLength = tcgAlgLength(algID[0]);
        digest = new byte[hashLength];
        is.read(digest);
        buffer = new byte[algID.length + digest.length];
        System.arraycopy(algID, 0, buffer, 0, algID.length);
        System.arraycopy(digest, 0, buffer, algID.length, digest.length);
    }

    /**
     * Returns the hash name via a lookup.
     * Lookup based upon section 6.3 for the TPM-Rev-2.0-Part-2-Structures.pdf document.
     * Only hash algorithms found in Table 7 are used.
     *
     * @param algId int to convert to string
     * @return name of the algorithm
     */
    public static String tcgAlgIdToString(final int algId) {
        return switch (algId) {
            case TPM_ALG_SHA1 -> TPM_ALG_SHA1_STR;
            case TPM_ALG_SHA256 -> TPM_ALG_SHA256_STR;
            case TPM_ALG_SHA384 -> TPM_ALG_SHA384_STR;
            case TPM_ALG_SHA512 -> TPM_ALG_SHA512_STR;
            case TPM_ALG_NULL -> "TPM_ALG_NULL";
            default -> "Unknown or invalid Hash";
        };
    }

    /**
     * Returns the TCG defined ID via a lookup o the TCG Defined Algorithm String.
     * Lookup based upon section 6.3 for the TPM-Rev-2.0-Part-2-Structures.pdf document.
     * Only hash algorithms found in Table 7 are used.
     *
     * @param algorithm String to convert to an id
     * @return id of hash algorithm
     */
    public static int tcgAlgStringToId(final String algorithm) {
        return switch (algorithm) {
            case "TPM_ALG_SHA1" -> TPM_ALG_SHA1;
            case "TPM_ALG_SHA256" -> TPM_ALG_SHA256;
            case "TPM_ALG_SHA384" -> TPM_ALG_SHA384;
            case "TPM_ALG_SHA512" -> TPM_ALG_SHA512;
            default -> TPM_ALG_NULL;
        };
    }

    /**
     * Sets the length of a given TPM ALG Identifier.
     * (lookup based upon section 6.3 for the TPM-Rev-2.0-Part-2-Structures.pdf document)
     * Only hash algorithms found in Table 7 are used.
     *
     * @param algId TCG defined Algorithm identifier
     * @return length of hash data in  bytes
     */
    public static int tcgAlgLength(final int algId) {
        return switch (algId) {
            case TPM_ALG_SHA1 -> TPM_ALG_SHA1_LENGTH;
            case TPM_ALG_SHA256 -> TPM_ALG_SHA256_LENGTH;
            case TPM_ALG_SHA384 -> TPM_ALG_SHA384_LENGTH;
            case TPM_ALG_SHA512 -> TPM_ALG_SHA512_LENGTH;
            default -> TPM_ALG_NULL_LENGTH;
        };
    }

    /**
     * Returns the contents of the TPMT_HA structure buffer.
     *
     * @return contents of the TPMT_HA structure.
     */
    public byte[] getBuffer() {
        return java.util.Arrays.copyOf(buffer, buffer.length);
    }

    /**
     * Readable description of the Algorithm.
     *
     * @return Readable Algorithm name
     */
    @Override
    public String toString() {
        return String.format("%s hash = %s", hashName, HexUtils.byteArrayToHexString(digest));
    }
}
