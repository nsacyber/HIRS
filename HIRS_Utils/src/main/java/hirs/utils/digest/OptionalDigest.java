package hirs.utils.digest;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Getter;

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
    @Column(name = "digest", length = SHA512_DIGEST_LENGTH,
            columnDefinition = "varbinary(64)")
    private final byte[] digest;

    @XmlElement
    @Column
    @Enumerated(EnumType.ORDINAL)
    @Getter
    private final DigestAlgorithm algorithm;

    /**
     * Creates a new <code>OptionalDigest</code>.
     *
     * @param digestAlgorithm algorithm used to generate the digest
     * @param optionalDigest  digest value
     * @throws IllegalArgumentException if digest length does not match that of the algorithm
     */
    public OptionalDigest(final DigestAlgorithm digestAlgorithm, final byte[] optionalDigest)
            throws IllegalArgumentException {
        validateInput(digestAlgorithm, optionalDigest);
        this.algorithm = digestAlgorithm;
        this.digest = Arrays.copyOf(optionalDigest, optionalDigest.length);
    }

    /**
     * Default constructor necessary for Hibernate.
     */
    private OptionalDigest() {
        this.algorithm = null;
        this.digest = null;
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
}
