package hirs.utils.tpm.eventlog.spdm;

import hirs.utils.HexUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Class for defining constants referenced in the DMTF
 * SPDM specification.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SpdmHa {


    /**
     * ------------------- SPDM Spec: MeasurementHashAlgo -------------------
     * SPDM 1.3.0, Table 21
     */
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
     * Lookup based upon section 10.4 for the SPDM v1.03 document.
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

}
