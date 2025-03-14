package hirs.utils.tpm.eventlog.spdm;

/**
 * Class for defining hash algorithms referenced in the DMTF SPDM specification.
 * SPDM 1.3.0, Table 21, MeasurementHashAlgo.
 */
public final class SpdmHa {

    /**
     * Spdm Hash Alg = Raw bit stream.
     */
    public static final int TPM_ALG_RAW = 1;
    /**
     * Spdm Hash Alg = TPM_ALG_SHA_256.
     */
    public static final int TPM_ALG_SHA_256 = 2;
    /**
     * Spdm Hash Alg = TPM_ALG_SHA_384.
     */
    public static final int TPM_ALG_SHA_384 = 4;
    /**
     * Spdm Hash Alg = TPM_ALG_SHA_512.
     */
    public static final int TPM_ALG_SHA_512 = 8;
    /**
     * Spdm Hash Alg = TPM_ALG_SHA3_256.
     */
    public static final int TPM_ALG_SHA3_256 = 16;
    /**
     * Spdm Hash Alg = TPM_ALG_SHA3_384.
     */
    public static final int TPM_ALG_SHA3_384 = 32;
    /**
     * Spdm Hash Alg = TPM_ALG_SHA3_512.
     */
    public static final int TPM_ALG_SHA3_512 = 64;

    /**
     * Default private constructor so checkstyles doesn't complain.
     */
    private SpdmHa() {
    }

    /**
     * Returns the hash name via a lookup.
     * Lookup based upon SPDM Spec v1.03 section 10.4.
     *
     * @param algId int to convert to string
     * @return name of the algorithm
     */
    public static String tcgAlgIdToString(final int algId) {
        String alg = switch (algId) {
            case TPM_ALG_RAW -> "Raw Bit Stream";
            case TPM_ALG_SHA_256 -> "TPM_ALG_SHA_256";
            case TPM_ALG_SHA_384 -> "TPM_ALG_SHA_384";
            case TPM_ALG_SHA_512 -> "TPM_ALG_SHA_512";
            case TPM_ALG_SHA3_256 -> "TPM_ALG_SHA3_256";
            case TPM_ALG_SHA3_384 -> "TPM_ALG_SHA3_384";
            case TPM_ALG_SHA3_512 -> "TPM_ALG_SHA3_512";
            default -> "Unknown or invalid Hash";
        };
        return alg;
    }

    /**
     * Returns the hash value size based on the hash algorithm.
     * Lookup based upon SPDM Spec v1.03 section 10.4.
     *
     * @param algId int to convert to string
     * @return size of the algorithm output
     */
    public static int tcgAlgIdToByteSize(final int algId) {
        final int byteSize256 = 32;
        final int byteSize384 = 48;
        final int byteSize512 = 64;

        return switch (algId) {
//            case TPM_ALG_RAW: // add this when have more test data
//                return ;
            case TPM_ALG_SHA_256, TPM_ALG_SHA3_256 -> byteSize256;
            case TPM_ALG_SHA_384, TPM_ALG_SHA3_384 -> byteSize384;
            case TPM_ALG_SHA_512, TPM_ALG_SHA3_512 -> byteSize512;
            default -> -1;
        };
    }
}
