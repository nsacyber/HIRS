package hirs.data.persist.enums;

import hirs.data.persist.AbstractDigest;

/**
 * Enum of digest algorithms. The enum values also provide a standardized
 * algorithm name. The standardized algorithm name is a String of the algorithm
 * name as defined by Java.
 */
public enum DigestAlgorithm {
    /**
     * MD2 digest algorithm.
     */
    MD2("MD2", AbstractDigest.MD2_DIGEST_LENGTH),
    /**
     * MD5 digest algorithm.
     */
    MD5("MD5", AbstractDigest.MD5_DIGEST_LENGTH),
    /**
     * SHA-1 digest algorithm.
     */
    SHA1("SHA-1", AbstractDigest.SHA1_DIGEST_LENGTH),
    /**
     * SHA-256 digest algorithm.
     */
    SHA256("SHA-256", AbstractDigest.SHA256_DIGEST_LENGTH),
    /**
     * SHA-384 digest algorithm.
     */
    SHA384("SHA-384", AbstractDigest.SHA384_DIGEST_LENGTH),
    /**
     * SHA-512 digest algorithm.
     */
    SHA512("SHA-512", AbstractDigest.SHA512_DIGEST_LENGTH);

    private final String standardAlgorithmName;

    private final int lengthInBytes;

    /**
     * Creates a new <code>DigestAlgorithm</code>.
     *
     * @param standardAlgorithmName
     *            Java standard algorithm name
     * @param lengthInBytes length of hash in bytes
     */
    DigestAlgorithm(final String standardAlgorithmName, final int lengthInBytes) {
        this.standardAlgorithmName = standardAlgorithmName;
        this.lengthInBytes = lengthInBytes;
    }

    /**
     * Returns the standard Java algorithm name.
     *
     * @return standard Java algorithm name
     */
    public String getStandardAlgorithmName() {
        return this.standardAlgorithmName;
    }

    /**
     * Returns the hash length in bytes (not hex characters).
     * @return hash length in bytes (not hex characters)
     */
    public int getLengthInBytes() {
        return lengthInBytes;
    }

    /**
     * Returns a DigestAlgorithm object given a String. The String is expected to be one of the
     * options for standardAlgorithmName. Throws an IllegalArgumentException if no Enum exists with
     * that value.
     *
     * @param standardAlgorithmName
     *            String value of the Enum
     * @return DigestAlgorithm object
     */
    public static DigestAlgorithm findByString(final String standardAlgorithmName) {
        for (DigestAlgorithm algorithm: DigestAlgorithm.values()) {
            if (algorithm.getStandardAlgorithmName().equals(standardAlgorithmName)) {
                return algorithm;
            }
        }
        throw new IllegalArgumentException(String.format("No constant with text \"%s\" found",
                standardAlgorithmName));
    }
}
