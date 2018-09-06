package hirs.data.persist;

/**
 * Enum of digest algorithms. The enum values also provide a standardized
 * algorithm name. The standardized algorithm name is a String of the algorithm
 * name as defined by Java.
 */
public enum DigestAlgorithm {
    /**
     * MD2 digest algorithm.
     */
    MD2("MD2"),
    /**
     * MD5 digest algorithm.
     */
    MD5("MD5"),
    /**
     * SHA-1 digest algorithm.
     */
    SHA1("SHA-1"),
    /**
     * SHA-256 digest algorithm.
     */
    SHA256("SHA-256"),
    /**
     * SHA-384 digest algorithm.
     */
    SHA384("SHA-384"),
    /**
     * SHA-512 digest algorithm.
     */
    SHA512("SHA-512");

    private String standardAlgorithmName;

    /**
     * Creates a new <code>DigestAlgorithm</code>.
     *
     * @param standardAlgorithmName
     *            Java standard algorithm name
     */
    DigestAlgorithm(final String standardAlgorithmName) {
        this.standardAlgorithmName = standardAlgorithmName;
    }

    /**
     * Returns the standard Java algorithm name.
     *
     * @return standard Java algorithm name
     */
    String getStandardAlgorithmName() {
        return this.standardAlgorithmName;
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
