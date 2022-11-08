package hirs.data.persist;

import hirs.data.persist.enums.DigestAlgorithm;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;
import java.util.Arrays;

/**
 * This class represents a message digest. This stores the bytes of a message
 * digest as computed by a hash function.
 * <p>
 * This class differs from Java's provided <code>MessageDigest</code> class by the
 * fact that it does not compute a digest. This class simply stores the result
 * of a digest. This is useful for scenarios where the digest is already known.
 * This is the case for IMA reports that already have the digest computed. The
 * <code>MessageDigest</code> class does not provide a means to store that value.
 * The value must be computed.
 */
@Embeddable
@Access(AccessType.FIELD)
public final class Digest extends AbstractDigest implements Serializable {
    /**
     * A SHA1 digest whose content is all zeros.
     */
    public static final Digest SHA1_ZERO = new Digest(
            DigestAlgorithm.SHA1,
            new byte[SHA1_DIGEST_LENGTH]
    );

    private static final String SHA1_EMPTY_HEX =
            "da39a3ee5e6b4b0d3255bfef95601890afd80709";

    /**
     * A SHA1 digest whose content is the hash of an empty buffer.
     */
    public static final Digest SHA1_OF_NO_DATA;

    static {
        try {
            SHA1_OF_NO_DATA = new Digest(
                    DigestAlgorithm.SHA1,
                    Hex.decodeHex(SHA1_EMPTY_HEX.toCharArray())
            );
        } catch (DecoderException e) {
            throw new RuntimeException("Could not decode hex value", e);
        }
    }

    @XmlElement
    @Column(nullable = false, name = "digest", length = SHA512_DIGEST_LENGTH,
            columnDefinition = "varbinary(64)")
    private final byte[] digest;

    @XmlElement
    @Column(nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private final DigestAlgorithm algorithm;

    /**
     * Creates a new <code>Digest</code>.
     *
     * @param algorithm algorithm used to generate the digest
     * @param digest digest value
     * @throws IllegalArgumentException if digest length does not match that of the algorithm
     */
    public Digest(final DigestAlgorithm algorithm, final byte[] digest)
            throws IllegalArgumentException {
        validateInput(algorithm, digest);
        this.algorithm = algorithm;
        this.digest = Arrays.copyOf(digest, digest.length);
    }

    /**
     * Creates a new <code>Digest</code> when an algorithm isn't specified.
     * @param digest byte array value
     */
    public Digest(final byte[] digest) {
        this(AbstractDigest.getDigestAlgorithm(digest), digest);
    }

    /**
     * Default constructor necessary for Hibernate.
     */
    protected Digest() {
        this.algorithm = null;
        this.digest = null;
    }

    /**
     * Retrieves the <code>DigestAlgorithm</code> that identifies which hash
     * function generated the digest.
     *
     * @return digest algorithm
     */
    @Override
    public DigestAlgorithm getAlgorithm() {
        return this.algorithm;
    }

    /**
     * Retrieves the digest.
     *
     * @return digest
     */
    @Override
    public byte[] getDigest() {
        return Arrays.copyOf(this.digest, this.digest.length);
    }

    /**
     * Returns a new Digest with the same attributes as this instance.
     *
     * @return a new equivalent Digest
     */
    public OptionalDigest asOptionalDigest() {
        return new OptionalDigest(algorithm, digest);
    }

    /**
     * Helper method to reverse the toString method. Returns a Digest given a String
     * that was created using an AbstractDigest's toString method.
     *
     * @param digest String representation of an AbstractDigest
     * @return Digest object recreated from the String passed in
     */
    public static Digest fromString(final String digest) {
        return new Digest(algorithmFromString(digest), digestFromString(digest));
    }
}
