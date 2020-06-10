package hirs.data.persist;

import hirs.data.persist.baseline.ImaBlacklistBaseline;
import hirs.data.persist.baseline.AbstractImaBaselineRecord;
import hirs.data.persist.enums.AlertType;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * This class holds information about blacklisted paths and hashes that, if found in a machine's
 * IMA log, would be adverse indicators of the integrity of that machine.  This class is intended
 * to be flexible, and as such, any instance may include only a path, only a hash, or both.
 * For blacklist records that contain only one of these two pieces of information, a measurement
 * record will be considered as matching if its respective path or hash matches that record.
 * For blacklist records that contain both pieces of information, only measurement records
 * that contain both a matching path and hash will be considered as matching.
 */
@Entity
public class ImaBlacklistRecord extends AbstractImaBaselineRecord {
    /**
     * Referenced in DbImaBlacklistBaselineRecordManager.iterateOverBaselineRecords().
     */
    public static final int FILENAME_HASH_BUCKET_COUNT = 4;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ima_baseline_id")
    private ImaBlacklistBaseline baseline;

    /**
     * Construct a new ImaBlacklistRecord that blacklists any file at the given path.
     *
     * @param path the path to blacklist
     */
    public ImaBlacklistRecord(final String path) {
        this(path, null, null, null);
    }

    /**
     * Construct a new ImaBlacklistRecord that blacklists any file at the given path.
     *
     * @param path the path to blacklist
     * @param description a description of the given path, may be null
     */
    public ImaBlacklistRecord(final String path, final String description) {
        this(path, null, description, null);
    }

    /**
     * Construct a new ImaBlacklistRecord that blacklists any file with the given hash.
     *
     * @param hash the hash to blacklist
     */
    public ImaBlacklistRecord(final Digest hash) {
        this(null, hash, null, null);
    }

    /**
     * Construct a new ImaBlacklistRecord that blacklists any file with the given hash.
     *
     * @param hash the hash to blacklist
     * @param description a description of the given hash. may be null
     */
    public ImaBlacklistRecord(final Digest hash, final String description) {
        this(null, hash, description, null);
    }

    /**
     * Construct a new ImaBlacklistRecord that blacklists a file at the given path with the given
     * hash.
     *
     * @param path the path to blacklist
     * @param hash the hash to blacklist
     */
    public ImaBlacklistRecord(final String path, final Digest hash) {
        this(path, hash, null, null);
    }

    /**
     * Construct a new ImaBlacklistRecord with the given parameters.  Either a path or hash,
     * or both, may be provided, as well as a description.
     *
     * If:
     *  - a non-null path and a null hash is provided, any file matching the path should be
     *  considered as blacklisted
     *  - a null path and a non-null hash is provided, any file whose hash matches the given hash
     *  should be considered as blacklisted
     *  - a non-null path and a non-null hash is provided, a file that has both a matching path
     *  and hash should be considered as blacklisted
     *
     *  This class cannot be instantiated with both a null path and hash.
     *
     * @param path a blacklisted path, as described above
     * @param hash a blacklisted hash, as described above
     * @param description a description of the nature of the blacklist record, may be null
     */
    public ImaBlacklistRecord(
            final String path,
            final Digest hash,
            final String description) {
        this(path, hash, description, null);
    }

    /**
     * Construct a new ImaBlacklistRecord with the given parameters.  Either a path or hash,
     * or both, may be provided, as well as a description.
     *
     * If:
     *  - a non-null path and a null hash is provided, any file matching the path should be
     *  considered as blacklisted
     *  - a null path and a non-null hash is provided, any file whose hash matches the given hash
     *  should be considered as blacklisted
     *  - a non-null path and a non-null hash is provided, a file that has both a matching path
     *  and hash should be considered as blacklisted
     *
     *  This class cannot be instantiated with both a null path and hash.
     *
     * @param path a blacklisted path, as described above
     * @param hash a blacklisted hash, as described above
     * @param description a description of the nature of the blacklist record, may be null
     * @param baseline the baseline that this record belongs to, may be null
     */
    public ImaBlacklistRecord(
            final String path,
            final Digest hash,
            final String description,
            final ImaBlacklistBaseline baseline) {
        super(path, hash, description);
        if (path == null && hash == null) {
            throw new IllegalArgumentException("Cannot instantiate with both a null path and hash");
        }

        if (path != null && StringUtils.isEmpty(path)) {
            throw new IllegalArgumentException(
                    "Cannot instantiate with an empty (and non-null) path"
            );
        }
        this.baseline = baseline;
    }

    /**
     * Zero-arg constructor necessary for Hibernate.
     */
    protected ImaBlacklistRecord() {
        super();
    }

    /**
     * Retrieve the IMA blacklist baseline that this record belongs to.
     *
     * @return this record's owning blacklist baseline
     */
    public ImaBlacklistBaseline getBaseline() {
        return baseline;
    }

    /**
     * Set this record's associated blacklist baseline.
     *
     * @param baseline the blacklist baseline to associate this record with
     */
    public void setBaseline(final ImaBlacklistBaseline baseline) {
        this.baseline = baseline;
    }

    /**
     * Get the alert match type that should be raised for a measurement record that matches this
     * baseline record.
     *
     * @return the alert match type
     */
    public AlertType getAlertMatchType() {
        if (getPath() == null) {
            return AlertType.IMA_BLACKLIST_HASH_MATCH;
        } else if (getHash() == null) {
            return AlertType.IMA_BLACKLIST_PATH_MATCH;
        } else {
            return AlertType.IMA_BLACKLIST_PATH_AND_HASH_MATCH;
        }
    }
}
