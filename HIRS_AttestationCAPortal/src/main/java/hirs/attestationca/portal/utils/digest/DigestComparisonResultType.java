package hirs.attestationca.portal.utils.digest;

/**
 * Enumeration identifying the different outcomes of a comparison between
 * two {@link Digest} objects.
 *
 */
public enum DigestComparisonResultType {
    /**
     * When one of the Digests compared has a hash that is uninitialized, defaulted, or
     * is a byte array equal to zero.
     */
    UNKNOWN,

    /**
     * When the two digest hashes are equal, and are not zeroized / defaulted hash arrays.
     */
    MATCH,

    /**
     * When the two digest hashes are not equal, and are not zeroized / defaulted hash arrays.
     */
    MISMATCH,
}
