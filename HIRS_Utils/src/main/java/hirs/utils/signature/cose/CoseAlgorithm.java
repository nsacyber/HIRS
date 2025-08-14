package hirs.utils.signature.cose;

import hirs.utils.rim.unsignedRim.cbor.ietfCoswid.CoswidItems;

/**
 * Class to handle COSE algorithms specified on https://www.iana.org/assignments/cose/cose.xhtml#algorithms.
 * As specified by the COSE specification (rfc rfc8152) and constrained by the TCG Component Rim binding spec
 * (for CoSwid).
 * Processing is limited to the Algorithm Combinations suited to TCG registered signatures.
 */
public final class CoseAlgorithm {
    /** IANA Registered COSE Algorithm. */
    public static final int COSE_RSA_SHA_512 = -259;  // Uses PKCS-v1_5 padding
    /** IANA Registered COSE Algorithm. */
    public static final int COSE_RSA_SHA_384 = -258;
    /** IANA Registered COSE Algorithm. */
    public static final int COSE_RSA_SHA_256 = -257;
    /** IANA Registered COSE Algorithm. */
    public static final int COSE_ES_SHA_512 = -36;
    /** IANA Registered COSE Algorithm. */
    public static final int COSE_ES_SHA_384 = -35;
    /** IANA Registered COSE Algorithm. */
    public static final int COSE_ES_SHA_256 = -7;
    /** IANA Registered COSE Algorithm. */
    public static final int COSE_RSA_PSS_512 = -39;
    /** IANA Registered COSE Algorithm. */
    public static final int COSE_RSA_PSS_384 = -38;
    /** IANA Registered COSE Algorithm. */
    public static final int COSE_RSA_PSS_256 = -37;
    /** IANA Registered COSE Algorithm. */
    public static final int COSE_SHA_256 = -16;
    /** IANA Registered COSE Algorithm Name. */
    public static final String RSA_SHA512_PKCS1 = "RS512";
    /** IANA Registered COSE Algorithm Name. */
    public static final String RSA_SHA384_PKCS1 = "RS384";
    /** IANA Registered COSE Algorithm Name. */
    public static final String RSA_SHA256_PKCS1 = "RS256";
    /** IANA Registered COSE Algorithm Name. */
    public static final String RSA_SHA512_PSS = "PS512";
    /** IANA Registered COSE Algorithm Name. */
    public static final String RSA_SHA384_PSS = "PS384";
    /** IANA Registered COSE Algorithm Name. */
    public static final String RSA_SHA256_PSS = "PS256";
    /** IANA Registered COSE Algorithm Name. */
    public static final String ECDSA_SHA256 = "ES256";
    /** IANA Registered COSE Algorithm Name. */
    public static final String ECDSA_SHA384 = "ES384";
    /** IANA Registered COSE Algorithm Name. */
    public static final String ECDSA_SHA512 = "ES512";
    /** IANA Registered COSE Algorithm Name. */
    public static final String SHA256 = "SHA-256";

    private static final String[][] ALG_NAMES = {
            {"-259", "RS512"},     // RSASSA-PKCS1-v1_5 using SHA-512
            {"-258", "RS384"},     // RSASSA-PKCS1-v1_5 using SHA-384
            {"-257", "RS256"},     // RSASSA-PKCS1-v1_5 using SHA-256
            {"-39", "PS512"},      // RSASSA-PSS w/ SHA-512
            {"-38", "PS384"},      // RSASSA-PSS w/ SHA-384
            {"-37", "PS256"},      // RSASSA-PSS w/ SHA-256
            {"-36", "ES512"},      // ECDSA w/ SHA-512
            {"-35", "ES384"},      // ECDSA w/ SHA-384
            {"-16", "SHA-256"},     // SHA-2 256-bit Hash
            {"-7", "ES256"}        // ECDSA w/ SHA-256
    };

