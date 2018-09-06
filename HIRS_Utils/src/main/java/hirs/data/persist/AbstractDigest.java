package hirs.data.persist;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;

/**
 * This abstract class represents a message digest. Extending classes include
 * {@link Digest} and {@link OptionalDigest}.
 * <p>
 * Two classes were made to facilitate persisting them with Hibernate in different ways.
 * To persist non-nullable entries in an embedded collection, use {@link Digest} (see
 * {@link TPMBaseline} for reference.)  To persist nullable entries, use {@link OptionalDigest}
 * (see {@link ImaBlacklistRecord} for reference.)
 */
public abstract class AbstractDigest {
    /**
     * Length of MD2 digest.
     */
    public static final int MD2_DIGEST_LENGTH = 16;
    /**
     * Length of MD5 digest.
     */
    public static final int MD5_DIGEST_LENGTH = 16;
    /**
     * Length of SHA1 digest.
     */
    public static final int SHA1_DIGEST_LENGTH = 20;
    /**
     * Length of SHA256 digest.
     */
    public static final int SHA256_DIGEST_LENGTH = 32;
    /**
     * Length of SHA384 digest.
     */
    public static final int SHA384_DIGEST_LENGTH = 48;
    /**
     * Length of SHA512 digest.
     */
    public static final int SHA512_DIGEST_LENGTH = 64;

    /**
     * Ensures the given algorithm type and digest byte array represent a valid digest.
     * This includes ensuring they are both not null or empty and ensuring that the length of the
     * digest matches the expected amount of data for the given algorithm.
     *
     * @param algorithm a digest algorithm
     * @param digest the digest computed by this algorithm
     * @throws IllegalArgumentException if the provided input does not represent a valid digest
     */
    void validateInput(final DigestAlgorithm algorithm, final byte[] digest)
            throws IllegalArgumentException {
        if (algorithm == null) {
            throw new IllegalArgumentException("Algorithm must not be null");
        }

        if (ArrayUtils.isEmpty(digest)) {
            final String msg = "Digest must have at least one byte";
            throw new IllegalArgumentException(msg);
        }

        switch (algorithm) {
            case MD2:
                if (digest.length != MD2_DIGEST_LENGTH) {
                    throw new AbstractDigest.IllegalDigestLength(algorithm, digest);
                }
                break;
            case MD5:
                if (digest.length != MD5_DIGEST_LENGTH) {
                    throw new AbstractDigest.IllegalDigestLength(algorithm, digest);
                }
                break;
            case SHA1:
                if (digest.length != SHA1_DIGEST_LENGTH) {
                    throw new AbstractDigest.IllegalDigestLength(algorithm, digest);
                }
                break;
            case SHA256:
                if (digest.length != SHA256_DIGEST_LENGTH) {
                    throw new AbstractDigest.IllegalDigestLength(algorithm, digest);
                }
                break;
            case SHA384:
                if (digest.length != SHA384_DIGEST_LENGTH) {
                    throw new AbstractDigest.IllegalDigestLength(algorithm, digest);
                }
                break;
            case SHA512:
                if (digest.length != SHA512_DIGEST_LENGTH) {
                    throw new AbstractDigest.IllegalDigestLength(algorithm, digest);
                }
                break;
            default:
                throw new IllegalArgumentException("Digest length does not match algorithm type");
        }
    }

    /**
     * Retrieves the <code>DigestAlgorithm</code> that identifies which hash
     * function generated the digest.
     *
     * @return digest algorithm
     */
    public abstract DigestAlgorithm getAlgorithm();

    /**
     * Retrieves the digest.
     *
     * @return digest
     */
    public abstract byte[] getDigest();

    /**
     * Returns a hex <code>String</code> representing the binary digest.
     *
     * @return hex representation of digest
     */
    public String getDigestString() {
        return Hex.encodeHexString(getDigest());
    }

    /**
     * Compares this digest's hash with another digest's hash.
     * @param otherDigest a Digest to compare to.
     * @return the comparison result type.
     */
    public DigestComparisonResultType compare(final Digest otherDigest) {
        if (null == otherDigest) {
            return DigestComparisonResultType.UNKNOWN;
        }

        // if either byte array is all zeroes or the digest of an empty buffer, then the comparison
        // result is UNKNOWN because the associated file was not read properly by IMA
        if (isUnknownHash(this) || isUnknownHash(otherDigest)) {
            return DigestComparisonResultType.UNKNOWN;
        }

        if (this.equals(otherDigest)) {
            return DigestComparisonResultType.MATCH;
        }

        return DigestComparisonResultType.MISMATCH;
    }

    /**
     * This method determines whether the given hash should be treated as 'unknown'.
     * Current unknown/invalid hashes are measurements that are recorded in the IMA log that are:
     * - all zero
     * - the SHA1 digest of an empty buffer (no data)
     *
     * @param digest the digest that should be evaluated
     * @return true if the given digest should be treated as invalid, false otherwise
     */
    private static boolean isUnknownHash(final AbstractDigest digest) {
        return digest.equals(Digest.SHA1_ALL_ZERO) || digest.equals(Digest.SHA1_EMPTY);
    }

    /**
     * Parses a {@link DigestAlgorithm} from a String returned by {@link AbstractDigest#toString()}.
     *
     * @param digest the digest string as computed above
     * @return the DigestAlgorithm component of the String
     */
    static DigestAlgorithm algorithmFromString(final String digest) {
        return DigestAlgorithm.findByString(matchString(digest).group(1));
    }

    /**
     * Parses a digest from a String returned by {@link AbstractDigest#toString()}.
     *
     * @param digest the digest string as computed above
     * @return the byte array representing the actual digest
     */
    static byte[] digestFromString(final String digest) {
        return DatatypeConverter.parseHexBinary(matchString(digest).group(2));
    }

    private static Matcher matchString(final String digest) {
        Pattern digestPattern = Pattern.compile("(.*) - 0x(.*)");
        Matcher matcher = digestPattern.matcher(digest);
        if (!matcher.matches()) {
            String message = String.format("String \"%s\" did not match pattern \"%s\"", digest,
                    digestPattern.toString());
            throw new IllegalArgumentException(message);
        }
        return matcher;
    }

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + getAlgorithm().hashCode();
        result = prime * result + Arrays.hashCode(getDigest());
        return result;
    }

    @Override
    public final boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof AbstractDigest)) {
            return false;
        }

        AbstractDigest other = (AbstractDigest) obj;

        if (getAlgorithm() != other.getAlgorithm()) {
            return false;
        }

        if (!Arrays.equals(getDigest(), other.getDigest())) {
            return false;
        }

        return true;
    }

    /**
     * Returns the standard algorithm name and a hexadecimal representation of
     * the bytes.
     *
     * @return string representation
     */
    @Override
    public String toString() {
        //NOTE: Any updates here should also be reflected in fromString()
        return String.format("%s - 0x%s", getAlgorithm().getStandardAlgorithmName(),
                Hex.encodeHexString(getDigest()));
    }

    private static final class IllegalDigestLength extends
            IllegalArgumentException {

        private static final long serialVersionUID = 8782184397041237374L;

        private IllegalDigestLength(final DigestAlgorithm algorithm,
                final byte[] digest) {
            super(String.format(
                    "digest length (%d) does not match that of algorithm (%s)",
                    digest.length, algorithm.toString()));
        }
    }
}
