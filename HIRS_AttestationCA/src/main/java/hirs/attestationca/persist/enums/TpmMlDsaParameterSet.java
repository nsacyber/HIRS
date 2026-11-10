package hirs.attestationca.persist.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

/**
 * ML-DSA parameter sets defined by the TPM 2.0 Library Specification, Version 185.
 */
@Getter
@AllArgsConstructor
public enum TpmMlDsaParameterSet {
    /** ML-DSA-44. */
    ML_DSA_44(0x0001, "ML-DSA-44", 1312, 2420),
    /** ML-DSA-65. */
    ML_DSA_65(0x0002, "ML-DSA-65", 1952, 3309),
    /** ML-DSA-87. */
    ML_DSA_87(0x0003, "ML-DSA-87", 2592, 4627);

    private final int tpmParameterSetId;
    private final String algorithmName;
    private final int publicKeySize;
    private final int signatureSize;

    /**
     * Resolves a TPM ML-DSA parameter set identifier.
     *
     * @param parameterSetId numeric TPM parameter-set identifier
     * @return matching parameter set, if supported
     */
    public static Optional<TpmMlDsaParameterSet> fromId(final int parameterSetId) {
        for (TpmMlDsaParameterSet parameterSet : values()) {
            if (parameterSet.tpmParameterSetId == parameterSetId) {
                return Optional.of(parameterSet);
            }
        }
        return Optional.empty();
    }
}
