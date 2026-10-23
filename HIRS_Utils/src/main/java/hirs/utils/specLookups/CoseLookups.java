package hirs.utils.specLookups;

/**
 * Class for defining constants referenced in the COSE Header Algorithm Parameters.
 * https://www.iana.org/assignments/cose/cose.xhtml#algorithms
 */
public final class CoseLookups {

    /** COSE name for SHA1. */
    public static final String SHA1_STR = "SHA-1";
    /** COSE name for SHA256. */
    public static final String SHA256_STR = "SHA-256";
    /** COSE name for SHA384. */
    public static final String SHA384_STR = "SHA-384";
    /** COSE name for SHA512. */
    public static final String SHA512_STR = "SHA-512";
    /** COSE name for SHA3-256. */
    public static final String SHA3_256_STR = "";
    /** COSE name for SHA3-384. */
    public static final String SHA3_384_STR = "";
    /** COSE name for SHA3-512. */
    public static final String SHA3_512_STR = "";

    /** COSE name for SHA256 with ECDSA. */
    public static final String SHA256_ECDSA_STR = "ES256";
    /** COSE name for SHA384 with ECDSA. */
    public static final String SHA384_ECDSA_STR = "ES384";
    /** COSE name for SHA512 with ECDSA. */
    public static final String SHA512_ECDSA_STR = "ES512";
    /** COSE name for SHA256 with RSA. */
    public static final String SHA256_RSA_STR = "RS256";
    /** COSE name for SHA384 with RSA. */
    public static final String SHA384_RSA_STR = "RS384";
    /** COSE name for SHA512 with RSA. */
    public static final String SHA512_RSA_STR = "RS512";
    /** COSE name for SHA256 with RSA PSS. */
    public static final String SHA256_RSA_PSS_STR = "PS256";
    /** COSE name for SHA384 with RSA PSS. */
    public static final String SHA384_RSA_PSS_STR = "PS384";
    /** COSE name for SHA512 with RSA PSS. */
    public static final String SHA512_RSA_PSS_STR = "PS512";

    /**
     * Default constructor.
     */
    private CoseLookups() {
    }
}
