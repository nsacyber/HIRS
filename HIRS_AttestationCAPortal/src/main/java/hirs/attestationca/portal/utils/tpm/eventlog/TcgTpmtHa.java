package hirs.attestationca.portal.utils.tpm.eventlog;

import hirs.attestationca.utils.HexUtils;
import lombok.AccessLevel;
import lombok.Getter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;

/**
 * Class to for the TCG defined TPMT_HA structure used to support the Crypto Agile Log format.
 * <p>
 * typedef struct {
 * TPMI_ALG_HASH hashAlg;
 * TPMU_HA       digest;
 * } TPMT_HA;
 */
public class TcgTpmtHa {
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
     * Human readable name of the hash algorithm.
     */
    @Getter
    private String hashName = "";
    /**
     * Hash data.
     */
    @Getter(value = AccessLevel.PROTECTED)
    private byte[] digest = null;
    /**
     * TCG ID for SHA1.
     */
    public static final int TPM_ALG_SHA1 = 0x04;
    /**
     * TCG ID for SHA1.
     */
    public static final int TPM_ALG_SHA256 = 0x0B;
    /**
     * TCG ID for SHA 384.
     */
    public static final int TPM_ALG_SHA384 = 0x0C;
    /**
     * TCG ID for SHA512.
     */
    public static final int TPM_ALG_SHA_512 = 0x0D;
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

    /**
     * Returns the hash name via a lookup.
     * Lookup based upon section 6.3 for the TPM-Rev-2.0-Part-2-Structures.pdf document.
     * Only hash algorithms found in Table 7 are used.
     *
     * @param algId int to convert to string
     * @return name of the algorithm
     */
    public static String tcgAlgIdToString(final int algId) {
        String alg;
        switch (algId) {
            case TPM_ALG_SHA1:
                alg = "TPM_ALG_SHA1";
                break;
            case TPM_ALG_SHA256:
                alg = "TPM_ALG_SHA256";
                break;
            case TPM_ALG_SHA384:
                alg = "TPM_ALG_SHA384";
                break;
            case TPM_ALG_SHA_512:
                alg = "TPM_ALG_SHA512";
                break;
            case TPM_ALG_NULL:
                alg = "TPM_ALG_NULL";
                break;
            default:
                alg = "Unknown or invalid Hash";
        }
        return alg;
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
        int alg;
        switch (algorithm) {
            case "TPM_ALG_SHA1":
                alg = TPM_ALG_SHA1;
                break;
            case "TPM_ALG_SHA256":
                alg = TPM_ALG_SHA256;
                break;
            case "TPM_ALG_SHA384":
                alg = TPM_ALG_SHA384;
                break;
            case "TPM_ALG_SHA512":
                alg = TPM_ALG_SHA_512;
                break;
            case "TPM_ALG_NULL":
            default:
                alg = TPM_ALG_NULL;
        }
        return alg;
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
        int length;
        switch (algId) {
            case TPM_ALG_SHA1:
                length = TPM_ALG_SHA1_LENGTH;
                break;
            case TPM_ALG_SHA256:
                length = TPM_ALG_SHA256_LENGTH;
                break;
            case TPM_ALG_SHA384:
                length = TPM_ALG_SHA384_LENGTH;
                break;
            case TPM_ALG_SHA_512:
                length = TPM_ALG_SHA512_LENGTH;
                break;
            case TPM_ALG_NULL:
            default:
                length = TPM_ALG_NULL_LENGTH;
        }
        return length;
    }
}
