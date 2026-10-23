package hirs.utils.specLookups;

/**
 * Class for defining constants referenced in the NIST Specification.
 */
public final class NistLookups {

    /** Length for Null algorithm. */
    public static final int ALG_NULL_LEN = 0;
    /** Length for SHA1. */
    public static final int SHA1_LEN = 20;
    /** Length for SHA56. */
    public static final int SHA256_LEN = 32;
    /** Length for 384. */
    public static final int SHA384_LEN = 48;
    /** Length for SHA512. */
    public static final int SHA512_LEN = 64;

    /**
     * Default constructor.
     */
    private NistLookups() {
    }
}
