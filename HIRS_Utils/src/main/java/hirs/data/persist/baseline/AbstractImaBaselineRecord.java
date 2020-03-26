package hirs.data.persist.baseline;

import hirs.data.persist.Digest;
import hirs.data.persist.enums.DigestAlgorithm;
import hirs.data.persist.OptionalDigest;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * An <code>AbstractImaBaselineRecord</code> represents an entry in a baseline used
 * during appraisal of a machine's IMA log.  These records indicate paths
 * and/or hashes that may be acceptable, ignored, or blacklisted, according
 * to their uses.
 * <p>
 * Known extending classes include:
 * - {@link IMABaselineRecord}
 * - {@link ImaIgnoreSetRecord}
 * - {@link ImaBlacklistRecord}
 */
@MappedSuperclass
public abstract class AbstractImaBaselineRecord {
    private static final Logger LOGGER = LogManager.getLogger(AbstractImaBaselineRecord.class);

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private final Long id;

    /**
     * Holds the name of the 'path' field.
     */
    public static final String PATH_FIELD = "path";

    @SuppressWarnings("checkstyle:magicnumber")
    @Column(name = PATH_FIELD, nullable = true, length = 2048)
    private final String path;

    /**
     * Holds the name of the 'hash' field.
     */
    public static final String HASH_FIELD = "hash";

    @Embedded
    private final OptionalDigest hash;

    @SuppressWarnings("checkstyle:magicnumber")
    @Column(nullable = true, length = 255)
    private final String description;

    /**
     * Creates a new <code>ImaBaseRecord</code>. Creates a new record and
     * specifies all of the properties. All of the properties may be null
     * except for path and hash.
     *
     * @param path
     *            file path
     * @param hash
     *            file SHA-1 hash
     * @param description
     *            a description for this baseline entry
     * @throws IllegalArgumentException
     *             if digest algorithm is not SHA-1
     */
    public AbstractImaBaselineRecord(final String path, final Digest hash, final String description)
            throws IllegalArgumentException {
        if (hash != null && hash.getAlgorithm() != DigestAlgorithm.SHA1) {
            throw new IllegalArgumentException("Hash algorithm is not SHA-1");
        }
        this.id = null;
        this.path = path;
        if (path != null && StringUtils.isBlank(path)) {
            throw new IllegalArgumentException("Path is blank");
        }
        if (hash != null) {
            this.hash = hash.asOptionalDigest();
        } else {
            this.hash = null;
        }
        this.description = description;
    }

    /**
     * Default constructor necessary for Hibernate.
     */
    protected AbstractImaBaselineRecord() {
        this.id = null;
        this.path = null;
        this.hash = null;
        this.description = null;
    }

    /**
     * Returns the database ID associated with this record. If this record has
     * not been persisted in the database then this method will return null.
     *
     * @return ID of this record
     */
    public final Long getId() {
        return id;
    }

    /**
     * Returns the path (including file name) of the IMA baseline record.
     *
     * @return file path of baseline record
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns the SHA1 hash of the file associated with IMA baseline record.
     *
     * @return hash of file associated with baseline record
     */
    @Transient
    public Digest getHash() {
        if (hash != null) {
            return hash.asDigest();
        } else {
            return null;
        }
    }

    /**
     * Get the partial path associated with this baseline record; may be null.
     *
     * @return the partial path associated with this baseline record; may be null.
     */
    public String getPartialPath() {
        if (path == null) {
            return null;
        }
        return getPartialPath(path);
    }

    /**
     * Returns the partial path (file name) of the given Path, represented by a string.
     *
     * @param path the path for which to generate a partial path
     * @return file name
     */
    public static String getPartialPath(final String path) {
        if (path == null) {
            throw new IllegalArgumentException("Cannot get partial path for null value");
        }
        Path filename = Paths.get(path).getFileName();
        //should only be triggered if path is '/' directory
        if (filename == null) {
            LOGGER.error("Invalid filename from path: " + path);
            return "";
        }
        return filename.toString();
    }


    /**
     * Returns the description of the file associated with IMA ignore baseline record.
     *
     * @return hash of file associated with baseline record
     */
    public final String getDescription() {
        return this.description;
    }

    @Override
    public final boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractImaBaselineRecord that = (AbstractImaBaselineRecord) o;
        return Objects.equals(path, that.path)
                && Objects.equals(hash, that.hash);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(path, hash);
    }

    @Override
    public final String toString() {
        return String.format("(%s, %s)", path, hash);
    }
}
