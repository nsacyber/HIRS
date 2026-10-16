package hirs.utils.specificationLookups;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class TcgAlgReg {

    /** TCG ID for Null algorithm. */
    public static final int TPM_ALG_NULL_ID = 0x10;
    /** TCG ID for SHA1. */
    public static final int TPM_ALG_SHA1_ID = 0x04;
    /** TCG Name for SHA1. */
    public static final String TPM_ALG_SHA1_STR = "TPM_ALG_SHA1";
    /** TCG ID for SHA56. */
    public static final int TPM_ALG_SHA256_ID = 0x0B;
    /** TCG Name for SHA56. */
    public static final String TPM_ALG_SHA256_STR = "TPM_ALG_SHA256";
    /** TCG ID for SHA 384. */
    public static final int TPM_ALG_SHA384_ID = 0x0C;
    /** TCG Name for SHA 384. */
    public static final String  TPM_ALG_SHA384_STR = "TPM_ALG_SHA384";
    /** TCG ID for SHA512. */
    public static final int TPM_ALG_SHA512_ID = 0x0D;
    /** TCG Name for SHA512. */
    public static final String TPM_ALG_SHA512_STR = "TPM_ALG_SHA512";


    @Getter
    @AllArgsConstructor
    public enum TcgHashAlg {
        ALG_NULL(TPM_ALG_NULL_ID, "", NistLookups.ALG_NULL_LEN),
        ALG_SHA1(TPM_ALG_SHA1_ID, TPM_ALG_SHA1_STR, NistLookups.ALG_SHA1_LEN),
        ALG_SHA256(TPM_ALG_SHA256_ID, TPM_ALG_SHA256_STR, NistLookups.ALG_SHA256_LEN),
        ALG_SHA384(TPM_ALG_SHA384_ID, TPM_ALG_SHA384_STR, NistLookups.ALG_SHA384_LEN);

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
