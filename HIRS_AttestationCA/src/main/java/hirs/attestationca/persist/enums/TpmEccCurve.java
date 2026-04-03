package hirs.attestationca.persist.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * Translates EC curve names from TPM 2.0 specifications to Java curve names.
 * @see <a href="https://trustedcomputinggroup.org/wp-content/uploads/TCG_TPM2_r1p59_Part2_Structures_pub.pdf">
 *     Trusted Platform Module Library Part 2: Structures (TPM 2.0)</a>
 */
@Getter
public enum TpmEccCurve {
    /** Corresponds to a P192 EC curve. */
    NIST_P192(0x00000001, "secp192r1"),
    /** Corresponds to a P224 EC curve. */
    NIST_P224(0x00000002, "secp224r1"),
    /** Corresponds to a P256 EC curve. */
    NIST_P256(0x00000003, "secp256r1"),
    /** Corresponds to a P384 EC curve. */
    NIST_P384(0x00000004, "secp384r1"),
    /** Corresponds to a P521 EC curve. */
    NIST_P521(0x00000005, "secp521r1");

    private final int tpmId;
    private final String javaName;

    /** Constructor. */
    TpmEccCurve(int tpmId, String javaName) {
        this.tpmId = tpmId;
        this.javaName = javaName;
    }

    /**
     * Constructs an enum value from a given unsigned integer, corresponding to a TPMI_ECC_CURVE inside a
     * TPM public area.
     * @return a TpmEcCurve constructed from an unsigned integer, or null otherwise
     */
    public static Optional<TpmEccCurve> fromTpmCurveId(int value) {
        return Arrays.stream(values())
                .filter(v -> v.tpmId == value)
                .findFirst();
    }

    /**
     * Constructs an enum value from a given Java name.
     * @return a TpmEcCurve constructed from a Java name, or null otherwise
     */
    public static Optional<TpmEccCurve> fromJavaName(String name) {
        return Arrays.stream(values())
                .filter(v -> v.javaName.equalsIgnoreCase(name))
                .findFirst();
    }
}