    /**
     * Default constructor for CoseAlgorithm.
     */
    private CoseAlgorithm() {
    }
    /**
     * Searches Rfc 9393 Items Names for match to a specified item name and returns the index.
     * @param coseAlg  Iem Name specified in rfc 8152
     * @return int tag of the cose type
     */
    public static int getAlgId(final String coseAlg) {
        int algId = 0;
        for (int i = 0; i < ALG_NAMES.length; i++) {
            if (coseAlg.compareToIgnoreCase(ALG_NAMES[i][1]) == 0) {
                return (Integer.parseInt(ALG_NAMES[i][0]));
            }
        }
        return CoswidItems.UNKNOWN_INT;
    }
    /**
     * Searches for an Rfc 8152 specified index and returns the item name associated with the index.
     * @param coseAlId IANA registered COSE Algorithm Value (ID)
     * @return String Algorithm name associated with the Algorithm Value (ID)
     */
    public static String getAlgName(final int coseAlId) {
        int algId = 0;
        for (int i = 0; i < ALG_NAMES.length; i++) {
            if (coseAlId == Integer.parseInt(ALG_NAMES[i][0])) {
                return ALG_NAMES[i][1];
            }
        }
        return CoswidItems.UNKNOWN_STR;
    }
    /**
     * Returns true if the specified COSE algorithm identifier is a supported algorithm.
     * from the ECDSA family of algorithms.
     * @param cosAlId
     * @return true if algorithm is COSE supported
     */
    public static boolean isEcdsa(final int cosAlId) {
        if ((cosAlId == CoseAlgorithm.COSE_ES_SHA_256) || (cosAlId == CoseAlgorithm.COSE_ES_SHA_384)
                || (cosAlId == CoseAlgorithm.COSE_ES_SHA_512)) {
            return true;
        }
        return false;
    }
    /**
     * Returns true of the specified COSE algorithm identifier is a supported algorithm
     * from the ECDSA family of algorithms.
     * @param coseAlgorithmName a IANA Registered COSE algorithm name
     * @return true if algorithm is an ecdsa variant
     */
    public static boolean isEcdsaName(final String coseAlgorithmName) {
        if ((coseAlgorithmName.compareToIgnoreCase(CoseAlgorithm.ECDSA_SHA256) == 0)
                || (coseAlgorithmName.compareToIgnoreCase(CoseAlgorithm.ECDSA_SHA384) == 0)
                || (coseAlgorithmName.compareToIgnoreCase(CoseAlgorithm.ECDSA_SHA512) == 0)) {
            return true;
        }
        return false;
    }
    /**
     * Returns true of the specified COSE algorithm identifier is a supported algorithm
     * from the RSA family of algorithms.
     * @param cosAlId cose registered algorithm id
     * @return true if algorithm is a rsa variant
     */
    public static boolean isRsa(final int cosAlId) {
        return cosAlId == CoseAlgorithm.COSE_RSA_PSS_256
                || cosAlId == CoseAlgorithm.COSE_RSA_PSS_384
                || cosAlId == CoseAlgorithm.COSE_RSA_SHA_256;
    }

    /**
     * Returns true of the specified COSE algorithm identifier is a supported algorithm
     * from the ECDSA family of algorithms.
     * @param coseAlgorithmName a IANA Registered COSE algorithm name
     * @return true if algorithm is a rsa variant
     */
    public static boolean isRsaName(final String coseAlgorithmName) {
        if ((coseAlgorithmName.compareToIgnoreCase(CoseAlgorithm.RSA_SHA256_PKCS1) == 0)
                || (coseAlgorithmName.compareToIgnoreCase(CoseAlgorithm.RSA_SHA384_PKCS1) == 0)
                || (coseAlgorithmName.compareToIgnoreCase(CoseAlgorithm.RSA_SHA512_PKCS1) == 0)
                || (coseAlgorithmName.compareToIgnoreCase(CoseAlgorithm.RSA_SHA256_PSS) == 0)
                || (coseAlgorithmName.compareToIgnoreCase(CoseAlgorithm.RSA_SHA384_PSS) == 0)
                || (coseAlgorithmName.compareToIgnoreCase(CoseAlgorithm.RSA_SHA512_PSS) == 0)) {
            return true;
        }
        return false;
    }

    /**
     * Returns true of the specified COSE algorithm is an RSA PSS variant.
     * @param coseAlgorithmName name of the algorithm
     * @return true if algorithm is a rsa-pss variant
     */
    public static boolean isRsaPssName(final String coseAlgorithmName) {
        if ((coseAlgorithmName.compareToIgnoreCase(CoseAlgorithm.RSA_SHA256_PSS) == 0)
                || (coseAlgorithmName.compareToIgnoreCase(CoseAlgorithm.RSA_SHA384_PSS) == 0)
                || (coseAlgorithmName.compareToIgnoreCase(CoseAlgorithm.RSA_SHA512_PSS) == 0)) {
            return true;
        }
        return false;
    }
}
