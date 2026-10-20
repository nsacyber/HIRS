package hirs.utils.specLookups;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Class for defining constants referenced in the TCG Algorithm Registry.
 * https://trustedcomputinggroup.org/resource/tcg-algorithm-registry/
 */
public final class TcgAlgRegLookups {

    /** TCG ID for Null algorithm. */
    public static final int ALG_NULL_ID = 0x10;
    /** TCG ID for SHA1. */
    public static final int SHA1_ID = 0x04;
    /** TCG name for SHA1. */
    public static final String SHA1_STR = "TPM_ALG_SHA1";
    /** TCG ID for SHA56. */
    public static final int SHA256_ID = 0x0B;
    /** TCG name for SHA56. */
    public static final String SHA256_STR = "TPM_ALG_SHA256";
    /** TCG ID for SHA 384. */
    public static final int SHA384_ID = 0x0C;
    /** TCG name for SHA 384. */
    public static final String  SHA384_STR = "TPM_ALG_SHA384";
    /** TCG ID for SHA512. */
    public static final int SHA512_ID = 0x0D;
    /** TCG name for SHA512. */
    public static final String SHA512_STR = "TPM_ALG_SHA512";
    /** TCG name for SHA3-256. */
    public static final String SHA3_256_STR = "TPM_ALG_SHA3_256";
    /** TCG name for SHA3-384. */
    public static final String SHA3_384_STR = "TPM_ALG_SHA3_384";
    /** TCG name for SHA3-512. */
    public static final String SHA3_512_STR = "TPM_ALG_SHA3_512";

    // Note: TCG does not combine assymmetric & hash into one name for a signing algorithm,
    //       so name combines these 2 with a *
    /** TCG name for SHA256 with ECDSA. */
    public static final String SHA256_ECDSA_STR = "TPM_ALG_ECDSA*TPM_ALG_SHA256";
    /** TCG name for SHA384 with ECDSA. */
    public static final String SHA384_ECDSA_STR = "TPM_ALG_ECDSA*TPM_ALG_SHA384";
    /** TCG name for SHA512 with ECDSA. */
    public static final String SHA512_ECDSA_STR = "TPM_ALG_ECDSA*TPM_ALG_SHA512";
    /** TCG name for SHA256 with RSA. */
    public static final String SHA256_RSA_STR = "TPM_ALG_RSASSA*TPM_ALG_SHA256";
    /** TCG name for SHA384 with RSA. */
    public static final String SHA384_RSA_STR = "TPM_ALG_RSASSA*TPM_ALG_SHA384";
    /** TCG name for SHA512 with RSA. */
    public static final String SHA512_RSA_STR = "TPM_ALG_RSASSA*TPM_ALG_SHA512";
    /** TCG name for SHA256 with RSA PSS. */
    public static final String SHA256_RSA_PSS_STR = "TPM_ALG_RSAPSS*TPM_ALG_SHA256";
    /** TCG name for SHA384 with RSA PSS. */
    public static final String SHA384_RSA_PSS_STR = "TPM_ALG_RSAPSS*TPM_ALG_SHA384";
    /** TCG name for SHA512 with RSA PSS. */
    public static final String SHA512_RSA_PSS_STR = "TPM_ALG_RSAPSS*TPM_ALG_SHA512";

    /**
     * Default constructor.
     */
    private TcgAlgRegLookups() {
    }

    @Getter
    @AllArgsConstructor
    public enum TcgHashAlg {
        /** NULL. **/
        NULL(ALG_NULL_ID, "", NistLookups.ALG_NULL_LEN),
        /** SHA1. **/
        SHA1(SHA1_ID, SHA1_STR, NistLookups.SHA1_LEN),
        /** SHA256. **/
        SHA256(SHA256_ID, SHA256_STR, NistLookups.SHA256_LEN),
        /** SHA384. **/
        SHA384(SHA384_ID, SHA384_STR, NistLookups.SHA384_LEN),
        /** SHA512. **/
        SHA512(SHA512_ID, SHA512_STR, NistLookups.SHA512_LEN);

        private final int algId;
        private final String algName;
        private final int algLen;

        private static final Map<Integer, TcgHashAlg> ID_MAP = new HashMap<>();
        private static final Map<String, TcgHashAlg> NAME_MAP = new HashMap<>();

        static {
            for (TcgHashAlg alg : values()) {
                ID_MAP.put(alg.getAlgId(), alg);
                NAME_MAP.put(alg.getAlgName(), alg);
            }
        }

        /**
         * Returns TcgHashAlg given a hash alg ID.
         *
         * @param id the hash alg ID
         * @return TcgHashAlg corresponding to hash alg ID
         */
        public static TcgHashAlg fromAlgId(final int id) {
            return ID_MAP.get(id); }

        /**
         * Returns TcgHashAlg given a hash alg name.
         *
         * @param name the hash alg name
         * @return TcgHashAlg corresponding to hash alg name
         */
        public static TcgHashAlg fromAlgName(final String name) {
            return NAME_MAP.get(name); }
    }
}
