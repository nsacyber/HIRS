package hirs.data.persist;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.xml.bind.annotation.XmlElement;
import java.util.Arrays;

/**
 * This class is identical to {@link Digest} except its fields are nullable.  However, in practice,
 * an instance of this class cannot have null values assigned to its fields.  The fields are marked
 * as nullable to allow Hibernate to set a reference an embedded instance of this class to null
 * (as there is no way for Hibernate to distinguish between a null reference and completely
 * null fields on an embedded entity.)  Otherwise, there is no operational difference between
 * this class and {@link Digest}.
 */
@Embeddable
@Access(AccessType.FIELD)
public final class OptionalDigest extends AbstractDigest {
    @XmlElement
    @Column(nullable = true, name = "digest", length = SHA512_DIGEST_LENGTH,
            columnDefinition = "varbinary(64)")
    private final byte[] digest;

    @XmlElement
    @Column(nullable = true)
    @Enumerated(EnumType.ORDINAL)
    private final DigestAlgorithm algorithm;

    /**
     * Creates a new <code>OptionalDigest</code>.
     *
     * @param algorithm algorithm used to generate the digest
     * @param digest digest value
     * @throws IllegalArgumentException if digest length does not match that of the algorithm
     */
    public OptionalDigest(final DigestAlgorithm algorithm, final byte[] digest)
            throws IllegalArgumentException {
        validateInput(algorithm, digest);
        this.algorithm = algorithm;
        this.digest = Arrays.copyOf(digest, digest.length);
    }

    /**
     * Default constructor necessary for Hibernate.
     */
    protected OptionalDigest() {
        this.algorithm = null;
        this.digest = null;
    }

    /**
     * Returns the <code>DigestAlgorithm</code> that identifies which hash
     * function generated the digest.
     *
     * @return digest algorithm
     */
    @Override
    public DigestAlgorithm getAlgorithm() {
        return algorithm;
    }

    /**
     * Returns the digest.
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
    public Digest asDigest() {
        return new Digest(algorithm, digest);
    }

    /**
     * Helper method to reverse the toString method. Returns an OptionalDigest given a String
     * that was created using an AbstractDigest's toString method.
     *
     * @param digest String representation of an AbstractDigest
     * @return OptionalDigest object recreated from the String passed in
     */
    public static OptionalDigest fromString(final String digest) {
        return new OptionalDigest(algorithmFromString(digest), digestFromString(digest));
    }
}
