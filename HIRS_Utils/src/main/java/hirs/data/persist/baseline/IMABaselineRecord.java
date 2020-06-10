package hirs.data.persist.baseline;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.google.common.base.Preconditions;
import hirs.data.persist.Digest;

/**
 * An <code>IMABaselineRecord</code> represents a single entry in an
 * {@link ImaAcceptableRecordBaseline}.  These contain paths and hashes of expected
 * entries in a machine's IMA log, and are used in the contexts of whitelists and required
 * sets via ImaAcceptableRecordBaselines.
 */
@Entity
@Table(indexes = { @Index(columnList = "bucket") })
public class IMABaselineRecord extends AbstractImaBaselineRecord {
    /**
     * IMABaselineRecords are randomly assigned buckets based on a hash of their path.  These
     * bucket values are used to artificially segment the baseline into equal divisions for
     * simultaneous multithreaded retrieval.  This defines the number of distinct bucket values that
     * will be used in this process.
     */
    public static final int FILENAME_HASH_BUCKET_COUNT = 4;

    /**
     * Holds the name of the 'bucket' field.
     */
    public static final String BUCKET_FIELD = "bucket";

    @Column(name = BUCKET_FIELD, nullable = false)
    private final int bucket;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ima_baseline_id")
    private SimpleImaBaseline baseline;

    @Transient
    private static final Pattern RECORD_PATTERN = Pattern.compile("\\((.*), (.*)\\)");

    /**
     * Creates a new <code>IMABaselineRecord</code>.
     * @param path
     *          file path of the record
     * @param hash
     *          hash of the record
     */
    public IMABaselineRecord(final String path, final Digest hash) {
        super(path, hash, null);
        Preconditions.checkNotNull(path, "Path cannot be null.");
        Preconditions.checkNotNull(hash, "Hash cannot be null.");
        this.bucket = getBucket(path);
    }

    /**
     * Creates a new <code>IMABaselineRecord</code>. Creates a new record and
     * specifies all of the properties. All of the properties may be null
     * except for path and hash.
     *
     * @param path
     *            file path
     * @param hash
     *            file SHA-1 hash
     * @param baseline
     *            baseline assigned to the record (nullable)
     * @throws IllegalArgumentException
     *             if digest algorithm is not SHA-1
     */
    public IMABaselineRecord(final String path, final Digest hash, final SimpleImaBaseline baseline)
            throws IllegalArgumentException {
        this(path, hash);
        setBaselineForRecordManager(baseline);
    }

    /**
     * Returns the 'bucket' of the given path (based on its hash).
     *
     * @param path the path to hash
     * @return the hash of the path
     */
    private static int getBucket(final String path) {
        if (path == null) {
            throw new IllegalArgumentException("Cannot get bucket for null value");
        }
        return Math.abs(getPartialPath(path).hashCode()) % FILENAME_HASH_BUCKET_COUNT;
    }

    /**
     * Default constructor necessary for Hibernate.
     */
    protected IMABaselineRecord() {
        super();
        this.bucket = 0;
    }

    /**
     * This gets the baseline.
     *
     * @return Baseline
     */
    public final ImaBaseline getBaseline() {
        return baseline;
    }

    /**
     * Sets the given baseline.
     *
     * @param baseline baseline that matches the given baseline
     */
    public final void setBaseline(final SimpleImaBaseline baseline) {
        setOnlyBaseline(baseline);
        if (baseline != null) {
            baseline.addOnlyToBaseline(this);
        }
    }

    /**
     * Sets the baseline for this record.
     *
     * @param baseline
     *            baseline or null
     */
    final void setOnlyBaseline(final SimpleImaBaseline baseline) {
        if (this.baseline != null && baseline != null) {
            this.baseline.removeOnlyBaseline(this);
        }

        this.baseline = baseline;
    }

    /**
     * This method is to be used strictly for when the record is being added or modified by the
     * <code>ImaBaselineRecordManager</code>.  The methods
     * {@link #setBaseline(hirs.data.persist.SimpleImaBaseline)} and {@link
     * #setOnlyBaseline(hirs.data.persist.SimpleImaBaseline)} will still need to exist for
     * use with the <code>BaselineManager</code>
     *
     * @param baseline
     *          SimpleImaBaseline that will be set and persisted by the
     *          <code>ImaBaselineRecordManager</code>
     */
    public final void setBaselineForRecordManager(final SimpleImaBaseline baseline) {
        this.baseline = baseline;
    }

    /**
     * Reverses the toString operation. Throws an IllegalArgumentException if an invalid String is
     * passed in
     *
     * @param record
     *            String representation of the IMABaselineRecord
     * @return IMABaselineRecord
     */
    public static IMABaselineRecord fromString(final String record) {
        Matcher m = RECORD_PATTERN.matcher(record);
        m.matches();
        if (m.groupCount() != 2) {
            String msg = String.format("Unexpected number of groups found with pattern \"%s\" "
                    + "on string \"%s\"", RECORD_PATTERN.toString(), record);
            throw new IllegalArgumentException(msg);
        }
        String path = m.group(1);
        String digestString = m.group(2);
        Digest digest = Digest.fromString(digestString);
        return new IMABaselineRecord(path, digest);
    }
}
