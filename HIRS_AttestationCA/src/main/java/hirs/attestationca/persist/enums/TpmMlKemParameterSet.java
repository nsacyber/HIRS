package hirs.attestationca.persist.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

/**
 * ML-KEM parameter sets defined by the TPM 2.0 Library Specification, Version 185.
 */
@Getter
@AllArgsConstructor
public enum TpmMlKemParameterSet {
    /** ML-KEM-512. */
    ML_KEM_512(0x0001, "ML-KEM-512", 800, 768),
    /** ML-KEM-768. */
    ML_KEM_768(0x0002, "ML-KEM-768", 1184, 1088),
    /** ML-KEM-1024. */
    ML_KEM_1024(0x0003, "ML-KEM-1024", 1568, 1568);

    private final int tpmParameterSetId;
    private final String algorithmName;
    private final int publicKeySize;
    private final int ciphertextSize;

    /**
     * Resolves a TPM ML-KEM parameter set identifier.
     *
     * @param parameterSetId numeric TPM parameter-set identifier
     * @return matching parameter set, if supported
     */
    public static Optional<TpmMlKemParameterSet> fromId(final int parameterSetId) {
        for (TpmMlKemParameterSet parameterSet : values()) {
            if (parameterSet.tpmParameterSetId == parameterSetId) {
                return Optional.of(parameterSet);
            }
        }
        return Optional.empty();
    }
}
