package hirs.attestationca.persist.provision.helper;

import hirs.attestationca.persist.enums.TcgAlgorithm;
import hirs.attestationca.persist.enums.TpmEccCurve;

import java.math.BigInteger;
import java.security.PublicKey;
import java.security.spec.ECPoint;

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
     * Parsed RSA data for the given TPM public area.
     * @param alg the {@link TcgAlgorithm} corresponding to the RSA algorithm used
     * @param nameAlg the {@link TcgAlgorithm} corresponding to the name algorithm
     * @param publicArea a byte array containing the public area contents
     * @param publicKey a {@link PublicKey} containing the constructed RSA key
     * @param params contains RSA-specific {@link RsaPublicParameters}
     */
    record RsaParsedTpmPublic(
            TcgAlgorithm alg,
            TcgAlgorithm nameAlg,
            byte[] publicArea,
            PublicKey publicKey,
            RsaPublicParameters params
    ) implements ParsedTpmPublic { }
    /**
     * Parsed ECC data for the given TPM public area.
     * @param alg the {@link TcgAlgorithm} corresponding to the ECC algorithm used
     * @param nameAlg the {@link TcgAlgorithm} corresponding to the name algorithm
     * @param publicArea a byte array containing the public area contents
     * @param publicKey a {@link PublicKey} containing the constructed ECC key
     * @param params contains ECC-specific {@link RsaPublicParameters}
     */
    record EccParsedTpmPublic(
            TcgAlgorithm alg,
            TcgAlgorithm nameAlg,
            byte[] publicArea,
            PublicKey publicKey,
            EccPublicParameters params
    ) implements ParsedTpmPublic { }
}

