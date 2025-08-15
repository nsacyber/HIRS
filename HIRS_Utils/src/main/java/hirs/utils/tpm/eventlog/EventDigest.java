package hirs.utils.tpm.eventlog;

import lombok.Getter;

public class EventDigest {

    /**
     * Human-readable name of the hash algorithm.
     */
    @Getter
    private final String hashName;
    /**
     * Hash data.
     */
    @Getter
    private final byte[] digest;

    /**
     * Constructor.
     *
     * @param hashNameIn hash alg name associate with this digest
     * @param digestIn the digest
     */
    public EventDigest(String hashNameIn, byte[] digestIn) {
        hashName = hashNameIn;
        digest = digestIn;
    }
}
