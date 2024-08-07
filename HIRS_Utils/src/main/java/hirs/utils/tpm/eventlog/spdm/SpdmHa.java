package hirs.utils.tpm.eventlog.spdm;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Class for defining hash algorithms referenced in the DMTF SPDM specification.
 * SPDM 1.3.0, Table 21, MeasurementHashAlgo.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SpdmHa {

    /**
     * Spdm Hash Alg = Raw bit stream
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
     * Returns the hash name via a lookup.
     * Lookup based upon SPDM Spec v1.03 section 10.4.
     *
     * @param algId int to convert to string
     * @return name of the algorithm
     */
    public static String tcgAlgIdToString(final int algId) {
        String alg;
        switch (algId) {
            case TPM_ALG_RAW:
                alg = "Raw Bit Stream";
                break;
            case TPM_ALG_SHA_256:
                alg = "TPM_ALG_SHA_256";
                break;
            case TPM_ALG_SHA_384:
                alg = "TPM_ALG_SHA_384";
                break;
            case TPM_ALG_SHA_512:
                alg = "TPM_ALG_SHA_512";
                break;
            case TPM_ALG_SHA3_256:
                alg = "TPM_ALG_SHA3_256";
                break;
            case TPM_ALG_SHA3_384:
                alg = "TPM_ALG_SHA3_384";
                break;
            case TPM_ALG_SHA3_512:
                alg = "TPM_ALG_SHA3_512";
                break;
            default:
                alg = "Unknown or invalid Hash";
        }
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
        int byteSize;
        switch (algId) {
            //case TPM_ALG_RAW: // add this when have more test data
            //    byteSize = ;
            //    break;
            case TPM_ALG_SHA_256:
                byteSize = 32;
                break;
            case TPM_ALG_SHA_384:
                byteSize = 48;
                break;
            case TPM_ALG_SHA_512:
                byteSize = 64;
                break;
            case TPM_ALG_SHA3_256:
                byteSize = 32;
                break;
            case TPM_ALG_SHA3_384:
                byteSize = 48;
                break;
            case TPM_ALG_SHA3_512:
                byteSize = 64;
                break;
            default:
                byteSize = -1;
        }
        return byteSize;
    }
}
