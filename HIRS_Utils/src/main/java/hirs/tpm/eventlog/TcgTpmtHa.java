package hirs.tpm.eventlog;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;

import hirs.utils.HexUtils;

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
     * TCG Defined Algorithm Identifiers .
     */
    private int hashAlgId = 0;
    /**
     * Length of the  hash.
     */
    private int hashLength = 0;
    /**
     * Human readable name of the hash algorithm.
     */
    private String hashName = "";
    /**
     * Hash data.
     */
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
     * Constructor.
     *
     * @param is ByteArrayInputStream holding the TcgTPMT_HA structured data
     * @throws IOException if TPMT_HA structure cannot be parsed
     */
    public TcgTpmtHa(final ByteArrayInputStream is) throws IOException {
        byte[] algID = new byte[2];
        is.read(algID);
        byte[] rAlgID = HexUtils.leReverseByte(algID);
        hashAlgId = new BigInteger(rAlgID).intValue();
        hashName = tcgAlgIdtoString(algID[0]);
        hashLength = tcgAlgLength(algID[0]);
        digest = new byte[hashLength];
        is.read(digest);
    }

    /**
     * Returns the TCG defined algorithm identifier.
     *
     * @return integer that specifies the algorithm as defined by the TCG
     */
    public int getAlgId() {
        return hashAlgId;
    }

    /**
     * Return the length of the Hash.
     *
     * @return the Hash length
     */
    public int getHashLength() {
        return hashLength;
    }

    /**
     * Readable name of the algorithm.
     *
     * @return Hash algorithm name
     */
    public String getHashName() {
        return hashName;
    }

    /**
     * @return digest held by the event
     */
    protected byte[] getDigest() {
        return digest;
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
     * @param algid int to convert to string
     * @return name of the algorithm
     */
    public static String tcgAlgIdtoString(final int algid) {
        String alg;
        switch (algid) {
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
     public static int tcgAlgStringtoId(final String algorithm) {
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
                alg = TPM_ALG_NULL;
                break;
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
