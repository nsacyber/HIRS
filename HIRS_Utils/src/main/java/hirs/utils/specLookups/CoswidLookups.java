package hirs.utils.specLookups;

/**
 * Class for defining constants referenced in the CoSWID (Named Information Hash Algorithm Registry).
 * https://www.rfc-editor.org/rfc/rfc9393.html
 * https://www.iana.org/assignments/named-information/named-information.xhtml
 */
public final class CoswidLookups {

    /** CoSWID name for SHA1. */
    public static final String SHA1_STR = "";
    /** CoSWID name for SHA256. */
    public static final String SHA256_STR = "sha-256";
    /** CoSWID name for SHA384. */
    public static final String SHA384_STR = "sha-384";
    /** CoSWID name for SHA512. */
    public static final String SHA512_STR = "sha-512";
    /** CoSWID name for SHA3-256. */
    public static final String SHA3_256_STR = "sha3-256";
    /** CoSWID name for SHA3-384. */
    public static final String SHA3_384_STR = "sha3-384";
    /** CoSWID name for SHA3-512. */
    public static final String SHA3_512_STR = "sha3-512";

    /**
     * Default constructor.
     */
    private CoswidLookups() {
    }
}
