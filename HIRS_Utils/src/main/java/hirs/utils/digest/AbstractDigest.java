package hirs.utils.digest;

import jakarta.xml.bind.DatatypeConverter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.core.util.ArrayUtils;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This abstract class represents a message digest. Extending classes include
 * {@link hirs.utils.digest.Digest} and {@link hirs.utils.digest.OptionalDigest}.
 * <p>
 * Two classes were made to facilitate persisting them with Hibernate in different ways.
 * To persist non-nullable entries in an embedded collection, use {@link hirs.utils.digest.Digest} (see
 * {@link TPMBaseline} for reference.)  To persist nullable entries,
 * use {@link hirs.utils.digest.OptionalDigest} (see {@link ImaBlacklistRecord} for reference.)
 */
@Log4j2
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
     * This method will help class determine the algorithm associated with the
     * pcr values given.
     *
     * @param digest list of pcr values.
     * @return the associated algorithm.
     */
    public static final DigestAlgorithm getDigestAlgorithm(final byte[] digest) {
        if (digest == null || ArrayUtils.isEmpty(digest)) {
            return DigestAlgorithm.UNSPECIFIED;
        }

        switch (digest.length) {
            case MD2_DIGEST_LENGTH:
                return DigestAlgorithm.MD5;
            case SHA1_DIGEST_LENGTH:
                return DigestAlgorithm.SHA1;
            case SHA256_DIGEST_LENGTH:
                return DigestAlgorithm.SHA256;
            case SHA384_DIGEST_LENGTH:
                return DigestAlgorithm.SHA384;
            case SHA512_DIGEST_LENGTH:
                return DigestAlgorithm.SHA512;
            default:
                return DigestAlgorithm.UNSPECIFIED;
        }
    }

    /**
     * This method will help class determine the algorithm associated with the
     * pcr values given.
     *
     * @param digest list of pcr values.
     * @return the associated algorithm.
     */
    public static final DigestAlgorithm getDigestAlgorithm(final String digest) {
        try {
            return getDigestAlgorithm(Hex.decodeHex(digest.toCharArray()));
        } catch (Exception deEx) {
            log.error(deEx);
        }

        return DigestAlgorithm.UNSPECIFIED;
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
                    digestPattern);
            throw new IllegalArgumentException(message);
        }
        return matcher;
    }

    /**
     * Ensures the given algorithm type and digest byte array represent a valid digest.
     * This includes ensuring they are both not null or empty and ensuring that the length of the
     * digest matches the expected amount of data for the given algorithm.
     *
     * @param algorithm a digest algorithm
     * @param digest    the digest computed by this algorithm
     * @throws IllegalArgumentException if the provided input does not represent a valid digest
     */
    void validateInput(final DigestAlgorithm algorithm, final byte[] digest)
            throws IllegalArgumentException {
        if (algorithm == null) {
            throw new IllegalArgumentException("Algorithm must not be null");
        }

        if (ArrayUtils.isEmpty(digest)) {
            throw new IllegalArgumentException("Digest must have at least one byte");
        }

        if (digest.length != algorithm.getLengthInBytes()) {
            throw new AbstractDigest.IllegalDigestLength(algorithm, digest);
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
     *
     * @param otherDigest a Digest to compare to.
     * @return the comparison result type.
     */
    public DigestComparisonResultType compare(final Digest otherDigest) {
        if (null == otherDigest) {
            return DigestComparisonResultType.UNKNOWN;
        }

        if (this.equals(otherDigest)) {
            return DigestComparisonResultType.MATCH;
        }

        return DigestComparisonResultType.MISMATCH;
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

        if (obj == null || !(obj instanceof AbstractDigest other)) {
            return false;
        }

        if (getAlgorithm() != other.getAlgorithm()) {
            return false;
        }

        return Arrays.equals(getDigest(), other.getDigest());
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
