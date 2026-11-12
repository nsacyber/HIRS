package hirs.utils.specLookups;

/**
 * Class for defining constants referenced in the XML Security Algorithm Cross-Reference.
 * https://www.w3.org/TR/xmlsec-algorithms/
 * RFC6931 (Additional XML Security Uniform Resource Identifiers (URIs)):
 * https://datatracker.ietf.org/doc/rfc6931/
 */
public final class XmlLookups {

    /** XML name for SHA1. */
    public static final String SHA1_STR = "SHA-1";
    /** XML name for SHA256. */
    public static final String SHA256_STR = "SHA-256";
    /** XML name for SHA384. */
    public static final String SHA384_STR = "SHA-384";
    /** XML name for SHA512. */
    public static final String SHA512_STR = "SHA-512";
    /** XML name for SHA3-256. */
    public static final String SHA3_256_STR = "";
    /** XML name for SHA3-384. */
    public static final String SHA3_384_STR = "";
    /** XML name for SHA3-512. */
    public static final String SHA3_512_STR = "";

    /** XML name for SHA256 with ECDSA. */
    public static final String SHA256_ECDSA_STR = "ECDSA-SHA256";
    /** XML name for SHA384 with ECDSA. */
    public static final String SHA384_ECDSA_STR = "ECDSA-SHA384";
    /** XML name for SHA512 with ECDSA. */
    public static final String SHA512_ECDSA_STR = "ECDSA-SHA512";
    /** XML name for SHA256 with RSA. */
    public static final String SHA256_RSA_STR = "RSA-SHA256";
    /** XML name for SHA384 with RSA. */
    public static final String SHA384_RSA_STR = "RSA-SHA384";
    /** XML name for SHA512 with RSA. */
    public static final String SHA512_RSA_STR = "RSA-SHA512";
    /** XML name for SHA256 with RSA PSS. */
    public static final String SHA256_RSA_PSS_STR = "";
    /** XML name for SHA384 with RSA PSS. */
    public static final String SHA384_RSA_PSS_STR = "";
    /** XML name for SHA512 with RSA PSS. */
    public static final String SHA512_RSA_PSS_STR = "";

    /**
     * Default constructor.
     */
    private XmlLookups() {
    }
}
