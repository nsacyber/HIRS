package hirs.attestationca.persist.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * Enum representing TCG algorithms for cryptography.
 * <p>
 * This enum currently includes:
 * <ul>
 *   <li>{@link #RSA} – RSA public-key algorithm.</li>
 *   <li>{@link #ECC} – Elliptic Curve (EC) public-key algorithm, commonly referred to as ECC.</li>
 * </ul>
 * <p>
 * Each enum constant holds the public key algorithm name and id. The ID is listed on the
 * <a href="https://trustedcomputinggroup.org/wp-content/uploads/TCG-Algorithm-Registry-Version-2.0_pub.pdf">
 *   TCG Algorithm Registry Version PDF.</a>
 */
@Getter
@AllArgsConstructor
@ToString
public enum TcgAlgorithm {
    /**
     * RSA Public Key Algorithm.
     */
    RSA(0x0001, "RSA"),

    /** SHA-256 algorithm. */
    SHA256(0x000B, "SHA-256"),
    /** SHA-384 algorithm. */
    SHA384(0x000C, "SHA-384"),
    /** SHA-512 algorithm. */
    SHA512(0x000D, "SHA-512"),

    /** RSASSA algorithm. */
    RSASSA(0x0014, "RSASSA"),
    /** RSAES algorithm. */
    RSAES(0x0015, "RSAES"),
    /** RSAPSS algorithm. */
    RSAPSS(0x0016, "RSAPSS"),
    /** OAEP algorithm. */
    OAEP(0x0017, "OAEP"),

    /** ECDSA algorithm. */
    ECDSA(0x0018, "ECDSA"),

    /** ECDH algorithm. */
    ECDH(0x0019, "ECDH"),

    /**
     * ECC Public Key Algorithm.
     */
    ECC(0x0023, "ECC"),

    /** AES algorithm. */
    AES(0x0006, "AES"),

    /** NULL algorithm. */
    NULL(0x0010, "NULL"),

    /**
     * Represents an unknown public key algorithm.
     * This is used when the application encounters a public key algorithm that is not recognized or supported.
     */
    UNKNOWN(0xFFFF, "UNKNOWN");

    /**
     * The hexadecimal representation of the algorithm ID as represented in the TCG documents.
     */
    private final int algorithmId;

    /**
     * The name of the algorithm.
     */
    private final String algorithmName;

    /**
     * Retrieves the enum by the algorithm ID.
     *
     * @param algorithmId algorithm ID
     * @return ENUM representation of the public key algorithm
     */
    public static TcgAlgorithm fromId(final int algorithmId) {
        for (TcgAlgorithm algo : values()) {
            if (algo.getAlgorithmId() == algorithmId) {
                return algo;
            }
        }
        return UNKNOWN; // If no match found, return UNKNOWN
    }
}
