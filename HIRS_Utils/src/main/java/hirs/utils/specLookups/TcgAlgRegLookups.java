package hirs.utils.specLookups;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class TcgAlgRegLookups {

    /** TCG ID for Null algorithm. */
    public static final int ALG_NULL_ID = 0x10;
    /** TCG ID for SHA1. */
    public static final int SHA1_ID = 0x04;
    /** TCG Name for SHA1. */
    public static final String SHA1_STR = "TPM_ALG_SHA1";
    /** TCG ID for SHA56. */
    public static final int SHA256_ID = 0x0B;
    /** TCG Name for SHA56. */
    public static final String SHA256_STR = "TPM_ALG_SHA256";
    /** TCG ID for SHA 384. */
    public static final int SHA384_ID = 0x0C;
    /** TCG Name for SHA 384. */
    public static final String  SHA384_STR = "TPM_ALG_SHA384";
    /** TCG ID for SHA512. */
    public static final int SHA512_ID = 0x0D;
    /** TCG Name for SHA512. */
    public static final String SHA512_STR = "TPM_ALG_SHA512";
    /** TCG Name for SHA512. */
    public static final String SHA3_256_STR = "TPM_ALG_SHA3_256";
    /** TCG Name for SHA512. */
    public static final String SHA3_384_STR = "TPM_ALG_SHA3_384";
    /** TCG Name for SHA512. */
    public static final String SHA3_512_STR = "TPM_ALG_SHA3_512";

    public static final String SHA256_ECDSA_STR = "TPM_ALG_ECDSA*TPM_ALG_SHA256";
    public static final String SHA384_ECDSA_STR = "TPM_ALG_ECDSA*TPM_ALG_SHA384";
    public static final String SHA512_ECDSA_STR = "TPM_ALG_ECDSA*TPM_ALG_SHA512";
    public static final String SHA256_RSA_STR = "TPM_ALG_RSASSA*TPM_ALG_SHA256";
    public static final String SHA384_RSA_STR = "TPM_ALG_RSASSA*TPM_ALG_SHA384";
    public static final String SHA512_RSA_STR = "TPM_ALG_RSASSA*TPM_ALG_SHA512";
    public static final String SHA256_RSA_PSS_STR = "TPM_ALG_RSAPSS*TPM_ALG_SHA256";
    public static final String SHA384_RSA_PSS_STR = "TPM_ALG_RSAPSS*TPM_ALG_SHA384";
    public static final String SHA512_RSA_PSS_STR = "TPM_ALG_RSAPSS*TPM_ALG_SHA512";

    @Getter
    @AllArgsConstructor
    public enum TcgHashAlg {
        ALG_NULL(ALG_NULL_ID, "", NistLookups.ALG_NULL_LEN),
        ALG_SHA1(SHA1_ID, SHA1_STR, NistLookups.SHA1_LEN),
        ALG_SHA256(SHA256_ID, SHA256_STR, NistLookups.SHA256_LEN),
        ALG_SHA384(SHA384_ID, SHA384_STR, NistLookups.SHA384_LEN);

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

        public static TcgHashAlg fromAlgId(int id) { return ID_MAP.get(id); }
        public static TcgHashAlg fromAlgName(String name) { return NAME_MAP.get(name); }
    }

}
