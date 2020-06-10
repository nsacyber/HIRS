package hirs.data.persist.baseline;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import hirs.data.persist.IMAMeasurementRecord;
import hirs.data.persist.IMAPolicy;
import hirs.ima.matching.BatchImaMatchStatus;
import hirs.ima.matching.ImaAcceptableHashRecordMatcher;
import hirs.ima.matching.ImaAcceptablePathAndHashRecordMatcher;
import hirs.persist.ImaBaselineRecordManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * Class represents a simple, flexible IMA measurement baseline. A baseline contains one or more
 * IMA records. A baseline is used to define a collection of approved files that
 * may be accessed or executed on an IMA supported computing platform. Each IMA
 * record contains information pertaining to a specific file including file name
 * and path, file hash, and other file attributes.
 * <p>
 * An IMA record can be either a full path record or a partial path record. Both
 * types can always be added to a baseline, but when a baseline is checked using
 * the contains() method, a parameter is passed in to determine whether or not
 * partial path records can be considered.
 * <p>
 * NOTE: This class is not thread-safe.
 */
@Entity
@Access(AccessType.FIELD)
public class SimpleImaBaseline extends ImaAcceptableRecordBaseline {
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER,
            orphanRemoval = true, mappedBy = "baseline")
    @Access(AccessType.PROPERTY)
    @JsonIgnore
    private Set<IMABaselineRecord> imaRecords;

    @Column(nullable = true)
    private URL yumRepoURL;

    private static final Logger LOGGER = getLogger(ImaBaseline.class);

    /**
     * Constructor used to initialize ImaBaseline object. Partial path support
     * is not enabled by default. Makes an empty <code>Set</code> of
     * <code>IMABaselineRecord</code>s and initializes the date to the current
     * date.
     *
     * @param name
     *            a name used to uniquely identify and reference the IMA
     *            baseline
     */
    public SimpleImaBaseline(final String name) {
        super(name);
        imaRecords = new LinkedHashSet<>();
    }

    /**
     * Constructor used to initialize an ImaBaseline object with a link to a Yum repository.
     *
     * @param name the name for this ImaBaseline
     * @param yumRepoURL the base URL of the repository from which to add package measurements
     */
    public SimpleImaBaseline(final String name, final URL yumRepoURL) {
        this(name);
        this.yumRepoURL = yumRepoURL;
    }

    /**
     * Default constructor necessary for Hibernate and BaselineAlertResolver. Makes an empty
     * <code>Set</code> of <code>IMABaselineRecord</code>s.
     */
    public SimpleImaBaseline() {
        super();
        imaRecords = new LinkedHashSet<>();
    }

    /**
     * Returns an unmodifiable set of IMA baseline records found in the IMA
     * baseline. The returned set only contains the baseline records from this
     * baseline.
     *
     * @return list of IMA records
     */
    public final synchronized Set<IMABaselineRecord> getBaselineRecords() {
        return Collections.unmodifiableSet(imaRecords);
    }

    /**
     * Adds a record to this baseline. If the record is not already part of the baseline,
     * then it is added. If an equal record is already part of the baseline, then the
     * request to add the record will be quietly ignored. This method also sets
     * <code>Baseline</code> field on the <code>IMABaselineRecord</code>.
     *
     * @param record
     *            record to add to the baseline
     * @return
     *      returns true if record was added to the baseline, false if it wasn't
     */
    public final boolean addToBaseline(final IMABaselineRecord record) {
        record.setOnlyBaseline(this);
        return addOnlyToBaseline(record);
    }

    /**
     * Remove IMA record from baseline.
     *
     * @param record
     *            to remove from baseline
     * @return a boolean indicated whether or not the ima record was
     *         successfully removed from the list.
     */
    public final boolean removeFromBaseline(final IMABaselineRecord record) {
        LOGGER.debug("removing record {} from baseline {}", record, getName());
        if (record == null) {
            LOGGER.error("null record");
            return false;
        }

        boolean retVal = imaRecords.remove(record);
        if (retVal) {
            record.setBaseline(null);
        }
        LOGGER.debug("record removed: {}", record);
        return retVal;
    }

    /**
     * Tests whether a collection of records are found in the baseline. This returns a
     * <code>BatchImaMatchStatus</code> representing the result of the search for
     * the records, which is itself a mapping of IMAMeasurementRecords to IMAMatchStatuses.
     * IMAMatchStatus conditions are as follows:
     * <ul>
     * <li>MATCH - if an <code>IMABaselineRecord</code> is found with a matching
     * path and hash</li>
     * <li>MISMATCH - if at least one <code>IMABaselineRecord</code> is found
     * with a matching path but none with a matching path and hash</li>
     * <li>UNKNOWN - if no <code>IMABaselineRecord</code>s found with a matching
     * path</li>
     * </ul>
     * <p>
     * If partial paths are enabled, records not starting with '/' are compared
     * against all baseline records not starting with '/' and the last segment
     * (the filename after the last '/') of each full path baseline record.
     * Records starting with '/' are compared against all full path baseline
     * records and the last segment of the record is compared against all
     * partial path baseline records.
     * <p>
     * If partial paths are disabled, records are only compared using the full
     * path of the baseline record and the report record.
     *
     * @param records
     *            measurement records
     * @param recordManager
     *            an ImaBaselineRecordManager that can be used to retrieve persisted records
     * @param imaPolicy
     *            the IMA policy to use while determining if a baseline contains the records
     * @return search status for the measurement record
     */
    @Override
    public final BatchImaMatchStatus<IMABaselineRecord> contains(
            final Collection<IMAMeasurementRecord> records,
            final ImaBaselineRecordManager recordManager,
            final IMAPolicy imaPolicy) {
        Preconditions.checkArgument(records != null, "Records cannot be null");
        Preconditions.checkArgument(imaPolicy != null, "IMA policy cannot be null");

        return new ImaAcceptablePathAndHashRecordMatcher(imaRecords, imaPolicy, this)
                .batchMatch(records);
    }


    @Override
    public BatchImaMatchStatus<IMABaselineRecord> containsHashes(
            final Collection<IMAMeasurementRecord> records,
            final ImaBaselineRecordManager recordManager,
            final IMAPolicy imaPolicy) {
        Preconditions.checkArgument(records != null, "Records cannot be null");
        Preconditions.checkArgument(imaPolicy != null, "IMA policy cannot be null");

        return new ImaAcceptableHashRecordMatcher(imaRecords, imaPolicy, this)
                .batchMatch(records);
    }

    @Override
    public Collection<IMABaselineRecord> getRecordsExcept(
            final ImaBaselineRecordManager recordManager,
            final Set<IMABaselineRecord> foundRecords) {
        if (foundRecords == null) {
            throw new IllegalArgumentException("foundRecords cannot be null");
        }

        if (recordManager == null) {
            throw new IllegalArgumentException("ImaBaselineRecordManager cannot be null");
        }

        Set<IMABaselineRecord> leftover = new HashSet<>();
        leftover.addAll(imaRecords);
        leftover.removeAll(foundRecords);
        return leftover;
    }

    /**
     * Set the Yum Repo URL.
     *
     * @param yumRepoURL a URL to the yum repository that measurements will be gathered from
     */
    public final void setYumRepoURL(final URL yumRepoURL) {
        this.yumRepoURL = yumRepoURL;
    }

    /**
     * Retrieve the Yum Repo URL.
     *
     * @return the Yum repository URL
     */
    public final URL getYumRepoURL() {
        return yumRepoURL;
    }

    /**
     * Adds an IMA record to this IMA baseline. If the record does not exist
     * then it is added. If an equal record exists, based upon
     * {@link IMABaselineRecord#equals(Object)}, then this method quietly
     * ignores the request to add the record because one already exists in the
     * baseline.
     *
     * @param record
     *            record to add to baseline
     * @return
     *      returns true is the record was added to the list, false if not
     */
    final synchronized boolean addOnlyToBaseline(final IMABaselineRecord record) {
        LOGGER.debug("adding record {} to baseline {}", record, getName());
        if (record == null) {
            LOGGER.error("null record");
            throw new NullPointerException("record");
        }

        if (imaRecords.contains(record)) {
            final String msg = String.format(
                    "record already exists in baseline: %s", record);
            LOGGER.debug(msg);

            return false;

        } else  {
            imaRecords.add(record);
            LOGGER.debug("record added: {}", record);
        }

        return true;
    }

    /**
     * Remove record from the baseline.
     *
     * @param record
     *            record to remove
     * @return a boolean indicating if the removal was successful
     */
    final boolean removeOnlyBaseline(final IMABaselineRecord record) {
        return imaRecords.remove(record);
    }

    /**
     * Returns the actual <code>Set</code> that contains the IMA records. See
     * {@link #setImaRecords(Set)} for more details on why this is needed.
     *
     * @return IMA records
     */
    private Set<IMABaselineRecord> getImaRecords() {
        return imaRecords;
    }

    /**
     * Sets the IMA records. This is needed for Hibernate. The issue is that
     * this class needs to be aware of when the <code>imaRecords</code>
     * <code>Set</code> is set. This allows isUpdateNeeded to be set, indicating
     * that the transient elements fullMap and partialMap should be populated
     * using the imaRecords set that comes back from the database. This method
     * should only be invoked by Hibernate.
     * <p>
     * Hibernate cannot use a public method like
     * {@link #getAllMeasurementRecords()} and a corresponding private setter
     * method. The reason is that the set pointer would be changing and confuse
     * Hibernate.
     *
     * @param imaRecords IMA records
     */
    private void setImaRecords(final Set<IMABaselineRecord> imaRecords) {
        this.imaRecords = imaRecords;
    }
}
