package hirs.attestationca.persist.provision.helper;

import hirs.attestationca.persist.enums.PublicKeyAlgorithm;
import hirs.attestationca.persist.enums.TpmEccCurve;

import java.math.BigInteger;
import java.security.PublicKey;
import java.security.spec.ECPoint;

/** Interface representing a parsed TPM2B_PUBLIC structure.
 *  @see <a href="https://trustedcomputinggroup.org/resource/tpm-library-specification/">
 *  Trusted Platform Module Library Part 2: Structures (TPM 2.0)</a>
 */
public sealed interface ParsedTpmPublic {
    /** The public key algorithm. */
    PublicKeyAlgorithm alg();
    /** The name algorithm. */
    PublicKeyAlgorithm nameAlg();
    /** Byte array containing the TPM public area. */
    byte[] publicArea();
    /** Public key formed from the TPM public area. */
    PublicKey publicKey();

    /** Parsed RSA parameters for the public area.
     * @param keyBits the number of RSA key bits
     * @param exponent the RSA exponent
     * @param modulus the RSA modulus
     */
    record RsaPublicParameters(
            int keyBits,
            BigInteger exponent,
            BigInteger modulus
    ) { }
    /** Parsed ECC parameters for the public area.
     * @param curveId the ECC curve ID (a {@link TpmEccCurve})
     * @param point the {@link ECPoint} representing the point on the curve
     */
    record EccPublicParameters(
            TpmEccCurve curveId,
            ECPoint point
    ) { }

    /** Parsed RSA data for the given TPM public area.
     * @param alg the {@link PublicKeyAlgorithm} corresponding to the RSA algorithm used
     * @param nameAlg the {@link PublicKeyAlgorithm} corresponding to the name algorithm
     * @param publicArea a byte array containing the public area contents
     * @param publicKey a {@link PublicKey} containing the constructed RSA key
     * @param params contains RSA-specific {@link RsaPublicParameters}
     */
    record RsaParsedTpmPublic(
            PublicKeyAlgorithm alg,
            PublicKeyAlgorithm nameAlg,
            byte[] publicArea,
            PublicKey publicKey,
            RsaPublicParameters params
    ) implements ParsedTpmPublic { }
    /** Parsed ECC data for the given TPM public area.
     * @param alg the {@link PublicKeyAlgorithm} corresponding to the ECC algorithm used
     * @param nameAlg the {@link PublicKeyAlgorithm} corresponding to the name algorithm
     * @param publicArea a byte array containing the public area contents
     * @param publicKey a {@link PublicKey} containing the constructed ECC key
     * @param params contains ECC-specific {@link RsaPublicParameters}
     */
    record EccParsedTpmPublic(
            PublicKeyAlgorithm alg,
            PublicKeyAlgorithm nameAlg,
            byte[] publicArea,
            PublicKey publicKey,
            EccPublicParameters params
    ) implements ParsedTpmPublic { }
}

