package hirs.utils.specLookups;

/**
 * Class for defining constants referenced in the X509 Specification.
 */
public final class X509Lookups {

    /** X509 name for SHA1. */
    public static final String SHA1_STR = "SHA1";
    /** X509 name for SHA256. */
    public static final String SHA256_STR = "SHA256";
    /** X509 name for SHA384. */
    public static final String SHA384_STR = "SHA384";
    /** X509 name for SHA512. */
    public static final String SHA512_STR = "SHA512";
    /** X509 name for SHA3-256. */
    public static final String SHA3_256_STR = "SHA3256";
    /** X509 name for SHA3-384. */
    public static final String SHA3_384_STR = "SHA3384";
    /** X509 name for SHA3-512. */
    public static final String SHA3_512_STR = "SHA3512";

    /** X509 name for SHA256 with ECDSA. */
    public static final String SHA256_ECDSA_STR = "SHA256withECDSA";
    /** X509 name for SHA384 with ECDSA. */
    public static final String SHA384_ECDSA_STR = "SHA384withECDSA";
    /** X509 name for SHA512 with ECDSA. */
    public static final String SHA512_ECDSA_STR = "SHA512withECDSA";
    /** X509 name for SHA256 with RSA. */
    public static final String SHA256_RSA_STR = "SHA256withRSA";
    /** X509 name for SHA384 with RSA. */
    public static final String SHA384_RSA_STR = "SHA384withRSA";
    /** X509 name for SHA512 with RSA. */
    public static final String SHA512_RSA_STR = "SHA512withRSA";
    /** X509 name for SHA256 with RSA PSS. */
    public static final String SHA256_RSA_PSS_STR = "";
    /** X509 name for SHA384 with RSA PSS. */
    public static final String SHA384_RSA_PSS_STR = "";
    /** X509 name for SHA512 with RSA PSS. */
    public static final String SHA512_RSA_PSS_STR = "";

    /**
     * Default constructor.
     */
    private X509Lookups() {
    }
}
