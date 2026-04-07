package hirs.attestationca.persist.enums;

import lombok.Getter;
import java.util.Arrays;
import java.util.Optional;

/**
 * Translates EC curve names from TPM 2.0 specifications to Java curve names. Currently, only NIST curves are supported.
 * @see <a href="https://trustedcomputinggroup.org/resource/tpm-library-specification/">
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

    /** Constructor.
     * @param tpmId TPM 2.0 byte ID for EC curve
     * @param javaName Java name for EC curve
     */
    TpmEccCurve(final int tpmId, final String javaName) {
        this.tpmId = tpmId;
        this.javaName = javaName;
    }

    /**
     * Constructs an enum value from a given unsigned integer, corresponding to a TPMI_ECC_CURVE.
     * @param value the unsigned integer pertaining to a TPM EC curve ID
     * @return an {@link Optional} {@code TpmEccCurve} constructed from an unsigned integer
     */
    public static Optional<TpmEccCurve> fromTpmCurveId(final int value) {
        return Arrays.stream(values()).filter(v -> v.tpmId == value).findFirst();
    }

    /**
     * Constructs an enum value from a given Java name.
     * @param name the Java name of the EC curve
     * @return an {@link Optional} {@code TpmEccCurve} constructed from a Java name
     */
    public static Optional<TpmEccCurve> fromJavaName(final String name) {
        return Arrays.stream(values()).filter(v -> v.javaName.equalsIgnoreCase(name)).findFirst();
    }
}
