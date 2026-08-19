package hirs.attestationca.persist.provision.helper;

import hirs.attestationca.persist.enums.TcgAlgorithm;
import hirs.attestationca.persist.enums.TpmEccCurve;
import hirs.attestationca.persist.enums.TpmMlDsaParameterSet;
import hirs.attestationca.persist.enums.TpmMlKemParameterSet;

import java.math.BigInteger;
import java.security.PublicKey;
import java.security.spec.ECPoint;
import java.util.Optional;

/**
 * Interface representing a parsed TPMT_PUBLIC structure.
 * @see <a href="https://trustedcomputinggroup.org/resource/tpm-library-specification/">
 * Trusted Platform Module Library Part 2: Structures (TPM 2.0)</a>
 */
public sealed interface ParsedTpmPublic {
    /**
     * The public key algorithm.
     * @return the public key algorithm
     */
    TcgAlgorithm alg();
    /**
     * The name algorithm.
     * @return the name algorithm
     */
    TcgAlgorithm nameAlg();
    /**
     * Byte array containing the TPM public area.
     * @return the public area byte array
     */
    byte[] publicArea();
    /**
     * Public key formed from the TPM public area.
     * @return the constructed public key
     */
    PublicKey publicKey();

    /**
     * Gets the symmetric definition used by restricted decryption keys.
     *
     * @return parsed TPMT_SYM_DEF_OBJECT
     */
    Optional<SymmetricDefinition> symmetricDefinition();

    /**
     * Parsed TPMT_SYM_DEF_OBJECT.
     *
     * @param algorithm symmetric algorithm, or TPM_ALG_NULL
     * @param keyBits symmetric key size in bits
     * @param mode symmetric cipher mode, or TPM_ALG_NULL
     */
    record SymmetricDefinition(
            TcgAlgorithm algorithm,
            int keyBits,
            TcgAlgorithm mode
    ) { }

    /**
     * Parsed RSA parameters for the public area.
     * @param keyBits the number of RSA key bits
     * @param exponent the RSA exponent
     * @param modulus the RSA modulus
     */
    record RsaPublicParameters(
            int keyBits,
            BigInteger exponent,
            BigInteger modulus
    ) { }
    /**
     * Parsed ECC parameters for the public area.
     * @param curveId the ECC curve ID (a {@link TpmEccCurve})
     * @param point the {@link ECPoint} representing the point on the curve
     */
    record EccPublicParameters(
            TpmEccCurve curveId,
            ECPoint point
    ) { }
    /**
     * Parsed ML-KEM parameters for the public area.
     *
     * @param parameterSet ML-KEM parameter set
     * @param encodedPublicKey raw FIPS 203 encapsulation key
     */
    record MlKemPublicParameters(
            TpmMlKemParameterSet parameterSet,
            byte[] encodedPublicKey
    ) { }
    /**
     * Parsed ML-DSA parameters for the public area.
     *
     * @param parameterSet ML-DSA parameter set
     * @param encodedPublicKey raw FIPS 203 encapsulation key
     */
    record MlDsaPublicParameters(
            TpmMlDsaParameterSet parameterSet,
            byte[] encodedPublicKey
    ) { }
    /**
     * Parsed RSA data for the given TPM public area.
     * @param alg the {@link TcgAlgorithm} corresponding to the RSA algorithm used
     * @param nameAlg the {@link TcgAlgorithm} corresponding to the name algorithm
     * @param publicArea a byte array containing the public area contents
     * @param publicKey a {@link PublicKey} containing the constructed RSA key
     * @param symmetricDefinition restricted-decryption symmetric parameters
     * @param params contains RSA-specific {@link RsaPublicParameters}
     */
    record RsaParsedTpmPublic(
            TcgAlgorithm alg,
            TcgAlgorithm nameAlg,
            byte[] publicArea,
            PublicKey publicKey,
            Optional<SymmetricDefinition> symmetricDefinition,
            RsaPublicParameters params
    ) implements ParsedTpmPublic { }
    /**
     * Parsed ECC data for the given TPM public area.
     * @param alg the {@link TcgAlgorithm} corresponding to the ECC algorithm used
     * @param nameAlg the {@link TcgAlgorithm} corresponding to the name algorithm
     * @param publicArea a byte array containing the public area contents
     * @param publicKey a {@link PublicKey} containing the constructed ECC key
     * @param symmetricDefinition restricted-decryption symmetric parameters
     * @param params contains ECC-specific {@link EccPublicParameters}
     */
    record EccParsedTpmPublic(
            TcgAlgorithm alg,
            TcgAlgorithm nameAlg,
            byte[] publicArea,
            PublicKey publicKey,
            Optional<SymmetricDefinition> symmetricDefinition,
            EccPublicParameters params
    ) implements ParsedTpmPublic { }

    /**
     * Parsed ML-KEM public area for the given TPM public area.
     * @param alg the {@link TcgAlgorithm} corresponding to the ML-KEM algorithm used
     * @param nameAlg the {@link TcgAlgorithm} corresponding to the name algorithm
     * @param publicArea a byte array containing the public area contents
     * @param publicKey a {@link PublicKey} containing the constructed ML-KEM key
     * @param symmetricDefinition restricted-decryption symmetric parameters
     * @param params contains ML-KEM-specific {@link MlKemPublicParameters}
     */
    record MlKemParsedTpmPublic(
            TcgAlgorithm alg,
            TcgAlgorithm nameAlg,
            byte[] publicArea,
            PublicKey publicKey,
            Optional<SymmetricDefinition> symmetricDefinition,
            MlKemPublicParameters params
    ) implements ParsedTpmPublic { }


    /**
     * Parsed ML-DSA public area for the given TPM public area.
     *
     * @param alg        the {@link TcgAlgorithm} corresponding to the ML-DSA algorithm used
     * @param nameAlg    the {@link TcgAlgorithm} corresponding to the name algorithm
     * @param publicArea a byte array containing the public area contents
     * @param publicKey  a {@link PublicKey} containing the constructed ML-DSA key
     * @param symmetricDefinition ignored for ML-DSA
     * @param params     contains ML-DSA-specific {@link MlDsaPublicParameters}
     */
    record MlDsaParsedTpmPublic(
            TcgAlgorithm alg,
            TcgAlgorithm nameAlg,
            byte[] publicArea,
            PublicKey publicKey,
            Optional<SymmetricDefinition> symmetricDefinition,
            MlDsaPublicParameters params
    ) implements ParsedTpmPublic { }
}

