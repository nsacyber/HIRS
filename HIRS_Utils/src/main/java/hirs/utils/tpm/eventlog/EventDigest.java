package hirs.utils.tpm.eventlog;

import lombok.AccessLevel;
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
    @Getter(value = AccessLevel.PROTECTED)
    private final byte[] digest;

    /**
     * Constructor.
     *
     * @param hashNameIn hash alg name associate with this digest
     * @param digestIn the digest
     */
    public EventDigest(String hashNameIn, byte[] digestIn) {
        hashName = hashNameIn;
        digest = new byte[digestIn.length];
        System.arraycopy(digestIn, 0, digest, 0, digestIn.length);
    }
}
