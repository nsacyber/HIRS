package hirs.attestationca.persist.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * Enum representing public key algorithms for asymmetric cryptography.
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
public enum PublicKeyAlgorithm {
    /**
     * RSA Public Key Algorithm.
     */
    RSA(0x0001, "RSA"),

    /**
     * ECC Public Key Algorithm.
     */
    ECC(0x0023, "ECC"),

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
    public static PublicKeyAlgorithm fromId(final int algorithmId) {
        for (PublicKeyAlgorithm algo : values()) {
            if (algo.getAlgorithmId() == algorithmId) {
                return algo;
            }
        }
        return UNKNOWN; // If no match found, return UNKNOWN
    }
}
