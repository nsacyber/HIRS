package hirs.attestationca.persist.provision.helper;

import hirs.attestationca.persist.enums.TcgAlgorithm;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Helper class that handles TPM name computation for public area data.
 * @see <a href="https://trustedcomputinggroup.org/resource/tpm-library-specification/">
 *     TPM 2.0 Library Part 1: Architecture</a>
 */
public final class TpmNameHelper {
    /** Prevent instantiation. */
    private TpmNameHelper() { }

    /**
     * Computes the TPM name for a given parsed TPM public area region. See Section 16 of the <i>TPM 2.0 Library
     * Part 1: Architecture</i> specification.
     * @param pub the parsed TPM public area containing a byte array and name algorithm
     * @return the computed TPM name byte array
     */
    public static byte[] computeName(final ParsedTpmPublic pub) {
        MessageDigest messageDigest = getMessageDigest(pub.nameAlg());
        byte[] digest = messageDigest.digest(pub.publicArea());
        ByteBuffer out = ByteBuffer.allocate(Short.BYTES + digest.length);
        out.putShort((short) pub.nameAlg().getAlgorithmId());
        out.put(digest);
        return out.array();
    }

    private static MessageDigest getMessageDigest(final TcgAlgorithm alg) {
        final String javaName = switch (alg) {
            case SHA256, SHA384, SHA512 -> alg.getAlgorithmName();
            default -> throw new IllegalArgumentException("Unsupported TPM name algorithm: " + alg);
        };

        try {
            return MessageDigest.getInstance(javaName);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Digest algorithm unavailable: " + javaName, e);
        }
    }
}
