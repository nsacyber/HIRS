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
 *   <li>{@link #EC} – Elliptic Curve (EC) public-key algorithm, commonly referred to as ECC.</li>
 * </ul>
 * <p>
 * Each enum constant also holds its corresponding JCA (Java Cryptography Architecture)
 * standard name, which can be used when creating KeyFactory or Cipher instances.
 */
@Getter
@AllArgsConstructor
@ToString
public enum PublicKeyAlgorithm {
    RSA("RSA", RSAPublicKey.class),
    EC("EC", ECPublicKey.class),
    UNKNOWN("", null);

    private final String algorithmName;
    private final Class<? extends PublicKey> keyClass;
}

