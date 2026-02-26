package hirs.attestationca.persist.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Enum representing public key algorithms for asymmetric cryptography.
 * <p>
 * This enum currently includes:
 * <ul>
 *   <li>{@link #RSA} – RSA public-key algorithm.</li>
 *   <li>{@link #ECC} – Elliptic Curve (EC) public-key algorithm, commonly referred to as ECC.</li>
 * </ul>
 * <p>
 * Each enum constant also holds its corresponding JCA (Java Cryptography Architecture)
 * standard name, which can be used when creating KeyFactory or Cipher instances.
 */
@Getter
@AllArgsConstructor
@ToString
public enum PublicKeyAlgorithm {
    /**
     * RSA Public Key Algorithm.
     */
    RSA("RSA", RSAPublicKey.class),

    /**
     * ECC Public Key Algorithm.
     */
    ECC("ECC", ECPublicKey.class),

    /**
     * Represents an unknown public key algorithm.
     * This is used when the application encounters a public key algorithm that is not recognized or supported.
     * It may occur if the algorithm is unsupported or not implemented in the current version of the application.
     */
    UNKNOWN("UNKNOWN", null);

    /**
     * The name of the cryptographic algorithm.
     */
    private final String algorithmName;

    /**
     * The Java Security class corresponding to the public key type used by the algorithm.
     */
    private final Class<? extends PublicKey> keyClass;

    /**
     * Converts the provided string public key algorithm into an ENUM.
     *
     * @param algorithmAsString public key algorithm name as a string
     * @return ENUM representation of the public key algorithm
     */
    public static PublicKeyAlgorithm fromString(final String algorithmAsString) {
        for (PublicKeyAlgorithm algorithmEnum : PublicKeyAlgorithm.values()) {
            if (algorithmEnum.getAlgorithmName().equalsIgnoreCase(algorithmAsString)) {
                return algorithmEnum;
            }
        }
        return UNKNOWN; // Return UNKNOWN if no match is found
    }
}

